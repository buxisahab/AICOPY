package com.musifly.android.download

import android.content.Context
import android.os.Handler
import android.util.Log
import kotlinx.coroutines.launch
import java.io.File
import kotlin.concurrent.thread

/**
 * Download Mission - Orchestrates the download process
 * 
 * Features:
 * - Multi-threaded downloading (via Downloader class)
 * - Single-stream support (Audio-only)
 * - Pause/Resume support with persistence
 * - Error recovery
 * - App restart resume capability
 */
class DownloadMission : Mission {
    
    companion object {
        private const val TAG = "DownloadMission"
        private const val serialVersionUID = 3L
        
        // Error codes
        const val ERROR_NOTHING = 0
        const val ERROR_FILE_CREATION = 1000
        const val ERROR_HTTP_NO_CONTENT = 204
        const val ERROR_HTTP_FORBIDDEN = 403
        const val ERROR_HTTP_NOT_FOUND = 404
        const val ERROR_HTTP_UNSUPPORTED_RANGE = 416
        const val ERROR_CONNECT_HOST = 1001
        const val ERROR_POSTPROCESSING = 1002
        const val ERROR_POSTPROCESSING_STOPPED = 1003
        const val ERROR_POSTPROCESSING_HOLD = 1004
        const val ERROR_INSUFFICIENT_STORAGE = 1005
        const val ERROR_PROGRESS_LOST = 1006
        const val ERROR_TIMEOUT = 1007
        const val ERROR_RESOURCE_GONE = 410
        
        // Mission states for persistence
        const val STATE_QUEUED = 0
        const val STATE_RUNNING = 1
        const val STATE_PAUSED = 2
        const val STATE_FINISHED = 3
        const val STATE_ERROR = 4
    }
    
    // Mission Data
    var urls: Array<String> = emptyArray()
    var threadCount = 3
    var kind: Char = 'v'
    var errCode = ERROR_NOTHING
    var nearLength: Long = 0
    override var done: Long = 0
    var maxRetry = 3
    private var retryCount = 0
    
    // Metadata for UI display
    var title: String? = null
    var artist: String? = null
    var thumbnailUrl: String? = null
    var quality: String? = null
    
    // URL refresh data (for expired YouTube URLs)
    var sourceVideoUrl: String? = null // Original YouTube video URL for re-extraction
    var urlExpiryTime: Long = 0 // When URLs were extracted
    var urlRefreshAttempts: Int = 0 // Track refresh attempts
    @Transient private val URL_EXPIRY_DURATION = 6 * 60 * 60 * 1000L // 6 hours in milliseconds
    
    // State
    @Transient override var running: Boolean = false
    @Transient override var finished: Boolean = false
    var enqueued = true
    @Transient var psState = 0 // 0=None, 1=Running, 2=Failed
    
    // Persistence state - saved progress before pause/stop
    var savedDone: Long = 0
    var savedLength: Long = 0
    var isPaused = false
    var pausedAt: Long = 0
    
    // Objects
    @Transient var mHandler: Handler? = null
    @Transient var errObject: Exception? = null
    @Transient var context: Context? = null
    @Transient var psAlgorithm: Postprocessing? = null // Legacy placeholder
    @Transient var threads: Array<Thread> = emptyArray() // Legacy compatibility
    @Transient var init: Thread? = null // Legacy compatibility
    
    // Persistence callback - called when mission state changes
    @Transient var onStateChanged: ((DownloadMission) -> Unit)? = null
    
    // UI update callback - fired whenever mission state changes (running/paused/finished)
    // BUG 2 FIX: Immediate UI updates
    @Transient var onUiStateChanged: ((DownloadMission) -> Unit)? = null
    
    // Internal
    @Transient private val downloaders = mutableListOf<Downloader>()
    @Transient private var mainThread: Thread? = null
    @Transient private val progressMap = java.util.concurrent.ConcurrentHashMap<Downloader, Long>()
    @Transient private val lengthMap = java.util.concurrent.ConcurrentHashMap<Downloader, Long>()
    @Transient private var isResuming = false
    @Transient private var lastProgressEventTime = 0L // For throttling progress events
    
    constructor() : super(null, 0, 0, null, null)
    
    constructor(
        urls: Array<String>,
        storage: StoredFileHelper,
        kind: Char,
        postprocessing: Postprocessing?,
        context: Context? = null,
        songId: String? = null
    ) : super(null, 0, System.currentTimeMillis(), storage, songId) {
        this.urls = urls
        this.kind = kind
        this.psAlgorithm = postprocessing
        this.context = context
    }
    
    fun start() {
        if (running || finished) return
        
        // Determine if this is a resume operation
        isResuming = isPaused || done > 0 || savedDone > 0
        
        Log.d(TAG, "${if (isResuming) "Resuming" else "Starting"} mission: ${storage?.getName()} (done: $done, savedDone: $savedDone)")
        
        // BUG 1 FIX: ALWAYS refresh YouTube URLs on resume
        if (isResuming && sourceVideoUrl != null && urls.isNotEmpty() && isYouTubeUrl(urls[0])) {
            Log.w(TAG, "🔄 YouTube Resume detected - refreshing URLs first")
            
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val videoId = getVideoIdFromSource()
                    Log.d(TAG, "Refreshing URLs for video: $videoId")
                    
                    val refreshed = refreshUrls()
                    if (refreshed) {
                        Log.d(TAG, "URLs refreshed, starting download")
                    } else {
                        Log.e(TAG, "URL refresh failed, trying old URLs")
                    }
                    startDownloadInternal()
                } catch (e: Exception) {
                    Log.e(TAG, "URL refresh error", e)
                    startDownloadInternal()
                }
            }
            return
        }
        
        startDownloadInternal()
    }
    
    /**
     * Internal start logic - separated for URL refresh flow
     */
    private fun startDownloadInternal() {
        running = true
        isPaused = false
        errCode = ERROR_NOTHING
        errObject = null
        
        // BUG 2 FIX: Post event to UI BEFORE download starts (instant UI update)
        DownloadEventBus.postStateChangeSync(
            missionId = timestamp,
            newState = DownloadEventBus.DownloadState.DOWNLOADING,
            progress = getProgress()
        )
        
        // Legacy callbacks
        onUiStateChanged?.invoke(this)
        onStateChanged?.invoke(this)
        
        // Only clear downloaders, preserve progress tracking for resume
        downloaders.clear()
        
        // Restore saved progress if resuming
        if (isResuming && savedDone > 0 && done == 0L) {
            done = savedDone
            if (savedLength > 0) length = savedLength
            Log.d(TAG, "Restored saved progress: $done / $length")
        }
        
        mainThread = thread(name = "Mission-${storage?.getName()}") {
            init = Thread.currentThread()
            try {
                if (urls.isEmpty()) throw Exception("No URLs provided")
                
                // For MediaStore URIs, we need to use temp files and then copy
                val storageHelper = storage ?: throw Exception("Storage null")
                val cacheDir = context?.cacheDir ?: File(System.getProperty("java.io.tmpdir"))
                val baseName = storageHelper.getName().substringBeforeLast('.')
                val extension = storageHelper.getType()

                if (urls.size == 1) {
                    // Single stream download - download to temp then copy
                    Log.d(TAG, "Mode: Single Stream")
                    val tempOutput = File(cacheDir, "${baseName}_single_temp.$extension")
                    var downloader: Downloader? = null
                    downloader = Downloader(urls[0], tempOutput, threadCount) { bytes, total ->
                        updateProgress(downloader!!, bytes, total)
                    }
                    downloaders.add(downloader)
                    downloader.start()
                    
                    // Copy temp file to final storage
                    if (running && tempOutput.exists()) {
                        Log.d(TAG, "Copying to final storage...")
                        if (!storageHelper.copyFromFile(tempOutput)) {
                            throw Exception("Failed to copy to storage")
                        }
                        tempOutput.delete()
                        Log.d(TAG, "Copy complete")
                    }
                    
                } else if (urls.size >= 2) {
                    // Multi-stream support removed (FFmpeg dependency removed)
                    Log.e(TAG, "Multi-stream download requested but FFmpeg merging is disabled")
                    throw Exception("Multi-stream download (video+audio) is no longer supported")
                }
                
                if (running) {
                    finished = true
                    
                    // Post FINISHED event to UI
                    DownloadEventBus.postStateChangeSync(
                        missionId = timestamp,
                        newState = DownloadEventBus.DownloadState.FINISHED,
                        progress = 100f
                    )
                    
                    notifyFinished()
                }
                
            } catch (e: Exception) {
                if (running) { // Only report error if not manually stopped
                    Log.e(TAG, "Mission failed", e)
                    errCode = ERROR_CONNECT_HOST
                    errObject = e
                    
                    // Post ERROR event to UI
                    DownloadEventBus.postStateChangeSync(
                        missionId = timestamp,
                        newState = DownloadEventBus.DownloadState.ERROR,
                        errorMessage = e.message
                    )
                    
                    notifyError()
                }
            } finally {
                running = false
                downloaders.clear()
            }
        }
        
        notifyRunning()
    }
    
    fun pause() {
        if (!running) return
        
        Log.d(TAG, "Pausing mission: ${storage?.getName()} at $done / $length")
        
        running = false
        isPaused = true
        pausedAt = System.currentTimeMillis()
        
        // BUG 2 FIX: Post event to UI IMMEDIATELY (before async operations)
        DownloadEventBus.postStateChangeSync(
            missionId = timestamp,
            newState = DownloadEventBus.DownloadState.PAUSED,
            progress = getProgress()
        )
        
        // Save current progress before stopping
        savedDone = done
        savedLength = length
        
        // Stop all downloaders - this will save their state files
        downloaders.forEach { it.stop() }
        
        // Wait a bit for downloaders to finish saving state
        try {
            Thread.sleep(100)
        } catch (e: InterruptedException) {
            // Ignore
        }
        
        mainThread?.interrupt()
        
        // Notify state change for persistence
        onStateChanged?.invoke(this)
        
        // BUG 2 FIX: Notify UI immediately
        onUiStateChanged?.invoke(this)
        
        notifyPaused()
        
        Log.d(TAG, "Mission paused successfully: ${storage?.getName()} (savedDone: $savedDone)")
    }
    
    private fun updateProgress(downloader: Downloader, bytes: Long, total: Long) {
        progressMap[downloader] = bytes
        if (total > 0) lengthMap[downloader] = total
        
        this.done = progressMap.values.sum()
        this.length = lengthMap.values.sum()
        
        // Post progress event to UI (throttled to avoid flooding)
        val now = System.currentTimeMillis()
        if (now - lastProgressEventTime > 200) { // Post every 200ms max
            lastProgressEventTime = now
            DownloadEventBus.postProgressSync(
                missionId = timestamp,
                downloadedBytes = this.done,
                totalBytes = this.length
            )
        }
        
        // Update saved state periodically for crash recovery
        if (this.done > savedDone + (1024 * 1024)) { // Every 1MB
            savedDone = this.done
            savedLength = this.length
            onStateChanged?.invoke(this)
        }
        
        notifyRunning()
    }
    
    // --- State Checks ---
    fun isFinished() = finished
    fun isPsRunning() = psState == 1
    fun isPsFailed() = psState == 2
    fun isPausedState() = isPaused && !running && !finished
    
    fun isCorrupt(): Boolean {
        return errCode != ERROR_NOTHING && 
               errCode != ERROR_POSTPROCESSING_HOLD &&
               retryCount >= maxRetry
    }
    
    fun isInitialized(): Boolean = mainThread == null
    
    fun canResume(): Boolean {
        return !running && !finished && !isCorrupt() && (isPaused || done > 0 || savedDone > 0)
    }
    
    fun resetState(rollback: Boolean, resetError: Boolean, errorCode: Int = ERROR_NOTHING) {
        if (rollback) {
            done = 0
            savedDone = 0
            savedLength = 0
        }
        finished = false
        isPaused = false
        if (resetError) {
            errCode = errorCode
            errObject = null
        }
        // retryCount logic if needed
    }
    
    /**
     * Get current state for persistence
     */
    fun getState(): Int {
        return when {
            finished -> STATE_FINISHED
            errCode != ERROR_NOTHING -> STATE_ERROR
            running -> STATE_RUNNING
            isPaused -> STATE_PAUSED
            else -> STATE_QUEUED
        }
    }
    
    /**
     * Restore state from persistence
     */
    fun restoreFromPersistence(savedDone: Long, savedLength: Long, wasPaused: Boolean) {
        this.savedDone = savedDone
        this.savedLength = savedLength
        this.done = savedDone
        this.length = savedLength
        this.isPaused = wasPaused
        this.running = false
        this.finished = false
        Log.d(TAG, "Restored from persistence: done=$savedDone, length=$savedLength, paused=$wasPaused")
    }
    
    fun hasInvalidStorage(): Boolean {
        return storage == null || storage!!.isInvalid()
    }

    override fun getProgress(): Float {
        if (length <= 0) return 0f
        return (done.toFloat() / length.toFloat()) * 100f
    }
    
    // --- Helpers ---
    fun updateEnqueuedState(enqueued: Boolean) { this.enqueued = enqueued }
    
    /**
     * Check if URL is from YouTube and might need refresh
     * BUG 1 FIX: Detect YouTube URLs
     */
    private fun isYouTubeUrl(url: String): Boolean {
        return url.contains("googlevideo.com") || 
               url.contains("youtube.com") ||
               url.contains("youtu.be")
    }
    
    /**
     * Extract video ID from source URL for logging
     */
    private fun getVideoIdFromSource(): String? {
        val url = sourceVideoUrl ?: return null
        return when {
            url.contains("youtube.com/watch?v=") -> {
                url.substringAfter("v=").substringBefore("&").take(11)
            }
            url.contains("youtu.be/") -> {
                url.substringAfter("youtu.be/").substringBefore("?").take(11)
            }
            else -> null
        }
    }
    
    /**
     * Check if URLs might be expired (YouTube URLs expire after ~6 hours)
     */
    fun isUrlExpired(): Boolean {
        if (urlExpiryTime == 0L) return false
        val ageInMillis = System.currentTimeMillis() - urlExpiryTime
        return ageInMillis > URL_EXPIRY_DURATION
    }
    
    /**
     * Get URL age in hours
     */
    fun getUrlAgeHours(): Long {
        if (urlExpiryTime == 0L) return 0
        return (System.currentTimeMillis() - urlExpiryTime) / (60 * 60 * 1000)
    }
    
    /**
     * Check if error is due to expired URL
     */
    fun isUrlExpiredError(errorCode: Int): Boolean {
        return errorCode == ERROR_HTTP_FORBIDDEN || // 403
               errorCode == ERROR_HTTP_NOT_FOUND ||  // 404
               errorCode == ERROR_RESOURCE_GONE ||   // 410
               errorCode == ERROR_CONNECT_HOST       // Connection failed
    }
    
    /**
     * Refresh expired URLs using NewPipe extractor
     * This re-extracts fresh URLs from YouTube for resume after long pause
     */
    suspend fun refreshUrls(): Boolean {
        if (sourceVideoUrl == null) {
            Log.e(TAG, "Cannot refresh URLs: sourceVideoUrl is null")
            return false
        }
        
        if (urlRefreshAttempts >= 3) {
            Log.e(TAG, "Max URL refresh attempts (3) reached")
            return false
        }
        
        try {
            Log.d(TAG, "Refreshing expired URLs from: $sourceVideoUrl")
            urlRefreshAttempts++
            
            // Use NewPipe extractor to get fresh URLs
            val newPipeDownloader = context?.let { 
                com.musifly.android.utils.NewPipeVideoDownloader
            } ?: run {
                Log.e(TAG, "Context is null, cannot refresh URLs")
                return false
            }
            
            val quality = com.musifly.android.utils.NewPipeVideoDownloader.VideoQuality.fromString(
                this.quality ?: "720p"
            )
            
            val streamInfo = newPipeDownloader.extractVideoStreams(
                sourceVideoUrl!!,
                quality
            ).getOrThrow()
            
            // Update URLs with fresh ones
            val newUrls = if (streamInfo.needsMerge && streamInfo.audioUrl != null) {
                arrayOf(streamInfo.url, streamInfo.audioUrl)
            } else {
                arrayOf(streamInfo.url)
            }
            
            this.urls = newUrls
            this.urlExpiryTime = System.currentTimeMillis()
            
            Log.d(TAG, "✅ URLs refreshed successfully (attempt $urlRefreshAttempts)")
            Log.d(TAG, "New URLs: ${newUrls.joinToString(", ") { it.take(50) + "..." }}")
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh URLs (attempt $urlRefreshAttempts)", e)
            errObject = e
            return false
        }
    }
    
    // --- Notification ---
    private var lastNotifyTime = 0L
    private fun notifyRunning() {
        val now = System.currentTimeMillis()
        if (now - lastNotifyTime > 500) {
            lastNotifyTime = now
            mHandler?.obtainMessage(0, this)?.sendToTarget()
        }
    }
    private fun notifyPaused() = mHandler?.obtainMessage(1, this)?.sendToTarget()
    private fun notifyFinished() = mHandler?.obtainMessage(2, this)?.sendToTarget()
    private fun notifyError() = mHandler?.obtainMessage(3, this)?.sendToTarget()
}

// Compatibility classes
data class MissionRecoveryInfo(val position: Long = 0, val length: Long = 0) : java.io.Serializable
interface Postprocessing : java.io.Serializable {
    val worksOnSameFile: Boolean
    fun cleanupTemporalDir()
    fun setTemporalDir(dir: File)
}
