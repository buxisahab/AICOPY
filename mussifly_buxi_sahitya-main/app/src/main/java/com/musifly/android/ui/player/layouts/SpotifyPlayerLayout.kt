package com.musifly.android.ui.player.layouts

import android.graphics.drawable.BitmapDrawable
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.offline.Download
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.SliderStyle
import com.musifly.android.db.entities.LyricsEntity
import com.musifly.android.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.musifly.android.di.LyricsHelperEntryPoint
import com.musifly.android.secure.SecureString
import com.musifly.android.ui.component.DownloadButton
import com.musifly.android.ui.component.Lyrics
import com.musifly.android.ui.component.PlayerSliderTrack
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.player.components.CircularGlassButton
import com.musifly.android.ui.player.components.CircularGlassIconButton
import com.musifly.android.ui.player.components.GlassActionBar
import com.musifly.android.ui.theme.extractGradientColors
import com.musifly.android.utils.HdThumbnailUtil
import com.musifly.android.utils.rememberPreference
import com.musifly.android.constants.FullScreenModeKey
import com.musifly.android.utils.makeTimeString
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import kotlin.math.absoluteValue

// Spotify brand colors
private val SpotifyGreen = Color(0xFF1DB954)
private val SpotifyBlack = Color(0xFF121212)
private val SpotifyDarkGray = Color(0xFF181818)
private val SpotifyLightGray = Color(0xFFB3B3B3)
private val SpotifyWhite = Color(0xFFFFFFFF)

/**
 * Spotify-style player layout - Fixed version
 * Features:
 * - Single album art (no duplicates)
 * - Perfect 1:1 aspect ratio
 * - No layout overflow
 * - Proper spacing and padding
 * - Swipe up for lyrics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyPlayerLayout(
    playerState: PlayerState,
    playerActions: PlayerActions,
    onOpenFullscreenLyrics: () -> Unit,
    onCollapse: () -> Unit = {},  // Back navigation callback
    sliderStyle: SliderStyle = SliderStyle.DEFAULT,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current
    val database = LocalDatabase.current
    
    // Audio device detection
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    val (isFullScreen) = rememberPreference(FullScreenModeKey, defaultValue = false)
    var audioDeviceType by remember { mutableStateOf<AudioDeviceType>(AudioDeviceType.SPEAKER) }
    
    // Initial detection
    LaunchedEffect(Unit) {
        updateAudioDeviceType(audioManager) { type ->
            audioDeviceType = type
        }
    }
    
    // Set up device change listener for real-time updates with proper cleanup
    DisposableEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    updateAudioDeviceType(audioManager) { type ->
                        audioDeviceType = type
                    }
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    updateAudioDeviceType(audioManager) { type ->
                        audioDeviceType = type
                    }
                }
            }
            audioManager.registerAudioDeviceCallback(callback, null)
            
            // Cleanup on dispose
            onDispose {
                audioManager.unregisterAudioDeviceCallback(callback)
            }
        } else {
            // For older versions, no callback available, just initial detection
            onDispose { }
        }
    }
    
    // For older Android versions, poll periodically
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                updateAudioDeviceType(audioManager) { type ->
                    audioDeviceType = type
                }
            }
        }
    }
    
    // Single artwork state - prevents duplicate rendering
    val artwork by remember(playerState.mediaMetadata?.id) {
        mutableStateOf(
            playerState.mediaMetadata?.let { metadata ->
                HdThumbnailUtil.getBestThumbnailUrl(metadata) ?: metadata.thumbnailUrl
            }
        )
    }
    
    // Swipe state for horizontal swipe gesture
    var swipeOffset by remember { mutableStateOf(0f) }
    
    // Slider state
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    val currentPosition = sliderPosition ?: playerState.position
    val totalDuration = if (playerState.duration == C.TIME_UNSET) 0L else playerState.duration
    
    // Dynamic gradient colors from album art
    var gradientColors by remember { mutableStateOf(listOf(SpotifyDarkGray, SpotifyBlack)) }
    
    // Sync slider override with actual player position updates to ensure smooth finish
    LaunchedEffect(playerState.position) {
        sliderPosition?.let { target ->
            // If player reports position within 2s of target, we consider the seek confirmed
            if (kotlin.math.abs(playerState.position - target) < 2000L) {
                sliderPosition = null
            }
        }
    }
    
    // Lyrics state
    val playerConnection = LocalPlayerConnection.current
    val lyricsEntity by playerConnection?.currentLyrics?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }
    val hasLyrics = !lyrics.isNullOrEmpty() && lyrics != LYRICS_NOT_FOUND && lyrics != "No lyrics found"
    val isLyricsNotFound = lyrics == LYRICS_NOT_FOUND || lyrics == "No lyrics found"
    val isLiquidGlass = isLiquidGlassActive()
    var showLyricsContent by remember { mutableStateOf(false) }
    var isCheckingForLyrics by remember(playerState.mediaMetadata?.id) { mutableStateOf(false) }
    
    // Auto-Fetch Lyrics Logic (Same as ModernVerticalPlayerLayout)
    val currentSongId = playerState.mediaMetadata?.id
    val mediaMetadata = playerState.mediaMetadata
    
    LaunchedEffect(currentSongId) {
        if (currentSongId != null && mediaMetadata != null) {
            isCheckingForLyrics = true
            withContext(Dispatchers.IO) {
                try {
                    val existingLyrics = try {
                        database.getLyrics(currentSongId)
                    } catch (e: Exception) { null }
                    
                    if (existingLyrics == null) {
                        // Not in DB, try to fetch
                        val entryPoint = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            LyricsHelperEntryPoint::class.java
                        )
                        val lyricsHelper = entryPoint.lyricsHelper()
                        val fetchedLyrics = lyricsHelper.getLyrics(mediaMetadata)
                        
                        if (!fetchedLyrics.isNullOrBlank()) {
                            database.query {
                                upsert(LyricsEntity(currentSongId, fetchedLyrics))
                            }
                        } else {
                            // Mark as not found to stop the "Checking..." state and show the error
                            database.query {
                                upsert(LyricsEntity(currentSongId, LYRICS_NOT_FOUND))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isCheckingForLyrics = false
                }
            }
        }
    }
    
    // Debug: Log when lyrics state changes
    LaunchedEffect(hasLyrics, showLyricsContent) {
        android.util.Log.d("SpotifyPlayerLayout", "🎵 Lyrics state: hasLyrics=$hasLyrics, showLyricsContent=$showLyricsContent")
    }
    
    // Extract colors from album art
    LaunchedEffect(artwork) {
        if (artwork != null) {
            withContext(Dispatchers.IO) {
                try {
                    val result = ImageLoader(context)
                        .execute(
                            ImageRequest.Builder(context)
                                .data(artwork)
                                .allowHardware(false)
                                .build()
                        ).drawable as? BitmapDrawable
                    
                    result?.bitmap?.extractGradientColors()?.let { colors ->
                        gradientColors = colors.map { 
                            it.copy(alpha = 0.8f) 
                        }.ifEmpty { listOf(SpotifyDarkGray, SpotifyBlack) }
                    }
                } catch (e: Exception) {
                    gradientColors = listOf(SpotifyDarkGray, SpotifyBlack)
                }
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = gradientColors + listOf(SpotifyBlack),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
            .pointerInput(playerState.canSkipNext, playerState.canSkipPrevious) {
                detectHorizontalDragGestures(
                    onDragStart = { swipeOffset = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        val adjustedDragAmount =
                            if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                        val allowLeft = adjustedDragAmount < 0 && playerState.canSkipNext
                        val allowRight = adjustedDragAmount > 0 && playerState.canSkipPrevious
                        if (allowLeft || allowRight) {
                            swipeOffset += adjustedDragAmount
                        }
                    },
                    onDragEnd = {
                        val threshold = 100f
                        if (swipeOffset.absoluteValue > threshold) {
                            val isRightSwipe = swipeOffset > 0
                            if (isRightSwipe && playerState.canSkipPrevious) {
                                playerActions.onPrevious()
                            } else if (!isRightSwipe && playerState.canSkipNext) {
                                playerActions.onNext()
                            }
                        }
                        swipeOffset = 0f
                    },
                    onDragCancel = { swipeOffset = 0f }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .navigationBarsPadding()
                .then(if (isFullScreen) Modifier.padding(top = 16.dp, bottom = 16.dp) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with collapse and menu - Compact
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Back button
                if (isLiquidGlass) {
                    CircularGlassIconButton(
                        painter = painterResource(R.drawable.sahitya_down),
                        contentDescription = "Back",
                        onClick = onCollapse,
                        buttonSize = 40.dp,
                        iconSize = 24.dp
                    )
                } else {
                    IconButton(onClick = onCollapse) {
                        Icon(
                            painter = painterResource(R.drawable.expand_more),
                            contentDescription = "Back",
                            tint = SpotifyWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "PLAYING FROM",
                        color = SpotifyLightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = playerState.mediaMetadata?.album?.title ?: "Your Library",
                        color = SpotifyWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                
                // Right side: Search + Menu
                if (isLiquidGlass) {
                    GlassActionBar {
                        IconButton(
                            onClick = playerActions.onOpenSearch,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = playerActions.onOpenMenu,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = "More",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = playerActions.onOpenSearch) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = "Search",
                                tint = SpotifyWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onClick = playerActions.onOpenMenu) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = "More",
                                tint = SpotifyWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Album Art - Maintain original size with rounded corners
            // Shows lyrics on long press in thumbnail position
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)  // Original size: 85% of width
                    .aspectRatio(1f)  // Perfect square
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(24.dp))
                    .pointerInput(hasLyrics) {
                        detectTapGestures(
                            onTap = { /* Consume tap to help gesture detection */ },
                            onLongPress = {
                                if (hasLyrics) {
                                    android.util.Log.d("SpotifyPlayerLayout", "🔵 Long press detected! Toggling lyrics")
                                    showLyricsContent = !showLyricsContent
                                } else {
                                    android.widget.Toast.makeText(context, "No lyrics available", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = showLyricsContent && hasLyrics,
                    animationSpec = tween(durationMillis = 300),
                    modifier = Modifier.fillMaxSize()
                ) { showLyrics ->
                    if (!showLyrics) {
                        // Album art - Original size with rounded corners
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            if (artwork != null && artwork!!.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(artwork)
                                        .crossfade(true)
                                        .allowHardware(false)
                                        .build(),
                                    contentDescription = "Album art",
                                    contentScale = ContentScale.Crop,
                                    placeholder = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                                    error = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(SpotifyDarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                                        contentDescription = "Album art placeholder",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }
                            
                            // Lyrics hint or "no lyrics" message
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                            ) {
                                if (hasLyrics && !showLyricsContent) {
                                    // Long press hint for available lyrics
                                    Text(
                                        text = "Long press for lyrics",
                                        color = SpotifyWhite.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        modifier = Modifier
                                            .background(
                                                Color.Black.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                } else if (isCheckingForLyrics) {
                                    // Searching state
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .background(
                                                Color.Black.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = SpotifyGreen,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Checking for lyrics...",
                                            color = SpotifyWhite.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                } else if (isLyricsNotFound) {
                                    // Explicitly confirmed NOT FOUND state
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .background(
                                                Color.Black.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.music_note),
                                            contentDescription = null,
                                            tint = SpotifyWhite.copy(alpha = 0.7f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Lyrics not found",
                                            color = SpotifyWhite.copy(alpha = 0.7f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Lyrics display - In thumbnail position with matching corners
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            val playerConnection = LocalPlayerConnection.current
                            
                            Lyrics(
                                sliderPositionProvider = { 
                                    // Use real-time player position for proper sync with animation
                                    playerConnection?.player?.currentPosition ?: playerState.position
                                },
                                onNavigateBack = null,
                                modifier = Modifier.fillMaxSize(),
                                forceShow = true
                            )
                            
                            // Close button for lyrics (top right) - Spotify style
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                IconButton(
                                    onClick = { showLyricsContent = false },
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
                                        tint = SpotifyWhite,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Song info row - Very compact to save space
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = playerState.mediaMetadata?.title ?: "Unknown",
                    color = SpotifyWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = playerState.mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
                    color = SpotifyLightGray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { /* Navigate to artist */ },
                    textAlign = TextAlign.Center
                )
                

            }
            
            // Follow button row with Shuffle, Repeat, and Like buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Follow button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpotifyWhite)
                        .clickable {
                            SecureString.openLink(context, SecureString.LINK_INSTA_APP)
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Follow",
                        color = SpotifyBlack,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Right side - Shuffle, Repeat, and Like buttons
                val isLiquidGlass = isLiquidGlassActive()
                if (isLiquidGlass) {
                    GlassActionBar {
                        IconButton(
                            onClick = playerActions.onToggleShuffle,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = "Shuffle",
                                tint = if (playerState.shuffleModeEnabled) SpotifyGreen else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = playerActions.onToggleRepeat,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    when (playerState.repeatMode) {
                                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                        else -> R.drawable.repeat
                                    }
                                ),
                                contentDescription = "Repeat",
                                tint = if (playerState.repeatMode != Player.REPEAT_MODE_OFF) SpotifyGreen else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = playerActions.onToggleLike,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (playerState.isLiked) R.drawable.favorite else R.drawable.favorite_border
                                ),
                                contentDescription = "Like",
                                tint = if (playerState.isLiked) SpotifyGreen else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = playerActions.onToggleShuffle,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = "Shuffle",
                                tint = if (playerState.shuffleModeEnabled) SpotifyGreen else SpotifyLightGray,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = playerActions.onToggleRepeat,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    when (playerState.repeatMode) {
                                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                        else -> R.drawable.repeat
                                    }
                                ),
                                contentDescription = "Repeat",
                                tint = if (playerState.repeatMode != Player.REPEAT_MODE_OFF) SpotifyGreen else SpotifyLightGray,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(onClick = playerActions.onToggleLike) {
                            Icon(
                                painter = painterResource(
                                    if (playerState.isLiked) R.drawable.favorite else R.drawable.favorite_border
                                ),
                                contentDescription = "Like",
                                tint = if (playerState.isLiked) SpotifyGreen else SpotifyWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            
            // Progress bar - Very compact
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 8.dp)
            ) {
                when (sliderStyle) {
                    SliderStyle.DEFAULT -> {
                        Slider(
                            value = currentPosition.toFloat(),
                            valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = {
                                sliderPosition?.let { pos ->
                                    playerActions.onSeek(pos)
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = SpotifyWhite,
                                activeTrackColor = SpotifyWhite,
                                inactiveTrackColor = SpotifyLightGray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    SliderStyle.SQUIGGLY -> {
                        SquigglySlider(
                            value = currentPosition.toFloat(),
                            valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = {
                                sliderPosition?.let { pos ->
                                    playerActions.onSeek(pos)
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = SpotifyWhite,
                                activeTrackColor = SpotifyWhite,
                                inactiveTrackColor = SpotifyLightGray.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            squigglesSpec = SquigglySlider.SquigglesSpec(
                                amplitude = if (playerState.isPlaying) 2.dp else 0.dp,
                                strokeWidth = 3.dp
                            )
                        )
                    }
                    SliderStyle.SLIM -> {
                        Slider(
                            value = currentPosition.toFloat(),
                            valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = {
                                sliderPosition?.let { pos ->
                                    playerActions.onSeek(pos)
                                    sliderPosition = null
                                }
                            },
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState ->
                                PlayerSliderTrack(
                                    sliderState = sliderState,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = SpotifyWhite,
                                        inactiveTrackColor = SpotifyLightGray.copy(alpha = 0.3f)
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Time indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = makeTimeString(currentPosition),
                        color = SpotifyLightGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = makeTimeString(totalDuration),
                        color = SpotifyLightGray,
                        fontSize = 12.sp
                    )
                }
            }
            
            // Main playback controls - Spotify style (Previous, Play/Pause, Next)
            // Controls must ALWAYS be visible without scrolling
            val isLiquidGlass = isLiquidGlassActive()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                if (isLiquidGlass) {
                    CircularGlassIconButton(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = "Previous",
                        onClick = playerActions.onPrevious,
                        buttonSize = 56.dp,
                        iconSize = 36.dp,
                        tint = if (playerState.canSkipPrevious) Color.White else Color.White.copy(alpha = 0.5f)
                    )
                } else {
                    IconButton(
                        onClick = playerActions.onPrevious,
                        enabled = playerState.canSkipPrevious,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous",
                            tint = if (playerState.canSkipPrevious) SpotifyWhite else SpotifyLightGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
                
                // Play/Pause button - Large white circle (Spotify style)
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .clip(CircleShape)
                        .background(SpotifyWhite)
                        .clickable { playerActions.onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (playerState.isPlaying) R.drawable.pause else R.drawable.play
                        ),
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = SpotifyBlack,
                        modifier = Modifier.size(40.dp)
                    )
                }
                
                // Next button
                if (isLiquidGlass) {
                    CircularGlassIconButton(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        onClick = playerActions.onNext,
                        buttonSize = 56.dp,
                        iconSize = 36.dp,
                        tint = if (playerState.canSkipNext) Color.White else Color.White.copy(alpha = 0.5f)
                    )
                } else {
                    IconButton(
                        onClick = playerActions.onNext,
                        enabled = playerState.canSkipNext,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            tint = if (playerState.canSkipNext) SpotifyWhite else SpotifyLightGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }
            
            // Bottom row - Additional controls - Spotify style (compact to save space)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Device picker (Spotify Connect style) - Active with device detection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { 
                        // Open device picker or show device info
                        android.widget.Toast.makeText(
                            context,
                            "Audio output: ${audioDeviceType.displayName}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Icon(
                        painter = painterResource(audioDeviceType.iconRes),
                        contentDescription = "Audio device",
                        tint = SpotifyGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = audioDeviceType.displayName,
                        color = SpotifyGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Right side - Action buttons (Spotify style - compact grouping)
                if (isLiquidGlass) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sound and Download pair
                        GlassActionBar {
                            IconButton(
                                onClick = playerActions.onOpenSoundSettings,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.sound_effect),
                                    contentDescription = "Audio settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            DownloadButton(
                                download = playerState.download,
                                onClick = playerActions.onDownload,
                                onLongClick = playerActions.onOpenDownloads,
                                modifier = Modifier.size(40.dp),
                                iconSize = 20.dp,
                                tint = Color.White
                            )
                        }
                        
                        // Timer and Queue pair
                        GlassActionBar {
                            IconButton(
                                onClick = playerActions.onOpenSleepTimer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.bedtime),
                                    contentDescription = "Sleep Timer",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = playerActions.onOpenQueue,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.queue_music),
                                    contentDescription = "Queue",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sound Effects
                        IconButton(
                            onClick = playerActions.onOpenSoundSettings,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.sound_effect),
                                contentDescription = "Audio settings",
                                tint = SpotifyLightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Download button
                        DownloadButton(
                            download = playerState.download,
                            onClick = playerActions.onDownload,
                            onLongClick = playerActions.onOpenDownloads,
                            modifier = Modifier.size(40.dp),
                            iconSize = 24.dp,
                            tint = SpotifyLightGray
                        )
                        
                        // Timer button
                        IconButton(
                            onClick = playerActions.onOpenSleepTimer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.bedtime),
                                contentDescription = "Sleep Timer",
                                tint = SpotifyLightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Queue button
                        IconButton(
                            onClick = playerActions.onOpenQueue,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = "Queue",
                                tint = SpotifyLightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Audio device type enum
 */
private enum class AudioDeviceType(
    val displayName: String,
    val iconRes: Int
) {
    SPEAKER("This device", R.drawable.sahitya_speaker),
    WIRED_HEADSET("Headphones", R.drawable.sahitya_earphones), // Fallback to computer icon
    BLUETOOTH("Bluetooth", R.drawable.sahitya_earbud) // Fallback to computer icon
}

/**
 * Detect current audio output device type
 */
private fun updateAudioDeviceType(
    audioManager: AudioManager,
    onDeviceTypeDetected: (AudioDeviceType) -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        try {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            var bluetoothDevice = false
            var wiredHeadset = false
            
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        bluetoothDevice = true
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        wiredHeadset = true
                    }
                }
            }
            
            val deviceType = when {
                bluetoothDevice -> AudioDeviceType.BLUETOOTH
                wiredHeadset -> AudioDeviceType.WIRED_HEADSET
                else -> AudioDeviceType.SPEAKER
            }
            
            onDeviceTypeDetected(deviceType)
        } catch (e: Exception) {
            // Fallback to speaker if detection fails
            onDeviceTypeDetected(AudioDeviceType.SPEAKER)
        }
    } else {
        // For older Android versions, check routing
        try {
            val isWiredHeadset = audioManager.isWiredHeadsetOn
            val isBluetoothScoOn = audioManager.isBluetoothScoOn
            val isBluetoothA2dpOn = audioManager.isBluetoothA2dpOn
            
            val deviceType = when {
                isBluetoothScoOn || isBluetoothA2dpOn -> AudioDeviceType.BLUETOOTH
                isWiredHeadset -> AudioDeviceType.WIRED_HEADSET
                else -> AudioDeviceType.SPEAKER
            }
            
            onDeviceTypeDetected(deviceType)
        } catch (e: Exception) {
            onDeviceTypeDetected(AudioDeviceType.SPEAKER)
        }
    }
}




