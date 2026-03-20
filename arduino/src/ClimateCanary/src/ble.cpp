#include "ble.h"

BLEService environmentalSensingService("0x181A");
BLECharacteristic sensorPacketCharacteristic(
  "12345678-1234-5678-1234-56789abcdef2",
  BLERead | BLENotify,
  sizeof(SensorPacket)
);
BLEService ledColorService("f60518ee-c7e2-4bac-af6e-e501e4406a98");
BLECharacteristic ledSetReadColor("ff9ff767-a7a2-46c7-bafc-5330fc8d9357", BLERead | BLEWrite, sizeof(RGBPacket));



void sendSensorPacket(SensorData data, BLECharacteristic& funcSensorDataCharacteristic) {
  SensorPacket packet;
  packet.timestamp = millis();
  packet.pressure = data.pressure;
  packet.temperature = data.temperature;
  packet.humidity = data.humidity;
  packet.gasResistance = data.gas_resistance;

  funcSensorDataCharacteristic.writeValue((const uint8_t*)&packet, sizeof(packet));
}
