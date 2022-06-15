## Smart IR remote on ESP8266

This *ESP8266* based device allows you to control your lamp, TV and air conditioner from your smartphone *anywhere in the world*. For this project, I created an *Android app*. In order to set up the commands of your remotes, I also developed a *Web interface*. I printed the case for this project on a *3D printer*. **More details in the video below**.

[![Video about my project on YouTube](https://img.youtube.com/vi/lcF6zeZ8F9g/0.jpg)](https://www.youtube.com/watch?v=lcF6zeZ8F9g)

## Tools and frameworks that I used

[<img align="left" alt="ArduinoIDE" width="36px" src="https://raw.githubusercontent.com/github/explore/80688e429a7d4ef2fca1e82350fe8e3517d3494d/topics/arduino/arduino.png"/>](https://www.arduino.cc/en/software)
[<img align="left" alt="AndroidStudio" width="36px" src="https://img.icons8.com/color/344/android-studio--v3.png"/>](https://developer.android.com/studio)
[<img align="left" alt="Firebase" width="36px" src="https://raw.githubusercontent.com/github/explore/80688e429a7d4ef2fca1e82350fe8e3517d3494d/topics/firebase/firebase.png"/>](https://firebase.google.com)
[<img align="left" alt="Fusion360" width="36px" src="https://img.icons8.com/color/344/autodesk-fusion-360.png"/>](https://www.autodesk.com/products/fusion-360/overview?term=1-YEAR&tab=subscription)
</br>
</br>

## Arduino IDE libraries

+ [Firebase ESP8266](https://github.com/mobizt/Firebase-ESP8266)
+ [IR Remote ESP8266](https://github.com/crankyoldgit/IRremoteESP8266)
+ [Arduino JSON v6.15.2](https://github.com/bblanchon/ArduinoJson)

## Firebase

I also created a project on the [Firebase](https://firebase.google.com) platform. I have used the following Firebase tools:
+ [Firebase Authentication](https://firebase.google.com/docs/auth)
+ [Realtime Database](https://firebase.google.com/docs/database)

## Components needed to create a device

1. ESP8266 NodeMCU 0.9
2. IR receiver VS1838B
3. NPN transistor
4. 6x IR leds
5. button
6. 10 kOhm resistor