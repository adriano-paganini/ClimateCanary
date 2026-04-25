#include "sensors.h"

Bsec bme;

SensorData lastValidData = {
  0.0,  // temperature
  0.0,  // humidity
  0.0,  // iaq
  0.0,  // gas_resistance
  0     // iaq_accuracy
};

bool newBsecDataAvailable = false;

void checkBsecStatus() {
  if (bme.bsecStatus < BSEC_OK) {
    Serial.print(F("BSEC error code: "));
    Serial.println(bme.bsecStatus);
  } else if (bme.bsecStatus > BSEC_OK) {
    Serial.print(F("BSEC warning code: "));
    Serial.println(bme.bsecStatus);
  }

  if (bme.bme68xStatus < BME68X_OK) {
    Serial.print(F("BME680 error code: "));
    Serial.println(bme.bme68xStatus);
  } else if (bme.bme68xStatus > BME68X_OK) {
    Serial.print(F("BME680 warning code: "));
    Serial.println(bme.bme68xStatus);
  }
}

bool setupSensors() {
  Wire.begin();

  Serial.println(F("Initializing BME680 with BSEC..."));

  bme.begin(BME68X_I2C_ADDR_LOW, Wire);
  checkBsecStatus();

  if (bme.bme68xStatus < BME68X_OK) {
    Serial.println(F("LOW address failed, trying HIGH address..."));

    bme.begin(BME68X_I2C_ADDR_HIGH, Wire);
    checkBsecStatus();

    if (bme.bme68xStatus < BME68X_OK) {
      Serial.println(F("Could not find BME680 sensor."));
      return false;
    }
  }

  bsec_virtual_sensor_t sensorList[] = {
    BSEC_OUTPUT_IAQ,
    BSEC_OUTPUT_SENSOR_HEAT_COMPENSATED_TEMPERATURE,
    BSEC_OUTPUT_SENSOR_HEAT_COMPENSATED_HUMIDITY,
    BSEC_OUTPUT_RAW_GAS
  };

  bme.updateSubscription(
    sensorList,
    sizeof(sensorList) / sizeof(sensorList[0]),
    BSEC_SAMPLE_RATE_LP
  );

  checkBsecStatus();

  if (bme.bsecStatus < BSEC_OK) {
    Serial.println(F("BSEC setup failed."));
    return false;
  }

  Serial.println(F("BME680/BSEC setup successful."));
  return true;
}

SensorData readSensors() {
  newBsecDataAvailable = false;

  if (bme.run()) {
    newBsecDataAvailable = true;

    lastValidData.temperature = bme.temperature;
    lastValidData.humidity = bme.humidity;
    lastValidData.gas_resistance = bme.gasResistance;

    if (bme.iaqAccuracy >= 2) {
      lastValidData.iaq = bme.iaq;
      lastValidData.iaq_accuracy = bme.iaqAccuracy;
    } else {
      lastValidData.iaq_accuracy = bme.iaqAccuracy;
    }

    Serial.println(F("----- BME680 / BSEC Reading -----"));

    Serial.print(F("Temperature: "));
    Serial.print(bme.temperature);
    Serial.println(F(" °C"));

    Serial.print(F("Humidity: "));
    Serial.print(bme.humidity);
    Serial.println(F(" %"));

    Serial.print(F("Gas resistance: "));
    Serial.print(bme.gasResistance);
    Serial.println(F(" Ohm"));

    Serial.print(F("Current IAQ: "));
    Serial.println(bme.iaq);

    Serial.print(F("IAQ accuracy: "));
    Serial.println(bme.iaqAccuracy);

    Serial.print(F("Stored reliable IAQ: "));
    Serial.println(lastValidData.iaq);

    Serial.print(F("Stored IAQ accuracy: "));
    Serial.println(lastValidData.iaq_accuracy);

    Serial.println(F("--------------------------------"));
  } else {
    checkBsecStatus();
  }

  return lastValidData;
}

bool bmeAsyncReadingReady() {
  return newBsecDataAvailable;
}