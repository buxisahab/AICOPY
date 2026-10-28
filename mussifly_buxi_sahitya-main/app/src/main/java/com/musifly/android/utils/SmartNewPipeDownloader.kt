package com.musifly.android.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.VideoStream
import timber.log.Timber
import java.io.IOException

/**
 * Smart NewPipe Downloader
 * Automatically searches for video using metadata and downloads
 */
object SmartNewPipeDownloader {
    
    private const val TAG = "SmartNewPipeDownloader"
    
    // Custom downloader using OkHttp (same as innertube module)
    private class NewPipeDownloaderImpl : Downloader() {
        
        private val client = OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .build()
        
        @Throws(IOException::class, ReCaptchaException::class)
        override fun execute(request: Request): Response {
            val httpMethod = request.httpMethod()
            val url = request.url()
            val headers = request.headers()
            val dataToSend = request.dataToSend()
            
            val requestBuilder = okhttp3.Request.Builder()
                .method(httpMethod, dataToSend?.toRequestBody())
                .url(url)
                .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB)
            
            headers.forEach { (headerName, headerValueList) ->
                if (headerValueList.size > 1) {
                    requestBuilder.removeHeader(headerName)
                    headerValueList.forEach { headerValue ->
                        requestBuilder.addHeader(headerName, headerValue)
                    }
                } else if (headerValueList.size == 1) {
                    requestBuilder.header(headerName, headerValueList[0])
                }
            }
            
            val response = client.newCall(requestBuilder.build()).execute()
            
            if (response.code == 429) {
                response.close()
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }
            
            val responseBodyToReturn = response.body?.string()
            val latestUrl = response.request.url.toString()
            
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBodyToReturn,
                latestUrl
            )
        }
        
    }
    
    init {
        // Initialize NewPipe with our custom downloader
        try {
            NewPipe.init(NewPipeDownloaderImpl())
            Timber.tag(TAG).d("NewPipe initialized successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize NewPipe")
        }
    }
    
    /**
     * Search for video using metadata (title + artist)
     * Returns the best matching video URL
     */
    suspend fun searchVideoByMetadata(
        title: String,
        artist: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val searchQuery = if (artist != null) {
                "$artist - $title"
            } else {
                title
            }
            
            Timber.tag(TAG).d("Searching for: $searchQuery")
            
            // Use YouTube service - use simple string search
            val service = ServiceList.YouTube
            val searchExtractor = service.getSearchExtractor(searchQuery)
            searchExtractor.fetchPage()
            
            // Get first video result
            val items = searchExtractor.initialPage.items
            if (items.isEmpty()) {
                throw Exception("No results found for: $searchQuery")
            }
            
            val firstResult = items.first()
            val videoUrl = firstResult.url
            Timber.tag(TAG).d("Found video: $videoUrl")
            
            videoUrl
        }
    }
    
    /**
     * Download audio from current playing song
     * Automatically searches using metadata
     */
    suspend fun downloadAudioFromMetadata(
        context: Context,
        title: String,
        artist: String? = null,
        onDownloadId: (Long) -> Unit = {}
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("Starting audio download for: $title by $artist")
            
            // Step 1: Search for video
            val videoUrl = searchVideoByMetadata(title, artist).getOrThrow()
            
            // Step 2: Extract audio stream
            val service = ServiceList.YouTube
            val linkHandler = service.getStreamLHFactory().fromUrl(videoUrl)
            val extractor = service.getStreamExtractor(linkHandler) as YoutubeStreamExtractor
            extractor.fetchPage()
            
            // Get best audio stream (prefer M4A)
            val audioStreams = extractor.audioStreams
            if (audioStreams.isEmpty()) {
                throw Exception("No audio streams found")
            }
            
            val bestAudio = audioStreams
                .filter { it.format?.mimeType?.contains("m4a") == true || it.format?.suffix == "m4a" }
                .maxByOrNull { it.averageBitrate }
                ?: audioStreams.maxByOrNull { it.averageBitrate }
                ?: throw Exception("No suitable audio stream found")
            
            Timber.tag(TAG).d("Selected audio: ${bestAudio.averageBitrate} bps")
            
            // Step 3: Use DownloadManagerService (NewPipe Downloader) for FAST multi-threaded download
            com.musifly.android.download.DownloadManagerService.startMission(
                context = context,
                urls = arrayOf(bestAudio.url ?: throw Exception("Audio URL is null")),
                title = title,
                kind = 'a',
                quality = "Audio ${bestAudio.averageBitrate / 1000}kbps",
                artist = artist ?: extractor.uploaderName,
                thumbnailUrl = extractor.thumbnails.firstOrNull()?.url,
                sourceVideoUrl = videoUrl
            )
            
            val timestamp = System.currentTimeMillis()
            onDownloadId(timestamp)
            
            timestamp
        }
    }
    
    /**
     * Download video from current playing song
     * Automatically searches using metadata
     */
    suspend fun downloadVideoFromMetadata(
        context: Context,
        title: String,
        artist: String? = null,
        quality: VideoQuality = VideoQuality.Q_720P,
        onDownloadId: (Long) -> Unit = {}
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            Timber.tag(TAG).d("Starting video download for: $title at ${quality.name}")
            
            // Step 1: Search for video
            val videoUrl = searchVideoByMetadata(title, artist).getOrThrow()
            
            // Step 2: Extract video stream
            val service = ServiceList.YouTube
            val linkHandler = service.getStreamLHFactory().fromUrl(videoUrl)
            val extractor = service.getStreamExtractor(linkHandler) as YoutubeStreamExtractor
            extractor.fetchPage()
            
            // Get video streams
            val videoStreams = extractor.videoStreams
            if (videoStreams.isEmpty()) {
                throw Exception("No video streams found")
            }
            
            // Filter for MP4 format
            val mp4Streams = videoStreams.filter { stream ->
                stream.format?.mimeType?.contains("mp4") == true ||
                stream.format?.suffix == "mp4"
            }
            
            if (mp4Streams.isEmpty()) {
                throw Exception("No MP4 streams found")
            }
            
            // Find best matching quality
            val selectedStream = findBestQualityStream(mp4Streams, quality)
                ?: throw Exception("No suitable stream found")
            
            Timber.tag(TAG).d("Selected video: ${selectedStream.resolution}")
            
            // Step 3: Download using Android DownloadManager
            val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\s-]"), "")
            val fileName = "$sanitizedTitle [${selectedStream.resolution}].mp4"
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(selectedStream.url)).apply {
                setTitle(title)
                setDescription("Downloading video (${selectedStream.resolution})")
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
                addRequestHeader("User-Agent", "Mozilla/5.0")
            }
            
            val downloadId = downloadManager.enqueue(request)
            Timber.tag(TAG).d("Video download started with ID: $downloadId")
            onDownloadId(downloadId)
            
            downloadId
        }
    }
    
    /**
     * Find best quality stream
     */
    private fun findBestQualityStream(
        streams: List<VideoStream>,
        preferredQuality: VideoQuality
    ): VideoStream? {
        // Try exact match
        val exactMatch = streams.find { stream ->
            stream.resolution?.contains("${preferredQuality.height}p", ignoreCase = true) == true
        }
        
        if (exactMatch != null) return exactMatch
        
        // Find closest quality
        return streams.minByOrNull { stream ->
            val height = stream.resolution?.replace("p", "")?.toIntOrNull() ?: 0
            kotlin.math.abs(height - preferredQuality.height)
        }
    }
    
    /**
     * Video quality enum
     */
    enum class VideoQuality(val height: Int) {
        Q_144P(144),
        Q_240P(240),
        Q_360P(360),
        Q_480P(480),
        Q_720P(720),
        Q_1080P(1080);
        
        companion object {
            fun fromString(quality: String): VideoQuality {
                return when (quality.lowercase()) {
                    "144p", "144" -> Q_144P
                    "240p", "240" -> Q_240P
                    "360p", "360" -> Q_360P
                    "480p", "480" -> Q_480P
                    "720p", "720" -> Q_720P
                    "1080p", "1080" -> Q_1080P
                    else -> Q_720P
                }
            }
        }
    }
}
