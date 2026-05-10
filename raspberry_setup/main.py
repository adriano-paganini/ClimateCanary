import asyncio
import aiosqlite
import uvicorn
import aiohttp
from bleak import BleakClient
from dataclasses import dataclass

import config as cfg
from config import DB_PATH, load_config, load_config_from_string, get_local_ip
from database import init_db, db_writer, load_stations
from ble_worker import ble_worker
from http_sender import http_sender
from state import set_privacy_mode
import app as app_module
from app import app
from thresholds import load_thresholds_from_db
from violation_tracker import load_window_seconds


@dataclass
class Station:
    address:              str
    sensor_station_id:    int
    room_id:              int
    name:                 str
    device_status:        str
    measurement_interval: int


async def post_booted() -> None:
    url = f"{cfg.BACKEND_URL}/api/cpi/{cfg.PI_ID}/booted"
    payload = {
        "ipAddress":        get_local_ip(),
        "hostName":         cfg.HOST_NAME,
        "deviceStatus":     "ONLINE",
        "roomId":           cfg.ROOM_ID,
        "sensorStationIds": [],
    }
    try:
        async with aiohttp.ClientSession() as session:
            async with session.post(
                url, json=payload,
                timeout=aiohttp.ClientTimeout(total=10),
            ) as resp:
                print(f"[CFG] POST booted → {resp.status}")
    except Exception as e:
        print(f"[CFG] could not post booted: {e}")


async def fetch_config_from_backend() -> None:
    url = f"{cfg.BACKEND_URL}/api/cpi/{cfg.PI_ID}/config"
    try:
        async with aiohttp.ClientSession() as session:
            async with session.get(
                url, timeout=aiohttp.ClientTimeout(total=10),
            ) as resp:
                if resp.status == 200:
                    yaml_text = await resp.text()
                    load_config_from_string(yaml_text)
                    print("[CFG] config loaded from backend.")
                else:
                    print(f"[CFG] backend returned {resp.status}, using local conf.yml.")
    except Exception as e:
        print(f"[CFG] could not fetch config: {e}")


async def wait_for_stations(db: aiosqlite.Connection) -> list[Station]:
    await app_module.stations_event.wait()
    app_module.stations_event.clear()
    rows     = await load_stations(db)
    stations = [Station(**r) for r in rows]
    print(f"[CFG] {len(stations)} station(s) loaded from DB.")
    return stations


async def device_loop(
    station: Station,
    queue: asyncio.Queue,
    db: aiosqlite.Connection,
) -> None:
    delay     = 5
    max_delay = 300

    while True:
        try:
            print(
                f"[BLE:{station.address}] connecting "
                f"(pi_id={cfg.PI_ID}, sensor_station_id={station.sensor_station_id}, "
                f"name={station.name!r})…"
            )
            async with BleakClient(station.address, timeout=20.0) as client:
                print(f"[BLE:{station.address}] connected.")
                delay = 5
                await asyncio.gather(
                    ble_worker(
                        queue, client,
                        station.sensor_station_id,
                        station.room_id,
                        station.measurement_interval,
                        db,
                    ),
                    db_writer(queue, db, client),
                )

        except Exception as e:
            print(f"[BLE:{station.address}] lost: {e}  – retrying in {delay}s…")

        await asyncio.sleep(delay)
        delay = min(delay * 2, max_delay)


async def station_manager(
    queue: asyncio.Queue,
    db: aiosqlite.Connection,
) -> None:
    active_tasks: dict[str, asyncio.Task] = {}

    rows = await load_stations(db)
    if rows:
        stations = [Station(**r) for r in rows]
        print(f"[SYS] restoring {len(stations)} station(s) from DB…")
        for station in stations:
            task = asyncio.create_task(
                device_loop(station, queue, db),
                name=f"ble-{station.address}",
            )
            active_tasks[station.address] = task

    while True:
        stations = await wait_for_stations(db)

        new_addresses    = {s.address for s in stations}
        active_addresses = set(active_tasks.keys())

        for addr in active_addresses - new_addresses:
            print(f"[SYS] removing station {addr}")
            active_tasks[addr].cancel()
            del active_tasks[addr]

        for station in stations:
            if station.address not in active_tasks:
                print(f"[SYS] adding station {station.address}")
                task = asyncio.create_task(
                    device_loop(station, queue, db),
                    name=f"ble-{station.address}",
                )
                active_tasks[station.address] = task


async def main() -> None:
    load_config("conf.yml")

    if cfg.BACKEND_URL:
        await fetch_config_from_backend()

    set_privacy_mode(cfg.PRIVACY_MODE)
    await post_booted()

    queue: asyncio.Queue = asyncio.Queue()

    async with aiosqlite.connect(DB_PATH) as db:
        await init_db(db)
        print(f"[DB] opened {DB_PATH}")

        await load_thresholds_from_db(db)
        await load_window_seconds(db)

        app_module.db_connection = db

        uv_config = uvicorn.Config(app, host="0.0.0.0", port=8000, log_level="info")
        server    = uvicorn.Server(uv_config)

        print("[SYS] starting station_manager, http_sender and webserver…")
        await asyncio.gather(
            station_manager(queue, db),
            http_sender(db),
            server.serve(),
        )


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("Stopped.")
async def wait_for_stations(db: aiosqlite.Connection) -> list[Station]:
    """
    Blocks until app.py sets stations_event (triggered by POST /api/spi/{piId}/stations).
    Reads the current station list from SQLite, DB is single source of truth.
    """
    await app_module.stations_event.wait()
    app_module.stations_event.clear()

    rows     = await load_stations(db)
    stations = [Station(**r) for r in rows]
    print(f"[CFG] {len(stations)} station(s) loaded from DB.")
    return stations


async def device_loop(
    station: Station,
    queue: asyncio.Queue,
    db: aiosqlite.Connection,
) -> None:
    """
    Manages the full lifecycle of one Arduino connection.
    Branches on device_status:

    AVAILABLE  -> first-time setup: write TrustedRpiId + measurementInterval,
                  then exit. Arduino reboots, backend re-sends /stations.
    other      -> normal operation: connect and run ble_worker + db_writer.
                  Reconnects automatically with exponential backoff on failure.
    """
    delay     = 5
    max_delay = 300

    while True:
        try:
            print(
                f"[BLE:{station.address}] connecting "
                f"(pi_id={cfg.PI_ID}, sensor_station_id={station.sensor_station_id}, "
                f"name={station.name!r})…"
            )
            async with BleakClient(station.address, timeout=20.0) as client:
                print(f"[BLE:{station.address}] connected.")
                delay = 5
                await asyncio.gather(
                    ble_worker(
                        queue, client,
                        station.sensor_station_id,
                        station.room_id,
                        station.measurement_interval,
                        db,
                    ),
                    db_writer(queue, db, client),
                )

        except Exception as e:
            print(f"[BLE:{station.address}] lost: {e}  – retrying in {delay}s…")

        await asyncio.sleep(delay)
        delay = min(delay * 2, max_delay)


async def station_manager(
    queue: asyncio.Queue,
    db: aiosqlite.Connection,
) -> None:
    """
    Reacts to backend pushes via POST /api/spi/{piId}/stations.
    On each update it
      - Cancels tasks for stations no longer in the list
      - Starts new tasks for newly added stations
      - Doesnt touch the tasks of exisiting stations
    """
    active_tasks: dict[str, asyncio.Task] = {}

    while True:
        stations = await wait_for_stations(db)

        new_addresses    = {s.address for s in stations}
        active_addresses = set(active_tasks.keys())

        for addr in active_addresses - new_addresses:
            print(f"[SYS] removing station {addr}")
            active_tasks[addr].cancel()
            del active_tasks[addr]

        for station in stations:
            if station.address not in active_tasks:
                print(f"[SYS] adding station {station.address}")
                task = asyncio.create_task(
                    device_loop(station, queue, db),
                    name=f"ble-{station.address}",
                )
                active_tasks[station.address] = task

async def main() -> None:
    load_config("conf.yml")

    if cfg.BACKEND_URL:
        await fetch_config_from_backend()

    set_privacy_mode(cfg.PRIVACY_MODE)

    await post_booted()

    queue: asyncio.Queue = asyncio.Queue()

    async with aiosqlite.connect(DB_PATH) as db:
        await init_db(db)
        print(f"[DB] opened {DB_PATH}")

        await load_thresholds_from_db(db)
        await load_window_seconds(db)

        app_module.db_connection = db

        uv_config = uvicorn.Config(app, host="0.0.0.0", port=8000, log_level="info")
        server    = uvicorn.Server(uv_config)

        print("[SYS] starting station_manager, http_sender and webserver…")
        await asyncio.gather(
            station_manager(queue, db),
            http_sender(db),
            server.serve(),
        )


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("Stopped.")