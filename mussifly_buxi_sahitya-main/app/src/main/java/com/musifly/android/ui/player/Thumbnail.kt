package com.musifly.android.ui.player


import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.ui.component.FullscreenHdThumbnail
import com.musifly.android.ui.utils.extractYouTubeVideoId
import com.musifly.android.ui.utils.getYouTubeThumbnailUrls
import com.musifly.android.utils.HdThumbnailUtil
import com.musifly.android.R
import com.musifly.android.constants.HapticFeedbackEnabledKey
import com.musifly.android.constants.HapticFeedbackIntensity
import com.musifly.android.constants.HapticFeedbackIntensityKey
import com.musifly.android.constants.ShowLyricsKey
import com.musifly.android.constants.SwipeThumbnailKey
import com.musifly.android.constants.VerticalSwipeSongChangeKey
import com.musifly.android.extensions.metadata
import com.musifly.android.ui.component.Lyrics
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    onOpenFullscreenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
    changeColor: Boolean = false,
    onScrollOffsetChanged: (Float) -> Unit = {}, // Expose scroll offset to parent
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val currentView = LocalView.current
    val haptic = LocalHapticFeedback.current
    
    // Get Vibrator service for direct haptic control
    val vibrator = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        }
    }

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    
    // Get best thumbnail URL using HdThumbnailUtil (handles JioSaavn quality upgrades)
    val bestThumbnailUrl = remember(mediaMetadata?.id, mediaMetadata?.thumbnailUrl) {
        val url = com.musifly.android.utils.HdThumbnailUtil.getBestThumbnailUrl(mediaMetadata)
            ?: mediaMetadata?.thumbnailUrl
        android.util.Log.d("Thumbnail", "📸 Thumbnail URL computed: id=${mediaMetadata?.id}, rawThumbnailUrl=${mediaMetadata?.thumbnailUrl?.take(40)}, finalUrl=${url?.take(80)}")
        url
    }
    
    // Instagram Reels-style: Prefetch next/previous images for smooth transitions
    val imageLoader = context.imageLoader
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val queueWindows by playerConnection.queueWindows.collectAsState()
    
    LaunchedEffect(currentWindowIndex, queueWindows) {
        // Prefetch next image
        if (currentWindowIndex + 1 < queueWindows.size) {
            val nextMetadata = queueWindows[currentWindowIndex + 1].mediaItem.metadata
            nextMetadata?.thumbnailUrl?.let { url ->
                imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(url)
                        .build()
                )
            }
        }
        // Prefetch previous image
        if (currentWindowIndex > 0) {
            val prevMetadata = queueWindows[currentWindowIndex - 1].mediaItem.metadata
            prevMetadata?.thumbnailUrl?.let { url ->
                imageLoader.enqueue(
                    ImageRequest.Builder(context)
                        .data(url)
                        .build()
                )
            }
        }
    }

    var showLyrics by rememberPreference(ShowLyricsKey, false)
    val swipeThumbnail by rememberPreference(SwipeThumbnailKey, true)
    val verticalSwipeEnabled by rememberPreference(VerticalSwipeSongChangeKey, defaultValue = true)
    val hapticEnabled by rememberPreference(HapticFeedbackEnabledKey, defaultValue = true)
    val hapticIntensity by rememberPreference(HapticFeedbackIntensityKey, defaultValue = HapticFeedbackIntensity.MEDIUM.name)
    
    // Thumbnail Display Mode
    val thumbnailMode by rememberEnumPreference(com.musifly.android.constants.ThumbnailDisplayModeKey, com.musifly.android.constants.ThumbnailDisplayMode.BLUR_HD)

    // Enable haptic feedback on the view
    androidx.compose.runtime.SideEffect {
        currentView.isHapticFeedbackEnabled = true
    }
    
    DisposableEffect(showLyrics) {
        currentView.keepScreenOn = showLyrics
        onDispose {
            currentView.keepScreenOn = false
        }
    }

    // ==================== CLEAN INSTAGRAM REELS IMPLEMENTATION ====================
    // Simple state: No complex scrolling, just swipe detection
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeightPx = remember(configuration.screenHeightDp) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    
    // Simple swipe tracking
    var swipeOffset by remember { mutableFloatStateOf(0f) }
    
    // Auto-play when thumbnail appears
    LaunchedEffect(mediaMetadata) {
        if (mediaMetadata != null && !playerConnection.isPlaying.value) {
            delay(300)
            playerConnection.player.play()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = !showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            // Clean swipe detection layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(verticalSwipeEnabled, canSkipNext, canSkipPrevious) {
                        if (verticalSwipeEnabled) {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    swipeOffset = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    swipeOffset += dragAmount
                                },
                                onDragEnd = {
                                    val threshold = screenHeightPx * 0.12f // 12% - Easy swipe!
                                    
                                    coroutineScope.launch {
                                        when {
                                            // Swipe up = next
                                            swipeOffset < -threshold && canSkipNext -> {
                                                // Haptic feedback
                                                vibrator?.let { vib ->
                                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                        vib.vibrate(android.os.VibrationEffect.createOneShot(30, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                                    } else {
                                                        @Suppress("DEPRECATION")
                                                        vib.vibrate(30)
                                                    }
                                                }
                                                swipeOffset = 0f
                                                playerConnection.player.seekToNext()
                                                playerConnection.player.play()
                                            }
                                            // Swipe down = previous
                                            swipeOffset > threshold && canSkipPrevious -> {
                                                // Haptic feedback
                                                vibrator?.let { vib ->
                                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                                        vib.vibrate(android.os.VibrationEffect.createOneShot(30, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                                                    } else {
                                                        @Suppress("DEPRECATION")
                                                        vib.vibrate(30)
                                                    }
                                                }
                                                swipeOffset = 0f
                                                playerConnection.player.seekToPrevious()
                                                playerConnection.player.play()
                                            }
                                            else -> {
                                                swipeOffset = 0f
                                            }
                                        }
                                    }
                                },
                                onDragCancel = {
                                    swipeOffset = 0f
                                }
                            )
                        }
                    }
            ) {
                // State for play/pause icon visibility
                var showPlayPauseIcon by remember { mutableStateOf(false) }
                
                // Multiple like animations (Instagram Reels style)
                data class LikeAnimation(
                    val id: Int,
                    val position: Offset,
                    val colorIndex: Int,
                    val timestamp: Long
                )
                
                val coroutineScope = rememberCoroutineScope()
                var likeAnimations by remember { mutableStateOf<List<LikeAnimation>>(emptyList()) }
                var likeAnimationIdCounter by remember { mutableStateOf(0) }
                var isDoubleTapping by remember { mutableStateOf(false) }
                
                // Track single tap with delay to distinguish from double tap
                var pendingSingleTap by remember { mutableStateOf<Offset?>(null) }
                var singleTapJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

                // PURE CONTENT DISPLAY - Instagram Reels animation
                // Content moves slightly with finger for tactile feedback
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Instagram Reels: Dampened movement (30% of swipe)
                            translationY = swipeOffset * 0.3f
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    // Delay single tap to check if it's part of a double tap
                                    singleTapJob?.cancel()
                                    pendingSingleTap = offset
                                    singleTapJob = coroutineScope.launch {
                                        delay(300) // Wait to see if double tap happens
                                        if (!isDoubleTapping && pendingSingleTap != null) {
                                            // Single tap = pause (if playing) or play (if paused)
                                            val isPlaying = playerConnection.isPlaying.value
                                            if (isPlaying) {
                                                playerConnection.player.pause()
                                            } else {
                                                playerConnection.player.play()
                                            }
                                            showPlayPauseIcon = true
                                            pendingSingleTap = null
                                        }
                                    }
                                },
                                onLongPress = {
                                    onOpenFullscreenLyrics()
                                },
                                onDoubleTap = { offset ->
                                    // Prevent single tap from triggering
                                    isDoubleTapping = true
                                    singleTapJob?.cancel()
                                    pendingSingleTap = null
                                    
                                    // Instagram-style: double tap to like with haptic feedback
                                    vibrator?.let { vib ->
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            vib.vibrate(
                                                android.os.VibrationEffect.createOneShot(
                                                    50, // 50ms for double-tap like
                                                    android.os.VibrationEffect.DEFAULT_AMPLITUDE
                                                )
                                            )
                                        } else {
                                            @Suppress("DEPRECATION")
                                            vib.vibrate(50)
                                        }
                                    }
                                    
                                    // Add new like animation at tap position
                                    val newAnimation = LikeAnimation(
                                        id = likeAnimationIdCounter++,
                                        position = offset,
                                        colorIndex = likeAnimationIdCounter % 5,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    likeAnimations = likeAnimations + newAnimation
                                    
                                    // Toggle like in database
                                    playerConnection.service.toggleLike()
                                    
                                    // Reset double tap flag after a short delay
                                    coroutineScope.launch {
                                        delay(350)
                                        isDoubleTapping = false
                                    }
                                },
                            )
                        }
                ) {
                    val isPlaying by playerConnection.isPlaying.collectAsState()

                    // Auto-hide play/pause icon after 1 second (faster like Instagram)
                    LaunchedEffect(showPlayPauseIcon) {
                        if (showPlayPauseIcon) {
                            delay(1000)
                            showPlayPauseIcon = false
                        }
                    }
                    
                    // Auto-remove like animations after 1.2 seconds
                    LaunchedEffect(likeAnimations.size) {
                        likeAnimations.forEach { animation ->
                            launch {
                                delay(1200)
                                likeAnimations = likeAnimations.filter { it.id != animation.id }
                            }
                        }
                    }

                    // ==================== THUMBNAIL RENDERING MODES ====================
                    
                    when (thumbnailMode) {
                        com.musifly.android.constants.ThumbnailDisplayMode.BLUR_HD -> {
                            // MODE 1: Blur Background + HD Thumbnail (Instagram/ReelsStyle)
                            
                            // 1. Blurred background ImageView (centerCrop)
                            // Use key to force reload when thumbnailUrl changes (important for JioSaavn enrichment)
                            key(bestThumbnailUrl) {
                                com.musifly.android.ui.component.HdThumbnail(
                                    thumbnailUrl = bestThumbnailUrl,
                                    videoId = mediaMetadata?.id,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(50.dp)
                                        .alpha(0.6f),
                                    contentScale = ContentScale.Crop,
                                    crossfadeEnabled = true
                                )
                            }
                            
                            // 2. Fullscreen thumbnail (fills entire screen - Instagram Reels style)
                            // Use key to force reload when thumbnailUrl changes (important for JioSaavn enrichment)
                            key(bestThumbnailUrl) {
                                com.musifly.android.ui.component.HdThumbnail(
                                    thumbnailUrl = bestThumbnailUrl,
                                    videoId = mediaMetadata?.id,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    crossfadeEnabled = true
                                )
                            }
                            
                            // 3. Top gradient overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.35f)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xCC000000), // #CC000000
                                                Color(0x66000000), // #66000000
                                                Color(0x00000000)  // #00000000
                                            )
                                        )
                                    )
                            )
                            
                            // 4. Bottom gradient overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(0.45f)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x00000000), // #00000000
                                                Color(0x80000000), // #80000000
                                                Color(0xE6000000)  // #E6000000
                                            )
                                        )
                                    )
                            )
                        }
                        
                        com.musifly.android.constants.ThumbnailDisplayMode.FIT_CENTER -> {
                            // MODE 2: FitCenter Mode (No Crop, No Blur)
                            // Add blur background ONLY if empty spaces appear
                            
                            // Blurred background (visible in empty spaces)
                            // Use key to force reload when thumbnailUrl changes (important for JioSaavn enrichment)
                            key(bestThumbnailUrl) {
                                com.musifly.android.ui.component.HdThumbnail(
                                    thumbnailUrl = bestThumbnailUrl,
                                    videoId = mediaMetadata?.id,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(80.dp)
                                        .alpha(0.5f),
                                    contentScale = ContentScale.Crop,
                                    crossfadeEnabled = true
                                )
                            }
                            
                            // Main thumbnail (fitCenter)
                            // Use key to force reload when thumbnailUrl changes (important for JioSaavn enrichment)
                            key(bestThumbnailUrl) {
                                com.musifly.android.ui.component.HdThumbnail(
                                    thumbnailUrl = bestThumbnailUrl,
                                    videoId = mediaMetadata?.id,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit,
                                    crossfadeEnabled = true
                                )
                            }
                        }
                        
                        com.musifly.android.constants.ThumbnailDisplayMode.CENTER_CROP -> {
                            // MODE 3: CenterCrop Mode (Immersive Fullscreen)
                            
                            // Thumbnail fills entire 9:16 screen
                            // Use key to force reload when thumbnailUrl changes (important for JioSaavn enrichment)
                            key(bestThumbnailUrl) {
                                com.musifly.android.ui.component.HdThumbnail(
                                    thumbnailUrl = bestThumbnailUrl,
                                    videoId = mediaMetadata?.id,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    crossfadeEnabled = true
                                )
                            }
                            
                            // Slight bottom gradient to improve text readability
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.6f),
                                                Color.Black.copy(alpha = 0.9f)
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // Play/Pause Icon Overlay (Instagram Reels style)
                    AnimatedVisibility(
                        visible = showPlayPauseIcon,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isPlaying) R.drawable.pause else R.drawable.play
                                ),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                    
                    // Multiple Like Animations (Instagram Reels style - appears wherever user double taps)
                    val density = LocalDensity.current
                    likeAnimations.forEach { animation ->
                        val heartColors = listOf(
                            Color(0xFFE91E63), // Pink
                            Color(0xFFFF1744), // Red
                            Color(0xFFFF6F00), // Orange
                            Color(0xFFFF4081), // Deep Pink
                            Color(0xFFFF5252)  // Light Red
                        )
                        val currentColor = heartColors[animation.colorIndex]
                        val isVisible = (System.currentTimeMillis() - animation.timestamp) < 1200
                        
                        if (isVisible) {
                            // Convert pixel offset to IntOffset (center the heart icon)
                            val heartSizePx = with(density) { 55.dp.toPx() } // Half of 110.dp
                            val positionX = (animation.position.x - heartSizePx).toInt()
                            val positionY = (animation.position.y - heartSizePx).toInt()
                            
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(100)) + scaleIn(
                                    initialScale = 0.3f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ),
                                exit = fadeOut(tween(500)) + scaleOut(
                                    targetScale = 1.5f,
                                    animationSpec = tween(500)
                                ),
                                modifier = Modifier
                                    .offset { IntOffset(positionX, positionY) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Outer glow effect
                                    Icon(
                                        painter = painterResource(R.drawable.favorite),
                                        contentDescription = null,
                                        tint = currentColor.copy(alpha = 0.3f),
                                        modifier = Modifier.size(140.dp)
                                    )
                                    // Main heart
                                    Icon(
                                        painter = painterResource(R.drawable.favorite),
                                        contentDescription = null,
                                        tint = currentColor,
                                        modifier = Modifier.size(110.dp)
                                    )
                        }
                    }
                }
            }
        }
    }
}

        AnimatedVisibility(
            visible = showLyrics && error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Lyrics(
                sliderPositionProvider = sliderPositionProvider,
            )
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .padding(32.dp)
                    .align(Alignment.Center),
        ) {
            error?.let { error ->
                // Determine stream type for better error context
                val currentMediaItem = playerConnection.player.currentMediaItem
                val streamType = when {
                    currentMediaItem?.localConfiguration?.uri?.scheme == "file" ||
                    currentMediaItem?.localConfiguration?.uri?.scheme == "content" ||
                    currentMediaItem?.mediaId?.startsWith("local_") == true -> "local"
                    currentMediaItem?.mediaId?.startsWith("saavn:") == true -> "saavn"
                    else -> "youtube"
                }
                
                PlaybackError(
                    error = error,
                    streamType = streamType,
                    retry = {
                        // ENHANCED RETRY: Reset retry counters and retry playback
                        // User-initiated retry should reset automatic retry counters
                        playerConnection.service.resetRetryCounters()
                        val currentMediaItemRetry = playerConnection.player.currentMediaItem
                        if (currentMediaItemRetry != null) {
                            playerConnection.service.retryPlayback()
                        } else {
                            playerConnection.player.prepare()
                        }
                    },
                )
            }
        }
    }
}

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

    var thumbnailCornerRadiusV2: Float
        get() = prefs.getFloat("THUMBNAIL_CORNER_RADIUS", 16f)
        set(value) = prefs.edit() { putFloat("THUMBNAIL_CORNER_RADIUS", value) }
}