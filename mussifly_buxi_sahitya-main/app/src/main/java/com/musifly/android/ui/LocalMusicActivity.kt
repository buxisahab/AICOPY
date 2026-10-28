package com.musifly.android.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.compose.rememberNavController
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalDownloadUtil
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.LocalSyncUtils
import com.musifly.android.constants.MiniPlayerHeight
import com.musifly.android.db.MusicDatabase
import com.musifly.android.playback.DownloadUtil
import com.musifly.android.playback.MusicService
import com.musifly.android.playback.PlayerConnection
import com.musifly.android.playback.safeStartMusicService
import com.musifly.android.ui.component.rememberBottomSheetState
import com.musifly.android.ui.player.BottomSheetPlayer
import com.musifly.android.ui.screens.LocalMusicScreen
import com.musifly.android.ui.theme.OpenTuneTheme
import com.musifly.android.utils.SyncUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Separate Activity for Local Music Screen
 * This solves navigation issues by using proper Activity-based navigation
 */
@AndroidEntryPoint
class LocalMusicActivity : ComponentActivity() {
    
    @Inject
    lateinit var database: MusicDatabase
    
    @Inject
    lateinit var downloadUtil: DownloadUtil
    
    @Inject
    lateinit var syncUtils: SyncUtils
    
    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicBinder) {
                playerConnection = PlayerConnection(
                    this@LocalMusicActivity,
                    service,
                    database,
                    lifecycleScope
                )
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
        }
    }
    
    override fun onStart() {
        super.onStart()
        val serviceIntent = Intent(this, MusicService::class.java)
        // Removed: safeStartMusicService(serviceIntent)
        // Bind the service to communicate with it. 
        // Foreground promotion will happen automatically on playback start.
        bindService(
            serviceIntent,
            serviceConnection,
            BIND_AUTO_CREATE
        )
    }
    
    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // Observe playerConnection state
            val currentPlayerConnection = playerConnection
            
            OpenTuneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Provide all necessary CompositionLocals
                    // Note: playerConnection may be null initially until service connects
                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalPlayerConnection provides currentPlayerConnection,
                        LocalPlayerAwareWindowInsets provides WindowInsets.systemBars,
                        LocalDownloadUtil provides downloadUtil,
                        LocalSyncUtils provides syncUtils,
                    ) {
                        // Only show content when playerConnection is available
                        if (currentPlayerConnection != null) {
                            LocalMusicActivityContent(
                                onBackPressed = {
                                    finish()
                                }
                            )
                        } else {
                            // Show loading while waiting for service connection
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                androidx.compose.material3.CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
    
    @SuppressLint("ConfigurationScreenWidthHeight")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun LocalMusicActivityContent(onBackPressed: () -> Unit) {
        val navController = rememberNavController()
        val playerConnection = LocalPlayerConnection.current
        
        // Get screen dimensions for bottom sheet
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val maxHeight = configuration.screenHeightDp.dp
        val windowsInsets = WindowInsets.systemBars
        val bottomInset = with(density) { 
            windowsInsets.getBottom(density).toDp() 
        }
        
        // Create bottom sheet state for mini player
        val playerBottomSheetState = rememberBottomSheetState(
            dismissedBound = 0.dp,
            collapsedBound = bottomInset + MiniPlayerHeight,
            expandedBound = maxHeight,
        )
        
        // Watch mediaMetadata flow to show mini-player when music plays
        LaunchedEffect(playerConnection) {
            playerConnection?.mediaMetadata?.collect { metadata ->
                if (metadata != null && playerBottomSheetState.isDismissed) {
                    playerBottomSheetState.collapseSoft()
                }
            }
        }
        
        // Listen to player events to show mini player
        DisposableEffect(playerConnection, playerBottomSheetState) {
            val player = playerConnection?.player ?: return@DisposableEffect onDispose { }
            val listener = object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    if (mediaItem != null && playerBottomSheetState.isDismissed) {
                        playerBottomSheetState.collapseSoft()
                    }
                }
                
                override fun onPlaybackStateChanged(playbackState: Int) {
                    // Ensure visible on PLAYING/PAUSED state if we have items
                    if (player.mediaItemCount > 0 && playerBottomSheetState.isDismissed) {
                        playerBottomSheetState.collapseSoft()
                    }
                }
            }
            player.addListener(listener)
            onDispose {
                player.removeListener(listener)
            }
        }
        
        Scaffold(
            bottomBar = {
                Box {
                    BottomSheetPlayer(
                        state = playerBottomSheetState,
                        navController = navController,
                        onOpenFullscreenLyrics = {
                            // Fullscreen lyrics can be handled here if needed
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        ) { jLocalMusicScreen ->
            LocalMusicScreen(
                navController = navController,
                onDeselect = onBackPressed
            )
        }
    }
    
    companion object {
        /**
         * Create an Intent to start LocalMusicActivity
         */
        fun createIntent(context: Context): Intent {
            return Intent(context, LocalMusicActivity::class.java)
        }
    }
}

