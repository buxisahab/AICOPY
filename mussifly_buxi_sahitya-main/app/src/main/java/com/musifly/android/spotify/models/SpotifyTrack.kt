package com.musifly.android.spotify.models

/**
 * SpotifyTrack - Represents a track from Spotify API
 */
data class SpotifyTrack(
    val title: String,
    val artists: List<String>,
    val query: String // Formatted as "$title $artists"
) {
    companion object {
        fun fromApiResponse(track: Map<String, Any>): SpotifyTrack? {
            try {
                val trackData = track["track"] as? Map<String, Any> ?: return null
                val title = (trackData["name"] as? String) ?: return null
                
                val artistsList = (trackData["artists"] as? List<*>)?.mapNotNull { artist ->
                    (artist as? Map<*, *>)?.get("name") as? String
                } ?: emptyList()
                
                val artistsString = artistsList.joinToString(", ")
                val query = "$title $artistsString".trim()
                
                return SpotifyTrack(
                    title = title,
                    artists = artistsList,
                    query = query
                )
            } catch (e: Exception) {
                return null
            }
        }
    }
}

