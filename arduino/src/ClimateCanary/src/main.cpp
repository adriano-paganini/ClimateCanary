#include "sensors.h"
#include "light.h"
#include "buttons.h"
#include "screen.h"
#include "ble.h"
#include <rtos.h>
#include "KVStore.h"
#include "kvstore_global_api.h"

using namespace rtos;

Thread bleThread;

SensorData globalSensorData;
Mutex dataMutex;

//update button-states all 50ms
unsigned long lastButtonUpdate = 0;
const unsigned long buttonInterval = 50;
//update sensor data all 1000ms - 1s
unsigned long lastSensorUpdate = 0;
unsigned long sensorInterval = 1000;
//update light all with info onMs, offMs, r,g,b values
unsigned long lastLightUpdate = 0;
unsigned long lightOnMs = 1;
unsigned long lightOffMs = 0;
uint8_t lightR = 255;
uint8_t lightG = 0;
uint8_t lightB = 0;
bool lightOn = false;
//update the screen every 0.1s, but with a specific update function
unsigned long lastScreenUpdate = 0;
const unsigned long screenInterval = 500;
String smoothString = "";
int smoothIndex = 0;
void (*screenUpdateFunction)(String smoothString, int smoothIndex) = nullptr;


bool bleClientConnected = false;
bool bleClientAuthenticated = false;

const char* const ID_KEY = "ID";
const char* const INTERVAL_KEY = "INTERVAL";

ButtonState previousButtonState = {0,0,0};


void evaluateMeasurementStatus(BLEDevice central, BLECharacteristic characteristic){
  if (!bleClientAuthenticated){
    central.disconnect();
    Serial.println("Unauthenticated client tried to write measurement status. Disconnecting...");
    return;
  }
  SensorPacketStatus packet;
  int n = characteristic.readValue((byte*)&packet, sizeof(packet));
  if (!n){
    Serial.println("Packet Malformed");
  }
  //Process the packet and it's codes (0 - good, 1 - low, 2 - high)
  uint32_t timestamp = packet.timestamp;
  uint16_t statusCode = packet.statusCode;
  short pressureStatus = statusCode & 0x000F;
  short temperatureStatus = (statusCode & 0x00F0) >> 4;
  short humidityStatus = (statusCode & 0x0F00) >> 8;
  short gasResistanceStatus = (statusCode & 0xF000) >> 12;
  if (pressureStatus > 2 || temperatureStatus > 2 || humidityStatus > 2 || gasResistanceStatus > 2){
    Serial.println("Invalid status code received.");
    return;
  }else if (timestamp>millis()){
    Serial.println("Invalid timestamp received.");
    return;
  }
  //STATE-LOGIC


  //OPT
  Serial.print("Measurement Status at ");
  Serial.print(timestamp);
  Serial.print(": Pressure - ");
  Serial.print(pressureStatus);
  Serial.print(", Temperature - ");
  Serial.print(temperatureStatus);
  Serial.print(", Humidity - ");
  Serial.print(humidityStatus);
  Serial.print(", Gas Resistance - ");
  Serial.println(gasResistanceStatus);
}

void onSetupConfigWritten(BLEDevice central, BLECharacteristic characteristic){
  SetupConfig config;
  int n = characteristic.readValue((byte*)&config, sizeof(config));
  if (!n){
    Serial.println("Packet Malformed");
    return;
  }
  uint8_t measurementInterval = config.measurementInterval;
  uint32_t id = config.id;

  Serial.print("Received new setup config: Measurement Interval - ");
  Serial.print(measurementInterval);
  Serial.print(", ID - ");
  Serial.println(id);

  int idResult = kv_set(ID_KEY, &id, sizeof(id), id);
  int measurementIntervalResult = kv_set(INTERVAL_KEY, &measurementInterval, sizeof(measurementInterval), measurementInterval);

  if (idResult != MBED_SUCCESS || measurementIntervalResult != MBED_SUCCESS){
    Serial.println("Failed to write config to KVStore. Restarting Pairing process...");
  }
  NVIC_SystemReset();
}

void bleFirstSetup(){
  smoothString = BLE.address()+"         ";
  screenUpdateFunction = &waitForNewConnection;

  //quick orange blinking - 125ms on, 125ms off, r=255, g=30, b=0
  lightOnMs = 125;
  lightOffMs = 125;
  lightR = 255;
  lightG = 30;
  lightB = 0;
  setupCharacteristic.setEventHandler(BLEWritten, onSetupConfigWritten);

  while(true){
    BLE.poll();
    ThisThread::sleep_for(10);
  }
}

void onBleConnected(BLEDevice central) {

  kv_info_t idInfo;
  kv_info_t intervalInfo;
  uint32_t idValue;
  uint8_t intervalValue;

  int idResult = kv_get_info(ID_KEY, &idInfo);
  int intervalResult = kv_get_info(INTERVAL_KEY, &intervalInfo);

  if (idResult != MBED_SUCCESS || intervalResult != MBED_SUCCESS){
    Serial.println("Keys do not exist. Starting Pairing process...");
    NVIC_SystemReset();
  }else if (idInfo.size != sizeof(idValue) || intervalInfo.size != sizeof(intervalValue)){
    Serial.println("Keys exists but have unexpected sizes. Restarting Pairing process...");
    NVIC_SystemReset();
  }else{
    Serial.println("Keys exist and have expected sizes. Continuing with normal execution...");
    sensorInterval = intervalValue * 1000;
    BLE.stopAdvertise();
  }
  bleClientConnected = true;
}

void onBleDisconnected(BLEDevice central) {
  bleClientConnected = false;
  bleClientAuthenticated = false;
  BLE.advertise();
}

void onSetupConfigWrittenAuth(BLEDevice central, BLECharacteristic characteristic){
  //extract id from packet and compare with stored id
  SetupConfig config;
  int n = characteristic.readValue((byte*)&config, sizeof(config));
  if (!n){
    Serial.println("Packet Malformed");
    return;
  }
  uint32_t id = config.id;
  kv_info_t idInfo;
  int idResult = kv_get_info(ID_KEY, &idInfo);
  if (idResult != MBED_SUCCESS || idInfo.size != sizeof(id)){
    Serial.println("ID Key does not exist or has unexpected size. Restarting Pairing process...");
    NVIC_SystemReset();
  }
  uint32_t storedId;
  int idReadResult = kv_get(ID_KEY, &storedId, sizeof(storedId),&idInfo.size);
  if (idReadResult != MBED_SUCCESS){
    Serial.println("Failed to read ID from KVStore. Restarting Pairing process...");
    NVIC_SystemReset();
  }
  if (id != storedId){
    central.disconnect();
    Serial.println("Client provided wrong ID. Disconnecting...");
  }
  else{
    bleClientAuthenticated = true;
    Serial.println("Client authenticated successfully.");
  }
}

void bleTask()
{
  smoothString = "Waiting for known connection...";
  screenUpdateFunction = &waitForKnownConnection;
  //slower yellow blinking - 125ms on, 125ms off, r=255, g=80, b=0
  lightOnMs = 500;
  lightOffMs = 500;
  lightR = 255;
  lightG = 80;
  lightB = 0;
  BLE.setEventHandler(BLEConnected, onBleConnected);
  BLE.setEventHandler(BLEDisconnected,onBleDisconnected);
  sensorPacketStatusCharacteristic.setEventHandler(BLEWritten, evaluateMeasurementStatus);
  setupCharacteristic.setEventHandler(BLEWritten, onSetupConfigWrittenAuth);

  uint32_t lastSend = 0;

  while (true) {
    BLE.poll();

    if (bleClientConnected && bleClientAuthenticated) {
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
  if (!setupSensors()) while (1);
  setupScreen();
  setupButtons();
  setupLight();
  setColorRGB(lightR, lightG, lightB);
  Serial.begin(9600);
  while(!Serial);
  kv_info_t idInfo;
  kv_info_t intervalInfo;
  uint32_t idValue;
  uint8_t intervalValue;

  int idResult = kv_get_info(ID_KEY, &idInfo);
  int intervalResult = kv_get_info(INTERVAL_KEY, &intervalInfo);

  if (idResult ==intervalResult && idResult == MBED_SUCCESS){
    if (idInfo.size != sizeof(idValue) || intervalInfo.size != sizeof(intervalValue)){
      Serial.println("Keys exists but have unexpected sizes. Restarting Pairing process...");
        if (!setupBLE("G5T4SETUP","00RDY", setupService,setupCharacteristic))while (1);
        bleThread.start(bleFirstSetup);
    }else{
      Serial.println("Keys exist and have expected sizes. Continuing with normal execution...");
        if (!setupBLE("G5T4CC","11LCK", environmentalSensingService, sensorPacketCharacteristic, sensorPacketStatusCharacteristic,setupCharacteristic))while (1);
        bleThread.start(bleTask);
    }
  }else{
    Serial.println("Keys do not exist. Starting Pairing process...");
      if (!setupBLE("G5T4SETUP","00RDY", setupService,setupCharacteristic))while (1);
      bleThread.start(bleFirstSetup);
  }
}

//TODO: Create characteristic for config of arduino;

void loop() {

  unsigned long currentMillis = millis();

  if (lightOnMs==1 && lightOffMs == 0){
    //avoid setting light on every loop, when it should be constantly on
    if (!lightOn){
      setColorRGB(lightR, lightG, lightB);
      lightOn = true;
    }
  }
  else if (currentMillis - lastLightUpdate >= (lightOn ? lightOnMs : lightOffMs)) {
    lastLightUpdate = currentMillis;
    if (lightOn) {
      setColorRGB(0,0,0);
      lightOn = false;
    } else {
      setColorRGB(lightR, lightG, lightB);
      lightOn = true;
    }
  }

  //get button-state in a given buttonInterval
  if (currentMillis - lastButtonUpdate >= buttonInterval) {
    lastButtonUpdate = currentMillis;
    //determine, wether the button was also released after being pressed, or is just beeing held down.
    ButtonState currentButtonState = updateButtons();
    int activatedButtons = checkButtonActivation(previousButtonState, currentButtonState);
    previousButtonState = currentButtonState;
    if (activatedButtons){
       Serial.print("Activated Buttons: ");
      Serial.println(activatedButtons);
      }
    }
  // get up-to-date sensor data in a given sensorInterval
  if (currentMillis - lastSensorUpdate >= sensorInterval) {
    lastSensorUpdate = currentMillis;
    SensorData data = readSensors();
    dataMutex.lock();
    globalSensorData = data;
    dataMutex.unlock();
  }
  if (screenUpdateFunction && currentMillis - lastScreenUpdate >= screenInterval) {
    lastScreenUpdate = currentMillis;
    screenUpdateFunction(smoothString, smoothIndex);
    smoothIndex++;
    if (smoothIndex > smoothString.length()*2+8){
      smoothIndex = 0;
    }
  }
}
