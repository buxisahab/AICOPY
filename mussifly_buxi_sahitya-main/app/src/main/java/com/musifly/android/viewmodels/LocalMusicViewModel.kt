package com.musifly.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.android.local.LocalMusicLoader
import com.musifly.android.models.LocalSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for LocalMusicScreen
 * Manages local music loading, searching, and state
 */
@HiltViewModel
class LocalMusicViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    
    private val localMusicLoader = LocalMusicLoader(application)
    
    // All local songs
    private val _allSongs = MutableStateFlow<List<LocalSong>>(emptyList())
    val allSongs: StateFlow<List<LocalSong>> = _allSongs.asStateFlow()
    
    // Filtered/searched songs (displayed in UI)
    private val _displayedSongs = MutableStateFlow<List<LocalSong>>(emptyList())
    val displayedSongs: StateFlow<List<LocalSong>> = _displayedSongs.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadLocalMusic()
    }
    
    /**
     * Load all local music from device
     */
    fun loadLocalMusic() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _error.value = null
            
            try {
                val songs = localMusicLoader.loadLocalMusic()
                _allSongs.value = songs
                _displayedSongs.value = songs
                
                if (songs.isEmpty()) {
                    _error.value = "No local music found"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load music: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Search local music
     */
    fun searchMusic(query: String) {
        _searchQuery.value = query
        
        if (query.isBlank()) {
            _displayedSongs.value = _allSongs.value
            return
        }
        
        viewModelScope.launch(Dispatchers.Default) {
            val filtered = _allSongs.value.filter { song ->
                song.title.contains(query, ignoreCase = true) ||
                song.artist.contains(query, ignoreCase = true) ||
                song.album.contains(query, ignoreCase = true)
            }
            _displayedSongs.value = filtered
        }
    }
    
    /**
     * Refresh - reload all music
     */
    fun refresh() {
        loadLocalMusic()
    }
    
    /**
     * Get song by ID
     */
    fun getSongById(id: Long): LocalSong? {
        return _allSongs.value.find { it.id == id }
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }
}
