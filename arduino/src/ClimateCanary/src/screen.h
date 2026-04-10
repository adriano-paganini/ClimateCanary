#ifndef SCREEN_H
#define SCREEN_H
#include <Arduino.h>
#include "buttons.h"
#include "sensors.h"
#include "ble.h"

#include <rgb_lcd.h>
extern rgb_lcd lcd; 

struct RelevantDisplayData{
    SensorData sensorData;
    String smoothString;
    bool altView;
};

void setupScreen();
void printScreen(int line, String text);
void printButtonScreen(ButtonState state);
void printSensorScreen(SensorData data);
void clearScreen();
void waitForNewConnection(int smoothIndex,RelevantDisplayData data);
void waitForKnownConnection(int smoothIndex, RelevantDisplayData);
#endif
