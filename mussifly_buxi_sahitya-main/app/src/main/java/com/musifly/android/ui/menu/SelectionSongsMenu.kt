package com.musifly.android.ui.menu

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.musifly.innertube.YouTube
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalDownloadUtil
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.db.entities.PlaylistSongMap
import com.musifly.android.db.entities.Song
import com.musifly.android.extensions.toMediaItem
import com.musifly.android.models.MediaMetadata
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.playback.ExoDownloadService
import com.musifly.android.playback.queues.ListQueue
import com.musifly.android.ui.component.DefaultDialog
import com.musifly.android.ui.component.DownloadGridMenu
import com.musifly.android.ui.component.GridMenu
import com.musifly.android.ui.component.GridMenuItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SelectionSongMenu(
    songSelection: List<Song>,
    onDismiss: () -> Unit,
    clearAction: () -> Unit,
    songPosition: List<PlaylistSongMap>? = emptyList(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val allInLibrary by remember {
        mutableStateOf(
            songSelection.all {
                it.song.inLibrary != null
            },
        )
    }

    val allLiked by remember(songSelection) {
        mutableStateOf(
            songSelection.isNotEmpty() && songSelection.all {
                it.song.liked
            },
        )
    }

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(songSelection) {
        if (songSelection.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songSelection.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songSelection.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                songSelection.forEach { song ->
                    playlist.playlist.browseId?.let { browseId ->
                        YouTube.addToPlaylist(browseId, song.id)
                    }
                }
            }
            songSelection.map { it.id }
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
    )

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, "selection"),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                    },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                                context.applicationContext,
                                com.musifly.android.di.CachedDownloadsRepositoryEntryPoint::class.java
                            )
                            val cachedRepo = entryPoint.cachedDownloadsRepository()
                            
                            songSelection.forEach { song ->
                                // 1. Remove from Repository (Supabase + Local DB)
                                cachedRepo.removeCachedDownload(song.song.id)

                                // 2. Remove from ExoPlayer Cache
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.song.id,
                                        false,
                                    )
                                }
                            }
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    GridMenu(
        contentPadding =
            PaddingValues(
                start = 8.dp,
                top = 8.dp,
                end = 8.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        GridMenuItem(
            icon = R.drawable.play,
            title = R.string.play,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    title = "Selection",
                    items = songSelection.map { it.toMediaItem() },
                ),
            )
            clearAction()
        }

        GridMenuItem(
            icon = R.drawable.shuffle,
            title = R.string.shuffle,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    title = "Selection",
                    items = songSelection.shuffled().map { it.toMediaItem() },
                ),
            )
            clearAction()
        }

        GridMenuItem(
            icon = R.drawable.queue_music,
            title = R.string.add_to_queue,
        ) {
            onDismiss()
            playerConnection.addToQueue(songSelection.map { it.toMediaItem() })
            clearAction()
        }

        GridMenuItem(
            icon = R.drawable.playlist_add,
            title = R.string.add_to_playlist,
        ) {
            showChoosePlaylistDialog = true
        }

        if (allInLibrary) {
            GridMenuItem(
                icon = R.drawable.library_add_check,
                title = R.string.remove_from_library,
            ) {
                database.query {
                    songSelection.forEach { song ->
                        inLibrary(song.id, null)
                    }
                }
            }
        } else {
            GridMenuItem(
                icon = R.drawable.library_add,
                title = R.string.add_to_library,
            ) {
                database.transaction {
                    songSelection.forEach { song ->
                        insert(song.toMediaMetadata())
                        inLibrary(song.id, LocalDateTime.now())
                    }
                }
            }
        }

        DownloadGridMenu(
            state = downloadState,
            onDownload = {
                songSelection.forEach { song ->
                    val downloadRequest =
                        DownloadRequest
                            .Builder(song.id, song.id.toUri())
                            .setCustomCacheKey(song.id)
                            .setData(song.song.title.toByteArray())
                            .build()
                    DownloadService.sendAddDownload(
                        context,
                        ExoDownloadService::class.java,
                        downloadRequest,
                        false,
                    )
                }
            },
            onRemoveDownload = {
                showRemoveDownloadDialog = true
            },
        )

        GridMenuItem(
            icon = if (allLiked) R.drawable.favorite else R.drawable.favorite_border,
            title = if (allLiked) R.string.dislike_all else R.string.like_all,
        ) {
            val currentlyAllLiked = songSelection.all { it.song.liked }
            onDismiss()
            
            coroutineScope.launch(Dispatchers.IO) {
                val likesRepo = dagger.hilt.android.EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    com.musifly.android.di.LikesRepositoryEntryPoint::class.java
                ).likesRepository()
                
                songSelection.forEach { song ->
                    if (currentlyAllLiked) {
                        // Dislike all: Only unlike those that are currently liked
                        if (song.song.liked) {
                            likesRepo.unlikeSong(song.id)
                        }
                    } else {
                        // Like all: Only like those that are NOT currently liked
                        if (!song.song.liked) {
                            val source = if (song.id.startsWith("saavn:")) "jiosaavn" else "youtube"
                            likesRepo.likeSong(
                                songId = song.id,
                                title = song.song.title,
                                thumbnailUrl = song.song.thumbnailUrl,
                                source = source
                            )
                        }
                    }
                }
                clearAction()
            }
        }

        if (songPosition?.size != 0) {
            GridMenuItem(
                icon = R.drawable.delete,
                title = R.string.delete,
            ) {
                onDismiss()
                var i = 0
                database.query {
                    songPosition?.forEach { cur ->
                        move(cur.playlistId, cur.position - i, Int.MAX_VALUE)
                        delete(cur.copy(position = Int.MAX_VALUE))
                        i++
                    }
                }
                clearAction()
            }
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SelectionMediaMetadataMenu(
    songSelection: List<MediaMetadata>,
    currentItems: List<Timeline.Window>,
    onDismiss: () -> Unit,
    clearAction: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val allLiked by remember(songSelection) {
        mutableStateOf(songSelection.isNotEmpty() && songSelection.all { it.liked })
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                songSelection.forEach { song ->
                    playlist.playlist.browseId?.let { browseId ->
                        YouTube.addToPlaylist(browseId, song.id)
                    }
                }
            }
            songSelection.map { it.id }
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
    )

    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(songSelection) {
        if (songSelection.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songSelection.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songSelection.all {
                        downloads[it.id]?.state == Download.STATE_QUEUED ||
                                downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, "selection"),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                    },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }

                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                                context.applicationContext,
                                com.musifly.android.di.CachedDownloadsRepositoryEntryPoint::class.java
                            )
                            val cachedRepo = entryPoint.cachedDownloadsRepository()
                            
                            songSelection.forEach { song ->
                                // 1. Remove from Repository (Supabase + Local DB)
                                cachedRepo.removeCachedDownload(song.id)

                                // 2. Remove from ExoPlayer Cache
                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.id,
                                        false,
                                    )
                                }
                            }
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    GridMenu(
        contentPadding =
            PaddingValues(
                start = 8.dp,
                top = 8.dp,
                end = 8.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        if (currentItems.isNotEmpty()) {
            GridMenuItem(
                icon = R.drawable.delete,
                title = R.string.delete,
            ) {
                onDismiss()
                var i = 0
                currentItems.forEach { cur ->
                    if (playerConnection.player.availableCommands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS)) {
                        playerConnection.player.removeMediaItem(cur.firstPeriodIndex - i++)
                    }
                }
                clearAction()
            }
        }

        GridMenuItem(
            icon = R.drawable.play,
            title = R.string.play,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    title = "Selection",
                    items = songSelection.map { it.toMediaItem() },
                ),
            )
            clearAction()
        }

        GridMenuItem(
            icon = R.drawable.shuffle,
            title = R.string.shuffle,
        ) {
            onDismiss()
            playerConnection.playQueue(
                ListQueue(
                    title = "Selection",
                    items = songSelection.shuffled().map { it.toMediaItem() },
                ),
            )
            clearAction()
        }

        GridMenuItem(
            icon = R.drawable.queue_music,
            title = R.string.add_to_queue,
        ) {
            onDismiss()
            playerConnection.addToQueue(songSelection.map { it.toMediaItem() })
            clearAction()
        }

        GridMenuItem(
            icon = R.drawable.playlist_add,
            title = R.string.add_to_playlist,
        ) {
            showChoosePlaylistDialog = true
        }

        GridMenuItem(
            icon = if (allLiked) R.drawable.favorite else R.drawable.favorite_border,
            title = R.string.like_all,
        ) {
            val currentlyAllLiked = songSelection.all { it.liked }
            onDismiss()
            
            coroutineScope.launch(Dispatchers.IO) {
                val likesRepo = dagger.hilt.android.EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    com.musifly.android.di.LikesRepositoryEntryPoint::class.java
                ).likesRepository()
                
                songSelection.forEach { mediaMetadata ->
                    if (currentlyAllLiked) {
                        // Dislike all
                        if (mediaMetadata.liked) {
                            likesRepo.unlikeSong(mediaMetadata.id)
                        }
                    } else {
                        // Like all
                        if (!mediaMetadata.liked) {
                            val source = if (mediaMetadata.id.startsWith("saavn:")) "jiosaavn" else "youtube"
                            likesRepo.likeSong(
                                songId = mediaMetadata.id,
                                title = mediaMetadata.title,
                                thumbnailUrl = mediaMetadata.thumbnailUrl,
                                source = source
                            )
                        }
                    }
                }
                clearAction()
            }
        }

        DownloadGridMenu(
            state = downloadState,
            onDownload = {
                songSelection.forEach { song ->
                    val downloadRequest =
                        DownloadRequest
                            .Builder(song.id, song.id.toUri())
                            .setCustomCacheKey(song.id)
                            .setData(song.title.toByteArray())
                            .build()
                    DownloadService.sendAddDownload(
                        context,
                        ExoDownloadService::class.java,
                        downloadRequest,
                        false,
                    )
                }
            },
            onRemoveDownload = {
                showRemoveDownloadDialog = true
            },
        )
    }
}