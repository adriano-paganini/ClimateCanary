import asyncio
import struct
from bleak import BleakClient
import aiohttp

API_URL = "http://192.168.0.90:8080/api/sensor"

DEVICE_ADDR = "7C:DE:CE:44:CC:B1"

DATA_CHAR_UUID  = "12345678-1234-5678-1234-56789abcdef2" 
LED_COLOUR_UUID = "ff9ff767-a7a2-46c7-bafc-5330fc8d9357"
COMMAND_UUID    = "0000181a-0000-1000-8000-00805f9b34fb" 

# Struct: Long, float, float, float, Long
# timestamp, pressure, temperature, humidity, gasResistance
SENSOR_STRUCT = "<LfffL" 

async def send_to_api(payload):
    async with aiohttp.ClientSession() as session:
        try:
            async with session.post(API_URL, json=payload) as resp:
                print(f"API Status: {resp.status}")
        except Exception as e:
            print(f"API Error: {e}")

async def main():
    print(f"Connecting to {DEVICE_ADDR}...")
    
    async with BleakClient(DEVICE_ADDR, timeout=20.0) as client:
        print(f"Connected: {client.is_connected}")

        async def handle_data(sender, data):
            try:
                timestamp, press, temp, hum, gas = struct.unpack(SENSOR_STRUCT, data)

                payload = {
                    "timestamp": timestamp,
                    "temperature": temp,
                    "humidity": hum,
                    "pressure": press,
                    "gasResistance": gas
                }

                await send_to_api(payload)
                
                print(f"\n--- {timestamp} ms ---")
                print(f"Temp: {temp:.2f}°C | Hum: {hum:.2f}% | Press: {press:.2f} hPa")
                print(f"Gas Resistance: {gas} ohms")
                
                if temp > 30.0:
                    print("ALERT: High Temp! Setting LED Red...")
                    await client.write_gatt_char(LED_COLOUR_UUID, bytearray([255, 0, 0]))
                else:
                    print("Temp OK. Setting LED Green...")
                    await client.write_gatt_char(LED_COLOUR_UUID, bytearray([0, 255, 0]))
                                            
            except Exception as e:
                print(f"Parsing Error: {e}")

        await client.start_notify(DATA_CHAR_UUID, handle_data)
        
        while True:
            await asyncio.sleep(1)

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("Disconnected.")
    except Exception as e:
        print(f"Main Loop Error: {e}")