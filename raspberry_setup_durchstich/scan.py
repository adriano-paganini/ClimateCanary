import asyncio
from bleak import BleakScanner

async def main():
    print("Suche nach BLE Geräten...")
    devices = await BleakScanner.discover()
    for d in devices:
        print(f"Gefunden: {d.name} | Adresse: {d.address}")

asyncio.run(main())