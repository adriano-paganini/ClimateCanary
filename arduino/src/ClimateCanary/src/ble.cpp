#include "ble.h"

BLEService arduinoTest("19B10000-E8F2-537E-4F6C-D104768A1214");
BLECharacteristic sensorDataCharacteristic("19B10001-E8F2-537E-4F6C-D104768A1214", BLERead | BLENotify, sizeof(RelativePacket));


bool setupBLE(String deviceName, BLEService service, BLECharacteristic sensorDataChar) {
  if (!BLE.begin()) {
    Serial.println("Starting BLE failed!");
    return false;
  }

  BLE.setLocalName(deviceName.c_str());
  BLE.setAdvertisedService(service);

  service.addCharacteristic(sensorDataChar);
  BLE.addService(service);

  BLE.advertise();
  
  Serial.println("BLE device is now advertising!");

  return true;
}