#ifndef BLE_H
#define BLE_H
#include <ArduinoBLE.h>
#include <vector>
#include "sensors.h"
#include "buttons.h"

extern BLEService environmentalSensingService;
extern BLECharacteristic sensorPacketCharacteristic;
extern BLECharacteristic sensorPacketStatusCharacteristic;

extern BLEService setupService;
extern BLECharacteristic setupCharacteristic;

extern BLECharacteristic authenticationCharacteristic;

extern BLECharacteristic warningMessageTotalLength;
extern BLECharacteristic warningMessageCharPack;
extern BLECharacteristic warningMessageAck;

struct __attribute__((packed)) WarningMessageCharPack{
  uint16_t sqn;
  char content;
};

struct __attribute__((packed)) SensorPacket {
  uint32_t timestamp;
  float pressure;
  float temperature;
  float humidity;
  uint32_t gasResistance;
};

struct __attribute__((packed)) SensorPacketStatus{
  uint32_t timestamp;
  uint16_t statusCode;
};

//This limits the measurement granularity SIGNIFICANTLY, but can always be changed.
// It is just to not to write too much data to the arduino
struct __attribute__((packed)) SetupConfig{
  uint8_t measurementInterval;
  uint32_t id;
};

struct __attribute__((packed))AuthentificationPacket{
  uint32_t id;
  char roomName[32];
  uint8_t roomNameLen;
};

inline void addCharacteristics(BLEService& service) {
  //needed for recursion
}

void sendSensorPacket(SensorData data, BLECharacteristic& funcSensorDataCharacteristic);


template <typename T, typename... Args>
void addCharacteristics(BLEService& service, T& firstChar, Args&... remainingChars) {
    service.addCharacteristic(firstChar);
    addCharacteristics(service, remainingChars...);
}

template <typename... Args>
bool setupBLE(String deviceName,String manufacturerData, BLEService& service, Args&... characteristics) {
  if (!BLE.begin()) {
    Serial.println("Starting BLE failed!");
    return false;
  }

  environmentalSensingService = service;

  BLE.setDeviceName(deviceName.c_str());
  BLE.setLocalName(deviceName.c_str());
  BLE.setAdvertisedService(service);
  BLE.setManufacturerData((const uint8_t*)manufacturerData.c_str(), manufacturerData.length());

  addCharacteristics(service, characteristics...);
  
  BLE.addService(service);
  BLE.advertise();
  
  Serial.println("BLE device is now advertising with multiple characteristics!");
  return true;
}

#endif