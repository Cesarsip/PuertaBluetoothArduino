package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// Punto de entrada de la app: acá decido qué pantalla se muestra según
// si hay sesión iniciada o no.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Guardo el nombre de quien inició sesión. Si está vacío muestro el
                // login; si tiene un nombre muestro la puerta y ese nombre es el que
                // queda registrado en los eventos.
                var usuarioSesion by remember { mutableStateOf("") }

                if (usuarioSesion.isNotEmpty()) {
                    PantallaPuerta(usuario = usuarioSesion, onSalir = { usuarioSesion = "" })
                } else {
                    PantallaLogin(alIngresar = { usuarioSesion = it })
                }
            }
        }
    }
}

