#include "sensors.h"
#include "light.h"
#include "buttons.h"
#include "screen.h"


unsigned long lastButtonUpdate = 0;
const unsigned long buttonInterval = 100;

unsigned long lastSensorUpdate = 0;
const unsigned long sensorInterval = 250;

static unsigned long lastLightUpdate = 0;
const unsigned long lightInterval = 50;
const unsigned int lightSpeed = 5;


void setup() {
  Serial.begin(9600); 

  if (!setupSensors()) while (1);

  setupScreen();

  setupButtons();

  setupLight();
}



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

    printSensorScreen(data);
  }

  if (currentMillis - lastLightUpdate >= lightInterval) {
    lastLightUpdate = currentMillis;
    updateLight(lightSpeed);
  }

}
