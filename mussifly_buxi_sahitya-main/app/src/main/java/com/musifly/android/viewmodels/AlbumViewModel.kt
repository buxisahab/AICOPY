package com.musifly.android.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.AlbumItem
import com.musifly.android.db.MusicDatabase
import com.musifly.android.streamingengines.saavn.SaavnAPI
import com.musifly.android.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
    val saavnAPI: SaavnAPI,
) : ViewModel() {
    val albumId = savedStateHandle.get<String>("albumId")!!
    val playlistId = MutableStateFlow("")
    val albumWithSongs =
        database
            .albumWithSongs(albumId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var otherVersions = MutableStateFlow<List<AlbumItem>>(emptyList())
    
    // For JioSaavn albums (not stored in database)
    val saavnAlbumData = MutableStateFlow<Map<String, Any?>?>(null)
    val saavnAlbumSongs = MutableStateFlow<List<Map<String, Any>>>(emptyList())

    init {
        Log.d("AlbumViewModel", "🔷 AlbumViewModel initialized with albumId: $albumId")
        viewModelScope.launch {
            // Check if this is a JioSaavn album
            if (albumId.startsWith("saavn:")) {
                val actualId = albumId.removePrefix("saavn:")
                Log.d("AlbumViewModel", "✅ Detected JioSaavn album, actual ID: $actualId")
                loadJioSaavnAlbum(actualId)
            } else {
                Log.d("AlbumViewModel", "📺 Loading YouTube album: $albumId")
                // Load from YouTube API as before
                val album = database.album(albumId).first()
                YouTube
                    .album(albumId)
                    .onSuccess {
                        playlistId.value = it.album.playlistId
                        otherVersions.value = it.otherVersions
                        database.transaction {
                            if (album == null) {
                                insert(it)
                            } else {
                                update(album.album, it, album.artists)
                            }
                        }
                    }.onFailure {
                        reportException(it)
                        if (it.message?.contains("NOT_FOUND") == true) {
                            database.query {
                                album?.album?.let(::delete)
                            }
                        }
                    }
            }
        }
    }
    
    private suspend fun loadJioSaavnAlbum(albumId: String) {
        try {
            Log.d("AlbumViewModel", "📀 Loading JioSaavn album: $albumId")
            val albumDetails = saavnAPI.getAlbumDetails(albumId)
            
            Log.d("AlbumViewModel", "📦 Album details keys: ${albumDetails.keys}")
            
            if (albumDetails.containsKey("error")) {
                val errorMsg = albumDetails["error"] as? String ?: "Unknown error"
                Log.e("AlbumViewModel", "❌ Failed to load JioSaavn album: $errorMsg")
                return
            }
            
            val songs = albumDetails["songs"] as? List<Map<String, Any>> ?: emptyList()
            val title = albumDetails["title"] as? String ?: "Unknown Album"
            Log.d("AlbumViewModel", "✅ Loaded JioSaavn album: $title with ${songs.size} songs")
            
            if (songs.isEmpty()) {
                Log.w("AlbumViewModel", "⚠️ Album loaded but has no songs!")
            }
            
            saavnAlbumData.value = albumDetails
            saavnAlbumSongs.value = songs
            
            Log.d("AlbumViewModel", "✅ Album data stored - saavnAlbumData: ${saavnAlbumData.value != null}, songs: ${saavnAlbumSongs.value.size}")
        } catch (e: Exception) {
            Log.e("AlbumViewModel", "❌ Error loading JioSaavn album: ${e.message}", e)
            reportException(e)
        }
    }
}
