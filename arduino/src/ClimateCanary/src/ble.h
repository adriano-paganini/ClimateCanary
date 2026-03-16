#ifndef BLE_H
#define BLE_H
#include <ArduinoBLE.h>
#include <vector>
#include "sensors.h"
#include "buttons.h"

extern BLEService arduinoTest;
extern BLECharacteristic sensorDataCharacteristic;

struct __attribute__((packed)) RelativePacket {
  uint32_t ms_stamp; 
  float s1, s2, s3, s4; 
};

void sendSensorData(SensorData data, uint32_t ms_stamp, BLECharacteristic& funcSensorDataCharacteristic);

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

  arduinoTest = service;

  BLE.setLocalName(deviceName.c_str());
  BLE.setAdvertisedService(service);

  addCharacteristics(service, characteristics...);
  
  BLE.addService(service);
  BLE.advertise();
  
  Serial.println("BLE device is now advertising with multiple characteristics!");
  return true;
}

#endif