package com.musifly.android.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.musifly.android.R
import com.musifly.android.models.LocalSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides thumbnails for media items with proper fallback logic:
 * - Local files: ALWAYS use app icon (per design requirements)
 * - Streaming: fetched artwork → app icon
 */
object ThumbnailProvider {
    private const val TAG = "ThumbnailProvider"
    
    // Cache for app icon bitmap
    private var appIconBitmap: Bitmap? = null
    
    /**
     * Gets the app icon as a fallback thumbnail
     * Never returns null - creates a colored bitmap as last resort
     */
    fun getAppIconBitmap(context: Context): Bitmap {
        if (appIconBitmap == null) {
            try {
                // Try to load the main app icon (Adaptive Icon)
                val iconDrawable = androidx.core.content.res.ResourcesCompat.getDrawable(
                    context.resources,
                    com.musifly.android.R.mipmap.ic_musifly_launcher,
                    null
                ) ?: androidx.core.content.res.ResourcesCompat.getDrawable(
                    context.resources,
                    com.musifly.android.R.drawable.opentune_monochrome,
                    null
                )
                
                if (iconDrawable != null) {
                    appIconBitmap = drawableToBitmap(iconDrawable)
                    Log.d(TAG, "✅ App icon loaded successfully: ic_musifly_launcher")
                } else {
                    Log.w(TAG, "Failed to load app icon drawable, creating colored fallback")
                    appIconBitmap = createFallbackBitmap()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading app icon drawable, creating fallback", e)
                appIconBitmap = createFallbackBitmap()
            }
        }
        return appIconBitmap!!
    }

    /**
     * Robust conversion from Drawable to Bitmap, handles Adaptive Icons correctly
     */
    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
        if (drawable is android.graphics.drawable.BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }

        // For AdaptiveIconDrawable or VectorDrawable, create a clean ARGB_8888 bitmap
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 512
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 512
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
    
    /**
     * Creates a simple colored bitmap as absolute last resort fallback
     */
    private fun createFallbackBitmap(): Bitmap {
        // Create a 512x512 bitmap with Spotify green color
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            color = 0xFF1DB954.toInt() // Spotify green
            style = android.graphics.Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 512f, 512f, paint)
        
        // Draw "M" for Musifly in white
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 300f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("M", 256f, 380f, paint)
        
        return bitmap
    }
    
    /**
     * Loads thumbnail for a MediaItem with proper fallback
     */
    suspend fun loadThumbnail(
        context: Context,
        mediaItem: MediaItem
    ): Bitmap = withContext(Dispatchers.IO) {
        try {
            // Check if this is a local file
            val isLocal = isLocalFile(mediaItem)
            
            if (isLocal) {
                loadLocalThumbnail(context, mediaItem)
            } else {
                loadStreamingThumbnail(context, mediaItem)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading thumbnail for ${mediaItem.mediaId}", e)
            getAppIconBitmap(context)
        }
    }
    
    /**
     * Loads thumbnail for local file:
     * 1. Try embedded album art
     * 2. Fall back to app icon
     */
    private fun loadLocalThumbnail(context: Context, mediaItem: MediaItem): Bitmap {
        Log.d(TAG, "📸 Loading LOCAL thumbnail for: ${mediaItem.mediaId}")
        
        // PER OBJECTIVE: Consistently use app icon for local music
        // We skip embedded art to ensure unified visual identity
        Log.d(TAG, "🎵 Using APP ICON for local music (per design requirement)")
        return getAppIconBitmap(context)
    }
    
    /**
     * Loads thumbnail for streaming content:
     * 1. Try fetched artwork from metadata
     * 2. Fall back to app icon
     */
    private fun loadStreamingThumbnail(context: Context, mediaItem: MediaItem): Bitmap {
        Log.d(TAG, "📸 Loading STREAMING thumbnail for: ${mediaItem.mediaId}")
        
        // Try metadata artwork data first (from fetched data)
        mediaItem.mediaMetadata.artworkData?.let { artworkData ->
            try {
                val bitmap = BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
                if (bitmap != null) {
                    Log.d(TAG, "✅ Loaded from fetched metadata")
                    return bitmap
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode fetched artworkData", e)
            }
        }
        
        // Try artworkUri if available
        mediaItem.mediaMetadata.artworkUri?.let { artworkUri ->
            try {
                // Note: This should be loaded asynchronously with Coil/Glide
                // For now, we'll just fall back to app icon
                Log.d(TAG, "Artwork URI available but not loaded: $artworkUri")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load artwork from URI: $artworkUri", e)
            }
        }
        
        // Fallback to app icon
        Log.d(TAG, "🌐 Using APP ICON fallback for streaming")
        return getAppIconBitmap(context)
    }
    
    /**
     * Checks if a MediaItem represents a local file
     */
    private fun isLocalFile(mediaItem: MediaItem): Boolean {
        // Check by mediaId prefix
        if (mediaItem.mediaId.startsWith("local_")) {
            return true
        }
        
        // Check by URI scheme
        mediaItem.localConfiguration?.uri?.let { uri ->
            return uri.scheme == "file" || uri.scheme == "content"
        }
        
        return false
    }
    
    /**
     * Updates MediaMetadata with embedded artwork for local files
     */
    suspend fun updateLocalFileMetadata(
        context: Context,
        mediaItem: MediaItem
    ): MediaMetadata = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 updateLocalFileMetadata called for: ${mediaItem.mediaId}")
        
        if (!isLocalFile(mediaItem)) {
            Log.d(TAG, "⚠️ Not a local file, skipping thumbnail update")
            return@withContext mediaItem.mediaMetadata
        }
        
        Log.d(TAG, "📱 LOCAL FILE - Using app icon as thumbnail")
        
        try {
            // For local files, ALWAYS use app icon
            val appIcon = getAppIconBitmap(context)
            Log.d(TAG, "✅ App icon loaded: ${appIcon.width}x${appIcon.height}")
            
            val iconBytes = bitmapToByteArray(appIcon)
            Log.d(TAG, "🔄 App icon converted to bytes: ${iconBytes.size} bytes")
            
            return@withContext mediaItem.mediaMetadata.buildUpon()
                .setArtworkData(iconBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error setting app icon for local file", e)
            return@withContext mediaItem.mediaMetadata
        }
    }
    
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
