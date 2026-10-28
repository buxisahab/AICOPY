package com.musifly.android.viewmodels

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.musifly.android.download.DownloadManager
import com.musifly.android.download.DownloadManagerService
import com.musifly.android.download.DownloadMission
import com.musifly.android.download.FinishedMission
import com.musifly.android.download.Mission
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for managing downloads
 */
@dagger.hilt.android.lifecycle.HiltViewModel
class DownloadViewModel @javax.inject.Inject constructor(
    application: Application,
    private val cachedDownloadsRepository: com.musifly.android.sync.repository.CachedDownloadsRepository
) : AndroidViewModel(application) {
    
    private var downloadManager: DownloadManager? = null
    private var serviceBinder: DownloadManagerService.DownloadManagerBinder? = null
    
    private val _pendingDownloads = MutableLiveData<List<DownloadMission>>(emptyList())
    val pendingDownloads: LiveData<List<DownloadMission>> = _pendingDownloads
    
    private val _finishedDownloads = MutableLiveData<List<FinishedMission>>(emptyList())
    val finishedDownloads: LiveData<List<FinishedMission>> = _finishedDownloads
    
    private val _isServiceBound = MutableLiveData(false)
    val isServiceBound: LiveData<Boolean> = _isServiceBound
    
    // Track if bindService was actually called to prevent unbind errors
    private var isServiceBindingAttempted = false
    
    private var refreshJob: Job? = null
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            serviceBinder = service as? DownloadManagerService.DownloadManagerBinder
            downloadManager = serviceBinder?.getDownloadManager()
            _isServiceBound.value = true
            refreshDownloads()
            startAutoRefresh()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBinder = null
            downloadManager = null
            _isServiceBound.value = false
        }
    }
    
    /**
     * Bind to download service
     */
    fun bindToService(context: Context) {
        try {
            val intent = Intent(context, DownloadManagerService::class.java)
            val bound = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            if (bound) {
                isServiceBindingAttempted = true
            }
        } catch (e: Exception) {
            Log.e("DownloadViewModel", "Failed to bind to service", e)
            isServiceBindingAttempted = false
        }
    }
    
    /**
     * Unbind from download service
     */
    fun unbindFromService(context: Context) {
        // Only attempt to unbind if we actually called bindService
        if (isServiceBindingAttempted) {
            try {
                context.unbindService(serviceConnection)
                _isServiceBound.value = false
                isServiceBindingAttempted = false
            } catch (e: IllegalArgumentException) {
                // Service was not registered, ignore this error
                Log.w("DownloadViewModel", "Service was not registered, cannot unbind", e)
                isServiceBindingAttempted = false
            } catch (e: Exception) {
                Log.e("DownloadViewModel", "Error unbinding from service", e)
                isServiceBindingAttempted = false
            }
        }
    }
    
    /**
     * Refresh downloads list
     */
    fun refreshDownloads() {
        downloadManager?.let { manager ->
            _pendingDownloads.value = manager.getPendingMissions()
            _finishedDownloads.value = manager.getFinishedMissions()
        }
    }
    
    /**
     * Pause a download
     */
    fun pauseDownload(mission: DownloadMission) {
        downloadManager?.pauseMission(mission)
        refreshDownloads()
    }
    
    /**
     * Resume a download
     */
    fun resumeDownload(mission: DownloadMission) {
        downloadManager?.resumeMission(mission)
        refreshDownloads()
    }
    
    /**
     * Delete a download
     */
    fun deleteDownload(mission: Mission, deleteFile: Boolean = false) {
        // Sync with Supabase if it's a song
        mission.songId?.let { songId ->
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                cachedDownloadsRepository.removeCachedDownload(songId)
            }
        }
        
        downloadManager?.deleteMission(mission, deleteFile)
        refreshDownloads()
    }
    
    /**
     * Pause all downloads
     */
    fun pauseAllDownloads() {
        downloadManager?.pauseAllMissions(false)
        refreshDownloads()
    }
    
    /**
     * Resume all downloads
     */
    fun resumeAllDownloads() {
        downloadManager?.startAllMissions()
        refreshDownloads()
    }
    
    /**
     * Clear finished downloads
     */
    fun clearFinishedDownloads() {
        // Sync all finished downloads with Supabase before clearing
        val finishedMissions = downloadManager?.getFinishedMissions() ?: emptyList()
        if (finishedMissions.isNotEmpty()) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                finishedMissions.forEach { mission ->
                    mission.songId?.let { songId ->
                        cachedDownloadsRepository.removeCachedDownload(songId)
                    }
                }
            }
        }
        
        downloadManager?.forgetFinishedDownloads()
        refreshDownloads()
    }
    
    /**
     * Start auto-refresh for real-time progress updates
     */
    private fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(1000) // Refresh every 1 second
                refreshDownloads()
            }
        }
    }
    
    /**
     * Stop auto-refresh
     */
    private fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
        unbindFromService(getApplication())
    }
}
