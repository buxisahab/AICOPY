package com.buxi.music.streaming

/**
 * Source Engine Enum
 * Migrated from Musifly
 */
enum class SourceEngine {
    JIOSAAVN,
    YOUTUBE_MUSIC,
    YOUTUBE_VIDEO,
    SPOTIFY,
    INNERTUBE;
    
    companion object {
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
 */
data class StreamingSong(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: String = "0",
    val imageUrl: String = "",
    val imageHighUrl: String = "",
    val streamUrl: String = "",
    val permaUrl: String = "",
    val source: SourceEngine = SourceEngine.JIOSAAVN
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id, "title" to title, "artist" to artist, "album" to album,
        "duration" to duration, "image" to imageUrl, "imageHigh" to imageHighUrl,
        "url" to streamUrl, "permaUrl" to permaUrl, "source" to source.name
    )
    
    companion object {
        fun fromMap(map: Map<String, Any>): StreamingSong {
            val sourceStr = map["source"] as? String
            val source = try {
                if (sourceStr != null) SourceEngine.valueOf(sourceStr) else SourceEngine.JIOSAAVN
            } catch (e: Exception) { SourceEngine.JIOSAAVN }
            
            return StreamingSong(
                id = map["id"] as? String ?: "",
                title = map["title"] as? String ?: "Unknown",
                artist = map["artist"] as? String ?: "Unknown",
                album = map["album"] as? String ?: "",
                duration = map["duration"] as? String ?: "0",
                imageUrl = map["image"] as? String ?: "",
                imageHighUrl = map["imageHigh"] as? String ?: "",
                streamUrl = map["url"] as? String ?: "",
                permaUrl = map["permaUrl"] as? String ?: "",
                source = source
            )
        }
    }
}
