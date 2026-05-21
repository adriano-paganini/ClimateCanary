import asyncio
import struct
import traceback
import aiohttp
from bleak import BleakClient
from bleak.exc import BleakCharacteristicNotFoundError

import config
from config import SETUP_CONFIG_UUID
from database import save_station


def _build_setup_config(measurement_interval: int, pi_id: int) -> bytes:
    if not (1 <= measurement_interval <= 255):
        raise ValueError("measurementInterval must be 1–255")
    return struct.pack("<BI", measurement_interval, pi_id)


async def patch_station_status(
    session: aiohttp.ClientSession,
    sensor_station_id: int,
    status: str,
    tag: str,
) -> None:
    url = f"{config.BACKEND_URL}/api/cpi/{config.PI_ID}/{sensor_station_id}"
    try:
        async with session.patch(
            url,
            json=status,
            timeout=aiohttp.ClientTimeout(total=5),
        ) as resp:
            print(f"[SETUP:{tag}] PATCH status={status} → {resp.status}")
    except Exception as e:
        print(f"[SETUP:{tag}] PATCH failed: {e}")


async def run_setup(
    station: dict,   # full SensorStationDTO dict: id, bleMac, name, roomId, …
    measurement_interval: int,
) -> bool:
    import app as app_module

    address = station["bleMac"]
    sensor_station_id = station["id"]
    tag = address

    payload = _build_setup_config(measurement_interval, config.PI_ID)
    print(
        f"[SETUP:{tag}] writing config: interval={measurement_interval}s "
        f"pi_id={config.PI_ID}  payload={payload.hex()}"
    )

    try:
        from ble_scanner import _scan_lock
        async with _scan_lock:
            pass  # wait for any active scan to finish before connecting

        async with BleakClient(address, timeout=20.0) as client:
            await client.write_gatt_char(SETUP_CONFIG_UUID, payload, response=True)
            print(f"[SETUP:{tag}] config written. Arduino will reboot.")
            await asyncio.sleep(6.0)

        return True

    except Exception as e:
        print(f"[SETUP:{tag}] connection failed: {type(e).__name__}: {e}")
        traceback.print_exc()
        async with aiohttp.ClientSession() as session:
            await patch_station_status(session, sensor_station_id, "CONNECTION_FAILED", tag)
        return False