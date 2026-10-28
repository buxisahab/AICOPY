package com.musifly.android.utils

import com.musifly.android.models.MediaMetadata

/**
 * Low-Resolution Thumbnail Utility for Real-Time Safe Playback
 * 
 * During playback, use low-res cached thumbnails to avoid blocking threads.
 * Never decode maxresdefault.jpg during active playback.
 */
object LowResThumbnailUtil {
    
    // YouTube thumbnail quality variants (lowest to highest)
    private const val MQ_DEFAULT = "mqdefault"  // 320x180 - fast, cached
    private const val HQ_DEFAULT = "hqdefault"  // 480x360 - medium
    private const val MAXRES_DEFAULT = "maxresdefault"  // 1280x720 - heavy, avoid during playback
    
    /**
     * Get low-resolution thumbnail URL for playback (real-time safe)
     * Uses mqdefault (320x180) which is fast to decode and usually cached
     */
    fun getLowResThumbnailUrl(mediaMetadata: MediaMetadata?): String? {
        if (mediaMetadata == null) return null
        
        val mediaId = mediaMetadata.id
        if (mediaId.isEmpty()) return mediaMetadata.thumbnailUrl
        
        // For JioSaavn, use existing thumbnailUrl (already optimized)
        if (mediaId.startsWith("saavn:")) {
            return mediaMetadata.thumbnailUrl
        }
        
        // For YouTube, use mqdefault (low-res, fast) instead of maxresdefault
        // Extract video ID from mediaId (format: "youtube:VIDEO_ID" or just "VIDEO_ID")
        val videoId = mediaId.removePrefix("youtube:").takeIf { it.isNotEmpty() } ?: return mediaMetadata.thumbnailUrl
        
        // Use mqdefault (320x180) - fast, cached, real-time safe
        return "https://img.youtube.com/vi/$videoId/$MQ_DEFAULT.jpg"
    }
    
    /**
     * Get medium-resolution thumbnail URL (for when not playing)
     * Uses hqdefault (480x360) - better quality but still reasonable
     */
    fun getMediumResThumbnailUrl(mediaMetadata: MediaMetadata?): String? {
        if (mediaMetadata == null) return null
        
        val mediaId = mediaMetadata.id
        if (mediaId.isEmpty()) return mediaMetadata.thumbnailUrl
        
        if (mediaId.startsWith("saavn:")) {
            return mediaMetadata.thumbnailUrl
        }
        
        val videoId = mediaId.removePrefix("youtube:").takeIf { it.isNotEmpty() } ?: return mediaMetadata.thumbnailUrl
        
        // Use hqdefault (480x360) - medium quality
        return "https://img.youtube.com/vi/$videoId/$HQ_DEFAULT.jpg"
    }
    
    /**
     * Check if URL is high-resolution (should be avoided during playback)
     */
    fun isHighResUrl(url: String?): Boolean {
        return url?.contains(MAXRES_DEFAULT) == true
    }
}
