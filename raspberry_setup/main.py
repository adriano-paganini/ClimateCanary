import asyncio
import aiosqlite
import uvicorn
from bleak import BleakClient
from config import DEVICE_ADDR, DB_PATH, load_config
from database import init_db, db_writer
from ble_worker import ble_worker
from http_sender import http_sender
from state import load_occupancy_from_db
from pathlib import Path
from app import app
import config as cfg

async def main():
    if Path("conf.yml").exists():
            load_config("conf.yml")
            await send_booted()
    else:
        print("[CFG] no conf.yml found. ")

    queue: asyncio.Queue = asyncio.Queue()

    async with aiosqlite.connect(DB_PATH) as db:
        await init_db(db)
        await load_occupancy_from_db(db)
        print(f"[DB] opened {DB_PATH}")

        async def ble_loop():
            delay = 5
            max_delay = 300

            while True:
                if not cfg.DEVICE_ADDR:
                    print("[BLE] no station bound yet. Waiting...")
                    await asyncio.sleep(10)
                    continue

                try:
                    print(f"[BLE] connecting to {cfg.DEVICE_ADDR}…")
                    async with BleakClient(cfg.DEVICE_ADDR, timeout=20.0) as client:
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

        uv_config = uvicorn.Config(app, host="0.0.0.0", port=8000, log_level="info")
        server = uvicorn.Server(uv_config)

        print("[SYS] Starting BLE, HTTP-Sender and Webserver...")
        await asyncio.gather(
            ble_loop(),
            http_sender(db),
            server.serve(),
        )

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("Stopped.")