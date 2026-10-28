package com.musifly.android.streamingengines.saavn

import android.util.Base64
import android.util.Log
import com.musifly.android.models.SaavnAlbum
import com.musifly.android.models.SaavnArtist
import com.musifly.android.models.SaavnChartEntry
import com.musifly.android.models.SaavnHomeSection
import com.musifly.android.models.SaavnPlaylist
import com.musifly.android.models.SaavnSectionType
import com.musifly.android.models.SaavnSong
import com.musifly.android.models.toSaavnAlbum
import com.musifly.android.models.toSaavnArtist
import com.musifly.android.models.toSaavnPlaylist
import com.musifly.android.models.toSaavnSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec

/**
 * JioSaavn API Client for Android
 * Converted from Bloomee Music Player (Flutter)
 * 
 * Features:
 * - Song search
 * - Album details  
 * - Playlist details
 * - Artist details
 * - High quality 320kbps streams
 * - Lyrics support
 * - Recommendations
 */
class SaavnAPI {
    
    companion object {
        private const val TAG = "SaavnAPI"
        private const val BASE_URL = "www.jiosaavn.com"
        private const val API_STR = "/api.php?_format=json&_marker=0&api_version=4&ctx=web6dot0"
        
        // Endpoints
        private const val EP_FROM_TOKEN = "__call=webapi.get"
        private const val EP_SONG_DETAILS = "__call=song.getDetails"
        private const val EP_GET_RECO = "__call=reco.getreco"
    }
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val headers = mapOf(
        "cookie" to "L=Hindi",
        "Accept" to "application/json, text/plain, */*",
        "Accept-Language" to "en-US,en;q=0.9,hi;q=0.8",
        "Referer" to "https://www.jiosaavn.com/",
        "Origin" to "https://www.jiosaavn.com",
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
    )
    
    /**
     * Generic GET request handler
     */
    private suspend fun getResponse(params: String, useV4: Boolean = false, customContext: String? = null): Result<String> = 
        withContext(Dispatchers.IO) {
            try {
                val finalParams = if (!useV4) {
                    params.replace("&api_version=4", "")
                } else params
                
                // Use custom context if provided, otherwise use default API_STR
                val apiStr = if (customContext != null) {
                    "/api.php?_format=json&_marker=0&api_version=4&ctx=$customContext"
                } else {
                    API_STR
                }
                
                val url = "https://$BASE_URL$apiStr&$finalParams"
                
                val request = Request.Builder()
                    .url(url)
                    .apply {
                        headers.forEach { (key, value) ->
                            addHeader(key, value)
                        }
                    }
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    Result.success(response.body?.string() ?: "")
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Log.w(TAG, "⚠️ API request failed for endpoint '$params': HTTP Error: ${response.code}")
                    if (errorBody.isNotEmpty() && errorBody.length < 500) {
                        Log.d(TAG, "   Error response body: $errorBody")
                    }
                    Result.failure(Exception("HTTP Error: ${response.code}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Request failed", e)
                Result.failure(e)
            }
        }
    
    /**
     * Search songs by query
     * @param searchQuery Search query
     * @param count Number of results (default: 20)
     * @return Map with songs list, error, and total
     */
    suspend fun fetchSearchResults(
        searchQuery: String,
        count: Int = 20
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            val apiUrl = "p=1&q=$encodedQuery&n=$count&__call=search.getResults"
            
            val result = getResponse(apiUrl, useV4 = true, customContext = "wap6dot0")
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val results = jsonObject.optJSONArray("results") ?: JSONArray()
                    
                    val songs = formatSongsResponse(results)
                    
                    mapOf(
                        "songs" to songs,
                        "error" to "",
                        "total" to songs.size
                    )
                },
                onFailure = { e ->
                    mapOf(
                        "songs" to emptyList<Map<String, Any>>(),
                        "error" to (e.message ?: "HTTP Error"),
                        "total" to 0
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Search failed: ${e.message}", e)
            mapOf(
                "songs" to emptyList<Map<String, Any>>(),
                "error" to (e.message ?: "Unknown error"),
                "total" to 0
            )
        }
    }

    /**
     * Search albums by query
     */
    suspend fun fetchAlbumsSearchResults(
        searchQuery: String,
        count: Int = 20
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            val apiUrl = "p=1&q=$encodedQuery&n=$count&__call=search.getAlbumResults"
            
            val result = getResponse(apiUrl, useV4 = true, customContext = "wap6dot0")
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val results = jsonObject.optJSONArray("results") ?: JSONArray()
                    val albums = formatAlbumsArray(results)
                    mapOf("albums" to albums, "error" to "", "total" to albums.size)
                },
                onFailure = { e ->
                    mapOf("albums" to emptyList<Map<String, Any>>(), "error" to (e.message ?: "HTTP Error"), "total" to 0)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Album search failed: ${e.message}", e)
            mapOf("albums" to emptyList<Map<String, Any>>(), "error" to (e.message ?: "Unknown error"), "total" to 0)
        }
    }

    /**
     * Search artists by query
     */
    suspend fun fetchArtistsSearchResults(
        searchQuery: String,
        count: Int = 20
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            val apiUrl = "p=1&q=$encodedQuery&n=$count&__call=search.getArtistResults"
            
            val result = getResponse(apiUrl, useV4 = true, customContext = "wap6dot0")
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val results = jsonObject.optJSONArray("results") ?: JSONArray()
                    val artists = formatArtistsArray(results)
                    mapOf("artists" to artists, "error" to "", "total" to artists.size)
                },
                onFailure = { e ->
                    mapOf("artists" to emptyList<Map<String, Any>>(), "error" to (e.message ?: "HTTP Error"), "total" to 0)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Artist search failed: ${e.message}", e)
            mapOf("artists" to emptyList<Map<String, Any>>(), "error" to (e.message ?: "Unknown error"), "total" to 0)
        }
    }

    /**
     * Search playlists by query
     */
    suspend fun fetchPlaylistsSearchResults(
        searchQuery: String,
        count: Int = 20
    ): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            val apiUrl = "p=1&q=$encodedQuery&n=$count&__call=search.getPlaylistResults"
            
            val result = getResponse(apiUrl, useV4 = true, customContext = "wap6dot0")
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val results = jsonObject.optJSONArray("results") ?: JSONArray()
                    val playlists = formatPlaylistsArray(results)
                    mapOf("playlists" to playlists, "error" to "", "total" to playlists.size)
                },
                onFailure = { e ->
                    mapOf("playlists" to emptyList<Map<String, Any>>(), "error" to (e.message ?: "HTTP Error"), "total" to 0)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Playlist search failed: ${e.message}", e)
            mapOf("playlists" to emptyList<Map<String, Any>>(), "error" to (e.message ?: "Unknown error"), "total" to 0)
        }
    }

    /**
     * Global search suggestions
     */
    suspend fun getSearchSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val apiUrl = "query=$encodedQuery&__call=autocomplete.get"
            
            val result = getResponse(apiUrl, useV4 = true, customContext = "wap6dot0")
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val suggestions = mutableListOf<String>()
                    
                    // Add top query if exists
                    val topQuery = jsonObject.optJSONObject("topquery")?.optJSONArray("data")
                    if (topQuery != null && topQuery.length() > 0) {
                        for (i in 0 until topQuery.length()) {
                            suggestions.add(topQuery.getJSONObject(i).optString("title"))
                        }
                    }
                    
                    // Add songs if exists
                    val songs = jsonObject.optJSONObject("songs")?.optJSONArray("data")
                    if (songs != null && songs.length() > 0) {
                        for (i in 0 until songs.length()) {
                            suggestions.add(songs.getJSONObject(i).optString("title"))
                        }
                    }
                    
                    suggestions.filter { it.isNotEmpty() }.distinct()
                },
                onFailure = { emptyList() }
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Global search results (Summary)
     */
    suspend fun fetchGlobalSearchResults(query: String): Map<String, List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        coroutineScope {
            val songs = async { fetchSearchResults(query, 10) }
            val albums = async { fetchAlbumsSearchResults(query, 10) }
            val artists = async { fetchArtistsSearchResults(query, 10) }
            val playlists = async { fetchPlaylistsSearchResults(query, 10) }
            
            mapOf(
                "songs" to (songs.await()["songs"] as? List<Map<String, Any>> ?: emptyList()),
                "albums" to (albums.await()["albums"] as? List<Map<String, Any>> ?: emptyList()),
                "artists" to (artists.await()["artists"] as? List<Map<String, Any>> ?: emptyList()),
                "playlists" to (playlists.await()["playlists"] as? List<Map<String, Any>> ?: emptyList())
            )
        }
    }
    
    /**
     * Get song details by ID
     */
    suspend fun getSongDetails(songId: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        try {
            val params = "${EP_SONG_DETAILS}&pids=$songId"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val songs = jsonObject.optJSONArray("songs")
                    
                    if (songs != null && songs.length() > 0) {
                        val song = songs.getJSONObject(0)
                        formatSongDetails(song)
                    } else {
                        mapOf("error" to "Song not found")
                    }
                },
                onFailure = { e ->
                    mapOf("error" to (e.message ?: "Unknown error"))
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Get song details failed: ${e.message}", e)
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Get high quality stream URL (320kbps)
     * @param songId JioSaavn song ID
     * @return Stream URL or null
     */
    suspend fun getSongUrl(songId: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎵 Fetching stream URL for song ID: $songId")
            
            // Try multiple endpoints - updated to avoid 403 errors
            // Note: JioSaavn API may require specific parameter formats
            data class EndpointConfig(val params: String, val useV4: Boolean, val context: String? = null)
            
            val endpoints = listOf(
                // Primary: Use song.getDetails with pids (plural) - most common format
                EndpointConfig("${EP_SONG_DETAILS}&pids=$songId", true),  // API v4 with pids (plural)
                // Alternative 1: Use pid (singular)
                EndpointConfig("${EP_SONG_DETAILS}&pid=$songId", true),  // API v4 with pid (singular)
                // Alternative 2: Use webapi.get with token format
                EndpointConfig("__call=webapi.get&token=$songId&type=song", true),  // webapi.get format
                // Alternative 3: Try without api_version (v3 format)
                EndpointConfig("${EP_SONG_DETAILS}&pids=$songId", false),  // Without v4
                // Alternative 4: Use different context (wap instead of web)
                EndpointConfig("${EP_SONG_DETAILS}&pids=$songId", true, "wap6dot0"),  // wap context
                // Alternative 5: Try with mobile context
                EndpointConfig("${EP_SONG_DETAILS}&pids=$songId", true, "mobile6dot0"),  // mobile context
                // Last resort: Direct token format
                EndpointConfig("token=$songId&type=song&$EP_FROM_TOKEN", false),  // Alternative endpoint
            )
            
            for (endpoint in endpoints) {
                Log.d(TAG, "🔍 Trying endpoint: ${endpoint.params.take(60)}... (useV4=${endpoint.useV4}, ctx=${endpoint.context ?: "default"})")
                val result = getResponse(endpoint.params, useV4 = endpoint.useV4, customContext = endpoint.context)
                
                val url = result.fold(
                    onSuccess = { body ->
                        if (body.isEmpty()) {
                            Log.w(TAG, "⚠️ Empty response body, trying next endpoint...")
                            return@fold null
                        }
                        Log.d(TAG, "📦 Stream URL API response received, length: ${body.length}")
                        if (body.isEmpty()) {
                            Log.w(TAG, "⚠️ Empty response body, trying next endpoint...")
                            return@fold null
                        }
                        
                        try {
                            val jsonObject = JSONObject(body)
                            
                            // Check if response has error
                            if (jsonObject.has("error")) {
                                val errorMsg = jsonObject.optString("error", "Unknown error")
                                Log.w(TAG, "⚠️ API returned error: $errorMsg, trying next endpoint...")
                                return@fold null
                            }
                            
                            // Log available keys for debugging
                            val keys = jsonObject.keys().asSequence().joinToString()
                            Log.d(TAG, "📋 Response top-level keys: $keys")
                            
                            // Try to get songs array from different possible locations
                            var songs = jsonObject.optJSONArray("songs")
                            if (songs == null || songs.length() == 0) {
                                // Check if response is a single song object
                                if (jsonObject.has("id") || jsonObject.has("title")) {
                                    songs = JSONArray().apply { put(jsonObject) }
                                    Log.d(TAG, "📋 Using response as single song object")
                                } else {
                                    // Check for nested structure
                                    val data = jsonObject.optJSONObject("data")
                                    if (data != null) {
                                        songs = data.optJSONArray("songs")
                                    }
                                }
                            }
                            
                            if (songs != null && songs.length() > 0) {
                                val song = songs.getJSONObject(0)
                                val songKeys = song.keys().asSequence().joinToString()
                                Log.d(TAG, "📋 Song object keys: $songKeys")
                                
                                // Following Flutter app logic: ONLY check encrypted_media_url in root and more_info
                                // Flutter code: response['encrypted_media_url'] ?? response['more_info']?['encrypted_media_url']
                                var encryptedUrl: String? = null
                                
                                // First, try root level encrypted_media_url
                                encryptedUrl = song.optString("encrypted_media_url", "")
                                    .takeIf { it.isNotEmpty() }
                                
                                // If not found in root, check more_info (exactly like Flutter app does)
                                if (encryptedUrl == null || encryptedUrl.isEmpty()) {
                                    val moreInfo = song.optJSONObject("more_info")
                                    if (moreInfo != null) {
                                        Log.d(TAG, "📋 Checking more_info object for encrypted_media_url")
                                        val moreInfoKeys = moreInfo.keys().asSequence().joinToString()
                                        Log.d(TAG, "📋 More_info keys: $moreInfoKeys")
                                        
                                        encryptedUrl = moreInfo.optString("encrypted_media_url", "")
                                            .takeIf { it.isNotEmpty() }
                                        
                                        if (encryptedUrl != null && encryptedUrl.isNotEmpty()) {
                                            Log.d(TAG, "✅ Found encrypted_media_url in more_info: ${encryptedUrl.take(80)}...")
                                        } else {
                                            Log.d(TAG, "⚠️ encrypted_media_url not found in more_info")
                                            // Log all more_info fields for debugging
                                            val moreInfoFields = mutableListOf<String>()
                                            moreInfo.keys().forEach { key ->
                                                val value = moreInfo.opt(key)
                                                when (value) {
                                                    is String -> {
                                                        if (value.length > 10 && value.length < 500) {
                                                            moreInfoFields.add("$key=${value.take(100)}")
                                                        }
                                                    }
                                                    is JSONObject -> moreInfoFields.add("$key=<JSONObject>")
                                                    is JSONArray -> moreInfoFields.add("$key=<JSONArray(${value.length()})>")
                                                    else -> moreInfoFields.add("$key=$value")
                                                }
                                            }
                                            Log.d(TAG, "📋 More_info fields: ${moreInfoFields.joinToString(", ")}")
                                        }
                                    } else {
                                        Log.d(TAG, "⚠️ more_info object is null or not found")
                                    }
                                } else {
                                    Log.d(TAG, "✅ Found encrypted_media_url in root: ${encryptedUrl.take(80)}...")
                                }
                                
                                // Decrypt the URL (Flutter app uses DES decryption, but we use simpler method for now)
                                if (encryptedUrl != null && encryptedUrl.isNotEmpty()) {
                                    val decryptedUrl = decryptUrl(encryptedUrl)
                                    if (decryptedUrl.isNotEmpty()) {
                                        Log.d(TAG, "✅ Successfully extracted and decrypted stream URL for song: $songId (length: ${decryptedUrl.length})")
                                        return@fold decryptedUrl
                                    } else {
                                        Log.e(TAG, "❌ Decryption resulted in empty URL for: $songId")
                                    }
                                } else {
                                    Log.e(TAG, "❌ encrypted_media_url not found in song object or more_info for: $songId")
                                }
                                
                                // If we get here, log all fields for debugging
                                val allFields = mutableListOf<String>()
                                song.keys().forEach { key ->
                                    val value = song.opt(key)
                                    when (value) {
                                        is String -> {
                                            if (value.length > 0) {
                                                allFields.add("$key=${value.take(100)}")
                                            }
                                        }
                                        is JSONObject -> allFields.add("$key=<JSONObject>")
                                        is JSONArray -> allFields.add("$key=<JSONArray(${value.length()})>")
                                        else -> allFields.add("$key=$value")
                                    }
                                }
                                Log.d(TAG, "📋 All song fields: ${allFields.joinToString(", ")}")
                                return@fold null
                            } else {
                                Log.w(TAG, "⚠️ No songs found in response for: $songId (songs array: ${songs?.length() ?: "null"})")
                                // Log response structure for debugging
                                Log.d(TAG, "📋 Full response structure (first 1000 chars): ${body.take(1000)}")
                                return@fold null
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Failed to parse JSON response: ${e.message}, trying next endpoint...")
                            Log.d(TAG, "📋 Response body (first 500 chars): ${body.take(500)}")
                            return@fold null
                        }
                    },
                    onFailure = { e ->
                        Log.w(TAG, "⚠️ API request failed for endpoint '${endpoint.params.take(60)}...': ${e.message}, trying next...")
                        null
                    }
                )
                
                // If we got a URL, return it immediately
                if (url != null && url.isNotEmpty()) {
                    return@withContext url
                }
            }
            
            // If all endpoints failed, return null
            Log.w(TAG, "⚠️ All direct endpoints failed (403 errors). This might indicate API blocking.")
            Log.w(TAG, "💡 Tip: Songs from playlist/album details may already have stream URLs cached")
            Log.e(TAG, "❌ All endpoints failed to get stream URL for song: $songId")
            null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Get song URL failed for $songId: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get album details with all songs
     */
    suspend fun getAlbumDetails(albumId: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📀 Fetching album details for ID: $albumId")
            // Use the specified endpoint: __call=album.getDetails&albumid=<id>
            val params = "__call=album.getDetails&albumid=$albumId"
            val result = getResponse(params, useV4 = false)
            
            result.fold(
                onSuccess = { body ->
                    Log.d(TAG, "📦 Album API response received, length: ${body.length}")
                    val jsonObject = JSONObject(body)
                    
                    // Check if response has error
                    if (jsonObject.has("error")) {
                        val errorMsg = jsonObject.optString("error", "Unknown error")
                        Log.e(TAG, "❌ API returned error: $errorMsg")
                        return@fold mapOf("error" to errorMsg)
                    }
                    
                    // Parse songs from "songs" array (primary) or "list" array (fallback)
                    val songsArray = jsonObject.optJSONArray("songs")
                    val songs = if (songsArray != null && songsArray.length() > 0) {
                        Log.d(TAG, "📀 Found 'songs' array with ${songsArray.length()} items")
                        val formatted = formatSongsResponse(songsArray)
                        if (formatted.isEmpty()) {
                            Log.w(TAG, "⚠️ formatSongsResponse returned empty, checking structure...")
                            Log.d(TAG, "📀 First item type: ${if (songsArray.length() > 0) songsArray.get(0).javaClass.simpleName else "empty"}")
                        }
                        formatted
                    } else {
                        // Try "list" array as fallback
                        val listArray = jsonObject.optJSONArray("list")
                        if (listArray != null && listArray.length() > 0) {
                            Log.d(TAG, "📀 Found 'list' array with ${listArray.length()} items (using as songs)")
                            formatSongsResponse(listArray)
                        } else {
                            // Try nested structures
                            val nestedSongs = jsonObject.optJSONObject("songs")?.optJSONArray("data")
                                ?: jsonObject.optJSONObject("list")?.optJSONArray("data")
                            if (nestedSongs != null && nestedSongs.length() > 0) {
                                Log.d(TAG, "📀 Found nested 'data' array with ${nestedSongs.length()} items")
                                formatSongsResponse(nestedSongs)
                            } else {
                                Log.w(TAG, "⚠️ No 'songs' or 'list' array found in album response")
                                Log.d(TAG, "📀 Available keys: ${jsonObject.keys().asSequence().joinToString()}")
                                emptyList()
                            }
                        }
                    }
                    
                    Log.d(TAG, "Loaded ${songs.size} songs for album $albumId")
                    if (songs.isEmpty()) {
                        Log.e(TAG, "❌ CRITICAL: Album has 0 songs! Check API response structure.")
                    }
                    
                    // Extract album metadata
                    val title = jsonObject.optString("title", "")
                        .takeIf { it.isNotEmpty() }
                        ?: jsonObject.optString("name", "")
                        ?: "Unknown Album"
                    
                    val image = extractImageUrl(jsonObject)
                    val artist = jsonObject.optString("primary_artists", "")
                        .takeIf { it.isNotEmpty() }
                        ?: jsonObject.optString("artist", "")
                        ?: "Unknown Artist"
                    
                    val year = jsonObject.optString("year", "")
                    val language = jsonObject.optString("language", "")
                    val description = jsonObject.optString("description", "")
                    
                    mapOf(
                        "id" to albumId,
                        "title" to title,
                        "image" to image,
                        "artist" to artist,
                        "year" to year,
                        "language" to language,
                        "description" to description,
                        "songs" to songs,
                        "songCount" to songs.size.toString()
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "❌ Failed to get album: ${e.message}", e)
                    mapOf("error" to (e.message ?: "Unknown error"))
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Get album details failed: ${e.message}", e)
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Get artist more music (all songs)
     * Uses endpoint: __call=artist.getArtistMoreMusic&artistId=<id>
     */
    suspend fun getArtistMoreMusic(artistId: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎤 Fetching artist more music for ID: $artistId")
            // Use the specified endpoint: __call=artist.getArtistMoreMusic&artistId=<id>
            val params = "__call=artist.getArtistMoreMusic&artistId=$artistId&page=1&n_song=100&n_album=50"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    Log.d(TAG, "📦 Artist More Music API response received, length: ${body.length}")
                    val jsonObject = JSONObject(body)
                    
                    // Check if response has error
                    if (jsonObject.has("error")) {
                        val errorMsg = jsonObject.optString("error", "Unknown error")
                        Log.e(TAG, "❌ API returned error: $errorMsg")
                        return@fold mapOf("error" to errorMsg)
                    }
                    
                    // Parse songs from "songs" array (primary) or "list" array (fallback)
                    val songsArray = jsonObject.optJSONArray("songs")
                    val songs = if (songsArray != null && songsArray.length() > 0) {
                        Log.d(TAG, "🎤 Found 'songs' array with ${songsArray.length()} items")
                        formatSongsResponse(songsArray)
                    } else {
                        // Try "list" array as fallback
                        val listArray = jsonObject.optJSONArray("list")
                        if (listArray != null && listArray.length() > 0) {
                            Log.d(TAG, "🎤 Found 'list' array with ${listArray.length()} items (using as songs)")
                            formatSongsResponse(listArray)
                        } else {
                            // Try "topSongs" as another fallback
                            val topSongsArray = jsonObject.optJSONArray("topSongs")
                                ?: jsonObject.optJSONObject("topSongs")?.optJSONArray("songs")
                            if (topSongsArray != null && topSongsArray.length() > 0) {
                                Log.d(TAG, "🎤 Found 'topSongs' array with ${topSongsArray.length()} items")
                                formatSongsResponse(topSongsArray)
                            } else {
                                Log.w(TAG, "⚠️ No 'songs', 'list', or 'topSongs' array found in artist response")
                                emptyList()
                            }
                        }
                    }
                    
                    Log.d(TAG, "Loaded ${songs.size} songs for artist $artistId")
                    if (songs.isEmpty()) {
                        Log.e(TAG, "❌ CRITICAL: Artist has 0 songs! Check API response structure.")
                    }
                    
                    // Extract artist metadata
                    val name = jsonObject.optString("name", "")
                        .takeIf { it.isNotEmpty() }
                        ?: jsonObject.optString("title", "")
                        ?: "Unknown Artist"
                    
                    val image = extractImageUrl(jsonObject)
                    val bio = jsonObject.optJSONArray("bio")?.optJSONObject(0)?.optString("text", "")
                        ?: jsonObject.optString("bio", "")
                    
                    mapOf(
                        "id" to artistId,
                        "name" to name,
                        "image" to image,
                        "bio" to bio,
                        "songs" to songs,
                        "songCount" to songs.size.toString()
                    )
                },
                onFailure = { e ->
                    Log.e(TAG, "❌ Failed to get artist more music: ${e.message}", e)
                    mapOf("error" to (e.message ?: "Unknown error"))
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Get artist more music failed: ${e.message}", e)
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Get playlist details with all songs
     * Uses endpoint: __call=playlist.getDetails&pids=<id>
     */
    suspend fun getPlaylistDetails(playlistId: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📋 Fetching playlist details for ID: $playlistId")
            // Try multiple parameter formats - API may require listid instead of pids
            val endpoints = listOf(
                "__call=playlist.getDetails&listid=$playlistId" to false,  // Primary: use listid
                "__call=playlist.getDetails&pids=$playlistId" to false,   // Fallback: use pids
                "__call=playlist.getDetails&pid=$playlistId" to false,    // Alternative: use pid
            )
            
            var lastError: String? = null
            for ((params, useV4) in endpoints) {
                Log.d(TAG, "🔍 Trying playlist endpoint: $params (useV4=$useV4)")
                val result = getResponse(params, useV4 = useV4)
                
                result.fold(
                    onSuccess = { body ->
                        if (body.isEmpty()) {
                            Log.w(TAG, "⚠️ Empty response, trying next endpoint...")
                            return@fold null
                        }
                        
                        val jsonObject = JSONObject(body)
                        
                        // Check if response has error
                        if (jsonObject.has("error")) {
                            val errorMsg = jsonObject.optString("error", "Unknown error")
                            Log.w(TAG, "⚠️ API returned error: $errorMsg, trying next endpoint...")
                            lastError = errorMsg
                            return@fold null
                        }
                        
                        // Success - parse and return
                        return@withContext parsePlaylistResponse(jsonObject, playlistId)
                    },
                    onFailure = { e ->
                        Log.w(TAG, "⚠️ Request failed: ${e.message}, trying next endpoint...")
                        lastError = e.message
                        null
                    }
                )?.let { return@withContext it }
            }
            
            // All endpoints failed
            Log.e(TAG, "❌ All playlist endpoints failed. Last error: $lastError")
            mapOf("error" to (lastError ?: "All endpoints failed"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Get playlist details failed: ${e.message}", e)
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Parse playlist response JSON
     */
    private fun parsePlaylistResponse(jsonObject: JSONObject, defaultPlaylistId: String): Map<String, Any?> {
        try {
            // Parse songs from "list" array (primary) or "songs" array (fallback)
            val listArray = jsonObject.optJSONArray("list")
            val songs = if (listArray != null && listArray.length() > 0) {
                Log.d(TAG, "📋 Found 'list' array with ${listArray.length()} items")
                val formatted = formatSongsResponse(listArray)
                if (formatted.isEmpty()) {
                    Log.w(TAG, "⚠️ formatSongsResponse returned empty list, checking array structure...")
                    Log.d(TAG, "📋 First item type: ${if (listArray.length() > 0) listArray.get(0).javaClass.simpleName else "empty"}")
                    formatted // Return empty, will try fallbacks below
                } else {
                    formatted
                }
            } else {
                // Try "songs" array as fallback
                val songsArray = jsonObject.optJSONArray("songs")
                if (songsArray != null && songsArray.length() > 0) {
                    Log.d(TAG, "📋 Found 'songs' array with ${songsArray.length()} items")
                    formatSongsResponse(songsArray)
                } else {
                    // Try nested structures
                    val nestedList = jsonObject.optJSONObject("list")?.optJSONArray("data")
                        ?: jsonObject.optJSONObject("songs")?.optJSONArray("data")
                    if (nestedList != null && nestedList.length() > 0) {
                        Log.d(TAG, "📋 Found nested 'data' array with ${nestedList.length()} items")
                        formatSongsResponse(nestedList)
                    } else {
                        Log.w(TAG, "⚠️ No 'list' or 'songs' array found in playlist response")
                        Log.d(TAG, "📋 Available keys: ${jsonObject.keys().asSequence().joinToString()}")
                        emptyList()
                    }
                }
            }
            
            Log.d(TAG, "Loaded ${songs.size} songs for playlist $defaultPlaylistId")
            if (songs.isEmpty()) {
                Log.e(TAG, "❌ CRITICAL: Playlist has 0 songs! Check API response structure.")
            }
            
            // Extract playlist metadata
            val title = jsonObject.optString("title", "")
                .takeIf { it.isNotEmpty() }
                ?: jsonObject.optString("name", "")
                ?: jsonObject.optString("listname", "")
                ?: "Unknown Playlist"
            
            val image = extractImageUrl(jsonObject)
            val subtitle = jsonObject.optString("subtitle", "")
                .takeIf { it.isNotEmpty() }
                ?: jsonObject.optString("firstname", "")
                ?: jsonObject.optString("creator", "")
            
            val description = jsonObject.optString("description", "")
                .takeIf { it.isNotEmpty() }
                ?: jsonObject.optString("header_desc", "")
            
            val songCount = jsonObject.optString("list_count", "")
                .takeIf { it.isNotEmpty() }
                ?: songs.size.toString()
            
            return mapOf(
                "id" to defaultPlaylistId,
                "title" to title,
                "image" to image,
                "subtitle" to subtitle,
                "description" to description,
                "songs" to songs,
                "songCount" to songCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error parsing playlist response: ${e.message}", e)
            return mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Fetch playlist title from the songs endpoint (used when title is missing)
     */
    /**
     * Fetch playlist songs separately (fallback when EP_FROM_TOKEN doesn't return songs)
     * Returns Pair of (songs list, title)
     */
    private suspend fun fetchPlaylistSongs(playlistId: String): Pair<List<Map<String, Any>>, String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Trying to fetch playlist songs separately for: $playlistId")
            // Try different endpoints to get playlist songs
            val endpoints = listOf(
                // Method 1: Use songs.getDetails with listid parameter (THIS IS THE WORKING ONE!)
                "__call=songs.getDetails&listid=$playlistId&includeMetaTags=0&ctx=android" to true,
                // Method 2: Try playlist.getDetails
                "__call=playlist.getDetails&listid=$playlistId&n=100" to false,
                // Method 3: Try webapi.get with proper parameters  
                "__call=webapi.get&token=$playlistId&type=playlist&p=1&n=100&includeMetaTags=0" to true,
                // Method 4: Try content.getPlaylistDetails
                "__call=content.getPlaylistDetails&listid=$playlistId&n=100" to false,
                // Method 5: Try with v4 API
                "pids=$playlistId&type=playlist" to true
            )
            
            for ((params, useV4) in endpoints) {
                try {
                    Log.d(TAG, "🔍 Trying endpoint: $params (v4=$useV4)")
                    val result = getResponse(params, useV4 = useV4)
                    result.fold(
                        onSuccess = { body ->
                            if (body.length > 100) { // Skip very small responses (likely errors)
                                val jsonObject = JSONObject(body)
                                val responseKeys = jsonObject.keys().asSequence().joinToString()
                                Log.d(TAG, "📋 Response keys: $responseKeys")
                                
                                // Check for error in response
                                if (jsonObject.has("error")) {
                                    val error = jsonObject.optString("error", "")
                                    if (error.isNotEmpty()) {
                                        Log.w(TAG, "⚠️ API returned error: $error, trying next endpoint...")
                                        return@fold
                                    }
                                }
                                
                                // Try to extract songs from various possible locations
                                var songsArray: JSONArray? = null
                                
                                //  Priority 1: Direct song/songs array
                                songsArray = jsonObject.optJSONArray("songs")
                                    ?: jsonObject.optJSONArray("song")
                                    ?: jsonObject.optJSONArray("list")
                                    ?: jsonObject.optJSONArray("items")
                                
                                // Priority 2: Check if response itself is an array
                                if (songsArray == null) {
                                    try {
                                        songsArray = JSONArray(body)
                                        if (songsArray.length() > 0) {
                                            Log.d(TAG, "📋 Response is a JSONArray with ${songsArray.length()} items")
                                        }
                                    } catch (e: Exception) {
                                        // Not an array, continue checking
                                    }
                                }
                                
                                // Priority 3: Nested structures
                                if (songsArray == null || songsArray.length() == 0) {
                                    songsArray = jsonObject.optJSONObject("songs")?.optJSONArray("items")
                                        ?: jsonObject.optJSONObject("data")?.optJSONArray("songs")
                                        ?: jsonObject.optJSONObject("data")?.optJSONArray("results")
                                        ?: jsonObject.optJSONObject("more_info")?.optJSONArray("song")
                                        ?: jsonObject.optJSONObject("more_info")?.optJSONArray("songs")
                                }
                                
                                // Priority 4: Check modules/sections structure (some playlists use this)
                                if (songsArray == null || songsArray.length() == 0) {
                                    val modules = jsonObject.optJSONObject("modules")
                                    if (modules != null) {
                                        val modulesKeys = modules.keys()
                                        while (modulesKeys.hasNext()) {
                                            val key = modulesKeys.next()
                                            val module = modules.optJSONObject(key)
                                            if (module != null) {
                                                songsArray = module.optJSONArray("songs")
                                                    ?: module.optJSONArray("list")
                                                if (songsArray != null && songsArray.length() > 0) {
                                                    Log.d(TAG, "📋 Found songs in modules.$key")
                                                    break
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                if (songsArray != null && songsArray.length() > 0) {
                                    Log.d(TAG, "✅ Found ${songsArray.length()} songs using endpoint: ${params.take(60)}... (v4=$useV4)")
                                    
                                    // Extract title from response  
                                    var title = jsonObject.optString("title", "")
                                        .takeIf { it.isNotEmpty() }
                                        ?: jsonObject.optString("name", "")
                                            .takeIf { it.isNotEmpty() }
                                        ?: jsonObject.optString("subtitle", "")
                                            .takeIf { it.isNotEmpty() }
                                        ?: jsonObject.optString("header_desc", "")
                                            .takeIf { it.isNotEmpty() }
                                        ?: jsonObject.optString("listname", "")
                                            .takeIf { it.isNotEmpty() }
                                        ?: ""
                                    
                                    // Try more_info for title if still empty
                                    if (title.isEmpty()) {
                                        val moreInfo = jsonObject.optJSONObject("more_info")
                                        if (moreInfo != null) {
                                            title = moreInfo.optString("firstname", "")
                                                .takeIf { it.isNotEmpty() }
                                                ?: moreInfo.optString("username", "")
                                                    .takeIf { it.isNotEmpty() }
                                                ?: ""
                                        }
                                    }
                                    
                                    if (title.isNotEmpty()) {
                                        Log.d(TAG, "📝 Found title in response: $title")
                                    }
                                    
                                    val formattedSongs = formatSongsResponse(songsArray)
                                    if (formattedSongs.isNotEmpty()) {
                                        return@withContext Pair(formattedSongs, title)
                                    } else {
                                        Log.w(TAG, "⚠️ Songs array had items but formatting returned empty list")
                                    }
                                } else {
                                    Log.d(TAG, "⚠️ No songs found in this endpoint's response")
                                }
                            } else {
                                Log.d(TAG, "⚠️ Response too small (${body.length} bytes), likely an error")
                            }
                        },
                        onFailure = { e ->
                            Log.w(TAG, "Endpoint failed: ${e.message}")
                        }
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Endpoint $params failed: ${e.message}")
                }
            }
            
            Log.w(TAG, "❌ All endpoints failed to fetch playlist songs")
            Pair(emptyList(), "")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching playlist songs separately", e)
            Pair(emptyList(), "")
        }
    }
    
    /**
     * Get Artist Details
     * Fetches artist info, top songs, and albums from JioSaavn
     */
    suspend fun getArtistDetails(artistId: String): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "👤 Fetching artist details for: $artistId")
            
            // Try multiple artist endpoints
            val endpoints = listOf(
                "__call=artist.getArtistPageDetails&artistId=$artistId&page=1&n_song=50&n_album=30&sub_type=",
                "__call=webapi.get&token=$artistId&type=artist&p=1&n_song=50&n_album=30",
                "token=$artistId&type=artist&$EP_FROM_TOKEN",
                "__call=content.getArtistPageDetails&artistId=$artistId&page=1"
            )
            
            for (params in endpoints) {
                try {
                    Log.d(TAG, "🔍 Trying artist endpoint: ${params.take(60)}...")
                    val result = getResponse(params, useV4 = true)
                    
                    result.fold(
                        onSuccess = { body ->
                            if (body.length > 100) {
                                val jsonObject = JSONObject(body)
                                Log.d(TAG, "📋 Artist response keys: ${jsonObject.keys().asSequence().joinToString()}")
                                
                                // Extract artist data
                                val name = jsonObject.optString("name", "")
                                    .takeIf { it.isNotEmpty() }
                                    ?: jsonObject.optString("title", "Unknown Artist")
                                
                                if (name != "Unknown Artist") {
                                    val topSongs = jsonObject.optJSONArray("topSongs")
                                        ?: jsonObject.optJSONObject("topSongs")?.optJSONArray("songs")
                                        ?: jsonObject.optJSONArray("songs")
                                        ?: JSONArray()
                                    
                                    val topAlbums = jsonObject.optJSONArray("topAlbums")
                                        ?: jsonObject.optJSONObject("topAlbums")?.optJSONArray("albums")
                                        ?: jsonObject.optJSONArray("albums")
                                        ?: JSONArray()
                                    
                                    // Extract all possible image sources
                                    val imageUrl = jsonObject.optString("image_url", "")
                                    val imageField = jsonObject.opt("image")
                                    val imagesField = jsonObject.opt("images")
                                    
                                    // Convert image field to appropriate format
                                    val imageValue = when {
                                        imageField is org.json.JSONArray -> {
                                            val imageList = mutableListOf<Any>()
                                            for (j in 0 until imageField.length()) {
                                                val item = imageField.get(j)
                                                when (item) {
                                                    is org.json.JSONObject -> {
                                                        val map = mutableMapOf<String, Any>()
                                                        item.keys().forEach { key ->
                                                            map[key] = item.get(key)
                                                        }
                                                        imageList.add(map)
                                                    }
                                                    is String -> imageList.add(item)
                                                    else -> {}
                                                }
                                            }
                                            imageList
                                        }
                                        imageField is String -> imageField
                                        else -> extractImageUrl(jsonObject, "image")
                                    }
                                    
                                    // Convert images field to List if it's an array
                                    val imagesValue = if (imagesField is org.json.JSONArray) {
                                        val imagesList = mutableListOf<Any>()
                                        for (j in 0 until imagesField.length()) {
                                            val item = imagesField.get(j)
                                            when (item) {
                                                is org.json.JSONObject -> {
                                                    val map = mutableMapOf<String, Any>()
                                                    item.keys().forEach { key ->
                                                        map[key] = item.get(key)
                                                    }
                                                    imagesList.add(map)
                                                }
                                                is String -> imagesList.add(item)
                                                else -> {}
                                            }
                                        }
                                        imagesList
                                    } else null
                                    
                                    // Format songs - ensure we get non-empty list
                                    val formattedSongs = formatSongsResponse(topSongs)
                                    Log.d(TAG, "📊 Formatted ${formattedSongs.size} songs from topSongs array (length: ${topSongs.length()})")
                                    
                                    // If no songs found, try alternative endpoints
                                    val finalSongs = if (formattedSongs.isEmpty()) {
                                        Log.w(TAG, "⚠️ No songs from topSongs, trying alternative methods...")
                                        // Try to get songs from other fields
                                        val alternativeSongs = jsonObject.optJSONArray("songs")
                                            ?: jsonObject.optJSONObject("songs")?.optJSONArray("data")
                                            ?: JSONArray()
                                        val altFormatted = formatSongsResponse(alternativeSongs)
                                        if (altFormatted.isNotEmpty()) {
                                            Log.d(TAG, "✅ Found ${altFormatted.size} songs from alternative source")
                                            altFormatted
                                        } else {
                                            // Last resort: search for artist songs
                                            Log.d(TAG, "🔍 Searching for artist songs as fallback...")
                                            try {
                                                val searchResults = fetchSearchResults(name, count = 50)
                                                val artistSongs = (searchResults["songs"] as? List<Map<String, Any>> ?: emptyList())
                                                    .filter { song ->
                                                        val songArtist = (song["artist"] as? String ?: "").lowercase()
                                                        songArtist.contains(name.lowercase()) || name.lowercase().contains(songArtist)
                                                    }
                                                    .take(30)
                                                Log.d(TAG, "✅ Found ${artistSongs.size} songs via search")
                                                artistSongs
                                            } catch (e: Exception) {
                                                Log.w(TAG, "⚠️ Search fallback failed: ${e.message}")
                                                emptyList()
                                            }
                                        }
                                    } else {
                                        formattedSongs
                                    }
                                    
                                    val artistData = mutableMapOf<String, Any>(
                                        "id" to artistId,
                                        "name" to cleanText(name),
                                        "followerCount" to jsonObject.optString("follower_count", ""),
                                        "fanCount" to jsonObject.optString("fan_count", ""),
                                        "bio" to cleanText(jsonObject.optJSONArray("bio")?.optJSONObject(0)?.optString("text", "") ?: ""),
                                        "dominantLanguage" to jsonObject.optString("dominantLanguage", ""),
                                        "dominantType" to jsonObject.optString("dominantType", ""),
                                        "topSongs" to finalSongs,
                                        "topAlbums" to formatAlbumsArray(topAlbums),
                                        "error" to ""
                                    )
                                    
                                    // Add image sources in priority order - ensure we extract a valid URL string
                                    val finalImage = when {
                                        imageUrl.isNotEmpty() -> imageUrl
                                        imageValue is String && imageValue.isNotEmpty() -> imageValue
                                        imageValue is List<*> && imageValue.isNotEmpty() -> {
                                            // Extract first valid string from list
                                            imageValue.firstNotNullOfOrNull { item ->
                                                when (item) {
                                                    is String -> item.takeIf { it.isNotEmpty() }
                                                    is Map<*, *> -> {
                                                        @Suppress("UNCHECKED_CAST")
                                                        val map = item as? Map<String, Any>
                                                        (map?.get("link") as? String)?.takeIf { it.isNotEmpty() }
                                                            ?: (map?.get("url") as? String)?.takeIf { it.isNotEmpty() }
                                                    }
                                                    else -> null
                                                }
                                            } ?: ""
                                        }
                                        imagesValue is List<*> && imagesValue.isNotEmpty() -> {
                                            imagesValue.firstNotNullOfOrNull { item ->
                                                when (item) {
                                                    is String -> item.takeIf { it.isNotEmpty() }
                                                    is Map<*, *> -> {
                                                        @Suppress("UNCHECKED_CAST")
                                                        val map = item as? Map<String, Any>
                                                        (map?.get("link") as? String)?.takeIf { it.isNotEmpty() }
                                                            ?: (map?.get("url") as? String)?.takeIf { it.isNotEmpty() }
                                                    }
                                                    else -> null
                                                }
                                            } ?: ""
                                        }
                                        else -> ""
                                    }
                                    
                                    if (imageUrl.isNotEmpty()) {
                                        artistData["image_url"] = imageUrl
                                    }
                                    artistData["image"] = finalImage
                                    if (imagesValue != null) {
                                        artistData["images"] = imagesValue
                                    }
                                    
                                    Log.d(TAG, "✅ Artist details loaded: $name (${finalSongs.size} songs, ${topAlbums.length()} albums, image: ${if (finalImage.isNotEmpty()) "✅" else "❌"})")
                                    return@withContext artistData
                                }
                            }
                        },
                        onFailure = { e ->
                            Log.w(TAG, "Artist endpoint failed: ${e.message}")
                        }
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Artist endpoint $params failed: ${e.message}")
                }
            }
            
            Log.w(TAG, "❌ All artist endpoints failed for: $artistId")
            mapOf("error" to "Failed to fetch artist details")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching artist details: ${e.message}", e)
            mapOf("error" to e.message.toString())
        }
    }
    
    /**
     * Fetch JioSaavn Home Page Data (Trending/Featured Playlists)
     * FULL INDEPENDENT IMPLEMENTATION - uses JioSaavn API directly
     * URL: https://www.jiosaavn.com/api.php?__call=webapi.getLaunchData&api_version=4&_format=json
     * 
     * @return Map with sections: featured playlists, top charts, new releases, etc.
     */
    suspend fun fetchHomePageData(): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            // Use proper JioSaavn API endpoint with version 4
            val params = "__call=webapi.getLaunchData"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    formatHomePageData(jsonObject)
                },
                onFailure = { e ->
                    Log.e(TAG, "Failed to fetch home data: ${e.message}", e)
                    mapOf("error" to (e.message ?: "Unknown error"), "sections" to emptyList<Map<String, Any>>())
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching home page data: ${e.message}", e)
            mapOf("error" to (e.message ?: "Unknown error"), "sections" to emptyList<Map<String, Any>>())
        }
    }
    
    /**
     * Get Featured Playlists (Trending/Popular)
     * Used for Home Screen content
     */
    suspend fun getFeaturedPlaylists(count: Int = 20): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val params = "__call=content.getFeaturedPlaylists&n=$count"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val playlistsArray = jsonObject.optJSONArray("playlists") ?: JSONArray()
                    
                    val playlists = mutableListOf<Map<String, Any>>()
                    for (i in 0 until playlistsArray.length()) {
                        try {
                            val playlist = playlistsArray.getJSONObject(i)
                            playlists.add(formatPlaylistItem(playlist))
                        } catch (e: Exception) {
                            Log.w(TAG, "Error formatting playlist $i: ${e.message}")
                        }
                    }
                    playlists
                },
                onFailure = { emptyList() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching featured playlists: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get Trending Playlists - Popular playlists from JioSaavn
     * Fetches trending/popular playlists that users can play
     */
    suspend fun getTrendingPlaylists(count: Int = 20): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📋 Fetching trending playlists...")
            
            // Method 1: Try to get from home page data
            val homeData = fetchHomePageData()
            val sections = homeData["sections"] as? List<Map<String, Any>> ?: emptyList()
            
            // Look for trending playlists section
            val trendingPlaylistsSection = sections.find { section ->
                val title = section["title"] as? String ?: ""
                val type = section["type"] as? String ?: ""
                (title.contains("Trending", ignoreCase = true) || 
                 title.contains("Popular", ignoreCase = true) ||
                 title.contains("Top", ignoreCase = true)) &&
                (type == "playlists" || type == "playlist")
            }
            
            if (trendingPlaylistsSection != null) {
                val items = trendingPlaylistsSection["items"] as? List<Map<String, Any>> ?: emptyList()
                if (items.isNotEmpty()) {
                    Log.d(TAG, "✅ Found ${items.size} trending playlists from home page")
                    return@withContext items.take(count)
                }
            }
            
            // Method 2: Try API endpoint for trending playlists
            val params = "__call=content.getTrendingPlaylists&n=$count"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val playlistsArray = jsonObject.optJSONArray("playlists") 
                        ?: jsonObject.optJSONArray("list")
                        ?: jsonObject.optJSONArray("data")
                        ?: JSONArray()
                    
                    val playlists = mutableListOf<Map<String, Any>>()
                    for (i in 0 until playlistsArray.length()) {
                        try {
                            val playlist = playlistsArray.getJSONObject(i)
                            playlists.add(formatPlaylistItem(playlist))
                        } catch (e: Exception) {
                            Log.w(TAG, "Error formatting trending playlist $i: ${e.message}")
                        }
                    }
                    if (playlists.isNotEmpty()) {
                        Log.d(TAG, "✅ Fetched ${playlists.size} trending playlists from API")
                        return@fold playlists
                    }
                    emptyList()
                },
                onFailure = { 
                    Log.w(TAG, "⚠️ API call failed, trying search fallback...")
                    emptyList()
                }
            ) ?: run {
                // Method 3: Fallback - search for popular playlists
                try {
                    Log.d(TAG, "🔍 Trying search fallback for trending playlists...")
                    val searchResults = fetchSearchResults("trending playlists", count)
                    val playlists = searchResults["playlists"] as? List<Map<String, Any>> ?: emptyList()
                    if (playlists.isNotEmpty()) {
                        Log.d(TAG, "✅ Found ${playlists.size} playlists from search")
                        return@withContext playlists.take(count)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Search fallback failed: ${e.message}")
                }
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching trending playlists: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get Playlists by Category/Genre
     * Fetches playlists for a specific category (e.g., Hindi, Punjabi, English, etc.)
     */
    suspend fun getPlaylistsByCategory(category: String, count: Int = 20): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📋 Fetching playlists for category: $category")
            
            // Try search with category name
            val searchQuery = "$category playlists"
            val searchResults = fetchSearchResults(searchQuery, count)
            val playlists = searchResults["playlists"] as? List<Map<String, Any>> ?: emptyList()
            
            if (playlists.isNotEmpty()) {
                Log.d(TAG, "✅ Found ${playlists.size} playlists for category: $category")
                return@withContext playlists.take(count)
            }
            
            // Fallback: Try API endpoint
            val params = "__call=content.getPlaylistsByCategory&category=${URLEncoder.encode(category, "UTF-8")}&n=$count"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val playlistsArray = jsonObject.optJSONArray("playlists") 
                        ?: jsonObject.optJSONArray("list")
                        ?: JSONArray()
                    
                    val formattedPlaylists = mutableListOf<Map<String, Any>>()
                    for (i in 0 until playlistsArray.length()) {
                        try {
                            val playlist = playlistsArray.getJSONObject(i)
                            formattedPlaylists.add(formatPlaylistItem(playlist))
                        } catch (e: Exception) {
                            Log.w(TAG, "Error formatting category playlist $i: ${e.message}")
                        }
                    }
                    formattedPlaylists
                },
                onFailure = { emptyList() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching playlists by category: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get Top Charts - Returns actual trending albums from JioSaavn
     * Fetches trending/new albums which serve as top charts
     */
    suspend fun getTopCharts(count: Int = 20): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📊 Fetching top charts (albums) from JioSaavn API...")
            
            // Method 1: Get trending/new albums from home page data (these are actual albums)
            val homeData = fetchHomePageData()
            val sections = homeData["sections"] as? List<Map<String, Any>> ?: emptyList()
            
            Log.d(TAG, "📋 Checking ${sections.size} sections for top charts")
            sections.forEachIndexed { index, section ->
                val title = section["title"] as? String ?: ""
                val type = section["type"] as? String ?: ""
                val items = section["items"] as? List<*> ?: emptyList<Any>()
                Log.d(TAG, "   Section $index: title='$title', type='$type', items=${items.size}")
            }
            
            // First priority: Look for "New Releases" or "Trending" section which contains albums
            var trendingSection = sections.find { section ->
                val title = section["title"] as? String ?: ""
                val type = section["type"] as? String ?: ""
                (title.contains("New Releases", ignoreCase = true) || 
                 title.contains("Trending", ignoreCase = true) ||
                 title.contains("New Albums", ignoreCase = true)) &&
                type == "albums"
            }
            
            // Second priority: Any section with type "albums" (these are actual albums)
            if (trendingSection == null) {
                trendingSection = sections.find { section ->
                    val type = section["type"] as? String ?: ""
                    type == "albums"
                }
            }
            
            // Third priority: If no albums section, try to convert playlists from charts to albums
            // But ONLY if we can't find albums - we prefer albums over playlists
            if (trendingSection == null) {
                Log.w(TAG, "⚠️ No albums section found. Checking if charts can be used...")
                // Don't use Top Charts if it's playlists - we want albums
            }
            
            if (trendingSection != null) {
                val trendingItems = trendingSection["items"] as? List<Map<String, Any>> ?: emptyList()
                if (trendingItems.isNotEmpty()) {
                    Log.d(TAG, "✅ Found ${trendingItems.size} trending albums from home data section: ${trendingSection["title"]}")
                    return@withContext trendingItems.take(count)
                } else {
                    Log.w(TAG, "⚠️ Trending section found but has no items")
                }
            } else {
                Log.w(TAG, "⚠️ No trending section found. Trying alternative methods...")
            }
            
            // Method 2: Try to get albums from raw home page API response
            try {
                val params = "__call=webapi.getLaunchData"
                val result = getResponse(params, useV4 = true)
                
                result.fold(
                    onSuccess = { body ->
                        val jsonObject = JSONObject(body)
                        // Look for new_trending which contains albums
                        val newTrendingArray = jsonObject.optJSONArray("new_trending") ?: JSONArray()
                        if (newTrendingArray.length() > 0) {
                            val albums = formatAlbumsArray(newTrendingArray)
                            if (albums.isNotEmpty()) {
                                Log.d(TAG, "✅ Fetched ${albums.size} trending albums from launch data")
                                return@withContext albums.take(count)
                            }
                        }
                        
                        // Fallback: Try trending albums endpoint
                        val trendingParams = "__call=content.getTrendingAlbums&n=$count"
                        val trendingResult = getResponse(trendingParams, useV4 = true)
                        trendingResult.fold(
                            onSuccess = { trendingBody ->
                                val trendingJson = JSONObject(trendingBody)
                                val albumsArray = trendingJson.optJSONArray("albums") 
                                    ?: trendingJson.optJSONArray("data")
                                    ?: JSONArray()
                                if (albumsArray.length() > 0) {
                                    val albums = formatAlbumsArray(albumsArray)
                                    Log.d(TAG, "✅ Fetched ${albums.size} albums from trending API")
                                    return@withContext albums.take(count)
                                }
                                emptyList()
                            },
                            onFailure = { emptyList() }
                        )
                    },
                    onFailure = { 
                        Log.w(TAG, "Home data API call failed: ${it.message}")
                        emptyList()
                    }
                )
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching from home data: ${e.message}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching top charts: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get Trending/Top Artists
     * Fetches popular artists from JioSaavn
     */
    suspend fun getTrendingArtists(count: Int = 20): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "👥 Fetching trending artists from JioSaavn API...")
            
            // Try to get artists from home page data first
            val homeData = fetchHomePageData()
            val sections = homeData["sections"] as? List<Map<String, Any>> ?: emptyList()
            
            // Look for artist section in home data
            val artistsSection = sections.find { section ->
                val type = section["type"] as? String ?: ""
                type == "artists"
            }
            
            if (artistsSection != null) {
                val artists = artistsSection["items"] as? List<Map<String, Any>> ?: emptyList()
                if (artists.isNotEmpty()) {
                    Log.d(TAG, "✅ Found ${artists.size} artists from home data")
                    return@withContext artists.take(count)
                }
            }
            
            // Fallback: Try dedicated trending artists endpoint
            val params = "__call=content.getTrendingArtists&n=$count"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val artistsArray = jsonObject.optJSONArray("artists") 
                        ?: jsonObject.optJSONArray("data")
                        ?: JSONArray()
                    
                    if (artistsArray.length() > 0) {
                        val artists = formatArtistsArray(artistsArray)
                        Log.d(TAG, "✅ Fetched ${artists.size} trending artists")
                        return@withContext artists.take(count)
                    }
                    emptyList()
                },
                onFailure = { 
                    Log.w(TAG, "Trending artists API call failed: ${it.message}")
                    emptyList()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching trending artists: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get song recommendations
     */
    suspend fun getSongRecommendations(songId: String): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val params = "${EP_GET_RECO}&pid=$songId"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    val jsonArray = JSONArray(body)
                    formatSongsResponse(jsonArray)
                },
                onFailure = { emptyList() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Get recommendations failed: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get Trending Songs
     */
    suspend fun getTrendingSongs(count: Int = 20): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎵 Fetching trending songs...")
            
            // Try to get from home page data first
            val homeData = fetchHomePageData()
            val sections = homeData["sections"] as? List<Map<String, Any>> ?: emptyList()
            
            // Look for trending songs section
            val trendingSection = sections.find { section ->
                val title = section["title"] as? String ?: ""
                val type = section["type"] as? String ?: ""
                (title.contains("Trending", ignoreCase = true) || 
                 title.contains("Top Songs", ignoreCase = true)) &&
                type == "songs"
            }
            
            if (trendingSection != null) {
                val songs = trendingSection["items"] as? List<Map<String, Any>> ?: emptyList()
                if (songs.isNotEmpty()) {
                    Log.d(TAG, "✅ Found ${songs.size} trending songs from home data")
                    return@withContext songs.take(count)
                }
            }
            
            // Fallback: Try dedicated trending songs endpoint
            val params = "__call=content.getTrendingSongs&n=$count"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val songsArray = jsonObject.optJSONArray("songs") 
                        ?: jsonObject.optJSONArray("data")
                        ?: JSONArray()
                    
                    if (songsArray.length() > 0) {
                        val songs = formatSongsResponse(songsArray)
                        Log.d(TAG, "✅ Fetched ${songs.size} trending songs")
                        return@withContext songs.take(count)
                    }
                    emptyList()
                },
                onFailure = { 
                    Log.w(TAG, "Trending songs API call failed: ${it.message}")
                    emptyList()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching trending songs: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get JioSaavn Originals (exclusive content)
     */
    suspend fun getSaavnOriginals(count: Int = 20): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎬 Fetching JioSaavn Originals...")
            
            // Try to get from home page data
            val homeData = fetchHomePageData()
            val sections = homeData["sections"] as? List<Map<String, Any>> ?: emptyList()
            
            val originalsSection = sections.find { section ->
                val title = section["title"] as? String ?: ""
                title.contains("Originals", ignoreCase = true) ||
                title.contains("Exclusive", ignoreCase = true)
            }
            
            if (originalsSection != null) {
                val items = originalsSection["items"] as? List<Map<String, Any>> ?: emptyList()
                if (items.isNotEmpty()) {
                    Log.d(TAG, "✅ Found ${items.size} originals from home data")
                    return@withContext items.take(count)
                }
            }
            
            // Fallback: Try dedicated originals endpoint
            val params = "__call=content.getOriginals&n=$count"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val itemsArray = jsonObject.optJSONArray("playlists")
                        ?: jsonObject.optJSONArray("albums")
                        ?: jsonObject.optJSONArray("data")
                        ?: JSONArray()
                    
                    if (itemsArray.length() > 0) {
                        val items = formatPlaylistsArray(itemsArray)
                        Log.d(TAG, "✅ Fetched ${items.size} originals")
                        return@withContext items.take(count)
                    }
                    emptyList()
                },
                onFailure = { 
                    Log.w(TAG, "Originals API call failed: ${it.message}")
                    emptyList()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching originals: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get Recommended Mixes
     */
    suspend fun getRecommendedMixes(count: Int = 20): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎧 Fetching recommended mixes...")
            
            // Try to get from home page data
            val homeData = fetchHomePageData()
            val sections = homeData["sections"] as? List<Map<String, Any>> ?: emptyList()
            
            val mixesSection = sections.find { section ->
                val title = section["title"] as? String ?: ""
                title.contains("Mix", ignoreCase = true) ||
                title.contains("Radio", ignoreCase = true)
            }
            
            if (mixesSection != null) {
                val items = mixesSection["items"] as? List<Map<String, Any>> ?: emptyList()
                if (items.isNotEmpty()) {
                    Log.d(TAG, "✅ Found ${items.size} mixes from home data")
                    return@withContext items.take(count)
                }
            }
            
            // Fallback: Try dedicated mixes endpoint
            val params = "__call=content.getMixes&n=$count"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val itemsArray = jsonObject.optJSONArray("playlists")
                        ?: jsonObject.optJSONArray("mixes")
                        ?: jsonObject.optJSONArray("data")
                        ?: JSONArray()
                    
                    if (itemsArray.length() > 0) {
                        val items = formatPlaylistsArray(itemsArray)
                        Log.d(TAG, "✅ Fetched ${items.size} mixes")
                        return@withContext items.take(count)
                    }
                    emptyList()
                },
                onFailure = { 
                    Log.w(TAG, "Mixes API call failed: ${it.message}")
                    emptyList()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching mixes: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get Genre-Based Playlists/Albums
     * @param genre Genre name (Hindi, Punjabi, Tamil, Telugu, English, etc.)
     */
    suspend fun getGenreContent(genre: String, count: Int = 20): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎭 Fetching $genre content...")
            
            // Try genre-specific endpoint
            val encodedGenre = java.net.URLEncoder.encode(genre, "UTF-8")
            val params = "__call=content.getGenreContent&genre=$encodedGenre&n=$count"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val itemsArray = jsonObject.optJSONArray("playlists")
                        ?: jsonObject.optJSONArray("albums")
                        ?: jsonObject.optJSONArray("data")
                        ?: JSONArray()
                    
                    if (itemsArray.length() > 0) {
                        val items = formatPlaylistsArray(itemsArray)
                        Log.d(TAG, "✅ Fetched ${items.size} items for genre: $genre")
                        return@withContext items.take(count)
                    }
                    emptyList()
                },
                onFailure = { 
                    Log.w(TAG, "Genre content API call failed: ${it.message}")
                    emptyList()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching genre content: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get Editor's Picks (curated playlists)
     */
    suspend fun getEditorsPicks(count: Int = 20): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "⭐ Fetching editor's picks...")
            
            // Try to get from home page data
            val homeData = fetchHomePageData()
            val sections = homeData["sections"] as? List<Map<String, Any>> ?: emptyList()
            
            val editorsSection = sections.find { section ->
                val title = section["title"] as? String ?: ""
                title.contains("Editor", ignoreCase = true) ||
                title.contains("Curated", ignoreCase = true) ||
                title.contains("Picks", ignoreCase = true)
            }
            
            if (editorsSection != null) {
                val items = editorsSection["items"] as? List<Map<String, Any>> ?: emptyList()
                if (items.isNotEmpty()) {
                    Log.d(TAG, "✅ Found ${items.size} editor's picks from home data")
                    return@withContext items.take(count)
                }
            }
            
            // Fallback: Use featured playlists as editor's picks
            val featured = getFeaturedPlaylists(count)
            if (featured.isNotEmpty()) {
                Log.d(TAG, "✅ Using ${featured.size} featured playlists as editor's picks")
                return@withContext featured
            }
            
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching editor's picks: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get New Releases (albums and singles)
     */
    suspend fun getNewReleases(count: Int = 20): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🆕 Fetching new releases...")
            
            // Try to get from home page data
            val homeData = fetchHomePageData()
            val sections = homeData["sections"] as? List<Map<String, Any>> ?: emptyList()
            
            val newReleasesSection = sections.find { section ->
                val title = section["title"] as? String ?: ""
                val type = section["type"] as? String ?: ""
                (title.contains("New Release", ignoreCase = true) ||
                 title.contains("New Album", ignoreCase = true)) &&
                type == "albums"
            }
            
            if (newReleasesSection != null) {
                val items = newReleasesSection["items"] as? List<Map<String, Any>> ?: emptyList()
                if (items.isNotEmpty()) {
                    Log.d(TAG, "✅ Found ${items.size} new releases from home data")
                    return@withContext items.take(count)
                }
            }
            
            // Fallback: Try dedicated new releases endpoint
            val params = "__call=content.getNewReleases&n=$count"
            val result = getResponse(params, useV4 = true)
            
            result.fold(
                onSuccess = { body ->
                    val jsonObject = JSONObject(body)
                    val itemsArray = jsonObject.optJSONArray("albums")
                        ?: jsonObject.optJSONArray("new_trending")
                        ?: jsonObject.optJSONArray("data")
                        ?: JSONArray()
                    
                    if (itemsArray.length() > 0) {
                        val items = formatAlbumsArray(itemsArray)
                        Log.d(TAG, "✅ Fetched ${items.size} new releases")
                        return@withContext items.take(count)
                    }
                    emptyList()
                },
                onFailure = { 
                    Log.w(TAG, "New releases API call failed: ${it.message}")
                    emptyList()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching new releases: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Mapper function: Convert JSON section to SaavnHomeSection
     * This is the critical mapping layer that was missing
     */
    private fun mapSaavnJsonToSection(
        title: String,
        sectionType: String,
        items: List<Map<String, Any>>
    ): SaavnHomeSection? {
        if (items.isEmpty()) {
            Log.w(TAG, "⚠️ Section '$title' has no items, skipping")
            return null
        }
        
        val saavnSectionType = when (sectionType.lowercase()) {
            "songs", "song" -> SaavnSectionType.SONG
            "albums", "album" -> SaavnSectionType.ALBUM
            "playlists", "playlist" -> SaavnSectionType.PLAYLIST
            "artists", "artist" -> SaavnSectionType.ARTIST
            "charts", "chart" -> SaavnSectionType.CHART
            "mixes", "mix" -> SaavnSectionType.MIX
            "trending" -> SaavnSectionType.TRENDING
            "new_release", "new releases" -> SaavnSectionType.NEW_RELEASE
            "featured" -> SaavnSectionType.FEATURED
            "originals" -> SaavnSectionType.ORIGINALS
            else -> {
                // Try to infer from title
                when {
                    title.contains("Trending", ignoreCase = true) -> SaavnSectionType.TRENDING
                    title.contains("New Release", ignoreCase = true) -> SaavnSectionType.NEW_RELEASE
                    title.contains("Chart", ignoreCase = true) -> SaavnSectionType.CHART
                    title.contains("Mix", ignoreCase = true) -> SaavnSectionType.MIX
                    title.contains("Original", ignoreCase = true) -> SaavnSectionType.ORIGINALS
                    title.contains("Featured", ignoreCase = true) -> SaavnSectionType.FEATURED
                    else -> SaavnSectionType.PLAYLIST // Default fallback
                }
            }
        }
        
        // Map items based on section type
        var successCount = 0
        var failCount = 0
        val saavnItems = items.mapIndexedNotNull { itemIndex, itemMap ->
            try {
                val mappedItem = when (saavnSectionType) {
                    SaavnSectionType.SONG, SaavnSectionType.TRENDING -> {
                        itemMap.toSaavnSong()
                    }
                    SaavnSectionType.ALBUM, SaavnSectionType.NEW_RELEASE -> {
                        itemMap.toSaavnAlbum()
                    }
                    SaavnSectionType.PLAYLIST, SaavnSectionType.MIX, SaavnSectionType.FEATURED, SaavnSectionType.ORIGINALS -> {
                        itemMap.toSaavnPlaylist()
                    }
                    SaavnSectionType.ARTIST -> {
                        itemMap.toSaavnArtist()
                    }
                    SaavnSectionType.CHART -> {
                        // Charts can contain albums or playlists
                        val chartItem = if (itemMap.containsKey("albumid") || itemMap["type"] == "album" || itemMap["type"] == "albums") {
                            itemMap.toSaavnAlbum()
                        } else {
                            itemMap.toSaavnPlaylist()
                        }
                        SaavnChartEntry(
                            id = chartItem.id,
                            title = chartItem.title,
                            image = chartItem.image,
                            subtitle = chartItem.subtitle,
                            rank = itemIndex + 1,
                            item = chartItem
                        )
                    }
                    else -> {
                        // Default fallback - try playlist
                        itemMap.toSaavnPlaylist()
                    }
                }
                
                if (mappedItem != null) {
                    successCount++
                } else {
                    failCount++
                }
                mappedItem
            } catch (e: Exception) {
                failCount++
                Log.e(TAG, "❌ Error mapping item at index $itemIndex in section '$title': ${e.message}", e)
                Log.e(TAG, "   Item keys: ${itemMap.keys.joinToString()}")
                null
            }
        }
        
        Log.d(TAG, "📊 Mapping stats for '$title': ${successCount} succeeded, ${failCount} failed, ${saavnItems.size} total")
        
        if (saavnItems.isEmpty()) {
            Log.w(TAG, "⚠️ Section '$title' mapped to 0 items after conversion, skipping")
            return null
        }
        
        Log.d(TAG, "✅ Mapped section '$title': ${saavnItems.size} items (type: $saavnSectionType)")
        return SaavnHomeSection(
            title = title,
            type = saavnSectionType,
            items = saavnItems
        )
    }
    
    /**
     * Fetch complete home feed with all sections in parallel
     * Returns structured sections ready for UI
     * FIXED: Now uses home page data directly and proper mapping
     */
    suspend fun fetchCompleteHomeFeed(): List<SaavnHomeSection> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🏠 Fetching complete JioSaavn home feed...")
            
            val sections = mutableListOf<SaavnHomeSection>()
            
            // PRIMARY METHOD: Use home page data which already has structured sections
            val homeData = fetchHomePageData()
            val homeSections = homeData["sections"] as? List<Map<String, Any>> ?: emptyList()
            
            Log.d(TAG, "📋 Found ${homeSections.size} sections in home page data")
            
            // Map home page sections directly
            homeSections.forEachIndexed { index, sectionMap ->
                try {
                    val sectionTitle = sectionMap["title"] as? String ?: "Section ${index + 1}"
                    val sectionType = sectionMap["type"] as? String ?: ""
                    val sectionItemsRaw = sectionMap["items"]
                    
                    // Handle different item formats - CRITICAL: Convert all formats to List<Map<String, Any>>
                    val sectionItems = when (sectionItemsRaw) {
                        is List<*> -> {
                            // Already a list - filter and convert
                            sectionItemsRaw.mapNotNull { item ->
                                when (item) {
                                    is Map<*, *> -> {
                                        // Convert to Map<String, Any>
                                        @Suppress("UNCHECKED_CAST")
                                        item as? Map<String, Any>
                                    }
                                    is JSONObject -> {
                                        // Convert JSONObject to Map
                                        val itemMap = mutableMapOf<String, Any>()
                                        item.keys().forEach { key ->
                                            try {
                                                itemMap[key] = item.get(key)
                                            } catch (e: Exception) {
                                                Log.w(TAG, "⚠️ Error getting key '$key' from JSONObject: ${e.message}")
                                            }
                                        }
                                        itemMap
                                    }
                                    else -> {
                                        Log.w(TAG, "⚠️ Unknown item type in list: ${item?.javaClass?.simpleName}")
                                        null
                                    }
                                }
                            }
                        }
                        is JSONArray -> {
                            val itemsList = mutableListOf<Map<String, Any>>()
                            for (i in 0 until sectionItemsRaw.length()) {
                                try {
                                    val item = sectionItemsRaw.getJSONObject(i)
                                    // Convert JSONObject to Map
                                    val itemMap = mutableMapOf<String, Any>()
                                    item.keys().forEach { key ->
                                        try {
                                            val value = item.get(key)
                                            // Handle nested JSONObjects and JSONArrays
                                            itemMap[key] = when (value) {
                                                is JSONObject -> {
                                                    // Convert nested JSONObject to Map
                                                    val nestedMap = mutableMapOf<String, Any>()
                                                    value.keys().forEach { nestedKey ->
                                                        nestedMap[nestedKey] = value.get(nestedKey)
                                                    }
                                                    nestedMap
                                                }
                                                is JSONArray -> {
                                                    // Keep JSONArray as is for now
                                                    value
                                                }
                                                else -> value
                                            }
                                        } catch (e: Exception) {
                                            Log.w(TAG, "⚠️ Error getting key '$key' from JSONObject: ${e.message}")
                                        }
                                    }
                                    itemsList.add(itemMap)
                                } catch (e: Exception) {
                                    Log.w(TAG, "⚠️ Error parsing item $i in section '$sectionTitle': ${e.message}")
                                }
                            }
                            itemsList
                        }
                        is JSONObject -> {
                            // Single item wrapped in JSONObject
                            val itemMap = mutableMapOf<String, Any>()
                            sectionItemsRaw.keys().forEach { key ->
                                itemMap[key] = sectionItemsRaw.get(key)
                            }
                            listOf(itemMap)
                        }
                        null -> {
                            Log.w(TAG, "⚠️ Items is null in section '$sectionTitle'")
                            emptyList()
                        }
                        else -> {
                            Log.w(TAG, "⚠️ Unknown items format in section '$sectionTitle': ${sectionItemsRaw?.javaClass?.simpleName}")
                            emptyList()
                        }
                    }
                    
                    Log.d(TAG, "📦 Processing section $index: '$sectionTitle' (type: $sectionType, items: ${sectionItems.size})")
                    
                    if (sectionItems.isEmpty()) {
                        Log.w(TAG, "⚠️ Section '$sectionTitle' has no items after parsing")
                    } else {
                        val mappedSection = mapSaavnJsonToSection(sectionTitle, sectionType, sectionItems)
                        if (mappedSection != null) {
                            sections.add(mappedSection)
                            Log.d(TAG, "✅ Added section: '$sectionTitle' with ${mappedSection.items.size} items")
                        } else {
                            Log.w(TAG, "⚠️ Skipped section: '$sectionTitle' (mapping returned null)")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing section $index: ${e.message}", e)
                }
            }
            
            // SECONDARY METHOD: Fetch additional sections in parallel for completeness
            coroutineScope {
                val trendingSongsDeferred = async { 
                    val songs = getTrendingSongs(20)
                    Log.d(TAG, "🎯 Trending Songs = ${songs.size} items")
                    songs
                }
                val featuredPlaylistsDeferred = async { 
                    val playlists = getFeaturedPlaylists(20)
                    Log.d(TAG, "🎯 Featured Playlists = ${playlists.size} items")
                    playlists
                }
                val topChartsDeferred = async { 
                    val charts = getTopCharts(20)
                    Log.d(TAG, "🎯 Top Charts = ${charts.size} items")
                    charts
                }
                val trendingArtistsDeferred = async { 
                    val artists = getTrendingArtists(20)
                    Log.d(TAG, "🎯 Trending Artists = ${artists.size} items")
                    artists
                }
                val newReleasesDeferred = async { 
                    val releases = getNewReleases(20)
                    Log.d(TAG, "🎯 New Releases = ${releases.size} items")
                    releases
                }
                val originalsDeferred = async { 
                    val originals = getSaavnOriginals(20)
                    Log.d(TAG, "🎯 Originals = ${originals.size} items")
                    originals
                }
                val mixesDeferred = async { 
                    val mixes = getRecommendedMixes(20)
                    Log.d(TAG, "🎯 Mixes = ${mixes.size} items")
                    mixes
                }
                val editorsPicksDeferred = async { 
                    val picks = getEditorsPicks(20)
                    Log.d(TAG, "🎯 Editor's Picks = ${picks.size} items")
                    picks
                }
                
                // Wait for all and add sections
                // CRITICAL: Always add sections from individual calls if home data is empty or missing sections
                // This ensures we have content even if home page data fails
                val trendingSongs = trendingSongsDeferred.await()
                if (trendingSongs.isNotEmpty()) {
                    val existingSection = sections.find { it.title.contains("Trending", ignoreCase = true) && it.type == SaavnSectionType.TRENDING }
                    if (existingSection == null || existingSection.items.isEmpty()) {
                        val section = mapSaavnJsonToSection("Trending Now", "songs", trendingSongs)
                        section?.let { 
                            if (existingSection != null) {
                                sections.remove(existingSection)
                            }
                            sections.add(it)
                            Log.d(TAG, "✅ Added Trending Songs section: ${it.items.size} items")
                        }
                    }
                }
                
                val featuredPlaylists = featuredPlaylistsDeferred.await()
                if (featuredPlaylists.isNotEmpty()) {
                    val existingSection = sections.find { it.title.contains("Featured", ignoreCase = true) }
                    if (existingSection == null || existingSection.items.isEmpty()) {
                        val section = mapSaavnJsonToSection("Featured Playlists", "playlists", featuredPlaylists)
                        section?.let { 
                            if (existingSection != null) {
                                sections.remove(existingSection)
                            }
                            sections.add(it)
                            Log.d(TAG, "✅ Added Featured Playlists section: ${it.items.size} items")
                        }
                    }
                }
                
                val topCharts = topChartsDeferred.await()
                if (topCharts.isNotEmpty()) {
                    val existingSection = sections.find { it.title.contains("Top Charts", ignoreCase = true) || it.title.contains("Chart", ignoreCase = true) }
                    if (existingSection == null || existingSection.items.isEmpty()) {
                        val section = mapSaavnJsonToSection("Top Charts", "charts", topCharts)
                        section?.let { 
                            if (existingSection != null) {
                                sections.remove(existingSection)
                            }
                            sections.add(it)
                            Log.d(TAG, "✅ Added Top Charts section: ${it.items.size} items")
                        }
                    }
                }
                
                val trendingArtists = trendingArtistsDeferred.await()
                if (trendingArtists.isNotEmpty()) {
                    val existingSection = sections.find { it.title.contains("Trending Artists", ignoreCase = true) || (it.type == SaavnSectionType.ARTIST && it.title.contains("Artist", ignoreCase = true)) }
                    if (existingSection == null || existingSection.items.isEmpty()) {
                        val section = mapSaavnJsonToSection("Trending Artists", "artists", trendingArtists)
                        section?.let { 
                            if (existingSection != null) {
                                sections.remove(existingSection)
                            }
                            sections.add(it)
                            Log.d(TAG, "✅ Added Trending Artists section: ${it.items.size} items")
                        }
                    }
                }
                
                val newReleases = newReleasesDeferred.await()
                if (newReleases.isNotEmpty()) {
                    val existingSection = sections.find { it.title.contains("New Release", ignoreCase = true) || it.type == SaavnSectionType.NEW_RELEASE }
                    if (existingSection == null || existingSection.items.isEmpty()) {
                        val section = mapSaavnJsonToSection("New Releases", "albums", newReleases)
                        section?.let { 
                            if (existingSection != null) {
                                sections.remove(existingSection)
                            }
                            sections.add(it)
                            Log.d(TAG, "✅ Added New Releases section: ${it.items.size} items")
                        }
                    }
                }
                
                val originals = originalsDeferred.await()
                if (originals.isNotEmpty()) {
                    val existingSection = sections.find { it.title.contains("Original", ignoreCase = true) || it.type == SaavnSectionType.ORIGINALS }
                    if (existingSection == null || existingSection.items.isEmpty()) {
                        val section = mapSaavnJsonToSection("JioSaavn Originals", "playlists", originals)
                        section?.let { 
                            if (existingSection != null) {
                                sections.remove(existingSection)
                            }
                            sections.add(it)
                            Log.d(TAG, "✅ Added Originals section: ${it.items.size} items")
                        }
                    }
                }
                
                val mixes = mixesDeferred.await()
                if (mixes.isNotEmpty()) {
                    val existingSection = sections.find { it.title.contains("Mix", ignoreCase = true) || it.type == SaavnSectionType.MIX }
                    if (existingSection == null || existingSection.items.isEmpty()) {
                        val section = mapSaavnJsonToSection("Recommended Mixes", "mixes", mixes)
                        section?.let { 
                            if (existingSection != null) {
                                sections.remove(existingSection)
                            }
                            sections.add(it)
                            Log.d(TAG, "✅ Added Mixes section: ${it.items.size} items")
                        }
                    }
                }
                
                val editorsPicks = editorsPicksDeferred.await()
                if (editorsPicks.isNotEmpty()) {
                    val existingSection = sections.find { it.title.contains("Editor", ignoreCase = true) || it.title.contains("Pick", ignoreCase = true) }
                    if (existingSection == null || existingSection.items.isEmpty()) {
                        val section = mapSaavnJsonToSection("Editor's Picks", "playlists", editorsPicks)
                        section?.let { 
                            if (existingSection != null) {
                                sections.remove(existingSection)
                            }
                            sections.add(it)
                            Log.d(TAG, "✅ Added Editor's Picks section: ${it.items.size} items")
                        }
                    }
                }
            }
            
            // CRITICAL: If no sections from home data, ensure we have at least some sections from individual calls
            if (sections.isEmpty()) {
                Log.w(TAG, "⚠️ No sections from home data, using individual API calls as fallback")
                // The individual API calls above should have populated sections
                // But if they didn't, we'll still return empty list (better than crashing)
            }
            
            Log.d(TAG, "🎯 Final Saavn Section Count = ${sections.size}")
            if (sections.isEmpty()) {
                Log.e(TAG, "❌ CRITICAL: No sections created! Check API responses and mapping logic.")
            } else {
                sections.forEachIndexed { index, section ->
                    Log.d(TAG, "   ✅ Section $index: '${section.title}' (${section.type}) = ${section.items.size} items")
                }
            }
            
            sections
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching complete home feed: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Fetch trending and editorial sections separately (no duplicates)
     * Used for the recommendation pipeline
     */
    suspend fun fetchTrendingAndEditorialSections(): List<SaavnHomeSection> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📊 Fetching trending and editorial sections...")
            val sections = mutableListOf<SaavnHomeSection>()
            val seenTitles = mutableSetOf<String>() // Track titles to avoid duplicates
            
            coroutineScope {
                // Fetch all sections in parallel
                val trendingSongsDeferred = async { getTrendingSongs(20) }
                val trendingAlbumsDeferred = async { getTopCharts(20) }
                val featuredPlaylistsDeferred = async { getFeaturedPlaylists(20) }
                val trendingPlaylistsDeferred = async { getTrendingPlaylists(20) } // NEW: Trending playlists
                val topChartsDeferred = async { getTopCharts(20) }
                val trendingArtistsDeferred = async { getTrendingArtists(20) }
                val newReleasesDeferred = async { getNewReleases(20) }
                val hotRightNowDeferred = async { getTrendingSongs(15) } // Hot Right Now = latest trending
                val moodPicksDeferred = async { getRecommendedMixes(20) }
                
                // Wait and add sections (checking for duplicates)
                fun addSectionIfNew(section: SaavnHomeSection?) {
                    section?.let {
                        val normalizedTitle = it.title.lowercase().trim()
                        if (!seenTitles.contains(normalizedTitle) && it.items.isNotEmpty()) {
                            seenTitles.add(normalizedTitle)
                            sections.add(it)
                            Log.d(TAG, "✅ Added section: '${it.title}' (${it.items.size} items)")
                        } else {
                            if (seenTitles.contains(normalizedTitle)) {
                                Log.d(TAG, "⚠️ Skipped duplicate section: '${it.title}'")
                            } else {
                                Log.d(TAG, "⚠️ Skipped empty section: '${it.title}' (${it.items.size} items)")
                            }
                        }
                    } ?: run {
                        Log.w(TAG, "⚠️ mapSaavnJsonToSection returned null")
                    }
                }
                
                // Trending Songs
                val trendingSongs = trendingSongsDeferred.await()
                Log.d(TAG, "🎯 Trending Songs = ${trendingSongs.size} items")
                if (trendingSongs.isNotEmpty()) {
                    val section = mapSaavnJsonToSection("Trending Songs", "songs", trendingSongs)
                    Log.d(TAG, "   Mapping result: ${if (section != null) "SUCCESS (${section.items.size} items)" else "FAILED (null)"}")
                    addSectionIfNew(section)
                }
                
                // Trending Albums (from top charts - these are actually albums)
                val trendingAlbums = trendingAlbumsDeferred.await()
                Log.d(TAG, "🎯 Trending Albums = ${trendingAlbums.size} items")
                if (trendingAlbums.isNotEmpty()) {
                    // Check if items are actually albums or playlists
                    val firstItem = trendingAlbums.firstOrNull()
                    val itemType = firstItem?.get("type") as? String ?: ""
                    val actualType = if (itemType == "album" || firstItem?.containsKey("albumid") == true) {
                        "albums"
                    } else {
                        "playlists" // Top charts often returns playlists
                    }
                    val section = mapSaavnJsonToSection("Trending Albums", actualType, trendingAlbums)
                    Log.d(TAG, "   Mapping result: ${if (section != null) "SUCCESS (${section.items.size} items)" else "FAILED (null)"}")
                    addSectionIfNew(section)
                }
                
                // Featured Playlists
                val featuredPlaylists = featuredPlaylistsDeferred.await()
                Log.d(TAG, "🎯 Featured Playlists = ${featuredPlaylists.size} items")
                if (featuredPlaylists.isNotEmpty()) {
                    val section = mapSaavnJsonToSection("Featured Playlists", "playlists", featuredPlaylists)
                    Log.d(TAG, "   Mapping result: ${if (section != null) "SUCCESS (${section.items.size} items)" else "FAILED (null)"}")
                    addSectionIfNew(section)
                }
                
                // Trending Playlists (NEW)
                val trendingPlaylists = trendingPlaylistsDeferred.await()
                Log.d(TAG, "🎯 Trending Playlists = ${trendingPlaylists.size} items")
                if (trendingPlaylists.isNotEmpty()) {
                    val section = mapSaavnJsonToSection("Trending Playlists", "playlists", trendingPlaylists)
                    Log.d(TAG, "   Mapping result: ${if (section != null) "SUCCESS (${section.items.size} items)" else "FAILED (null)"}")
                    addSectionIfNew(section)
                }
                
                // Top Charts (these are playlists from the API)
                val topCharts = topChartsDeferred.await()
                Log.d(TAG, "🎯 Top Charts = ${topCharts.size} items")
                if (topCharts.isNotEmpty()) {
                    // Top charts from API are playlists, map them as playlists but display as charts
                    val section = mapSaavnJsonToSection("Top Charts", "playlists", topCharts)
                    Log.d(TAG, "   Mapping result: ${if (section != null) "SUCCESS (${section.items.size} items)" else "FAILED (null)"}")
                    if (section != null) {
                        // Convert to chart section type
                        val chartSection = SaavnHomeSection(
                            title = section.title,
                            type = SaavnSectionType.CHART,
                            items = section.items.mapIndexed { index, item ->
                                when (item) {
                                    is SaavnPlaylist -> SaavnChartEntry(
                                        id = item.id,
                                        title = item.title,
                                        image = item.image,
                                        subtitle = item.subtitle,
                                        rank = index + 1,
                                        item = item
                                    )
                                    is SaavnAlbum -> SaavnChartEntry(
                                        id = item.id,
                                        title = item.title,
                                        image = item.image,
                                        subtitle = item.subtitle,
                                        rank = index + 1,
                                        item = item
                                    )
                                    else -> item
                                }
                            }
                        )
                        addSectionIfNew(chartSection)
                    } else {
                        addSectionIfNew(section)
                    }
                }
                
                // Trending Artists
                val trendingArtists = trendingArtistsDeferred.await()
                Log.d(TAG, "🎯 Trending Artists = ${trendingArtists.size} items")
                if (trendingArtists.isNotEmpty()) {
                    val section = mapSaavnJsonToSection("Popular Artists", "artists", trendingArtists)
                    Log.d(TAG, "   Mapping result: ${if (section != null) "SUCCESS (${section.items.size} items)" else "FAILED (null)"}")
                    addSectionIfNew(section)
                }
                
                // New Releases
                val newReleases = newReleasesDeferred.await()
                Log.d(TAG, "🎯 New Releases = ${newReleases.size} items")
                if (newReleases.isNotEmpty()) {
                    val section = mapSaavnJsonToSection("New Releases", "albums", newReleases)
                    Log.d(TAG, "   Mapping result: ${if (section != null) "SUCCESS (${section.items.size} items)" else "FAILED (null)"}")
                    addSectionIfNew(section)
                }
                
                // Hot Right Now
                val hotRightNow = hotRightNowDeferred.await()
                Log.d(TAG, "🎯 Hot Right Now = ${hotRightNow.size} items")
                if (hotRightNow.isNotEmpty()) {
                    val section = mapSaavnJsonToSection("Hot Right Now", "songs", hotRightNow)
                    Log.d(TAG, "   Mapping result: ${if (section != null) "SUCCESS (${section.items.size} items)" else "FAILED (null)"}")
                    addSectionIfNew(section)
                }
                
                // Mood Based Picks
                val moodPicks = moodPicksDeferred.await()
                Log.d(TAG, "🎯 Mood Based Picks = ${moodPicks.size} items")
                if (moodPicks.isNotEmpty()) {
                    val section = mapSaavnJsonToSection("Mood Based Picks", "playlists", moodPicks)
                    Log.d(TAG, "   Mapping result: ${if (section != null) "SUCCESS (${section.items.size} items)" else "FAILED (null)"}")
                    addSectionIfNew(section)
                }
            }
            
            Log.d(TAG, "✅ Fetched ${sections.size} trending/editorial sections (no duplicates)")
            sections
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching trending/editorial sections: ${e.message}", e)
            emptyList()
        }
    }
    
    
    // ============ FORMATTING HELPERS ============
    
    private fun formatSongsResponse(songsArray: JSONArray, albumImageUrl: String? = null): List<Map<String, Any>> {
        val songs = mutableListOf<Map<String, Any>>()
        
        for (i in 0 until songsArray.length()) {
            try {
                val item = songsArray.get(i)
                when (item) {
                    is JSONObject -> {
                        // Check if this is a song object directly or nested
                        val song = if (item.has("song") || item.has("title") || item.has("id")) {
                            item
                        } else if (item.has("song") && item.get("song") is JSONObject) {
                            // Song is nested inside
                            item.getJSONObject("song")
                        } else {
                            // Try to extract song data from item
                            item
                        }
                        songs.add(formatSongDetails(song, albumImageUrl))
                    }
                    else -> {
                        Log.w(TAG, "⚠️ Item $i is not a JSONObject, skipping")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting song $i: ${e.message}", e)
            }
        }
        
        return songs
    }
    
    private fun formatSongDetails(song: JSONObject, albumImageUrl: String? = null): Map<String, Any> {
        // Extract song ID - try multiple possible field names
        val songId = song.optString("id", "")
            .takeIf { it.isNotEmpty() }
            ?: song.optString("songid", "")
                .takeIf { it.isNotEmpty() }
            ?: song.optString("song_id", "")
                .takeIf { it.isNotEmpty() }
            ?: song.optString("perma_url", "")
                .takeIf { it.isNotEmpty() }
                ?.takeIf { it.contains("/") }
                ?.split("/")?.lastOrNull()?.takeIf { it.isNotEmpty() }
            ?: ""
        
        // Extract title - try "song" first (Flutter app logic), then "title"
        val title = cleanText(
            song.optString("song", "")
                .takeIf { it.isNotEmpty() }
                ?: song.optString("title", "Unknown")
        )
        
        // Extract artist - following Flutter app logic:
        // 1. Check more_info.music
        // 2. Check more_info.artistMap.primary_artists (array)
        // 3. Check subtitle (fallback)
        // 4. Check primary_artists (root level)
        // 5. Check singers
        val artist = extractArtistName(song)
        
        // Extract album - try "album" or "more_info.album"
        val moreInfo = song.optJSONObject("more_info")
        val album = cleanText(
            song.optString("album", "")
                .takeIf { it.isNotEmpty() }
                ?: moreInfo?.optString("album", "") ?: ""
        )
        
        // Extract duration - try "duration" or "more_info.duration" (in seconds)
        val durationStr = song.optString("duration", "")
            .takeIf { it.isNotEmpty() && it != "0" }
            ?: moreInfo?.optString("duration", "") ?: "0"
        
        // Parse duration (JioSaavn returns in seconds as string)
        val durationSeconds = try {
            durationStr.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
        
        // Extract image - handle array format [{"quality":"50x50","link":"..."}, ...]
        // Check both root level and more_info (following Flutter app pattern)
        // Also log the actual structure for debugging
        val imageField = song.opt("image")
        if (imageField != null) {
            Log.d(TAG, "📸 Image field type: ${imageField.javaClass.simpleName}")
            if (imageField is JSONArray) {
                Log.d(TAG, "📸 Image is array with ${(imageField as JSONArray).length()} items")
            } else if (imageField is String) {
                Log.d(TAG, "📸 Image is string: ${(imageField as String).take(80)}...")
            }
        } else {
            Log.w(TAG, "⚠️ Image field is null in root")
        }
        
        var bestImageUrl = extractImageUrl(song, "image")
        
        // If not found in root, check more_info
        if (bestImageUrl.isEmpty() && moreInfo != null) {
            Log.d(TAG, "📸 Checking more_info for image...")
            bestImageUrl = extractImageUrl(moreInfo, "image")
        }
        
        // Log for debugging
        if (bestImageUrl.isEmpty()) {
            Log.w(TAG, "⚠️ No image found for song: ${song.optString("title", song.optString("song", "Unknown"))}")
            // Try alternative field names
            val altImage = song.optString("image_url", "")
                .takeIf { it.isNotEmpty() }
                ?: song.optString("thumbnail", "")
                    .takeIf { it.isNotEmpty() }
                ?: song.optString("cover_image", "")
                    .takeIf { it.isNotEmpty() }
                ?: ""
            
            if (altImage.isNotEmpty()) {
                Log.d(TAG, "📸 Found image in alternative field: ${altImage.take(80)}...")
                bestImageUrl = formatImageUrl(altImage)
            } else if (albumImageUrl != null && albumImageUrl.isNotEmpty()) {
                // Use album image as fallback (important for top charts songs)
                Log.d(TAG, "📸 Using album image as fallback: ${albumImageUrl.take(80)}...")
                bestImageUrl = formatImageUrl(albumImageUrl)
            }
        } else {
            Log.d(TAG, "✅ Extracted image URL: ${bestImageUrl.take(80)}...")
        }
        
        return mapOf(
            "id" to songId,
            "title" to title,
            "artist" to artist,
            "album" to album,
            "duration" to durationSeconds.toString(), // Store as string for compatibility
            "durationSeconds" to durationSeconds, // Also store as int for easier access
            "year" to song.optString("year", ""),
            "language" to song.optString("language", ""),
            "image" to bestImageUrl, // Always use highest quality
            "imageHigh" to bestImageUrl, // Same as image since we upgrade to highest
            "hasLyrics" to (song.optString("has_lyrics", "false") == "true" || moreInfo?.optString("has_lyrics", "false") == "true"),
            "lyrics" to (song.optString("lyrics", "") ?: moreInfo?.optString("lyrics_snippet", "") ?: ""),
            "url" to decryptUrl(song.optString("encrypted_media_url", "") ?: moreInfo?.optString("encrypted_media_url", "") ?: ""),
            "permaUrl" to song.optString("perma_url", ""),
            "releaseDate" to (song.optString("release_date", "") ?: moreInfo?.optString("release_date", "") ?: ""),
            "playCount" to song.optString("play_count", "0"),
            "copyright" to song.optString("copyright_text", "")
        )
    }
    
    /**
     * Extract artist name from song JSON object following Flutter app logic
     */
    private fun extractArtistName(song: JSONObject): String {
        val moreInfo = song.optJSONObject("more_info")
        
        // 1. Check more_info.music (highest priority in Flutter app)
        if (moreInfo != null) {
            val music = moreInfo.optString("music", "")
            if (music.isNotEmpty()) {
                return cleanText(music)
            }
            
            // 2. Check more_info.artistMap.primary_artists (array)
            val artistMap = moreInfo.optJSONObject("artistMap")
            if (artistMap != null) {
                val primaryArtists = artistMap.optJSONArray("primary_artists")
                if (primaryArtists != null && primaryArtists.length() > 0) {
                    val artistList = mutableListOf<String>()
                    for (i in 0 until primaryArtists.length()) {
                        val artistObj = primaryArtists.optJSONObject(i)
                        if (artistObj != null) {
                            val name = artistObj.optString("name", "")
                            if (name.isNotEmpty()) {
                                artistList.add(cleanText(name))
                            }
                        }
                    }
                    if (artistList.isNotEmpty()) {
                        return artistList.joinToString(", ")
                    }
                }
            }
        }
        
        // 3. Check subtitle (common fallback)
        val subtitle = song.optString("subtitle", "")
        if (subtitle.isNotEmpty()) {
            return cleanText(subtitle)
        }
        
        // 4. Check primary_artists (root level)
        val primaryArtists = song.optString("primary_artists", "")
        if (primaryArtists.isNotEmpty()) {
            return cleanText(primaryArtists)
        }
        
        // 5. Check singers
        val singers = song.optString("singers", "")
        if (singers.isNotEmpty()) {
            return cleanText(singers)
        }
        
        // Final fallback
        return "Unknown Artist"
    }
    
    private fun formatAlbumDetails(album: JSONObject): Map<String, Any> {
        val songsArray = album.optJSONArray("songs") ?: JSONArray()
        val albumImageUrl = extractImageUrl(album, "image")
        
        return mapOf(
            "id" to album.optString("albumid", ""),
            "title" to cleanText(album.optString("title", "Unknown Album")),
            "artist" to cleanText(album.optString("primary_artists", "Unknown Artist")),
            "image" to albumImageUrl,
            "year" to album.optString("year", ""),
            "songCount" to album.optString("song_count", "0"),
            "songs" to formatSongsResponse(songsArray, albumImageUrl), // Pass album image as fallback
            "language" to album.optString("language", ""),
            "releaseDate" to album.optString("release_date", "")
        )
    }
    
    private fun formatPlaylistDetails(playlist: JSONObject, defaultId: String = ""): Map<String, Any> {
        // Try multiple possible field names for playlist ID
        // If all are empty, use the defaultId parameter (passed from getPlaylistDetails)
        val playlistId = playlist.optString("listid", "")
            .takeIf { it.isNotEmpty() && it.isNotBlank() }
            ?: playlist.optString("id", "")
                .takeIf { it.isNotEmpty() && it.isNotBlank() }
            ?: playlist.optString("playlist_id", "")
                .takeIf { it.isNotEmpty() && it.isNotBlank() }
            ?: defaultId
        
        // Try multiple possible field names for title
        // JioSaavn API might have title in different fields or need to extract from subtitle
        var title = playlist.optString("title", "")
            .takeIf { it.isNotEmpty() && it.isNotBlank() }
            ?: playlist.optString("name", "")
                .takeIf { it.isNotEmpty() && it.isNotBlank() }
            ?: playlist.optString("playlist_name", "")
                .takeIf { it.isNotEmpty() && it.isNotBlank() }
        
        // If title is still empty, try subtitle or header_desc
        if (title == null || title.isEmpty() || title.isBlank()) {
            title = playlist.optString("subtitle", "")
                .takeIf { it.isNotEmpty() && it.isNotBlank() }
                ?: playlist.optString("header_desc", "")
                    .takeIf { it.isNotEmpty() && it.isNotBlank() }
                ?: "Unknown Playlist"
        }
        
        Log.d(TAG, "📋 Raw values - id field: '${playlist.optString("id", "")}', title field: '${playlist.optString("title", "")}', subtitle: '${playlist.optString("subtitle", "")}'")
        
        // JioSaavn API uses "list" array for songs, not "songs"
        var songsArray = playlist.optJSONArray("list")
        
        // Check if list is actually an object containing an array
        if (songsArray == null) {
            val listObject = playlist.optJSONObject("list")
            if (listObject != null) {
                // Try to get songs from nested structure
                songsArray = listObject.optJSONArray("songs") 
                    ?: listObject.optJSONArray("items")
                    ?: listObject.optJSONArray("data")
                Log.d(TAG, "📋 List is an object, extracted songs array with ${songsArray?.length() ?: 0} items")
            }
        }
        
        // Fallback to other field names
        if (songsArray == null || songsArray.length() == 0) {
            songsArray = playlist.optJSONArray("songs")
                ?: playlist.optJSONArray("song")
                ?: playlist.optJSONArray("tracks")
                ?: playlist.optJSONArray("items")
                ?: JSONArray()
        }
        
        Log.d(TAG, "📋 Formatting playlist - id: $playlistId, title: $title, songs array length: ${songsArray.length()}")
        
        // Check if list array has objects or if we need to extract from more_info
        val finalSongsArray = if (songsArray.length() == 0) {
            // Try getting songs from more_info.contents first (this is where JioSaavn stores playlist songs)
            val moreInfo = playlist.optJSONObject("more_info")
            if (moreInfo != null) {
                // Check contents - might be array, object, or string
                val contentsValue = moreInfo.opt("contents")
                val contentsArray = when (contentsValue) {
                    is JSONArray -> contentsValue
                    is JSONObject -> {
                        // Contents might be an object containing an array
                        contentsValue.optJSONArray("songs") 
                            ?: contentsValue.optJSONArray("items")
                            ?: contentsValue.optJSONArray("list")
                            ?: null
                    }
                    else -> null
                }
                
                if (contentsArray != null && contentsArray.length() > 0) {
                    Log.d(TAG, "📋 Found ${contentsArray.length()} items in more_info.contents")
                    contentsArray
                } else {
                    // Fallback to more_info.list
                    val moreInfoList = moreInfo.optJSONArray("list")
                    if (moreInfoList != null && moreInfoList.length() > 0) {
                        Log.d(TAG, "📋 Found ${moreInfoList.length()} songs in more_info.list")
                        moreInfoList
                    } else {
                        Log.w(TAG, "⚠️ No songs found in more_info.contents or more_info.list. Contents type: ${contentsValue?.javaClass?.simpleName ?: "null"}")
                        songsArray
                    }
                }
            } else {
                songsArray
            }
        } else {
            songsArray
        }
        
        // Extract playlist image to use as fallback for songs
        val playlistImageUrl = extractImageUrl(playlist, "image")
        val formattedSongs = formatSongsResponse(finalSongsArray, playlistImageUrl) // Pass playlist image as fallback
        Log.d(TAG, "✅ Formatted ${formattedSongs.size} songs from array")
        
        // Get song count from list_count or count songs
        val songCount = if (formattedSongs.isNotEmpty()) {
            formattedSongs.size.toString()
        } else {
            playlist.optString("list_count", playlist.optString("song_count", "0"))
        }
        
        // Get creator from more_info if available
        val creator = playlist.optJSONObject("more_info")?.optString("firstname", "")
            ?: playlist.optString("firstname", "")
            ?: playlist.optString("creator", "JioSaavn")
        
        return mapOf(
            "id" to playlistId,
            "title" to cleanText(title),
            "image" to playlistImageUrl,
            "songCount" to songCount,
            "songs" to formattedSongs,
            "creator" to creator,
            "description" to cleanText(playlist.optString("description", playlist.optString("header_desc", "")))
        )
    }
    
    private fun formatArtistDetails(artist: JSONObject): Map<String, Any> {
        val topSongs = artist.optJSONArray("topSongs") ?: JSONArray()
        
        return mapOf(
            "id" to artist.optString("artistId", ""),
            "name" to cleanText(artist.optString("name", "Unknown Artist")),
            "image" to extractImageUrl(artist, "image"),
            "followerCount" to artist.optString("follower_count", "0"),
            "topSongs" to formatSongsResponse(topSongs),
            "bio" to cleanText(artist.optString("bio", "")),
            "fanCount" to artist.optString("fan_count", "0")
        )
    }
    
    // ============ UTILITY FUNCTIONS ============
    
    /**
     * Decrypt JioSaavn URL using DES decryption (matching Flutter app logic)
     * Flutter code: Uses DES with key '38346591' in ECB mode
     */
    private fun decryptUrl(encryptedUrl: String): String {
        if (encryptedUrl.isEmpty()) return ""
        
        return try {
            // Key from Flutter app: '38346591'
            val keyString = "38346591"
            val keyBytes = keyString.toByteArray(Charsets.UTF_8)
            
            // Create DES key spec and secret key
            val keySpec = DESKeySpec(keyBytes)
            val keyFactory = SecretKeyFactory.getInstance("DES")
            val secretKey = keyFactory.generateSecret(keySpec)
            
            // Initialize cipher for decryption in ECB mode with PKCS5Padding
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            
            // Base64 decode the encrypted URL
            val encryptedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            
            // Decrypt
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            
            // Convert to string and clean up
            var decoded = String(decryptedBytes, Charsets.UTF_8)
            
            // Clean up extension (remove anything after .mp4 or .m4a) - matching Flutter app
            decoded = decoded.replace(Regex("\\.mp4.*"), ".mp4")
            decoded = decoded.replace(Regex("\\.m4a.*"), ".m4a")
            
            // Upgrade to 320kbps quality for better audio
            decoded = decoded.replace("_96.mp4", "_320.mp4")
            decoded = decoded.replace("_160.mp4", "_320.mp4")
            
            // Replace http with https
            decoded = decoded.replace("http://", "https://")
            
            Log.d(TAG, "✅ DES decryption successful, URL length: ${decoded.length}")
            decoded
        } catch (e: Exception) {
            Log.e(TAG, "❌ DES decryption failed: ${e.message}", e)
            // Fallback: try to at least fix quality and protocol
            encryptedUrl
                .replace("_96.mp4", "_320.mp4")
                .replace("_160.mp4", "_320.mp4")
                .replace("http://", "https://")
        }
    }
    
    /**
     * Extract image URL from JioSaavn JSON response
     * Handles both array format: [{"quality":"50x50","link":"..."}, ...]
     * and string format (for backward compatibility)
     * Always returns highest quality (500x500) or last item in array
     */
    private fun extractImageUrl(jsonObject: JSONObject, fieldName: String = "image"): String {
        val imageField = jsonObject.opt(fieldName) ?: return ""
        
        // Check if image is an array (new format)
        if (imageField is JSONArray) {
            val imageArray = imageField
            if (imageArray.length() == 0) {
                Log.d(TAG, "📸 Image array is empty for field: $fieldName")
                return ""
            }
            
            Log.d(TAG, "📸 Found image array with ${imageArray.length()} items")
            
            // Try to find 500x500 quality first
            var bestLink = ""
            var bestQuality = 0
            
            for (i in 0 until imageArray.length()) {
                try {
                    val item = imageArray.get(i)
                    var quality = ""
                    var link = ""
                    
                    // Handle both object format {"quality":"...", "link":"..."} and string format
                    when (item) {
                        is JSONObject -> {
                            quality = item.optString("quality", "")
                            link = item.optString("link", "")
                            // Also try "url" as alternative field name
                            if (link.isEmpty()) {
                                link = item.optString("url", "")
                            }
                        }
                        is String -> {
                            // If it's a direct string URL, use it
                            link = item
                            quality = "unknown"
                        }
                    }
                    
                    Log.d(TAG, "📸 Image[$i]: quality=$quality, link=${link.take(60)}...")
                    
                    if (link.isNotEmpty()) {
                        // Calculate quality score (prefer 500x500)
                        val qualityScore = when {
                            quality == "500x500" -> 1000
                            quality.contains("500") -> 500
                            quality.contains("300") -> 300
                            quality.contains("250") -> 250
                            quality.contains("150") -> 150
                            quality.contains("50") -> 50
                            else -> 0
                        }
                        
                        if (qualityScore > bestQuality) {
                            bestQuality = qualityScore
                            bestLink = link
                        }
                        
                        // If we found 500x500, use it immediately
                        if (quality == "500x500") {
                            val finalUrl = formatImageUrl(link)
                            Log.d(TAG, "✅ Found 500x500 image: ${finalUrl.take(80)}...")
                            return finalUrl
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing image array item $i: ${e.message}")
                }
            }
            
            // Use the best quality link found, or last item as fallback
            if (bestLink.isNotEmpty()) {
                val finalUrl = formatImageUrl(bestLink)
                Log.d(TAG, "✅ Using best quality image (score=$bestQuality): ${finalUrl.take(80)}...")
                return finalUrl
            }
            
            // Last resort: try to get last item
            try {
                val lastItem = imageArray.get(imageArray.length() - 1)
                val link = when (lastItem) {
                    is JSONObject -> lastItem.optString("link", "").takeIf { it.isNotEmpty() }
                        ?: lastItem.optString("url", "")
                    is String -> lastItem
                    else -> ""
                }
                if (link.isNotEmpty()) {
                    val finalUrl = formatImageUrl(link)
                    Log.d(TAG, "✅ Using last image from array: ${finalUrl.take(80)}...")
                    return finalUrl
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error getting last image from array: ${e.message}")
            }
            
            Log.w(TAG, "⚠️ No valid image link found in array")
            return ""
        }
        
        // Fallback: treat as string (backward compatibility)
        val imageString = jsonObject.optString(fieldName, "")
        if (imageString.isNotEmpty()) {
            val finalUrl = formatImageUrl(imageString)
            Log.d(TAG, "📸 Found image as string: ${finalUrl.take(80)}...")
            return finalUrl
        }
        
        Log.d(TAG, "⚠️ Image field '$fieldName' is empty or null")
        return ""
    }
    
    /**
     * Format image URL to high quality (matching Flutter app's getImageUrl function exactly)
     * Flutter logic: 
     * - trim()
     * - replaceAll('http:', 'https:')
     * - replaceAll('50x50', '500x500')
     * - replaceAll('150x150', '500x500')
     */
    private fun formatImageUrl(imageUrl: String, quality: String = "500x500"): String {
        if (imageUrl.isEmpty()) return ""
        
        var processedUrl = imageUrl.trim()
        
        // Convert HTTP to HTTPS (matching Flutter app exactly)
        // Flutter does: replaceAll('http:', 'https:') - handles both http:// and http:
        processedUrl = processedUrl.replace("http://", "https://")
        processedUrl = processedUrl.replace("http:", "https:")
        
        // Convert low quality sizes to high quality (matching Flutter app exactly)
        // Flutter does: replaceAll('50x50', '500x500').replaceAll('150x150', '500x500')
        processedUrl = processedUrl
            .replace("50x50", quality)  // Must be before 150x150 to avoid partial matches
            .replace("150x150", quality)
        
        // Also handle underscore variants for better compatibility
        processedUrl = processedUrl
            .replace("_50x50.jpg", "_500x500.jpg")
            .replace("_50x50.png", "_500x500.png")
            .replace("_150x150.jpg", "_500x500.jpg")
            .replace("_150x150.png", "_500x500.png")
            .replace("_250x250.jpg", "_500x500.jpg")
            .replace("_250x250.png", "_500x500.png")
            .replace("_300x300.jpg", "_500x500.jpg")
            .replace("_300x300.png", "_500x500.png")
        
        // If URL contains size pattern but not 500x500, upgrade it
        if (processedUrl.contains(Regex("\\d+x\\d+")) && !processedUrl.contains("500x500")) {
            processedUrl = processedUrl.replace(Regex("\\d+x\\d+"), quality)
        }
        
        return processedUrl
    }
    
    /**
     * Clean HTML entities from text
     */
    private fun cleanText(text: String): String {
        return text
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }
    
    /**
     * Format home page data sections
     */
    private fun formatHomePageData(data: JSONObject): Map<String, Any> {
        val sections = mutableListOf<Map<String, Any>>()
        
        // Extract featured playlists
        val featuredPlaylists = data.optJSONArray("featured_playlists") ?: JSONArray()
        if (featuredPlaylists.length() > 0) {
            sections.add(mapOf(
                "title" to "Featured Playlists",
                "type" to "playlists",
                "items" to formatPlaylistsArray(featuredPlaylists)
            ))
        }
        
        // Extract new releases
        val newReleases = data.optJSONArray("new_trending") ?: JSONArray()
        if (newReleases.length() > 0) {
            sections.add(mapOf(
                "title" to "New Releases",
                "type" to "albums",
                "items" to formatAlbumsArray(newReleases)
            ))
        }
        
        // Extract top charts
        val topCharts = data.optJSONArray("charts") ?: JSONArray()
        if (topCharts.length() > 0) {
            sections.add(mapOf(
                "title" to "Top Charts",
                "type" to "playlists",
                "items" to formatPlaylistsArray(topCharts)
            ))
        }
        
        // Extract trending artists
        val trendingArtists = data.optJSONArray("top_artists") 
            ?: data.optJSONArray("artists")
            ?: data.optJSONArray("trending_artists") 
            ?: JSONArray()
        if (trendingArtists.length() > 0) {
            sections.add(mapOf(
                "title" to "Trending Artists",
                "type" to "artists",
                "items" to formatArtistsArray(trendingArtists)
            ))
        }
        
        return mapOf(
            "sections" to sections,
            "error" to ""
        )
    }
    
    /**
     * Format playlists array from home data
     */
    private fun formatPlaylistsArray(playlistsArray: JSONArray): List<Map<String, Any>> {
        val playlists = mutableListOf<Map<String, Any>>()
        for (i in 0 until playlistsArray.length()) {
            try {
                val playlist = playlistsArray.getJSONObject(i)
                playlists.add(formatPlaylistItem(playlist))
            } catch (e: Exception) {
                Log.w(TAG, "Error formatting playlist: ${e.message}")
            }
        }
        return playlists
    }
    
    /**
     * Format single playlist item
     */
    private fun formatPlaylistItem(playlist: JSONObject): Map<String, Any> {
        return mapOf(
            "id" to playlist.optString("listid", playlist.optString("id", "")),
            "title" to cleanText(playlist.optString("title", "Unknown Playlist")),
            "image" to extractImageUrl(playlist, "image"),
            "songCount" to playlist.optString("song_count", "0"),
            "subtitle" to cleanText(playlist.optString("subtitle", "")),
            "type" to "playlist"
        )
    }
    
    /**
     * Format albums array from home data
     */
    private fun formatAlbumsArray(albumsArray: JSONArray): List<Map<String, Any>> {
        val albums = mutableListOf<Map<String, Any>>()
        for (i in 0 until albumsArray.length()) {
            try {
                val album = albumsArray.getJSONObject(i)
                val artist = cleanText(album.optString("primary_artists", album.optString("artist", "Unknown Artist")))
                albums.add(mapOf(
                    "id" to album.optString("albumid", album.optString("id", "")),
                    "title" to cleanText(album.optString("title", "Unknown Album")),
                    "artist" to artist,
                    "subtitle" to artist, // Add subtitle for UI display
                    "image" to extractImageUrl(album, "image"),
                    "songCount" to album.optString("song_count", "0"),
                    "type" to "album"
                ))
            } catch (e: Exception) {
                Log.w(TAG, "Error formatting album: ${e.message}")
            }
        }
        return albums
    }
    
    /**
     * Format artists array 
     */
    private fun formatArtistsArray(artistsArray: JSONArray): List<Map<String, Any>> {
        val artists = mutableListOf<Map<String, Any>>()
        for (i in 0 until artistsArray.length()) {
            try {
                val artist = artistsArray.getJSONObject(i)
                
                // Extract all possible image sources
                val imageUrl = artist.optString("image_url", "")
                val imageField = artist.opt("image")
                val imagesField = artist.opt("images")
                
                // Convert image field to appropriate format
                val imageValue = when {
                    imageField is org.json.JSONArray -> {
                        // Convert JSONArray to List<Map<String, Any>> or List<String>
                        val imageList = mutableListOf<Any>()
                        for (j in 0 until imageField.length()) {
                            val item = imageField.get(j)
                            when (item) {
                                is org.json.JSONObject -> {
                                    val map = mutableMapOf<String, Any>()
                                    item.keys().forEach { key ->
                                        map[key] = item.get(key)
                                    }
                                    imageList.add(map)
                                }
                                is String -> imageList.add(item)
                                else -> {}
                            }
                        }
                        imageList
                    }
                    imageField is String -> imageField
                    else -> extractImageUrl(artist, "image")
                }
                
                // Convert images field to List if it's an array
                val imagesValue = if (imagesField is org.json.JSONArray) {
                    val imagesList = mutableListOf<Any>()
                    for (j in 0 until imagesField.length()) {
                        val item = imagesField.get(j)
                        when (item) {
                            is org.json.JSONObject -> {
                                val map = mutableMapOf<String, Any>()
                                item.keys().forEach { key ->
                                    map[key] = item.get(key)
                                }
                                imagesList.add(map)
                            }
                            is String -> imagesList.add(item)
                            else -> {}
                        }
                    }
                    imagesList
                } else null
                
                // Extract final image URL string (not List) for better compatibility
                val finalImageUrl = when {
                    imageUrl.isNotEmpty() -> imageUrl
                    imageValue is String && imageValue.isNotEmpty() -> imageValue
                    imageValue is List<*> && imageValue.isNotEmpty() -> {
                        // Extract first valid string from list
                        imageValue.firstNotNullOfOrNull { item ->
                            when (item) {
                                is String -> item.takeIf { it.isNotEmpty() }
                                is Map<*, *> -> {
                                    @Suppress("UNCHECKED_CAST")
                                    val map = item as? Map<String, Any>
                                    (map?.get("link") as? String)?.takeIf { it.isNotEmpty() }
                                        ?: (map?.get("url") as? String)?.takeIf { it.isNotEmpty() }
                                }
                                else -> null
                            }
                        } ?: ""
                    }
                    imagesValue is List<*> && imagesValue.isNotEmpty() -> {
                        imagesValue.firstNotNullOfOrNull { item ->
                            when (item) {
                                is String -> item.takeIf { it.isNotEmpty() }
                                is Map<*, *> -> {
                                    @Suppress("UNCHECKED_CAST")
                                    val map = item as? Map<String, Any>
                                    (map?.get("link") as? String)?.takeIf { it.isNotEmpty() }
                                        ?: (map?.get("url") as? String)?.takeIf { it.isNotEmpty() }
                                }
                                else -> null
                            }
                        } ?: ""
                    }
                    else -> extractImageUrl(artist, "image")
                }
                
                val artistMap = mutableMapOf<String, Any>(
                    "id" to artist.optString("artistid", artist.optString("id", "")),
                    "name" to cleanText(artist.optString("name", artist.optString("title", "Unknown Artist"))),
                    "subtitle" to cleanText(artist.optString("subtitle", artist.optString("role", ""))),
                    "type" to "artist"
                )
                
                // Add image sources - prioritize single URL string for compatibility
                if (imageUrl.isNotEmpty()) {
                    artistMap["image_url"] = imageUrl
                }
                artistMap["image"] = finalImageUrl  // Use extracted string URL
                if (imagesValue != null) {
                    artistMap["images"] = imagesValue  // Keep original for fallback
                }
                
                Log.d(TAG, "📸 Artist '${artistMap["name"]}' image extracted: ${if (finalImageUrl.isNotEmpty()) "✅ ($finalImageUrl)" else "❌"}")
                
                artists.add(artistMap)
            } catch (e: Exception) {
                Log.w(TAG, "Error formatting artist: ${e.message}")
            }
        }
        return artists
    }
}
