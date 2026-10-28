package com.musifly.android.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.CONTENT_TYPE_LIST
import com.musifly.android.constants.ListItemHeight
import com.musifly.android.db.entities.Album
import com.musifly.android.db.entities.Artist
import com.musifly.android.db.entities.Playlist
import com.musifly.android.db.entities.Song
import com.musifly.android.extensions.toMediaItem
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.playback.queues.ListQueue
import com.musifly.android.ui.component.AlbumListItem
import com.musifly.android.ui.component.ArtistListItem
import com.musifly.android.ui.component.ChipsRow
import com.musifly.android.ui.component.EmptyPlaceholder
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.component.PlaylistListItem
import com.musifly.android.ui.component.SongListItem
import com.musifly.android.ui.menu.SongMenu
import com.musifly.android.viewmodels.LocalFilter
import com.musifly.android.viewmodels.LocalSearchViewModel
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalSearchScreen(
    query: String,
    navController: NavController,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
    pureBlack: Boolean,
    viewModel: LocalSearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val searchFilter by viewModel.filter.collectAsState()
    val result by viewModel.result.collectAsState()
    val queueSearchedSongs = stringResource(R.string.queue_searched_songs)

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
            .padding(top = 8.dp)
    ) {
        ChipsRow(
            chips = listOf(
                LocalFilter.ALL to stringResource(R.string.filter_all),
                LocalFilter.SONG to stringResource(R.string.filter_songs),
                LocalFilter.ALBUM to stringResource(R.string.filter_albums),
                LocalFilter.ARTIST to stringResource(R.string.filter_artists),
                LocalFilter.PLAYLIST to stringResource(R.string.filter_playlists),
            ),
            currentValue = searchFilter,
            onValueUpdate = { viewModel.filter.value = it },
        )

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f),
        ) {
            result.map.forEach { (filter, items) ->
                if (result.filter == LocalFilter.ALL) {
                    item(key = filter) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ListItemHeight)
                                .clickable { viewModel.filter.value = filter }
                                .padding(start = 12.dp, end = 18.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    when (filter) {
                                        LocalFilter.SONG -> R.string.filter_songs
                                        LocalFilter.ALBUM -> R.string.filter_albums
                                        LocalFilter.ARTIST -> R.string.filter_artists
                                        LocalFilter.PLAYLIST -> R.string.filter_playlists
                                        LocalFilter.SAAVN_SONG -> R.string.filter_songs // Reuse songs string
                                         LocalFilter.ALL -> error("")
                                     }
                                 ),
                                 style = MaterialTheme.typography.titleLarge,
                                 modifier = Modifier.weight(1f),
                             )

                             Icon(
                                 painter = painterResource(R.drawable.navigate_next),
                                 contentDescription = null,
                             )
                         }
                     }
                 }

                 items(
                     items = items,
                     key = { item ->
                         when (item) {
                             is com.musifly.android.models.SaavnSong -> item.id
                             is Song -> item.id
                             is Album -> item.id
                             is Artist -> item.id
                             is Playlist -> item.id
                             else -> item.hashCode()
                         }
                     },
                     contentType = { CONTENT_TYPE_LIST },
                 ) { item ->
                     when (item) {
                         is com.musifly.android.models.SaavnSong -> {
                             val saavnMediaMetadata = item.toMediaMetadata()
                             com.musifly.android.ui.component.MediaMetadataListItem(
                                 mediaMetadata = saavnMediaMetadata,
                                 isActive = saavnMediaMetadata.id == mediaMetadata?.id,
                                 isPlaying = isPlaying,
                                 trailingContent = {
                                     IconButton(
                                         onClick = {
                                             menuState.show {
                                                 com.musifly.android.ui.menu.SaavnSongMenu(
                                                     mediaMetadata = saavnMediaMetadata,
                                                     navController = navController,
                                                     onDismiss = {
                                                         onDismiss()
                                                         menuState.dismiss()
                                                     },
                                                     saavnAPI = viewModel.saavnAPI
                                                 )
                                             }
                                         }
                                     ) {
                                         Icon(
                                             painter = painterResource(R.drawable.more_vert),
                                             contentDescription = null,
                                         )
                                     }
                                 },
                                 modifier = Modifier
                                     .combinedClickable(
                                         onClick = {
                                             if (saavnMediaMetadata.id == mediaMetadata?.id) {
                                                 playerConnection.player.togglePlayPause()
                                             } else {
                                                 playerConnection.playQueue(
                                                     com.musifly.android.playback.queues.SaavnQueue.radio(saavnMediaMetadata, viewModel.saavnAPI)
                                                 )
                                             }
                                         },
                                         onLongClick = {
                                             menuState.show {
                                                 com.musifly.android.ui.menu.SaavnSongMenu(
                                                     mediaMetadata = saavnMediaMetadata,
                                                     navController = navController,
                                                     onDismiss = {
                                                         onDismiss()
                                                         menuState.dismiss()
                                                     },
                                                     saavnAPI = viewModel.saavnAPI
                                                 )
                                             }
                                         }
                                     )
                                     .animateItem(),
                             )
                         }
                          is Song -> SongListItem(
                             song = item,
                             showInLibraryIcon = true,
                             isActive = item.id == mediaMetadata?.id,
                             isPlaying = isPlaying,
                             trailingContent = {
                                 IconButton(
                                     onClick = {
                                         menuState.show {
                                             SongMenu(
                                                 originalSong = item,
                                                 navController = navController,
                                                 onDismiss = {
                                                     onDismiss()
                                                     menuState.dismiss()
                                                 },
                                                 isFromCache = isFromCache
                                             )
                                         }
                                     }
                                 ) {
                                     Icon(
                                         painter = painterResource(R.drawable.more_vert),
                                         contentDescription = null,
                                     )
                                 }
                             },
                             modifier = Modifier
                                 .combinedClickable(
                                     onClick = {
                                         if (item.id == mediaMetadata?.id) {
                                             playerConnection.player.togglePlayPause()
                                         } else {
                                             val songs = result.map
                                                 .getOrDefault(LocalFilter.SONG, emptyList())
                                                 .filterIsInstance<Song>()
                                                 .map { it.toMediaItem() }
                                             playerConnection.playQueue(
                                                 ListQueue(
                                                     title = queueSearchedSongs,
                                                     items = songs,
                                                     startIndex = songs.indexOfFirst { it.mediaId == item.id },
                                                 )
                                             )
                                         }
                                     },
                                     onLongClick = {
                                         menuState.show {
                                             SongMenu(
                                                 originalSong = item,
                                                 navController = navController,
                                                 onDismiss = {
                                                     onDismiss()
                                                     menuState.dismiss()
                                                 },
                                                 isFromCache = isFromCache
                                             )
                                         }
                                     }
                                 )
                                 .animateItem(),
                         )

                         is Album -> AlbumListItem(
                             album = item,
                             isActive = item.id == mediaMetadata?.album?.id,
                             isPlaying = isPlaying,
                             modifier = Modifier
                                 .clickable {
                                     onDismiss()
                                     navController.navigate("album/${item.id}")
                                 }
                                 .animateItem(),
                         )

                         is Artist -> ArtistListItem(
                             artist = item,
                             modifier = Modifier
                                 .clickable {
                                     onDismiss()
                                     navController.navigate("artist/${item.id}")
                                 }
                                 .animateItem(),
                         )

                         is Playlist -> PlaylistListItem(
                             playlist = item,
                             modifier = Modifier
                                 .clickable {
                                     onDismiss()
                                     navController.navigate("local_playlist/${item.id}")
                                 }
                                 .animateItem(),
                         )
                     }
                 }
             }

            if (result.query.isNotEmpty() && result.map.isEmpty()) {
                item(key = "no_result") {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        }
    }
}