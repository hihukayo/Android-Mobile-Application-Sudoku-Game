package com.example.sudoku.ui.game

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
import com.example.sudoku.ui.AppIcons
import com.example.sudoku.ui.Blue
import com.example.sudoku.ui.DarkSlate
import com.example.sudoku.ui.GreyBlue
import com.example.sudoku.ui.Ink
import com.example.sudoku.ui.Red
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameScreen(controller: GameController) {
    val scope = rememberCoroutineScope()
    var showModeMenu by remember { mutableStateOf(false) }
    var pendingResume by remember { mutableStateOf<JSONObject?>(null) }
    var pendingLoad by remember { mutableStateOf<JSONObject?>(null) }
    var focusTick by remember { mutableStateOf(0) }
    val keyboard = LocalSoftwareKeyboardController.current

    // 进入游戏页时检查存档，提示续玩
    LaunchedEffect(Unit) {
        if (controller.resumeChecked) return@LaunchedEffect
        controller.resumeChecked = true
        controller.newGame()
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
            .background(Color.White)
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
                tint = GreyBlue,
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
                tint = if (controller.noteMode) Blue else GreyBlue,
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
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                AppIcons.ErrorOutline,
                contentDescription = null,
                tint = if (controller.errors >= controller.maxErrors) Red else GreyBlue,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "${controller.errors}/${controller.maxErrors}",
                fontSize = 12.sp,
                color = if (controller.errors >= controller.maxErrors) Red else DarkSlate,
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
                    tint = if (controller.gameOver || controller.hasGivenUp) Color(0xFFD0D0D0) else Blue,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    controller.formatTime(controller.seconds),
                    fontSize = 12.sp,
                    color = if (controller.gameOver || controller.hasGivenUp) Color(0xFFD0D0D0) else DarkSlate,
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
            Text("${controller.cluesRemaining()}空", fontSize = 12.sp, color = DarkSlate)
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
                SudokuBoard(
                    puzzle = controller.puzzle,
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
                scope.launch {
                    val res = controller.fetchSave()
                    if (res != null && res.optBoolean("success")) {
                        pendingLoad = res
                    }
                }
            },
        )
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
    val diff = if (controller.isKiller) controller.killerDifficulty else controller.difficulty
    return when (diff) {
        "极简" -> Color(0xFFC62828)
        "困难" -> Color(0xFFE65100)
        "入门" -> Color(0xFF2E7D32)
        "中等" -> Blue
        "简单" -> Color(0xFF2E7D32)
        else -> DarkSlate
    }
}

private val GreenText = Color(0xFF2E7D32)

@Composable
private fun StatusText(controller: GameController) {
    val style = MaterialTheme.typography.bodySmall
    val (text, color) = when {
        controller.isSolved -> "解答正确！用时 ${controller.formatTime(controller.seconds)}，获得 ${controller.lastScore} 积分" to GreenText
        controller.hasGivenUp -> "已查看答案" to Color(0xFFFF9800)
        controller.gameOver -> "错误 ${controller.errors} 次，游戏结束，用时 ${controller.formatTime(controller.seconds)}，获得 ${controller.lastScore} 积分" to Red
        controller.paused -> "已暂停" to DarkSlate
        controller.statusMsg.isNotEmpty() -> controller.statusMsg to DarkSlate
        else -> "" to DarkSlate
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
    val disabled = controller.paused || controller.gameOver
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            GameTextBtn("新局", onClick = { controller.newGame() })
            GameTextBtn(
                "完成",
                fill = true,
                enabled = !disabled && !controller.isSolved && !controller.hasGivenUp,
                onClick = { controller.checkCompletion() },
            )
            GameTextBtn(
                "求解",
                enabled = !disabled && !controller.isSolved && !controller.hasGivenUp,
                onClick = { controller.autoSolve() },
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            GameIconTextBtn(AppIcons.Undo, "撤销", enabled = !disabled && controller.canUndo(), onClick = { controller.undo() })
            GameIconTextBtn(AppIcons.Replay, "重置", enabled = true, onClick = { controller.restart() })
            GameIconTextBtn(AppIcons.Redo, "重做", enabled = !disabled && controller.canRedo(), onClick = { controller.redo() })
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 40.dp))
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameIconTextBtn(AppIcons.CloudUpload, "存档", enabled = true, onClick = { controller.saveGame() })
            Spacer(Modifier.width(24.dp))
            Box(Modifier.width(1.dp).height(24.dp).background(Color(0xFFE0E0E0)))
            Spacer(Modifier.width(24.dp))
            GameIconTextBtn(AppIcons.CloudDownload, "读档", enabled = true, onClick = onLoad)
        }
    }
}

@Composable
private fun GameTextBtn(label: String, onClick: () -> Unit, fill: Boolean = false, enabled: Boolean = true) {
    val bg = when {
        !enabled -> Color(0xFFF1F1F1)
        fill -> Blue
        else -> Color.Transparent
    }
    val fg = when {
        !enabled -> Color(0xFFC0C0C0)
        fill -> Color.White
        else -> DarkSlate
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
        Text(label, fontSize = 15.sp, fontWeight = if (fill) FontWeight.SemiBold else FontWeight.Medium, color = fg)
    }
}

@Composable
private fun GameIconTextBtn(icon: ImageVector, label: String, onClick: () -> Unit, enabled: Boolean = true) {
    val color = if (enabled) DarkSlate else Color(0xFFC0C0C0)
    Box(
        Modifier
            .width(88.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 13.sp, color = color)
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
        "3×3-killer" to "3×3 杀手",
        "3×3" to "3×3 常规",
        "4×4" to "4×4 常规",
    )
    Column(
        modifier
            .width(104.dp)
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(vertical = 4.dp),
    ) {
        options.forEachIndexed { i, (value, label) ->
            if (i > 0) HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(value) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, fontSize = 12.sp, color = DarkSlate, modifier = Modifier.weight(1f))
                if (current == value) {
                    Icon(AppIcons.Check, contentDescription = null, tint = Blue, modifier = Modifier.size(13.dp))
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
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
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
                    tint = Blue,
                    modifier = Modifier.size(44.dp),
                )
                Spacer(Modifier.height(14.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    fontSize = 14.sp,
                    color = GreyBlue,
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
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        Text(cancelText, fontSize = 15.sp, color = DarkSlate)
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Blue,
                            contentColor = Color.White,
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
