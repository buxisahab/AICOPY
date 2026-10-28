package com.musifly.android.download

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Global Event Bus for Download State Changes
 * 
 * Provides real-time state update notifications to UI components
 * without requiring screen refresh or manual polling.
 * 
 * Usage:
 * - DownloadMission posts events when state changes
 * - UI screens collect events and trigger recomposition
 * - Ensures instant UI updates when downloads are paused/resumed
 */
object DownloadEventBus {
    
    /**
     * Download state enum matching mission states
     */
    enum class DownloadState {
        QUEUED,      // Waiting to start
        DOWNLOADING, // Currently downloading
        PAUSED,      // Manually paused by user
        FINISHED,    // Successfully completed
        ERROR,       // Failed with error
        MERGING      // Deprecated: No longer used (FFmpeg removed)
    }
    
    /**
     * State change event
     */
    data class StateChangeEvent(
        val missionId: Long,        // Mission timestamp (unique ID)
        val newState: DownloadState,
        val progress: Float = 0f,    // Optional progress update
        val errorMessage: String? = null
    )
    
    /**
     * Progress update event (for real-time progress bars)
     */
    data class ProgressEvent(
        val missionId: Long,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val progress: Float
    )
    
    /**
     * Internal mutable flow for posting events
     */
    private val _stateChangeEvents = MutableSharedFlow<StateChangeEvent>(
        replay = 0,        // Don't replay old events to new subscribers
        extraBufferCapacity = 10  // Buffer up to 10 events if consumers are slow
    )
    
    /**
     * Internal mutable flow for progress updates
     */
    private val _progressEvents = MutableSharedFlow<ProgressEvent>(
        replay = 0,
        extraBufferCapacity = 50  // Higher buffer for frequent progress updates
    )
    
    /**
     * Public read-only flow for collecting events
     */
    val stateChangeEvents: SharedFlow<StateChangeEvent> = _stateChangeEvents.asSharedFlow()
    
    /**
     * Public read-only flow for progress updates
     */
    val progressEvents: SharedFlow<ProgressEvent> = _progressEvents.asSharedFlow()
    
    /**
     * Post a state change event
     * 
     * @param missionId Unique mission identifier (timestamp)
     * @param newState The new download state
     * @param progress Optional progress percentage (0-100)
     * @param errorMessage Optional error message if state is ERROR
     */
    suspend fun postStateChange(
        missionId: Long,
        newState: DownloadState,
        progress: Float = 0f,
        errorMessage: String? = null
    ) {
        val event = StateChangeEvent(
            missionId = missionId,
            newState = newState,
            progress = progress,
            errorMessage = errorMessage
        )
        _stateChangeEvents.emit(event)
    }
    
    /**
     * Synchronous version for non-coroutine contexts
     */
    fun postStateChangeSync(
        missionId: Long,
        newState: DownloadState,
        progress: Float = 0f,
        errorMessage: String? = null
    ) {
        val event = StateChangeEvent(
            missionId = missionId,
            newState = newState,
            progress = progress,
            errorMessage = errorMessage
        )
        _stateChangeEvents.tryEmit(event)
    }
    
    /**
     * Post a progress update event
     * 
     * @param missionId Unique mission identifier
     * @param downloadedBytes Bytes downloaded so far
     * @param totalBytes Total bytes to download
     */
    suspend fun postProgress(
        missionId: Long,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
        val progress = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()) * 100f
        } else {
            0f
        }
        
        val event = ProgressEvent(
            missionId = missionId,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            progress = progress
        )
        _progressEvents.emit(event)
    }
    
    /**
     * Synchronous version for non-coroutine contexts
     */
    fun postProgressSync(
        missionId: Long,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
        val progress = if (totalBytes > 0) {
            (downloadedBytes.toFloat() / totalBytes.toFloat()) * 100f
        } else {
            0f
        }
        
        val event = ProgressEvent(
            missionId = missionId,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            progress = progress
        )
        _progressEvents.tryEmit(event)
    }
    
    /**
     * Helper: Map DownloadMission to DownloadState
     */
    fun getStateFromMission(mission: DownloadMission): DownloadState {
        return when {
            mission.finished -> DownloadState.FINISHED
            mission.isPaused -> DownloadState.PAUSED
            mission.running && mission.psState == 1 -> DownloadState.MERGING
            mission.running -> DownloadState.DOWNLOADING
            mission.errCode != DownloadMission.ERROR_NOTHING -> DownloadState.ERROR
            mission.enqueued -> DownloadState.QUEUED
            else -> DownloadState.PAUSED
        }
    }
}
