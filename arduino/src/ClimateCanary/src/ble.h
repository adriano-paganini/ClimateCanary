#ifndef BLE_H
#define BLE_H
#include <ArduinoBLE.h>
#include <vector>
#include "sensors.h"
#include "buttons.h"

extern BLEService environmentalSensingService;
extern BLECharacteristic sensorDataCharacteristic;
extern BLECharacteristic sensorDataStatusCharacteristic;

extern BLEService deviceSetupService;
extern BLECharacteristic deviceSetupCharacteristic;
extern BLECharacteristic cachedSensorDataCharacteristic;
extern BLEBoolCharacteristic cachedSensorDataAckCharacteristic;

extern BLEService warningControlService;
extern BLECharacteristic warningAuthCharacteristic;
extern BLEBoolCharacteristic warningAcknowledgedCharacteristic;
extern BLECharacteristic warningMessageLengthCharacteristic;
extern BLECharacteristic warningMessageChunkCharacteristic;
extern BLECharacteristic warningMessageAckRequestCharacteristic;


struct __attribute__((packed)) DeviceSetupConfig {
  uint8_t measurementInterval;
  uint32_t deviceId;
};

struct __attribute__((packed)) DeviceAuthenticationPacket {
  uint32_t deviceId;
  char roomName[32];
  uint8_t roomNameLength;
};

struct __attribute__((packed)) SensorDataPacket {
  uint32_t timestamp;
  float pressure;
  float temperature;
  float humidity;
  uint32_t gasResistance;
};

struct __attribute__((packed)) SensorStatusPacket {
  uint32_t timestamp;
  uint16_t statusCode;
};

struct __attribute__((packed)) WarningMessageChunk {
  uint16_t sequenceNumber;
  char content;
};

void sendSensorPacket(SensorData data, BLECharacteristic& funcSensorDataCharacteristic);

bool initialSetupBLE(const char* deviceName, const char* manufacturerData);

bool normalSetupBLE(const char* deviceName, const char* manufacturerData);

#endif