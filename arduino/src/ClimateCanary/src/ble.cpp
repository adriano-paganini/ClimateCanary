#include "ble.h"

BLEService arduinoTest("19B10000-E8F2-537E-4F6C-D104768A1214");
BLECharacteristic sensorDataCharacteristic("19B10001-E8F2-537E-4F6C-D104768A1214", BLERead | BLENotify, sizeof(RelativePacket));


void sendSensorData(SensorData data, uint32_t ms_stamp, BLECharacteristic& funcSensorDataCharacteristic) {
  RelativePacket packet;
  packet.ms_stamp = ms_stamp;
  packet.s1 = data.temperature;
  packet.s2 = data.humidity;
  packet.s3 = data.pressure;
  packet.s4 = data.gas_resistance;

  funcSensorDataCharacteristic.writeValue((uint8_t*)&packet, sizeof(packet));
}

