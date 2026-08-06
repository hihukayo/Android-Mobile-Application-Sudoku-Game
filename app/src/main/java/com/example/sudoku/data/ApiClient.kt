package com.example.sudoku.data

import android.os.Build
import com.example.sudoku.model.Cage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {
    /** 自动区分模拟器（10.0.2.2）与真机（adb reverse 后 localhost） */
    val baseUrl: String
        get() {
            val custom = Session.getServerAddress()
            if (custom.isNotBlank()) return "http://$custom/api"
            val fingerprint =
                "${Build.MODEL} ${Build.PRODUCT} ${Build.FINGERPRINT}".lowercase()
            return if (fingerprint.contains("sdk") ||
                fingerprint.contains("generic") ||
                fingerprint.contains("emulator")
            ) {
                "http://10.0.2.2:8080/api"
            } else {
                "http://localhost:8080/api"
            }
        }

    /** 单次请求总超时（毫秒），超过即停止，避免一直转圈 */
    private const val REQUEST_TIMEOUT_MS = 8000L

    private suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject =
        withTimeout(REQUEST_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                val conn = URL("$baseUrl$path").openConnection() as HttpURLConnection
                try {
                    conn.requestMethod = method
                    conn.connectTimeout = 5000
                    conn.readTimeout = 8000
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.doInput = true
                    if (body != null) {
                        conn.doOutput = true
                        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    }
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                    JSONObject(text)
                } finally {
                    conn.disconnect()
                }
            }
        }

    suspend fun register(username: String, phone: String, password: String): JSONObject =
        request("POST", "/register", JSONObject().apply {
            put("username", username)
            put("phone", phone)
            put("password", password)
        })

    suspend fun login(account: String, password: String): JSONObject =
        request("POST", "/login", JSONObject().apply {
            put("account", account)
            put("password", password)
        })

    suspend fun updateUsername(username: String, newUsername: String, password: String): JSONObject =
        request("PUT", "/user/update-username", JSONObject().apply {
            put("username", username)
            put("newUsername", newUsername)
            put("password", password)
        })

    suspend fun updatePassword(username: String, oldPassword: String, newPassword: String): JSONObject =
        request("PUT", "/user/update-password", JSONObject().apply {
            put("username", username)
            put("oldPassword", oldPassword)
            put("newPassword", newPassword)
        })

    suspend fun updatePhone(username: String, newPhone: String, password: String): JSONObject =
        request("PUT", "/user/update-phone", JSONObject().apply {
            put("username", username)
            put("newPhone", newPhone)
            put("password", password)
        })

    suspend fun deleteAccount(username: String, phone: String, password: String): JSONObject =
        request("DELETE", "/user/delete", JSONObject().apply {
            put("username", username)
            put("phone", phone)
            put("password", password)
        })

    suspend fun saveGame(
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
            cagesArr.put(cageJson)
        }
        return request("POST", "/save", JSONObject().apply {
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
            put("cages", cagesArr)
        })
    }

    suspend fun loadGame(username: String): JSONObject =
        request("GET", "/load?username=${java.net.URLEncoder.encode(username, "UTF-8")}")

    suspend fun submitScore(
        username: String,
        won: Boolean,
        gameMode: String,
        boardSize: Int,
        score: Int,
    ): JSONObject = request("POST", "/rank/submit", JSONObject().apply {
        put("username", username)
        put("won", won)
        put("gameMode", gameMode)
        put("boardSize", boardSize)
        put("score", score)
    })

    suspend fun getRankList(): JSONObject = request("GET", "/rank/list")

    suspend fun getUserStats(username: String): JSONObject =
        request("GET", "/rank/user?username=${java.net.URLEncoder.encode(username, "UTF-8")}")
}
