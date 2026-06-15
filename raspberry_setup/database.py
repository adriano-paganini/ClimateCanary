import asyncio
import logging
from datetime import datetime, timedelta
import aiosqlite
from bleak import BleakClient

log = logging.getLogger(__name__)

async def init_db(db: aiosqlite.Connection) -> None:
    await db.executescript(
        """
        CREATE TABLE IF NOT EXISTS sensor_data (
            id                INTEGER PRIMARY KEY AUTOINCREMENT,
            sensor_station_id INTEGER NOT NULL,
            room_id           INTEGER NOT NULL,
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
    log.info("[DB] schema initialised.")

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
            log.info(f"[DB] saved row timestamp={pkt['timestamp']} "
                     f"station={pkt['sensor_station_id']} room={pkt['room_id']}")
        except Exception as e:
            log.error(f"[DB] write error: {e}")
        finally:
            queue.task_done()

async def save_station(db: aiosqlite.Connection, station: dict) -> None:
    log.info(
        f"[DB] saving station: address={station['bleMac']} id={station['id']} "
        f"room_id={station['roomId']} name={station['name']!r} "
        f"status={station['deviceStatus']} interval={station['measurementInterval']}"
    )
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
    log.info(f"[DB] upserted station address={station['bleMac']} id={station['id']}")


async def remove_station(db: aiosqlite.Connection, address: str) -> None:
    await db.execute("DELETE FROM stations WHERE address = ?", (address,))
    await db.commit()
    log.info(f"[DB] removed station address={address}")


async def cleanup_old_rows(db: aiosqlite.Connection, retention_hours: int = 48) -> None:
    cutoff = (datetime.now() - timedelta(hours=retention_hours)).strftime("%Y-%m-%dT%H:%M:%S.000")

    cursor = await db.execute(
        "DELETE FROM sensor_data WHERE timestamp < ? AND sent = 1", (cutoff,)
    )
    sent_deleted = cursor.rowcount

    async with db.execute(
        "SELECT COUNT(*) FROM sensor_data WHERE timestamp < ? AND sent = 0", (cutoff,)
    ) as cursor:
        (unsent_count,) = await cursor.fetchone()

    if unsent_count:
        log.warning(f"[DB] cleanup: deleting {unsent_count} unsent row(s) older than "
                    f"{retention_hours}h — backend was unreachable too long, data lost")
        await db.execute(
            "DELETE FROM sensor_data WHERE timestamp < ? AND sent = 0", (cutoff,)
        )

    cursor = await db.execute(
        "DELETE FROM threshold_violations WHERE start_time < ?", (cutoff,)
    )
    violations_deleted = cursor.rowcount

    await db.commit()
    log.info(f"[DB] cleanup: removed {sent_deleted} sent measurement(s), "
             f"{unsent_count} unsent measurement(s), "
             f"{violations_deleted} violation(s) older than {retention_hours}h")


async def load_stations(db: aiosqlite.Connection) -> list[dict]:
    async with db.execute(
        """SELECT address, sensor_station_id, room_id,
                  name, device_status, measurement_interval
           FROM stations"""
    ) as cursor:
        rows = await cursor.fetchall()
    for row in rows:
        log.info(
            f"[DB] loaded station row: address={row[0]} id={row[1]} "
            f"room_id={row[2]} name={row[3]!r} status={row[4]} interval={row[5]}"
        )
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
