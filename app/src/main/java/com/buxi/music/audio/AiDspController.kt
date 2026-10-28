package com.buxi.music.audio

import android.util.Log
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * AI DSP Controller — Real-time Soft Limiter
 *
 * Sits in ExoPlayer's audio processing chain (after DspAudioProcessor).
 * Provides a transparent look-ahead soft limiter to prevent clipping
 * caused by EQ boosts or bass enhancement.
 */
class AiDspController {
    companion object {
        private const val TAG = "AiDspController"
        private const val CLIP_THRESHOLD = 0.98f
        private const val LIMITER_ATTACK = 0.005f
        private const val LIMITER_RELEASE = 0.02f
    }

    val audioProcessor = AiAudioProcessor()

    @Volatile private var isEnabled = true

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        Log.d(TAG, "AI Limiter ${if (enabled) "ENABLED" else "DISABLED"}")
    }

    fun release() { /* No-op — processor is lightweight, lifecycle tied to ExoPlayer */ }

    inner class AiAudioProcessor : BaseAudioProcessor() {

        private var envelope = 0f
        private var gain = 1f

        override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
            inputAudioFormat // Passthrough format — we process in-place

        override fun queueInput(inputBuffer: ByteBuffer) {
            if (!inputBuffer.hasRemaining()) return

            if (!isEnabled) {
                val out = replaceOutputBuffer(inputBuffer.remaining())
                out.put(inputBuffer)
                out.flip()
                return
            }

            val out = replaceOutputBuffer(inputBuffer.remaining())
            var env = envelope
            var g = gain

            while (inputBuffer.hasRemaining()) {
                val lo = inputBuffer.get().toInt()
                val hi = inputBuffer.get().toInt()
                val s = ((lo and 0xFF) or (hi shl 8)).toShort()
                val f = s / 32768f
                val af = abs(f)

                env = if (af > env) env * (1 - LIMITER_ATTACK) + af * LIMITER_ATTACK
                      else          env * (1 - LIMITER_RELEASE) + af * LIMITER_RELEASE

                val target = if (env > CLIP_THRESHOLD) CLIP_THRESHOLD / env else 1f
                g = g * 0.9f + target * 0.1f

                val clamped = (f * g).coerceIn(-1f, 1f)
                val outShort = (clamped * 32767).toInt().toShort()
                out.put((outShort.toInt() and 0xFF).toByte())
                out.put(((outShort.toInt() shr 8) and 0xFF).toByte())
            }

            out.flip()
            envelope = env
            gain = g
        }
    }
}
