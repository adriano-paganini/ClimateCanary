import asyncio
import logging
import aiohttp
import aiosqlite

import config
from config import SEND_INTERVAL

log = logging.getLogger(__name__)

async def http_sender(db: aiosqlite.Connection) -> None:
    async with aiohttp.ClientSession() as session:
        while True:
            await asyncio.sleep(SEND_INTERVAL)
            try:
                async with db.execute(
                    """SELECT id, timestamp, temperature, humidity,
                              pressure, air_quality, sensor_station_id, room_id
                       FROM sensor_data WHERE sent = 0 ORDER BY id LIMIT 50"""
                ) as cursor:
                    rows = await cursor.fetchall()

                for row in rows:
                    row_id, timestamp, temp, hum, press, gas, station_id, room_id = row
                    payload = {
                        "timestamp":       timestamp,
                        "temperature":     temp,
                        "humidity":        hum,
                        "pressure":        press,
                        "airQuality":      gas,
                        "sensorStationId": station_id,
                        "roomId":          room_id,
                    }
                    url = f"{config.BACKEND_URL}/api/cpi/{config.PI_ID}/measurements"
                    try:
                        async with session.post(
                            url, json=payload,
                            timeout=aiohttp.ClientTimeout(total=5),
                        ) as resp:
                            if resp.status in (200, 201, 204):
                                await db.execute(
                                    "UPDATE sensor_data SET sent = 1 WHERE id = ?",
                                    (row_id,)
                                )
                                log.info(f"[HTTP] sent row {row_id} → {resp.status}")
                            else:
                                body = await resp.text()
                                log.warning(f"[HTTP] server returned {resp.status} for row {row_id}: {body}")
                    except Exception as e:
                        log.warning(f"[HTTP] failed for row {row_id}: {e}")

                await db.commit()

            except Exception as e:
                log.error(f"[HTTP] sender error: {e}")
