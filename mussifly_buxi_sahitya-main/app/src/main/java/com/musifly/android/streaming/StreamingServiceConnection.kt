package com.musifly.android.streaming

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import timber.log.Timber

/**
 * Streaming Service Connection Helper
 * Manages connection to StreamingPlayerService
 * Lifecycle-safe binding/unbinding
 */
class StreamingServiceConnection(
    private val context: Context
) {
    
    private val TAG = "ServiceConnection"
    
    private var streamingService: StreamingPlayerService? = null
    private var isBound = false
    
    /**
     * Service connection callbacks
     */
    interface ConnectionCallback {
        fun onServiceConnected(service: StreamingPlayerService)
        fun onServiceDisconnected()
    }
    
    private var connectionCallback: ConnectionCallback? = null
    
    /**
     * Service connection implementation
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Timber.tag(TAG).d("✅ Service connected")
            
            val streamingBinder = binder as? StreamingPlayerService.StreamingBinder
            streamingService = streamingBinder?.getService()
            isBound = true
            
            streamingService?.let { service ->
                connectionCallback?.onServiceConnected(service)
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            Timber.tag(TAG).d("⚠️ Service disconnected")
            streamingService = null
            isBound = false
            connectionCallback?.onServiceDisconnected()
        }
    }
    
    /**
     * Bind to streaming service
     */
    fun bind(callback: ConnectionCallback? = null) {
        this.connectionCallback = callback
        
        Timber.tag(TAG).d("🔗 Binding to service...")
        
        val intent = Intent(context, StreamingPlayerService::class.java)
        
        // Start service (if not already running)
        context.startService(intent)
        
        // Bind to service
        val bound = context.bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        
        if (bound) {
            Timber.tag(TAG).d("✅ Bind request sent")
        } else {
            Timber.tag(TAG).e("❌ Failed to bind to service")
        }
    }
    
    /**
     * Unbind from streaming service
     */
    fun unbind() {
        if (isBound) {
            Timber.tag(TAG).d("🔓 Unbinding from service")
            context.unbindService(serviceConnection)
            isBound = false
            streamingService = null
        }
    }
    
    /**
     * Get service instance
     */
    fun getService(): StreamingPlayerService? = streamingService
    
    /**
     * Check if service is bound
     */
    fun isBound(): Boolean = isBound
    
    /**
     * Play stream using service
     */
    fun playStream(songTitle: String, artistName: String? = null) {
        streamingService?.playStream(songTitle, artistName)
            ?: Timber.tag(TAG).w("⚠️ Service not bound, cannot play stream")
    }
    
    /**
     * Toggle play/pause
     */
    fun togglePlayPause() {
        streamingService?.togglePlayPause()
            ?: Timber.tag(TAG).w("⚠️ Service not bound")
    }
    
    /**
     * Get streaming engine
     */
    fun getStreamingEngine(): NewPipeStreamingEngine? {
        return streamingService?.getStreamingEngine()
    }
}
