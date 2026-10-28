package com.musifly.android.spotify.utils

import java.net.URI

/**
 * SpotifyUrlExtractor - Utility functions to extract IDs from Spotify URLs
 * Equivalent to Bloomee's url_checker.dart functions
 */
object SpotifyUrlExtractor {
    
    /**
     * Extract playlist ID from Spotify URL
     * Supports: open.spotify.com/playlist/{id}
     */
    fun extractPlaylistId(url: String): String? {
        return try {
            val uri = URI(url)
            if (uri.host == "open.spotify.com") {
                val pathParts = uri.path.split("/").filter { it.isNotEmpty() }
                if (pathParts.size >= 2 && pathParts[0] == "playlist") {
                    pathParts[1]
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract track ID from Spotify URL
     */
    fun extractTrackId(url: String): String? {
        return try {
            val uri = URI(url)
            if (uri.host == "open.spotify.com") {
                val pathParts = uri.path.split("/").filter { it.isNotEmpty() }
                if (pathParts.size >= 2 && pathParts[0] == "track") {
                    pathParts[1]
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract album ID from Spotify URL
     */
    fun extractAlbumId(url: String): String? {
        return try {
            val uri = URI(url)
            if (uri.host == "open.spotify.com") {
                val pathParts = uri.path.split("/").filter { it.isNotEmpty() }
                if (pathParts.size >= 2 && pathParts[0] == "album") {
                    pathParts[1]
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if URL is a Spotify URL
     */
    fun isSpotifyUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            uri.host == "open.spotify.com"
        } catch (e: Exception) {
            false
        }
    }
}

