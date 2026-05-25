from fastapi import FastAPI, HTTPException, Request, BackgroundTasks
import asyncio
import aiosqlite
import logging
import re
from pathlib import Path
from pydantic import BaseModel
from typing import Optional

import config
from state import set_privacy_mode
from database import save_station

log = logging.getLogger(__name__)

app = FastAPI()

db_connection: aiosqlite.Connection | None = None
stations_event: asyncio.Event = asyncio.Event()

class SensorStationDTO(BaseModel):
    id:                  int
    bleMac:              str
    name:                str
    deviceStatus:        str
    measurementInterval: Optional[int] = None
    raspberryPiId:       Optional[int] = None
    roomId:              Optional[int] = None

class OccupancyDTO(BaseModel):
    roomName:    str
    privacyMode: bool

# Matches the Java record toString() key sent by the backend:
# ThresholdDTO[id=X, roomId=Y, metric=METRIC, boundValue=Z, thresholdType=TYPE, ...]
_THRESHOLD_KEY_RE = re.compile(
    r'metric=(\w+), boundValue=([^,]+), thresholdType=(\w+),.*?enabled=(\w+)\]'
)

_METRIC_MAP: dict[str, str] = {
    "TEMPERATURE": "temperature",
    "HUMIDITY":    "humidity",
    "PRESSURE":    "pressure",
    "IAQ":         "air_quality",
}

def _parse_threshold_key(key: str) -> dict | None:
    m = _THRESHOLD_KEY_RE.search(key)
    if not m:
        return None
    metric_raw, bound_str, bound_type, enabled_str = m.groups()
    try:
        bound_value = float(bound_str.strip())
    except ValueError:
        bound_value = None
    return {
        "metric":        metric_raw,
        "boundValue":    bound_value,
        "thresholdType": bound_type,
        "enabled":       enabled_str.strip().lower() == "true",
    }

class ViolationResolvedDTO(BaseModel):
    metric:   str
    roomId:   int
    endTime:  str            # ISO string e.g. "2026-05-04T14:30:00.123"
    status:   Optional[str] = None  # computed method on Java record, may not be serialised


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
    Path("conf.yml").write_text(yaml_text)
    log.info(f"[CFG] config reloaded from backend: pi_id={config.PI_ID}, room_id={config.ROOM_ID}")
    return {"status": "ok"}


@app.post("/api/spi/{piId}/occupancy")
async def receive_occupancy(piId: int, payload: OccupancyDTO):
    """Backend pushes current occupancy / privacy mode for this Pi's room."""
    _check_pi_id(piId)
    set_privacy_mode(payload.privacyMode)
    config.PRIVACY_MODE = payload.privacyMode
    try:
        p = Path("conf.yml")
        import yaml as _yaml
        data = _yaml.safe_load(p.read_text())
        data["pi"]["privacy_mode"] = payload.privacyMode
        p.write_text(_yaml.dump(data, allow_unicode=True))
    except Exception as e:
        log.warning(f"[CFG] could not persist privacy_mode to conf.yml: {e}")
    log.info(f"[CFG] privacy_mode={payload.privacyMode}, room={payload.roomName}")
    return {"status": "ok"}


@app.get("/api/spi/{piId}/heartbeat")
async def heartbeat(piId: int):
    """Backend checks if this Pi is alive."""
    _check_pi_id(piId)
    return {"status": "ok"}


@app.post("/api/spi/{piId}/scan")
async def trigger_scan(piId: int, background_tasks: BackgroundTasks):
    """
    Backend manually triggers a BLE scan for available sensor stations.
    Not needed in daily operations.
    """
    _check_pi_id(piId)
    from ble_scanner import scan_for_stations
    background_tasks.add_task(scan_for_stations)
    return {"status": "ok"}

@app.post("/api/spi/{piId}/setup")
async def setup_station(piId: int, payload: SensorStationDTO):
    _check_pi_id(piId)
    db = _check_db()
    from setup_flow import run_setup
    success = await run_setup(
        station=payload.model_dump(),
        measurement_interval=payload.measurementInterval or 60,
    )

    if not success:
        raise HTTPException(status_code=500, detail="Setup failed — check Pi logs")

    data = payload.model_dump()
    data["measurementInterval"] = data.get("measurementInterval") or 60
    await save_station(db, data)
    log.info(f"[APP] station saved after setup: id={payload.id}, mac={payload.bleMac}, name={payload.name!r}")
    stations_event.set()

    return {"status": "ok"}


@app.post("/api/spi/{piId}/stations")
async def receive_stations(piId: int, payload: SensorStationDTO):
    _check_pi_id(piId)
    db = _check_db()

    data = payload.model_dump()
    data["measurementInterval"] = data.get("measurementInterval") or 60
    await save_station(db, data)
    log.info(f"[APP] station received via /stations: id={payload.id}, mac={payload.bleMac}")
    stations_event.set()

    return {"status": "ok"}


@app.post("/api/spi/{piId}/config/thresholds")
async def receive_thresholds(piId: int, request: Request):
    """Backend pushes new threshold configuration.
    Body: Map<ThresholdDTO.toString(), List<ClimateHintDTO>> — one entry per bound direction.
    Multiple entries for the same metric (UPPER + LOWER) are merged.
    """
    _check_pi_id(piId)
    db = _check_db()

    body: dict = await request.json()

    metric_data: dict[str, dict] = {}
    for key_str, hint_list in body.items():
        entry = _parse_threshold_key(key_str)
        if entry is None or not entry["enabled"]:
            continue
        metric = _METRIC_MAP.get(entry["metric"])
        if metric is None:
            log.warning(f"[THRESH] unknown metric {entry['metric']!r}, skipping")
            continue

        if metric not in metric_data:
            metric_data[metric] = {"upperBound": None, "lowerBound": None, "hintTexts": []}

        if entry["thresholdType"] == "UPPER":
            metric_data[metric]["upperBound"] = entry["boundValue"]
        elif entry["thresholdType"] == "LOWER":
            metric_data[metric]["lowerBound"] = entry["boundValue"]

        for hint in hint_list:
            text = hint.get("hintText")
            if text and text not in metric_data[metric]["hintTexts"]:
                metric_data[metric]["hintTexts"].append(text)

    updates = [{"metric": m, **data} for m, data in metric_data.items()]

    from thresholds import update_thresholds
    await update_thresholds(updates, db)
    return {"status": "ok", "updated": len(updates)}


@app.delete("/api/spi/{piId}/config/thresholds/remove")
async def remove_thresholds(piId: int, request: Request):
    """Backend tells Pi to remove a specific threshold bound.
    Body: List<ThresholdDTO> — each entry has metric and thresholdType (UPPER|LOWER).
    """
    _check_pi_id(piId)
    db = _check_db()

    entries: list = await request.json()

    from thresholds import remove_threshold_bounds
    await remove_threshold_bounds(entries, db)
    return {"status": "ok", "removed": len(entries)}


@app.post("/api/spi/{piId}/violation/resolve")
async def resolve_violation(piId: int, payload: ViolationResolvedDTO):
    """Backend tells Pi to manually turn off the violation warning on the Arduino."""
    _check_pi_id(piId)

    db = _check_db()
    from violation_tracker import resolve_violation as do_resolve
    metric = _METRIC_MAP.get(payload.metric, payload.metric.lower())
    await do_resolve(metric, payload.roomId, db)
    log.info(f"[VIO] manual resolve: metric={payload.metric}, room={payload.roomId}, end={payload.endTime}")
    return {"status": "ok"}