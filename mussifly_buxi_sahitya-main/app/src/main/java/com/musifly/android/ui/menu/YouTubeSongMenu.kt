package com.musifly.android.ui.menu

import android.annotation.SuppressLint
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.SongItem
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalDownloadUtil
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.musifly.android.constants.ListItemHeight
import com.musifly.android.constants.ListThumbnailSize
import com.musifly.android.constants.ThumbnailCornerRadius
import com.musifly.android.db.entities.SongEntity
import com.musifly.android.extensions.toMediaItem
import com.musifly.android.models.MediaMetadata
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.playback.ExoDownloadService
import com.musifly.android.playback.queues.YouTubeQueue
import com.musifly.android.ui.component.DownloadGridMenu
import com.musifly.android.ui.component.GridMenu
import com.musifly.android.ui.component.GridMenuItem
import com.musifly.android.ui.component.ListDialog
import com.musifly.android.ui.component.ListItem
import com.musifly.android.utils.joinByBullet
import com.musifly.android.utils.makeTimeString
import java.time.LocalDateTime

@SuppressLint("MutableCollectionMutableState")
@Composable
fun YouTubeSongMenu(
    song: SongItem,
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(song.id).collectAsState(initial = null)
    val download by LocalDownloadUtil.current.getDownload(song.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val artists =
        remember {
            song.artists.mapNotNull {
                it.id?.let { artistId ->
                    MediaMetadata.Artist(id = artistId, name = it.name)
                }
            }
        }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<MediaMetadata>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(song.toMediaMetadata())
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { browseId ->
                    YouTube.addToPlaylist(browseId, song.id)
                }
            }
            listOf(song.id)
        },
        onDismiss = { showChoosePlaylistDialog = false }
    )

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists) { artist ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier =
                            Modifier
                                .fillParentMaxWidth()
                                .height(ListItemHeight)
                                .clickable {
                                    navController.navigate("artist/${artist.id}")
                                    showSelectArtistDialog = false
                                    onDismiss()
                                }
                                .padding(horizontal = 24.dp),
                    ) {
                        Text(
                            text = artist.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }

    ListItem(
        title = song.title,
        subtitle =
            joinByBullet(
                song.artists.joinToString { it.name },
                song.duration?.let { makeTimeString(it * 1000L) },
            ),
        thumbnailContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.thumbnail)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(ListThumbnailSize)
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )
        },
        trailingContent = {
            IconButton(
                onClick = {
                    // Use LikesRepository to ensure Firestore sync for logged-in users
                    val likesRepository = dagger.hilt.android.EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.musifly.android.di.LikesRepositoryEntryPoint::class.java
                    ).likesRepository()
                    
                    coroutineScope.launch(Dispatchers.IO) {
                        val isCurrentlyLiked = librarySong?.song?.liked == true
                        if (isCurrentlyLiked) {
                            likesRepository.unlikeSong(song.id).fold(
                                onSuccess = {
                                    // Success - repository already updated database
                                },
                                onFailure = { e ->
                                    android.util.Log.e("YouTubeSongMenu", "Failed to unlike song", e)
                                }
                            )
                        } else {
                            likesRepository.likeSong(
                                songId = song.id,
                                title = song.title,
                                thumbnailUrl = song.thumbnail,
                                source = "youtube"
                            ).fold(
                                onSuccess = {
                                    // Success - repository already updated database
                                },
                                onFailure = { e ->
                                    android.util.Log.e("YouTubeSongMenu", "Failed to like song", e)
                                }
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
            playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
            onDismiss()
        }
        GridMenuItem(
            icon = R.drawable.playlist_play,
            title = R.string.play_next,
        ) {
            playerConnection.playNext(song.toMediaItem())
            onDismiss()
        }
        GridMenuItem(
            icon = R.drawable.queue_music,
            title = R.string.add_to_queue,
        ) {
            playerConnection.addToQueue((song.toMediaItem()))
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
                    insert(song.toMediaMetadata())
                }
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
            },
            onRemoveDownload = {
                // Permanent Delete Logic
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.musifly.android.di.CachedDownloadsRepositoryEntryPoint::class.java
                        )
                        val cachedRepo = entryPoint.cachedDownloadsRepository()
                        cachedRepo.removeCachedDownload(song.id)
                    } catch (e: Exception) {
                        android.util.Log.e("YouTubeSongMenu", "Failed to remove from repo", e)
                    }
                    
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        DownloadService.sendRemoveDownload(
                            context,
                            ExoDownloadService::class.java,
                            song.id,
                            false,
                        )
                        onDismiss()
                    }
                }
            },
        )
        if (artists.isNotEmpty()) {
            GridMenuItem(
                icon = R.drawable.artist,
                title = R.string.view_artist,
            ) {
                if (artists.size == 1) {
                    navController.navigate("artist/${artists[0].id}")
                    onDismiss()
                } else {
                    showSelectArtistDialog = true
                }
            }
        }
        song.album?.let { album ->
            GridMenuItem(
                icon = R.drawable.album,
                title = R.string.view_album,
            ) {
                navController.navigate("album/${album.id}")
                onDismiss()
            }
        }
        if (librarySong?.song?.inLibrary != null) {
            GridMenuItem(
                icon = R.drawable.library_add_check,
                title = R.string.remove_from_library,
            ) {
                database.query {
                    inLibrary(song.id, null)
                }
            }
        } else {
            GridMenuItem(
                icon = R.drawable.library_add,
                title = R.string.add_to_library,
            ) {
                database.transaction {
                    insert(song.toMediaMetadata())
                    inLibrary(song.id, LocalDateTime.now())
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
                    putExtra(Intent.EXTRA_TEXT, song.shareLink)
                }
            context.startActivity(Intent.createChooser(intent, null))
            onDismiss()
        }
    }
}
