package com.musifly.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.Song
import com.musifly.android.sync.UserSessionManager
import com.musifly.android.sync.repository.LikesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing liked songs
 * Handles both guest mode (local only) and logged-in mode (with Firestore sync)
 */
@HiltViewModel
class LikesViewModel @Inject constructor(
    application: Application,
    private val likesRepository: LikesRepository,
    private val database: MusicDatabase
) : AndroidViewModel(application) {
    
    // Liked song IDs
    private val _likedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val likedSongIds: StateFlow<Set<String>> = _likedSongIds.asStateFlow()
    
    // Liked songs with full details
    private val _likedSongs = MutableStateFlow<List<Song>>(emptyList())
    val likedSongs: StateFlow<List<Song>> = _likedSongs.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Login state
    val isLoggedIn: StateFlow<Boolean> = MutableStateFlow(UserSessionManager.isLoggedIn())
    
    init {
        loadLikedSongs()
    }
    
    /**
     * Load liked songs from local database
     */
    fun loadLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Get liked song IDs from repository
                likesRepository.getAllLikedSongIds().collect { songIds ->
                    _likedSongIds.value = songIds.toSet()
                    
                    // Load full song details
                    val songs = songIds.mapNotNull { songId ->
                        database.getSongById(songId)
                    }
                    _likedSongs.value = songs
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load liked songs: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle like status for a song
     * Uses optimistic UI update for instant feedback
     */
    fun toggleLike(songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _error.value = null
                
                // Optimistic UI update - change state immediately
                val currentLikedIds = _likedSongIds.value.toMutableSet()
                val wasLiked = currentLikedIds.contains(songId)
                val newLikedState = !wasLiked
                
                if (newLikedState) {
                    currentLikedIds.add(songId)
                } else {
                    currentLikedIds.remove(songId)
                }
                _likedSongIds.value = currentLikedIds
                
                // Update liked songs list
                if (newLikedState) {
                    database.getSongById(songId)?.let { song ->
                        _likedSongs.value = _likedSongs.value + song
                    }
                } else {
                    _likedSongs.value = _likedSongs.value.filter { it.id != songId }
                }
                
                // Perform actual network/database operation
                val result = likesRepository.toggleLike(songId)
                result.fold(
                    onSuccess = { isLiked ->
                        // Success - state already updated optimistically
                    },
                    onFailure = { error ->
                        // Revert optimistic update on failure
                        val revertedIds = _likedSongIds.value.toMutableSet()
                        if (wasLiked) {
                            revertedIds.add(songId)
                        } else {
                            revertedIds.remove(songId)
                        }
                        _likedSongIds.value = revertedIds
                        
                        // Revert liked songs list
                        if (wasLiked) {
                            database.getSongById(songId)?.let { song ->
                                _likedSongs.value = _likedSongs.value + song
                            }
                        } else {
                            _likedSongs.value = _likedSongs.value.filter { it.id != songId }
                        }
                        
                        _error.value = "Failed to toggle like: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error toggling like: ${e.message}"
            }
        }
    }
    
    /**
     * Check if a song is liked
     */
    fun isLiked(songId: String): Boolean {
        return _likedSongIds.value.contains(songId)
    }
    
    /**
     * Refresh liked songs
     */
    fun refresh() {
        loadLikedSongs()
    }
}


