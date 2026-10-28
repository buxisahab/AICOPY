package com.buxi.music.jiosaavn.models

import kotlinx.serialization.Serializable

@Serializable
data class JioSaavnSong(
    val id: String,
    val song: String? = null,
    val title: String? = null, // Sometimes title, sometimes song
    val album: String,
    val year: String? = null,
    val primary_artists: String? = null,
    val singer: String? = null,
    val image: String,
    val albumid: String? = null,
    val language: String? = null,
    val encrypted_media_url: String? = null,
    val duration: String? = null,
    val perma_url: String? = null,
    val explicit_content: String? = null
) {
    val realTitle: String get() = title ?: song ?: "Unknown"
}

@Serializable
data class JioSaavnSearchResponse(
    val total: Int = 0,
    val start: Int = 0,
    val results: List<JioSaavnSong> = emptyList()
)
