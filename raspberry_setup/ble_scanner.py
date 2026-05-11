"""
ble_scanner.py
--------------
BLE scan for Arduinos in setup mode.
Called from app.py when POST /api/spi/{piId}/scan arrives.

Filters on setup-mode advertising data:
  Name:              G5T4SETUP
  Manufacturer Data: 00RDY
  Service UUID:      94050000-af44-4a64-b339-8b04d5565014
"""

import asyncio
import aiohttp
from bleak import BleakScanner
from bleak.backends.device import BLEDevice
from bleak.backends.scanner import AdvertisementData

import config
from config import (
    BLE_NAME_SETUP,
    MANUF_DATA_SETUP,
    SVC_SETUP,
    SCAN_DURATION,
)


def _manuf_matches(adv: AdvertisementData, expected: bytes) -> bool:
    return any(
        payload == expected or expected in payload
        for payload in adv.manufacturer_data.values()
    )


async def scan_for_stations() -> list[str]:
    found: dict[str, BLEDevice] = {}

    def callback(device: BLEDevice, adv: AdvertisementData) -> None:
        if device.address in found:
            return
        if device.name != BLE_NAME_SETUP:
            return
        #if not _manuf_matches(adv, MANUF_DATA_SETUP):
          #  return
        #adv_svcs = [str(s).lower() for s in adv.service_uuids]
        #if not any(SVC_SETUP in s or s in SVC_SETUP for s in adv_svcs):
        #    return
        print(f"[SCAN] found setup device: {device.name}  {device.address}")
        found[device.address] = device

    async with BleakScanner(detection_callback=callback):
        print(f"[SCAN] scanning {SCAN_DURATION}s for {BLE_NAME_SETUP!r} devices…")
        await asyncio.sleep(SCAN_DURATION)

    await asyncio.sleep(2)
    addresses = list(found.keys())

    if not addresses:
        print("[SCAN] WARN: no setup devices found.")
    else:
        print(f"[SCAN] found {len(addresses)} device(s): {addresses}")
        await _post_discovered(addresses)

    return addresses


async def _post_discovered(addresses: list[str]) -> None:
    """POST found addresses to /api/cpi/{piId}/discovered."""
    url = f"{config.BACKEND_URL}/api/cpi/{config.PI_ID}/discovered"
    try:
        async with aiohttp.ClientSession() as session:
            async with session.post(
                url, json=addresses,
                timeout=aiohttp.ClientTimeout(total=10),
            ) as resp:
                print(f"[SCAN] POST discovered → {resp.status}")
    except Exception as e:
        print(f"[SCAN] POST discovered failed: {e}")