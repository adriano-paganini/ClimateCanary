#include "screen.h"

rgb_lcd lcd;

void setupScreen() {
  lcd.begin(16, 2);
  printScreen(0, "Climare Canary");
  printScreen(1, "Initializing...");
}

void rendersString(String smoothString, int smoothIndex){
  String scroll = smoothString + "    " + smoothString + "    ";
  int len = scroll.length();

  String doubled = scroll + scroll;
  printScreen(1, doubled.substring(smoothIndex, smoothIndex + 16));
}

void waitForNewConnection(String smoothString, int smoothIndex){
    printScreen(0,"ADVERTISING AS: ");
    rendersString(smoothString, smoothIndex);
}

void waitForKnownConnection(String smoothString, int smoothIndex){
    printScreen(0,"  DISCONNECTED ");
    rendersString(smoothString, smoothIndex);
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
void printSensorScreen(SensorData data){
    char line[17];
    snprintf(line, sizeof(line),
        "T%.0f H%.0f P%.0f G%.0f",
        data.temperature,
        data.humidity,
        data.pressure / 100.0,
        data.gas_resistance / 1000.0);
        printScreen(0, String(line));
};
void clearScreen(){
    lcd.clear();
}