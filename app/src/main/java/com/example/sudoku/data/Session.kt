package com.example.sudoku.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64

object Session {
    /** 进程内是否已自动检查过存档（仅首次进入主页时弹一次续玩提示） */
    @Volatile
    var autoResumeChecked = false

    private const val PREFS = "sudoku_session"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    val username: String?
        get() = prefs.getString("login_username", null)

    val phone: String?
        get() = prefs.getString("login_phone", null)

    fun saveLogin(username: String, phone: String) {
        prefs.edit().putString("login_username", username).putString("login_phone", phone).apply()
    }

    fun clearLogin() {
        prefs.edit().remove("login_username").remove("login_phone").apply()
    }

    fun getAvatar(username: String): ByteArray? {
        val s = prefs.getString("avatar_$username", null) ?: return null
        return try {
            Base64.decode(s, Base64.DEFAULT)
        } catch (_: Exception) {
            null
        }
    }

    fun setAvatar(username: String, bytes: ByteArray) {
        prefs.edit().putString("avatar_$username", Base64.encodeToString(bytes, Base64.NO_WRAP)).apply()
    }

    fun getServerAddress(): String = prefs.getString("server_address", null) ?: ""

    fun setServerAddress(address: String) {
        prefs.edit().putString("server_address", address.trim()).apply()
    }

    fun getThemeMode(): String = prefs.getString("theme_mode", "system") ?: "system"

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
    }
}
