package com.uj.homebill.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BlueDarkMode,
    onPrimary = DarkText,
    primaryContainer = BlueDarkAccent,
    onPrimaryContainer = White,
    secondary = BlueDarkAccent,
    onSecondary = DarkText,
    secondaryContainer = Color(0xFF0D47A1),
    onSecondaryContainer = White,
    tertiary = BlueDarkMode,
    onTertiary = DarkText,
    tertiaryContainer = BlueDarkAccent,
    onTertiaryContainer = White,
    background = DarkBackground,
    onBackground = White,
    surface = DarkSurface,
    onSurface = White,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFE0E0E0),
    error = RedError,
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = BlueAccent,
    onPrimary = White,
    primaryContainer = BlueLight,
    onPrimaryContainer = BlueDark,
    secondary = BlueDark,
    onSecondary = White,
    secondaryContainer = BlueSoft,
    onSecondaryContainer = BlueDark,
    tertiary = BlueAccent,
    onTertiary = White,
    tertiaryContainer = BlueLight,
    onTertiaryContainer = BlueDark,
    background = LightGray,
    onBackground = DarkText,
    surface = White,
    onSurface = DarkText,
    surfaceVariant = WhiteSmoke,
    onSurfaceVariant = GrayText,
    error = RedError,
    onError = White
)

@Composable
fun HomeBillTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled to keep consistent blue/white theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
