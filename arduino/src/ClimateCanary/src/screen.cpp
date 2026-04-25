#include "screen.h"

rgb_lcd lcd;

void setupScreen() {
  lcd.begin(16, 2);
  printScreen(0, "Climate Canary");
  printScreen(1, "Initializing...");
}

void rendersString(int smoothIndex, String smoothString, int line = 1){
  int normalizedIndex = smoothIndex % (smoothString.length() + 4); // +4 for the spaces added in scroll
  String scroll = smoothString + "    " + smoothString + "    ";
  String doubled = scroll + scroll;
  printScreen(line, doubled.substring(normalizedIndex, normalizedIndex + 16));
}

void prettySensorScreen(int smoothIndex, SensorData data){
    char line1[17];
    char line2[17];

    float tempC = data.temperature;
    float iaq = data.iaq;
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
        snprintf(line1, sizeof(line1), "   Air Quality");
        snprintf(line2, sizeof(line2), "       %.0f,%d", iaq,data.iaq_accuracy);
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

void prettySensorScreenOneLine(int smoothIndex, SensorData data, int line){
    char line2[17];

    float tempC = data.temperature;
    float iaq = data.iaq;
    float humidityPct = data.humidity;
    float gasKOhm = data.gas_resistance / 1000.0;

    const int displayStep = 8;
    int screen = (smoothIndex / displayStep) % 4;

    switch (screen) {
    case 0:
        snprintf(line2, sizeof(line2), "Temperature: %.0fC", tempC);
        break;

    case 1:
        snprintf(line2, sizeof(line2), "Air Quality: %.0f", iaq);
        break;

    case 2:
        snprintf(line2, sizeof(line2), "Humidity: %.0f%%", humidityPct);
        break;

    case 3:
        snprintf(line2, sizeof(line2), "Gas: %.0f kOhm", gasKOhm);
        break;
    }
    printScreen(line, String(line2));
}

void prettySensorScreenOneLineActiveWarning(int smoothIndex, RelevantDisplayData data, int line){
    char line2[17];

    float tempC = data.sensorData.temperature;
    float iaq = data.sensorData.iaq;
    float humidityPct = data.sensorData.humidity;
    float gasKOhm = data.sensorData.gas_resistance / 1000.0;
    uint16_t statusCode = data.statusCode;
    const int displayStep = 8;
    int screen = (smoothIndex / displayStep) % 4;

    switch (screen) {
    case 0:
        if ((statusCode & 0x00F0)){
            snprintf(line2, sizeof(line2), "TEMPERATURE: %.0fC", tempC);
        }else{
            snprintf(line2, sizeof(line2), "Temperature: %.0fC", tempC);
        }
    break;

    case 1:
        if ((statusCode & 0x000F)){
            snprintf(line2, sizeof(line2), "AIR QUALITY: %.0f", iaq);
        }else{
            snprintf(line2, sizeof(line2), "Air Quality: %.0f", iaq);
        }
        break;
        
    case 2:
        if ((statusCode & 0x0F00)){
            snprintf(line2, sizeof(line2), "HUMIDITY: %.0f%%", humidityPct);
        }else{
            snprintf(line2, sizeof(line2), "Humidity: %.0f%%", humidityPct);
        }
        break;
    case 3:
        if ((statusCode & 0xF000)){
            snprintf(line2, sizeof(line2), "GAS: %.0f kOhm", gasKOhm);
        }else{
            snprintf(line2, sizeof(line2), "Gas: %.0f kOhm", gasKOhm);
        }
        break;
    }
    printScreen(line, String(line2));
}

void displayActiveWarnings(int smoothIndex, RelevantDisplayData data, int line){
    char line2[17];
    std::vector<String> activeWarnings;

    uint16_t statusCode = data.statusCode;
    const int displayStep = 8;

    uint8_t iaq =(statusCode & 0x000F);
    uint8_t temp =(statusCode & 0x00F0) >> 4;
    uint8_t humidity =(statusCode & 0x0F00) >> 8;
    uint8_t gas =(statusCode & 0xF000) >> 12;

    if (temp) {
        activeWarnings.push_back(String("TEMPERATURE ") + ((temp == 2) ||(temp==4)  ? "HIGH" : "LOW"));
    }
    if (iaq) {
        activeWarnings.push_back(String("AIR QUALITY ") + ((iaq == 2) ||(iaq==4)  ? "HIGH" : "LOW"));
    }
    if (humidity) {
        activeWarnings.push_back(String("HUMIDITY ") + ((humidity == 2) ||(humidity==4)  ? "HIGH" : "LOW"));
    }
    if (gas) {
        activeWarnings.push_back(String("GAS ") + ((gas == 2) ||(gas==4)  ? "HIGH" : "LOW"));
    }

    int screen = (smoothIndex / displayStep) % activeWarnings.size();
    snprintf(line2, sizeof(line2), "%s", activeWarnings[screen].c_str());
    printScreen(line, String(line2));
}

void connectedAllValidData(int smoothIndex, RelevantDisplayData data){
    if (data.altView){
        printScreen(0, "   NO ACTIVE");
        printScreen(1, "   WARNINGS!");
    }else{
        rendersString(smoothIndex, data.smoothString, 0);
        prettySensorScreenOneLine(smoothIndex, data.sensorData, 1);
    }
}

void acknowledgedWarningsScreen(int smoothIndex, RelevantDisplayData data){
    if (data.altView){
        rendersString(smoothIndex, data.smoothString,0);
        displayActiveWarnings(smoothIndex,data,1);
    }else{
        String titleScreen = "ACKNOWLEDGED WARNINGS FOR: " + data.smoothString + " "+data.skipText+"/"+String(data.currentWarningMessages.size());
        rendersString(smoothIndex, titleScreen,0);
        rendersString(smoothIndex, data.currentWarningMessages.at(data.skipText-1),1);
    }
}

void connectedActiveWarning(int smoothIndex, RelevantDisplayData data){
    if (data.altView){
        rendersString(smoothIndex,data.smoothString,0);
        displayActiveWarnings(smoothIndex,data,1);
    }else{
        printScreen(0,"WARNING "+String(data.skipText)+"/"+String(data.currentWarningMessages.size()));
        if (warningStatus!=2){
            rendersString(smoothIndex, "RECEIVING MESSAGES...",1);
        }else{
        rendersString(smoothIndex, data.currentWarningMessages.at(data.skipText-1),1);
        }
    }
}

void connectedSomeShortInvalidData(int smoothIndex, RelevantDisplayData data){
    if (data.altView){
        printScreen(0, "ACTIVE DANGERS!");
        displayActiveWarnings(smoothIndex, data, 1);
    }else{
        rendersString(smoothIndex, data.smoothString, 0);
        prettySensorScreenOneLineActiveWarning(smoothIndex, data, 1);
    }
}

void waitForKnownConnection(int smoothIndex, RelevantDisplayData data){
    if (data.altView){
        prettySensorScreen(smoothIndex, data.sensorData);
    }else{
        printScreen(0,"  DISCONNECTED ");
        rendersString(smoothIndex, data.smoothString, 1);
    }
}

void waitForAuthenticatedConnection(int smoothIndex, RelevantDisplayData data){
    if (data.altView){
        prettySensorScreen(smoothIndex, data.sensorData);
    }else{
        printScreen(0,"   CONNECTED ");
        rendersString(smoothIndex, data.smoothString, 1);
    }
}

void waitForNewConnection(int smoothIndex, RelevantDisplayData data){
    if (data.altView){
        prettySensorScreen(smoothIndex, data.sensorData);
    }else{
        printScreen(0,"ADVERTISING AS: ");
        rendersString(smoothIndex, data.smoothString, 1);
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
        data.iaq,
        data.gas_resistance / 1000.0);
        printScreen(0, String(line));
};

void clearScreen(){
    lcd.clear();
}