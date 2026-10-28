package com.musifly.android.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.musifly.android.R

/**
 * Advanced Notification Manager - Enhanced notifications with actions and progress
 * 
 * Features:
 * - Rich notifications with custom layouts
 * - Pause/Resume/Cancel actions
 * - Real-time progress updates
 * - Speed and ETA display
 * - Grouped notifications for multiple downloads
 * - Expandable notifications with detailed stats
 * - Sound and vibration customization
 */
class AdvancedNotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AdvancedNotificationManager"
        
        // Notification channels
        private const val CHANNEL_DOWNLOAD_PROGRESS = "download_progress"
        private const val CHANNEL_DOWNLOAD_COMPLETE = "download_complete"
        private const val CHANNEL_DOWNLOAD_FAILED = "download_failed"
        
        // Notification IDs
        private const val NOTIFICATION_ID_FOREGROUND = 1
        private const val NOTIFICATION_ID_SUMMARY = 2
        private const val NOTIFICATION_ID_BASE = 1000
        
        // Group
        private const val NOTIFICATION_GROUP = "downloads"
        
        // Actions
        const val ACTION_PAUSE = "com.musifly.android.PAUSE_DOWNLOAD"
        const val ACTION_RESUME = "com.musifly.android.RESUME_DOWNLOAD"
        const val ACTION_CANCEL = "com.musifly.android.CANCEL_DOWNLOAD"
        const val EXTRA_MISSION_ID = "mission_id"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val activeNotifications = mutableMapOf<Long, Int>()
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Progress channel
            val progressChannel = NotificationChannel(
                CHANNEL_DOWNLOAD_PROGRESS,
                "Download Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            // Complete channel
            val completeChannel = NotificationChannel(
                CHANNEL_DOWNLOAD_COMPLETE,
                "Download Complete",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when downloads complete"
                setShowBadge(true)
            }
            
            // Failed channel
            val failedChannel = NotificationChannel(
                CHANNEL_DOWNLOAD_FAILED,
                "Download Failed",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when downloads fail"
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(progressChannel, completeChannel, failedChannel)
            )
        }
    }
    
    /**
     * Create foreground service notification
     */
    fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(context, CHANNEL_DOWNLOAD_PROGRESS)
            .setContentTitle("Download Manager")
            .setContentText("Managing downloads in background")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    /**
     * Show progress notification for download
     */
    fun showProgressNotification(
        mission: DownloadMission,
        progress: Int,
        speed: Long,
        eta: Long
    ) {
        val notificationId = getNotificationId(mission)
        
        val notification = createProgressNotification(
            mission = mission,
            progress = progress,
            speed = speed,
            eta = eta
        )
        
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Create progress notification
     */
    private fun createProgressNotification(
        mission: DownloadMission,
        progress: Int,
        speed: Long,
        eta: Long
    ): Notification {
        val title = mission.title ?: mission.storage?.getName() ?: "Download"
        val speedText = formatSpeed(speed)
        val etaText = formatETA(eta)
        
        val builder = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD_PROGRESS)
            .setContentTitle(title)
            .setContentText("$progress% • $speedText • ETA: $etaText")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setGroup(NOTIFICATION_GROUP)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        
        // Add actions based on mission state
        if (mission.running) {
            builder.addAction(createPauseAction(mission))
            builder.addAction(createCancelAction(mission))
        } else if (mission.isPausedState()) {
            builder.addAction(createResumeAction(mission))
            builder.addAction(createCancelAction(mission))
        }
        
        // Expanded view with detailed stats
        val bigText = """
            |Quality: ${mission.quality ?: "N/A"}
            |Downloaded: ${formatBytes(mission.done)} / ${formatBytes(mission.length)}
            |Speed: $speedText
            |ETA: $etaText
            |Threads: ${mission.threadCount}
        """.trimMargin()
        
        builder.setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(bigText)
                .setBigContentTitle(title)
        )
        
        return builder.build()
    }
    
    /**
     * Show completion notification
     */
    fun showCompletionNotification(mission: DownloadMission) {
        val notificationId = getNotificationId(mission)
        activeNotifications.remove(mission.timestamp)
        
        val title = mission.title ?: mission.storage?.getName() ?: "Download"
        val fileSize = formatBytes(mission.length)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD_COMPLETE)
            .setContentTitle("✅ Download Complete")
            .setContentText("$title ($fileSize)")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$title\n\nQuality: ${mission.quality}\nSize: $fileSize")
            )
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Show failure notification
     */
    fun showFailureNotification(mission: DownloadMission, errorMessage: String) {
        val notificationId = getNotificationId(mission)
        activeNotifications.remove(mission.timestamp)
        
        val title = mission.title ?: mission.storage?.getName() ?: "Download"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD_FAILED)
            .setContentTitle("❌ Download Failed")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$title\n\nError: $errorMessage")
            )
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    /**
     * Show summary notification for grouped downloads
     */
    fun showSummaryNotification(activeCount: Int, completedCount: Int) {
        if (activeCount == 0 && completedCount == 0) {
            notificationManager.cancel(NOTIFICATION_ID_SUMMARY)
            return
        }
        
        val title = when {
            activeCount > 0 -> "Downloading $activeCount ${if (activeCount == 1) "file" else "files"}"
            else -> "Downloads"
        }
        
        val text = buildString {
            if (activeCount > 0) append("$activeCount active")
            if (activeCount > 0 && completedCount > 0) append(" • ")
            if (completedCount > 0) append("$completedCount completed")
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD_PROGRESS)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setGroup(NOTIFICATION_GROUP)
            .setGroupSummary(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_SUMMARY, notification)
    }
    
    /**
     * Cancel notification for mission
     */
    fun cancelNotification(mission: DownloadMission) {
        val notificationId = activeNotifications.remove(mission.timestamp) ?: return
        notificationManager.cancel(notificationId)
    }
    
    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        activeNotifications.values.forEach { notificationManager.cancel(it) }
        activeNotifications.clear()
        notificationManager.cancel(NOTIFICATION_ID_SUMMARY)
    }
    
    /**
     * Create pause action
     */
    private fun createPauseAction(mission: DownloadMission): NotificationCompat.Action {
        val intent = Intent(context, DownloadManagerService::class.java).apply {
            action = ACTION_PAUSE
            putExtra(EXTRA_MISSION_ID, mission.timestamp)
        }
        
        val pendingIntent = PendingIntent.getService(
            context,
            mission.timestamp.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_pause,
            "Pause",
            pendingIntent
        ).build()
    }
    
    /**
     * Create resume action
     */
    private fun createResumeAction(mission: DownloadMission): NotificationCompat.Action {
        val intent = Intent(context, DownloadManagerService::class.java).apply {
            action = ACTION_RESUME
            putExtra(EXTRA_MISSION_ID, mission.timestamp)
        }
        
        val pendingIntent = PendingIntent.getService(
            context,
            mission.timestamp.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_play,
            "Resume",
            pendingIntent
        ).build()
    }
    
    /**
     * Create cancel action
     */
    private fun createCancelAction(mission: DownloadMission): NotificationCompat.Action {
        val intent = Intent(context, DownloadManagerService::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_MISSION_ID, mission.timestamp)
        }
        
        val pendingIntent = PendingIntent.getService(
            context,
            mission.timestamp.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )
        
        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_delete,
            "Cancel",
            pendingIntent
        ).build()
    }
    
    /**
     * Get or create notification ID for mission
     */
    private fun getNotificationId(mission: DownloadMission): Int {
        return activeNotifications.getOrPut(mission.timestamp) {
            NOTIFICATION_ID_BASE + activeNotifications.size
        }
    }
    
    /**
     * Format speed for display
     */
    private fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond < 1024 -> "${bytesPerSecond} B/s"
            bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
            else -> "%.1f MB/s".format(bytesPerSecond / (1024.0 * 1024.0))
        }
    }
    
    /**
     * Format bytes for display
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes} B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * Format ETA for display
     */
    private fun formatETA(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
}
