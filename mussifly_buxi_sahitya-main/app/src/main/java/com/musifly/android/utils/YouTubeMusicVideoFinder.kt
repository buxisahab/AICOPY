package com.musifly.android.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.VideoStream
import timber.log.Timber

/**
 * YouTube Music Video Finder
 * 
 * Intelligently searches YouTube and finds the REAL music video
 * for a song (not audio-only, not lyrics, not topic videos)
 * 
 * Usage:
 * ```
 * val videoUrl = YouTubeMusicVideoFinder.getRealVideoFromTitleAndArtist(
 *     title = "Blinding Lights",
 *     artist = "The Weeknd"
 * )
 * ```
 */
object YouTubeMusicVideoFinder {
    
    private const val TAG = "MusicVideoFinder"
    
    // Search query templates
    private val SEARCH_TEMPLATES = listOf(
        "%s %s official music video",
        "%s %s official video",
        "%s %s music video",
        "%s %s mv",
        "%s %s"
    )
    
    // Keywords that indicate REAL music videos
    private val REAL_VIDEO_KEYWORDS = listOf(
        "official video",
        "official music video",
        "music video",
        "mv",
        "official mv",
        "(official video)",
        "[official video]",
        "videoclip",
        "video clip"
    )
    
    // Keywords that indicate FAKE/AUDIO-ONLY videos (to skip)
    private val SKIP_KEYWORDS = listOf(
        "topic",
        "official audio",
        "audio only",
        "lyric video",
        "lyrics video",
        "lyrics",
        "lyric",
        "audio visualizer",
        "visualizer",
        "album",
        "cover",
        "karaoke",
        "instrumental",
        "remix",
        "live performance",
        "concert"
    )
    
    /**
     * Main function: Get real music video URL from title and artist
     * 
     * @param title Song title
     * @param artist Artist name (optional but recommended)
     * @return MP4 video URL or null if not found
     */
    suspend fun getRealVideoFromTitleAndArtist(
        title: String,
        artist: String?
    ): String? = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).d("🔍 Searching for REAL music video")
            Timber.tag(TAG).d("Title: $title")
            Timber.tag(TAG).d("Artist: ${artist ?: "Unknown"}")
            
            // Build search query
            val searchQuery = buildSearchQuery(title, artist)
            Timber.tag(TAG).d("Search query: $searchQuery")
            
            // Search YouTube
            val searchResults = searchYouTube(searchQuery)
            
            if (searchResults.isEmpty()) {
                Timber.tag(TAG).w("⚠️ No search results found")
                return@withContext null
            }
            
            Timber.tag(TAG).d("Found ${searchResults.size} search results")
            
            // Filter and find best music video
            val bestVideo = filterBestMusicVideo(searchResults, title, artist)
            
            if (bestVideo == null) {
                Timber.tag(TAG).w("⚠️ No suitable music video found after filtering")
                return@withContext null
            }
            
            Timber.tag(TAG).d("✅ Selected video: ${bestVideo.name}")
            Timber.tag(TAG).d("Uploader: ${bestVideo.uploaderName}")
            Timber.tag(TAG).d("Video ID: ${bestVideo.url}")
            
            // Extract best MP4 video URL
            val videoUrl = extractBestMp4VideoUrl(bestVideo.url)
            
            if (videoUrl != null) {
                Timber.tag(TAG).d("✅ SUCCESS: Found real music video URL")
                Timber.tag(TAG).d("URL: ${videoUrl.take(100)}...")
            } else {
                Timber.tag(TAG).w("⚠️ Failed to extract video URL")
            }
            
            videoUrl
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Error finding music video")
            null
        }
    }
    
    /**
     * Build intelligent search query
     * Tries multiple query formats for best results
     */
    private fun buildSearchQuery(title: String, artist: String?): String {
        val sanitizedTitle = sanitizeTitleForSearch(title)
        val sanitizedArtist = artist?.let { sanitizeTitleForSearch(it) }
        
        // If we have both title and artist, use template
        return if (sanitizedArtist != null) {
            // Try "Title Artist official music video" first
            String.format(SEARCH_TEMPLATES[0], sanitizedTitle, sanitizedArtist)
        } else {
            // Just title with "official music video"
            "$sanitizedTitle official music video"
        }
    }
    
    /**
     * Sanitize title/artist for search
     * Remove special characters, clean up text
     */
    private fun sanitizeTitleForSearch(text: String): String {
        return text
            .trim()
            // Remove content in parentheses/brackets (often metadata)
            .replace(Regex("\\([^)]*\\)"), "")
            .replace(Regex("\\[[^]]*\\]"), "")
            // Remove extra quotes
            .replace("\"", "")
            .replace("'", "")
            // Clean up whitespace
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    /**
     * Search YouTube using NewPipe SearchExtractor
     */
    private suspend fun searchYouTube(query: String): List<StreamInfoItem> = withContext(Dispatchers.IO) {
        try {
            val service = ServiceList.YouTube
            
            // Create search extractor
            val searchExtractor = service.getSearchExtractor(query)
            searchExtractor.fetchPage()
            
            // Get search results (only videos)
            val results = searchExtractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .filter { it.streamType == StreamType.VIDEO_STREAM }
            
            Timber.tag(TAG).d("Search returned ${results.size} video results")
            
            results
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Search failed")
            emptyList()
        }
    }
    
    /**
     * Filter and select the BEST music video from search results
     * 
     * Scoring system:
     * - Official/Verified channel: +50 points
     * - Has "official video" in title: +40 points
     * - Has "music video" in title: +35 points
     * - Has "mv" in title: +30 points
     * - Higher view count: +10 points (for top result)
     * - Skip videos with "topic", "audio", "lyrics": -1000 points
     */
    private fun filterBestMusicVideo(
        results: List<StreamInfoItem>,
        title: String,
        artist: String?
    ): StreamInfoItem? {
        if (results.isEmpty()) return null
        
        Timber.tag(TAG).d("🎯 Filtering best music video from ${results.size} results")
        
        val scored = results.map { item ->
            val itemTitle = item.name?.lowercase() ?: ""
            val uploaderName = item.uploaderName?.lowercase() ?: ""
            var score = 0
            
            // Skip if no title
            if (itemTitle.isEmpty()) {
                return@map item to -1000
            }
            
            // Check if should skip
            if (isSkippableVideo(itemTitle)) {
                Timber.tag(TAG).d("❌ SKIP: ${item.name} (contains skip keywords)")
                return@map item to -1000
            }
            
            // Check if it's a real music video
            if (isRealMusicVideo(itemTitle)) {
                score += 40
                Timber.tag(TAG).d("✅ REAL VIDEO: ${item.name} (+40)")
            }
            
            // Verified/Official channel bonus
            if (item.isUploaderVerified || uploaderName.contains("official") || uploaderName.contains("vevo")) {
                score += 50
                Timber.tag(TAG).d("✅ VERIFIED: ${item.uploaderName} (+50)")
            }
            
            // Title match bonus
            val titleWords = title.lowercase().split(" ")
            val matchedWords = titleWords.count { word ->
                word.length > 2 && itemTitle.contains(word)
            }
            score += matchedWords * 5
            
            // Artist match bonus
            if (artist != null) {
                val artistWords = artist.lowercase().split(" ")
                val matchedArtist = artistWords.count { word ->
                    word.length > 2 && (itemTitle.contains(word) || uploaderName.contains(word))
                }
                score += matchedArtist * 10
            }
            
            // View count bonus (for popularity)
            val views = item.viewCount
            if (views > 1_000_000) score += 10
            if (views > 10_000_000) score += 5
            
            Timber.tag(TAG).d("Score $score: ${item.name} by ${item.uploaderName}")
            
            item to score
        }
        
        // Get best scored video
        val best = scored.maxByOrNull { it.second }
        
        if (best != null && best.second > 0) {
            Timber.tag(TAG).d("🏆 BEST MATCH (score ${best.second}): ${best.first.name}")
            return best.first
        }
        
        Timber.tag(TAG).w("⚠️ No good match found (all scores <= 0)")
        return null
    }
    
    /**
     * Check if video title indicates it's a REAL music video
     */
    private fun isRealMusicVideo(titleLowercase: String): Boolean {
        return REAL_VIDEO_KEYWORDS.any { keyword ->
            titleLowercase.contains(keyword)
        }
    }
    
    /**
     * Check if video should be skipped (audio-only, lyrics, etc.)
     */
    private fun isSkippableVideo(titleLowercase: String): Boolean {
        return SKIP_KEYWORDS.any { keyword ->
            titleLowercase.contains(keyword)
        }
    }
    
    /**
     * Extract best MP4 video URL from video URL
     * Returns the highest quality MP4 video stream
     */
    private suspend fun extractBestMp4VideoUrl(videoUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).d("📹 Extracting best MP4 video URL")
            Timber.tag(TAG).d("From: $videoUrl")
            
            // Get YouTube service
            val service = ServiceList.YouTube
            val linkHandler = service.getStreamLHFactory().fromUrl(videoUrl)
            
            // Get stream extractor
            val extractor = service.getStreamExtractor(linkHandler) as YoutubeStreamExtractor
            extractor.fetchPage()
            
            // Get video streams
            val videoStreams = extractor.videoStreams // Combined video+audio
            val videoOnlyStreams = extractor.videoOnlyStreams // Video-only (higher quality)
            
            Timber.tag(TAG).d("Found ${videoStreams.size} combined streams")
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
            
            // PRIORITY 1: Combined streams (video+audio) - sorted by quality
            if (mp4Combined.isNotEmpty()) {
                val bestCombined = mp4Combined
                    .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
                    .first()
                
                Timber.tag(TAG).d("✅ Selected combined stream: ${bestCombined.resolution}")
                return@withContext bestCombined.url
            }
            
            // PRIORITY 2: Video-only streams (highest quality)
            if (mp4VideoOnly.isNotEmpty()) {
                val bestVideoOnly = mp4VideoOnly
                    .sortedByDescending { it.resolution?.replace("p", "")?.toIntOrNull() ?: 0 }
                    .first()
                
                Timber.tag(TAG).d("✅ Selected video-only stream: ${bestVideoOnly.resolution} (needs audio merge)")
                return@withContext bestVideoOnly.url
            }
            
            Timber.tag(TAG).w("⚠️ No MP4 streams found")
            null
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Failed to extract video URL")
            null
        }
    }
    
    /**
     * Alternative: Try fallback search if first attempt fails
     */
    suspend fun getRealVideoWithFallback(
        title: String,
        artist: String?
    ): String? {
        // Try main search first
        var videoUrl = getRealVideoFromTitleAndArtist(title, artist)
        
        if (videoUrl != null) return videoUrl
        
        // Try without "official music video" if first attempt failed
        if (artist != null) {
            Timber.tag(TAG).d("🔄 Trying fallback search without 'official music video'")
            val simpleQuery = "$title $artist"
            val results = searchYouTube(simpleQuery)
            val bestVideo = filterBestMusicVideo(results, title, artist)
            
            if (bestVideo != null) {
                videoUrl = extractBestMp4VideoUrl(bestVideo.url)
            }
        }
        
        return videoUrl
    }
    
    // ========================================================================
    // PERFECT ARCHITECTURE FUNCTIONS (As per latest requirements)
    // ========================================================================
    
    /**
     * 1. Search YouTube for song video
     * Searches YouTube using title + artist and returns raw search results
     * 
     * @param title Song title
     * @param artist Artist name
     * @return List of search results (InfoItem)
     */
    suspend fun searchSongVideo(title: String, artist: String): List<InfoItem> = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).d("🔍 Searching YouTube for: $title by $artist")
            
            // Build optimized search query
            val searchQuery = buildSearchQuery(title, artist)
            Timber.tag(TAG).d("Query: $searchQuery")
            
            // Use YouTube service
            val service = ServiceList.YouTube
            val searchExtractor = service.getSearchExtractor(searchQuery)
            searchExtractor.fetchPage()
            
            // Get all results
            val results = searchExtractor.initialPage.items
            
            Timber.tag(TAG).d("Found ${results.size} total results")
            
            results
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Search failed")
            emptyList()
        }
    }
    
    /**
     * 2. Filter and select the BEST music video from search results
     * Applies intelligent filtering to find REAL music videos (not audio/lyrics/topic)
     * 
     * @param results List of search results
     * @return Best music video or null if none found
     */
    fun filterBestMusicVideo(results: List<InfoItem>): InfoItem? {
        if (results.isEmpty()) return null
        
        Timber.tag(TAG).d("🎯 Filtering ${results.size} results for REAL music video")
        
        // Filter only video streams
        val videoResults = results.filterIsInstance<StreamInfoItem>()
            .filter { it.streamType == StreamType.VIDEO_STREAM }
        
        if (videoResults.isEmpty()) {
            Timber.tag(TAG).w("⚠️ No video results found")
            return null
        }
        
        Timber.tag(TAG).d("Found ${videoResults.size} video results")
        
        // Score each video
        val scored = videoResults.map { item ->
            val titleLower = item.name?.lowercase() ?: ""
            val uploaderLower = item.uploaderName?.lowercase() ?: ""
            var score = 0
            
            // Skip if no title
            if (titleLower.isEmpty()) {
                return@map item to -1000
            }
            
            // ✅ REJECT keywords (auto-reject)
            val rejectKeywords = listOf(
                "topic", "official audio", "audio only", "audio",
                "lyric", "lyrics", "lyric video", "lyrics video",
                "visualizer", "audio visualizer",
                "slowed", "reverb", "slowed + reverb",
                "cover", "karaoke", "instrumental", "remix"
            )
            
            if (rejectKeywords.any { titleLower.contains(it) }) {
                Timber.tag(TAG).d("❌ REJECT: ${item.name} (contains reject keyword)")
                return@map item to -1000
            }
            
            // ✅ PREFER keywords (bonus points)
            val preferKeywords = mapOf(
                "official music video" to 50,
                "official video" to 45,
                "music video" to 40,
                "mv" to 35,
                "hd video" to 30,
                "(official video)" to 45,
                "[official video]" to 45,
                "videoclip" to 35
            )
            
            for ((keyword, points) in preferKeywords) {
                if (titleLower.contains(keyword)) {
                    score += points
                    Timber.tag(TAG).d("✅ +$points: ${item.name} (has '$keyword')")
                }
            }
            
            // Verified/Official channel
            if (item.isUploaderVerified || uploaderLower.contains("official") || uploaderLower.contains("vevo")) {
                score += 50
                Timber.tag(TAG).d("✅ +50: Verified channel (${item.uploaderName})")
            }
            
            // View count bonus
            val views = item.viewCount
            if (views > 1_000_000) score += 10
            if (views > 10_000_000) score += 5
            if (views > 100_000_000) score += 5
            
            Timber.tag(TAG).d("Score $score: ${item.name}")
            
            item to score
        }
        
        // Get best scored
        val best = scored.maxByOrNull { it.second }
        
        if (best != null && best.second > 0) {
            Timber.tag(TAG).d("🏆 BEST (score ${best.second}): ${best.first.name}")
            return best.first
        }
        
        Timber.tag(TAG).w("⚠️ No suitable music video found (all rejected)")
        return null
    }
    
    /**
     * 3. Get detailed StreamInfo for a video
     * Fetches complete stream information including all available video/audio streams
     * 
     * @param videoUrl YouTube video URL
     * @return StreamInfo object with all stream details
     */
    suspend fun getStreamInfoForVideo(videoUrl: String): StreamInfo = withContext(Dispatchers.IO) {
        Timber.tag(TAG).d("📹 Fetching StreamInfo for: $videoUrl")
        
        val service = ServiceList.YouTube
        val streamInfo = StreamInfo.getInfo(service, videoUrl)
        
        Timber.tag(TAG).d("✅ StreamInfo loaded")
        Timber.tag(TAG).d("Title: ${streamInfo.name}")
        Timber.tag(TAG).d("Video streams: ${streamInfo.videoStreams.size}")
        Timber.tag(TAG).d("Video-only streams: ${streamInfo.videoOnlyStreams.size}")
        Timber.tag(TAG).d("Audio streams: ${streamInfo.audioStreams.size}")
        
        streamInfo
    }
    
    /**
     * 4. Extract best MP4 video stream from StreamInfo
     * PERFECT PRIORITY SYSTEM:
     * 1. videoStreams (mixed video+audio MP4) → highest resolution
     * 2. videoOnlyStreams (video-only MP4) → highest resolution
     * 3. NEVER returns audio streams
     * 4. Returns null if no MP4 video found
     * 
     * @param streamInfo StreamInfo object
     * @return MP4 video stream URL or null
     */
    fun extractBestMp4Video(streamInfo: StreamInfo): String? {
        Timber.tag(TAG).d("🎬 Extracting BEST MP4 video stream")
        
        // Get all video streams
        val videoStreams = streamInfo.videoStreams // Mixed video+audio
        val videoOnlyStreams = streamInfo.videoOnlyStreams // Video-only (higher quality)
        
        Timber.tag(TAG).d("Available: ${videoStreams.size} mixed, ${videoOnlyStreams.size} video-only")
        
        // PRIORITY 1: Mixed video+audio MP4 streams
        val mp4MixedStreams = videoStreams.filter { stream ->
            isMp4Stream(stream)
        }
        
        if (mp4MixedStreams.isNotEmpty()) {
            val bestMixed = mp4MixedStreams
                .sortedByDescending { getResolutionValue(it) }
                .first()
            
            val resolution = bestMixed.resolution ?: "unknown"
            Timber.tag(TAG).d("✅ SELECTED: Mixed MP4 stream ($resolution)")
            Timber.tag(TAG).d("URL: ${bestMixed.url?.take(80)}...")
            
            return bestMixed.url
        }
        
        // PRIORITY 2: Video-only MP4 streams (fallback for HD)
        val mp4VideoOnlyStreams = videoOnlyStreams.filter { stream ->
            isMp4Stream(stream)
        }
        
        if (mp4VideoOnlyStreams.isNotEmpty()) {
            val bestVideoOnly = mp4VideoOnlyStreams
                .sortedByDescending { getResolutionValue(it) }
                .first()
            
            val resolution = bestVideoOnly.resolution ?: "unknown"
            Timber.tag(TAG).d("✅ SELECTED: Video-only MP4 stream ($resolution) [needs audio merge]")
            Timber.tag(TAG).d("URL: ${bestVideoOnly.url?.take(80)}...")
            
            return bestVideoOnly.url
        }
        
        // NO MP4 VIDEO FOUND
        Timber.tag(TAG).w("❌ No MP4 video streams found")
        Timber.tag(TAG).w("❌ NEVER using audio streams (correct behavior)")
        
        return null
    }
    
    /**
     * 5. Main function: Get real music video URL
     * Complete pipeline from search to MP4 URL extraction
     * 
     * @param title Song title
     * @param artist Artist name
     * @return Direct MP4 video URL or null if not found
     */
    suspend fun getRealMusicVideoUrl(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            Timber.tag(TAG).d("🎯 PIPELINE START: Getting real music video")
            Timber.tag(TAG).d("Title: $title")
            Timber.tag(TAG).d("Artist: $artist")
            
            // Step A & B: Search YouTube
            val searchResults = searchSongVideo(title, artist)
            
            if (searchResults.isEmpty()) {
                Timber.tag(TAG).w("⚠️ No search results")
                return@withContext null
            }
            
            // Step C & D: Filter and select best music video
            val bestVideo = filterBestMusicVideo(searchResults)
            
            if (bestVideo !is StreamInfoItem) {
                Timber.tag(TAG).w("⚠️ No suitable music video found")
                return@withContext null
            }
            
            Timber.tag(TAG).d("✅ Selected: ${bestVideo.name}")
            Timber.tag(TAG).d("Uploader: ${bestVideo.uploaderName}")
            
            // Step E: Get detailed StreamInfo
            val streamInfo = getStreamInfoForVideo(bestVideo.url)
            
            // Step F: Extract best MP4 video
            val mp4VideoUrl = extractBestMp4Video(streamInfo)
            
            if (mp4VideoUrl == null) {
                Timber.tag(TAG).w("⚠️ No MP4 video stream available")
                return@withContext null
            }
            
            Timber.tag(TAG).d("🎉 SUCCESS: Real music video URL obtained")
            Timber.tag(TAG).d("🎉 This is a REAL video (MP4), NOT audio+thumbnail")
            
            mp4VideoUrl
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Pipeline failed")
            null
        }
    }
    
    // ========================================================================
    // HELPER FUNCTIONS
    // ========================================================================
    
    /**
     * Check if stream is MP4 format
     */
    private fun isMp4Stream(stream: VideoStream): Boolean {
        val mimeType = stream.format?.mimeType?.lowercase() ?: ""
        val suffix = stream.format?.suffix?.lowercase() ?: ""
        
        return mimeType.contains("mp4") || suffix == "mp4"
    }
    
    /**
     * Get numeric resolution value for sorting
     */
    private fun getResolutionValue(stream: VideoStream): Int {
        val resolution = stream.resolution ?: return 0
        
        // Extract number from "1080p", "720p", etc.
        return resolution.replace("p", "").toIntOrNull() ?: 0
    }
}
