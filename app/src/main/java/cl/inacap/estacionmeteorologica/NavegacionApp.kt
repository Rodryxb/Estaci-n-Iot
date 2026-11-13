package cl.inacap.estacionmeteorologica

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.inacap.estacionmeteorologica.Screens.ActivadorScreen
import cl.inacap.estacionmeteorologica.Screens.InterconexionScreen
import cl.inacap.estacionmeteorologica.Screens.LoginScreen
import cl.inacap.estacionmeteorologica.Screens.MonitoreoScreen
import cl.inacap.estacionmeteorologica.Screens.SeguridadOtScreen

/**
 * ============================================================================================
 * CONTROLADOR DE NAVEGACIÓN PRINCIPAL DE LA APLICACIÓN (Jetpack Navigation Compose)
 * ============================================================================================
 * Define las rutas de las 5 pantallas de la aplicación móvil:
 * 1. "login"         -> Pantalla de ingreso con credenciales SHA-256 (Criterio 2.1.1.1)
 * 2. "monitoreo"     -> Pantalla de visualización de sensores de la Estación R4 (Criterio 2.1.1.2)
 * 3. "activador"     -> Pantalla de control con EditText, Sliders, RadioButtons y Switches (Criterio 2.1.1.2)
 * 4. "interconexion" -> Pantalla de Wi-Fi, Bluetooth, 2 Apps y Pruebas de Estabilidad (Criterios 2.1.2.3, 2.1.4.7, 2.1.4.8)
 * 5. "seguridad"     -> Pantalla de Cifrado AES-256 y Cumplimiento ISO 27001 / OT (Criterios 2.1.3.5, 2.1.3.6)
 */
@Composable
fun NavegacionApp() {
    // 1. rememberNavController() crea y mantiene el estado del historial de navegación entre recomposiciones
    val navController = rememberNavController()

    // 2. NavHost vincula el navController con las pantallas @Composable definiendo "login" como inicio
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        // Ruta 1: Pantalla de Autenticación Segura
        composable(route = "login") {
            LoginScreen(navController = navController)
        }

        // Ruta 2: Pantalla de Monitoreo Meteorológico en Tiempo Real
        composable(route = "monitoreo") {
            MonitoreoScreen(navController = navController)
        }

        // Ruta 3: Pantalla de Activador y Envío de Comandos Remotos
        composable(route = "activador") {
            ActivadorScreen(navController = navController)
        }

        // Ruta 4: Pantalla de Conectividad Inalámbrica, Interconexión Multi-App y Escenarios
        composable(route = "interconexion") {
            InterconexionScreen(navController = navController)
        }

        // Ruta 5: Pantalla de Auditoría ISO/IEC 27001, ISO 27400 y Lineamientos Industria OT
        composable(route = "seguridad") {
            SeguridadOtScreen(navController = navController)
        }
    }
}
