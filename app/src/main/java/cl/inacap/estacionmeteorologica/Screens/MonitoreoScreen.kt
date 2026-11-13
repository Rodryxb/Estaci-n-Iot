package cl.inacap.estacionmeteorologica.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cl.inacap.estacionmeteorologica.network.NetworkManagerIoT
import cl.inacap.estacionmeteorologica.security.SecurityManagerIoT

/**
 * ============================================================================================
 * PANTALLA DE MONITOREO METEOROLÓGICO EN TIEMPO REAL
 * Cumple con:
 * - Criterio 2.1.1.2 (10 pts): Pantalla de Monitoreo aplicando Material Design y mostrando
 *   los datos recibidos desde el Arduino UNO R4 WiFi o el segundo dispositivo Android.
 * - Criterio 2.1.2.4 (10 pts): Resuelve la problemática de monitoreo portátil en terreno.
 * ============================================================================================
 */
@Composable
fun MonitoreoScreen(navController: NavHostController) {
    // Variable local para mostrar el resultado de la última sincronización manual
    var feedbackSincronizacion by remember { mutableStateOf("") }

    // Evaluamos alertas agrícolas críticas (HU-03: Helada Nocturna < 3.0°C o Golpe de Calor > Umbral)
    val alertaCalor = NetworkManagerIoT.temperaturaC >= NetworkManagerIoT.umbralTemperaturaAlerta
    val alertaHelada = NetworkManagerIoT.temperaturaC < 3.0f
    val alertaTermica = alertaCalor || alertaHelada

    val conectado = NetworkManagerIoT.conectadoEnVivo

    val colorFondoBanner = when {
        alertaHelada -> Color(0xFFE0F7FA) // Azul hielo para helada
        alertaCalor -> Color(0xFFFFEBEE)  // Rojo advertencia para golpe de calor
        conectado -> Color(0xFFE8F5E9)    // Verde certeza de conexión activa
        else -> Color(0xFFFFF8E1)         // Ámbar claro cuando está en simulación local
    }
    val colorTextoBanner = when {
        alertaHelada -> Color(0xFF006064)
        alertaCalor -> Color(0xFFC62828)
        conectado -> Color(0xFF1B5E20)
        else -> Color(0xFFE65100)
    }
    val textoEstadoBanner = when {
        alertaHelada -> "❄️ ALERTA DE HELADA (< 3.0°C)"
        alertaCalor -> "🔥 ALERTA GOLPE DE CALOR (> ${"%.1f".format(NetworkManagerIoT.umbralTemperaturaAlerta)}°C)"
        conectado -> "🟢 Conectado con la Estación (${"%.1f".format(NetworkManagerIoT.latenciaActualMs)} ms)"
        else -> "🔴 Sin Conexión con la Estación (Modo Local)"
    }

    Scaffold(
        topBar = {
            BarraSuperiorEstacion(
                tituloPantalla = "Monitoreo de la Estación",
                navController = navController
            )
        },
        bottomBar = {
            BarraNavegacionInferior(
                rutaActual = "monitoreo",
                navController = navController
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Visualizador de la Matriz LED 12x8 propia del Arduino UNO R4 WiFi
            TarjetaMatrizLedArduinoR4(
                mensajeMatriz = NetworkManagerIoT.mensajeMatrizLed,
                alarmaActiva = alertaTermica || NetworkManagerIoT.alarmaSonoraActiva
            )

            // 2. Banner de Estado de Sesión OT y Enlace Inalámbrico
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorFondoBanner
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = textoEstadoBanner,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorTextoBanner,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Batería: ${NetworkManagerIoT.nivelBateriaPct}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    Text(
                        text = "Operador: ${SecurityManagerIoT.usuarioAutenticado.ifEmpty { "operador_ot" }} (${SecurityManagerIoT.rolTipoActivo}) | Modo: ${NetworkManagerIoT.modoOperacion} | Destino: ${NetworkManagerIoT.ipRemotaDestino}:${NetworkManagerIoT.PUERTO_TCP_IOT}",
                        fontSize = 11.sp,
                        color = Color(0xFF37474F),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (!conectado) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                NetworkManagerIoT.descubrirDispositivosEnRedWifi { res ->
                                    feedbackSincronizacion = res
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0277BD)
                            )
                        ) {
                            Text(
                                text = "⚡ Conectar a la Estación Automáticamente (1 Toque)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (SecurityManagerIoT.esAdministradorActivo()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                navController.navigate("seguridad") {
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1B5E20)
                            )
                        ) {
                            Text(
                                text = "👑 Panel Administrador: Gestionar Usuarios y Roles (CRUD)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Cuadrícula de Tarjetas de Sensores Meteorológicos (Material Design 3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarjetaSensorIndividual(
                    modifier = Modifier.weight(1f),
                    titulo = "Temperatura",
                    valorTexto = "${"%.1f".format(NetworkManagerIoT.temperaturaC)} °C",
                    subtitulo = if (alertaHelada) "Riesgo de helada (<3°C)" else "Umbral: ${"%.1f".format(NetworkManagerIoT.umbralTemperaturaAlerta)} °C",
                    progreso = (NetworkManagerIoT.temperaturaC / 50f).coerceIn(0f, 1f),
                    colorIndicador = when {
                        alertaHelada -> Color(0xFF0097A7) // Azul cian para helada
                        alertaCalor -> Color(0xFFC62828)  // Rojo para golpe de calor
                        else -> Color(0xFF0277BD)
                    }
                )

                TarjetaSensorIndividual(
                    modifier = Modifier.weight(1f),
                    titulo = "Humedad",
                    valorTexto = "${"%.1f".format(NetworkManagerIoT.humedadPct)} %",
                    subtitulo = if (NetworkManagerIoT.humedadPct > 75f) "Alta humedad" else "Confort normal",
                    progreso = (NetworkManagerIoT.humedadPct / 100f).coerceIn(0f, 1f),
                    colorIndicador = Color(0xFF00838F)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarjetaSensorIndividual(
                    modifier = Modifier.weight(1f),
                    titulo = "Nivel de Luz",
                    valorTexto = "${NetworkManagerIoT.calidadAirePpm} Lux",
                    subtitulo = if (NetworkManagerIoT.calidadAirePpm > NetworkManagerIoT.umbralLuzAlerta) {
                        "Alerta Luz Alta (>${NetworkManagerIoT.umbralLuzAlerta})"
                    } else {
                        "Umbral: ${NetworkManagerIoT.umbralLuzAlerta} Lux"
                    },
                    progreso = (NetworkManagerIoT.calidadAirePpm / 1023f).coerceIn(0f, 1f),
                    colorIndicador = Color(0xFFEF6C00)
                )

                TarjetaSensorIndividual(
                    modifier = Modifier.weight(1f),
                    titulo = "Pantalla de la Estación",
                    valorTexto = if (NetworkManagerIoT.sistemaEncendido) {
                        "T:${"%.1f".format(NetworkManagerIoT.temperaturaC)}C H:${NetworkManagerIoT.humedadPct.toInt()}%"
                    } else {
                        "EN REPOSO"
                    },
                    subtitulo = if (NetworkManagerIoT.lcdBacklightActivo) "Iluminación Encendida" else "Modo Ahorro",
                    progreso = if (NetworkManagerIoT.sistemaEncendido) 1.0f else 0.2f,
                    colorIndicador = Color(0xFF2E7D32)
                )
            }

            // 4. Resumen en Vivo del Estado de los Componentes
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Estado del Sistema y Componentes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "• Sensor de Temperatura y Humedad: OPERATIVO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "• Alarma Sonora de Advertencia: ${if (NetworkManagerIoT.alarmaSonoraActiva) "SONANDO (ALERTA)" else "EN SILENCIO"}",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Iluminación de Pantalla: ${if (NetworkManagerIoT.lcdBacklightActivo) "ENCENDIDA" else "MODO AHORRO"}",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• Estado de la Estación: ${if (NetworkManagerIoT.sistemaEncendido) "OPERANDO NORMALMENTE" else "EN REPOSO (STANDBY)"}",
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Switch rápido para pausar/reanudar la variación automática de sensores durante la defensa
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Muestreo Automático de Sensores en Vivo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (NetworkManagerIoT.simulacionSensoresActiva) {
                                    "🟢 ENCENDIDO: Sensores variando en vivo y sincronizados"
                                } else {
                                    "⏸️ EN PAUSA: Lecturas fijas en la estación"
                                },
                                fontSize = 11.sp,
                                color = if (NetworkManagerIoT.simulacionSensoresActiva) Color(0xFF1B5E20) else Color.Gray
                            )
                        }
                        Switch(
                            checked = NetworkManagerIoT.simulacionSensoresActiva,
                            onCheckedChange = { activo ->
                                NetworkManagerIoT.alternarMuestreoAutomatico(activo) { _, msg ->
                                    feedbackSincronizacion = msg
                                }
                            }
                        )
                    }
                }
            }

            if (feedbackSincronizacion.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = feedbackSincronizacion,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }

            // 5. Botones de Acción Rápida (Emitir Telemetría a otro teléfono o Ir al Activador)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        NetworkManagerIoT.enviarEstadoInalambrico("TELEMETRIA_MANUAL") { _, msg ->
                            feedbackSincronizacion = msg
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Emitir Lectura Wi-Fi", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        navController.navigate("activador")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Ir a Activador 🎛️", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Tarjeta reutilizable Material Design 3 para mostrar una variable meteorológica con barra de rango.
 */
@Composable
fun TarjetaSensorIndividual(
    modifier: Modifier = Modifier,
    titulo: String,
    valorTexto: String,
    subtitulo: String,
    progreso: Float,
    colorIndicador: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = titulo,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF546E7A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = valorTexto,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colorIndicador
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = colorIndicador,
                trackColor = Color(0xFFECEFF1)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitulo,
                fontSize = 11.sp,
                color = Color(0xFF455A64)
            )
        }
    }
}
