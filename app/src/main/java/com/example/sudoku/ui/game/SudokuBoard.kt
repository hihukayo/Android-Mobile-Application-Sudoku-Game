package com.example.sudoku.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sudoku.model.SudokuPuzzle
import com.example.sudoku.ui.Blue
import com.example.sudoku.ui.DarkSlate
import com.example.sudoku.ui.Green
import com.example.sudoku.ui.HighlightBg
import com.example.sudoku.ui.Ink
import com.example.sudoku.ui.LightGrey
import com.example.sudoku.ui.Red
import com.example.sudoku.ui.SelectedBg

@Composable
fun SudokuBoard(
    puzzle: SudokuPuzzle,
    readOnly: Boolean,
    selectedRow: Int?,
    selectedCol: Int?,
    errorCells: Set<String>,
    revision: Int,
    onCellTap: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gs = puzzle.gridSize
    val bs = puzzle.boardSize
    val textMeasurer = rememberTextMeasurer()
    val fontSize = if (gs == 9) 22.sp else 12.sp // 4x4 shuzi tiaoxiao
    val noteSize = if (gs == 9) 13.sp else 9.sp
    var boardPx by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp)),
    ) {
        key(revision) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { boardPx = it }
                    .pointerInput(gs) {
                        detectTapGestures { offset ->
                            if (boardPx.width > 0 && boardPx.height > 0) {
                                val cell = boardPx.width.toFloat() / gs
                                val r = (offset.y / cell).toInt().coerceIn(0, gs - 1)
                                val c = (offset.x / cell).toInt().coerceIn(0, gs - 1)
                                onCellTap(r, c)
                            }
                        }
                    },
            ) {
            val cell = size.width / gs
            // 背景
            for (r in 0 until gs) {
                for (c in 0 until gs) {
                    val highlighted = selectedRow != null && selectedCol != null &&
                        (r == selectedRow || c == selectedCol ||
                            (r / bs == selectedRow / bs && c / bs == selectedCol / bs))
                    val bg = when {
                        r == selectedRow && c == selectedCol -> SelectedBg
                        highlighted -> HighlightBg
                        else -> Color.White
                    }
                    drawRect(bg, topLeft = Offset(c * cell, r * cell), size = Size(cell, cell))
                }
            }
            // 数字与笔记
            for (r in 0 until gs) {
                for (c in 0 until gs) {
                    val v = puzzle.cells[r][c]
                    if (v != 0) {
                        val given = puzzle.given[r][c]
                        val isError = errorCells.contains("$r,$c")
                        val color = when {
                            given -> Ink
                            isError -> Red
                            else -> Green
                        }
                        val weight = if (given) FontWeight.Bold else FontWeight.SemiBold
                        val style = TextStyle(
                            fontSize = fontSize,
                            fontWeight = weight,
                            color = color,
                            textAlign = TextAlign.Center,
                        )
                        val layout = textMeasurer.measure(AnnotatedString(SudokuPuzzle.displayValue(v)), style = style)
                        val cx = c * cell + (cell - layout.size.width) / 2f
                        val cy = r * cell + (cell - layout.size.height) / 2f
                        drawText(layout, topLeft = Offset(cx, cy))
                    } else {
                        val note = puzzle.notes[r][c]
                        if (note.isNotEmpty()) {
                            val n = note.first()
                            val label = if (n <= 9) "$n" else ('A' + (n - 10)).toString()
                            val style = TextStyle(
                                fontSize = noteSize,
                                fontWeight = FontWeight.Medium,
                                color = Blue,
                            )
                            val layout = textMeasurer.measure(AnnotatedString(label), style = style)
                            drawText(layout, topLeft = Offset(c * cell + cell * 0.06f, r * cell + cell * 0.05f))
                        }
                    }
                }
            }
            // 网格线（仅内部线条，外圈由下方统一圆角边框承担；杀手模式内部统一浅灰细线）
            val thin = 0.5.dp.toPx()
            val thick = 2.dp.toPx()
            for (i in 1 until gs) {
                val x = i * cell
                val y = i * cell
                val isThick = !puzzle.isKiller && i % bs == 0
                val stroke = if (isThick) thick else thin
                val color = if (isThick) DarkSlate else LightGrey
                drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = stroke)
                drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke)
            }
            // 棋盘外圈边框（深色圆角，所有模式统一；错误笼子贴边时对应线段在笼子层最后标红）
            val outerW = 2.5.dp.toPx()
            drawRoundRect(
                color = DarkSlate,
                topLeft = Offset(outerW / 2, outerW / 2),
                size = Size(size.width - outerW, size.height - outerW),
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(width = outerW),
            )
            // 杀手笼子覆盖层
            if (puzzle.isKiller) {
                drawCages(puzzle, cell, textMeasurer)
            }
            }
        }
    }
}

private fun DrawScope.drawCages(puzzle: SudokuPuzzle, cell: Float, textMeasurer: androidx.compose.ui.text.TextMeasurer) {
    val gs = puzzle.gridSize
    val cages = puzzle.cages ?: return
    val invalid = puzzle.invalidCages()
    // 先画正常笼子，再画错误笼子（红色最后绘制，确保整圈边框完整变红，不被相邻笼子覆盖）
    for (isBad in listOf(false, true)) {
        for (ci in cages.indices) {
            if (invalid.contains(ci) != isBad) continue
            val color = if (isBad) Red else DarkSlate
            val stroke = 2.dp.toPx()
            val cells = cages[ci].cellIndices.toSet()
            for (idx in cells) {
                val r = idx / gs
                val c = idx % gs
                val x = c * cell
                val y = r * cell
                if (r > 0 && !cells.contains((r - 1) * gs + c)) {
                    drawLine(color, Offset(x, y), Offset(x + cell, y), stroke)
                }
                if (r < gs - 1 && !cells.contains((r + 1) * gs + c)) {
                    drawLine(color, Offset(x, y + cell), Offset(x + cell, y + cell), stroke)
                }
                if (c > 0 && !cells.contains(r * gs + (c - 1))) {
                    drawLine(color, Offset(x, y), Offset(x, y + cell), stroke)
                }
                if (c < gs - 1 && !cells.contains(r * gs + (c + 1))) {
                    drawLine(color, Offset(x + cell, y), Offset(x + cell, y + cell), stroke)
                }
                // 错误笼子贴棋盘外圈：外圈边框对应线段也标红（2.5dp 与外圈一致）
                if (isBad) {
                    val outer = 2.5.dp.toPx()
                    if (r == 0) drawLine(color, Offset(x, y), Offset(x + cell, y), outer)
                    if (r == gs - 1) drawLine(color, Offset(x, y + cell), Offset(x + cell, y + cell), outer)
                    if (c == 0) drawLine(color, Offset(x, y), Offset(x, y + cell), outer)
                    if (c == gs - 1) drawLine(color, Offset(x + cell, y), Offset(x + cell, y + cell), outer)
                }
            }
        }
    }
    for (ci in cages.indices) {
        // 笼子标签：每个笼子显示 运算符+结果（如 +10、-2、×100、÷3）
        var botR = -1
        var botC = -1
        for (idx in cages[ci].cellIndices) {
            val r = idx / gs
            val c = idx % gs
            if (r > botR || (r == botR && c > botC)) {
                botR = r
                botC = c
            }
        }
        val sumStyle = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Bold, color = DarkSlate)
        val label = cages[ci].labelText()
        val layout = textMeasurer.measure(AnnotatedString(label), style = sumStyle)
        // 所有笼子标签统一向右内缩，避免与棋盘右边框及圆角边线重叠
        val rightMargin = 5.dp.toPx()
        val sx = botC * cell + cell - rightMargin - layout.size.width
        val sy = botR * cell + cell - 12.dp.toPx()
        drawText(layout, topLeft = Offset(sx, sy))
    }
}
