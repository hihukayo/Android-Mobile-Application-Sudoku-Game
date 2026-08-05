package com.example.sudoku.model

import kotlin.random.Random

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
        "困难" -> listOf(30, 30, 20, 20)
        else -> listOf(40, 35, 15, 10)
    }

    /** 生成杀手数独 */
    fun generateKiller(difficulty: String = "中等"): SudokuPuzzle {
        require(boardSize == 3) { "杀手数独仅支持 3×3" }
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

                // 从笼子任意边界扩展（支持 L 型等异形）
                while (cage.size < size) {
                    val candidates = mutableSetOf<Int>()
                    for (idx in cage) {
                        val r = idx / gs
                        val c = idx % gs
                        for ((dr, dc) in dirs) {
                            val nr = r + dr
                            val nc = c + dc
                            if (nr < 0 || nr >= gs || nc < 0 || nc >= gs) continue
                            val nIdx = nr * gs + nc
                            if (assigned[nIdx] == -1) candidates.add(nIdx)
                        }
                    }
                    if (candidates.isEmpty()) {
                        ok = false
                        break
                    }
                    val chosen = candidates.elementAt(rng.nextInt(candidates.size))
                    cage.add(chosen)
                    assigned[chosen] = cages.size
                }
                if (!ok) break
                cages.add(cage)
            }

            // 检查全部格已分配
            if (ok && assigned.all { it != -1 }) {
                // 计算和值写入 puzzle
                puzzle.cages = cages.map { c ->
                    var sum = 0
                    for (idx in c) sum += puzzle.solution[idx / gs][idx % gs]
                    Cage(cellIndices = c.toList(), sum = sum)
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
            for (sz in 2..5) {
                cum += probs[sz - 2]
                if (roll < cum) {
                    if (remaining < sz) break
                    if (sz == 4 && count4 >= max4) continue
                    if (sz == 5 && difficulty == "入门") continue
                    val rest = remaining - sz
                    if (rest == 0 || rest >= 2) return sz
                }
            }
        }
        // fallback: 取能放下的最大尺寸
        for (sz in listOf(5, 4, 3, 2)) {
            if (remaining >= sz) {
                if (sz == 4 && count4 >= max4) continue
                if (sz == 5 && difficulty == "入门") continue
                val rest = remaining - sz
                if (rest == 0 || rest >= 2) return sz
            }
        }
        return -1
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
                if (countSolutions(puzzle.clone(), 2) != 1) {
                    puzzle.cells[r][c] = saved
                } else {
                    target--
                }
            } else {
                target--
            }
        }
    }

    private fun countSolutions(puzzle: SudokuPuzzle, limit: Int): Int {
        var count = 0
        fun solve(grid: Array<IntArray>) {
            if (count >= limit) return
            val empty = findEmpty(grid) ?: run { count++; return }
            val (r, c) = empty
            for (n in 1..gridSize) {
                if (isValid(grid, r, c, n)) {
                    grid[r][c] = n
                    solve(grid)
                    grid[r][c] = 0
                    if (count >= limit) return
                }
            }
        }
        val grid = Array(gridSize) { r -> puzzle.cells[r].copyOf() }
        solve(grid)
        return count
    }
}
