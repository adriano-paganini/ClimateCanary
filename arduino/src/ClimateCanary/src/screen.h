#ifndef SCREEN_H
#define SCREEN_H
#include <Arduino.h>
#include "buttons.h"
#include "sensors.h"
#include "ble.h"

#include <rgb_lcd.h>
extern rgb_lcd lcd; 

void setupScreen();
void printScreen(int line, String text);
void printButtonScreen(ButtonState state);
void printSensorScreen(SensorData data);
void clearScreen();
void waitForNewConnection( String smoothString, int smoothIndex);
void waitForKnownConnection( String smoothString, int smoothIndex);
#endif
