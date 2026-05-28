import asyncio
import logging
import sys
import aiosqlite
import uvicorn
import aiohttp
from bleak import BleakClient
from dataclasses import dataclass
import config as cfg
from config import DB_PATH, load_config, get_local_ip

from database import init_db, db_writer, load_stations, remove_station, cleanup_old_rows
from ble_worker import ble_worker
from http_sender import http_sender
from state import set_privacy_mode
import app as app_module
from app import app
from thresholds import load_thresholds_from_db
from violation_tracker import load_window_seconds

log = logging.getLogger(__name__)


def _setup_logging() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)-8s %(message)s",
        datefmt="%Y-%m-%dT%H:%M:%S",
        handlers=[
            logging.StreamHandler(sys.stdout),
            logging.FileHandler("app.log", encoding="utf-8"),
        ],
    )


@dataclass
class Station:
    address:             str    # bleMac
    sensor_station_id:   int    # id
    room_id:             int    # roomId
    name:                str    # name
    device_status:       str    # deviceStatus
    measurement_interval: int   # measurementInterval


async def post_booted() -> None:
    """Notify the backend that this Pi has started up (POST /api/cpi/{piId}/booted)."""
    url = f"{cfg.BACKEND_URL}/api/cpi/{cfg.PI_ID}/booted"
    payload = {
        "ipAddress":    get_local_ip(),
        "hostName":     cfg.HOST_NAME,
        "deviceStatus": "ONLINE",
    }
    try:
        async with aiohttp.ClientSession() as session:
            async with session.post(
                url, json=payload,
                timeout=aiohttp.ClientTimeout(total=10),
            ) as resp:
                log.info(f"[CFG] POST booted → {resp.status}")
    except Exception as e:
        log.warning(f"[CFG] could not post booted: {type(e).__name__}: {e!r}")




MAX_CONNECT_FAILURES = 5
MIN_HEALTHY_UPTIME   = 30  # seconds — reset counter only if connection lasted this long

async def device_loop(
    station: Station,
    queue: asyncio.Queue,
    db: aiosqlite.Connection,
) -> None:
    import time as _time
    delay        = 5
    max_delay    = 300
    fail_count   = 0
    connected_at: float | None = None

    while True:
        try:
            log.info(
                f"[BLE:{station.address}] connecting "
                f"(pi_id={cfg.PI_ID}, sensor_station_id={station.sensor_station_id}, "
                f"name={station.name!r})…"
            )
            async with BleakClient(station.address, timeout=20.0) as client:
                log.info(f"[BLE:{station.address}] connected.")
                connected_at = _time.monotonic()
                delay        = 5
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
            uptime = _time.monotonic() - connected_at if connected_at is not None else 0
            connected_at = None
            if uptime >= MIN_HEALTHY_UPTIME:
                fail_count = 0
            fail_count += 1
            log.warning(
                f"[BLE:{station.address}] lost: {e}  – retrying in {delay}s… "
                f"(failure {fail_count}/{MAX_CONNECT_FAILURES}, uptime={uptime:.0f}s)"
            )
            try:
                from setup_flow import patch_station_status
                async with aiohttp.ClientSession() as http_session:
                    await patch_station_status(
                        http_session, station.sensor_station_id, "CONNECTION_FAILED", station.address
                    )
            except Exception as patch_err:
                log.warning(f"[BLE:{station.address}] CONNECTION_FAILED patch failed: {patch_err}")
            if fail_count >= MAX_CONNECT_FAILURES:
                log.error(
                    f"[BLE:{station.address}] too many consecutive failures – "
                    f"removing from DB, waiting for backend to re-scan"
                )
                await remove_station(db, station.address)
                app_module.stations_event.set()
                return

        await asyncio.sleep(delay)
        delay = min(delay * 2, max_delay)


async def station_manager(
    queue: asyncio.Queue,
    db: aiosqlite.Connection,
) -> None:
    active_tasks: dict[str, asyncio.Task] = {}

    while True:
        rows     = await load_stations(db)
        stations = [Station(**r) for r in rows]
        log.info(f"[SYS] {len(stations)} station(s) loaded from DB")

        new_addresses    = {s.address for s in stations}
        active_addresses = set(active_tasks.keys())

        for addr in active_addresses - new_addresses:
            log.info(f"[SYS] removing station {addr}")
            active_tasks[addr].cancel()
            del active_tasks[addr]

        for station in stations:
            if station.address not in active_tasks:
                log.info(f"[SYS] adding station {station.address}")
                task = asyncio.create_task(
                    device_loop(station, queue, db),
                    name=f"ble-{station.address}",
                )
                active_tasks[station.address] = task

        await app_module.stations_event.wait()
        app_module.stations_event.clear()


async def _db_cleanup_loop(db: aiosqlite.Connection) -> None:
    while True:
        await asyncio.sleep(3600)
        await cleanup_old_rows(db)


async def main() -> None:
    _setup_logging()
    load_config("conf.yml")

    set_privacy_mode(cfg.PRIVACY_MODE)

    await post_booted()

    queue: asyncio.Queue = asyncio.Queue()

    async with aiosqlite.connect(DB_PATH) as db:
        await init_db(db)
        log.info(f"[DB] opened {DB_PATH}")

        await load_thresholds_from_db(db)
        await load_window_seconds()

        app_module.db_connection = db

        uv_config = uvicorn.Config(app, host="0.0.0.0", port=8000, log_level="info")
        server    = uvicorn.Server(uv_config)

        log.info("[SYS] Starting station manager, HTTP-Sender and Webserver…")
        await asyncio.gather(
            station_manager(queue, db),
            http_sender(db),
            _db_cleanup_loop(db),
            server.serve(),
        )

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        log.info("Stopped.")
