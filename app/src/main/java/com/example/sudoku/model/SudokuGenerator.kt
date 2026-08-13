package com.example.sudoku.model

import kotlin.random.Random
import kotlin.math.abs

class SudokuGenerator(val boardSize: Int = 3, seed: Int? = null) {
    private val rng: Random = if (seed != null) kotlin.random.Random(seed) else Random
    val gridSize: Int get() = boardSize * boardSize

    fun generate(clues: Int = 30): SudokuPuzzle {
        val puzzle = SudokuPuzzle(boardSize)
        fillGrid(puzzle.solution)
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                puzzle.cells[r][c] = puzzle.solution[r][c]
            }
        }
        removeCells(puzzle, clues)
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                puzzle.given[r][c] = puzzle.cells[r][c] != 0
            }
        }
        return puzzle
    }

    /** 难度对应的笼子大小概率 [2格, 3格, 4格, 5格] */
    private fun cageProbs(difficulty: String): List<Int> = when (difficulty) {
        "入门" -> listOf(60, 35, 5, 0)
        "困难" -> listOf(30, 30, 40, 0)
        else -> listOf(40, 35, 25, 0)
    }

    /** 生成算数数独 */
    fun generateKiller(difficulty: String = "中等"): SudokuPuzzle {
        require(boardSize == 3) { "算数数独仅支持 3×3" }
        val maxAttempts = 50
        repeat(maxAttempts) {
            val puzzle = SudokuPuzzle(boardSize)
            fillGrid(puzzle.solution)
            if (generateCages(puzzle, difficulty)) {
                puzzle.killerDifficulty = difficulty
                // 清空所有格子（杀手数独不给任何数字）
                for (r in 0 until gridSize) {
                    for (c in 0 until gridSize) {
                        puzzle.cells[r][c] = 0
                        puzzle.given[r][c] = false
                    }
                }
                return puzzle
            }
        }
        // 保底：返回一个简单难度生成的谜题
        return generateKiller("入门")
    }

    /** 快速生成笼子划分（迭代 + 异形支持，超时则重试） */
    private fun generateCages(puzzle: SudokuPuzzle, difficulty: String): Boolean {
        val gs = gridSize
        val total = gs * gs
        val probs = cageProbs(difficulty)
        val dirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

        for (attempt in 0 until 30) {
            val assigned = MutableList(total) { -1 }
            val cages = mutableListOf<List<Int>>()
            var count4 = 0
            var ok = true

            for (i in 0 until total) {
                if (assigned[i] != -1) continue
                val size = pickSize(assigned, probs, difficulty, count4)
                if (size < 0) {
                    ok = false
                    break
                }
                if (size == 4) count4++

                val cage = mutableListOf(i)
                assigned[i] = cages.size

                // 从笼子任意边界扩展（加权随机：与笼子相邻边多的格子优先，形成俄罗斯方块式异形）
                while (cage.size < size) {
                    val scores = mutableMapOf<Int, Int>()
                    for (idx in cage) {
                        val r = idx / gs
                        val c = idx % gs
                        for ((dr, dc) in dirs) {
                            val nr = r + dr
                            val nc = c + dc
                            if (nr < 0 || nr >= gs || nc < 0 || nc >= gs) continue
                            val nIdx = nr * gs + nc
                            if (assigned[nIdx] == -1) scores[nIdx] = (scores[nIdx] ?: 0) + 1
                        }
                    }
                    if (scores.isEmpty()) {
                        ok = false
                        break
                    }
                    val entries = scores.entries.toList()
                    val weights = entries.map { it.value * it.value }
                    val total = weights.sum()
                    var roll = rng.nextInt(total)
                    var chosen = entries.last().key
                    for (i in entries.indices) {
                        roll -= weights[i]
                        if (roll < 0) {
                            chosen = entries[i].key
                            break
                        }
                    }
                    cage.add(chosen)
                    assigned[chosen] = cages.size
                }
                if (!ok) break
                cages.add(cage)
            }

            // 检查全部格已分配
            if (ok && assigned.all { it != -1 }) {
                // 为每个笼子挑选算数运算符并写入 puzzle
                puzzle.cages = cages.map { c ->
                    val values = c.map { puzzle.solution[it / gs][it % gs] }
                    val chosen = pickCageOp(c, values)
                    Cage(cellIndices = c.toList(), sum = chosen.second, op = chosen.first)
                }.toMutableList()
                return true
            }
        }
        return false
    }

    /** 根据概率选取笼子大小，-1 表示剩余格子无法组成有效笼子 */
    private fun pickSize(assigned: List<Int>, probs: List<Int>, difficulty: String, count4: Int): Int {
        val remaining = assigned.count { it == -1 }
        if (remaining < 2) return remaining

        // 入门难度限制 4 格笼子不超过 3 个
        val max4 = if (difficulty == "入门") (if (count4 >= 3) 0 else 3) else 99

        // 按概率选取大小
        for (attempt in 0 until 20) {
            val roll = rng.nextInt(100)
            var cum = 0
            for (sz in 2..4) {
                cum += probs[sz - 2]
                if (roll < cum) {
                    if (remaining < sz) break
                    if (sz == 4 && count4 >= max4) continue

                    val rest = remaining - sz
                    if (rest == 0 || rest >= 2) return sz
                }
            }
        }
        // fallback: 取能放下的最大尺寸
        for (sz in listOf(4, 3, 2)) {
            if (remaining >= sz) {
                if (sz == 4 && count4 >= max4) continue
                val rest = remaining - sz
                if (rest == 0 || rest >= 2) return sz
            }
        }
        return -1
    }

    /** 为笼子挑选算数运算符：2 格支持 + - × ÷，3/4 格只支持 + 或乘积较小的 × */
    private fun pickCageOp(cage: List<Int>, values: List<Int>): Pair<Char, Int> {
        val sum = values.sum()
        val ops = mutableListOf<Pair<Char, Int>>()
        val weights = mutableListOf<Int>()
        fun add(op: Char, target: Int, weight: Int) {
            ops.add(op to target)
            weights.add(weight)
        }
        if (cage.size == 2) {
            val a = maxOf(values[0], values[1])
            val b = minOf(values[0], values[1])
            val diff = a - b
            val quotient = if (b > 0 && a % b == 0) a / b else -1
            add('+', sum, 25)
            if (quotient in 2..9) add('÷', quotient, 65)
            if (diff >= 2) add('-', diff, 10)
            add('×', a * b, 10)
        } else {
            val product = values.fold(1L) { acc, v -> acc * v }
            add('+', sum, 90)
            if (product <= 50) add('×', product.toInt(), 10)
        }
        val total = weights.sum()
        var roll = rng.nextInt(total)
        for (i in ops.indices) {
            roll -= weights[i]
            if (roll < 0) return ops[i]
        }
        return ops.last()
    }

    fun fillGrid(grid: Array<IntArray>): Boolean {
        val empty = findEmpty(grid) ?: return true
        val (r, c) = empty
        val nums = (1..gridSize).toMutableList().apply { shuffle(rng) }
        for (n in nums) {
            if (isValid(grid, r, c, n)) {
                grid[r][c] = n
                if (fillGrid(grid)) return true
                grid[r][c] = 0
            }
        }
        return false
    }

    private fun isValid(grid: Array<IntArray>, r: Int, c: Int, n: Int): Boolean {
        for (i in 0 until gridSize) {
            if (grid[r][i] == n) return false
            if (grid[i][c] == n) return false
        }
        val br = r - r % boardSize
        val bc = c - c % boardSize
        for (i in br until br + boardSize) {
            for (j in bc until bc + boardSize) {
                if (grid[i][j] == n) return false
            }
        }
        return true
    }

    private fun findEmpty(grid: Array<IntArray>): Pair<Int, Int>? {
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (grid[r][c] == 0) return r to c
            }
        }
        return null
    }

    private fun removeCells(puzzle: SudokuPuzzle, clues: Int) {
        val total = gridSize * gridSize
        val all = (0 until total).toMutableList().apply { shuffle(rng) }
        var target = total - clues
        for (pos in all) {
            if (target <= 0) break
            val r = pos / gridSize
            val c = pos % gridSize
            val saved = puzzle.cells[r][c]
            puzzle.cells[r][c] = 0
            if (boardSize == 3) {
                if (countSolutions(puzzle, 2) != 1) {
                    puzzle.cells[r][c] = saved
                } else {
                    target--
                }
            } else {
                target--
            }
        }
    }

    /** 快速统计解的数量（MRV 最少候选优先 + 位掩码，达到 limit 即提前返回） */
    private fun countSolutions(puzzle: SudokuPuzzle, limit: Int): Int {
        val gs = gridSize
        val b = boardSize
        val grid = Array(gs) { r -> puzzle.cells[r].copyOf() }
        val rowMask = IntArray(gs)
        val colMask = IntArray(gs)
        val boxMask = IntArray(gs)
        for (r in 0 until gs) {
            for (c in 0 until gs) {
                val v = grid[r][c]
                if (v != 0) {
                    val bit = 1 shl (v - 1)
                    rowMask[r] = rowMask[r] or bit
                    colMask[c] = colMask[c] or bit
                    boxMask[(r / b) * b + (c / b)] = boxMask[(r / b) * b + (c / b)] or bit
                }
            }
        }
        var count = 0
        val full = (1 shl gs) - 1

        fun solve() {
            if (count >= limit) return
            // MRV：选出候选数最少的空格（局部变量，避免递归相互覆盖）
            var br = -1
            var bc = -1
            var bCand = 0
            var best = 10
            for (r in 0 until gs) {
                for (c in 0 until gs) {
                    if (grid[r][c] != 0) continue
                    val used = rowMask[r] or colMask[c] or boxMask[(r / b) * b + (c / b)]
                    val cand = full and used.inv()
                    val n = cand.countOneBits()
                    if (n < best) {
                        best = n; br = r; bc = c; bCand = cand
                        if (n <= 1) break // 唯一候选或死路
                    }
                }
                if (best <= 1) break
            }
            if (br < 0) { count++; return }
            var cand = bCand
            val boxIdx = (br / b) * b + (bc / b)
            while (cand != 0) {
                val bit = cand and -cand
                cand = cand xor bit
                grid[br][bc] = Integer.numberOfTrailingZeros(bit) + 1
                rowMask[br] = rowMask[br] or bit
                colMask[bc] = colMask[bc] or bit
                boxMask[boxIdx] = boxMask[boxIdx] or bit
                solve()
                rowMask[br] = rowMask[br] xor bit
                colMask[bc] = colMask[bc] xor bit
                boxMask[boxIdx] = boxMask[boxIdx] xor bit
                grid[br][bc] = 0
                if (count >= limit) return
            }
        }
        solve()
        return count
    }
}
