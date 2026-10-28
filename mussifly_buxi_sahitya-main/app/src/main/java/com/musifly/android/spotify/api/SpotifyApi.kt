package com.musifly.android.spotify.api

import android.util.Base64
import com.musifly.android.spotify.models.SpotifyPlaylist
import com.musifly.android.spotify.models.SpotifyTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * SpotifyApi - Handles all Spotify API interactions
 * Equivalent to Bloomee's spotify_api.dart
 * 
 * Uses Client Credentials OAuth flow (no user login required)
 */
class SpotifyApi {
    
    companion object {
        private const val CLIENT_ID = "4ede44382bf14ac3ba1d97ad753b233f"
        private const val CLIENT_SECRET = "fb01ad204aff4c12bbcb3ca7ac617990"
        private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
        private const val API_BASE_URL = "https://api.spotify.com/v1"
        
        // Token cache
        private var cachedToken: String? = null
        private var tokenExpiryTime: Long = 0
    }
    
    /**
     * Get access token using Client Credentials flow
     * Equivalent to Bloomee's getAccessTokenCC()
     */
    suspend fun getAccessTokenCC(): String = withContext(Dispatchers.IO) {
        // Check if cached token is still valid
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            return@withContext cachedToken!!
        }
        
        try {
            val url = URL(TOKEN_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            
            // Create Basic Auth header
            val credentials = "$CLIENT_ID:$CLIENT_SECRET"
            val auth = Base64.encodeToString(
                credentials.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
            connection.setRequestProperty("Authorization", "Basic $auth")
            
            // Request body
            val postData = "grant_type=client_credentials"
            connection.doOutput = true
            connection.outputStream.use { it.write(postData.toByteArray(Charsets.UTF_8)) }
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val accessToken = json.getString("access_token")
                val expiresIn = json.getInt("expires_in")
                
                // Cache token (subtract 60 seconds for safety margin)
                cachedToken = accessToken
                tokenExpiryTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expiresIn.toLong()) - 60000
                
                return@withContext accessToken
            } else {
                throw Exception("Failed to get access token: HTTP $responseCode")
            }
        } catch (e: Exception) {
            throw Exception("Error getting Spotify access token: ${e.message}", e)
        }
    }
    
    /**
     * Get playlist metadata and all tracks
     * Equivalent to Bloomee's getAllTracksOfPlaylist()
     */
    suspend fun getAllTracksOfPlaylist(playlistId: String): SpotifyPlaylist = withContext(Dispatchers.IO) {
        val accessToken = getAccessTokenCC()
        
        // First, get playlist metadata
        val playlistUrl = URL("$API_BASE_URL/playlists/$playlistId")
        val playlistConnection = playlistUrl.openConnection() as HttpURLConnection
        playlistConnection.requestMethod = "GET"
        playlistConnection.setRequestProperty("Authorization", "Bearer $accessToken")
        playlistConnection.setRequestProperty("Accept", "application/json")
        
        val playlistResponseCode = playlistConnection.responseCode
        if (playlistResponseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Failed to get playlist info: HTTP $playlistResponseCode")
        }
        
        val playlistJson = JSONObject(
            playlistConnection.inputStream.bufferedReader().use { it.readText() }
        )
        
        val playlistName = playlistJson.getString("name")
        val description = playlistJson.optString("description", null)
        val externalUrl = playlistJson.getJSONObject("external_urls").getString("spotify")
        val images = playlistJson.optJSONArray("images")
        val imageUrl = if (images != null && images.length() > 0) {
            images.getJSONObject(0).getString("url")
        } else null
        
        // Get all tracks in batches
        val allTracks = mutableListOf<SpotifyTrack>()
        var offset = 0
        val limit = 100
        var totalTracks = 0
        
        do {
            val tracksUrl = URL("$API_BASE_URL/playlists/$playlistId/tracks?limit=$limit&offset=$offset")
            val tracksConnection = tracksUrl.openConnection() as HttpURLConnection
            tracksConnection.requestMethod = "GET"
            tracksConnection.setRequestProperty("Authorization", "Bearer $accessToken")
            tracksConnection.setRequestProperty("Accept", "application/json")
            
            val tracksResponseCode = tracksConnection.responseCode
            if (tracksResponseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("Failed to get tracks: HTTP $tracksResponseCode")
            }
            
            val tracksJson = JSONObject(
                tracksConnection.inputStream.bufferedReader().use { it.readText() }
            )
            
            totalTracks = tracksJson.getInt("total")
            val items = tracksJson.getJSONArray("items")
            
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                // Skip null tracks (removed tracks)
                if (!item.isNull("track") && !item.getJSONObject("track").isNull("id")) {
                    val track = SpotifyTrack.fromApiResponse(item.toMap())
                    if (track != null) {
                        allTracks.add(track)
                    }
                }
            }
            
            offset += limit
        } while (offset < totalTracks)
        
        return@withContext SpotifyPlaylist(
            id = playlistId,
            name = playlistName,
            description = description,
            imageUrl = imageUrl,
            externalUrl = externalUrl,
            tracks = allTracks
        )
    }
    
    /**
     * Get track details by track ID
     * Equivalent to Bloomee's getTrackDetails()
     */
    suspend fun getTrackDetails(trackId: String): Map<String, Any> = withContext(Dispatchers.IO) {
        val accessToken = getAccessTokenCC()
        
        val url = URL("$API_BASE_URL/tracks/$trackId")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.setRequestProperty("Accept", "application/json")
        
        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Failed to get track details: HTTP $responseCode")
        }
        
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        return@withContext JSONObject(response).toMap()
    }
    
    /**
     * Helper extension to convert JSONObject to Map
     */
    private fun JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = this.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = this.get(key)
            val convertedValue = when (value) {
                is JSONObject -> value.toMap()
                is JSONArray -> value.toList()
                JSONObject.NULL -> ""
                null -> ""
                else -> value
            }
            map[key] = convertedValue
        }
        return map
    }
    
    /**
     * Helper extension to convert JSONArray to List
     */
    private fun JSONArray.toList(): List<Any> {
        val list = mutableListOf<Any>()
        for (i in 0 until this.length()) {
            val value = this.get(i)
            val convertedValue = when (value) {
                is JSONObject -> value.toMap()
                is JSONArray -> value.toList()
                JSONObject.NULL -> ""
                null -> ""
                else -> value
            }
            list.add(convertedValue)
        }
        return list
    }
}

