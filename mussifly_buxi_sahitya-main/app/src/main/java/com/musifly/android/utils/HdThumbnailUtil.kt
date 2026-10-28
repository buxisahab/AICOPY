package com.musifly.android.utils

import android.content.Context
import coil.request.ImageRequest
import coil.size.Size
import com.musifly.android.models.MediaMetadata

/**
 * Utility object for loading high-quality YouTube thumbnails.
 * Supports fallback from maxresdefault -> hqdefault -> mqdefault
 */
object HdThumbnailUtil {
    
    // YouTube thumbnail quality variants (highest to lowest)
    private const val MAXRES_DEFAULT = "maxresdefault"
    private const val HQ_DEFAULT = "hqdefault"
    private const val MQ_DEFAULT = "mqdefault"
    private const val SD_DEFAULT = "sddefault"
    
    /**
     * Get the highest quality thumbnail URL for a video ID
     */
    fun getMaxResThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/$MAXRES_DEFAULT.jpg"
    }
    
    /**
     * Get HQ fallback thumbnail URL
     */
    fun getHqThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/$HQ_DEFAULT.jpg"
    }
    
    /**
     * Get MQ fallback thumbnail URL
     */
    fun getMqThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/$MQ_DEFAULT.jpg"
    }
    
    /**
     * Get SD fallback thumbnail URL
     */
    fun getSdThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/$SD_DEFAULT.jpg"
    }
    
    /**
     * Get ordered list of thumbnail URLs from highest to lowest quality
     */
    fun getThumbnailUrls(videoId: String): List<String> {
        return listOf(
            getMaxResThumbnailUrl(videoId),
            getHqThumbnailUrl(videoId),
            getMqThumbnailUrl(videoId),
            getSdThumbnailUrl(videoId)
        )
    }
    
    /**
     * Extract video ID from media metadata. 
     * Returns null for local files and JioSaavn to prevent YouTube fallback.
     */
    fun extractVideoId(mediaMetadata: MediaMetadata?): String? {
        val id = mediaMetadata?.id ?: return null
        if (id.startsWith("local_") || id.startsWith("saavn:") || id.startsWith("spotify:")) {
            return null
        }
        return id
    }
    
    /**
     * Get best thumbnail URL from MediaMetadata, with YouTube HD fallback
     * For JioSaavn songs, upgrades image URLs to highest quality (500x500)
     */
    fun getBestThumbnailUrl(mediaMetadata: MediaMetadata?): String? {
        if (mediaMetadata == null) return null
        
        val mediaId = mediaMetadata.id
        if (mediaId.isEmpty()) return mediaMetadata.thumbnailUrl
        
        // Check if this is a JioSaavn song
        if (mediaId.startsWith("saavn:")) {
            // For JioSaavn, upgrade thumbnail URL to highest quality
            val thumbnailUrl = mediaMetadata.thumbnailUrl
            return if (thumbnailUrl != null && thumbnailUrl.isNotEmpty()) {
                // Upgrade to highest quality (500x500)
                val upgraded = com.musifly.android.models.upgradeJioSaavnImageUrl(thumbnailUrl)
                android.util.Log.d("HdThumbnailUtil", "📸 JioSaavn thumbnail: original=${thumbnailUrl.take(60)}..., upgraded=${upgraded.take(60)}...")
                upgraded
            } else {
                // Log when thumbnailUrl is empty for debugging
                android.util.Log.w("HdThumbnailUtil", "⚠️ JioSaavn song (${mediaId}) has empty thumbnailUrl - metadata may not be enriched yet")
                null
            }
        }
        
        // Check if this is a local song
        if (mediaId.startsWith("local_")) {
            android.util.Log.d("HdThumbnailUtil", "🎵 Local song thumbnail: ${mediaMetadata.thumbnailUrl}")
            return mediaMetadata.thumbnailUrl
        }
        
        // For YouTube, prefer maxres thumbnail
        val videoId = extractVideoId(mediaMetadata)
        return if (videoId != null) {
            getMaxResThumbnailUrl(videoId)
        } else {
            // Fallback to thumbnailUrl if available (Local files, JioSaavn fallback, etc.)
            mediaMetadata.thumbnailUrl
        }
    }
    
    /**
     * Build a Coil ImageRequest with fallback URLs
     */
    fun buildThumbnailRequest(
        context: Context,
        mediaMetadata: MediaMetadata?,
        size: Size = Size.ORIGINAL
    ): ImageRequest {
        val videoId = extractVideoId(mediaMetadata)
        val primaryUrl = if (videoId != null) {
            getMaxResThumbnailUrl(videoId)
        } else {
            mediaMetadata?.thumbnailUrl
        }
        
        return ImageRequest.Builder(context)
            .data(primaryUrl)
            .size(size)
            .crossfade(true)
            .crossfade(300)
            .allowHardware(true)
            .build()
    }
}
