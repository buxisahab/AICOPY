package com.musifly.android.utils

import android.util.Log
import java.net.URI

/**
 * URL Checker utility for extracting IDs from various music platform URLs
 * Converted from Bloomee Music Player (Flutter)
 */
object UrlChecker {
    private const val TAG = "UrlChecker"
    
    /**
     * Check if URL is a Spotify URL and return type
     */
    fun isSpotifyUrl(url: String): Pair<Boolean, String> {
        return try {
            val uri = URI(url)
            if (uri.host != "open.spotify.com") {
                return false to ""
            }
            
            val pathParts = uri.path.split("/").filter { it.isNotEmpty() }
            if (pathParts.isEmpty()) {
                return false to ""
            }
            
            val type = when (pathParts[0]) {
                "track" -> "track"
                "playlist" -> "playlist"
                "album" -> "album"
                else -> ""
            }
            
            true to type
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Spotify URL: ${e.message}")
            false to ""
        }
    }
    
    /**
     * Check if URL is a YouTube link
     */
    fun isYoutubeLink(link: String): Boolean {
        return try {
            val uri = URI(link)
            val host = uri.host?.lowercase() ?: ""
            host.contains("youtube.com") || host.contains("youtu.be")
        } catch (e: Exception) {
            // Fallback to string check
            link.contains("youtube.com", ignoreCase = true) || link.contains("youtu.be", ignoreCase = true)
        }
    }
    
    /**
     * Extract video ID from YouTube URL
     */
    fun extractVideoId(url: String): String? {
        return try {
            val uri = URI(url)
            when {
                uri.host == "youtube.com" || uri.host == "www.youtube.com" -> {
                    if (uri.path.contains("/watch")) {
                        uri.query?.split("&")?.find { it.startsWith("v=") }?.substring(2)
                    } else null
                }
                uri.host == "youtu.be" -> {
                    uri.path.split("/").filter { it.isNotEmpty() }.firstOrNull()
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting video ID: ${e.message}")
            null
        }
    }
    
    /**
     * Extract playlist ID from YouTube URL
     */
    fun extractYoutubePlaylistId(url: String): String? {
        return try {
            Log.d(TAG, "Extracting playlist ID from: $url")
            val uri = URI(url)
            val host = uri.host?.lowercase() ?: ""
            val query = uri.query ?: ""
            val path = uri.path ?: ""
            
            Log.d(TAG, "Host: $host, Query: $query, Path: $path")
            
            when {
                host.contains("youtube.com") && !host.contains("music") -> {
                    // Try query parameter first (most common: list=xxx)
                    val listParam = query.split("&").find { it.trim().startsWith("list=") }
                    if (listParam != null) {
                        val playlistId = listParam.substring(5).trim()
                        if (playlistId.isNotEmpty()) {
                            Log.d(TAG, "Found playlist ID in query: $playlistId")
                            return playlistId
                        }
                    }
                    
                    // Try path-based (e.g., /playlist?list=xxx - but this is actually query)
                    // Or /playlist/PLxxxx format (less common)
                    if (path.contains("/playlist")) {
                        val pathParts = path.split("/").filter { it.isNotEmpty() }
                        val playlistIndex = pathParts.indexOf("playlist")
                        if (playlistIndex >= 0 && playlistIndex < pathParts.size - 1) {
                            val playlistId = pathParts[playlistIndex + 1]
                            if (playlistId.isNotEmpty()) {
                                Log.d(TAG, "Found playlist ID in path: $playlistId")
                                return playlistId
                            }
                        }
                    }
                    
                    Log.w(TAG, "No playlist ID found in YouTube URL")
                    null
                }
                host.contains("youtu.be") -> {
                    // For youtu.be, playlist ID is in query parameter
                    val listParam = query.split("&").find { it.trim().startsWith("list=") }
                    if (listParam != null) {
                        val playlistId = listParam.substring(5).trim()
                        if (playlistId.isNotEmpty()) {
                            Log.d(TAG, "Found playlist ID in youtu.be query: $playlistId")
                            return playlistId
                        }
                    }
                    null
                }
                else -> {
                    Log.w(TAG, "Unknown YouTube host: $host")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting YouTube playlist ID: ${e.message}", e)
            // Fallback: try string parsing
            try {
                val listIndex = url.indexOf("list=")
                if (listIndex != -1) {
                    val afterList = url.substring(listIndex + 5)
                    val endIndex = afterList.indexOf("&").takeIf { it > 0 } ?: afterList.length
                    val playlistId = afterList.substring(0, endIndex).trim()
                    if (playlistId.isNotEmpty()) {
                        Log.d(TAG, "Found playlist ID via fallback: $playlistId")
                        return playlistId
                    }
                }
                null
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback extraction failed: ${e2.message}")
                null
            }
        }
    }
    
    /**
     * Extract video ID from YouTube Music URL
     */
    fun extractYTMusicId(url: String): String? {
        return try {
            val uri = URI(url)
            if (uri.host == "music.youtube.com") {
                if (uri.path.contains("/watch")) {
                    uri.query?.split("&")?.find { it.startsWith("v=") }?.substring(2)
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting YouTube Music ID: ${e.message}")
            null
        }
    }
    
    /**
     * Extract playlist ID from YouTube Music URL
     */
    fun extractYTMusicPlaylistId(url: String): String? {
        return try {
            val uri = URI(url)
            if (uri.host == "music.youtube.com") {
                uri.query?.split("&")?.find { it.startsWith("list=") }?.substring(5)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting YouTube Music playlist ID: ${e.message}")
            null
        }
    }
    
    /**
     * Extract playlist ID from Spotify URL
     */
    fun extractSpotifyPlaylistId(url: String): String? {
        return try {
            val uri = URI(url)
            if (uri.host == "open.spotify.com") {
                val pathParts = uri.path.split("/").filter { it.isNotEmpty() }
                if (pathParts.size >= 2 && pathParts[0] == "playlist") {
                    return pathParts[1]
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting Spotify playlist ID: ${e.message}")
            null
        }
    }
    
    /**
     * Extract album ID from Spotify URL
     */
    fun extractSpotifyAlbumId(url: String): String? {
        return try {
            val uri = URI(url)
            if (uri.host == "open.spotify.com") {
                val pathParts = uri.path.split("/").filter { it.isNotEmpty() }
                if (pathParts.size >= 2 && pathParts[0] == "album") {
                    return pathParts[1]
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting Spotify album ID: ${e.message}")
            null
        }
    }
    
    /**
     * Extract track ID from Spotify URL
     */
    fun extractSpotifyTrackId(url: String): String? {
        return try {
            val uri = URI(url)
            if (uri.host == "open.spotify.com") {
                val pathParts = uri.path.split("/").filter { it.isNotEmpty() }
                if (pathParts.size >= 2 && pathParts[0] == "track") {
                    return pathParts[1]
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting Spotify track ID: ${e.message}")
            null
        }
    }
    
    /**
     * Check if string is a valid URL
     */
    fun isUrl(url: String): Boolean {
        return try {
            URI(url)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * URL types enum
     */
    enum class UrlType {
        YOUTUBE_VIDEO,
        YOUTUBE_PLAYLIST,
        YOUTUBE_MUSIC_VIDEO,
        YOUTUBE_MUSIC_PLAYLIST,
        SPOTIFY_TRACK,
        SPOTIFY_PLAYLIST,
        SPOTIFY_ALBUM,
        OTHER
    }
    
    /**
     * Get URL type from URL string
     */
    fun getUrlType(url: String): UrlType {
        if (!isUrl(url)) {
            return UrlType.OTHER
        }
        
        if (isYoutubeLink(url)) {
            return try {
                val uri = URI(url)
                
                // Check for playlist ID in query parameters (list=)
                val hasPlaylistId = uri.query?.split("&")?.any { it.startsWith("list=") && it.length > 5 } == true
                
                // Check if path contains playlist
                val hasPlaylistPath = uri.path.contains("/playlist")
                
                if (url.contains("music.youtube.com")) {
                    // YouTube Music
                    if (hasPlaylistId || hasPlaylistPath) {
                        UrlType.YOUTUBE_MUSIC_PLAYLIST
                    } else {
                        UrlType.YOUTUBE_MUSIC_VIDEO
                    }
                } else {
                    // Regular YouTube
                    if (hasPlaylistId || hasPlaylistPath) {
                        UrlType.YOUTUBE_PLAYLIST
                    } else {
                        UrlType.YOUTUBE_VIDEO
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing YouTube URL: ${e.message}")
                // Fallback to simple string check
                if (url.contains("playlist") || url.contains("list=")) {
                    if (url.contains("music.youtube.com")) {
                        UrlType.YOUTUBE_MUSIC_PLAYLIST
                    } else {
                        UrlType.YOUTUBE_PLAYLIST
                    }
                } else if (url.contains("music.youtube.com")) {
                    UrlType.YOUTUBE_MUSIC_VIDEO
                } else {
                    UrlType.YOUTUBE_VIDEO
                }
            }
        }
        
        val (isSpotify, type) = isSpotifyUrl(url)
        if (isSpotify) {
            return when (type) {
                "playlist" -> UrlType.SPOTIFY_PLAYLIST
                "track" -> UrlType.SPOTIFY_TRACK
                "album" -> UrlType.SPOTIFY_ALBUM
                else -> UrlType.OTHER
            }
        }
        
        return UrlType.OTHER
    }
}
