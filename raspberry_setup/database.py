import asyncio
import aiosqlite
from bleak import BleakClient

async def init_db(db: aiosqlite.Connection) -> None:
    await db.executescript(
        """
        CREATE TABLE IF NOT EXISTS sensor_data (
            id                INTEGER PRIMARY KEY AUTOINCREMENT,
            sensor_station_id INTEGER NOT NULL,
            room_id           INTEGER NOT NULL,
            -- ISO 8601 local datetime string (e.g. '2026-05-04T14:30:00.123').
            -- Derived in ble_worker: datetime.fromtimestamp(anchor_pi_time + (pkt_millis - anchor_millis) / 1000.0)
            timestamp         TEXT    NOT NULL,
            temperature       REAL    NOT NULL,
            humidity          REAL    NOT NULL,
            pressure          REAL    NOT NULL,
            air_quality       REAL    NOT NULL,
            sent              INTEGER NOT NULL DEFAULT 0
        );

        CREATE TABLE IF NOT EXISTS thresholds (
            metric      TEXT PRIMARY KEY,
            upper_bound REAL,
            lower_bound REAL,
            hint_text   TEXT
        );

        CREATE TABLE IF NOT EXISTS threshold_violations (
            id                INTEGER PRIMARY KEY AUTOINCREMENT,
            sensor_station_id INTEGER NOT NULL,
            metric            TEXT    NOT NULL,
            room_id           INTEGER NOT NULL,
            status            TEXT    NOT NULL DEFAULT 'ACTIVE',
            start_time        TEXT    NOT NULL,
            end_time          TEXT,
            value_at_trigger  REAL    NOT NULL
        );

        CREATE TABLE IF NOT EXISTS pi_config (
            key   TEXT PRIMARY KEY,
            value TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS pi_state (
            key   TEXT PRIMARY KEY,
            value TEXT
        );

        CREATE TABLE IF NOT EXISTS stations (
            address              TEXT    PRIMARY KEY,
            sensor_station_id    INTEGER NOT NULL,
            room_id              INTEGER NOT NULL,
            name                 TEXT    NOT NULL,
            device_status        TEXT    NOT NULL,
            measurement_interval INTEGER NOT NULL
        );
        """
    )
    await db.commit()
    print("[DB] schema initialised.")

async def db_writer(
    queue: asyncio.Queue,
    db: aiosqlite.Connection,
    client: BleakClient,
) -> None:
    while True:
        pkt: dict = await queue.get()
        try:
            await db.execute(
                """
                INSERT INTO sensor_data
                    (sensor_station_id, room_id, timestamp,
                     temperature, humidity, pressure, air_quality, sent)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0)
                """,
                (
                    pkt["sensor_station_id"],
                    pkt["room_id"],
                    pkt["timestamp"],
                    pkt["temperature"],
                    pkt["humidity"],
                    pkt["pressure"],
                    pkt["air_quality"],
                ),
            )
            await db.commit()
            print(f"[DB] saved row timestamp={pkt['timestamp']} "
                  f"station={pkt['sensor_station_id']} room={pkt['room_id']}")
        except Exception as e:
            print(f"[DB] write error: {e}")
        finally:
            queue.task_done()

async def save_station(db: aiosqlite.Connection, station: dict) -> None:
    """
    Upserts a single station by BLE address.
    Called whenever POST /api/spi/{piId}/stations delivers an assignment.
    Existing stations are preserved, only the upserted address is updated.
    """
    await db.execute(
        """
        INSERT INTO stations
            (address, sensor_station_id, room_id, name, device_status, measurement_interval)
        VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT(address) DO UPDATE SET
            sensor_station_id    = excluded.sensor_station_id,
            room_id              = excluded.room_id,
            name                 = excluded.name,
            device_status        = excluded.device_status,
            measurement_interval = excluded.measurement_interval
        """,
        (
            station["bleMac"],
            station["id"],
            station["roomId"],
            station["name"],
            station["deviceStatus"],
            station["measurementInterval"],
        ),
    )
    await db.commit()
    print(f"[DB] upserted station address={station['bleMac']} id={station['id']}")


async def remove_station(db: aiosqlite.Connection, address: str) -> None:
    await db.execute("DELETE FROM stations WHERE address = ?", (address,))
    await db.commit()
    print(f"[DB] removed station address={address}")


async def load_stations(db: aiosqlite.Connection) -> list[dict]:
    """
    Reads all known station assignments from SQLite.
    Returns snake_case keys matching the Station dataclass in main.py.
    Used on boot and whenever station_manager wakes up.
    """
    async with db.execute(
        """SELECT address, sensor_station_id, room_id,
                  name, device_status, measurement_interval
           FROM stations"""
    ) as cursor:
        rows = await cursor.fetchall()
    return [
        {
            "address":             row[0],
            "sensor_station_id":   row[1],
            "room_id":             row[2],
            "name":                row[3],
            "device_status":       row[4],
            "measurement_interval": row[5],
        }
        for row in rows
    ]