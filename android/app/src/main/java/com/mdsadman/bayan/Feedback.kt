package com.mdsadman.bayan

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Press feedback: a short synthesized click plus a light tick, so every tap in
 * the app answers back. Both are optional and never fatal — a device without a
 * vibrator, or with sound off, simply stays quiet. */
object BayanFeedback {
    private var vibrator: Vibrator? = null
    private var enabled = true

    fun init(context: Context) {
        enabled = true
        vibrator = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
        } catch (ignored: Throwable) {
            null
        }
    }

    /** One press: click tone (only when sound is on) + a light haptic tick. */
    fun press(soundOn: Boolean) {
        if (soundOn) ToneFeedback.play("tap", 0, true)
        tick()
    }

    fun tick() {
        if (!enabled) return
        val v = vibrator ?: return
        try {
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(12L)
            }
        } catch (ignored: Throwable) {
            enabled = false
        }
    }
}
