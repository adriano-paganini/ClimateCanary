import struct
import aiohttp
from bleak import BleakClient

import config
from config import SETUP_CONFIG_UUID


def _build_setup_config(measurement_interval: int, pi_id: int) -> bytes:
    """
    DeviceSetupConfig: uint8 measurementInterval | uint32 deviceId  →  5 bytes LE
    Example: interval=10, pi_id=123456 => 0A 40 E2 01 00
    """
    assert 1 <= measurement_interval <= 255, "measurementInterval must be 1–255"
    return struct.pack("<BI", measurement_interval, pi_id)


async def patch_station_status(
    session: aiohttp.ClientSession,
    sensor_station_id: int,
    status: str,
    tag: str,
) -> None:
    """PATCH /api/cpi/{piId}/{sensorStationId} with the given DeviceStatus."""
    url = f"{config.BACKEND_URL}/api/cpi/{config.PI_ID}/{sensor_station_id}"
    try:
        async with session.patch(
            url, json={"deviceStatus": status},
            timeout=aiohttp.ClientTimeout(total=5),
        ) as resp:
            print(f"[SETUP:{tag}] PATCH status={status} → {resp.status}")
    except Exception as e:
        print(f"[SETUP:{tag}] PATCH failed: {e}")


async def run_setup(
    address: str,
    sensor_station_id: int,
    measurement_interval: int,
) -> bool:
    tag     = address
    payload = _build_setup_config(measurement_interval, config.PI_ID)
    print(f"[SETUP:{tag}] writing config: interval={measurement_interval}s "
          f"pi_id={config.PI_ID}  payload={payload.hex()}")

    async with aiohttp.ClientSession() as session:
        try:
            async with BleakClient(address, timeout=20.0) as client:
                #print(f"[SETUP:{tag}] payload bytes: {' '.join(f)}")
                await client.write_gatt_char(
                    SETUP_CONFIG_UUID, payload, response=False,
                )
                print(f"[SETUP:{tag}] config written — Arduino will reboot.")
            return True

        except Exception as e:
            import traceback
            print(f"[SETUP:{tag}] connection failed: {type(e).__name__}: {e}")
            traceback.print_exc()
            await patch_station_status(session, sensor_station_id, "CONNECTION_FAILED", tag)
            return False