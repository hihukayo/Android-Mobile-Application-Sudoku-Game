package com.example.sudoku.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.sudoku.data.ApiClient
import com.example.sudoku.model.Cage
import com.example.sudoku.model.SudokuGenerator
import com.example.sudoku.model.SudokuPuzzle
import com.example.sudoku.sound.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.random.Random

private class UndoEntry(
    val r: Int,
    val c: Int,
    val oldVal: Int,
    val oldNotes: Set<Int>,
    val newVal: Int,
    val newNotes: Set<Int>,
)

class GameController(val username: String) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val rng = Random

    /** 会话内只提示一次续玩 */
    var resumeChecked = false

    /** 控制器是否已自动开过一局（重建后补一局，避免空棋盘） */
    private var autoStarted = false

    /** 存档/读档进行中，防止重复请求 */
    var saving by mutableStateOf(false)
    var loadingSave by mutableStateOf(false)

    /** 存档在途时又点了存档：记录参数，完成后自动补存一次，避免按键无反应 */
    private var saveAgain = false
    private var saveAgainSilent = false
    private var saveAgainSuccess = "存档成功"
    private var saveAgainFail = "存档失败，请检查网络连接后重试"

    /** 棋盘是否被玩家动过（防止新盘无意覆盖旧存档） */
    var dirty by mutableStateOf(false)
        private set

    var puzzle by mutableStateOf(SudokuPuzzle(3))
        private set
    var boardSize by mutableStateOf(3)
        private set
    var isKiller by mutableStateOf(false)
        private set
    var killerDifficulty by mutableStateOf("中等")
        private set
    var difficulty by mutableStateOf("中等")
        private set
    var clueCount by mutableStateOf(30)
        private set
    var seconds by mutableStateOf(0)
        private set
    var paused by mutableStateOf(false)
        private set
    var isSolved by mutableStateOf(false)
        private set
    var hasGivenUp by mutableStateOf(false)
        private set
    var noteMode by mutableStateOf(false)
        private set
    var gameOver by mutableStateOf(false)
        private set
    var errors by mutableStateOf(0)
        private set
    var statusMsg by mutableStateOf("")
        private set
    var lastScore by mutableStateOf(0)
        private set
    var selectedRow by mutableStateOf<Int?>(null)
        private set
    var selectedCol by mutableStateOf<Int?>(null)
        private set
    var errorCells by mutableStateOf(setOf<String>())
        private set
    /** 修订号：棋盘数据就地修改后递增，驱动 Compose 重绘 */
    var revision by mutableStateOf(0)
        private set

    private val lastClueCounts = mutableListOf<Int>()
    private val undoStack = ArrayDeque<UndoEntry>()
    private val redoStack = ArrayDeque<UndoEntry>()
    var undoDepth by mutableStateOf(0)
        private set
    var redoDepth by mutableStateOf(0)
        private set
    var generating by mutableStateOf(false)
        private set
    private var timerJob: Job? = null
    private var statusJob: Job? = null
    /** 新局生成代次：恢复存档时递增，使进行中的生成失效，避免覆盖恢复的棋盘与计时 */
    private var gameGen = 0

    val maxErrors: Int get() = if (boardSize == 3) 3 else 6

    // ---- 计时 ----
    private fun startTimer() {
        timerJob?.cancel()
        paused = false
        timerJob = scope.launch {
            while (true) {
                delay(1000)
                if (!paused && !gameOver && !isSolved) seconds++
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun dispose() {
        stopTimer()
        statusJob?.cancel()
        if (!gameOver && !isSolved && seconds > 3) autoSave()
        scope.coroutineContext[Job]?.cancel()
    }

    // ---- 难度选择 ----
    private fun pickClueCount() {
        val diffs: List<String>
        val weights: List<Int>
        val ranges: Map<String, List<Int>>
        if (boardSize == 4) {
            diffs = listOf("困难", "中等", "简单")
            weights = listOf(25, 50, 25)
            ranges = mapOf("困难" to listOf(70, 80), "中等" to listOf(92, 105), "简单" to listOf(110, 130))
        } else {
            diffs = listOf("极简", "困难", "中等", "简单")
            weights = listOf(10, 25, 40, 25)
            ranges = mapOf(
                "极简" to listOf(17, 22),
                "困难" to listOf(23, 28),
                "中等" to listOf(29, 32),
                "简单" to listOf(33, 36),
            )
        }
        var roll = rng.nextInt(100)
        var diff = diffs[0]
        for (i in diffs.indices) {
            roll -= weights[i]
            if (roll < 0) {
                diff = diffs[i]
                break
            }
        }
        val range = ranges[diff]!!
        var clues = range[0] + rng.nextInt(range[1] - range[0] + 1)
        var tries = 0
        while (lastClueCounts.contains(clues) && tries < 30) {
            clues = range[0] + rng.nextInt(range[1] - range[0] + 1)
            tries++
        }
        lastClueCounts.add(clues)
        if (lastClueCounts.size > 3) lastClueCounts.removeAt(0)
        difficulty = diff
        clueCount = clues
    }

    /** 首次检查存档后若控制器被重建，自动补开一局，避免显示空棋盘 */
    fun ensureStarted() {
        if (autoStarted || generating) return
        autoStarted = true
        newGame(silent = true, feedback = false)
    }

    fun newGame(silent: Boolean = false, feedback: Boolean = true) {
        if (generating) return
        if (feedback) {
            if (silent) {
                SoundManager.tap()
                SoundManager.vibrate()
            } else {
                if (!SoundManager.debounce()) return
                SoundManager.click()
            }
        }
        // 有生成在途时也重新开始：废弃在途生成，保证模式/难度切换后棋盘一定按新模式刷新
        gameGen++
        generating = true
        val gen = ++gameGen
        scope.launch {
            // 主线程先决定难度，再切后台生成谜题，避免卡 UI 与连点堆积
            val newDiff: String
            val genBoardSize: Int
            if (isKiller) {
                val diffRoll = rng.nextInt(100)
                newDiff = if (diffRoll < 25) "入门" else if (diffRoll < 75) "中等" else "困难"
                killerDifficulty = newDiff
                genBoardSize = 3
            } else {
                pickClueCount()
                newDiff = difficulty
                genBoardSize = boardSize
            }
            val newPuzzle = withContext(Dispatchers.Default) {
                if (isKiller) SudokuGenerator(3).generateKiller(newDiff)
                else SudokuGenerator(genBoardSize).generate(clueCount)
            }
            // 生成期间若已恢复存档，放弃本次新局，避免覆盖恢复的棋盘与计时
            if (gen != gameGen) return@launch
            puzzle = newPuzzle
            generating = false
            isSolved = false
            hasGivenUp = false
            noteMode = false
            gameOver = false
            errors = 0
            dirty = false
            paused = false
            seconds = 0
            statusMsg = ""
            lastScore = 0
            undoStack.clear()
            redoStack.clear()
            undoDepth = 0
            redoDepth = 0
            errorCells = emptySet()
            selectedRow = null
            selectedCol = null
            revision++
            startTimer()
        }
    }

    fun togglePause() {
        SoundManager.click()
        val becoming = !paused
        paused = becoming
        // 暂停时仅当玩过（动过棋盘）才静默自动存档，未玩过不覆盖旧存档；内容与手动存档一致（含计时）
        if (becoming && !gameOver && !isSolved && dirty) saveGame(silent = true)
    }

    fun selectCell(r: Int, c: Int) {
        selectedRow = r
        selectedCol = c
    }

    fun toggleNoteMode() {
        noteMode = !noteMode
    }

    fun canUndo(): Boolean = undoDepth > 0

    fun canRedo(): Boolean = redoDepth > 0

    /** 填入/切换笔记（对应 Flutter 版棋盘 fillNumber + onCellChanged） */
    fun fillNumber(n: Int) {
        val r = selectedRow ?: return
        val c = selectedCol ?: return
        if (paused || gameOver) return
        if (puzzle.given[r][c]) return
        dirty = true

        if (noteMode) {
            val oldNotes = puzzle.notes[r][c].toSet()
            if (puzzle.notes[r][c].contains(n)) {
                puzzle.notes[r][c].remove(n)
            } else {
                puzzle.setNote(r, c, n)
            }
            val newNotes = puzzle.notes[r][c].toSet()
            pushUndo(UndoEntry(r, c, 0, oldNotes, 0, newNotes))
            revision++
            return
        }

        val old = puzzle.cells[r][c]
        val oldNotes = puzzle.notes[r][c].toSet()
        puzzle.cells[r][c] = n
        puzzle.notes[r][c].clear()
        val isError = if (isKiller) puzzle.isConflictAt(r, c, n) else n != puzzle.solution[r][c]
        errorCells = if (isError) {
            // 违反笼子约束：整个笼子的格子一起标红
            if (isKiller) puzzle.conflictCells() else errorCells + "$r,$c"
        } else {
            errorCells - "$r,$c"
        }
        revision++
        pushUndo(UndoEntry(r, c, old, oldNotes, n, emptySet()))
        SoundManager.placement()
        if (isError) {
            errors++
            if (errors >= maxErrors) {
                stopTimer()
                SoundManager.failed()
                lastScore = calculateScore()
                paused = true
                gameOver = true
                return
            }
        }
    }

    /** 清除当前选中格（Backspace/Delete） */
    fun clearSelected() {
        val r = selectedRow ?: return
        val c = selectedCol ?: return
        if (paused || gameOver) return
        if (puzzle.given[r][c]) return
        val old = puzzle.cells[r][c]
        val oldNotes = puzzle.notes[r][c].toSet()
        if (old == 0 && oldNotes.isEmpty()) return
        dirty = true
        puzzle.cells[r][c] = 0
        puzzle.notes[r][c].clear()
        errorCells = errorCells - "$r,$c"
        revision++
        if (old != 0) {
            pushUndo(UndoEntry(r, c, old, oldNotes, 0, emptySet()))
            SoundManager.placement()
        }
    }

    private fun pushUndo(entry: UndoEntry) {
        undoStack.addLast(entry)
        if (undoStack.size > 50) undoStack.removeFirst()
        undoDepth = undoStack.size
        redoStack.clear()
        redoDepth = 0
    }

    fun undo() {
        SoundManager.click()
        if (undoStack.isEmpty() || paused || gameOver) return
        dirty = true
        val entry = undoStack.removeLast()
        redoStack.addLast(entry)
        undoDepth = undoStack.size
        redoDepth = redoStack.size
        puzzle.cells[entry.r][entry.c] = entry.oldVal
        puzzle.notes[entry.r][entry.c] = entry.oldNotes.toMutableSet()
        revision++
        syncErrors()
    }

    fun redo() {
        SoundManager.click()
        if (redoStack.isEmpty() || paused || gameOver) return
        dirty = true
        val entry = redoStack.removeLast()
        undoStack.addLast(entry)
        undoDepth = undoStack.size
        redoDepth = redoStack.size
        puzzle.cells[entry.r][entry.c] = entry.newVal
        puzzle.notes[entry.r][entry.c] = entry.newNotes.toMutableSet()
        revision++
        syncErrors()
    }

    fun syncErrors() {
        val gs = puzzle.gridSize
        val bad = mutableSetOf<String>()
        for (r in 0 until gs) {
            for (c in 0 until gs) {
                val v = puzzle.cells[r][c]
                if (v == 0) continue
                val conflict = if (isKiller) puzzle.isConflictAt(r, c, v) else v != puzzle.solution[r][c]
                if (conflict) bad.add("$r,$c")
            }
        }
        errorCells = bad
    }

    fun restart() {
        SoundManager.click()
        undoStack.clear()
        redoStack.clear()
        undoDepth = 0
        redoDepth = 0
        val gs = puzzle.gridSize
        for (r in 0 until gs) {
            for (c in 0 until gs) {
                if (!puzzle.given[r][c]) puzzle.cells[r][c] = 0
                puzzle.notes[r][c].clear()
            }
        }
        errors = 0
        gameOver = false
        isSolved = false
        hasGivenUp = false
        errorCells = emptySet()
        seconds = 0
        statusJob?.cancel()
        statusMsg = ""
        revision++
        startTimer()
    }

    fun checkCompletion() {
        SoundManager.click()
        if (paused || gameOver) return
        if (puzzle.isComplete() && puzzle.isCorrect()) {
            stopTimer()
            SoundManager.success()
            scope.launch {
                lastScore = submitScore(won = true)
                paused = true
                isSolved = true
            }
        } else {
            showStatus("还有空格未填，请再检查一下吧")
        }
    }

    fun autoSolve() {
        SoundManager.click()
        stopTimer()
        val gs = puzzle.gridSize
        for (r in 0 until gs) {
            for (c in 0 until gs) {
                puzzle.cells[r][c] = puzzle.solution[r][c]
            }
        }
        paused = true
        hasGivenUp = true
        revision++
        syncErrors()
    }

    // ---- 模式切换 ----
    fun switchMode(mode: String) {
        if (!SoundManager.debounce()) return
        SoundManager.vibrate()
        val newKiller = mode == "3×3-killer"
        val newSize = if (mode == "4×4") 4 else 3
        if (newSize != boardSize || newKiller != isKiller) {
            boardSize = newSize
            isKiller = newKiller
            newGame(silent = true)
        }
    }

    // ---- 存档 ----
    fun saveGame(silent: Boolean = false, successMsg: String = "存档成功", failMsg: String = "存档失败，请检查网络连接后重试") {
        if (saving) {
            // 已有存档请求在途：记录本次存档参数，完成后自动补存
            saveAgain = true
            saveAgainSilent = silent
            saveAgainSuccess = successMsg
            saveAgainFail = failMsg
            return
        }
        saving = true
        if (!silent) showStatus("正在保存...")
        scope.launch {
            try {
                ApiClient.saveGame(
                    username = username,
                    boardSize = boardSize,
                    cells = puzzle.cells,
                    notes = puzzle.notes,
                    solution = puzzle.solution,
                    given = puzzle.given,
                    seconds = seconds,
                    errors = errors,
                    isKiller = isKiller,
                    killerDifficulty = killerDifficulty,
                    cages = puzzle.cages,
                )
                if (!silent) showStatus(successMsg)
            } catch (_: Exception) {
                if (!silent) showStatus(failMsg)
            } finally {
                saving = false
                if (saveAgain) {
                    saveAgain = false
                    saveGame(saveAgainSilent, saveAgainSuccess, saveAgainFail)
                }
            }
        }
    }

    /** 等待在途的自动存档完成，避免读档拿到旧数据 */
    suspend fun awaitPendingSave() {
        while (saving) delay(100)
    }

    fun autoSave() {
        if (!dirty) return
        if (!gameOver && !isSolved && seconds > 3) saveGame(silent = true)
    }

    suspend fun fetchSave(): JSONObject? = try {
        ApiClient.loadGame(username)
    } catch (_: Exception) {
        null
    }

    fun restoreFromData(res: JSONObject) {
        gameGen++ // 使进行中的新局生成失效，防止其覆盖恢复的棋盘与计时
        val boardSize = res.optInt("boardSize", 3)
        val isKiller = res.optBoolean("isKiller")
        val cellsRaw = res.optJSONArray("cells") ?: JSONArray()
        val notesRaw = res.optJSONArray("notes") ?: JSONArray()
        val solutionRaw = res.optJSONArray("solution") ?: JSONArray()
        val givenRaw = res.optJSONArray("given") ?: JSONArray()
        val seconds = res.optInt("seconds", 0)
        val errors = res.optInt("errors", 0)
        val killerDifficulty = res.optString("killerDifficulty", "中等")
        val cagesRaw = res.optJSONArray("cages")

        val gs = boardSize * boardSize
        this.boardSize = boardSize
        this.isKiller = isKiller
        this.killerDifficulty = killerDifficulty
        val p = SudokuPuzzle(boardSize)
        for (r in 0 until gs) {
            for (c in 0 until gs) {
                val row = cellsRaw.optJSONArray(r)
                if (row != null && c < row.length()) p.cells[r][c] = row.optInt(c, 0)
                val sol = solutionRaw.optJSONArray(r)
                if (sol != null && c < sol.length()) p.solution[r][c] = sol.optInt(c, 0)
                val giv = givenRaw.optJSONArray(r)
                if (giv != null && c < giv.length()) p.given[r][c] = giv.optInt(c, 0) == 1
                val notesRow = notesRaw.optJSONArray(r)
                if (notesRow != null && c < notesRow.length()) {
                    val noteList = notesRow.optJSONArray(c)
                    if (noteList != null) {
                        for (i in 0 until noteList.length()) p.notes[r][c].add(noteList.optInt(i))
                    }
                }
            }
        }
        if (isKiller && cagesRaw != null) {
            val cages = mutableListOf<Cage>()
            for (i in 0 until cagesRaw.length()) {
                val cMap = cagesRaw.optJSONObject(i) ?: continue
                val indices = mutableListOf<Int>()
                val idxArr = cMap.optJSONArray("cellIndices") ?: JSONArray()
                for (j in 0 until idxArr.length()) indices.add(idxArr.optInt(j))
                val op = cMap.optString("op", "+").firstOrNull() ?: '+'
                cages.add(Cage(indices, cMap.optInt("sum", 0), op))
            }
            p.cages = cages
        }
        puzzle = p
        this.seconds = seconds
        this.errors = errors
        isSolved = false
        hasGivenUp = false
        gameOver = errors >= (if (boardSize == 3) 3 else 6)
        paused = false
        // 读档恢复的进度视为可存档：暂停/退出自动保存（含计时），无需再动棋盘
        dirty = true
        undoStack.clear()
        redoStack.clear()
        undoDepth = 0
        redoDepth = 0
        syncErrors()
        selectedRow = null
        selectedCol = null
        revision++
        startTimer()
        showStatus("存档已恢复")
    }

    // ---- 积分 ----
    private fun standardTime(): Int {
        if (boardSize == 4) {
            return when (difficulty) {
                "简单" -> 3600
                "中等" -> 7200
                "困难" -> 14400
                else -> 7200
            }
        }
        if (isKiller) {
            return when (killerDifficulty) {
                "入门" -> 2400
                "中等" -> 4800
                "困难" -> 9600
                else -> 4800
            }
        }
        return when (difficulty) {
            "简单" -> 1800
            "中等" -> 3600
            "困难", "极简" -> 7200
            else -> 3600
        }
    }

    fun calculateScore(): Int {
        val base: Double = when {
            isKiller -> 200.0
            boardSize == 4 -> 250.0
            else -> 100.0
        }
        val diff = if (isKiller) killerDifficulty else difficulty
        val diffCoeff: Double = when (diff) {
            "简单", "入门" -> 1.0
            "中等" -> 1.5
            "困难", "极简" -> 2.0
            else -> 1.0
        }
        var timeCoeff = (standardTime().toDouble() / seconds) * 0.5 + 0.5
        timeCoeff = timeCoeff.coerceIn(0.5, 5.0)
        var errorPenalty = (maxErrors - errors).toDouble() / maxErrors
        if (errorPenalty < 0) errorPenalty = 0.0
        return (base * diffCoeff * timeCoeff * errorPenalty).roundToInt()
    }

    private suspend fun submitScore(won: Boolean): Int {
        val score = calculateScore()
        val mode = when {
            isKiller -> "算数$killerDifficulty"
            boardSize == 4 -> "4×4$difficulty"
            else -> "3×3$difficulty"
        }
        return try {
            val res = ApiClient.submitScore(username, won, mode, boardSize, score)
            if (won) {
                if (res.optBoolean("success")) showStatus("积分已保存：$score 分") else showStatus("提交失败")
            }
            score
        } catch (_: Exception) {
            if (won) showStatus("提交失败")
            score
        }
    }

    // ---- UI 辅助 ----
    fun showStatus(msg: String) {
        statusMsg = msg
        statusJob?.cancel()
        statusJob = scope.launch {
            delay(4000)
            // 保存/读档请求在途时暂不清空，避免“正在保存...”和结果之间闪现“已暂停”；
            // 等待在途请求结束后再清空，防止结果消息残留
            while (saving || loadingSave) delay(100)
            statusMsg = ""
        }
    }

    fun formatTime(s: Int): String {
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return "%02d:%02d:%02d".format(h, m, sec)
    }

    fun cluesRemaining(): Int {
        val gs = puzzle.gridSize
        var n = 0
        for (r in 0 until gs) {
            for (c in 0 until gs) {
                if (puzzle.cells[r][c] == 0) n++
            }
        }
        return n
    }
}
