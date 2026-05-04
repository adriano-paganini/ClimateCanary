"""
state.py
--------
Global runtime state for the Raspberry Pi process.
Currently holds the privacy mode flag, which is set by:
  - main.py on startup (from conf.yml)
  - app.py when POST /api/spi/{piId}/occupancy arrives
  - app.py when POST /api/spi/{piId}/config arrives
"""

PRIVACY_MODE: bool = False


def set_privacy_mode(value: bool) -> None:
    global PRIVACY_MODE
    PRIVACY_MODE = value
    print(f"[STATE] privacy_mode={'ON' if value else 'OFF'}")


def get_privacy_mode() -> bool:
    return PRIVACY_MODE