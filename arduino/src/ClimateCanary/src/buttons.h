#ifndef BUTTONS_H
#define BUTTONS_H

#include <Arduino.h>

struct ButtonState {
    int v1;
    int v2;
    int v3;
};

bool setupButtons(pin_size_t button1Pin=D2, pin_size_t button2Pin=D3, pin_size_t button3Pin=D10);
ButtonState updateButtons();

#endif
