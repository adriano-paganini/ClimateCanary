import logging
import time
from datetime import datetime, timedelta
from collections import deque
from dataclasses import dataclass, field
from typing import Optional

import aiohttp
import aiosqlite

import config
from state import get_privacy_mode
from thresholds import get_threshold, get_hint_texts

_TO_BACKEND_METRIC: dict[str, str] = {
    "temperature": "TEMPERATURE",
    "humidity":    "HUMIDITY",
    "pressure":    "PRESSURE",
    "air_quality": "IAQ",
}

log = logging.getLogger(__name__)

def _to_iso(dt: datetime) -> str:
    return dt.strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3]


VIOLATION_WINDOW_SECONDS: int = 300
ALERT_COOLDOWN_SECONDS:   int = 900

ALL_METRICS = ("temperature", "humidity", "pressure", "air_quality")


@dataclass
class _MetricState:
    samples:              deque         = field(default_factory=deque)
    violation_open:       bool          = False
    local_violation_id:   Optional[int] = None
    remote_violation_id:  Optional[int] = None
    last_alert_time:      float         = 0.0
    room_id:              int           = 0
    resolve_pending:      bool          = False

_states: dict[tuple[str, str], _MetricState] = {}

def _get_state(address: str, metric: str, room_id: int = 0) -> _MetricState:
    key = (address, metric)
    if key not in _states:
        _states[key] = _MetricState(room_id=room_id)
    elif room_id:
        _states[key].room_id = room_id
    return _states[key]

def _classify(value: float, metric: str) -> int:
    thresh = get_threshold(metric)

    if thresh.lower_bound is None and thresh.upper_bound is None:
        return 0

    if thresh.lower_bound is not None:
        if value < thresh.lower_bound:
            return 3
        if value < thresh.lower_bound * 1.05:
            return 1

    if thresh.upper_bound is not None:
        if value > thresh.upper_bound:
            return 4
        if value > thresh.upper_bound * 0.95:
            return 2

    return 5


def _is_breaching(status: int) -> bool:
    return status in {3, 4}

async def process_measurement(
    pkt: dict,
    db: aiosqlite.Connection,
    session: aiohttp.ClientSession,
    ble_tag: str,
) -> tuple[dict[str, int], list[str]]:
    now               = datetime.fromisoformat(pkt["timestamp"])
    sensor_station_id = pkt["sensor_station_id"]
    room_id           = pkt["room_id"]

    metric_values = {
        "temperature": pkt["temperature"],
        "humidity":    pkt["humidity"],
        "pressure":    pkt["pressure"],
        "air_quality": pkt["air_quality"],
    }

    statuses:      dict[str, int] = {}
    hint_messages: list[str]      = []

    for metric, value in metric_values.items():
        state = _get_state(ble_tag, metric, room_id)

        if state.resolve_pending:
            state.resolve_pending  = False
            state.violation_open   = False
            state.local_violation_id  = None
            state.remote_violation_id = None
            statuses[metric] = 5
            continue

        state.samples.append((now, value))
        cutoff = now - timedelta(seconds=VIOLATION_WINDOW_SECONDS)
        while state.samples and state.samples[0][0] < cutoff:
            state.samples.popleft()

        avg        = sum(v for _, v in state.samples) / len(state.samples)
        avg_status = _classify(avg, metric)
        raw_status = _classify(value, metric)

        is_breaching = _is_breaching(avg_status)
        now_wall     = time.time()

        if is_breaching and not state.violation_open:
            state.violation_open  = True
            state.last_alert_time = now_wall

            if not get_privacy_mode():
                vid = await _open_violation(db, sensor_station_id, metric, room_id, now, avg)
                state.local_violation_id = vid
                remote_id = await _post_violation(
                    session, metric, room_id, now, avg, ble_tag,
                )
                state.remote_violation_id = remote_id
                log.warning(f"[VIOL:{ble_tag}] CONFIRMED {metric} (avg={avg:.2f}, localId={vid})")
            else:
                log.warning(f"[VIOL:{ble_tag}] CONFIRMED {metric} (avg={avg:.2f}) [privacy — not reported]")

            hint_messages.extend(get_hint_texts(metric))

        elif is_breaching and state.violation_open:
            if not get_privacy_mode() and now_wall - state.last_alert_time >= ALERT_COOLDOWN_SECONDS:
                state.last_alert_time = now_wall
                await _post_violation(session, metric, room_id, now, avg, ble_tag)
                log.warning(f"[VIOL:{ble_tag}] REMINDER {metric} (avg={avg:.2f})")

        elif not is_breaching and state.violation_open:
            state.violation_open = False
            raw_status = 5

            if not get_privacy_mode():
                if state.local_violation_id is not None:
                    await _resolve_violation(db, state.local_violation_id, now)
                await _patch_violation(
                    session, metric, room_id,
                    state.remote_violation_id,
                    now, ble_tag,
                )

            state.local_violation_id  = None
            state.remote_violation_id = None
            log.info(f"[VIOL:{ble_tag}] RESOLVED {metric} (avg={avg:.2f})")

        statuses[metric] = raw_status

    return statuses, hint_messages

async def _open_violation(
    db: aiosqlite.Connection,
    sensor_station_id: int,
    metric: str,
    room_id: int,
    start_time: datetime,
    avg_value: float,
) -> int:
    cursor = await db.execute(
        """
        INSERT INTO threshold_violations
            (sensor_station_id, metric, room_id, status, start_time, value_at_trigger)
        VALUES (?, ?, ?, 'ACTIVE', ?, ?)
        """,
        (sensor_station_id, metric, room_id, _to_iso(start_time), avg_value),
    )
    await db.commit()
    return cursor.lastrowid


async def _resolve_violation(
    db: aiosqlite.Connection,
    violation_id: int,
    end_time: datetime,
) -> None:
    await db.execute(
        """
        UPDATE threshold_violations
        SET status = 'RESOLVED', end_time = ?
        WHERE id = ?
        """,
        (_to_iso(end_time), violation_id),
    )
    await db.commit()

async def _post_violation(
    session: aiohttp.ClientSession,
    metric: str,
    room_id: int,
    start_time: datetime,
    avg_value: float,
    tag: str,
) -> Optional[int]:
    url = f"{config.BACKEND_URL}/api/cpi/{config.PI_ID}/violation"
    payload = {
        "metric":    _TO_BACKEND_METRIC.get(metric, metric.upper()),
        "roomId":    room_id,
        "startTime": _to_iso(start_time),
        "avgValue":  avg_value,
        "status":    "ACTIVE",
    }
    try:
        async with session.post(
            url, json=payload, timeout=aiohttp.ClientTimeout(total=5),
        ) as resp:
            log.info(f"[VIOL:{tag}] POST violation/{metric} → {resp.status}")
            if resp.status in (200, 201):
                data = await resp.json()
                return data.get("id") or data.get("thresholdViolationId")
    except Exception as e:
        log.error(f"[VIOL:{tag}] POST violation failed: {e}")
    return None


async def _patch_violation(
    session: aiohttp.ClientSession,
    metric: str,
    room_id: int,
    remote_id: Optional[int],
    end_time: datetime,
    tag: str,
) -> None:
    url = f"{config.BACKEND_URL}/api/cpi/{config.PI_ID}/violation/resolve"
    payload = {
        "metric":  _TO_BACKEND_METRIC.get(metric, metric.upper()),
        "roomId":  room_id,
        "endTime": _to_iso(end_time),
        "status":  "RESOLVED",
    }
    try:
        async with session.patch(
            url, json=payload, timeout=aiohttp.ClientTimeout(total=5),
        ) as resp:
            log.info(f"[VIOL:{tag}] PATCH resolve/{metric} → {resp.status}")
    except Exception as e:
        log.error(f"[VIOL:{tag}] PATCH resolve failed: {e}")


async def resolve_violation(
    metric: str,
    room_id: int,
    db: aiosqlite.Connection,
) -> None:
    matched = False
    for (addr, m), state in _states.items():
        if m == metric and state.room_id == room_id and state.violation_open:
            state.resolve_pending = True
            if state.local_violation_id is not None:
                await _resolve_violation(db, state.local_violation_id, datetime.now())
                state.local_violation_id  = None
                state.remote_violation_id = None
            matched = True
            log.info(f"[VIOL:{addr}] manual RESOLVE {metric} room={room_id}")
    if not matched:
        log.warning(f"[VIOL] manual resolve: no active violation for {metric} room={room_id}")


async def load_window_seconds() -> None:
    log.info(f"[VIOL] window={VIOLATION_WINDOW_SECONDS}s, cooldown={ALERT_COOLDOWN_SECONDS}s")
