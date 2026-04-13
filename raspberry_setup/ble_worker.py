import asyncio
import struct
from bleak import BleakClient
from config import DATA_CHAR_UUID, SENSOR_STRUCT

async def ble_worker(queue: asyncio.Queue, client: BleakClient):
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