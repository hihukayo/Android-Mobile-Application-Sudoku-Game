package com.example.sudoku.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

/**
 * 本地 SQLite 存档（离线也能存/读）。
 * 结构尽量与后端 saves 表一致，另加 synced 标记表示是否已同步到云端。
 */
object LocalSaveStore {
    private const val DB_NAME = "sudoku_local.db"
    private const val DB_VERSION = 1
    private lateinit var helper: Helper

    fun init(context: Context) {
        if (!::helper.isInitialized) {
            helper = Helper(context.applicationContext)
        }
    }

    private class Helper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS local_saves (" +
                        "username TEXT PRIMARY KEY," +
                        "save_json TEXT NOT NULL," +
                        "saved_at TEXT," +
                        "synced INTEGER DEFAULT 0)"
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    data class LocalSave(val json: JSONObject, val savedAt: String, val synced: Boolean)

    /** 保存/覆盖本地存档；synced 表示这份存档是否已同步到云端 */
    fun save(username: String, save: JSONObject, synced: Boolean) {
        if (!::helper.isInitialized) return
        val values = ContentValues().apply {
            put("username", username)
            put("save_json", save.toString())
            put("saved_at", save.optString("savedAt"))
            put("synced", if (synced) 1 else 0)
        }
        helper.writableDatabase.insertWithOnConflict(
            "local_saves", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /** 云端上传成功后调用：标记为已同步 */
    fun markSynced(username: String) {
        if (!::helper.isInitialized) return
        val values = ContentValues().apply { put("synced", 1) }
        helper.writableDatabase.update("local_saves", values, "username = ?", arrayOf(username))
    }

    fun load(username: String): LocalSave? {
        if (!::helper.isInitialized) return null
        val cursor = helper.readableDatabase.rawQuery(
            "SELECT save_json, saved_at, synced FROM local_saves WHERE username = ?",
            arrayOf(username))
        cursor.use {
            if (it.moveToFirst()) {
                return try {
                    LocalSave(
                        JSONObject(it.getString(0)),
                        it.getString(1),
                        it.getInt(2) == 1
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }
}
