package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonRed,
    onPrimary = Color.White,
    primaryContainer = DarkCrimson,
    onPrimaryContainer = SoftRed,
    secondary = GlowingCoral,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = SoftRed,
    tertiary = ElectricCyan,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkBorder,
    outlineVariant = Color(0xFF2E0D14),
    error = NeonRed
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = DarkCrimson,
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = Color(0xFFB71C1C),
    tertiary = Color(0xFF00838F),
    background = LightBackground,
    onBackground = Color(0xFF212121),
    surface = LightSurface,
    onSurface = Color(0xFF212121),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF424242),
    outline = LightBorder,
    outlineVariant = Color(0xFFE0E0E0),
    error = LightPrimary
)

@Composable
fun ProductLensTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
