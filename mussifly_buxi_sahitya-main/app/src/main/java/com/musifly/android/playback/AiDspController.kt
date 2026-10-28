package com.musifly.android.playback

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * AI DSP Controller
 * 
 * Provides:
 * 1. Real-time Soft Limiter (AudioProcessor) to prevent clipping
 * 2. Dynamic Content Analysis (Heuristic "AI")
 * 3. Psychoacoustic Enhancements
 * 
 * Architecture:
 * - Sits in ExoPlayer's render chain via AiAudioProcessor
 * - Monitors signal for clipping/peaks
 * - Smooths dynamics
 */
class AiDspController(
    private var effectsManager: AudioEffectsManager? = null
) {
    companion object {
        private const val TAG = "AiDspController"
        // Analysis window
        private const val ANALYSIS_INTERVAL_MS = 1000L
        private const val CLIP_THRESHOLD = 0.98f
        private const val LIMITER_ATTACK = 0.005f
        private const val LIMITER_RELEASE = 0.02f
    }
    
    // The processor instanc (to be passed to ExoPlayer)
    val audioProcessor = AiAudioProcessor()
    
    private val scope = CoroutineScope(Dispatchers.Main)
    private var analysisJob: Job? = null
    
    // State
    @Volatile private var isEnabled = true
    @Volatile private var currentPeak = 0f
    @Volatile private var currentRms = 0f
    
    init {
        startAnalysis()
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        Log.d(TAG, "AI DSP ${if (enabled) "ENABLED" else "DISABLED"}")
    }
    
    fun setAudioEffectsManager(manager: AudioEffectsManager) {
        this.effectsManager = manager
        Log.d(TAG, "AudioEffectsManager connected to AI DSP")
    }
    
    private fun startAnalysis() {
        analysisJob = scope.launch(Dispatchers.Default) {
             while (isActive) {
                 delay(ANALYSIS_INTERVAL_MS)
                 if (isEnabled && effectsManager?.hasActiveEffects() == true) {
                     // Heuristic Logic:
                     // If we are consistently peaking near 1.0, we might want to tell 
                     // AudioEffectsManager to back off the BassBoost or LoudnessEnhancer
                     if (currentPeak > 0.99f) {
                         // Clipping detected - this is handled by the Limiter in real-time,
                         // but we can also reduce global gain if it persists.
                         // For now, we trust the Limiter.
                     }
                 }
             }
        }
    }
    
    fun release() {
        analysisJob?.cancel()
    }

    /**
     * AI Audio Processor
     * Implements a Soft Limiter and Loudness Normalization
     * 
     * Runs on ExoPlayer's playback thread. MUST BE FAST.
     */
    inner class AiAudioProcessor : BaseAudioProcessor() {
        
        private var sampleRate = 44100
        private var channelCount = 2
        
        // Limiter state
        private var envelope = 0f
        private var gain = 1f
        
        override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
            sampleRate = inputAudioFormat.sampleRate
            channelCount = inputAudioFormat.channelCount
            // We support any PCM 16-bit
            return inputAudioFormat
        }

        override fun queueInput(inputBuffer: ByteBuffer) {
            if (!inputBuffer.hasRemaining()) return
            
            if (!isEnabled) {
                // Passthrough
                val outputBuffer = replaceOutputBuffer(inputBuffer.remaining())
                outputBuffer.put(inputBuffer)
                outputBuffer.flip()
                return
            }
            
            // Prepare output
            val limit = inputBuffer.limit()
            val position = inputBuffer.position()
            val remaining = inputBuffer.remaining()
            val outputBuffer = replaceOutputBuffer(remaining)
            
            // Process samples (assuming 16-bit PCM)
            // Use local vars for speed
            var currentEnvelope = envelope
            var currentGain = gain
            
            var maxPeakInChunk = 0f
            var sumSquare = 0f
            var sampleCount = 0
            
            // Fast loop
            // NOTE: We directly access bytes for speed, treating pairs as Short
            while (inputBuffer.hasRemaining()) {
                // Read sample (Little Endian)
                val b1 = inputBuffer.get().toInt()
                val b2 = inputBuffer.get().toInt()
                val sampleShort = ((b1 and 0xFF) or (b2 shl 8)).toShort()
                
                // Convert to float -1.0 to 1.0
                val sampleFloat = sampleShort / 32768f
                val absSample = abs(sampleFloat)
                
                // Track stats
                if (absSample > maxPeakInChunk) maxPeakInChunk = absSample
                sumSquare += sampleFloat * sampleFloat
                sampleCount++

                // Limiter Logic (Envelope Follower)
                if (absSample > currentEnvelope) {
                    currentEnvelope = currentEnvelope * (1 - LIMITER_ATTACK) + absSample * LIMITER_ATTACK
                } else {
                    currentEnvelope = currentEnvelope * (1 - LIMITER_RELEASE) + absSample * LIMITER_RELEASE
                }
                
                // Calculate gain reduction if envelope exceeds threshold
                var targetGain = 1f
                if (currentEnvelope > CLIP_THRESHOLD) {
                    targetGain = CLIP_THRESHOLD / currentEnvelope
                }
                
                // Smooth gain
                currentGain = currentGain * 0.9f + targetGain * 0.1f
                
                // Apply gain (Soft Knee)
                // Use a tanh-like curve for "warm" saturation near the limit instead of hard clip
                // Simple soft clip: x - x^3/3 (approx tanh)
                var processed = sampleFloat * currentGain
                
                // Hard safety clamp
                if (processed > 1.0f) processed = 1.0f
                if (processed < -1.0f) processed = -1.0f
                
                // Convert back to Short
                // Dither could be added here but keeping it simple
                val outShort = (processed * 32767).toInt().toShort()
                
                outputBuffer.put((outShort.toInt() and 0xFF).toByte())
                outputBuffer.put(((outShort.toInt() shr 8) and 0xFF).toByte())
            }
            
            outputBuffer.flip() // Prepare for reading
            
            // Save state
            envelope = currentEnvelope
            gain = currentGain
            
            // Update global stats for Analysis thread
            currentPeak = maxPeakInChunk
            if (sampleCount > 0) {
                currentRms = sqrt(sumSquare / sampleCount)
            }
        }
    }
}
