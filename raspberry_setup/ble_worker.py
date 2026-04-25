import asyncio
import struct
from bleak import BleakClient
from config import DATA_CHAR_UUID, AUTH_CHAR_UUID, SENSOR_STRUCT, DEVICE_ID, ROOM_NAME

async def _send_auth(client: BleakClient):
    """Sendet DeviceAuthenticationPacket direkt nach dem Connect."""
    room_bytes = ROOM_NAME.encode("ascii")
    room_len = len(room_bytes)
    assert room_len <= 32, "ROOM_NAME darf max. 32 Zeichen haben"

    # uint32 deviceId, char[32] roomName, uint8 roomNameLength
    payload = struct.pack(
        "<I32sB",
        DEVICE_ID,
        room_bytes.ljust(32, b"\x00"),
        room_len,
    )

    await client.write_gatt_char(AUTH_CHAR_UUID, payload, response=True)
    print(f"[BLE] authenticated (deviceId={DEVICE_ID}, room={ROOM_NAME!r})")

async def ble_worker(queue: asyncio.Queue, client: BleakClient):
    await _send_auth(client)

    def handle_notification(sender, data):
        try:
            ts, press, temp, hum, gas = struct.unpack(SENSOR_STRUCT, data)
            payload = {
                "timestamp":      ts,
                "temperature":    temp,
                "humidity":       hum,
                "pressure":       press,
                "gas_resistance": gas,
            }
            queue.put_nowait(payload)
            print(f"[BLE] queued {ts}: {temp:.1f}°C {hum:.1f}% {press:.1f}hPa {gas}Ω")
        except Exception as e:
            print(f"[BLE] parse error: {e}")

    await client.start_notify(DATA_CHAR_UUID, handle_notification)
    print("[BLE] subscribed to notifications")

    while True:
        await asyncio.sleep(1)