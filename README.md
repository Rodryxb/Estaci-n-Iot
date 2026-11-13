# Estación IoT — Sistema Móvil de Monitoreo y Control Meteorológico

Sistema móvil desarrollado en **Android nativo (Kotlin + Jetpack Compose / Material Design 3)** orientado a resolver la problemática de monitoreo ambiental y control remoto en terreno para entornos agrícolas e industriales.

El proyecto permite a los operadores de campo supervisar variables climáticas críticas en tiempo real (temperatura, humedad, radiación lumínica y calidad de aire), gestionar actuadores físicos a distancia y mantener una comunicación segura y sincronizada entre múltiples dispositivos móviles y estaciones IoT basadas en microcontroladores (**ESP32 / Arduino UNO R4 WiFi**).

---

## Problemática y Solución Implementada

En faenas agrícolas e invernaderos, las heladas repentinas y los golpes de calor provocan pérdidas significativas si no se detectan y controlan a tiempo. Los sistemas tradicionales de monitoreo suelen ser fijos, complejos de configurar y carecen de interfaces móviles accesibles para el personal operativo.

Para dar solución a este requerimiento se diseñó una solución integral que consta de:

1. **Terminal Portátil de Operación:** Aplicación Android que transforma cualquier smartphone en un panel de control industrial de bolsillo.
2. **Estación IoT en Terreno:** Microcontrolador (ESP32 / Arduino UNO R4 WiFi) con sensores ambientales (DHT22, LDR, MQ), display local y actuadores (alarma acústica buzzer y relés de iluminación/ventilación).
3. **Interconexión Inalámbrica Local (Wi-Fi / TCP / UDP):** Comunicación directa en red local o zona portátil (Hotspot) sin depender de servidores en la nube ni conexión a internet externa.

---

## Módulos de la Aplicación

### 1. Acceso y Autenticación de Operadores
Control de acceso mediante usuario y contraseña con cifrado unidireccional **SHA-256 con Salt industrial** y control de acceso basado en roles (**Admin** y **Operador de Campo**). Incluye bloqueo automático ante intentos fallidos reiterados.

* **Credenciales configuradas por defecto:**
  * Administrador: `admin` / clave `1234`
  * Operador de Campo: `operador_ot` / clave `Meteo2026!`
  * Supervisor: `inacap` / clave `ti3042`

### 2. Panel de Monitoreo de Sensores
Visualización continua de las lecturas ambientales de la estación:
* **Temperatura:** Sensor DHT22 con cálculo de alertas de helada (< 3.0°C) y sobrecalentamiento (> 30.0°C).
* **Humedad Relativa:** Detección de rangos óptimos de cultivo (30% - 75%).
* **Iluminación:** Medición lumínica LDR para control de radiación solar y sombreado.
* **Diagnóstico de Hardware:** Estado de enlace, nivel de batería de la estación y estado de actuadores.

### 3. Activador y Mando Remoto
Permite intervenir y enviar comandos directos a la estación meteorológica y sincronizarlos con otros terminales móviles:
* **Mensajes en Pantalla:** Envío de texto personalizado al display local de la estación.
* **Interruptores de Actuadores:** Activación y silenciamiento de la alarma sonora (Piezo Buzzer) y control de retroiluminación LCD.
* **Ajuste de Umbrales:** Deslizadores configurables para disparar alertas automáticas de temperatura y luminosidad.
* **Régimen de Operación:** Modos Eco Portátil, Continuo Normal y Emergencia Climática.

### 4. Conectividad e Interconexión entre Dispositivos
* **Autodescubrimiento Automático:** Localización de estaciones y teléfonos en la red local mediante paquetes UDP Broadcast en un solo toque, evitando que el usuario deba escribir direcciones IP manualmente.
* **Enlace TCP Directo:** Intercambio bidireccional de tramas de telemetría y comandos en tiempo real.
* **Soporte Bluetooth BLE:** Detección y vinculación con dispositivos inalámbricos de proximidad.
* **Métricas de Estabilidad:** Registro de latencia de enlace, jitter y tasa de entrega de paquetes para asegurar la disponibilidad operativa en campo.

### 5. Seguridad y Auditoría Industrial
* **Cifrado en Tránsito:** Paquetes JSON protegidos con **AES-256 (CBC)** para evitar intercepciones en redes inalámbricas abiertas.
* **Integridad de Datos:** Firma hash SHA-256 que previene la adulteración de comandos (anti-tampering).
* **Bitácora de Eventos:** Registro cronológico de inicios de sesión, cambios de estado y alertas climáticas con trazabilidad de operador.

---

## Mejoras y Funcionalidades Adicionales Implementadas

Durante el desarrollo y pruebas de integración con el cliente, se identificaron necesidades operativas que no estaban contempladas en el informe inicial pero que resultaron fundamentales para la usabilidad y robustez del sistema:

### 1. Ventana de Protección de Edición Local (20 Segundos)
* **Motivo:** En las primeras versiones, la sincronización continua por latidos de red (heartbeats cada 800 ms) sobreescribía el texto que el operador estaba redactando o la dirección IP que intentaba modificar si se pausaba a escribir.
* **Solución:** Se implementó una ventana de gracia de 20 segundos que suspende temporalmente la sobreescritura externa mientras el usuario tiene el foco o escribe en cualquier campo, garantizando que pueda borrar, corregir o redactar sin interferencias.

### 2. Sincronización Bidireccional en Paralelo
* **Motivo:** Cuando dos operadores trabajan en el mismo sector agrícola, uno puede estar junto a la estación y otro en la bodega o vehículo de supervisión. Si uno activa un actuador o cambia el mensaje, el otro debe ver el cambio reflejado inmediatamente para evitar decisiones contradictorias.
* **Solución:** Se habilitó un protocolo de propagación inmediata de estados entre nodos clientes y la estación.

### 3. Optimización de Latencia TCP (`TCP_NODELAY`)
* **Motivo:** El algoritmo de Nagle por defecto en sockets TCP introducía retardos de hasta 500 ms en redes locales y entornos emulados.
* **Solución:** Se activó la directiva `TCP_NODELAY = true` a nivel de socket, reduciendo los tiempos de respuesta a menos de 5 ms para garantizar que el apagado de alarmas o emergencias sea instantáneo.

### 4. Persistencia Local de Perfiles de Operador (Base de Datos JSON)
* **Motivo:** En faenas rurales es común perder la conexión a internet. El sistema no podía depender de servicios externos para validar accesos.
* **Solución:** Se integró un almacén local con almacenamiento criptográfico de contraseñas (`salt + hash`) que sincroniza perfiles de forma autónoma entre terminales cuando se restablece el enlace.

### 5. Identidad Visual Institucional en la APK
* **Motivo:** Brindar un producto con presentación profesional y reconocible para el cliente.
* **Solución:** Se diseñó el ícono oficial con torre de comunicaciones y ondas de radio utilizando los colores institucionales (Rojo `#D32F2F`, Azul Marino `#0A1A2F` y Blanco), disponible en formatos adaptativo y mipmap para cualquier resolución de pantalla Android.

---

## Estructura del Repositorio

```text
├── app/                                    # Código fuente de la aplicación Android
│   └── src/main/java/cl/inacap/estacionmeteorologica/
│       ├── MainActivity.kt                 # Actividad principal y arranque del servicio
│       ├── NavegacionApp.kt                # Navegación entre pantallas Jetpack Compose
│       ├── Screens/                        # Pantallas (Login, Monitoreo, Activador, Conexión, Seguridad)
│       ├── network/                        # Gestor de sockets TCP, UDP y Bluetooth
│       └── security/                       # Criptografía AES-256, hash SHA-256 y control RBAC
├── arduino_r4_wifi/                        # Firmware para placa Arduino UNO R4 WiFi
├── arduino_esp32_wifi/                     # Firmware alternativo para microcontroladores ESP32
├── simulador_esp32_pc.py                   # Simulador de hardware IoT en Python (TCP 8888 / UDP 8889)
├── usuarios_estacion_db.json               # Base de datos local de operadores y credenciales
└── EstacionMeteorologica_GrupoG_ESP32_WiFi.apk  # Paquete instalable compilado listo para uso
```

---

## Instrucciones de Instalación y Uso

1. **Instalación en Smartphone Android:**
   * Copiar el archivo `EstacionMeteorologica_GrupoG_ESP32_WiFi.apk` al teléfono o instalarlo mediante `adb install -r EstacionMeteorologica_GrupoG_ESP32_WiFi.apk`.
   * Otorgar permisos de red local y Bluetooth cuando la aplicación lo solicite.

2. **Ejecución con Hardware Físico o Simulador:**
   * **Con ESP32 / Arduino:** Cargar el firmware ubicado en `arduino_r4_wifi/` o `arduino_esp32_wifi/` y conectar la placa a la misma red Wi-Fi que el teléfono.
   * **Con Simulador de PC:** Ejecutar en terminal `python simulador_esp32_pc.py` para levantar la estación virtual en los puertos TCP 8888 y UDP 8889.

3. **Operación:**
   * Abrir la aplicación **Estación IoT**, ingresar con el usuario `operador_ot` o `admin`, presionar **Conectar Automáticamente** y comenzar la supervisión y control en tiempo real.
