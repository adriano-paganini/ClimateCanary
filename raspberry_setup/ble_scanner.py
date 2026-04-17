from bleak import BleakScanner
from bleak.backends.device import BLEDevice
from bleak.backends.scanner import AdvertisementData

EXPECTED_SERVICE_UUID = "0000xxxx-0000-1000-8000-00805f9b34fb" #dont know it yet

async def scan_for_stations() -> list[str]:
    results = []

    def callback(device: BLEDevice, adv: AdvertisementData):
        service_uuids = [str(u).lower() for u in adv.service_uuids]
        if EXPECTED_SERVICE_UUID.lower() in service_uuids:
            print(f"[SCAN] found station: {device.address}")
            results.append(device.address)

    scanner = BleakScanner(detection_callback=callback)
    await scanner.start()
    await asyncio.sleep(10)
    await scanner.stop()

    if not results:
        print("[SCAN] WARN: no stations found")

    return results