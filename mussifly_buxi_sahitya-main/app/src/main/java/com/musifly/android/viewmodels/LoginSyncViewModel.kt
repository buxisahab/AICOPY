package com.musifly.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.android.sync.SyncManager
import com.musifly.android.sync.SyncResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for handling sync operations on login
 * Manages the sync process when user logs in
 */
@HiltViewModel
class LoginSyncViewModel @Inject constructor(
    application: Application,
    private val syncManager: SyncManager
) : AndroidViewModel(application) {
    
    // Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    // Sync result
    private val _syncResult = MutableStateFlow<Result<SyncResult>?>(null)
    val syncResult: StateFlow<Result<SyncResult>?> = _syncResult.asStateFlow()
    
    /**
     * Trigger sync after login
     * Downloads data from Firestore and uploads local unsynced data
     */
    fun syncAfterLogin() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isSyncing.value = true
                _syncResult.value = null
                
                Timber.tag("LoginSyncViewModel").d("Starting sync after login")
                
                syncManager.syncAllUserData(viewModelScope) { result ->
                    _syncResult.value = result
                    _isSyncing.value = false
                    
                    result.fold(
                        onSuccess = { syncResult ->
                            Timber.tag("LoginSyncViewModel").d(
                                "Sync complete: ${syncResult.likedSongsCount} likes, ${syncResult.playlistsCount} playlists, ${syncResult.cachedDownloadsCount} cached downloads"
                            )
                        },
                        onFailure = { error ->
                            Timber.tag("LoginSyncViewModel").e(error, "Sync failed after login")
                        }
                    )
                }
            } catch (e: Exception) {
                Timber.tag("LoginSyncViewModel").e(e, "Error triggering sync after login")
                _syncResult.value = Result.failure(e)
                _isSyncing.value = false
            }
        }
    }
    
    /**
     * Clear sync result
     */
    fun clearSyncResult() {
        _syncResult.value = null
    }
}
