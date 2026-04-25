import asyncio
from bleak import BleakScanner
from bleak.backends.device import BLEDevice
from bleak.backends.scanner import AdvertisementData

EXPECTED_NAME = "G5T4CC"
EXPECTED_MANUFACTURER_DATA = b"11LCK"
EXPECTED_SERVICE_UUID = "0000181a-0000-1000-8000-00805f9b34fb"

async def scan_for_stations() -> list[str]:
    results = []

    def callback(device: BLEDevice, adv: AdvertisementData):
        name_ok = (adv.local_name or "").upper() == EXPECTED_NAME
        svc_ok = EXPECTED_SERVICE_UUID in [str(u).lower() for u in adv.service_uuids]
        mfr_ok = any(
            v == EXPECTED_MANUFACTURER_DATA
            for v in adv.manufacturer_data.values()
        )
        if name_ok and svc_ok and mfr_ok:
            print(f"[SCAN] found station: {device.address}")
            results.append(device.address)

    scanner = BleakScanner(detection_callback=callback)
    await scanner.start()
    await asyncio.sleep(10)
    await scanner.stop()

    if not results:
        print("[SCAN] WARN: no stations found")

    return results