package com.buxi.music.ui.player

import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.text.format.Formatter
import com.buxi.music.utils.Toast
import android.os.Environment
import android.os.Build
import android.provider.MediaStore
import android.content.ContentValues
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
// import android.media.audiofx.AudioEffect
// import android.media.audiofx.BassBoost
// import android.media.audiofx.Virtualizer
// import android.media.audiofx.Equalizer
// import android.media.audiofx.PresetReverb
// import android.media.audiofx.LoudnessEnhancer
// import android.media.audiofx.EnvironmentalReverb
// import com.buxi.music.dsp.UniversalDSPManager
// import com.buxi.music.dsp.PlayerType
// import com.buxi.music.dsp.PerformanceMode
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioManager
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
import androidx.compose.animation.core.Spring.StiffnessLow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
 
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.buxi.music.ui.screens.AudioEffectsScreen
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
 
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
 
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.ImageLoader
import coil.request.ImageRequest
import com.buxi.music.LocalDatabase
import com.buxi.music.LocalDownloadUtil
import com.buxi.music.LocalPlayerConnection
import com.buxi.music.R
import com.buxi.music.constants.DarkModeKey
import com.buxi.music.constants.PlayerBackgroundStyle
import com.buxi.music.constants.PlayerBackgroundStyleKey
import com.buxi.music.constants.PlayerButtonsStyle
import com.buxi.music.constants.PlayerButtonsStyleKey
import com.buxi.music.constants.PlayerHorizontalPadding
import com.buxi.music.constants.PlayerTextAlignmentKey
import com.buxi.music.constants.PureBlackKey
import com.buxi.music.constants.QueuePeekHeight
import com.buxi.music.constants.ShowLyricsKey
import com.buxi.music.constants.SliderStyle
import com.buxi.music.constants.SliderStyleKey
import com.buxi.music.extensions.togglePlayPause
import com.buxi.music.extensions.toggleRepeatMode
import com.buxi.music.models.MediaMetadata
import com.buxi.music.playback.ExoDownloadService
import com.buxi.music.playback.PlayerConnection
import com.buxi.music.ui.component.BottomSheet
import com.buxi.music.ui.component.BottomSheetState
import com.buxi.music.ui.component.LocalMenuState
import com.buxi.music.ui.component.PlayerSliderTrack
import com.buxi.music.ui.component.ResizableIconButton
import com.buxi.music.ui.component.rememberBottomSheetState
import com.buxi.music.ui.menu.PlayerMenu
import com.buxi.music.ui.player.MiniPlayer
import com.buxi.music.ui.player.Queue
import com.buxi.music.ui.screens.settings.DarkMode
import com.buxi.music.ui.screens.settings.PlayerTextAlignment
import com.buxi.music.ui.theme.extractGradientColors
import com.buxi.music.utils.makeTimeString
import com.buxi.music.utils.rememberEnumPreference
import com.buxi.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import me.saket.squiggles.SquigglySlider
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.sp


@Composable
fun FullScreenThumbnailWithGestures(
    mediaMetadata: MediaMetadata?,
    playerConnection: PlayerConnection,
    onOpenFullscreenLyrics: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onShowAudioEffectsDialog: () -> Unit,
    likeButtonRefresh: Int,
    onLikeButtonRefresh: () -> Unit,
    hasActiveEffects: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPlayPauseOverlay by remember { mutableStateOf(false) }

    // Get current liked status from playerConnection
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val isPlaying by playerConnection.isPlaying.collectAsState()


    // Double-tap to like animation state
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var heartOffset by remember { mutableStateOf(Offset.Zero) }
    var showHeart by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(1f) }
    val heartSize = 120.dp

    // Enhanced particle effects
    var showParticles by remember { mutableStateOf(false) }
    val particleScale = remember { Animatable(0f) }
    val particleAlpha = remember { Animatable(0f) }

    // Smooth song transition state
    var isTransitioning by remember { mutableStateOf(false) }
    val transitionOffset = remember { Animatable(0f) }
    val transitionAlpha = remember { Animatable(1f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // Ckbar area - if so, don't handle it
                        val screenHeight = size.height
                        val seekbarAreaTop = screenHeight - 300.dp.toPx() // Increased seekbar area exclusion

                        if (offset.y < seekbarAreaTop) {
                            // Single tap for play/pause with visual feedback (only if not in seekbar area)
                            onPlayPauseClick()
                            showPlayPauseOverlay = true
                        }
                    },
                    onDoubleTap = { offset ->
                        // Check if tap is in seekbar area - if so, don't handle it
                        val screenHeight = size.height
                        val seekbarAreaTop = screenHeight - 300.dp.toPx() // Increased seekbar area exclusion

                        if (offset.y < seekbarAreaTop) {
                            // Double tap: like with Instagram-style heart animation (only if not in seekbar area)
                            heartOffset = offset

                            // Always show heart animation on double tap, regardless of current like status
                            showHeart = true
                            showParticles = true

                            // Only like, never unlike on double tap
                            if (currentSong?.song?.liked != true) {
                                playerConnection.toggleLike()
                                // Force refresh like button
                                onLikeButtonRefresh()
                            }

                            // Haptic feedback disabled for double tap

                            coroutineScope.launch {
                                // Instagram-style heart animation
                                heartScale.snapTo(0f)
                                heartAlpha.snapTo(1f)

                                // Bounce animation like Instagram
                                heartScale.animateTo(
                                    targetValue = 1.5f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )

                                // Settle back to normal size
                                heartScale.animateTo(1f, animationSpec = tween(100))

                                // Particle effects around heart
                                particleScale.snapTo(0f)
                                particleAlpha.snapTo(1f)
                                particleScale.animateTo(
                                    targetValue = 1.3f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessVeryLow
                                    )
                                )

                                // Keep visible briefly then fade out
                                delay(500)
                                heartAlpha.animateTo(0f, animationSpec = tween(150))
                                particleAlpha.animateTo(0f, animationSpec = tween(200))
                                showHeart = false
                                showParticles = false
                            }
                        }
                    },
                    onLongPress = { offset ->
                        // Check if tap is in seekbar area - if so, don't handle it
                        val screenHeight = size.height
                        val seekbarAreaTop = screenHeight - 200.dp.toPx() // Approximate seekbar area

                        if (offset.y < seekbarAreaTop) {
                            // Long press for lyrics (only if not in seekbar area)
                            onOpenFullscreenLyrics()
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { },
                    onDragEnd = { },
                    onHorizontalDrag = { change, dragAmount ->
                        // Check if drag is in seekbar area - if so, don't handle it
                        val screenHeight = size.height
                        val seekbarAreaTop = screenHeight - 300.dp.toPx() // Increased seekbar area exclusion

                        if (change.position.y < seekbarAreaTop) {
                            // Only handle swipe LEFT (negative dragAmount)
                            if (dragAmount < -50f) {
                                // Consume the event to prevent conflicts
                                change.consume()
                                // Swipe left to show audio effects dialog
                                android.util.Log.d("AudioEffects", "Swipe left detected")
                                onShowAudioEffectsDialog()
                            }
                            // Swipe right is ignored - no action needed
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                var lastSongChangeTime = 0L
                val minTimeBetweenChanges = 500L // Minimum 500ms between song changes
                
                detectVerticalDragGestures { change, dragAmount ->
                    // Full-screen vertical swipe detection (no seekbar exclusion)
                    val absoluteDrag = kotlin.math.abs(dragAmount)
                    val currentTime = System.currentTimeMillis()

                    // Lower threshold for smoother triggering; rate limited
                    if (absoluteDrag > 80f && (currentTime - lastSongChangeTime) > minTimeBetweenChanges) {
                        lastSongChangeTime = currentTime
                        
                        if (dragAmount > 0) {
                            // Swipe down - previous song
                            if (playerConnection.canSkipPrevious.value) {
                                playerConnection.seekToPrevious()
                                // Ensure playback starts
                                playerConnection.player.playWhenReady = true
                            }
                        } else {
                            // Swipe up - next song
                            if (playerConnection.canSkipNext.value) {
                                playerConnection.seekToNext()
                                // Ensure playback starts
                                playerConnection.player.playWhenReady = true
                            }
                        }
                        // Consume the drag to prevent multiple triggers
                        change.consume()
                    }
                }
            }
    ) {
        // Base background follows app theme color (upper/lower areas stay plain)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )

        // Centered 4:5 thumbnail with drop shadow
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val corner = 0.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.7f)
                    .then(
                        if (hasActiveEffects) {
                            Modifier.blur(15.dp)
                        } else {
                            Modifier
                        }
                    )
                    .shadow(50.dp, RoundedCornerShape(corner), clip = false, spotColor = Color.Black.copy(alpha = 0.7f), ambientColor = Color.Black.copy(alpha = 0.5f))
                    .clip(RoundedCornerShape(corner))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mediaMetadata?.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Shadow overlay on thumbnail to blend with background (stronger at top and bottom, increased lower shadow)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
                
                // Horizontal shadow overlay for side edges
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.1f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.1f)
                                )
                            )
                        )
                )

                // Top scrim to blend into background color (extended and stronger)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Bottom scrim to blend into background color (darker and extended)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
            }
        }

        // Play/Pause overlay icon that appears on tap
        AnimatedVisibility(
            visible = showPlayPauseOverlay,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(200))
        ) {
            LaunchedEffect(showPlayPauseOverlay) {
                if (showPlayPauseOverlay) {
                    delay(500)
                    showPlayPauseOverlay = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                val playbackState by playerConnection.playbackState.collectAsState()

                Image(
                    painter = painterResource(
                        if (playbackState == STATE_ENDED) {
                            R.drawable.replay
                        } else if (isPlaying) {
                            R.drawable.pause
                        } else {
                            R.drawable.play
                        }
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(70.dp)
                )
            }
        }

        // Dark overlay when song is paused
        AnimatedVisibility(
            visible = !isPlaying,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }

        // Instagram-style heart animation on double tap
        if (showHeart) {
            // Convert pixel offset to Dp
            val xDp = with(density) { heartOffset.x.toDp() }
            val yDp = with(density) { heartOffset.y.toDp() }

            // Main heart with Instagram-style styling
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF0050), // Instagram pink color
                modifier = Modifier
                    .size(heartSize)
                    .offset(x = xDp - heartSize / 2, y = yDp - heartSize / 2)
                    .scale(heartScale.value)
                    .alpha(heartAlpha.value)
            )

            // Glow effect behind heart
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF0050).copy(alpha = 0.3f),
                modifier = Modifier
                    .size(heartSize * 1.2f)
                    .offset(x = xDp - (heartSize * 1.2f) / 2, y = yDp - (heartSize * 1.2f) / 2)
                    .scale(heartScale.value * 0.8f)
                    .alpha(heartAlpha.value * 0.5f)
            )
        }

        // Instagram-style particle effects around heart
        if (showParticles) {
            val xDp = with(density) { heartOffset.x.toDp() }
            val yDp = with(density) { heartOffset.y.toDp() }

            // Create multiple heart particles around the main heart
            repeat(12) { index ->
                val angle = (index * 30f) * (Math.PI / 180f)
                val radius = 80.dp * particleScale.value
                val particleX = xDp + (radius * kotlin.math.cos(angle).toFloat())
                val particleY = yDp + (radius * kotlin.math.sin(angle).toFloat())

                // Instagram-style heart particles
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFF0050).copy(alpha = particleAlpha.value),
                    modifier = Modifier
                        .size(12.dp)
                        .offset(x = particleX - 6.dp, y = particleY - 6.dp)
                        .scale(particleScale.value * 0.5f)
                )
            }

            // Additional sparkle effects
            repeat(6) { index ->
                val angle = (index * 60f) * (Math.PI / 180f)
                val radius = 100.dp * particleScale.value
                val particleX = xDp + (radius * kotlin.math.cos(angle).toFloat())
                val particleY = yDp + (radius * kotlin.math.sin(angle).toFloat())

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .offset(x = particleX - 3.dp, y = particleY - 3.dp)
                        .background(
                            color = Color.White.copy(alpha = particleAlpha.value),
                            shape = RoundedCornerShape(3.dp)
                        )
                        .scale(particleScale.value * 0.8f)
                )
            }
        }

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    onOpenFullscreenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current

    val clipboardManager = LocalClipboardManager.current

    val playerConnection = LocalPlayerConnection.current ?: return

    // Ensure player connection is valid
    if (playerConnection.player == null) {
        return
    }

    // Use safe preference loading with fallbacks
    val playerTextAlignment by rememberEnumPreference(
        PlayerTextAlignmentKey,
        PlayerTextAlignment.SIDED
    )

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    // Force dark theme always - disable system theme detection completely
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    // Force dark theme always - ignore system theme completely
    val useDarkTheme = remember { true }
    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }
    // Force dark background - always use dark theme
    val useBlackBackground = remember(pureBlack) {
        pureBlack
    }
    val backgroundColor = if (useBlackBackground && state.value > state.collapsedBound) {
        lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    // Use safe state collection with proper error handling
    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }

    // Using absolute position seek like Playerold.kt

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }

    var changeColor by remember {
        mutableStateOf(false)
    }

    // Animations for background effects
    var backgroundImageUrl by remember { mutableStateOf<String?>(null) }
    val blurRadius by animateDpAsState(
        targetValue = if (state.isExpanded && playerBackground == PlayerBackgroundStyle.BLUR) 150.dp else 0.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "blurRadius"
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (state.isExpanded && playerBackground != PlayerBackgroundStyle.DEFAULT) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "backgroundAlpha"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = when {
            !state.isExpanded -> 0f
            playerBackground == PlayerBackgroundStyle.BLUR -> 0.3f
            playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2 -> 0.2f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "overlayAlpha"
    )

    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    LaunchedEffect(mediaMetadata, playerBackground) {
        // Update image URL for smooth transitions
        backgroundImageUrl = mediaMetadata?.thumbnailUrl

        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.BLUR) {
            gradientColors = listOf(Color.Black, Color.Black)
        }
        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.GRADIENT) {
            gradientColors = listOf(Color.Black, Color.Black)
        } else if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            withContext(Dispatchers.IO) {
                val result =
                    (
                            ImageLoader(context)
                                .execute(
                                    ImageRequest
                                        .Builder(context)
                                        .data(mediaMetadata?.thumbnailUrl)
                                        .allowHardware(false)
                                        .build(),
                                ).drawable as? BitmapDrawable
                            )?.bitmap?.extractGradientColors()

                result?.let {
                    gradientColors = it
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val changeBound = state.expandedBound / 3

    val TextBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            PlayerBackgroundStyle.BLUR -> Color.White
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    changeColor = true
                    Color.Black
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    changeColor = true
                    Color.White
                } else {
                    changeColor = false
                    Color.White
                }
            }
        }

    val icBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR -> Color.Black
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    changeColor = true
                    Color.White
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    changeColor = true
                    Color.Black
                } else {
                    changeColor = false
                    Color.Black
                }
            }
        }

    val (textButtonColor, iconButtonColor) = when (playerButtonsStyle) {
        PlayerButtonsStyle.DEFAULT -> Pair(TextBackgroundColor, icBackgroundColor)
        PlayerButtonsStyle.SECONDARY -> Pair(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )
    }

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    androidx.compose.material3.Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                    )

                    OutlinedButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        },
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    var showDetailsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDetailsDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showDetailsDialog = false },
            containerColor = if (useBlackBackground) Color.Black else AlertDialogDefaults.containerColor,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDetailsDialog = false },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    listOf(
                        stringResource(R.string.song_title) to mediaMetadata?.title,
                        stringResource(R.string.song_artists) to mediaMetadata?.artists?.joinToString { it.name },
                        stringResource(R.string.media_id) to mediaMetadata?.id,
                        "Itag" to currentFormat?.itag?.toString(),
                        stringResource(R.string.mime_type) to currentFormat?.mimeType,
                        stringResource(R.string.codecs) to currentFormat?.codecs,
                        stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                        stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                        stringResource(R.string.loudness) to currentFormat?.loudnessDb?.let { "$it dB" },
                        stringResource(R.string.volume) to "${(playerConnection.player.volume * 100).toInt()}%",
                        stringResource(R.string.file_size) to
                                currentFormat?.contentLength?.let {
                                    Formatter.formatShortFileSize(
                                        context,
                                        it
                                    )
                                },
                    ).forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            modifier =
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(displayText))
                                    },
                                ),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
        )
    }

    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues()
                .calculateBottomPadding(),
            expandedBound = state.expandedBound,
        )

/*
    var showAudioEffectsDialog by remember { mutableStateOf(false) }
    var likeButtonRefresh by remember { mutableStateOf(0) }

    // Audio Effects State Management
    val sharedPrefs =
        remember { context.getSharedPreferences("audio_effects", Context.MODE_PRIVATE) }

    // DSP Manager initialization
    val dspManager = remember { UniversalDSPManager.getInstance() }
    var dspInitialized by remember { mutableStateOf(false) }
    var dspDiagnostics by remember { mutableStateOf<org.json.JSONObject?>(null) }

    // Audio effects state variables
    var bassBoostEnabled by remember {
        mutableStateOf(
            sharedPrefs.getBoolean(
                "bass_boost_enabled",
                false
            )
        )
    }
    var bassBoost by remember { mutableStateOf(sharedPrefs.getFloat("bass_boost", 0f)) }
    var is3DEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("is_3d", false)) }
    var bassLevel by remember { mutableStateOf(sharedPrefs.getFloat("bass_level", 50f)) }
    var midLevel by remember { mutableStateOf(sharedPrefs.getFloat("mid_level", 50f)) }
    var trebleLevel by remember { mutableStateOf(sharedPrefs.getFloat("treble_level", 50f)) }
    var reverbEnabled by remember {
        mutableStateOf(
            sharedPrefs.getBoolean(
                "reverb_enabled",
                false
            )
        )
    }
    var loudnessGain by remember { mutableStateOf(sharedPrefs.getFloat("loudness_gain", 0f)) }
    var echoStrength by remember { mutableStateOf(sharedPrefs.getFloat("echo_strength", 0f)) }

    // Rain effect state variables
    var rainEffectEnabled by remember {
        mutableStateOf(
            sharedPrefs.getBoolean(
                "rain_effect_enabled",
                false
            )
        )
    }
    var rainIntensity by remember { mutableStateOf(sharedPrefs.getFloat("rain_intensity", 0f)) }
    var rainMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isRainPlaying by remember { mutableStateOf(false) }
*/
    var likeButtonRefresh by remember { mutableStateOf(0) }
    
    val sharedPrefs = remember { context.getSharedPreferences("audio_effects", Context.MODE_PRIVATE) }

    var hasActiveEffects by remember { mutableStateOf(false) }

    DisposableEffect(sharedPrefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, _ ->
            val preset = prefs.getString("preset", "NONE") ?: "NONE"
            val bass = prefs.getFloat("bass", 0f)
            val deep = prefs.getFloat("deep", 0f)
            val treble = prefs.getFloat("treble", 0f)
            val virt = prefs.getFloat("virt", 0f)
            val rv = prefs.getFloat("rv_wet", 0f)
            val ec = prefs.getFloat("ec_wet", 0f)
            
            hasActiveEffects = preset != "NONE" || bass > 0f || deep > 0f || treble > 0f || virt > 0f || rv > 0f || ec > 0f
        }
        sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
        listener.onSharedPreferenceChanged(sharedPrefs, null)
        onDispose {
            sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // Download dialog state - removed, using single download button now

    // Coroutine scope for background operations
    val scope = rememberCoroutineScope()

    // Combined download function - saves to app AND device
    fun downloadSong() {
        val videoId = mediaMetadata?.id ?: ""
        if (videoId.isEmpty()) return

        // Start both downloads in background
        scope.launch(Dispatchers.IO) {
            try {
                // 1. Download to app first
                try {
                    database.transaction {
                        mediaMetadata?.let { insert(it) }
                    }
                    val downloadRequest =
                        DownloadRequest
                            .Builder(videoId, videoId.toUri())
                            .setCustomCacheKey(videoId)
                            .setData((mediaMetadata?.title ?: "").toByteArray())
                            .build()
                    DownloadService.sendAddDownload(
                        context,
                        ExoDownloadService::class.java,
                        downloadRequest,
                        false,
                    )
                    withContext(Dispatchers.Main) {
                        android.util.Log.d("DownloadSong", "App download started")
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "DownloadSong",
                        "Failed to start app download: ${e.message}",
                        e
                    )
                }

                // 2. Download to device
                try {
                    val songTitle = mediaMetadata?.title ?: "Unknown"

                    // Try to get playback URL from database first
                    var audioUrl: String? = null
                    var retryCount = 0
                    val maxRetries = 3

                    // Try database first
                    try {
                        val format = database.format(videoId).first()
                        if (format != null && !format.playbackUrl.isNullOrEmpty()) {
                            audioUrl = format.playbackUrl
                            android.util.Log.d(
                                "DownloadSong",
                                "Got URL from database: ${audioUrl?.take(50)}..."
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.d(
                            "DownloadSong",
                            "No URL in database, will fetch from YTPlayerUtils"
                        )
                    }

                    // If not in database, fetch it using YTPlayerUtils with retry
                    while (audioUrl == null && retryCount < maxRetries) {
                        try {
                            val connectivityManager =
                                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                            if (connectivityManager == null) {
                                return@launch
                            }

                            val audioQuality = com.buxi.music.constants.AudioQuality.AUTO
                            val playbackData =
                                com.buxi.music.utils.YTPlayerUtils.playerResponseForPlayback(
                                    videoId,
                                    audioQuality = audioQuality,
                                    connectivityManager = connectivityManager
                                )

                            audioUrl = playbackData.getOrNull()?.streamUrl

                            if (audioUrl != null) {
                                android.util.Log.d(
                                    "DownloadSong",
                                    "Successfully got URL: ${audioUrl.take(50)}..."
                                )
                                break
                            } else {
                                retryCount++
                                if (retryCount < maxRetries) {
                                    android.util.Log.d(
                                        "DownloadSong",
                                        "Retry $retryCount/$maxRetries"
                                    )
                                    kotlinx.coroutines.delay((1000 * retryCount).toLong()) // Exponential backoff
                                } else {
                                    return@launch
                                }
                            }
                        } catch (e: Exception) {
                            retryCount++
                            android.util.Log.e(
                                "DownloadSong",
                                "Error fetching URL (attempt $retryCount): ${e.message}",
                                e
                            )
                            if (retryCount >= maxRetries) {
                                return@launch
                            }
                            kotlinx.coroutines.delay((1000 * retryCount).toLong())
                        }
                    }

                    if (audioUrl == null) {
                        return@launch
                    }

                    // Ensure URL has protocol
                    val finalUrl =
                        if (audioUrl.startsWith("http://") || audioUrl.startsWith("https://")) {
                            audioUrl
                        } else {
                            "https://$audioUrl"
                        }

                    android.util.Log.d(
                        "DownloadSong",
                        "Starting device download with URL: ${finalUrl.take(100)}..."
                    )

                    // Use FileDownloader to handle the download with proper verification
                    com.buxi.music.utils.FileDownloader.downloadFile(
                        context = context,
                        fileUrl = finalUrl,
                        fileName = songTitle,
                        songId = videoId,
                        database = database,
                        onSuccess = {
                            android.util.Log.d("DownloadSong", "Device download successful!")
                        },
                        onError = { errorMessage ->
                            android.util.Log.e("DownloadSong", "Device download failed: $errorMessage")
                        }
                    )
                } catch (e: Exception) {
                    android.util.Log.e("DownloadSong", "Download exception", e)
                }
            } catch (e: Exception) {
                android.util.Log.e("DownloadSong", "Download failed", e)
            }
        }
    }

    // Legacy function - keeping for compatibility but redirecting to downloadSong
    @Deprecated("Use downloadSong instead", ReplaceWith("downloadSong()"))
    fun downloadInApp() {
        downloadSong()
    }

    // Legacy function - keeping for compatibility but redirecting to downloadSong
    @Deprecated("Use downloadSong instead", ReplaceWith("downloadSong()"))
    fun downloadToPhone() {
        try {
            val songTitle = mediaMetadata?.title ?: "Unknown"
            val videoId = mediaMetadata?.id ?: ""

            // Start download in background
            scope.launch(Dispatchers.IO) {
                try {
                    // Try to get playback URL from database first
                    var audioUrl: String? = null
                    var retryCount = 0
                    val maxRetries = 3

                    // Try database first
                    try {
                        val format = database.format(videoId).first()
                        if (format != null && !format.playbackUrl.isNullOrEmpty()) {
                            audioUrl = format.playbackUrl
                            android.util.Log.d(
                                "DownloadToPhone",
                                "Got URL from database: ${audioUrl?.take(50)}..."
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.d(
                            "DownloadToPhone",
                            "No URL in database, will fetch from YTPlayerUtils"
                        )
                    }

                    // If not in database, fetch it using YTPlayerUtils with retry
                    while (audioUrl == null && retryCount < maxRetries) {
                        try {
                            val connectivityManager =
                                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                            if (connectivityManager == null) {
                                return@launch
                            }

                            val audioQuality = com.buxi.music.constants.AudioQuality.AUTO
                            val playbackData =
                                com.buxi.music.utils.YTPlayerUtils.playerResponseForPlayback(
                                    videoId,
                                    audioQuality = audioQuality,
                                    connectivityManager = connectivityManager
                                )

                            // Error logging only
                            android.util.Log.e("DownloadToPhone", "Error fetching URL: ${playbackData.exceptionOrNull()?.message}")

                            audioUrl = playbackData.getOrNull()?.streamUrl

                            if (audioUrl != null) {
                                android.util.Log.d(
                                    "DownloadToPhone",
                                    "Successfully got URL: ${audioUrl.take(50)}..."
                                )
                                break
                            } else {
                                retryCount++
                                if (retryCount < maxRetries) {
                                    android.util.Log.d(
                                        "DownloadToPhone",
                                        "Retry $retryCount/$maxRetries"
                                    )
                                    kotlinx.coroutines.delay((1000 * retryCount).toLong()) // Exponential backoff
                                } else {
                                    return@launch
                                }
                            }
                        } catch (e: Exception) {
                            retryCount++
                            android.util.Log.e(
                                "DownloadToPhone",
                                "Error fetching URL (attempt $retryCount): ${e.message}",
                                e
                            )
                            if (retryCount >= maxRetries) {
                                return@launch
                            }
                            kotlinx.coroutines.delay((1000 * retryCount).toLong())
                        }
                    }

                    if (audioUrl == null) {
                        return@launch
                    }

                    // Ensure URL has protocol
                    val finalUrl =
                        if (audioUrl.startsWith("http://") || audioUrl.startsWith("https://")) {
                            audioUrl
                        } else {
                            "https://$audioUrl"
                        }

                    android.util.Log.d(
                        "DownloadToPhone",
                        "Starting download with URL: ${finalUrl.take(100)}..."
                    )

                    android.util.Log.d("DownloadToPhone", "Starting download to device...")

                    // Use FileDownloader to handle the download
                    com.buxi.music.utils.FileDownloader.downloadFile(
                        context = context,
                        fileUrl = finalUrl,
                        fileName = songTitle,
                        songId = videoId,
                        database = database,
                        onSuccess = {
                            android.util.Log.d("DownloadToPhone", "Download successful!")
                        },
                        onError = { errorMessage ->
                            android.util.Log.e("DownloadToPhone", "Download failed: $errorMessage")
                        }
                    )
                } catch (e: Exception) {
                    android.util.Log.e("DownloadToPhone", "Download failed: ${e.message}")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DownloadToPhone", "Failed to start download", e)
            e.printStackTrace()
        }
    }


    var showAudioEffects by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showAudioQuality by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }
    var showHideSongDialog by remember { mutableStateOf(false) }

    BottomSheet(
        state = state,
        modifier = modifier,
        brushBackgroundColor =
            if (gradientColors.size >=
                2 &&
                state.value > changeBound
            ) {
                Brush.verticalGradient(gradientColors)
            } else {
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
                )
            },
        onDismiss = {
            playerConnection.service.clearAutomix()
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            RessoMiniPlayer(
                position = position,
                duration = duration,
                onExpand = { state.expandSoft() }
            )
        }
    ) {
        // Main content with full thumbnail background and gestures
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Resso Full-Screen Player Feed
            RessoPlayerFeed(
                modifier = Modifier.fillMaxSize(),
                onOpenAudioEffects = { showAudioEffects = true },
                onOpenQueue = { queueSheetState.expandSoft() },
                onOpenSleepTimer = { showSleepTimer = true },
                onOpenSpeed = { showSpeedSheet = true },
                onOpenAudioQuality = { showAudioQuality = true },
                onOpenMore = { showMoreSheet = true },
                onCollapse = { state.collapseSoft() }
            )





            if (false) { // Legacy overlay disabled in favor of RessoPlayerFeed
            // INSTAGRAM REELS STYLE VERTICAL ACTION BUTTONS - BOTTOM RIGHT
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 20.dp,
                        bottom = 120.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Like Button with count
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable {
                                playerConnection.toggleLike()
                                // Force refresh like button
                                likeButtonRefresh++
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Force refresh like button when likeButtonRefresh changes
                        LaunchedEffect(likeButtonRefresh) {
                            // This will trigger recomposition
                        }

                        Image(
                            painter = painterResource(
                                if (currentSong?.song?.liked == true) R.drawable.favorite
                                else R.drawable.favorite_border
                            ),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(
                                if (currentSong?.song?.liked == true) Color(0xFFFF0050)
                                else Color.White
                            ),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "Like",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp
                    )
                }

                // Download Button with progress
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable {
                                if (download?.state == Download.STATE_COMPLETED) {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        mediaMetadata?.id ?: "",
                                        false,
                                    )
                                } else {
                                    downloadSong()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (download?.state == Download.STATE_DOWNLOADING) {
                            val progress = download!!.percentDownloaded / 100f
                            val animatedProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = tween(
                                    durationMillis = 150,
                                    easing = FastOutSlowInEasing
                                ),
                                label = "downloadProgress"
                            )

                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            ) {
                                val strokeWidth = 3.dp.toPx()
                                val radius = (size.minDimension - strokeWidth) / 2
                                val center = Offset(size.width / 2, size.height / 2)

                                // Background circle
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.3f),
                                    radius = radius,
                                    center = center,
                                    style = Stroke(width = strokeWidth)
                                )

                                // Progress circle
                                if (animatedProgress > 0f) {
                                    drawArc(
                                        color = Color.White,
                                        startAngle = -90f,
                                        sweepAngle = 360f * animatedProgress,
                                        useCenter = false,
                                        topLeft = Offset(center.x - radius, center.y - radius),
                                        size = Size(radius * 2, radius * 2),
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )
                                }
                            }

                            // Small progress text in center
                            val progressPercent = (download!!.percentDownloaded).toInt()
                            Text(
                                text = "$progressPercent%",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.alpha(0.9f)
                            )
                        } else {
                            val iconResource = when (download?.state) {
                                Download.STATE_COMPLETED -> R.drawable.offline
                                Download.STATE_DOWNLOADING -> R.drawable.pause
                                Download.STATE_FAILED -> R.drawable.error
                                else -> R.drawable.download
                            }

                            val iconColor = when (download?.state) {
                                Download.STATE_FAILED -> Color(0xFFFF3048)
                                Download.STATE_COMPLETED -> Color(0xFF34C759)
                                else -> Color.White
                            }

                            Image(
                                painter = painterResource(iconResource),
                                contentDescription = when (download?.state) {
                                    Download.STATE_COMPLETED -> "Downloaded"
                                    Download.STATE_DOWNLOADING -> "Downloading..."
                                    Download.STATE_FAILED -> "Download failed"
                                    else -> "Download"
                                },
                                colorFilter = ColorFilter.tint(iconColor),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = when (download?.state) {
                            Download.STATE_COMPLETED -> "Saved"
                            Download.STATE_DOWNLOADING -> "Downloading"
                            Download.STATE_FAILED -> "Failed"
                            else -> "Download"
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp
                    )
                }


                // Audio Effects Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (hasActiveEffects) Color(0xFFFF0050).copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.6f))
                            .then(
                                if (hasActiveEffects) {
                                    Modifier.border(1.dp, Color(0xFFFF0050).copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                                } else Modifier
                            )
                            .clickable {
                                showAudioEffects = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.equalizer),
                            contentDescription = "Audio Effects",
                            colorFilter = ColorFilter.tint(if (hasActiveEffects) Color(0xFFFF0050) else Color.White),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "Effects",
                        color = if (hasActiveEffects) Color(0xFFFF0050) else Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp,
                        fontWeight = if (hasActiveEffects) FontWeight.Bold else FontWeight.Normal
                    )
                }


                // Sleep Timer Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable {
                                if (sleepTimerEnabled) {
                                    playerConnection.service.sleepTimer.clear()
                                } else {
                                    showSleepTimerDialog = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (sleepTimerEnabled) {
                            Text(
                                text = makeTimeString(sleepTimerTimeLeft),
                                color = Color(0xFFFFD600),
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Image(
                                painter = painterResource(R.drawable.bedtime),
                                contentDescription = "Sleep Timer",
                                colorFilter = ColorFilter.tint(Color.White),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = if (sleepTimerEnabled) "Stop Timer" else "Timer",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp
                    )
                }

            }

            // PROGRESS SLIDER - BOTTOM LEFT
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(
                        start = PlayerHorizontalPadding,
                        end = 80.dp,
                        bottom = 60.dp
                    )
            ) {
                // Small song info display above seekbar
                mediaMetadata?.let { md ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        AsyncImage(
                            model = md.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(Modifier.width(8.dp))

                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = md.title ?: "",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = md.artists.joinToString { it.name },
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Seekbar section
                when (sliderStyle) {
                    SliderStyle.DEFAULT -> {
                        androidx.compose.material3.Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    playerConnection.player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    SliderStyle.SQUIGGLY -> {
                        SquigglySlider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    playerConnection.player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                activeTickColor = Color.White,
                                inactiveTickColor = Color.White.copy(alpha = 0.3f),
                                thumbColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            squigglesSpec =
                                SquigglySlider.SquigglesSpec(
                                    amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                                    strokeWidth = 3.dp,
                                ),
                        )
                    }

                    SliderStyle.SLIM -> {
                        androidx.compose.material3.Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            onValueChange = {
                                sliderPosition = it.toLong()
                            },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    playerConnection.player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = makeTimeString(sliderPosition ?: position),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            } // end legacy overlay

            // Queue component - restored for three-dot menu access
            Queue(
                state = queueSheetState,
                playerBottomSheetState = state,
                navController = navController,
                backgroundColor = MaterialTheme.colorScheme.surface,
                onBackgroundColor = MaterialTheme.colorScheme.onSurface,
                TextBackgroundColor = MaterialTheme.colorScheme.surface
            )

            if (showAudioEffects) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { showAudioEffects = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    AudioEffectsScreen(onClose = { showAudioEffects = false })
                }
            }

            if (showSleepTimer) {
                com.buxi.music.ui.component.RessoSleepTimerSheet(
                    onDismiss = { showSleepTimer = false },
                    playerConnection = playerConnection
                )
            }

            if (showSpeedSheet) {
                com.buxi.music.ui.component.RessoSpeedSheet(
                    onDismiss = { showSpeedSheet = false },
                    playerConnection = playerConnection
                )
            }

            if (showAudioQuality) {
                com.buxi.music.ui.component.RessoAudioQualitySheet(
                    onDismiss = { showAudioQuality = false }
                )
            }

            if (showMoreSheet) {
                com.buxi.music.ui.component.RessoSongMoreSheet(
                    mediaMetadata = mediaMetadata,
                    isLiked = currentSong?.song?.liked == true,
                    onToggleLike = { playerConnection.toggleLike() },
                    onOpenSleepTimer = { showSleepTimer = true },
                    onOpenSpeed = { showSpeedSheet = true },
                    onOpenAudioQuality = { showAudioQuality = true },
                    onOpenAudioEffects = { showAudioEffects = true },
                    onAddToPlaylist = {
                        mediaMetadata?.let { song ->
                            menuState.show {
                                com.buxi.music.ui.menu.AddToPlaylistDialog(
                                    isVisible = true,
                                    onDismiss = menuState::dismiss,
                                    initialSongs = listOf(song)
                                )
                            }
                        }
                    },
                    onViewArtist = {
                        mediaMetadata?.artists?.firstOrNull()?.id?.let { artistId ->
                            navController.navigate("artist/$artistId")
                        }
                    },
                    onViewAlbum = {
                        mediaMetadata?.album?.id?.let { albumId ->
                            navController.navigate("album/$albumId")
                        }
                    },
                    onHideSong = { showHideSongDialog = true },
                    onDismiss = { showMoreSheet = false }
                )
            }

            if (showHideSongDialog) {
                com.buxi.music.ui.component.RessoHideSongDialog(
                    mediaMetadata = mediaMetadata,
                    onConfirmHide = {
                        playerConnection.player.seekToNextMediaItem()
                    },
                    onDismiss = { showHideSongDialog = false }
                )
            }
        }
    }


/*
            // Simple Audio Effects Dialog
            if (showAudioEffectsDialog) {
                android.util.Log.d("AudioEffects", "Creating audio effects dialog")
                SimpleAudioEffectsDialog(
                    bassBoost = bassBoost,
                    onBassBoostChange = { newValue ->
                        bassBoost = newValue
                        // Auto-enable bass boost when value > 0
                        if (newValue > 0) {
                            bassBoostEnabled = true
                        } else {
                            bassBoostEnabled = false
                        }
                        // Update animation state immediately for instant response
                        hasActiveEffects =
                            newValue > 0 || is3DEnabled || bassLevel != 50f || midLevel != 50f || trebleLevel != 50f || reverbEnabled || loudnessGain > 0 || echoStrength > 0
                        saveAudioEffectSettings()
                    },
                    is3DEnabled = is3DEnabled,
                    on3DToggle = { enabled ->
                        is3DEnabled = enabled
                        // Update animation state immediately for instant response
                        hasActiveEffects =
                            bassBoost > 0 || enabled || bassLevel != 50f || midLevel != 50f || trebleLevel != 50f || reverbEnabled || loudnessGain > 0 || echoStrength > 0
                        saveAudioEffectSettings()
                    },
                    reverbEnabled = reverbEnabled,
                    onReverbToggle = { enabled ->
                        reverbEnabled = enabled
                        // Update animation state immediately for instant response
                        hasActiveEffects =
                            bassBoost > 0 || is3DEnabled || bassLevel != 50f || midLevel != 50f || trebleLevel != 50f || enabled || loudnessGain > 0 || echoStrength > 0
                        saveAudioEffectSettings()
                    },
                    bassLevel = bassLevel,
                    onBassLevelChange = { newValue ->
                        bassLevel = newValue
                        saveAudioEffectSettings()
                    },
                    midLevel = midLevel,
                    onMidLevelChange = { newValue ->
                        midLevel = newValue
                        saveAudioEffectSettings()
                    },
                    trebleLevel = trebleLevel,
                    onTrebleLevelChange = { newValue ->
                        trebleLevel = newValue
                        saveAudioEffectSettings()
                    },
                    loudnessGain = loudnessGain,
                    onLoudnessGainChange = { newValue ->
                        loudnessGain = newValue
                        saveAudioEffectSettings()
                    },
                    echoStrength = echoStrength,
                    onEchoStrengthChange = { newValue ->
                        echoStrength = newValue
                        saveAudioEffectSettings()
                    },
                    rainIntensity = rainIntensity,
                    onRainIntensityChange = { newValue ->
                        rainIntensity = newValue
                        saveAudioEffectSettings()
                    },
                    onResetAll = {
                        resetAllAudioEffects()
                    },
                    onClose = { showAudioEffectsDialog = false },
                    dspDiagnostics = dspDiagnostics,
                    dspInitialized = dspInitialized
                )
            }
*/


/*
@Composable
fun AudioEffectsAnimation( ... ) { ... }
*/

/*
@Composable
fun HologramThumbnailEffect( ... ) { ... }
*/

/*
@Composable
fun FallbackAudioEffectsDialog(
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.6f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.equalizer),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Audio Effects",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Audio effects are temporarily unavailable. Please try again later.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

*/

@Composable
fun AnimatedProgressBar(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "progress_animation")
    val animatedProgress = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress_glow"
    )

    val progress = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val progressWidth = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(6.dp)
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(3.dp)
            )
    ) {
        // Animated progress track with glow effect
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progressWidth)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        ),
                        startX = 0f,
                        endX = Float.POSITIVE_INFINITY
                    ),
                    shape = RoundedCornerShape(3.dp)
                )
        )

        // Glowing animation overlay
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(20.dp)
                    .offset(x = (progressWidth * 300f).dp - 10.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.6f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = Float.POSITIVE_INFINITY
                        ),
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}

}
