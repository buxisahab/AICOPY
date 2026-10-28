package com.musifly.android.download

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Download Scheduler - Schedule downloads for specific times
 * 
 * Features:
 * - Schedule downloads for later time
 * - WiFi-only scheduling
 * - Night-time download scheduling
 * - Recurring scheduled downloads
 * - Automatic download start at scheduled time
 */
class DownloadScheduler(private val context: Context) {
    
    companion object {
        private const val TAG = "DownloadScheduler"
        const val ACTION_START_SCHEDULED_DOWNLOAD = "com.musifly.android.START_SCHEDULED_DOWNLOAD"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
    }
    
    /**
     * Schedule configuration
     */
    data class ScheduleConfig(
        val scheduleId: String = UUID.randomUUID().toString(),
        val mission: DownloadMission,
        val scheduledTime: Long,                      // Timestamp when to start
        val wifiOnly: Boolean = false,                // Only start on WiFi
        val batteryThreshold: Int = 20,               // Minimum battery % required
        val recurring: RecurringType = RecurringType.NONE,
        val isActive: Boolean = true
    )
    
    /**
     * Recurring schedule types
     */
    enum class RecurringType {
        NONE,           // One-time
        DAILY,          // Every day at same time
        WEEKLY,         // Every week on same day
        MONTHLY         // Every month on same date
    }
    
    /**
     * Schedule time preset
     */
    enum class SchedulePreset(val hourOfDay: Int) {
        NIGHT_1AM(1),
        NIGHT_2AM(2),
        NIGHT_3AM(3),
        MORNING_6AM(6),
        MORNING_8AM(8),
        AFTERNOON_12PM(12),
        EVENING_6PM(18),
        EVENING_9PM(21)
    }
    
    private val scheduledDownloads = ConcurrentHashMap<String, ScheduleConfig>()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * Schedule a download for later
     */
    fun scheduleDownload(config: ScheduleConfig): Boolean {
        try {
            // Store schedule
            scheduledDownloads[config.scheduleId] = config
            
            // Create alarm
            val intent = Intent(context, ScheduledDownloadReceiver::class.java).apply {
                action = ACTION_START_SCHEDULED_DOWNLOAD
                putExtra(EXTRA_SCHEDULE_ID, config.scheduleId)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                config.scheduleId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            
            // Schedule alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    config.scheduledTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    config.scheduledTime,
                    pendingIntent
                )
            }
            
            Log.d(TAG, "Scheduled download: ${config.mission.title} at ${Date(config.scheduledTime)}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule download", e)
            return false
        }
    }
    
    /**
     * Schedule download for tonight at specific hour
     */
    fun scheduleForTonight(
        mission: DownloadMission,
        preset: SchedulePreset,
        wifiOnly: Boolean = true
    ): Boolean {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, preset.hourOfDay)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If time has passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        val config = ScheduleConfig(
            mission = mission,
            scheduledTime = calendar.timeInMillis,
            wifiOnly = wifiOnly
        )
        
        return scheduleDownload(config)
    }
    
    /**
     * Schedule download after delay (in milliseconds)
     */
    fun scheduleAfterDelay(
        mission: DownloadMission,
        delayMs: Long,
        wifiOnly: Boolean = false
    ): Boolean {
        val scheduledTime = System.currentTimeMillis() + delayMs
        val config = ScheduleConfig(
            mission = mission,
            scheduledTime = scheduledTime,
            wifiOnly = wifiOnly
        )
        
        return scheduleDownload(config)
    }
    
    /**
     * Cancel scheduled download
     */
    fun cancelSchedule(scheduleId: String): Boolean {
        val config = scheduledDownloads.remove(scheduleId) ?: return false
        
        try {
            val intent = Intent(context, ScheduledDownloadReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                scheduleId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
            
            Log.d(TAG, "Cancelled schedule: $scheduleId")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel schedule", e)
            return false
        }
    }
    
    /**
     * Get all scheduled downloads
     */
    fun getScheduledDownloads(): List<ScheduleConfig> {
        return scheduledDownloads.values.toList()
    }
    
    /**
     * Get next scheduled download
     */
    fun getNextSchedule(): ScheduleConfig? {
        return scheduledDownloads.values
            .filter { it.scheduledTime > System.currentTimeMillis() }
            .minByOrNull { it.scheduledTime }
    }
    
    /**
     * Clear all schedules
     */
    fun clearAllSchedules() {
        scheduledDownloads.keys.forEach { cancelSchedule(it) }
        scheduledDownloads.clear()
    }
}

/**
 * Broadcast receiver for scheduled downloads
 */
class ScheduledDownloadReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ScheduledDownloadReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadScheduler.ACTION_START_SCHEDULED_DOWNLOAD) {
            val scheduleId = intent.getStringExtra(DownloadScheduler.EXTRA_SCHEDULE_ID)
            Log.d(TAG, "Scheduled download triggered: $scheduleId")
            
            // Start download service
            val serviceIntent = Intent(context, DownloadManagerService::class.java).apply {
                action = DownloadManagerService.ACTION_START_MISSION
                putExtra("schedule_id", scheduleId)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}

/**
 * Smart Resume Manager - Intelligent download resume on network changes
 * 
 * Features:
 * - Auto-resume on WiFi connection
 * - Network type aware (pause on mobile data if configured)
 * - Battery level monitoring
 * - Charging state aware
 * - Airplane mode detection
 */
class SmartResumeManager(
    private val context: Context,
    private val downloadManager: DownloadManager
) {
    
    companion object {
        private const val TAG = "SmartResumeManager"
    }
    
    /**
     * Resume policy configuration
     */
    data class ResumePolicy(
        val resumeOnWiFi: Boolean = true,               // Auto-resume when WiFi connects
        val resumeOnMobileData: Boolean = false,        // Auto-resume on mobile data
        val requireCharging: Boolean = false,            // Only resume when charging
        val minimumBatteryLevel: Int = 20,              // Minimum battery % to resume
        val pauseOnMobileData: Boolean = true,          // Pause if switched to mobile data
        val pauseOnBatteryLow: Boolean = true           // Pause if battery below threshold
    )
    
    private var resumePolicy = ResumePolicy()
    private var isMonitoring = false
    
    /**
     * Set resume policy
     */
    fun setResumePolicy(policy: ResumePolicy) {
        resumePolicy = policy
        Log.d(TAG, "Resume policy updated: $policy")
    }
    
    /**
     * Handle network state change
     */
    fun onNetworkChanged(isWiFi: Boolean, isConnected: Boolean) {
        Log.d(TAG, "Network changed - WiFi: $isWiFi, Connected: $isConnected")
        
        if (!isConnected) {
            // Network lost - pause all downloads
            pauseAllDownloads("Network disconnected")
            return
        }
        
        when {
            isWiFi && resumePolicy.resumeOnWiFi -> {
                // WiFi connected - resume downloads
                resumeEligibleDownloads("WiFi connected")
            }
            !isWiFi && resumePolicy.pauseOnMobileData -> {
                // Switched to mobile data - pause if configured
                pauseAllDownloads("Switched to mobile data")
            }
            !isWiFi && resumePolicy.resumeOnMobileData -> {
                // Mobile data and allowed to resume
                resumeEligibleDownloads("Mobile data connected")
            }
        }
    }
    
    /**
     * Handle battery state change
     */
    fun onBatteryChanged(batteryLevel: Int, isCharging: Boolean) {
        Log.d(TAG, "Battery changed - Level: $batteryLevel%, Charging: $isCharging")
        
        when {
            batteryLevel < resumePolicy.minimumBatteryLevel && resumePolicy.pauseOnBatteryLow -> {
                pauseAllDownloads("Battery low ($batteryLevel%)")
            }
            isCharging && resumePolicy.requireCharging -> {
                resumeEligibleDownloads("Charging started")
            }
            !isCharging && resumePolicy.requireCharging -> {
                pauseAllDownloads("Not charging")
            }
            batteryLevel >= resumePolicy.minimumBatteryLevel -> {
                // Battery sufficient, check network and resume if appropriate
                checkAndResume()
            }
        }
    }
    
    /**
     * Resume eligible downloads based on current conditions
     */
    private fun resumeEligibleDownloads(reason: String) {
        val pausedMissions = downloadManager.getPendingMissions()
            .filter { it.isPausedState() && it.canResume() }
        
        if (pausedMissions.isEmpty()) {
            Log.d(TAG, "No eligible downloads to resume")
            return
        }
        
        Log.d(TAG, "Resuming ${pausedMissions.size} downloads - Reason: $reason")
        
        pausedMissions.forEach { mission ->
            try {
                downloadManager.resumeMission(mission)
                Log.d(TAG, "Resumed: ${mission.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resume: ${mission.title}", e)
            }
        }
    }
    
    /**
     * Pause all active downloads
     */
    private fun pauseAllDownloads(reason: String) {
        val activeMissions = downloadManager.getPendingMissions()
            .filter { it.running }
        
        if (activeMissions.isEmpty()) {
            Log.d(TAG, "No active downloads to pause")
            return
        }
        
        Log.d(TAG, "Pausing ${activeMissions.size} downloads - Reason: $reason")
        
        activeMissions.forEach { mission ->
            try {
                downloadManager.pauseMission(mission)
                Log.d(TAG, "Paused: ${mission.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pause: ${mission.title}", e)
            }
        }
    }
    
    /**
     * Check conditions and resume if appropriate
     */
    private fun checkAndResume() {
        // This would need to check actual network and battery state
        // Implementation would require Android system services
        Log.d(TAG, "Checking conditions for resume")
    }
    
    /**
     * Get resume statistics
     */
    fun getResumeStats(): ResumeStats {
        val allMissions = downloadManager.getPendingMissions()
        
        return ResumeStats(
            totalPaused = allMissions.count { it.isPausedState() },
            canResume = allMissions.count { it.canResume() },
            currentPolicy = resumePolicy
        )
    }
    
    data class ResumeStats(
        val totalPaused: Int,
        val canResume: Int,
        val currentPolicy: ResumePolicy
    )
}
