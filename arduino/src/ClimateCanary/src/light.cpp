#include "light.h"

int r = 255;
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

  return true;
}

void updateLight(int speed) {

  analogWrite(rPin, r);
  analogWrite(gPin, g);
  analogWrite(bPin, b);
  
  for(int i= 0; i < speed; i++){
    if (r == 255 && g < 255 && b == 0) g++;
    else if (g == 255 && r > 0 && b == 0) r--;
    else if (g == 255 && b < 255 && r == 0) b++;
    else if (b == 255 && g > 0 && r == 0) g--;
    else if (b == 255 && r < 255 && g == 0) r++;
    else if (r == 255 && b > 0 && g == 0) b--;
  }
}
