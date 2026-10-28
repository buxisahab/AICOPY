package com.musifly.android.playback

import android.util.Log
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Playback Priority Manager
 * 
 * Pauses background work (Home refresh, Recommendations, API fetches) during playback
 * to ensure real-time audio never blocks.
 */
object PlaybackPriorityManager {
    private const val TAG = "PlaybackPriorityManager"
    
    // State flow for playback priority mode
    private val _isPlaybackPriorityMode = MutableStateFlow(false)
    val isPlaybackPriorityMode: StateFlow<Boolean> = _isPlaybackPriorityMode
    
    /**
     * Update priority mode based on player state
     */
    fun updatePriorityMode(player: Player) {
        val shouldEnable = player.isPlaying && player.playbackState == Player.STATE_READY
        val wasEnabled = _isPlaybackPriorityMode.value
        
        if (shouldEnable != wasEnabled) {
            _isPlaybackPriorityMode.value = shouldEnable
            if (shouldEnable) {
                Log.i(TAG, "🎵 Playback priority mode ENABLED - pausing background work")
            } else {
                Log.i(TAG, "🎵 Playback priority mode DISABLED - resuming background work")
            }
        }
    }
    
    /**
     * Check if background work should be paused
     */
    fun shouldPauseBackgroundWork(): Boolean {
        return _isPlaybackPriorityMode.value
    }
}
