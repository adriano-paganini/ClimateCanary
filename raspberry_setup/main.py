import asyncio
import aiosqlite
import uvicorn
import aiohttp
from bleak import BleakClient, BleakScanner
from bleak.backends.device import BLEDevice
from bleak.backends.scanner import AdvertisementData
from dataclasses import dataclass

import config as cfg
from config import DB_PATH, load_config
from database import init_db, db_writer
from ble_worker import ble_worker
from http_sender import http_sender
from state import set_privacy_mode
import app as app_module
from app import app
from config import BLE_NAME_NORMAL, MANUF_DATA_NORMAL, SVC_ENV_NORMAL, SCAN_DURATION

@dataclass
class Station:
    address:   str
    device_id: int
    room_name: str


def _manuf_matches(adv: AdvertisementData, expected: bytes) -> bool:
    return any(
        payload == expected or expected in payload
        for payload in adv.manufacturer_data.values()
    )


async def scan_for_all_devices() -> list[str]:
    """
    Scannt _SCAN_DURATION Sekunden nach Arduinos.
    Gibt eine deduplizierte Liste von BLE-Adressen zurück.
    """
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


async def post_discovered(addresses: list[str]) -> None:
    """
    POST /api/pi/{PI_ID}/discovered
    Body: { "addresses": ["AA:BB:...", ...] }
    Backend zeigt dem User die Liste zur Auswahl und ruft dann POST /stations auf.
    """
    url = f"{cfg.BACKEND_URL}/api/pi/{cfg.PI_ID}/discovered"
    try:
        async with aiohttp.ClientSession() as session:
            async with session.post(
                url, json={"addresses": addresses},
                timeout=aiohttp.ClientTimeout(total=10)
            ) as resp:
                print(f"[CFG] POST discovered → {resp.status}")
    except Exception as e:
        print(f"[CFG] could not post discovered devices: {e}")


async def wait_for_stations() -> list[Station]:
    """
    Blockiert bis app.py stations_event setzt (ausgelöst durch POST /stations).
    Liest dann selected_stations aus und gibt sie als Station-Objekte zurück.
    Setzt das Event danach zurück, damit es beim nächsten Aufruf wieder wartet.
    """
    print("[CFG] waiting for backend to push station selection…")
    await app_module.stations_event.wait()
    app_module.stations_event.clear()

    stations = [
        Station(
            address=s["address"],
            device_id=s["device_id"],
            room_name=s["room_name"],
        )
        for s in app_module.selected_stations
    ]
    print(f"[CFG] {len(stations)} station(s) received via POST /stations.")
    return stations


async def device_loop(
    station: Station,
    queue: asyncio.Queue,
    db: aiosqlite.Connection,
) -> None:
    """
    Verwaltet die Verbindung zu genau einer Station.
    Bei Verbindungsverlust: direkt per bekannter Adresse neu verbinden.
    Ist die Station nicht mehr in der nächsten Backend-Auswahl → Task beendet sich.
    """
    delay     = 5
    max_delay = 300
    current   = station

    while True:
        try:
            print(f"[BLE:{current.address}] connecting "
                  f"(deviceId={current.device_id}, room={current.room_name!r})…")
            async with BleakClient(current.address, timeout=20.0) as client:
                print(f"[BLE:{current.address}] connected.")
                delay = 5
                await asyncio.gather(
                    ble_worker(queue, client, current.device_id, current.room_name),
                    db_writer(queue, db, client),
                )
        except Exception as e:
            print(f"[BLE:{current.address}] lost: {e}  – retrying in {delay}s…")

        await asyncio.sleep(delay)
        delay = min(delay * 2, max_delay)


async def station_manager(
    queue: asyncio.Queue,
    db: aiosqlite.Connection,
) -> None:
    """
    Backend wählt stations aus, mit denen sich raspberry verbinden soll.
    Danach wartet es auf weitere POST /stations Aufrufe (wenn User stations hinzufügen/entfernen)

    """
    active_tasks: dict[str, asyncio.Task] = {}

    while True:
        stations = await wait_for_stations()

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


async def fetch_config_from_backend() -> None:
    url = f"{cfg.BACKEND_URL}/api/pi/{cfg.PI_ID}/config"
    try:
        async with aiohttp.ClientSession() as session:
            async with session.get(
                url, timeout=aiohttp.ClientTimeout(total=10)
            ) as resp:
                if resp.status == 200:
                    data = await resp.json()
                    load_config(data)
                    print("[CFG] config loaded from backend.")
                else:
                    print(f"[CFG] backend returned {resp.status}, using local conf.yml.")
    except Exception as e:
        print(f"[CFG] could not fetch config: {e}")


async def main() -> None:
    if cfg.BACKEND_URL:
        await fetch_config_from_backend()

    addresses = await scan_for_all_devices()

    if addresses:
        await post_discovered(addresses)
    else:
        print("[SYS] no devices found during scan.")

    queue: asyncio.Queue = asyncio.Queue()

    async with aiosqlite.connect(DB_PATH) as db:
        await init_db(db)
        print(f"[DB] opened {DB_PATH}")

        uv_config = uvicorn.Config(app, host="0.0.0.0", port=8000, log_level="info")
        server = uvicorn.Server(uv_config)

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