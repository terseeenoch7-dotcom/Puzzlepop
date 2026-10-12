package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PuzzlePopColorScheme = darkColorScheme(
    primary = PopGoldPrimary,
    onPrimary = Color.Black,
    primaryContainer = PopGoldSecondary,
    secondary = PopCyan,
    onSecondary = Color.White,
    tertiary = PopEmerald,
    background = DeepIndigoBg,
    onBackground = Color.White,
    surface = CardSurfaceIndigo,
    onSurface = Color.White,
    surfaceVariant = DarkNavySurface,
    onSurfaceVariant = Color.White.copy(alpha = 0.85f)
)

@Composable
fun PuzzlePopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PuzzlePopColorScheme,
        typography = Typography,
        content = content
    )
}

// Retain alias for any existing reference
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    PuzzlePopTheme(content = content)
}
