from fastapi import FastAPI
import aiohttp
import asyncio
import uvicorn
from config import SPRING_BOOT_URL
from ble_scanner import scan_for_stations
from pydantic import BaseModel

app = FastAPI()

http_session = None

class ConfigPayload(BaseModel):
    id: int
    room_id: int

class OccupancyPayload(BaseModel):
    id: int
    roomId: int
    belowMinOccupancy: bool

@app.on_event("startup")
async def startup_event():
    """Wird aufgerufen, wenn FastAPI startet."""
    global http_session
    http_session = aiohttp.ClientSession()
    print("[SYS] HTTP Client Session gestartet.")

@app.on_event("shutdown")
async def shutdown_event():
    """Wird aufgerufen, wenn FastAPI beendet wird."""
    global http_session
    if http_session:
        await http_session.close()
        print("[SYS] HTTP Client Session geschlossen.")

# --- ENDPUNKTE ---

@app.get("/")
async def read_root():
    return {
        "message": "Raspberry Pi Gateway (FastAPI)",
        "status": "online",
        "info": "Nutzt aiohttp für asynchrone Requests"
    }

@app.get("/test")
async def read_test():
    return {
        "device": "Raspberry Pi",
        "tech": "FastAPI",
        "ble_ready": True
    }

@app.get("/data-from-pi")
async def give_string_to_backend():
    return "Hallo Backend, hier spricht dein Raspberry Pi!"

@app.get("/trigger-backend-call")
async def trigger_external_call():
    """Sendet einen Status-Check an das Spring Boot Backend."""
    global http_session

    if http_session is None:
        return {"status": "fehler", "error": "HTTP Session nicht initialisiert"}

    try:
        payload = {"message": "Aktueller Status vom Pi: Betriebsbereit"}

        async with http_session.post(
            SPRING_BOOT_URL,
            json=payload,
            timeout=aiohttp.ClientTimeout(total=5)
        ) as response:

            antwort_text = await response.text()

            return {
                "status": "erfolgreich",
                "http_code": response.status,
                "backend_antwort": antwort_text
            }

    except Exception as e:
        return {"status": "fehler", "error": str(e)}


@app.post("/api/pi/{piId}/config")
async def receive_config(piId: int, payload: ConfigPayload):
    if Path("conf.yml").exists() and piId != config.PI_ID:
            return {"status": "error", "reason": "ID mismatch"}

    cfg = {
            "pi": {
                "id": payload.id,
                "room_id": payload.room_id
            }
        }

    Path("conf.yml").write_text(yaml.dump(cfg))
    config.load_config("conf.yml")

    print(f"[CFG] config written and reloaded: pi_id={config.PI_ID}, room_id={config.ROOM_ID}")
    return {"status": "ok", "pi_id": config.PI_ID}

@app.get("/api/setup/verify/{piId}")
async def verify_pi(piId: int):
    exists = piId == config.PI_ID
    return {"exists": exists, "pi_id": piId}

@app.get("/api/scan")
async def scan():
    addresses = await scan_for_stations()
    return {"stations": addresses}

@app.post("/api/pi/connect/{address}")
async def connect_station(address: str):
    try:
        async with BleakClient(address, timeout=20.0) as client:
            pi_id_bytes = config.PI_ID.to_bytes(8, byteorder='little')
            await client.write_gatt_char(TRUSTED_RPI_UUID, pi_id_bytes)
            print(f"[BLE] wrote TrustedRpiId={config.PI_ID} to {address}")

        async with aiohttp.ClientSession() as session:
            payload = {
                "bluetoothAddress": address,
                "piId":             config.PI_ID,
                "roomId":           config.ROOM_ID,
            }
            async with session.post(
                f"{SPRING_BOOT_URL}/sensorstation/register",
                json=payload,
                timeout=aiohttp.ClientTimeout(total=5)
            ) as resp:
                data = await resp.json()
                config.SENSOR_STATION_ID = data["id"]
                config.DEVICE_ADDR = address
                print(f"[HTTP] station registered: id={config.SENSOR_STATION_ID}")
                print(f"[HTTP] backend notified → {resp.status}")

        return {"status": "ok", "address": address}

    except Exception as e:
        print(f"[BLE] connect failed for {address}: {e}")
        return {"status": "error", "reason": str(e)}


@app.post("/api/pi/{piId}/occupancy")
async def receive_occupancy(piId: int, payload: OccupancyPayload):
    if piId != config.PI_ID:
        return {"status": "error", "reason": "ID mismatch"}

    config.BELOW_MIN_OCCUPANCY = payload.belowMinOccupancy

    await db.execute(
        "INSERT OR REPLACE INTO pi_state (key, value) VALUES ('below_min_occupancy', ?)",
        ("1" if payload.belowMinOccupancy else "0",)
    )
    await db.commit()

    return {"status": "ok", "below_min_occupancy": config.BELOW_MIN_OCCUPANCY}
