## Credenciales

| Usuario | Rol | Contraseña |
|---------|-----|------------|
| Administrador | Administrador | `Adm1n#Puerta2026` |
| Cesar | Usuario | `C3s@r#Puerta2026` |
| Benjamin | Usuario | `B3nj@m1n#IoT26` |
| Mati | Usuario | `M4t1#Control!26` |

La tabla `usuarios` incluye la columna `rol` (Administrador o Usuario) y el evento de
sesión guarda el rol de quien entra.

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
│       ├── SeguridadISO.kt       # Contador anti-replay y firma HMAC-SHA256
│       └── ui/theme/Theme.kt     # Tema oscuro fijo (Material 3)
└── Arduino/ControlPuerta/
    └── ControlPuerta.ino     # Sketch: relé, sensor magnético, validación de firma
```
