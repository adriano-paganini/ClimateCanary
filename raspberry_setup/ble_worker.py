import asyncio
import struct
import time
from bleak import BleakClient

from config import (
    DATA_CHAR_UUID,
    AUTH_CHAR_UUID,
    SENSOR_STRUCT,
    SENSOR_STATUS_UUID,
    CACHED_DATA_UUID,
    CACHED_DATA_ACK_UUID,
    WARNING_ACK_UUID,
    WARNING_TOTAL_LEN_UUID,
    WARNING_CHAR_PACK_UUID,
    WARNING_ACK_REQUEST_UUID,
)

def _build_auth(device_id: int, room_name: str) -> bytes:
    room_bytes = room_name.encode("ascii")
    room_len = len(room_bytes)
    assert room_len <= 32, "room_name darf max. 32 Zeichen haben"
    return struct.pack("<I32sB", device_id, room_bytes.ljust(32, b"\x00"), room_len)


def _build_status(timestamp_ms: int,
                  pressure_s: int = 0, temp_s: int = 0,
                  hum_s: int = 0, gas_s: int = 0) -> bytes:
    code = (
        (pressure_s & 0xF)
        | ((temp_s  & 0xF) << 4)
        | ((hum_s   & 0xF) << 8)
        | ((gas_s   & 0xF) << 12)
    )
    return struct.pack("<IH", timestamp_ms, code)


def _build_all_good(timestamp_ms: int) -> bytes:
    return _build_status(timestamp_ms, 5, 5, 5, 5)


def _is_critical(*statuses: int) -> bool:
    return any(s in {3, 4} for s in statuses)


def _build_warning_stream(messages: list[str]) -> bytes:
    return b"".join(m.encode("utf-8") + b"\x00" for m in messages)


def _evaluate(pkt: dict) -> tuple[int, int, int, int]:
    def classify(v, lo_warn, lo_crit, hi_warn, hi_crit):
        if   v < lo_crit:  return 3
        elif v > hi_crit:  return 4
        elif v < lo_warn:  return 1
        elif v > hi_warn:  return 2
        else:              return 0

    return (
        classify(pkt["pressure"],        950,   930,  1050, 1070),
        classify(pkt["temperature"],      16,    10,    30,   35),
        classify(pkt["humidity"],         30,    20,    70,   80),
        classify(pkt["air_quality"], 5000,  2000, 50000, 100000),
    )


def _warning_messages(pkt: dict, press_s, temp_s, hum_s, gas_s) -> list[str]:
    labels = {
        "Druck":            (press_s, pkt["pressure"],       "hPa"),
        "Temperatur":       (temp_s,  pkt["temperature"],    "°C"),
        "Luftfeuchtigkeit": (hum_s,   pkt["humidity"],       "%"),
        "Gaswiderstand":    (gas_s,   pkt["air_quality"], "Ω"),
    }
    desc = {3: "langfristig zu niedrig", 4: "langfristig zu hoch"}
    msgs = [
        f"{name} {desc[s]}: {val:.1f} {unit}"
        for name, (s, val, unit) in labels.items()
        if s in {3, 4}
    ]
    return msgs or ["Kritischer Umgebungszustand erkannt"]


async def _send_warning(client: BleakClient, messages: list[str], tag: str) -> None:
    stream = _build_warning_stream(messages)
    print(f"[BLE:{tag}] warning transfer start ({len(stream)}B): {messages}")

    await client.write_gatt_char(
        WARNING_TOTAL_LEN_UUID,
        struct.pack("<H", len(stream)),
        response=True,
    )
    await client.write_gatt_char(
        WARNING_CHAR_PACK_UUID,
        struct.pack("<Hc", 0, stream[0:1]),
        response=True,
    )

    if len(stream) == 1:
        print(f"[BLE:{tag}] warning transfer complete.")
        return

    done = asyncio.Event()

    async def on_ack(sender, data: bytearray) -> None:
        (seq,) = struct.unpack("<H", bytes(data))
        if seq >= len(stream):
            print(f"[BLE:{tag}] warning transfer complete.")
            done.set()
            return
        await client.write_gatt_char(
            WARNING_CHAR_PACK_UUID,
            struct.pack("<Hc", seq, stream[seq : seq + 1]),
            response=True,
        )

    await client.start_notify(WARNING_ACK_REQUEST_UUID, on_ack)
    try:
        await asyncio.wait_for(done.wait(), timeout=60.0)
    except asyncio.TimeoutError:
        print(f"[BLE:{tag}] warning transfer timeout!")
    finally:
        await client.stop_notify(WARNING_ACK_REQUEST_UUID)

async def ble_worker(
    queue: asyncio.Queue,
    client: BleakClient,
    device_id: int,
    room_name: str,
) -> None:
    """
    Vollständiger BLE-Workflow für genau ein Gerät.
    Wird von main.py pro Station in einer eigenen Task aufgerufen.

    device_id und room_name kommen vom Backend, nicht mehr aus config.
    """
    tag = client.address

    payload = _build_auth(device_id, room_name)
    await client.write_gatt_char(AUTH_CHAR_UUID, payload, response=True)
    print(f"[BLE:{tag}] authenticated (deviceId={device_id}, room={room_name!r})")

    boot_offset: float | None = None

    def register_ts(device_ms: int) -> None:
        nonlocal boot_offset
        if boot_offset is None:
            boot_offset = time.time() - device_ms / 1000.0

    def to_unix(device_ms: int) -> float:
        return (boot_offset or 0.0) + device_ms / 1000.0

    print(f"[BLE:{tag}] reading cached sensor data…")
    cached_count = 0
    while True:
        await client.write_gatt_char(CACHED_DATA_ACK_UUID, b"\x01", response=True)
        raw = await client.read_gatt_char(CACHED_DATA_UUID)
        if not raw or len(raw) < 20:
            break
        ts, press, temp, hum, gas = struct.unpack(SENSOR_STRUCT, bytes(raw))
        register_ts(ts)
        queue.put_nowait({
            "timestamp":      ts,
            "unix_time":      to_unix(ts),
            "temperature":    temp,
            "humidity":       hum,
            "pressure":       press,
            "air_quality": gas,
            "device_id":      device_id,
            "room_name":      room_name,
        })
        cached_count += 1
    print(f"[BLE:{tag}] cached packets: {cached_count}")

    warning_active = False

    def on_warning_ack(sender, data: bytearray) -> None:
        nonlocal warning_active
        if data and data[0]:
            print(f"[BLE:{tag}] warning acknowledged.")
            warning_active = False

    await client.start_notify(WARNING_ACK_UUID, on_warning_ack)

    async def handle_notification(sender, data: bytearray) -> None:
        nonlocal warning_active

        try:
            ts, press, temp, hum, gas = struct.unpack(SENSOR_STRUCT, bytes(data))
        except Exception as e:
            print(f"[BLE:{tag}] parse error: {e}")
            return

        register_ts(ts)
        pkt = {
            "timestamp":      ts,
            "unix_time":      to_unix(ts),
            "temperature":    temp,
            "humidity":       hum,
            "pressure":       press,
            "air_quality": gas,
            "device_id":      device_id,
            "room_name":      room_name,
        }
        queue.put_nowait(pkt)
        print(f"[BLE:{tag}] {ts}: {temp:.1f}°C {hum:.1f}% {press:.1f}hPa {gas}Ω")

        press_s, temp_s, hum_s, gas_s = _evaluate(pkt)

        status_payload = _build_status(ts, press_s, temp_s, hum_s, gas_s)
        await client.write_gatt_char(SENSOR_STATUS_UUID, status_payload, response=True)

        if _is_critical(press_s, temp_s, hum_s, gas_s) and not warning_active:
            warning_active = True
            msgs = _warning_messages(pkt, press_s, temp_s, hum_s, gas_s)
            await _send_warning(client, msgs, tag)
            await client.write_gatt_char(SENSOR_STATUS_UUID, status_payload, response=True)

        if warning_active and all(s == 5 for s in (press_s, temp_s, hum_s, gas_s)):
            await client.write_gatt_char(
                SENSOR_STATUS_UUID, _build_all_good(ts), response=True
            )
            print(f"[BLE:{tag}] all-good sent (0x5555).")
            warning_active = False

    await client.start_notify(DATA_CHAR_UUID, handle_notification)
    print(f"[BLE:{tag}] subscribed to notifications")

    try:
        while True:
            await asyncio.sleep(1)
    finally:
        await client.stop_notify(DATA_CHAR_UUID)
        await client.stop_notify(WARNING_ACK_UUID)