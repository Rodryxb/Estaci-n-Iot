/*
 * Copia sincronizada del firmware Wi-Fi con ESP32 + DHT22 para el Grupo G.
 */
#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <DHT.h>

#if defined(ESP32)
  #include <WiFi.h>
#else
  #include <WiFiS3.h>
#endif

const char* WIFI_SSID = "GrupoG_Hotspot";
const char* WIFI_PASS = "Meteo2026!";
WiFiServer servidorApp(8888);

#define PIN_DHT22 4
#define TIPO_DHT  DHT22

#if defined(ESP32)
  const int pinLDR    = 34;
  const int pinBoton  = 18;
  const int pinBuzzer = 19;
#else
  const int pinLDR    = A1;
  const int pinBoton  = 2;
  const int pinBuzzer = 8;
#endif

DHT dht(PIN_DHT22, TIPO_DHT);
LiquidCrystal_I2C lcd(0x27, 16, 2);

bool sistemaEncendido = true;
bool estadoAnteriorBoton = HIGH;
unsigned long tiempoAnterior = 0;
unsigned long intervaloMuestreo = 2500; // Modo Eco: 4500ms, Continuo: 2500ms, Emergencia: 1000ms
String modoActual = "Continuo Normal";

float tempC = 23.5;
float humedadPct = 55.0;
int nivelLuz = 450;
float umbralTempAlerta = 30.0;
int umbralLuzAlerta = 800;
bool alarmaRemotaForzada = false;

void setup() {
  Serial.begin(115200);
  pinMode(pinBuzzer, OUTPUT);
  pinMode(pinBoton, INPUT_PULLUP);
  dht.begin();
  lcd.init();
  lcd.backlight();

  int intentos = 0;
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  while (WiFi.status() != WL_CONNECTED && intentos < 6) {
    delay(1000);
    intentos++;
  }

  IPAddress ipAsignada;
  if (WiFi.status() != WL_CONNECTED) {
    #if defined(ESP32)
      WiFi.softAP("Estacion_GrupoG_ESP32", "Meteo2026!");
      ipAsignada = WiFi.softAPIP();
    #else
      WiFi.beginAP("Estacion_GrupoG_ESP32", "Meteo2026!");
      delay(1000);
      ipAsignada = WiFi.localIP();
    #endif
  } else {
    ipAsignada = WiFi.localIP();
  }

  servidorApp.begin();
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("IP WiFi ESP32:");
  lcd.setCursor(0, 1);
  lcd.print(ipAsignada);
  delay(3000);
  lcd.clear();
}

void loop() {
  bool estadoBoton = digitalRead(pinBoton);
  if (estadoBoton == LOW && estadoAnteriorBoton == HIGH) {
    sistemaEncendido = !sistemaEncendido;
    lcd.clear();
    if (!sistemaEncendido) {
      lcd.setCursor(0, 0);
      lcd.print("MODO STANDBY");
      digitalWrite(pinBuzzer, LOW);
    }
    delay(200);
  }
  estadoAnteriorBoton = estadoBoton;

  WiFiClient clienteApp = servidorApp.available();
  if (clienteApp) {
    clienteApp.setTimeout(1500);
    String trama = clienteApp.readStringUntil('\n');
    if (trama.length() > 0) {
      if (trama.indexOf("\"alarmaOn\":true") >= 0)  alarmaRemotaForzada = true;
      if (trama.indexOf("\"alarmaOn\":false") >= 0) { alarmaRemotaForzada = false; digitalWrite(pinBuzzer, LOW); }

      if (trama.indexOf("\"lcdBacklight\":true") >= 0)  lcd.backlight();
      if (trama.indexOf("\"lcdBacklight\":false") >= 0) lcd.noBacklight();

      if (trama.indexOf("\"sistemaOn\":false") >= 0) {
        sistemaEncendido = false;
        lcd.clear();
        lcd.setCursor(0, 0);
        lcd.print("MODO STANDBY");
        digitalWrite(pinBuzzer, LOW);
      } else if (trama.indexOf("\"sistemaOn\":true") >= 0) {
        sistemaEncendido = true;
      }

      if (trama.indexOf("Eco") >= 0) {
        intervaloMuestreo = 4500;
        modoActual = "Eco Portatil";
      } else if (trama.indexOf("Emergencia") >= 0) {
        intervaloMuestreo = 1000;
        modoActual = "Emergencia";
      } else if (trama.indexOf("Continuo") >= 0) {
        intervaloMuestreo = 2500;
        modoActual = "Continuo Normal";
      }

      int posUmbral = trama.indexOf("\"umbralTemp\":");
      if (posUmbral >= 0) {
        int finUmbral = trama.indexOf(",", posUmbral);
        if (finUmbral > posUmbral) {
          float nuevoUmbral = trama.substring(posUmbral + 13, finUmbral).toFloat();
          if (nuevoUmbral >= 15.0 && nuevoUmbral <= 50.0) {
            umbralTempAlerta = nuevoUmbral;
          }
        }
      }

      int posLuz = trama.indexOf("\"umbralLuz\":");
      if (posLuz >= 0) {
        int finLuz = trama.indexOf(",", posLuz);
        if (finLuz > posLuz) {
          int nuevoUmbralLuz = trama.substring(posLuz + 12, finLuz).toInt();
          if (nuevoUmbralLuz >= 100 && nuevoUmbralLuz <= 1023) {
            umbralLuzAlerta = nuevoUmbralLuz;
          }
        }
      }

      String json = "{\"evento\":\"ACK_ESP32_WIFI\",\"estacion\":\"GRUPO-G-ESP32-WIFI\",\"temp\":" + String(tempC, 1) +
                    ",\"hum\":" + String(humedadPct, 1) + ",\"ppm\":" + String(nivelLuz) + ",\"modo\":\"" + modoActual + "\"}";
      clienteApp.println(json);
    }
    clienteApp.stop();
  }

  if (sistemaEncendido && (millis() - tiempoAnterior >= intervaloMuestreo)) {
    tiempoAnterior = millis();
    float t = dht.readTemperature();
    float h = dht.readHumidity();
    if (!isnan(t)) tempC = t;
    if (!isnan(h)) humedadPct = h;
    nivelLuz = analogRead(pinLDR);

    lcd.setCursor(0, 0);
    lcd.print("T:"); lcd.print(tempC, 1); lcd.print("C H:"); lcd.print((int)humedadPct); lcd.print("%  ");
    lcd.setCursor(0, 1);
    if (tempC < 3.0) {
      lcd.print("!HELADA AGRICOLA!");
    } else if (tempC > umbralTempAlerta) {
      lcd.print("!CALOR! U:"); lcd.print((int)umbralTempAlerta); lcd.print("C   ");
    } else {
      lcd.print("Luz:"); lcd.print(nivelLuz); lcd.print(" "); lcd.print(modoActual.substring(0, 6)); lcd.print(" ");
    }

    if (tempC > umbralTempAlerta || tempC < 3.0 || nivelLuz > umbralLuzAlerta || alarmaRemotaForzada) {
      digitalWrite(pinBuzzer, HIGH);
    } else {
      digitalWrite(pinBuzzer, LOW);
    }
  }
}
