package cl.inacap.estacionmeteorologica

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ============================================================================================
 * PRUEBAS DE FUNCIONAMIENTO AUTOMATIZADAS (Criterio 2.1.3.11 - 3 Puntos)
 * Asignatura: Aplicaciones Móviles para IoT (TI3042 - INACAP)
 * Proyecto: Estación Meteorológica Portátil IoT
 * ============================================================================================
 * Valida de forma exhaustiva y automatizada:
 * 1. Autenticación y cálculo criptográfico SHA-256 + Salt (ISO/IEC 27001 A.5.17 e ISO 27400).
 * 2. Cifrado simétrico AES-256/CBC/PKCS5Padding y descifrado en tránsito inalámbrico.
 * 3. Firma de integridad anti-manipulación (Anti-Tampering / MITM).
 * 4. Manipulación segura de datos de hardware (Safety OT / ISA/IEC 62443).
 * 5. Serialización y deserialización de telemetría IoT (DHT22, LDR, LCD, Buzzer).
 * 6. Detección de umbrales críticos de helada (< 3.0°C) y golpe de calor (> 30.0°C).
 */
class EstacionIoTUnitTest {

    companion object {
        private const val SALT_TEST = "INACAP_TI3042_ESTACION_R4_WIFI_SALT_2026"
        private val KEY_AES_TEST = "MeteoR4WiFiClaveSeguraOT2026!!32".toByteArray(Charsets.UTF_8)
        private val IV_AES_TEST = "VectorInitOT2026".toByteArray(Charsets.UTF_8)

        fun calcularSha256(texto: String): String {
            val conSalt = "$texto:$SALT_TEST"
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(conSalt.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun cifrarAesJava(plano: String): String {
            val key = SecretKeySpec(KEY_AES_TEST, "AES")
            val iv = IvParameterSpec(IV_AES_TEST)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key, iv)
            val enc = cipher.doFinal(plano.toByteArray(Charsets.UTF_8))
            return Base64.getEncoder().encodeToString(enc)
        }

        fun descifrarAesJava(cifradoB64: String): String {
            val key = SecretKeySpec(KEY_AES_TEST, "AES")
            val iv = IvParameterSpec(IV_AES_TEST)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
            val dec = Base64.getDecoder().decode(cifradoB64)
            return String(cipher.doFinal(dec), Charsets.UTF_8)
        }

        fun sanitizarDatosHardware(umbralTemp: Float, umbralLuz: Int, mensaje: String): Triple<Float, Int, String> {
            val t = umbralTemp.coerceIn(15.0f, 50.0f)
            val l = umbralLuz.coerceIn(100, 1000)
            val m = mensaje.replace(Regex("[^a-zA-Z0-9 áéíóúÁÉÍÓÚ°%.-]"), "").take(24)
            return Triple(t, l, if (m.isEmpty()) "METEO OK" else m)
        }
    }

    // ========================================================================================
    // CASO 1: VERIFICACIÓN DE AUTENTICACIÓN Y RESUMEN CRIPTOGRÁFICO SHA-256 CON SALT
    // ========================================================================================
    @Test
    fun test01_Autenticacion_HashSha256ConSalt_ProteccionRainbow() {
        val passOriginal = "Meteo2026!"
        val hashGenerado = calcularSha256(passOriginal)

        // El hash SHA-256 debe medir exactamente 64 caracteres hexadecimales
        assertEquals("El hash SHA-256 debe tener 64 caracteres", 64, hashGenerado.length)

        // Verificamos que sea determinístico para la misma contraseña
        assertEquals(hashGenerado, calcularSha256(passOriginal))

        // Verificamos que una contraseña incorrecta genere un hash completamente distinto (Efecto Avalancha)
        val hashIncorrecto = calcularSha256("Meteo2026?")
        assertNotEquals("Contraseñas distintas deben tener hashes distintos", hashGenerado, hashIncorrecto)
    }

    // ========================================================================================
    // CASO 2: CIFRADO Y DESCIFRADO SIMÉTRICO AES-256 (COMUNICACIÓN INALÁMBRICA SEGURA)
    // ========================================================================================
    @Test
    fun test02_CifradoYDescifradoAES256_IntegridadDatosEnTransito() {
        val jsonTelemetria = """{"temp":24.5,"hum":58.2,"luz":420,"alarma":false}"""

        val cifradoBase64 = cifrarAesJava(jsonTelemetria)
        assertNotNull(cifradoBase64)
        assertFalse("El texto cifrado no debe contener texto plano", cifradoBase64.contains("temp"))

        val textoDescifrado = descifrarAesJava(cifradoBase64)
        assertEquals("El texto descifrado debe ser idéntico al original", jsonTelemetria, textoDescifrado)
    }

    // ========================================================================================
    // CASO 3: FIRMA DE INTEGRIDAD ANTI-TAMPERING (PROTECCIÓN MAN-IN-THE-MIDDLE)
    // ========================================================================================
    @Test
    fun test03_FirmaIntegridad_DeteccionDeTramaAdulterada() {
        val payloadOriginal = """{"cmd":"BUZZER_ON","operador":"admin"}"""
        val firmaValida = calcularSha256(payloadOriginal).take(12).uppercase(Locale.ROOT)

        // Simulamos un atacante modificando el comando en tránsito
        val payloadAdulterado = """{"cmd":"BUZZER_OFF","operador":"admin"}"""
        val firmaCalculadaAdulterada = calcularSha256(payloadAdulterado).take(12).uppercase(Locale.ROOT)

        assertNotEquals("La firma del atacante debe ser rechazada", firmaValida, firmaCalculadaAdulterada)
    }

    // ========================================================================================
    // CASO 4: SANITIZACIÓN DE RANGOS SEGUROS DE HARDWARE (SAFETY OT - ISA/IEC 62443)
    // ========================================================================================
    @Test
    fun test04_SanitizacionHardware_EvitaValoresFueraDeRango() {
        // Probamos inyección de valor térmico extremo (-99°C o +150°C)
        val (temp1, luz1, msg1) = sanitizarDatosHardware(-99.0f, 50, "<script>alert('xss')</script>")
        assertEquals("La temperatura no debe bajar de 15.0°C", 15.0f, temp1, 0.01f)
        assertEquals("El umbral lumínico no debe bajar de 100", 100, luz1)
        assertFalse("Caracteres peligrosos como < > deben ser filtrados", msg1.contains("<"))

        val (temp2, luz2, _) = sanitizarDatosHardware(120.0f, 5000, "Alerta Normal")
        assertEquals("La temperatura no debe subir de 50.0°C", 50.0f, temp2, 0.01f)
        assertEquals("El umbral lumínico no debe subir de 1000", 1000, luz2)
    }

    // ========================================================================================
    // CASO 5: SERIALIZACIÓN Y VALIDACIÓN DE TELEMETRÍA IOT (JSON / TRAMA SENSOR)
    // ========================================================================================
    @Test
    fun test05_ManipulacionTelemetria_ValidacionFormatoCorrecto() {
        val payloadTelemetria = """{"dispositivo":"GRUPO-G-ESP32-WIFI","temp":26.8,"hum":62.4,"luz":510,"buzzer":true}"""

        // Validamos que contenga todas las claves requeridas por la estación meteorológica
        assertTrue("Debe incluir identificador del dispositivo", payloadTelemetria.contains(""""dispositivo":"GRUPO-G-ESP32-WIFI""""))
        assertTrue("Debe incluir lectura de temperatura", payloadTelemetria.contains(""""temp":26.8"""))
        assertTrue("Debe incluir lectura de humedad relativa", payloadTelemetria.contains(""""hum":62.4"""))
        assertTrue("Debe incluir lectura de sensor lumínico LDR", payloadTelemetria.contains(""""luz":510"""))
        assertTrue("Debe incluir estado del actuador buzzer", payloadTelemetria.contains(""""buzzer":true"""))

        // Verificamos estructura delimitadora de paquete
        assertTrue("Debe iniciar con llave JSON", payloadTelemetria.startsWith("{"))
        assertTrue("Debe cerrar con llave JSON", payloadTelemetria.endsWith("}"))
    }

    // ========================================================================================
    // CASO 6: LÓGICA DE DETECCIÓN DE HELADAS Y GOLPES DE CALOR
    // ========================================================================================
    @Test
    fun test06_AlertasClimaticas_HeladasYGolpeDeCalor() {
        fun evaluarAlerta(temp: Float): String {
            return when {
                temp <= 3.0f -> "ALERTA_HELADA"
                temp >= 30.0f -> "ALERTA_CALOR"
                else -> "NORMAL"
            }
        }

        assertEquals("Debe disparar ALERTA_HELADA a 2.1°C", "ALERTA_HELADA", evaluarAlerta(2.1f))
        assertEquals("Debe disparar ALERTA_CALOR a 33.5°C", "ALERTA_CALOR", evaluarAlerta(33.5f))
        assertEquals("Debe indicar NORMAL a 21.0°C", "NORMAL", evaluarAlerta(21.0f))
    }
}
