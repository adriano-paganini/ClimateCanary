#include "sensors.h"
#include "light.h"
#include "buttons.h"
#include "screen.h"
#include "ble.h"


unsigned long lastButtonUpdate = 0;
const unsigned long buttonInterval = 100;

unsigned long lastSensorUpdate = 0;
const unsigned long sensorInterval = 250;

static unsigned long lastLightUpdate = 0;
const unsigned long lightInterval = 5;
const unsigned int lightSpeed = 20;


void setupTest(){

  if (!setupSensors()) while (1);

  setupScreen();

  setupButtons();

  setupLight();
}

void testLoop(){
    unsigned long currentMillis = millis();

  if (currentMillis - lastButtonUpdate >= buttonInterval) {
    lastButtonUpdate = currentMillis;

    ButtonState state = updateButtons();

    printButtonScreen(state);
  }
  if (currentMillis - lastSensorUpdate >= sensorInterval) {
    lastSensorUpdate = currentMillis;

    SensorData data = readSensors();

    printSensorScreen(data);
  }

  if (currentMillis - lastLightUpdate >= lightInterval) {
    lastLightUpdate = currentMillis;
    updateLight(lightSpeed);
  }
}

// 1. Define a Service and a Characteristic with unique IDs (UUIDs)
// You can generate your own at uuidgenerator.net


void setup() {
  Serial.begin(9600);

  setupTest();

  // 2. Start the BLE stack:
  if (!setupBLE("BLE33_ClimateCanary_G5T4", arduinoTest,sensorDataCharacteristic)){
    Serial.println("Failed to initialize BLE!");
    while (1);
  }
}

void loop() {
  // 6. Listen for BLE centrals (your phone) to connect:
  BLEDevice central = BLE.central();
  if (central) {
    Serial.print("Connected to central: ");
    Serial.println(central.address());

    while (central.connected()) {
      SensorData data = readSensors();
      // 7. If a central is connected, write the value of the characteristic:
      RelativePacket packet;
      packet.ms_stamp = millis();
      packet.s1 = data.temperature;
      packet.s2 = data.humidity;
      packet.s3 = data.pressure;
      packet.s4 = data.gas_resistance;
      sensorDataCharacteristic.writeValue((uint8_t*)&packet, sizeof(RelativePacket)); // You can change this to send different data
      delay(1000); // Send data every second
    }

    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
  }
}
