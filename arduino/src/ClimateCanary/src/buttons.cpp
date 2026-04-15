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

int checkButtonActivation(ButtonState previousState, ButtonState currentState){
    int activatedButtons = 0;
    if (currentState.v1 && !previousState.v1){
      activatedButtons += 1;
    }if (currentState.v2 && !previousState.v2){
      activatedButtons += 2;
    }if (currentState.v3 && !previousState.v3){
      activatedButtons += 4;
    }
    return activatedButtons;
}