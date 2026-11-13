/*
 * ============================================================================================
 * CÓDIGO DEFINITIVO WI-FI CON ESP32 - GRUPO G (UNIDAD 2 - TI3042 INACAP)
 * Proyecto: Estación Meteorológica Portátil IoT (Wi-Fi con ESP32 + Sensor DHT22)
 * ============================================================================================
 * PROBLEMAS DE LA UNIDAD 1 QUE ESTE CÓDIGO SOLUCIONA DE RAÍZ:
 * 1. FALLO DE BLUETOOTH SOLUCIONADO (MIGRACIÓN A WI-FI CON ESP32):
 *    Se eliminó la comunicación Bluetooth que fallaba y se implementó conexión inalámbrica
 *    Wi-Fi con ESP32 (<WiFi.h>), levantando un Servidor TCP en el puerto 8888 (con modo
 *    Punto de Acceso automático "Estacion_GrupoG_ESP32" si no encuentra red externa) para
 *    comunicarse de forma directa y estable con la Aplicación Móvil Android.
 *
 * 2. FALLO DE TEMPERATURA NEGATIVA SOLUCIONADO (CAMBIO DE DHT11 A DHT22):
 *    Se eliminó la conversión analógica inestable y el DHT11 básico, integrando el sensor
 *    digital calibrado DHT22 mediante la librería <DHT.h>, entregando Temperatura exacta en °C
 *    (sin valores negativos falsos) y Humedad Relativa (%).
 */

#include <Wire.h>
#include <LiquidCrystal_I2C.h>
#include <DHT.h>

// Selección automática de librería Wi-Fi para ESP32 (<WiFi.h>) o módulo ESP32-S3 integrado
#if defined(ESP32)
  #include <WiFi.h>
#else
  #include <WiFiS3.h>
#endif

// ============================================================================================
// 1. CONFIGURACIÓN DE RED WI-FI CON ESP32 (CONEXIÓN CON LA APP MÓVIL ANDROID DEL GRUPO G)
// ============================================================================================
const char* WIFI_SSID = "GrupoG_Hotspot";
const char* WIFI_PASS = "Meteo2026!";

WiFiServer servidorApp(8888); // Servidor TCP en el puerto 8888 que escucha a la App Android

// ============================================================================================
// 2. DEFINICIÓN DE PINES Y COMPONENTES REALES DEL PROTOTIPO (GRUPO G - ESP32)
// ============================================================================================
#define PIN_DHT22 4           // Pin Digital GPIO 4 conectado al pin DATA del sensor DHT22
#define TIPO_DHT  DHT22       // Modelo DHT22 (solución definitiva al fallo de temperatura negativa)

#if defined(ESP32)
  const int pinLDR    = 34;   // Entrada ADC1_CH6 (GPIO 34) en ESP32 para fotoresistor LDR
  const int pinBoton  = 18;   // Pulsador físico con INPUT_PULLUP (GPIO 18)
  const int pinBuzzer = 19;   // Actuador acústico Piezo Buzzer (GPIO 19)
#else
  const int pinLDR    = A1;
  const int pinBoton  = 2;
  const int pinBuzzer = 8;
#endif

DHT dht(PIN_DHT22, TIPO_DHT);
LiquidCrystal_I2C lcd(0x27, 16, 2);

// ============================================================================================
// 3. VARIABLES DE ESTADO Y TELEMETRÍA
// ============================================================================================
bool sistemaEncendido = true;
bool estadoAnteriorBoton = HIGH;
unsigned long tiempoAnterior = 0;
unsigned long intervaloMuestreo = 2500; // Variable dinámica: Modo Eco (4.5s), Continuo (2.5s), Emergencia (1s)

float tempC = 23.5;
float humedadPct = 55.0;
int nivelLuz = 450;
float umbralTempAlerta = 30.0; // Modificable desde el Slider de la App Android
int umbralLuzAlerta = 800;
bool alarmaRemotaForzada = false;
String modoActual = "Continuo Normal";

void setup() {
  Serial.begin(115200);

  pinMode(pinBuzzer, OUTPUT);
  pinMode(pinBoton, INPUT_PULLUP);

  // Inicialización del nuevo sensor digital DHT22
  dht.begin();

  // Inicialización de Pantalla LCD 16x2 I2C (En ESP32 SDA=GPIO 21, SCL=GPIO 22)
  lcd.init();
  lcd.backlight();

  lcd.setCursor(0, 0);
  lcd.print("Estacion Meteo");
  lcd.setCursor(0, 1);
  lcd.print("Grupo G - ESP32");
  delay(1500);

  // Inicialización de conectividad Wi-Fi con ESP32
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("WiFi ESP32...");

  int intentos = 0;
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  while (WiFi.status() != WL_CONNECTED && intentos < 6) {
    delay(1000);
    intentos++;
  }

  IPAddress ipAsignada;
  // Si no encuentra el Hotspot externo, el ESP32 crea su propia red Wi-Fi (IP: 192.168.4.1)
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

  // Mostramos la dirección IP del ESP32 en el LCD 16x2 para conectarse desde la App Android
  lcd.clear();
  lcd.setCursor(0, 0);
  lcd.print("IP WiFi ESP32:");
  lcd.setCursor(0, 1);
  lcd.print(ipAsignada);
  Serial.print("ESP32 Wi-Fi activo. Conectar App Android a IP: ");
  Serial.println(ipAsignada);
  delay(3000);
  lcd.clear();
}

void loop() {
  // 1. Lectura del Botón Físico Standby
  bool estadoBoton = digitalRead(pinBoton);
  if (estadoBoton == LOW && estadoAnteriorBoton == HIGH) {
    sistemaEncendido = !sistemaEncendido;
    lcd.clear();
    if (!sistemaEncendido) {
      lcd.setCursor(0, 0);
      lcd.print("MODO STANDBY");
      lcd.setCursor(0, 1);
      lcd.print("Grupo G - ESP32");
      digitalWrite(pinBuzzer, LOW);
    }
    delay(200);
  }
  estadoAnteriorBoton = estadoBoton;

  // 2. Recepción y respuesta inalámbrica Wi-Fi con ESP32 hacia la App Android (Puerto 8888)
  WiFiClient clienteApp = servidorApp.available();
  if (clienteApp) {
    clienteApp.setTimeout(1500);
    String tramaRecibida = clienteApp.readStringUntil('\n');
    tramaRecibida.trim();

    if (tramaRecibida.length() > 0) {
      Serial.println("Comando recibido desde App Android: " + tramaRecibida);

      if (tramaRecibida.indexOf("\"alarmaOn\":true") >= 0) {
        alarmaRemotaForzada = true;
      }
      if (tramaRecibida.indexOf("\"alarmaOn\":false") >= 0) {
        alarmaRemotaForzada = false;
        digitalWrite(pinBuzzer, LOW);
      }

      // Control remoto de la Luz de Fondo de la Pantalla LCD 16x2 I2C (Switch 2)
      if (tramaRecibida.indexOf("\"lcdBacklight\":true") >= 0) {
        lcd.backlight();
      }
      if (tramaRecibida.indexOf("\"lcdBacklight\":false") >= 0) {
        lcd.noBacklight();
      }

      // Control remoto de Modo Standby / Monitoreo Activo (Switch 3)
      if (tramaRecibida.indexOf("\"sistemaOn\":false") >= 0) {
        sistemaEncendido = false;
        lcd.clear();
        lcd.setCursor(0, 0);
        lcd.print("MODO STANDBY");
        lcd.setCursor(0, 1);
        lcd.print("Grupo G - ESP32");
        digitalWrite(pinBuzzer, LOW);
      } else if (tramaRecibida.indexOf("\"sistemaOn\":true") >= 0) {
        sistemaEncendido = true;
      }

      // Procesamiento de Modo de Operación (Modo Eco para ahorro de batería OT)
      if (tramaRecibida.indexOf("Eco") >= 0) {
        intervaloMuestreo = 4500;
        modoActual = "Eco Portatil";
        Serial.println("Modo Eco activado: Muestreo cada 4500 ms (Ahorro OT)");
      } else if (tramaRecibida.indexOf("Emergencia") >= 0) {
        intervaloMuestreo = 1000;
        modoActual = "Emergencia";
        Serial.println("Modo Emergencia activado: Muestreo cada 1000 ms");
      } else if (tramaRecibida.indexOf("Continuo") >= 0) {
        intervaloMuestreo = 2500;
        modoActual = "Continuo Normal";
        Serial.println("Modo Continuo Normal: Muestreo cada 2500 ms");
      }

      // Extraemos el umbral de temperatura si viene desde el Slider 1 de la App Android
      int posUmbral = tramaRecibida.indexOf("\"umbralTemp\":");
      if (posUmbral >= 0) {
        int finUmbral = tramaRecibida.indexOf(",", posUmbral);
        if (finUmbral > posUmbral) {
          float nuevoUmbral = tramaRecibida.substring(posUmbral + 13, finUmbral).toFloat();
          if (nuevoUmbral >= 15.0 && nuevoUmbral <= 50.0) {
            umbralTempAlerta = nuevoUmbral;
          }
        }
      }

      // Extraemos el umbral de luz LDR si viene desde el Slider 2 de la App Android
      int posLuz = tramaRecibida.indexOf("\"umbralLuz\":");
      if (posLuz >= 0) {
        int finLuz = tramaRecibida.indexOf(",", posLuz);
        if (finLuz > posLuz) {
          int nuevoUmbralLuz = tramaRecibida.substring(posLuz + 12, finLuz).toInt();
          if (nuevoUmbralLuz >= 100 && nuevoUmbralLuz <= 1023) {
            umbralLuzAlerta = nuevoUmbralLuz;
          }
        }
      }

      // Respondemos a la App Android con el paquete JSON de telemetría real del DHT22 y LDR
      String jsonRespuesta = "{";
      jsonRespuesta += "\"evento\":\"ACK_ESP32_WIFI\",";
      jsonRespuesta += "\"estacion\":\"GRUPO-G-ESP32-WIFI\",";
      jsonRespuesta += "\"temp\":" + String(tempC, 1) + ",";
      jsonRespuesta += "\"hum\":" + String(humedadPct, 1) + ",";
      jsonRespuesta += "\"ppm\":" + String(nivelLuz) + ",";
      jsonRespuesta += "\"umbralTemp\":" + String(umbralTempAlerta, 1) + ",";
      jsonRespuesta += "\"umbralLuz\":" + String(umbralLuzAlerta) + ",";
      jsonRespuesta += "\"modo\":\"" + modoActual + "\",";
      jsonRespuesta += "\"alarmaOn\":" + String((tempC > umbralTempAlerta || tempC < 3.0 || alarmaRemotaForzada) ? "true" : "false");
      jsonRespuesta += "}";

      clienteApp.println(jsonRespuesta);
    }
    clienteApp.stop();
  }

  // 3. Muestreo autónomo dinámico según Modo Operativo (Eco: 4.5s / Continuo: 2.5s / Emergencia: 1s)
  if (sistemaEncendido && (millis() - tiempoAnterior >= intervaloMuestreo)) {
    tiempoAnterior = millis();

    // Lectura digital del sensor DHT22 (elimina definitivamente las temperaturas negativas falsas)
    float lecturaTemp = dht.readTemperature();
    float lecturaHum  = dht.readHumidity();

    if (!isnan(lecturaTemp) && lecturaTemp > -40.0 && lecturaTemp < 80.0) {
      tempC = lecturaTemp;
    }
    if (!isnan(lecturaHum) && lecturaHum >= 0.0 && lecturaHum <= 100.0) {
      humedadPct = lecturaHum;
    }

    // Lectura del sensor LDR
    nivelLuz = analogRead(pinLDR);

    // Despliegue en Pantalla LCD 16x2 I2C
    lcd.setCursor(0, 0);
    lcd.print("T:");
    lcd.print(tempC, 1);
    lcd.print("C H:");
    lcd.print((int)humedadPct);
    lcd.print("%   ");

    lcd.setCursor(0, 1);
    if (tempC < 3.0) {
      lcd.print("!HELADA AGRICOLA!");
    } else if (tempC > umbralTempAlerta) {
      lcd.print("!CALOR! U:");
      lcd.print((int)umbralTempAlerta);
      lcd.print("C   ");
    } else {
      lcd.print("Luz:");
      lcd.print(nivelLuz);
      lcd.print(" ");
      lcd.print(modoActual.substring(0, 6));
      lcd.print("  ");
    }

    // Control de Alarma Sonora (Buzzer): Alerta por Golpe de Calor (> umbral) O Helada (< 3°C)
    if (tempC > umbralTempAlerta || tempC < 3.0 || nivelLuz > umbralLuzAlerta || alarmaRemotaForzada) {
      digitalWrite(pinBuzzer, HIGH);
    } else {
      digitalWrite(pinBuzzer, LOW);
    }
  }
}
