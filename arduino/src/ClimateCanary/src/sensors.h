#ifndef SENSORS_H
#define SENSORS_H
#include <Arduino.h>
#include "Adafruit_BME680.h"
#define SEALEVELPRESSURE_HPA (1013.25)

extern Adafruit_BME680 bme;

struct SensorData {
    float temperature;
    float humidity;
    float pressure;
    float gas_resistance;
};

bool setupSensors();
SensorData readSensors();

#endif