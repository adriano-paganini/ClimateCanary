#include "sensors.h"
#include "light.h"
#include "buttons.h"
#include "screen.h"
#include "ble.h"
#include <rtos.h>
#include "KVStore.h"
#include "kvstore_global_api.h"
#include <vector>
#include <string>

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

ButtonState previousButtonState = {0,0,0};

String currentState = "STARTUP";
bool stateChanged = false;
String roomName = "";
String smoothString = "";
int smoothIndex = 0;
bool altView = false;
uint16_t statusCode = 0;

std::vector<String> currentWarningMessages;
int16_t skipText = 0;


void beginMessageTransfer() {
  skipText = 0;
  uint16_t totalWarningLength = 0;
  int warnLen = warningMessageTotalLength.readValue(&totalWarningLength, sizeof(totalWarningLength));

  if (warnLen != sizeof(totalWarningLength)) {
    Serial.println("Message transfer failed: could not read total length.");
    return;
  }

  if (!warningMessageAck.writeValue((uint16_t)0)) {
    Serial.println("Failed to write first ACK");
    return;
  }

  currentWarningMessages.clear();

  Serial.print("Total warning length: ");
  Serial.println(totalWarningLength);

  if (totalWarningLength == 0) {
    return;
  }

  String tempString = "";

  for (uint16_t i = 0; i < totalWarningLength; ++i) {
    WarningMessageCharPack charPack;
    int charPackLen = warningMessageCharPack.readValue(&charPack, sizeof(charPack));

    if (charPackLen != sizeof(charPack)) {
      Serial.print("Failed to read char pack at index ");
      Serial.println(i);
      return;
    }

    if (charPack.sqn != i) {
      Serial.print("Sequence mismatch. Expected ");
      Serial.print(i);
      Serial.print(", got ");
      Serial.println(charPack.sqn);
      continue;
    }

    if (charPack.content == '\0') {
      currentWarningMessages.push_back(tempString);
      tempString = "";
    } else {
      tempString += charPack.content;
    }
  }

  // Save trailing message if stream did not end with '\0'
  if (tempString.length() > 0) {
    currentWarningMessages.push_back(tempString);
  }
}

bool isNewWarning3(uint16_t oldStatusCode, uint16_t newStatusCode) {
  for (int shift = 0; shift <= 12; shift += 4) {
    uint8_t oldNibble = (oldStatusCode >> shift) & 0x0F;
    uint8_t newNibble = (newStatusCode >> shift) & 0x0F;

    if (oldNibble != 3 && newNibble == 3) {
      return true;
    }
  }
  return false;
}

void evaluateMeasurementStatus(BLEDevice central, BLECharacteristic characteristic) {
  if (!bleClientAuthenticated) {
    Serial.println("Unauthenticated client tried to write measurement status. Disconnecting...");
    central.disconnect();

    currentState = "WAITING_FOR_KNOWN_CONNECTION";
    stateChanged = true;
    return;
  }

  SensorPacketStatus packet;
  int n = characteristic.readValue((byte*)&packet, sizeof(packet));
  if (n != sizeof(packet)) {
    Serial.println("Packet malformed");
    return;
  }

  const uint32_t timestamp = packet.timestamp;
  const uint16_t newStatusCode = packet.statusCode;

  Serial.println("Received new measurement status:");
  Serial.println(newStatusCode, BIN);

  const uint8_t pressureStatus =  newStatusCode & 0x000F;
  const uint8_t temperatureStatus = (newStatusCode >> 4) & 0x000F;
  const uint8_t humidityStatus  = (newStatusCode >> 8) & 0x000F;
  const uint8_t gasResistanceStatus = (newStatusCode >> 12) & 0x000F;

  if (pressureStatus > 4 || temperatureStatus > 4 || humidityStatus > 4 || gasResistanceStatus > 4) {
    Serial.println("Invalid status code received.");
    return;
  }

  if (timestamp > millis()) {
    Serial.println("Invalid timestamp received.");
    return;
  }

  const bool hasShortInvalid =
      ((pressureStatus > 0 && pressureStatus < 3) ||
       (temperatureStatus > 0 && temperatureStatus < 3) ||
       (humidityStatus > 0 && humidityStatus < 3) ||
       (gasResistanceStatus > 0 && gasResistanceStatus < 3));

  const bool hasActiveWarning =
      (pressureStatus == 3 ||
       temperatureStatus == 3 ||
       humidityStatus == 3 ||
       gasResistanceStatus == 3);

  // Start transfer only if a NEW warning appeared
  if (isNewWarning3(statusCode, newStatusCode)) {
    beginMessageTransfer();
  }

  statusCode = newStatusCode;

  if (hasActiveWarning) {
    currentState = "CONNECTED_ACTIVE_WARNING";
    stateChanged = true;
  } else if (hasShortInvalid) {
    currentWarningMessages.clear();
    skipText = 0;
    currentState = "CONNECTED_SOME_SHORT_INVALID_DATA";
    stateChanged = true;
  } else {
    currentWarningMessages.clear();
    skipText = 0;
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
    }else if (currentState == "CONNECTED_ACTIVE_WARNING"){
      smoothString = roomName;
      screenUpdateFunction = &connectedActiveWarning;
      //light on constantly full red - r=255, g=0, b=0
      lightOnMs = 1;
      lightOffMs=0;
      lightR = 255;
      lightG = 0;
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
      //TODO: Add logic to limit skipText-values
    if (activatedButtons&2){
      skipText +=1;
    }if (activatedButtons&4){
      skipText-=1;
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
    displayData.currentWarningMessages = currentWarningMessages;
    displayData.skipText=skipText;

    screenUpdateFunction(smoothIndex,displayData);
    smoothIndex++;
    if (smoothIndex > smoothString.length()*2+8){
      smoothIndex = 0;
    }
  }
}
