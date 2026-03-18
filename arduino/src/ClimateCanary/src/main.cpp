#include "sensors.h"
#include "light.h"
#include "buttons.h"
#include "screen.h"
#include "ble.h"
#include <rtos.h>

using namespace rtos;

Thread bleThread;

SensorData globalSensorData;
Mutex dataMutex;

unsigned long lastButtonUpdate = 0;
const unsigned long buttonInterval = 100;

unsigned long lastSensorUpdate = 0;
const unsigned long sensorInterval = 1000;

static unsigned long lastLightUpdate = 0;
const unsigned long lightInterval = 50;
const unsigned int lightSpeed = 1;

bool hasEverConnected = false;

void bleTask() {
  uint32_t lastSend = 0;

  while (true) {
    BLE.poll();

    BLEDevice central = BLE.central();

    if (central.connected()) {
      if (!hasEverConnected){
        BLE.stopAdvertise();
        BLE.setManufacturerData((const uint8_t*)"11LCK", 5);
        BLE.advertise();
        hasEverConnected = true;
      }
      uint32_t now = millis();

      if (now - lastSend >= 1000) {
        dataMutex.lock();
        sendSensorPacket(globalSensorData, sensorPacketCharacteristic);
        dataMutex.unlock();

        lastSend = now;
      }
    }

    ThisThread::sleep_for(10);
  }
}


void setup() {
  Serial.begin(9600);

  if (!setupSensors()) while (1);
  if (!setupBLE("G5T4CC", environmentalSensingService, sensorPacketCharacteristic))while (1);
  setupScreen();
  setupButtons();
  setupLight();
  bleThread.start(bleTask);
}

//TODO: Create characteristic for config of arduino;

void loop() {

  unsigned long currentMillis = millis();

  if (currentMillis - lastButtonUpdate >= buttonInterval) {
    lastButtonUpdate = currentMillis;

    ButtonState state = updateButtons();

    printButtonScreen(state);
  }

  if (currentMillis - lastSensorUpdate >= sensorInterval) {
    lastSensorUpdate = currentMillis;

    SensorData data = readSensors();

    dataMutex.lock();
    globalSensorData = data;
    dataMutex.unlock();
    Serial.print("Temp: ");
    Serial.print(data.temperature);
    Serial.print(" °C, Humidity: ");
    Serial.print(data.humidity);
    Serial.print(" %, Pressure: ");
    Serial.print(data.pressure);
    Serial.print(" hPa, Gas Resistance: ");
    Serial.print(data.gas_resistance);
    Serial.println(" ohms");
    printSensorScreen(data);
  }

  if (currentMillis - lastLightUpdate >= lightInterval) {
    lastLightUpdate = currentMillis;
    updateLight(lightSpeed);
  }
}
