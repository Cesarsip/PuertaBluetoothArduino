package com.example.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Mi base de datos local: guarda los usuarios (con la contraseña hasheada y su salt)
// y todos los eventos de la puerta con su fecha y hora.
class BaseDatos(context: Context) : SQLiteOpenHelper(context, "puerta.db", null, 3) {

    // Se ejecuta la primera vez que se abre la base (o al subir la versión)
    // y crea las dos tablas con las 3 cuentas del proyecto.
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE usuarios (usuario TEXT PRIMARY KEY, clave TEXT, salt TEXT)")
        db.execSQL("CREATE TABLE registros (id INTEGER PRIMARY KEY AUTOINCREMENT, fecha TEXT, evento TEXT)")

        // Creo las 3 cuentas, cada una con su contraseña distinta.
        // A cada una le genero un salt aleatorio propio: así dos usuarios con la
        // misma contraseña tendrían hashes distintos y no se pueden adivinar
        // con tablas de hashes precalculadas.
        insertarUsuario(db, "Cesar", "C3s@r#Puerta2026")
        insertarUsuario(db, "Benjamin", "B3nj@m1n#IoT26")
        insertarUsuario(db, "Mati", "M4t1#Control!26")
    }

    // Acá genero el salt, calculo el hash y lo guardo; y lo muestro en Logcat
    // para poder ver el hasheo en vivo desde Android Studio.
    private fun insertarUsuario(db: SQLiteDatabase, usuario: String, clave: String) {
        val salt = generarSalt()
        val hash = cifrar(clave, salt)

        val values = ContentValues()
        values.put("usuario", usuario)
        values.put("clave", hash)
        values.put("salt", salt)
        db.insert("usuarios", null, values)

        // Formato de Logcat: Usuario | Nuevo Salt | Hash SHA-256
        // (la contraseña en claro nunca se imprime en ningún lado)
        Log.d("LoginHash", "Usuario: $usuario | Nuevo Salt: $salt | Hash SHA-256: $hash")
    }

    // Si subo la versión de la base, borro todo y la recreo con las cuentas nuevas.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS usuarios")
        db.execSQL("DROP TABLE IF EXISTS registros")
        onCreate(db)
    }

    // Valido el login: busco el salt del usuario, calculo el hash de la contraseña
    // escrita con ese salt y lo comparo con el guardado. Nunca comparo en claro.
    fun verificar(usuario: String, clave: String): Boolean {
        val cursor = readableDatabase.rawQuery(
            "SELECT clave, salt FROM usuarios WHERE usuario = ?",
            arrayOf(usuario)
        )
        if (!cursor.moveToFirst()) {
            cursor.close()
            return false
        }
        val hashGuardado = cursor.getString(0)
        val salt = cursor.getString(1)
        cursor.close()

        val hashCalculado = cifrar(clave, salt)

        // Muestro en Logcat el salt y el hash que se calculó al intentar entrar,
        // con el mismo formato que al crear el usuario.
        Log.d("LoginHash", "Usuario: $usuario | Nuevo Salt: $salt | Hash SHA-256: $hashCalculado")

        return hashGuardado == hashCalculado
    }

    // Genero un salt aleatorio de 16 bytes (32 caracteres en hexadecimal).
    // Uso SecureRandom para que no se pueda predecir.
    private fun generarSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Guardo un evento con fecha y hora: sirve para saber quién inició sesión,
    // quién se conectó al módulo, qué comandos llegaron, etc.
    fun guardarRegistro(evento: String) {
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        val values = ContentValues()
        values.put("fecha", fecha)
        values.put("evento", evento)
        writableDatabase.insert("registros", null, values)
    }

    // Leo todos los eventos ordenados de más reciente a más antiguo
    // para mostrarlos en la pantalla de historial.
    fun leerRegistros(): List<String> {
        val lista = mutableListOf<String>()
        val cursor = readableDatabase.rawQuery(
            "SELECT fecha, evento FROM registros ORDER BY id DESC",
            null
        )
        while (cursor.moveToNext()) {
            lista.add("${cursor.getString(0)}  ${cursor.getString(1)}")
        }
        cursor.close()
        return lista
    }

    // Calculo el hash SHA-256 de salt + contraseña: 64 caracteres en hexadecimal.
    // Al mezclar el salt, el mismo texto da un hash distinto en cada usuario.
    private fun cifrar(texto: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest((salt + texto).toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
