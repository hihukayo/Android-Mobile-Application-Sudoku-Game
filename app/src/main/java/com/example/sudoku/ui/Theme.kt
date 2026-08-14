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
val HighlightBg = Color(0xFFE7EFFA) // 浅色模式行列/宫高亮：柔和淡蓝底，与细格线和谐
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
    val danger: Color,          // 危险操作按钮（暗色下柔和红）
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
    danger = Color(0xFFEF5350),
)

private val DarkSudokuColors = SudokuColors(
    background = Color(0xFF0E1526),
    surface = Color(0xFF151E33),
    surfaceAlt = Color(0xFF1B2540),
    textPrimary = Color(0xFFE8EDF9),
    textSecondary = Color(0xFFA9B7D6),
    textFaint = Color(0xFF7184A8),
    divider = Color(0xFF25314E),
    inputBg = Color(0xFF131B2E),
    selectedBg = Color(0xFF21406E),
    highlightBg = Color(0xFF1E2B47),
    boardBg = Color(0xFF111A2C),
    boardLine = Color(0xFF35456B),
    chipBg = Color(0xFF1C2640),
    disabledText = Color(0xFF5A6B8E),
    userInput = Color(0xFF7FD6A8),
    noteText = Color(0xFF9CC6FF),
    primary = Color(0xFF4E7BFF),
    onPrimary = Color(0xFFEEF2FF),
    danger = Color(0xFFE07A7A),
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
    primary = Color(0xFF4E7BFF),
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
    background = Color(0xFF0E1526),
    onBackground = Color(0xFFE8EDF9),
    surface = Color(0xFF151E33),
    onSurface = Color(0xFFE8EDF9),
    surfaceVariant = Color(0xFF1B2540),
    onSurfaceVariant = Color(0xFFA9B7D6),
    surfaceTint = Color(0xFF4E7BFF),
    inverseSurface = Color(0xFFE8EDF9),
    inverseOnSurface = Color(0xFF1A1A2E),
    inversePrimary = Color(0xFFB3C6FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8A97B3),
    outlineVariant = Color(0xFF25314E),
    scrim = Color(0xFF0A101E),
    surfaceContainerLowest = Color(0xFF0A101E),
    surfaceContainerLow = Color(0xFF121A2C),
    surfaceContainer = Color(0xFF151E33),
    surfaceContainerHigh = Color(0xFF1A243A),
    surfaceContainerHighest = Color(0xFF202B45),
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
