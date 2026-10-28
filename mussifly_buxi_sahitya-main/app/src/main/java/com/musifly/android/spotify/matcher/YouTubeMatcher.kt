package com.musifly.android.spotify.matcher

import com.musifly.android.models.MediaMetadata
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
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * YouTubeMatcher - Matches Spotify tracks to YouTube Music audio using NewPipe
 * Equivalent to Bloomee's MixedAPI.getYtTrackByMeta()
 * 
 * Uses NewPipe (already integrated in OpenTune) to search YouTube Music
 */
class YouTubeMatcher {
    
    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        
        @Volatile
        private var isInitialized = false
        
        /**
         * OkHttp-based downloader for NewPipe
         */
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
        
        /**
         * Initialize NewPipe with OkHttp downloader
         * Thread-safe and idempotent
         */
        @Synchronized
        fun ensureInitialized() {
            if (!isInitialized) {
                try {
                    NewPipe.init(OkHttpDownloader())
                    isInitialized = true
                    Timber.d("YouTubeMatcher: NewPipe initialized successfully")
                } catch (e: Exception) {
                    // May already be initialized by another component
                    isInitialized = true
                    Timber.w(e, "YouTubeMatcher: NewPipe init warning (may already be initialized)")
                }
            }
        }
    }
    
    /**
     * Match a track query to a YouTube Music audio item
     * Equivalent to Bloomee's MixedAPI.getYtTrackByMeta()
     * 
     * @param query Search query in format "Title Artist1, Artist2"
     * @param useStringMatcher Whether to use fuzzy matching (default: false for simplicity)
     * @return MediaMetadata if found, null otherwise
     */
    suspend fun match(
        query: String,
        useStringMatcher: Boolean = false
    ): MediaMetadata? = withContext(Dispatchers.IO) {
        try {
            // Search YouTube using NewPipe
            val searchResults = searchYouTubeMusic(query)
            if (searchResults.isEmpty()) {
                Timber.d("No results found for query: $query")
                return@withContext null
            }
            
            // If only one result, return it
            if (searchResults.size == 1) {
                return@withContext streamInfoItemToMediaMetadata(searchResults[0])
            }
            
            // If multiple results and useStringMatcher is true, use fuzzy matching
            if (useStringMatcher && searchResults.size > 1) {
                // Find best match based on title/artist similarity
                val bestMatch = findBestMatch(searchResults, query)
                return@withContext bestMatch?.let { streamInfoItemToMediaMetadata(it) }
            }
            
            // Return first result
            return@withContext streamInfoItemToMediaMetadata(searchResults[0])
        } catch (e: Exception) {
            Timber.e(e, "Error matching track: $query")
            return@withContext null
        }
    }
    
    /**
     * Search YouTube Music for tracks using NewPipe
     */
    private suspend fun searchYouTubeMusic(query: String): List<StreamInfoItem> = withContext(Dispatchers.IO) {
        try {
            // Ensure NewPipe is initialized before use
            ensureInitialized()
            
            val service = ServiceList.YouTube
            val searchExtractor = service.getSearchExtractor(query)
            searchExtractor.fetchPage()
            
            val items = searchExtractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .filter { it.streamType == org.schabi.newpipe.extractor.stream.StreamType.VIDEO_STREAM }
            
            Timber.d("Found ${items.size} results for query: $query")
            return@withContext items
        } catch (e: Exception) {
            Timber.e(e, "Error searching YouTube: $query")
            return@withContext emptyList()
        }
    }
    
    /**
     * Find best matching video from search results
     */
    private fun findBestMatch(
        items: List<StreamInfoItem>,
        query: String
    ): StreamInfoItem? {
        val queryLower = query.lowercase()
        
        // Score each result
        val scoredItems = items.map { item ->
            var score = 0
            val itemTitle = item.name?.lowercase() ?: ""
            val uploaderName = item.uploaderName?.lowercase() ?: ""
            
            // Title match
            if (itemTitle.contains(queryLower.substringBefore("(").trim())) {
                score += 50
            }
            
            // Uploader name match
            if (uploaderName.contains(queryLower)) {
                score += 40
            }
            
            // Verified channel bonus
            if (item.isUploaderVerified) {
                score += 30
            }
            
            // Official indicators
            if (itemTitle.contains("official") && !itemTitle.contains("audio")) {
                score += 25
            }
            
            // Penalties for unwanted content
            if (itemTitle.contains("cover") && !queryLower.contains("cover")) {
                score -= 30
            }
            if (itemTitle.contains("live") && !queryLower.contains("live")) {
                score -= 20
            }
            if (itemTitle.contains("remix") && !queryLower.contains("remix")) {
                score -= 25
            }
            if (itemTitle.contains("karaoke")) {
                score -= 40
            }
            
            item to score
        }
        
        // Return highest scoring item if score is above threshold
        val best = scoredItems.maxByOrNull { it.second }
        return if (best != null && best.second >= 30) best.first else items.firstOrNull()
    }
    
    /**
     * Convert NewPipe StreamInfoItem to MediaMetadata
     */
    private fun streamInfoItemToMediaMetadata(item: StreamInfoItem): MediaMetadata {
        // Extract video ID from URL
        val videoId = item.url
            .substringAfter("watch?v=")
            .substringBefore("&")
            .takeIf { it.isNotBlank() }
            ?: item.url.substringAfterLast("/")
        
        // Get thumbnail URL
        val thumbnailUrl = try {
            item.thumbnails?.firstOrNull()?.url
                ?: "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
        } catch (e: Exception) {
            "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
        }
        
        return MediaMetadata(
            id = videoId,
            title = item.name ?: "Unknown",
            artists = listOf(
                MediaMetadata.Artist(
                    id = null,
                    name = item.uploaderName ?: "Unknown Artist"
                )
            ),
            duration = item.duration.toInt(),
            thumbnailUrl = thumbnailUrl
        )
    }
}

