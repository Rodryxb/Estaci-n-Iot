package cl.inacap.estacionmeteorologica.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cl.inacap.estacionmeteorologica.network.NetworkManagerIoT
import cl.inacap.estacionmeteorologica.security.SecurityManagerIoT

/**
 * ============================================================================================
 * COMPONENTES VISUALES COMPARTIDOS (MATERIAL DESIGN 3 - Criterio 2.1.1.2)
 * ============================================================================================
 * Incluye la Barra Superior (TopAppBar), la Barra de Navegación Inferior (NavigationBar) y el
 * Simulador visual de la Matriz LED 12x8 propia de la placa Arduino UNO R4 WiFi.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperiorEstacion(
    tituloPantalla: String,
    navController: NavHostController
) {
    val conectado = NetworkManagerIoT.conectadoEnVivo
    val verificando = NetworkManagerIoT.verificandoConexion

    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = tituloPantalla,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (conectado) "Estación Conectada • Modo Seguro" else "Modo Local • Sin Conexión",
                        fontSize = 11.sp,
                        color = Color(0xFFB3E5FC)
                    )
                }
            },
            actions = {
                // Badge rápido de estado de conexión en la esquina superior
                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                verificando -> Color(0xFFFF8F00)
                                conectado -> Color(0xFF2E7D32)
                                else -> Color(0xFFC62828)
                            },
                            shape = RoundedCornerShape(50)
                        )
                        .clickable {
                            navController.navigate("interconexion") {
                                launchSingleTop = true
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            verificando -> "🟡 Conectando"
                            conectado -> "🟢 En línea"
                            else -> "🔴 Desconectado"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }

                // Botón para cerrar sesión segura (revoca el acceso actual y vuelve al Login)
                TextButton(
                    onClick = {
                        SecurityManagerIoT.registrarEvento(
                            nivel = "INFO",
                            norma = "ISO/IEC 27001 A.5.17",
                            detalle = "Cierre de sesión manual del operador '${SecurityManagerIoT.usuarioAutenticado}'."
                        )
                        navController.navigate("login") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                ) {
                    Text(
                        text = "Salir",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        )

        // Franja global de certeza de conexión (visible en todas las pantallas)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        verificando -> Color(0xFFFFF8E1)
                        conectado -> Color(0xFFE8F5E9)
                        else -> Color(0xFFFFEBEE)
                    }
                )
                .clickable {
                    navController.navigate("interconexion") {
                        launchSingleTop = true
                    }
                }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        verificando -> "🟡 Conectando con la Estación..."
                        conectado -> "🟢 Conectado con la Estación en tiempo real (${"%.1f".format(NetworkManagerIoT.latenciaActualMs)} ms)"
                        else -> "🔴 Sin conexión con la Estación • Toca para conectar"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        verificando -> Color(0xFFE65100)
                        conectado -> Color(0xFF1B5E20)
                        else -> Color(0xFFB71C1C)
                    },
                    modifier = Modifier.weight(1f)
                )
                if (conectado) {
                    Text(
                        text = "Últ. RX: ${NetworkManagerIoT.horaUltimoLatido}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

/**
 * Barra de navegación inferior Material Design 3 que permite cambiar entre las 4 áreas
 * evaluadas en la rúbrica sin perder el estado de la conexión inalámbrica.
 */
@Composable
fun BarraNavegacionInferior(
    rutaActual: String,
    navController: NavHostController
) {
    val items = listOf(
        Triple("monitoreo", "1. Monitoreo", "🌡️"),
        Triple("activador", "2. Controles", "🎛️"),
        Triple("interconexion", "3. Conexión", "📡"),
        Triple("seguridad", "4. Usuarios", "👥")
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { (ruta, etiqueta, icono) ->
            val seleccionado = rutaActual == ruta
            NavigationBarItem(
                selected = seleccionado,
                onClick = {
                    if (rutaActual != ruta) {
                        navController.navigate(ruta) {
                            launchSingleTop = true
                        }
                    }
                },
                icon = {
                    Text(text = icono, fontSize = 18.sp)
                },
                label = {
                    Text(
                        text = etiqueta,
                        fontSize = 11.sp,
                        fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color(0xFFE1F5FE)
                )
            )
        }
    }
}

/**
 * Componente que representa visualmente la Matriz LED roja de 12x8 integrada en el
 * hardware del Arduino UNO R4 WiFi, mostrando el mensaje activo y el estado del enlace.
 */
@Composable
fun TarjetaMatrizLedArduinoR4(
    mensajeMatriz: String,
    alarmaActiva: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF101820) // Fondo oscuro tipo PCB electrónica
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (alarmaActiva) Color(0xFFFF1744) else Color(0xFF00E676),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ESTACIÓN METEOROLÓGICA • PANTALLA EN VIVO",
                        color = Color(0xFF80D8FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Latencia: ${"%.1f".format(NetworkManagerIoT.latenciaActualMs)} ms",
                    color = Color(0xFFA7FFEB),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pantalla simulada de puntos LED rojos propia del Arduino UNO R4 WiFi
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E0808), RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ":: [ ${mensajeMatriz.uppercase()} ] ::",
                    color = if (alarmaActiva) Color(0xFFFF5252) else Color(0xFFFF3D00),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
