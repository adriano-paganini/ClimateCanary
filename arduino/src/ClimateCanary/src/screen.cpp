#include "screen.h"

rgb_lcd lcd;

boolean setupScreen() {
  lcd.begin(16, 2);
  printScreen(0, "Climare Canary");
  printScreen(1, "Initializing...");
  return true;
}
void printScreen(int line, String text){
    lcd.setCursor(0, line);
    lcd.print("                ");
    lcd.setCursor(0, line);
    lcd.print(text);
}
void printButtonScreen(ButtonState state){
    bool val1 = state.v1;
    bool val2 = state.v2;
    bool val3 = state.v3;

    String line = "B:" + String(val1) + " " + String(val2) + " " + String(val3);
    printScreen(1, line);
}
void printSensorScreen(float temperature, float humidity, float pressure, float gas_resistance);
void clearScreen(){
    lcd.clear();
}