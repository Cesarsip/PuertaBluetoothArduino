package com.example.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Pantalla de Login: solo deja pasar a quien tenga usuario y contraseña válidos.
// Si se equivoca 3 veces seguidas bloqueo el ingreso con un tiempo que crece
// cada vez: 5 minutos la primera vez, 15 la segunda, 45 la tercera (tope 60),
// para que nadie pueda adivinar la contraseña probando una y otra vez.
@Composable
fun PantallaLogin(alIngresar: (String, String) -> Unit) {
    val contexto = LocalContext.current

    // Guardo lo que se escribe en los campos y el mensaje de error si falla el login.
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    // Ojito del campo contraseña: alterna entre mostrar la clave en texto plano
    // o dejarla oculta con puntos, como en cualquier app de verdad.
    var mostrarClave by remember { mutableStateOf(false) }

    // Contador de intentos fallidos de la ronda actual: con 3 se bloquea.
    // "bloqueos" cuenta cuántas veces se bloqueó ya, para subir el tiempo.
    var intentos by remember { mutableStateOf(0) }
    var bloqueos by remember { mutableStateOf(0) }

    // Momento en que se desbloquea (milisegundos desde 1970). 0 = sin bloqueo.
    var bloqueadoHasta by remember { mutableStateOf(0L) }

    // Cuenta regresiva que se muestra mientras dure el bloqueo.
    var restante by remember { mutableStateOf("") }

    val bloqueado = bloqueadoHasta > System.currentTimeMillis()

    // Cada segundo recalculo cuánto falta para poder volver a intentar.
    LaunchedEffect(bloqueadoHasta) {
        while (bloqueadoHasta > System.currentTimeMillis()) {
            val seg = ((bloqueadoHasta - System.currentTimeMillis()) / 1000).toInt()
            restante = "${seg / 60}:${(seg % 60).toString().padStart(2, '0')}"
            delay(1000)
        }
        restante = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Control de Puerta",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFECECEC)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Inicia sesión para continuar",
            fontSize = 14.sp,
            color = Color(0xFF9E9E9E)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Los dos campos del login. El de contraseña va con teclado de contraseña,
        // puntos para que no se lea y un ojito para mostrarla si hace falta.
        OutlinedTextField(
            value = usuario,
            onValueChange = {
                usuario = it
                error = ""
            },
            label = { Text("Usuario") },
            singleLine = true,
            enabled = !bloqueado,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = clave,
            onValueChange = {
                clave = it
                error = ""
            },
            label = { Text("Contraseña") },
            singleLine = true,
            enabled = !bloqueado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (mostrarClave) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                // Al tocar el ojito cambio entre ver y ocultar la contraseña.
                IconButton(onClick = { mostrarClave = !mostrarClave }) {
                    Icon(
                        imageVector = if (mostrarClave) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = if (mostrarClave) {
                            "Ocultar contraseña"
                        } else {
                            "Mostrar contraseña"
                        },
                        tint = Color(0xFF9E9E9E)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Si algo salió mal, muestro el error en rojo claro debajo de los campos.
        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = Color(0xFFEF9A9A),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Al presionar Ingresar: valido contra SQLite y, si coincide, registro
        // el evento de sesión con el nombre de quien entró y paso a la pantalla principal.
        // Si falla, sumo un intento; al tercero bloqueo con tiempo creciente.
        Button(
            onClick = {
                if (bloqueadoHasta > System.currentTimeMillis()) return@Button
                val db = BaseDatos(contexto)
                if (db.verificar(usuario.trim(), clave)) {
                    // Entró bien: reinicio los contadores del bloqueo y
                    // paso a la pantalla principal el usuario y su rol.
                    intentos = 0
                    bloqueos = 0
                    bloqueadoHasta = 0
                    val rol = db.obtenerRol(usuario.trim())
                    db.guardarRegistro("Sesión iniciada: ${usuario.trim()} ($rol)")
                    alIngresar(usuario.trim(), rol)
                } else {
                    // Cada intento fallido queda guardado en el historial con el
                    // usuario que se intentó usar, para que se pueda auditar.
                    intentos++
                    db.guardarRegistro("Intento de sesión fallido: ${usuario.trim()} (intento $intentos de 3)")
                    if (intentos >= 3) {
                        // Tercer fallo seguido: bloqueo con un tiempo que crece
                        // 5, 15 y 45 minutos (después tope de 60) y lo registro
                        // como evento de seguridad en el historial.
                        bloqueos++
                        var minutos = 5
                        repeat(bloqueos - 1) { minutos *= 3 }
                        minutos = minOf(minutos, 60)
                        bloqueadoHasta = System.currentTimeMillis() + minutos * 60_000L
                        intentos = 0
                        error = "3 intentos fallidos. Acceso bloqueado durante $minutos minutos."
                        db.guardarRegistro("Acceso bloqueado: $minutos minutos tras 3 intentos fallidos (${usuario.trim()})")
                    } else {
                        // Aviso cuántos intentos le quedan antes de bloquearse.
                        error = "Usuario o contraseña incorrectos. Intentos restantes: ${3 - intentos}"
                    }
                }
            },
            enabled = !bloqueado, // mientras dure el bloqueo el botón queda deshabilitado
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = when {
                    bloqueado -> "Bloqueado ($restante)"
                    else -> "Ingresar"
                },
                fontSize = 18.sp
            )
        }
    }
}
