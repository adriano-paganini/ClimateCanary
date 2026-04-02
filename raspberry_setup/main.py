import asyncio
import aiosqlite
from bleak import BleakClient
from config import DEVICE_ADDR, DB_PATH
from database import init_db, db_writer
from ble_worker import ble_worker
from http_sender import http_sender

async def main():
    queue: asyncio.Queue = asyncio.Queue()

    async with aiosqlite.connect(DB_PATH) as db:
        await init_db(db)
        print(f"[DB] opened {DB_PATH}")

        async def ble_loop():
            delay = 5
            max_delay = 300

            while True:
                try:
                    print(f"[BLE] connecting to {DEVICE_ADDR}…")
                    async with BleakClient(DEVICE_ADDR, timeout=20.0) as client:
                        print(f"[BLE] connected: {client.is_connected}")
                        delay = 5 
                        await asyncio.gather(
                            ble_worker(queue, client),
                            db_writer(queue, db, client),
                        )
                except Exception as e:
                    print(f"[BLE] connection failed: {e}")
                    print(f"[BLE] retrying in {delay}s…")
                    await asyncio.sleep(delay)
                    delay = min(delay * 2, max_delay)

        await asyncio.gather(
            ble_loop(),
            http_sender(db),
        )

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("Stopped.")