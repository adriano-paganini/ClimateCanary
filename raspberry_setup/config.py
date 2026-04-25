DEVICE_ADDR     = "7C:DE:CE:44:CC:B1"
DATA_CHAR_UUID  = "12345678-1234-5678-1234-56789abcdef2"
LED_COLOUR_UUID = "ff9ff767-a7a2-46c7-bafc-5330fc8d9357"
SENSOR_STRUCT   = "<LfffL"
API_URL         = "http://10.33.23.18:8080/api/sensor"
DB_PATH         = "sensor.db"
SEND_INTERVAL   = 5
SPRING_BOOT_URL = "http://10.33.23.18:8080/api/status"

# Environmental Sensing Service
DATA_CHAR_UUID        = "4b8e0001-2581-4c5c-8a61-deb186a46179"  # Sensor Packet (Notify)
STATUS_CHAR_UUID      = "4b8e0002-2581-4c5c-8a61-deb186a46179"  # Sensor Packet Status (Write)

# Warning Control Service
AUTH_CHAR_UUID        = "bda70001-24ff-4f28-af24-8293a69561ca"  # Authentication Packet (Write)

PI_ID: int = None
ROOM_ID: int = None
HOST_NAME: str = None
BACKEND_URL: str = None
SENSOR_STATION_ID: int = None
DEVICE_ID: int = None
ROOM_NAME: str = None

def load_config(path: str = "conf.yml"):
    global PI_ID, ROOM_ID, HOST_NAME, BACKEND_URL, DEVICE_ID, ROOM_NAME
    cfg = yaml.safe_load(Path(path).read_text())
    PI_ID        = cfg["pi"]["id"]
    ROOM_ID      = cfg["pi"]["room_id"]
    HOST_NAME    = cfg["pi"]["host_name"]
    BACKEND_URL  = cfg["pi"]["backend_url"]
    DEVICE_ID    = cfg["pi"]["device_id"]
    ROOM_NAME    = cfg["pi"]["room_name"]