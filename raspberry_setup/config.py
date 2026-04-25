import yaml
from pathlib import Path

BLE_NAME_NORMAL   = "G5T4CC"
MANUF_DATA_NORMAL = b"11LCK"
SVC_ENV_NORMAL    = "0000181a-0000-1000-8000-00805f9b34fb"
SCAN_DURATION     = 10.0

# do I still need them?
DEVICE_ADDR     = "7C:DE:CE:44:CC:B1"
LED_COLOUR_UUID = "ff9ff767-a7a2-46c7-bafc-5330fc8d9357"
API_URL         = "http://10.33.23.18:8080/api/sensor"
SPRING_BOOT_URL = "http://10.33.23.18:8080/api/status"

DB_PATH         = "sensor.db"
SEND_INTERVAL   = 5
SENSOR_STRUCT   = "<LfffL"   # uint32 ts | float press | float temp | float hum | uint32 gas

BLE_NAME_NORMAL   = "G5T4CC"
BLE_NAME_SETUP    = "G5T4SETUP"
MANUF_DATA_NORMAL = b"11LCK"
MANUF_DATA_SETUP  = b"00RDY"


# Environmental Sensing Service (UUID 181A)
DATA_CHAR_UUID        = "4b8e0001-2581-4c5c-8a61-deb186a46179"  # Sensor Packet        Read, Notify
SENSOR_STATUS_UUID    = "4b8e0002-2581-4c5c-8a61-deb186a46179"  # Sensor Packet Status  Read, Write
CACHED_DATA_UUID      = "4b8e0003-2581-4c5c-8a61-deb186a46179"  # Cached Sensor Data    Read
CACHED_DATA_ACK_UUID  = "4b8e0004-2581-4c5c-8a61-deb186a46179"  # Cached Sensor ACK     Write, Notify

# Warning Control Service
AUTH_CHAR_UUID            = "bda70001-24ff-4f28-af24-8293a69561ca"  # Authentication Packet     Write
WARNING_ACK_UUID          = "bda70002-24ff-4f28-af24-8293a69561ca"  # Warning Acknowledged      Read, Write, Notify
WARNING_TOTAL_LEN_UUID    = "bda70003-24ff-4f28-af24-8293a69561ca"  # Warning Message Total Len Read, Write
WARNING_CHAR_PACK_UUID    = "bda70004-24ff-4f28-af24-8293a69561ca"  # Warning Message Char Pack Read, Write
WARNING_ACK_REQUEST_UUID  = "bda70005-24ff-4f28-af24-8293a69561ca"  # Warning Message ACK Req   Read, Notify

STATUS_CHAR_UUID = SENSOR_STATUS_UUID

PI_ID:             int  = None
ROOM_ID:           int  = None
HOST_NAME:         str  = None
BACKEND_URL:       str  = None
SENSOR_STATION_ID: int  = None
DEVICE_ID:         int  = None
ROOM_NAME:         str  = None

def load_config(path: str = "conf.yml"):
    global PI_ID, ROOM_ID, HOST_NAME, BACKEND_URL, DEVICE_ID, ROOM_NAME
    cfg = yaml.safe_load(Path(path).read_text())
    PI_ID        = cfg["pi"]["id"]
    ROOM_ID      = cfg["pi"]["room_id"]
    HOST_NAME    = cfg["pi"]["host_name"]
    BACKEND_URL  = cfg["pi"]["backend_url"]
    DEVICE_ID    = cfg["pi"]["device_id"]
    ROOM_NAME    = cfg["pi"]["room_name"]
