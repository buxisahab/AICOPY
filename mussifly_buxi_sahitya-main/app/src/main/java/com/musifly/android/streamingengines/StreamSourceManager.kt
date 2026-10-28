package com.musifly.android.streamingengines

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Callback type for source change notifications
 * Must be defined outside object (Kotlin doesn't support nested type aliases)
 */
typealias SourceChangeCallback = (StreamSourceManager.StreamType) -> Unit

/**
 * StreamSourceManager - Central manager for Home Screen source selection
 * 
 * Manages which streaming engine is currently active on Home Screen:
 * - YouTube
 * - YouTube Music  
 * - JioSaavn
 * 
 * When user switches source, Home Screen reloads with content from selected engine
 */
object StreamSourceManager {
    private const val TAG = "StreamSourceManager"
    
    /**
     * Supported streaming sources for Home Screen
     */
    enum class StreamType {
        YOUTUBE,      // youtube.com / googlevideo.com
        YT_MUSIC,     // music.youtube.com  
        JIOSAAVN      // saavn.com / jiosaavn.com
    }
    
    private val sourceChangeCallbacks = mutableListOf<SourceChangeCallback>()
    
    /**
     * Register a callback for source changes
     */
    fun registerSourceChangeCallback(callback: SourceChangeCallback) {
        sourceChangeCallbacks.add(callback)
    }
    
    /**
     * Unregister a callback
     */
    fun unregisterSourceChangeCallback(callback: SourceChangeCallback) {
        sourceChangeCallbacks.remove(callback)
    }
    
    /**
     * Current active source (default: YouTube)
     * Using StateFlow for reactive updates in Compose
     */
    private val _currentSourceFlow = kotlinx.coroutines.flow.MutableStateFlow(StreamType.YOUTUBE)
    val currentSourceFlow: kotlinx.coroutines.flow.StateFlow<StreamType> = _currentSourceFlow
    
    @Volatile
    private var _currentSource: StreamType = StreamType.YOUTUBE
    
    var currentSource: StreamType
        get() = _currentSource
        set(value) {
            if (_currentSource != value) {
                val oldSource = _currentSource
                Log.d(TAG, "🔄 Source changed: ${oldSource.name} → ${value.name}")
                _currentSource = value
                _currentSourceFlow.value = value // Update StateFlow for Compose reactivity
                // Notify all callbacks
                sourceChangeCallbacks.forEach { it(value) }
            }
        }
    
    /**
     * Set source with forced refresh (clears cache and triggers reload)
     * This should be called from UI when user switches source
     */
    fun setSource(type: StreamType) {
        if (_currentSource != type) {
            val oldSource = _currentSource
            Log.d(TAG, "🔄 FORCED Source switch: ${oldSource.name} → ${type.name}")
            _currentSource = type
            // Update StateFlow for reactive UI updates
            _currentSourceFlow.value = type
            // Notify all callbacks for forced refresh
            sourceChangeCallbacks.forEach { it(type) }
        }
    }
    
    /**
     * Handle source change with immediate reload
     * This ensures UI and playback update instantly
     * 
     * CRITICAL: This is called from UI when user switches source tabs
     * It triggers:
     * 1. Source update (sets currentSource)
     * 2. Callbacks fire (HomeViewModel clears cache + reloads, MusicService clears URL cache)
     */
    fun onSourceChange(newSource: StreamType) {
        if (currentSource == newSource) {
            Log.d(TAG, "⚠️ Source already set to ${newSource.name}, skipping")
            return
        }
        
        val oldSource = currentSource
        Log.d(TAG, "🔄 ENFORCING Source change: ${oldSource.name} → ${newSource.name}")
        Log.d(TAG, "   → This will trigger: clearHomeCache() + loadHomeFromSource() + clearSongUrlCache()")
        
        // CRITICAL: Log source change for playback verification
        Log.d("PlaybackSourceCheck", "TOPBAR_SELECTED = ${newSource.name}")
        Log.d(TAG, "🎵 StreamSourceManager.currentSource updated to: ${newSource.name}")
        
        // Update source FIRST - this triggers all registered callbacks
        setSource(newSource)
        
        // Callbacks registered in HomeViewModel and MusicService will handle:
        // 1. HomeViewModel.clearHomeCache() + loadHomeFromSource()
        // 2. MusicService.clearSongUrlCache() + reset currentStreamingEngine
    }
    
    /**
     * Clear player cache (called when source switches to prevent old stream URLs from being used)
     */
    fun onSourceSwitch(newSource: StreamType) {
        Log.d(TAG, "🧹 Source switch detected: Clearing player caches for ${newSource.name}")
        // This will be called from MusicService to clear URL cache
    }
    
    /**
     * Get display name for source
     */
    fun getDisplayName(source: StreamType): String {
        return when (source) {
            StreamType.YOUTUBE -> "YouTube"
            StreamType.YT_MUSIC -> "YouTube Music"
            StreamType.JIOSAAVN -> "JioSaavn"
        }
    }
    
    /**
     * Get color for source (for UI indicators)
     */
    fun getSourceColor(source: StreamType): Long {
        return when (source) {
            StreamType.YOUTUBE -> 0xFFFF0000L      // Red
            StreamType.YT_MUSIC -> 0xFFFF005DL     // Pink
            StreamType.JIOSAAVN -> 0xFF4CAF50L     // Green
        }
    }
    
    /**
     * Get icon/emoji for source
     */
    fun getSourceIcon(source: StreamType): String {
        return when (source) {
            StreamType.YOUTUBE -> "🔰"
            StreamType.YT_MUSIC -> "🎵"
            StreamType.JIOSAAVN -> "🎧"
        }
    }
    
    /**
     * Detect source from URL
     */
    fun detectSourceFromUrl(url: String?): StreamType {
        if (url == null) return StreamType.YOUTUBE
        
        return when {
            url.contains("music.youtube.com", ignoreCase = true) -> StreamType.YT_MUSIC
            url.contains("youtube.com", ignoreCase = true) || 
            url.contains("googlevideo.com", ignoreCase = true) -> StreamType.YOUTUBE
            url.contains("saavn", ignoreCase = true) || 
            url.contains("jiosaavn", ignoreCase = true) -> StreamType.JIOSAAVN
            else -> StreamType.YOUTUBE // Default fallback
        }
    }
    
    /**
     * Detect source from mediaId
     */
    fun detectSourceFromMediaId(mediaId: String): StreamType {
        return when {
            mediaId.startsWith("saavn:") -> StreamType.JIOSAAVN
            mediaId.startsWith("local_") -> StreamType.YOUTUBE // Local files default to YouTube format
            else -> StreamType.YOUTUBE // YouTube video IDs
        }
    }
}
