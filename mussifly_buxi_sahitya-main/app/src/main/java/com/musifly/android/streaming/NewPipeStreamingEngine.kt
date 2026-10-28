package com.musifly.android.streaming

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.database.StandaloneDatabaseProvider
import timber.log.Timber
import java.io.File

/**
 * NewPipe Streaming Engine
 * ExoPlayer-based streaming system with:
 * - Large buffers for smooth playback
 * - Automatic retry on errors
 * - Network-aware buffering
 * - Caching support
 * 
 * ⚠️ IMPORTANT: This class creates its own ExoPlayer instance.
 * For MusicService, use NewPipeAudioStreamExtractor directly instead.
 * MusicService has a unified ExoPlayer instance that handles all playback.
 * 
 * This class is only used by StreamingPlayerService (separate service).
 * Do NOT use this with MusicService to avoid dual player conflicts.
 */
class NewPipeStreamingEngine(
    private val context: Context
) {
    
    private val TAG = "StreamingEngine"
    
    private var exoPlayer: ExoPlayer? = null
    private var playerListener: Player.Listener? = null
    private var cache: SimpleCache? = null
    
    // Streaming configuration
    companion object {
        // Buffer configuration (in milliseconds)
        const val MIN_BUFFER_MS = 25_000          // 25 seconds minimum buffer
        const val MAX_BUFFER_MS = 60_000          // 60 seconds maximum buffer
        const val BUFFER_FOR_PLAYBACK_MS = 1_000  // 1 second to start playback
        const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000  // 5 seconds after rebuffer
        
        // Cache configuration
        const val CACHE_SIZE_MB = 100L
        
        // Retry configuration
        const val MAX_RETRY_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 2000L
    }
    
    /**
     * Player state listener interface
     */
    interface StreamingListener {
        fun onStreamReady()
        fun onStreamPlaying()
        fun onStreamPaused()
        fun onStreamEnded()
        fun onStreamError(error: String)
        fun onBufferingStarted()
        fun onBufferingEnded()
        fun onPositionChanged(positionMs: Long, durationMs: Long)
    }
    
    private var streamingListener: StreamingListener? = null
    private var currentRetryAttempt = 0
    
    /**
     * Initialize ExoPlayer with optimized settings for streaming
     */
    fun initializePlayer(listener: StreamingListener? = null) {
        Timber.tag(TAG).d("🎵 Initializing streaming engine...")
        
        this.streamingListener = listener
        
        // Initialize cache
        initializeCache()
        
        // Create custom load control with large buffers
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        
        // Create HTTP data source factory with custom user agent
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(10000)
            .setAllowCrossProtocolRedirects(true)
        
        // Create cache data source factory
        val cacheDataSourceFactory = cache?.let { simpleCache ->
            CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setCacheWriteDataSinkFactory(null) // Write to cache
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        } ?: httpDataSourceFactory
        
        // Create media source factory
        val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)
        
        // Build ExoPlayer
        exoPlayer = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                // Handle audio focus
                setAudioAttributes(
                    androidx.media3.common.AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true // Handle audio focus automatically
                )
                
                // Add player listener
                playerListener = createPlayerListener()
                addListener(playerListener!!)
            }
        
        Timber.tag(TAG).d("✅ Streaming engine initialized")
    }
    
    /**
     * Initialize cache for offline playback and faster loading
     */
    private fun initializeCache() {
        try {
            val cacheDir = File(context.cacheDir, "streaming_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val cacheEvictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE_MB * 1024 * 1024)
            val databaseProvider = StandaloneDatabaseProvider(context)
            
            // Release any existing cache lock before creating new instance
            cache?.release()
            cache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)
            
            Timber.tag(TAG).d("✅ Cache initialized: ${cacheDir.absolutePath}")
        } catch (e: Exception) {
            Timber.tag(TAG).w("⚠️ Cache initialization failed: ${e.message}")
            // Continue without cache - streaming will still work
            cache = null
        }
    }
    
    /**
     * Create player listener for handling events
     */
    private fun createPlayerListener(): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        Timber.tag(TAG).d("▶️ Stream ready")
                        streamingListener?.onStreamReady()
                        currentRetryAttempt = 0 // Reset retry counter on success
                    }
                    Player.STATE_BUFFERING -> {
                        Timber.tag(TAG).d("⏳ Buffering...")
                        streamingListener?.onBufferingStarted()
                    }
                    Player.STATE_ENDED -> {
                        Timber.tag(TAG).d("🏁 Stream ended")
                        streamingListener?.onStreamEnded()
                    }
                    Player.STATE_IDLE -> {
                        Timber.tag(TAG).d("⏸️ Stream idle")
                    }
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    Timber.tag(TAG).d("▶️ Playing")
                    streamingListener?.onStreamPlaying()
                } else {
                    Timber.tag(TAG).d("⏸️ Paused")
                    streamingListener?.onStreamPaused()
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                Timber.tag(TAG).e("❌ Playback error: ${error.message}")
                Timber.tag(TAG).e("   Error code: ${error.errorCode}")
                Timber.tag(TAG).e("   Retry attempt: $currentRetryAttempt/$MAX_RETRY_ATTEMPTS")
                
                // Attempt automatic retry
                if (currentRetryAttempt < MAX_RETRY_ATTEMPTS) {
                    currentRetryAttempt++
                    Timber.tag(TAG).d("🔄 Retrying... (attempt $currentRetryAttempt)")
                    
                    // Retry after delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        exoPlayer?.prepare()
                        exoPlayer?.play()
                    }, RETRY_DELAY_MS)
                } else {
                    // Max retries reached
                    streamingListener?.onStreamError(error.message ?: "Playback error")
                }
            }
        }
    }
    
    /**
     * Load and prepare stream for playback
     */
    fun loadStream(streamUrl: String) {
        Timber.tag(TAG).d("📡 Loading stream: ${streamUrl.take(80)}...")
        
        val mediaItem = MediaItem.Builder()
            .setUri(streamUrl)
            .build()
        
        exoPlayer?.apply {
            setMediaItem(mediaItem)
            prepare()
        }
    }
    
    /**
     * Start playback
     */
    fun play() {
        Timber.tag(TAG).d("▶️ Starting playback")
        exoPlayer?.play()
    }
    
    /**
     * Pause playback
     */
    fun pause() {
        Timber.tag(TAG).d("⏸️ Pausing playback")
        exoPlayer?.pause()
    }
    
    /**
     * Stop playback and release resources
     */
    fun stop() {
        Timber.tag(TAG).d("⏹️ Stopping playback")
        exoPlayer?.stop()
    }
    
    /**
     * Seek to position
     */
    fun seekTo(positionMs: Long) {
        Timber.tag(TAG).d("⏩ Seeking to: ${positionMs}ms")
        exoPlayer?.seekTo(positionMs)
    }
    
    /**
     * Get current position
     */
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }
    
    /**
     * Get duration
     */
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }
    
    /**
     * Check if player is playing
     */
    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying ?: false
    }
    
    /**
     * Set volume (0.0 to 1.0)
     */
    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }
    
    /**
     * Get ExoPlayer instance for advanced control
     */
    fun getPlayer(): ExoPlayer? = exoPlayer
    
    /**
     * Release all resources
     * IMPORTANT: Call this when done
     */
    fun release() {
        Timber.tag(TAG).d("🧹 Releasing streaming engine")
        
        playerListener?.let { exoPlayer?.removeListener(it) }
        exoPlayer?.release()
        exoPlayer = null
        
        // Release cache
        cache?.release()
        cache = null
        
        streamingListener = null
        
        Timber.tag(TAG).d("✅ Streaming engine released")
    }
}
