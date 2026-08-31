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

    /** 保存登录信息；password 非空时同时缓存（只保留最新一条），用于同设备离线快速登入 */
    fun saveLogin(username: String, phone: String, password: String? = null) {
        val editor = prefs.edit().putString("login_username", username).putString("login_phone", phone)
        if (password != null) {
            // 只保留最新一条成功登录的缓存记录
            editor.putString("login_password", password)
            editor.putString("last_login_username", username)
            editor.putString("last_login_phone", phone)
        }
        editor.apply()
    }

    /** 上次成功登录缓存的密码（退出登录不清除，便于同设备直登） */
    fun getCachedPassword(): String? = prefs.getString("login_password", null)

    /** 上次成功登录缓存的用户名（只保留最新一条） */
    fun getLastLoginUsername(): String? = prefs.getString("last_login_username", null)

    /** 上次成功登录缓存的手机号（只保留最新一条） */
    fun getLastLoginPhone(): String? = prefs.getString("last_login_phone", null)

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
