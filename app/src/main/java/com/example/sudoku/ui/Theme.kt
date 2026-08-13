package com.example.sudoku.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.sudoku.data.Session

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

/** 语义色：界面统一从这里取色，随深色模式自动切换 */
data class SudokuColors(
    val background: Color,      // 页面背景
    val surface: Color,         // 卡片/面板背景
    val surfaceAlt: Color,      // 次级面板背景
    val textPrimary: Color,     // 主文字
    val textSecondary: Color,   // 次要文字
    val textFaint: Color,       // 更淡文字
    val divider: Color,         // 分割线
    val inputBg: Color,         // 输入框/浅底
    val selectedBg: Color,      // 选中高亮（浅蓝）
    val highlightBg: Color,     // 棋盘高亮格
    val boardBg: Color,         // 棋盘背景
    val boardLine: Color,       // 棋盘细线
    val chipBg: Color,          // 禁用/浅灰底
    val disabledText: Color,    // 禁用文字
    val userInput: Color,       // 玩家填写的数字
    val noteText: Color,        // 笔记/浅蓝强调（暗色下用亮蓝）
    val primary: Color,         // 主按钮底色（暗色下柔和蓝）
    val onPrimary: Color,       // 主按钮文字（暗色下柔和近白）
)

private val LightSudokuColors = SudokuColors(
    background = Color.White,
    surface = Color.White,
    surfaceAlt = Color(0xFFF5F7FA),
    textPrimary = Ink,
    textSecondary = DarkSlate,
    textFaint = GreyBlue,
    divider = Color(0xFFE0E0E0),
    inputBg = Color(0xFFF5F7FA),
    selectedBg = SelectedBg,
    highlightBg = HighlightBg,
    boardBg = Color.White,
    boardLine = LightGrey,
    chipBg = Color(0xFFF1F1F1),
    disabledText = Color(0xFFC0C0C0),
    userInput = Green,
    noteText = Blue,
    primary = Blue,
    onPrimary = Color.White,
)

private val DarkSudokuColors = SudokuColors(
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceAlt = Color(0xFF262626),
    textPrimary = Color(0xFFE8EAF0),
    textSecondary = Color(0xFFB0BEC5),
    textFaint = Color(0xFF90A4AE),
    divider = Color(0xFF2E2E2E),
    inputBg = Color(0xFF252525),
    selectedBg = Color(0xFF1E3A5F),
    highlightBg = Color(0xFF232A3A),
    boardBg = Color(0xFF1E1E1E),
    boardLine = Color(0xFF4A4A4A),
    chipBg = Color(0xFF2A2A2A),
    disabledText = Color(0xFF6E6E6E),
    userInput = Color(0xFF81C784),
    noteText = Color(0xFF9CBDF0),
    primary = Color(0xFF2B46B8),
    onPrimary = Color(0xFFE8EAF0),
)

val LocalSudokuColors = staticCompositionLocalOf { LightSudokuColors }

/** 深色模式偏好：light / dark / system，设置页可切换 */
val AppThemeMode = mutableStateOf(Session.getThemeMode())

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

private val DarkColors = darkColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF27418C),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFB9C3DE),
    onSecondary = Color(0xFF232D45),
    secondaryContainer = Color(0xFF3A455C),
    onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFC0C6D3),
    onTertiary = Color(0xFF2A303B),
    tertiaryContainer = Color(0xFF404752),
    onTertiaryContainer = Color(0xFFDFE4EC),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE8EAF0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE8EAF0),
    surfaceVariant = Color(0xFF252525),
    onSurfaceVariant = Color(0xFFB0BEC5),
    surfaceTint = Blue,
    inverseSurface = Color(0xFFE8EAF0),
    inverseOnSurface = Color(0xFF1A1A2E),
    inversePrimary = Color(0xFFB3C6FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8A9199),
    outlineVariant = Color(0xFF2E2E2E),
    scrim = Color.Black,
    surfaceContainerLowest = Color(0xFF0F0F0F),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF232323),
    surfaceContainerHighest = Color(0xFF2A2A2A),
)

@Composable
fun SudokuTheme(content: @Composable () -> Unit) {
    val dark = when (AppThemeMode.value) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkSudokuColors else LightSudokuColors
    val scheme = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalSudokuColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
