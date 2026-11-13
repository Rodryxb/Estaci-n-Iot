package cl.inacap.estacionmeteorologica.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import kotlin.math.roundToInt

/**
 * ============================================================================================
 * PANTALLA DE ACTIVADOR Y CONTROL REMOTO DE HARDWARE IOT
 * Cumple con el Criterio 2.1.1.2 (10 pts):
 * "Desarrolla las pantallas de Monitoreo y Activador, aplicando correctamente los principios
 * de diseño de Material Design y asegurando la funcionalidad de mostrar datos recibidos y
 * enviar datos a otro dispositivo mediante elementos interactivos (editTexts, sliders,
 * radioButtons, switches, etc.)."
 * ============================================================================================
 */
@Composable
fun ActivadorScreen(navController: NavHostController) {
    // Variable para mostrar en pantalla la confirmación (ACK) y latencia del último comando enviado
    var confirmacionEnvio by remember { mutableStateOf("") }
    var envioExitoso by remember { mutableStateOf(true) }

    val tengoMando = NetworkManagerIoT.tengoPermisoDeMando()

    // Lista de opciones para los RadioButtons de Modo Operativo
    val modosOperativos = listOf(
        "Eco Portátil" to "Ahorro de batería en terreno (muestreo cada 4.5s)",
        "Continuo Normal" to "Operación estándar de estación (muestreo cada 2.5s)",
        "Emergencia Climática" to "Monitoreo intensivo de ráfagas/alertas (cada 1.5s)"
    )

    Scaffold(
        topBar = {
            BarraSuperiorEstacion(
                tituloPantalla = "Activador y Control Remoto",
                navController = navController
            )
        },
        bottomBar = {
            BarraNavegacionInferior(
                rutaActual = "activador",
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
            // Vista en tiempo real de la Matriz LED 12x8 que cambia al editar los controles o desde el otro dispositivo
            TarjetaMatrizLedArduinoR4(
                mensajeMatriz = NetworkManagerIoT.mensajeMatrizLed,
                alarmaActiva = NetworkManagerIoT.alarmaSonoraActiva
            )

            // ================================================================================
            // SECCIÓN 1: MENSAJE EN PANTALLA DE LA ESTACIÓN (EditText)
            // ================================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Mensaje en Pantalla de la Estación",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "El mensaje que escribas aquí se mostrará en la pantalla de la estación y en los demás dispositivos en tiempo real.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // EditText 1: Mensaje de la Estación
                    OutlinedTextField(
                        value = NetworkManagerIoT.mensajeMatrizLed,
                        enabled = tengoMando,
                        onValueChange = { nuevoTexto ->
                            NetworkManagerIoT.notificarEdicionTextoEnCurso()
                            NetworkManagerIoT.mensajeMatrizLed = nuevoTexto.take(24)
                            NetworkManagerIoT.registrarCambioLocalActuador(
                                tipoEvento = "CAMBIO_TEXTO_LED",
                                debounceMs = 1200L
                            ) { ok, msg ->
                                envioExitoso = ok
                                confirmacionEnvio = msg
                            }
                        },
                        label = { Text("Mensaje de la Estación (Edición protegida 20s)") },
                        placeholder = { Text("Ej: ALERTA VIENTO 45KMH") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botones rápidos de mensajes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ESTACION OK", "ALERTA VIENTO", "HELADA CRITICA").forEach { msgPreset ->
                            Button(
                                onClick = {
                                    NetworkManagerIoT.notificarEdicionTextoEnCurso()
                                    NetworkManagerIoT.mensajeMatrizLed = msgPreset
                                    NetworkManagerIoT.registrarCambioLocalActuador("CAMBIO_TEXTO_LED") { ok, msg ->
                                        envioExitoso = ok
                                        confirmacionEnvio = msg
                                    }
                                },
                                enabled = tengoMando,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0277BD))
                            ) {
                                Text(msgPreset.take(13), fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // EditText 2: Nombre de la Estación
                    OutlinedTextField(
                        value = NetworkManagerIoT.nombreEstacion,
                        enabled = tengoMando,
                        onValueChange = {
                            NetworkManagerIoT.notificarEdicionTextoEnCurso()
                            NetworkManagerIoT.nombreEstacion = it.take(20)
                            NetworkManagerIoT.registrarCambioLocalActuador(
                                tipoEvento = "CAMBIO_TEXTO_LED",
                                debounceMs = 1200L
                            )
                        },
                        label = { Text("Nombre de la Estación (Edición protegida 20s)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ================================================================================
            // SECCIÓN 2: INTERRUPTORES Y CONTROLES (Switches)
            // ================================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Interruptores y Controles",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Al activar o desactivar un control en el PC, cambia inmediatamente en el Teléfono y viceversa.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Switch 1: Alarma Sonora
                    FilaSwitchActuador(
                        titulo = "Alarma Sonora de la Estación",
                        subtitulo = if (NetworkManagerIoT.alarmaSonoraActiva) "Estado: ACTIVADA (Sonando)" else "Estado: EN SILENCIO",
                        activado = NetworkManagerIoT.alarmaSonoraActiva,
                        habilitado = tengoMando,
                        onCambio = {
                            NetworkManagerIoT.alarmaSonoraActiva = it
                            if (it) {
                                NetworkManagerIoT.emitirAlertaAcusticaMovel()
                            }
                            NetworkManagerIoT.mensajeMatrizLed = if (it) "ALERTA OT ACTIVA" else "ESTACION OK"
                            NetworkManagerIoT.registrarCambioLocalActuador("SWITCH_ALARMA") { ok, msg ->
                                envioExitoso = ok
                                confirmacionEnvio = msg
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Switch 2: Iluminación de Pantalla
                    FilaSwitchActuador(
                        titulo = "Iluminación de Pantalla",
                        subtitulo = if (NetworkManagerIoT.lcdBacklightActivo) "Estado: ENCENDIDA" else "Estado: APAGADA (Ahorro de energía)",
                        activado = NetworkManagerIoT.lcdBacklightActivo,
                        habilitado = tengoMando,
                        onCambio = {
                            NetworkManagerIoT.lcdBacklightActivo = it
                            NetworkManagerIoT.registrarCambioLocalActuador("SWITCH_LCD_BACKLIGHT") { ok, msg ->
                                envioExitoso = ok
                                confirmacionEnvio = msg
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Switch 3: Modo de Operación de la Estación
                    FilaSwitchActuador(
                        titulo = "Modo de la Estación (Operando / Reposo)",
                        subtitulo = if (NetworkManagerIoT.sistemaEncendido) "Estado: EN OPERACIÓN" else "Estado: EN REPOSO (STANDBY)",
                        activado = NetworkManagerIoT.sistemaEncendido,
                        habilitado = tengoMando,
                        onCambio = {
                            NetworkManagerIoT.sistemaEncendido = it
                            if (!it) {
                                NetworkManagerIoT.alarmaSonoraActiva = false
                                NetworkManagerIoT.mensajeMatrizLed = "MODO STANDBY"
                            } else {
                                NetworkManagerIoT.mensajeMatrizLed = "ESTACION OK"
                            }
                            NetworkManagerIoT.registrarCambioLocalActuador("SWITCH_STANDBY") { ok, msg ->
                                envioExitoso = ok
                                confirmacionEnvio = msg
                            }
                        }
                    )
                }
            }

            // ================================================================================
            // SECCIÓN 3: AJUSTE DE LÍMITES DE ALERTA (Sliders)
            // ================================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. Ajuste de Límites de Alerta (Sliders)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Slider 1: Límite Máximo de Temperatura
                    Text(
                        text = "Límite Máximo de Temperatura: ${"%.1f".format(NetworkManagerIoT.umbralTemperaturaAlerta)} °C",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFC62828)
                    )
                    Text(
                        text = "Si la temperatura ambiente supera este valor, se activará la alarma sonora.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Slider(
                        value = NetworkManagerIoT.umbralTemperaturaAlerta,
                        enabled = tengoMando,
                        onValueChange = {
                            NetworkManagerIoT.umbralTemperaturaAlerta = (it * 10).roundToInt() / 10f
                        },
                        onValueChangeFinished = {
                            NetworkManagerIoT.registrarCambioLocalActuador("SLIDER_UMBRAL") { ok, msg ->
                                envioExitoso = ok
                                confirmacionEnvio = msg
                            }
                        },
                        valueRange = 15f..50f
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Slider 2: Límite Máximo de Iluminación
                    Text(
                        text = "Límite Máximo de Iluminación: ${NetworkManagerIoT.umbralLuzAlerta} Lux",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFEF6C00)
                    )
                    Text(
                        text = "Calibra el nivel de luz a partir del cual se emitirá advertencia.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Slider(
                        value = NetworkManagerIoT.umbralLuzAlerta.toFloat(),
                        enabled = tengoMando,
                        onValueChange = {
                            NetworkManagerIoT.umbralLuzAlerta = it.roundToInt()
                        },
                        onValueChangeFinished = {
                            NetworkManagerIoT.registrarCambioLocalActuador("SLIDER_UMBRAL") { ok, msg ->
                                envioExitoso = ok
                                confirmacionEnvio = msg
                            }
                        },
                        valueRange = 200f..1000f
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Slider 3: Simular Temperatura Ambiental
                    Text(
                        text = "Simular Temperatura Ambiental: ${"%.1f".format(NetworkManagerIoT.temperaturaC)} °C",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00695C)
                    )
                    Text(
                        text = "Pruebe mover bajo 3°C para simular helada o sobre 30°C para sobrecalentamiento.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Slider(
                        value = NetworkManagerIoT.temperaturaC,
                        enabled = tengoMando,
                        onValueChange = {
                            NetworkManagerIoT.temperaturaC = (it * 10).roundToInt() / 10f
                        },
                        onValueChangeFinished = {
                            NetworkManagerIoT.registrarCambioLocalActuador("SLIDER_TEMP_MANUAL") { ok, msg ->
                                envioExitoso = ok
                                confirmacionEnvio = msg
                            }
                        },
                        valueRange = -5f..50f
                    )
                }
            }

            // ================================================================================
            // SECCIÓN 4: ELEMENTOS INTERACTIVOS TIPO RADIOBUTTON (Modo Operativo)
            // ================================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. Selector de Régimen Operativo (RadioButtons)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Define la frecuencia de muestreo y consumo energético de la Estación Portátil.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    modosOperativos.forEach { (modo, explicacion) ->
                        val seleccionado = NetworkManagerIoT.modoOperacion == modo
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = tengoMando) {
                                    NetworkManagerIoT.modoOperacion = modo
                                    NetworkManagerIoT.registrarCambioLocalActuador("RADIO_MODO_OPERACION") { ok, msg ->
                                        envioExitoso = ok
                                        confirmacionEnvio = msg
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = seleccionado,
                                enabled = tengoMando,
                                onClick = {
                                    NetworkManagerIoT.modoOperacion = modo
                                    NetworkManagerIoT.registrarCambioLocalActuador("RADIO_MODO_OPERACION") { ok, msg ->
                                        envioExitoso = ok
                                        confirmacionEnvio = msg
                                    }
                                }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = modo,
                                    fontSize = 14.sp,
                                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
                                    color = if (seleccionado) MaterialTheme.colorScheme.primary else Color.Black
                                )
                                Text(
                                    text = explicacion,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // Mensaje de confirmación del envío inalámbrico
            if (confirmacionEnvio.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (envioExitoso) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = confirmacionEnvio,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (envioExitoso) Color(0xFF1B5E20) else Color(0xFFC62828)
                    )
                }
            }

            // Botón Principal para transmitir todos los parámetros configurados al otro dispositivo
            Button(
                onClick = {
                    NetworkManagerIoT.registrarCambioLocalActuador("COMANDO_ACTIVADOR_COMPLETO") { exito, mensaje ->
                        envioExitoso = exito
                        confirmacionEnvio = mensaje
                    }
                },
                enabled = tengoMando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D47A1)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
            ) {
                Text(
                    text = "📡 Sincronizar en Tiempo Real con PC y Teléfono",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Componente auxiliar para renderizar cada fila de Switch con su etiqueta y estado actual.
 */
@Composable
fun FilaSwitchActuador(
    titulo: String,
    subtitulo: String,
    activado: Boolean,
    habilitado: Boolean = true,
    onCambio: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (habilitado) Color.Black else Color.Gray
            )
            Text(
                text = subtitulo,
                fontSize = 12.sp,
                color = if (activado) Color(0xFF2E7D32) else Color(0xFF757575),
                fontWeight = if (activado) FontWeight.Bold else FontWeight.Normal
            )
        }
        Switch(
            checked = activado,
            enabled = habilitado,
            onCheckedChange = onCambio
        )
    }
}
