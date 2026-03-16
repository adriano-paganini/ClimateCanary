#ifndef BLE_H
#define BLE_H
#include <ArduinoBLE.h>
#include <vector>

extern BLEService arduinoTest;
extern BLECharacteristic sensorDataCharacteristic;

struct __attribute__((packed)) RelativePacket {
  uint32_t ms_stamp; 
  float s1, s2, s3, s4; 
};

bool setupBLE(String deviceName = "BLE33_ClimateCanary_G5T4", BLEService service = arduinoTest, BLECharacteristic sensorDataChar = sensorDataCharacteristic);

#endif