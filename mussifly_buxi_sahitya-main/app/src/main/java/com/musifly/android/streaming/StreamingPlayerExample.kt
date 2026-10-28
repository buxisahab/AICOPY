package com.musifly.android.streaming

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.musifly.android.constants.UseNewPipeStreamingKey
import com.musifly.android.utils.dataStore
import timber.log.Timber

/**
 * EXAMPLE: Streaming Player Usage
 * 
 * Shows how to integrate NewPipe streaming into your existing player
 * 
 * INTEGRATION STEPS:
 * 
 * 1. Check if streaming is enabled:
 *    ```kotlin
 *    val useStreaming = context.dataStore.data
 *        .map { it[UseNewPipeStreamingKey] ?: false }
 *        .first()
 *    ```
 * 
 * 2. Connect to service:
 *    ```kotlin
 *    val serviceConnection = StreamingServiceConnection(context)
 *    serviceConnection.bind(callback)
 *    ```
 * 
 * 3. Play song:
 *    ```kotlin
 *    if (useStreaming) {
 *        serviceConnection.playStream(title, artist)
 *    } else {
 *        // Use normal playback
 *    }
 *    ```
 * 
 * 4. Clean up:
 *    ```kotlin
 *    override fun onDestroy() {
 *        serviceConnection.unbind()
 *        super.onDestroy()
 *    }
 *    ```
 */

/**
 * Example Activity showing streaming integration
 */
class StreamingPlayerExample : ComponentActivity() {
    
    private val TAG = "StreamingExample"
    private var serviceConnection: StreamingServiceConnection? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            StreamingPlayerUI()
        }
    }
    
    override fun onDestroy() {
        serviceConnection?.unbind()
        super.onDestroy()
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StreamingPlayerUI() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        
        // State
        var isConnected by remember { mutableStateOf(false) }
        var isPlaying by remember { mutableStateOf(false) }
        var currentSong by remember { mutableStateOf("") }
        var statusMessage by remember { mutableStateOf("Ready") }
        
        // Example song data
        var songTitle by remember { mutableStateOf("Blinding Lights") }
        var artistName by remember { mutableStateOf("The Weeknd") }
        
        // Service connection callback
        val connectionCallback = remember {
            object : StreamingServiceConnection.ConnectionCallback {
                override fun onServiceConnected(service: StreamingPlayerService) {
                    Timber.tag(TAG).d("✅ Connected to service")
                    isConnected = true
                    statusMessage = "Connected to streaming service"
                }
                
                override fun onServiceDisconnected() {
                    Timber.tag(TAG).d("⚠️ Disconnected from service")
                    isConnected = false
                    statusMessage = "Disconnected from service"
                }
            }
        }
        
        // Initialize service connection
        LaunchedEffect(Unit) {
            serviceConnection = StreamingServiceConnection(context).apply {
                bind(connectionCallback)
            }
        }
        
        // UI
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("NewPipe Streaming Example") }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Status",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (isConnected) {
                            Text(
                                text = "✅ Service Connected",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                // Song input
                OutlinedTextField(
                    value = songTitle,
                    onValueChange = { songTitle = it },
                    label = { Text("Song Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = artistName,
                    onValueChange = { artistName = it },
                    label = { Text("Artist Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Play button
                Button(
                    onClick = {
                        scope.launch {
                            statusMessage = "Loading stream..."
                            serviceConnection?.playStream(songTitle, artistName)
                            currentSong = "$songTitle - $artistName"
                            isPlaying = true
                            statusMessage = "Playing: $currentSong"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isConnected
                ) {
                    Text("▶️ Play Stream")
                }
                
                // Play/Pause toggle
                Button(
                    onClick = {
                        serviceConnection?.togglePlayPause()
                        isPlaying = !isPlaying
                        statusMessage = if (isPlaying) "Playing" else "Paused"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isConnected && currentSong.isNotEmpty()
                ) {
                    Text(if (isPlaying) "⏸️ Pause" else "▶️ Resume")
                }
                
                // Current song
                if (currentSong.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Now Playing",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentSong,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Features",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("✅ No downloads required")
                        Text("✅ Works with screen off")
                        Text("✅ Works when app minimized")
                        Text("✅ Automatic buffering (25-60s)")
                        Text("✅ Network-aware retry")
                        Text("✅ WiFi lock prevents interruptions")
                        Text("✅ Caching for faster loading")
                    }
                }
            }
        }
        
        // Cleanup
        DisposableEffect(Unit) {
            onDispose {
                serviceConnection?.unbind()
            }
        }
    }
}

/**
 * INTEGRATION INTO EXISTING PLAYER
 * 
 * Example: How to modify your existing MusicPlayerActivity
 */
object StreamingIntegrationExample {
    
    /**
     * Example 1: Play song with streaming support
     */
    suspend fun playSongWithStreaming(
        context: Context,
        songTitle: String,
        artistName: String?,
        serviceConnection: StreamingServiceConnection
    ) {
        // Check if streaming is enabled
        val useStreaming = context.dataStore.data
            .map { it[UseNewPipeStreamingKey] ?: false }
            .first()
        
        if (useStreaming) {
            Timber.d("🎵 Using NewPipe streaming")
            serviceConnection.playStream(songTitle, artistName)
        } else {
            Timber.d("🎵 Using normal playback")
            // Your existing playback code here
            // playerConnection.playQueue(...)
        }
    }
    
    /**
     * Example 2: Switch between streaming and normal playback
     */
    @Composable
    fun PlaySongButton(
        songTitle: String,
        artistName: String?,
        serviceConnection: StreamingServiceConnection
    ) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        
        // Read streaming preference
        val useStreaming = context.dataStore.data
            .map { it[UseNewPipeStreamingKey] ?: false }
            .collectAsState(initial = false)
        
        Button(
            onClick = {
                scope.launch {
                    if (useStreaming.value) {
                        // Use streaming
                        serviceConnection.playStream(songTitle, artistName)
                    } else {
                        // Use normal playback
                        // Your existing code
                    }
                }
            }
        ) {
            Text(if (useStreaming.value) "🌐 Stream" else "📥 Play")
        }
    }
    
    /**
     * Example 3: Fragment lifecycle integration
     */
    class MusicPlayerFragment : androidx.fragment.app.Fragment() {
        
        private var serviceConnection: StreamingServiceConnection? = null
        
        override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            
            // Initialize service connection
            serviceConnection = StreamingServiceConnection(requireContext()).apply {
                bind(object : StreamingServiceConnection.ConnectionCallback {
                    override fun onServiceConnected(service: StreamingPlayerService) {
                        // Service ready
                        Timber.d("Service connected")
                    }
                    
                    override fun onServiceDisconnected() {
                        // Service disconnected
                        Timber.d("Service disconnected")
                    }
                })
            }
        }
        
        fun playSong(title: String, artist: String?) {
            serviceConnection?.playStream(title, artist)
        }
        
        override fun onDestroyView() {
            serviceConnection?.unbind()
            super.onDestroyView()
        }
    }
}

// Note: dataStore is imported from utils.dataStore
// It's a Context extension property defined in utils/DataStore.kt
