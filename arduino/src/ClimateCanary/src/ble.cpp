#include "ble.h"

BLEService environmentalSensingService("0x181A");
BLECharacteristic sensorPacketCharacteristic(
  "12345678-1234-5678-1234-56789abcdef2",
  BLERead | BLENotify,
  sizeof(SensorPacket)
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