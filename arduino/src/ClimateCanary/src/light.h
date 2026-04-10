#ifndef LIGHT_H
#define LIGHT_H

#include <Arduino.h>
#include "buttons.h"

bool setupLight(pin_size_t init_rPin=A0, pin_size_t init_gPin=A6, pin_size_t init_bPin=A7);
void setColorRGB(int r, int g, int b);

#endif