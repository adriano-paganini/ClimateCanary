import asyncio
import aiosqlite
import uvicorn
import aiohttp
from bleak import BleakClient, BleakScanner
from bleak.backends.device import BLEDevice
from bleak.backends.scanner import AdvertisementData
from dataclasses import dataclass

import config as cfg
from config import (
    DB_PATH, load_config, load_config_from_string, get_local_ip,
    BLE_NAME_NORMAL, MANUF_DATA_NORMAL, SVC_ENV_NORMAL, SCAN_DURATION,
)
from database import init_db, db_writer, load_stations
from ble_worker import ble_worker
from setup_flow import run_setup
from http_sender import http_sender
from state import set_privacy_mode
import app as app_module
from app import app
from thresholds import load_thresholds_from_db
from violation_tracker import load_window_seconds

@dataclass
class Station:
    address:             str    # bleMac
    sensor_station_id:   int    # id
    room_id:             int    # roomId
    name:                str    # name
    device_status:       str    # deviceStatus
    measurement_interval: int   # measurementInterval


# ---------------------------------------------------------------------------
# BLE scanning helpers
# ---------------------------------------------------------------------------

def _manuf_matches(adv: AdvertisementData, expected: bytes) -> bool:
    return any(
        payload == expected or expected in payload
        for payload in adv.manufacturer_data.values()
    )


async def scan_for_all_devices() -> list[str]:
    found: dict[str, BLEDevice] = {}

    def callback(device: BLEDevice, adv: AdvertisementData) -> None:
        if device.address in found:
            return
        if device.name != BLE_NAME_NORMAL:
            return
        if not _manuf_matches(adv, MANUF_DATA_NORMAL):
            return
        adv_svcs = [str(s).lower() for s in adv.service_uuids]
        if not any(SVC_ENV_NORMAL in s or s in SVC_ENV_NORMAL for s in adv_svcs):
            return
        print(f"[SCAN] discovered: {device.name}  {device.address}")
        found[device.address] = device

    async with BleakScanner(detection_callback=callback):
        print(f"[SCAN] scanning {SCAN_DURATION}s for {BLE_NAME_NORMAL!r} devices…")
        await asyncio.sleep(SCAN_DURATION)

    addresses = list(found.keys())
    print(f"[SCAN] found {len(addresses)} device(s): {addresses}")
    return addresses


# ---------------------------------------------------------------------------
# Backend communication helpers
# ---------------------------------------------------------------------------

async def post_booted() -> None:
    """Notify the backend that this Pi has started up (POST /api/cpi/{piId}/booted)."""
    url = f"{cfg.BACKEND_URL}/api/cpi/{cfg.PI_ID}/booted"
    payload = {
        "ipAddress":        get_local_ip(),
        "hostName":         cfg.HOST_NAME,
        "deviceStatus":     "ONLINE",
        "roomId":           cfg.ROOM_ID,
        "sensorStationIds": [],   # backend already knows assignments; empty on boot is fine
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


async def post_discovered(addresses: list[str]) -> None:
    """Tell the backend which BLE addresses were found during the scan."""
    url = f"{cfg.BACKEND_URL}/api/cpi/{cfg.PI_ID}/discovered"
    try:
        async with aiohttp.ClientSession() as session:
            async with session.post(
                url, json={"addresses": addresses},
                timeout=aiohttp.ClientTimeout(total=10),
            ) as resp:
                print(f"[CFG] POST discovered → {resp.status}")
    except Exception as e:
        print(f"[CFG] could not post discovered devices: {e}")


async def fetch_config_from_backend() -> None:
    """
    Fetch config from the backend (GET /api/cpi/{piId}/config).
    The backend returns a raw YAML string, not JSON.
    Falls back silently to the local conf.yml if unreachable.
    """
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

    rows = await load_stations(db)
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
            if station.device_status == "AVAILABLE":
                print(f"[BLE:{station.address}] setup mode — writing config…")
                success = await run_setup(
                    address=station.address,
                    sensor_station_id=station.sensor_station_id,
                    measurement_interval=station.measurement_interval,
                )
                if success:
                    print(f"[BLE:{station.address}] setup done — waiting for reboot.")
                    return
                else:
                    print(f"[BLE:{station.address}] setup failed — retrying in {delay}s…")

            else:
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

    addresses = await scan_for_all_devices()
    if addresses:
        await post_discovered(addresses)
    else:
        print("[SYS] no devices found during scan.")

    queue: asyncio.Queue = asyncio.Queue()

    async with aiosqlite.connect(DB_PATH) as db:
        await init_db(db)
        print(f"[DB] opened {DB_PATH}")

        await load_thresholds_from_db(db)
        await load_window_seconds(db)

        app_module.db_connection = db

        uv_config = uvicorn.Config(app, host="0.0.0.0", port=8000, log_level="info")
        server    = uvicorn.Server(uv_config)

        print("[SYS] Starting station manager, HTTP-Sender and Webserver…")
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