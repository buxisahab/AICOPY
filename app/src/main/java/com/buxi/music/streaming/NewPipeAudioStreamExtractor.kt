package com.buxi.music.streaming

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
 * Migrated from Musifly for AI Music app
 */
object NewPipeAudioStreamExtractor {
    
    private const val TAG = "AudioStreamExtractor"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    
    private class OkHttpDownloader : Downloader() {
        private val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
        
        override fun execute(request: Request): Response {
            val requestBuilder = OkHttpRequest.Builder().url(request.url())
            request.headers().forEach { (key, values) -> values.forEach { requestBuilder.addHeader(key, it) } }
            if (!request.headers().containsKey("User-Agent")) requestBuilder.addHeader("User-Agent", USER_AGENT)
            
            val httpMethod = request.httpMethod()
            val requestBody = if (httpMethod == "POST" || httpMethod == "PUT") {
                val bodyData = request.dataToSend() ?: ByteArray(0)
                val contentType = request.headers()["Content-Type"]?.firstOrNull() ?: "application/x-www-form-urlencoded"
                bodyData.toRequestBody(contentType.toMediaType())
            } else null
            
            requestBuilder.method(httpMethod, requestBody)
            val okHttpResponse = client.newCall(requestBuilder.build()).execute()
            return Response(okHttpResponse.code, okHttpResponse.message, okHttpResponse.headers.toMultimap(), okHttpResponse.body?.string(), okHttpResponse.request.url.toString())
        }
    }
    
    init {
        try {
            NewPipe.init(OkHttpDownloader())
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to init NewPipe")
        }
    }

    private fun ensureInitialized() {
        try { NewPipe.init(OkHttpDownloader()) } catch (e: Exception) {}
    }
    
    data class AudioStreamInfo(
        val url: String,
        val bitrate: Int,
        val format: String,
        val videoId: String,
        val title: String,
        val duration: Long
    )
    
    suspend fun getBestAudioStream(videoUrl: String): Result<AudioStreamInfo> = withContext(Dispatchers.IO) {
        runCatching {
            ensureInitialized()
            val service = ServiceList.YouTube
            val linkHandler = service.getStreamLHFactory().fromUrl(videoUrl)
            val extractor = service.getStreamExtractor(linkHandler) as YoutubeStreamExtractor
            extractor.fetchPage()
            
            val audioStreams = extractor.audioStreams
            if (audioStreams.isEmpty()) throw Exception("No audio streams found")
            
            val bestAudio = selectBestAudioStream(audioStreams) ?: throw Exception("No suitable audio stream")
            @Suppress("DEPRECATION")
            val streamUrl = bestAudio.content ?: bestAudio.url ?: throw Exception("URL null")
            
            AudioStreamInfo(
                url = streamUrl,
                bitrate = bestAudio.averageBitrate,
                format = bestAudio.format?.suffix ?: "m4a",
                videoId = linkHandler.id,
                title = extractor.name ?: "Unknown",
                duration = extractor.length
            )
        }
    }
    
    private fun selectBestAudioStream(streams: List<AudioStream>): AudioStream? {
        return streams.filter { it.format?.suffix?.equals("m4a", true) == true }.maxByOrNull { it.averageBitrate }
            ?: streams.maxByOrNull { it.averageBitrate }
    }
}
