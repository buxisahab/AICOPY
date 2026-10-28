package com.musifly.android.streaming

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.session.MediaSession
import com.musifly.android.R
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Streaming Player Service
 * Foreground service for uninterrupted audio streaming
 * Features:
 * - Foreground service (survives app minimize/screen off)
 * - WiFi lock (prevents disconnection)
 * - Wake lock (optional, for extreme reliability)
 * - MediaSession integration
 * - Network-aware retry
 */
class StreamingPlayerService : Service() {
    
    private val TAG = "StreamingService"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "streaming_playback"
    
    private val binder = StreamingBinder()
    private var streamingEngine: NewPipeStreamingEngine? = null
    private var mediaSession: MediaSession? = null
    
    // Locks for preventing interruptions
    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Service scope for coroutines
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Current stream information
    private var currentStreamInfo: NewPipeAudioStreamExtractor.AudioStreamInfo? = null
    private var isInitialized = false
    
    /**
     * Binder for service connection
     */
    inner class StreamingBinder : Binder() {
        fun getService(): StreamingPlayerService = this@StreamingPlayerService
    }
    
    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("🎵 StreamingPlayerService created")
        
        // Create notification channel
        createNotificationChannel()
        
        // Initialize streaming engine
        streamingEngine = NewPipeStreamingEngine(this).apply {
            initializePlayer(streamingListener)
        }
        
        // Initialize MediaSession
        initializeMediaSession()
        
        // Acquire WiFi lock
        acquireWifiLock()
        
        isInitialized = true
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.tag(TAG).d("onStartCommand")
        
        // Start as foreground service
        startForeground()
        
        return START_STICKY // Restart service if killed
    }
    
    /**
     * Create notification channel for Android O+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing music"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Start foreground service with notification
     */
    private fun startForeground() {
        val notification = buildNotification(
            title = currentStreamInfo?.title ?: "Loading...",
            artist = currentStreamInfo?.uploader ?: "Unknown"
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        Timber.tag(TAG).d("✅ Started as foreground service")
    }
    
    /**
     * Build notification
     */
    private fun buildNotification(title: String, artist: String): Notification {
        // Create intent to open app
        val openIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.media3_icon_circular_play)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    /**
     * Update notification with new info
     */
    fun updateNotification(title: String, artist: String) {
        val notification = buildNotification(title, artist)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Initialize MediaSession for media controls
     */
    private fun initializeMediaSession() {
        streamingEngine?.getPlayer()?.let { player ->
            mediaSession = MediaSession.Builder(this, player)
                .build()
            
            Timber.tag(TAG).d("✅ MediaSession initialized")
        }
    }
    
    /**
     * Acquire WiFi lock to prevent disconnection
     */
    private fun acquireWifiLock() {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "StreamingPlayerService:WifiLock"
            ).apply {
                acquire()
            }
            Timber.tag(TAG).d("✅ WiFi lock acquired")
        } catch (e: Exception) {
            Timber.tag(TAG).w("⚠️ Failed to acquire WiFi lock: ${e.message}")
        }
    }
    
    /**
     * Release WiFi lock
     */
    private fun releaseWifiLock() {
        wifiLock?.let {
            if (it.isHeld) {
                it.release()
                Timber.tag(TAG).d("WiFi lock released")
            }
        }
        wifiLock = null
    }
    
    /**
     * Streaming engine listener
     */
    private val streamingListener = object : NewPipeStreamingEngine.StreamingListener {
        override fun onStreamReady() {
            Timber.tag(TAG).d("🎵 Stream ready")
        }
        
        override fun onStreamPlaying() {
            Timber.tag(TAG).d("▶️ Stream playing")
            currentStreamInfo?.let {
                updateNotification(it.title, it.uploader)
            }
        }
        
        override fun onStreamPaused() {
            Timber.tag(TAG).d("⏸️ Stream paused")
        }
        
        override fun onStreamEnded() {
            Timber.tag(TAG).d("🏁 Stream ended")
        }
        
        override fun onStreamError(error: String) {
            Timber.tag(TAG).e("❌ Stream error: $error")
        }
        
        override fun onBufferingStarted() {
            Timber.tag(TAG).d("⏳ Buffering started")
        }
        
        override fun onBufferingEnded() {
            Timber.tag(TAG).d("✅ Buffering ended")
        }
        
        override fun onPositionChanged(positionMs: Long, durationMs: Long) {
            // Handle position updates if needed
        }
    }
    
    /**
     * Load and play stream from title and artist
     */
    fun playStream(songTitle: String, artistName: String? = null) {
        if (!isInitialized) {
            Timber.tag(TAG).e("❌ Service not initialized")
            return
        }
        
        Timber.tag(TAG).d("🎵 Playing: $songTitle by ${artistName ?: "Unknown"}")
        
        serviceScope.launch {
            try {
                // 1. Find correct video
                val videoUrl = NewPipeStreamFinder.findCorrectVideo(songTitle, artistName).getOrThrow()
                
                // 2. Extract audio stream
                val streamInfo = NewPipeAudioStreamExtractor.getBestAudioStream(videoUrl).getOrThrow()
                currentStreamInfo = streamInfo
                
                // 3. Load and play stream
                streamingEngine?.loadStream(streamInfo.url)
                streamingEngine?.play()
                
                // 4. Update notification
                updateNotification(streamInfo.title, streamInfo.uploader)
                
                Timber.tag(TAG).d("✅ Stream loaded and playing")
                
            } catch (e: Exception) {
                Timber.tag(TAG).e("❌ Failed to play stream: ${e.message}")
            }
        }
    }
    
    /**
     * Load stream directly from URL
     */
    fun playStreamFromUrl(streamUrl: String, title: String, artist: String) {
        streamingEngine?.loadStream(streamUrl)
        streamingEngine?.play()
        updateNotification(title, artist)
    }
    
    /**
     * Play/Pause toggle
     */
    fun togglePlayPause() {
        streamingEngine?.let { engine ->
            if (engine.isPlaying()) {
                engine.pause()
            } else {
                engine.play()
            }
        }
    }
    
    /**
     * Get streaming engine
     */
    fun getStreamingEngine(): NewPipeStreamingEngine? = streamingEngine
    
    override fun onDestroy() {
        Timber.tag(TAG).d("🧹 StreamingPlayerService destroyed")
        
        // Release all resources
        streamingEngine?.release()
        streamingEngine = null
        
        mediaSession?.release()
        mediaSession = null
        
        releaseWifiLock()
        
        serviceScope.cancel()
        
        super.onDestroy()
    }
}
