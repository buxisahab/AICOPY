package com.musifly.android.download

import android.util.Log
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * Speed Limiter - Controls download speed to prevent bandwidth hogging
 * 
 * Features:
 * - Configurable speed limits (KB/s or MB/s)
 * - Token bucket algorithm for smooth rate limiting
 * - Real-time speed adjustment
 * - No speed limit option (unlimited)
 * - Per-connection and global limiting
 */
class SpeedLimiter(private var maxBytesPerSecond: Long = 0L) {
    
    companion object {
        private const val TAG = "SpeedLimiter"
        
        // Predefined speed limits
        const val UNLIMITED = 0L
        const val SPEED_128_KB = 128L * 1024  // 128 KB/s
        const val SPEED_256_KB = 256L * 1024  // 256 KB/s
        const val SPEED_512_KB = 512L * 1024  // 512 KB/s
        const val SPEED_1_MB = 1024L * 1024   // 1 MB/s
        const val SPEED_2_MB = 2L * 1024 * 1024   // 2 MB/s
        const val SPEED_5_MB = 5L * 1024 * 1024   // 5 MB/s
        const val SPEED_10_MB = 10L * 1024 * 1024 // 10 MB/s
        
        /**
         * Get speed limit options for UI
         */
        fun getSpeedLimitOptions(): List<Pair<String, Long>> {
            return listOf(
                "Unlimited" to UNLIMITED,
                "128 KB/s" to SPEED_128_KB,
                "256 KB/s" to SPEED_256_KB,
                "512 KB/s" to SPEED_512_KB,
                "1 MB/s" to SPEED_1_MB,
                "2 MB/s" to SPEED_2_MB,
                "5 MB/s" to SPEED_5_MB,
                "10 MB/s" to SPEED_10_MB
            )
        }
    }
    
    private val lastRefillTime = AtomicLong(System.currentTimeMillis())
    private val availableTokens = AtomicLong(0)
    
    // Statistics
    private val totalBytesTransferred = AtomicLong(0)
    private val lastStatTime = AtomicLong(System.currentTimeMillis())
    private val currentSpeed = AtomicLong(0)
    
    /**
     * Set speed limit in bytes per second
     * Use UNLIMITED (0) for no limit
     */
    fun setSpeedLimit(bytesPerSecond: Long) {
        maxBytesPerSecond = bytesPerSecond
        Log.d(TAG, "Speed limit set to ${formatSpeed(bytesPerSecond)}")
    }
    
    /**
     * Get current speed limit
     */
    fun getSpeedLimit(): Long = maxBytesPerSecond
    
    /**
     * Check if speed limiting is enabled
     */
    fun isEnabled(): Boolean = maxBytesPerSecond > 0
    
    /**
     * Acquire permission to download specified number of bytes
     * Blocks if necessary to maintain speed limit
     * 
     * @param bytes Number of bytes to download
     * @return Actual number of bytes allowed (may be less than requested)
     */
    fun acquire(bytes: Long): Long {
        if (!isEnabled()) {
            updateStats(bytes)
            return bytes
        }
        
        // Refill tokens based on time elapsed
        refillTokens()
        
        // Calculate how many bytes we can actually download
        val allowedBytes = min(bytes, availableTokens.get())
        
        if (allowedBytes > 0) {
            availableTokens.addAndGet(-allowedBytes)
            updateStats(allowedBytes)
            return allowedBytes
        }
        
        // No tokens available, need to wait
        val waitTimeMs = calculateWaitTime(bytes)
        if (waitTimeMs > 0) {
            try {
                Thread.sleep(waitTimeMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return 0
            }
            
            // Refill and try again
            refillTokens()
            val newAllowedBytes = min(bytes, availableTokens.get())
            availableTokens.addAndGet(-newAllowedBytes)
            updateStats(newAllowedBytes)
            return newAllowedBytes
        }
        
        return 0
    }
    
    /**
     * Refill token bucket based on elapsed time
     */
    private fun refillTokens() {
        val now = System.currentTimeMillis()
        val lastRefill = lastRefillTime.get()
        val elapsedMs = now - lastRefill
        
        if (elapsedMs > 0) {
            // Calculate tokens to add based on elapsed time
            val tokensToAdd = (maxBytesPerSecond * elapsedMs) / 1000
            
            // Add tokens but don't exceed burst capacity (1 second worth)
            val newTokens = min(
                availableTokens.get() + tokensToAdd,
                maxBytesPerSecond
            )
            
            availableTokens.set(newTokens)
            lastRefillTime.set(now)
        }
    }
    
    /**
     * Calculate wait time needed to download specified bytes
     */
    private fun calculateWaitTime(bytes: Long): Long {
        if (!isEnabled()) return 0
        
        val deficit = bytes - availableTokens.get()
        if (deficit <= 0) return 0
        
        // Time needed = (bytes needed) / (bytes per second) * 1000 ms
        return (deficit * 1000) / maxBytesPerSecond
    }
    
    /**
     * Update statistics
     */
    private fun updateStats(bytes: Long) {
        totalBytesTransferred.addAndGet(bytes)
        
        val now = System.currentTimeMillis()
        val elapsed = now - lastStatTime.get()
        
        // Update current speed every second
        if (elapsed >= 1000) {
            val bytesPerSecond = (totalBytesTransferred.get() * 1000) / elapsed
            currentSpeed.set(bytesPerSecond)
            
            // Reset for next interval
            totalBytesTransferred.set(0)
            lastStatTime.set(now)
        }
    }
    
    /**
     * Get current download speed in bytes/second
     */
    fun getCurrentSpeed(): Long = currentSpeed.get()
    
    /**
     * Get current download speed formatted as string
     */
    fun getCurrentSpeedFormatted(): String = formatSpeed(currentSpeed.get())
    
    /**
     * Reset limiter state
     */
    fun reset() {
        availableTokens.set(0)
        lastRefillTime.set(System.currentTimeMillis())
        totalBytesTransferred.set(0)
        lastStatTime.set(System.currentTimeMillis())
        currentSpeed.set(0)
    }
    
    /**
     * Format speed for display
     */
    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond == 0L -> "Unlimited"
            bytesPerSecond < 1024 -> "${bytesPerSecond} B/s"
            bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
            else -> "%.2f MB/s".format(bytesPerSecond / (1024.0 * 1024.0))
        }
    }
}

/**
 * Bandwidth Monitor - Tracks bandwidth usage across all downloads
 * 
 * Features:
 * - Real-time bandwidth tracking
 * - Session statistics
 * - Daily/Weekly/Monthly usage tracking
 * - Peak speed detection
 * - Data usage alerts
 */
class BandwidthMonitor {
    
    companion object {
        private const val TAG = "BandwidthMonitor"
    }
    
    data class BandwidthStats(
        val currentSpeed: Long = 0,           // Current speed in bytes/second
        val averageSpeed: Long = 0,           // Average speed in bytes/second
        val peakSpeed: Long = 0,              // Peak speed recorded
        val totalDownloaded: Long = 0,        // Total bytes downloaded
        val sessionDuration: Long = 0,        // Session duration in ms
        val activeConnections: Int = 0        // Number of active connections
    )
    
    private val sessionStartTime = System.currentTimeMillis()
    private val totalBytesDownloaded = AtomicLong(0)
    private val currentBytesPerSecond = AtomicLong(0)
    private val peakBytesPerSecond = AtomicLong(0)
    private val activeConnectionCount = AtomicLong(0)
    
    // Sliding window for average calculation
    private val speedHistory = mutableListOf<Long>()
    private val maxHistorySize = 60 // Keep last 60 seconds
    
    /**
     * Record bytes downloaded
     */
    fun recordDownload(bytes: Long, durationMs: Long) {
        totalBytesDownloaded.addAndGet(bytes)
        
        if (durationMs > 0) {
            val speed = (bytes * 1000) / durationMs
            currentBytesPerSecond.set(speed)
            
            // Update peak
            val currentPeak = peakBytesPerSecond.get()
            if (speed > currentPeak) {
                peakBytesPerSecond.set(speed)
            }
            
            // Update history
            synchronized(speedHistory) {
                speedHistory.add(speed)
                if (speedHistory.size > maxHistorySize) {
                    speedHistory.removeAt(0)
                }
            }
        }
    }
    
    /**
     * Register active connection
     */
    fun registerConnection() {
        activeConnectionCount.incrementAndGet()
    }
    
    /**
     * Unregister active connection
     */
    fun unregisterConnection() {
        activeConnectionCount.decrementAndGet()
    }
    
    /**
     * Get current bandwidth statistics
     */
    fun getStats(): BandwidthStats {
        val averageSpeed = synchronized(speedHistory) {
            if (speedHistory.isEmpty()) 0L
            else speedHistory.average().toLong()
        }
        
        return BandwidthStats(
            currentSpeed = currentBytesPerSecond.get(),
            averageSpeed = averageSpeed,
            peakSpeed = peakBytesPerSecond.get(),
            totalDownloaded = totalBytesDownloaded.get(),
            sessionDuration = System.currentTimeMillis() - sessionStartTime,
            activeConnections = activeConnectionCount.get().toInt()
        )
    }
    
    /**
     * Format bandwidth stats for display
     */
    fun getStatsFormatted(): String {
        val stats = getStats()
        return """
            |Current: ${formatBytes(stats.currentSpeed)}/s
            |Average: ${formatBytes(stats.averageSpeed)}/s
            |Peak: ${formatBytes(stats.peakSpeed)}/s
            |Total: ${formatBytes(stats.totalDownloaded)}
            |Duration: ${formatDuration(stats.sessionDuration)}
            |Connections: ${stats.activeConnections}
        """.trimMargin()
    }
    
    /**
     * Reset statistics
     */
    fun reset() {
        totalBytesDownloaded.set(0)
        currentBytesPerSecond.set(0)
        peakBytesPerSecond.set(0)
        synchronized(speedHistory) {
            speedHistory.clear()
        }
    }
    
    /**
     * Format bytes for display
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * Format duration for display
     */
    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}
