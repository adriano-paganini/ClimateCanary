import asyncio
import json
import logging
import aiosqlite

log = logging.getLogger(__name__)
from dataclasses import dataclass, field
from typing import Optional

@dataclass
class Threshold:
    metric:      str
    upper_bound: Optional[float]
    lower_bound: Optional[float]

@dataclass
class MetricConfig:
    threshold:  Threshold
    hint_texts: list[str] = field(default_factory=list)


_lock:  asyncio.Lock | None     = None
_store: dict[str, MetricConfig] = {}

_NO_CONFIG = MetricConfig(
    threshold=Threshold("", upper_bound=None, lower_bound=None),
    hint_texts=[],
)

def _get_lock() -> asyncio.Lock:
    global _lock
    if _lock is None:
        _lock = asyncio.Lock()
    return _lock


def get_metric_config(metric: str) -> MetricConfig:
    return _store.get(metric, _NO_CONFIG)


def get_threshold(metric: str) -> Threshold:
    return get_metric_config(metric).threshold


def get_hint_texts(metric: str) -> list[str]:
    return get_metric_config(metric).hint_texts


async def update_thresholds(
    updates: list[dict],
    db: aiosqlite.Connection,
) -> None:
    """Called by app.py when the backend POSTs new threshold config.
    Each item: metric, upperBound, lowerBound, hintTexts (list[str]).
    """
    async with _get_lock():
        for item in updates:
            metric      = item["metric"]
            upper_bound = item.get("upperBound")
            lower_bound = item.get("lowerBound")
            hint_texts  = item.get("hintTexts") or []

            _store[metric] = MetricConfig(
                threshold=Threshold(metric, upper_bound, lower_bound),
                hint_texts=hint_texts,
            )

            await db.execute(
                """
                INSERT INTO thresholds (metric, upper_bound, lower_bound, hint_text)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(metric) DO UPDATE SET
                    upper_bound = excluded.upper_bound,
                    lower_bound = excluded.lower_bound,
                    hint_text   = excluded.hint_text
                """,
                (metric, upper_bound, lower_bound,
                 json.dumps(hint_texts, ensure_ascii=False)),
            )

        await db.commit()
        log.info(f"[THRESH] updated {len(updates)} metric(s): "
                 f"{[u['metric'] for u in updates]}")


_METRIC_MAP: dict[str, str] = {
    "TEMPERATURE": "temperature",
    "HUMIDITY":    "humidity",
    "PRESSURE":    "pressure",
    "IAQ":         "air_quality",
}


async def remove_threshold_bounds(
    entries: list[dict],
    db: aiosqlite.Connection,
) -> None:
    """Called by app.py when the backend DELETEs /config/thresholds/remove.
    Each entry is a ThresholdDTO dict with 'metric' and 'thresholdType'.
    Nulls out the specified bound; removes the metric entirely if both become None.
    """
    async with _get_lock():
        for entry in entries:
            metric = _METRIC_MAP.get(entry.get("metric", ""))
            if not metric:
                continue
            bound_type = entry.get("thresholdType", "")

            current = _store.get(metric)
            if current is None:
                continue

            upper = current.threshold.upper_bound
            lower = current.threshold.lower_bound
            hints = current.hint_texts

            if bound_type == "UPPER":
                upper = None
            elif bound_type == "LOWER":
                lower = None

            if upper is None and lower is None:
                _store.pop(metric, None)
                await db.execute("DELETE FROM thresholds WHERE metric = ?", (metric,))
            else:
                _store[metric] = MetricConfig(
                    threshold=Threshold(metric, upper, lower),
                    hint_texts=hints,
                )
                await db.execute(
                    """
                    INSERT INTO thresholds (metric, upper_bound, lower_bound, hint_text)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT(metric) DO UPDATE SET
                        upper_bound = excluded.upper_bound,
                        lower_bound = excluded.lower_bound
                    """,
                    (metric, upper, lower,
                     json.dumps(hints, ensure_ascii=False)),
                )

        await db.commit()
        log.info(f"[THRESH] removed {len(entries)} bound(s)")


async def load_thresholds_from_db(db: aiosqlite.Connection) -> None:
    """Load persisted thresholds + hints on startup."""
    async with db.execute(
        "SELECT metric, upper_bound, lower_bound, hint_text FROM thresholds"
    ) as cursor:
        rows = await cursor.fetchall()

    count = 0
    for metric, upper_bound, lower_bound, hint_text_raw in rows:
        if hint_text_raw:
            try:
                hints = json.loads(hint_text_raw)
                if isinstance(hints, str):
                    hints = [hints]
            except (json.JSONDecodeError, TypeError):
                hints = [hint_text_raw]
        else:
            hints = []

        _store[metric] = MetricConfig(
            threshold=Threshold(metric, upper_bound, lower_bound),
            hint_texts=hints,
        )
        count += 1

    if count:
        log.info(f"[THRESH] loaded {count} metric config(s) from DB.")
    else:
        log.info("[THRESH] no thresholds in DB – using built-in defaults.")
