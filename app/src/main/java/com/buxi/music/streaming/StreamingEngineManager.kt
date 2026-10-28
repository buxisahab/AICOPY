package com.buxi.music.streaming

import android.util.Log
import com.buxi.music.jiosaavn.JioSaavn
import com.buxi.music.jiosaavn.JioSaavnDecryption
import com.buxi.music.innertube.YouTube
import com.buxi.music.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Streaming Engine Manager
 * Adapted for AI Music app with existing JioSaavn module
 */
class StreamingEngineManager {
    
    companion object {
        private const val TAG = "StreamingEngineManager"
        @Volatile private var instance: StreamingEngineManager? = null
        fun getInstance(): StreamingEngineManager = instance ?: synchronized(this) {
            instance ?: StreamingEngineManager().also { instance = it }
        }
    }
    
    private var countryCode: String = "IN"
    
    fun setCountryCode(code: String) { countryCode = code }
    
    suspend fun searchSongsFromSaavn(query: String, limit: Int = 20): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val result = JioSaavn.searchSongs(query, limit = limit)
            result.getOrNull()?.results?.map { song ->
                StreamingSong(
                    id = song.id,
                    title = song.song ?: "Unknown",
                    artist = song.primary_artists ?: "Unknown",
                    album = song.album ?: "",
                    duration = song.duration ?: "0",
                    imageUrl = song.image ?: "",
                    imageHighUrl = song.image ?: "",
                    source = SourceEngine.JIOSAAVN
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Saavn search failed", e)
            emptyList()
        }
    }
    
    suspend fun getSaavnStreamUrl(songId: String): String? = withContext(Dispatchers.IO) {
        try {
            val result = JioSaavn.getSongDetails(songId)
            val song = result.getOrNull()
            if (song?.encrypted_media_url != null) {
                val decrypted = JioSaavnDecryption.decryptMediaUrl(song.encrypted_media_url!!)
                if (decrypted.isNotEmpty()) {
                    JioSaavnDecryption.getDownloadLink(decrypted)
                } else null
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Saavn stream URL", e)
            null
        }
    }
    
    suspend fun searchSongs(query: String, limit: Int = 20): List<StreamingSong> = withContext(Dispatchers.IO) {
        if (countryCode == "IN") {
            val saavnResults = searchSongsFromSaavn(query, limit)
            if (saavnResults.isNotEmpty()) return@withContext saavnResults
        }
        searchSongsFromYouTube(query, limit)
    }
    
    private suspend fun searchSongsFromYouTube(query: String, limit: Int = 20): List<StreamingSong> = withContext(Dispatchers.IO) {
        try {
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
            result.fold(
                onSuccess = { res ->
                    res.items.filterIsInstance<SongItem>().take(limit).map { song ->
                        StreamingSong(
                            id = song.id, title = song.title,
                            artist = song.artists.joinToString(", ") { it.name },
                            imageUrl = song.thumbnail, imageHighUrl = song.thumbnail,
                            source = SourceEngine.YOUTUBE_MUSIC
                        )
                    }
                },
                onFailure = { emptyList() }
            )
        } catch (e: Exception) { emptyList() }
    }
}
