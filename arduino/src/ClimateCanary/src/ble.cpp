#include "ble.h"

BLEService arduinoTest("19B10000-E8F2-537E-4F6C-D104768A1214");
BLECharacteristic sensorDataCharacteristic("19B10001-E8F2-537E-4F6C-D104768A1214", BLERead | BLENotify, sizeof(RelativePacket));
BLECharCharacteristic buttonPressCharacteristic("19B10002-E8F2-537E-4F6C-D104768A1214", BLERead | BLENotify);

template <typename T, typename... Args>
void addCharacteristics(BLEService& service, T& firstChar, Args&... remainingChars) {
    service.addCharacteristic(firstChar);
    addCharacteristics(service, remainingChars...);
}

template <typename... Args>
bool setupBLE(String deviceName, BLEService& service, Args&... characteristics) {
  if (!BLE.begin()) {
    Serial.println("Starting BLE failed!");
    return false;
  }

  arduinoTest = service;

  BLE.setLocalName(deviceName.c_str());
  BLE.setAdvertisedService(service);

  addCharacteristics(service, characteristics...);
  
  BLE.addService(service);
  BLE.advertise();
  
  Serial.println("BLE device is now advertising with multiple characteristics!");
  return true;
}

void sendSensorData(SensorData data, uint32_t ms_stamp, BLECharacteristic& funcSensorDataCharacteristic) {
  RelativePacket packet;
  packet.ms_stamp = ms_stamp;
  packet.s1 = data.temperature;
  packet.s2 = data.humidity;
  packet.s3 = data.pressure;
  packet.s4 = data.gas_resistance;

  funcSensorDataCharacteristic.writeValue((uint8_t*)&packet, sizeof(packet));
}

