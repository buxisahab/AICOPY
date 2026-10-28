package com.musifly.android.download

import android.content.Context
import android.util.Log
import com.musifly.android.constants.CustomDownloadDirectoryKey
import com.musifly.android.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.io.File
import java.util.UUID

/**
 * Smart Music Downloader - Complete solution for finding and downloading
 * the correct YouTube video based on song title and artist name.
 * 
 * Features:
 * - Intelligent YouTube search with fallback strategies
 * - NewPipe-style stream extraction
 * - Multi-threaded downloading with progress callbacks
 * - Production-ready error handling and recovery
 * 
 * Usage:
 * ```
 * val downloader = SmartMusicDownloader(context)
 * downloader.downloadSong(
 *     songTitle = "Shape of You",
 *     artistName = "Ed Sheeran",
 *     videoQuality = VideoQuality.Q_1080P,
 *     callbacks = myCallbacks
 * )
 * ```
 */
class SmartMusicDownloader(private val context: Context) {
    
    private val TAG = "SmartMusicDownloader"
    
    // User agent matching Firefox 140 ESR (as used in NewPipe v0.28.0)
    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    
    // OkHttp-based downloader for NewPipe
    private inner class OkHttpDownloader : org.schabi.newpipe.extractor.downloader.Downloader() {
        
        private val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        
        override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): org.schabi.newpipe.extractor.downloader.Response {
            val requestBuilder = okhttp3.Request.Builder()
                .url(request.url())
            
            // Add all headers
            request.headers().forEach { (key, values) ->
                values.forEach { value ->
                    requestBuilder.addHeader(key, value)
                }
            }
            
            // Always add User-Agent if not present
            if (!request.headers().containsKey("User-Agent")) {
                requestBuilder.addHeader("User-Agent", USER_AGENT)
            }
            
            // Handle POST/PUT/PATCH body
            val httpMethod = request.httpMethod()
            val requestBody = when {
                httpMethod.equals("POST", ignoreCase = true) || 
                httpMethod.equals("PUT", ignoreCase = true) ||
                httpMethod.equals("PATCH", ignoreCase = true) -> {
                    val bodyData = request.dataToSend()
                    val contentType = request.headers()["Content-Type"]?.firstOrNull() 
                        ?: "application/x-www-form-urlencoded"
                    
                    if (bodyData != null && bodyData.isNotEmpty()) {
                        bodyData.toRequestBody(contentType.toMediaType())
                    } else {
                        ByteArray(0).toRequestBody(contentType.toMediaType())
                    }
                }
                else -> null
            }
            
            requestBuilder.method(httpMethod, requestBody)
            
            val okHttpResponse = client.newCall(requestBuilder.build()).execute()
            
            return org.schabi.newpipe.extractor.downloader.Response(
                okHttpResponse.code,
                okHttpResponse.message,
                okHttpResponse.headers.toMultimap(),
                okHttpResponse.body?.string(),
                okHttpResponse.request.url.toString()
            )
        }
    }
    
    init {
        // Initialize NewPipe downloader (required before extracting streams)
        // Safe to call multiple times - NewPipe silently ignores if already initialized
        try {
            org.schabi.newpipe.extractor.NewPipe.init(OkHttpDownloader())
            Log.d(TAG, "✅ NewPipe initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ NewPipe initialization warning (may already be initialized): ${e.message}")
            // Non-critical - NewPipe might already be initialized
        }
    }
    
    companion object {
        private const val DEFAULT_VIDEO_PATH = "/storage/emulated/0/Music/MusiFly/Videos"
        private const val DEFAULT_AUDIO_PATH = "/storage/emulated/0/Music/MusiFly/Music"
    }
    
    /**
     * Get the output directory for downloads
     * Returns custom directory if set, otherwise default directory
     * Creates the directory if it doesn't exist
     */
    private suspend fun getOutputDirectory(): File = withContext(Dispatchers.IO) {
        // Try to get custom directory from preferences
        val customPath = try {
            context.dataStore.data.first()[CustomDownloadDirectoryKey]
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read custom directory preference: ${e.message}")
            null
        }
        
        val directoryPath = customPath?.takeIf { it.isNotEmpty() } ?: DEFAULT_VIDEO_PATH
        
        // Check if path is a URI (starts with content:// or file://)
        val finalPath = if (directoryPath.startsWith("content://") || directoryPath.startsWith("file://")) {
            // Try to extract file path from URI
            try {
                val uri = android.net.Uri.parse(directoryPath)
                if (uri.scheme == "file") {
                    uri.path ?: DEFAULT_VIDEO_PATH
                } else {
                    // For content:// URIs, try to extract path
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
                            DEFAULT_VIDEO_PATH
                        }
                    } else {
                        DEFAULT_VIDEO_PATH
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse URI: ${e.message}")
                DEFAULT_VIDEO_PATH
            }
        } else {
            directoryPath
        }
        
        val directory = File(finalPath)
        
        // Create directory if it doesn't exist
        if (!directory.exists()) {
            val created = directory.mkdirs()
            if (created) {
                Log.d(TAG, "Created download directory: ${directory.absolutePath}")
            } else {
                Log.e(TAG, "Failed to create download directory: ${directory.absolutePath}")
            }
        }
        
        directory
    }
    
    /**
     * Video quality options
     */
    enum class VideoQuality(val height: Int, val displayName: String) {
        Q_144P(144, "144p"),
        Q_240P(240, "240p"),
        Q_360P(360, "360p"),
        Q_480P(480, "480p"),
        Q_720P(720, "720p HD"),
        Q_1080P(1080, "1080p Full HD"),
        Q_1440P(1440, "1440p 2K"),
        Q_2160P(2160, "2160p 4K"),
        Q_4320P(4320, "4320p 8K")
    }
    
    /**
     * Audio quality options
     */
    enum class AudioQuality(val bitrate: Int, val displayName: String) {
        LOW(64, "64 kbps"),
        MEDIUM(128, "128 kbps"),
        HIGH(192, "192 kbps"),
        VERY_HIGH(256, "256 kbps"),
        EXTREME(320, "320 kbps")
    }
    
    /**
     * Download configuration
     */
    data class DownloadConfig(
        val songTitle: String,
        val artistName: String,
        val videoQuality: VideoQuality = VideoQuality.Q_1080P,
        val audioQuality: AudioQuality = AudioQuality.VERY_HIGH,
        val outputDirectory: File,
        val useYouTubeSearch: Boolean = true,
        val directVideoUrl: String? = null
    )
    
    /**
     * Download a song/video with intelligent search and NewPipe-style downloading
     * 
     * @param songTitle Title of the song
     * @param artistName Artist name
     * @param videoQuality Desired video quality
     * @param audioQuality Desired audio quality
     * @param callbacks Progress and state callbacks
     * @return Mission ID for tracking, or null if failed
     */
    suspend fun downloadSong(
        songTitle: String,
        artistName: String,
        videoQuality: VideoQuality = VideoQuality.Q_1080P,
        audioQuality: AudioQuality = AudioQuality.VERY_HIGH,
        callbacks: DownloadCallbacks? = null
    ): String? {
        val outputDirectory = getOutputDirectory()
        val config = DownloadConfig(
            songTitle = songTitle,
            artistName = artistName,
            videoQuality = videoQuality,
            audioQuality = audioQuality,
            outputDirectory = outputDirectory
        )
        return downloadWithConfig(config, callbacks)
    }
    
    /**
     * Download a song/video directly from YouTube URL
     * 
     * @param videoUrl Direct YouTube video URL
     * @param title Output file title
     * @param videoQuality Desired video quality
     * @param callbacks Progress and state callbacks
     * @return Mission ID for tracking, or null if failed
     */
    suspend fun downloadFromUrl(
        videoUrl: String,
        title: String,
        videoQuality: VideoQuality = VideoQuality.Q_1080P,
        callbacks: DownloadCallbacks? = null
    ): String? {
        val outputDirectory = getOutputDirectory()
        val config = DownloadConfig(
            songTitle = title,
            artistName = "",
            videoQuality = videoQuality,
            outputDirectory = outputDirectory,
            useYouTubeSearch = false,
            directVideoUrl = videoUrl
        )
        return downloadWithConfig(config, callbacks)
    }
    
    /**
     * Main download orchestration with complete configuration
     */
    private suspend fun downloadWithConfig(
        config: DownloadConfig,
        callbacks: DownloadCallbacks?
    ): String? = withContext(Dispatchers.IO) {
        try {
            val missionId = UUID.randomUUID().toString()
            
            // Step 1: Find the correct video
            val videoUrl = if (config.useYouTubeSearch) {
                findCorrectVideo(config, callbacks)
            } else {
                config.directVideoUrl
            } ?: run {
                callbacks?.onError(null, "Could not find video", null)
                return@withContext null
            }
            
            Log.d(TAG, "Video URL: $videoUrl")
            callbacks?.onStreamExtractionStarted(videoUrl)
            
            // Step 2: Extract streams
            val streamInfo = extractStreams(videoUrl, callbacks) ?: run {
                callbacks?.onError(missionId, "Failed to extract streams", null)
                return@withContext null
            }
            
            // Step 3: Select best streams
            val selectedStreams = selectBestStreams(streamInfo, config, callbacks) ?: run {
                callbacks?.onError(missionId, "No suitable streams found", null)
                return@withContext null
            }
            
            // Step 4: Initialize download mission
            val mission = initializeDownloadMission(
                missionId = missionId,
                config = config,
                streams = selectedStreams,
                callbacks = callbacks
            ) ?: run {
                callbacks?.onError(missionId, "Failed to initialize download", null)
                return@withContext null
            }
            
            // Step 5: Start download
            mission.start()
            callbacks?.onDownloadStarted(missionId)
            
            Log.d(TAG, "Download mission $missionId started successfully")
            return@withContext missionId
            
        } catch (e: Exception) {
            Log.e(TAG, "Download failed: ${e.message}", e)
            callbacks?.onError(null, "Download failed: ${e.message}", e)
            return@withContext null
        }
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * Find the correct video using YouTube search
     */
    private suspend fun findCorrectVideo(
        config: DownloadConfig,
        callbacks: DownloadCallbacks?
    ): String? {
        Log.d(TAG, "Searching for: ${config.songTitle} by ${config.artistName}")
        
        val searchResult = YouTubeSearchEngine.findCorrectVideoWithFallback(
            songTitle = config.songTitle,
            artistName = config.artistName
        )
        
        if (searchResult != null) {
            Log.d(TAG, "Found video: ${searchResult.title} by ${searchResult.uploaderName}")
            callbacks?.onCorrectVideoFound(searchResult)
            return searchResult.videoUrl
        }
        
        Log.w(TAG, "Video not found: ${config.songTitle} - ${config.artistName}")
        return null
    }
    
    /**
     * Extract stream information from video URL
     */
    private suspend fun extractStreams(
        videoUrl: String,
        callbacks: DownloadCallbacks?
    ): StreamInfo? = withContext(Dispatchers.IO) {
        try {
            val youtubeService = ServiceList.YouTube
            val streamExtractor = youtubeService.getStreamExtractor(videoUrl)
            streamExtractor.fetchPage()
            
            val streamInfo = StreamInfo.getInfo(streamExtractor)
            Log.d(TAG, "Streams extracted: ${streamInfo.videoStreams.size} video, ${streamInfo.audioStreams.size} audio")
            return@withContext streamInfo
            
        } catch (e: Exception) {
            Log.e(TAG, "Stream extraction failed: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Stream selection result
     */
    private data class SelectedStreams(
        val videoStream: VideoStream?,
        val audioStream: AudioStream?,
        val requiresMerge: Boolean,
        val videoQuality: String,
        val audioQuality: String
    )
    
    /**
     * Select the best video and audio streams based on quality preferences
     */
    private fun selectBestStreams(
        streamInfo: StreamInfo,
        config: DownloadConfig,
        callbacks: DownloadCallbacks?
    ): SelectedStreams? {
        // Since FFmpeg merging is removed, we only select ONE stream.
        // Priority: 
        // 1. If video is requested, try to find a COMBINED stream (has audio).
        // 2. If no combined stream or only audio requested, get BEST audio stream.
        
        var selectedVideoStream: VideoStream? = null
        var selectedAudioStream: AudioStream? = null
        
        // Try to find combined MP4 streams (usually <= 720p or 480p)
        val combinedStreams = streamInfo.videoStreams
            .filter { it.format?.mimeType?.contains("mp4") == true && !it.isVideoOnly }
            .sortedByDescending { it.height }
        
        val combinedAtQuality = combinedStreams.find { it.height >= config.videoQuality.height }
            ?: combinedStreams.firstOrNull()
            
        if (combinedAtQuality != null) {
            selectedVideoStream = combinedAtQuality
        } else {
            // Fallback to audio only
            selectedAudioStream = selectAudioStream(streamInfo, config.audioQuality)
        }
        
        val videoQuality = selectedVideoStream?.resolution ?: "N/A"
        val audioQuality = selectedAudioStream?.averageBitrate?.let { "${it / 1000} kbps" } ?: "N/A"
        
        Log.d(TAG, "Selected stream - Video: $videoQuality, Audio: $audioQuality (No Merge Support)")
        
        callbacks?.onStreamsExtracted(
            hasVideoStream = selectedVideoStream != null,
            hasAudioStream = selectedAudioStream != null,
            requiresMerge = false,
            videoQuality = videoQuality,
            audioQuality = audioQuality
        )
        
        if (selectedVideoStream == null && selectedAudioStream == null) {
            return null
        }
        
        return SelectedStreams(
            videoStream = selectedVideoStream,
            audioStream = selectedAudioStream,
            requiresMerge = false,
            videoQuality = videoQuality,
            audioQuality = audioQuality
        )
    }
    
    /**
     * Select best video stream matching quality preference
     */
    private fun selectVideoStream(streamInfo: StreamInfo, quality: VideoQuality): VideoStream? {
        val requestedHeight = quality.height
        
        // Get all video streams, prefer MP4
        val mp4Streams = streamInfo.videoStreams
            .filter { it.format?.mimeType?.contains("mp4") == true }
        
        // Find streams >= requested quality
        val suitableStreams = mp4Streams.filter { it.height >= requestedHeight }
        
        // Return lowest quality that's still >= requested (bandwidth efficient)
        return suitableStreams.minByOrNull { it.height }
            ?: mp4Streams.maxByOrNull { it.height } // Fallback to highest available
    }
    
    /**
     * Select best audio stream matching quality preference
     */
    private fun selectAudioStream(streamInfo: StreamInfo, quality: AudioQuality): AudioStream? {
        val requestedBitrate = quality.bitrate * 1000 // Convert to bps
        
        // Get all audio streams, prefer M4A
        val m4aStreams = streamInfo.audioStreams
            .filter { it.format?.mimeType?.contains("m4a") == true || it.format?.mimeType?.contains("mp4") == true }
        
        // Find streams >= requested bitrate
        val suitableStreams = m4aStreams.filter { it.averageBitrate >= requestedBitrate }
        
        // Return lowest quality that's still >= requested
        return suitableStreams.minByOrNull { it.averageBitrate }
            ?: m4aStreams.maxByOrNull { it.averageBitrate } // Fallback to highest available
    }
    
    /**
     * Initialize download mission with proper configuration
     */
    private fun initializeDownloadMission(
        missionId: String,
        config: DownloadConfig,
        streams: SelectedStreams,
        callbacks: DownloadCallbacks?
    ): DownloadMission? {
        try {
            // Prepare output file
            val fileName = sanitizeFileName("${config.songTitle}_${config.videoQuality.displayName}.mp4")
            val outputFile = File(config.outputDirectory, fileName)
            config.outputDirectory.mkdirs()
            
            // Collect stream URLs
            val urls = mutableListOf<String>()
            if (streams.videoStream != null) {
                urls.add(streams.videoStream.url ?: streams.videoStream.content ?: "")
            }
            if (streams.audioStream != null) {
                urls.add(streams.audioStream.url ?: streams.audioStream.content ?: "")
            }
            
            if (urls.isEmpty() || urls.any { it.isEmpty() }) {
                Log.e(TAG, "Invalid stream URLs")
                return null
            }
            
            // Calculate total size (stream size not available before download, will be determined during download)
            // Using -1 to indicate unknown size
            val totalSize = -1L
            
            callbacks?.onDownloadInitialized(missionId, fileName, totalSize)
            
            // Create StoredFileHelper for output file
            val storageHelper = StoredFileHelper(outputFile)
            
            // Create download mission with correct constructor
            val mission = DownloadMission(
                urls = urls.toTypedArray(),  // Convert List to Array
                storage = storageHelper,
                kind = 'v',  // 'v' for video download
                postprocessing = null,  // Merging is handled internally by DownloadMission
                context = context
            )
            
            // Set thread count
            mission.threadCount = 3
            
            // Attach progress callbacks
            attachCallbacks(mission, streams, callbacks)
            
            Log.d(TAG, "Download mission initialized: $fileName (size will be determined during download)")
            return mission
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize mission: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Attach progress callbacks to download mission
     */
    private fun attachCallbacks(
        mission: DownloadMission,
        streams: SelectedStreams,
        callbacks: DownloadCallbacks?
    ) {
        // This would require modifying DownloadMission to support callbacks
        // For now, callbacks would be handled via LiveData observation
        // See usage example below for implementation pattern
    }
    
    /**
     * Sanitize filename for filesystem safety
     */
    private fun sanitizeFileName(fileName: String): String {
        return fileName
            .replace(Regex("[^a-zA-Z0-9._\\-() ]"), "_")
            .take(200) // Limit filename length
    }

    /**
     * Resolve streams and start download via DownloadManagerService (foreground service)
     * Recommended for background tasks like SyncWorker
     */
    suspend fun startDownloadInService(
        videoUrl: String,
        title: String,
        artist: String? = null,
        videoQuality: VideoQuality = VideoQuality.Q_1080P,
        thumbnailUrl: String? = null,
        songId: String? = null
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Step 1: Extract streams
                val streamInfo = extractStreams(videoUrl, null) ?: return@withContext false
                
                // Step 2: Select best streams
                // Create dummy config for selection
                val outputDir = getOutputDirectory()
                val config = DownloadConfig(
                    songTitle = title,
                    artistName = artist ?: "",
                    videoQuality = videoQuality,
                    outputDirectory = outputDir
                )
                
                val streams = selectBestStreams(streamInfo, config, null) ?: return@withContext false
                
                // Check if file already exists
                val fileName = sanitizeFileName("${title}_${streams.videoQuality}.mp4")
                val outputFile = File(outputDir, fileName)
                
                if (outputFile.exists() && outputFile.length() > 0) {
                     Log.d(TAG, "File already exists, skipping download: $fileName")
                     return@withContext true
                }
                
                // Step 3: Collect URLs
                val urls = mutableListOf<String>()
                if (streams.videoStream != null) {
                    urls.add(streams.videoStream.url ?: streams.videoStream.content ?: "")
                }
                if (streams.audioStream != null) {
                    urls.add(streams.audioStream.url ?: streams.audioStream.content ?: "")
                }
                
                if (urls.isEmpty()) return@withContext false
                
                // Step 4: Start Service
                DownloadManagerService.startMission(
                    context = context,
                    urls = urls.toTypedArray(),
                    title = title,
                    kind = 'v', // Video
                    quality = streams.videoQuality,
                    artist = artist,
                    thumbnailUrl = thumbnailUrl ?: streamInfo.name, // Fallback if needed, though name isn't thumbnail. 
                    sourceVideoUrl = videoUrl,
                    songId = songId
                )
                Log.d(TAG, "Started service download for: $title")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start download in service", e)
                false
            }
        }
    }
}
