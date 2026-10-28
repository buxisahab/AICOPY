package com.musifly.android.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.PlaylistItem
import com.musifly.innertube.models.SongItem
import com.musifly.innertube.utils.completed
import com.musifly.android.db.MusicDatabase
import com.musifly.android.streamingengines.saavn.SaavnAPI
import com.musifly.android.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class OnlinePlaylistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    database: MusicDatabase,
    val saavnAPI: SaavnAPI,
) : ViewModel() {
    private val playlistId = savedStateHandle.get<String>("playlistId")!!

    val playlist = MutableStateFlow<PlaylistItem?>(null)
    val playlistSongs = MutableStateFlow<List<SongItem>>(emptyList())
    val dbPlaylist = database.playlistByBrowseId(playlistId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    
    // For JioSaavn playlists (not stored in database)
    val saavnPlaylistData = MutableStateFlow<Map<String, Any?>?>(null)
    val saavnPlaylistSongs = MutableStateFlow<List<Map<String, Any>>>(emptyList())

    init {
        Log.d("OnlinePlaylistViewModel", "🔷 OnlinePlaylistViewModel initialized with playlistId: $playlistId")
        viewModelScope.launch(Dispatchers.IO) {
            // Check if this is a JioSaavn playlist
            if (playlistId.startsWith("saavn:")) {
                val actualId = playlistId.removePrefix("saavn:")
                Log.d("OnlinePlaylistViewModel", "✅ Detected JioSaavn playlist, actual ID: $actualId")
                loadJioSaavnPlaylist(actualId)
            } else {
                Log.d("OnlinePlaylistViewModel", "📺 Loading YouTube playlist: $playlistId")
                YouTube.playlist(playlistId).completed()
                    .onSuccess { playlistPage ->
                        playlist.value = playlistPage.playlist
                        playlistSongs.value = playlistPage.songs
                    }.onFailure {
                        reportException(it)
                    }
            }
        }
    }
    
    private suspend fun loadJioSaavnPlaylist(playlistId: String) {
        try {
            Log.d("OnlinePlaylistViewModel", "📋 Loading JioSaavn playlist: $playlistId")
            val playlistDetails = saavnAPI.getPlaylistDetails(playlistId)
            
            Log.d("OnlinePlaylistViewModel", "📦 Playlist details keys: ${playlistDetails.keys}")
            
            if (playlistDetails.containsKey("error")) {
                val errorMsg = playlistDetails["error"] as? String ?: "Unknown error"
                Log.e("OnlinePlaylistViewModel", "❌ Failed to load JioSaavn playlist: $errorMsg")
                return
            }
            
            val songs = playlistDetails["songs"] as? List<Map<String, Any>> ?: emptyList()
            val title = playlistDetails["title"] as? String ?: "Unknown Playlist"
            Log.d("OnlinePlaylistViewModel", "✅ Loaded JioSaavn playlist: $title with ${songs.size} songs")
            
            if (songs.isEmpty()) {
                Log.w("OnlinePlaylistViewModel", "⚠️ Playlist loaded but has no songs!")
            }
            
            saavnPlaylistData.value = playlistDetails
            saavnPlaylistSongs.value = songs
            
            Log.d("OnlinePlaylistViewModel", "✅ Playlist data stored - saavnPlaylistData: ${saavnPlaylistData.value != null}, songs: ${saavnPlaylistSongs.value.size}")
        } catch (e: Exception) {
            Log.e("OnlinePlaylistViewModel", "❌ Error loading JioSaavn playlist: ${e.message}", e)
            reportException(e)
        }
    }
}
