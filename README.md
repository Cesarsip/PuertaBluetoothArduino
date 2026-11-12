# Control de Puerta IoT — EVA 2 (TI3042)

Aplicación móvil Android (Kotlin + Jetpack Compose) que controla una puerta por Bluetooth
conectada a un módulo IoT basado en Arduino Uno + HC-05. Evaluación sumativa de la
Unidad 2, curso *Aplicaciones Móviles Android*.

## Funcionalidades

- **Login con credenciales**: pantalla de usuario y contraseña con base de datos local (SQLite).
- **Credenciales cifradas**: las contraseñas se guardan con **SHA-256 + un salt único por usuario**
  (nunca en texto plano). Se puede verificar el hash desde Logcat (tag `LoginHash`).
- **Registro de eventos**: cada inicio/cierre de sesión, conexión y mensaje del sensor queda
  guardado en SQLite con fecha y hora, visible en la pantalla de historial.
- **Conexión Bluetooth (RFCOMM/SPP)** con el módulo HC-05 en un hilo aparte.
- **Reconexión automática**: si la conexión se corta (módulo apagado, fuera de rango,
  Bluetooth del teléfono apagado), la app reintenta cada 5 segundos sola; se puede
  activar/desactivar con un switch.
- **Seguridad ISO 27400**: cada comando se envía firmado con **HMAC-SHA256** y un
  **contador anti-replay** guardado en la EEPROM del Arduino (rechaza comandos reenviados).
- **Material Design**: indicador circular LOCKED/UNLOCKED, botones con estados de color,
  botón de acción deshabilitado sin conexión, permisos de Bluetooth en tiempo de ejecución.

## Credenciales

| Usuario | Contraseña |
|---------|------------|
| Cesar | `C3s@r#Puerta2026` |
| Benjamin | `B3nj@m1n#IoT26` |
| Mati | `M4t1#Control!26` |

## Estructura del repositorio

```
IOT/
├── App/                      # Proyecto Android (Kotlin + Jetpack Compose)
│   └── app/src/main/java/com/example/app/
│       ├── MainActivity.kt       # Punto de entrada: decide login o pantalla principal
│       ├── PantallaLogin.kt      # Login con usuario y contraseña
│       ├── PantallaPuerta.kt     # Pantalla principal, historial y switch de reconexión
│       ├── GestorConexion.kt     # Bluetooth: conectar, enviar, recibir y reconectar
│       ├── BaseDatos.kt          # SQLite: usuarios con hash+salt y tabla de eventos
│       └── SeguridadISO.kt       # Contador anti-replay y firma HMAC-SHA256
└── Arduino/ControlPuerta/
    └── ControlPuerta.ino     # Sketch: relé, sensor magnético, validación de firma
```

## Cómo compilar

1. Abrir la carpeta `App/` en Android Studio.
2. Sincronizar Gradle y ejecutar en un emulador o teléfono (API 31+ para los permisos nuevos).
3. Desde terminal: `gradlew assembleDebug` → APK en `app/build/outputs/apk/debug/`.

## Cómo ver el hasheo en Logcat

En Android Studio, pestaña **Logcat** y filtrar por `tag:LoginHash`. Al crear las cuentas
y al iniciar sesión se muestra:

```
Usuario: Cesar | Nuevo Salt: 9f2a7c1e4b8d03a5... | Hash SHA-256: 1c9e...
```

## Arduino

- **Pin 8**: relé (abrir/cerrar cerradura) — **Pin 4**: sensor magnético (reed switch)
- **Pines 2 y 3**: módulo HC-05 (SoftwareSerial a 9600 baudios)
- Cargar el sketch con el IDE de Arduino; el módulo debe estar emparejado con el teléfono.

## Herramientas

Android Studio · Kotlin · Jetpack Compose · SQLite · Arduino Uno · Bluetooth HC-05 ·
norma ISO 27400 (cifrado de credenciales, control de acceso, integridad de comandos).
