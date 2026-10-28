package com.musifly.android.models

import androidx.compose.runtime.Immutable
import com.musifly.android.db.entities.Album
import com.musifly.android.db.entities.Artist
import com.musifly.android.db.entities.Playlist
import com.musifly.android.db.entities.Song

/**
 * Recommendation section types for Home Screen
 */
enum class RecommendationSectionType {
    DAILY_MIX,
    TRENDING,
    BECAUSE_YOU_LIKE,
    RECENTLY_PLAYED,
    MOOD_MIX,
    TOP_CHARTS,
    RECOMMENDED_PLAYLISTS,
    RECOMMENDED_ARTISTS
}

/**
 * Mood types for mood-based recommendations
 */
enum class MoodType {
    CHILL,
    ROMANTIC,
    WORKOUT,
    PARTY,
    FOCUS,
    SLEEP
}

/**
 * Recommendation section data model
 */
@Immutable
data class RecommendationSection(
    val type: RecommendationSectionType,
    val title: String,
    val subtitle: String? = null,
    val items: List<RecommendationItem>,
    val shuffleEnabled: Boolean = false
)

/**
 * Unified recommendation item (can be Song, Playlist, Artist, or Album)
 */
@Immutable
sealed class RecommendationItem {
    abstract val id: String
    abstract val title: String
    abstract val thumbnailUrl: String?
    abstract val subtitle: String?
    
    data class SongItem(
        override val id: String,
        override val title: String,
        override val thumbnailUrl: String?,
        override val subtitle: String?,
        val duration: Int = -1,
        val artists: List<String> = emptyList()
    ) : RecommendationItem()
    
    data class PlaylistItem(
        override val id: String,
        override val title: String,
        override val thumbnailUrl: String?,
        override val subtitle: String?,
        val itemCount: Int? = null,
        val type: String = "playlist" // "playlist" or "album"
    ) : RecommendationItem()
    
    data class ArtistItem(
        override val id: String,
        override val title: String,
        override val thumbnailUrl: String?,
        override val subtitle: String? = null
    ) : RecommendationItem()
}

/**
 * Listening pattern analysis result
 */
@Immutable
data class ListeningPattern(
    val topArtists: List<Pair<Artist, Int>>, // Artist and play count
    val topGenres: List<Pair<String, Int>>, // Genre and play count
    val topSongs: List<Pair<Song, Long>>, // Song and total play time
    val recentSongs: List<Song>, // Last 10 songs played
    val dominantMood: MoodType? = null,
    val tempoPattern: String? = null // "fast", "slow", "mixed"
)

/**
 * Helper functions to convert database entities to RecommendationItem
 */
fun Song.toRecommendationItem(): RecommendationItem.SongItem {
    return RecommendationItem.SongItem(
        id = id,
        title = title,
        thumbnailUrl = thumbnailUrl,
        subtitle = album?.title ?: song.albumName ?: "Unknown Album",
        duration = song.duration,
        artists = artists.map { it.name } // Use artists from relation
    )
}

fun Playlist.toRecommendationItem(): RecommendationItem.PlaylistItem {
    return RecommendationItem.PlaylistItem(
        id = id,
        title = title,
        thumbnailUrl = thumbnailUrl,
        subtitle = "Playlist",
        itemCount = songCount,
        type = "playlist"
    )
}

fun Album.toRecommendationItem(): RecommendationItem.PlaylistItem {
    return RecommendationItem.PlaylistItem(
        id = id,
        title = title,
        thumbnailUrl = thumbnailUrl,
        subtitle = "Album",
        itemCount = null,
        type = "album"
    )
}

fun Artist.toRecommendationItem(): RecommendationItem.ArtistItem {
    return RecommendationItem.ArtistItem(
        id = id,
        title = title, // Artist.title already returns artist.name
        thumbnailUrl = thumbnailUrl,
        subtitle = null
    )
}
