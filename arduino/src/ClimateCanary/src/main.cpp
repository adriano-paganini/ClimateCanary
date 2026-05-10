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
using namespace std::chrono_literals;
using namespace std::chrono_literals;

// ============================================================
// BLE / RTOS
// ============================================================
Thread bleThread;

bool bleClientConnected = false;
bool bleClientAuthenticated = false;
bool setupResetPending = false;
unsigned long setupResetAt = 0;

constexpr char NORMAL_NAME[] = "G5T4CC";
constexpr char SETUP_NAME[]  = "G5T4SETUP";

const char* const ID_KEY       = "ID";
const char* const INTERVAL_KEY = "INTERVAL";

// ============================================================
// Shared sensor data
// ============================================================
SensorData globalSensorData;
Mutex dataMutex;

// ============================================================
// Timing
// ============================================================
unsigned long lastButtonUpdate = 0;
const unsigned long buttonInterval = 50;      // update button states every 50 ms

unsigned long lastSensorUpdate = 0;
unsigned long sensorInterval = 3000;          // update sensor data every 1000 ms

unsigned long lastLightUpdate = 0;            // update LED using on/off timing

unsigned long lastScreenUpdate = 0;
const unsigned long screenInterval = 500;     // update screen every 500 ms

unsigned long lastSensorRead = 0;

// ============================================================
// RGB LED state
// ============================================================
unsigned long lightOnMs = 1;
unsigned long lightOffMs = 0;

uint8_t lightR = 0;
uint8_t lightG = 15;
uint8_t lightB = 240;

bool lightOn = false;

// ============================================================
// Screen state
// ============================================================
void (*screenUpdateFunction)(int smoothIndex, RelevantDisplayData data) = nullptr;

String smoothString = "";
int smoothIndex = 0;
bool altView = false;

// ============================================================
// Buttons
// ============================================================
ButtonState previousButtonState = {0, 0, 0};

// ============================================================
// Application state
// ============================================================
String currentState = "STARTUP";
bool stateChanged = false;

String roomName = "";
uint16_t statusCode = 0;

// ============================================================
// Warning / alert handling
// ============================================================
std::vector<String> currentWarningMessages;

// 0 inactive, 1 active, 2 completed transfer, 3 acknowledged
uint16_t warningStatus = 0;         
uint16_t warningMessageLength = 0;
uint16_t warningMessageAck = 0;

String warningMessageBuffer = "";
uint skipText = 1;

// ============================================================
// Buffer Management
// ============================================================
constexpr int16_t SENSOR_DATA_RING_BUFFER_SIZE = 1000;

SensorDataPacket sensorDataRingBuffer[SENSOR_DATA_RING_BUFFER_SIZE];

int16_t sensorDataRingBufferIndex = 0;
int16_t sensorDataRingBufferCount = 0;
int16_t sensorDataRingBufferInsertionCounter = 0;
int16_t sensorDataRingBufferTransmittedIndex = 0;
int16_t sensorDataRingBufferSendCount = 0;

void sensorDataRingBufferInsert(SensorData data) {
  Serial.println("Inserting new sensor data into ring buffer at index: " + String(sensorDataRingBufferIndex));

  SensorDataPacket packet;
  packet.timestamp = millis();
  packet.iaq = data.iaq;
  packet.temperature = data.temperature;
  packet.humidity = data.humidity;
  packet.pressure = data.pressure;

  sensorDataRingBuffer[sensorDataRingBufferIndex] = packet;

  sensorDataRingBufferIndex =
      (sensorDataRingBufferIndex + 1) % SENSOR_DATA_RING_BUFFER_SIZE;

  if (sensorDataRingBufferCount < SENSOR_DATA_RING_BUFFER_SIZE) {
    sensorDataRingBufferCount++;
  }else{
    sensorDataRingBuffer[sensorDataRingBufferIndex] = packet;
    sensorDataRingBufferIndex = (sensorDataRingBufferIndex + 1) % sensorDataRingBufferSize;
  }

}

void evaluateMeasurementStatus(BLEDevice central, BLECharacteristic characteristic) {
  if (!bleClientAuthenticated) {
    Serial.println("Unauthenticated client tried to write measurement status. Disconnecting...");
    central.disconnect();

    currentState = "WAITING_FOR_KNOWN_CONNECTION";
    stateChanged = true;
    return;
  }

  SensorStatusPacket packet;
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
  //0 = valid, 1 = short invalid low, 2 = short invalid high, 3 = long invalid low, 4 = long invalid high
  if (pressureStatus > 5 || temperatureStatus > 5 || humidityStatus > 5 || gasResistanceStatus > 5) {
    Serial.println("Invalid status code received.");
    return;
  }

  if (timestamp > millis()) {
    Serial.println("Invalid timestamp received.");
    return;
  }

  bool hasShortInvalid =
      ((pressureStatus > 0 && pressureStatus < 3) ||
       (temperatureStatus > 0 && temperatureStatus < 3) ||
       (humidityStatus > 0 && humidityStatus < 3) ||
       (gasResistanceStatus > 0 && gasResistanceStatus < 3));

  bool hasLongInvalid =
      ((pressureStatus > 2 && pressureStatus < 5) ||
       (temperatureStatus > 2 && temperatureStatus < 5) ||
       (humidityStatus > 2 && humidityStatus < 5) ||
       (gasResistanceStatus > 2 && gasResistanceStatus < 5));

  bool isLongValid =
      ((pressureStatus == 5) &&
       (temperatureStatus == 5) &&
       (humidityStatus == 5) &&
       (gasResistanceStatus == 5));

  statusCode = newStatusCode;

  if ((hasShortInvalid && warningStatus == 0)|| (hasLongInvalid && warningStatus == 1)|| (hasLongInvalid && warningStatus == 0)){
    currentState = "CONNECTED_SOME_SHORT_INVALID_DATA";
    stateChanged = true;
  }else if(warningStatus == 0){
    currentState = "CONNECTED_ALL_VALID_DATA";
    stateChanged = true;
  }else if(isLongValid && warningStatus != 1){
    if (warningStatus == 2){
      warningStatus = 3; //mark as acknowledged
    }
    if(warningStatus == 3){
      warningStatus = 0; //reset to inactive
      //communicate acknowledgment to app
      if(!warningAcknowledgedCharacteristic.writeValue(true)){
        Serial.println("Failed to set warning acknowledgment characteristic.");
      }
    }

    currentState = "CONNECTED_ALL_VALID_DATA";
    stateChanged = true;
  }
}

void onDeviceSetupConfigWritten(BLEDevice central, BLECharacteristic characteristic){
  DeviceSetupConfig config;
  int n = characteristic.readValue((byte*)&config, sizeof(config));
  if (n != sizeof(config)){
    Serial.println("Packet Malformed");
    return;
  }

  uint8_t measurementInterval = config.measurementInterval;
  uint32_t id = config.deviceId;

  if (measurementInterval < 3 || measurementInterval > 60) {
    Serial.print("Invalid measurement interval received: ");
    Serial.println(measurementInterval);
    Serial.println("Measurement interval must be between 3 and 60 seconds.");
    return;
  }


  Serial.print("Received new setup config: Measurement Interval - ");
  Serial.print(measurementInterval);
  Serial.print(", ID - ");
  Serial.println(id);

  int idResult = kv_set(ID_KEY, &id, sizeof(id), 0);
  int measurementIntervalResult = kv_set(
      INTERVAL_KEY,
      &measurementInterval,
      sizeof(measurementInterval),
      0
  );

  if (idResult != MBED_SUCCESS || measurementIntervalResult != MBED_SUCCESS){
    Serial.println("Failed to write config to KVStore. Restarting Pairing process...");
  }else{
    Serial.print("Keys written with values: ID - ");
    Serial.print(id);
    Serial.print(", Measurement Interval - ");
    Serial.println(measurementInterval);
    setupResetPending = true;
    setupResetAt = millis() + 2000;
  }
}

void bleFirstSetup(){
  currentState = "WAITING_FOR_NEW_CONNECTION";
  stateChanged = true;

  deviceSetupCharacteristic.setEventHandler(BLEWritten, onDeviceSetupConfigWritten);

  while(true){
    BLE.poll();

    if (setupResetPending && millis() >= setupResetAt) {
      NVIC_SystemReset();
    }

    ThisThread::sleep_for(10ms);
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
    return;
  }else if (idInfo.size != sizeof(idValue) || intervalInfo.size != sizeof(intervalValue)){
    Serial.println("Keys exists but have unexpected sizes. Restarting Pairing process...");
    NVIC_SystemReset();
    return;
  }

  int n = kv_get(INTERVAL_KEY, &intervalValue, sizeof(intervalValue), &intervalInfo.size);

  if (n != MBED_SUCCESS) {
    Serial.println("Failed to read measurement interval from KVStore. Restarting Pairing process...");
    NVIC_SystemReset();
    return;
  }

  Serial.println("Keys exist and have expected sizes. Continuing with normal execution...");
  sensorInterval = intervalValue * 1000;
  Serial.println("Measurement interval set to: " + String(sensorInterval) + " ms");
  BLE.stopAdvertise();

  bleClientConnected = true;

}

void onBleDisconnected(BLEDevice central) {
  bleClientConnected = false;
  bleClientAuthenticated = false;

  // reset all relevant variables
  roomName = "";
  smoothString = "";
  smoothIndex = 0;
  altView = false;
  statusCode = 0;
  warningStatus= 0;
  //not resetting, to avoid having to synchronize access
  //currentWarningMessages.clear();
  //write acknowledged
  if (!warningAcknowledgedCharacteristic.writeValue(false)) {
    Serial.println("Failed to reset warning acknowledgment.");
  }

  resetSensorDataRingBuffer();

  BLE.stopAdvertise();
  BLE.setDeviceName("G5T4CC");
  BLE.setLocalName("G5T4CC");
  BLE.advertise();

  currentState = "WAITING_FOR_KNOWN_CONNECTION";
  stateChanged = true;
}

void onAuthenticationPacketWritten(BLEDevice central, BLECharacteristic characteristic){
  DeviceAuthenticationPacket packet;
  int n = characteristic.readValue((byte*)&packet, sizeof(packet));
  if (n != sizeof(packet)) {
    Serial.println("Packet malformed");
    central.disconnect();
    return;
  }

  uint8_t len = packet.roomNameLength;
  if (len > sizeof(packet.roomName)) {
    Serial.println("roomNameLength out of range");
    central.disconnect();
    return;
  }

  uint32_t id = packet.deviceId;
  kv_info_t idInfo;
  int idResult = kv_get_info(ID_KEY, &idInfo);
  if (idResult != MBED_SUCCESS || idInfo.size != sizeof(id)) {
    Serial.println("ID Key invalid. Restarting Pairing process...");
    NVIC_SystemReset();
    return;
  }

  uint32_t storedId;
  int idReadResult = kv_get(ID_KEY, &storedId, sizeof(storedId), &idInfo.size);
  if (idReadResult != MBED_SUCCESS) {
    Serial.println("Failed to read ID from KVStore. Restarting Pairing process...");
    NVIC_SystemReset();
    return;
  }

  if (id != storedId) {
    Serial.println("Client provided wrong ID. Disconnecting...");
    central.disconnect();
    return;
  }

  char safeRoomName[33];
  memcpy(safeRoomName, packet.roomName, len);
  safeRoomName[len] = '\0';

  roomName = String(safeRoomName);
  bleClientAuthenticated = true;
  sensorDataRingBufferTransmittedIndex = 0;

  Serial.println("Client authenticated successfully.");
  Serial.print("Room Name: ");
  Serial.println(roomName);

  currentState = "CONNECTED_ALL_VALID_DATA";
  stateChanged = true;
}

void onWarningmessageLengthWritten(BLEDevice central, BLECharacteristic characteristic){
  if (!bleClientAuthenticated) {
    Serial.println("Unauthenticated client tried to write warning message length. Disconnecting...");
    central.disconnect();

    currentState = "WAITING_FOR_KNOWN_CONNECTION";
    stateChanged = true;
    return;
  }
  uint16_t newLength;
  int n = characteristic.readValue((byte*)&newLength, sizeof(newLength));
  if (n != sizeof(newLength)) {
    Serial.println("Packet malformed");
    return;
  }

  warningMessageLength = newLength;
  currentWarningMessages.clear();
  skipText = 1;
  warningStatus = 1;
  warningMessageAck = 0;
  warningMessageBuffer = "";
  currentState = "CONNECTED_ACTIVE_WARNING";
  stateChanged = true;   

  Serial.print("Received new warning message length: ");
  Serial.println(warningMessageLength);
}

void onCharPacketWritten(BLEDevice central, BLECharacteristic characteristic){
  if (!bleClientAuthenticated) {
    Serial.println("Unauthenticated client tried to write warning message chunk. Disconnecting...");
    central.disconnect();

    currentState = "WAITING_FOR_KNOWN_CONNECTION";
    stateChanged = true;
    return;
  }

  WarningMessageChunk packet;
  int n = characteristic.readValue((byte*)&packet,sizeof(packet));
  if (n != sizeof(packet)){
    Serial.println("Packet malformed");
    return;
  }
  switch(warningStatus){
    case 0:
      Serial.println("Received warning message packet without active warning. Ignoring...");
      return;
    case 1:
      {
        if (packet.sequenceNumber == warningMessageAck){
          if (packet.content == '\0'){
            currentWarningMessages.push_back(warningMessageBuffer);
            Serial.println(warningMessageBuffer);
            warningMessageBuffer = "";
          }else{
            warningMessageBuffer += packet.content;
            Serial.print("Received char: ");
            Serial.println(packet.content);
          }
          warningMessageAck++;
          if (warningMessageAck >= warningMessageLength){
            warningStatus = 2; //all messages received
            Serial.println("Completed receiving warning messages.");
          }
          }else{
            Serial.print("Received wrong sequence number:");
            Serial.println(packet.sequenceNumber);
            Serial.print("Expected sequence number:");
            Serial.println(warningMessageAck);
          }
          warningMessageAckRequestCharacteristic.writeValue(warningMessageAck); //request send of relevant packet
        }
      break;
    case 2:
      {
        Serial.println("Received warning message packet after completed transfer. Ignoring...");
        return;
      }
    case 3:
      {
        Serial.println("Received warning message packet after acknowledgment. Ignoring...");
        return;
      }
  }
}

void onWarningAcknowledgedWritten(BLEDevice central, BLECharacteristic characteristic){
  if (!bleClientAuthenticated) {
    Serial.println("Unauthenticated client tried to write warning acknowledgment. Disconnecting...");
    central.disconnect();

    currentState = "WAITING_FOR_KNOWN_CONNECTION";
    stateChanged = true;
    return;
  }

  bool acknowledged;
  int n = characteristic.readValue((byte*)&acknowledged, sizeof(acknowledged));
  if (n != sizeof(acknowledged)){
    Serial.println("Packet malformed");
    return;
  }
  if (acknowledged && warningStatus == 2){
    warningStatus = 3; //warning acknowledged
    Serial.println("Warning acknowledged by client.");
    currentState = "ACTIVE_WARNING_ACKNOWLEDGED";
    stateChanged = true;
  }else{
    Serial.println("Received unexpected warning acknowledgment. Resetting acknowledgment to false...");
    if (!warningAcknowledgedCharacteristic.writeValue(false)) {
      Serial.println("Failed to reset warning acknowledgment.");
    }
  }
}

void onCachedSensorDataAckWritten(BLEDevice central, BLECharacteristic characteristic) {
  if (!bleClientAuthenticated) {
    Serial.println("Unauthenticated client tried to write cached sensor data acknowledgment. Disconnecting...");
    central.disconnect();

    currentState = "WAITING_FOR_KNOWN_CONNECTION";
    stateChanged = true;
    return;
  }

  bool ack = false;
  int n = characteristic.readValue((byte*)&ack, sizeof(ack));
  if (n != sizeof(ack) || !ack) {
    Serial.println("Packet malformed or ACK was false");
    return;
  }

  if (sensorDataRingBufferCount == 0) {
    Serial.println("No cached sensor data available.");
    writeCachedTransferDone();
    return;
  }

  if (sensorDataRingBufferSendCount >= sensorDataRingBufferCount) {
    Serial.println("All cached sensor data packets have been acknowledged by the client.");
    writeCachedTransferDone();
    resetSensorDataRingBuffer();
    return;
  }

  int16_t packetIndex = sensorDataRingBufferTransmittedIndex;
  if (sensorDataRingBufferCount == sensorDataRingBufferSize) {
    packetIndex = (sensorDataRingBufferIndex + sensorDataRingBufferTransmittedIndex)
        % sensorDataRingBufferSize;
  }

  SensorDataPacket packetToSend = sensorDataRingBuffer[packetIndex];

  bool ok = cachedSensorDataCharacteristic.writeValue((byte*)&packetToSend, sizeof(packetToSend));
  Serial.println(String("cached write ok=") + (ok ? "true" : "false"));

  if (!ok) {
    Serial.println("Failed to update cached sensor data characteristic.");
    return;
  }

  Serial.print("Prepared cached sensor data packet with timestamp: ");
  Serial.println(packetToSend.timestamp);

  sensorDataRingBufferTransmittedIndex =
      (sensorDataRingBufferTransmittedIndex + 1) % SENSOR_DATA_RING_BUFFER_SIZE;

  sensorDataRingBufferSendCount++;
}

void bleTask() {
  currentState = "WAITING_FOR_KNOWN_CONNECTION";
  stateChanged = true;

  BLE.setEventHandler(BLEConnected, onBleConnected);
  BLE.setEventHandler(BLEDisconnected,onBleDisconnected);
  sensorDataStatusCharacteristic.setEventHandler(BLEWritten, evaluateMeasurementStatus);
  warningAuthCharacteristic.setEventHandler(BLEWritten, onAuthenticationPacketWritten);
  warningMessageLengthCharacteristic.setEventHandler(BLEWritten, onWarningmessageLengthWritten);
  warningMessageChunkCharacteristic.setEventHandler(BLEWritten, onCharPacketWritten);
  warningAcknowledgedCharacteristic.setEventHandler(BLEWritten, onWarningAcknowledgedWritten);
  cachedSensorDataAckCharacteristic.setEventHandler(BLEWritten, onCachedSensorDataAckWritten);

  uint32_t lastSend = 0;

  while (true) {
    BLE.poll();

    if (bleClientConnected && bleClientAuthenticated) {
      uint32_t now = millis();

      if (now - lastSend >= sensorInterval) {
        dataMutex.lock();
        sendSensorPacket(globalSensorData, sensorDataCharacteristic);
        dataMutex.unlock();

        lastSend = now;
      }
    }
    ThisThread::sleep_for(10ms);
  }
}

void setStateData(){
    if (currentState == "WAITING_FOR_KNOWN_CONNECTION"){
      smoothString = "Waiting for known connection...";
      screenUpdateFunction = &waitForKnownConnection;
      // warm amber pulse
      lightOnMs = 500;
      lightOffMs = 500;
      lightR = 255;
      lightG = 56;   // 140 * 0.40
      lightB = 11;   // 20 * 0.55

  }else if (currentState == "WAITING_FOR_AUTHENTICATION"){
      smoothString = "Waiting for authentication...";
      screenUpdateFunction = &waitForAuthenticatedConnection;
      // vivid aqua-green fast blink
      lightOnMs = 25;
      lightOffMs = 75;
      lightR = 0;
      lightG = 102;  // 255 * 0.40
      lightB = 99;   // 180 * 0.55

  }else if (currentState == "WAITING_FOR_NEW_CONNECTION"){
      smoothString = BLE.address();
      screenUpdateFunction = &waitForNewConnection;
      // bright pink-magenta blink
      lightOnMs = 125;
      lightOffMs = 125;
      lightR = 255;
      lightG = 24;   // 60 * 0.40
      lightB = 66;   // 120 * 0.55

  }else if (currentState == "CONNECTED_ALL_VALID_DATA"){
      smoothString = roomName;
      screenUpdateFunction = &connectedAllValidData;
      // soft turquoise solid
      lightOnMs = 1;
      lightOffMs = 0;
      lightR = 40;
      lightG = 102;  // 255 * 0.40
      lightB = 99;   // 180 * 0.55
      lightOn = false; // force update of light in next loop

  }else if (currentState == "CONNECTED_SOME_SHORT_INVALID_DATA"){
      smoothString = roomName;
      screenUpdateFunction = &connectedSomeShortInvalidData;
      // golden coral solid
      lightOnMs = 1;
      lightOffMs = 0;
      lightR = 255;
      lightG = 40;   // 100 * 0.40
      lightB = 22;   // 40 * 0.55
      lightOn = false; // force update of light in next loop

  }else if (currentState == "CONNECTED_ACTIVE_WARNING"){
      smoothString = "ACTIVE WARNINGS FOR: " + roomName;
      screenUpdateFunction = &connectedActiveWarning;
      // red urgent flash
      lightOnMs = 25;
      lightOffMs=75;
      lightR = 255;
      lightG = 0;
      lightB = 0;
  
  }else if( currentState == "ACTIVE_WARNING_ACKNOWLEDGED"){
      smoothString = roomName;
      screenUpdateFunction = &acknowledgedWarningsScreen;

      if (!warningAcknowledgedCharacteristic.writeValue(true)){
          Serial.println("Failed to set warning acknowledgment characteristic.");
      }

      // deep purple-red solid
      lightOnMs = 1;
      lightOffMs = 0;
      lightR = 120;
      lightG = 0;
      lightB = 50;   // 90 * 0.55
      lightOn = false; // force update of light in next loop
  }
}

void setup() {
  setupScreen();

  if (!setupSensors()) while (1){
    printScreen(0,"SUKA");
    String mills = String(millis());
    printScreen(1,mills);
    ThisThread::sleep_for(500ms);
  };
  setupButtons();
  setupLight();
  setColorRGB(lightR, lightG, lightB);
  Serial.begin(115200);
  kv_info_t idInfo;
  kv_info_t intervalInfo;
  uint32_t idValue;
  uint8_t intervalValue;

  int idResult = kv_get_info(ID_KEY, &idInfo);
  int intervalResult = kv_get_info(INTERVAL_KEY, &intervalInfo);

  if (idResult ==intervalResult && idResult == MBED_SUCCESS){
    if (idInfo.size != sizeof(idValue) || intervalInfo.size != sizeof(intervalValue)){
      Serial.println("Keys exists but have unexpected sizes. Restarting Pairing process...");
        if (!initialSetupBLE("G5T4SETUP","00RDY"))while (1);
        bleThread.start(bleFirstSetup);
    }else{
      Serial.println("Keys exist and have expected sizes. Continuing with normal execution...");
        if (!normalSetupBLE("G5T4CC","11LCK"))while (1);
        bleThread.start(bleTask);
    }
  }else{
    Serial.println("Keys do not exist. Starting Pairing process...");
      if (!initialSetupBLE("G5T4SETUP","00RDY"))while (1);
      bleThread.start(bleFirstSetup);
  }
  //sleep 2 seconds to ensure stable startup of sensors and ble
  ThisThread::sleep_for(2s);
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
    if ((activatedButtons&7) == 7){
      //delete kv pairs and restart entire arduino
      kv_reset("/kv/");
      Serial.println("Factory reset triggered by pressing all buttons. Restarting...");
      NVIC_SystemReset();
    }
    if(warningStatus == 2&& !altView){
      if (activatedButtons&2){
        if (skipText == static_cast<int16_t>(currentWarningMessages.size())){
          warningStatus = 3; //acknowledge warning after user skipped all warning messages
          skipText = 1;
          currentState = "ACTIVE_WARNING_ACKNOWLEDGED";
          stateChanged = true;
          Serial.println("User acknowledged warning by skipping all messages.");
        }else{
          skipText+=1;
        }
      }if (activatedButtons&4 && skipText>1){
        skipText-=1;
      }
    }
    if (warningStatus == 3 && !altView){
      if (activatedButtons&2){
        skipText+=1;
        if (skipText > static_cast<int16_t>(currentWarningMessages.size())){
          skipText = 1;
        }
      }if (activatedButtons&4){
        skipText-=1;
        if (skipText < 1){
          skipText = static_cast<int16_t>(currentWarningMessages.size());
        }
      }
    }
  }

  if (currentMillis - lastSensorRead >= 150){
    lastSensorRead = currentMillis;
    SensorData data = readSensors();
    dataMutex.lock();
    globalSensorData = data;
    dataMutex.unlock();
  }
  // get up-to-date sensor data in a given sensorInterval
  if (currentMillis - lastSensorUpdate >= sensorInterval) {
    lastSensorUpdate = currentMillis;
  if (!(bleClientConnected && bleClientAuthenticated) &&
      sensorDataRingBufferInsertionCounter % 5 == 0) {
      sensorDataRingBufferInsert(globalSensorData);
  }
    sensorDataRingBufferInsertionCounter++;
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
  }
}