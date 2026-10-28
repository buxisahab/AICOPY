package com.musifly.android.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import com.musifly.android.ui.component.AppIconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.musifly.android.ui.component.SongListItem
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.menu.SongMenu
import com.musifly.android.viewmodels.LikesViewModel
import kotlinx.coroutines.flow.first

/**
 * Screen displaying user's liked songs
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LikedSongsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val viewModel: LikesViewModel = hiltViewModel()
    
    // Use songs directly from ViewModel which handles fetching by ID
    // independent of "inLibrary" status
    val songs by viewModel.likedSongs.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    
    val mediaMetadata = playerConnection.mediaMetadata.collectAsState(initial = null).value
    val isPlaying = playerConnection.isPlaying.collectAsState().value
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.liked)) },
                navigationIcon = {
                    AppIconButton(
                        onClick = { 
                            navController.navigateUp()
                        },
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null
                    )
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
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
                            .padding(24.dp)
                            .windowInsetsPadding(
                                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.favorite),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No liked songs",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Songs you like will appear here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    val bottomPadding = LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom)
                        .asPaddingValues()
                        .calculateBottomPadding()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            end = 8.dp,
                            top = 8.dp,
                            bottom = 8.dp + bottomPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
