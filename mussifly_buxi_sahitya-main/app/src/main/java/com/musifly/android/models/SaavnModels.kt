package com.musifly.android.models

/**
 * JioSaavn Home Feed Data Models
 * Structured data classes for JioSaavn home feed sections and items
 */

/**
 * Section types for JioSaavn home feed
 */
enum class SaavnSectionType {
    SONG,
    ALBUM,
    PLAYLIST,
    ARTIST,
    CHART,
    MIX,
    TRENDING,
    NEW_RELEASE,
    GENRE,
    FEATURED,
    ORIGINALS,
    PERSONAL_RECO // AI-powered personalized recommendations
}

/**
 * Home feed section containing items of a specific type
 */
data class SaavnHomeSection(
    val title: String,
    val type: SaavnSectionType,
    val items: List<SaavnItem>
)

/**
 * Sealed class representing different types of JioSaavn items
 */
sealed class SaavnItem {
    abstract val id: String
    abstract val title: String
    abstract val image: String
    abstract val subtitle: String?
}

/**
 * JioSaavn Song item
 */
data class SaavnSong(
    override val id: String,
    override val title: String,
    override val image: String,
    override val subtitle: String?,
    val artist: String,
    val album: String?,
    val duration: Int, // in seconds
    val year: String?,
    val language: String?,
    val hasLyrics: Boolean = false,
    val playCount: String? = null
) : SaavnItem()

/**
 * JioSaavn Album item
 */
data class SaavnAlbum(
    override val id: String,
    override val title: String,
    override val image: String,
    override val subtitle: String?,
    val artist: String,
    val songCount: String?,
    val year: String?,
    val language: String?,
    val releaseDate: String? = null
) : SaavnItem()

/**
 * JioSaavn Playlist item
 */
data class SaavnPlaylist(
    override val id: String,
    override val title: String,
    override val image: String,
    override val subtitle: String?,
    val songCount: String?,
    val creator: String? = null,
    val description: String? = null
) : SaavnItem()

/**
 * JioSaavn Artist item
 */
data class SaavnArtist(
    override val id: String,
    override val title: String,
    override val image: String,
    override val subtitle: String?,
    val followerCount: String? = null,
    val fanCount: String? = null,
    val bio: String? = null
) : SaavnItem()

/**
 * JioSaavn Chart entry (ranked item)
 */
data class SaavnChartEntry(
    override val id: String,
    override val title: String,
    override val image: String,
    override val subtitle: String?,
    val rank: Int,
    val change: String? = null, // "up", "down", "new", null
    val item: SaavnItem // The actual item (song, album, etc.)
) : SaavnItem()

/**
 * Extension functions to convert from Map<String, Any> to SaavnItem
 * IMPROVED: More robust field extraction with better fallbacks
 */
fun Map<String, Any>.toSaavnSong(): SaavnSong {
    // Extract ID - try multiple field names (CRITICAL: ID can be empty, we'll still create the item)
    val songId = (this["id"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["songid"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["song_id"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["perma_url"] as? String)?.takeIf { it.isNotEmpty() && it.contains("/") }
            ?.split("/")?.lastOrNull()?.takeIf { it.isNotEmpty() }
        ?: ""
    
    // Extract title - try multiple field names (MUST have title, even if "Unknown")
    val songTitle = (this["title"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["song"] as? String)?.takeIf { it.isNotEmpty() }
        ?: "Unknown Song"
    
    // Extract image - ensure it's not empty
    val songImage = (this["image"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["imageHigh"] as? String)?.takeIf { it.isNotEmpty() }
        ?: ""
    
    // Extract artist
    val songArtist = (this["artist"] as? String)?.takeIf { it.isNotEmpty() && it != "Unknown Artist" }
        ?: (this["subtitle"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["primary_artists"] as? String)?.takeIf { it.isNotEmpty() }
        ?: "Unknown Artist"
    
    // Extract subtitle (usually artist name)
    val songSubtitle = (this["subtitle"] as? String)?.takeIf { it.isNotEmpty() }
        ?: songArtist.takeIf { it != "Unknown Artist" }
    
    // Extract duration
    val duration = when {
        this["durationSeconds"] is Int -> this["durationSeconds"] as Int
        this["durationSeconds"] is Long -> (this["durationSeconds"] as Long).toInt()
        this["duration"] is Int -> this["duration"] as Int
        this["duration"] is Long -> (this["duration"] as Long).toInt()
        this["duration"] is String -> (this["duration"] as String).toIntOrNull() ?: 0
        else -> 0
    }
    
    return SaavnSong(
        id = songId,
        title = songTitle,
        image = songImage,
        subtitle = songSubtitle,
        artist = songArtist,
        album = (this["album"] as? String)?.takeIf { it.isNotEmpty() },
        duration = duration,
        year = (this["year"] as? String)?.takeIf { it.isNotEmpty() },
        language = (this["language"] as? String)?.takeIf { it.isNotEmpty() },
        hasLyrics = (this["hasLyrics"] as? Boolean) ?: false,
        playCount = (this["playCount"] as? String)?.takeIf { it.isNotEmpty() }
    )
}

fun Map<String, Any>.toSaavnAlbum(): SaavnAlbum {
    // Extract ID (can be empty, we'll still create item)
    val albumId = (this["id"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["albumid"] as? String)?.takeIf { it.isNotEmpty() }
        ?: ""
    
    // Extract title (MUST have title, even if "Unknown")
    val albumTitle = (this["title"] as? String)?.takeIf { it.isNotEmpty() }
        ?: "Unknown Album"
    
    // Extract image
    val albumImage = (this["image"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["imageHigh"] as? String)?.takeIf { it.isNotEmpty() }
        ?: ""
    
    // Extract artist
    val albumArtist = (this["artist"] as? String)?.takeIf { it.isNotEmpty() && it != "Unknown Artist" }
        ?: (this["primary_artists"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["subtitle"] as? String)?.takeIf { it.isNotEmpty() }
        ?: "Unknown Artist"
    
    // Extract subtitle
    val albumSubtitle = (this["subtitle"] as? String)?.takeIf { it.isNotEmpty() }
        ?: albumArtist.takeIf { it != "Unknown Artist" }
    
    return SaavnAlbum(
        id = albumId,
        title = albumTitle,
        image = albumImage,
        subtitle = albumSubtitle,
        artist = albumArtist,
        songCount = (this["songCount"] as? String)?.takeIf { it.isNotEmpty() }
            ?: (this["song_count"] as? String)?.takeIf { it.isNotEmpty() },
        year = (this["year"] as? String)?.takeIf { it.isNotEmpty() },
        language = (this["language"] as? String)?.takeIf { it.isNotEmpty() },
        releaseDate = (this["releaseDate"] as? String)?.takeIf { it.isNotEmpty() }
            ?: (this["release_date"] as? String)?.takeIf { it.isNotEmpty() }
    )
}

fun Map<String, Any>.toSaavnPlaylist(): SaavnPlaylist {
    // Extract ID (can be empty)
    val playlistId = (this["id"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["listid"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["playlist_id"] as? String)?.takeIf { it.isNotEmpty() }
        ?: ""
    
    // Extract title (MUST have title)
    val playlistTitle = (this["title"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["name"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["playlist_name"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["subtitle"] as? String)?.takeIf { it.isNotEmpty() }
        ?: "Unknown Playlist"
    
    // Extract image
    val playlistImage = (this["image"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["imageHigh"] as? String)?.takeIf { it.isNotEmpty() }
        ?: ""
    
    // Extract subtitle
    val playlistSubtitle = (this["subtitle"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["creator"] as? String)?.takeIf { it.isNotEmpty() }
    
    return SaavnPlaylist(
        id = playlistId,
        title = playlistTitle,
        image = playlistImage,
        subtitle = playlistSubtitle,
        songCount = (this["songCount"] as? String)?.takeIf { it.isNotEmpty() }
            ?: (this["song_count"] as? String)?.takeIf { it.isNotEmpty() }
            ?: (this["list_count"] as? String)?.takeIf { it.isNotEmpty() },
        creator = (this["creator"] as? String)?.takeIf { it.isNotEmpty() }
            ?: (this["firstname"] as? String)?.takeIf { it.isNotEmpty() },
        description = (this["description"] as? String)?.takeIf { it.isNotEmpty() }
            ?: (this["header_desc"] as? String)?.takeIf { it.isNotEmpty() }
    )
}

/**
 * Helper function to extract artist image from Map with all possible sources
 * Priority: image_url -> image (string) -> image[2] -> image[1] -> image[0] -> images[2] -> images[1] -> images[0]
 */
private fun Map<String, Any>.extractArtistImage(): String {
    // 1. Check "image_url" first
    val imageUrl = (this["image_url"] as? String)?.takeIf { it.isNotEmpty() }
    if (imageUrl != null) return imageUrl
    
    // 2. Check "image" as string
    val imageString = (this["image"] as? String)?.takeIf { it.isNotEmpty() }
    if (imageString != null) return imageString
    
    // 3. Check "imageHigh" as string
    val imageHigh = (this["imageHigh"] as? String)?.takeIf { it.isNotEmpty() }
    if (imageHigh != null) return imageHigh
    
    // 4. Check "image" as array - try indexes [2], [1], [0] in that order
    val imageArray = this["image"] as? List<*>
    if (imageArray != null) {
        // Try index 2 (highest quality usually)
        if (imageArray.size > 2) {
            val img2 = extractImageFromArrayItem(imageArray[2])
            if (img2.isNotEmpty()) return img2
        }
        // Try index 1
        if (imageArray.size > 1) {
            val img1 = extractImageFromArrayItem(imageArray[1])
            if (img1.isNotEmpty()) return img1
        }
        // Try index 0
        if (imageArray.isNotEmpty()) {
            val img0 = extractImageFromArrayItem(imageArray[0])
            if (img0.isNotEmpty()) return img0
        }
    }
    
    // 5. Check "images" array - try indexes [2], [1], [0]
    val imagesArray = this["images"] as? List<*>
    if (imagesArray != null) {
        // Try index 2
        if (imagesArray.size > 2) {
            val img2 = extractImageFromArrayItem(imagesArray[2])
            if (img2.isNotEmpty()) return img2
        }
        // Try index 1
        if (imagesArray.size > 1) {
            val img1 = extractImageFromArrayItem(imagesArray[1])
            if (img1.isNotEmpty()) return img1
        }
        // Try index 0
        if (imagesArray.isNotEmpty()) {
            val img0 = extractImageFromArrayItem(imagesArray[0])
            if (img0.isNotEmpty()) return img0
        }
    }
    
    // 6. Last resort: check if "image" is a Map with "link" or "url"
    val imageMap = this["image"] as? Map<*, *>
    if (imageMap != null) {
        @Suppress("UNCHECKED_CAST")
        val imageMapTyped = imageMap as? Map<String, Any>
        val link = imageMapTyped?.get("link") as? String
        if (link?.isNotEmpty() == true) return link
        val url = imageMapTyped?.get("url") as? String
        if (url?.isNotEmpty() == true) return url
    }
    
    return ""
}

/**
 * Extract image URL from array item (can be String, Map, or JSONObject-like structure)
 */
private fun extractImageFromArrayItem(item: Any?): String {
    if (item == null) return ""
    
    return when (item) {
        is String -> item.takeIf { it.isNotEmpty() } ?: ""
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val map = item as? Map<String, Any>
            (map?.get("link") as? String)?.takeIf { it.isNotEmpty() }
                ?: (map?.get("url") as? String)?.takeIf { it.isNotEmpty() }
                ?: ""
        }
        else -> ""
    }
}

fun Map<String, Any>.toSaavnArtist(): SaavnArtist {
    // Extract ID (can be empty)
    val artistId = (this["id"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["artistid"] as? String)?.takeIf { it.isNotEmpty() }
        ?: ""
    
    // Extract name/title (MUST have name)
    val artistName = (this["name"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["title"] as? String)?.takeIf { it.isNotEmpty() }
        ?: "Unknown Artist"
    
    // Extract image using comprehensive helper function
    val artistImage = this.extractArtistImage()
    
    // Extract subtitle
    val artistSubtitle = (this["subtitle"] as? String)?.takeIf { it.isNotEmpty() }
        ?: (this["role"] as? String)?.takeIf { it.isNotEmpty() }
    
    return SaavnArtist(
        id = artistId,
        title = artistName,
        image = artistImage,
        subtitle = artistSubtitle,
        followerCount = (this["followerCount"] as? String)?.takeIf { it.isNotEmpty() }
            ?: (this["follower_count"] as? String)?.takeIf { it.isNotEmpty() },
        fanCount = (this["fanCount"] as? String)?.takeIf { it.isNotEmpty() }
            ?: (this["fan_count"] as? String)?.takeIf { it.isNotEmpty() },
        bio = (this["bio"] as? String)?.takeIf { it.isNotEmpty() }
    )
}

/**
 * Convert SaavnSong to MediaMetadata for playback
 */
fun SaavnSong.toMediaMetadata(): com.musifly.android.models.MediaMetadata {
    return com.musifly.android.models.MediaMetadata(
        id = "saavn:${id}",
        title = title,
        artists = artist.split(",").map { artistName ->
            com.musifly.android.models.MediaMetadata.Artist(
                id = null,
                name = artistName.trim(),
            )
        },
        duration = duration,
        thumbnailUrl = image.takeIf { it.isNotEmpty() },
        album = album?.takeIf { it.isNotEmpty() }?.let {
            com.musifly.android.models.MediaMetadata.Album(
                id = "",
                title = it,
            )
        },
        explicit = false,
    )
}
