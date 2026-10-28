package com.musifly.android.download

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Download Statistics - Track and analyze download history and performance
 * 
 * Features:
 * - Session statistics
 * - Daily/Weekly/Monthly download tracking
 * - Success/Failure rate analysis
 * - Average speed tracking
 * - Storage usage tracking
 * - Popular quality preferences
 * - Time-based analytics
 */
class DownloadStatistics(context: Context) {
    
    companion object {
        private const val TAG = "DownloadStatistics"
        private const val PREFS_NAME = "download_statistics"
        private const val KEY_TOTAL_DOWNLOADS = "total_downloads"
        private const val KEY_SUCCESSFUL_DOWNLOADS = "successful_downloads"
        private const val KEY_FAILED_DOWNLOADS = "failed_downloads"
        private const val KEY_TOTAL_BYTES = "total_bytes"
        private const val KEY_TOTAL_TIME = "total_time"
        private const val KEY_DOWNLOAD_HISTORY = "download_history"
        private const val MAX_HISTORY_ENTRIES = 1000
    }
    
    /**
     * Download record for history
     */
    data class DownloadRecord(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val artist: String? = null,
        val quality: String,
        val fileSize: Long,
        val downloadSpeed: Long,              // bytes/second
        val downloadDuration: Long,           // milliseconds
        val timestamp: Long = System.currentTimeMillis(),
        val success: Boolean,
        val errorMessage: String? = null,
        val retryCount: Int = 0,
        val threadCount: Int = 3
    )
    
    /**
     * Statistics summary
     */
    data class StatsSummary(
        val totalDownloads: Int,
        val successfulDownloads: Int,
        val failedDownloads: Int,
        val totalDataDownloaded: Long,        // bytes
        val totalDownloadTime: Long,          // milliseconds
        val averageSpeed: Long,               // bytes/second
        val successRate: Float,               // percentage
        val mostPopularQuality: String?,
        val todayDownloads: Int,
        val weekDownloads: Int,
        val monthDownloads: Int
    )
    
    /**
     * Time period for analytics
     */
    enum class TimePeriod {
        TODAY,
        WEEK,
        MONTH,
        ALL_TIME
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val downloadHistory = ConcurrentHashMap<String, DownloadRecord>()
    
    init {
        loadHistory()
    }
    
    /**
     * Record a completed download
     */
    fun recordDownload(record: DownloadRecord) {
        try {
            // Add to history
            downloadHistory[record.id] = record
            
            // Update totals
            val editor = prefs.edit()
            editor.putInt(KEY_TOTAL_DOWNLOADS, prefs.getInt(KEY_TOTAL_DOWNLOADS, 0) + 1)
            
            if (record.success) {
                editor.putInt(KEY_SUCCESSFUL_DOWNLOADS, prefs.getInt(KEY_SUCCESSFUL_DOWNLOADS, 0) + 1)
                editor.putLong(KEY_TOTAL_BYTES, prefs.getLong(KEY_TOTAL_BYTES, 0) + record.fileSize)
                editor.putLong(KEY_TOTAL_TIME, prefs.getLong(KEY_TOTAL_TIME, 0) + record.downloadDuration)
            } else {
                editor.putInt(KEY_FAILED_DOWNLOADS, prefs.getInt(KEY_FAILED_DOWNLOADS, 0) + 1)
            }
            
            editor.apply()
            
            // Save history (throttle to prevent too many writes)
            saveHistory()
            
            Log.d(TAG, "Recorded download: ${record.title} - Success: ${record.success}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record download", e)
        }
    }
    
    /**
     * Get statistics summary
     */
    fun getSummary(period: TimePeriod = TimePeriod.ALL_TIME): StatsSummary {
        val filteredRecords = filterByPeriod(downloadHistory.values.toList(), period)
        
        val totalDownloads = if (period == TimePeriod.ALL_TIME) {
            prefs.getInt(KEY_TOTAL_DOWNLOADS, 0)
        } else {
            filteredRecords.size
        }
        
        val successfulDownloads = if (period == TimePeriod.ALL_TIME) {
            prefs.getInt(KEY_SUCCESSFUL_DOWNLOADS, 0)
        } else {
            filteredRecords.count { it.success }
        }
        
        val failedDownloads = if (period == TimePeriod.ALL_TIME) {
            prefs.getInt(KEY_FAILED_DOWNLOADS, 0)
        } else {
            filteredRecords.count { !it.success }
        }
        
        val totalBytes = if (period == TimePeriod.ALL_TIME) {
            prefs.getLong(KEY_TOTAL_BYTES, 0)
        } else {
            filteredRecords.filter { it.success }.sumOf { it.fileSize }
        }
        
        val totalTime = if (period == TimePeriod.ALL_TIME) {
            prefs.getLong(KEY_TOTAL_TIME, 0)
        } else {
            filteredRecords.filter { it.success }.sumOf { it.downloadDuration }
        }
        
        val averageSpeed = if (totalTime > 0) {
            (totalBytes * 1000) / totalTime
        } else 0
        
        val successRate = if (totalDownloads > 0) {
            (successfulDownloads.toFloat() / totalDownloads) * 100
        } else 0f
        
        val mostPopularQuality = filteredRecords
            .groupBy { it.quality }
            .maxByOrNull { it.value.size }
            ?.key
        
        return StatsSummary(
            totalDownloads = totalDownloads,
            successfulDownloads = successfulDownloads,
            failedDownloads = failedDownloads,
            totalDataDownloaded = totalBytes,
            totalDownloadTime = totalTime,
            averageSpeed = averageSpeed,
            successRate = successRate,
            mostPopularQuality = mostPopularQuality,
            todayDownloads = filterByPeriod(downloadHistory.values.toList(), TimePeriod.TODAY).size,
            weekDownloads = filterByPeriod(downloadHistory.values.toList(), TimePeriod.WEEK).size,
            monthDownloads = filterByPeriod(downloadHistory.values.toList(), TimePeriod.MONTH).size
        )
    }
    
    /**
     * Get download history
     */
    fun getHistory(period: TimePeriod = TimePeriod.ALL_TIME, limit: Int = 100): List<DownloadRecord> {
        return filterByPeriod(downloadHistory.values.toList(), period)
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    /**
     * Get quality distribution
     */
    fun getQualityDistribution(): Map<String, Int> {
        return downloadHistory.values
            .groupBy { it.quality }
            .mapValues { it.value.size }
            .toSortedMap()
    }
    
    /**
     * Get download speed trends
     */
    fun getSpeedTrends(period: TimePeriod = TimePeriod.WEEK): List<Pair<Long, Long>> {
        return filterByPeriod(downloadHistory.values.toList(), period)
            .filter { it.success }
            .sortedBy { it.timestamp }
            .map { it.timestamp to it.downloadSpeed }
    }
    
    /**
     * Get hourly download distribution
     */
    fun getHourlyDistribution(): Map<Int, Int> {
        val calendar = Calendar.getInstance()
        return downloadHistory.values
            .groupBy { record ->
                calendar.timeInMillis = record.timestamp
                calendar.get(Calendar.HOUR_OF_DAY)
            }
            .mapValues { it.value.size }
    }
    
    /**
     * Get daily download count for last N days
     */
    fun getDailyDownloads(days: Int = 30): Map<String, Int> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        
        return downloadHistory.values
            .filter { it.timestamp >= cutoffTime }
            .groupBy { dateFormat.format(Date(it.timestamp)) }
            .mapValues { it.value.size }
            .toSortedMap()
    }
    
    /**
     * Get failure reasons
     */
    fun getFailureReasons(): Map<String, Int> {
        return downloadHistory.values
            .filter { !it.success }
            .groupBy { it.errorMessage ?: "Unknown error" }
            .mapValues { it.value.size }
    }
    
    /**
     * Get retry statistics
     */
    fun getRetryStats(): Map<String, Any> {
        val retriedDownloads = downloadHistory.values.filter { it.retryCount > 0 }
        
        return mapOf(
            "total_retries" to retriedDownloads.sumOf { it.retryCount },
            "downloads_with_retries" to retriedDownloads.size,
            "average_retries" to if (retriedDownloads.isNotEmpty()) {
                retriedDownloads.sumOf { it.retryCount }.toFloat() / retriedDownloads.size
            } else 0f,
            "max_retries" to (retriedDownloads.maxOfOrNull { it.retryCount } ?: 0)
        )
    }
    
    /**
     * Clear all statistics
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        downloadHistory.clear()
        Log.d(TAG, "All statistics cleared")
    }
    
    /**
     * Export statistics as JSON
     */
    fun exportToJson(): String {
        val json = JSONObject()
        
        try {
            // Summary
            val summary = getSummary()
            json.put("total_downloads", summary.totalDownloads)
            json.put("successful_downloads", summary.successfulDownloads)
            json.put("failed_downloads", summary.failedDownloads)
            json.put("total_data_mb", summary.totalDataDownloaded / (1024 * 1024))
            json.put("average_speed_kbps", summary.averageSpeed / 1024)
            json.put("success_rate", summary.successRate)
            
            // History
            val historyArray = JSONArray()
            downloadHistory.values.forEach { record ->
                val recordJson = JSONObject()
                recordJson.put("title", record.title)
                recordJson.put("quality", record.quality)
                recordJson.put("size_mb", record.fileSize / (1024 * 1024))
                recordJson.put("speed_kbps", record.downloadSpeed / 1024)
                recordJson.put("timestamp", record.timestamp)
                recordJson.put("success", record.success)
                historyArray.put(recordJson)
            }
            json.put("history", historyArray)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export statistics", e)
        }
        
        return json.toString(2)
    }
    
    /**
     * Filter records by time period
     */
    private fun filterByPeriod(records: List<DownloadRecord>, period: TimePeriod): List<DownloadRecord> {
        if (period == TimePeriod.ALL_TIME) return records
        
        val calendar = Calendar.getInstance()
        val cutoffTime = when (period) {
            TimePeriod.TODAY -> {
                calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            TimePeriod.WEEK -> {
                System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            }
            TimePeriod.MONTH -> {
                System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            }
            else -> 0L
        }
        
        return records.filter { it.timestamp >= cutoffTime }
    }
    
    /**
     * Save history to preferences
     */
    private fun saveHistory() {
        try {
            val historyArray = JSONArray()
            
            // Keep only recent entries to prevent storage bloat
            val recentRecords = downloadHistory.values
                .sortedByDescending { it.timestamp }
                .take(MAX_HISTORY_ENTRIES)
            
            recentRecords.forEach { record ->
                val json = JSONObject().apply {
                    put("id", record.id)
                    put("title", record.title)
                    put("artist", record.artist)
                    put("quality", record.quality)
                    put("fileSize", record.fileSize)
                    put("downloadSpeed", record.downloadSpeed)
                    put("downloadDuration", record.downloadDuration)
                    put("timestamp", record.timestamp)
                    put("success", record.success)
                    put("errorMessage", record.errorMessage)
                    put("retryCount", record.retryCount)
                    put("threadCount", record.threadCount)
                }
                historyArray.put(json)
            }
            
            prefs.edit().putString(KEY_DOWNLOAD_HISTORY, historyArray.toString()).apply()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save history", e)
        }
    }
    
    /**
     * Load history from preferences
     */
    private fun loadHistory() {
        try {
            val historyJson = prefs.getString(KEY_DOWNLOAD_HISTORY, null) ?: return
            val historyArray = JSONArray(historyJson)
            
            for (i in 0 until historyArray.length()) {
                val json = historyArray.getJSONObject(i)
                val record = DownloadRecord(
                    id = json.getString("id"),
                    title = json.getString("title"),
                    artist = json.optString("artist").takeIf { it.isNotEmpty() },
                    quality = json.getString("quality"),
                    fileSize = json.getLong("fileSize"),
                    downloadSpeed = json.getLong("downloadSpeed"),
                    downloadDuration = json.getLong("downloadDuration"),
                    timestamp = json.getLong("timestamp"),
                    success = json.getBoolean("success"),
                    errorMessage = json.optString("errorMessage").takeIf { it.isNotEmpty() },
                    retryCount = json.optInt("retryCount", 0),
                    threadCount = json.optInt("threadCount", 3)
                )
                downloadHistory[record.id] = record
            }
            
            Log.d(TAG, "Loaded ${downloadHistory.size} history entries")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load history", e)
        }
    }
    
    /**
     * Format statistics for display
     */
    fun getFormattedSummary(): String {
        val summary = getSummary()
        
        return """
            |📊 Download Statistics
            |
            |Total Downloads: ${summary.totalDownloads}
            |✅ Successful: ${summary.successfulDownloads}
            |❌ Failed: ${summary.failedDownloads}
            |Success Rate: ${"%.1f".format(summary.successRate)}%
            |
            |📦 Total Data: ${formatBytes(summary.totalDataDownloaded)}
            |⚡ Average Speed: ${formatSpeed(summary.averageSpeed)}
            |
            |📅 Today: ${summary.todayDownloads} downloads
            |📅 This Week: ${summary.weekDownloads} downloads
            |📅 This Month: ${summary.monthDownloads} downloads
            |
            |🎬 Popular Quality: ${summary.mostPopularQuality ?: "N/A"}
        """.trimMargin()
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
     * Format speed for display
     */
    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond < 1024 -> "${bytesPerSecond} B/s"
            bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
            else -> "%.2f MB/s".format(bytesPerSecond / (1024.0 * 1024.0))
        }
    }
}
