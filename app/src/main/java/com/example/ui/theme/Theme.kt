package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlueDark,
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFD2E3FC),
    secondary = LightBlueDarkAccent,
    secondaryContainer = Color(0xFF1B2E4B),
    onSecondaryContainer = Color(0xFF8AB4F8),
    tertiary = Color(0xFF5C93FC),
    onTertiary = MidnightDbBg,
    tertiaryContainer = Color(0xFF0F1E36),
    onTertiaryContainer = Color(0xFFECEFF4),
    background = MidnightDbBg,
    surface = SleekMidnightSurface,
    surfaceVariant = Color(0xFF1C253B),
    onPrimary = MidnightDbBg,
    onSecondary = OnDarkMidnightBg,
    onBackground = OnDarkMidnightBg,
    onSurface = OnDarkMidnightBg,
    onSurfaceVariant = Color(0xFFC4C7C5),
    outline = Color(0xFF43474E),
    outlineVariant = Color(0xFF3B3B3B)
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlueLight,
    primaryContainer = Color(0xFFD2E3FC),
    onPrimaryContainer = RoyalBlueLight,
    secondary = LightBlueAccent,
    secondaryContainer = Color(0xFFE8F0FE),
    onSecondaryContainer = Color(0xFF0A46DE),
    tertiary = Color(0xFF1976D2),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F0FE),
    onTertiaryContainer = Color(0xFF1976D2),
    background = WhiteBlueBg,
    surface = SoftBlueSurface,
    surfaceVariant = Color(0xFFE1E8F5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = OnWhiteBlueBg,
    onSurface = OnWhiteBlueBg,
    onSurfaceVariant = Color(0xFF3F4756),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C7C5)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
