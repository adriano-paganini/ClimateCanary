from fastapi import FastAPI
import aiohttp
import asyncio
import yaml
from pathlib import Path
from pydantic import BaseModel
from bleak import BleakClient

import config
from config import SPRING_BOOT_URL
from ble_scanner import scan_for_stations
from state import set_privacy_mode

app = FastAPI()

http_session = None

# geteilter Zustand mit main.py
stations_event: asyncio.Event = asyncio.Event()
selected_stations: list[dict] = []


class ConfigPayload(BaseModel):
    id: int
    room_id: int

class OccupancyPayload(BaseModel):
    id: int
    roomId: int
    privacy_mode: bool

class StationEntry(BaseModel):
    address:   str
    device_id: int
    room_name: str

class StationsPayload(BaseModel):
    stations: list[StationEntry]


@app.on_event("startup")
async def startup_event():
    global http_session
    http_session = aiohttp.ClientSession()
    print("[SYS] HTTP Client Session gestartet.")

@app.on_event("shutdown")
async def shutdown_event():
    global http_session
    if http_session:
        await http_session.close()
        print("[SYS] HTTP Client Session geschlossen.")


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


class SetupPayload(BaseModel):
    device_id:            int
    measurement_interval: int = 10

@app.post("/api/pi/setup/{address}")
async def setup_station_by_address(address: str, payload: SetupPayload):
    from setup_flow import run_setup
    result = await run_setup(
        device_id=payload.device_id,
        measurement_interval=payload.measurement_interval,
        address=address,
    )
    return result

@app.get("/api/scan")
async def scan():
    addresses = await scan_for_stations()
    return {"stations": addresses}

@app.post("/api/pi/{piId}/occupancy")
async def receive_occupancy(piId: int, payload: OccupancyPayload):
    if piId != config.PI_ID:
        return {"status": "error", "reason": "ID mismatch"}

    set_privacy_mode(payload.privacy_mode)
    config.PRIVACY_MODE = payload.privacy_mode

    print(f"[CFG] privacy_mode={payload.privacy_mode}")
    return {"status": "ok", "privacy_mode": config.PRIVACY_MODE}


@app.post("/api/pi/{piId}/stations")
async def receive_stations(piId: int, payload: StationsPayload):
    """
    Wird vom Backend aufgerufen sobald der User Stationen ausgewählt hat.
    Weckt main.py über stations_event auf.

    Body:
    {
      "stations": [
        { "address": "7C:DE:CE:44:CC:B1", "device_id": 123456, "room_name": "Lab1" }
      ]
    }
    """
    if piId != config.PI_ID:
        return {"status": "error", "reason": "ID mismatch"}

    global selected_stations
    selected_stations = [s.model_dump() for s in payload.stations]
    print(f"[APP] received {len(selected_stations)} station(s) from backend.")

    if stations_event:
        stations_event.set()

    return {"status": "ok", "count": len(selected_stations)}
