package com.musifly.android.streaming

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
import org.schabi.newpipe.extractor.stream.AudioStream
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * NewPipe Audio Stream Extractor
 * Extracts the best quality audio stream from a YouTube video
 */
object NewPipeAudioStreamExtractor {
    
    private const val TAG = "AudioStreamExtractor"
    
    // User agent matching Firefox 140 ESR (as used in NewPipe v0.28.0)
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    
    // OkHttp-based downloader matching NewPipe's implementation
    private class OkHttpDownloader : Downloader() {
        
        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                // Add retry logic for failed requests
                var request = chain.request()
                var response = chain.proceed(request)
                var tryCount = 0
                while (!response.isSuccessful && tryCount < 2) {
                    tryCount++
                    response.close()
                    Thread.sleep(500)
                    response = chain.proceed(request)
                }
                response
            }
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
        try {
            NewPipe.init(OkHttpDownloader())
            Timber.tag(TAG).d("✅ NewPipe initialized successfully for audio streaming")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed to initialize NewPipe")
        }
    }
    
    /**
     * Ensure NewPipe is initialized before use
     * Safe to call multiple times - NewPipe.init() can be called repeatedly
     */
    private fun ensureInitialized() {
        try {
            // Initialize NewPipe - safe to call multiple times
            NewPipe.init(OkHttpDownloader())
        } catch (e: Exception) {
            // If already initialized or other error, log and continue
            Timber.tag(TAG).w(e, "⚠️ NewPipe initialization check failed (may already be initialized)")
        }
    }
    
    /**
     * Audio stream information
     */
    data class AudioStreamInfo(
        val url: String,
        val bitrate: Int,
        val format: String,
        val mimeType: String,
        val videoId: String,
        val title: String,
        val uploader: String,
        val thumbnailUrl: String?,
        val duration: Long
    )
    
    /**
     * Get best audio stream from video URL
     * Prefers: M4A > Opus > WebM
     * Always picks highest bitrate available
     * 
     * @param videoUrl YouTube video URL
     * @return Result containing audio stream info or error
     */
    suspend fun getBestAudioStream(videoUrl: String): Result<AudioStreamInfo> = withContext(Dispatchers.IO) {
        runCatching {
            // Ensure NewPipe is initialized before use
            ensureInitialized()
            
            Timber.tag(TAG).d("🎵 Extracting audio from: $videoUrl")
            
            // Get YouTube service
            val service = ServiceList.YouTube
            val linkHandler = service.getStreamLHFactory().fromUrl(videoUrl)
            
            // Get stream extractor
            val extractor = service.getStreamExtractor(linkHandler) as YoutubeStreamExtractor
            extractor.fetchPage()
            
            val videoTitle = extractor.name ?: "Unknown"
            val uploader = extractor.uploaderName ?: "Unknown"
            val thumbnailUrl = extractor.thumbnails.firstOrNull()?.url
            val duration = extractor.length
            val videoId = linkHandler.id
            
            Timber.tag(TAG).d("Video: $videoTitle")
            Timber.tag(TAG).d("Uploader: $uploader")
            Timber.tag(TAG).d("Duration: ${duration}s")
            
            // Get audio streams
            val audioStreams = extractor.audioStreams
            
            if (audioStreams.isEmpty()) {
                throw Exception("No audio streams found")
            }
            
            Timber.tag(TAG).d("Found ${audioStreams.size} audio streams")
            
            // Log all available streams
            audioStreams.forEachIndexed { index, stream ->
                Timber.tag(TAG).d(
                    "  Stream $index: ${stream.format?.mimeType} | " +
                    "${stream.averageBitrate} bps | " +
                    "${stream.format?.suffix}"
                )
            }
            
            // Find best audio stream
            val bestAudio = selectBestAudioStream(audioStreams)
                ?: throw Exception("No suitable audio stream found")
            
            // Use .content instead of deprecated .url
            @Suppress("DEPRECATION")
            val streamUrl = bestAudio.content ?: bestAudio.url ?: throw Exception("Stream URL is null")
            
            Timber.tag(TAG).d("✅ Selected stream:")
            Timber.tag(TAG).d("   Format: ${bestAudio.format?.mimeType}")
            Timber.tag(TAG).d("   Bitrate: ${bestAudio.averageBitrate} bps")
            Timber.tag(TAG).d("   Suffix: ${bestAudio.format?.suffix}")
            Timber.tag(TAG).d("   URL: ${streamUrl.take(80)}...")
            
            AudioStreamInfo(
                url = streamUrl,
                bitrate = bestAudio.averageBitrate,
                format = bestAudio.format?.suffix ?: "unknown",
                mimeType = bestAudio.format?.mimeType ?: "audio/mp4",
                videoId = videoId,
                title = videoTitle,
                uploader = uploader,
                thumbnailUrl = thumbnailUrl,
                duration = duration
            )
        }
    }
    
    /**
     * Select best audio stream based on format and bitrate
     * Priority: M4A > Opus > Others
     * Within same format: Highest bitrate wins
     */
    private fun selectBestAudioStream(streams: List<AudioStream>): AudioStream? {
        // Separate streams by format
        val m4aStreams = streams.filter { 
            it.format?.mimeType?.contains("m4a", ignoreCase = true) == true ||
            it.format?.suffix?.equals("m4a", ignoreCase = true) == true
        }
        
        val opusStreams = streams.filter { 
            it.format?.mimeType?.contains("opus", ignoreCase = true) == true ||
            it.format?.suffix?.equals("opus", ignoreCase = true) == true
        }
        
        val webmStreams = streams.filter { 
            it.format?.mimeType?.contains("webm", ignoreCase = true) == true ||
            it.format?.suffix?.equals("webm", ignoreCase = true) == true
        }
        
        // Pick best from each category (highest bitrate)
        val bestM4a = m4aStreams.maxByOrNull { it.averageBitrate }
        val bestOpus = opusStreams.maxByOrNull { it.averageBitrate }
        val bestWebm = webmStreams.maxByOrNull { it.averageBitrate }
        val bestOther = streams
            .filter { it !in m4aStreams && it !in opusStreams && it !in webmStreams }
            .maxByOrNull { it.averageBitrate }
        
        // Return in order of preference
        return when {
            bestM4a != null -> {
                Timber.tag(TAG).d("Selected M4A format (best quality)")
                bestM4a
            }
            bestOpus != null -> {
                Timber.tag(TAG).d("Selected Opus format (fallback)")
                bestOpus
            }
            bestWebm != null -> {
                Timber.tag(TAG).d("Selected WebM format (fallback)")
                bestWebm
            }
            bestOther != null -> {
                Timber.tag(TAG).d("Selected other format (last resort)")
                bestOther
            }
            else -> streams.maxByOrNull { it.averageBitrate }
        }
    }
}
