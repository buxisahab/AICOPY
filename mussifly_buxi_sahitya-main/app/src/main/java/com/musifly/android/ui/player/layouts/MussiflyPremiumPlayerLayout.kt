package com.musifly.android.ui.player.layouts

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.exoplayer.offline.Download
import androidx.palette.graphics.Palette
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.collectAsState
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.ui.component.DownloadButton
import com.musifly.android.db.entities.LyricsEntity
import com.musifly.android.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.musifly.android.di.LyricsHelperEntryPoint
import com.musifly.android.lyrics.LyricsEntry
import com.musifly.android.lyrics.LyricsUtils
import com.musifly.android.utils.HdThumbnailUtil
import com.musifly.android.utils.makeTimeString
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import com.musifly.android.secure.SecureString
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.utils.rememberPreference
import com.musifly.android.constants.FullScreenModeKey
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.musifly.android.ui.component.liquidglass.catalog.utils.rememberUISensor
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.HighlightStyle
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.drawscope.scale
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberCombinedBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.rememberBackdrop
import androidx.compose.ui.graphics.lerp
import com.musifly.android.ui.player.components.CircularGlassIconButton
import com.musifly.android.ui.player.components.CircularGlassDownloadButton
import com.musifly.android.ui.player.components.GlassActionBar

/**
 * Format play count for display
 * Examples: 1234567 -> "1.2M", 50000 -> "50K", 999 -> "999"
 */
private fun formatPlayCount(count: String): String {
    val number = count.toLongOrNull() ?: return "0"
    
    return when {
        number >= 1_000_000 -> {
            val millions = number / 1_000_000.0
            String.format("%.1fM", millions)
        }
        number >= 1_000 -> {
            val thousands = number / 1_000.0
            String.format("%.1fK", thousands)
        }
        else -> number.toString()
    }
}

// Import the shared CircularGlassIconButton from components package
// (Local implementation removed - now using shared component)



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MussiflyPremiumPlayerLayout(
    playerState: PlayerState,
    playerActions: PlayerActions,
    onOpenFullscreenLyrics: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (isFullScreen) = rememberPreference(FullScreenModeKey, defaultValue = false)
    val title = playerState.mediaMetadata?.title ?: "Unknown Song"
    val artist = playerState.mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist"
    val coverUrl = HdThumbnailUtil.getBestThumbnailUrl(playerState.mediaMetadata)
    val database = LocalDatabase.current

    // Palette Color Extraction
    var sliderColor by remember { mutableStateOf(Color.White) }
    
    LaunchedEffect(coverUrl) {
        if (!coverUrl.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(coverUrl)
                        .allowHardware(false)
                        .build()
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = result.drawable.toBitmap()
                        val palette = Palette.from(bitmap).generate()
                        // Try Vibrant -> Light Vibrant -> Dominant -> White
                        val extracted = palette.vibrantSwatch?.rgb
                            ?: palette.lightVibrantSwatch?.rgb
                            ?: palette.dominantSwatch?.rgb
                        
                        if (extracted != null) {
                            sliderColor = Color(extracted)
                        } else {
                             sliderColor = Color.White
                        }
                    }
                } catch (e: Exception) {
                    sliderColor = Color.White
                }
            }
        } else {
            sliderColor = Color.White
        }
    }
    
    // Auto-Fetch Lyrics Logic (AI/API Integration)
    val currentSongId = playerState.mediaMetadata?.id
    val mediaMetadata = playerState.mediaMetadata
    
    LaunchedEffect(currentSongId) {
        if (currentSongId != null && mediaMetadata != null) {
            withContext(Dispatchers.IO) {
                val existingLyrics = try {
                     database.getLyrics(currentSongId)
                } catch (e: Exception) { null }
                
                if (existingLyrics == null || existingLyrics.lyrics == LYRICS_NOT_FOUND) {
                    try {
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
                            // Mark as not found to avoid repeated fetching loops if needed, 
                            // or just leave it to retry later. For now, we try once per session.
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
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

    // Swipe Gesture State
    val density = LocalDensity.current
    val view = LocalView.current
    val minSwipeDist = with(density) { 50.dp.toPx() }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragOffset = 0f },
                    onDragEnd = {
                        if (dragOffset < -minSwipeDist) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            playerActions.onNext() // Swipe Up -> Next
                        } else if (dragOffset > minSwipeDist) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            playerActions.onPrevious() // Swipe Down -> Previous
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                    }
                )
            }
    ) {
        // 1. Background Layer (Blurred Fullscreen to fill voids)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(coverUrl)
                .crossfade(true)
                .allowHardware(false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholder = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
            error = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 80.dp) // Strong blur for smooth ambient color
                .alpha(0.6f)
        )

        // 2. Main Thumbnail Layer
        Box(modifier = Modifier.fillMaxWidth()
            .aspectRatio(1f)
            .align(Alignment.Center)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                .data(coverUrl)
                    .crossfade(true)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                error = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { playerActions.onPlayPause() } // Tap to Play/Pause
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Black,
                                0.15f to Color.Transparent,
                                0.85f to Color.Transparent,
                                1.0f to Color.Black
                            ),
                            blendMode = BlendMode.DstOut
                        )
                    }
            )
            
            // Central Play/Pause Button
            if (!playerState.isPlaying) {
                 CircularGlassIconButton(
                    painter = painterResource(if (playerState.isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = "Play/Pause",
                    onClick = playerActions.onPlayPause,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp),
                    iconSize = 32.dp
                )
            }
        }
        
        // 3. Edge-Disguising Gradient (Dark only at the top/start)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.8f),      // Dark at the very top
                            0.15f to Color.Transparent,                  // Quickly fade to transparent
                            1.0f to Color.Transparent                    // Remain transparent for the rest
                        )
                    )
                )
        )

        // 3. Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .then(if (isFullScreen) Modifier.padding(top = 20.dp) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button
                CircularGlassIconButton(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "Back",
                    onClick = onCollapse,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = "Your Daily Mix",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                
                // Right side: Search + Menu
                if (isLiquidGlassActive()) {
                    GlassActionBar {
                        IconButton(
                            onClick = playerActions.onOpenSearch,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = "Search",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = playerActions.onOpenMenu,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = "Menu",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search button
                        CircularGlassIconButton(
                            painter = painterResource(R.drawable.search),
                            contentDescription = "Search",
                            onClick = playerActions.onOpenSearch,
                            buttonSize = 48.dp
                        )
                        
                        // Menu button
                        CircularGlassIconButton(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = "Menu",
                            onClick = playerActions.onOpenMenu,
                            buttonSize = 48.dp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // -- LYRICS MOVED HERE (Top Fade Area) --
            val playerConn = LocalPlayerConnection.current
            if (playerConn != null) {
                 val lyricsEntity by playerConn.currentLyrics.collectAsState(initial = null)
                 val lyricsText = lyricsEntity?.lyrics
                 
                 val lines = remember(lyricsText) {
                     if (lyricsText.isNullOrEmpty()) {
                         emptyList()
                     } else {
                         LyricsUtils.parseLyrics(lyricsText)
                     }
                 }
                 
                 var currentLineIndex by remember { mutableStateOf(-1) }
                 
                 LaunchedEffect(lines) {
                     if (lines.isNotEmpty()) {
                         while (isActive) {
                             val pos = playerConn.player.currentPosition
                             // Always update for responsive sync and proper seeking
                             currentLineIndex = LyricsUtils.findCurrentLineIndex(lines, pos)
                             delay(100) // Reduced delay for smoother sync
                         }
                     } else {
                         currentLineIndex = -1
                     }
                 }

                 // Top Info Section (Context/Lyrics on Fade)
                 Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                        .height(60.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                 ) {
                     if (lines.isNotEmpty() && currentLineIndex in lines.indices) {
                         AnimatedContent(
                             targetState = lines[currentLineIndex].text,
                             transitionSpec = {
                                 (slideInVertically { height -> height } + fadeIn()).togetherWith(
                                     slideOutVertically { height -> -height } + fadeOut())
                             },
                             label = "LyricsAnimation"
                         ) { targetText ->
                             Text(
                                text = targetText, 
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                         }
                     } else {
                         // Fallback (show Song Title or empty)
                         Text(
                            text = title, 
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                         )
                     }
                     
                     // Subtitle (Next line)
                     if (lines.isNotEmpty() && currentLineIndex + 1 in lines.indices) {
                         Text(
                            text = lines[currentLineIndex + 1].text,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                         )
                     } else {
                         Text(
                            text = artist,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(top = 4.dp)
                         )
                     }
                 }
            } else {
                Spacer(modifier = Modifier.height(60.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // -- LYRICS MOVED HERE (Bottom Fade Area) --

            
            // -- NEW Bottom Content Area: Song Info + Actions (Stacked) --
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // 1. Title
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 24.sp, // Slightly larger for emphasis
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    letterSpacing = 0.5.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                
                // 2. Artist + Follow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = artist,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable {
                                // Open Instagram profile
                                SecureString.openLink(context, SecureString.LINK_INSTA_WEB)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Follow",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 3. Actions Row (Glass Grouped Style for Liquid Theme)
                if (isLiquidGlassActive()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Like and Download Pair
                        GlassActionBar {
                            IconButton(
                                onClick = playerActions.onToggleLike,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.favorite),
                                    contentDescription = "Like",
                                    tint = if (playerState.isLiked) Color(0xFFFF2D55) else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            DownloadButton(
                                download = playerState.download,
                                onClick = playerActions.onDownload,
                                onLongClick = playerActions.onOpenDownloads,
                                modifier = Modifier.size(48.dp),
                                iconSize = 24.dp,
                                tint = Color.White
                            )
                        }

                        // Sound, Timer and Queue Group
                        GlassActionBar {
                            IconButton(
                                onClick = playerActions.onOpenSoundSettings,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.sound_effect),
                                    contentDescription = "Audio Effects",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = playerActions.onOpenSleepTimer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.bedtime),
                                    contentDescription = "Sleep Timer",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            IconButton(
                                onClick = playerActions.onOpenQueue,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.queue_music),
                                    contentDescription = "Queue",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Standard Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Like Button (Left)
                        Icon(
                            painter = painterResource(if (playerState.isLiked) R.drawable.favorite else R.drawable.favorite),
                            contentDescription = "Like",
                            tint = if (playerState.isLiked) Color(0xFFFF2D55) else Color.White,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { playerActions.onToggleLike() }
                        )

                        // Right Group (Download, Queue)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            DownloadButton(
                                download = playerState.download,
                                onClick = playerActions.onDownload,
                                onLongClick = playerActions.onOpenDownloads,
                                modifier = Modifier.size(26.dp),
                                iconSize = 26.dp,
                                tint = Color.White
                            )

                            Icon(
                                painter = painterResource(R.drawable.sound_effect),
                                contentDescription = "Audio Effects",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable { playerActions.onOpenSoundSettings() }
                            )

                            Icon(
                                painter = painterResource(R.drawable.bedtime),
                                contentDescription = "Sleep Timer",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable { playerActions.onOpenSleepTimer() }
                            )

                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = "Queue",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable { playerActions.onOpenQueue() }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bottom Playback Controls (Play + Slider)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play/Pause Button (Left Side)
                if (isLiquidGlassActive()) {
                    CircularGlassIconButton(
                        painter = painterResource(if (playerState.isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = "Play/Pause",
                        onClick = { playerActions.onPlayPause() },
                        modifier = Modifier.size(48.dp),
                        iconSize = 20.dp
                    )
                } else {
                    IconButton(
                        onClick = { playerActions.onPlayPause() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (playerState.isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Current Position Time
                Text(
                    text = makeTimeString(currentPosition),
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
                
                Spacer(modifier = Modifier.width(8.dp))

                // Adaptive, Slim Slider (Right Side, Fills width)
                Slider(
                    value = currentPosition.toFloat(),
                    valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                    onValueChange = { sliderPosition = it.toLong() },
                    onValueChangeFinished = {
                        sliderPosition?.let { pos ->
                            playerActions.onSeek(pos)
                        }
                    },
                    modifier = Modifier
                        .weight(1f) // Takes remaining space
                        .height(24.dp) // Touch target
                        .scale(scaleX = 1f, scaleY = 0.5f), // Slim down track to ~2dp
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f), 
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    thumb = {
                       // Counter-scaled Box to appear as a circle
                       Box(
                           Modifier
                               .width(8.dp) // 8dp visual width
                               .height(16.dp) // 16dp * 0.5 scale = 8dp visual height
                               .background(Color.White, CircleShape)
                       )
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Remaining Time (with minus sign)
                val remainingTime = (totalDuration - currentPosition).coerceAtLeast(0)
                Text(
                    text = "-${makeTimeString(remainingTime)}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}


