package com.musifly.android.ui.player.layouts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.media3.common.C
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalDownloadUtil
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.constants.PlayerLayoutStyle
import com.musifly.android.constants.PlayerLayoutStyleKey
import com.musifly.android.constants.SliderStyle
import com.musifly.android.constants.SliderStyleKey
import com.musifly.android.db.MusicDatabase
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.extensions.toggleRepeatMode
import com.musifly.android.models.MediaMetadata
import com.musifly.android.playback.PlayerConnection
import com.musifly.android.ui.component.BottomSheetState
import com.musifly.android.ui.component.DownloadButton
import com.musifly.android.utils.rememberEnumPreference
import kotlinx.coroutines.launch

/**
 * Data class holding all common player state
 */
data class PlayerState(
    val mediaMetadata: MediaMetadata?,
    val isPlaying: Boolean,
    val position: Long,
    val duration: Long,
    val isLiked: Boolean,
    val canSkipPrevious: Boolean,
    val canSkipNext: Boolean,
    val repeatMode: Int,
    val shuffleModeEnabled: Boolean,
    val playbackState: Int,
    val download: Download?,
    val streamEngine: String? = null  // Current streaming source (YouTube, NewPipe, JioSaavn, etc.)
)

/**
 * Common player actions that all layouts can use
 */
data class PlayerActions(
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onToggleLike: () -> Unit,
    val onToggleRepeat: () -> Unit,
    val onToggleShuffle: () -> Unit,
    val onOpenQueue: () -> Unit,
    val onOpenMenu: () -> Unit,
    val onOpenLyrics: () -> Unit,
    val onOpenSleepTimer: () -> Unit,
    val onDownload: () -> Unit,
    val onOpenDownloads: () -> Unit,
    val onOpenSoundSettings: () -> Unit = {},
    val onOpenSearch: () -> Unit = {}, // New: Search songs
    val onSwitchPlayer: () -> Unit = {} // New: Switch player layout
)

/**
 * Controller composable that selects and displays the appropriate player layout
 * based on user preference
 */
@Composable
fun PlayerLayoutController(
    state: BottomSheetState,
    navController: NavController,
    playerState: PlayerState,
    playerActions: PlayerActions,
    onOpenFullscreenLyrics: () -> Unit,
    onCollapse: () -> Unit = {},  // Back navigation callback
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val layoutStyle by rememberEnumPreference(
        key = PlayerLayoutStyleKey,
        defaultValue = PlayerLayoutStyle.MUSSIFLY_PREMIUM
    )
    
    val sliderStyle by rememberEnumPreference(
        key = SliderStyleKey,
        defaultValue = SliderStyle.DEFAULT
    )
    
    val effectiveLayoutStyle = layoutStyle
    
    when (effectiveLayoutStyle) {
        PlayerLayoutStyle.INSTAGRAM -> {
            InstagramPlayerLayout(
                playerState = playerState,
                playerActions = playerActions,
                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                sliderStyle = sliderStyle,
                modifier = modifier
            )
        }
        PlayerLayoutStyle.SPOTIFY -> {
            SpotifyPlayerLayout(
                playerState = playerState,
                playerActions = playerActions,
                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                onCollapse = onCollapse,  // Pass back navigation callback
                sliderStyle = sliderStyle,
                modifier = modifier
            )
        }

        PlayerLayoutStyle.BLOOME -> {
            BloomePlayerLayout(
                playerState = playerState,
                playerActions = playerActions,
                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                sliderStyle = sliderStyle,
                modifier = modifier
            )
        }
        PlayerLayoutStyle.MUSSIFLY_PREMIUM -> {
            MussiflyPremiumPlayerLayout(
                playerState = playerState,
                playerActions = playerActions,
                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                onCollapse = onCollapse,
                modifier = modifier
            )
        }
    }
}

/**
 * Helper to create PlayerState from PlayerConnection
 */
@Composable
fun rememberPlayerState(): PlayerState? {
    val playerConnection = LocalPlayerConnection.current ?: return null
    
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val position by playerConnection.currentPosition.collectAsState(initial = 0L)
    val duration by playerConnection.duration.collectAsState(initial = C.TIME_UNSET)
    val isLiked by playerConnection.isLiked.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val streamEngine by playerConnection.streamEngine.collectAsState()
    
    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)
    
    return PlayerState(
        mediaMetadata = mediaMetadata,
        isPlaying = isPlaying,
        position = position,
        duration = duration,
        isLiked = isLiked,
        canSkipPrevious = canSkipPrevious,
        canSkipNext = canSkipNext,
        repeatMode = repeatMode,
        shuffleModeEnabled = shuffleModeEnabled,
        playbackState = playbackState,
        download = download,
        streamEngine = streamEngine
    )
}

/**
 * Create PlayerActions from PlayerConnection
 */
@Composable
fun rememberPlayerActions(
    onOpenQueue: () -> Unit = {},
    onOpenMenu: () -> Unit = {},
    onOpenLyrics: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onDownload: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenSoundSettings: () -> Unit = {}
): PlayerActions? {
    val playerConnection = LocalPlayerConnection.current ?: return null
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    
    return PlayerActions(
        onPlayPause = { playerConnection.player.togglePlayPause() },
        onNext = { playerConnection.player.seekToNext() },
        onPrevious = { playerConnection.player.seekToPrevious() },
        onSeek = { position -> playerConnection.player.seekTo(position) },
        onToggleLike = { playerConnection.toggleLike() },
        onToggleRepeat = { playerConnection.player.toggleRepeatMode() },
        onToggleShuffle = { 
            playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
        },
        onOpenQueue = onOpenQueue,
        onOpenMenu = onOpenMenu,
        onOpenLyrics = onOpenLyrics,
        onOpenSleepTimer = onOpenSleepTimer,
        onDownload = onDownload,
        onOpenDownloads = onOpenDownloads,
        onOpenSoundSettings = onOpenSoundSettings
    )
}

/**
 * Get the current player layout style preference
 */
@Composable
fun rememberPlayerLayoutStyle(): PlayerLayoutStyle {
    val layoutStyle by rememberEnumPreference(
        key = PlayerLayoutStyleKey,
        defaultValue = PlayerLayoutStyle.MUSSIFLY_PREMIUM
    )
    return layoutStyle
}
