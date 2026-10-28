package com.musifly.android.streamingengines.models

/**
 * Source Engine Enum
 */
enum class SourceEngine {
    JIOSAAVN,
    YOUTUBE_MUSIC,
    YOUTUBE_VIDEO,
    SPOTIFY,
    INNERTUBE;
    
    companion object {
        /**
         * Get available engines based on country code
         */
        fun getAvailableEngines(countryCode: String): List<SourceEngine> {
            val saavnCountries = listOf("IN", "NP", "BT", "LK")
            
            return if (saavnCountries.contains(countryCode)) {
                listOf(JIOSAAVN, YOUTUBE_MUSIC, INNERTUBE)
            } else {
                listOf(YOUTUBE_MUSIC, INNERTUBE)
            }
        }
    }
}

/**
 * Streaming Song Model
 * Compatible with JioSaavn API
 */
data class StreamingSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: String = "0",
    val year: String = "",
    val language: String = "",
    val imageUrl: String = "",
    val imageHighUrl: String = "",
    val hasLyrics: Boolean = false,
    val lyrics: String = "",
    val streamUrl: String = "",
    val permaUrl: String = "",
    val releaseDate: String = "",
    val playCount: String = "0",
    val copyright: String = "",
    val source: SourceEngine = SourceEngine.JIOSAAVN
) {
    
    /**
     * Convert to Map for compatibility
     */
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "title" to title,
        "artist" to artist,
        "album" to album,
        "duration" to duration,
        "year" to year,
        "language" to language,
        "image" to imageUrl,
        "imageHigh" to imageHighUrl,
        "hasLyrics" to hasLyrics,
        "lyrics" to lyrics,
        "url" to streamUrl,
        "permaUrl" to permaUrl,
        "releaseDate" to releaseDate,
        "playCount" to playCount,
        "copyright" to copyright,
        "source" to source.name
    )
    
    companion object {
        /**
         * Create from Map (from API response)
         */
        fun fromMap(map: Map<String, Any>): StreamingSong {
            val sourceStr = map["source"] as? String
            val source = try {
                if (sourceStr != null) SourceEngine.valueOf(sourceStr) else SourceEngine.JIOSAAVN
            } catch (e: Exception) {
                SourceEngine.JIOSAAVN
            }
            
            return StreamingSong(
                id = map["id"] as? String ?: "",
                title = map["title"] as? String ?: "Unknown",
                artist = map["artist"] as? String ?: "Unknown",
                album = map["album"] as? String ?: "",
                duration = map["duration"] as? String ?: "0",
                year = map["year"] as? String ?: "",
                language = map["language"] as? String ?: "",
                imageUrl = map["image"] as? String ?: "",
                imageHighUrl = map["imageHigh"] as? String ?: "",
                hasLyrics = map["hasLyrics"] as? Boolean ?: false,
                lyrics = map["lyrics"] as? String ?: "",
                streamUrl = map["url"] as? String ?: "",
                permaUrl = map["permaUrl"] as? String ?: "",
                releaseDate = map["releaseDate"] as? String ?: "",
                playCount = map["playCount"] as? String ?: "0",
                copyright = map["copyright"] as? String ?: "",
                source = source
            )
        }
    }
}

/**
 * Album Model
 */
data class StreamingAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String = "",
    val year: String = "",
    val songCount: Int = 0,
    val language: String = "",
    val releaseDate: String = "",
    val source: SourceEngine = SourceEngine.JIOSAAVN
)

/**
 * Artist Model
 */
data class StreamingArtist(
    val id: String,
    val name: String,
    val imageUrl: String = "",
    val followerCount: String = "0",
    val bio: String = "",
    val fanCount: String = "0",
    val source: SourceEngine = SourceEngine.JIOSAAVN
)

/**
 * Playlist Model
 */
data class StreamingPlaylist(
    val id: String,
    val title: String,
    val imageUrl: String = "",
    val songCount: Int = 0,
    val creator: String = "",
    val description: String = "",
    val source: SourceEngine = SourceEngine.JIOSAAVN
)

/**
 * Search Result Container
 */
data class SearchResult(
    val songs: List<StreamingSong> = emptyList(),
    val albums: List<StreamingAlbum> = emptyList(),
    val artists: List<StreamingArtist> = emptyList(),
    val playlists: List<StreamingPlaylist> = emptyList(),
    val error: String? = null,
    val total: Int = 0
)
