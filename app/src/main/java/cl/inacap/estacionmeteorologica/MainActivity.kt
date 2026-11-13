package cl.inacap.estacionmeteorologica

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.inacap.estacionmeteorologica.network.NetworkManagerIoT
import cl.inacap.estacionmeteorologica.security.SecurityManagerIoT
import cl.inacap.estacionmeteorologica.ui.theme.EstacionMeteorologicaTheme

/**
 * ============================================================================================
 * ACTIVIDAD PRINCIPAL DE ANDROID (MainActivity.kt)
 * Proyecto: Estación Meteorológica Portátil con Arduino UNO R4 WiFi
 * Asignatura: Aplicaciones Móviles para IoT (TI3042 - INACAP)
 * ============================================================================================
 * Explicación para defensa del código:
 * - Hereda de ComponentActivity(), que es la clase base moderna de Android para Jetpack Compose.
 * - En el método de ciclo de vida onCreate() se inicializa el motor inalámbrico (Servidor TCP 8888,
 *   Autodescubrimiento UDP 8889 y detector de Bluetooth/Wi-Fi) y se carga el tema Material Design 3.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicializamos el gestor de conectividad inalámbrica y persistencia de usuarios RBAC
        SecurityManagerIoT.inicializarPersistenciaUsuarios(applicationContext)
        NetworkManagerIoT.inicializarSistemaInalambrico(applicationContext)

        // 2. Habilitamos el diseño de borde a borde (Edge-to-Edge) siguiendo estándares Android modernos
        enableEdgeToEdge()

        // 3. setContent define la interfaz gráfica declarativa aplicando nuestro tema Material Design 3
        setContent {
            EstacionMeteorologicaTheme {
                NavegacionApp()
            }
        }
    }
}
