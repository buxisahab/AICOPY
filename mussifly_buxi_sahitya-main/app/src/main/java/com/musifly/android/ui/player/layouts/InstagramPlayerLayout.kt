package com.musifly.android.ui.player.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.musifly.android.R
import com.musifly.android.constants.SliderStyle
import com.musifly.android.constants.ThumbnailDisplayMode
import com.musifly.android.constants.ThumbnailDisplayModeKey
import com.musifly.android.constants.FullScreenModeKey
import com.musifly.android.ui.component.InstagramLikeAnimation
import com.musifly.android.ui.component.PlayerSliderTrack
import com.musifly.android.utils.HdThumbnailUtil
import com.musifly.android.utils.makeTimeString
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference
import me.saket.squiggles.SquigglySlider

/**
 * Instagram Reels-style fullscreen player layout
 * Features:
 * - Fullscreen vertical layout (9:16)
 * - Background = full-size video thumbnail with centerCrop
 * - Right-side vertical icons: Queue, Like, Sound, Download, Timer
 * - Bottom: Thumbnail + Title + Artist + Slider + Playback controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstagramPlayerLayout(
    playerState: PlayerState,
    playerActions: PlayerActions,
    onOpenFullscreenLyrics: () -> Unit,
    sliderStyle: SliderStyle = SliderStyle.DEFAULT,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Thumbnail display mode preference
    val thumbnailMode by rememberEnumPreference(
        key = ThumbnailDisplayModeKey,
        defaultValue = ThumbnailDisplayMode.BLUR_HD
    )
    
    // Instagram-style double tap to like animation state
    var showLikeAnimation by remember { mutableStateOf(false) }
    
    // Slider state
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    val currentPosition = sliderPosition ?: playerState.position
    val totalDuration = if (playerState.duration == C.TIME_UNSET) 0L else playerState.duration
    
    // Thumbnail URL - recompute when metadata ID or thumbnailUrl changes
    // CRITICAL: For JioSaavn, thumbnailUrl might be empty initially and get populated after enrichment
    // Watch playerState.mediaMetadata directly - it's already reactive via collectAsState()
    val metadata = playerState.mediaMetadata
    
    // Compute best thumbnail URL - match Thumbnail.kt approach exactly
    // Watch both metadata object reference AND thumbnailUrl string to catch enrichment updates
    val thumbnailUrl = remember(metadata?.id, metadata?.thumbnailUrl) {
        val url = if (metadata != null) {
            // Use HdThumbnailUtil which handles JioSaavn quality upgrades
            val bestUrl = HdThumbnailUtil.getBestThumbnailUrl(metadata)
            // If HdThumbnailUtil returns null but we have a thumbnailUrl, use it directly
            bestUrl ?: metadata.thumbnailUrl
        } else {
            null
        }
        android.util.Log.d("InstagramPlayerLayout", "📸 Thumbnail URL computed: id=${metadata?.id}, rawThumbnailUrl=${metadata?.thumbnailUrl?.take(40)}, finalUrl=${url?.take(80)}")
        url
    }
    
    // Force recomposition when thumbnailUrl changes - important for JioSaavn metadata enrichment
    LaunchedEffect(thumbnailUrl) {
        android.util.Log.d("InstagramPlayerLayout", "🔄 Thumbnail URL changed: ${thumbnailUrl?.take(80)}")
    }
    
    // Watch for metadata changes to catch enrichment updates
    // This ensures we recompose when JioSaavn metadata gets enriched
    LaunchedEffect(metadata?.id, metadata?.thumbnailUrl) {
        android.util.Log.d("InstagramPlayerLayout", "🔄 Metadata changed: id=${metadata?.id}, thumbnailUrl=${metadata?.thumbnailUrl?.take(80)}")
    }

    // Sync slider override with actual player position updates to ensure smooth finish
    LaunchedEffect(playerState.position) {
        sliderPosition?.let { target ->
            // If player reports position within 2s of target, we consider the seek confirmed
            if (kotlin.math.abs(playerState.position - target) < 2000L) {
                sliderPosition = null
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Double tap to like (Instagram behavior)
                        showLikeAnimation = true
                        if (!playerState.isLiked) {
                            playerActions.onToggleLike()
                        }
                    }
                )
            }
    ) {
        // Render thumbnail based on selected mode
        // Use key to force recomposition when thumbnailUrl changes (critical for JioSaavn enrichment)
        key(thumbnailUrl, thumbnailMode) {
            when (thumbnailMode) {
                ThumbnailDisplayMode.BLUR_HD -> {
                    BlurHdThumbnailMode(thumbnailUrl = thumbnailUrl, context = context)
                }
                ThumbnailDisplayMode.FIT_CENTER -> {
                    FitCenterThumbnailMode(thumbnailUrl = thumbnailUrl, context = context)
                }
                ThumbnailDisplayMode.CENTER_CROP -> {
                    CenterCropThumbnailMode(thumbnailUrl = thumbnailUrl, context = context)
                }
            }
        }
        
        // Instagram-style center like animation
        InstagramLikeAnimation(
            visible = showLikeAnimation,
            onAnimationEnd = { showLikeAnimation = false },
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Right side action buttons (Instagram Reels style)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Search button (at top)
            ActionIconButton(
                icon = R.drawable.search,
                label = "Search",
                onClick = playerActions.onOpenSearch
            )
            
            // Queue button
            ActionIconButton(
                icon = R.drawable.queue_music,
                label = "Queue",
                onClick = playerActions.onOpenQueue
            )
            
            // Like button
            ActionIconButton(
                icon = if (playerState.isLiked) R.drawable.favorite else R.drawable.favorite_border,
                label = if (playerState.isLiked) "Liked" else "Like",
                tint = if (playerState.isLiked) Color.Red else Color.White,
                onClick = playerActions.onToggleLike
            )
            
            // Volume/Sound button
            ActionIconButton(
                icon = R.drawable.sound_effect,
                label = "Sound",
                onClick = playerActions.onOpenSoundSettings
            )
            
            // Download button
            ActionIconButton(
                icon = R.drawable.download,
                label = "Save",
                onClick = playerActions.onDownload
            )
            
            // Sleep Timer button
            ActionIconButton(
                icon = R.drawable.bedtime,
                label = "Timer",
                onClick = playerActions.onOpenSleepTimer
            )
        }
        
        // Bottom content area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .then(if (rememberPreference(FullScreenModeKey, defaultValue = false).value) Modifier.padding(bottom = 24.dp) else Modifier)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Album art thumbnail + Song info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Small album art
                key(thumbnailUrl) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailUrl)
                            .crossfade(300)
                            .allowHardware(false)
                            .memoryCacheKey(thumbnailUrl) // Use URL as cache key
                            .diskCacheKey(thumbnailUrl)
                            .build(),
                        contentDescription = "Album art",
                        contentScale = ContentScale.Crop,
                        placeholder = rememberAsyncImagePainter(com.musifly.android.R.drawable.sahitya_h),
                        error = rememberAsyncImagePainter(R.drawable.sahitya_h),
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Title and Artist
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = playerState.mediaMetadata?.title ?: "Unknown",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = playerState.mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Menu button
                IconButton(onClick = playerActions.onOpenMenu) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = "More options",
                        tint = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress slider
            Column(modifier = Modifier.fillMaxWidth()) {
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
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
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
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
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
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Time indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = makeTimeString(currentPosition),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = makeTimeString(totalDuration),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = playerActions.onToggleShuffle) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = "Shuffle",
                        tint = if (playerState.shuffleModeEnabled) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Previous
                IconButton(
                    onClick = playerActions.onPrevious,
                    enabled = playerState.canSkipPrevious
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = "Previous",
                        tint = if (playerState.canSkipPrevious) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // Play/Pause
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { playerActions.onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (playerState.isPlaying) R.drawable.pause else R.drawable.play
                        ),
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Next
                IconButton(
                    onClick = playerActions.onNext,
                    enabled = playerState.canSkipNext
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        tint = if (playerState.canSkipNext) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // Repeat
                IconButton(onClick = playerActions.onToggleRepeat) {
                    Icon(
                        painter = painterResource(
                            when (playerState.repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                else -> R.drawable.repeat
                            }
                        ),
                        contentDescription = "Repeat",
                        tint = if (playerState.repeatMode != Player.REPEAT_MODE_OFF) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Mode 1: Blur Background + HD Thumbnail (Instagram/Reels/Resso Style)
 * - Fullscreen blurred background using centerCrop
 * - Top gradient overlay (black and gray) for attractive look
 * - Bottom gradient overlay for controls
 * - Main thumbnail displayed at center using fitCenter (no stretch, no crop, HD quality)
 */
@Composable
private fun BlurHdThumbnailMode(
    thumbnailUrl: String?,
    context: android.content.Context
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Fullscreen blurred background thumbnail (centerCrop)
        // Use key to force reload when thumbnailUrl changes (important for JioSaavn enrichment)
        if (thumbnailUrl != null && thumbnailUrl.isNotEmpty()) {
            key(thumbnailUrl) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .crossfade(500)
                        .allowHardware(false)
                        .memoryCacheKey(thumbnailUrl) // Use URL as cache key
                        .diskCacheKey(thumbnailUrl)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = rememberAsyncImagePainter(R.mipmap.ic_musifly_launcher),
                    error = rememberAsyncImagePainter(R.mipmap.ic_musifly_launcher),
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(25.dp)
                )
            }
        } else {
            // Show placeholder when thumbnailUrl is not available yet (before enrichment)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
        
        // Layer 2: Top gradient overlay (20% of screen - increased opacity)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.2f) // Top 20% of screen
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black, // Top: deep black (100% opacity)
                            Color.Black.copy(alpha = 0.98f), // High opacity
                            Color.Gray.copy(alpha = 0.85f), // Gray with high opacity
                            Color.Black.copy(alpha = 0.7f), // High opacity
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent // Bottom: fully transparent - blends with thumbnail
                        )
                    )
                )
        )
        
        // Layer 3: Bottom gradient overlay (1 inch from bottom - very dark gradient)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(96.dp) // 1 inch from bottom (96dp = 1 inch)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent, // Top: fully transparent - blends with thumbnail
                            Color.Black.copy(alpha = 0.9f), // Start fade - very very dark
                            Color.Black.copy(alpha = 0.98f), // Mid fade - almost black
                            Color.Black // Bottom: 100% opacity (deep black)
                        )
                    )
                )
        )
        
        // Layer 4: Main HD thumbnail (16:9 ratio, fitCenter - NO stretch, NO crop, NOT full screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 0.dp),
            contentAlignment = Alignment.Center
        ) {
            // Use key to force reload when thumbnailUrl changes (important for JioSaavn enrichment)
            if (thumbnailUrl != null && thumbnailUrl.isNotEmpty()) {
                key(thumbnailUrl) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailUrl)
                            .crossfade(500)
                            .allowHardware(false)
                            .memoryCacheKey(thumbnailUrl) // Use URL as cache key
                            .diskCacheKey(thumbnailUrl)
                            .build(),
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Fit, // fitCenter - maintains aspect ratio, no stretch, no crop
                        placeholder = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                        error = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                        modifier = Modifier
                            .fillMaxWidth(0.7f) // 70% width - NOT full screen
                            .fillMaxHeight(0.6f) // 60% height - NOT full screen
                            .aspectRatio(16f / 9f) // 16:9 ratio maintain karega
                    )
                }
            } else {
                // Show placeholder when thumbnailUrl is not available yet
                Icon(
                    painter = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                    contentDescription = "Album Art",
                    tint = Color.Unspecified, // Don't tint the app icon
                    modifier = Modifier.size(200.dp)
                )
            }
        }
    }
}

/**
 * Mode 2: FitCenter Mode (No Crop, No Blur)
 * - Thumbnail displays in original aspect ratio (no forced 16:9)
 * - No stretch and no crop allowed
 * - Blur background ONLY if empty spaces appear
 * - Maintain HD quality 100%
 * - Center the thumbnail vertically
 * - Top and bottom gradients for attractive look
 */
@Composable
private fun FitCenterThumbnailMode(
    thumbnailUrl: String?,
    context: android.content.Context
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Blurred background only for empty spaces (behind the thumbnail)
        // This will show in letterbox areas if thumbnail doesn't fill screen
        // Use key to force reload when thumbnailUrl changes (important for JioSaavn enrichment)
        if (thumbnailUrl != null && thumbnailUrl.isNotEmpty()) {
            key(thumbnailUrl) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .crossfade(500)
                        .allowHardware(false)
                        .memoryCacheKey(thumbnailUrl) // Use URL as cache key
                        .diskCacheKey(thumbnailUrl)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                    error = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(25.dp)
                )
            }
        } else {
            // Show black background when thumbnailUrl is not available yet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
        
        // Top gradient (20% of screen - increased opacity)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.2f) // Top 20% of screen
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black, // Top: deep black (100% opacity)
                            Color.Black.copy(alpha = 0.95f), // High opacity
                            Color.Gray.copy(alpha = 0.8f), // Gray with high opacity
                            Color.Black.copy(alpha = 0.6f),
                            Color.Transparent // Bottom: fully transparent - blends with thumbnail
                        )
                    )
                )
        )
        
        // Bottom gradient (1 inch from bottom - very dark gradient)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(96.dp) // 1 inch from bottom (96dp = 1 inch)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent, // Top: fully transparent - blends with thumbnail
                            Color.Black.copy(alpha = 0.9f), // Start fade - very very dark
                            Color.Black.copy(alpha = 0.98f), // Mid fade - almost black
                            Color.Black // Bottom: 100% opacity (deep black)
                        )
                    )
                )
        )
        
        // Main thumbnail on top (16:9 ratio, fitCenter - no stretch, no crop, NOT full screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 0.dp),
            contentAlignment = Alignment.Center
        ) {
            // Use key to force reload when thumbnailUrl changes (important for JioSaavn enrichment)
            if (thumbnailUrl != null && thumbnailUrl.isNotEmpty()) {
                key(thumbnailUrl) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbnailUrl)
                            .crossfade(500)
                            .allowHardware(false)
                            .memoryCacheKey(thumbnailUrl) // Use URL as cache key
                            .diskCacheKey(thumbnailUrl)
                            .build(),
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Fit, // fitCenter - no stretch, no crop
                        placeholder = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                        error = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                        modifier = Modifier
                            .fillMaxWidth(0.7f) // 70% width - NOT full screen
                            .fillMaxHeight(0.6f) // 60% height - NOT full screen
                            .aspectRatio(16f / 9f) // 16:9 ratio maintain karega
                    )
                }
            } else {
                // Show placeholder when thumbnailUrl is not available yet
                Icon(
                    painter = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                    contentDescription = "Album Art",
                    tint = Color.Unspecified, // Don't tint the app icon
                    modifier = Modifier.size(200.dp)
                )
            }
        }
    }
}

/**
 * Mode 3: CenterCrop Mode (Immersive Fullscreen)
 * - Thumbnail fills entire 9:16 screen
 * - Slight cropping on left and right is acceptable
 * - Add slight bottom gradient to improve text readability
 * - Must look like Resso immersive mode
 */
@Composable
private fun CenterCropThumbnailMode(
    thumbnailUrl: String?,
    context: android.content.Context
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen thumbnail (centerCrop)
        // Use key to force reload when thumbnailUrl changes (important for JioSaavn enrichment)
        if (thumbnailUrl != null && thumbnailUrl.isNotEmpty()) {
            key(thumbnailUrl) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(thumbnailUrl)
                        .crossfade(500)
                        .allowHardware(false)
                        .memoryCacheKey(thumbnailUrl) // Use URL as cache key
                        .diskCacheKey(thumbnailUrl)
                        .build(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    placeholder = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                    error = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Show placeholder when thumbnailUrl is not available yet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
        
        // Slight bottom gradient for text readability
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.25f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent, // Top: fully transparent
                            Color.Black.copy(alpha = 0.5f), // 50% opacity
                            Color.Black.copy(alpha = 0.8f) // 80% opacity at bottom
                        )
                    )
                )
        )
    }
}

/**
 * Instagram-style action button with icon and label
 */
@Composable
private fun ActionIconButton(
    icon: Int,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
