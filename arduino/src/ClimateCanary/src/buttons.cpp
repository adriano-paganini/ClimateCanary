#include "buttons.h"

bool setupButtons(pin_size_t button1Pin, pin_size_t button2Pin, pin_size_t button3Pin) {
  pinMode(button1Pin, INPUT_PULLUP);
  pinMode(button2Pin, INPUT_PULLUP);
  pinMode(button3Pin, INPUT_PULLUP);

  return true;
}

ButtonState updateButtons(){
    ButtonState state;
    state.v1 = !digitalRead(D2);
    state.v2 = !digitalRead(D3);
    state.v3 = !digitalRead(D10);
    return state;
}