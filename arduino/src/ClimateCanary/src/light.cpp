#include "light.h"
#include "ble.h"

int r = 0;
int g = 0;
int b = 0;

pin_size_t rPin;
pin_size_t gPin;
pin_size_t bPin;

bool setupLight(pin_size_t init_rPin, pin_size_t init_gPin, pin_size_t init_bPin) {
  rPin = init_rPin;
  gPin = init_gPin;
  bPin = init_bPin;

  pinMode(rPin, OUTPUT);
  pinMode(gPin, OUTPUT);
  pinMode(bPin, OUTPUT);

  setColorRGB(0,255,0);

  return true;
}

void setColorRGB(int red, int green, int blue){
  if (red>255||green>255||blue>255){
    Serial.println("Value too high.");
    return;
  }
  r=red;
  g=green;
  b=blue;
  analogWrite(rPin,r);
  analogWrite(gPin,g);
  analogWrite(bPin,b);

  sendLedSetReadColor(r,g,b,ledSetReadColor);
}


void updateLightWithButtonState(ButtonState state){
  if (r <255)setColorRGB(state.v1*254, state.v2*254, state.v3*254);
  if (r == 0 && g == 0 && b== 0)setColorRGB(0,255,0);
}
