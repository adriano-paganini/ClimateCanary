"""
setup_flow.py
-------------
UC-10: Erstkonfiguration einer Sensorstation (Arduino im Setup-Modus).

Called from device_loop in main.py when station.device_status == 'AVAILABLE'.

Flow:
  1. Connect to Arduino (already in setup mode, address known from SensorStationDTO)
  2. Write DeviceSetupConfig (measurementInterval + PI_ID as TrustedRpiId)
  3. Arduino reboots → BLE connection drops (expected)
  4. PATCH backend with CONNECTED or CONNECTION_FAILED
"""

import struct
import aiohttp
from bleak import BleakClient

import config
from config import SETUP_CONFIG_UUID


def _build_setup_config(measurement_interval: int, pi_id: int) -> bytes:
    """
    DeviceSetupConfig: uint8 measurementInterval | uint32 deviceId  →  5 bytes LE
    Example: interval=10, pi_id=123456 → 0A 40 E2 01 00
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
    """
    Writes TrustedRpiId + measurementInterval to the Arduino's
    deviceSetupCharacteristic. The Arduino reboots afterwards — the
    connection drop is expected and not treated as an error.

    Returns True if the write succeeded, False otherwise.
    In both cases the backend is PATCHed with the appropriate status.
    """
    tag     = address
    payload = _build_setup_config(measurement_interval, config.PI_ID)
    print(f"[SETUP:{tag}] writing config: interval={measurement_interval}s "
          f"pi_id={config.PI_ID}  payload={payload.hex()}")

    async with aiohttp.ClientSession() as session:
        try:
            async with BleakClient(address, timeout=20.0) as client:
                await client.write_gatt_char(
                    SETUP_CONFIG_UUID, payload, response=True,
                )
                print(f"[SETUP:{tag}] config written — Arduino will reboot.")
            return True

        except Exception as e:
            print(f"[SETUP:{tag}] failed: {e}")
            await patch_station_status(session, sensor_station_id, "CONNECTION_FAILED", tag)
            return False