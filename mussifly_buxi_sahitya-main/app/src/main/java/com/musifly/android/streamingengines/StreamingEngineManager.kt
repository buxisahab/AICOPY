package com.musifly.android.streamingengines

import android.util.Log
import com.musifly.android.streamingengines.models.SourceEngine
import com.musifly.android.streamingengines.models.StreamingSong
import com.musifly.android.streamingengines.saavn.SaavnAPI
import com.musifly.android.streamingengines.spotify.SpotifyAPI
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Streaming Engine Manager
 * 
 * Central manager for all streaming engines (JioSaavn, YouTube, etc.)
 * Provides unified interface for searching and streaming music
 */
class StreamingEngineManager {
    
    companion object {
        private const val TAG = "StreamingEngineManager"
        
        // Singleton instance
        @Volatile
        private var instance: StreamingEngineManager? = null
        
        fun getInstance(): StreamingEngineManager {
            return instance ?: synchronized(this) {
                instance ?: StreamingEngineManager().also { instance = it }
            }
        }
    }
    
    private val saavnAPI = SaavnAPI()
    private val spotifyAPI = SpotifyAPI()
    private var countryCode: String = "IN" // Default to India
    
    // Engine enable/disable state (set from preferences)
    private var enabledEngines: Set<SourceEngine> = setOf(
        SourceEngine.JIOSAAVN,
        SourceEngine.YOUTUBE_MUSIC,
        SourceEngine.YOUTUBE_VIDEO,
        SourceEngine.SPOTIFY
    ) // Default: all enabled
    
    /**
     * Set country code for engine selection
     */
    fun setCountryCode(code: String) {
        countryCode = code
    }
    
    /**
     * Set enabled engines based on user preferences
     * @param jioSaavnEnabled Whether JioSaavn is enabled
     * @param youtubeMusicEnabled Whether YouTube Music is enabled
     * @param youtubeVideoEnabled Whether YouTube Video is enabled
     * @param spotifyEnabled Whether Spotify is enabled
     */
    fun setEnabledEngines(
        jioSaavnEnabled: Boolean = true,
        youtubeMusicEnabled: Boolean = true,
        youtubeVideoEnabled: Boolean = true,
        spotifyEnabled: Boolean = true
    ) {
        val enabled = mutableSetOf<SourceEngine>()
        
        if (jioSaavnEnabled) enabled.add(SourceEngine.JIOSAAVN)
        if (youtubeMusicEnabled) enabled.add(SourceEngine.YOUTUBE_MUSIC)
        if (youtubeVideoEnabled) enabled.add(SourceEngine.YOUTUBE_VIDEO)
        if (spotifyEnabled) enabled.add(SourceEngine.SPOTIFY)
        
        // Always keep INNERTUBE enabled as it's used internally
        enabled.add(SourceEngine.INNERTUBE)
        
        enabledEngines = enabled
        Log.d(TAG, "Enabled engines updated: $enabledEngines")
    }
    
    /**
     * Get available engines for current country (filtered by user preferences)
     */
    fun getAvailableEngines(): List<SourceEngine> {
        val regionAvailableEngines = SourceEngine.getAvailableEngines(countryCode)
        // Filter by user preferences
        return regionAvailableEngines.filter { it in enabledEngines }
    }
    
    /**
     * Check if a specific engine is enabled
     */
    fun isEngineEnabled(engine: SourceEngine): Boolean {
        return engine in enabledEngines
    }
    
    /**
     * Search songs from JioSaavn
     * @param query Search query
     * @param limit Number of results
     * @return List of StreamingSong objects
     */
    suspend fun searchSongsFromSaavn(
        query: String,
        limit: Int = 20
    ): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val results = saavnAPI.fetchSearchResults(query, limit)
            val songsMapList = results["songs"] as? List<Map<String, Any>> ?: emptyList()
            
            songsMapList.map { songMap ->
                StreamingSong.fromMap(songMap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "JioSaavn search failed: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get stream URL from JioSaavn (320kbps)
     * @param songId JioSaavn song ID
     * @return Stream URL or null
     */
    suspend fun getSaavnStreamUrl(songId: String): String? = withContext(Dispatchers.IO) {
        try {
            saavnAPI.getSongUrl(songId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get stream URL: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get song details from JioSaavn
     */
    suspend fun getSaavnSongDetails(songId: String): StreamingSong? = withContext(Dispatchers.IO) {
        try {
            val details = saavnAPI.getSongDetails(songId)
            if (details["error"] == null) {
                StreamingSong.fromMap(details as Map<String, Any>)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get song details: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get album details from JioSaavn
     */
    suspend fun getSaavnAlbumDetails(albumId: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        try {
            saavnAPI.getAlbumDetails(albumId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get album details: ${e.message}", e)
            mapOf("error" to e.message)
        }
    }
    
    /**
     * Get playlist details from JioSaavn
     */
    suspend fun getSaavnPlaylistDetails(playlistId: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        try {
            saavnAPI.getPlaylistDetails(playlistId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get playlist details: ${e.message}", e)
            mapOf("error" to e.message)
        }
    }
    
    /**
     * Get artist details from JioSaavn
     */
    suspend fun getSaavnArtistDetails(artistId: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        try {
            saavnAPI.getArtistDetails(artistId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get artist details: ${e.message}", e)
            mapOf("error" to e.message)
        }
    }
    
    /**
     * Get recommendations for a song
     */
    suspend fun getSaavnRecommendations(songId: String): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val recommendations = saavnAPI.getSongRecommendations(songId)
            recommendations.map { StreamingSong.fromMap(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get recommendations: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Universal search method
     * Searches across enabled engines based on availability
     */
    suspend fun searchSongs(
        query: String,
        limit: Int = 20,
        preferredEngine: SourceEngine? = null
    ): List<StreamingSong> = withContext(Dispatchers.IO) {
        val enginesToTry = if (preferredEngine != null && isEngineEnabled(preferredEngine)) {
            listOf(preferredEngine)
        } else {
            getAvailableEngines().filter { it != SourceEngine.INNERTUBE }
        }
        
        if (enginesToTry.isEmpty()) {
            Log.w(TAG, "No engines enabled, cannot search")
            return@withContext emptyList()
        }
        
        // Try engines in order until we get results
        for (engine in enginesToTry) {
            try {
                when (engine) {
                    SourceEngine.JIOSAAVN -> {
                        val results = searchSongsFromSaavn(query, limit)
                        if (results.isNotEmpty()) {
                            Log.d(TAG, "Search successful using $engine")
                            return@withContext results
                        }
                    }
                    SourceEngine.YOUTUBE_MUSIC -> {
                        val results = searchSongsFromYouTubeMusic(query, limit)
                        if (results.isNotEmpty()) {
                            Log.d(TAG, "Search successful using $engine")
                            return@withContext results
                        }
                    }
                    SourceEngine.YOUTUBE_VIDEO -> {
                        val results = searchSongsFromYouTubeVideo(query, limit)
                        if (results.isNotEmpty()) {
                            Log.d(TAG, "Search successful using $engine")
                            return@withContext results
                        }
                    }
                    SourceEngine.SPOTIFY -> {
                        val results = searchSongsFromSpotify(query, limit)
                        if (results.isNotEmpty()) {
                            Log.d(TAG, "Search successful using $engine")
                            return@withContext results
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Search failed for $engine: ${e.message}")
                continue
            }
        }
        
        Log.w(TAG, "All engines failed or returned no results")
        emptyList()
    }
    
    /**
     * Check if JioSaavn is available in current region
     */
    fun isSaavnAvailable(): Boolean {
        val availableEngines = getAvailableEngines()
        return availableEngines.contains(SourceEngine.JIOSAAVN)
    }
    
    /**
     * Search songs from YouTube Music
     * @param query Search query
     * @param limit Number of results
     * @return List of StreamingSong objects
     */
    private suspend fun searchSongsFromYouTubeMusic(
        query: String,
        limit: Int = 20
    ): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
            result.fold(
                onSuccess = { searchResult ->
                    val songs = searchResult.items.filterIsInstance<SongItem>().take(limit)
                    songs.map { songItem ->
                        StreamingSong(
                            id = songItem.id,
                            title = songItem.title,
                            artist = songItem.artists.joinToString(", ") { it.name },
                            album = songItem.album?.name ?: "",
                            duration = (songItem.duration ?: 0).toString(),
                            imageUrl = songItem.thumbnail,
                            imageHighUrl = songItem.thumbnail,
                            permaUrl = songItem.shareLink,
                            streamUrl = "", // Will be fetched via YTPlayerUtils when playing
                            source = SourceEngine.YOUTUBE_MUSIC
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "YouTube Music search failed: ${e.message}", e)
                    emptyList()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "YouTube Music search error: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Search songs from YouTube Video
     * @param query Search query
     * @param limit Number of results
     * @return List of StreamingSong objects
     */
    private suspend fun searchSongsFromYouTubeVideo(
        query: String,
        limit: Int = 20
    ): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO)
            result.fold(
                onSuccess = { searchResult ->
                    val songs = searchResult.items.filterIsInstance<SongItem>().take(limit)
                    songs.map { songItem ->
                        StreamingSong(
                            id = songItem.id,
                            title = songItem.title,
                            artist = songItem.artists.joinToString(", ") { it.name },
                            album = songItem.album?.name ?: "",
                            duration = (songItem.duration ?: 0).toString(),
                            imageUrl = songItem.thumbnail,
                            imageHighUrl = songItem.thumbnail,
                            permaUrl = songItem.shareLink,
                            streamUrl = "", // Will be fetched via YTPlayerUtils when playing
                            source = SourceEngine.YOUTUBE_VIDEO
                        )
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "YouTube Video search failed: ${e.message}", e)
                    emptyList()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "YouTube Video search error: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Search songs from Spotify
     * @param query Search query
     * @param limit Number of results
     * @return List of StreamingSong objects
     */
    private suspend fun searchSongsFromSpotify(
        query: String,
        limit: Int = 20
    ): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val results = spotifyAPI.searchTracks(query, limit)
            val tracksMapList = results["tracks"] as? List<Map<String, Any>> ?: emptyList()
            
            tracksMapList.map { trackMap ->
                val songMap = mutableMapOf<String, Any>().apply {
                    // Convert nullable map to non-nullable, filtering out nulls
                    trackMap.forEach { (key, value) ->
                        put(key, value ?: "")
                    }
                    put("source", SourceEngine.SPOTIFY.name)
                }
                StreamingSong.fromMap(songMap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Spotify search failed: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get Spotify track details
     */
    suspend fun getSpotifyTrackDetails(trackId: String): StreamingSong? = withContext(Dispatchers.IO) {
        try {
            val details = spotifyAPI.getTrackDetails(trackId)
            if (details["error"] == null) {
                val songMap = mutableMapOf<String, Any>().apply {
                    putAll(details)
                    put("source", SourceEngine.SPOTIFY.name)
                }
                StreamingSong.fromMap(songMap)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Spotify track details: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get Spotify album details
     */
    suspend fun getSpotifyAlbumDetails(albumId: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        try {
            spotifyAPI.getAlbumDetails(albumId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Spotify album details: ${e.message}", e)
            mapOf("error" to e.message)
        }
    }
    
    /**
     * Get Spotify playlist details
     */
    suspend fun getSpotifyPlaylistDetails(playlistId: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        try {
            spotifyAPI.getPlaylistDetails(playlistId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Spotify playlist details: ${e.message}", e)
            mapOf("error" to e.message)
        }
    }
}
