package com.musifly.android.utils

import timber.log.Timber

/**
 * Extract YouTube video ID from various URL formats
 */
object VideoIdExtractor {
    
    private const val TAG = "VideoIdExtractor"
    
    /**
     * Extract video ID from YouTube URL
     * 
     * Supported formats:
     * - https://www.youtube.com/watch?v=VIDEO_ID
     * - https://youtu.be/VIDEO_ID
     * - https://youtube.com/shorts/VIDEO_ID
     * - https://youtube.com/embed/VIDEO_ID
     * - https://m.youtube.com/watch?v=VIDEO_ID
     * - youtube.com/watch?v=VIDEO_ID&other=params
     * 
     * @param url YouTube URL
     * @return Video ID or null if not found
     */
    fun extractVideoId(url: String): String? {
        try {
            Timber.tag(TAG).d("Extracting video ID from: $url")
            
            // Remove whitespace
            val cleanUrl = url.trim()
            
            // Pattern 1: youtube.com/watch?v=VIDEO_ID
            val watchPattern = Regex("""[?&]v=([a-zA-Z0-9_-]{11})""")
            watchPattern.find(cleanUrl)?.let {
                val videoId = it.groupValues[1]
                Timber.tag(TAG).d("Found video ID (watch): $videoId")
                return videoId
            }
            
            // Pattern 2: youtu.be/VIDEO_ID
            val shortPattern = Regex("""youtu\.be/([a-zA-Z0-9_-]{11})""")
            shortPattern.find(cleanUrl)?.let {
                val videoId = it.groupValues[1]
                Timber.tag(TAG).d("Found video ID (short): $videoId")
                return videoId
            }
            
            // Pattern 3: youtube.com/shorts/VIDEO_ID
            val shortsPattern = Regex("""youtube\.com/shorts/([a-zA-Z0-9_-]{11})""")
            shortsPattern.find(cleanUrl)?.let {
                val videoId = it.groupValues[1]
                Timber.tag(TAG).d("Found video ID (shorts): $videoId")
                return videoId
            }
            
            // Pattern 4: youtube.com/embed/VIDEO_ID
            val embedPattern = Regex("""youtube\.com/embed/([a-zA-Z0-9_-]{11})""")
            embedPattern.find(cleanUrl)?.let {
                val videoId = it.groupValues[1]
                Timber.tag(TAG).d("Found video ID (embed): $videoId")
                return videoId
            }
            
            // Pattern 5: youtube.com/v/VIDEO_ID
            val vPattern = Regex("""youtube\.com/v/([a-zA-Z0-9_-]{11})""")
            vPattern.find(cleanUrl)?.let {
                val videoId = it.groupValues[1]
                Timber.tag(TAG).d("Found video ID (v): $videoId")
                return videoId
            }
            
            // If URL is already just the video ID (11 characters)
            if (cleanUrl.matches(Regex("""^[a-zA-Z0-9_-]{11}$"""))) {
                Timber.tag(TAG).d("Input is already a video ID: $cleanUrl")
                return cleanUrl
            }
            
            Timber.tag(TAG).w("Could not extract video ID from: $url")
            return null
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error extracting video ID")
            return null
        }
    }
    
    /**
     * Validate if string is a valid YouTube video ID
     */
    fun isValidVideoId(videoId: String): Boolean {
        return videoId.matches(Regex("""^[a-zA-Z0-9_-]{11}$"""))
    }
    
    /**
     * Create YouTube watch URL from video ID
     */
    fun createWatchUrl(videoId: String): String {
        return "https://www.youtube.com/watch?v=$videoId"
    }
    
    /**
     * Create YouTube short URL from video ID
     */
    fun createShortUrl(videoId: String): String {
        return "https://youtu.be/$videoId"
    }
}
