import yaml
import socket
from pathlib import Path

BLE_NAME_NORMAL   = "G5T4CC"
BLE_NAME_SETUP    = "G5T4SETUP"
MANUF_DATA_NORMAL = b"11LCK"
MANUF_DATA_SETUP  = b"00RDY"
SVC_ENV_NORMAL    = "0000181a-0000-1000-8000-00805f9b34fb"
SVC_SETUP         = "94050000-af44-4a64-b339-8b04d5565014"
SETUP_CONFIG_UUID = "94050001-af44-4a64-b339-8b04d5565014"
SCAN_DURATION     = 10.0


# Environmental Sensing Service (UUID 181A)
DATA_CHAR_UUID       = "4b8e0001-2581-4c5c-8a61-deb186a46179"  # Sensor Packet        Read, Notify
SENSOR_STATUS_UUID   = "4b8e0002-2581-4c5c-8a61-deb186a46179"  # Sensor Packet Status  Read, Write
CACHED_DATA_UUID     = "4b8e0003-2581-4c5c-8a61-deb186a46179"  # Cached Sensor Data    Read
CACHED_DATA_ACK_UUID = "4b8e0004-2581-4c5c-8a61-deb186a46179"  # Cached Sensor ACK     Write, Notify

# Warning Control Service
AUTH_CHAR_UUID           = "bda70001-24ff-4f28-af24-8293a69561ca"  # Authentication Packet     Write
WARNING_ACK_UUID         = "bda70002-24ff-4f28-af24-8293a69561ca"  # Warning Acknowledged      Read, Write, Notify
WARNING_TOTAL_LEN_UUID   = "bda70003-24ff-4f28-af24-8293a69561ca"  # Warning Message Total Len Read, Write
WARNING_CHAR_PACK_UUID   = "bda70004-24ff-4f28-af24-8293a69561ca"  # Warning Message Char Pack Read, Write
WARNING_ACK_REQUEST_UUID = "bda70005-24ff-4f28-af24-8293a69561ca"  # Warning Message ACK Req   Read, Notify

STATUS_CHAR_UUID = SENSOR_STATUS_UUID

# runtime config
DB_PATH      = "sensor.db"
SEND_INTERVAL = 5
SENSOR_STRUCT = "<LfffL"   # uint32 ts | float press | float temp | float hum | uint32 gas

PI_ID:        int  = None
ROOM_ID:      int  = None
ROOM_NAME:    str  = None
HOST_NAME:    str  = None
BACKEND_URL:  str  = None
PRIVACY_MODE: bool = False


def _apply(cfg: dict) -> None:
    """Write parsed config dict into module-level globals."""
    global PI_ID, ROOM_ID, ROOM_NAME, HOST_NAME, BACKEND_URL, PRIVACY_MODE
    pi          = cfg["pi"]
    PI_ID        = int(pi["id"])
    ROOM_ID      = int(pi["room_id"])
    ROOM_NAME    = str(pi["room_name"])
    HOST_NAME    = str(pi["host_name"])
    BACKEND_URL  = str(pi["backend_url"]).rstrip("/")
    PRIVACY_MODE = bool(pi.get("privacy_mode", False))


def load_config(path: str = "conf.yml") -> None:
    """Load config from a local YAML file (used on first boot from SD card)."""
    _apply(yaml.safe_load(Path(path).read_text()))


def load_config_from_string(yaml_string: str) -> None:
    """Load config from a raw YAML string returned by the backend."""
    _apply(yaml.safe_load(yaml_string))


def get_local_ip() -> str:
    """Best-effort local IP address (used for the /booted call)."""
    import urllib.parse
    try:
        host = urllib.parse.urlparse(BACKEND_URL).hostname or "8.8.8.8"
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.connect((host, 80))
            return s.getsockname()[0]
    except Exception:
        return "0.0.0.0"