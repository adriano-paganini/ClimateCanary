"""Core unit tests for the Raspberry Pi BLE gateway."""
import json
from datetime import datetime, timedelta

import pytest
import aiosqlite


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

@pytest.fixture(autouse=True)
def _reset_stores():
    """Isolate threshold _store and violation _states between tests."""
    import thresholds
    import violation_tracker
    from state import set_privacy_mode

    orig_store  = dict(thresholds._store)
    orig_states = dict(violation_tracker._states)
    set_privacy_mode(False)
    yield
    thresholds._store.clear()
    thresholds._store.update(orig_store)
    violation_tracker._states.clear()
    violation_tracker._states.update(orig_states)


async def _make_db() -> aiosqlite.Connection:
    db = await aiosqlite.connect(":memory:")
    await db.executescript("""
        CREATE TABLE IF NOT EXISTS thresholds (
            metric      TEXT PRIMARY KEY,
            upper_bound REAL,
            lower_bound REAL,
            hint_text   TEXT
        );
        CREATE TABLE IF NOT EXISTS threshold_violations (
            id                INTEGER PRIMARY KEY AUTOINCREMENT,
            sensor_station_id INTEGER NOT NULL,
            metric            TEXT    NOT NULL,
            room_id           INTEGER NOT NULL,
            status            TEXT    NOT NULL DEFAULT 'ACTIVE',
            start_time        TEXT    NOT NULL,
            end_time          TEXT,
            value_at_trigger  REAL    NOT NULL
        );
    """)
    await db.commit()
    return db


class _MockResp:
    def __init__(self, status=201, data=None):
        self.status = status
        self._data  = data or {}

    async def json(self):           return self._data
    async def text(self):           return ""
    async def __aenter__(self):     return self
    async def __aexit__(self, *_):  pass


class _MockSession:
    def post(self,  *a, **kw): return _MockResp(201, {"id": 42})
    def patch(self, *a, **kw): return _MockResp(200, {})


# ---------------------------------------------------------------------------
# 1.  _parse_threshold_key
# ---------------------------------------------------------------------------

class TestParseThresholdKey:
    def test_valid_upper_enabled(self):
        from app import _parse_threshold_key
        key = (
            "ThresholdDTO[id=1, roomId=1, metric=HUMIDITY, "
            "boundValue=60.0, thresholdType=UPPER, climateHintIds=[1], enabled=true]"
        )
        r = _parse_threshold_key(key)
        assert r is not None
        assert r["metric"]        == "HUMIDITY"
        assert r["boundValue"]    == 60.0
        assert r["thresholdType"] == "UPPER"
        assert r["enabled"]       is True

    def test_valid_lower_disabled(self):
        from app import _parse_threshold_key
        key = (
            "ThresholdDTO[id=2, roomId=1, metric=TEMPERATURE, "
            "boundValue=18.0, thresholdType=LOWER, climateHintIds=[], enabled=false]"
        )
        r = _parse_threshold_key(key)
        assert r is not None
        assert r["thresholdType"] == "LOWER"
        assert r["enabled"]       is False

    def test_iaq_metric(self):
        from app import _parse_threshold_key
        key = (
            "ThresholdDTO[id=3, roomId=2, metric=IAQ, "
            "boundValue=50000.0, thresholdType=UPPER, climateHintIds=[5], enabled=true]"
        )
        r = _parse_threshold_key(key)
        assert r is not None
        assert r["metric"]     == "IAQ"
        assert r["boundValue"] == 50000.0

    def test_malformed_returns_none(self):
        from app import _parse_threshold_key
        assert _parse_threshold_key("not a threshold key") is None
        assert _parse_threshold_key("")                    is None


# ---------------------------------------------------------------------------
# 2.  _classify
# ---------------------------------------------------------------------------

class TestClassify:
    def _setup_temp(self, upper=30.0, lower=16.0):
        import thresholds
        thresholds._store["temperature"] = thresholds.MetricConfig(
            threshold=thresholds.Threshold("temperature", upper, lower),
            hint_texts=[],
        )

    def test_above_upper(self):
        from violation_tracker import _classify
        self._setup_temp()
        assert _classify(35.0, "temperature") == 4

    def test_near_upper(self):
        from violation_tracker import _classify
        self._setup_temp()
        # 29.5 > 30 * 0.95 = 28.5 but < 30 → 2
        assert _classify(29.5, "temperature") == 2

    def test_below_lower(self):
        from violation_tracker import _classify
        self._setup_temp()
        assert _classify(14.0, "temperature") == 3

    def test_near_lower(self):
        from violation_tracker import _classify
        self._setup_temp()
        # 16.5 > 16 but < 16 * 1.05 = 16.8 → 1
        assert _classify(16.5, "temperature") == 1

    def test_normal_range(self):
        from violation_tracker import _classify
        self._setup_temp()
        assert _classify(22.0, "temperature") == 5

    def test_no_bounds(self):
        from violation_tracker import _classify
        import thresholds
        thresholds._store["temperature"] = thresholds.MetricConfig(
            threshold=thresholds.Threshold("temperature", None, None),
            hint_texts=[],
        )
        assert _classify(999.0, "temperature") == 0


# ---------------------------------------------------------------------------
# 3.  Threshold update (UPPER + LOWER merge) with DB persistence
# ---------------------------------------------------------------------------

class TestThresholdUpdate:
    @pytest.mark.asyncio
    async def test_bounds_and_hints_persisted(self):
        from thresholds import update_thresholds, get_metric_config

        db = await _make_db()
        try:
            await update_thresholds(
                [{"metric": "temperature", "upperBound": 28.0, "lowerBound": 18.0,
                  "hintTexts": ["Zu warm", "Fenster öffnen"]}],
                db,
            )

            cfg = get_metric_config("temperature")
            assert cfg.threshold.upper_bound == 28.0
            assert cfg.threshold.lower_bound == 18.0
            assert "Zu warm"        in cfg.hint_texts
            assert "Fenster öffnen" in cfg.hint_texts

            async with db.execute(
                "SELECT upper_bound, lower_bound, hint_text "
                "FROM thresholds WHERE metric='temperature'"
            ) as cur:
                row = await cur.fetchone()
            assert row is not None
            assert row[0] == 28.0
            assert row[1] == 18.0
            hints = json.loads(row[2])
            assert "Zu warm" in hints
        finally:
            await db.close()

    @pytest.mark.asyncio
    async def test_reload_from_db_restores_store(self):
        from thresholds import update_thresholds, load_thresholds_from_db, get_metric_config
        import thresholds

        db = await _make_db()
        try:
            await update_thresholds(
                [{"metric": "humidity", "upperBound": 65.0, "lowerBound": 35.0,
                  "hintTexts": ["Lüften bitte"]}],
                db,
            )
            thresholds._store.clear()          # simulate fresh process start
            await load_thresholds_from_db(db)

            cfg = get_metric_config("humidity")
            assert cfg.threshold.upper_bound == 65.0
            assert cfg.hint_texts            == ["Lüften bitte"]
        finally:
            await db.close()


# ---------------------------------------------------------------------------
# 4.  Violation state machine
# ---------------------------------------------------------------------------

class TestViolationStateMachine:
    def _pkt(self, temp, ts):
        return {
            "sensor_station_id": 1,
            "room_id":           1,
            "timestamp":         ts.isoformat(),
            "temperature":       temp,
            "humidity":          50.0,
            "pressure":          1013.0,
            "air_quality":       100.0,
        }

    @pytest.mark.asyncio
    async def test_open_then_resolve(self):
        import thresholds
        import violation_tracker

        thresholds._store["temperature"] = thresholds.MetricConfig(
            threshold=thresholds.Threshold("temperature", upper_bound=30.0, lower_bound=16.0),
            hint_texts=["Zu warm!"],
        )

        db      = await _make_db()
        session = _MockSession()
        tag     = "AA:BB:CC:DD:EE:FF"
        t0      = datetime(2026, 5, 23, 12, 0, 0)
        t1      = t0 + timedelta(seconds=30)

        try:
            # First packet: temp=35 → single-sample avg=35 → breaching → violation opens
            _, hints = await violation_tracker.process_measurement(
                self._pkt(35.0, t0), db, session, tag,
            )
            state = violation_tracker._states[(tag, "temperature")]
            assert state.violation_open      is True
            assert state.local_violation_id  is not None
            assert "Zu warm!" in hints

            # Second packet: temp=20 → avg=(35+20)/2=27.5 → not breaching → violation resolves
            _, hints2 = await violation_tracker.process_measurement(
                self._pkt(20.0, t1), db, session, tag,
            )
            state = violation_tracker._states[(tag, "temperature")]
            assert state.violation_open is False
            assert hints2 == []
        finally:
            await db.close()

    @pytest.mark.asyncio
    async def test_no_violation_in_normal_range(self):
        import violation_tracker

        db      = await _make_db()
        session = _MockSession()
        tag     = "11:22:33:44:55:66"
        t0      = datetime(2026, 5, 23, 12, 0, 0)

        try:
            _, hints = await violation_tracker.process_measurement(
                self._pkt(22.0, t0), db, session, tag,
            )
            state = violation_tracker._states.get((tag, "temperature"))
            assert state is None or not state.violation_open
            assert hints == []
        finally:
            await db.close()


# ---------------------------------------------------------------------------
# 5.  _make_iso timestamp
# ---------------------------------------------------------------------------

class TestMakeIso:
    def test_anchor_plus_delta(self):
        from ble_worker import _make_iso
        # anchor: pi_time=1000.0s, arduino_ms=5000
        # pkt_ms=7000 → delta=2000ms=2s → unix=1002.0
        result   = _make_iso(1000.0, 5000, 7000)
        expected = datetime.fromtimestamp(1002.0).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3]
        assert result == expected

    def test_millisecond_precision(self):
        from ble_worker import _make_iso
        result   = _make_iso(1000.0, 0, 500)
        expected = datetime.fromtimestamp(1000.5).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3]
        assert result == expected

    def test_no_delta(self):
        from ble_worker import _make_iso
        result   = _make_iso(1000.0, 100, 100)
        expected = datetime.fromtimestamp(1000.0).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3]
        assert result == expected
