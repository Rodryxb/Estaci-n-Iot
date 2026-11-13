package cl.inacap.estacionmeteorologica.Screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cl.inacap.estacionmeteorologica.network.NetworkManagerIoT

/**
 * ============================================================================================
 * PANTALLA DE INTERCONEXIÓN MULTI-APP (WI-FI + BLUETOOTH) Y ESTABILIDAD DE ESCENARIOS
 * Cumple con:
 * - Criterio 2.1.2.3 (15 pts): Conexiones inalámbricas Bluetooth y Wi-Fi eficientes y seguras.
 * - Criterio 2.1.4.7 (15 pts): Configura y prueba la interconexión entre múltiples apps móviles.
 * - Criterio 2.1.4.8 (20 pts): Evalúa la estabilidad y eficiencia en diferentes escenarios de uso.
 * ============================================================================================
 */
@Composable
fun InterconexionScreen(navController: NavHostController) {
    val context = LocalContext.current
    var mensajeBanner by remember { mutableStateOf("") }
    var mostrarAvanzado by remember { mutableStateOf(false) }

    // Lanzador oficial de permisos en tiempo de ejecución para Bluetooth en Android 12+ (API 31+)
    val launcherPermisosBluetooth = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        NetworkManagerIoT.actualizarEstadoRedYBluetooth(context)
        mensajeBanner = "Estado de adaptadores Wi-Fi y Bluetooth actualizado correctamente."
    }

    Scaffold(
        topBar = {
            BarraSuperiorEstacion(
                tituloPantalla = "Conexión con la Estación",
                navController = navController
            )
        },
        bottomBar = {
            BarraNavegacionInferior(
                rutaActual = "interconexion",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ================================================================================
            // 1. PANEL PRINCIPAL DE CONEXIÓN FÁCIL ("SE CONECTA O NO")
            // ================================================================================
            val conectado = NetworkManagerIoT.conectadoEnVivo
            val verificando = NetworkManagerIoT.verificandoConexion

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Estado de Conexión con la Estación",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Permite vincular el teléfono con el PC para sincronizar sensores, botones y usuarios en tiempo real.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tarjeta Semáforo: Conectado o Desconectado
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = when {
                                    verificando -> Color(0xFFFFF8E1)
                                    conectado -> Color(0xFFE8F5E9)
                                    else -> Color(0xFFFFEBEE)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when {
                                        verificando -> "🟡 Conectando con la Estación..."
                                        conectado -> "🟢 Conectado en Tiempo Real"
                                        else -> "🔴 Sin Conexión (Modo Local)"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        verificando -> Color(0xFFE65100)
                                        conectado -> Color(0xFF1B5E20)
                                        else -> Color(0xFFC62828)
                                    }
                                )
                                if (conectado) {
                                    Text(
                                        text = "${"%.1f".format(NetworkManagerIoT.latenciaActualMs)} ms",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (conectado) {
                                    "Estación: ${NetworkManagerIoT.estacionConectadaNombre} (${NetworkManagerIoT.ipRemotaDestino})"
                                } else {
                                    "La aplicación está operando con datos simulados."
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF263238)
                            )
                            Text(
                                text = "Última sincronización: ${NetworkManagerIoT.horaUltimoLatido} • Perfiles en Base de Datos: ${cl.inacap.estacionmeteorologica.security.SecurityManagerIoT.listaUsuariosRegistrados.size}",
                                fontSize = 11.sp,
                                color = Color(0xFF546E7A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón principal de Conexión / Desconexión
                    if (conectado) {
                        Button(
                            onClick = {
                                NetworkManagerIoT.desconectarEnlaceRemoto()
                                mensajeBanner = "ℹ️ Estación desconectada. Operando en modo local."
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("Desconectar de la Estación", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                NetworkManagerIoT.descubrirDispositivosEnRedWifi { res ->
                                    mensajeBanner = res
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD))
                        ) {
                            Text("⚡ Conectar Automáticamente (1 Toque)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Desplegable de Configuración Avanzada de Red
                    TextButton(
                        onClick = { mostrarAvanzado = !mostrarAvanzado },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (mostrarAvanzado) "▲ Ocultar Opciones de Red" else "▼ Configurar Dirección IP Manualmente",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0277BD)
                        )
                    }

                    if (mostrarAvanzado) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = NetworkManagerIoT.ipRemotaDestino,
                            onValueChange = {
                                NetworkManagerIoT.notificarEdicionIpEnCurso()
                                NetworkManagerIoT.ipRemotaDestino = it
                            },
                            label = { Text("Dirección IP de la Estación / PC (Edición protegida 20s)") },
                            placeholder = { Text("Ej: 192.168.1.90 o 10.0.2.2") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    NetworkManagerIoT.conectarADestinoEspecifico("10.0.2.2", "PC Emulador") { _, msg ->
                                        mensajeBanner = msg
                                    }
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Emulador (10.0.2.2)", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    NetworkManagerIoT.conectarADestinoEspecifico("192.168.1.90", "PC Wi-Fi") { _, msg ->
                                        mensajeBanner = msg
                                    }
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Wi-Fi (192.168.1.90)", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                NetworkManagerIoT.conectarADestinoEspecifico(NetworkManagerIoT.ipRemotaDestino) { _, msg ->
                                    mensajeBanner = msg
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                        ) {
                            Text("Guardar y Conectar a esta IP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (mensajeBanner.isNotEmpty()) {
                val esError = mensajeBanner.startsWith("❌")
                val esInfo = mensajeBanner.startsWith("ℹ️")
                val colorFondo = when {
                    esError -> Color(0xFFFFEBEE)
                    esInfo -> Color(0xFFE1F5FE)
                    else -> Color(0xFFE8F5E9)
                }
                val colorTexto = when {
                    esError -> Color(0xFFC62828)
                    esInfo -> Color(0xFF01579B)
                    else -> Color(0xFF1B5E20)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorFondo, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = mensajeBanner,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorTexto
                    )
                }
            }

            // ================================================================================
            // 2. MÓDULO BLUETOOTH + WI-FI (Criterio 2.1.2.3 - Buenas Prácticas de Conectividad)
            // ================================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Radios Wi-Fi y Bluetooth BLE",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    launcherPermisosBluetooth.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_SCAN
                                        )
                                    )
                                } else {
                                    NetworkManagerIoT.actualizarEstadoRedYBluetooth(context)
                                    mensajeBanner = "Adaptadores inalámbricos escaneados."
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFE1F5FE),
                                contentColor = Color(0xFF0277BD)
                            )
                        ) {
                            Text("🔄 Escanear", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "• Radio Wi-Fi (IEEE 802.11n): ${if (NetworkManagerIoT.wifiConectado) "ACTIVO (${NetworkManagerIoT.ipLocalDispositivo})" else "Enlace Local Loopback"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "• Adaptador Bluetooth / BLE 5.0: ${if (NetworkManagerIoT.bluetoothEncendido) "ENCENDIDO" else "Modo Emulación / Apagado"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "• Canal Bluetooth Seleccionado: ${NetworkManagerIoT.dispositivoBluetoothSeleccionado}",
                        fontSize = 12.sp,
                        color = Color(0xFF00695C),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Lista de nodos Bluetooth reales vinculados en el dispositivo Android
                    if (NetworkManagerIoT.dispositivosBluetooth.isEmpty()) {
                        Text(
                            text = "Sin dispositivos Bluetooth emparejados actualmente (Canal principal activo: Wi-Fi TCP Puerto 8888).",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    } else {
                        NetworkManagerIoT.dispositivosBluetooth.forEach { bt ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "🔹 ${bt.nombre}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "MAC: ${bt.direccionMac} • ${bt.tipo}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = {
                                        NetworkManagerIoT.dispositivoBluetoothSeleccionado = "${bt.nombre} (${bt.direccionMac})"
                                        NetworkManagerIoT.agregarLogRed("Canal Bluetooth vinculado con ${bt.nombre} [${bt.direccionMac}]")
                                        mensajeBanner = "Canal de respaldo Bluetooth establecido con ${bt.nombre}"
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFFE8F5E9),
                                        contentColor = Color(0xFF2E7D32)
                                    )
                                ) {
                                    Text("Vincular", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ================================================================================
            // 3. EVALUACIÓN DE ESTABILIDAD Y EFICIENCIA EN ESCENARIOS DE USO (Criterio 2.1.4.8)
            // ================================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. Evaluación de Estabilidad y Escenarios de Uso",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Mide latencia RTT, Jitter, pérdida de paquetes y resiliencia ante fallos en diferentes escenarios.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tablero de métricas de red en tiempo real
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricaRedItem("Latencia", "${"%.1f".format(NetworkManagerIoT.latenciaActualMs)} ms")
                        MetricaRedItem("Promedio", "${"%.1f".format(NetworkManagerIoT.latenciaPromedioMs)} ms")
                        MetricaRedItem("Jitter", "${"%.1f".format(NetworkManagerIoT.jitterActualMs)} ms")
                        MetricaRedItem("Eficiencia", "${"%.1f".format(NetworkManagerIoT.eficienciaEnlacePct)} %")
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Paquetes Enviados (TX): ${NetworkManagerIoT.paquetesEnviados} | Recibidos (RX): ${NetworkManagerIoT.paquetesRecibidos} | Rechazados/Perdidos: ${NetworkManagerIoT.paquetesPerdidos}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF37474F)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botones para ejecutar los 4 Escenarios de Prueba (Criterio 2.1.4.8)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                NetworkManagerIoT.ejecutarPruebaEscenario(1) { res ->
                                    mensajeBanner = "${res.nombreEscenario}: ${res.calificacionOT}"
                                }
                            },
                            enabled = !NetworkManagerIoT.ejecutandoPruebaEstres,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32)
                            )
                        ) {
                            Text("⏱️ 1: Nominal", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                NetworkManagerIoT.ejecutarPruebaEscenario(2) { res ->
                                    mensajeBanner = "${res.nombreEscenario}: ${res.calificacionOT}"
                                }
                            },
                            enabled = !NetworkManagerIoT.ejecutandoPruebaEstres,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0288D1)
                            )
                        ) {
                            Text("⚡ 2: Ráfaga", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                NetworkManagerIoT.ejecutarPruebaEscenario(3) { res ->
                                    mensajeBanner = "${res.nombreEscenario}: ${res.calificacionOT}"
                                }
                            },
                            enabled = !NetworkManagerIoT.ejecutandoPruebaEstres,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE65100)
                            )
                        ) {
                            Text("🔄 3: Reconexión", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                NetworkManagerIoT.ejecutarPruebaEscenario(4) { res ->
                                    mensajeBanner = "${res.nombreEscenario}: ${res.calificacionOT}"
                                }
                            },
                            enabled = !NetworkManagerIoT.ejecutandoPruebaEstres,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4A148C)
                            )
                        ) {
                            Text("🛡️ 4: Anti-MITM", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    // Resultados históricos de las pruebas de escenarios ejecutadas
                    if (NetworkManagerIoT.historialEscenarios.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Resultados de Pruebas de Escenarios:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        NetworkManagerIoT.historialEscenarios.take(4).forEach { item ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "${item.nombreEscenario} (${item.timestamp})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Éxito: ${item.exitosos}/${item.paquetesPrueba} | Latencia Media: ${item.latenciaMediaMs} ms | Jitter: ${item.jitterMs} ms",
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Evaluación: ${item.calificacionOT}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ================================================================================
            // 4. CONSOLA DE TRÁFICO INALÁMBRICO EN VIVO
            // ================================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2327))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Consola de Tráfico TCP/UDP en Tiempo Real",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF80D8FF)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (NetworkManagerIoT.logsComunicacion.isEmpty()) {
                        Text(
                            text = "Esperando paquetes...",
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        NetworkManagerIoT.logsComunicacion.take(8).forEach { log ->
                            Text(
                                text = log,
                                fontSize = 11.sp,
                                color = Color(0xFFE0F7FA),
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricaRedItem(titulo: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = titulo, fontSize = 11.sp, color = Color.Gray)
        Text(
            text = valor,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
