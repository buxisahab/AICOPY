package com.musifly.android.spotify.models

/**
 * ImportState - Represents the state of a Spotify playlist import operation
 * Equivalent to Bloomee's ImporterState class
 */
data class ImportState(
    val totalItems: Int = 0,
    val importedItems: Int = 0,
    val failedItems: Int = 0,
    val isDone: Boolean = false,
    val isFailed: Boolean = false,
    val message: String = "",
    val playlistId: String? = null // ID of the created playlist
)

