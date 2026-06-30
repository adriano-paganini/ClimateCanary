#!/usr/bin/env python3
"""Interactive BLE test UI for the Climate Canary prototype.

Requires:
    pip install bleak

This app provides:
- Scan / connect / disconnect
- Initial setup flow for fresh G5T4SETUP devices
- Login/authentication flow
- Predefined warning flow from comma-separated warning strings
- Manual raw-hex write access for every known characteristic using human-readable names
- Structured helpers for writable characteristics plus optional notifications
- Manual buffered-history fetch workflow after authentication
"""

from __future__ import annotations

import asyncio
import queue
import struct
import threading
import time
import tkinter as tk
from dataclasses import dataclass
from tkinter import messagebox, ttk
from typing import Any, Callable, Dict, List, Optional

from bleak import BleakClient, BleakScanner


DEVICE_NAME_DEFAULT = "G5T4CC"
SETUP_DEVICE_NAME_DEFAULT = "G5T4SETUP"

ENVIRONMENTAL_SENSING_SERVICE_UUID = "181A"
DEVICE_SETUP_SERVICE_UUID = "94050000-af44-4a64-b339-8b04d5565014"
WARNING_CONTROL_SERVICE_UUID = "bda70000-24ff-4f28-af24-8293a69561ca"


@dataclass(frozen=True)
class CharDef:
    key: str
    name: str
    uuid: str
    readable: bool
    writable: bool
    notifiable: bool
    notes: str = ""


CHARS: Dict[str, CharDef] = {
    "sensor_packet": CharDef(
        key="sensor_packet",
        name="Sensor Packet",
        uuid="4b8e0001-2581-4c5c-8a61-deb186a46179",
        readable=True,
        writable=False,
        notifiable=True,
        notes="SensorDataPacket: timestamp:uint32 + pressure/temp/humidity/IAQ floats",
    ),
    "sensor_status": CharDef(
        key="sensor_status",
        name="Sensor Packet Status",
        uuid="4b8e0002-2581-4c5c-8a61-deb186a46179",
        readable=True,
        writable=True,
        notifiable=False,
        notes="timestamp:uint32 + statusCode:uint16",
    ),
    "cached_sensor_data": CharDef(
        key="cached_sensor_data",
        name="Cached Sensor Data",
        uuid="4b8e0003-2581-4c5c-8a61-deb186a46179",
        readable=True,
        writable=True,
        notifiable=False,
        notes="Buffered SensorDataPacket; timestamp 0 marks transfer complete",
    ),
    "cached_sensor_data_ack": CharDef(
        key="cached_sensor_data_ack",
        name="Cached Sensor Data ACK",
        uuid="4b8e0004-2581-4c5c-8a61-deb186a46179",
        readable=False,
        writable=True,
        notifiable=True,
        notes="bool; write true to request next cached packet",
    ),
    "setup": CharDef(
        key="setup",
        name="Setup Config",
        uuid="94050001-af44-4a64-b339-8b04d5565014",
        readable=False,
        writable=True,
        notifiable=False,
        notes="Packed as <BI> (measurementInterval:uint8 3..60, deviceId:uint32)",
    ),
    "auth": CharDef(
        key="auth",
        name="Authentication Packet",
        uuid="bda70001-24ff-4f28-af24-8293a69561ca",
        readable=False,
        writable=True,
        notifiable=False,
        notes="id:uint32 + roomName[32] + roomNameLen:uint8",
    ),
    "warning_total_len": CharDef(
        key="warning_total_len",
        name="Warning Message Total Length",
        uuid="bda70003-24ff-4f28-af24-8293a69561ca",
        readable=True,
        writable=True,
        notifiable=False,
        notes="uint16 payload length",
    ),
    "warning_char_pack": CharDef(
        key="warning_char_pack",
        name="Warning Message Char Pack",
        uuid="bda70004-24ff-4f28-af24-8293a69561ca",
        readable=True,
        writable=True,
        notifiable=False,
        notes="sqn:uint16 + content:uint8",
    ),
    "warning_ack_request": CharDef(
        key="warning_ack_request",
        name="Warning Message ACK Request",
        uuid="bda70005-24ff-4f28-af24-8293a69561ca",
        readable=True,
        writable=False,
        notifiable=True,
        notes="uint16 next requested sequence number",
    ),
    "warning_acknowledged": CharDef(
        key="warning_acknowledged",
        name="Warning Acknowledged",
        uuid="bda70002-24ff-4f28-af24-8293a69561ca",
        readable=True,
        writable=True,
        notifiable=True,
        notes="bool",
    ),
}

STATUS_LABELS = {
    0: "valid",
    1: "short invalid low",
    2: "short invalid high",
    3: "long invalid low",
    4: "long invalid high",
    5: "long valid",
}
STATUS_NAME_TO_VALUE = {v: k for k, v in STATUS_LABELS.items()}
STATUS_OPTIONS = list(STATUS_NAME_TO_VALUE.keys())


# -----------------------------
# Packing / parsing helpers
# -----------------------------

def pack_auth_packet(host_id: int, room_name: str) -> bytes:
    room_bytes = room_name.encode("utf-8")
    if len(room_bytes) > 32:
        raise ValueError("Room name must be at most 32 UTF-8 bytes")
    padded = room_bytes.ljust(32, b"\x00")
    return struct.pack("<I32sB", host_id, padded, len(room_bytes))


def unpack_auth_packet(data: bytes) -> str:
    if len(data) != struct.calcsize("<I32sB"):
        return f"hex={data.hex()}"
    host_id, raw_name, raw_len = struct.unpack("<I32sB", data)
    room = raw_name[:raw_len].decode("utf-8", errors="replace")
    return f"id={host_id}, room={room!r}, len={raw_len}"


# Setup struct matches DeviceSetupConfig in ble.h: uint8_t measurementInterval; uint32_t deviceId.
def pack_setup_packet(host_id: int, interval_seconds: int) -> bytes:
    if not (3 <= interval_seconds <= 60):
        raise ValueError("Measurement interval must be 3..60 seconds")
    if not (1 <= host_id <= 0xFFFFFFFF):
        raise ValueError("ID must be a positive uint32")
    return struct.pack("<BI", interval_seconds, host_id)


def unpack_setup_packet(data: bytes) -> str:
    if len(data) == struct.calcsize("<BI"):
        interval, host_id = struct.unpack("<BI", data)
        return f"id={host_id}, measurementInterval={interval}"
    return f"hex={data.hex()}"


def build_status_code(pressure: int, temperature: int, humidity: int, gas: int) -> int:
    for value in (pressure, temperature, humidity, gas):
        if value not in STATUS_LABELS:
            raise ValueError("Status nibble must be in range 0..5")
    return (
        (pressure & 0xF)
        | ((temperature & 0xF) << 4)
        | ((humidity & 0xF) << 8)
        | ((gas & 0xF) << 12)
    )



def split_status_code(status_code: int) -> Dict[str, int]:
    return {
        "pressure": status_code & 0x000F,
        "temperature": (status_code >> 4) & 0x000F,
        "humidity": (status_code >> 8) & 0x000F,
        "gas": (status_code >> 12) & 0x000F,
    }



def pack_sensor_status_packet(timestamp: int, status_code: int) -> bytes:
    return struct.pack("<IH", timestamp, status_code)



def unpack_sensor_status_packet(data: bytes) -> str:
    if len(data) != struct.calcsize("<IH"):
        return f"hex={data.hex()}"
    timestamp, status_code = struct.unpack("<IH", data)
    parts = split_status_code(status_code)
    pretty = ", ".join(
        f"{key}={STATUS_LABELS.get(value, value)}" for key, value in parts.items()
    )
    return f"timestamp={timestamp}, statusCode=0x{status_code:04X} ({pretty})"



def pack_warning_total_length(length: int) -> bytes:
    return struct.pack("<H", length)



def unpack_warning_total_length(data: bytes) -> str:
    if len(data) != struct.calcsize("<H"):
        return f"hex={data.hex()}"
    (length,) = struct.unpack("<H", data)
    return f"length={length}"



def pack_warning_char_packet(sqn: int, content_byte: int) -> bytes:
    return struct.pack("<HB", sqn, content_byte)



def unpack_warning_char_packet(data: bytes) -> str:
    if len(data) != struct.calcsize("<HB"):
        return f"hex={data.hex()}"
    sqn, value = struct.unpack("<HB", data)
    if value == 0:
        desc = r"\0"
    elif 32 <= value <= 126:
        desc = repr(chr(value))
    else:
        desc = f"0x{value:02X}"
    return f"sqn={sqn}, content={desc}"



def pack_warning_acknowledged(value: bool) -> bytes:
    return struct.pack("<?", value)



def unpack_warning_acknowledged(data: bytes) -> str:
    if len(data) != struct.calcsize("<?"):
        return f"hex={data.hex()}"
    (ack,) = struct.unpack("<?", data)
    return f"acknowledged={ack}"



def unpack_ack_request(data: bytes) -> str:
    if len(data) != struct.calcsize("<H"):
        return f"hex={data.hex()}"
    (sqn,) = struct.unpack("<H", data)
    return f"requested_sqn={sqn}"


def pack_cached_sensor_data_ack(value: bool = True) -> bytes:
    return struct.pack("<?", value)


def unpack_cached_sensor_data_ack(data: bytes) -> str:
    if len(data) != struct.calcsize("<?"):
        return f"hex={data.hex()}"
    (ack,) = struct.unpack("<?", data)
    return f"ack={ack}"


def decode_sensor_packet_fields(data: bytes) -> Optional[dict[str, float]]:
    if len(data) != struct.calcsize("<Iffff"):
        return None
    ts, pressure, temperature, humidity, iaq = struct.unpack("<Iffff", data)
    return {
        "timestamp_ms": ts,
        "pressure_pa": pressure,
        "pressure_hpa": pressure / 100.0,
        "temperature_c": temperature,
        "humidity_pct": humidity,
        "air_quality_iaq": iaq,
    }



def unpack_sensor_packet(data: bytes) -> str:
    decoded = decode_sensor_packet_fields(data)
    if decoded is not None:
        if int(decoded["timestamp_ms"]) == 0:
            return "cached transfer complete"
        return (
            "timestamp={timestamp_ms:.0f} ms, pressure={pressure_hpa:.2f} hPa, "
            "temperature={temperature_c:.2f} C, humidity={humidity_pct:.2f} %, "
            "IAQ={air_quality_iaq:.2f}"
        ).format(**decoded)

    return f"hex={data.hex()}"




DEFAULT_RAW_HEX: Dict[str, str] = {
    "sensor_packet": "0100000000000000000000000000000000000000",
    "sensor_status": pack_sensor_status_packet(1, build_status_code(0, 0, 0, 0)).hex(),
    "cached_sensor_data": "0100000000000000000000000000000000000000",
    "cached_sensor_data_ack": pack_cached_sensor_data_ack(True).hex(),
    "setup": pack_setup_packet(1, 3).hex(),
    "auth": pack_auth_packet(1, "TestRoom01").hex(),
    "warning_total_len": pack_warning_total_length(0).hex(),
    "warning_char_pack": pack_warning_char_packet(0, ord("A")).hex(),
    "warning_ack_request": struct.pack("<H", 1).hex(),
    "warning_acknowledged": pack_warning_acknowledged(False).hex(),
}

PARSERS: Dict[str, Callable[[bytes], str]] = {
    "sensor_packet": unpack_sensor_packet,
    "sensor_status": unpack_sensor_status_packet,
    "cached_sensor_data": unpack_sensor_packet,
    "cached_sensor_data_ack": unpack_cached_sensor_data_ack,
    "setup": unpack_setup_packet,
    "auth": unpack_auth_packet,
    "warning_total_len": unpack_warning_total_length,
    "warning_char_pack": unpack_warning_char_packet,
    "warning_ack_request": unpack_ack_request,
    "warning_acknowledged": unpack_warning_acknowledged,
}


class BLEBackend:
    def __init__(self, ui_queue: queue.Queue[tuple[str, Any]]):
        self.ui_queue = ui_queue
        self.loop = asyncio.new_event_loop()
        self.thread = threading.Thread(target=self._run_loop, daemon=True)
        self.thread.start()
        self.client: Optional[BleakClient] = None
        self.known_devices: list[Any] = []
        self.notification_states: Dict[str, bool] = {}
        self.cached_history_records: List[dict[str, float]] = []
        self.history_fetch_in_progress = False

    def _run_loop(self) -> None:
        asyncio.set_event_loop(self.loop)
        self.loop.run_forever()

    def submit(self, coro: asyncio.Future) -> Any:
        return asyncio.run_coroutine_threadsafe(coro, self.loop)

    def emit(self, event: str, payload: Any) -> None:
        self.ui_queue.put((event, payload))

    def on_notification(self, key: str, sender: str, data: bytearray) -> None:
        parser = PARSERS.get(key, lambda raw: raw.hex())
        self.emit(
            "notification",
            {
                "key": key,
                "sender": sender,
                "hex": bytes(data).hex(),
                "pretty": parser(bytes(data)),
            },
        )

    async def scan(self, timeout: float = 6.0) -> None:
        self.emit("busy", True)
        try:
            devices = await BleakScanner.discover(timeout=timeout)
            self.known_devices = devices
            self.emit(
                "scan_results",
                [
                    {
                        "name": d.name or "<unnamed>",
                        "address": d.address,
                        "rssi": getattr(d, "rssi", None),
                    }
                    for d in devices
                ],
            )
        except Exception as exc:
            self.emit("error", f"Scan failed: {exc}")
        finally:
            self.emit("busy", False)

    async def connect(self, address: str) -> None:
        self.emit("busy", True)
        try:
            if self.client and self.client.is_connected:
                await self.client.disconnect()
            self.client = BleakClient(address)
            await self.client.connect()
            self.emit("connected", address)
        except Exception as exc:
            self.emit("error", f"Connect failed: {exc}")
        finally:
            self.emit("busy", False)

    async def initial_setup(self, address: str, host_id: int, interval_seconds: int) -> None:
        self.emit("busy", True)
        payload = pack_setup_packet(host_id, interval_seconds)
        setup_client: Optional[BleakClient] = None
        try:
            if self.client and self.client.is_connected:
                await self.client.disconnect()
                self.emit("disconnected", None)

            self.emit("log", f"Setup: connecting to {address}")
            setup_client = BleakClient(address, timeout=20.0)
            await setup_client.connect()
            self.emit("connected", address)

            self.emit(
                "log",
                f"Setup: writing ID={host_id}, measurementInterval={interval_seconds}s",
            )
            await setup_client.write_gatt_char(CHARS["setup"].uuid, payload, response=True)
            self.emit(
                "write_result",
                {
                    "key": "setup",
                    "hex": payload.hex(),
                    "pretty": unpack_setup_packet(payload),
                },
            )
            self.emit("log", "Setup: write accepted; Arduino should reboot after about 2 seconds")

            await asyncio.sleep(3.0)
            if setup_client.is_connected:
                await setup_client.disconnect()
            self.client = None
            self.emit("disconnected", None)
            self.emit("log", "Setup: finished. Scan for G5T4CC after reboot.")
        except Exception as exc:
            self.emit(
                "error",
                (
                    "Initial setup failed. If the Arduino serial log shows the config was "
                    f"written, the reboot may have interrupted BLE clean-up. Details: {exc}"
                ),
            )
        finally:
            try:
                if setup_client and setup_client.is_connected:
                    await setup_client.disconnect()
            except Exception:
                pass
            self.emit("busy", False)

    async def disconnect(self) -> None:
        self.emit("busy", True)
        try:
            if self.client and self.client.is_connected:
                await self.client.disconnect()
            self.emit("disconnected", None)
        except Exception as exc:
            self.emit("error", f"Disconnect failed: {exc}")
        finally:
            self.emit("busy", False)

    async def read_char(self, key: str) -> None:
        if not self.client or not self.client.is_connected:
            self.emit("error", "Not connected")
            return
        char = CHARS[key]
        try:
            data = await self.client.read_gatt_char(char.uuid)
            parser = PARSERS.get(key, lambda raw: raw.hex())
            self.emit(
                "read_result",
                {
                    "key": key,
                    "hex": bytes(data).hex(),
                    "pretty": parser(bytes(data)),
                },
            )
        except Exception as exc:
            self.emit("error", f"Read failed for {char.name}: {exc}")

    async def write_char(self, key: str, payload: bytes) -> None:
        if not self.client or not self.client.is_connected:
            self.emit("error", "Not connected")
            return
        char = CHARS[key]
        try:
            await self.client.write_gatt_char(char.uuid, payload, response=True)
            parser = PARSERS.get(key, lambda raw: raw.hex())
            self.emit(
                "write_result",
                {
                    "key": key,
                    "hex": payload.hex(),
                    "pretty": parser(payload),
                },
            )
        except Exception as exc:
            self.emit("error", f"Write failed for {char.name}: {exc}")

    async def set_notify(self, key: str, enabled: bool) -> None:
        if not self.client or not self.client.is_connected:
            self.emit("error", "Not connected")
            return
        char = CHARS[key]
        try:
            if enabled:
                await self.client.start_notify(
                    char.uuid,
                    lambda sender, data, _key=key: self.on_notification(_key, sender, data),
                )
            else:
                await self.client.stop_notify(char.uuid)
            self.notification_states[key] = enabled
            self.emit("notify_state", {"key": key, "enabled": enabled})
        except Exception as exc:
            self.emit("error", f"Notification toggle failed for {char.name}: {exc}")

    async def fetch_cached_history(self, settle_delay: float = 0.08, max_packets: int = 10000) -> None:
        if not self.client or not self.client.is_connected:
            self.emit("error", "Not connected")
            return

        self.emit("busy", True)
        self.cached_history_records = []
        self.history_fetch_in_progress = True
        self.emit("history_reset", None)

        try:
            self.emit("log", "History fetch started")

            while len(self.cached_history_records) < max_packets:
                await self.client.write_gatt_char(
                    CHARS["cached_sensor_data_ack"].uuid,
                    pack_cached_sensor_data_ack(True),
                    response=True,
                )
                await asyncio.sleep(settle_delay)

                raw = bytes(await self.client.read_gatt_char(CHARS["cached_sensor_data"].uuid))
                decoded = decode_sensor_packet_fields(raw)
                if decoded is None:
                    self.emit("log", f"History fetch: ignored undecodable packet hex={raw.hex()}")
                    break

                ts = int(decoded["timestamp_ms"])
                if ts == 0:
                    self.emit("log", "History fetch: device sent completion packet")
                    break

                self.cached_history_records.append(decoded)
                self.emit(
                    "history_row",
                    {
                        "timestamp_ms": ts,
                        "pressure_hpa": decoded["pressure_hpa"],
                        "temperature_c": decoded["temperature_c"],
                        "humidity_pct": decoded["humidity_pct"],
                        "air_quality_iaq": decoded["air_quality_iaq"],
                        "raw_hex": raw.hex(),
                    },
                )

            self.emit("log", f"History fetch completed with {len(self.cached_history_records)} packet(s)")
        except Exception as exc:
            self.emit("error", f"History fetch failed: {exc}")
        finally:
            self.history_fetch_in_progress = False
            self.emit("history_done", len(self.cached_history_records))
            self.emit("busy", False)

    async def login(self, host_id: int, room_name: str) -> None:
        payload = pack_auth_packet(host_id, room_name)
        await self.write_char("auth", payload)

    async def send_warning_flow(
        self,
        messages: list[str],
        timestamp: int,
        pressure: int,
        temperature: int,
        humidity: int,
        gas: int,
        timeout: float = 15.0,
    ) -> None:
        if not self.client or not self.client.is_connected:
            self.emit("error", "Not connected")
            return

        payload = b"".join(msg.encode("utf-8") + b"\x00" for msg in messages)
        if not payload:
            self.emit("error", "Warning flow requires at least one message")
            return

        requested_queue: asyncio.Queue[int] = asyncio.Queue()
        done_event = asyncio.Event()

        def ack_handler(sender: str, data: bytearray) -> None:
            self.on_notification("warning_ack_request", sender, data)
            if len(data) == 2:
                sqn = struct.unpack("<H", bytes(data))[0]
                self.loop.call_soon_threadsafe(requested_queue.put_nowait, sqn)

        try:
            await self.client.start_notify(CHARS["warning_ack_request"].uuid, ack_handler)
            self.emit("notify_state", {"key": "warning_ack_request", "enabled": True})

            await self.client.write_gatt_char(
                CHARS["warning_total_len"].uuid,
                pack_warning_total_length(len(payload)),
                response=True,
            )
            self.emit("log", f"Warning flow: wrote total length {len(payload)}")

            first_packet = pack_warning_char_packet(0, payload[0])
            await self.client.write_gatt_char(
                CHARS["warning_char_pack"].uuid,
                first_packet,
                response=True,
            )
            self.emit("log", "Warning flow: sent first character packet (SQN 0)")

            highest_sent = 0
            while highest_sent < len(payload) - 1:
                try:
                    requested_sqn = await asyncio.wait_for(requested_queue.get(), timeout=timeout)
                except asyncio.TimeoutError:
                    raise TimeoutError("Timed out waiting for next requested SQN")

                if requested_sqn >= len(payload):
                    self.emit("log", f"Warning flow: device requested SQN {requested_sqn}, beyond payload")
                    break

                packet = pack_warning_char_packet(requested_sqn, payload[requested_sqn])
                await self.client.write_gatt_char(
                    CHARS["warning_char_pack"].uuid,
                    packet,
                    response=True,
                )
                highest_sent = max(highest_sent, requested_sqn)
                self.emit("log", f"Warning flow: sent SQN {requested_sqn}")

            status_code = build_status_code(pressure, temperature, humidity, gas)
            status_payload = pack_sensor_status_packet(timestamp, status_code)
            await self.client.write_gatt_char(
                CHARS["sensor_status"].uuid,
                status_payload,
                response=True,
            )
            self.emit(
                "log",
                (
                    "Warning flow: triggered measurement response with "
                    f"statusCode=0x{status_code:04X}"
                ),
            )
            done_event.set()
            self.emit("log", "Warning flow completed")
        except Exception as exc:
            self.emit("error", f"Warning flow failed: {exc}")
        finally:
            try:
                await self.client.stop_notify(CHARS["warning_ack_request"].uuid)
                self.emit("notify_state", {"key": "warning_ack_request", "enabled": False})
            except Exception:
                pass


class CharacteristicCard(ttk.LabelFrame):
    def __init__(self, master: tk.Widget, app: "BLEApp", char_def: CharDef):
        super().__init__(master, text=char_def.name, padding=8)
        self.app = app
        self.char_def = char_def
        self.raw_var = tk.StringVar(value=DEFAULT_RAW_HEX.get(char_def.key, ""))
        self.pretty_var = tk.StringVar(value="")
        self._build()

    def _build(self) -> None:
        ttk.Label(self, text=self.char_def.notes).grid(row=0, column=0, columnspan=6, sticky="w")
        row = 1

        self.form_widgets: Dict[str, Any] = {}

        if self.char_def.key == "sensor_packet":
            ttk.Label(self, text="Read-only / notify").grid(row=row, column=0, sticky="w")
            row += 1

        elif self.char_def.key == "sensor_status":
            self.form_widgets["timestamp"] = self._entry(row, "Timestamp", "1")
            row += 1
            for field in ("pressure", "temperature", "humidity", "gas"):
                var = tk.StringVar(value=STATUS_LABELS[0])
                self.form_widgets[field] = var
                ttk.Label(self, text=field.capitalize()).grid(row=row, column=0, sticky="w", padx=(0, 6), pady=2)
                cb = ttk.Combobox(self, textvariable=var, values=STATUS_OPTIONS, state="readonly", width=24)
                cb.grid(row=row, column=1, sticky="ew", pady=2)
                row += 1

        elif self.char_def.key == "cached_sensor_data":
            ttk.Label(self, text="Read / notify history packets").grid(row=row, column=0, sticky="w")
            row += 1

        elif self.char_def.key == "cached_sensor_data_ack":
            var = tk.BooleanVar(value=True)
            self.form_widgets["value"] = var
            chk = ttk.Checkbutton(self, text="ACK / request next packet", variable=var)
            chk.grid(row=row, column=0, sticky="w")
            row += 1

        elif self.char_def.key == "setup":
            self.form_widgets["id"] = self._entry(row, "ID", "1")
            row += 1
            self.form_widgets["interval"] = self._entry(row, "Measurement interval (s)", "3")
            row += 1

        elif self.char_def.key == "auth":
            self.form_widgets["id"] = self._entry(row, "ID", "1")
            row += 1
            self.form_widgets["room"] = self._entry(row, "Room name", "TestRoom01")
            row += 1

        elif self.char_def.key == "warning_total_len":
            self.form_widgets["length"] = self._entry(row, "Length", "0")
            row += 1

        elif self.char_def.key == "warning_char_pack":
            self.form_widgets["sqn"] = self._entry(row, "Sequence number", "0")
            row += 1
            self.form_widgets["content"] = self._entry(row, "Content (single char or integer 0..255)", "A")
            row += 1

        elif self.char_def.key == "warning_ack_request":
            ttk.Label(self, text="Read-only / notify").grid(row=row, column=0, sticky="w")
            row += 1

        elif self.char_def.key == "warning_acknowledged":
            var = tk.BooleanVar(value=False)
            self.form_widgets["value"] = var
            chk = ttk.Checkbutton(self, text="Acknowledged", variable=var)
            chk.grid(row=row, column=0, sticky="w")
            row += 1

        ttk.Separator(self).grid(row=row, column=0, columnspan=6, sticky="ew", pady=6)
        row += 1

        ttk.Label(self, text="Raw hex write").grid(row=row, column=0, sticky="w")
        ttk.Entry(self, textvariable=self.raw_var, width=40).grid(row=row, column=1, columnspan=3, sticky="ew", padx=(6, 6))
        ttk.Button(self, text="Write Raw", command=self.write_raw).grid(row=row, column=4, sticky="ew")
        ttk.Label(
            self,
            text="Manual raw writes are available for every listed characteristic. The device may still reject writes to read-only characteristics.",
            wraplength=650,
            justify="left",
        ).grid(row=row + 1, column=0, columnspan=5, sticky="w", pady=(4, 0))
        row += 2

        button_col = 0
        if self.char_def.writable:
            ttk.Button(self, text="Write Structured", command=self.write_structured).grid(row=row, column=button_col, sticky="ew", pady=(4, 0))
            button_col += 1
        if self.char_def.readable:
            ttk.Button(self, text="Read", command=self.read_value).grid(row=row, column=button_col, sticky="ew", pady=(4, 0), padx=(4, 0))
            button_col += 1
        if self.char_def.notifiable:
            ttk.Button(self, text="Start Notify", command=lambda: self.toggle_notify(True)).grid(row=row, column=button_col, sticky="ew", pady=(4, 0), padx=(4, 0))
            button_col += 1
            ttk.Button(self, text="Stop Notify", command=lambda: self.toggle_notify(False)).grid(row=row, column=button_col, sticky="ew", pady=(4, 0), padx=(4, 0))
            button_col += 1
        row += 1

        ttk.Label(self, text="Last decoded value").grid(row=row, column=0, sticky="w", pady=(6, 0))
        ttk.Label(self, textvariable=self.pretty_var, wraplength=650, justify="left").grid(row=row, column=1, columnspan=5, sticky="w", pady=(6, 0))

        self.columnconfigure(1, weight=1)
        self.columnconfigure(2, weight=1)
        self.columnconfigure(3, weight=1)

    def _entry(self, row: int, label: str, default: str) -> ttk.Entry:
        ttk.Label(self, text=label).grid(row=row, column=0, sticky="w", padx=(0, 6), pady=2)
        entry = ttk.Entry(self)
        entry.insert(0, default)
        entry.grid(row=row, column=1, columnspan=2, sticky="ew", pady=2)
        return entry

    def toggle_notify(self, enabled: bool) -> None:
        self.app.backend.submit(self.app.backend.set_notify(self.char_def.key, enabled))

    def read_value(self) -> None:
        self.app.backend.submit(self.app.backend.read_char(self.char_def.key))

    def write_raw(self) -> None:
        raw_text = self.raw_var.get().strip().replace(" ", "")
        if not raw_text:
            messagebox.showerror("Raw write", "Enter hex bytes first")
            return
        try:
            payload = bytes.fromhex(raw_text)
        except ValueError as exc:
            messagebox.showerror("Raw write", f"Invalid hex: {exc}")
            return
        self.app.backend.submit(self.app.backend.write_char(self.char_def.key, payload))

    def write_structured(self) -> None:
        try:
            payload = self._build_payload_from_form()
        except Exception as exc:
            messagebox.showerror(self.char_def.name, str(exc))
            return
        if payload is None:
            return
        self.raw_var.set(payload.hex())
        self.app.backend.submit(self.app.backend.write_char(self.char_def.key, payload))

    def _build_payload_from_form(self) -> Optional[bytes]:
        key = self.char_def.key
        if key == "sensor_status":
            timestamp = int(self.form_widgets["timestamp"].get())
            pressure = STATUS_NAME_TO_VALUE[self.form_widgets["pressure"].get()]
            temperature = STATUS_NAME_TO_VALUE[self.form_widgets["temperature"].get()]
            humidity = STATUS_NAME_TO_VALUE[self.form_widgets["humidity"].get()]
            gas = STATUS_NAME_TO_VALUE[self.form_widgets["gas"].get()]
            status_code = build_status_code(pressure, temperature, humidity, gas)
            return pack_sensor_status_packet(timestamp, status_code)
        if key == "cached_sensor_data_ack":
            return pack_cached_sensor_data_ack(bool(self.form_widgets["value"].get()))
        if key == "setup":
            host_id = int(self.form_widgets["id"].get())
            interval = int(self.form_widgets["interval"].get())
            return pack_setup_packet(host_id, interval)
        if key == "auth":
            host_id = int(self.form_widgets["id"].get())
            room = self.form_widgets["room"].get()
            return pack_auth_packet(host_id, room)
        if key == "warning_total_len":
            return pack_warning_total_length(int(self.form_widgets["length"].get()))
        if key == "warning_char_pack":
            sqn = int(self.form_widgets["sqn"].get())
            raw_content = self.form_widgets["content"].get()
            if len(raw_content) == 1 and not raw_content.isdigit():
                content_value = ord(raw_content)
            else:
                content_value = int(raw_content)
            if not (0 <= content_value <= 255):
                raise ValueError("Content byte must be in range 0..255")
            return pack_warning_char_packet(sqn, content_value)
        if key == "warning_acknowledged":
            return pack_warning_acknowledged(bool(self.form_widgets["value"].get()))
        messagebox.showinfo(self.char_def.name, "No structured writer for this characteristic")
        return None

    def update_value(self, pretty: str, raw_hex: str) -> None:
        self.pretty_var.set(pretty)
        self.raw_var.set(raw_hex)


class BLEApp:
    def __init__(self, root: tk.Tk):
        self.root = root
        self.root.title("Climate Canary BLE Helper")
        self.root.geometry("1280x900")
        self.root.minsize(1080, 760)
        self._configure_style()

        self.ui_queue: queue.Queue[tuple[str, Any]] = queue.Queue()
        self.backend = BLEBackend(self.ui_queue)

        self.devices_by_display: Dict[str, str] = {}
        self.cards: Dict[str, CharacteristicCard] = {}
        self.busy_var = tk.StringVar(value="Idle")
        self.connection_var = tk.StringVar(value="Disconnected")
        self.selected_device_var = tk.StringVar()
        self.device_name_filter_var = tk.StringVar(value=DEVICE_NAME_DEFAULT)
        self.login_id_var = tk.StringVar(value="1")
        self.login_room_var = tk.StringVar(value="TestRoom01")
        self.setup_id_var = tk.StringVar(value="1")
        self.setup_interval_var = tk.StringVar(value="3")
        self.warning_messages_var = tk.StringVar(value="Hello,Bye")
        self.warning_timestamp_var = tk.StringVar(value="1")
        self.warning_status_vars = {
            field: tk.StringVar(value=STATUS_LABELS[0])
            for field in ("pressure", "temperature", "humidity", "gas")
        }
        self.warning_status_vars["temperature"].set(STATUS_LABELS[3])
        self.history_rows: list[str] = []

        self._build_ui()
        self._poll_ui_queue()

    def _configure_style(self) -> None:
        style = ttk.Style(self.root)
        try:
            style.theme_use("clam")
        except tk.TclError:
            pass
        style.configure(".", font=("TkDefaultFont", 10))
        style.configure("Title.TLabel", font=("TkDefaultFont", 16, "bold"))
        style.configure("Status.TLabel", padding=(8, 4))
        style.configure("Accent.TButton", padding=(10, 4))
        style.configure("Danger.TButton", padding=(10, 4))
        style.configure("Treeview", rowheight=24)

    def _build_ui(self) -> None:
        main = ttk.Frame(self.root, padding=10)
        main.pack(fill="both", expand=True)

        header = ttk.Frame(main)
        header.pack(fill="x", pady=(0, 8))
        ttk.Label(header, text="Climate Canary BLE Helper", style="Title.TLabel").pack(side="left")
        ttk.Label(header, textvariable=self.connection_var, style="Status.TLabel").pack(side="right")
        ttk.Label(header, textvariable=self.busy_var, style="Status.TLabel").pack(side="right", padx=(0, 8))

        top = ttk.LabelFrame(main, text="Connection", padding=8)
        top.pack(fill="x")

        ttk.Label(top, text="Name filter").grid(row=0, column=0, sticky="w")
        ttk.Entry(top, textvariable=self.device_name_filter_var, width=18).grid(row=0, column=1, sticky="w", padx=(6, 8))
        ttk.Button(top, text="Scan", command=self.scan).grid(row=0, column=2, padx=(0, 4))
        ttk.Button(top, text="Setup Devices", command=self.scan_setup_devices).grid(row=0, column=3, padx=(0, 8))

        self.device_combo = ttk.Combobox(top, textvariable=self.selected_device_var, width=60, state="readonly")
        self.device_combo.grid(row=0, column=4, sticky="ew", padx=(0, 8))
        ttk.Button(top, text="Connect", command=self.connect).grid(row=0, column=5, padx=(0, 8))
        ttk.Button(top, text="Disconnect", command=self.disconnect).grid(row=0, column=6)

        ttk.Label(top, text="Use G5T4SETUP for first-time pairing, then scan for G5T4CC after reboot.").grid(
            row=1,
            column=0,
            columnspan=7,
            sticky="w",
            pady=(8, 0),
        )
        top.columnconfigure(4, weight=1)

        flow_frame = ttk.Frame(main)
        flow_frame.pack(fill="x", pady=(10, 0))

        setup = ttk.LabelFrame(flow_frame, text="Initial Setup", padding=8)
        setup.pack(side="left", fill="both", expand=True, padx=(0, 5))
        ttk.Label(setup, text="ID").grid(row=0, column=0, sticky="w")
        ttk.Entry(setup, textvariable=self.setup_id_var, width=12).grid(row=0, column=1, sticky="w", padx=(6, 8))
        ttk.Label(setup, text="Interval (3-60s)").grid(row=0, column=2, sticky="w")
        ttk.Entry(setup, textvariable=self.setup_interval_var, width=12).grid(row=0, column=3, sticky="w", padx=(6, 8))
        ttk.Button(setup, text="Write Setup + Reboot", command=self.initial_setup).grid(row=0, column=4, sticky="w")
        ttk.Label(setup, text="Requires a selected G5T4SETUP device.").grid(row=1, column=0, columnspan=5, sticky="w", pady=(8, 0))

        login = ttk.LabelFrame(flow_frame, text="Normal Connection", padding=8)
        login.pack(side="left", fill="both", expand=True, padx=(5, 5))
        ttk.Label(login, text="Host ID").grid(row=0, column=0, sticky="w")
        ttk.Entry(login, textvariable=self.login_id_var, width=12).grid(row=0, column=1, sticky="w", padx=(6, 8))
        ttk.Label(login, text="Room name").grid(row=0, column=2, sticky="w")
        ttk.Entry(login, textvariable=self.login_room_var, width=24).grid(row=0, column=3, sticky="w", padx=(6, 8))
        ttk.Button(login, text="Login", command=self.login).grid(row=0, column=4, sticky="w")
        ttk.Button(login, text="Fetch Buffered History", command=self.fetch_buffered_history).grid(row=0, column=5, sticky="w", padx=(8, 0))

        warning = ttk.LabelFrame(flow_frame, text="Warning Flow", padding=8)
        warning.pack(side="left", fill="both", expand=True, padx=(5, 0))
        ttk.Label(warning, text="Warnings (comma-separated)").grid(row=0, column=0, sticky="w")
        ttk.Entry(warning, textvariable=self.warning_messages_var, width=36).grid(row=0, column=1, columnspan=4, sticky="ew", padx=(6, 8))
        ttk.Label(warning, text="Timestamp").grid(row=1, column=0, sticky="w", pady=(6, 0))
        ttk.Entry(warning, textvariable=self.warning_timestamp_var, width=12).grid(row=1, column=1, sticky="w", padx=(6, 8), pady=(6, 0))

        col = 2
        for field in ("pressure", "temperature", "humidity", "gas"):
            ttk.Label(warning, text=field.capitalize()).grid(row=1, column=col, sticky="w", pady=(6, 0))
            cb = ttk.Combobox(
                warning,
                textvariable=self.warning_status_vars[field],
                values=STATUS_OPTIONS,
                width=18,
                state="readonly",
            )
            cb.grid(row=1, column=col + 1, sticky="w", padx=(6, 8), pady=(6, 0))
            col += 2
        ttk.Button(warning, text="Start Warning Flow", command=self.start_warning_flow).grid(row=2, column=0, columnspan=2, sticky="w", pady=(8, 0))
        warning.columnconfigure(1, weight=1)

        content = ttk.Panedwindow(main, orient="vertical")
        content.pack(fill="both", expand=True, pady=(10, 0))

        cards_frame = ttk.Frame(content)
        content.add(cards_frame, weight=3)

        canvas = tk.Canvas(cards_frame, highlightthickness=0)
        scrollbar = ttk.Scrollbar(cards_frame, orient="vertical", command=canvas.yview)
        self.scroll_inner = ttk.Frame(canvas)
        self.scroll_inner.bind(
            "<Configure>",
            lambda e: canvas.configure(scrollregion=canvas.bbox("all")),
        )
        canvas.create_window((0, 0), window=self.scroll_inner, anchor="nw")
        canvas.configure(yscrollcommand=scrollbar.set)
        canvas.pack(side="left", fill="both", expand=True)
        scrollbar.pack(side="right", fill="y")

        for index, key in enumerate(CHARS):
            card = CharacteristicCard(self.scroll_inner, self, CHARS[key])
            card.grid(row=index, column=0, sticky="ew", pady=6)
            self.cards[key] = card
        self.scroll_inner.columnconfigure(0, weight=1)

        history_frame = ttk.LabelFrame(content, text="Buffered History", padding=8)
        content.add(history_frame, weight=2)

        history_toolbar = ttk.Frame(history_frame)
        history_toolbar.pack(fill="x", pady=(0, 6))
        ttk.Button(history_toolbar, text="Clear Table", command=self.clear_history_table).pack(side="left")
        ttk.Label(history_toolbar, text="Fetch sends ACK/request, reads packet, stops at timestamp 0.").pack(side="left", padx=(10, 0))

        columns = ("timestamp_ms", "pressure_hpa", "temperature_c", "humidity_pct", "air_quality_iaq")
        self.history_tree = ttk.Treeview(history_frame, columns=columns, show="headings", height=10)
        headings = {
            "timestamp_ms": "Timestamp (ms)",
            "pressure_hpa": "Pressure (hPa)",
            "temperature_c": "Temperature (C)",
            "humidity_pct": "Humidity (%)",
            "air_quality_iaq": "IAQ",
        }
        widths = {
            "timestamp_ms": 140,
            "pressure_hpa": 140,
            "temperature_c": 140,
            "humidity_pct": 140,
            "air_quality_iaq": 140,
        }
        for col in columns:
            self.history_tree.heading(col, text=headings[col])
            self.history_tree.column(col, width=widths[col], anchor="center")
        history_scroll = ttk.Scrollbar(history_frame, orient="vertical", command=self.history_tree.yview)
        self.history_tree.configure(yscrollcommand=history_scroll.set)
        self.history_tree.pack(side="left", fill="both", expand=True)
        history_scroll.pack(side="right", fill="y")

        log_frame = ttk.LabelFrame(content, text="Log", padding=8)
        content.add(log_frame, weight=1)
        self.log_text = tk.Text(log_frame, height=12, wrap="word")
        self.log_text.pack(fill="both", expand=True)

    def log(self, message: str) -> None:
        timestamp = time.strftime("%H:%M:%S")
        self.log_text.insert("end", f"[{timestamp}] {message}\n")
        self.log_text.see("end")

    def scan(self) -> None:
        self.backend.submit(self.backend.scan())

    def scan_setup_devices(self) -> None:
        self.device_name_filter_var.set(SETUP_DEVICE_NAME_DEFAULT)
        self.backend.submit(self.backend.scan())

    def connect(self) -> None:
        selection = self.selected_device_var.get().strip()
        if not selection:
            messagebox.showerror("Connect", "Select a scanned device first")
            return
        address = self.devices_by_display.get(selection)
        if not address:
            messagebox.showerror("Connect", "Selected device is no longer available")
            return
        self.backend.submit(self.backend.connect(address))

    def disconnect(self) -> None:
        self.backend.submit(self.backend.disconnect())

    def _selected_address(self, title: str) -> Optional[str]:
        selection = self.selected_device_var.get().strip()
        if not selection:
            messagebox.showerror(title, "Select a scanned device first")
            return None
        address = self.devices_by_display.get(selection)
        if not address:
            messagebox.showerror(title, "Selected device is no longer available")
            return None
        return address

    def initial_setup(self) -> None:
        address = self._selected_address("Initial setup")
        if not address:
            return
        try:
            host_id = int(self.setup_id_var.get())
            interval = int(self.setup_interval_var.get())
            payload = pack_setup_packet(host_id, interval)
        except Exception as exc:
            messagebox.showerror("Initial setup", str(exc))
            return

        if not messagebox.askyesno(
            "Initial setup",
            (
                "Write setup config to the selected device?\n\n"
                f"ID: {host_id}\n"
                f"Measurement interval: {interval}s\n\n"
                "The Arduino will reboot after accepting it."
            ),
        ):
            return

        self.cards["setup"].update_value(unpack_setup_packet(payload), payload.hex())
        self.backend.submit(self.backend.initial_setup(address, host_id, interval))

    def login(self) -> None:
        try:
            host_id = int(self.login_id_var.get())
        except ValueError:
            messagebox.showerror("Login", "Host ID must be an integer")
            return
        room = self.login_room_var.get().strip()
        self.backend.submit(self.backend.login(host_id, room))

    def start_warning_flow(self) -> None:
        try:
            timestamp = int(self.warning_timestamp_var.get())
        except ValueError:
            messagebox.showerror("Warning flow", "Timestamp must be an integer")
            return
        messages = [part.strip() for part in self.warning_messages_var.get().split(",") if part.strip()]
        if not messages:
            messagebox.showerror("Warning flow", "Enter at least one warning message")
            return
        try:
            pressure = STATUS_NAME_TO_VALUE[self.warning_status_vars["pressure"].get()]
            temperature = STATUS_NAME_TO_VALUE[self.warning_status_vars["temperature"].get()]
            humidity = STATUS_NAME_TO_VALUE[self.warning_status_vars["humidity"].get()]
            gas = STATUS_NAME_TO_VALUE[self.warning_status_vars["gas"].get()]
        except KeyError:
            messagebox.showerror("Warning flow", "Choose valid status values")
            return
        if all(code not in (3, 4) for code in (pressure, temperature, humidity, gas)):
            messagebox.showerror(
                "Warning flow",
                "At least one measurement status must be 'long invalid low' or 'long invalid high' to trigger the warning state.",
            )
            return
        self.backend.submit(
            self.backend.send_warning_flow(messages, timestamp, pressure, temperature, humidity, gas)
        )

    def clear_history_table(self) -> None:
        for row_id in self.history_tree.get_children():
            self.history_tree.delete(row_id)

    def fetch_buffered_history(self) -> None:
        try:
            host_id = int(self.login_id_var.get())
        except ValueError:
            messagebox.showerror("Buffered history", "Host ID must be an integer")
            return

        room = self.login_room_var.get().strip()
        if not room:
            messagebox.showerror("Buffered history", "Room name is required for authentication")
            return

        self.clear_history_table()

        async def _login_then_fetch() -> None:
            await self.backend.login(host_id, room)
            await asyncio.sleep(0.3)
            await self.backend.fetch_cached_history()

        self.backend.submit(_login_then_fetch())

    def _poll_ui_queue(self) -> None:
        try:
            while True:
                event, payload = self.ui_queue.get_nowait()
                self._handle_event(event, payload)
        except queue.Empty:
            pass
        self.root.after(100, self._poll_ui_queue)

    def _handle_event(self, event: str, payload: Any) -> None:
        if event == "busy":
            self.busy_var.set("Working..." if payload else "Idle")
            return
        if event == "scan_results":
            name_filter = self.device_name_filter_var.get().strip().lower()
            choices = []
            self.devices_by_display.clear()
            for item in payload:
                name = item["name"]
                if name_filter and name_filter not in name.lower():
                    continue
                display = f"{name} | {item['address']}"
                choices.append(display)
                self.devices_by_display[display] = item["address"]
            self.device_combo["values"] = choices
            if choices:
                self.selected_device_var.set(choices[0])
                self.log(f"Scan found {len(choices)} matching device(s)")
            else:
                self.selected_device_var.set("")
                self.log("Scan finished; no matching devices found")
            return
        if event == "connected":
            self.connection_var.set(f"Connected to {payload}")
            self.log(f"Connected to {payload}")
            return
        if event == "disconnected":
            self.connection_var.set("Disconnected")
            self.log("Disconnected")
            return
        if event == "error":
            self.log(f"ERROR: {payload}")
            messagebox.showerror("BLE Tester", payload)
            return
        if event == "history_reset":
            self.clear_history_table()
            self.log("Buffered history table cleared")
            return
        if event == "history_row":
            row_id = self.history_tree.insert(
                "",
                "end",
                values=(
                    payload["timestamp_ms"],
                    f"{payload['pressure_hpa']:.2f}",
                    f"{payload['temperature_c']:.2f}",
                    f"{payload['humidity_pct']:.2f}",
                    f"{payload['air_quality_iaq']:.2f}",
                ),
            )
            self.history_tree.see(row_id)
            return
        if event == "history_done":
            self.log(f"Buffered history fetch finished with {payload} row(s)")
            return
        if event == "log":
            self.log(str(payload))
            return
        if event in {"read_result", "write_result", "notification"}:
            key = payload["key"]
            card = self.cards.get(key)
            if card:
                card.update_value(payload["pretty"], payload["hex"])
            if event == "notification":
                self.log(f"Notify [{CHARS[key].name}]: {payload['pretty']} | hex={payload['hex']}")
            elif event == "read_result":
                self.log(f"Read [{CHARS[key].name}]: {payload['pretty']} | hex={payload['hex']}")
            else:
                self.log(f"Write [{CHARS[key].name}]: {payload['pretty']} | hex={payload['hex']}")
            return
        if event == "notify_state":
            key = payload["key"]
            state = "enabled" if payload["enabled"] else "disabled"
            self.log(f"Notifications {state} for {CHARS[key].name}")
            return


def main() -> None:
    root = tk.Tk()
    app: Optional[BLEApp] = None
    try:
        app = BLEApp(root)
        root.mainloop()
    finally:
        try:
            if app is not None:
                app.backend.submit(app.backend.disconnect())
        except Exception:
            pass


if __name__ == "__main__":
    main()
