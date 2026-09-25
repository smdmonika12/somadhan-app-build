package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SomadhanOrange,
    onPrimary = Color.White,
    primaryContainer = SomadhanOrangeLight,
    onPrimaryContainer = SomadhanOrangePressed,
    secondary = SomadhanOrange,
    onSecondary = Color.White,
    secondaryContainer = SomadhanOrangeLight,
    onSecondaryContainer = SomadhanOrangePressed,
    tertiary = SomadhanInfo,
    onTertiary = Color.White,
    background = SomadhanBg,
    onBackground = SomadhanTextPrimary,
    surface = SomadhanSurface,
    onSurface = SomadhanTextPrimary,
    surfaceVariant = SomadhanSurfaceVariant,
    onSurfaceVariant = SomadhanTextSecondary,
    outline = SomadhanBorder,
    outlineVariant = SomadhanDivider,
    error = SomadhanError,
    onError = Color.White
)

private val DarkColorScheme = lightColorScheme(
    // As per user specification: Clean, high-contrast white + black + orange theme.
    primary = SomadhanOrange,
    onPrimary = Color.White,
    primaryContainer = SomadhanOrangeLight,
    onPrimaryContainer = SomadhanOrangePressed,
    secondary = SomadhanOrange,
    onSecondary = Color.White,
    background = SomadhanBg,
    onBackground = SomadhanTextPrimary,
    surface = SomadhanSurface,
    onSurface = SomadhanTextPrimary,
    surfaceVariant = SomadhanSurfaceVariant,
    onSurfaceVariant = SomadhanTextSecondary,
    outline = SomadhanBorder,
    error = SomadhanError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use custom consistent Somadhan branding
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
