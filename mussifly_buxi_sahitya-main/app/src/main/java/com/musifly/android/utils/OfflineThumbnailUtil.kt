package com.musifly.android.utils

import android.content.Context
import coil.request.ImageRequest
import coil.request.CachePolicy

/**
 * Utility for loading thumbnails with offline support
 * Ensures thumbnails work even when device is offline by using Coil's disk cache
 */
object OfflineThumbnailUtil {
    
    /**
     * Creates an ImageRequest that works offline by using cached images
     * 
     * @param context Android context
     * @param thumbnailUrl URL of the thumbnail
     * @param allowNetworkAccess If false, only uses disk cache (for offline mode)
     * @return ImageRequest configured for offline support
     */
    fun createOfflineImageRequest(
        context: Context,
        thumbnailUrl: String?,
        allowNetworkAccess: Boolean = true
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(thumbnailUrl)
            .crossfade(300)
            .memoryCacheKey(thumbnailUrl) // Use URL as cache key
            .diskCacheKey(thumbnailUrl)
            .allowHardware(true) // Hardware acceleration for performance
            // CRITICAL: Use disk cache even when offline
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // If offline mode, don't attempt network fetch
            .networkCachePolicy(if (allowNetworkAccess) CachePolicy.ENABLED else CachePolicy.DISABLED)
            .build()
    }
    
    /**
     * Creates an ImageRequest optimized for offline playback
     * This version ONLY uses cached images and never attempts network fetch
     * 
     * @param context Android context
     * @param thumbnailUrl URL of the thumbnail
     * @return ImageRequest configured for offline-only mode
     */
    fun createOfflineOnlyImageRequest(
        context: Context,
        thumbnailUrl: String?
    ): ImageRequest {
        return createOfflineImageRequest(context, thumbnailUrl, allowNetworkAccess = false)
    }
}
