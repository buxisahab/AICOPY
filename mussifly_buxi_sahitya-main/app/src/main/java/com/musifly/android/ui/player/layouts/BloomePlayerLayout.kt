package com.musifly.android.ui.player.layouts

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.offline.Download
import com.musifly.android.ui.component.DownloadButton
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.SliderStyle
import com.musifly.android.db.entities.LyricsEntity
import com.musifly.android.ui.component.Lyrics
import com.musifly.android.ui.theme.extractGradientColors
import com.musifly.android.utils.HdThumbnailUtil
import com.musifly.android.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.player.components.CircularGlassIconButton
import com.musifly.android.ui.player.components.CircularGlassDownloadButton

// Bloome theme colors (from Flutter Music theme)
private val BloomeBackground = Color(0xFF0C0409) // Dark purple/black
private val BloomePrimary1 = Color(0xFFDAEAF7) // Light blue
private val BloomeAccent1 = Color(0xFF0EA5E0) // Blue (Sky Blue)
private val BloomeAccent2 = Color(0xFFFE385E) // Pink/red

/**
 * Bloome-style player layout with all Flutter app animations
 * Features:
 * - Animated background shadow (radial gradient that fades on tab switch)
 * - Animated play/pause button (color transition + glow effect)
 * - Gradient progress bar with animated colors
 * - Smooth tab transitions
 * - Volume drag controller on cover image
 * - Exact Flutter app layout structure
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloomePlayerLayout(
    playerState: PlayerState,
    playerActions: PlayerActions,
    onOpenFullscreenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
    sliderStyle: SliderStyle = SliderStyle.DEFAULT
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()
    
    // Stream engine state
    val streamEngine by playerConnection?.streamEngine?.collectAsState(initial = null) ?: remember { mutableStateOf<String?>(null) }
    val useNewPipeStreaming = streamEngine?.contains("NewPipe", ignoreCase = true) == true || 
                             streamEngine?.contains("JioSaavn", ignoreCase = true) == true
    
    // Tab state for switching between cover image and lyrics
    val pagerState = rememberPagerState(pageCount = { 2 })
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // Slider state
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    val currentPosition = sliderPosition ?: playerState.position
    val totalDuration = if (playerState.duration == C.TIME_UNSET) 0L else playerState.duration
    
    // Sync slider override with actual player position updates to ensure smooth finish
    LaunchedEffect(playerState.position) {
        sliderPosition?.let { target ->
            // If player reports position within 2s of target, we consider the seek confirmed
            if (kotlin.math.abs(playerState.position - target) < 2000L) {
                sliderPosition = null
            }
        }
    }
    
    // Thumbnail URL
    val thumbnailUrl = remember(playerState.mediaMetadata?.id, playerState.mediaMetadata?.thumbnailUrl) {
        playerState.mediaMetadata?.let { metadata ->
            HdThumbnailUtil.getBestThumbnailUrl(metadata) ?: metadata.thumbnailUrl
        }
    }
    
    // Animated background shadow color (extracted from album art)
    var shadowColor by remember { mutableStateOf<Color?>(null) }
    var cachedShadowColor by remember { mutableStateOf<Color>(Color(0xFFA32C73)) } // Default pink-purple
    
    // Extract color from album art for background shadow
    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl != null) {
            withContext(Dispatchers.IO) {
                try {
                    val result = ImageLoader(context)
                        .execute(
                            ImageRequest.Builder(context)
                                .data(thumbnailUrl)
                                .allowHardware(false)
                                .build()
                        ).drawable as? BitmapDrawable
                    
                    result?.bitmap?.let { bitmap ->
                        val colors = bitmap.extractGradientColors(darkTheme = true)
                        if (colors.isNotEmpty()) {
                            shadowColor = colors.first()
                            cachedShadowColor = colors.first()
                        }
                    }
                } catch (_: Exception) {
                    // Use cached color on error
                }
            }
        }
    }
    
    val finalShadowColor = shadowColor ?: cachedShadowColor
    
    // Tab animation offset (0.0 = cover tab, 1.0 = lyrics tab)
    val tabAnimationOffset by remember {
        derivedStateOf {
            pagerState.currentPage + pagerState.currentPageOffsetFraction
        }
    }
    
    // Background shadow opacity (fades when switching to lyrics tab)
    val shadowOpacity by animateFloatAsState(
        targetValue = (1f - tabAnimationOffset).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "shadowOpacity"
    )
    
    // Simplified synchronization: Pager is the source of truth for current page
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
    }
    
    LaunchedEffect(pagerState.currentPage) {
        selectedTabIndex = pagerState.currentPage
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BloomeBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Animated Background Shadow (AmbientImgShadowWidget equivalent)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = size.width * 0.65f
                
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            finalShadowColor.copy(alpha = 0.30f * shadowOpacity),
                            Color.Transparent
                        ),
                        center = Offset(centerX, centerY),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar with App Name, Stream Engine, Sound Effects, and More button (Instagram Reels style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - App Name only
                Text(
                    text = "Mussifly",
                    color = BloomePrimary1,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                // Right side - Search, Stream Engine, Sound Effects, and More button
                val isLiquidGlass = isLiquidGlassActive()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search button (first on right)
                    if (isLiquidGlass) {
                        CircularGlassIconButton(
                            painter = painterResource(R.drawable.search),
                            contentDescription = "Search",
                            onClick = playerActions.onOpenSearch,
                            buttonSize = 40.dp,
                            iconSize = 20.dp
                        )
                    } else {
                        IconButton(onClick = playerActions.onOpenSearch) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = "Search",
                                tint = BloomePrimary1,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    

                    
                    // Sound Effects
                    if (isLiquidGlass) {
                        CircularGlassIconButton(
                            painter = painterResource(R.drawable.sound_effect),
                            contentDescription = "Sound effects",
                            onClick = playerActions.onOpenSoundSettings,
                            buttonSize = 40.dp,
                            iconSize = 20.dp
                        )
                    } else {
                        IconButton(onClick = playerActions.onOpenSoundSettings) {
                            Icon(
                                painter = painterResource(R.drawable.sound_effect),
                                contentDescription = "Sound effects",
                                tint = BloomePrimary1,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Sleep Timer button
                    if (isLiquidGlass) {
                        CircularGlassIconButton(
                            painter = painterResource(R.drawable.bedtime),
                            contentDescription = "Sleep Timer",
                            onClick = playerActions.onOpenSleepTimer,
                            buttonSize = 40.dp,
                            iconSize = 20.dp
                        )
                    } else {
                        IconButton(onClick = playerActions.onOpenSleepTimer) {
                            Icon(
                                painter = painterResource(R.drawable.bedtime),
                                contentDescription = "Sleep Timer",
                                tint = BloomePrimary1,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    // More button (Instagram style)
                    if (isLiquidGlass) {
                        CircularGlassIconButton(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = "More options",
                            onClick = playerActions.onOpenMenu,
                            buttonSize = 40.dp,
                            iconSize = 20.dp
                        )
                    } else {
                        IconButton(onClick = playerActions.onOpenMenu) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = "More options",
                                tint = BloomePrimary1,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = BloomePrimary1,
                indicator = { },
                divider = { }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { 
                        Text(
                            "Cover", 
                            color = if (selectedTabIndex == 0) BloomeAccent2 else BloomePrimary1.copy(alpha = 0.6f)
                        ) 
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { 
                        Text(
                            "Lyrics", 
                            color = if (selectedTabIndex == 1) BloomeAccent2 else BloomePrimary1.copy(alpha = 0.6f)
                        ) 
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tab Content with smooth transitions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true
                ) { page ->
                    when (page) {
                        0 -> {
                            // Cover Image Tab with Volume Drag support
                            CoverImageWithVolumeControl(
                                thumbnailUrl = thumbnailUrl,
                                context = context,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        1 -> {
                            // Lyrics Tab
                            LyricsTabContent(
                                playerState = playerState,
                                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                                onCloseLyrics = { selectedTabIndex = 0 }, // Switch back to Cover tab
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Song Info Row with Queue button
            SongInfoRow(
                title = playerState.mediaMetadata?.title ?: "Unknown",
                artist = playerState.mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
                isLiked = playerState.isLiked,
                onToggleLike = playerActions.onToggleLike,
                onDownload = playerActions.onDownload,
                onOpenDownloads = playerActions.onOpenDownloads,
                download = playerState.download,
                onOpenQueue = playerActions.onOpenQueue
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Gradient Progress Bar
            GradientProgressBar(
                currentPosition = currentPosition,
                totalDuration = totalDuration,
                isPlaying = playerState.isPlaying,
                onSeek = { pos ->
                    sliderPosition = pos
                },
                onSeekFinished = {
                    sliderPosition?.let { playerActions.onSeek(it) }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(25.dp))
            
            // Player Controls with Animated Play/Pause Button
            PlayerControlsRow(
                playerState = playerState,
                playerActions = playerActions,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Cover Image with Volume Drag Control and Horizontal Swipe for Lyrics
 */
@Composable
private fun CoverImageWithVolumeControl(
    thumbnailUrl: String?,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    var volumeOffset by remember { mutableFloatStateOf(0f) }
    val playerConnection = LocalPlayerConnection.current
    
    Box(
        modifier = modifier
            .padding(16.dp)
            .pointerInput(Unit) {
                // Vertical drag gesture for volume control
                // Using detectVerticalDragGestures ensures horizontal swipes 
                // aren't consumed and can reach the HorizontalPager
                detectVerticalDragGestures(
                    onDragStart = { },
                    onDragEnd = {
                        volumeOffset = 0f
                    },
                    onDragCancel = {
                        volumeOffset = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        volumeOffset += dragAmount
                        // Volume control logic can be added here
                        // For example: playerConnection?.setVolume(...)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(thumbnailUrl)
                .crossfade(300)
                .allowHardware(false)
                .build(),
            contentDescription = null,
            placeholder = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
            error = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(25.dp))
                .shadow(8.dp, RoundedCornerShape(25.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Lyrics Tab Content - Uses proper Lyrics component with synchronization
 */
@Composable
private fun LyricsTabContent(
    playerState: PlayerState,
    onOpenFullscreenLyrics: () -> Unit,
    onCloseLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current
    
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Lyrics(
            sliderPositionProvider = { 
                // Use real-time player position for proper sync with animation
                playerConnection?.player?.currentPosition ?: playerState.position
            },
            onNavigateBack = null, // Not fullscreen in tab
            modifier = Modifier.fillMaxSize(),
            forceShow = true // Force show lyrics in Bloome player
        )
        
        // Close button for lyrics (top right) - Instagram Reels style
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onCloseLyrics,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = "Close lyrics",
                    tint = BloomePrimary1,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Song Info Row
 */
@Composable
private fun SongInfoRow(
    title: String,
    artist: String,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onDownload: () -> Unit,
    onOpenDownloads: () -> Unit,
    download: Download?,
    onOpenQueue: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title and Artist
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                color = BloomePrimary1,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artist,
                color = BloomePrimary1.copy(alpha = 0.7f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
        
        // Queue, Download and Like buttons
        val isLiquidGlass = isLiquidGlassActive()
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Queue button (Instagram style)
            if (isLiquidGlass) {
                CircularGlassIconButton(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = "Queue",
                    onClick = onOpenQueue,
                    buttonSize = 44.dp,
                    iconSize = 20.dp
                )
            } else {
                IconButton(
                    onClick = onOpenQueue,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = "Queue",
                        tint = BloomePrimary1.copy(alpha = 0.7f),
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
            
            // Download button
            if (isLiquidGlass) {
                CircularGlassDownloadButton(
                    download = download,
                    onClick = onDownload,
                    onLongClick = onOpenDownloads,
                    buttonSize = 44.dp,
                    iconSize = 20.dp
                )
            } else {
                DownloadButton(
                    download = download,
                    onClick = onDownload,
                    onLongClick = onOpenDownloads,
                    modifier = Modifier.size(40.dp),
                    iconSize = 28.dp,
                    tint = BloomePrimary1.copy(alpha = 0.5f)
                )
            }
            
            // Like button
            if (isLiquidGlass) {
                CircularGlassIconButton(
                    painter = painterResource(
                        if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                    ),
                    contentDescription = "Like",
                    onClick = onToggleLike,
                    buttonSize = 44.dp,
                    iconSize = 20.dp,
                    tint = if (isLiked) BloomeAccent2 else Color.White
                )
            } else {
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = "Like",
                        tint = if (isLiked) BloomeAccent2 else BloomePrimary1.copy(alpha = 0.7f),
                        modifier = Modifier.size(25.dp)
                    )
                }
            }
        }
    }
}

/**
 * Gradient Progress Bar with animated colors
 */
@Composable
private fun GradientProgressBar(
    currentPosition: Long,
    totalDuration: Long,
    isPlaying: Boolean,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressRatio = if (totalDuration > 0) {
        (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    
    var sliderValue by remember { mutableFloatStateOf(progressRatio) }
    
    // Update slider value when position changes (but not when dragging)
    LaunchedEffect(currentPosition, totalDuration) {
        if (totalDuration > 0) {
            val newProgress = (currentPosition.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
            if (abs(sliderValue - newProgress) > 0.01f) { // Only update if significant change
                sliderValue = newProgress
            }
        }
    }
    
    // Animated colors based on playing state
    val activeColor by animateColorAsState(
        targetValue = if (isPlaying) BloomeAccent1 else BloomeAccent2,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "activeColor"
    )
    
    // Generate gradient colors
    val activeGradient = remember(activeColor) {
        listOf(
            activeColor.copy(alpha = 0.8f),
            activeColor
        )
    }
    
    Column(modifier = modifier) {
        // Gradient Progress Bar
        @OptIn(ExperimentalMaterial3Api::class)
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                sliderValue = newValue
                val seekPosition = (newValue * totalDuration).toLong()
                onSeek(seekPosition)
            },
            onValueChangeFinished = {
                onSeekFinished()
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = BloomePrimary1.copy(alpha = 0.1f)
            ),
            track = { sliderState ->
                // Custom gradient track
                Box(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Inactive track
                        drawLine(
                            color = BloomePrimary1.copy(alpha = 0.1f),
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        
                        // Active track with gradient
                        val progress = sliderState.value.coerceIn(0f, 1f)
                        val activeWidth = size.width * progress
                        if (activeWidth > 0) {
                            drawLine(
                                brush = Brush.horizontalGradient(activeGradient),
                                start = Offset(0f, size.height / 2),
                                end = Offset(activeWidth, size.height / 2),
                                strokeWidth = 6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            },
            thumb = {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        // Time indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = makeTimeString(currentPosition),
                color = BloomePrimary1.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
            Text(
                text = makeTimeString(totalDuration),
                color = BloomePrimary1.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Player Controls Row with Animated Play/Pause Button
 */
@Composable
private fun PlayerControlsRow(
    playerState: PlayerState,
    playerActions: PlayerActions,
    modifier: Modifier = Modifier
) {
    // Animated play/pause button color and glow
    val buttonColor by animateColorAsState(
        targetValue = if (playerState.isPlaying) BloomeAccent1 else BloomeAccent2,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "buttonColor"
    )
    
    val glowColor by animateColorAsState(
        targetValue = if (playerState.isPlaying) {
            BloomeAccent1.copy(alpha = 0.6f)
        } else {
            BloomeAccent2.copy(alpha = 0.6f)
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "glowColor"
    )
    
    val isLiquidGlass = isLiquidGlassActive()
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shuffle
        if (isLiquidGlass) {
            CircularGlassIconButton(
                painter = painterResource(R.drawable.shuffle),
                contentDescription = "Shuffle",
                onClick = playerActions.onToggleShuffle,
                buttonSize = 48.dp,
                iconSize = 22.dp,
                tint = if (playerState.shuffleModeEnabled) BloomeAccent1 else Color.White
            )
        } else {
            IconButton(
                onClick = playerActions.onToggleShuffle,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = "Shuffle",
                    tint = if (playerState.shuffleModeEnabled) BloomeAccent1 else BloomePrimary1.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Previous
        if (isLiquidGlass) {
            CircularGlassIconButton(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = "Previous",
                onClick = playerActions.onPrevious,
                buttonSize = 56.dp,
                iconSize = 28.dp,
                tint = if (playerState.canSkipPrevious) Color.White else Color.White.copy(alpha = 0.3f)
            )
        } else {
            IconButton(
                onClick = playerActions.onPrevious,
                enabled = playerState.canSkipPrevious,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = "Previous",
                    tint = if (playerState.canSkipPrevious) BloomePrimary1 else BloomePrimary1.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Animated Play/Pause Button with Glow
        Box(
            modifier = Modifier
                .size(75.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    ambientColor = glowColor,
                    spotColor = glowColor
                )
                .background(buttonColor, shape = CircleShape)
                .clickable { playerActions.onPlayPause() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = playerState.isPlaying,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                        scaleIn(initialScale = 0.8f, animationSpec = tween(200)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)) +
                            scaleOut(targetScale = 0.8f, animationSpec = tween(200))
                        )
                },
                label = "playPauseIcon"
            ) { isPlaying ->
                Icon(
                    painter = painterResource(
                        if (isPlaying) R.drawable.pause else R.drawable.play
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(35.dp)
                )
            }
        }
        
        // Next
        if (isLiquidGlass) {
            CircularGlassIconButton(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = "Next",
                onClick = playerActions.onNext,
                buttonSize = 56.dp,
                iconSize = 28.dp,
                tint = if (playerState.canSkipNext) Color.White else Color.White.copy(alpha = 0.3f)
            )
        } else {
            IconButton(
                onClick = playerActions.onNext,
                enabled = playerState.canSkipNext,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = "Next",
                    tint = if (playerState.canSkipNext) BloomePrimary1 else BloomePrimary1.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Repeat
        if (isLiquidGlass) {
            CircularGlassIconButton(
                painter = painterResource(
                    when (playerState.repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                        Player.REPEAT_MODE_ALL -> R.drawable.repeat
                        else -> R.drawable.repeat
                    }
                ),
                contentDescription = "Repeat",
                onClick = playerActions.onToggleRepeat,
                buttonSize = 48.dp,
                iconSize = 22.dp,
                tint = if (playerState.repeatMode != Player.REPEAT_MODE_OFF) BloomeAccent1 else Color.White
            )
        } else {
            IconButton(
                onClick = playerActions.onToggleRepeat,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(
                        when (playerState.repeatMode) {
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            else -> R.drawable.repeat
                        }
                    ),
                    contentDescription = "Repeat",
                    tint = if (playerState.repeatMode != Player.REPEAT_MODE_OFF) BloomeAccent1 else BloomePrimary1.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

