#include <WiFi.h>
#include <WiFiManager.h>
#include <PubSubClient.h>
#include <DHT.h>

// ---------- Wi-Fi / MQTT ----------
const char* MQTT_BROKER = "broker.hivemq.com";
const uint16_t MQTT_PORT = 1883;
const char* MQTT_TOPIC_MOISTURE    = "home/plants/moisture";
const char* MQTT_TOPIC_TEMPERATURE = "home/plants/temperature";
const char* MQTT_TOPIC_HUMIDITY    = "home/plants/humidity";
const char* MQTT_TOPIC_WATER       = "home/plants/water";

WiFiClient wifiClient;
PubSubClient mqtt(wifiClient);

// ---------- Moisture sensor / pump ----------
const int moisturePin = 34;
const int IN1 = 18;
const int IN2 = 19;
int dryValue = 2200;
const int rawDry = 4095;
const int rawWet = 1000;

// ---------- DHT22 ----------
#define DHT_PIN  4
#define DHT_TYPE DHT22
DHT dht(DHT_PIN, DHT_TYPE);

// ---------- Timing ----------
unsigned long lastPublish = 0;
const unsigned long PUBLISH_INTERVAL_MS = 2000;

// ---------- Pump manual timer variables ----------
bool pumpActive = false;
unsigned long pumpStartTime = 0;
const unsigned long PUMP_DURATION_MS = 7000; // 7 seconds duration

String mqttClientId;

// MQTT subscription callback function
void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("MQTT Command [");
  Serial.print(topic);
  Serial.print("]: ");
  
  String msg = "";
  for (int i = 0; i < length; i++) {
    msg += (char)payload[i];
  }
  Serial.println(msg);

  if (strcmp(topic, MQTT_TOPIC_WATER) == 0) {
    if (msg == "water" || msg == "1") {
      Serial.println("Watering command received! Turning pump ON for 7 seconds.");
      pumpActive = true;
      pumpStartTime = millis();
      digitalWrite(IN1, HIGH);
      digitalWrite(IN2, LOW);
    }
  }
}

void mqttReconnect() {
  while (!mqtt.connected()) {
    Serial.print("MQTT connecting...");
    if (mqtt.connect(mqttClientId.c_str())) {
      Serial.println(" connected");
      mqtt.subscribe(MQTT_TOPIC_WATER);
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

  dht.begin();

  WiFiManager wm;
  if (!wm.autoConnect("Alex-Setup")) {
    Serial.println("WiFi failed — restarting");
    ESP.restart();
  }
  Serial.print("WiFi OK, IP: ");
  Serial.println(WiFi.localIP());

  mqttClientId = "alex-esp-" + String((uint32_t)ESP.getEfuseMac(), HEX);
  mqtt.setServer(MQTT_BROKER, MQTT_PORT);
  mqtt.setCallback(callback);
}

void loop() {
  if (!mqtt.connected()) mqttReconnect();
  mqtt.loop();

  // --- Moisture read ---
  int moisture = analogRead(moisturePin);
  Serial.print("Moisture raw: ");
  Serial.println(moisture);

  // --- Pump control logic ---
  if (pumpActive) {
    // Check if 7 seconds elapsed
    if (millis() - pumpStartTime >= PUMP_DURATION_MS) {
      Serial.println("7 seconds elapsed -> Pump OFF");
      digitalWrite(IN1, LOW);
      digitalWrite(IN2, LOW);
      pumpActive = false;
    }
  } else {
    // Fallback to normal automatic moisture-based pump control
    if (moisture > dryValue) {
      Serial.println("Soil is DRY -> Pump ON");
      digitalWrite(IN1, HIGH);
      digitalWrite(IN2, LOW);
    } else {
      Serial.println("Soil is WET -> Pump OFF");
      digitalWrite(IN1, LOW);
      digitalWrite(IN2, LOW);
    }
  }

  // --- Publish every PUBLISH_INTERVAL_MS ---
  unsigned long now = millis();
  if (now - lastPublish >= PUBLISH_INTERVAL_MS) {
    lastPublish = now;

    // Moisture %
    int percent = map(moisture, rawDry, rawWet, 0, 100);
    percent = constrain(percent, 0, 100);
    char mPayload[8];
    snprintf(mPayload, sizeof(mPayload), "%d", percent);
    mqtt.publish(MQTT_TOPIC_MOISTURE, mPayload);
    Serial.printf("MQTT moisture -> %s\n", mPayload);

    // Temperature & Humidity (DHT22 needs ~250 ms to sample; non-blocking here
    // because we only read every 2 s which is well above that limit)
    float temp = dht.readTemperature();   // Celsius
    float hum  = dht.readHumidity();

    if (isnan(temp) || isnan(hum)) {
      Serial.println("DHT22 read failed — check wiring");
    } else {
      char tPayload[8], hPayload[8];
      snprintf(tPayload, sizeof(tPayload), "%.1f", temp);
      snprintf(hPayload, sizeof(hPayload), "%.1f", hum);

      mqtt.publish(MQTT_TOPIC_TEMPERATURE, tPayload);
      mqtt.publish(MQTT_TOPIC_HUMIDITY,    hPayload);

      Serial.printf("MQTT temp    -> %s °C\n", tPayload);
      Serial.printf("MQTT humidity-> %s %%\n", hPayload);
    }
  }

  delay(100);
}
