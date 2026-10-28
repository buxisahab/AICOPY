package com.musifly.android.spotify.models

/**
 * SpotifyPlaylist - Represents a Spotify playlist with metadata
 */
data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val externalUrl: String,
    val tracks: List<SpotifyTrack>
)

