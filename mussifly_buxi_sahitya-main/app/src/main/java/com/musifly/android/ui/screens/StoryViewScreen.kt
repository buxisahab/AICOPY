package com.musifly.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.navigation.NavController
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musifly.android.R
import com.musifly.android.models.UserStoryGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.musifly.android.viewmodels.StoryViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.musifly.android.sync.supabase.SupabaseStoryView
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId
import androidx.compose.material.icons.filled.MusicNote
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewScreen(
    viewModel: StoryViewModel,
    group: UserStoryGroup,
    navController: NavController,
    onDeleteClick: (String, String) -> Unit,
    onAllStoriesViewed: () -> Unit,
    onNextUser: () -> Unit = {},
    onPrevUser: () -> Unit = {}
) {
    if (group.stories.isEmpty()) return
    var currentStoryIndex by remember { mutableIntStateOf(0) }
    // Ensure index is valid
    if (currentStoryIndex >= group.stories.size) {
        currentStoryIndex = (group.stories.size - 1).coerceAtLeast(0)
    }
    val currentStory = group.stories.getOrNull(currentStoryIndex) ?: return
    
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    
    // Viewers Sheet State
    var showViewersSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    
    // Mark as viewed
    LaunchedEffect(currentStory.id) {
        if (!currentStory.isViewed) {
             viewModel.viewStory(currentStory.id)
        }
    }

    // Auto-advance and progress logic
    LaunchedEffect(currentStoryIndex, isPaused, showViewersSheet) {
        if (showViewersSheet) return@LaunchedEffect // Pause when sheet is open

        if (currentStory.isVideo) {
            // Progress handled by player
            progress = 0f
            return@LaunchedEffect
        }
        
        progress = 0f
        val storyDuration = if (currentStory.duration != null && currentStory.duration > 0) currentStory.duration else 5000L
        val updateInterval = 16L
        val steps = storyDuration / updateInterval
        val increment = 1f / steps
        
        while (progress < 1f && !isPaused && isActive) {
            delay(updateInterval)
            progress += increment
        }
        
        if (progress >= 1f && !isPaused) {
            if (currentStoryIndex < group.stories.size - 1) {
                currentStoryIndex++
                progress = 0f
            } else {
                onNextUser()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        val width = size.width
                        if (offset.x < width * 0.3f) {
                            if (currentStoryIndex > 0) {
                                currentStoryIndex--
                                progress = 0f
                            } else {
                                onPrevUser()
                            }
                        } else if (offset.x > width * 0.7f) {
                            if (currentStoryIndex < group.stories.size - 1) {
                                currentStoryIndex++
                                progress = 0f
                            } else {
                                onNextUser()
                            }
                        }
                    }
                )
            }
            .pointerInput(group.userId) {
                // Swipe Up for viewers (only if own story)
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (group.userId == currentUserId) {
                    detectVerticalDragGestures { change, dragAmount ->
                        if (dragAmount < -20) { // Swipe Up
                            showViewersSheet = true
                        }
                    }
                }
            }
    ) {
        // Media Layer
        if (currentStory.isVideo) {
            VideoPlayer(
                videoUrl = currentStory.mediaUrl,
                trimStart = currentStory.trimStart, // Use trim start
                trimEnd = currentStory.trimEnd,     // Use trim end
                isPaused = isPaused || showViewersSheet,
                onProgressUpdate = { p -> progress = p },
                onVideoEnded = {
                    if (!showViewersSheet) {
                        if (currentStoryIndex < group.stories.size - 1) {
                            currentStoryIndex++
                        } else {
                            onNextUser()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentStory.mediaUrl)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Music Layer
        if (currentStory.musicId != null) {
            BackgroundMusicPlayer(
                musicId = currentStory.musicId!!,
                isPaused = isPaused || showViewersSheet,
                startTime = currentStory.startTime ?: 0L,
                endTime = currentStory.endTime,
                modifier = Modifier.size(0.dp) // Hidden
            )
        }

        // UI Overlay
        if (!showViewersSheet) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Progress Indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 8.dp, end = 8.dp)
                        .systemBarsPadding(), // Add safe area
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    group.stories.forEachIndexed { index, _ ->
                        val segmentProgress = when {
                            index < currentStoryIndex -> 1f
                            index > currentStoryIndex -> 0f
                            else -> progress
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }

                // 2. Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(group.userImageUrl)
                            .crossfade(true)
                            .error(R.drawable.person)
                            .placeholder(R.drawable.person)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                    
                    Column(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = group.username,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (group.userId == currentUserId) {
                        IconButton(onClick = { onDeleteClick(currentStory.id, currentStory.mediaUrl) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Story",
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(onClick = { onAllStoriesViewed() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                
                // 2.5 Music Info (if playing)
                if (currentStory.musicId != null) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Music Playing...", // Ideally we'd show the real name here
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))

                // 3. Caption
                if (!currentStory.caption.isNullOrBlank()) {
                    Text(
                        text = currentStory.caption,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // 4. Interaction / Viewers Hint
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (group.userId == currentUserId) {
                   // Show "Swipe up for viewers" hint
                   Column(
                       horizontalAlignment = Alignment.CenterHorizontally,
                       modifier = Modifier
                           .fillMaxWidth()
                           .padding(bottom = 24.dp)
                           .clickable { showViewersSheet = true }
                   ) {
                       Icon(
                           imageVector = Icons.Default.Visibility,
                           contentDescription = "Viewers",
                           tint = Color.White,
                           modifier = Modifier.size(24.dp)
                       )
                       Text(
                           text = "Swipe up for viewers",
                           color = Color.White,
                           style = MaterialTheme.typography.labelSmall,
                           modifier = Modifier.padding(top = 4.dp)
                       )
                   }
                } else {
                    // Reply / Like Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        var isLiked by remember { mutableStateOf(false) }

                        // Reply Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Send message",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        // Like Button
                        IconButton(onClick = { isLiked = !isLiked }) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) Color.Red else Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        IconButton(onClick = { /* Share logic */ }) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Share",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Viewers Bottom Sheet
    if (showViewersSheet) {
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (group.userId == currentUserId) {
            ModalBottomSheet(
                onDismissRequest = { showViewersSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color.White
            ) {
                // Fetch viewers
                val viewers by produceState<List<SupabaseStoryView>>(initialValue = emptyList(), key1 = currentStory.id) {
                    value = viewModel.getStoryViewers(currentStory.id)
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        text = "Viewers (${viewers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    
                    if (viewers.isEmpty()) {
                         Text(
                            text = "No viewers yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn {
                             items(viewers) { viewer ->
                                 Row(
                                     verticalAlignment = Alignment.CenterVertically,
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .padding(vertical = 8.dp)
                                 ) {
                                     AsyncImage(
                                        model = viewer.viewer?.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color.Gray),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.padding(start = 12.dp)) {
                                        Text(
                                            text = viewer.viewer?.username ?: "Unknown",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        // Format time
                                        Text(
                                            text = try {
                                                // Simple formatting
                                                viewer.viewedAt.substringBefore("T") + " " + viewer.viewedAt.substringAfter("T").take(5)
                                            } catch (e: Exception) { "" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                 }
                             }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    trimStart: Long,
    trimEnd: Long?,
    isPaused: Boolean,
    onProgressUpdate: (Float) -> Unit,
    onVideoEnded: () -> Unit,
    isLooping: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            playWhenReady = true
        }
    }

    LaunchedEffect(videoUrl, trimStart) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        exoPlayer.prepare()
        exoPlayer.seekTo(trimStart)
        exoPlayer.play()
    }

    LaunchedEffect(isPaused) {
        if (isPaused) exoPlayer.pause() else exoPlayer.play()
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onVideoEnded()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Polling for progress
    LaunchedEffect(Unit) {
        while (true) {
            val currentPos = exoPlayer.currentPosition
            val effectiveEnd = trimEnd ?: exoPlayer.duration
            val duration = effectiveEnd - trimStart
            
            // Check if we passed the trim end
            if (trimEnd != null && currentPos >= trimEnd) {
                 exoPlayer.pause()
                 onVideoEnded()
            } else if (duration > 0) {
                 val p = ((currentPos - trimStart).toFloat() / duration).coerceIn(0f, 1f)
                 onProgressUpdate(p)
            }
            delay(16) // Faster update for smooth bar
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Fill screen
            }
        },
        modifier = modifier
    )
}

@Composable
fun BackgroundMusicPlayer(
    musicId: String,
    isPaused: Boolean,
    startTime: Long,
    endTime: Long?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    var resolvedUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(musicId) {
        resolvedUrl = null
        scope.launch(Dispatchers.IO) {
            try {
                val url = if (musicId.startsWith("saavn:")) {
                    val id = musicId.substringAfter("saavn:")
                    com.musifly.android.streamingengines.StreamingEngineManager.getInstance().getSaavnStreamUrl(id)
                } else if (musicId.startsWith("content://") || musicId.startsWith("file://")) {
                    musicId // Local URI
                } else if (!musicId.contains(":") && musicId.length >= 10) { 
                    // Likely YouTube ID (11 chars usually)
                    val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                    val playbackData = com.musifly.android.utils.YTPlayerUtils.playerResponseForPlayback(
                        videoId = musicId,
                        audioQuality = com.musifly.android.constants.AudioQuality.AUTO,
                        connectivityManager = connectivityManager
                    ).getOrNull()
                    playbackData?.streamUrl
                } else {
                    null
                }
                
                if (url != null) {
                    withContext(Dispatchers.Main) {
                        resolvedUrl = url
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to resolve story music: $musicId")
            }
        }
    }

    LaunchedEffect(resolvedUrl) {
        resolvedUrl?.let { url ->
            exoPlayer.setMediaItem(MediaItem.fromUri(url))
            exoPlayer.prepare()
            exoPlayer.seekTo(startTime)
            exoPlayer.play()
        }
    }

    LaunchedEffect(isPaused) {
        if (isPaused) exoPlayer.pause() else if (resolvedUrl != null) exoPlayer.play()
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Progress Loop/Trim logic
    LaunchedEffect(resolvedUrl, endTime) {
        if (resolvedUrl == null || endTime == null) return@LaunchedEffect
        while (true) {
            if (exoPlayer.currentPosition >= endTime) {
                exoPlayer.seekTo(startTime)
            }
            delay(100)
        }
    }
}



