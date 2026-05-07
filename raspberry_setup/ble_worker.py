import asyncio
import struct
import time
from datetime import datetime

import aiohttp
import aiosqlite
from bleak import BleakClient

import config
import aiohttp as _aiohttp
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
from state import get_privacy_mode
from violation_tracker import process_measurement


def _build_auth(pi_id: int, room_name: str) -> bytes:
    """Write TrustedRpiId + room_name into warningAuthCharacteristic."""
    room_bytes = room_name.encode("ascii")
    room_len   = len(room_bytes)
    assert room_len <= 32, "room_name must be ≤ 32 characters"
    return struct.pack("<I32sB", pi_id, room_bytes.ljust(32, b"\x00"), room_len)


def _build_status(
    timestamp_ms: int,
    pressure_s: int = 0, temp_s: int = 0,
    hum_s: int = 0, gas_s: int = 0,
) -> bytes:
    code = (
        (pressure_s & 0xF)
        | ((temp_s & 0xF) << 4)
        | ((hum_s  & 0xF) << 8)
        | ((gas_s  & 0xF) << 12)
    )
    return struct.pack("<IH", timestamp_ms, code)


def _build_all_good(timestamp_ms: int) -> bytes:
    return _build_status(timestamp_ms, 5, 5, 5, 5)


def _build_warning_stream(messages: list[str]) -> bytes:
    """Concatenate null-terminated UTF-8 strings into the wire format."""
    return b"".join(m.encode("utf-8") + b"\x00" for m in messages)


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

def _make_iso(anchor_pi_time: float, anchor_arduino_millis: int, pkt_millis: int) -> str:
    """
    Convert Arduino-relative millis to a Pi wall-clock ISO 8601 string.
    anchor_pi_time and anchor_arduino_millis are captured together on the
    first packet so relative timing between measurements is preserved.
    """
    unix_seconds = anchor_pi_time + (pkt_millis - anchor_arduino_millis) / 1000.0
    return datetime.fromtimestamp(unix_seconds).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3]


async def ble_worker(
    queue: asyncio.Queue,
    client: BleakClient,
    sensor_station_id: int,
    room_id: int,
    measurement_interval: int,
    db: aiosqlite.Connection,
) -> None:
    tag = client.address

    await client.write_gatt_char(
        AUTH_CHAR_UUID,
        _build_auth(config.PI_ID, config.ROOM_NAME),
        response=True,
    )
    print(f"[BLE:{tag}] authenticated (pi_id={config.PI_ID}, room={config.ROOM_NAME!r})")

    anchor_pi_time:        float | None = None
    anchor_arduino_millis: int   | None = None

    def _set_anchor(pkt_millis: int) -> None:
        nonlocal anchor_pi_time, anchor_arduino_millis
        if anchor_pi_time is None:
            anchor_pi_time        = time.time()
            anchor_arduino_millis = pkt_millis
            print(f"[BLE:{tag}] timestamp anchor set at pi={anchor_pi_time:.3f}, "
                  f"arduino_ms={anchor_arduino_millis}")

    def _timestamp(pkt_millis: int) -> str:
        return _make_iso(anchor_pi_time, anchor_arduino_millis, pkt_millis)


    print(f"[BLE:{tag}] reading cached sensor data…")
    cached_count = 0
    while True:
        await client.write_gatt_char(CACHED_DATA_ACK_UUID, b"\x01", response=True)
        raw = await client.read_gatt_char(CACHED_DATA_UUID)
        if not raw or len(raw) < 20:
            break
        ts, press, temp, hum, gas = struct.unpack(SENSOR_STRUCT, bytes(raw))
        _set_anchor(ts)
        pkt = {
            "sensor_station_id": sensor_station_id,
            "room_id":           room_id,
            "timestamp":         _timestamp(ts),   # ISO string → stored + sent to backend
            "temperature":       temp,
            "humidity":          hum,
            "pressure":          press,
            "air_quality":       gas,
        }
        if not get_privacy_mode():
            queue.put_nowait(pkt)
        cached_count += 1
    print(f"[BLE:{tag}] cached packets: {cached_count}")

    async with aiohttp.ClientSession() as http_session:

        warning_active        = False
        first_packet_reported = False  # PATCH CONNECTED after first data packet

        def on_warning_ack(sender, data: bytearray) -> None:
            nonlocal warning_active
            if data and data[0]:
                print(f"[BLE:{tag}] warning acknowledged by Arduino.")
                warning_active = False

        await client.start_notify(WARNING_ACK_UUID, on_warning_ack)

        async def handle_notification(sender, data: bytearray) -> None:
            nonlocal warning_active

            try:
                ts, press, temp, hum, gas = struct.unpack(SENSOR_STRUCT, bytes(data))
            except Exception as e:
                print(f"[BLE:{tag}] parse error: {e}")
                return

            _set_anchor(ts)
            pkt = {
                "sensor_station_id": sensor_station_id,
                "room_id":           room_id,
                "timestamp":         _timestamp(ts),   # ISO string → stored + sent to backend
                "temperature":       temp,
                "humidity":          hum,
                "pressure":          press,
                "air_quality":       gas,
            }

            print(f"[BLE:{tag}] {pkt['timestamp']}: {temp:.1f}°C  {hum:.1f}%  "
                  f"{press:.1f}hPa  {gas}Ω")

            statuses, hint_messages = await process_measurement(
                pkt, db, http_session, tag
            )

            if not get_privacy_mode():
                queue.put_nowait(pkt)

            nonlocal first_packet_reported
            if not first_packet_reported:
                first_packet_reported = True
                try:
                    url = f"{config.BACKEND_URL}/api/cpi/{config.PI_ID}/{sensor_station_id}"
                    async with http_session.patch(
                        url, json={"deviceStatus": "CONNECTED"},
                        timeout=_aiohttp.ClientTimeout(total=5),
                    ) as resp:
                        print(f"[BLE:{tag}] PATCH CONNECTED → {resp.status}")
                except Exception as e:
                    print(f"[BLE:{tag}] PATCH CONNECTED failed: {e}")

            press_s = statuses.get("pressure",    0)
            temp_s  = statuses.get("temperature", 0)
            hum_s   = statuses.get("humidity",    0)
            gas_s   = statuses.get("air_quality", 0)

            status_payload = _build_status(ts, press_s, temp_s, hum_s, gas_s)
            await client.write_gatt_char(
                SENSOR_STATUS_UUID, status_payload, response=True
            )

            if hint_messages and not warning_active:
                warning_active = True
                await _send_warning(client, hint_messages, tag)
                await client.write_gatt_char(
                    SENSOR_STATUS_UUID, status_payload, response=True
                )

            if all(s == 5 for s in (press_s, temp_s, hum_s, gas_s)) and warning_active:
                await client.write_gatt_char(
                    SENSOR_STATUS_UUID, _build_all_good(ts), response=True
                )
                print(f"[BLE:{tag}] all-good sent.")
                warning_active = False

        await client.start_notify(DATA_CHAR_UUID, handle_notification)
        print(f"[BLE:{tag}] subscribed to notifications")

        try:
            while True:
                await asyncio.sleep(1)
        finally:
            await client.stop_notify(DATA_CHAR_UUID)
            await client.stop_notify(WARNING_ACK_UUID)