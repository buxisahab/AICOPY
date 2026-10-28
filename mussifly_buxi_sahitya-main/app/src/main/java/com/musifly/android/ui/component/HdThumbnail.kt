package com.musifly.android.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.musifly.android.ui.utils.ThumbnailQuality
import com.musifly.android.ui.utils.extractYouTubeVideoId
import com.musifly.android.ui.utils.getYouTubeThumbnailUrls

/**
 * HD Thumbnail Composable with automatic fallback support
 * 
 * Loads YouTube thumbnails in HD quality (maxresdefault.jpg) with automatic
 * fallback to lower quality thumbnails if HD is not available:
 * - maxresdefault.jpg (1280x720)
 * - hqdefault.jpg (480x360)
 * - mqdefault.jpg (320x180)
 * 
 * Uses centerCrop for Instagram Reels-like fullscreen display without stretching.
 */
@Composable
fun HdThumbnail(
    thumbnailUrl: String?,
    videoId: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    crossfadeEnabled: Boolean = true,
    enableHardwareAcceleration: Boolean = false,
) {
    val context = LocalContext.current
    
    // Extract video ID from URL or use provided ID
    // CRITICAL: Filter out local and saavn IDs to prevent YouTube fallback
    val extractedVideoId = remember(thumbnailUrl, videoId) {
        val id = videoId ?: thumbnailUrl?.extractYouTubeVideoId()
        if (id?.startsWith("local_") == true || id?.startsWith("saavn:") == true || id?.startsWith("spotify:") == true) {
            null
        } else {
            id
        }
    }
    
    // REAL-TIME SAFE: Block maxresdefault decoding during playback
    val isPlaybackPriority = com.musifly.android.playback.PlaybackPriorityManager.shouldPauseBackgroundWork()
    
    // Generate fallback URLs if we have a video ID
    // During playback: use low-res only, never maxresdefault
    val thumbnailUrls = remember(extractedVideoId, thumbnailUrl, isPlaybackPriority) {
        if (extractedVideoId != null) {
            val urls = getYouTubeThumbnailUrls(extractedVideoId)
            // Filter out maxresdefault during playback
            if (isPlaybackPriority) {
                urls.filter { !it.contains("maxresdefault") }
            } else {
                urls
            }
        } else if (thumbnailUrl != null && thumbnailUrl.isNotEmpty()) {
            // Block high-res URLs during playback (only for streaming)
            val isStreaming = !thumbnailUrl.startsWith("file://") && !thumbnailUrl.startsWith("content://")
            if (isPlaybackPriority && isStreaming && com.musifly.android.utils.LowResThumbnailUtil.isHighResUrl(thumbnailUrl)) {
                emptyList() // Skip high-res during playback
            } else {
                listOf(thumbnailUrl)
            }
        } else {
            emptyList()
        }
    }
    
    // Track current URL index for fallback - reset when thumbnailUrls change
    var currentUrlIndex by remember(thumbnailUrls) { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Current URL to load
    val currentUrl = thumbnailUrls.getOrNull(currentUrlIndex)
    
    // IMAGE MODEL: Use app icon as fallback if URL is null
    val imageModel = remember(currentUrl, context) {
        ImageRequest.Builder(context)
            .data(currentUrl ?: com.musifly.android.R.mipmap.ic_musifly_launcher)
            .allowHardware(enableHardwareAcceleration)
            .memoryCacheKey(extractedVideoId ?: currentUrl)
            .diskCacheKey(extractedVideoId ?: currentUrl)
            .crossfade(crossfadeEnabled)
            .build()
    }
    
    // Use key to force recomposition when URL changes (important for JioSaavn metadata enrichment)
    key(currentUrl ?: "null") {
        AsyncImage(
            model = imageModel,
            contentDescription = contentDescription,
            contentScale = contentScale,
            placeholder = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
            error = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
            onError = { _: AsyncImagePainter.State.Error ->
                // Try next fallback URL if available
                if (currentUrlIndex < thumbnailUrls.size - 1) {
                    currentUrlIndex++
                }
                isLoading = false
            },
            onSuccess = { _: AsyncImagePainter.State.Success ->
                isLoading = false
            },
            onLoading = { _: AsyncImagePainter.State.Loading ->
                isLoading = true
            },
            modifier = modifier
                .then(
                    if (enableHardwareAcceleration) {
                        Modifier.graphicsLayer {
                            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                        }
                    } else {
                        Modifier
                    }
                )
        )
    }
}

/**
 * Full-screen HD Thumbnail optimized for player view (Instagram Reels style)
 * 
 * - Uses centerCrop for perfect fullscreen fit without distortion
 * - Hardware-accelerated for smooth scrolling
 * - Automatic HD quality with fallback
 */
@Composable
fun FullscreenHdThumbnail(
    thumbnailUrl: String?,
    videoId: String? = null,
    modifier: Modifier = Modifier,
    crossfadeEnabled: Boolean = true,
) {
    HdThumbnail(
        thumbnailUrl = thumbnailUrl,
        videoId = videoId,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop, // centerCrop equivalent
        crossfadeEnabled = crossfadeEnabled,
        enableHardwareAcceleration = false
    )
}

/**
 * Get the best HD thumbnail URL for a given song/video ID
 * This can be used to generate HD URLs before loading
 */
fun getHdThumbnailUrl(videoId: String): String {
    return "https://img.youtube.com/vi/$videoId/${ThumbnailQuality.MAX_RES.fileName}"
}

/**
 * Get the best HD thumbnail URL from an existing thumbnail URL
 */
fun String.upgradeToHdThumbnail(): String {
    val videoId = extractYouTubeVideoId() ?: return this
    return getHdThumbnailUrl(videoId)
}
