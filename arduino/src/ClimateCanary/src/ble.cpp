#include "ble.h"

BLEService environmentalSensingService("181A");
BLECharacteristic sensorDataCharacteristic(
  "4b8e0001-2581-4c5c-8a61-deb186a46179",
  BLERead | BLENotify,
  sizeof(SensorDataPacket)
);

BLECharacteristic sensorDataStatusCharacteristic(
  "4b8e0002-2581-4c5c-8a61-deb186a46179",
  BLERead | BLEWrite,
  sizeof(SensorStatusPacket)
);

BLECharacteristic cachedSensorDataCharacteristic(
  "4b8e0003-2581-4c5c-8a61-deb186a46179",
  BLERead|BLEWrite,
  sizeof(SensorDataPacket)
);

BLEBoolCharacteristic cachedSensorDataAckCharacteristic(
  "4b8e0004-2581-4c5c-8a61-deb186a46179",
  BLEWrite | BLENotify
);

BLEService deviceSetupService("94050000-af44-4a64-b339-8b04d5565014");
BLECharacteristic deviceSetupCharacteristic(
  "94050001-af44-4a64-b339-8b04d5565014",
  BLEWrite,
  sizeof(DeviceSetupConfig)
);

BLEService warningControlService("bda70000-24ff-4f28-af24-8293a69561ca");
BLECharacteristic warningAuthCharacteristic(
  "bda70001-24ff-4f28-af24-8293a69561ca",
  BLEWrite,
  sizeof(DeviceAuthenticationPacket)
);
BLEBoolCharacteristic warningAcknowledgedCharacteristic(
  "bda70002-24ff-4f28-af24-8293a69561ca",
  BLERead | BLENotify | BLEWrite
);
BLECharacteristic warningMessageLengthCharacteristic(
  "bda70003-24ff-4f28-af24-8293a69561ca",
  BLEWrite | BLERead,
  sizeof(uint16_t)
);
BLECharacteristic warningMessageChunkCharacteristic(
  "bda70004-24ff-4f28-af24-8293a69561ca",
  BLERead | BLEWrite,
  sizeof(WarningMessageChunk)
);
BLECharacteristic warningMessageAckRequestCharacteristic(
  "bda70005-24ff-4f28-af24-8293a69561ca",
  BLERead | BLENotify,
  sizeof(uint16_t)
);

void sendSensorPacket(SensorData data, BLECharacteristic& funcSensorDataCharacteristic) {
  SensorDataPacket packet;
  packet.timestamp = millis();
  packet.iaq = data.iaq;
  packet.temperature = data.temperature;
  packet.humidity = data.humidity;
  packet.pressure = data.pressure;

  funcSensorDataCharacteristic.writeValue((const uint8_t*)&packet, sizeof(packet));
}

bool initialSetupBLE(const char* deviceName, const char* manufacturerData){
  if (!BLE.begin()) {
    Serial.println("Starting BLE failed!");
    return false;
  }

  BLE.setDeviceName(deviceName);
  BLE.setLocalName(deviceName);
  BLE.setAdvertisedService(deviceSetupService);
  BLE.setManufacturerData((const uint8_t*)manufacturerData, strlen(manufacturerData));

  deviceSetupService.addCharacteristic(deviceSetupCharacteristic);
  BLE.addService(deviceSetupService);
  BLE.advertise();

  Serial.println("BLE device is now advertising!");
  return true;
}

bool normalSetupBLE(const char* deviceName, const char* manufacturerData){
  if (!BLE.begin()){
    Serial.println("Starting BLE failed!");
    return false;
  }
  BLE.setDeviceName(deviceName);
  BLE.setLocalName(deviceName);
  BLE.setAdvertisedService(environmentalSensingService);
  BLE.setManufacturerData((const uint8_t*)manufacturerData, strlen(manufacturerData));

  environmentalSensingService.addCharacteristic(sensorDataCharacteristic);
  environmentalSensingService.addCharacteristic(sensorDataStatusCharacteristic);
  environmentalSensingService.addCharacteristic(cachedSensorDataCharacteristic);
  environmentalSensingService.addCharacteristic(cachedSensorDataAckCharacteristic);

  warningControlService.addCharacteristic(warningAuthCharacteristic);
  warningControlService.addCharacteristic(warningAcknowledgedCharacteristic);
  warningControlService.addCharacteristic(warningMessageLengthCharacteristic);
  warningControlService.addCharacteristic(warningMessageChunkCharacteristic);
  warningControlService.addCharacteristic(warningMessageAckRequestCharacteristic);

  BLE.addService(environmentalSensingService);
  BLE.addService(warningControlService);

  BLE.advertise();

  Serial.println("BLE device is now advertising multiple Services and Characteristics!");
  return true;
}