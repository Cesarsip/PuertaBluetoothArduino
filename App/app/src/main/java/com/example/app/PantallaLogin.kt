package com.example.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Pantalla de Login: solo deja pasar a quien tenga usuario y contraseña válidos.
@Composable
fun PantallaLogin(alIngresar: (String) -> Unit) {
    val contexto = LocalContext.current

    // Guardo lo que se escribe en los campos y el mensaje de error si falla el login.
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Control de Puerta",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Inicia sesión para continuar",
            fontSize = 14.sp,
            color = Color(0xFF616161)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Los dos campos del login; el de la contraseña va con puntos para que no se lea.
        OutlinedTextField(
            value = usuario,
            onValueChange = { usuario = it },
            label = { Text("Usuario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = clave,
            onValueChange = { clave = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        // Si algo salió mal, muestro el error en rojo debajo de los campos.
        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = error,
                color = Color(0xFFC62828),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Al presionar Ingresar: valido contra SQLite y, si coincide, registro
        // el evento de sesión con el nombre de quien entró y paso a la pantalla principal.
        Button(
            onClick = {
                val db = BaseDatos(contexto)
                if (db.verificar(usuario.trim(), clave)) {
                    db.guardarRegistro("Sesión iniciada: ${usuario.trim()}")
                    alIngresar(usuario.trim())
                } else {
                    error = "Usuario o contraseña incorrectos"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(text = "Ingresar", fontSize = 18.sp)
        }
    }
}
