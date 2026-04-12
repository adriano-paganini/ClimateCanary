#include "ble.h"

BLEService environmentalSensingService("0x181A");
BLECharacteristic sensorPacketCharacteristic(
  "4b8eba77-2581-4c5c-8a61-deb186a46179",
  BLERead | BLENotify,
  sizeof(SensorPacket)
);
BLECharacteristic sensorPacketStatusCharacteristic(
  "3f7346be-e224-45d7-91d2-71047d229b0a",
  BLERead | BLEWrite,
  sizeof(SensorPacketStatus)
);

BLEService setupService("9405d1c7-af44-4a64-b339-8b04d5565014");
BLECharacteristic setupCharacteristic(
  "b7c5b6c6-820c-47eb-a641-b64cde06e6ac",
  BLEWrite,
  sizeof(SetupConfig)
);

BLECharacteristic authenticationCharacteristic(
  "bda7eaf2-24ff-4f28-af24-8293a69561ca",
  BLEWrite,
  sizeof(AuthentificationPacket)
);

BLECharacteristic warningMessageTotalLengthCharacteristic(
  "c7e9187b-173a-4a56-bdd7-cc890f2b1366",
  BLEWrite|BLERead,
  sizeof(uint16_t)
);

BLECharacteristic warningMessageCharPackCharacteristic(
  "a41f4b38-61c5-4528-84b2-9283bd185619",
  BLERead|BLEWrite,
  sizeof(WarningMessageCharPack)
);

BLECharacteristic warningMessageAckCharacteristic(
  "e6b0f5a2-4c9a-4f7d-8f3d-1a2b3c4d5e6f",
  BLERead|BLENotify,
  sizeof(uint16_t)
);


void sendSensorPacket(SensorData data, BLECharacteristic& funcSensorDataCharacteristic) {
  SensorPacket packet;
  packet.timestamp = millis();
  packet.pressure = data.pressure;
  packet.temperature = data.temperature;
  packet.humidity = data.humidity;
  packet.gasResistance = data.gas_resistance;

  funcSensorDataCharacteristic.writeValue((const uint8_t*)&packet, sizeof(packet));
}
