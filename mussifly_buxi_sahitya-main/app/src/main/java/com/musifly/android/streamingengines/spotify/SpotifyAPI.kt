package com.musifly.android.streamingengines.spotify

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.Base64
import java.nio.charset.StandardCharsets

/**
 * Spotify API Client for Android
 * Converted from Bloomee Music Player (Flutter)
 * 
 * Features:
 * - OAuth authentication (Client Credentials)
 * - Track search
 * - Album details
 * - Playlist details
 * - Featured playlists
 * - Track metadata
 */
class SpotifyAPI {
    
    companion object {
        private const val TAG = "SpotifyAPI"
        private const val CLIENT_ID = "4ede44382bf14ac3ba1d97ad753b233f"
        private const val CLIENT_SECRET = "fb01ad204aff4c12bbcb3ca7ac617990"
        private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
        private const val API_BASE_URL = "https://api.spotify.com/v1"
        
        private var cachedAccessToken: String? = null
        private var tokenExpiryTime: Long = 0
    }
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Get access token using Client Credentials flow
     * Token is cached until expiry
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            // Return cached token if still valid
            if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
                Log.d(TAG, "Using cached access token")
                return@withContext cachedAccessToken
            }
            
            val authString = "$CLIENT_ID:$CLIENT_SECRET"
            val authBytes = authString.toByteArray(StandardCharsets.UTF_8)
            val encodedAuth = Base64.getEncoder().encodeToString(authBytes)
            
            val requestBody = "grant_type=client_credentials".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            
            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(requestBody)
                .addHeader("Authorization", "Basic $encodedAuth")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val jsonObject = JSONObject(body)
                val accessToken = jsonObject.getString("access_token")
                val expiresIn = jsonObject.getInt("expires_in")
                
                // Cache token (expire 60 seconds before actual expiry)
                cachedAccessToken = accessToken
                tokenExpiryTime = System.currentTimeMillis() + ((expiresIn - 60) * 1000L)
                
                Log.d(TAG, "Got new access token, expires in $expiresIn seconds")
                return@withContext accessToken
            } else {
                Log.e(TAG, "Failed to get access token: ${response.code} - ${response.body?.string()}")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting access token: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Search tracks on Spotify
     * @param query Search query
     * @param limit Number of results (default: 20)
     * @return Map with tracks list and total
     */
    suspend fun searchTracks(
        query: String,
        limit: Int = 20
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken()
            if (accessToken == null) {
                return@withContext mapOf<String, Any>(
                    "tracks" to emptyList<Map<String, Any>>(),
                    "error" to "Failed to get access token",
                    "total" to 0
                )
            }
            
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$API_BASE_URL/search?q=$encodedQuery&type=track&limit=$limit"
            
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val jsonObject = JSONObject(body)
                val tracksObject = jsonObject.optJSONObject("tracks")
                
                if (tracksObject != null) {
                    val items = tracksObject.optJSONArray("items") ?: JSONArray()
                    val total = tracksObject.optInt("total", 0)
                    
                    val tracks = formatTracksResponse(items)
                    
                    return@withContext mapOf<String, Any>(
                        "tracks" to tracks,
                        "total" to total,
                        "error" to ""
                    )
                }
            }
            
            return@withContext mapOf<String, Any>(
                "tracks" to emptyList<Map<String, Any>>(),
                "error" to "HTTP Error: ${response.code}",
                "total" to 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "Search tracks failed: ${e.message}", e)
            return@withContext mapOf<String, Any>(
                "tracks" to emptyList<Map<String, Any>>(),
                "error" to (e.message ?: "Unknown error"),
                "total" to 0
            )
        }
    }
    
    /**
     * Get track details by ID
     */
    suspend fun getTrackDetails(trackId: String): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken()
            if (accessToken == null) {
                return@withContext mapOf<String, Any>("error" to "Failed to get access token")
            }
            
            val url = "$API_BASE_URL/tracks/$trackId"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val track = JSONObject(body)
                return@withContext formatTrackDetails(track)
            } else {
                return@withContext mapOf<String, Any>("error" to "HTTP Error: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get track details failed: ${e.message}", e)
            return@withContext mapOf<String, Any>("error" to (e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Get album details with all tracks
     */
    suspend fun getAlbumDetails(albumId: String): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken()
            if (accessToken == null) {
                return@withContext mapOf<String, Any>("error" to "Failed to get access token")
            }
            
            val url = "$API_BASE_URL/albums/$albumId"
            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val album = JSONObject(body)
                return@withContext formatAlbumDetails(album)
            } else {
                return@withContext mapOf<String, Any>("error" to "HTTP Error: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Get album details failed: ${e.message}", e)
            return@withContext mapOf<String, Any>("error" to (e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Get playlist details with all tracks
     */
    suspend fun getPlaylistDetails(playlistId: String): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val accessToken = getAccessToken()
            if (accessToken == null) {
                return@withContext mapOf<String, Any>("error" to "Failed to get access token")
            }
            
            // First get playlist info
            val playlistUrl = "$API_BASE_URL/playlists/$playlistId"
            val playlistRequest = Request.Builder()
                .url(playlistUrl)
                .get()
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Accept", "application/json")
                .build()
            
            val playlistResponse = client.newCall(playlistRequest).execute()
            if (!playlistResponse.isSuccessful) {
                return@withContext mapOf<String, Any>("error" to "HTTP Error: ${playlistResponse.code}")
            }
            
            val playlistBody = playlistResponse.body?.string() ?: ""
            val playlist = JSONObject(playlistBody)
            
            // Get all tracks
            val tracks = mutableListOf<Map<String, Any>>()
            var offset = 0
            val limit = 100
            var total = 0
            
            do {
                val tracksUrl = "$API_BASE_URL/playlists/$playlistId/tracks?limit=$limit&offset=$offset"
                val tracksRequest = Request.Builder()
                    .url(tracksUrl)
                    .get()
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Accept", "application/json")
                    .build()
                
                val tracksResponse = client.newCall(tracksRequest).execute()
                if (tracksResponse.isSuccessful) {
                    val tracksBody = tracksResponse.body?.string() ?: ""
                    val tracksJson = JSONObject(tracksBody)
                    total = tracksJson.optInt("total", 0)
                    val items = tracksJson.optJSONArray("items") ?: JSONArray()
                    
                    for (i in 0 until items.length()) {
                        val item = items.getJSONObject(i)
                        val track = item.optJSONObject("track")
                        if (track != null) {
                            tracks.add(formatTrackDetails(track))
                        }
                    }
                    
                    offset += limit
                } else {
                    break
                }
            } while (offset < total)
            
            val images = playlist.optJSONArray("images")
            val imageUrl = if (images != null && images.length() > 0) {
                images.getJSONObject(0).optString("url", "")
            } else {
                ""
            }
            
            return@withContext mapOf<String, Any>(
                "id" to playlistId,
                "title" to playlist.optString("name", "Unknown Playlist"),
                "image" to imageUrl,
                "description" to playlist.optString("description", ""),
                "songs" to tracks,
                "total" to tracks.size,
                "url" to (playlist.optJSONObject("external_urls")?.optString("spotify") ?: "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Get playlist details failed: ${e.message}", e)
            return@withContext mapOf<String, Any>("error" to (e.message ?: "Unknown error"))
        }
    }
    
    // ============ FORMATTING HELPERS ============
    
    private fun formatTracksResponse(tracksArray: JSONArray): List<Map<String, Any>> {
        val tracks = mutableListOf<Map<String, Any>>()
        
        for (i in 0 until tracksArray.length()) {
            try {
                val track = tracksArray.getJSONObject(i)
                tracks.add(formatTrackDetails(track))
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting track $i: ${e.message}")
            }
        }
        
        return tracks
    }
    
    private fun formatTrackDetails(track: JSONObject): Map<String, Any> {
        // Extract artists
        val artists = track.optJSONArray("artists") ?: JSONArray()
        val artistNames = mutableListOf<String>()
        for (j in 0 until artists.length()) {
            val artist = artists.getJSONObject(j)
            artistNames.add(artist.optString("name", ""))
        }
        val artistString = artistNames.joinToString(", ")
        
        // Extract album
        val album = track.optJSONObject("album")
        val albumName = album?.optString("name") ?: ""
        val albumImages = album?.optJSONArray("images")
        val imageUrl = if (albumImages != null && albumImages.length() > 0) {
            albumImages.getJSONObject(0).optString("url", "")
        } else {
            ""
        }
        
        // Duration in milliseconds, convert to seconds
        val durationMs = track.optInt("duration_ms", 0)
        val durationSeconds = durationMs / 1000
        
        return mapOf<String, Any>(
            "id" to track.optString("id", ""),
            "title" to track.optString("name", "Unknown"),
            "artist" to artistString,
            "album" to albumName,
            "duration" to durationSeconds.toString(),
            "image" to imageUrl,
            "imageHigh" to imageUrl,
            "permaUrl" to (track.optJSONObject("external_urls")?.optString("spotify") ?: ""),
            "releaseDate" to (album?.optString("release_date") ?: ""),
            "year" to (album?.optString("release_date")?.take(4) ?: ""),
            "hasLyrics" to false,
            "language" to "",
            "url" to "", // Spotify doesn't provide direct stream URLs
            "playCount" to "0",
            "copyright" to ""
        )
    }
    
    private fun formatAlbumDetails(album: JSONObject): Map<String, Any> {
        val tracksArray = album.optJSONObject("tracks")?.optJSONArray("items") ?: JSONArray()
        val artists = album.optJSONArray("artists") ?: JSONArray()
        val artistNames = mutableListOf<String>()
        for (j in 0 until artists.length()) {
            val artist = artists.getJSONObject(j)
            artistNames.add(artist.optString("name", ""))
        }
        val artistString = artistNames.joinToString(", ")
        
        val images = album.optJSONArray("images")
        val imageUrl = if (images != null && images.length() > 0) {
            images.getJSONObject(0).optString("url", "")
        } else {
            ""
        }
        
        val tracks = mutableListOf<Map<String, Any>>()
        for (i in 0 until tracksArray.length()) {
            try {
                val track = tracksArray.getJSONObject(i)
                // Create a simplified track object
                val trackArtists = track.optJSONArray("artists") ?: JSONArray()
                val trackArtistNames = mutableListOf<String>()
                for (j in 0 until trackArtists.length()) {
                    val artist = trackArtists.getJSONObject(j)
                    trackArtistNames.add(artist.optString("name", ""))
                }
                
                tracks.add(mapOf<String, Any>(
                    "id" to track.optString("id", ""),
                    "title" to track.optString("name", "Unknown"),
                    "artist" to trackArtistNames.joinToString(", "),
                    "album" to album.optString("name", ""),
                    "duration" to (track.optInt("duration_ms", 0) / 1000),
                    "image" to imageUrl,
                    "imageHigh" to imageUrl,
                    "permaUrl" to (track.optJSONObject("external_urls")?.optString("spotify") ?: "")
                ))
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting album track $i: ${e.message}")
            }
        }
        
        return mapOf<String, Any>(
            "id" to album.optString("id", ""),
            "title" to album.optString("name", "Unknown Album"),
            "artist" to artistString,
            "image" to imageUrl,
            "year" to (album.optString("release_date", "").take(4)),
            "songCount" to tracks.size.toString(),
            "songs" to tracks,
            "language" to "",
            "releaseDate" to album.optString("release_date", "")
        )
    }
}
