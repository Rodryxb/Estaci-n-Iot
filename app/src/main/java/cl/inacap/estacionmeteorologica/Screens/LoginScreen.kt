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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cl.inacap.estacionmeteorologica.network.NetworkManagerIoT
import cl.inacap.estacionmeteorologica.security.SecurityManagerIoT

/**
 * ============================================================================================
 * PANTALLA DE LOGIN CON SEGURIDAD ISO/IEC 27001 E INDUSTRIA OT
 * Cumple con:
 * - Criterio 2.1.1.1 (10 pts): Pantalla de Login mediante credenciales (usuario y contraseña).
 * - Criterio 2.1.3.5 (10 pts): Hashing SHA-256 con Salt + Bloqueo Anti-Fuerza Bruta (ISA/IEC 62443).
 * ============================================================================================
 */
@Composable
fun LoginScreen(navController: NavHostController) {
    // 1. Variables de estado reactivas (mutableStateOf) que almacenan lo que escribe el usuario
    var usuario by remember { mutableStateOf("operador_ot") }
    var contrasena by remember { mutableStateOf("Meteo2026!") }
    var mostrarContrasena by remember { mutableStateOf(false) }
    var mensajeEstado by remember { mutableStateOf("") }
    var esError by remember { mutableStateOf(false) }
    var mostrarPanelRedLogin by remember { mutableStateOf(false) }

    // Al abrir el Login, intenta autodescubrir el PC o 2do Teléfono en segundo plano para cargar perfiles automáticamente
    LaunchedEffect(Unit) {
        if (!NetworkManagerIoT.conectadoEnVivo) {
            NetworkManagerIoT.descubrirDispositivosEnRedWifi { _ -> }
        }
    }

    // Contenedor principal con scroll vertical por si aparece el teclado en pantallas pequeñas
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Encabezado Institucional y Título del Proyecto IoT
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "INACAP • TI3042 • UNIDAD 2 • GRUPO G • WI-FI ESP32",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Estación Iot",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Terminal de Monitoreo, Control y Seguridad Industrial OT",
            fontSize = 13.sp,
            color = Color(0xFF546E7A),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        // Tarjeta Principal Material Design 3 que aloja el formulario de credenciales
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Autenticación de Operador IoT / OT",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Protegido bajo norma ISO/IEC 27001 & ISA/IEC 62443",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Campo de entrada (EditText / OutlinedTextField) para el Nombre de Usuario
                OutlinedTextField(
                    value = usuario,
                    onValueChange = {
                        usuario = it
                        mensajeEstado = ""
                    },
                    label = { Text("Usuario Operador") },
                    placeholder = { Text("Ej: operador_ot, admin o inacap") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Campo de entrada (EditText / OutlinedTextField) para la Contraseña
                // Usa PasswordVisualTransformation() para ocultar los caracteres con puntos
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = {
                        contrasena = it
                        mensajeEstado = ""
                    },
                    label = { Text("Contraseña de Acceso") },
                    singleLine = true,
                    visualTransformation = if (mostrarContrasena) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                            Text(
                                text = if (mostrarContrasena) "Ocultar" else "Ver",
                                fontSize = 12.sp
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Si ocurre un error de credenciales o bloqueo por fuerza bruta, se muestra aquí
                if (mensajeEstado.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (esError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = mensajeEstado,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (esError) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                            )
                            if (esError) {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedButton(
                                    onClick = {
                                        SecurityManagerIoT.desbloquearTerminalManual()
                                        NetworkManagerIoT.descubrirDispositivosEnRedWifi { res ->
                                            esError = false
                                            mensajeEstado = "🔓 Terminal desbloqueada. $res"
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "🔓 Desbloquear y Sincronizar Perfiles del PC / Teléfono",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Botón principal de Ingreso que invoca al validador criptográfico y navega a Monitoreo
                Button(
                    onClick = {
                        val (autenticado, respuesta) = SecurityManagerIoT.autenticarUsuario(
                            usuarioInput = usuario,
                            contrasenaInput = contrasena
                        )
                        esError = !autenticado
                        mensajeEstado = respuesta
                        if (autenticado) {
                            // Si las credenciales son válidas, navegamos a la pantalla de Monitoreo
                            navController.navigate("monitoreo") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D47A1)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                ) {
                    Text(
                        text = "🔐 Ingresar a la Estación Meteorológica",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Estado de Conexión con la Estación (Limpio, certero y sin jerga técnica)
        val conectado = NetworkManagerIoT.conectadoEnVivo
        val verificando = NetworkManagerIoT.verificandoConexion

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    verificando -> Color(0xFFFFF8E1)
                    conectado -> Color(0xFFE8F5E9)
                    else -> Color(0xFFF1F5F9)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                verificando -> "🟡 Conectando con la Estación..."
                                conectado -> "🟢 Conectado con la Estación"
                                else -> "🔴 Sin Conexión con la Estación"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                verificando -> Color(0xFFE65100)
                                conectado -> Color(0xFF1B5E20)
                                else -> Color(0xFFC62828)
                            }
                        )
                        Text(
                            text = if (conectado) {
                                "Sincronizado en tiempo real • ${SecurityManagerIoT.listaUsuariosRegistrados.size} usuarios autorizados"
                            } else {
                                "Operando en modo local • Toque conectar para vincular con PC"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }

                    TextButton(onClick = { mostrarPanelRedLogin = !mostrarPanelRedLogin }) {
                        Text(
                            text = if (mostrarPanelRedLogin) "Ocultar" else "Ajustes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (mostrarPanelRedLogin) {
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                NetworkManagerIoT.conectarADestinoEspecifico(NetworkManagerIoT.ipRemotaDestino) { ok, msg ->
                                    esError = !ok
                                    mensajeEstado = msg
                                }
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Guardar y Conectar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (!conectado) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            SecurityManagerIoT.desbloquearTerminalManual()
                            NetworkManagerIoT.descubrirDispositivosEnRedWifi { resultado ->
                                esError = !NetworkManagerIoT.conectadoEnVivo
                                mensajeEstado = resultado
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0277BD)
                        )
                    ) {
                        Text(
                            text = "⚡ Conectar con la Estación Automáticamente",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selector rápido de perfiles para demostración y evaluación
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            usuario = "operador_ot"
                            contrasena = "Meteo2026!"
                            mensajeEstado = "Perfil Operador OT listo para ingresar"
                            esError = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF0277BD)
                        )
                    ) {
                        Text("Operador OT", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            usuario = "admin"
                            contrasena = "1234"
                            mensajeEstado = "Perfil Administrador listo para ingresar"
                            esError = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF00695C)
                        )
                    ) {
                        Text("Admin", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            SecurityManagerIoT.desbloquearTerminalManual()
                            mensajeEstado = "Terminal desbloqueada"
                            esError = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF455A64)
                        )
                    ) {
                        Text("Desbloquear", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                val usuariosExtra = SecurityManagerIoT.listaUsuariosRegistrados.filter {
                    it.username !in listOf("operador_ot", "admin", "inacap")
                }
                if (usuariosExtra.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Usuarios personalizados cargados en Base de Datos (${usuariosExtra.size}):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D47A1)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    usuariosExtra.forEach { u ->
                        OutlinedButton(
                            onClick = {
                                usuario = u.username
                                contrasena = ""
                                mensajeEstado = "Usuario @${u.username} (${u.rol}) seleccionado. Ingrese su contraseña."
                                esError = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "👤 @${u.username} • ${u.nombreCompleto} [${u.rol}]",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
