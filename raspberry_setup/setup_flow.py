"""
Einmalige Erstkonfiguration eines Arduinos im Setup-Modus.
Wird von app.py über POST /api/pi/setup/{address} aufgerufen.
"""

import asyncio
import struct
from bleak import BleakClient, BleakScanner
from bleak.backends.device import BLEDevice
from bleak.backends.scanner import AdvertisementData
from config import BLE_NAME_NORMAL, MANUF_DATA_NORMAL, SVC_ENV_NORMAL, SCAN_DURATION
from config import SETUP_CONFIG_UUID

def _build_setup_config(measurement_interval: int, device_id: int) -> bytes:
    """
    DeviceSetupConfig: uint8 measurementInterval | uint32 deviceId  →  5 Byte LE
    Beispiel: interval=10, deviceId=123456 → 0A 40 E2 01 00
    """
    assert 1 <= measurement_interval <= 255, "measurementInterval muss 1–255 sein"
    return struct.pack("<BI", measurement_interval, device_id)


async def _scan_for_setup_device() -> BLEDevice | None:
    """
    Scannt nach einem Gerät im Setup-Modus.
    Filter: Name == G5T4SETUP  AND  ManufData enthält 00RDY  AND  Service 94050000-…
    """
    found: BLEDevice | None = None
    stop = asyncio.Event()

    def callback(device: BLEDevice, adv: AdvertisementData) -> None:
        nonlocal found
        if found:
            return
        if device.name != _BLE_NAME_SETUP:
            return
        manuf_match = any(
            _MANUF_DATA_SETUP in payload
            for payload in adv.manufacturer_data.values()
        )
        if not manuf_match:
            return
        adv_svcs = [str(s).lower() for s in adv.service_uuids]
        if not any(_SVC_SETUP in s or s in _SVC_SETUP for s in adv_svcs):
            return
        print(f"[SETUP] found: {device.name}  {device.address}")
        found = device
        stop.set()

    async with BleakScanner(detection_callback=callback):
        print(f"[SETUP] scanning {_SCAN_DURATION}s for {_BLE_NAME_SETUP!r}…")
        try:
            await asyncio.wait_for(stop.wait(), timeout=_SCAN_DURATION)
        except asyncio.TimeoutError:
            print("[SETUP] scan timeout – no setup device found.")

    return found

async def run_setup(
    device_id: int,
    measurement_interval: int = 10,
    address: str | None = None,
) -> dict:
    """
    Vollständiger Setup-Workflow (§7):
      1. Gerät suchen (Scan) – oder direkt per Adresse verbinden falls angegeben
      2. DeviceSetupConfig schreiben
      3. Disconnect abwarten (Gerät startet neu – gewolltes Verhalten laut Spec)

    Gibt ein dict mit status + details zurück (für FastAPI-Response).
    """
    if address:
        target_address = address
        print(f"[SETUP] connecting directly to {target_address}…")
    else:
        device = await _scan_for_setup_device()
        if device is None:
            return {"status": "error", "reason": "no setup device found during scan"}
        target_address = device.address

    payload = _build_setup_config(measurement_interval, device_id)
    print(f"[SETUP] writing config: interval={measurement_interval}s "
          f"deviceId={device_id}  payload={payload.hex()}")

    try:
        async with BleakClient(target_address, timeout=20.0) as client:
            await client.write_gatt_char(SETUP_CONFIG_UUID, payload, response=True)
            print("[SETUP] config written. Device will reboot – connection will drop.")
            await asyncio.sleep(2)

        return {
            "status":               "ok",
            "address":              target_address,
            "device_id":            device_id,
            "measurement_interval": measurement_interval,
            "note":                 "Device is rebooting into normal mode."
        }

    except Exception as e:
        print(f"[SETUP] failed: {e}")
        return {"status": "error", "reason": str(e)}