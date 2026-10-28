package com.musifly.android.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.innertube.YouTube
import com.musifly.innertube.pages.ArtistPage
import com.musifly.android.db.MusicDatabase
import com.musifly.android.streamingengines.saavn.SaavnAPI
import com.musifly.android.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class ArtistViewModel @Inject constructor(
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
    val saavnAPI: SaavnAPI,
) : ViewModel() {
    val artistId = savedStateHandle.get<String>("artistId")!!
    var artistPage by mutableStateOf<ArtistPage?>(null)
    
    // JioSaavn artist data
    val saavnArtistData = MutableStateFlow<Map<String, Any>?>(null)
    val isSaavnArtist = artistId.startsWith("saavn:")
    
    val libraryArtist = database.artist(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    val librarySongs = database.artistSongsPreview(artistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        if (isSaavnArtist) {
            fetchArtistFromJioSaavn()
        } else {
            fetchArtistsFromYTM()
        }
    }

    private fun fetchArtistsFromYTM() {
        viewModelScope.launch {
            YouTube.artist(artistId)
                .onSuccess {
                    artistPage = it
                }.onFailure {
                    reportException(it)
                }
        }
    }
    
    private fun fetchArtistFromJioSaavn() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val actualId = artistId.removePrefix("saavn:")
                Log.d("ArtistViewModel", "📥 Fetching JioSaavn artist: $actualId")
                
                // Use getArtistMoreMusic for songs, and getArtistDetails for metadata
                val artistMoreMusic = saavnAPI.getArtistMoreMusic(actualId)
                val artistDetails = saavnAPI.getArtistDetails(actualId)
                
                // Combine data: use songs from getArtistMoreMusic, metadata from getArtistDetails
                val combinedData = mutableMapOf<String, Any>()
                
                // Get songs from getArtistMoreMusic (primary source)
                val songs = artistMoreMusic["songs"] as? List<Map<String, Any>> ?: emptyList()
                Log.d("ArtistViewModel", "🎵 Found ${songs.size} songs from getArtistMoreMusic")
                
                // If no songs from getArtistMoreMusic, try getArtistDetails
                val finalSongs = if (songs.isEmpty()) {
                    val detailSongs = artistDetails["topSongs"] as? List<Map<String, Any>> ?: emptyList()
                    Log.d("ArtistViewModel", "🎵 Found ${detailSongs.size} songs from getArtistDetails (fallback)")
                    detailSongs
                } else {
                    songs
                }
                
                // Merge metadata from getArtistDetails (has more complete info)
                if (artistDetails["error"] == null || (artistDetails["error"] as? String)?.isEmpty() == true) {
                    // Convert Map<String, Any> to Map<String, Any> (already compatible)
                    combinedData.putAll(artistDetails)
                    // Override songs with the ones from getArtistMoreMusic
                    combinedData["topSongs"] = finalSongs
                    combinedData["songs"] = finalSongs
                    
                    saavnArtistData.value = combinedData
                    Log.d("ArtistViewModel", "✅ JioSaavn artist loaded: ${combinedData["name"]} with ${finalSongs.size} songs")
                } else if (artistMoreMusic["error"] == null || (artistMoreMusic["error"] as? String)?.isEmpty() == true) {
                    // Fallback: use getArtistMoreMusic data if getArtistDetails failed
                    // Convert Map<String, Any?> to Map<String, Any> by filtering out nulls and converting
                    artistMoreMusic.forEach { (key, value) ->
                        if (value != null) {
                            combinedData[key] = value
                        }
                    }
                    combinedData["topSongs"] = finalSongs
                    saavnArtistData.value = combinedData
                    Log.d("ArtistViewModel", "✅ JioSaavn artist loaded (from getArtistMoreMusic): ${combinedData["name"]} with ${finalSongs.size} songs")
                } else {
                    Log.e("ArtistViewModel", "❌ Failed to load JioSaavn artist: ${artistDetails["error"]} / ${artistMoreMusic["error"]}")
                    saavnArtistData.value = null
                }
            } catch (e: Exception) {
                Log.e("ArtistViewModel", "❌ Error fetching JioSaavn artist: ${e.message}", e)
                reportException(e)
                saavnArtistData.value = null
            }
        }
    }
}