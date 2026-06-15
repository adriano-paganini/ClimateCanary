import asyncio
import logging
import struct
import aiohttp
from bleak import BleakClient

import config
from config import SETUP_CONFIG_UUID

log = logging.getLogger(__name__)

SETUP_CONNECT_ATTEMPTS = 3
SETUP_CONNECT_TIMEOUT_SECONDS = 60.0
SETUP_RETRY_DELAY_SECONDS = 2.0


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
            log.info(f"[SETUP:{tag}] PATCH status={status} → {resp.status}")
    except Exception as e:
        log.warning(f"[SETUP:{tag}] PATCH failed: {e}")


async def run_setup(
    station: dict,
    measurement_interval: int,
) -> bool:
    address = station["bleMac"]
    sensor_station_id = station["id"]
    tag = address

    payload = _build_setup_config(measurement_interval, config.PI_ID)
    log.info(
        f"[SETUP:{tag}] writing config: interval={measurement_interval}s "
        f"pi_id={config.PI_ID} station_id={sensor_station_id} payload={payload.hex()}"
    )
    log.info(f"[SETUP:{tag}] full station payload before setup: {station}")

    try:
        from ble_scanner import _scan_lock
        log.info(f"[SETUP:{tag}] waiting for scanner lock before connecting")
        async with _scan_lock:
            log.info(f"[SETUP:{tag}] scanner lock acquired")
        log.info(
            f"[SETUP:{tag}] scanner lock free; trying setup connect "
            f"{SETUP_CONNECT_ATTEMPTS} time(s), timeout={SETUP_CONNECT_TIMEOUT_SECONDS:.0f}s"
        )

        last_error = None

        for attempt in range(1, SETUP_CONNECT_ATTEMPTS + 1):
            setup_client = None
            try:
                log.info(
                    f"[SETUP:{tag}] connecting to setup address "
                    f"(attempt {attempt}/{SETUP_CONNECT_ATTEMPTS})"
                )
                setup_client = BleakClient(address, timeout=SETUP_CONNECT_TIMEOUT_SECONDS)
                await setup_client.connect()
                log.info(f"[SETUP:{tag}] connected to setup address")

                log.info(f"[SETUP:{tag}] writing characteristic {SETUP_CONFIG_UUID}")
                await setup_client.write_gatt_char(SETUP_CONFIG_UUID, payload, response=True)
                log.info(f"[SETUP:{tag}] config written. Disconnecting before Arduino reboot.")

                try:
                    if setup_client.is_connected:
                        await setup_client.disconnect()
                    log.info(f"[SETUP:{tag}] setup BLE connection closed after config write")
                except Exception as disconnect_error:
                    log.warning(
                        f"[SETUP:{tag}] setup disconnect after successful write failed; "
                        f"treating setup as successful because write was accepted: "
                        f"{type(disconnect_error).__name__}: {disconnect_error!r}"
                    )

                await asyncio.sleep(3.0)
                return True
            except Exception as e:
                last_error = e
                log.warning(
                    f"[SETUP:{tag}] connect/write attempt {attempt}/{SETUP_CONNECT_ATTEMPTS} failed: "
                    f"{type(e).__name__}: {e!r}"
                )
                if attempt < SETUP_CONNECT_ATTEMPTS:
                    await asyncio.sleep(SETUP_RETRY_DELAY_SECONDS)
            finally:
                try:
                    if setup_client and setup_client.is_connected:
                        await setup_client.disconnect()
                        log.info(f"[SETUP:{tag}] disconnected setup client after failed attempt")
                except Exception as disconnect_error:
                    log.warning(
                        f"[SETUP:{tag}] setup disconnect cleanup failed: "
                        f"{type(disconnect_error).__name__}: {disconnect_error!r}"
                    )

        log.error(
            f"[SETUP:{tag}] setup failed after {SETUP_CONNECT_ATTEMPTS} attempt(s); "
            f"last_error={type(last_error).__name__}: {last_error!r}"
        )
        return False

    except Exception as e:
        log.exception(f"[SETUP:{tag}] connection failed: {type(e).__name__}: {e}")
        async with aiohttp.ClientSession() as session:
            await patch_station_status(session, sensor_station_id, "CONNECTION_FAILED", tag)
        return False
