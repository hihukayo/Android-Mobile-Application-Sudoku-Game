package com.example.sudoku.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ---- 与 Flutter 版一致的配色 ----
val Blue = Color(0xFF0B4CFF)
val Red = Color(0xFFE53935)
val DarkSlate = Color(0xFF455A64)
val Ink = Color(0xFF1A1A2E)
val SelectedBg = Color(0xFFBBDEFB)
val HighlightBg = Color(0xFFF0F4F8)
val Green = Color(0xFF2E7D32)
val GreyBlue = Color(0xFF78909C)
val LightGrey = Color(0xFFE0E0E0)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF00226B),
    secondary = Color(0xFF55607A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF3D4A63),
    tertiary = Color(0xFF5B6472),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDFE4EC),
    onTertiaryContainer = Color(0xFF444C58),
    background = Color.White,
    onBackground = Color(0xFF1A1A2E),
    surface = Color.White,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFF0F4F8),
    onSurfaceVariant = Color(0xFF78909C),
    surfaceTint = Blue,
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFFB3C6FF),
    error = Red,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFFB0BEC5),
    outlineVariant = Color(0xFFE0E0E0),
    scrim = Color(0xFF1A1A2E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = Color(0xFFF5F7FA),
    surfaceContainerHigh = Color(0xFFF0F4F8),
    surfaceContainerHighest = Color(0xFFE8EDF2),
)

@Composable
fun SudokuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
