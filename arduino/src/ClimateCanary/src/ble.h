#ifndef BLE_H
#define BLE_H
#include <ArduinoBLE.h>
#include <vector>
#include "sensors.h"
#include "buttons.h"

extern BLEService environmentalSensingService;
extern BLECharacteristic sensorPacketCharacteristic;
extern BLEService ledColorService;
extern BLECharacteristic ledSetReadColor;

struct __attribute__((packed)) SensorPacket {
  uint32_t timestamp;
  float pressure;
  float temperature;
  float humidity;
  uint32_t gasResistance;
};

struct __attribute__((packed))RGBPacket{
  uint8_t r;
  uint8_t g;
  uint8_t b;
};


void sendSensorPacket(SensorData data, BLECharacteristic& funcSensorDataCharacteristic);

inline void addCharacteristics(BLEService& service) {
  //needed for recursion
}


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

  environmentalSensingService = service;

  BLE.setLocalName(deviceName.c_str());
  BLE.setAdvertisedService(service);
  BLE.setManufacturerData((const uint8_t*)"00RDY", 5);

  addCharacteristics(service, characteristics...);
  
  BLE.addService(service);
  BLE.advertise();
  
  Serial.println("BLE device is now advertising with multiple characteristics!");
  return true;
}

#endif