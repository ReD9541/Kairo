package com.example.kairo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LunarDarkColorScheme = darkColorScheme(
    primary = LunarCyanPrimary,
    onPrimary = Color(0xFF06080D),
    primaryContainer = LunarCyanDark,
    onPrimaryContainer = Color.White,
    secondary = LunarSilverHighland,
    onSecondary = Color(0xFF06080D),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = LunarSilverHighland,
    tertiary = LunarMineralGold,
    onTertiary = Color(0xFF06080D),
    background = Color.Transparent, // Gradient handled by AnimatedStarsBackground
    onBackground = OnDarkSurface,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    error = Color(0xFFF87171),
    onError = Color.Black
)

@Composable
fun KairoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LunarDarkColorScheme,
        typography = Typography,
        content = content
    )
}
