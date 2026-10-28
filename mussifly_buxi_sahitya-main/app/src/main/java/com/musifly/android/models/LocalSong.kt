package com.musifly.android.models

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.Serializable

/**
 * Data class representing a local music file from device storage
 */
data class LocalSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val duration: Long,
    val size: Long,
    val albumId: Long,
    val albumArtUri: Uri?,
    val dateAdded: Long,
    val mimeType: String,
    val bitrate: Int = 0
) : Serializable {
    
    /**
     * Convert LocalSong to Media3 MediaItem for playback
     * 📱 FOR LOCAL MUSIC: Creates green test bitmap and saves to file
     * Uses FILE URI - works for notification AND player UI!
     */
    fun toMediaItem(context: Context): MediaItem {
        android.util.Log.d("LocalSong", "🎵 toMediaItem() called for: $title (ID: $id)")
        
        val mediaStoreUri = android.content.ContentUris.withAppendedId(
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            id
        )
        
        val mediaId = "local_$id"
        
        // 📱 BETTER URI STRATEGY:
        // 1. Use MediaStore album art URI if available
        // 2. Use system-readable resource URI for app icon fallback
        // This avoids process/permission issues with file:// URIs from internal cache
        val thumbUri = albumArtUri ?: Uri.parse("android.resource://${context.packageName}/${com.musifly.android.R.mipmap.ic_musifly_launcher}")
        
        val mediaMetadata = toMediaMetadata(thumbUri.toString())
        
        return MediaItem.Builder()
            .setUri(mediaStoreUri)
            .setMediaId(mediaId)
            .setCustomCacheKey(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(thumbUri)
                    .setDurationMs(duration)
                    .build()
            )
            .setTag(mediaMetadata)
            .build()
    }

    /**
     * Convert LocalSong to our custom MediaMetadata for UI state
     */
    fun toMediaMetadata(thumbnailUrl: String?): com.musifly.android.models.MediaMetadata {
        return com.musifly.android.models.MediaMetadata(
            id = "local_$id",
            title = title,
            artists = listOf(com.musifly.android.models.MediaMetadata.Artist(id = null, name = artist)),
            duration = (duration / 1000).toInt(),
            thumbnailUrl = thumbnailUrl,
            album = com.musifly.android.models.MediaMetadata.Album(id = albumId.toString(), title = album)
        )
    }
    
    /**
     * Helper to convert Bitmap to ByteArray for artwork
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
    
    /**
     * Get formatted duration string (MM:SS)
     */
    fun getFormattedDuration(): String {
        val seconds = (duration / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
    
    /**
     * Get file size in human-readable format
     */
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
            else -> String.format("%.1f MB", size / (1024.0 * 1024.0))
        }
    }
    
    /**
     * Check if this is a local song (for MusicService detection)
     */
    fun isLocal(): Boolean = true
    
    companion object {
        /**
         * Check if a media ID represents a local song
         */
        fun isLocalMediaId(mediaId: String): Boolean {
            return mediaId.startsWith("local_")
        }
        
        /**
         * Extract local song ID from media ID
         */
        fun extractLocalId(mediaId: String): Long? {
            return if (isLocalMediaId(mediaId)) {
                mediaId.removePrefix("local_").toLongOrNull()
            } else null
        }
    }
}
