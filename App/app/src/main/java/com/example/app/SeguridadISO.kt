package com.example.app

import android.content.Context
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Acá van las medidas de seguridad ISO: el contador anti-replay y la firma HMAC.
object SeguridadISO {

    // Clave secreta que comparto entre la app y el Arduino; no se muestra en la pantalla.
    private const val CLAVE = "TI3042-Puerta-2026"

    // Devuelvo el siguiente número de contador y lo guardo en el teléfono.
    // Cada comando usa un número mayor al anterior, así el Arduino puede
    // detectar si alguien reintenta un comando que ya se usó.
    fun siguienteContador(context: Context): Long {
        val prefs = context.getSharedPreferences("seguridad", Context.MODE_PRIVATE)
        val contador = prefs.getLong("contador", 0L) + 1L
        prefs.edit().putLong("contador", contador).apply()
        return contador
    }

    // Armo el token completo que le mando al Arduino: COMANDO|CONTADOR|FIRMA.
    fun generarToken(comando: String, contador: Long): String {
        val datos = "$comando|$contador"
        return "$datos|${firmar(datos)}"
    }

    // Calculo la firma HMAC-SHA256 con mi clave secreta: es como un sello que
    // solo yo y el Arduino podemos verificar, y cambia si cambia el comando.
    private fun firmar(datos: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(CLAVE.toByteArray(), "HmacSHA256"))
        val bytes = mac.doFinal(datos.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
