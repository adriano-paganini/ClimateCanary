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
void (*screenUpdateFunction)(int smoothIndex, RelevantDisplayData data) = nullptr;


bool bleClientConnected = false;
bool bleClientAuthenticated = false;

const char* const ID_KEY = "ID";
const char* const INTERVAL_KEY = "INTERVAL";

uint16_t activeWarningCode=0;

ButtonState previousButtonState = {0,0,0};

String currentState = "STARTUP";
bool stateChanged = false;
String roomName = "";
String smoothString = "";
int smoothIndex = 0;
bool altView = false;
uint16_t statusCode = 0;

std::vector<std::string> currentWarningMessages;


void beginMessageTransfer(){
  uint16_t totalWarningLength;
  int warnLen =  warningMessageTotalLength.readValue(&totalWarningLength,sizeof(totalWarningLength));
  if (warnLen == sizeof(totalWarningLength)){
    Serial.println(totalWarningLength);
    if (totalWarningLength != 0){
      for (uint16_t i = 0; i< totalWarningLength; i++){
        //TODO: implement Logic to save allstrings - SEPERATED BY \0 - into the global String-repository.
      }
    }
  }else{
    Serial.println("Message Transfer Failed, retrying at next iteration.");
  }
}

void evaluateMeasurementStatus(BLEDevice central, BLECharacteristic characteristic){
  //if the client is not authenticated, disconnect them immediately
  if (!bleClientAuthenticated){
    central.disconnect();
    Serial.println("Unauthenticated client tried to write measurement status. Disconnecting...");

    currentState = "WAITING_FOR_KNOWN_CONNECTION";
    stateChanged = true;

    return;
  }
  SensorPacketStatus packet;
  int n = characteristic.readValue((byte*)&packet, sizeof(packet));
  if (!n){
    Serial.println("Packet Malformed");
  }
  //Process the packet and it's codes (0 - good, 1 - low, 2 - high, 3 - active warning, 4 - acknowledged warning)
  uint32_t timestamp = packet.timestamp;
  statusCode = packet.statusCode;
  Serial.println("Received new measurement status: ");
  Serial.println(statusCode, BIN);
  short pressureStatus = statusCode & 0x000F;
  short temperatureStatus = (statusCode & 0x00F0) >> 4;
  short humidityStatus = (statusCode & 0x0F00) >> 8;
  short gasResistanceStatus = (statusCode & 0xF000) >> 12;
  if (pressureStatus > 4 || temperatureStatus > 4 || humidityStatus > 4 || gasResistanceStatus > 4){
    Serial.println("Invalid status code received.");
    return;
  }else if (timestamp>millis()){
    Serial.println("Invalid timestamp received.");
    return;
  }
  //STATE-LOGIC
  if ((pressureStatus > 0 && pressureStatus < 3) || (temperatureStatus > 0 && temperatureStatus < 3) 
    || (humidityStatus > 0 && humidityStatus < 3) || (gasResistanceStatus > 0 && gasResistanceStatus < 3)){
      currentState = "CONNECTED_SOME_SHORT_INVALID_DATA";
      stateChanged = true;
  }if (pressureStatus == 3 || temperatureStatus == 3 || humidityStatus == 3 || gasResistanceStatus == 3){
    if (statusCode != activeWarningCode){
      activeWarningCode=statusCode;
      beginMessageTransfer();
    }
  }else{
    currentState = "CONNECTED_ALL_VALID_DATA";
    stateChanged = true;
  }



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
  currentState = "WAITING_FOR_NEW_CONNECTION";
  stateChanged = true;

  setupCharacteristic.setEventHandler(BLEWritten, onSetupConfigWritten);

  while(true){
    BLE.poll();
    ThisThread::sleep_for(10);
  }
}

void onBleConnected(BLEDevice central) {

  currentState = "WAITING_FOR_AUTHENTICATION";
  stateChanged = true;


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

  currentState = "WAITING_FOR_KNOWN_CONNECTION";
  stateChanged = true;

  BLE.advertise();
}

void onAuthenticationPacketWritten(BLEDevice central, BLECharacteristic characteristic){
  //extract id from packet and compare with stored id
  AuthentificationPacket packet;
  int n = characteristic.readValue((byte*)&packet, sizeof(packet));
  if (!n){
    Serial.println("Packet Malformed");
    return;
  }
  uint32_t id = packet.id;
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
    roomName = String(packet.roomName).substring(0, packet.roomNameLen);
    Serial.print("Room Name: ");
    Serial.println(roomName);

    currentState = "CONNECTED_ALL_VALID_DATA";
    stateChanged = true;
  }
}

void bleTask()
{
  currentState = "WAITING_FOR_KNOWN_CONNECTION";
  stateChanged = true;

  BLE.setEventHandler(BLEConnected, onBleConnected);
  BLE.setEventHandler(BLEDisconnected,onBleDisconnected);
  sensorPacketStatusCharacteristic.setEventHandler(BLEWritten, evaluateMeasurementStatus);
  authenticationCharacteristic.setEventHandler(BLEWritten, onAuthenticationPacketWritten);

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

void setStateData(){
  if (currentState == "WAITING_FOR_KNOWN_CONNECTION"){
      smoothString = "Waiting for known connection...";
      screenUpdateFunction = &waitForKnownConnection;
      //slower yellow blinking - 500ms on, 500ms off, r=255, g=80, b=0
      lightOnMs = 500;
      lightOffMs = 500;
      lightR = 255;
      lightG = 80;
      lightB = 0;
  }else if (currentState == "WAITING_FOR_AUTHENTICATION"){
      smoothString = "Waiting for authentication...";
      screenUpdateFunction = &waitForAuthenticatedConnection;
      // very quick green blinking - 25ms on 75 ms off, r=0, g=255, b=0
      lightOnMs = 25;
      lightOffMs = 75;
      lightR = 0;
      lightG = 255;
      lightB = 0;
  }else if (currentState == "WAITING_FOR_NEW_CONNECTION"){
      smoothString = BLE.address();
      screenUpdateFunction = &waitForNewConnection;
      //quick orange blinking - 125ms on, 125ms off, r=255, g=30, b=0
      lightOnMs = 125;
      lightOffMs = 125;
      lightR = 255;
      lightG = 30;
      lightB = 0;
    }else if (currentState == "CONNECTED_ALL_VALID_DATA"){
      smoothString = roomName;
      screenUpdateFunction = &connectedAllValidData;
      //light on constantly with a soft green color - r=0, g=255, b=0
      lightOnMs = 1;
      lightOffMs = 0;
      lightR = 0;
      lightG = 255;
      lightB = 0;
      lightOn = false; //force update of light in next loop
    }else if (currentState == "CONNECTED_SOME_SHORT_INVALID_DATA"){
      smoothString = roomName;
      screenUpdateFunction = &connectedSomeShortInvalidData;
      //light on constantly with a orange color - r=255, g=30, b=0
      lightOnMs = 1;
      lightOffMs = 0;
      lightR = 255;
      lightG = 30;
      lightB = 0;
      lightOn = false; //force update of light in next loop
    }
}

void setup() {
  if (!setupSensors()) while (1);
  setupScreen();
  setupButtons();
  setupLight();
  setColorRGB(lightR, lightG, lightB);
  Serial.begin(9600);
  //TODO: remove while(!Serial); for final product
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
        if (!setupBLE("G5T4CC","11LCK", environmentalSensingService,
              sensorPacketCharacteristic,
              sensorPacketStatusCharacteristic,
              authenticationCharacteristic,
              warningMessageTotalLength,
              warningMessageCharPack,
              warningMessageAck))while (1);
        bleThread.start(bleTask);
    }
  }else{
    Serial.println("Keys do not exist. Starting Pairing process...");
      if (!setupBLE("G5T4SETUP","00RDY", setupService,setupCharacteristic))while (1);
      bleThread.start(bleFirstSetup);
  }
}

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
    if (activatedButtons&1){
      altView = !altView;
      }
    if (activatedButtons&2){
      //delete complete kv store
      //TODO: remove kv_reset("/kv/") line for final/when implementing next/previous for warning messages.
      kv_reset("/kv/");
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
  if (currentMillis - lastScreenUpdate >= screenInterval) {
    lastScreenUpdate = currentMillis;

    if (stateChanged){
      setStateData();
      stateChanged = false;
    }

    RelevantDisplayData displayData;
    dataMutex.lock();
    displayData.sensorData = globalSensorData;
    dataMutex.unlock();
    displayData.smoothString = smoothString;
    displayData.altView = altView;
    displayData.statusCode = statusCode;
        
    screenUpdateFunction(smoothIndex,displayData);
    smoothIndex++;
    if (smoothIndex > smoothString.length()*2+8){
      smoothIndex = 0;
    }
  }
}
