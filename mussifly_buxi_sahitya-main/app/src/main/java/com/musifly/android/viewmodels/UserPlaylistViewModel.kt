package com.musifly.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.android.sync.local.UserPlaylistEntity
import com.musifly.android.sync.repository.PlaylistRepository
import com.musifly.android.sync.usecases.AddSongToPlaylistUseCase
import com.musifly.android.sync.usecases.GetUserPlaylistsUseCase
import com.musifly.android.sync.usecases.RemoveSongFromPlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for managing user playlists
 */
@HiltViewModel
class UserPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {
    private val getUserPlaylistsUseCase = GetUserPlaylistsUseCase(playlistRepository)
    private val addSongToPlaylistUseCase = AddSongToPlaylistUseCase(playlistRepository)
    private val removeSongFromPlaylistUseCase = RemoveSongFromPlaylistUseCase(playlistRepository)
    
    // Global upload/download sync status
    val syncStatus = playlistRepository.syncStatus
    
    /**
     * Get all user playlists
     */
    val playlists: Flow<List<UserPlaylistEntity>> = getUserPlaylistsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * Get playlist by ID
     */
    fun getPlaylist(playlistId: String): Flow<UserPlaylistEntity?> {
        return playlistRepository.getPlaylist(playlistId)
    }
    
    /**
     * Get songs in a playlist
     */
    fun getPlaylistSongs(playlistId: String): Flow<List<String>> {
        return playlistRepository.getPlaylistSongs(playlistId)
    }
    
    /**
     * Create a new playlist
     */
    fun createPlaylist(name: String, onResult: (Result<String>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = playlistRepository.createPlaylist(name)
                onResult(result)
            } catch (e: Exception) {
                Timber.tag("UserPlaylistViewModel").e(e, "Failed to create playlist")
                onResult(Result.failure(e))
            }
        }
    }
    
    /**
     * Rename playlist
     */
    fun renamePlaylist(playlistId: String, newName: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = playlistRepository.renamePlaylist(playlistId, newName)
                onResult(result)
            } catch (e: Exception) {
                Timber.tag("UserPlaylistViewModel").e(e, "Failed to rename playlist")
                onResult(Result.failure(e))
            }
        }
    }
    
    /**
     * Delete playlist
     */
    fun deletePlaylist(playlistId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = playlistRepository.deletePlaylist(playlistId)
                onResult(result)
            } catch (e: Exception) {
                Timber.tag("UserPlaylistViewModel").e(e, "Failed to delete playlist")
                onResult(Result.failure(e))
            }
        }
    }
    
    /**
     * Add song to playlist
     */
    fun addSongToPlaylist(playlistId: String, songId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = addSongToPlaylistUseCase(playlistId, songId)
                onResult(result)
            } catch (e: Exception) {
                Timber.tag("UserPlaylistViewModel").e(e, "Failed to add song to playlist")
                onResult(Result.failure(e))
            }
        }
    }
    
    /**
     * Remove song from playlist
     */
    fun removeSongFromPlaylist(playlistId: String, songId: String, onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = removeSongFromPlaylistUseCase(playlistId, songId)
                onResult(result)
            } catch (e: Exception) {
                Timber.tag("UserPlaylistViewModel").e(e, "Failed to remove song from playlist")
                onResult(Result.failure(e))
            }
        }
    }
    
    /**
     * Check if song is in playlist
     */
    suspend fun isSongInPlaylist(playlistId: String, songId: String): Boolean {
        return try {
            playlistRepository.isSongInPlaylist(playlistId, songId)
        } catch (e: Exception) {
            Timber.tag("UserPlaylistViewModel").e(e, "Failed to check if song in playlist")
            false
        }
    }
    
    /**
     * Sync playlists from Supabase
     */
    fun syncFromSupabase(onResult: (Result<Int>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val result = playlistRepository.syncFromSupabase()
                onResult(result)
            } catch (e: Exception) {
                Timber.tag("UserPlaylistViewModel").e(e, "Failed to sync playlists")
                onResult(Result.failure(e))
            }
        }
    }
}
