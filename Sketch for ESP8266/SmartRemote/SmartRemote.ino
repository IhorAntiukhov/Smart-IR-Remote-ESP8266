#include <ESP8266WiFi.h> // библиотека для WiFi
#include <ESPAsyncTCP.h> // библиотека для асинхронных TCP запросов
#include <ESPAsyncWebServer.h> // библиотека для асинхронного Web сервера
#include <FirebaseESP8266.h> // библиотека для платформы Firebase

// аддоны для библиотеки для платформы Firebase
#include <addons/TokenHelper.h>
#include <addons/RTDBHelper.h>

#include <Arduino.h>
#include <IRremoteESP8266.h> // библиотека для ИК приёмника и ИК светодиода
#include <IRrecv.h> // библиотека для ИК приёмника
#include <IRsend.h> // библиотека для отправки ИК сигналов
#include <IRutils.h> // библиотека для работы с полученными ИК сигналами
#include <IRac.h> // библиотека для отправки команд кондиционера

#include <FS.h> // библиотека для SPIFFS
#include <ArduinoJson.h> // библиотека для JSON

#include "DumpACresults.h" // функции для декодирования и преобразования ИК протоколов

const uint16_t IR_RECEIVER_PIN = D5; // пин ИК приёмника
const uint16_t IR_LED_PIN = D2; // пин ИК светодиода
#define RESET_BUTTON_PIN D7 // пин кнопки для перехода в режим настройки параметров работы

String ssidName;  // название WiFi сети
String ssidPass;  // пароль WiFi сети
String userEmail; // почта пользователя
String userPass;  // пароль пользователя

bool settingsMode = false;

int lightRemoteButton = 0; // номер кнопки пульта освещения
int tvRemoteButton = 0; // номер кнопки пульта телевизора
bool acPageOpened = false; // переменная для того, открыта ли Web страница пульта кондиционера

String lightRemoteKeys[8]; // ИК команды пульта освещения
String lightRemoteId; // ID пульта освещения
String lightRemoteSaved; // переменная для того, сохранён ли пульт освещения в Firebase

bool lightState = false; // переменная для того, включен ли свет
int timerTime = 0; // время работы таймера выключения света в минутах

uint16_t *tvRemoteKeysSettings[13]; // ИК команды пульта телевизора, нужные при настройке пульта
uint16_t tvRemoteKeys[13][256]; // ИК команды пульта телевизора, нужные при их отправке
uint16_t tvRemoteKeyLength; // длина ИК команд пульта телевизора

String rgbRemoteKeys[24]; // ИК команды пульта RGB контроллера
bool rgbState = false; // переменная для того, включена ли RGB подсветка

String acRemoteProtocol; // протокол пульта кондиционера
int acModel; // модель пульта кондиционера

bool resetButtonFlag; // флажок кнопки для перехода в режим настройки параметров работы
bool espStartedNow = false; // переменная для того, запустилась ли только что плата ESP8266

unsigned long IRreceiverMillis = 0; // время последнего получения ИК сигнала в millis
unsigned long resetButtonMillis = 0; // время нажатия кнопки для перехода в режим настройки параметров работы в millis
unsigned long lightTimerMillis = 0; // время запуска таймера освещения в millis
unsigned long WiFiDisconnectedMillis = 0;

String lightRemoteData[2] = {"", ""};
int lightTimerData[2] = {0, 0};
String tvRemoteData[2] = {"", ""};
String rgbRemoteData[2] = {"", ""};
String acRemoteData[2] = {"", ""};
bool changeLightRemoteVariable = false;
bool changeLightTimerVariable = false;
bool changeTvRemoteVariable = false;
bool changeRgbRemoteVariable = false;
bool changeAcRemoteVariable = false;

FirebaseAuth firebaseAuth; // объект для работы с системой авторизации Firebase
FirebaseConfig firebaseConfig; // объект для настройки подключения к Firebase
FirebaseData firebaseData; // объект для работы с данными в Firebase
FirebaseData firebaseStream; // слушатель изменения данных в Firebase

IRrecv IRreceiver(IR_RECEIVER_PIN, 256, 30); // настраиваем ИК приёмник (пин, размер буфера, таймаут)
IRsend IRsender(IR_LED_PIN); // объект для управления ИК светодиодом
IRac acSender(IR_LED_PIN); // объект для отправки команд кондиционера
decode_results results; // объект для работы с данными, полученными от ИК приёмника

AsyncWebServer server(80); // объект для управления Web сервером для настройки параметров работы

// функция слушателя изменения данных в Firebase
void firebaseStreamCallback(MultiPathStreamData streamData) {
  if (espStartedNow) { // если слушатель сработал не при запуске платы
    if (streamData.get("/" + lightRemoteId + "/remoteButton")) { // если получена команда пульта освещения
      String remoteButton = String(streamData.value.c_str());
      changeLightRemoteVariable = !changeLightRemoteVariable;
      if (changeLightRemoteVariable) {
        lightRemoteData[0] = remoteButton;
      } else {
        lightRemoteData[1] = remoteButton;
      }
      if (lightRemoteData[0] != lightRemoteData[1]) {
        if (remoteButton.indexOf("OnOff") != -1) { // если получена команда на включение/выключение освещения
          lightState = !lightState;
          IRsender.sendNEC((uint64_t)strtoull((const char*)lightRemoteKeys[0].c_str(), 0, 16)); // отправляем ИК команду по протоколу NEC
          Serial.println("Отправили команду на включение/выключение освещения");
        }
        else if (remoteButton.indexOf("NightMode") != -1) { // если получена команда на переход в ночной режим
          IRsender.sendNEC((uint64_t)strtoull((const char*)lightRemoteKeys[1].c_str(), 0, 16));
          Serial.println("Отправили команду на включение ночного режима");
        }
        else if (remoteButton.indexOf("IncreaseBrightness") != -1) { // если получена команда на повышение яркости
          IRsender.sendNEC((uint64_t)strtoull((const char*)lightRemoteKeys[2].c_str(), 0, 16));
          Serial.println("Отправили команду на увеличение яркости");
        }
        else if (remoteButton.indexOf("DecreaseBrightness") != -1) { // если получена команда на понижение яркости
          IRsender.sendNEC((uint64_t)strtoull((const char*)lightRemoteKeys[3].c_str(), 0, 16));
          Serial.println("Отправили команду на уменьшение яркости");
        }
        else if (remoteButton.indexOf("IncreaseTemperature") != -1) { // если получена команда на повышение температуры света
          IRsender.sendNEC((uint64_t)strtoull((const char*)lightRemoteKeys[4].c_str(), 0, 16));
          Serial.println("Отправили команду на увеличение температуры света");
        }
        else if (remoteButton.indexOf("DecreaseTemperature") != -1) {
          IRsender.sendNEC((uint64_t)strtoull((const char*)lightRemoteKeys[5].c_str(), 0, 16)); // если получена команда на понижение температуры света
          Serial.println("Отправили команду на уменьшение температуры света");
        }
        else if (remoteButton.indexOf("Memory") != -1) {
          IRsender.sendNEC((uint64_t)strtoull((const char*)lightRemoteKeys[6].c_str(), 0, 16)); // если получена команда на запоминание параметров освещения
          Serial.println("Отправили команду на запоминание параметров лампы");
        }
        else if (remoteButton.indexOf("ColdColor") != -1) {
          IRsender.sendNEC((uint64_t)strtoull((const char*)lightRemoteKeys[7].c_str(), 0, 16)); // если получена команда на изменение режима температуры света
          Serial.println("Отправили команду на включение холодного света");
        }
      } else {
        Serial.println("Слушатель сработал случайно");
      }
    }

    if (streamData.get("/" + lightRemoteId + "/timerTime")) { // если получена команда на запуск таймера освещения
      timerTime = streamData.value.toInt(); // записываем время работы таймера
      changeLightTimerVariable = !changeLightTimerVariable;
      if (changeLightTimerVariable) {
        lightTimerData[0] = timerTime;
      } else {
        lightTimerData[1] = timerTime;
      }
      if (lightTimerData[0] != lightTimerData[1]) {
        if (timerTime != 0) { // если таймер запущен
          Serial.println("Таймер освещения запущен! Время работы таймера: " + String(timerTime));
          lightTimerMillis = millis(); // записываем время запуска таймера
          if (!lightState) { // если освещение выключено
            lightState = true;
            IRsender.sendNEC((uint64_t)strtoull((const char*)lightRemoteKeys[0].c_str(), 0, 16)); // отправляем ИК команду на включение освещения
          }
        } else {
          Serial.println("Таймер освещения остановлен!");
          lightTimerMillis = 0;
        }
      } else {
        Serial.println("Слушатель сработал случайно");
      }
    }

    if (streamData.get("/tvRemoteButton")) { // если получена команда пульта телевизора
      String remoteButton = String(streamData.value.c_str());
      changeTvRemoteVariable = !changeTvRemoteVariable;
      if (changeTvRemoteVariable) {
        tvRemoteData[0] = remoteButton;
      } else {
        tvRemoteData[1] = remoteButton;
      }
      if (tvRemoteData[0] != tvRemoteData[1]) {
        if (remoteButton.indexOf("OnOff") != -1) { // если получена команда на включение/выключение телевизора
          IRsender.sendRaw(tvRemoteKeys[0], tvRemoteKeyLength, 38); // отправляем ИК команду (её данные) в "сыром" виде
          Serial.println("Отправили команду на включение/выключение телевизора");
        }
        else if (remoteButton.indexOf("TvDtv") != -1) { // если получена команда на переход в кабельное тв
          IRsender.sendRaw(tvRemoteKeys[1], tvRemoteKeyLength, 38);
          Serial.println("Отправили команду tv/dtv телевизора");
        }
        else if (remoteButton.indexOf("IncreaseVolume") != -1) { // если получена команда на повышение громкости
          IRsender.sendRaw(tvRemoteKeys[2], tvRemoteKeyLength, 38);
          Serial.println("Отправили команду на повышение громкости");
        }
        else if (remoteButton.indexOf("DecreaseVolume") != -1) { // если получена команда на понижение громкости
          IRsender.sendRaw(tvRemoteKeys[3], tvRemoteKeyLength, 38);
          Serial.println("Отправили команду на уменьшение громкости");
        }
        else if (remoteButton.indexOf("NextChannel") != -1) { // если получена команда на следующий канал
          IRsender.sendRaw(tvRemoteKeys[4], tvRemoteKeyLength, 38);
          Serial.println("Отправили команду на следующий канал");
        }
        else if (remoteButton.indexOf("PreviousChannel") != -1) { // если получена команда на предыдущий канал
          IRsender.sendRaw(tvRemoteKeys[5], tvRemoteKeyLength, 38);
          Serial.println("Отправили команду на предыдущий канал");
        }
        else if (remoteButton.indexOf("Mute") != -1) { // если получена команда на включение/выключение звука
          IRsender.sendRaw(tvRemoteKeys[6], tvRemoteKeyLength, 38);
          Serial.println("Отправили команду на выключение звука");
        }
        else if (remoteButton.indexOf("Source") != -1) { // если получена команда на открытие меню источника
          IRsender.sendRaw(tvRemoteKeys[7], tvRemoteKeyLength, 38);
          Serial.println("Отправили команду источник");
        }
        else if (remoteButton.indexOf("Up") != -1) { // если получена команда "вверх" в меню телевизора
          IRsender.sendRaw(tvRemoteKeys[8], tvRemoteKeyLength, 38);
          Serial.println("Отправили команду вверх");
        }
        else if (remoteButton.indexOf("Left") != -1) { // если получена команда "влево" в меню телевизора
          IRsender.sendRaw(tvRemoteKeys[9], tvRemoteKeyLength, 38);
          Serial.println("Отправили команду влево");
        }
        else if (remoteButton.indexOf("Right") != -1) { // если получена команда "вправо" в меню телевизора
          IRsender.sendRaw(tvRemoteKeys[10], tvRemoteKeyLength, 38);
          Serial.println("Отправили команду вправо");
        }
        else if (remoteButton.indexOf("Down") != -1) { // если получена команда "вниз" в меню телевизора
          IRsender.sendRaw(tvRemoteKeys[11], tvRemoteKeyLength, 38);
          Serial.println("Отправили команду вниз");
        }
        else if (remoteButton.indexOf("Screen") != -1) { // если получена команда на выбор источника, или же команда "ok"
          IRsender.sendRaw(tvRemoteKeys[12], tvRemoteKeyLength, 38);
          Serial.println("Отправили команду \"ok\"");
        }
      } else {
        Serial.println("Слушатель сработал случайно");
      }
    }

    if (streamData.get("/rgbRemoteButton")) { // если получена команда пульта RGB контроллера
      String remoteButton = String(streamData.value.c_str());
      changeRgbRemoteVariable = !changeRgbRemoteVariable;
      if (changeRgbRemoteVariable) {
        rgbRemoteData[0] = remoteButton;
      } else {
        rgbRemoteData[1] = remoteButton;
      }
      if (rgbRemoteData[0] != rgbRemoteData[1]) {
        if (remoteButton.indexOf("OnOff") != -1) { // если получена команда на включение/выключение RGB подсветки
          rgbState = !rgbState;
          if (rgbState) {
            IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[0].c_str(), 0, 16)); // отправляем ИК команду по протоколу NEC
            Serial.println("Отправили команду на включение RGB контроллера");
          } else {
            IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[1].c_str(), 0, 16)); // отправляем ИК команду по протоколу NEC
            Serial.println("Отправили команду на выключение RGB контроллера");
          }
        }
        else if (remoteButton.indexOf("IncreaseBrightness") != -1) { // если получена команда на повышение громкости
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[2].c_str(), 0, 16));
          Serial.println("Отправили команду на увеличение яркости");
        }
        else if (remoteButton.indexOf("DecreaseBrightness") != -1) { // если получена команда на понижение яркости
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[3].c_str(), 0, 16));
          Serial.println("Отправили команду на уменьшение яркости");
        }
        else if (remoteButton == "Red") { // если получена команда на включение красного света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[4].c_str(), 0, 16));
          Serial.println("Отправили команду на включение красного света");
        }
        else if (remoteButton == "Red2") { // если получена команда на включение второго красного света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[5].c_str(), 0, 16));
          Serial.println("Отправили команду на включение второго красного света");
        }
        else if (remoteButton == "Red3") { // если получена команда на включение третьего красного света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[6].c_str(), 0, 16));
          Serial.println("Отправили команду на включение третьего красного света");
        }
        else if (remoteButton == "Red4") { // если получена команда на включение оранжевого света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[7].c_str(), 0, 16));
          Serial.println("Отправили команду на включение оранжевого света");
        }
        else if (remoteButton == "Red5") { // если получена команда на включение жёлтого света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[8].c_str(), 0, 16));
          Serial.println("Отправили команду на включение желтого света");
        }
        else if (remoteButton == "Green") { // если получена команда на включение зелёного света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[9].c_str(), 0, 16));
          Serial.println("Отправили команду на включение зелёного света");
        }
        else if (remoteButton == "Green2") { // если получена команда на включение второго зелёного света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[10].c_str(), 0, 16));
          Serial.println("Отправили команду на включение второго зелёного света");
        }
        else if (remoteButton == "Green3") { // если получена команда на включение третьего зелёного света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[11].c_str(), 0, 16));
          Serial.println("Отправили команду на включение голубого света");
        }
        else if (remoteButton == "Green4") { // если получена команда на включение морского света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[12].c_str(), 0, 16));
          Serial.println("Отправили команду на включение морского света");
        }
        else if (remoteButton == "Green5") { // если получена команда на включение темно-бирюзового света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[13].c_str(), 0, 16));
          Serial.println("Отправили команду на включение темно-бирюзового света");
        }
        else if (remoteButton == "Blue") { // если получена команда на включение синего света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[14].c_str(), 0, 16));
          Serial.println("Отправили команду на включение синего света");
        }
        else if (remoteButton == "Blue2") { // если получена команда на включение второго синего света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[15].c_str(), 0, 16));
          Serial.println("Отправили команду на включение второго синего света");
        }
        else if (remoteButton == "Blue3") { // если получена команда на включение темно-фиолетового света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[16].c_str(), 0, 16));
          Serial.println("Отправили команду на включение темно-фиолетового света");
        }
        else if (remoteButton == "Blue4") { // если получена команда на включение фиолетового света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[17].c_str(), 0, 16));
          Serial.println("Отправили команду на включение фиолетового света");
        }
        else if (remoteButton == "Blue5") { // если получена команда на включение розового света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[18].c_str(), 0, 16));
          Serial.println("Отправили команду на включение розового света");
        }
        else if (remoteButton == "White") { // если получена команда на включение белого света
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[19].c_str(), 0, 16));
          Serial.println("Отправили команду на включение белого света");
        }
        else if (remoteButton == "Flash") { // если получена команда на включение режима "flash"
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[20].c_str(), 0, 16));
          Serial.println("Отправили команду на включение режима \"flash\"");
        }
        else if (remoteButton == "Fade") { // если получена команда на включение режима "fade"
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[21].c_str(), 0, 16));
          Serial.println("Отправили команду на включение режима \"fade\"");
        }
        else if (remoteButton == "Strobe") { // если получена команда на включение режима "strobe"
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[22].c_str(), 0, 16));
          Serial.println("Отправили команду на включение режима \"strobe\"");
        }
        else if (remoteButton == "Smooth") { // если получена команда на включение режима "smooth"
          IRsender.sendNEC((uint64_t)strtoull((const char*)rgbRemoteKeys[23].c_str(), 0, 16));
          Serial.println("Отправили команду на включение режима \"smooth\"");
        }
      } else {
        Serial.println("Слушатель сработал случайно");
      }
    }

    if (streamData.get("/acRemote")) { // если получены параметры работы кондиционера
      String acRemote = String(streamData.value.c_str());
      changeAcRemoteVariable = !changeAcRemoteVariable;
      if (changeAcRemoteVariable) {
        acRemoteData[0] = acRemote;
      } else {
        acRemoteData[1] = acRemote;
      }
      if (acRemoteData[0] != acRemoteData[1]) {
        Serial.println("Параметры кондиционера: " + String(acRemote));

        if (acRemote != "off") { // если кондиционер запущен
          // записываем индексы разделительных хештегов
          int secondHashIndex = acRemote.indexOf("#", acRemote.indexOf("#") + 1);
          int thirdHashIndex = acRemote.indexOf("#", secondHashIndex + 1);
          int fourthHashIndex = acRemote.indexOf("#", thirdHashIndex + 1);
          int fifthHashIndex = acRemote.indexOf("#", fourthHashIndex + 1);
          int sixthHashIndex = acRemote.indexOf("#", fifthHashIndex + 1);

          acSender.next.degrees = (acRemote.substring(0, acRemote.indexOf("#"))).toInt(); // устанавливаем температуру
          switch ((acRemote.substring(acRemote.indexOf("#") + 1, secondHashIndex)).toInt()) { // устанавливаем режим работы кондиционера
            case 0: acSender.next.mode = stdAc::opmode_t::kFan; break;  // обдув
            case 1: acSender.next.mode = stdAc::opmode_t::kCool; break; // холод
            case 2: acSender.next.mode = stdAc::opmode_t::kHeat; break; // обогрев
            case 3: acSender.next.mode = stdAc::opmode_t::kDry; break;  // осушение
            case 4: acSender.next.mode = stdAc::opmode_t::kAuto; break; // авто
          }
          switch ((acRemote.substring(secondHashIndex + 1, thirdHashIndex)).toInt()) { // устанавливаем скорость обдува
            case 0: acSender.next.fanspeed = stdAc::fanspeed_t::kMin; break;    // минимальная
            case 1: acSender.next.fanspeed = stdAc::fanspeed_t::kMedium; break; // средняя
            case 2: acSender.next.fanspeed = stdAc::fanspeed_t::kMax; break;    // максимальная
            case 3: acSender.next.fanspeed = stdAc::fanspeed_t::kAuto; break;   // авто
          }
          if (acRemote.substring(thirdHashIndex + 1, fourthHashIndex) == "true") { // устанавливаем то, запущен ли турбо режим (повышенная скорость обдува)
            acSender.next.turbo = true;
          } else {
            acSender.next.turbo = false;
          }
          if (acRemote.substring(fourthHashIndex + 1, fifthHashIndex) == "true") { // устанавливаем то, запущена ли функция swing по вертикали
            acSender.next.swingv = stdAc::swingv_t::kAuto;
          } else {
            acSender.next.swingv = stdAc::swingv_t::kOff;
          }
          if (acRemote.substring(fifthHashIndex + 1, sixthHashIndex) == "true") { // устанавливаем то, запущена ли функция swing по горизонтали
            acSender.next.swingh = stdAc::swingh_t::kAuto;
          } else {
            acSender.next.swingh = stdAc::swingh_t::kOff;
          }
          if (acRemote.substring(sixthHashIndex + 1, acRemote.length()) == "true") { // устанавливаем то, запущен ли режим сна
            acSender.next.sleep = true;
          } else {
            acSender.next.sleep = false;
          }
          acSender.next.power = true; // устанавливаем то, что кондиционер включен
        } else {
          acSender.next.power = false; // устанавливаем то, что кондиционер выключен
        }
        acSender.sendAc(); // отправляем ИК команду кондиционера
      } else {
        Serial.println("Слушатель сработал случайно");
      }
    }
  }
  espStartedNow = true;
}

// функция таймаута слушателя изменения данных в Firebase
void firebaseStreamTimeoutCallback(bool timeout) {
  if (timeout) {
    espStartedNow = false;
    Serial.println("Таймаут слушателя изменений узла пользователя истёк!");
  }
  if (!firebaseStream.httpConnected()) Serial.printf("Код ошибки: %d, Причина: %s\n\n", firebaseStream.httpCode(), firebaseStream.errorReason().c_str());
}

void setup() {
  Serial.begin(115200, SERIAL_8N1, SERIAL_TX_ONLY);

  pinMode(RESET_BUTTON_PIN, INPUT);

  if (!SPIFFS.begin()) { // запускаем SPIFFS
    Serial.println("Не удалось монтировать SPIFFS :(");
    return;
  }

  if (SPIFFS.exists("/Settings.txt")) { // если параметры работы настроены
    File settingsFile = SPIFFS.open("/Settings.txt", "r"); // открываем файл с параметрами работы
    if (settingsFile) {
      String settings;
      while (settingsFile.available()) settings = settingsFile.readString(); // записываем параметры работы
      settingsFile.close();

      Serial.println("Параметры работы: " + String(settings));

      // записываем индексы разделительных хештегов
      int secondHashIndex = settings.indexOf("#", settings.indexOf("#") + 1);
      int thirdHashIndex = settings.indexOf("#", secondHashIndex + 1);

      ssidName = settings.substring(0, settings.indexOf("#")); // записываем название вашей WiFi сети
      ssidPass = settings.substring(settings.indexOf("#") + 1, secondHashIndex); // записываем пароль вашей WiFi сети
      userEmail = settings.substring(secondHashIndex + 1, thirdHashIndex); // записываем почту вашего пользователя
      userPass = settings.substring(thirdHashIndex + 1, settings.length()); // записываем пароль вашего пользователя

      Serial.println("Название WiFi сети: " + String(ssidName));
      Serial.println("Пароль WiFi сети: " + String(ssidPass));
      Serial.println("Почта пользователя: " + String(userEmail));
      Serial.println("Пароль пользователя: " + String(userPass));

      if (SPIFFS.exists("/LightRemote.txt")) { // если пульт освещения настроен
        File lightRemoteFile = SPIFFS.open("/LightRemote.txt", "r"); // открываем файл с ИК командами пульта освещения
        if (lightRemoteFile) {
          String lightRemote;
          while (lightRemoteFile.available()) lightRemote = lightRemoteFile.readString(); // записываем ИК команды пульта освещения
          lightRemoteFile.close();

          Serial.println("Содержимое файла пульта освещения: " + String(lightRemote));

          int indexCount = 0;
          int lastHashIndex = 0; // записываем индекс предыдущего разделительного символа
          for (int i = 0; i < 13; i++) { // записываем ИК команды в массив
            if (i == 0) {
              lightRemoteKeys[0] = lightRemote.substring(0, lightRemote.indexOf("#"));
            } else {
              lightRemoteKeys[i] = lightRemote.substring(lastHashIndex + 1, lightRemote.indexOf("#", lastHashIndex + 1));
            }
            lastHashIndex = lightRemote.indexOf("#", lastHashIndex + 1);
            indexCount++;
            if (indexCount == 8) break;
          }
          lightRemoteId = lightRemote.substring(lastHashIndex + 1, lightRemote.indexOf("#", lastHashIndex + 1));
          lastHashIndex = lightRemote.indexOf("#", lastHashIndex + 1);
          lightRemoteSaved = lightRemote.substring(lastHashIndex + 1, lightRemote.length());

          Serial.println("ID пульта освещения: " + String(lightRemoteId));
          Serial.println("Пульт освещения сохранён: " + String(lightRemoteSaved));
        } else {
          Serial.println("Не удалось открыть файл пульта освещения :(");
          return;
        }
      }

      if (SPIFFS.exists("/TvRemote0.txt")) { // если пульт телевизора настроен
        for (int i = 0; i <= 12; i++) { // открываем файлы с данными каждой ИК команды пульта телевизора
          File tvRemoteFile = SPIFFS.open("/TvRemote" + String(i) + ".txt", "r");
          if (tvRemoteFile) {
            while (tvRemoteFile.available()) {
              String tvRemote = tvRemoteFile.readString(); // записываем данные ИК команды
              DynamicJsonDocument doc(2048); // создаём JSON документ
              deserializeJson(doc, tvRemote); // конвертируем массив с данными ИК команды из JSON в int
              tvRemoteKeyLength = copyArray(doc, tvRemoteKeys[i]); // записываем размер данных ИК команды
            }
            tvRemoteFile.close(); // закрываем файл с данными ИК команды
          } else {
            Serial.println("Не удалось открыть файл пульта телевизора " + String(i) + " :(");
            return;
          }
        }
        Serial.println("Пульт телевизора прочитан!");
      }

      if (SPIFFS.exists("/RGBRemote.txt")) {
        File rgbRemoteFile = SPIFFS.open("/RGBRemote.txt", "r");
        if (rgbRemoteFile) {
          String rgbRemote;
          while (rgbRemoteFile.available()) rgbRemote = rgbRemoteFile.readString(); // записываем данные ИК команды
          rgbRemoteFile.close(); // закрываем файл с данными ИК команды

          int lastHashIndex = 0; // записываем индекс предыдущего разделительного символа
          for (int i = 0; i < 24; i++) { // записываем ИК команды в массив
            if (i == 0) {
              rgbRemoteKeys[0] = rgbRemote.substring(0, rgbRemote.indexOf("#"));
            } else {
              rgbRemoteKeys[i] = rgbRemote.substring(lastHashIndex + 1, rgbRemote.indexOf("#", lastHashIndex + 1));
            }
            lastHashIndex = rgbRemote.indexOf("#", lastHashIndex + 1);
          }
        } else {
          Serial.println("Не удалось открыть файл пульта rgb контроллера :(");
          return;
        }
        Serial.println("Пульт RGB контроллера прочитан!");
      }

      if (SPIFFS.exists("/AcRemote.txt")) { // если пульт кондиционера настроен
        File acRemoteFile = SPIFFS.open("/AcRemote.txt", "r"); // открываем файл с ИК протоколом и моделью пульта кондиционера
        if (acRemoteFile) {
          String acRemote;
          while (acRemoteFile.available()) acRemote = acRemoteFile.readString(); // записываем ИК протокол и модель пульта кондиционера
          acRemoteFile.close();

          acRemoteProtocol = acRemote.substring(0, acRemote.indexOf("#")); // записываем ИК протокол пульта кондиционера
          acModel = String(acRemote.charAt(acRemote.length() - 1)).toInt(); // записываем модель пульта кондиционера
          Serial.println("Протокол пульта кондиционера: " + String(acRemoteProtocol));
          Serial.println("Модель пульта кондиционера: " + String(acModel));

          acSender.next.protocol = getACprotocol(acRemoteProtocol); // устанавливаем ИК протокол пульта кондиционера
          acSender.next.model = acModel; // устанавливаем модель пульта кондиционера
          acSender.next.celsius = true; // устанавливаем то, будет ли температура указываться в градусах цельсия
          acSender.next.light = -1;
          acSender.next.beep = -1;
          acSender.next.econo = -1;
          acSender.next.filter = -1;
          acSender.next.quiet = -1;
          acSender.next.clean = -1;
          acSender.next.clock = -1;
        } else {
          Serial.println("Не удалось открыть файл пульта кондиционера :(");
          return;
        }
      }

      if (!digitalRead(RESET_BUTTON_PIN)) { // если кнопка для перехода в режим настройки параметров работы нажата
        for (int i = 0; i <= 3025; i++) { // проверяем на протяжении 3-ёх секунд, удерживается ли кнопка
          if (resetSettings()) break; // если кнопка удерживается 3 секунды, выходим из цикла
          delay(1);
        }
      }

      if (!settingsMode) { // если вы не перешли в режим настройки параметров работы
        IRsender.begin(); // запускаем ИК светодиод
        Serial.println("Подключаемся к " + String(ssidName) + " ...");
        WiFi.begin(ssidName.c_str(), ssidPass.c_str()); // подключаемся к вашей WiFi сети

        if (WiFi.waitForConnectResult() == WL_CONNECTED) { // если плата ESP8266 подключилась к WiFi сети
          Serial.print("Подключились к WiFi сети! Локальный IP адрес: "); Serial.println(WiFi.localIP());

          firebaseAuth.user.email = userEmail.c_str(); // устанавливаем почту вашего пользователя
          firebaseAuth.user.password = userPass.c_str(); // устанавливаем пароль вашего пользователя

          firebaseConfig.api_key = "AIzaSyAx01GbNCPKGYIJr-6TbCJlSgb1QtGmQ3A"; // устанавливаем api ключ базы данных Firebase
          firebaseConfig.database_url = "https://smartwifiremote-default-rtdb.europe-west1.firebaseDatabase.app/"; // устанавливаем url базы данных Firebase
          firebaseConfig.token_status_callback = tokenStatusCallback;

          Firebase.begin(&firebaseConfig, &firebaseAuth); // подключаемся к Firebase
          Firebase.reconnectWiFi(false);

          firebaseStream.setBSSLBufferSize(2048, 512); // устанавливаем размер буфера для BearSSL

          if (lightRemoteSaved == "0") { // если пульт освещения не сохранён в Firebase
            String uid = firebaseAuth.token.uid.c_str(); // записываем uid вашего пользователя
            // сохраняем узел для получения команды пульта освещения
            bool remoteButtonNode = Firebase.setString(firebaseData, ("/users/" + uid + "/" + lightRemoteId + "/remoteButton").c_str(), "0");
            bool timerTimeNode = Firebase.setInt(firebaseData, ("/users/" + uid + "/" + lightRemoteId + "/timerTime").c_str(), 0); // сохраняем узел для времени работы таймера
            if (remoteButtonNode && timerTimeNode) { // если оба узла успешно сохранены в Firebase
              lightRemoteSaved = "1";
              saveLightRemote(); // обновляем файл пульта освещения
            } else {
              Serial.println("Не удалось создать узел пульта освещения :(");
            }
          }

          if (Firebase.beginMultiPathStream(firebaseStream, ("/users/" + firebaseAuth.token.uid).c_str())) { // запускаем слушатель изменения данных в Firebase
            Firebase.setMultiPathStreamCallback(firebaseStream, firebaseStreamCallback, firebaseStreamTimeoutCallback); // устанавливаем функцию слушателя
          } else {
            Serial.printf("\nНе удалось установить слушатель получения команды пульта освещения, %s\n\n", firebaseStream.errorReason().c_str());
            return;
          }
        } else if (WiFi.waitForConnectResult() == WL_CONNECT_FAILED) {
          Serial.println("Не удалось подключиться к WiFi сети :(");
          return;
        }
      }
    } else {
      Serial.println("Не удалось открыть файл с параметрами работы :(");
      return;
    }
  } else { // если параметры работы не настроены
    settingsMode = true;
    beginWebServer(); // запускаем Web сервер для настройки параметров работы
  }
}

void loop() {
  if (lightRemoteButton != 0) { // если пользователь выбрал кнопку пульта освещения
    tvRemoteButton = 0;
    if (millis() - IRreceiverMillis >= 250) {
      IRreceiverMillis = millis();

      if (IRreceiver.decode(&results)) { // если ИК приёмник получил ИК сигнал
        String keyValue = String(results.value, HEX); // записываем ИК команду
        if (results.decode_type == 3 && keyValue != "ffffffffffffffff") { // если протокол ИК команды NEC и эта команда не является специальным кодом протокола NEC
          lightRemoteKeys[lightRemoteButton - 1] = keyValue;
          Serial.println("Ключ кнопки пульта освещения: " + String(keyValue));
        }
        IRreceiver.resume(); // ждём получения следующих ИК сигналов
      }
    }
  }

  if (tvRemoteButton != 0) { // если пользователь выбрал кнопку пульта телевизора
    lightRemoteButton = 0;
    if (IRreceiver.decode(&results)) { // если ИК приёмник получил ИК сигнал
      if (results.decode_type != -1 && results.decode_type != 47 && results.decode_type != 82) { // фильтруем неправильные, или ложные ИК сигналы
        tvRemoteKeysSettings[tvRemoteButton - 1] = resultToRawArray(&results); // записываем данные ИК команды в "сыром" виде
        tvRemoteKeyLength = getCorrectedRawLength(&results); // записываем размер ИК команды
        Serial.print("Ключ кнопки телевизора: ");
        serialPrintUint64(results.value, HEX); Serial.print("\n");
      }
      IRreceiver.resume(); // ждём получения следующих ИК сигналов
    }
  }

  if (acPageOpened) { // если открыта Web страница пульта кондиционера
    tvRemoteButton = 0;
    lightRemoteButton = 0;
    if (IRreceiver.decode(&results)) { // если ИК приёмник получил ИК сигнал
      if (results.decode_type != -1 && results.decode_type != 47 && results.decode_type != 82) { // фильтруем неправильные, или ложные ИК сигналы
        String description = IRAcUtils::resultAcToString(&results); // записываем описание ИК команды (параметры работы кондиционера)
        acRemoteProtocol = dumpACprotocol(results.decode_type); // записываем ИК протокол пульта кондиционера
        acModel = String(description.charAt(7)).toInt(); // записываем модель пульта кондиционера
        if (acModel == 0) acModel = 1; // если не удалось определить модель пульта кондиционера
        Serial.println("Получена команда кондиционера! Протокол: " + String(acRemoteProtocol) + " Модель: " + String(acModel));
      }
      IRreceiver.resume(); // ждём получения следующих ИК сигналов
    }
  }

  if (timerTime != 0) { // если таймер освещения запущен
    if (millis() - lightTimerMillis >= timerTime * 60000) { // запускаем таймер на установленое вами время
      timerTime = 0;
      lightState = false;
      IRsender.sendNEC((uint64_t)strtoull((const char*)lightRemoteKeys[0].c_str(), 0, 16)); // отправляем ИК команду на выключение освещения
      Serial.println("Время таймера освещения истекло!");
    }
  }

  if (WiFiDisconnectedMillis == 0) WiFiDisconnectedMillis = millis();
  if (!settingsMode && (WiFi.status() != WL_CONNECTED) && (millis() - WiFiDisconnectedMillis >= 5000)) {
    WiFiDisconnectedMillis = millis();
    Serial.println("Отключились от WiFi сети! Перезагружаем плату ...");
    ESP.restart();
  }
}

String processor(const String& var) { // функция для заполнения полей с параметрами работы, если пользователь до этого их настраивал
  String placeholder;
  if (var == "SSID_NAME") {
    placeholder = ssidName;
  } else if (var == "SSID_PASS") {
    placeholder = ssidPass;
  } else if (var == "USER_EMAIL") {
    placeholder = userEmail;
  } else if (var == "USER_PASS") {
    placeholder = userPass;
  }
  return placeholder;
}

String AcPageProcessor(const String& var) { // функция для отображения ИК протокола пульта кондиционера
  String protocol;
  if (var == "PROTOCOL") {
    protocol = "ИК протокол пульта: " + acRemoteProtocol;
  }
  return protocol;
}

void beginWebServer() { // функция для запуска Web сервера для настройки параметров работы
  assert(irutils::lowLevelSanityCheck() == 0);
  IRreceiver.enableIRIn(); // запускаем ИК приёмник
  WiFi.softAP("Умный Пульт", "12345678", 1, false, 1); // запускаем WiFi точку для настройки параметров работы (максимум одно подключение)

  server.on("/", HTTP_GET, [](AsyncWebServerRequest * request) { // если открыта основная страница
    request->send(SPIFFS, "/index.html", String(), false, processor); // отправляем HTML страницу из SPIFFS
    acPageOpened = false;

    // если параметры работы настроены
    if (request->hasArg("ssid") and request->hasArg("ssid_pass") and request->hasArg("user_email") and request->hasArg("user_pass")) {
      File settingsFile = SPIFFS.open("/Settings.txt", "w"); // открываем файл с параметрами работы
      if (settingsFile) {
        String settings = request->arg("ssid") + "#" + request->arg("ssid_pass") + "#" + request->arg("user_email") + "#" + request->arg("user_pass");
        Serial.println("Параметры работы: " + String(settings));
        if (settingsFile.print(settings)) { // сохраняем параметры работы в SPIFFS
          settingsFile.close();
          ESP.restart();
        } else {
          Serial.println("Не удалось сохранить файл с параметрами работы :(");
          return;
        }
      } else {
        Serial.println("Не удалось открыть файл с параметрами работы :(");
        return;
      }
    }
  });

  server.on("/indexStyle.css", HTTP_GET, [](AsyncWebServerRequest * request) {
    request->send(SPIFFS, "/indexStyle.css", "text/css"); // отправляем css файл основной страницы
  });

  server.on("/light_remote", HTTP_GET, [](AsyncWebServerRequest * request) { // если открыта страница пульта освещения
    request->send(SPIFFS, "/LightRemote.html", String(), false);
    acPageOpened = false;
  });

  server.on("/LightRemoteStyle.css", HTTP_GET, [](AsyncWebServerRequest * request) {
    request->send(SPIFFS, "/LightRemoteStyle.css", "text/css"); // отправляем css файл страницы пульта освещения
  });

  server.on("/tv_remote", HTTP_GET, [](AsyncWebServerRequest * request) { // если открыта страница пульта телевизора
    request->send(SPIFFS, "/TvRemote.html", String(), false);
    acPageOpened = false;
  });

  server.on("/TvRemoteStyle.css", HTTP_GET, [](AsyncWebServerRequest * request) {
    request->send(SPIFFS, "/TvRemoteStyle.css", "text/css"); // отправляем css файл страницы пульта телевизора
  });

  server.on("/ac_remote", HTTP_GET, [](AsyncWebServerRequest * request) { // если открыта страница пульта кондиционера
    request->send(SPIFFS, "/AcRemote.html", String(), false, AcPageProcessor);
    Serial.println("Страница пульта кондиционера открыта");
    acPageOpened = true;
  });

  server.on("/AcRemoteStyle.css", HTTP_GET, [](AsyncWebServerRequest * request) {
    request->send(SPIFFS, "/AcRemoteStyle.css", "text/css"); // отправляем css файл страницы пульта кондиционера
  });

  // устанавливаем слушатели получения HTTP заголовков для передачи того, на какую кнопку пульта освещения нажал пользователь
  server.on("/button_on_off", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Освещение: включение/выключение");
    lightRemoteButton = 1;
  });
  server.on("/button_night_mode", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Освещение: ночной режим");
    lightRemoteButton = 2;
  });
  server.on("/button_increase_brightness", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Освещение: повысить яркость");
    lightRemoteButton = 3;
  });
  server.on("/button_decrease_brightness", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Освещение: понизить яркость");
    lightRemoteButton = 4;
  });
  server.on("/button_increase_temperature", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Освещение: повысить температуру цвета");
    lightRemoteButton = 5;
  });
  server.on("/button_decrease_temperature", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Освещение: понизить температуру цвета");
    lightRemoteButton = 6;
  });
  server.on("/button_memory", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Освещение: запомнить параметры лампы");
    lightRemoteButton = 7;
  });
  server.on("/button_color_temperature", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Освещение: холодный свет");
    lightRemoteButton = 8;
  });
  server.on("/button_light_remote_id", HTTP_GET, [](AsyncWebServerRequest * request) {
    lightRemoteId = request->arg("id");
    Serial.println("Пульт освещения сохранён! ID Пульта освещения: " + String(lightRemoteId));
    lightRemoteSaved = "0";
    saveLightRemote(); // сохраняем ИК команды пульта освещения в SPIFFS
  });

  // устанавливаем слушатели получения HTTP заголовков для передачи того, на какую кнопку пульта телевизора нажал пользователь
  server.on("/button_tv_on_off", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: включение/выключение");
    tvRemoteButton = 1;
  });
  server.on("/button_tv_dtv", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: tv/dtv");
    tvRemoteButton = 2;
  });
  server.on("/button_increase_volume", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: повысить громкость");
    tvRemoteButton = 3;
  });
  server.on("/button_decrease_volume", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: понизить громкость");
    tvRemoteButton = 4;
  });
  server.on("/button_next_channel", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: следующий канал");
    tvRemoteButton = 5;
  });
  server.on("/button_previous_channel", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: предыдущий канал");
    tvRemoteButton = 6;
  });
  server.on("/button_mute", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: убрать звук");
    tvRemoteButton = 7;
  });
  server.on("/button_source", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: источник");
    tvRemoteButton = 8;
  });
  server.on("/button_up", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: вверх");
    tvRemoteButton = 9;
  });
  server.on("/button_left", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: влево");
    tvRemoteButton = 10;
  });
  server.on("/button_right", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: вправо");
    tvRemoteButton = 11;
  });
  server.on("/button_down", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: вниз");
    tvRemoteButton = 12;
  });
  server.on("/button_screen", HTTP_GET, [](AsyncWebServerRequest * request) {
    Serial.println("Телевизор: экран");
    tvRemoteButton = 13;
  });
  server.on("/button_save_tv_remote", HTTP_GET, [](AsyncWebServerRequest * request) {
    tvRemoteButton = 0;
    for (int i = 0; i <= 12; i++) { // сохраняем данные каждой ИК команды в отдельный файл SPIFFS
      File tvRemoteFile = SPIFFS.open("/TvRemote" + String(i) + ".txt", "w"); // открываем файл с данными ИК команды
      if (tvRemoteFile) {
        DynamicJsonDocument doc(2048); // создаём JSON документ
        copyArray(tvRemoteKeysSettings[i], tvRemoteKeyLength, doc); // записываем массив с данными ИК команды в JSON документ
        char rawArrayBuffer[2048]; // создаём char массив для хранения массива с данными ИК команды
        serializeJson(doc, rawArrayBuffer); // записываем массив с данными ИК команды в char массив (конвертируем из JSON, фактически, в строку)
        if (!tvRemoteFile.print(String(rawArrayBuffer))) { // сохраняем данные ИК команды в SPIFFS
          Serial.println("Не удалось сохранить файл пульта телевизора " + String(i) + " :(");
          return;
        }
      } else {
        Serial.println("Не удалось открыть файл пульта телевизора " + String(i) + " :(");
        return;
      }
    }
    Serial.println("Пульт телевизора сохранён!");
  });

  server.on("/ir_protocol", HTTP_GET, [](AsyncWebServerRequest * request) { // если получен запрос на отправку ИК протокола пульта кондиционера
    request->send(200, "text/plain", String(acRemoteProtocol));
  });
  // если получен запрос на сохранение ИК протокола пульта кондиционера и его модели в SPIFFS
  server.on("/button_save_ac_remote", HTTP_GET, [](AsyncWebServerRequest * request) {
    File acRemoteFile = SPIFFS.open("/AcRemote.txt", "w"); // открываем файл с ИК протоколом и моделью пульта кондиционера
    if (acRemoteFile) {
      if (acRemoteFile.print(acRemoteProtocol + "#" + acModel)) { // записываем ИК протокол и модель пульта кондиционера в SPIFFS
        Serial.println("Пульт кондиционера сохранён!");
      } else {
        Serial.println("Не удалось открыть файл пульта кондиционера :(");
        return;
      }
    } else {
      Serial.println("Не удалось открыть файл пульта кондиционера :(");
      return;
    }
  });

  server.begin();
  Serial.println("Перешли в режим настройки параметров работы!\nАдрес Web сервера: 192.168.4.1");
}

// функция для проверки того, удерживается ли кнопка для перехода в режим настройки параметров работы дольше 3 секунд
bool resetSettings() {
  bool resetButtonState = !digitalRead(RESET_BUTTON_PIN);
  if (resetButtonState && !resetButtonFlag && millis() - resetButtonMillis >= 100) { // если кнопка нажата
    resetButtonMillis = millis(); // записываем время нажатия кнопки
    resetButtonFlag = true; // поднимаем флажок
    Serial.println("Кнопка сброса настроек нажата!");
  }

  if (!resetButtonState && resetButtonFlag && millis() - resetButtonMillis >= 250) { // если кнопка отпущена
    resetButtonMillis = millis(); // записываем время отпускания кнопки
    resetButtonFlag = false; // опускаем флажок
    Serial.println("Кнопка сброса настроек отпущена!");
    return false; // выходим из функции
  }

  if (resetButtonState && resetButtonFlag && millis() - resetButtonMillis >= 3000) { // если кнопка удерживается дольше 3 секунд
    settingsMode = true; // переходим в режим настройки параметров работы
    WiFi.mode(WIFI_AP); // переходим в режим WiFi точки
    beginWebServer(); // запускаем Web сервер для настройки параметров работы
    return true;
  }
  return false;
}

// функция для сохранения ИК команды пульта освещения в SPIFFS
void saveLightRemote() {
  lightRemoteButton = 0;

  File lightRemoteFile = SPIFFS.open("/LightRemote.txt", "w"); // открываем файл с ИК командами пульта освещения
  if (lightRemoteFile) {
    // записываем ИК команды пульта освещения
    if (!lightRemoteFile.print(lightRemoteKeys[0] + "#" + lightRemoteKeys[1] + "#" + lightRemoteKeys[2] + "#" + lightRemoteKeys[3]
                               + "#" + lightRemoteKeys[4] + "#" + lightRemoteKeys[5] + "#" + lightRemoteKeys[6] + "#" + lightRemoteKeys[7]
                               + "#" + lightRemoteId + "#" + lightRemoteSaved)) {
      Serial.println("Не удалось сохранить файл пульта освещения :(");
      return;
    }
  } else {
    Serial.println("Не удалось открыть файл пульта освещения :(");
    return;
  }
}
