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
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * NewPipe Stream Finder
 * Finds the correct YouTube video for a given song title and artist
 * Prioritizes official/verified channels
 */
object NewPipeStreamFinder {
    
    private const val TAG = "NewPipeStreamFinder"
    
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
        try {
            NewPipe.init(OkHttpDownloader())
            Timber.tag(TAG).d("NewPipe initialized successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to initialize NewPipe")
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
            Timber.tag(TAG).w(e, "NewPipe initialization check failed (may already be initialized)")
        }
    }
    
    /**
     * Video search result data class
     */
    data class VideoSearchResult(
        val title: String,
        val url: String,
        val uploaderName: String?,
        val thumbnailUrl: String?,
        val duration: Long,
        val viewCount: Long,
        val isVerified: Boolean,
        val score: Int = 0
    )
    
    /**
     * Get all search results for video selection
     * 
     * @param songTitle The song title
     * @param artistName The artist name (optional)
     * @return Result containing list of video search results
     */
    suspend fun getAllSearchResults(
        songTitle: String,
        artistName: String? = null
    ): Result<List<VideoSearchResult>> = withContext(Dispatchers.IO) {
        runCatching {
            ensureInitialized()
            
            Timber.tag(TAG).d("🔍 Searching for all videos: $songTitle by ${artistName ?: "Unknown"}")
            
            val searchQueries = buildSearchQueries(songTitle, artistName)
            val allVideos = mutableListOf<VideoSearchResult>()
            val seenUrls = mutableSetOf<String>()
            
            // Try first few search queries to get diverse results
            for (query in searchQueries.take(3)) {
                try {
                    Timber.tag(TAG).d("Trying search query: $query")
                    
                    val service = ServiceList.YouTube
                    
                    // Try to get search extractor with fallback for URLEncoder issues
                    val searchExtractor = try {
                        service.getSearchExtractor(query)
                    } catch (e: NoSuchMethodError) {
                        // URLEncoder compatibility issue on old Android versions
                        Timber.tag(TAG).e(e, "URLEncoder compatibility error - this may occur on Android < 13")
                        throw Exception("Search not available on this Android version. Please use direct video URLs.", e)
                    }
                    
                    searchExtractor.fetchPage()
                    
                    val items = searchExtractor.initialPage.items
                    Timber.tag(TAG).d("Found ${items.size} results")
                    
                    val videos = items.filterIsInstance<StreamInfoItem>()
                        .filter { 
                            it.streamType == org.schabi.newpipe.extractor.stream.StreamType.VIDEO_STREAM
                        }
                        .filter { video ->
                            val uploaderName = video.uploaderName?.lowercase() ?: ""
                            !uploaderName.contains("topic") && !uploaderName.contains("- topic")
                        }
                    
                    videos.forEach { video ->
                        val videoUrl = video.url
                        if (!seenUrls.contains(videoUrl)) {
                            seenUrls.add(videoUrl)
                            
                            // Calculate score for sorting
                            val score = calculateVideoScore(video, songTitle, artistName)
                            
                            // Get thumbnail URL - try multiple methods
                            val thumbnailUrl = try {
                                // Method 1: Direct thumbnails property
                                video.thumbnails?.firstOrNull()?.url
                            } catch (e: Exception) {
                                try {
                                    // Method 2: Extract from video URL
                                    val videoId = videoUrl.substringAfterLast("v=").substringBefore("&")
                                    "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
                                } catch (e2: Exception) {
                                    null
                                }
                            }
                            
                            allVideos.add(
                                VideoSearchResult(
                                    title = video.name ?: "Unknown",
                                    url = videoUrl,
                                    uploaderName = video.uploaderName,
                                    thumbnailUrl = thumbnailUrl,
                                    duration = video.duration,
                                    viewCount = video.viewCount,
                                    isVerified = video.isUploaderVerified,
                                    score = score
                                )
                            )
                        }
                    }
                    
                    // If we have enough results, break
                    if (allVideos.size >= 10) break
                    
                } catch (e: NoSuchMethodError) {
                    // URLEncoder compatibility issue - stop trying and throw clear error
                    Timber.tag(TAG).e(e, "URLEncoder not available on this Android version")
                    throw Exception("Video search requires Android 13 or newer. Please use direct video URLs.", e)
                } catch (e: Exception) {
                    Timber.tag(TAG).w("Search failed for '$query': ${e.message}")
                    continue
                }
            }
            
            // Sort by score (highest first) and return top results
            val sortedVideos = allVideos
                .sortedByDescending { it.score }
                .take(15) // Return top 15 results
            
            Timber.tag(TAG).d("✅ Found ${sortedVideos.size} unique videos")
            return@withContext Result.success(sortedVideos)
        }
    }
    
    /**
     * Calculate score for a video (same logic as findBestMatchingVideo)
     */
    private fun calculateVideoScore(
        video: StreamInfoItem,
        songTitle: String,
        artistName: String?
    ): Int {
        var score = 0
        val videoTitle = video.name?.lowercase() ?: ""
        val uploaderName = video.uploaderName?.lowercase() ?: ""
        val isVerified = video.isUploaderVerified
        
        if (videoTitle.contains("official music video") || videoTitle.contains("official video")) {
            score += 200
        }
        if (isVerified) {
            score += 150
        }
        if (videoTitle.contains("official") && !videoTitle.contains("audio")) {
            score += 100
        }
        if (videoTitle.contains("audio") && !videoTitle.contains("video")) {
            score -= 100
        }
        if (videoTitle.contains("lyrics") || videoTitle.contains("lyric")) {
            score -= 50
        }
        
        val titleLower = songTitle.lowercase()
        val cleanTitle = titleLower
            .replace(Regex("\\([^)]*\\)"), "")
            .replace(Regex("\\[[^]]*\\]"), "")
            .trim()
        
        if (videoTitle.contains(cleanTitle)) {
            score += 50
        }
        
        if (artistName != null) {
            val artistLower = artistName.lowercase()
            if (uploaderName.contains(artistLower)) {
                score += 80
            }
            if (videoTitle.contains(artistLower)) {
                score += 30
            }
        }
        
        if (videoTitle.contains("live") || videoTitle.contains("concert")) {
            score -= 30
        }
        if (videoTitle.contains("cover")) {
            score -= 40
        }
        
        val views = video.viewCount
        if (views > 0) {
            score += minOf(20, (views / 1_000_000).toInt())
        }
        
        if (videoTitle.contains("music video") || videoTitle.contains("mv")) {
            score += 60
        }
        
        return score
    }
    
    /**
     * Find correct video URL from title and artist
     * 
     * @param songTitle The song title
     * @param artistName The artist name (optional)
     * @return Result containing video URL or error
     */
    suspend fun findCorrectVideo(
        songTitle: String,
        artistName: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // Ensure NewPipe is initialized before use
            ensureInitialized()
            
            Timber.tag(TAG).d("🔍 Searching for: $songTitle by ${artistName ?: "Unknown"}")
            
            // Build search queries in order of preference
            val searchQueries = buildSearchQueries(songTitle, artistName)
            
            var lastException: Exception? = null
            
            // Try each search query
            for (query in searchQueries) {
                try {
                    Timber.tag(TAG).d("Trying search query: $query")
                    
                    val service = ServiceList.YouTube
                    val searchExtractor = service.getSearchExtractor(query)
                    searchExtractor.fetchPage()
                    
                    val items = searchExtractor.initialPage.items
                    Timber.tag(TAG).d("Found ${items.size} results")
                    
                    if (items.isNotEmpty()) {
                        // Find best matching video
                        val bestVideo = findBestMatchingVideo(items, songTitle, artistName)
                        
                        if (bestVideo != null) {
                            Timber.tag(TAG).d("✅ Found video: ${bestVideo.name}")
                            Timber.tag(TAG).d("   Uploader: ${bestVideo.uploaderName}")
                            Timber.tag(TAG).d("   URL: ${bestVideo.url}")
                            return@withContext Result.success(bestVideo.url)
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).w("Search failed for '$query': ${e.message}")
                    lastException = e
                    continue
                }
            }
            
            // No video found after all attempts
            throw lastException 
                ?: Exception("No video found after trying ${searchQueries.size} search queries")
        }
    }
    
    /**
     * Build search queries in order of preference
     * Prioritizes official music videos over audio-only
     */
    private fun buildSearchQueries(songTitle: String, artistName: String?): List<String> {
        val queries = mutableListOf<String>()
        
        // Clean title - remove content in parentheses/brackets for better search
        val cleanTitle = songTitle
            .replace(Regex("\\([^)]*\\)"), "") // Remove (From "Movie Name")
            .replace(Regex("\\[[^]]*\\]"), "") // Remove [Remix]
            .trim()
        
        if (artistName != null) {
            // ✅ HIGHEST PRIORITY: Official Music Video (actual video, not audio)
            queries.add("$cleanTitle $artistName official music video")
            queries.add("$cleanTitle $artistName official video")
            queries.add("$artistName $cleanTitle official music video")
            queries.add("$artistName $cleanTitle official video")
            
            // ✅ HIGH PRIORITY: Just title + artist (will find best match)
            queries.add("$cleanTitle $artistName")
            queries.add("$artistName $cleanTitle")
            
            // ⚠️ LOWER PRIORITY: Audio versions (only if no video found)
            queries.add("$cleanTitle $artistName official audio")
        }
        
        // Title only queries (if no artist)
        queries.add("$cleanTitle official music video")
        queries.add("$cleanTitle official video")
        queries.add(cleanTitle)
        
        // If title is long, try simplified version (first 3-4 words)
        val words = cleanTitle.split(Regex("\\s+|\\s*-\\s*")).filter { it.isNotBlank() }
        if (words.size > 4) {
            val simplified = words.take(4).joinToString(" ")
            if (artistName != null) {
                queries.add("$simplified $artistName official music video")
                queries.add("$simplified $artistName")
            }
            queries.add("$simplified official music video")
            queries.add(simplified)
        }
        
        Timber.tag(TAG).d("Built ${queries.size} search queries")
        queries.forEachIndexed { index, query ->
            Timber.tag(TAG).d("  Query ${index + 1}: $query")
        }
        
        return queries
    }
    
    /**
     * Find best matching video from search results
     * Prioritizes official music videos (NOT audio-only or thumbnail videos)
     */
    private fun findBestMatchingVideo(
        items: List<Any>,
        songTitle: String,
        artistName: String?
    ): StreamInfoItem? {
        // Filter to only video streams (exclude audio-only)
        val videos = items.filterIsInstance<StreamInfoItem>()
            .filter { 
                it.streamType == org.schabi.newpipe.extractor.stream.StreamType.VIDEO_STREAM
            }
            .filter { video ->
                // Filter out Topic channels (these are usually audio-only with static thumbnails)
                val uploaderName = video.uploaderName?.lowercase() ?: ""
                !uploaderName.contains("topic") && !uploaderName.contains("- topic")
            }
        
        if (videos.isEmpty()) {
            Timber.tag(TAG).w("No video streams found (only audio or topic channels)")
            return null
        }
        
        Timber.tag(TAG).d("Found ${videos.size} potential video streams")
        
        // Scoring system for finding best match
        val scoredVideos = videos.map { video ->
            var score = 0
            
            val videoTitle = video.name?.lowercase() ?: ""
            val uploaderName = video.uploaderName?.lowercase() ?: ""
            val isVerified = video.isUploaderVerified
            
            // ✅ HIGHEST PRIORITY: Official Music Video
            if (videoTitle.contains("official music video") || videoTitle.contains("official video")) {
                score += 200
                Timber.tag(TAG).d("  ⭐⭐⭐ Official Music Video: ${video.name}")
            }
            
            // ✅ HIGH PRIORITY: Verified/Official channel
            if (isVerified) {
                score += 150
                Timber.tag(TAG).d("  ✓ Verified channel: ${video.uploaderName}")
            }
            
            // ✅ HIGH PRIORITY: Official in title (but not audio-only)
            if (videoTitle.contains("official") && !videoTitle.contains("audio")) {
                score += 100
                Timber.tag(TAG).d("  ✓ Official video (not audio): ${video.name}")
            }
            
            // ❌ PENALTY: Audio-only indicators (we want actual videos)
            if (videoTitle.contains("audio") && !videoTitle.contains("video")) {
                score -= 100
                Timber.tag(TAG).d("  ❌ Audio-only detected: ${video.name}")
            }
            
            // ❌ PENALTY: Lyrics videos (usually static images)
            if (videoTitle.contains("lyrics") || videoTitle.contains("lyric")) {
                score -= 50
                Timber.tag(TAG).d("  ❌ Lyrics video: ${video.name}")
            }
            
            // Title match
            val titleLower = songTitle.lowercase()
            val cleanTitle = titleLower
                .replace(Regex("\\([^)]*\\)"), "") // Remove content in parentheses
                .replace(Regex("\\[[^]]*\\]"), "") // Remove content in brackets
                .trim()
            
            if (videoTitle.contains(cleanTitle)) {
                score += 50
                Timber.tag(TAG).d("  ✓ Title matches")
            }
            
            // Artist match in uploader name (high priority)
            if (artistName != null) {
                val artistLower = artistName.lowercase()
                if (uploaderName.contains(artistLower)) {
                    score += 80
                    Timber.tag(TAG).d("  ✓ Artist in uploader: ${video.uploaderName}")
                }
                if (videoTitle.contains(artistLower)) {
                    score += 30
                }
            }
            
            // ❌ PENALTY: Live performances (usually lower quality)
            if (videoTitle.contains("live") || videoTitle.contains("concert")) {
                score -= 30
            }
            
            // ❌ PENALTY: Covers (not original)
            if (videoTitle.contains("cover") || videoTitle.contains("by") && uploaderName != artistName?.lowercase()) {
                score -= 40
            }
            
            // View count as tie-breaker (normalize to 0-20 range)
            val views = video.viewCount
            if (views > 0) {
                val normalizedViews = minOf(20, (views / 1_000_000).toInt())
                score += normalizedViews
            }
            
            // ✅ BONUS: Music video keywords
            if (videoTitle.contains("music video") || videoTitle.contains("mv")) {
                score += 60
                Timber.tag(TAG).d("  ✓ Music video keyword found")
            }
            
            Timber.tag(TAG).d("  📊 Score: $score - ${video.name} by ${video.uploaderName}")
            
            video to score
        }
        
        // Return video with highest score
        val bestVideo = scoredVideos
            .maxByOrNull { it.second }
            ?.first
        
        if (bestVideo != null) {
            val bestScore = scoredVideos.maxByOrNull { it.second }?.second ?: 0
            Timber.tag(TAG).d("✅ Selected video: ${bestVideo.name}")
            Timber.tag(TAG).d("   Uploader: ${bestVideo.uploaderName}")
            Timber.tag(TAG).d("   URL: ${bestVideo.url}")
            Timber.tag(TAG).d("   Final Score: $bestScore")
        }
        
        return bestVideo
    }
}


