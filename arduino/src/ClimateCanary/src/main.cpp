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

void setup() {
  Serial.begin(9600);

  setupTest();

  if (!setupBLE("BLE33_ClimateCanary_G5T4", arduinoTest,sensorDataCharacteristic)){
    Serial.println("Failed to initialize BLE!");
    while (1);
  }
}

//TODO: Multithread BLE and screen-Display; Define speed of BLE updates; Create characteristic for config of arduino;

void loop() {
  BLEDevice central = BLE.central();
  if (central) {
    while (central.connected()) {
      SensorData data = readSensors();
      sendSensorData(data, millis(), sensorDataCharacteristic);
    }

    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
  }
}
