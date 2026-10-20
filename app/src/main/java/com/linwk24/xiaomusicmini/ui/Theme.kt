package com.linwk24.xiaomusicmini.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B9BFF),
    onPrimary = Color(0xFF0B1020),
    primaryContainer = Color(0xFF2A3260),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = Color(0xFFC7A6FF),
    background = Color(0xFF0B0D14),
    onBackground = Color(0xFFEEF0F6),
    surface = Color(0xFF12151F),
    onSurface = Color(0xFFEEF0F6),
    surfaceVariant = Color(0xFF1C2130),
    onSurfaceVariant = Color(0xFF9AA1B5),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5867D8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE1FF),
    onPrimaryContainer = Color(0xFF141A3A),
    secondary = Color(0xFF7C4DFF),
    background = Color(0xFFF4F6FC),
    onBackground = Color(0xFF171A26),
    surface = Color.White,
    onSurface = Color(0xFF171A26),
    surfaceVariant = Color(0xFFE9ECF7),
    onSurfaceVariant = Color(0xFF5C6275),
)

@Composable
fun XiaomusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
