package com.musifly.android.models

import androidx.compose.runtime.Immutable
import com.musifly.innertube.models.SongItem
import com.musifly.android.db.entities.Song
import com.musifly.android.db.entities.SongEntity
import com.musifly.android.ui.utils.resize
import com.musifly.android.ui.utils.ThumbnailQuality
import com.musifly.android.ui.utils.toVideoThumbnailUrl
import com.musifly.android.streamingengines.models.StreamingSong
import com.musifly.android.streamingengines.models.SourceEngine
import java.io.Serializable

/**
 * Upgrade JioSaavn image URL to highest quality (500x500)
 * Converts low quality URLs like "_50x50.jpg", "_150x150.jpg" to "_500x500.jpg"
 * 
 * Example:
 * - "_50x50.jpg" → "_500x500.jpg"
 * - "150x150" → "500x500"
 * - "http://..." → "https://..."
 */
fun upgradeJioSaavnImageUrl(imageUrl: String): String {
    if (imageUrl.isEmpty()) return imageUrl
    
    var processedUrl = imageUrl
    
    // Convert HTTP to HTTPS
    processedUrl = processedUrl.replace("http://", "https://")
    
    // Convert various low quality sizes to high quality (500x500)
    // Priority: Replace specific patterns first to avoid partial matches
    processedUrl = processedUrl
        .replace("_50x50.jpg", "_500x500.jpg")
        .replace("_50x50.png", "_500x500.png")
        .replace("50x50", "500x500")
        .replace("_150x150.jpg", "_500x500.jpg")
        .replace("_150x150.png", "_500x500.png")
        .replace("150x150", "500x500")
        .replace("_250x250.jpg", "_500x500.jpg")
        .replace("_250x250.png", "_500x500.png")
        .replace("250x250", "500x500")
        .replace("_300x300.jpg", "_500x500.jpg")
        .replace("_300x300.png", "_500x500.png")
        .replace("300x300", "500x500")
    
    // If URL contains size pattern but not 500x500, upgrade it
    val sizePattern = Regex("\\d+x\\d+")
    if (sizePattern.containsMatchIn(processedUrl) && !processedUrl.contains("500x500")) {
        processedUrl = processedUrl.replace(sizePattern, "500x500")
    }
    
    return processedUrl
}

@Immutable
data class MediaMetadata(
    val id: String,
    val title: String,
    val artists: List<Artist>,
    val duration: Int,
    val thumbnailUrl: String? = null,
    val album: Album? = null,
    val setVideoId: String? = null,
    val explicit: Boolean = false,
    val liked: Boolean = false,
    val playCount: String = "0", // Actual play/like count from streaming APIs
) : Serializable {
    data class Artist(
        val id: String?,
        val name: String,
    ) : Serializable

    data class Album(
        val id: String,
        val title: String,
    ) : Serializable

    fun toSongEntity() =
        SongEntity(
            id = id,
            title = title,
            duration = duration,
            thumbnailUrl = thumbnailUrl,
            albumId = album?.id,
            albumName = album?.title,
        )
}

fun Song.toMediaMetadata() =
    MediaMetadata(
        id = song.id,
        title = song.title,
        artists =
            artists.map {
                MediaMetadata.Artist(
                    id = it.id,
                    name = it.name,
                )
            },
        duration = song.duration,
        thumbnailUrl = song.thumbnailUrl,
        album =
            album?.let {
                MediaMetadata.Album(
                    id = it.id,
                    title = it.title,
                )
            } ?: song.albumId?.let { albumId ->
                MediaMetadata.Album(
                    id = albumId,
                    title = song.albumName.orEmpty(),
                )
            },
    )

fun SongItem.toMediaMetadata() =
    MediaMetadata(
        id = id,
        title = title,
        artists =
            artists.map {
                MediaMetadata.Artist(
                    id = it.id,
                    name = it.name,
                )
            },
        duration = duration ?: -1,
        thumbnailUrl = if (id.startsWith("saavn:")) thumbnail else id.toVideoThumbnailUrl(ThumbnailQuality.MAX_RES), // Use provided thumbnail for Saavn
        album =
            album?.let {
                MediaMetadata.Album(
                    id = it.id,
                    title = it.name,
                )
            },
        explicit = explicit,
        setVideoId = setVideoId
    )

fun StreamingSong.toMediaMetadata() =
    MediaMetadata(
        id = when (source) {
            SourceEngine.JIOSAAVN -> "saavn:${id}"
            SourceEngine.YOUTUBE_MUSIC -> id
            SourceEngine.YOUTUBE_VIDEO -> id
            SourceEngine.SPOTIFY -> "spotify:${id}"
            else -> id
        },
        title = title,
        artists = artist.split(",").map { artistName ->
            MediaMetadata.Artist(
                id = null,
                name = artistName.trim(),
            )
        },
        duration = duration.toIntOrNull() ?: 0,
        thumbnailUrl = imageHighUrl.ifEmpty { imageUrl }.takeIf { it.isNotEmpty() },
        album = if (album.isNotEmpty()) {
            MediaMetadata.Album(
                id = "",
                title = album,
            )
        } else null,
        explicit = false,
        playCount = playCount, // Extract actual play count from API
    )

/**
 * Convert JioSaavn Map<String, Any> item to MediaMetadata
 * Used for Home Screen JioSaavn content
 */
fun Map<String, Any>.toMediaMetadataFromSaavnMap(albumImageUrl: String? = null): MediaMetadata {
    // Extract song ID
    val itemId = (this["id"] as? String) ?: ""
    
    // Extract title - ensure it's not "Unknown" or empty
    val title = (this["title"] as? String)
        ?.takeIf { it.isNotEmpty() && it != "Unknown" }
        ?: (this["song"] as? String)
            ?.takeIf { it.isNotEmpty() }
        ?: "Unknown Song"
    
    // Extract artist - try multiple sources
    val artist = (this["artist"] as? String)
        ?.takeIf { it.isNotEmpty() && it != "Unknown Artist" }
        ?: (this["subtitle"] as? String)
            ?.takeIf { it.isNotEmpty() }
        ?: (this["primary_artists"] as? String)
            ?.takeIf { it.isNotEmpty() }
        ?: (this["singers"] as? String)
            ?.takeIf { it.isNotEmpty() }
        ?: "Unknown Artist"
    
    // Extract album
    val album = (this["album"] as? String)
        ?.takeIf { it.isNotEmpty() }
        ?: ""
    
    // Extract duration - handle both string and int, prefer durationSeconds if available
    val duration = when {
        this["durationSeconds"] is Int -> (this["durationSeconds"] as Int)
        this["durationSeconds"] is Long -> (this["durationSeconds"] as Long).toInt()
        this["duration"] is Int -> (this["duration"] as Int)
        this["duration"] is Long -> (this["duration"] as Long).toInt()
        this["duration"] is String -> {
            val durStr = (this["duration"] as String).trim()
            // JioSaavn duration is typically in seconds as string
            durStr.toIntOrNull() ?: 0
        }
        else -> 0
    }
    
    // Extract image URL - prefer imageHigh, fallback to image, ensure highest quality
    // Also check if image is an array (JioSaavn format)
    // If song doesn't have image, use album image as fallback (important for top charts)
    val rawImageUrl = when {
        // First try imageHigh (already processed)
        (this["imageHigh"] as? String)?.takeIf { it.isNotEmpty() } != null -> {
            (this["imageHigh"] as String)
        }
        // Then try image as string
        (this["image"] as? String)?.takeIf { it.isNotEmpty() } != null -> {
            (this["image"] as String)
        }
        // Check if image is an array (JioSaavn format: [{"quality":"500x500","link":"..."}])
        this["image"] is List<*> -> {
            val imageList = this["image"] as List<*>
            // Find highest quality (500x500)
            imageList
                .filterIsInstance<Map<*, *>>()
                .mapNotNull { item ->
                    val quality = (item["quality"] as? String) ?: ""
                    val link = (item["link"] as? String) ?: (item["url"] as? String) ?: ""
                    if (link.isNotEmpty()) Pair(quality, link) else null
                }
                .maxByOrNull { (quality, _) ->
                    when {
                        quality == "500x500" -> 1000
                        quality.contains("500") -> 500
                        quality.contains("300") -> 300
                        quality.contains("250") -> 250
                        quality.contains("150") -> 150
                        quality.contains("50") -> 50
                        else -> 0
                    }
                }?.second ?: ""
        }
        // Try alternative field names
        (this["thumbnail"] as? String)?.takeIf { it.isNotEmpty() } != null -> {
            (this["thumbnail"] as String)
        }
        (this["image_url"] as? String)?.takeIf { it.isNotEmpty() } != null -> {
            (this["image_url"] as String)
        }
        (this["cover_image"] as? String)?.takeIf { it.isNotEmpty() } != null -> {
            (this["cover_image"] as String)
        }
        // Fallback to album image (important for top charts songs that might not have individual images)
        albumImageUrl?.takeIf { it.isNotEmpty() } != null -> {
            albumImageUrl
        }
        else -> ""
    }
    
    // Upgrade to highest quality if needed (for JioSaavn)
    val imageUrl = if (rawImageUrl.isNotEmpty()) {
        upgradeJioSaavnImageUrl(rawImageUrl)
    } else {
        ""
    }
    
    // Log for debugging top charts thumbnail issue
    if (imageUrl.isEmpty() && itemId.isNotEmpty()) {
        android.util.Log.w("MediaMetadata", "⚠️ No thumbnail URL for JioSaavn song: id=$itemId, title=$title")
        android.util.Log.d("MediaMetadata", "   Available fields: imageHigh=${this["imageHigh"]}, image=${this["image"]}, thumbnail=${this["thumbnail"]}, albumImageUrl=$albumImageUrl")
        android.util.Log.d("MediaMetadata", "   Image field type: ${this["image"]?.javaClass?.simpleName ?: "null"}")
        if (this["image"] is List<*>) {
            android.util.Log.d("MediaMetadata", "   Image is List with ${(this["image"] as List<*>).size} items")
        }
    } else if (imageUrl.isNotEmpty()) {
        android.util.Log.d("MediaMetadata", "✅ Thumbnail URL extracted: id=$itemId, url=${imageUrl.take(80)}")
    }
    
    // Split artists by comma and clean up
    val artistList = artist.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != "Unknown Artist" }
        .takeIf { it.isNotEmpty() }
        ?: listOf(artist)
    
    return MediaMetadata(
        id = "saavn:$itemId", // CRITICAL: Prefix with "saavn:" for engine detection
        title = title,
        artists = artistList.map { artistName ->
            MediaMetadata.Artist(
                id = null,
                name = artistName,
            )
        },
        duration = duration.coerceAtLeast(0), // Ensure non-negative
        thumbnailUrl = imageUrl.takeIf { it.isNotEmpty() },
        album = if (album.isNotEmpty()) {
            MediaMetadata.Album(
                id = "",
                title = album,
            )
        } else null,
        explicit = false,
    )
}
