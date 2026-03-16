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



void bleTask() {
  while (true) {
      BLEDevice central = BLE.central();
    if (central) {
      while (central.connected()) {
        dataMutex.lock();
        sendSensorData(globalSensorData, millis(), sensorDataCharacteristic);
        dataMutex.unlock();
        ThisThread::sleep_for(1000);
      }
    }
    ThisThread::sleep_for(500);
  }
}

void setup() {
  Serial.begin(9600);

  if (!setupSensors()) while (1);
  if (!setupBLE("BLE33_ClimateCanary_G5T4", arduinoTest,sensorDataCharacteristic))while (1);
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
    printSensorScreen(data);
  }

  if (currentMillis - lastLightUpdate >= lightInterval) {
    lastLightUpdate = currentMillis;
    updateLight(lightSpeed);
  }
}
