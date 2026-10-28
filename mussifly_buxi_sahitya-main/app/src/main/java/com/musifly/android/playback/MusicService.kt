@file:Suppress("DEPRECATION")

package com.musifly.android.playback

// JossRed import removed - fake fallback mechanism

// JossRedFallbackKey import removed - fake fallback mechanism
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.SQLException
import android.media.AudioFocusRequest
import android.media.AudioManager

import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.google.common.util.concurrent.MoreExecutors
import com.musifly.android.MainActivity
import com.musifly.android.R
import com.musifly.android.constants.AudioNormalizationKey
import com.musifly.android.constants.AudioQualityKey
import com.musifly.android.constants.AutoLoadMoreKey
import com.musifly.android.constants.AutoSkipNextOnErrorKey
import com.musifly.android.constants.BassBoostEnabledKey
import com.musifly.android.constants.BassBoostStrengthKey

import com.musifly.android.constants.EqualizerEnabledKey
import com.musifly.android.constants.EqualizerPreset
import com.musifly.android.constants.EqualizerPresetKey
import com.musifly.android.constants.HideExplicitKey
import com.musifly.android.constants.HistoryDuration
import com.musifly.android.constants.LiveAlertEnabledKey
import com.musifly.android.constants.MediaSessionConstants.CommandToggleLike
import com.musifly.android.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.musifly.android.constants.MediaSessionConstants.CommandToggleShuffle
import com.musifly.android.constants.PauseListenHistoryKey
import com.musifly.android.constants.PersistentQueueKey
import com.musifly.android.constants.PlayerVolumeKey
import com.musifly.android.constants.RepeatModeKey
import com.musifly.android.constants.ShowLyricsKey
import com.musifly.android.constants.SimilarContent
import com.musifly.android.constants.SkipSilenceKey
import com.musifly.android.constants.UseNewPipeStreamingKey
import com.musifly.android.constants.VirtualizerEnabledKey
import com.musifly.android.constants.VirtualizerStrengthKey
import com.musifly.android.constants.VolumeBoostEnabledKey
import com.musifly.android.constants.DSPModeKey
import com.musifly.android.constants.NativeDSPCompressorEnabledKey
import com.musifly.android.constants.NativeDSPCompressorThresholdKey
import com.musifly.android.constants.NativeDSPCompressorRatioKey
import com.musifly.android.constants.VolumeBoostGainKey
import com.musifly.android.constants.AiDspEnabledKey
import com.musifly.android.constants.DolbyAtmosModeKey
import com.musifly.android.constants.Surround3dEnabledKey
import com.musifly.android.constants.Surround3dStrengthKey
import com.musifly.android.constants.TrebleBoostEnabledKey
import com.musifly.android.constants.TrebleBoostStrengthKey
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.Event
import com.musifly.android.db.entities.FormatEntity
import com.musifly.android.db.entities.LyricsEntity
import com.musifly.android.db.entities.RelatedSongMap
import com.musifly.android.di.DownloadCache
import com.musifly.android.di.PlayerCache


import com.musifly.android.extensions.SilentHandler
import com.musifly.android.extensions.collect
import com.musifly.android.extensions.collectLatest
import com.musifly.android.extensions.currentMetadata
import com.musifly.android.extensions.findNextMediaItemById
import com.musifly.android.extensions.mediaItems
import com.musifly.android.extensions.metadata
import com.musifly.android.extensions.toMediaItem
import com.musifly.android.extensions.toQueue
import com.musifly.android.lyrics.LyricsHelper
import com.musifly.android.models.PersistPlayerState
import com.musifly.android.models.PersistQueue
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.playback.queues.EmptyQueue
import com.musifly.android.playback.queues.Queue
import com.musifly.android.playback.queues.YouTubeQueue
import com.musifly.android.playback.queues.filterExplicit
import com.musifly.android.streaming.NewPipeAudioStreamExtractor
import com.musifly.android.streamingengines.StreamSourceManager
import com.musifly.android.streamingengines.StreamingEngineManager
import com.musifly.android.ui.widgets.WidgetUpdateHelper

import com.musifly.android.utils.NetworkConnectivityObserver
import com.musifly.android.utils.YTPlayerUtils
import com.musifly.android.utils.dataStore
import com.musifly.android.utils.enumPreference
import com.musifly.android.utils.CoilBitmapLoader
import com.musifly.android.constants.DisableLoadMoreWhenRepeatAllKey
import com.musifly.android.utils.get
import com.musifly.android.utils.reportException
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.SongItem
import com.musifly.innertube.models.WatchEndpoint
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@AndroidEntryPoint
class MusicService :
    MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"

        const val PLAYBACK_CHANNEL_ID = "musifly_playback"
        const val PLAYBACK_NOTIFICATION_ID = 1001

        // Legacy constants for backward compatibility
        const val CHANNEL_ID = PLAYBACK_CHANNEL_ID
        const val NOTIFICATION_ID = PLAYBACK_NOTIFICATION_ID
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"
        const val MAX_CONSECUTIVE_ERR = 5
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"

        // Constants for audio normalization
        const val MAX_GAIN_MB = 800 // Maximum gain in millibels (8 dB)
        const val MIN_GAIN_MB = -800 // Minimum gain in millibels (-8 dB)
    }

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    @Inject
    lateinit var likesRepository: com.musifly.android.sync.repository.LikesRepository

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var wasPlayingBeforeAudioFocusLoss = false
    private var hasAudioFocus = false

    var scope = CoroutineScope(Dispatchers.Main) + Job()

    // Main handler for notification rebinding operations
    private val mainHandler = Handler(Looper.getMainLooper())

    // Background state tracking for lightweight audio processing
    private val isAppInBackground = MutableStateFlow(false)
    private val isScreenOff = MutableStateFlow(false)

    // Background audio processor (completely independent from real-time

    // Flag to track if we're restoring state - prevents auto-playback during restoration
    private var isRestoringState = false

    // Flag to track if we've already handled auto-playback prevention for this service lifecycle
    // Prevents multiple concurrent prevention attempts from multiple onStartCommand calls
    private var hasHandledAutoPlaybackPrevention = false
    private var autoPlaybackPreventionJob: Job? = null

    // MusicBinder for service binding - defined early to avoid forward reference issues
    inner class MusicBinder : Binder() {
        val service: MusicService
            get() {
                // Prevent auto-playback when service is accessed (e.g., when opening settings)
                // Only allow playback if explicitly started by user action
                // NOTE: We don't auto-pause here anymore - let the user control playback
                // The onStartCommand logic handles preventing auto-play on cold start
                return this@MusicService
            }
    }

    private val binder = MusicBinder()

    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    private val isNetworkConnected = MutableStateFlow(false)

    // Track if streaming engine toast has been shown in this session
    private var hasShownStreamingToast = false

    private val audioQuality by enumPreference(
        this,
        AudioQualityKey,
        com.musifly.android.constants.AudioQuality.AUTO
    )

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null

    val currentMediaMetadata = MutableStateFlow<com.musifly.android.models.MediaMetadata?>(null)
    private val currentSong =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.stateIn(scope, SharingStarted.Lazily, null)
    private val currentFormat =
        currentMediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    val playerVolume = MutableStateFlow(dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f))

    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    // ==================== AUDIO EFFECTS SYSTEM ====================


    // 🎯 Session-Independent DSP (No Audio Session ID dependency!)
    // Replaces: AudioEffectsManager (session-based, unreliable)
    // Features: Always works, Bluetooth-safe, Background-safe, Universal compatibility
    private var dspAudioProcessor: DspAudioProcessor? = null
    private var sessionIndependentDspManager: SessionIndependentDspManager? = null

    // Legacy: Deprecated session-based manager (will be removed)
    @Deprecated("Replaced by session-independent DSP", ReplaceWith("sessionIndependentDspManager"))
    var audioEffectsManager: AudioEffectsManager? = null
    
    @Deprecated("No longer needed with session-independent DSP")
    private var isAudioEffectSessionOpened = false


    // 🔥 HYBRID DSP Manager (Default + Native)
    var hybridDSPManager: HybridDSPManager? = null
    
    // Background Audio Processor
    private var backgroundAudioProcessor: BackgroundAudioProcessor? = null
    
    // Legacy DSP Manager
    private var dspManager: DSPManager? = null
    
    // AI DSP Controller
    public var aiDspController: AiDspController? = null

    // Legacy DSP Manager reference (for backward compatibility)




    // Foreground service management flags for Android 14/15 compatibility
    private var pendingForeground = false
    private var isForegroundServiceStarted = false

    // Audio becoming noisy receiver (headphone unplug)
    private val audioBecomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    Log.d(TAG, "🔊 Audio becoming noisy - pausing playback")
                    player.pause()
                }
            }
        }
    }

    // Screen on/off receiver for automatic AI disable
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOff.value = false
                    Log.d(TAG, "📱 Screen ON - AI processing enabled")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOff.value = true
                    Log.d(TAG, "📱 Screen OFF - AI processing disabled")
                }
            }
        }
    }

    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())

    private var consecutivePlaybackErr = 0
    private var bufferingStartTime = 0L
    private var lastBufferingCheckJob: Job? = null
    private val BUFFERING_TIMEOUT_MS = 45000L // 45 seconds timeout (increased for better reliability)

    // Track current streaming engine and switch attempts
    private val _currentStreamingEngine = MutableStateFlow<String?>(null) // "NewPipe", "YouTube", "YouTube Music", "JioSaavn", "Local"
    val currentStreamingEngineFlow = _currentStreamingEngine.asStateFlow()
    private var activeStreamingEngine: String?
        get() = _currentStreamingEngine.value
        set(value) { _currentStreamingEngine.value = value }

    // Track YouTube fallback attempts to prevent infinite loops
    private val ytFallbackTried = mutableSetOf<String>()

    // UNIFIED AUDIO ENGINE: Track active playback job to prevent overlapping players
    private var activePlaybackJob: Job? = null
    private val playbackLock = Any() // Synchronization lock for playback operations
    private var isPlayerReleasing = false // Flag to prevent concurrent release operations

    // TAG constant for logging (avoid accessing private MediaSessionService.TAG)
    private val TAG = "MusicService"

    /**
     * Detect stream engine based on URL and mediaId
     * Called automatically when stream URL is resolved
     */
    private fun detectStreamEngine(streamUrl: String?, mediaId: String): String {
        return when {
            // Local/Downloaded files
            mediaId.startsWith("local_") ||
                    streamUrl?.startsWith("file://") == true ||
                    streamUrl?.startsWith("content://") == true -> "Local"

            // JioSaavn
            mediaId.startsWith("saavn:") ||
                    streamUrl?.contains("saavn", ignoreCase = true) == true ||
                    streamUrl?.contains("jiosaavn", ignoreCase = true) == true -> "JioSaavn"

            // YouTube Music (NewPipe or specific URL patterns)
            streamUrl?.contains("music.youtube.com", ignoreCase = true) == true ||
                    (mediaId.isNotBlank() && activeStreamingEngine == "NewPipe" && streamUrl?.contains("googlevideo.com", ignoreCase = true) == true) -> "YouTube Music"

            // YouTube (default for YouTube URLs)
            streamUrl?.contains("youtube.com", ignoreCase = true) == true ||
                    streamUrl?.contains("googlevideo.com", ignoreCase = true) == true -> "YouTube"

            // Heuristic detection for YouTube IDs (usually 11 chars) when URL is missing
            mediaId.length == 11 && mediaId.all { it.isLetterOrDigit() || it == '-' || it == '_' } -> "YouTube"

            // Fallback: Unknown
            else -> "Unknown"
        }
    }
    private var engineSwitchAttempts = 0
    private val MAX_ENGINE_SWITCH_ATTEMPTS = 2 // Max switches per song

    // Enhanced retry mechanism for transient errors
    private var automaticRetryCount = 0
    private val MAX_AUTOMATIC_RETRIES = 3 // Max automatic retries per error
    private var lastRetryErrorCategory: PlayerErrorMapper.ErrorCategory? = null
    private var lastRetryMediaId: String? = null

    // Shared URL cache for streaming engines (class-level to persist across DataSourceFactory recreations)
    private val songUrlCache = HashMap<String, Pair<String, Long>>()

    /**
     * Clear all cached stream URLs when source switches
     * This prevents old engine URLs from being reused
     */
    fun clearSongUrlCache() {
        Log.d(TAG, "🧹 Clearing songUrlCache (${songUrlCache.size} entries)")
        songUrlCache.clear()
    }

    /**
     * Force rebind Media3 playback notification after notification permission is granted/changed.
     *
     * CRITICAL FIX: When permission is GRANTED, we should NOT touch the channel or notification.
     * Media3 notification is already working. We should NOT call createPlaybackChannel() or
     * startForeground() as it will interfere with Media3's notification and cause it to disappear.
     *
     * Only use fallback when permission is NOT granted.
     */
    fun forceRebindPlaybackNotification() {
        try {
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            val notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationManager.areNotificationsEnabled()
            } else {
                true // Pre-Android 7.0, notifications are always enabled
            }

            // Check POST_NOTIFICATIONS permission for Android 13+
            val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            val hasNotificationPermission = notificationsEnabled && permissionGranted
            val channelExists = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.getNotificationChannel(PLAYBACK_CHANNEL_ID) != null
            } else {
                true
            }

            Log.d(TAG, "🔄 PlaybackNotification: rebind requested, hasPermission=$hasNotificationPermission, channelExists=$channelExists")

            // Step 1: If permission is NOT granted, use fallback notification
            if (!hasNotificationPermission) {
                Log.d(TAG, "⚠️ PlaybackNotification: Permission not granted, using fallback notification")
                // Ensure channel exists for fallback
                if (!channelExists) {
                    createPlaybackChannel()
                }
                try {
                    val fallback = buildFallbackPlaybackNotification()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            PLAYBACK_NOTIFICATION_ID,
                            fallback,
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                        )
                    } else {
                        startForeground(PLAYBACK_NOTIFICATION_ID, fallback)
                    }
                    Log.d(TAG, "✅ PlaybackNotification: Fallback notification set (permission not granted)")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ PlaybackNotification: Failed to set fallback notification", e)
                }
                return
            }

            // Step 2: Permission IS granted - DO NOTHING!
            // Media3 notification is already working perfectly when permission is granted.
            // We should NOT:
            // - Call createPlaybackChannel() (channel already exists and is being used)
            // - Call startForeground() (Media3 is managing the notification)
            // - Touch the notification in any way
            //
            // Any interference will cause Media3 notification to disappear!
            if (::mediaSession.isInitialized && ::player.isInitialized) {
                Log.d(TAG, "✅ PlaybackNotification: Permission granted - Media3 notification is active, NO ACTION NEEDED")
                Log.d(TAG, "✅ PlaybackNotification: DO NOT interfere with Media3 notification - it's working correctly")

                // Just verify notification exists (for logging only)
                mainHandler.post {
                    try {
                        val media3Notification = try {
                            val activeNotifications = notificationManager.activeNotifications
                            activeNotifications.find { it.id == PLAYBACK_NOTIFICATION_ID }?.notification
                        } catch (e: Exception) {
                            null
                        }

                        if (media3Notification != null) {
                            Log.d(TAG, "✅ PlaybackNotification: Media3 notification confirmed active - all good!")
                        } else {
                            Log.d(TAG, "ℹ️ PlaybackNotification: Media3 notification will appear when playback starts")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ PlaybackNotification: Error checking notification", e)
                    }
                }
            } else {
                Log.w(TAG, "⚠️ PlaybackNotification: MediaSession or Player not initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ PlaybackNotification: Failed to force rebind notification", e)
        }
    }

    /**
     * Build a fallback playback notification (only used if Media3 notification unavailable).
     * This should rarely be needed as Media3 handles notifications automatically.
     */
    private fun buildFallbackPlaybackNotification(): android.app.Notification {
        val title = if (::player.isInitialized) {
            player.currentMediaItem?.mediaMetadata?.title?.toString() ?: getString(R.string.music_player)
        } else {
            getString(R.string.music_player)
        }
        val artist = if (::player.isInitialized) {
            player.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Music Player"
        } else {
            "Music Player"
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, PLAYBACK_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.opentune_monochrome)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(android.app.Notification.PRIORITY_LOW)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
                .build()
        } else {
            android.app.Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.opentune_monochrome)
                .setOngoing(true)
                .setPriority(android.app.Notification.PRIORITY_LOW)
                .build()
        }
    }

    /**
     * Legacy method name for backward compatibility.
     */
    fun forceRebindNotification() {
        forceRebindPlaybackNotification()
    }

    /**
     * Create dedicated playback notification channel.
     * This must be called BEFORE initializing Media3 notification provider.
     * Handles channel recreation if it was deleted by user or OEM.
     *
     * IMPORTANT: Cannot delete channel if it's being used by a foreground service.
     * In that case, we just ensure it exists and has correct ID.
     *
     * @param context Context to use (defaults to this service)
     */
    private fun createPlaybackChannel(context: Context = this) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val manager = context.getSystemService(android.app.NotificationManager::class.java)

                // Check if channel already exists (some OEMs allow users to delete channels)
                val existingChannel = manager.getNotificationChannel(PLAYBACK_CHANNEL_ID)
                if (existingChannel != null) {
                    // Channel exists - check if it has correct settings
                    val hasCorrectSettings = existingChannel.importance == android.app.NotificationManager.IMPORTANCE_LOW &&
                            existingChannel.lockscreenVisibility == android.app.Notification.VISIBILITY_PUBLIC

                    if (hasCorrectSettings) {
                        Log.d(TAG, "✅ Playback notification channel '${PLAYBACK_CHANNEL_ID}' already exists with correct settings")
                        return
                    } else {
                        // Channel exists but has wrong settings
                        // CRITICAL: Cannot delete channel if it's being used by foreground service
                        // Android throws SecurityException: "Not allowed to delete channel with a foreground service"
                        // So we just log a warning and continue - Media3 will still work with the channel
                        Log.w(TAG, "⚠️ Playback channel exists but has incorrect settings. Cannot delete while service is running. Media3 will use existing channel.")
                        return // Don't try to recreate - it will fail
                    }
                }

                // Channel doesn't exist - create it
                val channel = android.app.NotificationChannel(
                    PLAYBACK_CHANNEL_ID,
                    "Music Playback",
                    android.app.NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setSound(null, null)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    description = "Music playback notifications"
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
                Log.d(TAG, "✅ Playback notification channel '${PLAYBACK_CHANNEL_ID}' created")
            } catch (e: SecurityException) {
                // Channel is being used by foreground service - this is OK, just log it
                Log.w(TAG, "⚠️ Cannot modify playback channel (in use by foreground service). This is normal during playback.")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to create playback notification channel", e)
                // Don't report exception for channel issues - they're not critical
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        // CRITICAL: Create playback notification channel FIRST (required before creating notifications)
        createPlaybackChannel()

        // REMOVED: Aggressive startForeground() call.
        // Previously, we started the foreground service immediately to workaround potential Android 12+
        // restrictions. However, this caused a persistent "Music Player" notification to appear
        // whenever the app was opened, which is a bad user experience.
        //
        // Since the app is in the foreground when the user opens it, we don't need to rush this.
        // Media3's MediaLibraryService will automatically handle foreground promotion when
        // playback actually starts (via MediaNotification.Provider).
        //
        // If specific background-start issues arise (e.g. from widgets when app is closed),
        // we should handle those in onStartCommand, not universally in onCreate.

        // CRITICAL for Android 12+: Set up exception handler BEFORE super.onCreate() to catch
        // ForegroundServiceStartNotAllowedException from Media3's MediaNotificationManager.
        // This exception was introduced in API 31 and occurs when Media3 tries to call
        // startForeground() from the background without a valid exemption.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
                    // Check for ForegroundServiceStartNotAllowedException (introduced in API 31)
                    if (exception is android.app.ForegroundServiceStartNotAllowedException) {
                        // Check if this is from Media3's MediaNotificationManager
                        val stackTrace = exception.stackTraceToString()
                        if (stackTrace.contains("MediaNotificationManager") ||
                            stackTrace.contains("MediaLibraryService") ||
                            stackTrace.contains("androidx.media3")) {
                            Log.w(TAG, "⚠️ Caught ForegroundServiceStartNotAllowedException from Media3. " +
                                    "Suppressing crash to prevent app termination. " +
                                    "This usually occurs when Media3 updates notifications from background.", exception)
                            // Don't crash - the service may already be in foreground or will be soon.
                            // Suppressing this prevents the app from force-closing for the user.
                            return@setDefaultUncaughtExceptionHandler
                        }
                    }
                    // For all other exceptions, use default handler
                    defaultHandler?.uncaughtException(thread, exception)
                }
                Log.d(TAG, "✅ Exception handler set up for Android 12+ Media3 compatibility")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to set up exception handler", e)
            }
        }

        super.onCreate()

        // 🧠 Initialize AI DSP Controller (Before Player Build)
        // This ensures the AudioProcessor is ready for the RenderersFactory
        aiDspController = AiDspController()
        Log.i(TAG, "🧠 AI DSP Controller initialized")

        // Reset auto-playback prevention flag for this service instance
        hasHandledAutoPlaybackPrevention = false
        autoPlaybackPreventionJob?.cancel()
        autoPlaybackPreventionJob = null

        // Initialize background audio processor (completely independent from real-time audio)
        // Real-time audio path: decode → play (zero AI dependency)
        try {
            val dspEngine = AiDspEngine.getInstance(this)
            backgroundAudioProcessor = BackgroundAudioProcessor(
                dspEngine,
                isAppInBackground,
                isScreenOff
            )
            Log.i(TAG, "✅ Background Audio Processor initialized (independent from real-time audio)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize background audio processor", e)
        }

        // Load and apply DSP settings from preferences to AI DSP
        try {
            val dspManager = DSPManager.getInstance(this)
            dspManager.loadSettings() // This loads settings and applies them to AiDspEngine (shared singleton)
            Log.i(TAG, "✅ AI DSP settings loaded and applied")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load AI DSP settings", e)
        }

        // Register screen on/off receiver for automatic AI disable
        try {
            val screenFilter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            registerReceiver(screenStateReceiver, screenFilter)
            Log.d(TAG, "Screen state receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register screen state receiver", e)
        }



        // 🎯 Initialize Professional DSP Processor
        Log.i(TAG, "🎯 Initializing professional DSP processor...")
        try {
            dspAudioProcessor = DspAudioProcessor()
            sessionIndependentDspManager = SessionIndependentDspManager(dspAudioProcessor!!)
            sessionIndependentDspManager?.setEnabled(true)
            
            Log.i(TAG, "✅ Professional DSP initialized (Crystal clear deep bass profile)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize DSP", e)
            reportException(e)
        }



        // 🔥 Initialize HYBRID DSP (Default + Native modes)
        Log.i(TAG, "🔥 Initializing Hybrid DSP Manager...")
        try {
            // Get sample rate from device
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val sampleRateStr = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            val sampleRate = sampleRateStr?.toIntOrNull() ?: 44100

            hybridDSPManager = HybridDSPManager(this, sampleRate)
            Log.i(TAG, "✅ Hybrid DSP Manager created successfully (Sample rate: $sampleRate Hz)")

            // Initialize with saved DSP mode
            val savedMode = try {
                val modeName = dataStore.get(DSPModeKey, DSPMode.DEFAULT.name)
                DSPMode.valueOf(modeName)
            } catch (e: Exception) {
                DSPMode.DEFAULT
            }
            hybridDSPManager?.switchMode(savedMode)
            Log.i(TAG, "✅ DSP Mode set to: $savedMode")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create Hybrid DSP Manager", e)
            reportException(e)
        }

        // Register audio becoming noisy receiver
        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            registerReceiver(audioBecomingNoisyReceiver, filter)
            Log.d(TAG, "Audio noisy receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register audio noisy receiver", e)
        }

        // Register for source change callbacks to clear cache when source switches
        StreamSourceManager.registerSourceChangeCallback { newSource ->
            Log.d(TAG, "🔄 Source changed callback received in MusicService: ${newSource.name}")
            // Clear URL cache to prevent old engine URLs from being reused
            clearSongUrlCache()
            // Clear current streaming engine state
            activeStreamingEngine = null
            Log.d(TAG, "🧹 Cleared songUrlCache and streaming engine state on source switch")
        }

        // Check if Live Alert is enabled
        val liveAlertEnabled = dataStore.get(LiveAlertEnabledKey, true)

        // Note: Playback notification channel is already created above via createPlaybackChannel()
        // Media3 notification provider MUST use the same channel ID
        // Media3 notification provider MUST use the exact same channel ID
        setMediaNotificationProvider(
            if (liveAlertEnabled) {
                LiveAlertNotificationProvider(
                    this,
                    { PLAYBACK_NOTIFICATION_ID },
                    PLAYBACK_CHANNEL_ID, // Use exact same channel ID
                    R.string.music_player
                )
            } else {
                DefaultMediaNotificationProvider(
                    this,
                    { PLAYBACK_NOTIFICATION_ID },
                    PLAYBACK_CHANNEL_ID, // Use exact same channel ID
                    R.string.music_player
                ).apply {
                    setSmallIcon(R.drawable.opentune_monochrome)
                }
            },
        )

        // REAL-TIME SAFE BUFFERING: Reduced buffers to prevent pipeline overflow
        // Smaller buffers = fewer in-flight frames = no pipelineFull warnings
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10_000,  // minBufferMs: Reduced from 50s to prevent pipeline overflow
                30_000,  // maxBufferMs: Reduced from 120s to limit frame backlog
                500,     // bufferForPlaybackMs: Fast start - only 500ms needed
                1_000    // bufferForPlaybackAfterRebufferMs: Quick recovery after rebuffer
            )
            .setPrioritizeTimeOverSizeThresholds(true) // Prioritize time-based buffering
            .setBackBuffer(10_000, true) // Reduced back buffer to prevent overflow
            .setTargetBufferBytes(-1) // Unlimited buffer size (prioritize time thresholds)
            .build()

        val exoPlayer =
            ExoPlayer
                .Builder(this)
                .setMediaSourceFactory(createMediaSourceFactory())
                .setRenderersFactory(createRenderersFactory())
                .setLoadControl(loadControl) // Apply professional buffering configuration
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    false, // Manual audio focus handling for better control
                ).setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .setHandleAudioBecomingNoisy(true) // Pause when headphones unplug
                .build()

        player = exoPlayer
        player.apply {
            addListener(this@MusicService)
            sleepTimer = SleepTimer(scope, this)
            addListener(sleepTimer)
            addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))

            // 🎯 Professional DSP active in audio pipeline
            Log.i(TAG, "✅ Professional DSP active (Crystal clear deep bass)")
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupAudioFocusRequest()

        mediaLibrarySessionCallback.apply {
            toggleLike = { this@MusicService.toggleLike() }
            toggleLibrary = { this@MusicService.toggleLibrary() }
        }
        mediaSession =
            MediaLibrarySession
                .Builder(this, player, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setBitmapLoader(CoilBitmapLoader(this, scope))
                .build()
        player.repeatMode = dataStore.get(RepeatModeKey, REPEAT_MODE_OFF)

        // Keep a connected controller so that notification works
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        connectivityManager = getSystemService()!!
        connectivityObserver = NetworkConnectivityObserver(this)

        // Observar conectividad de red
        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                isNetworkConnected.value = isConnected
                if (isConnected && waitingForNetworkConnection.value) {
                    // Reintentar reproducción cuando vuelve la conexión
                    waitingForNetworkConnection.value = false
                    if (player.currentMediaItem != null && player.playWhenReady) {
                        player.prepare()
                        player.play()
                    }
                }
            }
        }

        playerVolume.collectLatest(scope) {
            player.volume = it
        }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        currentSong.debounce(1000).collect(scope) { song ->
            updateNotification()
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(scope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id)
                    .first() == null
            ) {
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = lyrics,
                        ),
                    )
                }
            }
        }

        dataStore.data
            .map { it[SkipSilenceKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                player.skipSilenceEnabled = it
            }

        // 🎛️ DSP Presets
        dataStore.data
            .map { it[DolbyAtmosModeKey] }
            .distinctUntilChanged()
            .collect(scope) { presetName ->
                val preset = try {
                    if (presetName != null) SessionIndependentDspManager.Preset.valueOf(presetName) 
                    else SessionIndependentDspManager.Preset.NONE
                } catch (e: Exception) { SessionIndependentDspManager.Preset.NONE }
                sessionIndependentDspManager?.applyPreset(preset)
            }

        // 🎵 Bass Boost
        combine(
            dataStore.data.map { it[BassBoostEnabledKey] ?: false },
            dataStore.data.map { it[BassBoostStrengthKey] ?: 0 }
        ) { enabled, strength -> enabled to strength }
            .distinctUntilChanged()
            .collect(scope) { (enabled, strength) ->
                if (enabled) {
                    val gainDb = (strength / 300f) * 12f
                    sessionIndependentDspManager?.setBassBoost(gainDb)
                } else {
                    sessionIndependentDspManager?.setBassBoost(0f)
                }
            }

        // 🎼 Treble Boost
        combine(
            dataStore.data.map { it[TrebleBoostEnabledKey] ?: false },
            dataStore.data.map { it[TrebleBoostStrengthKey] ?: 0 }
        ) { enabled, strength -> enabled to strength }
            .distinctUntilChanged()
            .collect(scope) { (enabled, strength) ->
                sessionIndependentDspManager?.setTrebleBoost(enabled, strength)
            }

        // 🎧 3D Surround
        combine(
            dataStore.data.map { it[Surround3dEnabledKey] ?: false },
            dataStore.data.map { it[Surround3dStrengthKey] ?: 0 }
        ) { enabled, strength -> enabled to strength }
            .distinctUntilChanged()
            .collect(scope) { (enabled, strength) ->
                sessionIndependentDspManager?.setSurround3d(enabled, strength)
            }

        // 🌌 Virtualizer (Spatializer)
        combine(
            dataStore.data.map { it[VirtualizerEnabledKey] ?: false },
            dataStore.data.map { it[VirtualizerStrengthKey] ?: 0 }
        ) { enabled, strength -> enabled to strength }
            .distinctUntilChanged()
            .collect(scope) { (enabled, strength) ->
                if (enabled) {
                    val strengthFactor = (strength / 1000f).coerceIn(0f, 1f)
                    sessionIndependentDspManager?.setVirtualizer(strengthFactor)
                } else {
                    sessionIndependentDspManager?.setVirtualizer(0f)
                }
            }

        // 🔊 Volume Boost
        combine(
            dataStore.data.map { it[VolumeBoostEnabledKey] ?: false },
            dataStore.data.map { it[VolumeBoostGainKey] ?: 0 }
        ) { enabled, gain -> enabled to gain }
            .distinctUntilChanged()
            .collect(scope) { (enabled, gain) ->
                if (enabled) {
                    val gainDb = (gain / 100f) * 10f
                    sessionIndependentDspManager?.setMasterGain(gainDb)
                } else {
                    sessionIndependentDspManager?.setMasterGain(0f)
                }
            }

        // 🎸 Deep Bass - PROFESSIONAL LOGIC (<80Hz, +10dB)
        dataStore.data
            .map { it[booleanPreferencesKey("deep_bass_enabled_v1")] ?: false }
            .distinctUntilChanged()
            .collect(scope) { enabled ->
                if (enabled) {
                    sessionIndependentDspManager?.setDeepBass(10f)
                } else {
                    sessionIndependentDspManager?.setDeepBass(0f)
                }
            }

        // 🎛️ 4-Band EQ
        dataStore.data
            .map { it[EqualizerEnabledKey] ?: false }
            .distinctUntilChanged()
            .collect(scope) { enabled ->
                sessionIndependentDspManager?.setFourBandEnabled(enabled)
            }

        dataStore.data
            .map { it[intPreferencesKey("four_band_eq_level_0")] ?: 0 }
            .distinctUntilChanged()
            .collect(scope) { level ->
                sessionIndependentDspManager?.setFourBandEq(0, level)
            }

        dataStore.data
            .map { it[intPreferencesKey("four_band_eq_level_1")] ?: 0 }
            .distinctUntilChanged()
            .collect(scope) { level ->
                sessionIndependentDspManager?.setFourBandEq(1, level)
            }

        dataStore.data
            .map { it[intPreferencesKey("four_band_eq_level_2")] ?: 0 }
            .distinctUntilChanged()
            .collect(scope) { level ->
                sessionIndependentDspManager?.setFourBandEq(2, level)
            }

        dataStore.data
            .map { it[intPreferencesKey("four_band_eq_level_3")] ?: 0 }
            .distinctUntilChanged()
            .collect(scope) { level ->
                sessionIndependentDspManager?.setFourBandEq(3, level)
            }





        // Observe Native DSP Compressor settings (only effective in Native mode)
        dataStore.data
            .map { it[NativeDSPCompressorEnabledKey] ?: false }
            .distinctUntilChanged()
            .collect(scope) { enabled ->
                hybridDSPManager?.setCompressorEnabled(enabled)
            }

        dataStore.data
            .map { it[NativeDSPCompressorThresholdKey] ?: -6.0f }
            .distinctUntilChanged()
            .collect(scope) { threshold ->
                hybridDSPManager?.setCompressorThreshold(threshold)
            }

        dataStore.data
            .map { it[NativeDSPCompressorRatioKey] ?: 4.0f }
            .distinctUntilChanged()
            .collect(scope) { ratio ->
                hybridDSPManager?.setCompressorRatio(ratio)
            }

        if (dataStore.get(PersistentQueueKey, true)) {
            // Set flag to prevent auto-playback during restoration
            isRestoringState = true

            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                // Convertir de vuelta al tipo de cola apropiado
                val restoredQueue = queue.toQueue()
                playQueue(
                    queue = restoredQueue,
                    playWhenReady = false, // Always restore paused - user must tap play
                )
            }
            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                automixItems.value = queue.items.map { it.toMediaItem() }
            }

            // Restaurar estado del reproductor
            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistPlayerState
                    }
                }
            }.onSuccess { playerState ->
                // Restaurar configuración del reproductor después de cargar la cola
                scope.launch {
                    delay(1000) // Esperar a que la cola se cargue

                    // Force pause BEFORE restoring anything else
                    player.pause()
                    player.playWhenReady = false

                    player.repeatMode = playerState.repeatMode
                    player.shuffleModeEnabled = playerState.shuffleModeEnabled
                    player.volume = playerState.volume

                    // Restaurar posición si sigue siendo válida
                    if (playerState.currentMediaItemIndex < player.mediaItemCount) {
                        player.seekTo(playerState.currentMediaItemIndex, playerState.currentPosition)
                    }

                    // Force pause again multiple times to ensure it stays paused
                    delay(500)
                    player.pause()
                    player.playWhenReady = false

                    delay(500)
                    player.pause()
                    player.playWhenReady = false

                    // Clear restoration flag after a delay
                    delay(1000)
                    isRestoringState = false

                    Log.d(TAG, "✅ Player restored in paused state - waiting for user to tap play")
                }
            }.onFailure {
                // Clear flag even if restoration fails
                isRestoringState = false
            }

            // Clear flag if no player state is found
            scope.launch {
                delay(3000)
                if (isRestoringState) {
                    isRestoringState = false
                }
            }
        }

        // Guardar cola periódicamente para prevenir pérdida por crash o force kill
        scope.launch {
            while (isActive) {
                delay(30.seconds)
                if (dataStore.get(PersistentQueueKey, true)) {
                    saveQueueToDisk()
                }
            }
        }

        // Guardar cola más frecuentemente cuando está reproduciendo
        scope.launch {
            while (isActive) {
                delay(10.seconds)
                if (dataStore.get(PersistentQueueKey, true) && player.isPlaying) {
                    saveQueueToDisk()
                }
            }
        }
    } // End of onCreate()

    /**
     * Override onStartCommand to handle service start requests.
     * We don't call startForeground() immediately here to avoid Android 14/15 crashes.
     * Instead, we let the Media3 notification system and our ensureForegroundService() handle it.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Capture current playback state before handling the command
        val wasPlayingBefore = player.isPlaying
        val playWhenReadyBefore = player.playWhenReady

        // CRITICAL: Check if notification permission is granted before creating custom notification
        // If permission is granted, Media3 notification is already active - DO NOT replace it!
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)
        val notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
        val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val hasNotificationPermission = notificationsEnabled && permissionGranted

        // Check if Media3 notification is already active
        val media3NotificationActive = try {
            val activeNotifications = notificationManager.activeNotifications
            activeNotifications.any { it.id == PLAYBACK_NOTIFICATION_ID }
        } catch (e: Exception) {
            false
        }

        // CRITICAL: If permission is granted and Media3 notification is active, DO NOTHING!
        // Calling startForeground() here will replace Media3's notification
        if (hasNotificationPermission && media3NotificationActive) {
            Log.d(TAG, "✅ onStartCommand: Permission granted & Media3 notification active - NOT interfering")
            isForegroundServiceStarted = true
            pendingForeground = false
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            (!isForegroundServiceStarted || Build.VERSION.SDK_INT >= 35) &&
            (player.isPlaying || intent?.action != null)) {
            // Permission not granted OR Media3 notification not active - use fallback
            try {
                // Create notification immediately
                val notification = try {
                    val builder = android.app.Notification.Builder(this, PLAYBACK_CHANNEL_ID)
                        .setContentTitle(getString(R.string.music_player))
                        .setContentText(player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Music Player")
                        .setSmallIcon(R.drawable.opentune_monochrome)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setPriority(android.app.Notification.PRIORITY_LOW)

                    // Set content intent
                    val contentIntent = PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    builder.setContentIntent(contentIntent)
                    builder.build()
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Failed to create notification, using minimal fallback", e)
                    android.app.Notification.Builder(this, PLAYBACK_CHANNEL_ID)
                        .setContentTitle(getString(R.string.music_player))
                        .setContentText("Music Player")
                        .setSmallIcon(R.drawable.opentune_monochrome)
                        .setOngoing(true)
                        .setPriority(android.app.Notification.PRIORITY_LOW)
                        .build()
                }

                // Start foreground IMMEDIATELY - this prevents ANR
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        PLAYBACK_NOTIFICATION_ID,
                        notification,
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(PLAYBACK_NOTIFICATION_ID, notification)
                }

                isForegroundServiceStarted = true
                pendingForeground = false
                Log.d(TAG, "✅ Service promoted to foreground in onStartCommand (fallback notification used)")
            } catch (e: Exception) {
                Log.e(TAG, "❌ CRITICAL: Failed to start foreground in onStartCommand", e)
                // Mark as needing foreground even if first attempt failed
                pendingForeground = true
                isForegroundServiceStarted = false
            }
        }

        // Update foreground state - app is now in foreground
        if (isAppInBackground.value) {
            isAppInBackground.value = false
            Log.d(TAG, "📱 App moved to foreground - enabling AI processing")
            // Re-enable if screen is on
            if (!isScreenOff.value) {
                backgroundAudioProcessor?.setEnabled(true)
            }
        }

        // Handle widget intents
        if (intent != null && intent.action != null) {
            when (intent.action) {
                com.musifly.android.ui.widgets.BaseMusicWidget.ACTION_PLAY_PAUSE -> {
                    if (player.isPlaying) player.pause() else player.play()
                }
                com.musifly.android.ui.widgets.BaseMusicWidget.ACTION_NEXT -> {
                    if (player.hasNextMediaItem()) {
                        player.seekToNext()
                        player.play()
                    }
                }
                com.musifly.android.ui.widgets.BaseMusicWidget.ACTION_PREV -> {
                    player.seekToPrevious()
                    player.play()
                }
                com.musifly.android.ui.widgets.BaseMusicWidget.ACTION_SHUFFLE -> {
                    player.shuffleModeEnabled = !player.shuffleModeEnabled
                }
                com.musifly.android.ui.widgets.BaseMusicWidget.ACTION_LIKE -> {
                    toggleLike()
                }
            }
            // Update widgets immediately after handling action
            WidgetUpdateHelper.updateAllWidgets(this, player)
        }

        // Let the parent MediaLibraryService handle the command
        val result = super.onStartCommand(intent, flags, startId)

        // Prevent auto-playback only when service is cold-started (not already playing)
        // This prevents auto-play when opening settings while allowing normal playback controls
        // BUT: Don't interfere if user explicitly started playback (e.g., clicked play button)
        // IMPORTANT: Only run prevention check ONCE per service lifecycle to avoid multiple
        // competing coroutines that pause/start playback in a loop
        // Prevent auto-playback when app is reopened/service restarted
        // This handles the "JIO SAAVN/Reopen" case where music auto-plays
        // We run this CHECK ALWAYS on start command unless explicitly initiated by user action
        if (!hasHandledAutoPlaybackPrevention) {
            // Cancel any previous prevention job
            autoPlaybackPreventionJob?.cancel()

            // Mark that we're handling prevention
            hasHandledAutoPlaybackPrevention = true

            autoPlaybackPreventionJob = scope.launch {
                delay(100) // Small delay to ensure player is initialized

                // Get intent info
                val wasUserInitiated = intent?.getBooleanExtra("user_initiated", false) ?: false
                val intentAction = intent?.action
                val isPlayAction = intentAction == com.musifly.android.ui.widgets.BaseMusicWidget.ACTION_PLAY_PAUSE ||
                        intentAction == "android.media.action.MEDIA_PLAY_FROM_SEARCH"

                // CRITICAL FIX: Even if restoring state, if it's not a user action, PAUSE IT.
                // This fixes "app removed -> reopen -> auto play" issue.
                // We check if player is trying to play (playWhenReady=true) without user request.
                // Use safe logic: If media is loaded and it tries to play without explicit user command -> PAUSE
                if (!wasUserInitiated && !isPlayAction && player.mediaItemCount > 0) {
                    if (player.playWhenReady) {
                        Log.d(TAG, "🛑 Preventing auto-playback on service start (not user-initiated, action=$intentAction)")
                        player.pause()
                        player.playWhenReady = false
                    }
                }

                // Reset flag after handling to allow prevention on next service start
                // We keep it true for a bit longer to prevent race conditions during startup
                delay(2000)
                if (isActive) hasHandledAutoPlaybackPrevention = false
            }
        }

        Log.d(TAG, "onStartCommand called, pendingForeground=$pendingForeground, isForegroundServiceStarted=$isForegroundServiceStarted")

        // PROFESSIONAL MUSIC PLAYER: Return START_STICKY to ensure service auto-restarts if killed
        // This prevents music from stopping when system kills the service due to memory pressure
        // MediaLibraryService may return START_NOT_STICKY, so we override it
        return android.app.Service.START_STICKY
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupAudioFocusRequest() {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                handleAudioFocusChange(focusChange)
            }
            .setAcceptsDelayedFocusGain(true)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleAudioFocusChange(focusChange: Int) {
        Log.d(TAG, "🎵 Audio focus changed: $focusChange (wasPlayingBeforeLoss: $wasPlayingBeforeAudioFocusLoss)")

        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true

                // PROFESSIONAL RECOVERY: Always restore volume first
                player.volume = playerVolume.value

                // Resume playback if it was playing before focus loss
                if (wasPlayingBeforeAudioFocusLoss) {
                    Log.d(TAG, "✅ Audio focus regained - resuming playback")
                    // Small delay to ensure audio system is ready
                    mainHandler.postDelayed({
                        if (!player.isPlaying && wasPlayingBeforeAudioFocusLoss) {
                            player.play()
                            // Ensure foreground service is active
                            ensureForegroundService()
                        }
                        // Reset flag only AFTER attempting to play
                        wasPlayingBeforeAudioFocusLoss = false
                    }, 100)
                }

                // 🔥 GLOBAL DSP (Session 0) - Always active, no recovery needed

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false

                // Permanent loss - usually means another app started playing
                // We pause and usually do NOT resume automatically
                if (player.isPlaying) {
                    Log.d(TAG, "⚠️ Audio focus lost permanently - pausing playback")
                    wasPlayingBeforeAudioFocusLoss = true // Remember we were playing, but strict logic might reset this
                    player.pause()
                } else {
                    // If already paused, don't change the flag - keeping it as is might be safer
                    // or we can allow resuming if focus comes back (rare for LOSS)
                }

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false

                // Only update flag if we are currently playing.
                // If we are already paused (e.g. from previous transient loss), don't overwrite with false!
                if (player.isPlaying) {
                    wasPlayingBeforeAudioFocusLoss = true
                    Log.d(TAG, "⏸️ Audio focus lost temporarily - pausing playback")
                    player.pause()
                }

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasAudioFocus = false

                if (player.isPlaying) {
                    wasPlayingBeforeAudioFocusLoss = true
                    Log.d(TAG, "🔉 Audio focus lost (can duck) - reducing volume")
                    player.volume = (playerVolume.value * 0.2f)
                }

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                hasAudioFocus = true

                // Restore volume
                player.volume = playerVolume.value

                if (wasPlayingBeforeAudioFocusLoss) {
                    Log.d(TAG, "✅ Transient audio focus regained - resuming playback")
                    mainHandler.postDelayed({
                        if (!player.isPlaying && wasPlayingBeforeAudioFocusLoss) {
                            player.play()
                            ensureForegroundService()
                        }
                        wasPlayingBeforeAudioFocusLoss = false
                    }, 100)
                }

                // 🔥 GLOBAL DSP (Session 0) - Always active

                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                hasAudioFocus = true

                // Restore volume to normal
                player.volume = playerVolume.value
                Log.d(TAG, "🔊 Audio focus regained (may duck) - volume restored")

                // 🔥 GLOBAL DSP (Session 0) - Always active

                lastAudioFocusState = focusChange
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun requestAudioFocus(): Boolean {
        // PROFESSIONAL AUDIO FOCUS: Only request once per session to prevent dual focus
        if (hasAudioFocus) {
            Log.d(TAG, "✅ Audio focus already granted")
            return true
        }

        audioFocusRequest?.let { request ->
            try {
                val result = audioManager.requestAudioFocus(request)
                hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                if (hasAudioFocus) {
                    Log.d(TAG, "✅ Audio focus granted successfully")
                    // Ensure foreground service when we have audio focus
                    ensureForegroundService()
                } else {
                    Log.w(TAG, "⚠️ Audio focus request denied (result: $result)")
                    // Try again after a short delay if focus was denied
                    if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED && player.isPlaying) {
                        mainHandler.postDelayed({
                            if (!hasAudioFocus && player.isPlaying) {
                                Log.d(TAG, "🔄 Retrying audio focus request...")
                                val retryResult = audioManager.requestAudioFocus(request)
                                hasAudioFocus = retryResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                                if (hasAudioFocus) {
                                    Log.d(TAG, "✅ Audio focus granted on retry")
                                    ensureForegroundService()
                                }
                            }
                        }, 500)
                    }
                }
                return hasAudioFocus
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error requesting audio focus: ${e.message}", e)
                return false
            }
        }
        Log.w(TAG, "⚠️ Audio focus request not initialized")
        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun abandonAudioFocus() {
        // PROFESSIONAL AUDIO FOCUS: Properly abandon focus on pause/stop
        if (hasAudioFocus) {
            audioFocusRequest?.let { request ->
                try {
                    audioManager.abandonAudioFocusRequest(request)
                    hasAudioFocus = false
                    Log.d(TAG, "✅ Audio focus abandoned")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error abandoning audio focus: ${e.message}", e)
                    hasAudioFocus = false // Reset flag even if abandon fails
                }
            }
        }
    }

    fun hasAudioFocusForPlayback(): Boolean {
        return hasAudioFocus
    }

    private fun waitOnNetworkError() {
        waitingForNetworkConnection.value = true
    }

    private fun skipOnError() {
        /**
         * Auto skip to the next media item on error.
         *
         * To prevent a "runaway diesel engine" scenario, force the user to take action after
         * too many errors come up too quickly. Pause to show player "stopped" state
         */
        consecutivePlaybackErr += 2
        val nextWindowIndex = player.nextMediaItemIndex

        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
            player.seekTo(nextWindowIndex, C.TIME_UNSET)
            player.prepare()
            player.play()
            return
        }

        player.pause()
        consecutivePlaybackErr = 0
    }

    private fun stopOnError() {
        player.pause()
    }

    /**
     * Ensures the service is running as a foreground service when playback starts.
     * This is called when the player transitions to PLAYING or BUFFERING state.
     *
     * On Android 14/15, we delay calling startForeground() until playback actually begins
     * to avoid ForegroundServiceStartNotAllowedException crashes.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun ensureForegroundService() {
        if (!pendingForeground || isForegroundServiceStarted) {
            return // Already in foreground or not needed
        }

        try {
            // CRITICAL: Check if notification permission is granted
            // If permission is granted, Media3 notification is already active - DO NOT replace it!
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            val notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationManager.areNotificationsEnabled()
            } else {
                true
            }
            val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            val hasNotificationPermission = notificationsEnabled && permissionGranted

            // Check if Media3 notification is already active
            val media3NotificationActive = try {
                val activeNotifications = notificationManager.activeNotifications
                activeNotifications.any { it.id == PLAYBACK_NOTIFICATION_ID }
            } catch (e: Exception) {
                false
            }

            // CRITICAL: If permission is granted and Media3 notification is active, DO NOTHING!
            // Calling startForeground() here will replace Media3's notification
            if (hasNotificationPermission && media3NotificationActive) {
                Log.d(TAG, "✅ ensureForegroundService: Permission granted & Media3 notification active - NOT interfering")
                isForegroundServiceStarted = true
                pendingForeground = false
                return
            }

            // Permission not granted OR Media3 notification not active - use fallback
            // Create a notification for foreground service
            val notification = try {
                // Try to get the current media notification
                val builder = android.app.Notification.Builder(this, PLAYBACK_CHANNEL_ID)
                    .setContentTitle(getString(R.string.music_player))
                    .setContentText(player.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Playing...")
                    .setSmallIcon(R.drawable.opentune_monochrome)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)

                // Set content intent to open the app
                val contentIntent = PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
                builder.setContentIntent(contentIntent)

                builder.build()
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Failed to create notification, using fallback", e)
                android.app.Notification.Builder(this, PLAYBACK_CHANNEL_ID)
                    .setContentTitle(getString(R.string.music_player))
                    .setContentText("Playing...")
                    .setSmallIcon(R.drawable.opentune_monochrome)
                    .setOngoing(true)
                    .build()
            }

            // Start foreground with appropriate service type
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    PLAYBACK_NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(PLAYBACK_NOTIFICATION_ID, notification)
            }

            isForegroundServiceStarted = true
            pendingForeground = false
            Log.d(TAG, "✅ Service promoted to foreground successfully (fallback notification)")
        } catch (e: Exception) {
            // If we still get an exception, log it but don't crash
            Log.e(TAG, "❌ Failed to promote service to foreground", e)
            reportException(e)
            // Reset flags to try again later
            pendingForeground = true
            isForegroundServiceStarted = false
        }
    }


    /**
     * Reset retry counters - called when user manually retries
     * This ensures user-initiated retries get full retry attempts
     */
    fun resetRetryCounters() {
        automaticRetryCount = 0
        lastRetryErrorCategory = null
        lastRetryMediaId = null
        Log.d(TAG, "🔄 Retry counters reset (user-initiated retry)")
    }

    /**
     * Retry playback after network error - attempts to replay current media item
     * Checks for cached content first before attempting network retry
     */
    fun retryPlayback() {
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem == null) {
            Log.w(TAG, "⚠️ Cannot retry: no current media item")
            return
        }

        val mediaId = currentMediaItem.mediaId ?: return
        val currentPosition = player.currentPosition

        Log.d(TAG, "🔄 Retrying playback for: $mediaId at position: $currentPosition")

        scope.launch(Dispatchers.IO) {
            try {
                // BLOOMEE-STYLE: Check for downloaded file FIRST (priority over cache)
                val downloadedFile = try {
                    getDownloadedFile(mediaId)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error checking downloaded file: ${e.message}")
                    null
                }

                if (downloadedFile != null && downloadedFile.exists()) {
                    Log.d(TAG, "✅ Found downloaded file for offline playback: ${downloadedFile.absolutePath}")

                    // Reload media item with file URI
                    withContext(Dispatchers.Main) {
                        try {
                            val currentItem = player.currentMediaItem
                            if (currentItem != null) {
                                val newMediaItem = MediaItem.Builder()
                                    .setMediaId(mediaId)
                                    .setUri(android.net.Uri.fromFile(downloadedFile))
                                    .apply {
                                        currentItem.mediaMetadata?.let {
                                            setMediaMetadata(it)
                                        }
                                    }
                                    .build()

                                // Replace current item at same index
                                val currentIndex = player.currentMediaItemIndex
                                val timeline = player.currentTimeline

                                if (currentIndex >= 0 && currentIndex < timeline.windowCount) {
                                    // Update media item in player
                                    val mediaItems = player.mediaItems.toMutableList()
                                    if (currentIndex < mediaItems.size) {
                                        mediaItems[currentIndex] = newMediaItem
                                        player.replaceMediaItems(0, mediaItems.size, mediaItems)
                                        player.seekTo(currentIndex, currentPosition)
                                        player.prepare()
                                        player.play()

                                        Log.d(TAG, "✅ Reloaded downloaded file: ${downloadedFile.name}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error reloading downloaded file: ${e.message}", e)
                        }
                    }
                    return@launch
                }

                // Check cache if no downloaded file found
                val isCachedInDownloadCache = try {
                    val cachedLength = downloadCache.getCachedLength(mediaId, 0, Long.MAX_VALUE)
                    val isComplete = cachedLength > 0
                    if (isComplete) {
                        Log.d(TAG, "✅ Download cache has content: $mediaId (length: $cachedLength)")
                    }
                    isComplete
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error checking download cache: ${e.message}")
                    false
                }

                val isCachedInPlayerCache = try {
                    // Require minimum 1KB to avoid using obviously corrupted/incomplete cache
                    val MIN_CACHE_SIZE = 1024L // 1KB minimum
                    val cachedLength = playerCache.getCachedLength(mediaId, 0, CHUNK_LENGTH)
                    val hasContent = cachedLength >= MIN_CACHE_SIZE
                    if (hasContent) {
                        Log.d(TAG, "✅ Player cache has content: $mediaId (length: $cachedLength)")
                    } else if (cachedLength > 0) {
                        Log.w(TAG, "⚠️ Player cache too small (${cachedLength} bytes), likely corrupted/incomplete: $mediaId")
                    }
                    hasContent
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error checking player cache: ${e.message}")
                    false
                }

                val isCached = isCachedInDownloadCache || isCachedInPlayerCache

                if (isCached) {
                    Log.d(TAG, "✅ Found cached content for offline playback - using cache (downloadCache: $isCachedInDownloadCache, playerCache: $isCachedInPlayerCache)")
                } else {
                    Log.d(TAG, "📡 No cache found - will attempt network retry")
                    // Clear error cache if it's a network error to force fresh fetch
                    songUrlCache.remove(mediaId)
                }

                // If cached, force ExoPlayer to reload from cache WITHOUT network call
                // If not cached, try network refresh
                withContext(Dispatchers.Main) {
                    try {
                        if (isCached) {
                            // FORCE cache usage: Reload media item directly from cache
                            // Don't call refreshCurrentStream - it tries network
                            // Instead, just reload the current media item - CacheDataSource will use cache
                            Log.d(TAG, "🔄 Force reloading from cache for: $mediaId (offline mode)")

                            // Get current media item and reload it
                            val currentItem = player.currentMediaItem
                            if (currentItem != null) {
                                // Create new MediaItem with mediaId - CacheDataSource will resolve from cache
                                val newMediaItem = MediaItem.Builder()
                                    .setMediaId(mediaId)
                                    .setUri(mediaId) // Use mediaId - DataSource resolver will check cache first
                                    .setCustomCacheKey(mediaId) // Ensure cache key matches
                                    .apply {
                                        // Preserve metadata
                                        currentItem.mediaMetadata?.let {
                                            setMediaMetadata(it)
                                        }
                                    }
                                    .build()

                                // Replace current item and reload
                                val currentIndex = player.currentMediaItemIndex
                                val timeline = player.currentTimeline

                                if (currentIndex >= 0 && currentIndex < timeline.windowCount) {
                                    // Update the media item in the timeline
                                    // For simplicity, just reload the player
                                    player.prepare()

                                    // Seek to position if needed
                                    if (currentPosition > 0) {
                                        player.seekTo(currentPosition)
                                    }

                                    // Play
                                    player.play()

                                    Log.d(TAG, "✅ Cache reload initiated for: $mediaId (position: $currentPosition)")
                                } else {
                                    Log.w(TAG, "⚠️ Invalid media index: $currentIndex")
                                }
                            }
                        } else {
                            // No cache - try network refresh
                            Log.d(TAG, "🔄 Attempting network refresh for: $mediaId")

                            // Clear error cache to force fresh fetch
                            songUrlCache.remove(mediaId)

                            // Try to refresh stream from network
                            refreshCurrentStream(mediaId, currentPosition)

                            Log.d(TAG, "✅ Network retry initiated for: $mediaId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error during retry playback: ${e.message}", e)
                        // Show user-friendly error
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(
                                this@MusicService,
                                if (isCached) "Error playing cached file" else "Network error - no connection",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in retry playback: ${e.message}", e)
            }
        }
    }

    // Duplicate restartAudioSession removed


    /**
     * Recover from stuck playback by switching streaming engine
     */
    private fun recoverFromStuckPlayback() {
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem == null) return

        val mediaId = currentMediaItem.mediaId ?: return
        val currentPosition = player.currentPosition

        Log.d(TAG, "🔄 Attempting to recover stuck playback for: $mediaId at position: $currentPosition")

        // Try switching streaming engine if we haven't exceeded max attempts
        if (engineSwitchAttempts < MAX_ENGINE_SWITCH_ATTEMPTS) {
            switchStreamingEngine(mediaId, currentPosition)
        } else {
            // If switching failed, try refreshing current engine
            refreshCurrentStream(mediaId, currentPosition)
        }
    }

    /**
     * PUBLIC API: Switch to a specific streaming engine (called by user)
     *
     * @param targetEngine "YouTube", "YouTube Music", or "JioSaavn"
     * @return true if switch was successful, false otherwise
     */
    fun switchToStreamingEngine(targetEngine: String): Boolean {
        val currentItem = player.currentMediaItem ?: return false
        val mediaId = currentItem.mediaId ?: return false
        val currentPosition = player.currentPosition

        return try {
            scope.launch(Dispatchers.IO) {
                switchToEngineInternal(mediaId, currentPosition, targetEngine)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to switch to $targetEngine", e)
            false
        }
    }

    /**
     * Get available streaming engines for current media item
     */
    fun getAvailableEngines(): List<String> {
        val currentItem = player.currentMediaItem ?: return emptyList()
        val mediaId = currentItem.mediaId ?: return emptyList()

        val available = mutableListOf<String>()

        // Check if it's a YouTube video (can use YouTube or YouTube Music)
        if (!mediaId.startsWith("saavn:") && !mediaId.startsWith("local_")) {
            available.add("YouTube")
            available.add("YouTube Music")
        }

        // Check if it's a JioSaavn song
        if (mediaId.startsWith("saavn:")) {
            available.add("JioSaavn")
        }

        return available
    }

    /**
     * Internal function to switch to a specific engine
     */
    private suspend fun switchToEngineInternal(mediaId: String, currentPosition: Long, targetEngine: String) {
        try {
            Log.d(TAG, "🔄 User requested switch to $targetEngine for $mediaId")

            var streamUrl: String? = null
            var switchedEngine: String? = null

            when (targetEngine) {
                "YouTube Music", "NewPipe" -> {
                    try {
                        val streamInfo = NewPipeAudioStreamExtractor
                            .getBestAudioStream("https://youtube.com/watch?v=$mediaId")
                            .getOrThrow()
                        streamUrl = streamInfo.url
                        switchedEngine = "YouTube Music"
                        Log.d(TAG, "✅ YouTube Music (NewPipe) stream obtained")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ YouTube Music switch failed", e)
                    }
                }

                "YouTube" -> {
                    try {
                        val playbackData = YTPlayerUtils.playerResponseForPlayback(
                            mediaId,
                            audioQuality = audioQuality,
                            connectivityManager = connectivityManager,
                        ).getOrThrow()
                        streamUrl = playbackData.streamUrl
                        switchedEngine = "YouTube"
                        Log.d(TAG, "✅ YouTube stream obtained")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ YouTube switch failed", e)
                    }
                }

                "JioSaavn" -> {
                    if (mediaId.startsWith("saavn:")) {
                        try {
                            val songId = mediaId.removePrefix("saavn:")
                            val streamingManager = StreamingEngineManager.getInstance()
                            streamUrl = streamingManager.getSaavnStreamUrl(songId)
                            if (streamUrl != null && streamUrl.isNotEmpty()) {
                                switchedEngine = "JioSaavn"
                                Log.d(TAG, "✅ JioSaavn stream obtained")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ JioSaavn switch failed", e)
                        }
                    }
                }
            }

            if (streamUrl != null && switchedEngine != null) {
                // Update engine tracking
                activeStreamingEngine = switchedEngine
                engineSwitchAttempts++

                // Cache the new URL
                val expirationTime = when (switchedEngine) {
                    "YouTube Music" -> System.currentTimeMillis() + (6 * 60 * 60 * 1000L) // 6 hours
                    "YouTube" -> {
                        // For YouTube, we'd need to get expiration from playbackData
                        // Using safe default of 1 hour
                        System.currentTimeMillis() + (60 * 60 * 1000L)
                    }
                    "JioSaavn" -> System.currentTimeMillis() + (6 * 60 * 60 * 1000L) // 6 hours
                    else -> System.currentTimeMillis() + (60 * 60 * 1000L) // 1 hour default
                }

                songUrlCache[mediaId] = streamUrl to expirationTime

                // Reload the media item
                withContext(Dispatchers.Main) {
                    val currentItem = player.currentMediaItem
                    val newMediaItemBuilder = MediaItem.Builder()
                        .setMediaId(mediaId)
                        .setUri(mediaId)
                        .setCustomCacheKey(mediaId)

                    currentItem?.let { item ->
                        item.localConfiguration?.tag?.let { tag ->
                            newMediaItemBuilder.setTag(tag)
                        }
                        item.mediaMetadata?.let { metadata ->
                            newMediaItemBuilder.setMediaMetadata(metadata)
                        }
                    }

                    val newMediaItem = newMediaItemBuilder.build()

                    player.replaceMediaItem(player.currentMediaItemIndex, newMediaItem)
                    player.prepare()

                    // Seek to approximate position
                    if (currentPosition > 0 && player.duration > 0) {
                        val seekPosition = currentPosition.coerceAtMost(player.duration - 1000).coerceAtLeast(0)
                        player.seekTo(seekPosition)
                    }
                    player.play()

                    // Show toast notification
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        // Toast removed - stream engine notification disabled
                    }

                    Log.d(TAG, "✅ Successfully switched to $switchedEngine")
                }
            } else {
                Log.e(TAG, "❌ Failed to switch to $targetEngine: Could not obtain stream URL")
                withContext(Dispatchers.Main) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(
                            this@MusicService,
                            "⚠️ Failed to switch to $targetEngine",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error switching to $targetEngine", e)
            withContext(Dispatchers.Main) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        this@MusicService,
                        "❌ Error: ${e.message?.take(50)}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Switch to alternative streaming engine (Hybrid approach) - PRIVATE for automatic retry
     */
    private fun switchStreamingEngine(mediaId: String, currentPosition: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                val currentUseNewPipe = dataStore.data.map { preferences ->
                    preferences[UseNewPipeStreamingKey] ?: false
                }.first()

                // Determine which engine to switch to
                val targetEngine = if (currentUseNewPipe) "YouTube" else "NewPipe"
                val wasUsingNewPipe = activeStreamingEngine == "NewPipe" || (activeStreamingEngine == null && currentUseNewPipe)

                Log.d(TAG, "🔄 Switching streaming engine: ${if (wasUsingNewPipe) "NewPipe" else "YouTube"} → $targetEngine")

                var streamUrl: String? = null
                var switchedEngine: String? = null

                // Try the alternative engine
                if (targetEngine == "NewPipe") {
                    try {
                        val streamInfo = NewPipeAudioStreamExtractor
                            .getBestAudioStream("https://youtube.com/watch?v=$mediaId")
                            .getOrThrow()
                        streamUrl = streamInfo.url
                        switchedEngine = "NewPipe"
                        Log.d(TAG, "✅ NewPipe stream obtained for switch")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ NewPipe switch failed, trying YouTube", e)
                    }
                }

                if (streamUrl == null) {
                    try {
                        val playbackData = YTPlayerUtils.playerResponseForPlayback(
                            mediaId,
                            audioQuality = audioQuality,
                            connectivityManager = connectivityManager,
                        ).getOrThrow()
                        streamUrl = playbackData.streamUrl
                        switchedEngine = "YouTube"
                        Log.d(TAG, "✅ YouTube stream obtained for switch")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ YouTube switch also failed", e)
                    }
                }

                if (streamUrl != null && switchedEngine != null) {
                    engineSwitchAttempts++
                    activeStreamingEngine = switchedEngine

                    // Update URL cache with new engine's stream URL
                    val expirationTime = if (switchedEngine == "NewPipe") {
                        System.currentTimeMillis() + (6 * 60 * 60 * 1000L) // 6 hours for NewPipe
                    } else {
                        // For YouTube, we need to get expiration from playbackData
                        // For now, use a safe default (1 hour)
                        System.currentTimeMillis() + (60 * 60 * 1000L)
                    }
                    songUrlCache[mediaId] = streamUrl to expirationTime

                    // Reload the media item using mediaId (DataSourceFactory will use cached URL)
                    withContext(Dispatchers.Main) {
                        val currentItem = player.currentMediaItem
                        // Preserve all metadata from the original MediaItem
                        val newMediaItemBuilder = MediaItem.Builder()
                            .setMediaId(mediaId)
                            .setUri(mediaId) // Use mediaId, not streamUrl - DataSourceFactory resolves it
                            .setCustomCacheKey(mediaId)

                        // Preserve metadata if available
                        currentItem?.let { item ->
                            item.localConfiguration?.tag?.let { tag ->
                                newMediaItemBuilder.setTag(tag)
                            }
                            item.mediaMetadata?.let { metadata ->
                                newMediaItemBuilder.setMediaMetadata(metadata)
                            }
                        }

                        val newMediaItem = newMediaItemBuilder.build()

                        // Update cache BEFORE replacing MediaItem to ensure DataSourceFactory uses new URL
                        // This ensures the new engine's URL is available when ExoPlayer resolves the data source
                        songUrlCache[mediaId] = streamUrl to expirationTime
                        Log.d(TAG, "✅ Cache updated for engine switch: $switchedEngine, URL cached for mediaId: $mediaId")

                        player.replaceMediaItem(player.currentMediaItemIndex, newMediaItem)
                        player.prepare()
                        // Seek to approximate position
                        if (currentPosition > 0 && player.duration > 0) {
                            val seekPosition = currentPosition.coerceAtMost(player.duration - 1000).coerceAtLeast(0)
                            player.seekTo(seekPosition)
                        }
                        player.play()

                        // Show professional toast notification
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            // Toast removed - stream engine notification disabled
                        }

                        Log.d(TAG, "✅ Successfully switched to $switchedEngine")
                    }
                } else {
                    // If switch failed, try refreshing current engine
                    refreshCurrentStream(mediaId, currentPosition)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to switch streaming engine", e)
                refreshCurrentStream(mediaId, currentPosition)
            }
        }
    }

    /**
     * ENHANCED: Refresh current streaming engine without switching
     *
     * This function fetches a fresh stream URL when:
     * - URL expires (YouTube signed URLs expire)
     * - Stream becomes unavailable
     * - Error recovery requires fresh URL
     *
     * URL EXPIRATION HANDLING:
     * - YouTube URLs have expiration tokens (expires in streamExpiresInSeconds)
     * - JioSaavn URLs typically last 6 hours
     * - URLs are cached with expiration times to avoid unnecessary refreshes
     */
    private fun refreshCurrentStream(mediaId: String, currentPosition: Long) {
        scope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔄 Refreshing stream URL for $mediaId (likely expired or invalid)")

                // Clear old cached URL to force fresh fetch
                songUrlCache.remove(mediaId)

                val useNewPipeStreaming = dataStore.data.map { preferences ->
                    preferences[UseNewPipeStreamingKey] ?: false
                }.first()

                var streamUrl: String? = null
                var expirationTime: Long = 0L

                if (useNewPipeStreaming) {
                    try {
                        val streamInfo = NewPipeAudioStreamExtractor
                            .getBestAudioStream("https://youtube.com/watch?v=$mediaId")
                            .getOrThrow()
                        streamUrl = streamInfo.url
                        activeStreamingEngine = "NewPipe"
                        // NewPipe URLs typically don't expire, but cache for 6 hours
                        expirationTime = System.currentTimeMillis() + (6 * 60 * 60 * 1000L)
                        Log.d(TAG, "✅ NewPipe stream refreshed for $mediaId")
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ NewPipe refresh failed, trying YouTube", e)
                    }
                }

                if (streamUrl == null) {
                    try {
                        val playbackData = YTPlayerUtils.playerResponseForPlayback(
                            mediaId,
                            audioQuality = audioQuality,
                            connectivityManager = connectivityManager,
                        ).getOrThrow()
                        streamUrl = playbackData.streamUrl
                        // YouTube URLs expire - use actual expiration time from response
                        // Add 5-minute safety margin to avoid using URLs about to expire
                        val safetyMargin = 5 * 60 * 1000L // 5 minutes
                        expirationTime = System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L) - safetyMargin
                        songUrlCache[mediaId] = streamUrl to expirationTime
                        activeStreamingEngine = "YouTube"
                        Log.d(TAG, "✅ YouTube stream refreshed for $mediaId (expires in ${playbackData.streamExpiresInSeconds}s)")
                    } catch (ytException: Exception) {
                        Log.e(TAG, "❌ YouTube refresh failed, trying JioSaavn fallback", ytException)

                        // Try JioSaavn as final fallback
                        val saavnLogTag = "JioSaavn_Fallback"
                        try {
                            Log.d(TAG, "🔄 Attempting JioSaavn fallback for video: $mediaId")

                            // Get song metadata from YouTube (we need title and artist for search)
                            val songMetadata = try {
                                YTPlayerUtils.playerResponseForMetadata(mediaId).getOrNull()
                            } catch (e: Exception) {
                                null
                            }

                            if (songMetadata != null) {
                                val title = songMetadata.videoDetails?.title ?: ""
                                val artist = songMetadata.videoDetails?.author ?: ""
                                val searchQuery = "$title $artist".trim()

                                if (searchQuery.isNotEmpty()) {
                                    Log.d(saavnLogTag, "🔍 Searching JioSaavn for: $searchQuery")

                                    // Search JioSaavn for the song
                                    val saavnAPI = com.musifly.android.streamingengines.saavn.SaavnAPI()
                                    val searchResults: Map<String, Any> = saavnAPI.fetchSearchResults(searchQuery, count = 5)

                                    @Suppress("UNCHECKED_CAST")
                                    val songs = searchResults["songs"] as? List<Map<String, Any>>
                                    if (!songs.isNullOrEmpty()) {
                                        val firstSong = songs[0]
                                        val saavnSongId = firstSong["id"] as? String

                                        if (saavnSongId != null) {
                                            Log.d(saavnLogTag, "✅ Found song on JioSaavn: ${firstSong["title"]} (ID: $saavnSongId)")

                                            // Get stream URL from JioSaavn
                                            val saavnStreamUrl: String? = saavnAPI.getSongUrl(saavnSongId)

                                            if (!saavnStreamUrl.isNullOrEmpty()) {
                                                Log.d(saavnLogTag, "✅ JioSaavn stream URL obtained: ${saavnStreamUrl.take(80)}...")
                                                Log.d(TAG, "✅ Automatic JioSaavn fallback successful - playback will continue")

                                                streamUrl = saavnStreamUrl
                                                activeStreamingEngine = "JioSaavn"
                                                expirationTime = System.currentTimeMillis() + (6 * 60 * 60 * 1000L)
                                                Log.d(TAG, "🔍 Detected stream engine: JioSaavn (automatic fallback)")
                                            } else {
                                                Log.w(saavnLogTag, "⚠️ JioSaavn returned empty stream URL")
                                            }
                                        } else {
                                            Log.w(saavnLogTag, "⚠️ No song ID found in JioSaavn search results")
                                        }
                                    } else {
                                        Log.w(saavnLogTag, "⚠️ No results found on JioSaavn for: $searchQuery")
                                    }
                                } else {
                                    Log.w(saavnLogTag, "⚠️ Empty search query, cannot search JioSaavn")
                                }
                            } else {
                                Log.w(saavnLogTag, "⚠️ Could not get song metadata for JioSaavn search")
                            }
                        } catch (e: Exception) {
                            Log.e(saavnLogTag, "❌ JioSaavn fallback failed: ${e.message}", e)
                        }

                        // If JioSaavn also failed, rethrow the original YouTube exception
                        if (streamUrl == null) {
                            Log.e(TAG, "❌ All streaming sources failed (YouTube, NewPipe, JioSaavn) during refresh")
                            throw ytException
                        }
                    }
                } else {
                    // Update cache with fresh NewPipe URL
                    songUrlCache[mediaId] = streamUrl to expirationTime
                }

                // Reload the media item using mediaId (DataSourceFactory will use cached URL)
                withContext(Dispatchers.Main) {
                    val currentItem = player.currentMediaItem
                    // Preserve all metadata from the original MediaItem
                    val newMediaItemBuilder = MediaItem.Builder()
                        .setMediaId(mediaId)
                        .setUri(mediaId) // Use mediaId, not streamUrl - DataSourceFactory resolves it
                        .setCustomCacheKey(mediaId)

                    // Preserve metadata if available
                    currentItem?.let { item ->
                        item.localConfiguration?.tag?.let { tag ->
                            newMediaItemBuilder.setTag(tag)
                        }
                        item.mediaMetadata?.let { metadata ->
                            newMediaItemBuilder.setMediaMetadata(metadata)
                        }
                    }

                    val newMediaItem = newMediaItemBuilder.build()

                    // Update cache BEFORE replacing MediaItem to ensure DataSourceFactory uses new URL
                    if (streamUrl != null) {
                        val expirationTime = if (activeStreamingEngine == "NewPipe") {
                            System.currentTimeMillis() + (6 * 60 * 60 * 1000L)
                        } else {
                            System.currentTimeMillis() + (60 * 60 * 1000L)
                        }
                        songUrlCache[mediaId] = streamUrl to expirationTime
                        Log.d(TAG, "✅ Cache updated for stream refresh, URL cached for mediaId: $mediaId")
                    }

                    player.replaceMediaItem(player.currentMediaItemIndex, newMediaItem)
                    player.prepare()
                    // Seek to approximate position
                    if (currentPosition > 0 && player.duration > 0) {
                        val seekPosition = currentPosition.coerceAtMost(player.duration - 1000).coerceAtLeast(0)
                        player.seekTo(seekPosition)
                    }
                    player.play()
                    Log.d(TAG, "✅ Playback recovered successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to recover from stuck playback", e)
                // If recovery fails, try to skip to next
                withContext(Dispatchers.Main) {
                    if (player.hasNextMediaItem()) {
                        player.seekToNext()
                        player.play()
                    } else {
                        player.pause()
                    }
                }
            }
        }
    }

    /**
     * Handle YouTube unsupported format errors (e.g., ParserException with atom length > 2147483647)
     * Tries to resolve a safer audio-only stream as fallback
     */
    private fun handleYouTubeUnsupportedFormat(mediaId: String, error: PlaybackException) {
        if (ytFallbackTried.contains(mediaId)) {
            // Already tried fallback (low quality), now try switching engine if possible
            if (engineSwitchAttempts < MAX_ENGINE_SWITCH_ATTEMPTS) {
                Log.d(TAG, "🔄 YouTube fallback failed, trying engine switch for $mediaId")
                // Remove from fallback tried to allow retry logic in new engine if needed
                ytFallbackTried.remove(mediaId) 
                
                val currentPosition = player.currentPosition
                switchStreamingEngine(mediaId, currentPosition)
                return
            }

            // All fallbacks exhausted (Low Quality & Engine Switch)
            Log.e("ENGINE_DEBUG", "All fallbacks exhausted for $mediaId, showing error to user.")
            ytFallbackTried.remove(mediaId) // Clear for future attempts
            
            // Show a less intrusive toast when auto-skipping
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    this@MusicService,
                    "⚠️ Unplayable track - skipping...",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            
            // Skip to next track
            if (player.hasNextMediaItem()) {
                player.seekToNext()
                player.play()
            } else {
                player.stop()
            }
            return
        }

        ytFallbackTried.add(mediaId)
        Log.e("ENGINE_DEBUG", "Unsupported YouTube MP4 format, trying fallback for $mediaId")

        scope.launch(Dispatchers.IO) {
            try {
                // Clear the problematic URL from cache
                songUrlCache.remove(mediaId)

                // Try to get a safer YouTube audio stream (Low Quality)
                val playbackData = YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = com.musifly.android.constants.AudioQuality.LOW, 
                    connectivityManager = connectivityManager,
                ).getOrNull()

                if (playbackData == null || playbackData.streamUrl.isEmpty()) {
                    Log.e("ENGINE_DEBUG", "Failed to get safe YT audio stream for $mediaId - trying engine switch")
                    
                    // If simple retry fails, try switching engine immediately
                    if (engineSwitchAttempts < MAX_ENGINE_SWITCH_ATTEMPTS) {
                        launch(Dispatchers.Main) {
                            switchStreamingEngine(mediaId, player.currentPosition)
                        }
                        // Don't remove from set yet, let switch logic handle it
                        return@launch
                    }

                    // If we can't switch, then skip
                    withContext(Dispatchers.Main) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(
                                this@MusicService,
                                "⚠️ Track unavailable - skipping...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        if (player.hasNextMediaItem()) {
                            player.seekToNext()
                            player.play()
                        } else {
                            player.stop()
                        }
                    }
                    ytFallbackTried.remove(mediaId)
                    return@launch
                }

                val safeUrl = playbackData.streamUrl
                Log.d("ENGINE_DEBUG", "Retrying with safe YT audio-only stream: ${safeUrl.take(80)}...")

                // Cache the new safe URL
                val expirationTime = System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L) - (5 * 60 * 1000L) 
                songUrlCache[mediaId] = safeUrl to expirationTime
                activeStreamingEngine = "YouTube"

                // Reload the media item with new stream URL
                withContext(Dispatchers.Main) {
                    val currentItem = player.currentMediaItem
                    val newMediaItemBuilder = MediaItem.Builder()
                        .setMediaId(mediaId)
                        .setUri(mediaId) 
                        .setCustomCacheKey(mediaId)

                    currentItem?.let { item ->
                        item.localConfiguration?.tag?.let { tag ->
                            newMediaItemBuilder.setTag(tag)
                        }
                        item.mediaMetadata?.let { metadata ->
                            newMediaItemBuilder.setMediaMetadata(metadata)
                        }
                    }

                    val newMediaItem = newMediaItemBuilder.build()
                    player.replaceMediaItem(player.currentMediaItemIndex, newMediaItem)
                    player.prepare()
                    player.play()

                    Log.d("ENGINE_DEBUG", "✅ Successfully retried with safe YT stream")
                    // Suppress "Retrying" toast to be less spammy, or keep it short
                }
            } catch (e: Exception) {
                Log.e("ENGINE_DEBUG", "Failed to resolve safe YT stream: ${e.message}", e)
                // Try switching engine on exception too
                 if (engineSwitchAttempts < MAX_ENGINE_SWITCH_ATTEMPTS) {
                     withContext(Dispatchers.Main) {
                        switchStreamingEngine(mediaId, player.currentPosition)
                     }
                 } else {
                    withContext(Dispatchers.Main) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(
                                this@MusicService,
                                "⚠️ Playback failed - skipping...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        if (player.hasNextMediaItem()) {
                            player.seekToNext()
                            player.play()
                        } else {
                            player.stop()
                        }
                    }
                    ytFallbackTried.remove(mediaId)
                 }
            }
        }
    }

    // Throttle notification updates to prevent audio thread blocking
    private var lastNotificationUpdateTime = 0L
    private val notificationUpdateThrottleMs = 200L // Max once per 200ms

    private fun updateNotification() {
        // Throttle updates to prevent blocking audio thread
        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdateTime < notificationUpdateThrottleMs) {
            return // Skip update - too soon
        }
        lastNotificationUpdateTime = now

        // Ensure this runs off audio thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread - safe to update
            updateNotificationInternal()
        } else {
            // Off main thread - post to main thread
            mainHandler.post { updateNotificationInternal() }
        }
    }

    private fun updateNotificationInternal() {
        mediaSession.setCustomLayout(
            listOf(
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            if (currentSong.value?.song?.liked ==
                                true
                            ) {
                                R.string.action_remove_like
                            } else {
                                R.string.action_like
                            },
                        ),
                    )
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            },
                        ),
                    ).setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        },
                    ).setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
            ),
        )
    }

    private suspend fun recoverSong(
        mediaId: String,
        playbackData: YTPlayerUtils.PlaybackData? = null
    ) {
        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return
        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (playbackData?.videoDetails ?: YTPlayerUtils.playerResponseForMetadata(mediaId)
                .getOrNull()?.videoDetails)?.lengthSeconds?.toInt()
            ?: -1
        database.query {
            if (song == null) insert(mediaMetadata.copy(duration = duration))
            else if (song.song.duration == -1) update(song.song.copy(duration = duration))
        }
        if (!database.hasRelatedSongs(mediaId)) {
            val relatedEndpoint =
                YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint
                    ?: return
            val relatedPage = YouTube.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach(::insert)
                    .map {
                        RelatedSongMap(
                            songId = mediaId,
                            relatedSongId = it.id
                        )
                    }
                    .forEach(::insert)
            }
        }
    }

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
    ) {
        // CRITICAL: Mark that this is user-initiated playback
        // This prevents auto-playback prevention logic from interfering
        hasHandledAutoPlaybackPrevention = true

        // PROFESSIONAL PLAYBACK CONTROL: Cancel any previous playback job to prevent overlap
        synchronized(playbackLock) {
            activePlaybackJob?.cancel()
            activePlaybackJob = null
        }

        // CRITICAL: Ensure we're using the current source for playback
        // Clear URL cache to prevent old source URLs from being reused
        val currentSource = StreamSourceManager.currentSource
        Log.d(TAG, "🎵 playQueue called - Current source: ${currentSource.name}")
        Log.d("PlaybackSourceCheck", "TOPBAR_SELECTED = ${currentSource.name}")

        // PROFESSIONAL PLAYER CLEANUP: Clean reset without full release (more efficient)
        // Full release/recreate only when switching sources or on errors to prevent distortion
        synchronized(playbackLock) {
            // Cancel any active playback job to prevent overlapping
            activePlaybackJob?.cancel()

            try {
                Log.d(TAG, "🧹 Professional player cleanup - resetting for clean playback")

                // Stop and clear everything (but don't release - reuse player for efficiency)
                if (player.playbackState != Player.STATE_IDLE) {
                    player.stop()
                }
                player.clearMediaItems()

                // Small delay to ensure cleanup completes
                kotlinx.coroutines.runBlocking {
                    kotlinx.coroutines.delay(50)
                }

                Log.d(TAG, "✅ Player reset complete - ready for ${currentSource.name} playback")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Error resetting player: ${e.message}", e)
            }
            // Fallback: just clear media items if recreation fails
            try {
                player.clearMediaItems()
            } catch (e2: Exception) {
                Log.e(TAG, "⚠️ Error clearing media items: ${e2.message}", e2)
            }
        }

        // Clear any cached URLs that might be from a different source
        clearSongUrlCache()
        activeStreamingEngine = null

        if (!scope.isActive) scope = CoroutineScope(Dispatchers.Main) + Job()
        currentQueue = queue
        queueTitle = null
        player.shuffleModeEnabled = false
        if (queue.preloadItem != null) {
            // Ensure MediaItem uses current source format
            val mediaItem = queue.preloadItem!!.toMediaItem()
            Log.d(TAG, "🎵 Preparing to play: ${mediaItem.mediaId} (source: ${currentSource.name})")
            Log.d("PlaybackSourceCheck", "MediaItem ID: ${mediaItem.mediaId}")
            Log.d("PlaybackSourceCheck", "Expected engine from mediaId: ${StreamSourceManager.detectSourceFromMediaId(mediaItem.mediaId).name}")

            // INSTANT PLAYBACK: Set media item and prepare immediately (no async delays)
            player.setMediaItem(mediaItem)
            player.prepare() // Prepare synchronously for instant start

            // Request audio focus before playing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestAudioFocus()
            }

            player.playWhenReady = playWhenReady
            // Explicitly start playback if requested
            if (playWhenReady && !isRestoringState) {
                player.play()
            }
            // Force pause during restoration
            if (isRestoringState) {
                player.pause()
                player.playWhenReady = false
            }
        }
        // PROFESSIONAL PLAYBACK CONTROL: Track and cancel previous job
        synchronized(playbackLock) {
            activePlaybackJob?.cancel()
            activePlaybackJob = scope.launch(SilentHandler) {
                val initialStatus =
                    withContext(Dispatchers.IO) {
                        queue.getInitialStatus().filterExplicit(dataStore.get(HideExplicitKey, false))
                    }
                if (queue.preloadItem != null && player.playbackState == STATE_IDLE) return@launch
                if (initialStatus.title != null) {
                    queueTitle = initialStatus.title
                }
                if (initialStatus.items.isEmpty()) return@launch
                if (queue.preloadItem != null) {
                    player.addMediaItems(
                        0,
                        initialStatus.items.subList(0, initialStatus.mediaItemIndex)
                    )
                    player.addMediaItems(
                        initialStatus.items.subList(
                            initialStatus.mediaItemIndex + 1,
                            initialStatus.items.size
                        )
                    )
                } else {
                    player.setMediaItems(
                        initialStatus.items,
                        if (initialStatus.mediaItemIndex >
                            0
                        ) {
                            initialStatus.mediaItemIndex
                        } else {
                            0
                        },
                        initialStatus.position,
                    )
                    player.prepare() // Prepare synchronously for instant start

                    // Request audio focus before playing
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        requestAudioFocus()
                    }

                    player.playWhenReady = playWhenReady
                    // Explicitly start playback if requested
                    if (playWhenReady && !isRestoringState) {
                        player.play()
                    }
                    // Force pause during restoration
                    if (isRestoringState) {
                        player.pause()
                        player.playWhenReady = false
                    }
                }
            }
        }
    }

    fun startRadioSeamlessly() {
        val currentMediaMetadata = player.currentMetadata ?: return

        // Guardar canción actual
        val currentSong = player.currentMediaItem

        // Remover otras canciones de la cola
        if (player.currentMediaItemIndex > 0) {
            player.removeMediaItems(0, player.currentMediaItemIndex)
        }
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) {
            player.removeMediaItems(player.currentMediaItemIndex + 1, player.mediaItemCount)
        }

        scope.launch(SilentHandler) {
            val radioQueue = YouTubeQueue(
                endpoint = WatchEndpoint(videoId = currentMediaMetadata.id)
            )
            val initialStatus = radioQueue.getInitialStatus()

            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }

            // Agregar canciones de radio después de la canción actual
            player.addMediaItems(initialStatus.items.drop(1))
            currentQueue = radioQueue
        }
    }

    fun getAutomixAlbum(albumId: String) {
        scope.launch(SilentHandler) {
            YouTube
                .album(albumId)
                .onSuccess {
                    getAutomix(it.album.playlistId)
                }
        }
    }

    fun getAutomix(playlistId: String) {
        if (dataStore[SimilarContent] == true &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)) {
            scope.launch(SilentHandler) {
                YouTube
                    .next(WatchEndpoint(playlistId = playlistId))
                    .onSuccess {
                        YouTube
                            .next(WatchEndpoint(playlistId = it.endpoint.playlistId))
                            .onSuccess {
                                automixItems.value =
                                    it.items.map { song ->
                                        song.toMediaItem()
                                    }
                            }
                    }
            }
        }
    }

    /**
     * Handles automatic playback of similar content when queue ends
     * Fetches "Up Next" / Radio content from YouTube and appends to queue
     */
    private fun handleAutoplay(mediaId: String?) {
        if (mediaId == null) return

        scope.launch(Dispatchers.IO) {
            try {
                // Check if Autoplay/Similar Content is enabled
                // Using the SimilarContent key which controls this feature
                val isAutoplayEnabled = dataStore.get(SimilarContent, false)

                if (!isAutoplayEnabled) {
                    return@launch
                }

                // Don't autoplay if Repeat All is on and setting forbids it
                if (player.repeatMode == REPEAT_MODE_ALL &&
                    dataStore.get(DisableLoadMoreWhenRepeatAllKey, false)) {
                    return@launch
                }

                Log.d(TAG, "🔄 Autoplay: Fetching similar content for $mediaId")

                // Fetch recommended songs (Watch Next endpoint)
                // This returns a mix based on the seed videoId
                val nextResult = YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()

                if (nextResult != null && nextResult.items.isNotEmpty()) {
                    val currentIndex = nextResult.currentIndex ?: -1

                    // Strategy: Get items that follow the current one
                    val newItemsRaw = if (currentIndex >= 0 && currentIndex < nextResult.items.size - 1) {
                        nextResult.items.drop(currentIndex + 1)
                    } else if (currentIndex == -1) {
                        nextResult.items.filter { it.id != mediaId }
                    } else {
                        emptyList()
                    }

                    val newItems = newItemsRaw.map { it.toMediaItem() }

                    if (newItems.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            // Verify we are still playing the same song and at end of queue
                            val currentItem = player.currentMediaItem
                            val isStillLast = player.currentMediaItemIndex >= player.mediaItemCount - 1

                            if (isStillLast && currentItem?.mediaId == mediaId) {
                                player.addMediaItems(newItems)
                                Log.d(TAG, "✅ Autoplay: Appended ${newItems.size} songs to queue")
                                android.widget.Toast.makeText(
                                    this@MusicService,
                                    "Autoplay: Added similar songs",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Autoplay error: ${e.message}", e)
            }
        }
    }

    fun addToQueueAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        addToQueue(listOf(item))
    }

    fun playNextAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value.toMutableList().apply {
                removeAt(position)
            }
        playNext(listOf(item))
    }

    fun clearAutomix() {
        automixItems.value = emptyList()
    }


    fun playNext(items: List<MediaItem>) {
        // Si la cola está vacía o el reproductor está inactivo, reproducir inmediatamente
        if (player.mediaItemCount == 0 || player.playbackState == STATE_IDLE) {
            player.setMediaItems(items)
            player.prepare() // Prepare synchronously for instant start

            // Request audio focus before playing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestAudioFocus()
            }

            player.play()
            return
        }

        val insertIndex = player.currentMediaItemIndex + 1
        val shuffleEnabled = player.shuffleModeEnabled

        // Insertar items inmediatamente después del item actual en el espacio de ventana/índice
        player.addMediaItems(insertIndex, items)
        player.prepare()

        if (shuffleEnabled) {
            // Reconstruir orden aleatorio para que los items recién insertados se reproduzcan a continuación
            val timeline = player.currentTimeline
            if (!timeline.isEmpty) {
                val size = timeline.windowCount
                val currentIndex = player.currentMediaItemIndex

                // Los índices recién insertados son un rango contiguo [insertIndex, insertIndex + items.size)
                val newIndices = (insertIndex until (insertIndex + items.size)).toSet()

                // Recopilar el orden de recorrido aleatorio existente excluyendo el índice actual
                val orderAfter = mutableListOf<Int>()
                var idx = currentIndex
                while (true) {
                    idx = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (idx == C.INDEX_UNSET) break
                    if (idx != currentIndex) orderAfter.add(idx)
                }

                val prevList = mutableListOf<Int>()
                var pIdx = currentIndex
                while (true) {
                    pIdx = timeline.getPreviousWindowIndex(pIdx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (pIdx == C.INDEX_UNSET) break
                    if (pIdx != currentIndex) prevList.add(pIdx)
                }
                prevList.reverse() // preservar el orden hacia adelante original

                val existingOrder = (prevList + orderAfter).filter { it != currentIndex && it !in newIndices }

                // Construir nuevo orden aleatorio: actual -> recién insertados (en orden de inserción) -> resto
                val nextBlock = (insertIndex until (insertIndex + items.size)).toList()
                val finalOrder = IntArray(size)
                var pos = 0
                finalOrder[pos++] = currentIndex
                nextBlock.forEach { if (it in 0 until size) finalOrder[pos++] = it }
                existingOrder.forEach { if (pos < size) finalOrder[pos++] = it }

                // Llenar cualquier índice faltante (seguridad) para asegurar una permutación completa
                if (pos < size) {
                    for (i in 0 until size) {
                        if (!finalOrder.contains(i)) {
                            finalOrder[pos++] = i
                            if (pos == size) break
                        }
                    }
                }

                player.setShuffleOrder(DefaultShuffleOrder(finalOrder, System.currentTimeMillis()))
            }
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        player.addMediaItems(items)
        player.prepare()
    }

    private fun toggleLibrary() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLibrary())
            }
        }
    }

    fun toggleLike() {
        try {
            // Get player information on main thread before database query
            val mediaMetadata = currentMediaMetadata.value
            val currentSongValue = currentSong.value
            val songId = currentSongValue?.song?.id ?: mediaMetadata?.id

            if (songId == null) {
                Log.e(TAG, "❌ Cannot like song: no song ID available")
                return
            }

            // Use LikesRepository to toggle like (handles both local DB and Firestore sync)
            // Use SupervisorJob to prevent crashes from propagating and ensure lifecycle safety
            scope.launch(Dispatchers.IO + kotlinx.coroutines.SupervisorJob()) {
                try {
                    Log.d(TAG, "🎵 toggleLike called for songId: $songId")

                    // Use repository's toggleLike method which handles the logic
                    // All Firestore operations are already on Dispatchers.IO in the repository
                    val result = likesRepository.toggleLike(songId)

                    result.fold(
                        onSuccess = { isLiked ->
                            // Also update main song table for UI consistency
                            database.query {
                                currentSongValue?.let { song ->
                                    update(song.song.copy(liked = isLiked, likedDate = if (isLiked) java.time.LocalDateTime.now() else null))
                                } ?: run {
                                    if (mediaMetadata != null) {
                                        val existingSong = getSongById(mediaMetadata.id)
                                        if (existingSong != null) {
                                            update(existingSong.song.copy(liked = isLiked, likedDate = if (isLiked) java.time.LocalDateTime.now() else null))
                                        } else {
                                            // Insert new song if doesn't exist
                                            insert(mediaMetadata) { songEntity ->
                                                songEntity.copy(
                                                    liked = isLiked,
                                                    likedDate = if (isLiked) java.time.LocalDateTime.now() else null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Log.d(TAG, "✅ Toggled like status for song: $songId, liked=$isLiked")

                            // CRITICAL: Update the current MediaItem's tag so widgets can read the new liked status
                            // We need to update the MediaItem but preserve playback position and state
                            withContext(Dispatchers.Main) {
                                try {
                                    val currentItem = player.currentMediaItem
                                    if (currentItem != null) {
                                        val currentTag = currentItem.localConfiguration?.tag as? com.musifly.android.models.MediaMetadata
                                        if (currentTag != null) {
                                            // Save current playback state
                                            val currentPosition = player.currentPosition
                                            val wasPlaying = player.isPlaying
                                            val currentIndex = player.currentMediaItemIndex

                                            // Create updated MediaItem with new liked status in tag
                                            val updatedTag = currentTag.copy(liked = isLiked)
                                            val updatedMediaItem = currentItem.buildUpon()
                                                .setTag(updatedTag)
                                                .build()

                                            // Replace the media item without triggering transition
                                            // Use replaceMediaItem which is designed for metadata updates
                                            player.replaceMediaItem(currentIndex, updatedMediaItem)

                                            // Restore playback state if it was playing
                                            if (wasPlaying && !player.isPlaying) {
                                                player.play()
                                            }

                                            Log.d(TAG, "✅ Updated MediaItem tag with liked=$isLiked")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "⚠️ Failed to update MediaItem tag: ${e.message}", e)
                                }

                                // Update widgets to reflect the new state immediately
                                com.musifly.android.ui.widgets.WidgetUpdateHelper.updateAllWidgets(this@MusicService, player)
                            }
                        },
                        onFailure = { e ->
                            Log.e(TAG, "❌ Failed to toggle like: $songId", e)
                            Timber.tag("MusicService").e(e, "Failed to toggle like: $songId")
                            // Fallback: Update local DB only
                            database.query {
                                currentSongValue?.let { song ->
                                    val updatedSong = song.song.localToggleLike()
                                    update(updatedSong)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                com.musifly.android.ui.widgets.WidgetUpdateHelper.updateAllWidgets(this@MusicService, player)
                            }
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in toggleLike: ${e.message}", e)
                    Timber.tag("MusicService").e(e, "Exception in toggleLike: $songId")
                    // Fallback: Update local DB only
                    database.query {
                        currentSongValue?.let { song ->
                            val updatedSong = song.song.localToggleLike()
                            update(updatedSong)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        com.musifly.android.ui.widgets.WidgetUpdateHelper.updateAllWidgets(this@MusicService, player)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in toggleLike: ${e.message}", e)
        }
    }



    // 🔥 REMOVED: setupDSPManager() - No longer needed with GLOBAL DSP (Session 0)
    // DSPManager is created once in onCreate() with session ID = 0 and persists for entire app lifecycle

    // 🔥 REMOVED: Legacy Audio Effect Management
    // AudioEffectsManager handles everything via onAudioSessionIdChanged

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {


        // REAL-TIME SAFE: Update MediaSession ONLY on track change (not position updates)
        // This is the ONLY place MediaSession should update during playback
        currentMediaMetadata.value = player.currentMetadata
        scope.launch {
            delay(200) // Small delay to ensure metadata is ready
            updateNotification() // Update notification on track change only
        }

        // PROFESSIONAL TRACK TRANSITION: Preload next track for gapless playback
        // Prefetch next track in background to eliminate start delay
        if (mediaItem != null && player.hasNextMediaItem()) {
            val nextIndex = player.nextMediaItemIndex
            if (nextIndex != C.INDEX_UNSET) {
                scope.launch(Dispatchers.IO) {
                    try {
                        // Preload next track metadata and URL
                        val nextMediaItem = player.getMediaItemAt(nextIndex)
                        val nextMediaId = nextMediaItem?.mediaId
                        if (nextMediaId != null && !nextMediaId.startsWith("local_")) {
                            // Prefetch stream URL for next track (non-blocking)
                            Log.d(TAG, "🎵 Prefetching next track: $nextMediaId")
                            // This will cache the URL so it's ready when track changes
                            // The DataSourceFactory will use cached URL instantly
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Error prefetching next track: ${e.message}")
                    }
                }
            }
        }

        // Reset engine switch attempts when song changes
        // Update widgets to reflect new song
        WidgetUpdateHelper.updateAllWidgets(this, player)

        engineSwitchAttempts = 0
        activeStreamingEngine = mediaItem?.let { detectStreamEngine(null, it.mediaId) }
        // Reset toast flag to allow showing engine switch toast for new song
        hasShownStreamingToast = false
        // Clear buffering timeout when song changes
        bufferingStartTime = 0L
        lastBufferingCheckJob?.cancel()
        lastBufferingCheckJob = null
        // Reset retry counters on new song
        automaticRetryCount = 0
        lastRetryErrorCategory = null
        lastRetryMediaId = null

        // Clear any previous errors when transitioning to new media item
        // This ensures error UI disappears when playback successfully starts
        if (mediaItem != null) {
            Log.d(TAG, "✅ Media item transition - clearing previous errors for new item: ${mediaItem.mediaId}")
        }

        // Clear background processor on track change
        try {
            backgroundAudioProcessor?.setEnabled(false)
            backgroundAudioProcessor?.setEnabled(true)
            Log.d(TAG, "🧹 Background audio processor reset on track change")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to reset background audio processor", e)
        }



        // 🔥 GLOBAL DSP (Session 0) - No need to reset on track change

        // 🎵 LOCAL FILE THUMBNAIL LOGIC
        mediaItem?.let { item ->
            val isLocal = item.localConfiguration?.uri?.let { uri ->
                uri.scheme == "file" || uri.scheme == "content" || item.mediaId?.startsWith("local_") == true
            } ?: false

            if (isLocal) {
                Log.d(TAG, "🎵 LOCAL MUSIC detected: ${item.mediaId} - Setting app icon as thumbnail")
                scope.launch(Dispatchers.IO) {
                    try {
                        val updatedMetadata = ThumbnailProvider.updateLocalFileMetadata(this@MusicService, item)

                        Log.d(TAG, "📱 App icon thumbnail ready: size=${updatedMetadata.artworkData?.size ?: 0} bytes")

                        // Update the player's current media item metadata
                        withContext(Dispatchers.Main) {
                            val currentIndex = player.currentMediaItemIndex
                            if (currentIndex != C.INDEX_UNSET && currentIndex < player.mediaItemCount) {
                                val updatedItem = item.buildUpon()
                                    .setMediaMetadata(updatedMetadata)
                                    .build()

                                // Replace the media item with updated metadata
                                player.replaceMediaItem(currentIndex, updatedItem)

                                Log.d(TAG, "🔄 Media item replaced with artwork, updating notification...")

                                // REAL-TIME SAFE: Single notification update (throttled internally)
                                updateNotification()

                                Log.d(TAG, "✅ LOCAL MUSIC thumbnail updated - App icon should be displayed")
                                Log.d(TAG, "📊 Current metadata artworkData size: ${player.mediaMetadata.artworkData?.size ?: 0}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to set app icon for local music", e)
                    }
                }
            }
        }

        // 🎵 JIOSAAVN METADATA ENRICHMENT
        mediaItem?.let { item ->
            val isJioSaavn = item.mediaId?.startsWith("saavn:") == true

            if (isJioSaavn) {
                Log.d(TAG, "🎵 JIOSAAVN SONG detected: ${item.mediaId} - Enriching metadata")
                scope.launch(Dispatchers.IO) {
                    try {
                        val songId = item.mediaId!!.removePrefix("saavn:")
                        val saavnAPI = com.musifly.android.streamingengines.saavn.SaavnAPI()
                        val songDetails = saavnAPI.getSongDetails(songId)

                        if (songDetails.containsKey("error")) {
                            Log.w(TAG, "⚠️ Failed to fetch JioSaavn song details: ${songDetails["error"]}")
                            return@launch
                        }

                        // Get current MediaMetadata tag (our custom metadata)
                        val currentTag = item.localConfiguration?.tag as? com.musifly.android.models.MediaMetadata

                        // Extract metadata from song details - use improved parsing
                        val title = (songDetails["title"] as? String)
                            ?.takeIf { it.isNotEmpty() && it != "Unknown" }
                            ?: item.mediaMetadata?.title?.toString()
                            ?: currentTag?.title
                            ?: "Unknown Song"

                        val artist = (songDetails["artist"] as? String)
                            ?.takeIf { it.isNotEmpty() && it != "Unknown Artist" }
                            ?: item.mediaMetadata?.artist?.toString()
                            ?: currentTag?.artists?.joinToString { it.name }
                            ?: "Unknown Artist"

                        val album = (songDetails["album"] as? String)
                            ?.takeIf { it.isNotEmpty() }
                            ?: item.mediaMetadata?.albumTitle?.toString()
                            ?: currentTag?.album?.title
                            ?: ""

                        // Extract duration - prefer durationSeconds if available
                        val durationSeconds = when {
                            songDetails["durationSeconds"] is Int -> (songDetails["durationSeconds"] as Int)
                            songDetails["durationSeconds"] is Long -> (songDetails["durationSeconds"] as Long).toInt()
                            songDetails["duration"] is Int -> (songDetails["duration"] as Int)
                            songDetails["duration"] is Long -> (songDetails["duration"] as Long).toInt()
                            songDetails["duration"] is String -> {
                                (songDetails["duration"] as String).toIntOrNull() ?: 0
                            }
                            else -> currentTag?.duration ?: 0
                        }

                        // Extract and upgrade image URL to highest quality
                        // Try multiple sources: imageHigh, image, and also check if image is an array
                        var rawImageUrl = (songDetails["imageHigh"] as? String)
                            ?.takeIf { it.isNotEmpty() }
                            ?: (songDetails["image"] as? String)
                                ?.takeIf { it.isNotEmpty() }
                            ?: ""

                        // If image is an array (JioSaavn format), extract from it
                        if (rawImageUrl.isEmpty() && songDetails["image"] is List<*>) {
                            val imageList = songDetails["image"] as List<*>
                            rawImageUrl = imageList
                                .filterIsInstance<Map<*, *>>()
                                .mapNotNull { item ->
                                    val quality = (item["quality"] as? String) ?: ""
                                    val link = (item["link"] as? String) ?: (item["url"] as? String) ?: ""
                                    if (link.isNotEmpty()) Pair(quality, link) else null
                                }
                                .maxByOrNull { (quality, _) ->
                                    when {
                                        quality == "500x500" -> 1000
                                        quality.contains("500") -> 500
                                        quality.contains("300") -> 300
                                        quality.contains("250") -> 250
                                        quality.contains("150") -> 150
                                        quality.contains("50") -> 50
                                        else -> 0
                                    }
                                }?.second ?: ""
                        }

                        // Fallback to current tag's thumbnailUrl if songDetails doesn't have image
                        if (rawImageUrl.isEmpty()) {
                            rawImageUrl = currentTag?.thumbnailUrl ?: ""
                            Log.d(TAG, "📸 Using thumbnailUrl from current tag: ${rawImageUrl.take(80)}...")
                        }

                        // Upgrade to highest quality (500x500)
                        val imageUrl = if (rawImageUrl.isNotEmpty()) {
                            com.musifly.android.models.upgradeJioSaavnImageUrl(rawImageUrl)
                        } else {
                            ""
                        }

                        Log.d(TAG, "📋 JioSaavn metadata: title=$title, artist=$artist, duration=${durationSeconds}s, rawImageUrl=${rawImageUrl.take(80)}..., imageUrl=${imageUrl.take(80)}...")
                        if (imageUrl.isEmpty()) {
                            Log.w(TAG, "⚠️ WARNING: No image URL found for JioSaavn song: id=$songId, title=$title")
                            Log.d(TAG, "   songDetails keys: ${songDetails.keys.joinToString()}")
                            Log.d(TAG, "   songDetails image field: ${songDetails["image"]}")
                            Log.d(TAG, "   songDetails imageHigh field: ${songDetails["imageHigh"]}")
                            Log.d(TAG, "   currentTag thumbnailUrl: ${currentTag?.thumbnailUrl?.take(80)}")
                        }

                        // Load image and convert to ByteArray for artworkData
                        var artworkBytes: ByteArray? = null
                        if (imageUrl.isNotEmpty()) {
                            try {
                                val url = URL(imageUrl)
                                val connection = url.openConnection()
                                connection.connectTimeout = 5000
                                connection.readTimeout = 5000
                                connection.connect()

                                connection.getInputStream().use { inputStream ->
                                    artworkBytes = inputStream.readBytes()
                                }

                                Log.d(TAG, "✅ Loaded artwork image: ${artworkBytes?.size ?: 0} bytes")
                            } catch (e: Exception) {
                                Log.w(TAG, "⚠️ Failed to load artwork image from URL, will use URI only", e)
                            }
                        }

                        // Update the player's current media item metadata
                        withContext(Dispatchers.Main) {
                            val currentIndex = player.currentMediaItemIndex
                            if (currentIndex != C.INDEX_UNSET && currentIndex < player.mediaItemCount) {
                                val currentMetadata = item.mediaMetadata
                                val currentTag = item.localConfiguration?.tag as? com.musifly.android.models.MediaMetadata

                                // Create updated MediaMetadata tag
                                val artistList = artist.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .takeIf { it.isNotEmpty() }
                                    ?: listOf(artist)

                                val updatedMediaMetadata = com.musifly.android.models.MediaMetadata(
                                    id = currentTag?.id ?: item.mediaId ?: "",
                                    title = title,
                                    artists = artistList.map { artistName ->
                                        com.musifly.android.models.MediaMetadata.Artist(
                                            id = null,
                                            name = artistName,
                                        )
                                    },
                                    duration = if (durationSeconds > 0) durationSeconds else (currentTag?.duration ?: 0),
                                    thumbnailUrl = if (imageUrl.isNotEmpty()) {
                                        // Always use the upgraded JioSaavn image URL
                                        imageUrl
                                    } else {
                                        // Fallback to existing thumbnailUrl if available
                                        currentTag?.thumbnailUrl ?: ""
                                    },
                                    album = if (album.isNotEmpty()) {
                                        com.musifly.android.models.MediaMetadata.Album(
                                            id = "",
                                            title = album,
                                        )
                                    } else currentTag?.album,
                                    explicit = currentTag?.explicit ?: false,
                                    liked = currentTag?.liked ?: false,
                                )

                                val updatedMetadataBuilder = androidx.media3.common.MediaMetadata.Builder()
                                    .apply {
                                        // Update with fetched data
                                        setTitle(title)
                                        setArtist(artist)
                                        if (album.isNotEmpty()) {
                                            setAlbumTitle(album)
                                        }
                                        if (imageUrl.isNotEmpty()) {
                                            setArtworkUri(android.net.Uri.parse(imageUrl))
                                            Log.d(TAG, "✅ Set artwork URI: $imageUrl")
                                        }
                                        // Set artworkData (ByteArray) for notifications
                                        artworkBytes?.let { bytes ->
                                            setArtworkData(bytes, androidx.media3.common.MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                                            Log.d(TAG, "✅ Set artworkData: ${bytes.size} bytes")
                                        }
                                        // Set duration in milliseconds
                                        if (durationSeconds > 0) {
                                            setDurationMs(durationSeconds * 1000L)
                                        }

                                        setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                                    }

                                // Update both the tag (MediaMetadata) and ExoPlayer metadata
                                val updatedItem = item.buildUpon()
                                    .setTag(updatedMediaMetadata)
                                    .setMediaMetadata(updatedMetadataBuilder.build())
                                    .build()

                                // Replace the media item with updated metadata
                                player.replaceMediaItem(currentIndex, updatedItem)

                                // Update currentMediaMetadata StateFlow
                                currentMediaMetadata.value = updatedMediaMetadata

                                Log.d(TAG, "✅ JIOSAAVN metadata enriched - Title: $title, Artist: $artist, Image: ${imageUrl.take(80)}...")
                                Log.d(TAG, "📸 UpdatedMediaMetadata.thumbnailUrl: ${updatedMediaMetadata.thumbnailUrl?.take(80)}...")
                                Log.d(TAG, "📸 PlayerConnection should receive updated metadata with thumbnailUrl")

                                // REAL-TIME SAFE: Single notification update (throttled internally)
                                updateNotification()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to enrich JioSaavn metadata", e)
                    }
                }
            }
        }

        // AUTOMATIC AUTOPLAY (Infinite Playback)
        // If we reached the last song, fetch similar songs and append to queue
        if (mediaItem != null && player.mediaItemCount > 0) {
            val isLastItem = player.currentMediaItemIndex == player.mediaItemCount - 1
            if (isLastItem) {
                handleAutoplay(mediaItem.mediaId)
            }
        }



        // Resetear errores consecutivos cuando hay transición exitosa
        consecutivePlaybackErr = 0

        // Auto cargar más canciones
        if (dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            currentQueue.hasNextPage() &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)
        ) {
            scope.launch(SilentHandler) {
                val mediaItems =
                    currentQueue.nextPage().filterExplicit(dataStore.get(HideExplicitKey, false))
                if (player.playbackState != STATE_IDLE) {
                    player.addMediaItems(mediaItems.drop(1))
                }
            }
        }

        // Guardar estado cuando cambia el item de medios
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(500) // Pequeño delay para asegurar que el estado esté estable
                saveQueueToDisk()
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        // Forward playing state to AudioEffectsManager for delayed attachment logic
        // audioEffectsManager?.onIsPlayingChanged(isPlaying) // Removed for Session Independent DSP
    }


    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        // Prevent auto-playback during state restoration
        if (isRestoringState && playbackState == Player.STATE_READY && player.playWhenReady) {
            Log.d(TAG, "🛑 Blocking auto-playback - playback state changed to READY during restoration")
            player.pause()
            player.playWhenReady = false
        }

        // Update playback priority mode (pauses background work during playback)
        PlaybackPriorityManager.updatePriorityMode(player)

        // PROFESSIONAL RECOVERY: When playback becomes ready, ensure audio focus and foreground service
        if (playbackState == Player.STATE_READY && player.playWhenReady && !isRestoringState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Ensure we have audio focus when ready to play
                if (!hasAudioFocus) {
                    Log.d(TAG, "🔄 Playback ready but no audio focus - requesting...")
                    requestAudioFocus()
                }
                // Ensure foreground service is active
                ensureForegroundService()
            }
        }

        // Guardar estado cuando cambia el estado de reproducción
        if (dataStore.get(PersistentQueueKey, true) && playbackState != Player.STATE_BUFFERING) {
            scope.launch {
                delay(500)
                saveQueueToDisk()
            }
        }

        // Cuando termina la reproducción, ocultar notificación si la cola está vacía
        if (playbackState == Player.STATE_ENDED) {
            scope.launch {
                delay(1000)
                if (!player.isPlaying && player.mediaItemCount == 0) {
                    // Limpiar metadata para forzar actualización de notificación
                    currentMediaMetadata.value = null
                } else if (player.mediaItemCount > 0 && !player.isPlaying && !isRestoringState) {
                    // If there are more songs but player stopped, try to play next
                    // PROFESSIONAL GAPLESS PLAYBACK: Automatically advance to next track
                    // ExoPlayer's seekToNext() maintains buffer state for seamless transition
                    val nextIndex = player.currentMediaItemIndex + 1
                    if (nextIndex < player.mediaItemCount) {
                        Log.d(TAG, "🔄 Song ended, advancing to next song (gapless) at index: $nextIndex")

                        // Request audio focus if needed
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            requestAudioFocus()
                        }

                        // GAPLESS: seekToNext() automatically handles smooth transition
                        player.seekToNext()
                        player.play() // Instant start - buffer already prefetched

                        Log.d(TAG, "✅ Next track started (gapless)")
                    }
                }
            }
        }

        // Update widgets to reflect playback state changes
        WidgetUpdateHelper.updateAllWidgets(this)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        // Prevent auto-playback during state restoration
        if (isRestoringState && playWhenReady) {
            Log.d(TAG, "🛑 Blocking auto-playback during state restoration")
            player.pause()
            player.playWhenReady = false
            return
        }

        // Update playback priority mode (pauses background work during playback)
        PlaybackPriorityManager.updatePriorityMode(player)

        // Disable background processor on pause to save resources
        if (!playWhenReady) {
            try {
                backgroundAudioProcessor?.setEnabled(false)
                Log.d(TAG, "🧹 Background audio processor disabled on pause")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to disable background audio processor", e)
            }
        } else {
            // Re-enable when playing
            try {
                if (!isAppInBackground.value && !isScreenOff.value) {
                    backgroundAudioProcessor?.setEnabled(true)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enable background audio processor", e)
            }
        }

        if (playWhenReady) {

            // 🔥 GLOBAL DSP (Session 0) - Already initialized in onCreate()

            // Request audio focus when playback starts
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requestAudioFocus()
                ensureForegroundService()
            }

            // PROFESSIONAL PREFETCHING: Preload next track for gapless playback
            if (player.hasNextMediaItem()) {
                // RUN ON MAIN THREAD: Accessing player on Dispatchers.IO causes "Player accessed on wrong thread" warning
                try {
                    val nextIndex = player.currentMediaItemIndex + 1
                    if (nextIndex < player.mediaItemCount) {
                        val nextItem = player.getMediaItemAt(nextIndex)
                        Log.d(TAG, "🎵 Prefetching next track for gapless playback: ${nextItem.mediaId}")
                        // ExoPlayer automatically prefetches next track when using seekToNext()
                        // The large back buffer (50s) ensures smooth transition
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Prefetch failed (non-critical): ${e.message}")
                }
            }
        } else {
            // Abandon audio focus when pausing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                abandonAudioFocus()
            }
        }

        // REAL-TIME SAFE: Update MediaSession ONLY on play/pause state change
        // Throttled to prevent frequent updates during playback
        scope.launch {
            delay(300)
            updateNotification()
        }

        // Update widgets to reflect play/pause state
        WidgetUpdateHelper.updateAllWidgets(this, player)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        // 🔥 GLOBAL DSP (Session 0) - Audio session changes don't affect DSP
        if (events.contains(Player.EVENT_AUDIO_SESSION_ID)) {
            Log.d(TAG, "Audio session ID changed to: ${this.player.audioSessionId} (GLOBAL DSP unaffected)")
        }

        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_MEDIA_METADATA_CHANGED
            )
        ) {
            WidgetUpdateHelper.updateAllWidgets(this, player)
        }

        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            val isBufferingOrReady =
                player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY

            // Monitor buffering state to detect stuck playback
            when (player.playbackState) {
                Player.STATE_BUFFERING -> {
                    if (bufferingStartTime == 0L) {
                        bufferingStartTime = System.currentTimeMillis()
                    }
                    // Check if buffering is taking too long
                    lastBufferingCheckJob?.cancel()
                    lastBufferingCheckJob = scope.launch {
                        delay(BUFFERING_TIMEOUT_MS)
                        if (player.playbackState == Player.STATE_BUFFERING && player.playWhenReady) {
                            val currentMediaItem = player.currentMediaItem
                            val mediaId = currentMediaItem?.mediaId
                            Log.w(TAG, "⚠️ Buffering timeout detected for mediaId: $mediaId, attempting to recover...")
                            // Try to recover by switching streaming engine or refreshing stream
                            if (mediaId != null && engineSwitchAttempts < MAX_ENGINE_SWITCH_ATTEMPTS) {
                                switchStreamingEngine(mediaId, player.currentPosition)
                            } else {
                                recoverFromStuckPlayback()
                            }
                        }
                    }
                }
                Player.STATE_READY -> {
                    // Reset buffering timeout when playback becomes ready
                    if (bufferingStartTime > 0L) {
                        val bufferingDuration = System.currentTimeMillis() - bufferingStartTime
                        Log.d(TAG, "✅ Playback ready after ${bufferingDuration}ms of buffering")
                    }
                    bufferingStartTime = 0L
                    lastBufferingCheckJob?.cancel()
                    lastBufferingCheckJob = null

                    // ENHANCED ERROR CLEARING: Clear error state when playback successfully becomes ready
                    // This ensures error UI disappears automatically when playback recovers
                    if (player.playerError == null && player.isPlaying) {
                        Log.d(TAG, "✅ Playback successful - error state should be cleared automatically by ExoPlayer")
                        // Reset retry counters on successful playback
                        automaticRetryCount = 0
                        lastRetryErrorCategory = null
                        lastRetryMediaId = null
                    }

                    // Prevent auto-playback during state restoration
                    if (isRestoringState && player.playWhenReady) {
                        Log.d(TAG, "🛑 Blocking auto-playback - player ready during restoration")
                        player.pause()
                        player.playWhenReady = false
                    }

                    // 🔥 GLOBAL DSP (Session 0) - Always operational regardless of player state
                    Log.d(TAG, "Playback ready - GLOBAL DSP active")
                }
                Player.STATE_ENDED, Player.STATE_IDLE -> {
                    bufferingStartTime = 0L
                    lastBufferingCheckJob?.cancel()
                    lastBufferingCheckJob = null
                }
                else -> {}
            }

            // Prevent auto-playback during state restoration
            if (isRestoringState && player.playWhenReady) {
                Log.d(TAG, "🛑 Blocking auto-playback during restoration - player became ready")
                player.pause()
                player.playWhenReady = false
            }

            if (isBufferingOrReady && player.playWhenReady && !isRestoringState) {
                // Ensure foreground service is started BEFORE requesting audio focus
                // This improves success rate on Android 12/13+ where FGS restrictions apply
                ensureForegroundService()

                val focusGranted = requestAudioFocus()
            } else {
                // Abandonar foco de audio cuando no está reproduciendo
                if (!player.playWhenReady || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                    abandonAudioFocus()
                }
            }
        }

        // REAL-TIME SAFE: Only update MediaSession on track change, NOT on position updates
        // Position updates cause pipelineFull warnings and audio glitches
        if (events.contains(EVENT_TIMELINE_CHANGED)) {
            // Timeline changed = track change - update metadata
            currentMediaMetadata.value = player.currentMetadata
            // Update notification only on track change (throttled)
            scope.launch {
                delay(200)
                updateNotification()
            }
        }
        // REMOVED: EVENT_POSITION_DISCONTINUITY - causes frequent updates during playback
        // Position updates should NOT trigger MediaSession updates

        // Actualización de Discord RPC
        // Actualización de Discord RPC (Removed)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            // Si la cola está vacía, no mezclar
            if (player.mediaItemCount == 0) return

            // Siempre poner el item que se está reproduciendo primero
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] =
                shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }

        // Guardar estado cuando cambia el modo aleatorio
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(300)
                saveQueueToDisk()
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }

        // Guardar estado cuando cambia el modo de repetición
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(300)
                saveQueueToDisk()
            }
        }
    }

    /**
     * Enhanced error handler using PlayerErrorMapper for intelligent error categorization
     *
     * ERROR ORIGIN:
     * - "Bad stream player response" comes from YTPlayerUtils.kt when all YouTube clients fail
     * - This can be: expired URLs, network issues, server errors, or format problems
     *
     * STREAM TYPES HANDLED:
     * - YouTube / YouTube Music (via NewPipe/Innertube)
     * - JioSaavn (via custom API)
     * - Local files (FileDataSource)
     */
    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        val currentMediaItem = player.currentMediaItem
        val mediaId = currentMediaItem?.mediaId
        val isLocalFile = currentMediaItem?.localConfiguration?.uri?.let { uri ->
            uri.scheme == "file" || uri.scheme == "content" || mediaId?.startsWith("local_") == true
        } ?: false

        // Determine stream type based on StreamSourceManager (selected engine), not just mediaId
        val selectedSource = StreamSourceManager.currentSource
        val streamType = when {
            isLocalFile -> "local"
            mediaId?.startsWith("saavn:") == true -> "saavn"
            selectedSource == StreamSourceManager.StreamType.JIOSAAVN -> "saavn"
            selectedSource == StreamSourceManager.StreamType.YT_MUSIC -> "youtube" // YouTube Music also uses "youtube" for error mapping
            else -> "youtube"
        }

        // Check for ParserException with "Skipping atom" error specifically
        val cause = error.cause
        val isParserExceptionWithAtom = cause?.javaClass?.simpleName == "ParserException" &&
                (cause.message?.contains("Skipping atom with length", ignoreCase = true) == true ||
                        cause.message?.contains("2147483647", ignoreCase = false) == true ||
                        error.message?.contains("Skipping atom with length", ignoreCase = true) == true)

        Log.d("ENGINE_DEBUG", "onPlayerError - Selected source: ${selectedSource.name}, StreamType: $streamType, ParserException: $isParserExceptionWithAtom")

        // Get current stream URL for logging (masked)
        val currentStreamUrl = songUrlCache[mediaId]?.first

        // Categorize error using PlayerErrorMapper
        val errorCategory = PlayerErrorMapper.categorizeError(error, streamType)
        val userMessage = PlayerErrorMapper.getUserFriendlyMessage(errorCategory, streamType)

        // Log detailed error info (masks sensitive data)
        val detailedInfo = PlayerErrorMapper.getDetailedErrorInfo(error, currentStreamUrl)
        Log.e(TAG, "❌ Playback Error: $detailedInfo")
        Log.e(TAG, "   Category: $errorCategory, StreamType: $streamType, Message: $userMessage")

        // Handle local file errors specifically (non-recoverable format issues)
        if (isLocalFile && errorCategory == PlayerErrorMapper.ErrorCategory.UNSUPPORTED_FORMAT) {
            Log.e(TAG, "💥 Unsupported local file format: $mediaId")
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    this@MusicService,
                    "⚠️ Cannot play this file - unsupported format",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }

            // Skip to next track automatically for local files
            if (player.hasNextMediaItem()) {
                Log.d(TAG, "⏭️ Skipping unsupported local file, playing next...")
                player.seekToNext()
                player.play()
            } else {
                Log.d(TAG, "🛑 No more tracks, stopping playback")
                player.stop()
            }
            return
        }

        // Check if this is the same error as last retry (prevent infinite loops)
        val isSameError = lastRetryMediaId == mediaId && lastRetryErrorCategory == errorCategory

        // Automatic retry logic for recoverable errors
        if (PlayerErrorMapper.isRecoverable(errorCategory) && mediaId != null) {
            // Reset retry count if it's a different media item or different error type
            if (lastRetryMediaId != mediaId || lastRetryErrorCategory != errorCategory) {
                automaticRetryCount = 0
                lastRetryMediaId = mediaId
                lastRetryErrorCategory = errorCategory
            }

            if (automaticRetryCount < MAX_AUTOMATIC_RETRIES && !isSameError) {
                automaticRetryCount++
                Log.d(TAG, "🔄 Automatic retry $automaticRetryCount/$MAX_AUTOMATIC_RETRIES for $errorCategory")

                // Handle different error categories with specific recovery strategies
                scope.launch(Dispatchers.Main) {
                    try {
                        when (errorCategory) {
                            PlayerErrorMapper.ErrorCategory.URL_EXPIRED -> {
                                // Clear expired URL cache and refresh stream
                                Log.d(TAG, "🔄 URL expired - clearing cache and refreshing stream for $mediaId")
                                songUrlCache.remove(mediaId)

                                // Clear corrupted cache if any
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        playerCache.removeResource(mediaId)
                                        downloadCache.removeResource(mediaId)
                                    } catch (e: Exception) {
                                        Log.w(TAG, "⚠️ Error clearing cache: ${e.message}")
                                    }
                                }

                                // Refresh stream with new URL
                                val currentPosition = player.currentPosition
                                kotlinx.coroutines.delay(500) // Small delay for cache clearing
                                refreshCurrentStream(mediaId, currentPosition)
                            }

                            PlayerErrorMapper.ErrorCategory.CORRUPTED_CACHE -> {
                                // Clear corrupted cache and retry
                                Log.d(TAG, "🔄 Corrupted cache detected - clearing and retrying for $mediaId")
                                songUrlCache.remove(mediaId)

                                // Stop player first to prevent reading corrupted data
                                withContext(Dispatchers.Main) {
                                    player.stop()
                                }

                                // Clear corrupted cache and wait for completion
                                val currentPosition = player.currentPosition
                                withContext(Dispatchers.IO) {
                                    try {
                                        playerCache.removeResource(mediaId)
                                        downloadCache.removeResource(mediaId)
                                        Log.d(TAG, "✅ Cleared corrupted cache for $mediaId")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Failed to clear cache: ${e.message}", e)
                                    }
                                    // Small delay to ensure cache operations complete
                                    kotlinx.coroutines.delay(500)
                                }

                                // Refresh stream with fresh data (will skip corrupted cache)
                                refreshCurrentStream(mediaId, currentPosition)
                            }

                            PlayerErrorMapper.ErrorCategory.NETWORK_ERROR -> {
                                // Network errors: wait a bit longer, then retry
                                Log.d(TAG, "🔄 Network error - waiting before retry for $mediaId")
                                if (isNetworkConnected.value) {
                                    // Network is connected, just retry
                                    kotlinx.coroutines.delay(1000)
                                    player.prepare()
                                    player.play()
                                } else {
                                    // Wait for network connection
                                    waitOnNetworkError()
                                    return@launch
                                }
                            }

                            PlayerErrorMapper.ErrorCategory.UNSUPPORTED_FORMAT -> {
                                // Unsupported format (e.g., ParserException with atom length > 2147483647)
                                // Try YouTube fallback with safer audio-only stream
                                if (streamType == "youtube" && mediaId != null) {
                                    Log.e(TAG, "🔄 Unsupported YouTube format detected - trying fallback for $mediaId")
                                    handleYouTubeUnsupportedFormat(mediaId, error)
                                } else {
                                    // Not YouTube or already tried - show error
                                    Log.e(TAG, "💥 Unsupported format error (non-YouTube or fallback already tried)")
                                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                                        android.widget.Toast.makeText(
                                            this@MusicService,
                                            "⚠️ Audio format not supported for this track",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    // Skip to next track
                                    if (player.hasNextMediaItem()) {
                                        player.seekToNext()
                                        player.play()
                                    } else {
                                        player.stop()
                                    }
                                }
                                return@launch // Exit early - handled
                            }

                            PlayerErrorMapper.ErrorCategory.SERVER_BAD_RESPONSE -> {
                                // Server error: try switching engines or refreshing URL
                                Log.d(TAG, "🔄 Server bad response - trying engine switch or refresh for $mediaId")

                                if (engineSwitchAttempts < MAX_ENGINE_SWITCH_ATTEMPTS) {
                                    // Try switching streaming engine
                                    val currentPosition = player.currentPosition
                                    switchStreamingEngine(mediaId, currentPosition)
                                } else {
                                    // Engines exhausted, clear URL cache and refresh
                                    songUrlCache.remove(mediaId)
                                    val currentPosition = player.currentPosition
                                    kotlinx.coroutines.delay(500)
                                    refreshCurrentStream(mediaId, currentPosition)
                                }
                            }

                            else -> {
                                // Generic retry for other recoverable errors
                                Log.d(TAG, "🔄 Generic retry for $errorCategory")
                                kotlinx.coroutines.delay(1000)
                                player.prepare()
                                player.play()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error during automatic retry: ${e.message}", e)
                        // If automatic retry fails, show error to user
                        showErrorToUser(errorCategory, userMessage)
                    }
                }

                // Show user-friendly message during retry
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        this@MusicService,
                        userMessage,
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }

                return // Exit early - retry is in progress
            } else {
                // Max retries reached
                Log.w(TAG, "⚠️ Max automatic retries ($MAX_AUTOMATIC_RETRIES) reached for $errorCategory")
                automaticRetryCount = 0 // Reset for next error
            }
        }

        // Try automatic engine switching before giving up (if not already tried)
        if (mediaId != null && engineSwitchAttempts < MAX_ENGINE_SWITCH_ATTEMPTS &&
            errorCategory == PlayerErrorMapper.ErrorCategory.SERVER_BAD_RESPONSE) {
            Log.d(TAG, "🔄 Attempting engine switch as last resort for $mediaId")
            val currentPosition = player.currentPosition
            switchStreamingEngine(mediaId, currentPosition)
            return
        }

        // All retries exhausted - show error to user
        showErrorToUser(errorCategory, userMessage)

        // Handle final error state
        if (dataStore.get(AutoSkipNextOnErrorKey, false)) {
            skipOnError()
        } else {
            stopOnError()
        }
    }

    /**
     * Show error message to user via toast
     */
    private fun showErrorToUser(
        errorCategory: PlayerErrorMapper.ErrorCategory,
        userMessage: String
    ) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(
                this@MusicService,
                userMessage,
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun createCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource
            .Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource
                    .Factory()
                    .setCache(playerCache)
                    .setUpstreamDataSourceFactory(
                        DefaultDataSource.Factory(
                            this,
                            // ENHANCED HTTP CLIENT: Proper timeouts, User-Agent, and redirect handling
                            // Ensures reliable streaming for YouTube and JioSaavn
                            // Note: OkHttpClient handles redirects automatically via followRedirects()
                            OkHttpDataSource.Factory(
                                OkHttpClient
                                    .Builder()
                                    .proxy(YouTube.proxy)
                                    .connectTimeout(10, TimeUnit.SECONDS)  // Faster connection (10s) for instant start
                                    .readTimeout(20, TimeUnit.SECONDS)     // Faster read (20s) for responsive streaming
                                    .writeTimeout(20, TimeUnit.SECONDS)    // Faster write (20s)
                                    .callTimeout(45, TimeUnit.SECONDS)     // Total call timeout (45s)
                                    .retryOnConnectionFailure(true)        // Auto-retry on connection failure
                                    .followRedirects(true)                 // Follow HTTP redirects
                                    .followSslRedirects(true)              // Follow HTTPS redirects (allows HTTP→HTTPS)
                                    .build(),
                            )
                                .setUserAgent("MusiFly-Android/1.0 (Linux; U; Android)") // User-Agent for YouTube/JioSaavn compatibility
                        ),
                    ),
            ).setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    /**
     * Create a composite DataSource factory that handles both local files and streaming
     */
    private fun createDataSourceFactory(): DataSource.Factory {
        // For local files, use DefaultDataSource to handle file://, content:// and assets
        // For streaming, use the resolving cache data source
        return object : DataSource.Factory {
            override fun createDataSource(): DataSource {
                // Use DefaultDataSource for local files to support content:// and file:// schemes correctly
                val localDataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this@MusicService)
                val streamingDataSourceFactory = createStreamingDataSource()

                return object : DataSource {
                    private var delegateDataSource: DataSource? = null
                    private var isLocalSupported = false
                    private val transferListeners = mutableListOf<TransferListener>()

                    override fun open(dataSpec: DataSpec): Long {
                        val uri = dataSpec.uri
                        val scheme = uri.scheme

                        // Determine if this is a local resource supported by DefaultDataSource (excluding http/https)
                        isLocalSupported = scheme == "file" || 
                                         scheme == "content" || 
                                         scheme == "asset" || 
                                         scheme == "rawresource" ||
                                         dataSpec.uri.toString().startsWith("local_")

                        delegateDataSource = if (isLocalSupported && scheme != "http" && scheme != "https") {
                            Log.d(TAG, "🎵 LOCAL/SYSTEM RESOURCE - DefaultDataSource: $uri")
                            localDataSourceFactory.createDataSource().also { ds ->
                                // Add all transfer listeners to the delegate
                                transferListeners.forEach { ds.addTransferListener(it) }
                            }
                        } else {
                            Log.d(TAG, "🌐 STREAMING - ResolvingDataSource: $uri")
                            streamingDataSourceFactory.createDataSource().also { ds ->
                                // Add all transfer listeners to the delegate
                                transferListeners.forEach { ds.addTransferListener(it) }
                            }
                        }

                        return delegateDataSource!!.open(dataSpec)
                    }

                    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                        return delegateDataSource?.read(buffer, offset, length)
                            ?: throw IllegalStateException("DataSource not opened")
                    }

                    override fun getUri(): android.net.Uri? {
                        return delegateDataSource?.uri
                    }

                    override fun getResponseHeaders(): Map<String, List<String>> {
                        return delegateDataSource?.responseHeaders ?: emptyMap()
                    }

                    override fun close() {
                        try {
                            delegateDataSource?.close()
                        } finally {
                            delegateDataSource = null
                        }
                    }

                    override fun addTransferListener(transferListener: TransferListener) {
                        // Store listeners to add them to the delegate when it's created in open()
                        transferListeners.add(transferListener)
                    }
                }
            }
        }
    }

    /**
     * Create DataSource for streaming content (YouTube/NewPipe)
     */
    private fun createStreamingDataSource(): DataSource.Factory {
        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            // Extract mediaId from key or URI
            val mediaId = dataSpec.key ?: dataSpec.uri.toString().takeIf {
                it.isNotBlank() && !it.startsWith("http")
            } ?: run {
                Log.e(TAG, "❌ No media id found in dataSpec. key=${dataSpec.key}, uri=${dataSpec.uri}")
                error("No media id")
            }

            Log.d(TAG, "🔍 Resolving streaming mediaId: $mediaId")

            // BLOOMEE-STYLE OFFLINE PLAYBACK: Check for downloaded file FIRST
            // This mimics Bloomee's AudioSourceManager.getAudioSource() logic
            // Note: ResolvingDataSource resolver is synchronous, so we use runBlocking
            val downloadedFile = try {
                kotlinx.coroutines.runBlocking {
                    getDownloadedFile(mediaId)
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error checking downloaded file: ${e.message}")
                null
            }

            if (downloadedFile != null && downloadedFile.exists()) {
                Log.d(TAG, "✅ Found downloaded file for offline playback: ${downloadedFile.absolutePath}")

                // Detect and set stream engine for local files
                activeStreamingEngine = detectStreamEngine("file://${downloadedFile.absolutePath}", mediaId)
                Log.d(TAG, "🔍 Detected stream engine: $activeStreamingEngine")

                // Show toast notification (like Bloomee does)
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(
                        this@MusicService,
                        "Playing Offline",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                // Return file URI - the DataSource factory will use FileDataSource
                return@Factory dataSpec.withUri(android.net.Uri.fromFile(downloadedFile))
            }

            // CRITICAL: Set the key to mediaId so CacheDataSource can find it in cache
            // The cache key MUST match what was used when storing to cache
            val dataSpecWithKey = if (dataSpec.key.isNullOrBlank()) {
                dataSpec.withRequestHeaders(dataSpec.httpRequestHeaders)
                    .buildUpon()
                    .setKey(mediaId)
                    .build()
            } else {
                dataSpec
            }

            // FIXED: Check cache first for offline playback - prioritize cached content
            // Check both download cache and player cache for complete or partial content
            val isCachedInDownloadCache = try {
                // Check if entire file is cached (download cache stores complete files)
                val cachedLength = downloadCache.getCachedLength(mediaId, dataSpecWithKey.position, Long.MAX_VALUE)
                val isComplete = cachedLength > 0
                if (isComplete) {
                    Log.d(TAG, "✅ Download cache has complete file: $mediaId (length: $cachedLength)")
                }
                isComplete
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error checking download cache: ${e.message}")
                false
            }

            val isCachedInPlayerCache = try {
                // For player cache, check if we have at least some content cached
                // Player cache stores chunks, so we check for reasonable chunk size
                // Require minimum 1KB to avoid using obviously corrupted/incomplete cache
                val MIN_CACHE_SIZE = 1024L // 1KB minimum
                val cachedLength = playerCache.getCachedLength(mediaId, dataSpecWithKey.position, CHUNK_LENGTH)
                val hasContent = cachedLength >= MIN_CACHE_SIZE
                if (hasContent) {
                    Log.d(TAG, "✅ Player cache has content: $mediaId (length: $cachedLength)")
                } else if (cachedLength > 0) {
                    Log.w(TAG, "⚠️ Player cache too small (${cachedLength} bytes), likely corrupted/incomplete: $mediaId")
                }
                hasContent
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Error checking player cache: ${e.message}")
                false
            }

            // If cached, return dataSpec with proper key - CacheDataSource will read from cache
            // The key MUST be set to mediaId for CacheDataSource to find it in cache
            if (isCachedInDownloadCache || isCachedInPlayerCache) {
                Log.d(TAG, "✅ Found cached content for offline playback: $mediaId (downloadCache: $isCachedInDownloadCache, playerCache: $isCachedInPlayerCache)")

                // Still try to recover song (for metadata) but don't wait
                scope.launch(Dispatchers.IO) {
                    try {
                        recoverSong(mediaId)
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ recoverSong failed for cached content (non-critical): ${e.message}")
                    }
                }

                // Return dataSpec with key set - CacheDataSource will automatically read from cache
                // The key (mediaId) tells CacheDataSource which cache entry to use
                // CacheDataSource checks cache first before trying network
                Log.d(TAG, "📦 Returning cached dataSpec with key=$mediaId - CacheDataSource will read from cache")
                return@Factory dataSpecWithKey
            }

            // ENHANCED URL CACHING: Check cached URLs but verify expiration
            // YouTube/JioSaavn URLs have expiration tokens - expired URLs cause "Bad stream player response"
            songUrlCache[mediaId]?.let { cached ->
                val cachedUrl = cached.first
                val expirationTime = cached.second
                val currentTime = System.currentTimeMillis()
                val timeUntilExpiry = expirationTime - currentTime

                // Check if URL is still valid (with 5-minute safety margin before actual expiry)
                // This prevents using URLs that are about to expire during playback
                val SAFETY_MARGIN_MS = 5 * 60 * 1000L // 5 minutes safety margin
                val isUrlValid = expirationTime > (currentTime + SAFETY_MARGIN_MS)

                if (isUrlValid) {
                    Log.d(TAG, "✅ Using cached stream URL for mediaId: $mediaId (expires in ${timeUntilExpiry / 1000}s, engine: ${activeStreamingEngine ?: "unknown"})")
                    scope.launch(Dispatchers.IO) {
                        try {
                            recoverSong(mediaId)
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ recoverSong failed (non-critical): ${e.message}")
                        }
                    }
                    // Use the cached URL directly - CacheDataSource will check cache first before downloading
                    // IMPORTANT: Set key to mediaId so cache lookup works
                    return@Factory dataSpecWithKey.withUri(cachedUrl.toUri())
                } else {
                    // URL expired or about to expire - remove from cache and fetch fresh
                    Log.w(TAG, "⚠️ Cached URL expired or about to expire (expires in ${timeUntilExpiry / 1000}s) - fetching fresh URL for $mediaId")
                    songUrlCache.remove(mediaId)
                }
            }

            // Check if this is a JioSaavn song
            if (mediaId.startsWith("saavn:")) {
                val saavnLogTag = "JioSaavn"
                val songId = mediaId.removePrefix("saavn:")
                val currentSource = StreamSourceManager.currentSource
                Log.d("PlaybackSourceCheck", "🎵 Resolving JioSaavn stream URL")
                Log.d("PlaybackSourceCheck", "   MediaId: $mediaId")
                Log.d("PlaybackSourceCheck", "   TOPBAR_SELECTED = ${currentSource.name}")
                try {
                    Timber.tag(saavnLogTag).d("🎵 Fetching JioSaavn stream URL for song ID: $songId")

                    val streamingManager = StreamingEngineManager.getInstance()
                    val streamUrl = runBlocking(Dispatchers.IO) {
                        streamingManager.getSaavnStreamUrl(songId)
                    }

                    if (streamUrl != null && streamUrl.isNotEmpty()) {
                        Timber.tag(saavnLogTag).i("✅ JioSaavn stream URL obtained: ${streamUrl.take(80)}...")

                        // Detect and set stream engine
                        activeStreamingEngine = detectStreamEngine(streamUrl, mediaId)
                        Log.d(TAG, "🔍 Detected stream engine: $activeStreamingEngine")
                        Log.d("PlaybackSourceCheck", "PLAYBACK_ENGINE_USED = $activeStreamingEngine")

                        // VERIFY: Check if selected source matches actual engine
                        if (currentSource != StreamSourceManager.StreamType.JIOSAAVN) {
                            Log.w("PlaybackSourceCheck", "⚠️ MISMATCH: Selected source (${currentSource.name}) != Expected JioSaavn")
                        } else {
                            Log.d("PlaybackSourceCheck", "✅ MATCH: Selected source (${currentSource.name}) == JioSaavn engine")
                        }

                        // Show notification only once per session
                        // Toast removed - stream engine notification disabled
                        hasShownStreamingToast = true

                        // Cache the URL (JioSaavn URLs typically don't expire, but cache for 6 hours)
                        songUrlCache[mediaId] = streamUrl to (System.currentTimeMillis() + (6 * 60 * 60 * 1000L))

                        scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                        // Return full stream URL with proper key for cache lookup
                        return@Factory dataSpecWithKey.withUri(streamUrl.toUri())
                    } else {
                        Timber.tag(saavnLogTag).e("❌ Failed to get JioSaavn stream URL for song ID: $songId")
                        throw PlaybackException(
                            "Failed to get stream URL from JioSaavn",
                            null,
                            PlaybackException.ERROR_CODE_REMOTE_ERROR
                        )
                    }
                } catch (e: Exception) {
                    Timber.tag(saavnLogTag).e(e, "❌ JioSaavn playback error: ${e.message}")
                    // ENHANCED: More specific error messages for JioSaavn
                    val errorMessage = when {
                        e.message?.contains("network", ignoreCase = true) == true -> "JioSaavn: Network connection failed"
                        e.message?.contains("404", ignoreCase = false) == true -> "JioSaavn: Song not found or unavailable"
                        e.message?.contains("403", ignoreCase = false) == true -> "JioSaavn: Access denied or URL expired"
                        else -> "JioSaavn: ${e.message ?: "Failed to get stream URL"}"
                    }
                    throw PlaybackException(
                        errorMessage,
                        e,
                        PlaybackException.ERROR_CODE_REMOTE_ERROR
                    )
                }
            }

            // Check if NewPipe streaming is enabled
            val useNewPipeStreaming = runBlocking {
                dataStore.data.map { preferences ->
                    preferences[UseNewPipeStreamingKey] ?: false
                }.first()
            }

            // Flag to track if we should try NewPipe/JioSaavn fallback after YouTube fails
            var shouldTryNewPipeFallback = false

            // Store metadata for JioSaavn fallback (captured early before YouTube fails)
            var fallbackTitle: String? = null
            var fallbackArtist: String? = null

            // Try NewPipe first if enabled
            if (useNewPipeStreaming) {
                val npLogTag = "NewPipe"
                val maxRetries = 2
                var lastError: Exception? = null

                for (attempt in 1..maxRetries) {
                    try {
                        Timber.tag(npLogTag).d("🎵 Using NewPipe streaming for: $mediaId (attempt $attempt/$maxRetries)")

                        val streamInfo = runBlocking(Dispatchers.IO) {
                            NewPipeAudioStreamExtractor
                                .getBestAudioStream("https://youtube.com/watch?v=$mediaId")
                                .getOrThrow()
                        }

                        val streamUrl = streamInfo.url

                        Timber.tag(npLogTag).i("✅ NewPipe stream URL obtained: ${streamUrl.take(80)}...")

                        // Detect and set stream engine (NewPipe is typically used for YouTube Music)
                        // Check if this is a YouTube Music URL or regular YouTube
                        activeStreamingEngine = if (mediaId.contains("music.youtube.com", ignoreCase = true) ||
                            streamUrl.contains("music.youtube.com", ignoreCase = true)) {
                            "YouTube Music"
                        } else {
                            detectStreamEngine(streamUrl, mediaId).takeIf {
                                it != "Unknown"
                            } ?: "YouTube Music" // Default to YouTube Music for NewPipe
                        }
                        Log.d(TAG, "🔍 Detected stream engine: $activeStreamingEngine")

                        // Toast removed - stream engine notification disabled
                        hasShownStreamingToast = true

                        // Cache the URL (NewPipe URLs typically expire in 6 hours)
                        songUrlCache[mediaId] = streamUrl to (System.currentTimeMillis() + (6 * 60 * 60 * 1000L))

                        // Return full stream URL without chunking for continuous playback
                        // Use dataSpecWithKey to ensure cache key is set
                        return@Factory dataSpecWithKey.withUri(streamUrl.toUri())

                    } catch (e: Exception) {
                        lastError = e
                        Timber.tag(npLogTag).e(e, "❌ NewPipe streaming attempt $attempt failed: ${e.message}")

                        // Small delay before retry
                        if (attempt < maxRetries) {
                            Thread.sleep(500)
                        }
                    }
                }

                // Log final failure - will try YouTube, then JioSaavn if needed
                Timber.tag(npLogTag).e(lastError, "❌ NewPipe streaming failed after $maxRetries attempts, falling back to YouTube")

                // Set flag to enable JioSaavn fallback if YouTube also fails
                shouldTryNewPipeFallback = true
            }

            // Intentar YouTube primero (fuente principal)
            val ytLogTag = "YouTube"
            val currentSource = StreamSourceManager.currentSource
            Log.d("PlaybackSourceCheck", "🎵 Resolving YouTube stream URL")
            Log.d("PlaybackSourceCheck", "   MediaId: $mediaId")
            Log.d("PlaybackSourceCheck", "   TOPBAR_SELECTED = ${currentSource.name}")

            try {
                // Try YouTube first, but automatically fallback to NewPipe if it fails with specific errors
                var playbackData: YTPlayerUtils.PlaybackData? = null
                var shouldTryNewPipeFallback = false

                try {
                    playbackData = runBlocking(Dispatchers.IO) {
                        YTPlayerUtils.playerResponseForPlayback(
                            mediaId,
                            audioQuality = audioQuality,
                            connectivityManager = connectivityManager,
                        )
                    }.getOrElse { throwable ->
                        // Log the actual error for debugging
                        Timber.tag(ytLogTag).e(throwable, "YouTube playback error: ${throwable.message}")
                        Log.w("PlaybackSourceCheck", "⚠️ YouTube stream resolution failed: ${throwable.message}")

                        // Check if this is a LOGIN_REQUIRED or "All YouTube clients failed" error
                        // These errors indicate YouTube is blocking the request, so we should try NewPipe
                        val isLoginRequiredError = throwable.message?.contains("LOGIN_REQUIRED", ignoreCase = true) == true ||
                                throwable.message?.contains("Sign in to confirm", ignoreCase = true) == true ||
                                throwable.message?.contains("All YouTube clients failed", ignoreCase = true) == true ||
                                throwable.message?.contains("bot", ignoreCase = true) == true

                        // Always trigger NewPipe fallback for login/bot errors
                        if (isLoginRequiredError) {
                            // Mark for NewPipe fallback instead of throwing error immediately
                            Log.d(TAG, "🔄 YouTube blocked (LOGIN_REQUIRED/bot detection) - will try NewPipe fallback")
                            shouldTryNewPipeFallback = true
                            null // Return null to trigger fallback
                        } else {
                            // For other errors, throw as before
                            when (throwable) {
                                is PlaybackException -> throw throwable

                                is ConnectException, is UnknownHostException -> {
                                    throw PlaybackException(
                                        getString(R.string.error_no_internet),
                                        throwable,
                                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                                    )
                                }

                                is SocketTimeoutException -> {
                                    throw PlaybackException(
                                        getString(R.string.error_timeout),
                                        throwable,
                                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                                    )
                                }

                                else -> {
                                    // Try to provide more specific error message
                                    val errorMessage = when {
                                        throwable.message?.contains("playability", ignoreCase = true) == true ||
                                                throwable.message?.contains("unavailable", ignoreCase = true) == true ->
                                            "Video unavailable or restricted"
                                        throwable.message?.contains("signature", ignoreCase = true) == true ||
                                                throwable.message?.contains("deobfuscate", ignoreCase = true) == true ->
                                            "Stream extraction failed - try again"
                                        throwable.message?.contains("format", ignoreCase = true) == true ->
                                            "No playable format found"
                                        throwable.message?.contains("parse", ignoreCase = true) == true ||
                                                throwable.message?.contains("parsing", ignoreCase = true) == true ->
                                            "Failed to parse stream data"
                                        else -> getString(R.string.error_unknown) + ": ${throwable.message?.take(50) ?: "No details"}"
                                    }
                                    throw PlaybackException(
                                        errorMessage,
                                        throwable,
                                        PlaybackException.ERROR_CODE_REMOTE_ERROR
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Check if this is a rethrown exception or a new error
                    val isLoginRequiredError = e.message?.contains("LOGIN_REQUIRED", ignoreCase = true) == true ||
                            e.message?.contains("Sign in to confirm", ignoreCase = true) == true ||
                            e.message?.contains("All YouTube clients failed", ignoreCase = true) == true ||
                            e.message?.contains("bot", ignoreCase = true) == true

                    // Always try NewPipe fallback for login/bot errors (even if NewPipe was already tried)
                    if (isLoginRequiredError) {
                        Log.d(TAG, "🔄 YouTube blocked (LOGIN_REQUIRED/bot detection) - will try NewPipe fallback")
                        shouldTryNewPipeFallback = true
                        playbackData = null
                    } else {
                        throw e // Rethrow non-fallback errors
                    }
                }

                // If YouTube failed with LOGIN_REQUIRED and NewPipe wasn't already tried, try NewPipe now
                if (shouldTryNewPipeFallback && playbackData == null) {
                    Log.d(TAG, "🔄 Attempting automatic NewPipe fallback due to YouTube blocking")
                    val npLogTag = "NewPipe_Fallback"
                    val maxRetries = 2
                    var lastError: Exception? = null
                    var newPipeSuccess = false

                    for (attempt in 1..maxRetries) {
                        try {
                            Timber.tag(npLogTag).d("🎵 Using NewPipe fallback for: $mediaId (attempt $attempt/$maxRetries)")

                            val streamInfo = runBlocking(Dispatchers.IO) {
                                NewPipeAudioStreamExtractor
                                    .getBestAudioStream("https://youtube.com/watch?v=$mediaId")
                                    .getOrThrow()
                            }

                            val streamUrl = streamInfo.url

                            Timber.tag(npLogTag).i("✅ NewPipe fallback stream URL obtained: ${streamUrl.take(80)}...")
                            Log.d(TAG, "✅ Automatic NewPipe fallback successful - playback will continue")

                            // Detect and set stream engine
                            activeStreamingEngine = if (mediaId.contains("music.youtube.com", ignoreCase = true) ||
                                streamUrl.contains("music.youtube.com", ignoreCase = true)) {
                                "YouTube Music"
                            } else {
                                detectStreamEngine(streamUrl, mediaId).takeIf {
                                    it != "Unknown"
                                } ?: "YouTube Music" // Default to YouTube Music for NewPipe
                            }
                            Log.d(TAG, "🔍 Detected stream engine: $activeStreamingEngine (NewPipe fallback)")

                            // Cache the URL (NewPipe URLs typically expire in 6 hours)
                            songUrlCache[mediaId] = streamUrl to (System.currentTimeMillis() + (6 * 60 * 60 * 1000L))

                            // Return full stream URL without chunking for continuous playback
                            newPipeSuccess = true
                            return@Factory dataSpecWithKey.withUri(streamUrl.toUri())

                        } catch (e: Exception) {
                            lastError = e
                            Timber.tag(npLogTag).e(e, "❌ NewPipe fallback attempt $attempt failed: ${e.message}")

                            // Small delay before retry
                            if (attempt < maxRetries) {
                                Thread.sleep(500)
                            }
                        }
                    }

                    // If NewPipe fallback also failed, try JioSaavn as final fallback
                    if (!newPipeSuccess) {
                        Timber.tag(npLogTag).e(lastError, "❌ NewPipe fallback failed after $maxRetries attempts")
                        Log.e(TAG, "❌ Both YouTube and NewPipe fallback failed - trying JioSaavn as final fallback")

                        // Try JioSaavn as the final fallback
                        val saavnLogTag = "JioSaavn_Fallback"
                        try {
                            Log.d(TAG, "🔄 Attempting JioSaavn fallback for video: $mediaId")

                            // Try to get song metadata from database first
                            var song = runBlocking(Dispatchers.IO) {
                                try {
                                    database.song(mediaId).first()
                                } catch (e: Exception) {
                                    Log.d(saavnLogTag, "⚠️ Song not in database yet: ${e.message}")
                                    null
                                }
                            }

                            // If not in database, try to get from player's current media item
                            var rawTitle: String? = null
                            var rawArtist: String? = null

                            if (song != null) {
                                val metadata = song.toMediaMetadata()
                                rawTitle = metadata.title
                                rawArtist = metadata.artists.firstOrNull()?.name ?: ""
                                Log.d(saavnLogTag, "📋 Got metadata from database")
                            } else {
                                // Get from player's current media item (thread-safe)
                                runBlocking(Dispatchers.Main) {
                                    player.currentMediaItem?.mediaMetadata?.let { meta ->
                                        rawTitle = meta.title?.toString()
                                        rawArtist = meta.artist?.toString()
                                        Log.d(saavnLogTag, "📋 Got metadata from player media item")
                                    }
                                }
                            }

                            if (rawTitle.isNullOrEmpty()) {
                                Log.w(saavnLogTag, "⚠️ Could not get song metadata for JioSaavn search")
                            } else {
                                Log.d(saavnLogTag, "📋 Raw metadata - Title: '$rawTitle', Artist: '$rawArtist'")

                                // Clean title - remove common YouTube suffixes
                                val cleanTitle = rawTitle!!
                                    .replace(Regex("\\(Official (Video|Audio|Music Video|Lyric Video)\\)", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("\\[Official (Video|Audio|Music Video|Lyric Video)\\]", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("\\| Official (Video|Audio|Music Video)", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("- Official (Video|Audio|Music Video)", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("\\(Lyrical\\)", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("\\(Lyrics\\)", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("\\(Full Song\\)", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("\\(HD\\)", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("\\(4K\\)", RegexOption.IGNORE_CASE), "")
                                    .replace(Regex("\\s+\\|\\s+.*$"), "") // Remove everything after |
                                    .replace(Regex("\\s+-\\s+.*$"), "") // Remove everything after -
                                    .trim()

                                // Extract primary artist (first artist if multiple)
                                val primaryArtist = rawArtist
                                    ?.split(",", "&", "feat.", "ft.", "Feat.", "Ft.")
                                    ?.firstOrNull()
                                    ?.trim() ?: ""

                                Log.d(saavnLogTag, "🧹 Cleaned metadata - Title: '$cleanTitle', Artist: '$primaryArtist'")

                                // Try multiple search strategies
                                val saavnAPI = com.musifly.android.streamingengines.saavn.SaavnAPI()
                                var saavnStreamUrl: String? = null

                                // Strategy 1: Search with cleaned title + primary artist
                                if (cleanTitle.isNotEmpty()) {
                                    val searchQuery1 = if (primaryArtist.isNotEmpty()) {
                                        "$cleanTitle $primaryArtist"
                                    } else {
                                        cleanTitle
                                    }

                                    Log.d(saavnLogTag, "🔍 Strategy 1: Searching for '$searchQuery1'")

                                    val searchResults1: Map<String, Any> = runBlocking(Dispatchers.IO) {
                                        saavnAPI.fetchSearchResults(searchQuery1, count = 5)
                                    }

                                    @Suppress("UNCHECKED_CAST")
                                    val songs1 = searchResults1["songs"] as? List<Map<String, Any>>
                                    if (!songs1.isNullOrEmpty()) {
                                        val firstSong = songs1[0]
                                        val saavnSongId = firstSong["id"] as? String
                                        val saavnTitle = firstSong["title"] as? String ?: "Unknown"
                                        val saavnArtist = firstSong["artist"] as? String ?: "Unknown"

                                        Log.d(saavnLogTag, "✅ Found match: '$saavnTitle' by $saavnArtist (ID: $saavnSongId)")

                                        if (saavnSongId != null) {
                                            saavnStreamUrl = runBlocking(Dispatchers.IO) {
                                                saavnAPI.getSongUrl(saavnSongId)
                                            }
                                        }
                                    } else {
                                        Log.d(saavnLogTag, "⚠️ Strategy 1 failed: No results")
                                    }
                                }

                                // Strategy 2: If strategy 1 failed, try title only (without artist)
                                if (saavnStreamUrl.isNullOrEmpty() && cleanTitle.isNotEmpty()) {
                                    Log.d(saavnLogTag, "🔍 Strategy 2: Searching for title only '$cleanTitle'")

                                    val searchResults2: Map<String, Any> = runBlocking(Dispatchers.IO) {
                                        saavnAPI.fetchSearchResults(cleanTitle, count = 5)
                                    }

                                    @Suppress("UNCHECKED_CAST")
                                    val songs2 = searchResults2["songs"] as? List<Map<String, Any>>
                                    if (!songs2.isNullOrEmpty()) {
                                        val firstSong = songs2[0]
                                        val saavnSongId = firstSong["id"] as? String
                                        val saavnTitle = firstSong["title"] as? String ?: "Unknown"
                                        val saavnArtist = firstSong["artist"] as? String ?: "Unknown"

                                        Log.d(saavnLogTag, "✅ Found match: '$saavnTitle' by $saavnArtist (ID: $saavnSongId)")

                                        if (saavnSongId != null) {
                                            saavnStreamUrl = runBlocking(Dispatchers.IO) {
                                                saavnAPI.getSongUrl(saavnSongId)
                                            }
                                        }
                                    } else {
                                        Log.d(saavnLogTag, "⚠️ Strategy 2 failed: No results")
                                    }
                                }

                                // Strategy 3: If still no results, try raw title
                                if (saavnStreamUrl.isNullOrEmpty() && rawTitle != cleanTitle) {
                                    Log.d(saavnLogTag, "🔍 Strategy 3: Searching for raw title '$rawTitle'")

                                    val searchResults3: Map<String, Any> = runBlocking(Dispatchers.IO) {
                                        saavnAPI.fetchSearchResults(rawTitle!!, count = 5)
                                    }

                                    @Suppress("UNCHECKED_CAST")
                                    val songs3 = searchResults3["songs"] as? List<Map<String, Any>>
                                    if (!songs3.isNullOrEmpty()) {
                                        val firstSong = songs3[0]
                                        val saavnSongId = firstSong["id"] as? String
                                        val saavnTitle = firstSong["title"] as? String ?: "Unknown"
                                        val saavnArtist = firstSong["artist"] as? String ?: "Unknown"

                                        Log.d(saavnLogTag, "✅ Found match: '$saavnTitle' by $saavnArtist (ID: $saavnSongId)")

                                        if (saavnSongId != null) {
                                            saavnStreamUrl = runBlocking(Dispatchers.IO) {
                                                saavnAPI.getSongUrl(saavnSongId)
                                            }
                                        }
                                    } else {
                                        Log.d(saavnLogTag, "⚠️ Strategy 3 failed: No results")
                                    }
                                }

                                // Check if we got a stream URL
                                if (!saavnStreamUrl.isNullOrEmpty()) {
                                    Log.d(saavnLogTag, "✅ JioSaavn stream URL obtained: ${saavnStreamUrl.take(80)}...")
                                    Log.d(TAG, "✅ Automatic JioSaavn fallback successful - playback will continue")

                                    // Update streaming engine
                                    activeStreamingEngine = "JioSaavn"
                                    Log.d(TAG, "🔍 Detected stream engine: JioSaavn (automatic fallback)")

                                    // Cache the URL (JioSaavn URLs typically expire in 6 hours)
                                    songUrlCache[mediaId] = saavnStreamUrl to (System.currentTimeMillis() + (6 * 60 * 60 * 1000L))

                                    // Return JioSaavn stream URL
                                    return@Factory dataSpecWithKey.withUri(saavnStreamUrl.toUri())
                                } else {
                                    Log.w(saavnLogTag, "❌ All search strategies failed - song not found on JioSaavn")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(saavnLogTag, "❌ JioSaavn fallback failed: ${e.message}", e)
                            Timber.tag(saavnLogTag).e(e, "❌ JioSaavn fallback error")
                        }

                        // If all fallbacks failed (YouTube, NewPipe, and JioSaavn), throw error
                        Log.e(TAG, "❌ All streaming sources failed (YouTube, NewPipe, JioSaavn) - throwing error")
                        throw PlaybackException(
                            "Unable to play this video. All streaming sources (YouTube, NewPipe, JioSaavn) failed.",
                            lastError,
                            PlaybackException.ERROR_CODE_REMOTE_ERROR
                        )
                    }
                }

                // If we got here, YouTube succeeded (playbackData is not null)
                if (playbackData == null) {
                    // This shouldn't happen, but handle it gracefully
                    throw PlaybackException(
                        "Failed to obtain stream URL from any source",
                        null,
                        PlaybackException.ERROR_CODE_REMOTE_ERROR
                    )
                }

                val format = playbackData.format

                database.query {
                    upsert(
                        FormatEntity(
                            id = mediaId,
                            itag = format.itag,
                            mimeType = format.mimeType.split(";")[0],
                            codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                            bitrate = format.bitrate,
                            sampleRate = format.audioSampleRate,
                            contentLength = format.contentLength!!,
                            loudnessDb = playbackData.audioConfig?.loudnessDb,
                            playbackUrl = playbackData.streamUrl
                        )
                    )
                }
                scope.launch(Dispatchers.IO) { recoverSong(mediaId, playbackData) }

                val streamUrl = playbackData.streamUrl

                // Detect and set stream engine
                activeStreamingEngine = detectStreamEngine(streamUrl, mediaId)
                Log.d(TAG, "🔍 Detected stream engine: $activeStreamingEngine")
                Log.d("PlaybackSourceCheck", "PLAYBACK_ENGINE_USED = $activeStreamingEngine")
 
                // VERIFY: Check if selected source matches actual engine
                val expectedEngine = when (currentSource) {
                    StreamSourceManager.StreamType.YOUTUBE -> "YouTube"
                    StreamSourceManager.StreamType.YT_MUSIC -> "YouTube Music"
                    StreamSourceManager.StreamType.JIOSAAVN -> "JioSaavn"
                }
                if (activeStreamingEngine != expectedEngine && activeStreamingEngine != "YouTube Music" && currentSource == StreamSourceManager.StreamType.YT_MUSIC) {
                    // YouTube Music can show as "YouTube" which is acceptable
                    Log.d("PlaybackSourceCheck", "✅ MATCH: Selected ${currentSource.name} → Detected $activeStreamingEngine (acceptable)")
                } else if (activeStreamingEngine == expectedEngine) {
                    Log.d("PlaybackSourceCheck", "✅ MATCH: Selected ${currentSource.name} → Detected $activeStreamingEngine")
                } else {
                    Log.w("PlaybackSourceCheck", "⚠️ MISMATCH: Selected ${currentSource.name} (expected $expectedEngine) → Detected $activeStreamingEngine")
                }

                // Toast removed - stream engine notification disabled
                hasShownStreamingToast = true

                // Cache the URL (YouTube URLs expire based on streamExpiresInSeconds)
                songUrlCache[mediaId] = streamUrl to (System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L))
                // Return full stream URL without chunking for continuous playback
                // Use dataSpecWithKey to ensure cache key is set for proper cache lookup
                return@Factory dataSpecWithKey.withUri(streamUrl.toUri())
            } catch (e: Exception) {
                Timber.tag(ytLogTag).e(e, "YouTube playback error")
                throw e
            }
        }
    }  // End of createStreamingDataSource

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            // Use DefaultExtractorsFactory to support ALL audio formats
            DefaultExtractorsFactory()
                .setConstantBitrateSeekingEnabled(true) // Better seeking for CBR files
                .setAdtsExtractorFlags(0) // Enable AAC ADTS
                .setMp4ExtractorFlags(0) // Enable MP4/M4A
                .setFragmentedMp4ExtractorFlags(0) // Enable fragmented MP4
                .setMatroskaExtractorFlags(0) // Enable MKV/WEBM/OGG
                .setMp3ExtractorFlags(0) // Enable MP3
                .setFlacExtractorFlags(0), // Enable FLAC
        )

    private fun createRenderersFactory() =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ) = DefaultAudioSink
                .Builder(this@MusicService)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioTrackBufferSizeProvider(object : DefaultAudioSink.AudioTrackBufferSizeProvider {
                    override fun getBufferSizeInBytes(
                        minBufferSizeInBytes: Int,
                        encoding: Int,
                        outputMode: Int,
                        pcmFrameSize: Int,
                        sampleRate: Int,
                        bitrate: Int,
                        maxContextualExtraDiscardBytes: Double
                    ): Int {
                        // Slightly reduce buffer size to prevent "too many frames in pipeline" warnings
                        // while staying above the hardware minimum.
                        val defaultSize = DefaultAudioSink.AudioTrackBufferSizeProvider.DEFAULT.getBufferSizeInBytes(
                            minBufferSizeInBytes, encoding, outputMode, pcmFrameSize, sampleRate, bitrate, maxContextualExtraDiscardBytes
                        )
                        return (defaultSize * 0.9).toInt().coerceAtMost(defaultSize).coerceAtLeast(minBufferSizeInBytes)
                    }
                })
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        // 🎯 Session-Independent DSP MUST be first to process raw audio
                        // No session ID dependency - always works!
                        *(listOfNotNull(
                            dspAudioProcessor, // ← Session-independent EQ, Bass, Gain
                            aiDspController?.audioProcessor, // ← AI DSP (if enabled)
                            SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                            SonicAudioProcessor()
                        ).toTypedArray())
                    ),
                )
                .build()
        }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
        val mediaItem =
            eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        if (playbackStats.totalPlayTimeMs >= (
                    dataStore[HistoryDuration]?.times(1000f)
                        ?: 30000f
                    ) &&
            !dataStore.get(PauseListenHistoryKey, false)
        ) {
            database.query {
                incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playbackStats.totalPlayTimeMs,
                        ),
                    )
                } catch (_: SQLException) {
                }
            }
            val PauseRemoteListenHistoryKey = booleanPreferencesKey("pauseRemoteListenHistory")
            if (!dataStore.get(PauseRemoteListenHistoryKey, false)) {
                CoroutineScope(Dispatchers.IO).launch {
                    val playbackUrl = database.format(mediaItem.mediaId).first()?.playbackUrl
                        ?: YTPlayerUtils.playerResponseForMetadata(mediaItem.mediaId, null)
                            .getOrNull()?.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    playbackUrl?.let {
                        YouTube.registerPlayback(null, playbackUrl)
                            .onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }
    }

    private fun saveQueueToDisk(forcePaused: Boolean = false) {
        if (player.playbackState == STATE_IDLE && player.mediaItemCount == 0) {
            filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete()
            filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).delete()
            return
        }

        try {
            val persistQueue =
                PersistQueue(
                    title = queueTitle,
                    items = player.mediaItems.mapNotNull { it.metadata },
                    mediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                    position = if (player.currentPosition >= 0) player.currentPosition else 0,
                )
            val persistAutomix =
                PersistQueue(
                    title = "automix",
                    items = automixItems.value.mapNotNull { it.metadata },
                    mediaItemIndex = 0,
                    position = 0,
                )

            // Guardar estado del reproductor
            // When forcePaused is true (e.g., on app close), always save as paused
            val playerState = PersistPlayerState(
                repeatMode = player.repeatMode,
                shuffleModeEnabled = player.shuffleModeEnabled,
                volume = player.volume,
                currentMediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                currentPosition = if (player.currentPosition >= 0) player.currentPosition else 0,
                playWhenReady = if (forcePaused) false else player.playWhenReady, // Force paused on app close
                playbackState = player.playbackState // Estado actual del reproductor
            )

            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistQueue)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error saving queue to disk", it)
                reportException(it)
            }

            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistAutomix)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error saving automix to disk", it)
                reportException(it)
            }

            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(playerState)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error saving player state to disk", it)
                reportException(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveQueueToDisk", e)
            reportException(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        // Cancel any pending auto-playback prevention job
        autoPlaybackPreventionJob?.cancel()
        autoPlaybackPreventionJob = null
        hasHandledAutoPlaybackPrevention = false

        // Force pause and save paused state before destroying
        if (player.playWhenReady) {
            player.pause()
            player.playWhenReady = false
            Log.d(TAG, "🛑 Pausing playback before service destroy")
        }

        if (dataStore.get(PersistentQueueKey, true)) {
            // Force save as paused when service is being destroyed
            saveQueueToDisk(forcePaused = true)
        }



        // 🎯 Production-Grade Audio Effects Manager Cleanup
        Log.i(TAG, "🧹 Releasing AudioEffectsManager...")
        try {
            audioEffectsManager?.release()
            audioEffectsManager = null
            Log.i(TAG, "✅ AudioEffectsManager released successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error releasing AudioEffectsManager", e)
        }

        // 🔥 HYBRID DSP Cleanup
        Log.i(TAG, "🧹 Cleaning up Hybrid DSP Manager...")
        try {
            hybridDSPManager?.release()
            hybridDSPManager = null
            Log.i(TAG, "✅ Hybrid DSP Manager released")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error releasing Hybrid DSP Manager", e)
        }

        try {
            unregisterReceiver(audioBecomingNoisyReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }

        // Cancel pending jobs
        lastBufferingCheckJob?.cancel()


        // Shutdown background audio processor
        try {
            backgroundAudioProcessor?.shutdown()
            backgroundAudioProcessor = null
            Log.d(TAG, "✅ Background audio processor shut down")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to shutdown background audio processor", e)
        }

        // Unregister screen state receiver
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }

        scope.cancel()

        mediaSession.release()
        player.stop()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        player.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        
        Log.d(TAG, "📱 Task removed (Swipe away) - Clean shutdown requested")
        
        try {
            // 1. Release Media Session FIRST (Crucial for Media3 notification removal)
            // Media3 manages the notification, so we must tell it to release
            if (::mediaSession.isInitialized) {
                try {
                    mediaSession.release()
                } catch (e: Exception) {
                    Log.e(TAG, "Error releasing media session", e)
                }
            }

            // 2. Stop and release player
            if (::player.isInitialized) {
                if (player.isPlaying) {
                     player.pause()
                }
                player.stop()
                player.release()
            }

            // 3. Stop foreground service and remove notification explicitly
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                 stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
            } else {
                 @Suppress("DEPRECATION")
                 stopForeground(true)
            }
            
            // 4. Stop service
            stopSelf()

            // 5. Force kill process after a small delay to allow IPC/Notification calls to complete
            // Killing immediately can prevent the system from removing the notification
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    Log.d(TAG, "💀 Force killing process final cleanup")
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(0)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in final kill", e)
                }
            }, 200) // 200ms delay for cleanup
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping app on task removed", e)
            // Fallback emergency kill
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(0)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaSession
    }



    /**
     * PROFESSIONAL AUDIO REPAIR: Called when user clicks "Fix Audio"
     * Handles audio effects session verification and playback recovery.
     */
    fun restartAudioSession() {
        Log.i(TAG, "🔄 PROFESSIONAL FIX: Restarting audio session requested")
        
        // 1. Verify Audio Effects Session integrity
        // This checks if the session ID has drifted (common issue) and repairs it
        val realSessionId = player.audioSessionId
        if (realSessionId != C.AUDIO_SESSION_ID_UNSET) {
            // audioEffectsManager?.verifyAndRepair(realSessionId) // Removed for Session Independent DSP
        } else {
            // Fallback for unset session
            // audioEffectsManager?.forceReset() // Removed for Session Independent DSP
        }
        
        // 2. Ensure playback is healthy
        if (player.playbackState == Player.STATE_IDLE || player.playerError != null) {
            player.prepare()
        }
        
        if (player.playWhenReady && !player.isPlaying) {
             player.play()
        }
        
        // 3. Re-request focus/foreground if needed (often lost during glitches)
        if (player.isPlaying) {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                 ensureForegroundService()
                 requestAudioFocus()
             }
        }
    }

    /**
     * Get current streaming engine for the active media item
     * Returns: "YouTube", "YouTube Music", "JioSaavn", "Local", or "Unknown"
     */
    fun getCurrentStreamingEngine(): String? = activeStreamingEngine ?: player.currentMediaItem?.let { detectStreamEngine(null, it.mediaId) }

    /**
     * BLOOMEE-STYLE: Get downloaded file for offline playback
     * Similar to Bloomee's BloomeeDBService.getDownloadDB()
     * Checks database for downloaded flag and verifies file exists on disk
     */
    private suspend fun getDownloadedFile(mediaId: String): java.io.File? {
        return try {
            // Check if song is marked as downloaded in database
            val song = try {
                database.song(mediaId).first()
            } catch (e: Exception) {
                Log.d(TAG, "📥 Song $mediaId not found in database")
                null
            }

            // Song is a wrapper containing SongEntity, so access via song.song
            if (song?.song?.dateDownload == null) {
                Log.d(TAG, "📥 Song $mediaId is not marked as downloaded in database")
                return null
            }

            // Get the song entity for title matching
            val songEntity = song.song

            // BLOOMEE-STYLE: Try to find the file in download directories
            // Bloomee checks database flag first (done above), then searches file system
            // Method 1: Check default download directory
            val defaultPath = "/storage/emulated/0/Music/MusiFly/Music"
            val sanitizedTitle = songEntity.title.replace(Regex("[^a-zA-Z0-9\\s-]"), "")

            // Try common file extensions
            val extensions = listOf("mp4", "m4a", "mp3", "webm", "ogg")
            for (ext in extensions) {
                val file = java.io.File(defaultPath, "$sanitizedTitle.$ext")
                if (file.exists()) {
                    Log.d(TAG, "✅ Found downloaded file in default directory: ${file.absolutePath}")
                    return file
                }

                // Also try with quality suffix (e.g., "Song [720p].mp4")
                val fileWithQuality = java.io.File(defaultPath, "$sanitizedTitle [720p].$ext")
                if (fileWithQuality.exists()) {
                    Log.d(TAG, "✅ Found downloaded file with quality: ${fileWithQuality.absolutePath}")
                    return fileWithQuality
                }
            }

            // Method 3: Check custom download directory from settings
            val customPath = try {
                dataStore.data.first()[com.musifly.android.constants.CustomDownloadDirectoryKey]
                    ?: defaultPath
            } catch (e: Exception) {
                defaultPath
            }

            if (customPath != defaultPath) {
                for (ext in extensions) {
                    val file = java.io.File(customPath, "$sanitizedTitle.$ext")
                    if (file.exists()) {
                        Log.d(TAG, "✅ Found downloaded file in custom directory: ${file.absolutePath}")
                        return file
                    }
                }
            }

            Log.d(TAG, "⚠️ Song marked as downloaded but file not found: $mediaId")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting downloaded file: ${e.message}", e)
            null
        }
    }
}