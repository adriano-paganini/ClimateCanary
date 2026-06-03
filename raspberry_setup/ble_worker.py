import asyncio
import logging
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

log = logging.getLogger(__name__)


def _build_auth(pi_id: int, room_name: str) -> bytes:
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
    return b"".join(m.encode("utf-8") + b"\x00" for m in messages)


async def _send_warning(client: BleakClient, messages: list[str], tag: str) -> bool:
    """Returns True if the transfer completed successfully, False on timeout."""
    stream = _build_warning_stream(messages)
    total_len = len(stream)
    log.info(f"[BLE:{tag}] warning transfer start ({total_len}B): {messages}")

    ack_queue: asyncio.Queue[int] = asyncio.Queue()

    def on_ack(sender, data: bytearray) -> None:
        (seq,) = struct.unpack("<H", bytes(data))
        ack_queue.put_nowait(seq)

    await client.start_notify(WARNING_ACK_REQUEST_UUID, on_ack)
    try:
        await client.write_gatt_char(
            WARNING_TOTAL_LEN_UUID,
            struct.pack("<H", total_len),
            response=True,
        )
        await client.write_gatt_char(
            WARNING_CHAR_PACK_UUID,
            struct.pack("<Hc", 0, stream[0:1]),
            response=True,
        )

        loop = asyncio.get_running_loop()
        deadline = loop.time() + 60.0

        while True:
            remaining = deadline - loop.time()
            if remaining <= 0:
                log.error(f"[BLE:{tag}] warning transfer timeout")
                return False
            try:
                seq = await asyncio.wait_for(ack_queue.get(), timeout=remaining)
            except asyncio.TimeoutError:
                log.error(f"[BLE:{tag}] warning transfer timeout")
                return False

            if seq >= total_len:
                log.info(f"[BLE:{tag}] warning transfer complete.")
                return True

            await client.write_gatt_char(
                WARNING_CHAR_PACK_UUID,
                struct.pack("<Hc", seq, stream[seq : seq + 1]),
                response=True,
            )
    finally:
        await client.stop_notify(WARNING_ACK_REQUEST_UUID)

def _make_iso(anchor_pi_time: float, anchor_arduino_millis: int, pkt_millis: int) -> str:
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
    log.info(f"[BLE:{tag}] authenticated (pi_id={config.PI_ID}, room={config.ROOM_NAME!r})")

    anchor_pi_time:        float | None = None
    anchor_arduino_millis: int   | None = None

    def _set_anchor(pkt_millis: int) -> None:
        nonlocal anchor_pi_time, anchor_arduino_millis
        if anchor_pi_time is None:
            anchor_pi_time        = time.time()
            anchor_arduino_millis = pkt_millis
            log.info(f"[BLE:{tag}] timestamp anchor set at pi={anchor_pi_time:.3f}, "
                     f"arduino_ms={anchor_arduino_millis}")

    def _timestamp(pkt_millis: int) -> str:
        return _make_iso(anchor_pi_time, anchor_arduino_millis, pkt_millis)

    log.info(f"[BLE:{tag}] reading cached sensor data…")
    cached_packets: list[tuple[int, float, float, float, float]] = []
    last_ts: int | None = None
    while True:
        await client.write_gatt_char(CACHED_DATA_ACK_UUID, b"\x01", response=True)
        raw = await client.read_gatt_char(CACHED_DATA_UUID)
        if not raw or len(raw) < 20:
            break
        ts, press, temp, hum, gas = struct.unpack(SENSOR_STRUCT, bytes(raw))
        if ts == last_ts:
            break
        last_ts = ts
        cached_packets.append((ts, press, temp, hum, gas))
    log.info(f"[BLE:{tag}] cached read done: {len(cached_packets)} packet(s) collected, timestamps deferred")

    async with aiohttp.ClientSession() as http_session:

        warning_active        = False
        first_packet_reported = False

        def on_warning_ack(sender, data: bytearray) -> None:
            nonlocal warning_active
            if data and data[0]:
                log.info(f"[BLE:{tag}] warning acknowledged by Arduino.")
                warning_active = False

        await client.start_notify(WARNING_ACK_UUID, on_warning_ack)

        async def handle_notification(sender, data: bytearray) -> None:
            nonlocal warning_active, cached_packets

            try:
                ts, press, temp, hum, gas = struct.unpack(SENSOR_STRUCT, bytes(data))
            except Exception as e:
                log.error(f"[BLE:{tag}] parse error: {e}")
                return

            _set_anchor(ts)

            if cached_packets:
                for i, (c_ts, c_press, c_temp, c_hum, c_gas) in enumerate(cached_packets):
                    c_pkt = {
                        "sensor_station_id": sensor_station_id,
                        "room_id":           room_id,
                        "timestamp":         _timestamp(c_ts),
                        "temperature":       c_temp,
                        "humidity":          c_hum,
                        "pressure":          c_press,
                        "air_quality":       c_gas,
                    }
                    log.info(f"[BLE:{tag}] cached [{i}] {c_pkt['timestamp']}: "
                             f"{c_temp:.1f}°C  {c_hum:.1f}%  {c_press:.1f}hPa  {c_gas:.1f}Ω")
                    if not get_privacy_mode():
                        queue.put_nowait(c_pkt)
                        log.info(f"[BLE:{tag}] cached [{i}] queued for upload")
                    else:
                        log.info(f"[BLE:{tag}] cached [{i}] suppressed (privacy mode)")
                log.info(f"[BLE:{tag}] cached replay done: {len(cached_packets)} packet(s) enqueued")
                cached_packets = []

            pkt = {
                "sensor_station_id": sensor_station_id,
                "room_id":           room_id,
                "timestamp":         _timestamp(ts),
                "temperature":       temp,
                "humidity":          hum,
                "pressure":          press,
                "air_quality":       gas,
            }

            log.info(f"[BLE:{tag}] {pkt['timestamp']}: {temp:.1f}°C  {hum:.1f}%  "
                     f"{press:.1f}hPa  {gas:.1f}Ω")

            statuses, hint_messages = await process_measurement(
                pkt, db, http_session, tag
            )

            if not get_privacy_mode():
                queue.put_nowait(pkt)
                log.info(f"[BLE:{tag}] measurement queued for upload")
            else:
                log.info(f"[BLE:{tag}] measurement suppressed (privacy mode)")

            nonlocal first_packet_reported
            if not first_packet_reported:
                first_packet_reported = True
                try:
                    url = f"{config.BACKEND_URL}/api/cpi/{config.PI_ID}/{sensor_station_id}"
                    async with http_session.patch(
                        url, json="CONNECTED",
                        timeout=_aiohttp.ClientTimeout(total=5),
                    ) as resp:
                        log.info(f"[BLE:{tag}] PATCH CONNECTED → {resp.status}")
                except Exception as e:
                    log.warning(f"[BLE:{tag}] PATCH CONNECTED failed: {e}")

            press_s = statuses.get("pressure",    0)
            temp_s  = statuses.get("temperature", 0)
            hum_s   = statuses.get("humidity",    0)
            gas_s   = statuses.get("air_quality", 0)

            log.info(f"[BLE:{tag}] statuses → press={press_s} temp={temp_s} "
                     f"hum={hum_s} gas={gas_s}")
            status_payload = _build_status(ts, press_s, temp_s, hum_s, gas_s)
            await client.write_gatt_char(
                SENSOR_STATUS_UUID, status_payload, response=True
            )
            log.info(f"[BLE:{tag}] status written to Arduino")

            if hint_messages:
                log.info(f"[BLE:{tag}] {len(hint_messages)} hint message(s): {hint_messages}")
            if hint_messages and not warning_active:
                warning_active = True  # block concurrent sends during transfer
                warning_active = await _send_warning(client, hint_messages, tag)
                await client.write_gatt_char(
                    SENSOR_STATUS_UUID, status_payload, response=True
                )

            if all(s == 5 for s in (press_s, temp_s, hum_s, gas_s)) and warning_active:
                await client.write_gatt_char(
                    SENSOR_STATUS_UUID, _build_all_good(ts), response=True
                )
                log.info(f"[BLE:{tag}] all-good sent.")
                warning_active = False

        await client.start_notify(DATA_CHAR_UUID, handle_notification)
        log.info(f"[BLE:{tag}] subscribed to notifications")

        try:
            while True:
                await asyncio.sleep(1)
        finally:
            await client.stop_notify(DATA_CHAR_UUID)
            await client.stop_notify(WARNING_ACK_UUID)
