package com.example.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.app.ui.theme.AppTheme

// Punto de entrada de la app: acá decido qué pantalla se muestra según
// si hay sesión iniciada o no.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Uso mi tema oscuro fijo para que la app se vea igual en todos los celulares.
            AppTheme {
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

