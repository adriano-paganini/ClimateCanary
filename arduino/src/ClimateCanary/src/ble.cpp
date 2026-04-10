#include "ble.h"

BLEService environmentalSensingService("0x181A");
BLECharacteristic sensorPacketCharacteristic(
  "12345678-1234-5678-1234-56789abcdef2",
  BLERead | BLENotify,
  sizeof(SensorPacket)
);
BLECharacteristic sensorPacketStatusCharacteristic(
  "ff9ff767-a7a2-46c7-bafc-5330fc8d9357",
  BLERead | BLEWrite,
  sizeof(SensorPacketStatus)
);

BLEService setupService("9405d1c7-af44-4a64-b339-8b04d5565014");
BLECharacteristic setupCharacteristic(
  "df2bd1ae-56f7-42bd-960a-1b9163fc2f12",
  BLEWrite,
  sizeof(SetupConfig)
);



void sendSensorPacket(SensorData data, BLECharacteristic& funcSensorDataCharacteristic) {
  SensorPacket packet;
  packet.timestamp = millis();
  packet.pressure = data.pressure;
  packet.temperature = data.temperature;
  packet.humidity = data.humidity;
  packet.gasResistance = data.gas_resistance;

  funcSensorDataCharacteristic.writeValue((const uint8_t*)&packet, sizeof(packet));
}
