from fastapi import FastAPI, HTTPException, Request
import asyncio
import aiosqlite
from pydantic import BaseModel
from typing import Optional

import config
from state import set_privacy_mode
from database import save_station, load_stations

app = FastAPI()

db_connection: aiosqlite.Connection | None = None
stations_event: asyncio.Event = asyncio.Event()

class SensorStationDTO(BaseModel):
    id:                  int
    bleMac:              str
    name:                str
    deviceStatus:        str
    measurementInterval: int
    raspberryPiId:       int
    roomId:              int

class OccupancyDTO(BaseModel):
    id:          int
    roomName:    str
    privacyMode: bool

class ThresholdItem(BaseModel):
    metric:     str
    upperBound: Optional[float] = None
    lowerBound: Optional[float] = None
    hintText:   Optional[str]   = None

class ThresholdConfigDTO(BaseModel):
    thresholds: list[ThresholdItem]

class ViolationResolvedDTO(BaseModel):
    metric:   str
    roomId:   int
    endTime:  int   #to do: change this to parsed string!
    status:   str   # "RESOLVED"


def _check_pi_id(piId: int) -> None:
    if piId != config.PI_ID:
        raise HTTPException(status_code=403, detail="piId mismatch")

def _check_db() -> aiosqlite.Connection:
    if db_connection is None:
        raise HTTPException(status_code=503, detail="DB not ready")
    return db_connection


@app.get("/api/spi/setup/verify/{piId}")
async def verify_pi(piId: int):
    """Backend verifies this Pi's ID exists in the system."""
    _check_pi_id(piId)
    return {"exists": True, "pi_id": piId}


@app.post("/api/spi/{piId}/config")
async def receive_config(piId: int, request: Request):
    """
    Backend sends updated config as a raw YAML string.
    Pi reloads its runtime config immediately.
    """
    _check_pi_id(piId)
    yaml_text = (await request.body()).decode("utf-8")
    config.load_config_from_string(yaml_text)
    set_privacy_mode(config.PRIVACY_MODE)
    print(f"[CFG] config reloaded from backend: pi_id={config.PI_ID}, room_id={config.ROOM_ID}")
    return {"status": "ok"}


@app.post("/api/spi/{piId}/occupancy")
async def receive_occupancy(piId: int, payload: OccupancyDTO):
    """Backend pushes current occupancy / privacy mode for this Pi's room."""
    _check_pi_id(piId)
    set_privacy_mode(payload.privacyMode)
    config.PRIVACY_MODE = payload.privacyMode
    print(f"[CFG] privacy_mode={payload.privacyMode}, room={payload.roomName}")
    return {"status": "ok"}


@app.get("/api/spi/{piId}/heartbeat")
async def heartbeat(piId: int):
    """Backend checks if this Pi is alive."""
    _check_pi_id(piId)
    return {"status": "ok"}


@app.post("/api/spi/{piId}/scan")
async def trigger_scan(piId: int):
    """
    Backend manually triggers a BLE scan for available sensor stations.
    Not needed in daily operations.
    """
    _check_pi_id(piId)
    from ble_scanner import scan_for_stations
    addresses = await scan_for_stations()
    return {"addresses": addresses}


@app.post("/api/spi/{piId}/stations")
async def receive_stations(piId: int, payload: SensorStationDTO):
    """
    Backend tells Pi which single station to connect to.
    Saved to DB immediately, wakes station_manager.
    """
    _check_pi_id(piId)
    db = _check_db()

    await save_station(db, payload.model_dump())
    print(f"[APP] station received: id={payload.id}, mac={payload.bleMac}, name={payload.name!r}")
    stations_event.set()

    return {"status": "ok"}



@app.post("/api/spi/{piId}/config/thresholds")
async def receive_thresholds(piId: int, body: ThresholdConfigDTO):
    """Backend pushes new threshold configuration."""
    _check_pi_id(piId)
    db = _check_db()

    from thresholds import update_thresholds
    updates = [item.model_dump() for item in body.thresholds]
    await update_thresholds(updates, db)
    return {"status": "ok", "updated": len(updates)}


@app.post("/api/spi/{piId}/config/thresholds/remove")
async def remove_thresholds(piId: int, body: ThresholdConfigDTO):
    """Backend tells Pi to delete existing thresholds."""
    _check_pi_id(piId)
    db = _check_db()

    from thresholds import remove_thresholds
    metrics = [item.metric for item in body.thresholds]
    await remove_thresholds(metrics, db)
    return {"status": "ok", "removed": len(metrics)}


@app.post("/api/spi/{piId}/violation/resolve")
async def resolve_violation(piId: int, payload: ViolationResolvedDTO):
    """Backend tells Pi to manually turn off the violation warning on the Arduino."""
    _check_pi_id(piId)

    from violation_tracker import resolve_violation as do_resolve
    await do_resolve(payload.metric, payload.roomId)
    print(f"[VIO] manual resolve: metric={payload.metric}, room={payload.roomId}, end={payload.endTime}")
    return {"status": "ok"}