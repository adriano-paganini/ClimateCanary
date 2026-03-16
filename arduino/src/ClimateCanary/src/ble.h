#ifndef BLE_H
#define BLE_H
#include <ArduinoBLE.h>
#include <vector>
#include "sensors.h"
#include "buttons.h"

extern BLEService arduinoTest;
extern BLECharacteristic sensorDataCharacteristic;
extern BLECharCharacteristic buttonPressCharacteristic;

struct __attribute__((packed)) RelativePacket {
  uint32_t ms_stamp; 
  float s1, s2, s3, s4; 
};

template <typename T, typename... Args>
void addCharacteristics(BLEService& service, T& firstChar, Args&... remainingChars);

template <typename... Args>
bool setupBLE(String deviceName, BLEService& service, Args&... characteristics);

void sendSensorData(SensorData data, uint32_t ms_stamp, BLECharacteristic& funcSensorDataCharacteristic);

#endif