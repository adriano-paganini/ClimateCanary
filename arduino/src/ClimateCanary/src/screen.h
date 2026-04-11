#ifndef SCREEN_H
#define SCREEN_H
#include <Arduino.h>
#include "buttons.h"
#include "sensors.h"
#include "ble.h"
#include <vector>

#include <rgb_lcd.h>
extern rgb_lcd lcd; 

struct RelevantDisplayData{
    SensorData sensorData;
    String smoothString;
    bool altView;
    uint16_t statusCode;
    std::vector<String> currentWarningMessages;
    int skipText;
};

void setupScreen();
void printScreen(int line, String text);
void printButtonScreen(ButtonState state);
void printSensorScreen(SensorData data);
void clearScreen();
void waitForNewConnection(int smoothIndex,RelevantDisplayData data);
void waitForAuthenticatedConnection(int smoothIndex, RelevantDisplayData data);
void waitForKnownConnection(int smoothIndex, RelevantDisplayData data);
void connectedAllValidData(int smoothIndex, RelevantDisplayData data);
void connectedSomeShortInvalidData(int smoothIndex, RelevantDisplayData data);
void connectedActiveWarning(int smoothIndex, RelevantDisplayData data);
#endif
