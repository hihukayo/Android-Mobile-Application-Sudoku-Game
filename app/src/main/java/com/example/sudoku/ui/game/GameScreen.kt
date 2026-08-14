package com.example.sudoku.ui.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.sudoku.data.Session
import com.example.sudoku.ui.AppIcons
import com.example.sudoku.ui.Blue
import com.example.sudoku.ui.LocalSudokuColors
import com.example.sudoku.ui.Red
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameScreen(controller: GameController) {
    val sc = LocalSudokuColors.current
    val scope = rememberCoroutineScope()
    var showModeMenu by remember { mutableStateOf(false) }
    var pendingResume by remember { mutableStateOf<JSONObject?>(null) }
    var pendingLoad by remember { mutableStateOf<JSONObject?>(null) }
    var focusTick by remember { mutableStateOf(0) }
    val keyboard = LocalSoftwareKeyboardController.current

    // 进入游戏页时检查存档，提示续玩
    LaunchedEffect(Unit) {
        if (Session.autoResumeChecked) {
            // 控制器若被重建（如进设置返回），补开一局避免空棋盘
            controller.ensureStarted()
            return@LaunchedEffect
        }
        Session.autoResumeChecked = true
        if (controller.resumeChecked) return@LaunchedEffect
        controller.resumeChecked = true
        controller.newGame()
        controller.awaitPendingSave()
        val res = controller.fetchSave()
        if (res != null && res.optBoolean("success")) {
            pendingResume = res
        }
    }

    LaunchedEffect(controller.isSolved, controller.gameOver, controller.paused) {
        if (controller.isSolved || controller.gameOver || controller.paused) {
            keyboard?.hide()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(sc.background)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.Backspace, Key.Delete -> {
                            controller.clearSelected()
                            true
                        }
                        else -> {
                            val n = digitOf(event.key) ?: letterOf(event.key, controller.boardSize)
                            if (n != null) {
                                controller.fillNumber(n)
                                true
                            } else {
                                false
                            }
                        }
                    }
                } else {
                    false
                }
            },
    ) {
        HiddenNumberInput(
            gs = controller.puzzle.gridSize,
            enabled = !controller.paused && !controller.gameOver,
            focusTick = focusTick,
            onInput = { n ->
                controller.fillNumber(n)
                if (controller.cluesRemaining() == 0) keyboard?.hide()
            },
            onDone = { keyboard?.hide() },
        )
        Column(Modifier.fillMaxSize()) {
        // ---- 顶栏：模式菜单 / 标题 / 笔记开关 ----
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                AppIcons.MoreHoriz,
                contentDescription = "模式",
                tint = sc.textFaint,
                modifier = Modifier
                    .padding(6.dp)
                    .size(24.dp)
                    .clickable {
                        showModeMenu = true
                        keyboard?.hide()
                    },
            )
            Spacer(Modifier.weight(1f))
            Text("数独", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            Icon(
                AppIcons.EditNote,
                contentDescription = "笔记模式",
                tint = if (controller.noteMode) sc.noteText else sc.textFaint,
                modifier = Modifier
                    .padding(6.dp)
                    .size(24.dp)
                    .clickable {
                        if (!controller.paused && !controller.gameOver) {
                            controller.toggleNoteMode()
                        }
                    },
            )
        }

        HorizontalDivider(thickness = 0.5.dp)

        // ---- 计数栏 ----
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .offset(x = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                AppIcons.ErrorOutline,
                contentDescription = null,
                tint = if (controller.errors >= controller.maxErrors) Red else sc.textFaint,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "${controller.errors}/${controller.maxErrors}",
                fontSize = 12.sp,
                color = if (controller.errors >= controller.maxErrors) Red else sc.textSecondary,
            )
            Spacer(Modifier.width(24.dp))
            Row(
                Modifier.clickable(enabled = !controller.gameOver && !controller.hasGivenUp) {
                    controller.togglePause()
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (controller.paused) AppIcons.PlayArrow else AppIcons.Pause,
                    contentDescription = null,
                    tint = if (controller.gameOver || controller.hasGivenUp) sc.disabledText else sc.noteText,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    controller.formatTime(controller.seconds),
                    fontSize = 12.sp,
                    color = if (controller.gameOver || controller.hasGivenUp) sc.disabledText else sc.textSecondary,
                )
            }
            Spacer(Modifier.width(24.dp))
            Text(
                if (controller.isKiller) controller.killerDifficulty else controller.difficulty,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = diffColor(controller),
            )
            Spacer(Modifier.width(4.dp))
            Text("${controller.cluesRemaining()}空", fontSize = 12.sp, color = sc.textSecondary)
        }

        // ---- 棋盘 + 提示（棋盘贴顶，提示紧贴棋盘下边框）----
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            val side = minOf(maxWidth, (maxHeight - 26.dp).coerceAtLeast(1.dp))
            Column(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = controller.boardSize to controller.isKiller,
                    transitionSpec = {
                        // 模式切换：旧棋盘淡出，新棋盘淡入并轻微放大，过渡更丝滑
                        (fadeIn(tween(220)) + scaleIn(initialScale = 0.94f, animationSpec = tween(220)))
                            .togetherWith(fadeOut(tween(120)))
                    },
                    label = "boardMode",
                ) { mode ->
                    val (boardSizeKey, killerKey) = mode
                    // 快照当前棋盘，动画期间旧棋盘保持旧内容，避免两边同时显示新棋盘
                    var snapshotPuzzle by remember(boardSizeKey, killerKey) { mutableStateOf(controller.puzzle) }
                    LaunchedEffect(controller.revision) {
                        snapshotPuzzle = controller.puzzle
                    }
                    SudokuBoard(
                        puzzle = snapshotPuzzle,
                        readOnly = controller.paused || controller.gameOver,
                        selectedRow = controller.selectedRow,
                        selectedCol = controller.selectedCol,
                        errorCells = controller.errorCells,
                        revision = controller.revision,
                        onCellTap = { r, c ->
                            controller.selectCell(r, c)
                            if (!controller.paused && !controller.gameOver) {
                                focusTick++
                                keyboard?.show()
                            }
                        },
                        modifier = Modifier
                            .size(side)
                            .align(Alignment.CenterHorizontally),
                    )
                }
                Spacer(Modifier.height(4.dp))
                StatusText(controller)
            }
        }

        HorizontalDivider(thickness = 0.5.dp)
        Spacer(Modifier.height(12.dp))

        // ---- 底部按钮 ----
        BottomBar(
            controller = controller,
            onLoad = {
                if (!controller.loadingSave) {
                    controller.loadingSave = true
                    controller.showStatus("正在加载...")
                    scope.launch {
                        controller.awaitPendingSave()
                        val res = controller.fetchSave()
                        controller.loadingSave = false
                        if (res != null && res.optBoolean("success")) {
                            pendingLoad = res
                        } else {
                            controller.showStatus("加载失败，请检查网络连接后重试")
                        }
                    }
                }
            },
        )
        }

        // ---- 新局生成中显示加载遮罩，避免 4×4（16×16）生成时看起来像卡死 ----
        if (controller.generating) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(sc.surface.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {},
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = sc.primary)
            }
        }

        // ---- 模式下拉菜单（紧贴顶部分割线下方）----
        if (showModeMenu) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { showModeMenu = false },
            )
            ModeMenuPopup(
                current = if (controller.isKiller) "3×3-killer" else if (controller.boardSize == 4) "4×4" else "3×3",
                onSelect = { mode ->
                    showModeMenu = false
                    controller.switchMode(mode)
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 52.dp)
                    .padding(start = 6.dp),
            )
        }
    }


    // 续玩提示
    pendingResume?.let { res ->
        ResumeDialog(
            title = "发现存档",
            message = "您有一个存档\n(${res.optString("savedAt")})\n是否继续上次的游戏？",
            confirmText = "继续",
            cancelText = "新游戏",
            onConfirm = {
                controller.restoreFromData(res)
                pendingResume = null
            },
            onCancel = { pendingResume = null },
        )
    }

    // 读档确认
    pendingLoad?.let { res ->
        ResumeDialog(
            title = "加载存档",
            message = "存档时间\n${res.optString("savedAt")}\n当前未保存的进度将丢失。",
            confirmText = "加载",
            cancelText = "取消",
            onConfirm = {
                controller.restoreFromData(res)
                pendingLoad = null
            },
            onCancel = { pendingLoad = null },
        )
    }

}

private fun digitOf(key: Key): Int? = when (key) {
    Key.One -> 1
    Key.NumPad1 -> 1
    Key.Two -> 2
    Key.NumPad2 -> 2
    Key.Three -> 3
    Key.NumPad3 -> 3
    Key.Four -> 4
    Key.NumPad4 -> 4
    Key.Five -> 5
    Key.NumPad5 -> 5
    Key.Six -> 6
    Key.NumPad6 -> 6
    Key.Seven -> 7
    Key.NumPad7 -> 7
    Key.Eight -> 8
    Key.NumPad8 -> 8
    Key.Nine -> 9
    Key.NumPad9 -> 9
    else -> null
}

private fun letterOf(key: Key, boardSize: Int): Int? {
    if (boardSize != 4) return null
    return when (key) {
        Key.A -> 10
        Key.B -> 11
        Key.C -> 12
        Key.D -> 13
        Key.E -> 14
        Key.F -> 15
        Key.G -> 16
        else -> null
    }
}

@Composable
private fun diffColor(controller: GameController): Color {
    val sc = LocalSudokuColors.current
    val diff = if (controller.isKiller) controller.killerDifficulty else controller.difficulty
    return when (diff) {
        "极简" -> Color(0xFFEF5350)
        "困难" -> Color(0xFFFFA726)
        "入门" -> sc.userInput
        "中等" -> Blue
        "简单" -> sc.userInput
        else -> sc.textSecondary
    }
}

@Composable
private fun StatusText(controller: GameController) {
    val sc = LocalSudokuColors.current
    val style = MaterialTheme.typography.bodySmall
    val (text, color) = when {
        controller.isSolved -> "解答正确！用时 ${controller.formatTime(controller.seconds)}，获得 ${controller.lastScore} 积分" to sc.userInput
        controller.hasGivenUp -> "已查看答案" to Color(0xFFFF9800)
        controller.gameOver -> "游戏结束，用时 ${controller.formatTime(controller.seconds)}，获得 ${controller.lastScore} 积分" to Red
        controller.statusMsg.isNotEmpty() -> controller.statusMsg to sc.textSecondary
        controller.paused -> "已暂停" to sc.textSecondary
        else -> "" to sc.textSecondary
    }
    if (text.isNotEmpty()) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth(),
            style = style,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BottomBar(controller: GameController, onLoad: () -> Unit) {
    val sc = LocalSudokuColors.current
    val disabled = controller.paused || controller.gameOver
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            GameTextBtn("新局", enabled = !controller.generating, onClick = { controller.newGame() }, icon = AppIcons.Refresh)
            GameTextBtn(
                "完成",
                fill = true,
                enabled = !disabled && !controller.isSolved && !controller.hasGivenUp,
                onClick = { controller.checkCompletion() },
                icon = AppIcons.StarOutline,
                overlayIcon = AppIcons.Check,
            )
            GameTextBtn(
                "求解",
                enabled = !disabled && !controller.isSolved && !controller.hasGivenUp,
                onClick = { controller.autoSolve() },
                icon = AppIcons.Lightbulb,
                iconAtEnd = true,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            GameIconTextBtn(AppIcons.Undo, "撤销", enabled = !disabled && controller.canUndo(), onClick = { controller.undo() })
            GameIconTextBtn(AppIcons.Replay, "重置", enabled = true, onClick = { controller.restart() })
            GameIconTextBtn(AppIcons.Redo, "重做", enabled = !disabled && controller.canRedo(), onClick = { controller.redo() }, iconAtEnd = true)
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 40.dp))
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameIconTextBtn(AppIcons.CloudUpload, "存档", onClick = { controller.saveGame() })
            Spacer(Modifier.width(24.dp))
            Box(Modifier.width(1.dp).height(24.dp).background(sc.divider))
            Spacer(Modifier.width(24.dp))
            GameIconTextBtn(AppIcons.CloudDownload, "读档", enabled = !controller.loadingSave, onClick = onLoad)
        }
    }
}

@Composable
private fun GameTextBtn(
    label: String,
    onClick: () -> Unit,
    fill: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    overlayIcon: ImageVector? = null,
    iconAtEnd: Boolean = false,
) {
    val sc = LocalSudokuColors.current
    val bg = when {
        fill && !enabled -> sc.primary.copy(alpha = 0.38f)
        fill -> sc.primary
        else -> Color.Transparent
    }
    val fg = when {
        fill && !enabled -> sc.onPrimary.copy(alpha = 0.72f)
        !enabled -> sc.disabledText
        fill -> sc.onPrimary
        else -> sc.textSecondary
    }
    Box(
        Modifier
            .width(88.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon == null) {
            Text(label, fontSize = 15.sp, fontWeight = if (fill) FontWeight.SemiBold else FontWeight.Medium, color = fg)
        } else {
            // 图标+文字整体居中，间距 4（与下排撤销/重置/重做一致）
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (iconAtEnd) {
                    Text(label, fontSize = 15.sp, fontWeight = if (fill) FontWeight.SemiBold else FontWeight.Medium, color = fg)
                    Spacer(Modifier.width(4.dp))
                    Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp)) {
                        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
                        if (overlayIcon != null) {
                            Icon(overlayIcon, contentDescription = null, tint = fg, modifier = Modifier.size(7.dp))
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(label, fontSize = 15.sp, fontWeight = if (fill) FontWeight.SemiBold else FontWeight.Medium, color = fg)
                }
            }
        }
    }
}

@Composable
private fun GameIconTextBtn(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconAtEnd: Boolean = false,
) {
    val sc = LocalSudokuColors.current
    val color = if (enabled) sc.textSecondary else sc.disabledText
    Box(
        Modifier
            .width(88.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // 图标+文字整体居中，间距 4（撤销/重置/重做/存档/读档统一）
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconAtEnd) {
                Text(label, fontSize = 15.sp, color = color) // 与上排按钮文字大小一致
                Spacer(Modifier.width(4.dp))
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            } else {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 15.sp, color = color) // 与上排按钮文字大小一致
            }
        }
    }
}
@Composable
private fun HiddenNumberInput(
    gs: Int,
    enabled: Boolean,
    focusTick: Int,
    onInput: (Int) -> Unit,
    onDone: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    // 单输入框方案：焦点与输入法会话全程不切换、不重建，输入即时响应。
    // 每个数字输入后下一帧清空缓冲区，保证同一数字可反复输入。
    val focus = remember { FocusRequester() }
    var buf by remember { mutableStateOf("") }
    var needClear by remember { mutableStateOf(false) }

    LaunchedEffect(focusTick) {
        if (focusTick > 0) {
            buf = ""
            needClear = false
            focus.requestFocus()
            delay(30)
            keyboard?.show()
        }
    }

    LaunchedEffect(needClear) {
        if (needClear) {
            needClear = false
            buf = ""
        }
    }

    fun accept(raw: String) {
        if (raw.length < buf.length) { // 退格/清空：只更新缓冲区，不产生输入
            buf = raw
            return
        }
        val clean = if (gs == 9) {
            raw.filter { it in '1'..'9' }
        } else {
            raw.uppercase().filter { it in '1'..'9' || it in 'A'..'G' }
        }
        if (clean.isNotEmpty()) {
            val ch = clean.last()
            val n = if (ch in 'A'..'G') ch - 'A' + 10 else ch - '0'
            onInput(n)
            buf = clean
            needClear = true
        } else {
            buf = ""
        }
    }

    val options = KeyboardOptions(
        keyboardType = if (gs == 9) KeyboardType.NumberPassword else KeyboardType.Password,
        capitalization = KeyboardCapitalization.None,
        imeAction = ImeAction.Done,
    )
    val actions = KeyboardActions(onDone = { keyboard?.hide(); onDone() })

    BasicTextField(
        value = buf,
        onValueChange = { accept(it) },
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .focusRequester(focus),
        textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = options,
        keyboardActions = actions,
        singleLine = true,
        enabled = enabled,
        decorationBox = { inner -> Box(Modifier.fillMaxWidth().height(1.dp)) { inner() } },
    )
}
@Composable
private fun ModeMenuPopup(current: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(
        "3×3-killer" to "3×3 算数",
        "3×3" to "3×3 常规",
        "4×4" to "4×4 常规",
    )
    val sc = LocalSudokuColors.current
    Column(
        modifier
            .width(104.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(sc.surface)
            .padding(vertical = 4.dp),
    ) {
        options.forEachIndexed { i, (value, label) ->
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = sc.divider)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(value) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, fontSize = 12.sp, color = sc.textSecondary, modifier = Modifier.weight(1f))
                if (current == value) {
                    Icon(AppIcons.Check, contentDescription = null, tint = sc.noteText, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}

@Composable
private fun ResumeDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    // 与 Flutter 原版一致的存档弹窗：图标 + 标题 + 说明 + 双按钮
    val sc = LocalSudokuColors.current
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = sc.surface,
            shadowElevation = 8.dp,
            modifier = Modifier.width(300.dp),
        ) {
            Column(
                Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    AppIcons.CloudDownload,
                    contentDescription = null,
                    tint = sc.noteText,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = sc.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    fontSize = 14.sp,
                    color = sc.textFaint,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, sc.divider),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        Text(cancelText, fontSize = 15.sp, color = sc.textSecondary)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = sc.primary,
                            contentColor = sc.onPrimary,
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        Text(confirmText, fontSize = 15.sp)
                    }
                }
            }
    }
}
}
