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
    /** 存档/读档同步用更短的超时，离线时快速失败，避免一直转圈 */
    private const val SYNC_TIMEOUT_MS = 3000L

    private suspend fun request(method: String, path: String, body: JSONObject? = null, timeoutMs: Long = REQUEST_TIMEOUT_MS): JSONObject =
        withTimeout(timeoutMs) {
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
        seed: Int,
        cages: List<Cage>?,
    ): JSONObject = request("POST", "/save", buildSaveJson(
        username, boardSize, cells, notes, solution, given,
        seconds, errors, isKiller, killerDifficulty, seed, cages), SYNC_TIMEOUT_MS)

    /** 把本地已序列化好的存档直接上传（用于“本地较新”时覆盖云端） */
    suspend fun uploadRawSave(save: JSONObject): JSONObject =
        request("POST", "/save", save, SYNC_TIMEOUT_MS)

    suspend fun loadGame(username: String): JSONObject =
        request("GET", "/load?username=${java.net.URLEncoder.encode(username, "UTF-8")}", timeoutMs = SYNC_TIMEOUT_MS)

    suspend fun submitScore(
        username: String,
        won: Boolean,
        gameMode: String,
        boardSize: Int,
        score: Int,
        puzzleKey: String = "",
    ): JSONObject = request("POST", "/rank/submit", JSONObject().apply {
        put("username", username)
        put("won", won)
        put("gameMode", gameMode)
        put("boardSize", boardSize)
        put("score", score)
        put("puzzleKey", puzzleKey)
    })

    suspend fun getRankList(): JSONObject = request("GET", "/rank/list")

    suspend fun getContributions(username: String, days: Int = 365): JSONObject =
        request("GET", "/rank/contributions?username=${java.net.URLEncoder.encode(username, "UTF-8")}&days=$days")

    suspend fun getUserStats(username: String): JSONObject =
        request("GET", "/rank/user?username=${java.net.URLEncoder.encode(username, "UTF-8")}")

    suspend fun getAvatar(username: String): JSONObject =
        request("GET", "/avatar?username=${java.net.URLEncoder.encode(username, "UTF-8")}")

    suspend fun uploadAvatar(username: String, avatarBase64: String): JSONObject =
        request("PUT", "/avatar", JSONObject().apply {
            put("username", username)
            put("avatar", avatarBase64)
        })
}
