package com.app.foodranker.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticUtils {

    // Tap ligero — para acciones secundarias (guardar, compartir)
    fun lightTap(context: Context) = vibrate(context, 10L, 80)

    // Tap medio — para like y acciones principales
    fun mediumTap(context: Context) = vibrate(context, 20L, 120)

    // Tap fuerte — para logros, level-up, primer like en propio plato
    fun heavyTap(context: Context) {
        vibrate(context, 30L, 255)
        context.mainExecutor.execute {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                vibrate(context, 20L, 200)
            }, 80)
        }
    }

    private fun vibrate(context: Context, durationMs: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(durationMs, amplitude)
            )
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                v?.vibrate(durationMs)
            }
        }
    }
}
