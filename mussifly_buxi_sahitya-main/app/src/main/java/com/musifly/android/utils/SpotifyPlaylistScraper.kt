package com.musifly.android.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Spotify Playlist Scraper
 * Scrapes public Spotify playlist data without requiring authentication
 */
object SpotifyPlaylistScraper {
    
    private const val TAG = "SpotifyPlaylistScraper"
    
    // Desktop browser User-Agent for better compatibility
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    
    /**
     * Spotify track data class
     */
    data class SpotifyTrack(
        val title: String,
        val artist: String,
        val album: String? = null,
        val durationMs: Long = 0
    )
    
    /**
     * Spotify playlist data class
     */
    data class SpotifyPlaylist(
        val id: String,
        val name: String,
        val description: String? = null,
        val tracks: List<SpotifyTrack>,
        val imageUrl: String? = null
    )
    
    /**
     * Extract playlist ID from Spotify URL
     * Supports:
     * - https://open.spotify.com/playlist/{id}
     * - http://open.spotify.com/playlist/{id}
     * - spotify:playlist:{id}
     * - https://open.spotify.com/playlist/{id}?si=xxx
     */
    fun extractPlaylistId(input: String): String? {
        val trimmed = input.trim()
        
        // Pattern 1: spotify:playlist:{id}
        if (trimmed.startsWith("spotify:playlist:")) {
            return trimmed.substringAfter("spotify:playlist:").takeIf { it.isNotBlank() }
        }
        
        // Pattern 2: URL format
        val urlPattern = Pattern.compile(
            "(?:https?://)?(?:open\\.)?spotify\\.com/playlist/([a-zA-Z0-9]+)",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = urlPattern.matcher(trimmed)
        if (matcher.find()) {
            return matcher.group(1)
        }
        
        // Pattern 3: Just the ID (22 alphanumeric characters)
        if (trimmed.matches(Regex("^[a-zA-Z0-9]{22}$"))) {
            return trimmed
        }
        
        return null
    }
    
    /**
     * Validate if the input is a valid Spotify playlist link
     */
    fun isValidSpotifyPlaylistLink(input: String): Boolean {
        return extractPlaylistId(input) != null
    }
    
    /**
     * Fetch playlist data from Spotify
     */
    suspend fun fetchPlaylist(playlistIdOrUrl: String): Result<SpotifyPlaylist> = withContext(Dispatchers.IO) {
        try {
            val playlistId = extractPlaylistId(playlistIdOrUrl)
                ?: return@withContext Result.failure(Exception("Invalid Spotify playlist link"))
            
            Timber.tag(TAG).d("Fetching playlist: $playlistId")
            
            val url = "https://open.spotify.com/playlist/$playlistId"
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to fetch playlist: HTTP ${response.code}")
                )
            }
            
            val html = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response from Spotify"))
            
            // Parse the HTML to extract playlist data
            val playlist = parsePlaylistHtml(html, playlistId)
            
            if (playlist != null) {
                Timber.tag(TAG).d("Successfully parsed playlist: ${playlist.name} with ${playlist.tracks.size} tracks")
                Result.success(playlist)
            } else {
                Result.failure(Exception("Could not parse playlist data. The playlist may be private or the format has changed."))
            }
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error fetching playlist")
            Result.failure(e)
        }
    }
    
    /**
     * Parse playlist HTML to extract data
     * Looks for embedded JSON in script tags
     */
    private fun parsePlaylistHtml(html: String, playlistId: String): SpotifyPlaylist? {
        try {
            // Method 1: Look for Spotify.Entity JSON
            val entityPattern = Pattern.compile(
                "<script[^>]*id=\"initial-state\"[^>]*>([^<]+)</script>",
                Pattern.DOTALL
            )
            var matcher = entityPattern.matcher(html)
            
            if (matcher.find()) {
                val encodedData = matcher.group(1)
                // Spotify encodes this as base64 sometimes
                val jsonData = try {
                    String(android.util.Base64.decode(encodedData, android.util.Base64.DEFAULT))
                } catch (e: Exception) {
                    encodedData
                }
                
                val result = parseSpotifyJson(jsonData, playlistId)
                if (result != null) return result
            }
            
            // Method 2: Look for __NEXT_DATA__ JSON
            val nextDataPattern = Pattern.compile(
                "<script[^>]*id=\"__NEXT_DATA__\"[^>]*>([^<]+)</script>",
                Pattern.DOTALL
            )
            matcher = nextDataPattern.matcher(html)
            
            if (matcher.find()) {
                val jsonData = matcher.group(1) ?: return null
                val result = parseNextDataJson(jsonData, playlistId)
                if (result != null) return result
            }
            
            // Method 3: Look for application/ld+json
            val ldJsonPattern = Pattern.compile(
                "<script[^>]*type=\"application/ld\\+json\"[^>]*>([^<]+)</script>",
                Pattern.DOTALL
            )
            matcher = ldJsonPattern.matcher(html)
            
            while (matcher.find()) {
                val jsonData = matcher.group(1) ?: continue
                val result = parseLdJson(jsonData, playlistId)
                if (result != null) return result
            }
            
            // Method 4: Fallback - extract from meta tags
            return parseFromMetaTags(html, playlistId)
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error parsing playlist HTML")
            return null
        }
    }
    
    /**
     * Parse Spotify's internal JSON format
     */
    private fun parseSpotifyJson(jsonData: String, playlistId: String): SpotifyPlaylist? {
        return try {
            val json = JSONObject(jsonData)
            
            // Navigate to playlist data
            val entities = json.optJSONObject("entities")
            val items = entities?.optJSONObject("items")
            
            // Find playlist entity
            var playlistName = "Spotify Playlist"
            var playlistDescription: String? = null
            var imageUrl: String? = null
            val tracks = mutableListOf<SpotifyTrack>()
            
            items?.keys()?.forEach { key ->
                val item = items.optJSONObject(key)
                if (item != null) {
                    val type = item.optString("type")
                    
                    if (type == "playlist" && key.contains(playlistId)) {
                        playlistName = item.optString("name", playlistName)
                        playlistDescription = item.optString("description")
                        imageUrl = item.optJSONObject("images")?.optJSONArray("items")
                            ?.optJSONObject(0)?.optString("url")
                    }
                    
                    if (type == "track") {
                        val trackName = item.optString("name")
                        val artists = item.optJSONObject("artists")?.optJSONArray("items")
                        val artistName = artists?.optJSONObject(0)?.optString("profile")
                            ?.let { JSONObject(it).optString("name") }
                            ?: artists?.optJSONObject(0)?.optString("name")
                            ?: "Unknown Artist"
                        val albumName = item.optJSONObject("albumOfTrack")?.optString("name")
                        val duration = item.optJSONObject("duration")?.optLong("totalMilliseconds") ?: 0
                        
                        if (trackName.isNotBlank()) {
                            tracks.add(SpotifyTrack(trackName, artistName, albumName, duration))
                        }
                    }
                }
            }
            
            if (tracks.isNotEmpty()) {
                SpotifyPlaylist(playlistId, playlistName, playlistDescription, tracks, imageUrl)
            } else null
            
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to parse Spotify JSON")
            null
        }
    }
    
    /**
     * Parse Next.js __NEXT_DATA__ JSON format
     */
    private fun parseNextDataJson(jsonData: String, playlistId: String): SpotifyPlaylist? {
        return try {
            val json = JSONObject(jsonData)
            val props = json.optJSONObject("props")
            val pageProps = props?.optJSONObject("pageProps")
            
            // Try to find playlist data in various locations
            val playlistData = pageProps?.optJSONObject("playlist")
                ?: pageProps?.optJSONObject("data")?.optJSONObject("playlist")
                ?: return null
            
            val name = playlistData.optString("name", "Spotify Playlist")
            val description = playlistData.optString("description")
            val imageUrl = playlistData.optJSONArray("images")?.optJSONObject(0)?.optString("url")
            
            val tracksData = playlistData.optJSONObject("tracks")?.optJSONArray("items")
                ?: playlistData.optJSONArray("items")
                ?: return null
            
            val tracks = mutableListOf<SpotifyTrack>()
            
            for (i in 0 until tracksData.length()) {
                val item = tracksData.optJSONObject(i)
                val track = item?.optJSONObject("track") ?: item
                
                if (track != null) {
                    val trackName = track.optString("name")
                    val artists = track.optJSONArray("artists")
                    val artistName = artists?.optJSONObject(0)?.optString("name") ?: "Unknown Artist"
                    val albumName = track.optJSONObject("album")?.optString("name")
                    val duration = track.optLong("duration_ms")
                    
                    if (trackName.isNotBlank()) {
                        tracks.add(SpotifyTrack(trackName, artistName, albumName, duration))
                    }
                }
            }
            
            if (tracks.isNotEmpty()) {
                SpotifyPlaylist(playlistId, name, description, tracks, imageUrl)
            } else null
            
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to parse Next.js JSON")
            null
        }
    }
    
    /**
     * Parse application/ld+json format
     */
    private fun parseLdJson(jsonData: String, playlistId: String): SpotifyPlaylist? {
        return try {
            val json = JSONObject(jsonData)
            
            if (json.optString("@type") != "MusicPlaylist") {
                return null
            }
            
            val name = json.optString("name", "Spotify Playlist")
            val description = json.optString("description")
            
            val tracksArray = json.optJSONArray("track") ?: return null
            val tracks = mutableListOf<SpotifyTrack>()
            
            for (i in 0 until tracksArray.length()) {
                val track = tracksArray.optJSONObject(i)
                if (track != null) {
                    val trackName = track.optString("name")
                    val artistObj = track.optJSONObject("byArtist")
                    val artistName = artistObj?.optString("name") ?: "Unknown Artist"
                    val albumObj = track.optJSONObject("inAlbum")
                    val albumName = albumObj?.optString("name")
                    
                    // Parse duration from ISO 8601 format (PT3M30S)
                    val durationStr = track.optString("duration")
                    val durationMs = parseDuration(durationStr)
                    
                    if (trackName.isNotBlank()) {
                        tracks.add(SpotifyTrack(trackName, artistName, albumName, durationMs))
                    }
                }
            }
            
            if (tracks.isNotEmpty()) {
                SpotifyPlaylist(playlistId, name, description, tracks, null)
            } else null
            
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to parse LD+JSON")
            null
        }
    }
    
    /**
     * Fallback: Parse from meta tags
     */
    private fun parseFromMetaTags(html: String, playlistId: String): SpotifyPlaylist? {
        try {
            // Extract title from og:title meta tag
            val titlePattern = Pattern.compile(
                "<meta[^>]*property=\"og:title\"[^>]*content=\"([^\"]+)\"",
                Pattern.CASE_INSENSITIVE
            )
            val titleMatcher = titlePattern.matcher(html)
            val playlistName = if (titleMatcher.find()) {
                titleMatcher.group(1)?.replace(" - playlist by .* \\| Spotify".toRegex(), "")
                    ?: "Spotify Playlist"
            } else "Spotify Playlist"
            
            // Extract image
            val imagePattern = Pattern.compile(
                "<meta[^>]*property=\"og:image\"[^>]*content=\"([^\"]+)\"",
                Pattern.CASE_INSENSITIVE
            )
            val imageMatcher = imagePattern.matcher(html)
            val imageUrl = if (imageMatcher.find()) imageMatcher.group(1) else null
            
            // Try to extract tracks from HTML content
            // This is a basic fallback - may not work for all playlists
            val tracks = extractTracksFromHtml(html)
            
            return if (tracks.isNotEmpty()) {
                SpotifyPlaylist(playlistId, playlistName, null, tracks, imageUrl)
            } else {
                // Return playlist with just name if we can't get tracks
                Timber.tag(TAG).w("Could not extract tracks from HTML")
                null
            }
            
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error parsing meta tags")
            return null
        }
    }
    
    /**
     * Extract tracks from HTML content (basic fallback)
     */
    private fun extractTracksFromHtml(html: String): List<SpotifyTrack> {
        val tracks = mutableListOf<SpotifyTrack>()
        
        try {
            // Look for track patterns in the HTML
            // This pattern looks for common track list structures
            val trackPattern = Pattern.compile(
                "\"name\":\"([^\"]+)\",\"type\":\"track\"[^}]*\"artists\":\\[\\{\"[^\"]*\":\"([^\"]+)\"",
                Pattern.CASE_INSENSITIVE
            )
            val matcher = trackPattern.matcher(html)
            
            val seen = mutableSetOf<String>()
            while (matcher.find() && tracks.size < 100) {
                val trackName = matcher.group(1)
                val artistName = matcher.group(2)
                
                if (trackName != null && artistName != null) {
                    val key = "$trackName-$artistName"
                    if (!seen.contains(key)) {
                        seen.add(key)
                        tracks.add(SpotifyTrack(trackName, artistName))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error extracting tracks from HTML")
        }
        
        return tracks
    }
    
    /**
     * Parse ISO 8601 duration to milliseconds
     */
    private fun parseDuration(duration: String): Long {
        if (duration.isBlank()) return 0
        
        try {
            var totalMs = 0L
            val pattern = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
            val matcher = pattern.matcher(duration)
            
            if (matcher.matches()) {
                val hours = matcher.group(1)?.toLongOrNull() ?: 0
                val minutes = matcher.group(2)?.toLongOrNull() ?: 0
                val seconds = matcher.group(3)?.toLongOrNull() ?: 0
                
                totalMs = (hours * 3600 + minutes * 60 + seconds) * 1000
            }
            
            return totalMs
        } catch (e: Exception) {
            return 0
        }
    }
}
