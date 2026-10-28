package com.musifly.android.download

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * YouTube Search Engine for finding the correct official video
 * based on song title and artist name using NewPipeExtractor.
 * 
 * This ensures accurate video downloads even when users enter
 * theme music, ringtone names, or short music titles.
 */
object YouTubeSearchEngine {
    
    private const val TAG = "YouTubeSearchEngine"
    
    /**
     * Search result containing the found video information
     */
    data class SearchResult(
        val videoUrl: String,
        val title: String,
        val uploaderName: String,
        val duration: Long,
        val viewCount: Long,
        val isVerified: Boolean,
        val thumbnailUrl: String
    )
    
    /**
     * Find the correct YouTube video based on song title and artist name.
     * 
     * Search Strategy:
     * 1. Build query: "{title} {artist} official video"
     * 2. Filter only VIDEO_STREAM types
     * 3. Prefer verified/official channels
     * 4. Return first matching result
     * 
     * @param songTitle The title of the song
     * @param artistName The artist/uploader name
     * @param includeOfficialKeyword Add "official video" to search query
     * @return SearchResult with video URL and metadata, or null if not found
     */
    suspend fun findCorrectVideo(
        songTitle: String,
        artistName: String,
        includeOfficialKeyword: Boolean = true
    ): SearchResult? = withContext(Dispatchers.IO) {
        try {
            // Build search query
            val query = buildSearchQuery(songTitle, artistName, includeOfficialKeyword)
            Log.d(TAG, "Searching YouTube for: $query")
            
            // Get YouTube service
            val youtubeService = ServiceList.YouTube
            val searchExtractor = youtubeService.getSearchExtractor(query)
            searchExtractor.fetchPage()
            
            // Get search results
            val items = searchExtractor.initialPage.items
            Log.d(TAG, "Found ${items.size} search results")
            
            // Filter and find the best match
            val bestMatch = findBestMatch(items, artistName)
            
            if (bestMatch != null) {
                Log.d(TAG, "Best match: ${bestMatch.name} by ${bestMatch.uploaderName}")
                return@withContext SearchResult(
                    videoUrl = bestMatch.url,
                    title = bestMatch.name,
                    uploaderName = bestMatch.uploaderName ?: "",
                    duration = bestMatch.duration,
                    viewCount = bestMatch.viewCount,
                    isVerified = bestMatch.isUploaderVerified,
                    thumbnailUrl = bestMatch.thumbnails?.firstOrNull()?.url ?: ""
                )
            } else {
                Log.w(TAG, "No suitable video found for: $query")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching for video: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Find the correct video by exact URL matching (for direct YouTube links)
     * 
     * @param videoUrl Direct YouTube video URL
     * @return SearchResult with video metadata, or null if invalid
     */
    suspend fun findVideoByUrl(videoUrl: String): SearchResult? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching video info for URL: $videoUrl")
            
            val youtubeService = ServiceList.YouTube
            val streamExtractor = youtubeService.getStreamExtractor(videoUrl)
            streamExtractor.fetchPage()
            
            return@withContext SearchResult(
                videoUrl = streamExtractor.url,
                title = streamExtractor.name,
                uploaderName = streamExtractor.uploaderName ?: "",
                duration = streamExtractor.length,
                viewCount = streamExtractor.viewCount,
                isVerified = streamExtractor.isUploaderVerified,
                thumbnailUrl = streamExtractor.thumbnails?.firstOrNull()?.url ?: ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching video by URL: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Search with fallback strategies if initial search fails
     * 
     * @param songTitle Song title
     * @param artistName Artist name
     * @return SearchResult or null
     */
    suspend fun findCorrectVideoWithFallback(
        songTitle: String,
        artistName: String
    ): SearchResult? {
        // Strategy 1: Official video search
        var result = findCorrectVideo(songTitle, artistName, includeOfficialKeyword = true)
        if (result != null) return result
        
        // Strategy 2: Without "official video" keyword
        result = findCorrectVideo(songTitle, artistName, includeOfficialKeyword = false)
        if (result != null) return result
        
        // Strategy 3: Title only (in case artist name is incorrect)
        result = findCorrectVideo(songTitle, "", includeOfficialKeyword = true)
        if (result != null) return result
        
        Log.w(TAG, "All search strategies failed for: $songTitle - $artistName")
        return null
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * Build optimized search query
     */
    private fun buildSearchQuery(
        title: String,
        artist: String,
        includeOfficialKeyword: Boolean
    ): String {
        val cleanTitle = title.trim()
        val cleanArtist = artist.trim()
        
        return buildString {
            append(cleanTitle)
            if (cleanArtist.isNotEmpty()) {
                append(" ")
                append(cleanArtist)
            }
            if (includeOfficialKeyword) {
                append(" official video")
            }
        }
    }
    
    /**
     * Find best matching video from search results
     * 
     * Scoring criteria:
     * - Must be VIDEO_STREAM type
     * - Prefer verified channels (+100 points)
     * - Prefer official channels (+50 points)
     * - Prefer artist name match (+50 points)
     * - Prefer higher view count (+log10(views) points)
     * - Prefer longer duration (full songs, not clips)
     */
    private fun findBestMatch(
        items: List<InfoItem>,
        artistName: String
    ): StreamInfoItem? {
        val videoItems = items.filterIsInstance<StreamInfoItem>()
            .filter { it.streamType == org.schabi.newpipe.extractor.stream.StreamType.VIDEO_STREAM }
        
        if (videoItems.isEmpty()) return null
        
        // Score each video
        val scoredVideos = videoItems.map { video ->
            var score = 0.0
            
            // Verified channel bonus
            if (video.isUploaderVerified) {
                score += 100.0
            }
            
            // Official channel bonus (contains "official" in name)
            val uploaderLower = video.uploaderName?.lowercase() ?: ""
            if (uploaderLower.contains("official") || uploaderLower.contains("vevo")) {
                score += 50.0
            }
            
            // Artist name match bonus
            if (artistName.isNotEmpty() && uploaderLower.contains(artistName.lowercase())) {
                score += 50.0
            }
            
            // View count bonus (logarithmic scale)
            if (video.viewCount > 0) {
                score += Math.log10(video.viewCount.toDouble())
            }
            
            // Duration bonus (prefer full songs, not clips)
            // Typical song duration: 120-360 seconds (2-6 minutes)
            if (video.duration in 120..600) {
                score += 20.0
            }
            
            Pair(video, score)
        }
        
        // Return highest scored video
        return scoredVideos.maxByOrNull { it.second }?.first
    }
    
    /**
     * Advanced search with multiple filters
     * 
     * @param songTitle Song title
     * @param artistName Artist name
     * @param minDuration Minimum duration in seconds (filters out clips)
     * @param maxDuration Maximum duration in seconds (filters out mixes)
     * @param preferVerified Prefer verified channels
     * @return List of matching results
     */
    suspend fun advancedSearch(
        songTitle: String,
        artistName: String,
        minDuration: Long = 60,
        maxDuration: Long = 600,
        preferVerified: Boolean = true
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            val query = buildSearchQuery(songTitle, artistName, includeOfficialKeyword = true)
            Log.d(TAG, "Advanced search for: $query")
            
            val youtubeService = ServiceList.YouTube
            val searchExtractor = youtubeService.getSearchExtractor(query)
            searchExtractor.fetchPage()
            
            val results = searchExtractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .filter { it.streamType == org.schabi.newpipe.extractor.stream.StreamType.VIDEO_STREAM }
                .filter { it.duration in minDuration..maxDuration }
                .filter { !preferVerified || it.isUploaderVerified }
                .map { item ->
                    SearchResult(
                        videoUrl = item.url,
                        title = item.name,
                        uploaderName = item.uploaderName ?: "",
                        duration = item.duration,
                        viewCount = item.viewCount,
                        isVerified = item.isUploaderVerified,
                        thumbnailUrl = item.thumbnails?.firstOrNull()?.url ?: ""
                    )
                }
            
            Log.d(TAG, "Advanced search found ${results.size} filtered results")
            return@withContext results
        } catch (e: Exception) {
            Log.e(TAG, "Advanced search error: ${e.message}", e)
            return@withContext emptyList()
        }
    }
}
