package com.example.sudoku.model

import kotlin.math.abs

/** 算数数独的笼子（Cage），op 为运算符：'+' 求和（默认）、'-' 求差、'×' 求积、'÷' 求商 */
class Cage(val cellIndices: List<Int>, val sum: Int, val op: Char = '+') {
    fun contains(r: Int, c: Int, gridSize: Int): Boolean = cellIndices.contains(r * gridSize + c)

    /** 笼子标签文本：运算符 + 结果，如 +10、-2、×100、÷3 */
    fun labelText(): String = "$op$sum"
}

class SudokuPuzzle(val boardSize: Int = 3) {
    val gridSize: Int = boardSize * boardSize
    val cells: Array<IntArray> = Array(gridSize) { IntArray(gridSize) }
    val given: Array<BooleanArray> = Array(gridSize) { BooleanArray(gridSize) }
    val notes: Array<Array<MutableSet<Int>>> = Array(gridSize) { Array(gridSize) { mutableSetOf() } }
    val solution: Array<IntArray> = Array(gridSize) { IntArray(gridSize) }
    var cages: MutableList<Cage>? = null
    var killerDifficulty: String = "中等"
    private var _cageLookup: List<Int>? = null

    val isKiller: Boolean get() = cages != null

    /** 谜题指纹：同一局（相同棋盘）只统计一次，与后端 puzzle_key 对应 */
    fun fingerprint(): String {
        val sb = StringBuilder()
        sb.append(boardSize).append('|')
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                sb.append(if (given[r][c]) '1' else '0')
            }
        }
        sb.append('|')
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (r > 0 || c > 0) sb.append(',')
                sb.append(solution[r][c])
            }
        }
        cages?.let { cageList ->
            sb.append('|')
            for ((i, cage) in cageList.withIndex()) {
                if (i > 0) sb.append(';')
                sb.append(cage.op).append(cage.sum).append('[')
                for ((j, idx) in cage.cellIndices.withIndex()) {
                    if (j > 0) sb.append('_')
                    sb.append(idx)
                }
                sb.append(']')
            }
        }
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(sb.toString().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    val cageLookup: List<Int>
        get() {
            _cageLookup?.let { return it }
            val cages = cages ?: return emptyList()
            val lookup = MutableList(gridSize * gridSize) { -1 }
            for (i in cages.indices) {
                for (idx in cages[i].cellIndices) lookup[idx] = i
            }
            _cageLookup = lookup
            return lookup
        }

    fun clone(): SudokuPuzzle {
        val p = SudokuPuzzle(boardSize)
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                p.cells[r][c] = cells[r][c]
                p.given[r][c] = given[r][c]
                p.notes[r][c] = notes[r][c].toMutableSet()
                p.solution[r][c] = solution[r][c]
            }
        }
        cages?.let { p.cages = it.map { cage -> Cage(cage.cellIndices.toList(), cage.sum, cage.op) }.toMutableList() }
        return p
    }

    fun setNote(r: Int, c: Int, n: Int) {
        notes[r][c].clear()
        notes[r][c].add(n)
    }

    fun isComplete(): Boolean {
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (cells[r][c] == 0) return false
            }
        }
        return true
    }

    /** 检查在 (r,c) 填 n 是否导致冲突（行列宫重复 或 笼子和值超限） */
    fun isConflictAt(r: Int, c: Int, n: Int): Boolean {
        for (i in 0 until gridSize) {
            if (i != c && cells[r][i] == n) return true
            if (i != r && cells[i][c] == n) return true
        }
        val br = r - r % boardSize
        val bc = c - c % boardSize
        for (ir in br until br + boardSize) {
            for (ic in bc until bc + boardSize) {
                if ((ir != r || ic != c) && cells[ir][ic] == n) return true
            }
        }
        cages?.let { cages ->
            val idx = r * gridSize + c
            for (cage in cages) {
                if (!cage.cellIndices.contains(idx)) continue
                if (cageBrokenWith(cage, idx, n)) return true
            }
        }
        return false
    }

    /** 找出所有冲突格子 */
    fun conflictCells(): Set<String> {
        val result = mutableSetOf<String>()
        cages?.let { cages ->
            val bad = invalidCages()
            for (ci in bad) {
                for (idx in cages[ci].cellIndices) result.add("${idx / gridSize},${idx % gridSize}")
            }
        }
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (cells[r][c] == 0) continue
                if (result.contains("$r,$c")) continue
                if (isConflictAt(r, c, cells[r][c])) result.add("$r,$c")
            }
        }
        return result
    }

    /** 找出当前填数违反笼子约束的笼子索引 */
    fun invalidCages(): Set<Int> {
        val cages = cages ?: return emptySet()
        val result = mutableSetOf<Int>()
        for (i in cages.indices) {
            if (cageBroken(cages[i])) result.add(i)
        }
        return result
    }

    /** 在 (r,c) 填入 n 后，笼子约束是否被违反（n 尚未写入 cells） */
    private fun cageBrokenWith(cage: Cage, idx: Int, n: Int): Boolean {
        val vals = cage.cellIndices.map { if (it == idx) n else cells[it / gridSize][it % gridSize] }
        return when (cage.op) {
            '-' -> vals.size == 2 && vals[0] != 0 && vals[1] != 0 && abs(vals[0] - vals[1]) != cage.sum
            '÷' -> {
                if (vals.size != 2 || vals[0] == 0 || vals[1] == 0) false
                else {
                    val a = maxOf(vals[0], vals[1])
                    val b = minOf(vals[0], vals[1])
                    a % b != 0 || a / b != cage.sum
                }
            }
            '×' -> {
                var prod = 1L
                for (v in vals) {
                    if (v == 0) return false
                    prod *= v
                    if (prod > cage.sum) return true
                }
                false
            }
            else -> {
                var s = 0
                for (v in vals) s += v
                s > cage.sum
            }
        }
    }

    /** 根据当前填数判断笼子约束是否被违反（'+' 超和、'×' 超积、'-'/'÷' 填满后不符） */
    private fun cageBroken(cage: Cage): Boolean {
        val vals = cage.cellIndices.map { cells[it / gridSize][it % gridSize] }
        return when (cage.op) {
            '-' -> vals.size == 2 && vals[0] != 0 && vals[1] != 0 && abs(vals[0] - vals[1]) != cage.sum
            '÷' -> {
                if (vals.size != 2 || vals[0] == 0 || vals[1] == 0) false
                else {
                    val a = maxOf(vals[0], vals[1])
                    val b = minOf(vals[0], vals[1])
                    a % b != 0 || a / b != cage.sum
                }
            }
            '×' -> {
                var prod = 1L
                for (v in vals) {
                    if (v == 0) return false
                    prod *= v
                    if (prod > cage.sum) return true
                }
                false
            }
            else -> {
                var s = 0
                for (v in vals) s += v
                s > cage.sum
            }
        }
    }

    /** 验证行/列/宫唯一性 */
    fun hasDuplicates(): Boolean {
        for (r in 0 until gridSize) {
            val rowSet = mutableSetOf<Int>()
            for (c in 0 until gridSize) {
                if (cells[r][c] == 0) continue
                if (!rowSet.add(cells[r][c])) return true
            }
        }
        for (c in 0 until gridSize) {
            val colSet = mutableSetOf<Int>()
            for (r in 0 until gridSize) {
                if (cells[r][c] == 0) continue
                if (!colSet.add(cells[r][c])) return true
            }
        }
        for (br in 0 until gridSize step boardSize) {
            for (bc in 0 until gridSize step boardSize) {
                val boxSet = mutableSetOf<Int>()
                for (r in br until br + boardSize) {
                    for (c in bc until bc + boardSize) {
                        if (cells[r][c] == 0) continue
                        if (!boxSet.add(cells[r][c])) return true
                    }
                }
            }
        }
        return false
    }

    fun isCorrect(): Boolean {
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (cells[r][c] != solution[r][c]) return false
            }
        }
        return true
    }

    /** 判断两个格子是否在同一笼子（杀手数独） */
    fun sameCage(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        val lookup = cageLookup
        if (lookup.isEmpty()) return false
        val idx1 = r1 * gridSize + c1
        val idx2 = r2 * gridSize + c2
        if (idx1 >= lookup.size || idx2 >= lookup.size) return false
        return lookup[idx1] >= 0 && lookup[idx1] == lookup[idx2]
    }

    /** 获取指定格子的笼子和值，null 表示不是笼子首格或无笼子 */
    fun cageInfoAt(r: Int, c: Int): Pair<Int, Boolean>? {
        val lookup = cageLookup
        if (lookup.isEmpty()) return null
        val idx = r * gridSize + c
        if (idx >= lookup.size) return null
        val cageIdx = lookup[idx]
        if (cageIdx < 0) return null
        val cage = cages!![cageIdx]
        var isFirst = true
        for (other in cage.cellIndices) {
            val or = other / gridSize
            val oc = other % gridSize
            if (or < r || (or == r && oc > c)) {
                isFirst = false
                break
            }
        }
        return cage.sum to isFirst
    }

    companion object {
        /** 将数值转换为显示字符：1-9 显示数字，10+ 显示 A-F */
        fun displayValue(v: Int): String = when {
            v in 1..9 -> "$v"
            v in 10..16 -> ('A' + (v - 10)).toString()
            else -> ""
        }
    }
}
