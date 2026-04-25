import asyncio
import aiohttp
import aiosqlite
import config
from config import API_URL, SEND_INTERVAL


async def http_sender(db: aiosqlite.Connection):
    async with aiohttp.ClientSession() as session:
        await send_booted(session)

        while True:
            await asyncio.sleep(SEND_INTERVAL)
            try:
                async with db.execute(
                    """SELECT id, timestamp, temperature, humidity, pressure, air_quality
                       FROM sensor_data WHERE sent = 0 ORDER BY id LIMIT 50"""
                ) as cursor:
                    rows = await cursor.fetchall()

                if not rows:
                    continue

                for row in rows:
                    row_id, ts, temp, hum, press, gas = row
                    payload = {
                        "timestamp":       ts,
                        "temperature":     temp,
                        "humidity":        hum,
                        "pressure":        press,
                        "airQuality":   gas,
                        "roomId":          config.ROOM_ID,
                        "sensorStationId": config.SENSOR_STATION_ID,
                    }
                    try:
                        async with session.post(
                            API_URL, json=payload,
                            timeout=aiohttp.ClientTimeout(total=5)
                        ) as resp:
                            if resp.status in (200, 201, 204):
                                await db.execute(
                                    "UPDATE sensor_data SET sent = 1 WHERE id = ?",
                                    (row_id,)
                                )
                                print(f"[HTTP] sent row {row_id} → {resp.status}")
                            else:
                                print(f"[HTTP] server returned {resp.status} for row {row_id}")
                    except Exception as e:
                        print(f"[HTTP] failed for row {row_id}: {e}")

                await db.commit()

            except Exception as e:
                print(f"[HTTP] sender error: {e}")


async def send_booted(session: aiohttp.ClientSession):
    try:
        async with session.post(
            f"{config.BACKEND_URL}/api/pi/{config.HOST_NAME}/booted",
            timeout=aiohttp.ClientTimeout(total=5)
        ) as resp:
            print(f"[HTTP] booted sent → {resp.status}")
    except Exception as e:
        print(f"[HTTP] booted failed: {e}")