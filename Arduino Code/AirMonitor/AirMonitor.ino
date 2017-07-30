#include <CurieBLE.h>
const unsigned char ledPin = 13;              // On-board LED

BLEPeripheral blePeripheral;  
BLEService tempService("fef431b0-51e0-11e7-9598-0800200c9a67"); // Create custom BLE Service
BLEUnsignedIntCharacteristic  tempChar("fef431b0-51e0-11e7-9598-0800200c9a66", BLERead | BLENotify); // allows remote device to get notifications
int oldTemp = 0;          // last temperature reading from analog input
long previousMillis = 0;  // last time the temperature was checked, in ms

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  pinMode(ledPin, OUTPUT);  // initialize the LED on pin 13 to indicate when a central is connected

  // initialize the BLE hardware
  BLE.begin();
  blePeripheral.setLocalName("Air Monitor");
  blePeripheral.setAdvertisedServiceUuid(tempService.uuid());    // add the service UUID
  blePeripheral.addAttribute(tempService);
  blePeripheral.addAttribute(tempChar);
  tempChar.setValue(oldTemp);               // initial value for this characteristic
//  BLE.advertise();

  Serial.println("BLE Central - LED control");

  blePeripheral.begin();
}

void loop() {
  // put your main code here, to run repeatedly:
  BLEDevice central = BLE.central();

  // if a central is connected to peripheral:
  if (central) {
    Serial.print("Connected to central: "); Serial.println(central.address());  // print the central's MAC address
    digitalWrite(ledPin, HIGH);  // turn on the LED to indicate the connection

    // check the temperature every 5000ms as long as the central is still connected:
    while (central.connected()) {
      long currentMillis = millis();
      if (currentMillis - previousMillis >= 1000) {
        previousMillis = currentMillis;
          updateAirQual();
      }
    }
    digitalWrite(ledPin, LOW);   // when the central disconnects, turn off the LED.
    Serial.print("Disconnected from central: "); Serial.println(central.address());
  }

}

void updateAirQual() {
  //Set quality from 0 to 255, with one to 100 being normal
  int sensorValue = analogRead(A0);
  int quality = map(sensorValue, 0, 1023, 0, 255);
  Serial.println(quality);
  tempChar.setValue(quality);
}

