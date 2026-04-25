#ifndef SENSORS_H
#define SENSORS_H

#include <Arduino.h>
#include <Wire.h>
#include "bsec.h"
#define SEALEVELPRESSURE_HPA (1013.25)

extern Bsec bme;

struct SensorData {
  float temperature;
  float humidity;
  float iaq;
  float gas_resistance;
  uint8_t iaq_accuracy;
};

bool setupSensors();
SensorData readSensors();
bool bmeAsyncReadingReady();
#endif
