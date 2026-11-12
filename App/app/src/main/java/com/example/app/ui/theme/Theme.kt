package com.example.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta fija en tono oscuro para toda la app: fondo casi negro, tarjetas
// gris oscuro y un verde sobrio como color principal. Sin color dinámico
// para que se vea igual en todos los celulares.
private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF102612),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = Color(0xFFEF9A9A)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaOscuro,
        typography = Typography,
        content = content
    )
}
