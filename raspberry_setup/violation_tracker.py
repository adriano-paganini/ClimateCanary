"""
violation_tracker.py
--------------------
UC-06: Grenzwertwarnung – Erkennung, Alerting und Deaktivierung

Sliding-window rule:
    A violation is confirmed only when the rolling average of all samples
    within the last VIOLATION_WINDOW_SECONDS exceeds a threshold bound for
    the entire window duration.  Transient spikes are smoothed out.

Per-metric state is keyed by (ble_address, metric) so each sensor station
tracks its metrics independently.

Violation lifecycle:
    CONFIRMED  → stored in local SQLite + POSTed to backend
    RESOLVED   → local row updated + PATCHed to backend

Alert-flooding prevention:
    Once a violation is active, no new POST is sent for 15 minutes
    (ALERT_COOLDOWN_SECONDS). After the cooldown a reminder can be sent.
"""

import time
from datetime import datetime, timedelta
from collections import deque
from dataclasses import dataclass, field
from typing import Optional

import aiohttp
import aiosqlite

import config
from thresholds import get_threshold, get_hint_text

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

VIOLATION_WINDOW_SECONDS: int = 300     # 5-minute sliding window
ALERT_COOLDOWN_SECONDS:   int = 900     # 15-minute re-alert cooldown

ALL_METRICS = ("temperature", "humidity", "pressure", "air_quality")

# ---------------------------------------------------------------------------
# Per-metric runtime state
# ---------------------------------------------------------------------------

@dataclass
class _MetricState:
    samples:              deque         = field(default_factory=deque)
    violation_open:       bool          = False
    local_violation_id:   Optional[int] = None
    remote_violation_id:  Optional[int] = None
    last_alert_time:      float         = 0.0   # wall-clock time of last POST


# Keyed by (ble_address, metric)
_states: dict[tuple[str, str], _MetricState] = {}


def _get_state(address: str, metric: str) -> _MetricState:
    key = (address, metric)
    if key not in _states:
        _states[key] = _MetricState()
    return _states[key]


# ---------------------------------------------------------------------------
# Classification
# ---------------------------------------------------------------------------

def _classify(value: float, metric: str) -> int:
    """
    Returns a BLE status code per metric:
        0  = no threshold configured
        1  = below lower warn band  (lower_bound * 1.05)
        2  = above upper warn band  (upper_bound * 0.95)
        3  = below lower_bound      (critical low)
        4  = above upper_bound      (critical high)
        5  = fully within bounds    (all-good)
    """
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


# ---------------------------------------------------------------------------
# Main entry point called from ble_worker
# ---------------------------------------------------------------------------

async def process_measurement(
    pkt: dict,
    db: aiosqlite.Connection,
    session: aiohttp.ClientSession,
    ble_tag: str,
) -> tuple[dict[str, int], list[str]]:
    """
    Evaluate one measurement packet against all metric thresholds.

    Returns:
        statuses      – {metric: int}  BLE status codes for the Arduino
        hint_messages – [str]          ClimateHint texts for newly confirmed violations
    """
    # Parse ISO string into datetime for sliding window comparisons
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
        state = _get_state(ble_tag, metric)

        # --- Update sliding window ---
        state.samples.append((now, value))
        cutoff = now - timedelta(seconds=VIOLATION_WINDOW_SECONDS)
        while state.samples and state.samples[0][0] < cutoff:
            state.samples.popleft()

        avg        = sum(v for _, v in state.samples) / len(state.samples)
        avg_status = _classify(avg, metric)
        raw_status = _classify(value, metric)

        is_breaching = _is_breaching(avg_status)
        now_wall     = time.time()

        # --- Violation opened ---
        if is_breaching and not state.violation_open:
            state.violation_open  = True
            state.last_alert_time = now_wall

            vid = await _open_violation(db, sensor_station_id, metric, room_id, now, avg)
            state.local_violation_id = vid

            remote_id = await _post_violation(
                session, metric, room_id, now, avg, ble_tag,
            )
            state.remote_violation_id = remote_id

            hint = get_hint_text(metric)
            if hint:
                hint_messages.append(hint)

            print(f"[VIOL:{ble_tag}] CONFIRMED {metric} "
                  f"(avg={avg:.2f}, localId={vid})")

        # --- Violation still active: re-alert after cooldown ---
        elif is_breaching and state.violation_open:
            if now_wall - state.last_alert_time >= ALERT_COOLDOWN_SECONDS:
                state.last_alert_time = now_wall
                await _post_violation(session, metric, room_id, now, avg, ble_tag)
                print(f"[VIOL:{ble_tag}] REMINDER {metric} (avg={avg:.2f})")

        # --- Violation resolved ---
        elif not is_breaching and state.violation_open:
            state.violation_open = False
            raw_status = 5

            if state.local_violation_id is not None:
                await _resolve_violation(db, state.local_violation_id, now)

            await _patch_violation(
                session, metric, room_id,
                state.remote_violation_id,
                now, ble_tag,
            )

            state.local_violation_id  = None
            state.remote_violation_id = None
            print(f"[VIOL:{ble_tag}] RESOLVED {metric} (avg={avg:.2f})")

        statuses[metric] = raw_status

    return statuses, hint_messages


# ---------------------------------------------------------------------------
# SQLite helpers
# ---------------------------------------------------------------------------

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
        (sensor_station_id, metric, room_id, start_time.timestamp(), avg_value),
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
        (end_time.timestamp(), violation_id),
    )
    await db.commit()


# ---------------------------------------------------------------------------
# Backend HTTP helpers
# ---------------------------------------------------------------------------

async def _post_violation(
    session: aiohttp.ClientSession,
    metric: str,
    room_id: int,
    start_time: datetime,
    avg_value: float,
    tag: str,
) -> Optional[int]:
    """POST new/reminder violation to backend. Returns backend-assigned ID if provided."""
    url = f"{config.BACKEND_URL}/api/cpi/{config.PI_ID}/violation"
    payload = {
        "metric":    metric,
        "roomId":    room_id,
        "startTime": int(start_time.timestamp() * 1000),  # epoch ms for Java Long
        "avgValue":  avg_value,
        "status":    "ACTIVE",
    }
    try:
        async with session.post(
            url, json=payload, timeout=aiohttp.ClientTimeout(total=5),
        ) as resp:
            print(f"[VIOL:{tag}] POST violation/{metric} → {resp.status}")
            if resp.status in (200, 201):
                data = await resp.json()
                return data.get("id") or data.get("thresholdViolationId")
    except Exception as e:
        print(f"[VIOL:{tag}] POST violation failed: {e}")
    return None


async def _patch_violation(
    session: aiohttp.ClientSession,
    metric: str,
    room_id: int,
    remote_id: Optional[int],
    end_time: datetime,
    tag: str,
) -> None:
    """PATCH the resolved violation on the backend."""
    url = f"{config.BACKEND_URL}/api/cpi/{config.PI_ID}/violation/resolve"
    payload = {
        "metric":  metric,
        "roomId":  room_id,
        "endTime": int(end_time.timestamp() * 1000),  # epoch ms for Java Long
        "status":  "RESOLVED",
    }
    try:
        async with session.patch(
            url, json=payload, timeout=aiohttp.ClientTimeout(total=5),
        ) as resp:
            print(f"[VIOL:{tag}] PATCH resolve/{metric} → {resp.status}")
    except Exception as e:
        print(f"[VIOL:{tag}] PATCH resolve failed: {e}")


# ---------------------------------------------------------------------------
# Startup helper
# ---------------------------------------------------------------------------

async def load_window_seconds(db: aiosqlite.Connection) -> None:
    """
    Called from main.py on startup.
    Currently a no-op — VIOLATION_WINDOW_SECONDS is a compile-time constant.
    Reserved for future dynamic configuration from the backend.
    """
    print(f"[VIOL] window={VIOLATION_WINDOW_SECONDS}s, cooldown={ALERT_COOLDOWN_SECONDS}s")