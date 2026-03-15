#include <Wire.h>
#include <Adafruit_Sensor.h>
#include "Adafruit_BME680.h"

#include "light.h"
#include "buttons.h"
#include "screen.h"

#define SEALEVELPRESSURE_HPA (1013.25)

Adafruit_BME680 bme;

int val1 = 0;  
int val2 = 0;  
int val3 = 0;

unsigned long bmeEndTime = 0;
bool bmeReading = false;

int rep_cnt = 0;

// put function declarations here:
int myFunction(int, int);


void setup() {
  Serial.begin(9600); 
//while (!Serial);
  Serial.println(F("BME680 async test"));

  if (!bme.begin()) {
    Serial.println(F("Could not find a valid BME680 sensor, check wiring!"));
    while (1);
  }

  bme.setTemperatureOversampling(BME680_OS_8X);
  bme.setHumidityOversampling(BME680_OS_2X);
  bme.setPressureOversampling(BME680_OS_4X);
  bme.setIIRFilterSize(BME680_FILTER_SIZE_3);
  bme.setGasHeater(320, 150);

  setupScreen();

  setupButtons();

  setupLight();
}

void loop() {

  // Start sensor measurement every 1000 ms
  if (rep_cnt % 1000 == 0 && !bmeReading) {
    bmeEndTime = bme.beginReading();
    if (bmeEndTime != 0) {
      bmeReading = true;
    }
  }

  // Finish measurement when ready
  if (bmeReading && millis() >= bmeEndTime) {
    if (bme.endReading()) {
    }

    bmeReading = false;
  }



  // ---- BUTTON READ every 100 ms ----
  if (rep_cnt % 100 == 0) {
    ButtonState state = updateButtons();
          // ---- SENSOR DISPLAY (line 0) ----

      char line[17];
      snprintf(line, sizeof(line),
               "T%.0f H%.0f P%.0f G%.0f",
               bme.temperature,
               bme.humidity,
               bme.pressure / 100.0,
               bme.gas_resistance / 1000.0);

      printScreen(0, String(line));

      // ---- BUTTON DISPLAY (line 1) ----
      printButtonScreen(state);

  }

  // ---- timing counter ----
  delay(10);
  rep_cnt += 10;

  if (rep_cnt > 5000) {
    rep_cnt = 10;
  }

  updateLight(10);
}

// put function definitions here:
int myFunction(int x, int y) {
  return x + y;
}