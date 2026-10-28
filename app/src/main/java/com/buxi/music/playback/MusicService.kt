package com.buxi.music.playback

import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.buxi.music.innertube.YouTube
import com.buxi.music.innertube.models.SongItem
import com.buxi.music.innertube.models.WatchEndpoint
import com.buxi.music.jossredconnect.JossRedClient
import com.buxi.music.constants.EnableNewPipeSourceKey
import com.buxi.music.MainActivity
import com.buxi.music.R
import com.buxi.music.constants.AudioNormalizationKey
import com.buxi.music.constants.AudioQualityKey
import com.buxi.music.constants.AutoLoadMoreKey
import com.buxi.music.constants.AutoSkipNextOnErrorKey
import com.buxi.music.constants.HideExplicitKey
import com.buxi.music.constants.NewPipelineKey
import com.buxi.music.constants.HistoryDuration
import com.buxi.music.constants.MediaSessionConstants.CommandToggleLike
import com.buxi.music.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.buxi.music.constants.MediaSessionConstants.CommandToggleShuffle
import com.buxi.music.constants.PauseListenHistoryKey
import com.buxi.music.constants.PersistentQueueKey
import com.buxi.music.constants.PlayerVolumeKey
import com.buxi.music.constants.RepeatModeKey
import com.buxi.music.constants.ShowLyricsKey
import com.buxi.music.constants.SimilarContent
import com.buxi.music.constants.SkipSilenceKey
import com.buxi.music.db.MusicDatabase
import com.buxi.music.db.entities.Event
import com.buxi.music.db.entities.FormatEntity
import com.buxi.music.db.entities.LyricsEntity
import com.buxi.music.db.entities.RelatedSongMap
import com.buxi.music.di.DownloadCache
import com.buxi.music.di.PlayerCache
import com.buxi.music.extensions.SilentHandler
import com.buxi.music.extensions.collect
import com.buxi.music.extensions.collectLatest
import com.buxi.music.extensions.currentMetadata
import com.buxi.music.extensions.findNextMediaItemById
import com.buxi.music.extensions.mediaItems
import com.buxi.music.extensions.metadata
import com.buxi.music.extensions.toMediaItem
import com.buxi.music.lyrics.LyricsHelper
import com.buxi.music.models.PersistQueue
import com.buxi.music.models.toMediaMetadata
import com.buxi.music.playback.queues.EmptyQueue
import com.buxi.music.playback.queues.ListQueue
import com.buxi.music.playback.queues.Queue
import com.buxi.music.playback.queues.YouTubeQueue
import com.buxi.music.playback.queues.filterExplicit
import com.buxi.music.utils.CoilBitmapLoader
import com.buxi.music.utils.NetworkConnectivityObserver
import com.buxi.music.utils.YTPlayerUtils
import com.buxi.music.utils.dataStore
import com.buxi.music.utils.enumPreference
import com.buxi.music.utils.get
import com.buxi.music.utils.isInternetAvailable
import com.buxi.music.utils.reportException
import com.buxi.music.audio.AudioEffectsManager
import com.buxi.music.audio.DspAudioProcessor
import com.buxi.music.audio.SessionIndependentDspManager
import com.buxi.music.audio.AiDspController
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import com.buxi.music.streaming.NewPipeAudioStreamExtractor
import com.buxi.music.streaming.StreamingEngineManager
import com.buxi.music.streaming.SourceEngine
import timber.log.Timber
import com.buxi.music.constants.UseNewPipeStreamingKey
import com.buxi.music.constants.EnableNewPipeSourceKey
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class, UnstableApi::class)
@AndroidEntryPoint
class MusicService :
    MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    private var scope = CoroutineScope(Dispatchers.Main) + Job()
    private val binder = MusicBinder()

    // ==================== AUDIO FOCUS MANAGEMENT ====================
    // Musifly jaise manual audio focus handling - song auto-pause nahi hoga
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var wasPlayingBeforeAudioFocusLoss = false
    private var hasAudioFocus = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // Audio becoming noisy receiver (headphone unplug)
    private val audioBecomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    Log.d("MusicService", "🔊 Headphone unplugged - pausing playback")
                    player.pause()
                }
            }
        }
    }
    // ================================================================

    // Streaming engine tracking and recovery logic (Musifly Style)
    private val songUrlCache = mutableMapOf<String, Pair<String, Long>>()
    private var automaticRetryCount = 0
    private val MAX_AUTOMATIC_RETRIES = 3
    private var lastRetryMediaId: String? = null
    private var lastRetryErrorCategory: PlayerErrorMapper.ErrorCategory? = null
    private var engineSwitchAttempts = 0
    private val MAX_ENGINE_SWITCH_ATTEMPTS = 2
    private var activeStreamingEngine: String? = null
    private val ytFallbackTried = mutableSetOf<String>()
    private var hasShownStreamingToast = false

    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    private val isNetworkConnected = MutableStateFlow(false)

    private val audioQuality by enumPreference(
        this,
        AudioQualityKey,
        com.buxi.music.constants.AudioQuality.AUTO
    )

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null

    val currentMediaMetadata = MutableStateFlow<com.buxi.music.models.MediaMetadata?>(null)
    private val currentSong =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.stateIn(scope, SharingStarted.Lazily, null)
    private val currentFormat =
        currentMediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }

    private val normalizeFactor = MutableStateFlow(1f)
    val playerVolume = MutableStateFlow(dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f))

    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: Player
    lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    // private var isAudioEffectSessionOpened = false
    // private lateinit var audioEffectsManager: AudioEffectsManager

    // Session-Independent DSP (Musifly-style – works on every device without session ID)
    val dspAudioProcessor: DspAudioProcessor = DspAudioProcessor()
    val sessionDspManager: SessionIndependentDspManager by lazy { SessionIndependentDspManager(dspAudioProcessor) }
    private val aiDspController: AiDspController = AiDspController()


    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())

    override fun onCreate() {
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
                            Log.w("MusicService", "⚠️ Caught ForegroundServiceStartNotAllowedException from Media3. " +
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
                Log.d("MusicService", "✅ Exception handler set up for Android 12+ Media3 compatibility")
            } catch (e: Exception) {
                Log.e("MusicService", "❌ Failed to set up exception handler", e)
            }
        }

        super.onCreate()
        
        // Initialize AudioEffectsManager (Old DSP - Commented out)
        // audioEffectsManager = AudioEffectsManager(this, scope)
        
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            )
                .apply {
                    setSmallIcon(R.drawable.opentune_monochrome)
                },
        )
        // Initialize AudioManager for manual audio focus management (Musifly style)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupAudioFocusRequest()

        // Register audio becoming noisy receiver (headphone unplug handler)
        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            registerReceiver(audioBecomingNoisyReceiver, filter)
            Log.d("MusicService", "✅ Audio noisy receiver registered")
        } catch (e: Exception) {
            Log.e("MusicService", "Failed to register audio noisy receiver", e)
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5000,   // Min buffer: 5s (default 15s) - lower for faster start
                15000,  // Max buffer: 15s (default 50s)
                1000,   // Buffer for playback: 1s (default 2.5s)
                1500    // Buffer for rebuffering: 1.5s (default 5s)
            )
            .setBackBuffer(5000, true) // 5s back buffer for smooth seeking
            .build()

        exoPlayer =
            ExoPlayer
                .Builder(this)
                .setMediaSourceFactory(createMediaSourceFactory())
                .setRenderersFactory(createRenderersFactory())
                .setLoadControl(loadControl)
                .setHandleAudioBecomingNoisy(false) // We handle this manually via BroadcastReceiver
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    false, // Manual audio focus handling - Musifly style, no auto-pause
                )
                .setSeekBackIncrementMs(5000)
                .setSeekForwardIncrementMs(5000)
                .build()
                .apply {
                    addListener(this@MusicService)
                    sleepTimer = SleepTimer(scope, this)
                    addListener(sleepTimer)
                    addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))
                }
        player = object : ForwardingPlayer(exoPlayer) {
            override fun seekToPrevious() {
                if (hasPreviousMediaItem()) {
                    seekToPreviousMediaItem()
                } else {
                    super.seekToPrevious()
                }
            }
        }
        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleLibrary = ::toggleLibrary
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
        
        // Initialize network connectivity observer
        connectivityObserver = NetworkConnectivityObserver(this)
        connectivityObserver.networkStatus.collect(scope) { isConnected ->
            isNetworkConnected.value = isConnected
            if (isConnected && waitingForNetworkConnection.value) {
                waitingForNetworkConnection.value = false
            }
        }

        combine(playerVolume, normalizeFactor) { playerVolume, normalizeFactor ->
            playerVolume * normalizeFactor
        }.collectLatest(scope) {
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
                exoPlayer.skipSilenceEnabled = it
            }

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) ->
            normalizeFactor.value =
                if (normalizeAudio && format?.loudnessDb != null) {
                    min(10f.pow(-format.loudnessDb.toFloat() / 20), 1f)
                } else {
                    1f
                }
        }


        if (dataStore.get(PersistentQueueKey, true)) {
            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                playQueue(
                    queue =
                        ListQueue(
                            title = queue.title,
                            items = queue.items.map { it.toMediaItem() },
                            startIndex = queue.mediaItemIndex,
                            position = queue.position,
                        ),
                    playWhenReady = false,
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
        }

        // Save queue periodically to prevent queue loss from crash or force kill
        scope.launch {
            while (isActive) {
                delay(30.seconds)
                if (dataStore.get(PersistentQueueKey, true)) {
                    saveQueueToDisk()
                }
            }
        }
    }

    private fun updateNotification() {
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
        if (!scope.isActive) scope = CoroutineScope(Dispatchers.Main) + Job()
        currentQueue = queue
        queueTitle = null
        player.shuffleModeEnabled = false
        if (queue.preloadItem != null) {
            player.setMediaItem(queue.preloadItem!!.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }
        scope.launch(SilentHandler) {
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
                player.prepare()
                player.playWhenReady = playWhenReady
            }
        }
    }

    fun startRadioSeamlessly() {
        val currentMediaMetadata = player.currentMetadata ?: return
        if (player.currentMediaItemIndex > 0) player.removeMediaItems(
            0,
            player.currentMediaItemIndex
        )
        if (player.currentMediaItemIndex <
            player.mediaItemCount - 1
        ) {
            player.removeMediaItems(player.currentMediaItemIndex + 1, player.mediaItemCount)
        }
        scope.launch(SilentHandler) {
            val radioQueue =
                YouTubeQueue(endpoint = WatchEndpoint(videoId = currentMediaMetadata.id))
            val initialStatus = radioQueue.getInitialStatus()
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
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
        if (dataStore[SimilarContent] == true) {
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
        filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
        automixItems.value = emptyList()
    }

    fun playNext(items: List<MediaItem>) {
        player.addMediaItems(
            if (player.mediaItemCount == 0) 0 else player.currentMediaItemIndex + 1,
            items
        )
        player.prepare()
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
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLike())
            }
        }
    }



    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        // Nonstop (Autoplay) logic - Musifly style
        if (mediaItem != null && player.mediaItemCount > 0) {
            val isLastItem = player.currentMediaItemIndex == player.mediaItemCount - 1
            if (isLastItem) {
                handleAutoplay(mediaItem.mediaId)
            }
        }

        // Auto load more songs for existing queues
        if (dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            currentQueue.hasNextPage()
        ) {
            scope.launch(SilentHandler) {
                val mediaItems =
                    currentQueue.nextPage().filterExplicit(dataStore.get(HideExplicitKey, false))
                if (player.playbackState != STATE_IDLE) {
                    player.addMediaItems(mediaItems.drop(1))
                }
            }
        }
    }

    /**
     * Handle nonstop music (autoplay) by fetching and appending similar songs.
     * Musifly style implementation.
     */
    private fun handleAutoplay(mediaId: String?) {
        if (mediaId == null) return

        scope.launch(Dispatchers.IO) {
            try {
                // Check if Similar Content (Autoplay) is enabled
                val isAutoplayEnabled = dataStore.get(SimilarContent, true)

                if (!isAutoplayEnabled) {
                    return@launch
                }

                Log.d("MusicService", "🔄 Nonstop Music: Fetching similar content for $mediaId")

                // Fetch recommended songs (Watch Next endpoint)
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
                                Log.d("MusicService", "✅ Nonstop: Appended ${newItems.size} similar songs to queue")
                                
                                // Optional: Show a toast like Musifly
                                com.buxi.music.utils.Toast.makeText(
                                    this@MusicService,
                                    "Nonstop: Added similar songs",
                                    com.buxi.music.utils.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MusicService", "❌ Nonstop music error: ${e.message}", e)
            }
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        if (playbackState == STATE_IDLE) {
            currentQueue = EmptyQueue
            player.shuffleModeEnabled = false
            queueTitle = null
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        // Musifly style: Request audio focus when playing starts, abandon when stopped
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isPlaying) {
                requestAudioFocus()
            } else if (player.playbackState == Player.STATE_IDLE ||
                       player.playbackState == Player.STATE_ENDED) {
                abandonAudioFocus()
            }
        }
    }

    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            val isBufferingOrReady =
                player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            // Keep audio effects session open when playing
            if (isBufferingOrReady && player.playWhenReady) {
                // Audio effects are now handled by SessionIndependentDspManager
            } else if (player.playbackState == Player.STATE_IDLE) {
                // Session cleanup handled bySessionIndependentDspManager
            }
        }
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            // Always put current playing item at first
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] =
                shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            exoPlayer.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        val currentMediaItem = player.currentMediaItem
        val mediaId = currentMediaItem?.mediaId
        
        val streamType = when {
            mediaId?.startsWith("local_") == true -> "local"
            mediaId?.length == 11 -> "youtube"
            mediaId?.all { it.isDigit() } == true -> "saavn"
            else -> "unknown"
        }

        val errorCategory = PlayerErrorMapper.categorizeError(error, streamType)
        val userMessage = PlayerErrorMapper.getUserFriendlyMessage(errorCategory, streamType)
        val currentPosition = player.currentPosition

        Log.e("MusicService", "❌ Playback Error: ${error.errorCodeName}")

        if (PlayerErrorMapper.isRecoverable(errorCategory) && mediaId != null) {
            if (mediaId != lastRetryMediaId || errorCategory != lastRetryErrorCategory) {
                automaticRetryCount = 0
                lastRetryMediaId = mediaId
                lastRetryErrorCategory = errorCategory
            }

            if (automaticRetryCount < MAX_AUTOMATIC_RETRIES) {
                automaticRetryCount++
                when (errorCategory) {
                    PlayerErrorMapper.ErrorCategory.URL_EXPIRED -> {
                        songUrlCache.remove(mediaId)
                        refreshCurrentStream(mediaId, currentPosition)
                    }
                    PlayerErrorMapper.ErrorCategory.CORRUPTED_CACHE -> {
                        playerCache.removeResource(mediaId)
                        downloadCache.removeResource(mediaId)
                        refreshCurrentStream(mediaId, currentPosition)
                    }
                    PlayerErrorMapper.ErrorCategory.NETWORK_ERROR -> {
                        scope.launch {
                            waitingForNetworkConnection.value = true
                            delay(2000)
                            refreshCurrentStream(mediaId, currentPosition)
                        }
                    }
                    PlayerErrorMapper.ErrorCategory.SERVER_BAD_RESPONSE -> {
                        if (engineSwitchAttempts < MAX_ENGINE_SWITCH_ATTEMPTS) {
                            engineSwitchAttempts++
                            switchStreamingEngine(mediaId, currentPosition)
                        } else {
                            player.seekToNext()
                        }
                    }
                    else -> refreshCurrentStream(mediaId, currentPosition)
                }
                
                if (!hasShownStreamingToast) {
                    mainHandler.post {
                        com.buxi.music.utils.Toast.makeText(this, userMessage, com.buxi.music.utils.Toast.LENGTH_SHORT).show()
                    }
                    hasShownStreamingToast = true
                    mainHandler.postDelayed({ hasShownStreamingToast = false }, 5000)
                }
                return
            }
        }

        if (errorCategory == PlayerErrorMapper.ErrorCategory.UNSUPPORTED_FORMAT && streamType == "youtube") {
            handleYouTubeUnsupportedFormat(mediaId!!, currentPosition)
        } else {
            mainHandler.post {
                com.buxi.music.utils.Toast.makeText(this, "Playback failed: $userMessage", com.buxi.music.utils.Toast.LENGTH_LONG).show()
            }
            if (dataStore.get(AutoSkipNextOnErrorKey, true)) {
                player.seekToNextMediaItem()
            }
        }
    }

    private fun refreshCurrentStream(mediaId: String, currentPosition: Long) {
        scope.launch(Dispatchers.IO) {
            val newUrl = fetchFreshStreamUrl(mediaId)
            if (newUrl != null) {
                withContext(Dispatchers.Main) {
                    val currentItems = player.mediaItems
                    val index = player.currentMediaItemIndex
                    if (index != -1 && currentItems[index].mediaId == mediaId) {
                        val updatedItem = currentItems[index].buildUpon()
                            .setUri(newUrl.toUri())
                            .build()
                        player.replaceMediaItem(index, updatedItem)
                        player.prepare()
                        player.seekTo(currentPosition)
                        player.play()
                    }
                }
            }
        }
    }

    private suspend fun fetchFreshStreamUrl(mediaId: String): String? {
        return when {
            mediaId.length == 11 -> {
                NewPipeAudioStreamExtractor.getBestAudioStream("https://www.youtube.com/watch?v=$mediaId")
                    .getOrNull()?.url
            }
            else -> StreamingEngineManager.getInstance().getSaavnStreamUrl(mediaId)
        }
    }

    private fun switchStreamingEngine(mediaId: String, currentPosition: Long) {
        refreshCurrentStream(mediaId, currentPosition)
    }

    private fun handleYouTubeUnsupportedFormat(mediaId: String, currentPosition: Long) {
        if (ytFallbackTried.contains(mediaId)) {
            player.seekToNextMediaItem()
            return
        }
        ytFallbackTried.add(mediaId)
        refreshCurrentStream(mediaId, currentPosition)
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
                            OkHttpDataSource.Factory(
                                OkHttpClient
                                    .Builder()
                                    .proxy(YouTube.proxy)
                                    .build(),
                            ),
                        ),
                    ),
            ).setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    private fun createDataSourceFactory(): DataSource.Factory {
        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")

            // 1. Check Caches
            if (downloadCache.isCached(mediaId, dataSpec.position, if (dataSpec.length >= 0) dataSpec.length else 1) ||
                playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)
            ) {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec
            }

            // 2. Check Class-level URL Cache
            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            // 3. JioSaavn JS ID handling
            if (mediaId.startsWith("js:")) {
                val realId = mediaId.removePrefix("js:")
                val streamUrl = runBlocking(Dispatchers.IO) {
                    StreamingEngineManager.getInstance().getSaavnStreamUrl(realId)
                }
                if (streamUrl != null) {
                    songUrlCache[mediaId] = streamUrl to System.currentTimeMillis() + (3600 * 1000L)
                    return@Factory dataSpec.withUri(streamUrl.toUri())
                }
            }

            // 4. Primary YouTube Source (InnerTube or NewPipe)
            try {
                val useNewPipe = dataStore.get(UseNewPipeStreamingKey, true)
                val playbackUrl = runBlocking(Dispatchers.IO) {
                    if (useNewPipe) {
                        Timber.tag("MusicService").d("🎵 Using NewPipe for extraction: $mediaId")
                        NewPipeAudioStreamExtractor.getBestAudioStream("https://www.youtube.com/watch?v=$mediaId")
                            .getOrNull()?.url
                    } else {
                        val playbackData = withTimeoutOrNull(10000) {
                            YTPlayerUtils.playerResponseForPlayback(
                                mediaId,
                                audioQuality = audioQuality,
                                connectivityManager = connectivityManager,
                                newPipeline = true,
                            )
                        }?.getOrNull()
                        playbackData?.streamUrl
                    }
                }

                if (playbackUrl != null) {
                    songUrlCache[mediaId] = playbackUrl to System.currentTimeMillis() + (3600 * 1000L)
                    scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                    return@Factory dataSpec.withUri(playbackUrl.toUri())
                }
            } catch (e: Exception) {
                Timber.tag("MusicService").e(e, "Primary source failed, trying fallbacks")
            }

            // 5. Final Fallbacks (JossRed / Saavn Search)
            val fallbackUrl = runBlocking(Dispatchers.IO) {
                // Try JossRed
                try {
                    JossRedClient.getStreamingUrl(mediaId)
                } catch (e: Exception) {
                    // Try Saavn Search as last resort
                    val song = database.getSongById(mediaId)
                    if (song != null) {
                        val query = "${song.song.title} ${song.artists.firstOrNull()?.name ?: ""}"
                        val results = StreamingEngineManager.getInstance().searchSongsFromSaavn(query, 1)
                        results.firstOrNull()?.let { StreamingEngineManager.getInstance().getSaavnStreamUrl(it.id) }
                    } else null
                }
            }

            if (fallbackUrl != null) {
                songUrlCache[mediaId] = fallbackUrl to System.currentTimeMillis() + (3600 * 1000L)
                return@Factory dataSpec.withUri(fallbackUrl.toUri())
            }

            throw Exception("All streaming sources failed for $mediaId")
        }
    }

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            ExtractorsFactory {
                arrayOf(MatroskaExtractor(), FragmentedMp4Extractor())
            },
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
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        // Session-Independent DSP chain – no session ID needed, always works
                        *listOf(
                            dspAudioProcessor,
                            aiDspController.audioProcessor
                        ).toTypedArray(),
                        SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                        SonicAudioProcessor(),
                    ),
                ).build()
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

    private fun saveQueueToDisk() {
        if (player.playbackState == STATE_IDLE) {
            filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete()
            filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            return
        }
        val persistQueue =
            PersistQueue(
                title = queueTitle,
                items = player.mediaItems.mapNotNull { it.metadata },
                mediaItemIndex = player.currentMediaItemIndex,
                position = player.currentPosition,
            )
        val persistAutomix =
            PersistQueue(
                title = "automix",
                items = automixItems.value.mapNotNull { it.metadata },
                mediaItemIndex = 0,
                position = 0,
            )
        runCatching {
            filesDir.resolve(PERSISTENT_QUEUE_FILE).outputStream().use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(persistQueue)
                }
            }
        }.onFailure {
            reportException(it)
        }
        runCatching {
            filesDir.resolve(PERSISTENT_AUTOMIX_FILE).outputStream().use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(persistAutomix)
                }
            }
        }.onFailure {
            reportException(it)
        }
    }

    override fun onDestroy() {
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        player.release()
        // Release audio effects manager (Old DSP - Commented out)
        // audioEffectsManager.release()
        // Unregister audio becoming noisy receiver
        try {
            unregisterReceiver(audioBecomingNoisyReceiver)
        } catch (e: Exception) {
            Log.e("MusicService", "Failed to unregister audio noisy receiver", e)
        }
        // Abandon audio focus on destroy
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            abandonAudioFocus()
        }
        super.onDestroy()
    }

    // ==================== AUDIO FOCUS METHODS (Musifly Style) ====================

    private fun setupAudioFocusRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        handleAudioFocusChange(focusChange)
                    }
                }
                .setAcceptsDelayedFocusGain(true)
                .build()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleAudioFocusChange(focusChange: Int) {
        Log.d("MusicService", "🎵 Audio focus changed: $focusChange (wasPlaying: $wasPlayingBeforeAudioFocusLoss)")

        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                // Restore normal volume
                player.volume = playerVolume.value
                // Resume playback if it was playing before focus loss
                if (wasPlayingBeforeAudioFocusLoss) {
                    Log.d("MusicService", "✅ Audio focus regained - resuming playback")
                    mainHandler.postDelayed({
                        if (!player.isPlaying && wasPlayingBeforeAudioFocusLoss) {
                            player.play()
                        }
                        wasPlayingBeforeAudioFocusLoss = false
                    }, 100)
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                // Permanent loss - another app started playing
                if (player.isPlaying) {
                    Log.d("MusicService", "⚠️ Audio focus lost permanently - pausing")
                    wasPlayingBeforeAudioFocusLoss = true
                    player.pause()
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                // Temporary loss (e.g. notification sound) - pause and auto-resume
                if (player.isPlaying) {
                    wasPlayingBeforeAudioFocusLoss = true
                    Log.d("MusicService", "⏸️ Audio focus lost temporarily - pausing, will resume")
                    player.pause()
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasAudioFocus = false
                // Duck: reduce volume instead of pausing (Musifly style)
                if (player.isPlaying) {
                    wasPlayingBeforeAudioFocusLoss = true
                    Log.d("MusicService", "🔉 Audio focus ducked - reducing volume (NOT pausing)")
                    player.volume = (playerVolume.value * 0.2f) // Reduce to 20%
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                hasAudioFocus = true
                player.volume = playerVolume.value
                if (wasPlayingBeforeAudioFocusLoss) {
                    Log.d("MusicService", "✅ Transient focus regained - resuming")
                    mainHandler.postDelayed({
                        if (!player.isPlaying && wasPlayingBeforeAudioFocusLoss) {
                            player.play()
                        }
                        wasPlayingBeforeAudioFocusLoss = false
                    }, 100)
                }
                lastAudioFocusState = focusChange
            }

            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                hasAudioFocus = true
                player.volume = playerVolume.value
                Log.d("MusicService", "🔊 Audio focus regained (may duck) - volume restored")
                lastAudioFocusState = focusChange
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    internal fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) {
            Log.d("MusicService", "✅ Audio focus already granted")
            return true
        }
        audioFocusRequest?.let { request ->
            try {
                val result = audioManager.requestAudioFocus(request)
                hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                if (hasAudioFocus) {
                    Log.d("MusicService", "✅ Audio focus granted")
                } else {
                    Log.w("MusicService", "⚠️ Audio focus request denied (result: $result)")
                }
                return hasAudioFocus
            } catch (e: Exception) {
                Log.e("MusicService", "❌ Error requesting audio focus", e)
                return false
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            audioFocusRequest?.let { request ->
                try {
                    audioManager.abandonAudioFocusRequest(request)
                    hasAudioFocus = false
                    Log.d("MusicService", "✅ Audio focus abandoned")
                } catch (e: Exception) {
                    Log.e("MusicService", "❌ Error abandoning audio focus", e)
                    hasAudioFocus = false
                }
            }
        }
    }
    // ==============================================================

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    /**
     * Immediately calls startForeground() the moment the service receives a start command.
     * This prevents ForegroundServiceStartNotAllowedException on Android 12+ where
     * Media3's MediaNotificationManager may call startForegroundService() while the app
     * is in the background.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure we enter foreground immediately — before Media3 tries
        ensureServiceForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Override Media3's notification update to catch ForegroundServiceStartNotAllowedException
     * gracefully (Android 12+ restriction) instead of crashing.
     */
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        try {
            super.onUpdateNotification(session, startInForegroundRequired)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is android.app.ForegroundServiceStartNotAllowedException) {
                Log.w("MusicService", "Suppressed ForegroundServiceStartNotAllowedException — app is in background")
            } else {
                throw e
            }
        }
    }

    private fun ensureServiceForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            nm.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.opentune_monochrome)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Preparing playback…")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w("MusicService", "startForeground failed in ensureServiceForeground", e)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession
    
    // Get AudioEffectsManager for external access (Old DSP - Commented out)
    // fun getAudioEffectsManager(): AudioEffectsManager = audioEffectsManager

    // Get SessionIndependentDspManager for UI access
    fun getDspManager(): SessionIndependentDspManager = sessionDspManager

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val ERROR_CODE_NO_STREAM = 1000001
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"
    }
}
