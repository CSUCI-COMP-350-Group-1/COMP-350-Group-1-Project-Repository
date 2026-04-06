package com.example.sprint1homeui.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = CoralRed,
    onPrimary = WarmWhite,
    secondary = HotPink,
    onSecondary = WarmWhite,
    tertiary = SunsetOrange,
    onTertiary = WarmWhite,
    background = WarmPeach,
    onBackground = Ink,
    surface = WarmWhite,
    onSurface = Ink,
    surfaceVariant = SoftBlush,
    onSurfaceVariant = Ink,
    outlineVariant = SoftBorder,
    errorContainer = ErrorFill,
    onErrorContainer = Ink
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralRed,
    onPrimary = WarmWhite,
    secondary = HotPink,
    onSecondary = WarmWhite,
    tertiary = SunsetOrange,
    onTertiary = WarmWhite,
    background = Ink,
    onBackground = WarmWhite,
    surface = ColorFallbackDarkSurface,
    onSurface = WarmWhite
)

@Composable
fun Sprint1HomeUITheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}