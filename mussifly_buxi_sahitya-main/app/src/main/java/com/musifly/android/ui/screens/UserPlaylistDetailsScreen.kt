package com.musifly.android.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.db.entities.Song
import com.musifly.android.extensions.toMediaItem
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.playback.queues.ListQueue
import com.musifly.android.playback.queues.YouTubeQueue
import com.musifly.android.streamingengines.StreamSourceManager
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.component.SongListItem
import com.musifly.android.ui.menu.SongMenu
import com.musifly.android.viewmodels.UserPlaylistViewModel
import kotlinx.coroutines.flow.first
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.LiquidHeader
import com.musifly.android.ui.component.liquidglass.LiquidGlassButton
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.layerBackdrop
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection

/**
 * Screen displaying details of a user playlist (songs in playlist)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UserPlaylistDetailsScreen(
    playlistId: String,
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val viewModel: UserPlaylistViewModel = hiltViewModel()
    
    val playlist by viewModel.getPlaylist(playlistId).collectAsState(initial = null)
    val songIds by viewModel.getPlaylistSongs(playlistId).collectAsState(initial = emptyList())
    
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(songIds) {
        isLoading = true
        try {
            val allSongs = database.songsByCreateDateAsc().first()
            songs = songIds.mapNotNull { id -> allSongs.find { it.id == id } }
            isLoading = false
        } catch (_: Exception) {
            isLoading = false
        }
    }
    
    val mediaMetadata = playerConnection.mediaMetadata.collectAsState(initial = null).value
    val isPlaying = playerConnection.isPlaying.collectAsState().value
    
    val isLiquidGlass = isLiquidGlassActive()
    val backdrop = rememberLayerBackdrop()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    if (isLiquidGlass) {
                        LiquidHeader(
                            navController = navController,
                            backdrop = backdrop,
                            title = playlist?.name ?: "Playlist",
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    } else {
                        TopAppBar(
                            title = { 
                                Text(
                                    text = playlist?.name ?: "Playlist",
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { navController.navigateUp() }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.arrow_back),
                                        contentDescription = null
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior
                        )
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isLiquidGlass) PaddingValues(0.dp) else paddingValues)
                        .windowInsetsPadding(
                            LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                        )
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        songs.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.playlist_play),
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No songs in playlist",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Add songs to this playlist",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = if (isLiquidGlass) {
                                    PaddingValues(
                                        top = 112.dp + 8.dp,
                                        bottom = 8.dp,
                                        start = 8.dp,
                                        end = 8.dp
                                    )
                                } else {
                                    PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                },
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (isLiquidGlassActive()) {
                                                LiquidGlassButton(
                                                    onClick = {
                                                        if (songs.isNotEmpty()) {
                                                            val mediaItems = songs.map { it.toMediaItem() }
                                                            playerConnection.playQueue(ListQueue(items = mediaItems))
                                                        }
                                                    },
                                                    isPrimary = true,
                                                    modifier = Modifier.weight(1f),
                                                    content = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(painter = painterResource(R.drawable.play), contentDescription = null)
                                                            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                                            Text(stringResource(R.string.play))
                                                        }
                                                    }
                                                )
                                            } else {
                                                Button(
                                                    onClick = {
                                                        if (songs.isNotEmpty()) {
                                                            val mediaItems = songs.map { it.toMediaItem() }
                                                            playerConnection.playQueue(ListQueue(items = mediaItems))
                                                        }
                                                    },
                                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(painter = painterResource(R.drawable.play), contentDescription = null)
                                                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                                    Text(stringResource(R.string.play))
                                                }
                                            }

                                            if (isLiquidGlassActive()) {
                                                LiquidGlassButton(
                                                    onClick = {
                                                        if (songs.isNotEmpty()) {
                                                            val mediaItems = songs.shuffled().map { it.toMediaItem() }
                                                            playerConnection.playQueue(ListQueue(items = mediaItems))
                                                        }
                                                    },
                                                    isPrimary = false,
                                                    modifier = Modifier.weight(1f),
                                                    content = {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(painter = painterResource(R.drawable.shuffle), contentDescription = null)
                                                            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                                            Text(stringResource(R.string.shuffle))
                                                        }
                                                    }
                                                )
                                            } else {
                                                OutlinedButton(
                                                    onClick = {
                                                        if (songs.isNotEmpty()) {
                                                            val mediaItems = songs.shuffled().map { it.toMediaItem() }
                                                            playerConnection.playQueue(ListQueue(items = mediaItems))
                                                        }
                                                    },
                                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(painter = painterResource(R.drawable.shuffle), contentDescription = null)
                                                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                                    Text(stringResource(R.string.shuffle))
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "${songs.size} songs",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                items(
                                    items = songs,
                                    key = { it.id }
                                ) { song ->
                                    SongListItem(
                                        song = song,
                                        isActive = song.id == mediaMetadata?.id,
                                        isPlaying = isPlaying && song.id == mediaMetadata?.id,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    if (song.id == mediaMetadata?.id) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        val metadata = song.toMediaMetadata()
                                                        val queue = when (StreamSourceManager.currentSource) {
                                                            StreamSourceManager.StreamType.YOUTUBE,
                                                            StreamSourceManager.StreamType.YT_MUSIC -> {
                                                                YouTubeQueue.radio(metadata)
                                                            }
                                                            else -> {
                                                                val mediaItem = metadata.toMediaItem()
                                                                ListQueue(items = listOf(mediaItem))
                                                            }
                                                        }
                                                        playerConnection.playQueue(queue)
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            ),
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    viewModel.removeSongFromPlaylist(playlistId, song.id) { }
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.close),
                                                    contentDescription = "Remove"
                                                )
                                            }
                                        }
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
