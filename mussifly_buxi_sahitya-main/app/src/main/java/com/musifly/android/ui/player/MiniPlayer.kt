package com.musifly.android.ui.player

// Agregar las importaciones correctas
import android.content.res.Configuration
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.blur
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.Backdrop
import androidx.compose.foundation.isSystemInDarkTheme
import com.musifly.android.utils.rememberPreference
import com.musifly.android.constants.PureBlackKey
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
// import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi // Removed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import com.musifly.android.ui.theme.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.musifly.android.ui.theme.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.ui.component.HdThumbnail
import com.musifly.android.R
import com.musifly.android.constants.DarkModeKey
import com.musifly.android.constants.PureBlackKey
import com.musifly.android.constants.ThumbnailCornerRadius
import com.musifly.android.constants.LiquidGlassMiniPlayerHeightKey
import com.musifly.android.constants.LiquidGlassMiniPlayerCornerRadiusKey
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.models.MediaMetadata
import com.musifly.android.utils.HdThumbnailUtil
import com.musifly.android.utils.LowResThumbnailUtil
import com.musifly.android.ui.screens.settings.DarkMode
import com.musifly.android.ui.menu.AddToPlaylistDialog
import com.musifly.android.LocalDatabase
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.exp

import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    backdrop: Backdrop? = null,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    
    // Collect states - DECOUPLED from position updates for real-time safety
    // Only collect essential states that change on track/artwork changes, NOT position updates
    val isLiquidGlass = isLiquidGlassActive()
    val (miniPlayerHeightPref, _) = rememberPreference(LiquidGlassMiniPlayerHeightKey, defaultValue = 50f)
    val (miniPlayerCornersPref, _) = rememberPreference(LiquidGlassMiniPlayerCornerRadiusKey, defaultValue = 15f)

    val isPlaying by playerConnection.isPlaying.collectAsState()
    // Collect playbackState but only use for buffering/ended checks (not position updates)
    val playbackState by playerConnection.playbackState.collectAsState()
    val error by playerConnection.error.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val isLiked by playerConnection.isLiked.collectAsState()
    
    // Use derivedStateOf for computed values to prevent unnecessary recompositions
    val hasError = remember(error) { error != null }
    
    // REAL-TIME SAFE: Only recompose on buffering/ended state changes, not position updates
    val isBuffering = remember(playbackState) { playbackState == Player.STATE_BUFFERING }
    val isEnded = remember(playbackState) { playbackState == Player.STATE_ENDED }


    // Obtener el estado del tema para calcular el color de fondo correcto
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)

    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    // Calcular el color de fondo correcto
    // Calcular el color de fondo correcto
    val miniPlayerBackgroundColor = when {
        useDarkTheme && pureBlack -> Color.Black
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    val currentView = LocalView.current
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    val configuration = LocalConfiguration.current
    val isTabletLandscape = configuration.screenWidthDp >= 600 &&
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.0f else 0.4f,
        label = "overlay_alpha",
        animationSpec = animationSpec
    )

    // Use RoundedCornerShape for a square with rounded corners as requested
    val currentThumbnailShape = remember(miniPlayerCornersPref) {
        RoundedCornerShape((miniPlayerCornersPref * 0.4f).dp)
    }

    /**
     * Calculates the auto-swipe threshold based on swipe sensitivity.
     * The formula uses a sigmoid function to determine the threshold dynamically.
     * Constants:
     * - -11.44748: Controls the steepness of the sigmoid curve.
     * - 9.04945: Adjusts the midpoint of the curve.
     * - 600: Base threshold value in pixels.
     *
     * @param swipeSensitivity The sensitivity value (typically between 0 and 1).
     * @return The calculated auto-swipe threshold in pixels.
     */
    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    val autoSwipeThreshold = calculateAutoSwipeThreshold(0.73f) // Default sensitivity

    // Thumbnail URL - REAL-TIME SAFE: Only update on track ID or artwork URL change
    // NEVER update on position/playback state changes
    // NEVER decode maxresdefault.jpg during playback - use cached low-res only
    val thumbnailUrl = remember(mediaMetadata?.id, mediaMetadata?.thumbnailUrl) {
        val url = mediaMetadata?.let { metadata ->
            // ALWAYS use low-res during playback for real-time safety
            // Check if currently playing (but don't recompute when isPlaying changes)
            val currentlyPlaying = playerConnection.player.isPlaying
            
            if (currentlyPlaying) {
                // During playback: ALWAYS use low-res (mqdefault) - fast, cached, real-time safe
                // Never decode maxresdefault.jpg - it blocks threads
                LowResThumbnailUtil.getLowResThumbnailUrl(metadata)
                    ?: metadata.thumbnailUrl
            } else {
                // When paused: can use medium quality (hqdefault), but still avoid maxres
                LowResThumbnailUtil.getMediumResThumbnailUrl(metadata)
                    ?: metadata.thumbnailUrl
            }
        }
        android.util.Log.d("MiniPlayer", "📸 Thumbnail URL computed ONCE per track: id=${mediaMetadata?.id}, finalUrl=${url?.take(80)}")
        url
    }
    
    // Stable thumbnail URL - only changes when track ID or artwork URL changes
    // This prevents re-rendering on position/playback state updates
    val finalThumbnailUrl = thumbnailUrl
    
    // State for add to playlist dialog
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isLiquidGlass) {
                    Modifier
                        .height((miniPlayerHeightPref + 8).dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                } else {
                    Modifier
                        .height(64.dp)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                }
            )
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .background(Color.Transparent)
    ) {
        val isLightTheme = !isSystemInDarkTheme()
        val pureBlack by rememberPreference(PureBlackKey, false)
        val containerColor =
            if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
            else if (pureBlack) Color.Black.copy(0.4f)
            else Color(0xFF121212).copy(0.4f)

        val activeBackdrop = backdrop ?: rememberLayerBackdrop()

        // Main MiniPlayer content
        Box(
            modifier = Modifier
                .then(
                    if (isTabletLandscape) {
                        Modifier
                            .width(480.dp)
                            .align(Alignment.CenterEnd)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                )
                .height(if (isLiquidGlass) miniPlayerHeightPref.dp else 64.dp)
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .then(
                    if (isLiquidGlass) {
                        val density = LocalDensity.current
                        Modifier
                            .drawBackdrop(
                                backdrop = activeBackdrop,
                                shape = { RoundedCornerShape(miniPlayerCornersPref.dp) },
                                effects = {
                                    vibrancy()
                                    blur(with(density) { 10.dp.toPx() })
                                    // Increased refraction for more dispersion effect
                                    lens(
                                        refractionHeight = with(density) { 35.dp.toPx() }, 
                                        refractionAmount = with(density) { 50.dp.toPx() },
                                        chromaticAberration = true,
                                        depthEffect = true
                                    )
                                },
                                onDrawSurface = { 
                                    // Volume Gradient for tube depth
                                    drawRect(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.12f),
                                                containerColor,
                                                Color.Black.copy(alpha = 0.08f)
                                            )
                                        )
                                    )
                                    
                                    // Top Specular Highlight removed to eliminate white horizontal shade

                                    // Bottom Reflected Light
                                    drawRect(
                                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                            0.85f to Color.Transparent,
                                            1.0f to Color.White.copy(alpha = 0.15f),
                                            startY = 0f,
                                            endY = size.height
                                        )
                                    )
                                }
                            )
                            .clip(RoundedCornerShape(miniPlayerCornersPref.dp))
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(miniPlayerBackgroundColor)
                    }
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragStartTime = System.currentTimeMillis()
                            totalDragDistance = 0f
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetXAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = animationSpec
                                )
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val adjustedDragAmount =
                                if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                            val allowLeft = adjustedDragAmount < 0 && canSkipNext
                            val allowRight = adjustedDragAmount > 0 && canSkipPrevious
                            if (allowLeft || allowRight) {
                                totalDragDistance += adjustedDragAmount.absoluteValue
                                coroutineScope.launch {
                                    offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                }
                            }
                        },
                        onDragEnd = {
                            val dragDuration = System.currentTimeMillis() - dragStartTime
                            val velocity =
                                if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                            val currentOffset = offsetXAnimatable.value
                            val minDistanceThreshold = 50f
                            val velocityThreshold = (0.73f * -8.25f) + 8.5f
                            val shouldChangeSong = (
                                    currentOffset.absoluteValue > minDistanceThreshold &&
                                            velocity > velocityThreshold
                                    ) || (currentOffset.absoluteValue > autoSwipeThreshold)

                            if (shouldChangeSong) {
                                val isRightSwipe = currentOffset > 0
                                if (isRightSwipe && canSkipPrevious) {
                                    playerConnection.player.seekToPreviousMediaItem()
                                } else if (!isRightSwipe && canSkipNext) {
                                    playerConnection.player.seekToNext()
                                }
                            }

                            coroutineScope.launch {
                                offsetXAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = animationSpec
                                )
                            }
                        }
                    )
                }
        ) {
            // Content layer
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    key(mediaMetadata?.id) {
                        Box {
                            val imageToShow = finalThumbnailUrl 
                                ?: mediaMetadata?.thumbnailUrl
                                ?: ""
                            
                            val safeThumbnail = if (isPlaying && LowResThumbnailUtil.isHighResUrl(imageToShow)) {
                                mediaMetadata?.let { LowResThumbnailUtil.getLowResThumbnailUrl(it) } ?: imageToShow
                            } else {
                                imageToShow
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(if (isLiquidGlass) (miniPlayerHeightPref - 10).dp else 48.dp)
                                    .clip(currentThumbnailShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (safeThumbnail.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalView.current.context)
                                            .data(safeThumbnail)
                                            .crossfade(300)
                                            .memoryCacheKey(safeThumbnail)
                                            .diskCacheKey(safeThumbnail)
                                            .allowHardware(false)
                                            .scale(coil.size.Scale.FILL)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        placeholder = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                                        error = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher)
                                    )
                                } else {
                                    Icon(
                                        painter = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isBuffering) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else if (isEnded) {
                                        IconButton(
                                            onClick = {
                                                playerConnection.player.seekTo(0, 0)
                                                playerConnection.player.playWhenReady = true
                                            },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.replay),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    } else {
                                        IconButton(
                                            onClick = { playerConnection.player.togglePlayPause() },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    if (isPlaying) R.drawable.pause else R.drawable.play
                                                ),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        mediaMetadata?.let { metadata ->
                            AnimatedContent(
                                targetState = metadata.title,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "",
                            ) { title ->
                                Text(
                                    text = title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee(),
                                )
                            }

                            if (metadata.artists.any { it.name.isNotBlank() }) {
                                AnimatedContent(
                                    targetState = metadata.artists.joinToString { it.name },
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "",
                                ) { artists ->
                                    Text(
                                        text = artists,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.basicMarquee(),
                                    )
                                }
                            }
                            

                        }
                    }
                    
                    IconButton(
                        onClick = { playerConnection.toggleLike() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                            ),
                            contentDescription = "Like song",
                            tint = if (isLiked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            if (canSkipPrevious) {
                                playerConnection.player.seekToPreviousMediaItem()
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        enabled = canSkipPrevious
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = "Previous song",
                            tint = if (canSkipPrevious) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = { 
                            if (canSkipNext) {
                                playerConnection.player.seekToNextMediaItem()
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        enabled = canSkipNext
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next song",
                            tint = if (canSkipNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                if (duration > 0 && !isEnded) {
                    LinearProgressIndicator(
                        progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isLiquidGlass) 3.dp else 2.dp)
                            .then(if (isLiquidGlass) Modifier.padding(horizontal = 16.dp) else Modifier),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    )
                }
            }
        }
        
        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 32.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
    
    // Add to Playlist Dialog
    val database = LocalDatabase.current
    val context = LocalContext.current
    
    AddToPlaylistDialog(
        isVisible = showAddToPlaylistDialog,
        onGetSong = { playlist ->
            mediaMetadata?.let { metadata ->
                database.transaction {
                    insert(metadata)
                }
            }
            coroutineScope.launch(Dispatchers.IO) {
                // Handle YouTube playlist sync if needed
                playlist.playlist.browseId?.let { browseId ->
                    mediaMetadata?.id?.let { songId ->
                        // YouTube.addToPlaylist(browseId, songId) // Uncomment if YouTube sync needed
                    }
                }
            }
            listOf(mediaMetadata?.id ?: "")
        },
        onDismiss = {
            showAddToPlaylistDialog = false
        }
    )
}

@Composable
fun MiniMediaInfo(
    mediaMetadata: MediaMetadata,
    error: PlaybackException?,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(6.dp)) {
            // REAL-TIME SAFE: Always use low-res thumbnails (render once per track)
            // Only update when track ID or artwork URL changes, NOT on play/pause or position updates
            val bestThumbnailUrl = remember(mediaMetadata.id, mediaMetadata.thumbnailUrl) {
                // Always use low-res for real-time safety (never decode maxresdefault)
                val url = LowResThumbnailUtil.getLowResThumbnailUrl(mediaMetadata)
                    ?: mediaMetadata.thumbnailUrl
                android.util.Log.d("MiniPlayer", "📸 CompactPlayer thumbnail URL: id=${mediaMetadata.id}, url=${url?.take(80)}...")
                url
            }
            
            // REAL-TIME SAFE: Only re-render when track ID changes
            key(mediaMetadata.id) {
                if (bestThumbnailUrl != null) {
                    HdThumbnail(
                        thumbnailUrl = bestThumbnailUrl,
                        videoId = mediaMetadata.id,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                    )
                }
            }
            // Error overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}