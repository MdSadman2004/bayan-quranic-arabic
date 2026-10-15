package com.mdsadman.bayan

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.min
import kotlin.math.pow

/** Tiny synthesized feedback tones (correct / wrong / complete) with a musical
 * pitch ladder for the Noor flow combo (feature 1): each streak step raises the
 * whole figure by the loaded semitone interval. Uses AudioTrack so no asset or
 * soundpool file is needed and it works fully offline. */
object ToneFeedback {
    private const val RATE = 44100
    private const val GAIN = 0.10f
    private const val SECONDS = 0.11f

    private var track: AudioTrack? = null
    private var disabled = false

    fun init() {
        if (track != null || disabled) return
        // AudioTrack.Builder.build() throws "Invalid audio buffer size" on some
        // devices (Samsung/Android 16) when the buffer is smaller than the
        // device's minimum or not a whole byte count for the frame size, and the
        // tones are a nicety — never let audio bring the app down.
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            // Bytes for the longest figure (0.54 s) with headroom, never below
            // the device minimum. MODE_STATIC needs the whole clip up front.
            val needed = (RATE * 0.6f).toInt() * 2
            val size = (if (minBuf > 0) maxOf(minBuf, needed) else needed) / 2 * 2
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } catch (ignored: Throwable) {
            disabled = true
            track = null
        }
    }

    fun release() {
        track?.release()
        track = null
    }

    /** kind: "ok" | "bad" | "complete"; semitones lifts the pitch along the flow ladder. */
    fun play(kind: String, semitones: Int = 0, on: Boolean = true) {
        if (!on) return
        init()
        val shift = 2f.pow(semitones / 12f)
        val tones: List<Pair<Double, Double>> = when (kind) {
            "tap" -> listOf(1180.0 to 0.0)
            "ok" -> listOf(659.0 to 0.0, 880.0 to 0.075)
            "bad" -> listOf(196.0 to 0.0, 147.0 to 0.09)
            else -> listOf(523.0 to 0.0, 659.0 to 0.09, 784.0 to 0.18, 1046.0 to 0.27)
        }
        val longest = tones.maxOf { it.second }
        val tail = if (kind == "tap") 0.03f else SECONDS
        val spanSeconds = (tail * 4).coerceAtLeast((longest.toFloat() + tail))
        val total = (spanSeconds * RATE).toInt() + RATE / 10
        val frames = total
        val buffer = ShortArray(frames)
        for ((index, tone) in tones.withIndex()) {
            val (freq, at) = tone
            val start = (at * RATE).toInt()
            val length = (SECONDS * RATE).toInt()
            val isSquare = kind == "bad"
            for (i in 0 until length) {
                val frame = start + i
                if (frame >= frames) break
                val t = i.toFloat() / RATE
                val envelope = min(1f, (length - i) / (0.35f * RATE)).coerceAtLeast(0f)
                val phase = 2.0 * Math.PI * (freq * shift) * t
                val wave = if (isSquare) (if (Math.sin(phase) >= 0) 0.8 else -0.8) else Math.sin(phase)
                val level = if (kind == "tap") GAIN * 0.55f else GAIN
                val sum = buffer[frame] / 32768f + (wave.toFloat() * envelope * level)
                buffer[frame] = (sum * 32767f).toInt().toShort()
            }
        }
        val t = track ?: return
        try {
            t.stop()
            t.reloadStaticData()
            t.write(buffer, 0, frames)
            t.play()
        } catch (ignored: IllegalStateException) {
        } catch (ignored: IllegalArgumentException) {
        }
    }

    /** Convenience: pitch from the lesson's flow state. */
    fun playFor(kind: String, flow: FlowState, on: Boolean) = play(kind, flow.semitones, on)
}
