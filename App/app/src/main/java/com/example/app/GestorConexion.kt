package com.example.app

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

// Yo manejo acá todo el Bluetooth del proyecto: conectarme al módulo HC-05,
// mandarle comandos y escuchar lo que me manda el Arduino.
class GestorConexion(private val context: Context) {

    // Estado actual de la conexión, que la pantalla va leyendo para mostrarlo.
    // "private set" es para que solo esta clase pueda cambiarlo.
    var estado by mutableStateOf("Desconectado")
        private set

    // Último mensaje que llegó del Arduino (LOCK:OPEN, SENSOR:CLOSED, etc.).
    var mensaje by mutableStateOf("")
        private set

    // Si está en true, la app se reconecta sola si se corta la conexión.
    var reconexionAutomatica = true

    // El "cable" virtual con el HC-05; es null mientras no esté conectado.
    private var socket: BluetoothSocket? = null

    // Con estas dos banderas controlo que solo haya un intento de conexión a la vez
    // y que se deje de reintentar cuando el usuario lo pida.
    private var autoReconectar = false
    private var conectando = false

    // Reviso si ya tengo los permisos de Bluetooth; en Android 11 o anterior no existen.
    fun permisosConcedidos(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val connect = ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
        val scan = ContextCompat.checkSelfPermission(
            context, Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED
        return connect && scan
    }

    // Conecto en un hilo aparte para que la pantalla no se congele mientras intento.
    fun conectar() {
        autoReconectar = true
        if (conectando) return
        conectando = true

        Thread {
            // Este bucle es el que me permite reintentar: mientras autoReconectar
            // sea true, sigo intentando conectar una y otra vez.
            while (autoReconectar) {
                try {
                    estado = "Conectando..."

                    val manager =
                        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                    val adaptador = manager.adapter

                    // Si el Bluetooth está apagado o el HC-05 no está emparejado,
                    // me detengo con un mensaje claro para que el usuario sepa qué falta.
                    if (adaptador == null || !adaptador.isEnabled) {
                        estado = "Bluetooth apagado"
                        conectando = false
                        return@Thread
                    }
                    val modulo = adaptador.bondedDevices.firstOrNull {
                        it.name?.contains("HC-05", ignoreCase = true) == true
                    }
                    if (modulo == null) {
                        estado = "HC-05 no emparejado"
                        conectando = false
                        return@Thread
                    }

                    adaptador.cancelDiscovery()

                    // El UUID es el del perfil serie (SPP), el que usa el HC-05.
                    val nuevoSocket = modulo.createRfcommSocketToServiceRecord(
                        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    )
                    nuevoSocket.connect()

                    socket = nuevoSocket
                    estado = "Conectado a ${modulo.name}"
                    conectando = false
                    escuchar(nuevoSocket)
                    return@Thread

                } catch (e: Exception) {
                    // Acá está la reconexión automática: si falla y está activada,
                    // espero 5 segundos y vuelvo a intentar solo, sin tocar la pantalla.
                    if (reconexionAutomatica) {
                        estado = "Reconectando..."
                        try {
                            Thread.sleep(5000)
                        } catch (e2: Exception) {
                        }
                    } else {
                        estado = "Error: ${e.message}"
                        conectando = false
                        return@Thread
                    }
                }
            }
            conectando = false
        }.start()
    }

    // Este hilo queda leyendo en bucle todo lo que llega del Arduino.
    private fun escuchar(bluetoothSocket: BluetoothSocket) {
        Thread {
            try {
                val lector = BufferedReader(InputStreamReader(bluetoothSocket.inputStream))
                while (true) {
                    val linea = lector.readLine() ?: break
                    if (linea.isNotBlank()) mensaje = linea.trim()
                }
                estado = "Desconectado"
            } catch (e: Exception) {
                estado = "Desconectado"
            }
            cerrarSocket()

            // Si la conexión se cortó sola (alguien apagó el módulo, se fue de rango)
            // y yo no pedí desconectarme, me vuelvo a conectar sin intervención del usuario.
            if (autoReconectar && reconexionAutomatica) {
                estado = "Reconectando..."
                conectar()
            }
        }.start()
    }

    // Armo el token de seguridad y lo mando en un hilo aparte para no trabar la pantalla.
    fun enviar(comando: String) {
        Thread {
            try {
                val contador = SeguridadISO.siguienteContador(context)
                val token = SeguridadISO.generarToken(comando, contador)
                socket?.outputStream?.write("$token\n".toByteArray())
            } catch (e: Exception) {
                estado = "Error al enviar"
            }
        }.start()
    }

    // Desconexión manual: el usuario tocó "Desconectar", así que dejo de reintentar.
    fun desconectar() {
        autoReconectar = false
        cerrarSocket()
        estado = "Desconectado"
    }

    // Cierro el socket ignorando errores, para que nunca se crashee la app al cerrar.
    private fun cerrarSocket() {
        try {
            socket?.close()
        } catch (e: Exception) {
        }
        socket = null
    }

    // La pantalla la llama al destruirse: apago todo para no dejar hilos corriendo.
    fun cerrar() {
        autoReconectar = false
        desconectar()
    }
}
