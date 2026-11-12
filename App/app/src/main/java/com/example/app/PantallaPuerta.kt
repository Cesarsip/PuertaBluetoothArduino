package com.example.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Pantalla principal en tono oscuro: cabecera con el estado del Bluetooth,
// indicador de la puerta, tarjeta con el último evento y los botones.
// Todo sobrio y legible, sin animaciones llamativas.
@Composable
fun PantallaPuerta(usuario: String, onSalir: () -> Unit = {}) {
    val contexto = LocalContext.current
    val gestor = remember { GestorConexion(contexto.applicationContext) }
    val db = remember { BaseDatos(contexto) }

    // Recuerdo el estado de la puerta y del sensor aunque la pantalla gire.
    var puertaAbierta by rememberSaveable { mutableStateOf(false) }
    var estadoSensor by rememberSaveable { mutableStateOf("Sin datos") }
    var mostrarHistorial by rememberSaveable { mutableStateOf(false) }
    var registros by remember { mutableStateOf(listOf<String>()) }

    // Último evento guardado en SQLite: lo muestro en pantalla para que en la
    // demo se vea que la app registra datos sin tener que abrir el historial.
    var ultimoEvento by remember { mutableStateOf("Sin eventos todavía") }

    // Para pedir los permisos de Bluetooth en Android 12 o superior:
    // si me los conceden, me conecto directo.
    val lanzadorPermisos = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        if (permisos.values.all { it }) gestor.conectar()
    }

    // Cada mensaje que llega del Arduino: actualizo la pantalla según el tipo
    // y lo guardo como evento en SQLite para que quede en el historial.
    LaunchedEffect(gestor.mensaje) {
        val mensaje = gestor.mensaje
        when {
            mensaje.startsWith("LOCK:OPEN") -> puertaAbierta = true
            mensaje.startsWith("LOCK:CLOSED") -> puertaAbierta = false
            mensaje.startsWith("SENSOR:OPEN") -> estadoSensor = "Puerta abierta"
            mensaje.startsWith("SENSOR:CLOSED") -> estadoSensor = "Puerta cerrada"
            mensaje.startsWith("ERROR") -> estadoSensor = "Error del módulo"
        }
        if (mensaje.isNotBlank()) {
            db.guardarRegistro(mensaje)
            ultimoEvento = mensaje
        }
    }

    // Registro los eventos de conexión comparando el estado actual con el anterior:
    // cuando alguien se une, cuando se corta y cuando entra la reconexión automática.
    var estadoAnterior by remember { mutableStateOf(gestor.estado) }
    LaunchedEffect(gestor.estado) {
        val estado = gestor.estado
        val anterior = estadoAnterior
        if (estado != anterior) {
            if (estado.startsWith("Conectado") && !anterior.startsWith("Conectado")) {
                db.guardarRegistro("Módulo HC-05 conectado")
                ultimoEvento = "Módulo HC-05 conectado"
            } else if (!estado.startsWith("Conectado") && anterior.startsWith("Conectado")) {
                db.guardarRegistro("Conexión Bluetooth perdida")
                ultimoEvento = "Conexión Bluetooth perdida"
            } else if (estado == "Reconectando..." && anterior != "Reconectando...") {
                db.guardarRegistro("Reconexión automática en curso")
                ultimoEvento = "Reconexión automática en curso"
            }
            estadoAnterior = estado
        }
    }

    // Cuando abro el historial, leo los eventos guardados.
    LaunchedEffect(mostrarHistorial) {
        if (mostrarHistorial) registros = db.leerRegistros()
    }

    // Si la pantalla se destruye, apago la conexión para no dejar hilos corriendo.
    DisposableEffect(Unit) {
        onDispose { gestor.cerrar() }
    }

    val estado = gestor.estado
    val conectado = estado.startsWith("Conectado")

    // Pido los permisos solo si faltan; si ya los tengo, me conecto.
    fun solicitarConexion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !gestor.permisosConcedidos()) {
            lanzadorPermisos.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            gestor.conectar()
        }
    }

    // Si pidió ver el historial, muestro la lista de eventos y corto acá.
    if (mostrarHistorial) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Historial de eventos",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFECECEC)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (registros.isEmpty()) {
                Text(
                    text = "Sin registros todavía",
                    color = Color(0xFF9E9E9E)
                )
            }

            // Cada evento es una tarjeta con su fecha, hora y texto.
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(registros) { registro ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF1E1E1E)
                        )
                    ) {
                        Text(
                            text = registro,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = Color(0xFFECECEC)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { mostrarHistorial = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Volver")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Cabecera: saludo con el nombre de quien entró, estado del Bluetooth y
        // botón de salir. El fondo de la tarjeta cambia según si estoy conectado.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (conectado) Color(0xFF1B3A20) else Color(0xFF3A1B1B)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hola, $usuario",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFECECEC)
                    )
                    Text(
                        text = "Bluetooth: $estado",
                        fontSize = 13.sp,
                        color = if (conectado) Color(0xFF81C784) else Color(0xFFEF9A9A)
                    )
                }

                // Al salir, registro el evento con el nombre de quien cerró sesión.
                TextButton(onClick = {
                    db.guardarRegistro("Sesión cerrada: $usuario")
                    onSalir()
                }) {
                    Text(text = "Salir", color = Color(0xFFBDBDBD))
                }
            }
        }

        // Indicador central: el círculo cambia de verde a rojo según el estado
        // de la puerta, con el estado del sensor debajo.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(
                        color = if (puertaAbierta) Color(0xFFE53935) else Color(0xFF43A047),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (puertaAbierta) "UNLOCKED" else "LOCKED",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (puertaAbierta) "Puerta Abierta" else "Puerta Bloqueada",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFECECEC)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Sensor: $estadoSensor",
                fontSize = 14.sp,
                color = Color(0xFF9E9E9E)
            )
        }

        // Tarjeta con el último evento guardado en la base de datos.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ÚLTIMO EVENTO",
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFF9E9E9E)
                )
                Text(
                    text = ultimoEvento,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFECECEC)
                )
            }
        }

        // Botón principal: manda OPEN o CLOSE firmado al Arduino.
        // Está deshabilitado si no hay conexión, como pide Material Design.
        Button(
            onClick = {
                val comando = if (puertaAbierta) "CLOSE" else "OPEN"
                gestor.enviar(comando)
                puertaAbierta = !puertaAbierta
            },
            enabled = conectado,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (puertaAbierta) Color(0xFF43A047) else Color(0xFFE53935),
                disabledContainerColor = Color(0xFF2A2A2A),
                disabledContentColor = Color(0xFF9E9E9E)
            )
        ) {
            Text(
                text = if (puertaAbierta) "Bloquear Puerta" else "Abrir Puerta",
                fontSize = 18.sp
            )
        }

        // Botón de conexión: abajo y ancho para que el texto no se parta en el celular.
        Button(
            onClick = {
                if (conectado) gestor.desconectar() else solicitarConexion()
            },
            enabled = estado != "Conectando...",
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (conectado) "Desconectar" else "Conectar",
                fontSize = 16.sp
            )
        }

        TextButton(onClick = { mostrarHistorial = true }) {
            Text(text = "Ver historial", color = Color(0xFFBDBDBD))
        }
    }
}
