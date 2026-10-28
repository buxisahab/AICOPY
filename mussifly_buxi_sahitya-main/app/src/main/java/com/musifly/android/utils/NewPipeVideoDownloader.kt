package com.musifly.android.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request as OkHttpRequest
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.VideoStream
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * NewPipe-based video downloader
 * Properly configured like official NewPipe app
 */
object NewPipeVideoDownloader {
    
    private const val TAG = "NewPipeVideoDownloader"
    
    // User agent matching Firefox 140 ESR (as used in NewPipe v0.28.0)
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    
    // OkHttp-based downloader matching NewPipe's implementation
    private class OkHttpDownloader : Downloader() {
        
        private val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
        
        override fun execute(request: Request): Response {
            val requestBuilder = OkHttpRequest.Builder()
                .url(request.url())
            
            // Add all headers from NewPipe request
            request.headers().forEach { (key, values) ->
                values.forEach { value ->
                    requestBuilder.addHeader(key, value)
                }
            }
            
            // Always add User-Agent if not already present
            if (!request.headers().containsKey("User-Agent")) {
                requestBuilder.addHeader("User-Agent", USER_AGENT)
            }
            
            // Handle request body for POST/PUT/PATCH
            val httpMethod = request.httpMethod()
            val requestBody = when {
                httpMethod.equals("POST", ignoreCase = true) || 
                httpMethod.equals("PUT", ignoreCase = true) ||
                httpMethod.equals("PATCH", ignoreCase = true) -> {
                    // Get body data from request
                    val bodyData = request.dataToSend()
                    val contentType = request.headers()["Content-Type"]?.firstOrNull() 
                        ?: "application/x-www-form-urlencoded"
                    
                    if (bodyData != null && bodyData.isNotEmpty()) {
                        bodyData.toRequestBody(contentType.toMediaType())
                    } else {
                        // Empty body for POST requests
                        ByteArray(0).toRequestBody(contentType.toMediaType())
                    }
                }
                else -> null
            }
            
            requestBuilder.method(httpMethod, requestBody)
            
            val okHttpResponse = client.newCall(requestBuilder.build()).execute()
            
            val responseHeaders = okHttpResponse.headers.toMultimap()
            val responseBody = okHttpResponse.body?.string()
            
            return Response(
                okHttpResponse.code,
                okHttpResponse.message,
                responseHeaders,
                responseBody,
                okHttpResponse.request.url.toString()
            )
        }
    }
    
    init {
        // Initialize NewPipe with OkHttp downloader
        NewPipe.init(OkHttpDownloader())
    }
    
    /**
     * Video quality enum with support up to 8K
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
        Q_4320P(4320, "4320p 8K");
        
        companion object {
            fun fromString(quality: String): VideoQuality {
                return when (quality.lowercase().replace("p", "")) {
                    "144" -> Q_144P
                    "240" -> Q_240P
                    "360" -> Q_360P
                    "480" -> Q_480P
                    "720" -> Q_720P
                    "1080" -> Q_1080P
                    "1440", "2k" -> Q_1440P
                    "2160", "4k" -> Q_2160P
                    "4320", "8k" -> Q_4320P
                    else -> Q_720P
                }
            }
            
            fun getAllQualities(): List<VideoQuality> {
                return values().toList()
            }
        }
    }
    
    /**
     * Video stream info
     */
    data class VideoStreamInfo(
        val url: String,
        val quality: String,
        val format: String,
        val resolution: String,
        val audioUrl: String? = null, // Deprecated: no longer used for merging
        val needsMerge: Boolean = false // Deprecated: merging no longer supported
    )

    /**
     * Detailed stream option for UI
     */
    data class StreamOption(
        val title: String, // e.g. "1080p (MPEG-4)"
        val resolution: String,
        val format: String,
        val size: Long, // bytes
        val isVideoOnly: Boolean,
        val videoUrl: String,
        val audioUrl: String? = null // for merging
    )

    /**
     * Get detailed available streams with size estimates
     * @return List of StreamOption sorted by quality
     */
    suspend fun getAvailableStreams(videoUrl: String): Result<List<StreamOption>> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("Getting detailed streams for: $videoUrl")
            
            val service = ServiceList.YouTube
            val linkHandler = service.getStreamLHFactory().fromUrl(videoUrl)
            val extractor = service.getStreamExtractor(linkHandler) as YoutubeStreamExtractor
            extractor.fetchPage()
            
            val options = mutableListOf<StreamOption>()
            val duration = extractor.length // seconds
            
            // 1. Combined Streams (Video + Audio)
            extractor.videoStreams.forEach { stream ->
                // Filter for MP4/WebM safely handling nullable format
                val mimeType = stream.format?.mimeType
                if (mimeType?.contains("mp4") == true || mimeType?.contains("webm") == true) {
                    // VideoStream doesn't have averageBitrate, try contentLength directly
                    // Note: contentLength might be -1 if unknown
                    val size = try {
                        -1L
                    } catch (e: Exception) { -1L }
                    
                    val formatName = stream.format?.name ?: "Unknown"
                    val resolution = stream.resolution ?: "Unknown"
                    val videoUrl = stream.url
                    
                    if (videoUrl != null) {
                        options.add(StreamOption(
                            title = "$resolution ($formatName)",
                            resolution = resolution,
                            format = formatName,
                            size = size,
                            isVideoOnly = false,
                            videoUrl = videoUrl,
                            audioUrl = null
                        ))
                    }
                }
            }
            
            // 2. Video-Only Streams (Higher Quality)
            // AudioStream HAS averageBitrate
            val bestAudio = extractor.audioStreams
                .maxByOrNull { it.averageBitrate }
            
            val audioSize = if (bestAudio != null && bestAudio.averageBitrate > 0 && duration > 0) {
                (bestAudio.averageBitrate * duration) / 8
            } else 0L
            
            extractor.videoOnlyStreams.forEach { stream ->
                 // Filter for MP4 safely
                val mimeType = stream.format?.mimeType
                if (mimeType?.contains("mp4") == true) {
                    // Fix: VideoStream doesn't have averageBitrate
                    val videoSize = -1L // size unknown
                    
                    val totalSize = if (videoSize > 0) videoSize + audioSize else -1L
                    val formatName = stream.format?.name ?: "Unknown"
                    val resolution = stream.resolution ?: "Unknown"
                    val videoUrl = stream.url
                    
                    if (videoUrl != null) {
                        options.add(StreamOption(
                            title = "$resolution ($formatName)",
                            resolution = resolution,
                            format = formatName,
                            size = totalSize, // We won't show size if we can't calc it
                            isVideoOnly = true,
                            videoUrl = videoUrl,
                            audioUrl = bestAudio?.url
                        ))
                    }
                }
            }
            
            // Sort by resolution (descending)
            options.sortedByDescending { 
                it.resolution.replace("p", "").toIntOrNull() ?: 0 
            }
        }
    }
    
    /**
     * Get available video qualities for a given video URL
     * Returns list of available qualities sorted from highest to lowest
     */
    suspend fun getAvailableQualities(videoUrl: String): Result<List<VideoQuality>> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("Getting available qualities for: $videoUrl")
            
            val service = ServiceList.YouTube
            val linkHandler = service.getStreamLHFactory().fromUrl(videoUrl)
            val extractor = service.getStreamExtractor(linkHandler) as YoutubeStreamExtractor
            extractor.fetchPage()
            
            val videoStreams = extractor.videoStreams
            
            // Extract unique resolutions
            val availableQualities = videoStreams
                .mapNotNull { stream ->
                    stream.resolution?.replace("p", "")?.toIntOrNull()
                }
                .distinct()
                .mapNotNull { height ->
                    VideoQuality.values().find { it.height == height }
                }
                .sortedByDescending { it.height }
            
            Timber.tag(TAG).d("Available qualities: ${availableQualities.map { it.displayName }}")
            availableQualities
        }
    }
    
    /**
     * Get best VIDEO URL for downloading
     * This extracts the actual VIDEO stream (MP4), NOT audio
     * Priority: videoStreams (mixed video+audio) → videoOnlyStreams (video-only fallback)
     * 
     * @param videoUrl YouTube video URL
     * @param preferredQuality Desired video quality
     * @return Direct video URL for download (highest quality MP4)
     */
    suspend fun getBestVideoUrl(
        videoUrl: String,
        preferredQuality: VideoQuality = VideoQuality.Q_720P
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("🎬 Getting BEST VIDEO URL (not audio)")
            Timber.tag(TAG).d("Video URL: $videoUrl")
            Timber.tag(TAG).d("Requested quality: ${preferredQuality.displayName}")
            
            // Get YouTube service
            val service = ServiceList.YouTube
            val linkHandler = service.getStreamLHFactory().fromUrl(videoUrl)
            val extractor = service.getStreamExtractor(linkHandler) as YoutubeStreamExtractor
            extractor.fetchPage()
            
            // Get video streams (combined video+audio)
            val videoStreams = extractor.videoStreams
            // Get video-only streams (higher quality, needs audio merge)
            val videoOnlyStreams = extractor.videoOnlyStreams
            
            Timber.tag(TAG).d("Found ${videoStreams.size} combined video+audio streams")
            Timber.tag(TAG).d("Found ${videoOnlyStreams.size} video-only streams")
            
            // Filter MP4 only
            val mp4Combined = videoStreams.filter { stream ->
                stream.format?.mimeType?.contains("mp4") == true ||
                stream.format?.suffix == "mp4"
            }
            
            val mp4VideoOnly = videoOnlyStreams.filter { stream ->
                stream.format?.mimeType?.contains("mp4") == true ||
                stream.format?.suffix == "mp4"
            }
            
            Timber.tag(TAG).d("Filtered: ${mp4Combined.size} MP4 combined, ${mp4VideoOnly.size} MP4 video-only")
            
            // PRIORITY 1: Try combined streams (video+audio in one file) at requested quality
            val requestedHeight = preferredQuality.height
            val combinedAtQuality = mp4Combined
                .filter { 
                    val height = it.resolution?.replace("p", "")?.toIntOrNull() ?: 0
                    height >= requestedHeight
                }
                .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
            
            if (combinedAtQuality.isNotEmpty()) {
                val bestCombined = combinedAtQuality.first()
                Timber.tag(TAG).d("✅ BEST VIDEO: Combined stream ${bestCombined.resolution} (has audio)")
                return@runCatching bestCombined.url ?: throw Exception("Video URL is null")
            }
            
            // PRIORITY 2: Try video-only streams (higher quality, needs audio merge later)
            val videoOnlyAtQuality = mp4VideoOnly
                .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
            
            if (videoOnlyAtQuality.isNotEmpty()) {
                val bestVideoOnly = videoOnlyAtQuality.first()
                Timber.tag(TAG).d("✅ BEST VIDEO: Video-only stream ${bestVideoOnly.resolution} (needs audio merge)")
                return@runCatching bestVideoOnly.url ?: throw Exception("Video URL is null")
            }
            
            // FALLBACK: Get highest quality from any combined stream
            val highestCombined = mp4Combined
                .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
                .firstOrNull()
            
            if (highestCombined != null) {
                Timber.tag(TAG).d("⚠️ FALLBACK: Using highest combined stream ${highestCombined.resolution}")
                return@runCatching highestCombined.url ?: throw Exception("Video URL is null")
            }
            
            throw Exception("No MP4 video streams found")
        }
    }
    
    /**
     * Extract video streams from URL
     */
    suspend fun extractVideoStreams(
        videoUrl: String,
        preferredQuality: VideoQuality = VideoQuality.Q_720P
    ): Result<VideoStreamInfo> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("Extracting streams from: $videoUrl")
            Timber.tag(TAG).d("⭐ REQUESTED QUALITY: ${preferredQuality.displayName} (${preferredQuality.height}p)")
            
            // Get YouTube service
            val service = ServiceList.YouTube
            val linkHandler = service.getStreamLHFactory().fromUrl(videoUrl)
            val extractor = service.getStreamExtractor(linkHandler) as YoutubeStreamExtractor
            extractor.fetchPage()
            
            // Get combined streams (video+audio) ONLY. Video-only streams are disabled as merging is removed.
            val combinedStreams = extractor.videoStreams 
            
            // Filter MP4 only
            val mp4Combined = combinedStreams.filter { stream ->
                (stream.format?.mimeType?.contains("mp4") == true ||
                stream.format?.suffix == "mp4") && !stream.isVideoOnly
            }
            
            if (mp4Combined.isEmpty()) {
                throw Exception("No combined MP4 streams found")
            }
            
            // Find best matching quality from combined streams
            val selectedStream = findBestQualityStream(mp4Combined, preferredQuality)
                ?: throw Exception("No suitable stream found for quality: $preferredQuality")
            
            Timber.tag(TAG).d("✅ SELECTED COMBINED STREAM: ${selectedStream.resolution} - ${selectedStream.format?.mimeType}")
            
            VideoStreamInfo(
                url = selectedStream.url ?: throw Exception("Stream URL is null"),
                quality = selectedStream.resolution ?: "unknown",
                format = selectedStream.format?.mimeType ?: "video/mp4",
                resolution = selectedStream.resolution ?: "unknown",
                audioUrl = null,
                needsMerge = false
            )
        }
    }

    /**
     * Search for a video by title and get the direct download link
     */
    suspend fun getDirectDownloadLink(
        title: String,
        quality: VideoQuality = VideoQuality.Q_720P
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Search for video
            val videoUrl = searchVideoByTitle(title).getOrThrow()
            
            // 2. Extract streams
            val streamInfo = extractVideoStreams(videoUrl, quality).getOrThrow()
            
            streamInfo.url
        }
    }

    /**
     * Search for video by title and return the first result's URL
     * Uses multiple fallback strategies for better results
     */
    suspend fun searchVideoByTitle(title: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("Searching for video: $title")
            
            val service = ServiceList.YouTube
            val searchQueries = mutableListOf<String>()
            
            // Build search queries in order of preference
            searchQueries.add(title) // Original
            
            // If title has " - ", try just the song part
            if (title.contains(" - ")) {
                val songPart = title.substringAfterLast(" - ").trim()
                searchQueries.add(songPart)
                searchQueries.add("$songPart song")
            }
            
            // Try adding "song" or "video"
            searchQueries.add("$title song")
            searchQueries.add("$title video")
            
            // Try simplified version (first 3 words)
            val words = title.split(Regex("\\s+|\\s*-\\s*")).filter { it.isNotBlank() }
            if (words.size > 3) {
                searchQueries.add(words.take(3).joinToString(" "))
            }
            
            // Try each search query
            for (query in searchQueries) {
                try {
                    Timber.tag(TAG).d("Trying search: $query")
                    val searchExtractor = service.getSearchExtractor(query)
                    searchExtractor.fetchPage()
                    val items = searchExtractor.initialPage.items
                    
                    if (items.isNotEmpty()) {
                        // Find first video item
                        val videoItem = items.find { it.url != null } 
                            ?: items.firstOrNull()
                        
                        if (videoItem != null) {
                            Timber.tag(TAG).d("✓ Found video: ${videoItem.name} (${videoItem.url})")
                            return@withContext Result.success(videoItem.url)
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).w("Search failed for '$query': ${e.message}")
                    continue
                }
            }
            
            throw Exception("No video found after trying ${searchQueries.size} different search queries for: $title")
        }
    }
    
    /**
     * Find best quality stream matching preferred quality
     * Prioritizes streams at requested quality or higher (not lower)
     */
    private fun findBestQualityStream(
        streams: List<VideoStream>,
        preferredQuality: VideoQuality
    ): VideoStream? {
        Timber.tag(TAG).d("🔍 Looking for quality: ${preferredQuality.height}p")
        
        // Get all streams at or above requested quality
        val qualityStreams = streams.filter { stream ->
            val height = stream.resolution?.replace("p", "")?.toIntOrNull() ?: 0
            height >= preferredQuality.height
        }.sortedBy { stream ->
            stream.resolution?.replace("p", "")?.toIntOrNull() ?: 0
        }
        
        if (qualityStreams.isNotEmpty()) {
            // Return the lowest quality that is still >= requested
            // This gives us the requested quality or the next available higher
            val selectedStream = qualityStreams.first()
            Timber.tag(TAG).d("✓ Found quality: ${selectedStream.resolution} (requested: ${preferredQuality.height}p)")
            return selectedStream
        }
        
        // If no streams at or above requested quality, get the highest available
        Timber.tag(TAG).w("⚠️ Quality ${preferredQuality.height}p not available!")
        val highestStream = streams.maxByOrNull { stream ->
            stream.resolution?.replace("p", "")?.toIntOrNull() ?: 0
        }
        
        highestStream?.let {
            Timber.tag(TAG).w("⚠️ Using HIGHEST available: ${it.resolution} (requested: ${preferredQuality.height}p)")
        }
        
        return highestStream
    }
    
    /**
     * Search for a video by title and download it
     */
    suspend fun downloadVideoByTitle(
        context: Context,
        title: String,
        quality: VideoQuality = VideoQuality.Q_720P,
        onDownloadId: (Long) -> Unit = {}
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("Searching and downloading by title: $title")
            val videoUrl = searchVideoByTitle(title).getOrThrow()
            downloadVideo(context, videoUrl, title, quality, onDownloadId).getOrThrow()
        }
    }
    
    /**
     * Download video using NewPipe Download System (with queue, pause/resume)
     */
    suspend fun downloadVideoWithNewPipe(
        context: Context,
        videoUrl: String,
        title: String,
        quality: VideoQuality = VideoQuality.Q_720P,
        artist: String? = null,
        thumbnailUrl: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("🚀 Starting NewPipe download system")
            Timber.tag(TAG).d("Title: $title")
            Timber.tag(TAG).d("Quality: ${quality.name}")
            Timber.tag(TAG).d("Video URL: $videoUrl")
            
            // Extract stream info
            Timber.tag(TAG).d("Extracting video streams...")
            val streamInfo = extractVideoStreams(videoUrl, quality).getOrThrow()
            
            Timber.tag(TAG).d("✅ Stream extracted successfully")
            Timber.tag(TAG).d("Stream quality: ${streamInfo.quality}")
            Timber.tag(TAG).d("Stream URL: ${streamInfo.url.take(100)}...")
            
            // Prepare URLs array (video + audio if needed)
            val urls = if (streamInfo.needsMerge && streamInfo.audioUrl != null) {
                Timber.tag(TAG).d("📦 Video needs audio merge")
                Timber.tag(TAG).d("Audio URL: ${streamInfo.audioUrl.take(100)}...")
                arrayOf(streamInfo.url, streamInfo.audioUrl)
            } else {
                Timber.tag(TAG).d("✅ Video has audio included")
                arrayOf(streamInfo.url)
            }
            
            // Use provided metadata or extract from video URL
            val finalThumbnail = thumbnailUrl ?: run {
                val videoInfo = getVideoInfo(videoUrl).getOrNull()
                videoInfo?.thumbnailUrl ?: extractThumbnailFromUrl(videoUrl)
            }
            val finalArtist = artist ?: run {
                val videoInfo = getVideoInfo(videoUrl).getOrNull()
                videoInfo?.uploader
            }
            
            // Start download service
            Timber.tag(TAG).d("Starting DownloadManagerService with ${urls.size} stream(s)...")
            com.musifly.android.download.DownloadManagerService.startMission(
                context = context,
                urls = urls,
                title = title,
                kind = 'v',
                quality = streamInfo.quality,
                artist = finalArtist,
                thumbnailUrl = finalThumbnail,
                sourceVideoUrl = videoUrl // Store for URL refresh after expiry
            )
            
            Timber.tag(TAG).d("✅ Download mission started successfully!")
        }.onFailure { error ->
            Timber.tag(TAG).e(error, "❌ Failed to start NewPipe download")
        }
    }

    /**
     * Start download for a specific pre-fetched stream option
     */
    fun startStreamDownload(
        context: Context,
        stream: StreamOption,
        title: String,
        artist: String? = null,
        thumbnailUrl: String? = null,
        sourceVideoUrl: String
    ) {
        val urls = if (stream.audioUrl != null) {
            arrayOf(stream.videoUrl, stream.audioUrl)
        } else {
            arrayOf(stream.videoUrl)
        }
        
        com.musifly.android.download.DownloadManagerService.startMission(
            context = context,
            urls = urls,
            title = title,
            kind = 'v',
            quality = stream.resolution,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            sourceVideoUrl = sourceVideoUrl
        )
    }

    /**
     * Download video using Android DownloadManager
     * ✅ Downloads actual VIDEO (MP4), NEVER audio streams
     */
    suspend fun downloadVideo(
        context: Context,
        videoUrl: String,
        title: String,
        quality: VideoQuality = VideoQuality.Q_720P,
        onDownloadId: (Long) -> Unit = {}
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("🎬 Starting VIDEO download (not audio): $title at ${quality.name}")
            
            // ✅ Get BEST VIDEO URL (actual video stream, not audio)
            val videoStreamUrl = getBestVideoUrl(videoUrl, quality).getOrThrow()
            
            Timber.tag(TAG).d("✅ Video stream URL obtained: ${videoStreamUrl.take(80)}...")
            Timber.tag(TAG).d("📦 This is a VIDEO file (MP4), not an audio file")
            
            // Sanitize filename
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\s-]"), "")
            val fileName = "$sanitizedTitle [${quality.displayName}].mp4"
            
            // Create download request
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(videoStreamUrl)).apply {
                setTitle(title)
                setDescription("Downloading VIDEO (${quality.displayName})")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "Musifly/video/$fileName"
                )
                setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)
                setMimeType("video/mp4")
                
                // Add headers
                addRequestHeader("User-Agent", USER_AGENT)
            }
            
            // Enqueue download
            val downloadId = downloadManager.enqueue(request)
            
            Timber.tag(TAG).d("✅ VIDEO download enqueued with ID: $downloadId")
            Timber.tag(TAG).d("📁 Saved to: Musifly/video/$fileName")
            onDownloadId(downloadId)
            
            downloadId
        }
    }
    
    /**
     * Download audio using DownloadManagerService
     * ✅ Downloads AUDIO ONLY (M4A), NEVER video streams
     * ✅ Downloads appear in Download Manager UI with real-time progress
     */
    suspend fun downloadAudio(
        context: Context,
        videoUrl: String,
        title: String,
        onDownloadId: (Long) -> Unit = {}
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("🎵 Starting AUDIO download (not video): $title")
            
            // Get YouTube service
            val service = ServiceList.YouTube
            val linkHandler = service.getStreamLHFactory().fromUrl(videoUrl)
            val extractor = service.getStreamExtractor(linkHandler) as YoutubeStreamExtractor
            extractor.fetchPage()
            
            // Get audio streams (AUDIO ONLY, not video)
            val audioStreams = extractor.audioStreams
            
            if (audioStreams.isEmpty()) {
                throw Exception("No audio streams found")
            }
            
            Timber.tag(TAG).d("Found ${audioStreams.size} AUDIO streams (not video)")
            
            // Get best quality audio (M4A format preferred)
            val bestAudio = audioStreams
                .filter { it.format?.mimeType?.contains("m4a") == true || it.format?.suffix == "m4a" }
                .maxByOrNull { it.averageBitrate }
                ?: audioStreams.maxByOrNull { it.averageBitrate }
                ?: throw Exception("No suitable audio stream found")
            
            Timber.tag(TAG).d("✅ Selected AUDIO stream: ${bestAudio.averageBitrate} bps - ${bestAudio.format?.mimeType}")
            Timber.tag(TAG).d("🎵 This is an AUDIO file (M4A), not a video file")
            Timber.tag(TAG).d("📦 Using DownloadManagerService for tracking in Download Manager")
            
            // Use DownloadManagerService for proper tracking
            com.musifly.android.download.DownloadManagerService.startMission(
                context = context,
                urls = arrayOf(bestAudio.url ?: throw Exception("Audio URL is null")),
                title = title,
                kind = 'a', // 'a' for Audio
                quality = "Audio ${bestAudio.averageBitrate / 1000}kbps",
                artist = extractor.uploaderName,
                thumbnailUrl = extractor.thumbnails.firstOrNull()?.url,
                sourceVideoUrl = videoUrl // For potential URL refresh
            )
            
            Timber.tag(TAG).d("✅ Audio download started via DownloadManagerService")
            Timber.tag(TAG).d("📁 Will appear in Download Manager UI")
            
            // Return timestamp as download ID
            val timestamp = System.currentTimeMillis()
            onDownloadId(timestamp)
            
            timestamp
        }
    }
    
    /**
     * Get video information without downloading
     */
    suspend fun getVideoInfo(videoUrl: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("Getting video info: $videoUrl")
            
            val service = ServiceList.YouTube
            val linkHandler = service.getStreamLHFactory().fromUrl(videoUrl)
            val extractor = service.getStreamExtractor(linkHandler) as YoutubeStreamExtractor
            extractor.fetchPage()
            
            VideoInfo(
                title = extractor.name ?: "Unknown",
                duration = extractor.length,
                uploader = extractor.uploaderName ?: "Unknown",
                thumbnailUrl = extractor.thumbnails.firstOrNull()?.url ?: "",
                viewCount = extractor.viewCount,
                description = extractor.description?.content ?: ""
            )
        }
    }
    
    /**
     * Extract thumbnail URL from YouTube video URL
     */
    private fun extractThumbnailFromUrl(videoUrl: String): String? {
        return try {
            // Extract video ID from URL
            val videoId = when {
                videoUrl.contains("youtube.com/watch?v=") -> {
                    videoUrl.substringAfter("v=").substringBefore("&")
                }
                videoUrl.contains("youtu.be/") -> {
                    videoUrl.substringAfter("youtu.be/").substringBefore("?")
                }
                else -> null
            }
            
            // Generate YouTube thumbnail URL
            videoId?.let { "https://img.youtube.com/vi/$it/maxresdefault.jpg" }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Video information data class
     */
    data class VideoInfo(
        val title: String,
        val duration: Long,
        val uploader: String,
        val thumbnailUrl: String,
        val viewCount: Long,
        val description: String
    )
}
