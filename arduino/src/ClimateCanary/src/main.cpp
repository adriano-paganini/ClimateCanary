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

static volatile bool bleClientConnected = false;


void onBleConnected(BLEDevice central)
{
  bleClientConnected = true;
  String addr = central.address();
  addr.toUpperCase();

  Serial.println(addr);

  if (!hasEverConnected) {
    BLE.stopAdvertise();
    BLE.setManufacturerData((const uint8_t*)"11LCK", 5);
    BLE.advertise();
    hasEverConnected = true;
  }
}

void ledSetReadColorWritten(BLEDevice central, BLECharacteristic characteristic){

  RGBPacket packet;

  int n = characteristic.readValue((byte*)&packet, sizeof(packet));
  if (!n){
    Serial.println("Packet Malformed");
  }
  setColorRGB(packet.r, packet.g, packet.b);
}

void onBleDisconnected(BLEDevice central)
{
  bleClientConnected = false;
  Serial.println("Disconnected from host");
}

void bleTask()
{
  BLE.setEventHandler(BLEConnected, onBleConnected);
  BLE.setEventHandler(BLEDisconnected, onBleDisconnected);
  ledSetReadColor.setEventHandler(BLEWritten, ledSetReadColorWritten);

  uint32_t lastSend = 0;

  while (true) {
    BLE.poll();

    if (bleClientConnected) {
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
  if (!setupBLE("G5T4CC", environmentalSensingService, sensorPacketCharacteristic, ledSetReadColor))while (1);
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
    updateLightWithButtonState(state);
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
}
