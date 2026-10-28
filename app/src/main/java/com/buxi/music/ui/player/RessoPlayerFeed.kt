package com.buxi.music.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.buxi.music.LocalDatabase
import com.buxi.music.LocalPlayerConnection
import com.buxi.music.R
import com.buxi.music.extensions.metadata
import com.buxi.music.extensions.togglePlayPause
import com.buxi.music.extensions.toggleRepeatMode
import com.buxi.music.lyrics.LyricsUtils
import com.buxi.music.models.MediaMetadata
import com.buxi.music.playback.PlayerConnection
import com.buxi.music.ui.component.RessoCommentsSheet
import com.buxi.music.ui.component.RessoLyricQuoteDialog
import com.buxi.music.ui.theme.ThemeAccent
import com.buxi.music.ui.theme.ThemeAccentBright
import com.buxi.music.ui.theme.ThemeCard
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.theme.ThemeTextPrimary
import com.buxi.music.ui.theme.ThemeTextSecondary
import com.buxi.music.ui.theme.ThemeTextTertiary
import com.buxi.music.utils.makeTimeString
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Resso-Style Full-Screen Immersive Music Player Feed.
 * Recreates the signature Resso UI/UX:
 * 1. Vertical Swipe Music Browsing (Swipe up/down to seamlessly transition between songs)
 * 2. Immersive visual canvas with dynamic color gradients & subtle artwork pulse
 * 3. Centered, interactive Synchronized Lyrics (tap any line to seek)
 * 4. Resso Right-Rail Action Stack:
 *    - Artist avatar with Follow '+' toggle
 *    - Bouncy heart with like counter
 *    - Comments button opening interactive RessoCommentsSheet
 *    - Vibe wave toggle button (animated visualizer overlay)
 *    - Lyric Quote button opening RessoLyricQuoteDialog
 *    - Contextual menu (Equalizer/DSP, Sleep Timer, Download, Queue)
 * 5. Minimalist floating scrubber and playback control bar
 * 6. "Swipe up for next song" animated chevron guide
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RessoPlayerFeed(
    modifier: Modifier = Modifier,
    onOpenAudioEffects: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onOpenSpeed: () -> Unit = {},
    onOpenAudioQuality: () -> Unit = {},
    onOpenMore: () -> Unit = {},
    onCollapse: (() -> Unit)? = null
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    // Real-time playback position tracker
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(isPlaying, mediaMetadata) {
        while (isActive) {
            val player = playerConnection.player
            position = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            delay(250)
        }
    }

    // Modal state for Comments and Lyric Quote dialogs
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showLyricQuoteDialog by remember { mutableStateOf(false) }
    var showVibeVisualizer by remember { mutableStateOf(true) }
    var showSwipeGuide by remember { mutableStateOf(true) }

    // Synchronized lyrics for the current song
    val lyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    val parsedLyrics = remember(lyricsEntity?.lyrics) {
        lyricsEntity?.lyrics?.let { LyricsUtils.parseLyrics(it) } ?: emptyList()
    }

    // Favorite / inLibrary state for current song
    val librarySong by playerConnection.currentSong.collectAsState(initial = null)
    val isFavorite = librarySong?.song?.inLibrary != null

    // Heart bounce animation
    val heartScale = remember { Animatable(1f) }

    // Follow artist toggle
    var isFollowingArtist by remember(mediaMetadata?.artists) { mutableStateOf(false) }

    // Dismiss swipe guide after 6 seconds
    LaunchedEffect(Unit) {
        delay(6000)
        showSwipeGuide = false
    }

    val pageCount = maxOf(1, queueWindows.size)
    val pagerState = rememberPagerState(
        initialPage = currentWindowIndex.coerceIn(0, pageCount - 1),
        pageCount = { pageCount }
    )

    // Sync pager with ExoPlayer track changes
    LaunchedEffect(currentWindowIndex) {
        if (currentWindowIndex in 0 until pageCount && pagerState.currentPage != currentWindowIndex) {
            pagerState.animateScrollToPage(currentWindowIndex)
        }
    }

    // Vertical swipe changes song
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentWindowIndex && pagerState.currentPage in queueWindows.indices) {
            playerConnection.player.seekToDefaultPosition(pagerState.currentPage)
            showSwipeGuide = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ThemeDark)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
        ) { page ->
            val pageMetadata = queueWindows.getOrNull(page)?.mediaItem?.metadata ?: mediaMetadata
            val isCurrentPage = page == currentWindowIndex

            RessoSongCanvas(
                mediaMetadata = pageMetadata,
                isPlaying = isPlaying && isCurrentPage,
                position = if (isCurrentPage) position else 0L,
                duration = if (isCurrentPage) duration else 0L,
                lyrics = if (isCurrentPage) parsedLyrics else emptyList(),
                showVibeVisualizer = showVibeVisualizer,
                onSeek = { targetMs ->
                    if (isCurrentPage) {
                        playerConnection.player.seekTo(targetMs)
                    }
                }
            )
        }

        // Top Header Bar: Resso Tab Title, Collapse button, Audio effect badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { onCollapse?.invoke() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = "Collapse Player",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Resso Vibe Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .clickable { onOpenAudioEffects() }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ThemeAccent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "VIBES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Color.White
                )
            }

            IconButton(
                onClick = { onOpenQueue() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = "Queue",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Right-Rail Action Stack (Resso Signature UI)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 14.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Artist Avatar with Follow '+' button
            Box(contentAlignment = Alignment.BottomCenter) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .background(ThemeCard)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(mediaMetadata?.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Artist",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Follow toggle badge
                Box(
                    modifier = Modifier
                        .offset(y = 6.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isFollowingArtist) Color.White else ThemeAccent)
                        .clickable { isFollowingArtist = !isFollowingArtist },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isFollowingArtist) R.drawable.check else R.drawable.add
                        ),
                        contentDescription = "Follow Artist",
                        tint = if (isFollowingArtist) ThemeDark else Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Favorite / Like Button with bouncy animation & like count
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            heartScale.animateTo(1.4f, spring(dampingRatio = 0.4f, stiffness = 500f))
                            heartScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 400f))
                        }
                        mediaMetadata?.let { song ->
                            database.query {
                                inLibrary(song.id, if (isFavorite) null else LocalDateTime.now())
                            }
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .scale(heartScale.value)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    Icon(
                        painter = painterResource(
                            if (isFavorite) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = "Like",
                        tint = if (isFavorite) ThemeAccent else Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = if (isFavorite) "1.4k" else "1.3k",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // Comments Button with count bubble
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { showCommentsSheet = true },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.comment),
                        contentDescription = "Comments",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "348",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // Vibe / Visualizer wave toggle button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { showVibeVisualizer = !showVibeVisualizer },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (showVibeVisualizer) ThemeAccent.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.35f)
                        )
                        .border(
                            width = if (showVibeVisualizer) 1.5.dp else 0.dp,
                            color = if (showVibeVisualizer) ThemeAccent else Color.Transparent,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.waves),
                        contentDescription = "Vibe Visualizer",
                        tint = if (showVibeVisualizer) ThemeAccentBright else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Vibe",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (showVibeVisualizer) ThemeAccent else Color.White.copy(alpha = 0.85f)
                )
            }

            // Lyric Quote Button (famous Resso feature)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { showLyricQuoteDialog = true },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.format_quote),
                        contentDescription = "Lyric Quote",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Quote",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // Audio Effects
            IconButton(
                onClick = { onOpenAudioEffects() },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Icon(
                    painter = painterResource(R.drawable.equalizer),
                    contentDescription = "Audio Effects",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Sleep Timer
            IconButton(
                onClick = { onOpenSleepTimer() },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = "Sleep Timer",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // More Options (⋯)
            IconButton(
                onClick = { onOpenMore() },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = "More Options",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bottom Area: Song Info, Scrubber, Playback Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 76.dp, // room for right action rail
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 10.dp
                )
        ) {
            // Track Title & Artist
            Text(
                text = mediaMetadata?.title ?: "No track playing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown artist",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))

                // VIP Sound format badge (Taps to open Audio Quality)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF2BE8A).copy(alpha = 0.2f))
                        .clickable { onOpenAudioQuality() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "VIP • 320K",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF2BE8A)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Chorus Mode Button (Resso playing_chorus_mode_skip_to_chorus)
                if (duration > 45_000L) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFF4D67).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFFF4D67).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .clickable {
                                val chorusTarget = (duration * 0.40f).toLong()
                                playerConnection.player.seekTo(chorusTarget)
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⚡ Chorus",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF4D67)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sleek Progress Scrubber
            val currentPos = sliderPosition?.toLong() ?: position
            val progress = if (duration > 0) (currentPos.toFloat() / duration).coerceIn(0f, 1f) else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = makeTimeString(currentPos),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )

                Slider(
                    value = progress,
                    onValueChange = { newProgress ->
                        sliderPosition = newProgress * duration
                    },
                    onValueChangeFinished = {
                        sliderPosition?.let { targetMs ->
                            playerConnection.player.seekTo(targetMs.toLong())
                            sliderPosition = null
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = ThemeAccent,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Text(
                    text = makeTimeString(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Shuffle button
                IconButton(
                    onClick = {
                        playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = "Shuffle",
                        tint = if (shuffleModeEnabled) ThemeAccent else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Previous track
                IconButton(
                    onClick = {
                        if (playerConnection.player.hasPreviousMediaItem()) {
                            playerConnection.player.seekToPrevious()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause Circular Glowing Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(ThemeAccent)
                        .shadow(16.dp, CircleShape, spotColor = ThemeAccentBright)
                        .clickable { playerConnection.player.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (playbackState == Player.STATE_BUFFERING) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(26.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Next track
                IconButton(
                    onClick = {
                        if (playerConnection.player.hasNextMediaItem()) {
                            playerConnection.player.seekToNext()
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Repeat Mode Toggle
                IconButton(
                    onClick = { playerConnection.player.toggleRepeatMode() },
                    modifier = Modifier.size(36.dp)
                ) {
                    val repeatIcon = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                        Player.REPEAT_MODE_ALL -> R.drawable.repeat
                        else -> R.drawable.repeat
                    }
                    val isRepeatActive = repeatMode != Player.REPEAT_MODE_OFF
                    Icon(
                        painter = painterResource(repeatIcon),
                        contentDescription = "Repeat",
                        tint = if (isRepeatActive) ThemeAccent else Color.White.copy(alpha = 0.65f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Animated "Swipe up for next song" guide indicator
        AnimatedVisibility(
            visible = showSwipeGuide,
            enter = fadeIn(tween(400)) + scaleIn(tween(400)),
            exit = fadeOut(tween(400)) + scaleOut(tween(400)),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 120.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "swipe_guide")
            val arrowOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "arrow_bounce"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_upward),
                    contentDescription = null,
                    tint = ThemeAccentBright,
                    modifier = Modifier
                        .offset(y = arrowOffset.dp)
                        .size(22.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Swipe up for next song",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Resso Comments Sheet
        RessoCommentsSheet(
            mediaMetadata = mediaMetadata,
            isVisible = showCommentsSheet,
            onDismiss = { showCommentsSheet = false }
        )

        // Resso Lyric Quote Dialog
        RessoLyricQuoteDialog(
            mediaMetadata = mediaMetadata,
            lyrics = parsedLyrics.map { it.text },
            isVisible = showLyricQuoteDialog,
            onDismiss = { showLyricQuoteDialog = false }
        )
    }
}

/**
 * Single Song Canvas inside the Vertical Pager.
 * Displays:
 * - Dynamic blurred artwork backdrop
 * - Rotating vinyl record or subtle pulsing cover
 * - Interactive Synchronized Lyrics centered vertically
 */
@Composable
private fun RessoSongCanvas(
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    lyrics: List<com.buxi.music.lyrics.LyricsEntry>,
    showVibeVisualizer: Boolean,
    onSeek: (Long) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Find current active lyric line
    val currentLyricIndex = remember(position, lyrics) {
        if (lyrics.isEmpty()) -1
        else {
            val idx = lyrics.indexOfLast { it.time <= position }
            if (idx >= 0) idx else 0
        }
    }

    // Auto-scroll lyrics to center the active line
    LaunchedEffect(currentLyricIndex) {
        if (currentLyricIndex >= 0 && currentLyricIndex < lyrics.size) {
            listState.animateScrollToItem(
                index = maxOf(0, currentLyricIndex - 1),
                scrollOffset = 0
            )
        }
    }

    // Cover art pulse / breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "canvas_vibe")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.04f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen dynamic blurred backdrop
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(mediaMetadata?.thumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(40.dp)
                .scale(1.2f)
                .alpha(0.55f)
        )

        // Dark gradient overlay for readability and Resso style
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.75f),
                            ThemeDark.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Background Waveform / Vibe visualizer
        if (showVibeVisualizer && isPlaying) {
            RessoWaveVisualizer(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.28f)
            )
        }

        // Center Content: Cover art & Synchronized Lyrics
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 60.dp,
                    bottom = 200.dp,
                    start = 24.dp,
                    end = 76.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rounded Album Artwork with subtle pulse
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(pulseScale)
                    .clip(RoundedCornerShape(24.dp))
                    .shadow(24.dp, RoundedCornerShape(24.dp), spotColor = ThemeAccent)
                    .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(mediaMetadata?.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Interactive Live Lyrics Area
            if (lyrics.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    itemsIndexed(lyrics) { idx, entry ->
                        val isCurrent = idx == currentLyricIndex
                        val color by animateColorAsState(
                            targetValue = if (isCurrent) Color.White else Color.White.copy(alpha = 0.35f),
                            animationSpec = tween(250),
                            label = "lyric_color"
                        )
                        val fontSize = if (isCurrent) 19.sp else 16.sp
                        val fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal

                        Text(
                            text = entry.text,
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            color = color,
                            textAlign = TextAlign.Start,
                            lineHeight = 26.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSeek(entry.time) }
                                .padding(vertical = 6.dp)
                        )
                    }
                }
            } else {
                // If lyrics not available, show aesthetic Resso quote
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Feel the music in every beat.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Synchronized lyrics will appear when available.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

/**
 * Animated Ambient Rhythm Waves in background (Resso Vibe)
 */
@Composable
private fun RessoWaveVisualizer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "resso_waves")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val midY = height * 0.55f

        val path = Path()
        path.moveTo(0f, midY)

        val points = 60
        for (i in 0..points) {
            val x = (width / points) * i
            val angle = (i.toFloat() / points) * 4 * PI.toFloat() + phase
            val y = midY + sin(angle) * 35f
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                listOf(
                    ThemeAccent.copy(alpha = 0.6f),
                    ThemeAccentBright.copy(alpha = 0.8f),
                    Color(0xFF8B5CF6).copy(alpha = 0.6f)
                )
            ),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
