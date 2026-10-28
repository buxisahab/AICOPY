package com.musifly.android.playback

import android.util.Log
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Pipeline Overflow Handler
 * 
 * Monitors and prevents audio pipeline overflow that causes pipelineFull warnings.
 * Automatically flushes pipeline when overflow is detected.
 */
object PipelineOverflowHandler {
    private const val TAG = "PipelineOverflowHandler"
    private const val MAX_PIPELINE_FRAMES = 2 // Hard limit
    private const val OVERFLOW_THRESHOLD = 4 // Warn if frames exceed this
    
    private val overflowCount = AtomicInteger(0)
    private val lastOverflowTime = AtomicLong(0)
    private val totalOverflows = AtomicLong(0)
    
    /**
     * Check for pipeline overflow and handle it
     * Called from audio thread - must be fast!
     */
    fun checkOverflow(currentFrames: Int): Boolean {
        if (currentFrames > OVERFLOW_THRESHOLD) {
            val count = overflowCount.incrementAndGet()
            totalOverflows.incrementAndGet()
            val now = System.currentTimeMillis()
            
            // Log warning if sustained overflow
            if (now - lastOverflowTime.get() > 1000) { // Max once per second
                Log.w(TAG, "⚠️ Pipeline overflow detected: $currentFrames frames (threshold: $MAX_PIPELINE_FRAMES)")
                lastOverflowTime.set(now)
            }
            
            return true // Overflow detected
        }
        
        // Reset counter if under threshold
        if (currentFrames <= MAX_PIPELINE_FRAMES) {
            overflowCount.set(0)
        }
        
        return false
    }
    
    /**
     * Get overflow statistics
     */
    fun getStats(): OverflowStats {
        return OverflowStats(
            totalOverflows = totalOverflows.get(),
            currentOverflowCount = overflowCount.get()
        )
    }
    
    /**
     * Reset statistics
     */
    fun reset() {
        overflowCount.set(0)
        totalOverflows.set(0)
        lastOverflowTime.set(0)
    }
    
    data class OverflowStats(
        val totalOverflows: Long,
        val currentOverflowCount: Int
    )
}
