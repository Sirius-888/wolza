#include <WiFi.h>
#include <WiFiManager.h>      // tzapu/WiFiManager — install via Library Manager
#include <PubSubClient.h>     // knolleary/PubSubClient — install via Library Manager

// ---------- Wi-Fi / MQTT ----------
const char* MQTT_BROKER = "broker.hivemq.com";
const uint16_t MQTT_PORT = 1883;
const char* MQTT_TOPIC = "home/plants/moisture";

WiFiClient wifiClient;
PubSubClient mqtt(wifiClient);

// ---------- Sensor / pump ----------
const int moisturePin = 34;

// L9110 Դրայվերի պիները (ENA-ն հեռացված է)
const int IN1 = 18;
const int IN2 = 19;

// Չորության սահմանային արժեքը (raw ADC)
int dryValue = 2200;

// Raw ADC → percent mapping. Tune these to your sensor:
//   rawDry  = reading in fully dry air (≈ 4095 for many capacitive sensors)
//   rawWet  = reading in a glass of water (≈ 1000–1500)
const int rawDry = 4095;
const int rawWet = 1000;

unsigned long lastPublish = 0;
const unsigned long PUBLISH_INTERVAL_MS = 2000;

String mqttClientId;

void mqttReconnect() {
  while (!mqtt.connected()) {
    Serial.print("MQTT connecting...");
    if (mqtt.connect(mqttClientId.c_str())) {
      Serial.println(" connected");
    } else {
      Serial.print(" failed rc=");
      Serial.print(mqtt.state());
      Serial.println(" — retry in 2s");
      delay(2000);
    }
  }
}

void setup() {
  Serial.begin(115200);

  pinMode(moisturePin, INPUT);
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);

  // First boot: device creates AP "Alex-Setup". Connect with your phone,
  // open the captive portal, pick your home Wi-Fi. Creds are saved in flash.
  WiFiManager wm;
  if (!wm.autoConnect("Alex-Setup")) {
    Serial.println("WiFi failed — restarting");
    ESP.restart();
  }
  Serial.print("WiFi OK, IP: ");
  Serial.println(WiFi.localIP());

  mqttClientId = "alex-esp-" + String((uint32_t)ESP.getEfuseMac(), HEX);
  mqtt.setServer(MQTT_BROKER, MQTT_PORT);
}

void loop() {
  if (!mqtt.connected()) mqttReconnect();
  mqtt.loop();

  int moisture = analogRead(moisturePin);

  Serial.print("Moisture Level: ");
  Serial.println(moisture);

  if (moisture > dryValue) {
    Serial.println("Soil is DRY -> Water pump ON");
    digitalWrite(IN1, HIGH);
    digitalWrite(IN2, LOW);
  } else {
    Serial.println("Soil is WET -> Pump OFF");
    digitalWrite(IN1, LOW);
    digitalWrite(IN2, LOW);
  }

  unsigned long now = millis();
  if (now - lastPublish >= PUBLISH_INTERVAL_MS) {
    lastPublish = now;
    int percent = map(moisture, rawDry, rawWet, 0, 100);
    percent = constrain(percent, 0, 100);
    char payload[8];
    snprintf(payload, sizeof(payload), "%d", percent);
    if (mqtt.publish(MQTT_TOPIC, payload)) {
      Serial.print("MQTT -> ");
      Serial.print(MQTT_TOPIC);
      Serial.print(": ");
      Serial.println(payload);
    } else {
      Serial.println("MQTT publish failed");
    }
  }

  delay(100);
}
