#include "screen.h"

rgb_lcd lcd;

void setupScreen() {
  lcd.begin(16, 2);
  printScreen(0, "Climate Canary");
  printScreen(1, "Initializing...");
}

void rendersString(int smoothIndex, String smoothString){
  String scroll = smoothString + "    " + smoothString + "    ";
  int len = scroll.length();

  String doubled = scroll + scroll;
  printScreen(1, doubled.substring(smoothIndex, smoothIndex + 16));
}

void prettySensorScreen(int smoothIndex, SensorData data){
    char line1[17];
    char line2[17];

    float tempC = data.temperature;
    float pressurehPa = data.pressure / 100.0;
    float humidityPct = data.humidity;
    float gasKOhm = data.gas_resistance / 1000.0;

    const int displayStep = 8;
    int screen = (smoothIndex / displayStep) % 4;

    switch (screen) {
    case 0:
        snprintf(line1, sizeof(line1), "   Temperature");
        snprintf(line2, sizeof(line2), "      %.0fC", tempC);
        break;

    case 1:
        snprintf(line1, sizeof(line1), "    Pressure");
        snprintf(line2, sizeof(line2), "    %.0f hPa", pressurehPa);
        break;

    case 2:
        snprintf(line1, sizeof(line1), "    Humidity");
        snprintf(line2, sizeof(line2), "      %.0f%%", humidityPct);
        break;

    case 3:
        snprintf(line1, sizeof(line1), "      Gas");
        snprintf(line2, sizeof(line2), "    %.0f kOhm", gasKOhm);
        break;
    }

    printScreen(0, String(line1));
    printScreen(1, String(line2));
}

void waitForKnownConnection(int smoothIndex, RelevantDisplayData data){
    if (data.altView){
        prettySensorScreen(smoothIndex, data.sensorData);
    }else{
        printScreen(0,"  DISCONNECTED ");
        rendersString(smoothIndex, data.smoothString);
    }
}

void waitForNewConnection(int smoothIndex, RelevantDisplayData data){
    if (data.altView){
        prettySensorScreen(smoothIndex, data.sensorData);
    }else{
        printScreen(0,"ADVERTISING AS: ");
        rendersString(smoothIndex, data.smoothString);
    }   
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