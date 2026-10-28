package com.musifly.android.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalDownloadUtil
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.extensions.toMediaItem
import com.musifly.android.models.MediaMetadata
import com.musifly.android.playback.ExoDownloadService
import com.musifly.android.playback.queues.SaavnQueue
import com.musifly.android.ui.component.DownloadGridMenu
import com.musifly.android.ui.component.GridMenu
import com.musifly.android.ui.component.GridMenuItem
import com.musifly.android.ui.component.MediaMetadataListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SaavnSongMenu(
    mediaMetadata: MediaMetadata,
    navController: NavController,
    onDismiss: () -> Unit,
    saavnAPI: com.musifly.android.streamingengines.saavn.SaavnAPI,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            database.transaction {
                insert(mediaMetadata)
            }
            listOf(mediaMetadata.id)
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    MediaMetadataListItem(
        mediaMetadata = mediaMetadata,
        isActive = false,
        isPlaying = false,
        trailingContent = {
            IconButton(
                onClick = {
                    val likesRepository = dagger.hilt.android.EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.musifly.android.di.LikesRepositoryEntryPoint::class.java
                    ).likesRepository()
                    
                    coroutineScope.launch(Dispatchers.IO) {
                        val isCurrentlyLiked = librarySong?.song?.liked == true
                        if (isCurrentlyLiked) {
                            likesRepository.unlikeSong(mediaMetadata.id).fold(
                                onSuccess = {},
                                onFailure = { e -> android.util.Log.e("SaavnSongMenu", "Failed to unlike song", e) }
                            )
                        } else {
                            likesRepository.likeSong(
                                songId = mediaMetadata.id,
                                title = mediaMetadata.title,
                                thumbnailUrl = mediaMetadata.thumbnailUrl,
                                source = "jiosaavn"
                            ).fold(
                                onSuccess = {},
                                onFailure = { e -> android.util.Log.e("SaavnSongMenu", "Failed to like song", e) }
                            )
                        }
                    }
                },
            ) {
                Icon(
                    painter = painterResource(if (librarySong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (librarySong?.song?.liked == true) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null,
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    HorizontalDivider()

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
            icon = R.drawable.radio,
            title = R.string.start_radio,
        ) {
            playerConnection.playQueue(SaavnQueue.radio(mediaMetadata, saavnAPI))
            onDismiss()
        }
        GridMenuItem(
            icon = R.drawable.playlist_play,
            title = R.string.play_next,
        ) {
            playerConnection.playNext(mediaMetadata.toMediaItem())
            onDismiss()
        }
        GridMenuItem(
            icon = R.drawable.queue_music,
            title = R.string.add_to_queue,
        ) {
            playerConnection.addToQueue(mediaMetadata.toMediaItem())
            onDismiss()
        }
        GridMenuItem(
            icon = R.drawable.playlist_add,
            title = R.string.add_to_playlist,
        ) {
            showChoosePlaylistDialog = true
        }
        DownloadGridMenu(
            state = download?.state,
            onDownload = {
                database.transaction {
                    insert(mediaMetadata)
                }
                val downloadRequest =
                    DownloadRequest
                        .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                        .setCustomCacheKey(mediaMetadata.id)
                        .setData(mediaMetadata.title.toByteArray())
                        .build()
                DownloadService.sendAddDownload(
                    context,
                    ExoDownloadService::class.java,
                    downloadRequest,
                    false,
                )
            },
            onRemoveDownload = {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.musifly.android.di.CachedDownloadsRepositoryEntryPoint::class.java
                        )
                        val cachedRepo = entryPoint.cachedDownloadsRepository()
                        cachedRepo.removeCachedDownload(mediaMetadata.id)
                    } catch (e: Exception) {
                        android.util.Log.e("SaavnSongMenu", "Failed to remove from repo", e)
                    }
                    
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        DownloadService.sendRemoveDownload(
                            context,
                            ExoDownloadService::class.java,
                            mediaMetadata.id,
                            false,
                        )
                        onDismiss()
                    }
                }
            },
        )
        
        if (librarySong?.song?.inLibrary != null) {
            GridMenuItem(
                icon = R.drawable.library_add_check,
                title = R.string.remove_from_library,
            ) {
                database.query {
                    inLibrary(mediaMetadata.id, null)
                }
            }
        } else {
            GridMenuItem(
                icon = R.drawable.library_add,
                title = R.string.add_to_library,
            ) {
                database.transaction {
                    insert(mediaMetadata)
                    inLibrary(mediaMetadata.id, LocalDateTime.now())
                }
            }
        }
        
        GridMenuItem(
            icon = R.drawable.share,
            title = R.string.share,
        ) {
            val intent =
                Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    // Saavn share link generation (simple implementation)
                    val shareLink = "https://www.jiosaavn.com/song/${mediaMetadata.id.removePrefix("saavn:")}"
                    putExtra(Intent.EXTRA_TEXT, shareLink)
                }
            context.startActivity(Intent.createChooser(intent, null))
            onDismiss()
        }
    }
}
