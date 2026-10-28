package com.musifly.android.download
import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Binder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.musifly.android.R
import com.musifly.android.constants.CustomDownloadDirectoryKey
import com.musifly.android.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Download Manager Service - Advanced foreground service for downloads
 * Enhanced with NewPipe-like features:
 * - Priority queue management
 * - Network type restrictions (WiFi/Mobile)
 * - Concurrent download limits
 * - Retry mechanism with exponential backoff
 * - Segment/chunk progress tracking
 * - Detailed error reporting
 * - Speed monitoring and statistics
 */
class DownloadManagerService : Service() {
    
    companion object {
        private const val TAG = "DownloadManagerService"
        
        const val MESSAGE_RUNNING = 0
        const val MESSAGE_PAUSED = 1
        const val MESSAGE_FINISHED = 2
        const val MESSAGE_ERROR = 3
        const val MESSAGE_PROGRESS = 4
        
        private const val FOREGROUND_NOTIFICATION_ID = 1000
        private const val CHANNEL_ID = "download_channel"
        private const val CHANNEL_NAME = "Downloads"
        
        // Download priorities
        const val PRIORITY_LOW = 0
        const val PRIORITY_NORMAL = 1
        const val PRIORITY_HIGH = 2
        const val PRIORITY_URGENT = 3
        
        // Default download paths
        private const val DEFAULT_VIDEO_PATH = "/storage/emulated/0/Music/MusiFly/Videos"
        private const val DEFAULT_AUDIO_PATH = "/storage/emulated/0/Music/MusiFly/Music"
        
        const val ACTION_START_MISSION = "com.musifly.android.START_MISSION"
        const val ACTION_PAUSE = "com.musifly.android.PAUSE_DOWNLOAD"
        const val ACTION_RESUME = "com.musifly.android.RESUME_DOWNLOAD"
        const val ACTION_CANCEL = "com.musifly.android.CANCEL_DOWNLOAD"
        const val ACTION_PAUSE_ALL = "com.musifly.android.PAUSE_ALL_DOWNLOADS"
        const val ACTION_RESUME_ALL = "com.musifly.android.RESUME_ALL_DOWNLOADS"
        const val ACTION_CANCEL_ALL = "com.musifly.android.CANCEL_ALL_DOWNLOADS"
        const val ACTION_RETRY = "com.musifly.android.RETRY_DOWNLOAD"
        const val ACTION_SAVE_ALL = "com.musifly.android.SAVE_ALL_DOWNLOADS"
        const val ACTION_SET_PRIORITY = "com.musifly.android.SET_PRIORITY"
        const val ACTION_ENABLE_WIFI_ONLY = "com.musifly.android.ENABLE_WIFI_ONLY"
        const val EXTRA_URLS = "urls"
        const val EXTRA_TITLE = "title"
        const val EXTRA_ARTIST = "artist"
        const val EXTRA_THUMBNAIL = "thumbnail"
        const val EXTRA_KIND = "kind"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_MISSION_TIMESTAMP = "mission_timestamp"
        const val EXTRA_PRIORITY = "priority"
        const val EXTRA_WIFI_ONLY = "wifi_only"
        const val EXTRA_NETWORK_TYPE = "network_type"
        const val EXTRA_SOURCE_VIDEO_URL = "source_video_url" // Original YouTube URL for re-extraction
        const val EXTRA_SONG_ID = "song_id" // Song ID in database
        
        // Network type constants
        const val NETWORK_WIFI = 1
        const val NETWORK_MOBILE = 2
        const val NETWORK_ANY = 3
        
        // Concurrent download settings
        private const val MAX_CONCURRENT_DOWNLOADS = 3
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_BASE_MS = 2000L // Base delay for exponential backoff
        
        /**
         * Start a new download mission
         */
        fun startMission(
            context: Context,
            urls: Array<String>,
            title: String,
            kind: Char,
            quality: String,
            artist: String? = null,
            thumbnailUrl: String? = null,
            sourceVideoUrl: String? = null, // Original YouTube URL for URL refresh
            songId: String? = null // Database song ID
        ) {
            val intent = Intent(context, DownloadManagerService::class.java).apply {
                action = ACTION_START_MISSION
                putExtra(EXTRA_URLS, urls)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ARTIST, artist)
                putExtra(EXTRA_THUMBNAIL, thumbnailUrl)
                putExtra(EXTRA_KIND, kind.toString()) // Send as String to avoid ClassCastException
                putExtra(EXTRA_QUALITY, quality)
                putExtra(EXTRA_SOURCE_VIDEO_URL, sourceVideoUrl)
                putExtra(EXTRA_SONG_ID, songId)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * Get custom directory from preferences or return null
         */
        fun getCustomDirectory(context: Context): String? {
            return try {
                runBlocking {
                    context.dataStore.data.first()[CustomDownloadDirectoryKey]
                }?.takeIf { it.isNotEmpty() }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read custom directory: ${e.message}")
                null
            }
        }
        
        /**
         * Parse custom directory path (handles content:// and file:// URIs)
         */
        fun parseDirectoryPath(path: String): String {
            return if (path.startsWith("content://") || path.startsWith("file://")) {
                try {
                    val uri = android.net.Uri.parse(path)
                    if (uri.scheme == "file") {
                        uri.path ?: path
                    } else {
                        // For content:// URIs, extract path from /tree/primary:path format
                        val uriPath = uri.path
                        if (uriPath != null && uriPath.startsWith("/tree/")) {
                            val pathPart = uriPath.substringAfter("/tree/")
                            if (pathPart.contains(":")) {
                                val (storage, rest) = pathPart.split(":", limit = 2)
                                if (storage == "primary") {
                                    "/storage/emulated/0/$rest"
                                } else {
                                    "/storage/$storage/$rest"
                                }
                            } else {
                                path
                            }
                        } else {
                            path
                        }
                    }
                } catch (e: Exception) {
                    path
                }
            } else {
                path
            }
        }
        
        /**
         * Get default download directory path
         */
        fun getDefaultDirectory(kind: Char): String {
            return when (kind) {
                'v' -> DEFAULT_VIDEO_PATH
                'a' -> DEFAULT_AUDIO_PATH
                else -> DEFAULT_VIDEO_PATH
            }
        }
    }
    
    private lateinit var binder: DownloadManagerBinder
    private lateinit var downloadManager: DownloadManager
    private lateinit var handler: Handler
    private lateinit var notificationManager: NotificationManager
    private lateinit var connectivityManager: ConnectivityManager
    
    private val cachedRepo by lazy {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            this,
            com.musifly.android.di.CachedDownloadsRepositoryEntryPoint::class.java
        ).cachedDownloadsRepository()
    }

    private var foreground = false
    private var downloadNotificationEnable = true
    private val activeNotifications = mutableMapOf<Long, Int>() // mission.timestamp -> notificationId
    private var notificationIdCounter = 2000 // Start from 2000 to avoid conflicts
    private val progressUpdateHandler = Handler(Looper.getMainLooper())
    private var isProgressUpdateRunning = false
    
    // Advanced features
    private var wifiOnlyMode = false
    private var currentNetworkType = NETWORK_ANY
    private val missionPriorities = mutableMapOf<Long, Int>() // timestamp -> priority
    private val missionRetryCount = mutableMapOf<Long, Int>() // timestamp -> retry count
    private val speedMonitor = BandwidthMonitor()
    private val downloadStatistics = mutableMapOf<Long, DownloadStats>()
    
    data class DownloadStats(
        var startTime: Long = 0,
        var bytesDownloaded: Long = 0,
        var averageSpeed: Long = 0,
        var peakSpeed: Long = 0,
        var segments: List<SegmentInfo> = emptyList(),
        var retries: Int = 0,
        var lastError: String? = null
    )
    
    data class SegmentInfo(
        val index: Int,
        val startByte: Long,
        val endByte: Long,
        val downloaded: Long,
        val speed: Long,
        val status: String // "downloading", "completed", "paused", "error"
    )
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            // Update progress for all running missions
            downloadManager.getPendingMissions().forEach { mission ->
                if (mission.running && !mission.isFinished()) {
                    // Live progress update - updates notification with current progress
                    notifyProgress(mission)
                } else if (mission.isFinished()) {
                    // Ensure finished notification is sent and progress bar is removed
                    val notificationId = activeNotifications[mission.timestamp]
                    if (notificationId != null) {
                        notifyFinished(mission)
                        // Remove from active notifications to prevent further updates
                        activeNotifications.remove(mission.timestamp)
                    }
                }
            }
            // Schedule next update only if there are running missions
            val hasRunningMissions = downloadManager.getPendingMissions().any { it.running && !it.isFinished() }
            if (hasRunningMissions) {
                progressUpdateHandler.postDelayed(this, 1000) // Throttle to 1 second to prevent UI freeze
            }
        }
    }
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            handleConnectivityState()
        }
        
        override fun onLost(network: Network) {
            handleConnectivityState()
        }
        
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            handleConnectivityState()
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        
        binder = DownloadManagerBinder()
        handler = Handler(Looper.getMainLooper(), ::handleMessage)
        
        // Initialize managers
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Create notification channel
        createNotificationChannel()
        
        // Initialize download manager
        downloadManager = DownloadManager(
            context = this,
            handler = handler,
            mainStorageVideo = null, // TODO: Load from preferences
            mainStorageAudio = null  // TODO: Load from preferences
        )
        
        // Register network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        handleConnectivityState()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        // CRITICAL: Call startForeground() IMMEDIATELY to prevent Android from killing the service
        // This must be done before any background processing on Android 8.0+ (especially Android 15+)
        if (!foreground) {
            val notification = createForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification)
            }
            foreground = true
            Log.d(TAG, "Service started in foreground mode")
        }
        
        when (intent?.action) {
            ACTION_START_MISSION -> {
                handler.post {
                    startMission(intent)
                }
            }
            ACTION_PAUSE -> {
                val timestamp = intent.getLongExtra(EXTRA_MISSION_TIMESTAMP, -1)
                if (timestamp != -1L) {
                    handler.post {
                        pauseMission(timestamp)
                    }
                }
            }
            ACTION_RESUME -> {
                val timestamp = intent.getLongExtra(EXTRA_MISSION_TIMESTAMP, -1)
                if (timestamp != -1L) {
                    handler.post {
                        resumeMission(timestamp)
                    }
                }
            }
            ACTION_CANCEL -> {
                val timestamp = intent.getLongExtra(EXTRA_MISSION_TIMESTAMP, -1)
                if (timestamp != -1L) {
                    handler.post {
                        cancelMission(timestamp)
                    }
                }
            }
            ACTION_PAUSE_ALL -> {
                handler.post {
                    pauseAllDownloads()
                }
            }
            ACTION_RESUME_ALL -> {
                handler.post {
                    resumeAllDownloads()
                }
            }
            ACTION_CANCEL_ALL -> {
                handler.post {
                    cancelAllDownloads()
                }
            }
            ACTION_RETRY -> {
                val timestamp = intent.getLongExtra(EXTRA_MISSION_TIMESTAMP, -1)
                if (timestamp != -1L) {
                    handler.post {
                        retryMission(timestamp)
                    }
                }
            }
            ACTION_SET_PRIORITY -> {
                val timestamp = intent.getLongExtra(EXTRA_MISSION_TIMESTAMP, -1)
                val priority = intent.getIntExtra(EXTRA_PRIORITY, PRIORITY_NORMAL)
                if (timestamp != -1L) {
                    handler.post {
                        setPriority(timestamp, priority)
                    }
                }
            }
            ACTION_ENABLE_WIFI_ONLY -> {
                val enabled = intent.getBooleanExtra(EXTRA_WIFI_ONLY, false)
                handler.post {
                    setWifiOnlyMode(enabled)
                }
            }
            ACTION_SAVE_ALL -> {
                handler.post {
                    downloadManager.saveAllMissions()
                    Log.d(TAG, "Saved all missions on request")
                }
            }
        }
        
        return START_STICKY
    }
    
    private fun pauseMission(timestamp: Long) {
        val mission = downloadManager.getPendingMissions().find { it.timestamp == timestamp }
        if (mission != null && mission.running) {
            downloadManager.pauseMission(mission)
            Log.d(TAG, "Paused mission: ${mission.storage?.getName()}")
        }
    }
    
    private fun resumeMission(timestamp: Long) {
        val mission = downloadManager.getPendingMissions().find { it.timestamp == timestamp }
        if (mission != null && !mission.running && !mission.isFinished()) {
            downloadManager.resumeMission(mission)
            Log.d(TAG, "Resumed mission: ${mission.storage?.getName()}")
        }
    }
    
    private fun cancelMission(timestamp: Long) {
        val mission = downloadManager.getPendingMissions().find { it.timestamp == timestamp }
        if (mission != null) {
            downloadManager.deleteMission(mission, true)
            // Remove notification
            activeNotifications.remove(timestamp)?.let { notificationId ->
                notificationManager.cancel(notificationId)
            }

            // Remove from Supabase if songId is present
            mission.songId?.let { songId ->
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    cachedRepo.removeCachedDownload(songId)
                }
            }

            // Clean up stats
            downloadStatistics.remove(timestamp)
            missionPriorities.remove(timestamp)
            missionRetryCount.remove(timestamp)
            Log.d(TAG, "Cancelled mission: ${mission.storage?.getName()}")
        }
    }
    
    private fun pauseAllDownloads() {
        downloadManager.getPendingMissions()
            .filter { it.running }
            .forEach { mission ->
                downloadManager.pauseMission(mission)
            }
        Log.d(TAG, "Paused all downloads")
    }
    
    private fun resumeAllDownloads() {
        // Check network restrictions
        if (wifiOnlyMode && !isWifiConnected()) {
            showWifiRequiredNotification()
            return
        }
        
        // Resume based on priority
        val missions = downloadManager.getPendingMissions()
            .filter { !it.running && !it.isFinished() }
            .sortedByDescending { missionPriorities[it.timestamp] ?: PRIORITY_NORMAL }
        
        var resumed = 0
        for (mission in missions) {
            if (getRunningDownloadsCount() >= MAX_CONCURRENT_DOWNLOADS) break
            downloadManager.resumeMission(mission)
            resumed++
        }
        
        Log.d(TAG, "Resumed $resumed downloads")
    }
    
    private fun cancelAllDownloads() {
        val missions = downloadManager.getPendingMissions().toList()
        missions.forEach { mission ->
            downloadManager.deleteMission(mission, true)
            activeNotifications.remove(mission.timestamp)?.let { notificationId ->
                notificationManager.cancel(notificationId)
            }
            
            // Sync with Supabase
            mission.songId?.let { songId ->
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    cachedRepo.removeCachedDownload(songId)
                }
            }
        }
        downloadStatistics.clear()
        missionPriorities.clear()
        missionRetryCount.clear()
        Log.d(TAG, "Cancelled all downloads")
    }
    
    private fun retryMission(timestamp: Long) {
        val mission = downloadManager.getPendingMissions().find { it.timestamp == timestamp }
        if (mission != null && !mission.running) {
            val retries = missionRetryCount[timestamp] ?: 0
            if (retries < MAX_RETRIES) {
                missionRetryCount[timestamp] = retries + 1
                
                // Check if URLs might be expired (for resume after long pause)
                val shouldRefreshUrl = mission.isUrlExpired() || 
                                      (mission.errCode != DownloadMission.ERROR_NOTHING && 
                                       mission.isUrlExpiredError(mission.errCode))
                
                if (shouldRefreshUrl && mission.sourceVideoUrl != null) {
                    Log.d(TAG, "URLs may be expired (age: ${mission.getUrlAgeHours()}h), refreshing before retry...")
                    
                    // Refresh URLs in background
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val refreshed = mission.refreshUrls()
                            if (refreshed) {
                                Log.d(TAG, "✅ URLs refreshed, proceeding with retry")
                                // Resume with fresh URLs
                                handler.post {
                                    mission.resetState(false, true)
                                    downloadManager.resumeMission(mission)
                                }
                            } else {
                                Log.e(TAG, "❌ URL refresh failed, retrying with old URLs")
                                // Exponential backoff and retry with old URLs
                                val delay = RETRY_DELAY_BASE_MS * (1 shl retries)
                                handler.postDelayed({
                                    mission.resetState(false, true)
                                    downloadManager.resumeMission(mission)
                                }, delay)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "URL refresh error", e)
                            // Retry with old URLs after delay
                            val delay = RETRY_DELAY_BASE_MS * (1 shl retries)
                            handler.postDelayed({
                                mission.resetState(false, true)
                                downloadManager.resumeMission(mission)
                            }, delay)
                        }
                    }
                } else {
                    // Normal retry with exponential backoff (URLs not expired)
                    val delay = RETRY_DELAY_BASE_MS * (1 shl retries)
                    Log.d(TAG, "Retrying mission (attempt ${retries + 1}/$MAX_RETRIES) after ${delay}ms delay")
                    
                    handler.postDelayed({
                        mission.resetState(false, true)
                        downloadManager.resumeMission(mission)
                    }, delay)
                }
            } else {
                Log.w(TAG, "Max retries reached for mission: ${mission.storage?.getName()}")
                notifyMaxRetriesReached(mission)
            }
        }
    }
    
    private fun setPriority(timestamp: Long, priority: Int) {
        missionPriorities[timestamp] = priority
        Log.d(TAG, "Set priority for mission $timestamp to $priority")
        // Re-sort and manage queue
        reorganizeQueue()
    }
    
    private fun setWifiOnlyMode(enabled: Boolean) {
        wifiOnlyMode = enabled
        Log.d(TAG, "WiFi-only mode: $enabled")
        
        if (enabled && !isWifiConnected()) {
            // Pause all running downloads
            pauseAllDownloads()
            showWifiRequiredNotification()
        } else if (!enabled) {
            // Can resume downloads on mobile
            resumeAllDownloads()
        }
    }
    
    private fun reorganizeQueue() {
        // Pause low priority downloads if concurrent limit reached
        val running = downloadManager.getPendingMissions().filter { it.running }
        if (running.size > MAX_CONCURRENT_DOWNLOADS) {
            val sorted = running.sortedBy { missionPriorities[it.timestamp] ?: PRIORITY_NORMAL }
            sorted.take(running.size - MAX_CONCURRENT_DOWNLOADS).forEach { mission ->
                downloadManager.pauseMission(mission)
            }
        }
        
        // Start high priority downloads
        val paused = downloadManager.getPendingMissions()
            .filter { !it.running && !it.isFinished() }
            .sortedByDescending { missionPriorities[it.timestamp] ?: PRIORITY_NORMAL }
        
        var started = 0
        for (mission in paused) {
            if (getRunningDownloadsCount() >= MAX_CONCURRENT_DOWNLOADS) break
            if (wifiOnlyMode && !isWifiConnected()) break
            downloadManager.resumeMission(mission)
            started++
        }
    }
    
    private fun getRunningDownloadsCount(): Int {
        return downloadManager.getPendingMissions().count { it.running }
    }
    
    private fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy - saving all missions before shutdown")
        
        // Stop progress updates
        progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
        isProgressUpdateRunning = false
        
        // Save all missions to database before shutdown
        downloadManager.saveAllMissions()
        
        // Pause all running missions
        downloadManager.pauseAllMissions(true)
        
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        connectivityManager.unregisterNetworkCallback(networkCallback)
        
        Log.d(TAG, "Service destroyed, all missions saved")
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved - app swiped from recents, saving missions")
        
        // Save all missions when app is swiped from recents
        downloadManager.saveAllMissions()
        downloadManager.pauseAllMissions(false)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download notifications"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun startMission(intent: Intent) {
        val urls = intent.getStringArrayExtra(EXTRA_URLS) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Download"
        val artist = intent.getStringExtra(EXTRA_ARTIST)
        val thumbnailUrl = intent.getStringExtra(EXTRA_THUMBNAIL)
        // Read kind as String (was sent as String from startMission)
        val kindString = intent.getStringExtra(EXTRA_KIND) ?: "v"
        val kind = kindString.firstOrNull() ?: 'v'
        val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "720p"
        val sourceVideoUrl = intent.getStringExtra(EXTRA_SOURCE_VIDEO_URL) // For URL refresh
        val songId = intent.getStringExtra(EXTRA_SONG_ID)
        
        // REMOVED: Storage permission check - Android 10+ uses scoped storage (MediaStore API)
        // which doesn't require WRITE_EXTERNAL_STORAGE permission.
        // The system handles permissions automatically for MediaStore operations.
        
        Log.d(TAG, "Starting mission: $title by $artist at $quality")
        Log.d(TAG, "Download URL: ${urls.firstOrNull()?.take(100)}")
        
        // Create storage
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\s-]"), "")
        val fileName = "$sanitizedTitle [$quality].mp4"
        val storage = createStorage(fileName, kind)
        
        if (storage == null) {
            Log.e(TAG, "Failed to create storage for: $fileName")
            return
        }
        
        Log.d(TAG, "Storage created: ${storage.getName()}")
        
        // For MediaStore URIs, we don't need to call create()
        // The URI was already created by MediaStore
        if (storage.isDirect()) {
            // Only for direct file access, we need to create the file
            if (!storage.create()) {
                Log.e(TAG, "Failed to create file: ${storage.getName()}")
                return
            }
        }
        
        Log.d(TAG, "File ready for download: ${storage.getUri()}")
        
        // Create mission
        val mission = DownloadMission(
            urls = urls,
            storage = storage,
            kind = kind,
            postprocessing = null,
            context = this,
            songId = songId
        )
        
        mission.source = urls.firstOrNull()
        // Set metadata
        mission.title = title
        mission.artist = artist
        mission.thumbnailUrl = thumbnailUrl
        mission.quality = quality
        // Store source video URL for potential URL refresh
        mission.sourceVideoUrl = sourceVideoUrl
        
        Log.d(TAG, "Starting download manager mission...")
        
        
        // Start download
        downloadManager.startMission(mission)
        
        // CRITICAL: Trigger progress updates immediately so download appears in UI
        // This ensures notification is created and progress bar starts updating
        if (!isProgressUpdateRunning) {
            isProgressUpdateRunning = true
            progressUpdateHandler.post(progressUpdateRunnable)
            Log.d(TAG, "✅ Progress updates triggered for new mission")
        }
        
        // Create initial notification for this download
        notifyProgress(mission)
        
        Log.d(TAG, "Download mission started successfully!")
    }
    
    /**
     * Check if app has necessary storage permissions
     */
    private fun hasStoragePermissions(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-12 (API 23-32)
                val readGranted = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                
                val writeGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    // Android 9 and below - WRITE permission is required
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    // Android 10-12 - WRITE permission not needed with scoped storage
                    true
                }
                
                readGranted && writeGranted
            }
            else -> true // Android 5 and below - permissions granted at install
        }
    }
    
    /**
     * Show error notification when permissions are not granted
     */
    private fun showPermissionErrorNotification(title: String) {
        val intent = Intent(this, com.musifly.android.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("❌ Download Failed: $title")
            .setContentText("Storage permission required. Please grant permission in app settings.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(notificationIdCounter++, notification)
    }
    
    /**
     * Create storage for download file
     * Uses MediaStore API on Android 10+ (API 29+) for scoped storage compliance
     */
    private fun createStorage(fileName: String, kind: Char): StoredFileHelper? {
        return try {
            // Sanitize filename to remove invalid characters
            val sanitizedFileName = fileName.replace(Regex("[\u003c\u003e:\"/\\|?*]"), "_")
            
            // Android 10+ (API 29+) requires using MediaStore API for external storage
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Log.d(TAG, "Using MediaStore API for Android 10+ (SDK ${Build.VERSION.SDK_INT})")
                return createStorageWithMediaStore(sanitizedFileName, kind)
            }
            
            // For Android 9 and below, use traditional file system approach
            Log.d(TAG, "Using traditional file system for Android \u003c 10 (SDK ${Build.VERSION.SDK_INT})")
            
            // Get custom directory from preferences
            val customPath = getCustomDirectory(this)
            
            val appDir: File
            
            if (customPath != null) {
                // Use custom directory
                val parsedPath = parseDirectoryPath(customPath)
                appDir = File(parsedPath)
                Log.d(TAG, "Using custom download directory: ${appDir.absolutePath}")
            } else {
                // Use default directory based on content type
                val defaultPath = when (kind) {
                    'v' -> DEFAULT_VIDEO_PATH
                    'a' -> DEFAULT_AUDIO_PATH
                    else -> DEFAULT_VIDEO_PATH
                }
                appDir = File(defaultPath)
                Log.d(TAG, "Using default download directory: ${appDir.absolutePath}")
            }
            
            // Create directory hierarchy if it doesn't exist
            if (!appDir.exists()) {
                val created = appDir.mkdirs()
                if (!created) {
                    Log.e(TAG, "Failed to create download directory: ${appDir.absolutePath}")
                    // Fallback to app-specific external storage (doesn't require permissions)
                    val fallbackDir = File(getExternalFilesDir(null), if (kind == 'v') "Videos" else "Music")
                    if (!fallbackDir.exists()) fallbackDir.mkdirs()
                    Log.d(TAG, "Using app-specific fallback directory: ${fallbackDir.absolutePath}")
                    return createStorageInDirectory(fallbackDir, sanitizedFileName, kind)
                }
            }
            
            if (!appDir.canWrite()) {
                Log.e(TAG, "Directory is not writable: ${appDir.absolutePath}")
                // Fallback to app-specific external storage
                val fallbackDir = File(getExternalFilesDir(null), if (kind == 'v') "Videos" else "Music")
                if (!fallbackDir.exists()) fallbackDir.mkdirs()
                Log.d(TAG, "Using app-specific fallback directory: ${fallbackDir.absolutePath}")
                return createStorageInDirectory(fallbackDir, sanitizedFileName, kind)
            }
            
            createStorageInDirectory(appDir, sanitizedFileName, kind)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating storage", e)
            null
        }
    }
    
    /**
     * Create storage using MediaStore API (Android 10+)
     * This is required for accessing external storage on Android 10+ (scoped storage)
     */
    private fun createStorageWithMediaStore(fileName: String, kind: Char): StoredFileHelper? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        
        return try {
            // Set relative path based on content type
            val relativePath = when (kind) {
                'v' -> "${Environment.DIRECTORY_MOVIES}/MusiFly"
                'a' -> "${Environment.DIRECTORY_MUSIC}/MusiFly"
                else -> "${Environment.DIRECTORY_DOWNLOADS}/MusiFly"
            }
            
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, if (kind == 'v') "video/mp4" else "audio/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                
                // Mark as pending until download completes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            
            // Choose collection based on content type
            val collection = when (kind) {
                'v' -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                'a' -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            
            // Insert into MediaStore
            val uri = contentResolver.insert(collection, contentValues)
            
            if (uri != null) {
                Log.d(TAG, "Created MediaStore entry: $uri")
                Log.d(TAG, "File will be saved to: $relativePath/$fileName")
                StoredFileHelper(this, uri, if (kind == 'v') "video" else "audio")
            } else {
                Log.e(TAG, "Failed to create MediaStore entry")
                // Fallback to app-specific external storage (doesn't require scoped storage)
                val fallbackDir = File(getExternalFilesDir(null), if (kind == 'v') "Videos" else "Music")
                if (!fallbackDir.exists()) fallbackDir.mkdirs()
                Log.d(TAG, "Using app-specific fallback directory: ${fallbackDir.absolutePath}")
                createStorageInDirectory(fallbackDir, fileName, kind)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating storage with MediaStore", e)
            // Fallback to app-specific external storage
            val fallbackDir = File(getExternalFilesDir(null), if (kind == 'v') "Videos" else "Music")
            if (!fallbackDir.exists()) fallbackDir.mkdirs()
            Log.d(TAG, "Using app-specific fallback directory: ${fallbackDir.absolutePath}")
            createStorageInDirectory(fallbackDir, fileName, kind)
        }
    }
    
    /**
     * Create storage in a specific directory (legacy file system approach)
     */
    private fun createStorageInDirectory(directory: File, fileName: String, kind: Char): StoredFileHelper? {
        return try {
            val file = File(directory, fileName)
            
            // If file exists, create unique name
            var finalFile = file
            if (finalFile.exists()) {
                var counter = 1
                val nameWithoutExt = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".", "")
                while (finalFile.exists()) {
                    val newName = if (ext.isNotEmpty()) {
                        "$nameWithoutExt ($counter).$ext"
                    } else {
                        "$nameWithoutExt ($counter)"
                    }
                    finalFile = File(directory, newName)
                    counter++
                }
                Log.d(TAG, "File exists, using unique name: ${finalFile.name}")
            }
            
            StoredFileHelper(finalFile, if (kind == 'v') "video" else "audio")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating storage in directory", e)
            null
        }
    }
    
    private fun handleMessage(msg: Message): Boolean {
        val mission = msg.obj as? DownloadMission ?: return true
        
        when (msg.what) {
            MESSAGE_RUNNING -> {
                updateForegroundState(true)
                // Start periodic progress updates if not already running
                if (!isProgressUpdateRunning) {
                    isProgressUpdateRunning = true
                    progressUpdateHandler.post(progressUpdateRunnable)
                }
                // Update progress notification immediately
                notifyProgress(mission)
            }
            MESSAGE_PAUSED -> {
                updateForegroundState(downloadManager.getRunningMissionsCount() > 0)
                notifyPaused(mission)
                // Stop progress updates if no running missions
                if (downloadManager.getRunningMissionsCount() == 0) {
                    progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
                    isProgressUpdateRunning = false
                }
            }
            MESSAGE_FINISHED -> {
                // Finalize the download - clear IS_PENDING flag so file is visible in storage
                mission.storage?.finalizeDownload(this)
                
                // Immediately update notification to remove progress bar and show completion
                notifyFinished(mission)
                // Remove from active notifications to prevent further progress updates
                activeNotifications.remove(mission.timestamp)
                
                // Add to Supabase repository
                mission.songId?.let { songId ->
                    kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        cachedRepo.addCachedDownload(songId)
                    }
                }

                downloadManager.setFinished(mission)
                handleConnectivityState()
                updateForegroundState(downloadManager.runMissions())
                // Stop progress updates if no running missions
                if (downloadManager.getRunningMissionsCount() == 0) {
                    progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
                    isProgressUpdateRunning = false
                }
            }
            MESSAGE_ERROR -> {
                notifyError(mission)
                handleConnectivityState()
                updateForegroundState(downloadManager.runMissions())
            }
        }
        
        return true
    }
    
    private fun handleConnectivityState() {
        val networkInfo = connectivityManager.activeNetwork
        val capabilities = if (networkInfo != null) {
            connectivityManager.getNetworkCapabilities(networkInfo)
        } else null
        
        val status = when {
            capabilities == null -> NetworkState.Unavailable
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) -> {
                NetworkState.Operating
            }
            else -> NetworkState.MeteredOperating
        }
        
        Log.d(TAG, "Network status: $status")
        downloadManager.handleConnectivityState(status, false)
    }
    
    private fun updateForegroundState(state: Boolean) {
        if (state == foreground) return
        
        if (state) {
            val notification = createForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification)
            }
        } else {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        
        foreground = state
    }
    
    private fun createForegroundNotification(): Notification {
        val runningCount = downloadManager.getRunningMissionsCount()
        val intent = Intent(this, com.musifly.android.MainActivity::class.java).apply {
            putExtra("route", "downloads")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenTune Downloads")
            .setContentText(if (runningCount > 0) "$runningCount download(s) in progress" else "Downloads active")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun notifyProgress(mission: DownloadMission) {
        if (!downloadNotificationEnable) return
        
        val notificationId = activeNotifications.getOrPut(mission.timestamp) {
            notificationIdCounter++
        }
        
        val progress = mission.getProgress().toInt()
        val fileName = mission.title ?: mission.storage?.getName() ?: "Downloading..."
        val doneBytes = mission.done
        val totalBytes = mission.length
        
        // Update statistics
        val stats = downloadStatistics.getOrPut(mission.timestamp) {
            DownloadStats(startTime = System.currentTimeMillis())
        }
        stats.bytesDownloaded = doneBytes
        
        // Calculate speed and ETA
        val speed = if (mission.running && stats.startTime > 0) {
            val elapsedSeconds = (System.currentTimeMillis() - stats.startTime) / 1000
            if (elapsedSeconds > 0) {
                val bytesPerSecond = doneBytes / elapsedSeconds
                // Update stats
                stats.averageSpeed = bytesPerSecond
                if (bytesPerSecond > stats.peakSpeed) {
                    stats.peakSpeed = bytesPerSecond
                }
                formatBytes(bytesPerSecond)
            } else {
                "calculating..."
            }
        } else {
            null
        }
        
        // Calculate ETA
        val eta = if (mission.running && stats.averageSpeed > 0 && totalBytes > doneBytes) {
            val remainingBytes = totalBytes - doneBytes
            val remainingSeconds = remainingBytes / stats.averageSpeed
            formatTime(remainingSeconds)
        } else {
            null
        }
        
        // Get priority badge
        val priorityBadge = when (missionPriorities[mission.timestamp] ?: PRIORITY_NORMAL) {
            PRIORITY_URGENT -> "🔴 "
            PRIORITY_HIGH -> "🟡 "
            PRIORITY_LOW -> "🔵 "
            else -> ""
        }
        
        // Get network type indicator
        val networkIndicator = if (isWifiConnected()) "📶 WiFi" else "📱 Mobile"
        
        // Main intent to open downloads
        val intent = Intent(this, com.musifly.android.MainActivity::class.java).apply {
            putExtra("route", "downloads")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Action intents
        val pauseIntent = Intent(this, DownloadManagerService::class.java).apply {
            action = ACTION_PAUSE
            putExtra(EXTRA_MISSION_TIMESTAMP, mission.timestamp)
        }
        val pausePendingIntent = PendingIntent.getService(
            this,
            (mission.timestamp + 1).toInt(),
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val resumeIntent = Intent(this, DownloadManagerService::class.java).apply {
            action = ACTION_RESUME
            putExtra(EXTRA_MISSION_TIMESTAMP, mission.timestamp)
        }
        val resumePendingIntent = PendingIntent.getService(
            this,
            (mission.timestamp + 2).toInt(),
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val cancelIntent = Intent(this, DownloadManagerService::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_MISSION_TIMESTAMP, mission.timestamp)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            (mission.timestamp + 3).toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val contentText = buildString {
            append("${formatBytes(doneBytes)} / ${formatBytes(totalBytes)}")
            if (speed != null) {
                append(" • ⚡ $speed/s")
            }
            if (eta != null) {
                append(" • ⏱️ $eta left")
            }
            append(" • $progress%")
        }
        
        val bigText = buildString {
            append("$fileName\n\n")
            append("Progress: ${formatBytes(doneBytes)} / ${formatBytes(totalBytes)} ($progress%)\n")
            if (speed != null) append("Speed: ⚡ $speed/s\n")
            if (eta != null) append("ETA: ⏱️ $eta\n")
            append("Network: $networkIndicator\n")
            val retries = missionRetryCount[mission.timestamp] ?: 0
            if (retries > 0) append("Retries: $retries/$MAX_RETRIES\n")
            if (stats.peakSpeed > 0) append("Peak Speed: ${formatBytes(stats.peakSpeed)}/s\n")
            append("\nTap to view in Download Manager")
        }
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$priorityBadge📥 $fileName")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, false) // Live progress bar - updates every second
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Prevents sound/vibration on every update
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(bigText))
            .setShowWhen(true) // Show timestamp
            .setSubText("Queue: ${getRunningDownloadsCount()}/$MAX_CONCURRENT_DOWNLOADS active")
        
        // Add action buttons based on state - only for active/paused downloads
        // Never add actions for finished downloads
        if (mission.running && !mission.isFinished()) {
            notificationBuilder
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "Pause",
                    pausePendingIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_delete,
                    "Cancel",
                    cancelPendingIntent
                )
        } else if (!mission.isFinished() && !mission.isCorrupt()) {
            notificationBuilder
                .addAction(
                    android.R.drawable.ic_media_play,
                    "Resume",
                    resumePendingIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_delete,
                    "Cancel",
                    cancelPendingIntent
                )
        }
        // No actions for finished/corrupt downloads - they will be handled by notifyFinished
        
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
    
    private fun notifyPaused(mission: DownloadMission) {
        if (!downloadNotificationEnable) return
        
        val notificationId = activeNotifications[mission.timestamp] ?: return
        val fileName = mission.title ?: mission.storage?.getName() ?: "Download"
        val progress = mission.getProgress().toInt()
        val doneBytes = mission.done
        val totalBytes = mission.length
        
        val intent = Intent(this, com.musifly.android.MainActivity::class.java).apply {
            putExtra("route", "downloads")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val resumeIntent = Intent(this, DownloadManagerService::class.java).apply {
            action = ACTION_RESUME
            putExtra(EXTRA_MISSION_TIMESTAMP, mission.timestamp)
        }
        val resumePendingIntent = PendingIntent.getService(
            this,
            (mission.timestamp + 2).toInt(),
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val cancelIntent = Intent(this, DownloadManagerService::class.java).apply {
            action = ACTION_CANCEL
            putExtra(EXTRA_MISSION_TIMESTAMP, mission.timestamp)
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            (mission.timestamp + 3).toInt(),
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val contentText = "${formatBytes(doneBytes)} / ${formatBytes(totalBytes)} • $progress%"
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏸️ Download paused")
            .setContentText("$fileName\n$contentText")
            .setSmallIcon(android.R.drawable.ic_media_pause)
            .setContentIntent(pendingIntent)
            .setOngoing(false)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$fileName\n\n$contentText\n\nTap to view in Download Manager"))
            .addAction(
                android.R.drawable.ic_media_play,
                "Resume",
                resumePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_delete,
                "Cancel",
                cancelPendingIntent
            )
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    private fun notifyFinished(mission: DownloadMission) {
        if (!downloadNotificationEnable) return
        
        val notificationId = activeNotifications.remove(mission.timestamp) ?: mission.timestamp.toInt()
        val fileName = mission.title ?: mission.storage?.getName() ?: "Download"
        val fileSize = formatBytes(mission.length)
        
        // Cancel the old notification first to remove any action buttons and progress bar
        notificationManager.cancel(notificationId)
        
        val intent = Intent(this, com.musifly.android.MainActivity::class.java).apply {
            putExtra("route", "downloads")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create completely fresh notification without any actions or progress bar
        // Use custom done icon to avoid system download arrow animation
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("✅ Download complete")
            .setContentText("$fileName ($fileSize)")
            .setSmallIcon(R.drawable.done) // Use custom static checkmark icon (no animation)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false) // Not ongoing - user can dismiss
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setShowWhen(true) // Show completion timestamp
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$fileName\n\nSize: $fileSize\n\n✅ Download completed successfully\n\nTap to view in Download Manager"))
            .build()
        
        // Notify with a slight delay to ensure old notification is cleared
        notificationManager.notify(notificationId, notification)
    }
    
    private fun notifyError(mission: DownloadMission) {
        if (!downloadNotificationEnable) return
        
        val notificationId = activeNotifications.remove(mission.timestamp) ?: mission.timestamp.toInt()
        val fileName = mission.storage?.getName() ?: "Download"
        
        val intent = Intent(this, com.musifly.android.MainActivity::class.java).apply {
            putExtra("route", "downloads")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val errorMessage = when (mission.errCode) {
            DownloadMission.ERROR_CONNECT_HOST -> "Connection failed"
            DownloadMission.ERROR_HTTP_NOT_FOUND -> "File not found"
            DownloadMission.ERROR_INSUFFICIENT_STORAGE -> "Insufficient storage"
            else -> "Download failed"
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("❌ $errorMessage")
            .setContentText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$fileName\n$errorMessage\nTap to view in Download Manager"))
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
    
    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.2f GB".format(gb)
    }
    
    private fun formatTime(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
    
    private fun showWifiRequiredNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📶 WiFi Required")
            .setContentText("Downloads paused. WiFi-only mode is enabled.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("WiFi-only mode is enabled. Downloads will resume when WiFi is available.\n\nConnect to WiFi or disable WiFi-only mode in settings."))
            .build()
        
        notificationManager.notify(9999, notification)
    }
    
    private fun notifyMaxRetriesReached(mission: DownloadMission) {
        val fileName = mission.title ?: mission.storage?.getName() ?: "Download"
        val retries = missionRetryCount[mission.timestamp] ?: 0
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("❌ Download Failed")
            .setContentText("$fileName - Max retries ($retries) reached")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$fileName\n\nFailed after $retries retry attempts.\n\nPossible causes:\n• Server unavailable\n• Network issues\n• Insufficient storage\n\nTap to manage downloads."))
            .build()
        
        notificationManager.notify(mission.timestamp.toInt(), notification)
    }
    
    /**
     * Binder for accessing download manager
     */
    inner class DownloadManagerBinder : Binder() {
        fun getDownloadManager(): DownloadManager = downloadManager
        
        fun enableNotifications(enable: Boolean) {
            downloadNotificationEnable = enable
        }
    }
}
