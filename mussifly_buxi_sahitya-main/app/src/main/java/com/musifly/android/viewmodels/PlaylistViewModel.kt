package com.musifly.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.Song
import com.musifly.android.sync.UserSessionManager
import com.musifly.android.sync.local.UserPlaylistEntity
import com.musifly.android.sync.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing user playlists
 * Handles both guest mode (local only) and logged-in mode (with Firestore sync)
 */
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    application: Application,
    private val playlistRepository: PlaylistRepository,
    private val database: MusicDatabase
) : AndroidViewModel(application) {
    
    // All playlists
    private val _playlists = MutableStateFlow<List<UserPlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<UserPlaylistEntity>> = _playlists.asStateFlow()
    
    // Current playlist (for detail view)
    private val _currentPlaylist = MutableStateFlow<UserPlaylistEntity?>(null)
    val currentPlaylist: StateFlow<UserPlaylistEntity?> = _currentPlaylist.asStateFlow()
    
    // Songs in current playlist
    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Login state
    val isLoggedIn: StateFlow<Boolean> = MutableStateFlow(UserSessionManager.isLoggedIn())
    
    init {
        loadPlaylists()
    }
    
    /**
     * Load all playlists
     */
    fun loadPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _error.value = null
                
                playlistRepository.getAllPlaylists().collect { playlists ->
                    _playlists.value = playlists
                }
            } catch (e: Exception) {
                _error.value = "Failed to load playlists: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load playlist details
     */
    fun loadPlaylist(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _error.value = null
                
                playlistRepository.getPlaylist(playlistId).collect { playlist ->
                    _currentPlaylist.value = playlist
                    
                    if (playlist != null) {
                        // Load songs in playlist
                        playlistRepository.getPlaylistSongs(playlistId).collect { songIds ->
                            val songs = songIds.mapNotNull { songId ->
                                database.getSongById(songId)
                            }
                            _playlistSongs.value = songs
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load playlist: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Create a new playlist
     */
    fun createPlaylist(name: String, onComplete: (Result<String>) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _error.value = null
                val result = playlistRepository.createPlaylist(name)
                result.fold(
                    onSuccess = { playlistId ->
                        onComplete(Result.success(playlistId))
                        loadPlaylists() // Refresh list
                    },
                    onFailure = { error ->
                        _error.value = "Failed to create playlist: ${error.message}"
                        onComplete(Result.failure(error))
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error creating playlist: ${e.message}"
                onComplete(Result.failure(e))
            }
        }
    }
    
    /**
     * Rename a playlist
     */
    fun renamePlaylist(playlistId: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _error.value = null
                val result = playlistRepository.renamePlaylist(playlistId, newName)
                result.fold(
                    onSuccess = {
                        loadPlaylists() // Refresh list
                    },
                    onFailure = { error ->
                        _error.value = "Failed to rename playlist: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error renaming playlist: ${e.message}"
            }
        }
    }
    
    /**
     * Delete a playlist
     */
    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _error.value = null
                val result = playlistRepository.deletePlaylist(playlistId)
                result.fold(
                    onSuccess = {
                        loadPlaylists() // Refresh list
                    },
                    onFailure = { error ->
                        _error.value = "Failed to delete playlist: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error deleting playlist: ${e.message}"
            }
        }
    }
    
    /**
     * Add song to playlist
     */
    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _error.value = null
                val result = playlistRepository.addSongToPlaylist(playlistId, songId)
                result.fold(
                    onSuccess = {
                        loadPlaylist(playlistId) // Refresh playlist
                    },
                    onFailure = { error ->
                        _error.value = "Failed to add song: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error adding song: ${e.message}"
            }
        }
    }
    
    /**
     * Remove song from playlist
     */
    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _error.value = null
                val result = playlistRepository.removeSongFromPlaylist(playlistId, songId)
                result.fold(
                    onSuccess = {
                        loadPlaylist(playlistId) // Refresh playlist
                    },
                    onFailure = { error ->
                        _error.value = "Failed to remove song: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error removing song: ${e.message}"
            }
        }
    }
    
    /**
     * Check if song is in playlist
     */
    suspend fun isSongInPlaylist(playlistId: String, songId: String): Boolean {
        return playlistRepository.isSongInPlaylist(playlistId, songId)
    }
    
    /**
     * Refresh playlists
     */
    fun refresh() {
        loadPlaylists()
    }
}

