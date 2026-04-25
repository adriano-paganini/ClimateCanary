import aiosqlite
import asyncio
import config
from config import DB_PATH, LED_COLOUR_UUID
from bleak import BleakClient


async def init_db(db: aiosqlite.Connection):
    await db.execute("""
        CREATE TABLE IF NOT EXISTS sensor_data (
            id                INTEGER PRIMARY KEY AUTOINCREMENT,
            timestamp         INTEGER,
            temperature       REAL,
            humidity          REAL,
            pressure          REAL,
            air_quality       INTEGER,
            room_id           INTEGER,
            sensor_station_id TEXT,
            sent              INTEGER DEFAULT 0
        )
    """)
    await db.execute("""
        CREATE TABLE IF NOT EXISTS pi_state (
            key   TEXT PRIMARY KEY,
            value TEXT
        )
    """)
    await db.commit()


async def db_writer(queue: asyncio.Queue, db: aiosqlite.Connection, client: BleakClient):
    while True:
        payload = await queue.get()
        try:
            await db.execute(
                """INSERT INTO sensor_data
                   (timestamp, temperature, humidity, pressure, air_quality,
                    room_id, sensor_station_id)
                   VALUES (?, ?, ?, ?, ?, ?, ?)""",
                (
                    payload["timestamp"],
                    payload["temperature"],
                    payload["humidity"],
                    payload["pressure"],
                    payload["air_quality"],
                    config.ROOM_ID,
                    config.SENSOR_STATION_ID,
                ),
            )
            await db.commit()
            print(f"[DB] saved row ts={payload['timestamp']}")

            if payload["temperature"] > 30.0:
                await client.write_gatt_char(LED_COLOUR_UUID, bytearray([255, 0, 0]))
            else:
                await client.write_gatt_char(LED_COLOUR_UUID, bytearray([0, 255, 0]))

        except Exception as e:
            print(f"[DB] write error: {e}")
        finally:
            queue.task_done()