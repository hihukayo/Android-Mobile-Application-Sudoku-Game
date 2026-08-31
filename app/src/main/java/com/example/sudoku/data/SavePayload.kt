package com.example.sudoku.data

import com.example.sudoku.model.Cage
import org.json.JSONArray
import org.json.JSONObject

/** 把一局棋序列化成与后端 /save 请求一致的 JSON（本地与云端共用） */
fun buildSaveJson(
    username: String,
    boardSize: Int,
    cells: Array<IntArray>,
    notes: Array<Array<MutableSet<Int>>>,
    solution: Array<IntArray>,
    given: Array<BooleanArray>,
    seconds: Int,
    errors: Int,
    isKiller: Boolean,
    killerDifficulty: String,
    seed: Int,
    cages: List<Cage>?,
): JSONObject {
    fun nestedInts(grid: Array<IntArray>): JSONArray {
        val arr = JSONArray()
        for (r in grid) {
            val row = JSONArray()
            for (v in r) row.put(v)
            arr.put(row)
        }
        return arr
    }
    val notesArr = JSONArray()
    for (row in notes) {
        val rowArr = JSONArray()
        for (s in row) {
            val list = JSONArray()
            for (v in s) list.put(v)
            rowArr.put(list)
        }
        notesArr.put(rowArr)
    }
    val givenArr = JSONArray()
    for (row in given) {
        val rowArr = JSONArray()
        for (b in row) rowArr.put(if (b) 1 else 0)
        givenArr.put(rowArr)
    }
    val cagesArr = JSONArray()
    cages?.forEach { cage ->
        val cageJson = JSONObject()
        val idx = JSONArray()
        for (i in cage.cellIndices) idx.put(i)
        cageJson.put("cellIndices", idx)
        cageJson.put("sum", cage.sum)
        cageJson.put("op", cage.op.toString())
        cagesArr.put(cageJson)
    }
    return JSONObject().apply {
        put("username", username)
        put("boardSize", boardSize)
        put("cells", nestedInts(cells))
        put("notes", notesArr)
        put("solution", nestedInts(solution))
        put("given", givenArr)
        put("seconds", seconds)
        put("errors", errors)
        put("isKiller", isKiller)
        put("killerDifficulty", killerDifficulty)
        put("seed", seed)
        put("cages", cagesArr)
    }
}
