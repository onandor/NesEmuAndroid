package com.onandor.nesemu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA5B1B0),
    onPrimary = Color(0xFF23282A),
    secondary = Color(0xFF8E9793),
    tertiary = Color(0xFF2F3638),
    onSecondary = Color(0xFF23282A),
    background = Color(0xFF23282A),
    onBackground = Color(0xFFB4C1C1),
    surface = Color(0xFF23282A),
    onSurface = Color(0xFFB4C1C1),
    secondaryContainer = Color(0xFF2F3638)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2D3436),
    onPrimary = Color(0xFFC6D4D3),
    secondary = Color(0xFF475355),
    onSecondary = Color(0xFFC6D4D3),
    tertiary = Color(0xFF929C9B),
    background = Color(0xFFA5B1B0),
    onBackground = Color(0xFF23282A),
    surface = Color(0xFFA5B1B0),
    onSurface = Color(0xFF23282A),
    secondaryContainer = Color(0xFFC6D4D3)
)

@OptIn(ExperimentalMaterial3Api::class)
private val NoRipple = RippleConfiguration(
    color = Color.Unspecified,
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0.0f,
        focusedAlpha = 0.0f,
        hoveredAlpha = 0.0f,
        pressedAlpha = 0.0f
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NesEmuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    CompositionLocalProvider(LocalRippleConfiguration provides NoRipple) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}