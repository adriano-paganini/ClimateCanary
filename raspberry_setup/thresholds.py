import asyncio
import aiosqlite
from dataclasses import dataclass
from typing import Optional

@dataclass
class Threshold:
    metric:      str
    upper_bound: Optional[float]
    lower_bound: Optional[float]

@dataclass
class MetricConfig:
    threshold: Threshold
    hint_text: Optional[str] = None


_lock:  asyncio.Lock | None     = None
_store: dict[str, MetricConfig] = {}


_DEFAULTS: dict[str, MetricConfig] = {
    "temperature": MetricConfig(
        threshold=Threshold("temperature", upper_bound=30.0,    lower_bound=16.0),
        hint_text="Bitte Fenster öffnen oder Heizung anpassen.",
    ),
    "humidity": MetricConfig(
        threshold=Threshold("humidity",    upper_bound=70.0,    lower_bound=30.0),
        hint_text="Bitte lüften oder Luftbefeuchter einsetzen.",
    ),
    "pressure": MetricConfig(
        threshold=Threshold("pressure",    upper_bound=1050.0,  lower_bound=950.0),
        hint_text="Ungewöhnlicher Luftdruck – bitte Fenster prüfen.",
    ),
    "air_quality": MetricConfig(
        threshold=Threshold("air_quality", upper_bound=50000.0, lower_bound=None),
        hint_text="Schlechte Luftqualität – bitte lüften.",
    ),
}


def _get_lock() -> asyncio.Lock:
    global _lock
    if _lock is None:
        _lock = asyncio.Lock()
    return _lock


def get_metric_config(metric: str) -> MetricConfig:
    return _store.get(metric, _DEFAULTS[metric])


def get_threshold(metric: str) -> Threshold:
    return get_metric_config(metric).threshold


def get_hint_text(metric: str) -> Optional[str]:
    return get_metric_config(metric).hint_text


async def update_thresholds(
    updates: list[dict],
    db: aiosqlite.Connection,
) -> None:
    """Called by app.py when the backend POSTs new threshold config."""
    async with _get_lock():
        for item in updates:
            metric      = item["metric"]
            upper_bound = float(item["upperBound"]) if item.get("upperBound") is not None else None
            lower_bound = float(item["lowerBound"]) if item.get("lowerBound") is not None else None
            hint_text   = item.get("hintText")

            _store[metric] = MetricConfig(
                threshold=Threshold(metric, upper_bound, lower_bound),
                hint_text=hint_text,
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
                (metric, upper_bound, lower_bound, hint_text),
            )

        await db.commit()
        print(f"[THRESH] updated {len(updates)} metric(s): "
              f"{[u['metric'] for u in updates]}")


async def remove_thresholds(
    metrics: list[str],
    db: aiosqlite.Connection,
) -> None:
    """
    Called by app.py when the backend POSTs /config/thresholds/remove.
    Removes from in-memory store and SQLite; falls back to defaults afterwards.
    """
    async with _get_lock():
        for metric in metrics:
            _store.pop(metric, None)
            await db.execute(
                "DELETE FROM thresholds WHERE metric = ?", (metric,)
            )

        await db.commit()
        print(f"[THRESH] removed {len(metrics)} metric(s): {metrics}")


async def load_thresholds_from_db(db: aiosqlite.Connection) -> None:
    """Load persisted thresholds + hints on startup (called once from main.py)."""
    async with db.execute(
        "SELECT metric, upper_bound, lower_bound, hint_text FROM thresholds"
    ) as cursor:
        rows = await cursor.fetchall()

    count = 0
    for metric, upper_bound, lower_bound, hint_text in rows:
        _store[metric] = MetricConfig(
            threshold=Threshold(metric, upper_bound, lower_bound),
            hint_text=hint_text,
        )
        count += 1

    if count:
        print(f"[THRESH] loaded {count} metric config(s) from DB.")
    else:
        print("[THRESH] no thresholds in DB – using built-in defaults.")