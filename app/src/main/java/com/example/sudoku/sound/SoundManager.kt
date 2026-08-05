package com.example.sudoku.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.sudoku.R

object SoundManager {
    private var pool: SoundPool? = null
    private var vibrator: Vibrator? = null
    private var clickId = 0
    private var successId = 0
    private var placementId = 0
    private var failedId = 0
    private val loaded = BooleanArray(4)
    private val pending = mutableSetOf<Int>()
    private var lastClickMs = 0L

    fun init(context: Context) {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val p = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        pool = p
        p.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                val idx = indexOf(sampleId)
                if (idx >= 0) loaded[idx] = true
                if (pending.remove(sampleId)) p.play(sampleId, 1f, 1f, 1, 0, 1f)
            }
        }
        clickId = p.load(context, R.raw.click, 1)
        successId = p.load(context, R.raw.success, 1)
        placementId = p.load(context, R.raw.placement, 1)
        failedId = p.load(context, R.raw.failed, 1)
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /** 300ms 防抖：禁止重复触发 */
    fun debounce(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastClickMs < 300) return false
        lastClickMs = now
        return true
    }

    private fun indexOf(id: Int): Int = when (id) {
        clickId -> 0
        successId -> 1
        placementId -> 2
        failedId -> 3
        else -> -1
    }

    private fun play(id: Int) {
        val p = pool ?: return
        val idx = indexOf(id)
        if (idx >= 0 && loaded[idx]) {
            p.play(id, 1f, 1f, 1, 0, 1f)
        } else if (id != 0) {
            pending.add(id)
        }
    }

    fun click() {
        play(clickId)
        vibrate(80)
    }

    fun placement() {
        play(placementId)
        vibrate(40)
    }

    fun success() {
        play(successId)
    }

    fun failed() {
        play(failedId)
    }

    fun tap() = vibrate(30)

    fun vibrate(ms: Long = 80) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(ms)
            }
        } catch (_: Exception) {
        }
    }
}
