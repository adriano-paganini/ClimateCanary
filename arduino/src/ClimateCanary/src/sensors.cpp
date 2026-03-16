#include "sensors.h"

Adafruit_BME680 bme;

unsigned long bmeEndTime = 0;
bool bmeReading = false;
SensorData lastValidData = {0, 0, 0, 0};

void startBmeReading() {
  bmeEndTime = bme.beginReading();
  if (bmeEndTime != 0) {
    bmeReading = true;
  }
}

bool setupSensors() {
  if (!bme.begin()) {
    Serial.println(F("Could not find a valid BME680 sensor, check wiring!"));
    return false;
  }

  bme.setTemperatureOversampling(BME680_OS_8X);
  bme.setHumidityOversampling(BME680_OS_2X);
  bme.setPressureOversampling(BME680_OS_4X);
  bme.setIIRFilterSize(BME680_FILTER_SIZE_3);
  bme.setGasHeater(320, 150);

  startBmeReading();

  return true;
}


SensorData readSensors() {

    if (bmeReading && millis() >= bmeEndTime) {
        if (bme.endReading()) {
            lastValidData.temperature = bme.temperature;
            lastValidData.humidity = bme.humidity;
            lastValidData.pressure = bme.pressure; 
            lastValidData.gas_resistance = bme.gas_resistance;
            
            bmeReading = false;
            startBmeReading();
        }
    }
    
    return lastValidData;
}

bool bmeAsyncReadingReady(){
    return bmeReading && millis() >= bmeEndTime;
}