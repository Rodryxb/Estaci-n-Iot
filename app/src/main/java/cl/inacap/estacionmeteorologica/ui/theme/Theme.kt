package cl.inacap.estacionmeteorologica.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Esquema de colores Material Design 3 personalizado para la Estación Meteorológica Portátil
private val EsquemaColorMeteo = lightColorScheme(
    primary = AzulMeteoPrimario,
    onPrimary = Color.White,
    primaryContainer = ContenedorPrimarioClaro,
    onPrimaryContainer = TextoSobrePrimarioClaro,
    secondary = CianTelemetria,
    onSecondary = Color.White,
    tertiary = VerdeSeguridadOT,
    background = FondoAppClaro,
    surface = SuperficieTarjeta,
    error = RojoCriticoOT
)

/**
 * Componente envoltorio del tema visual Material Design 3.
 * Aplica colores semánticos y tipografía consistente en todas las pantallas (Login, Monitoreo, Activador, etc.).
 */
@Composable
fun EstacionMeteorologicaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EsquemaColorMeteo,
        typography = Typography,
        content = content
    )
}
