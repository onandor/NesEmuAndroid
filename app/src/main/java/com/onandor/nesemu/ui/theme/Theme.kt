package com.onandor.nesemu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5B1B0),
    onPrimary = Color(0xFF23282A),
    background = Color(0xFF23282A),
    onBackground = Color(0xFFB4C1C1),
    surface = Color(0xFF23282A),
    onSurface = Color(0xFFB4C1C1)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2D3436),
    onPrimary = Color(0xFFC6D4D3),
    background = Color(0xFFA5B1B0),
    onBackground = Color(0xFF23282A),
    surface = Color(0xFFA5B1B0),
    onSurface = Color(0xFF23282A)
)

@Composable
fun NesEmuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}