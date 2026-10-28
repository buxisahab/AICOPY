package com.buxi.music.ui.screens.artist

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.buxi.music.utils.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.buxi.music.ui.theme.ThemeTextTertiary
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.buxi.music.innertube.models.AlbumItem
import com.buxi.music.innertube.models.ArtistItem
import com.buxi.music.innertube.models.PlaylistItem
import com.buxi.music.innertube.models.SongItem
import com.buxi.music.innertube.models.WatchEndpoint
import com.buxi.music.LocalDatabase
import com.buxi.music.LocalPlayerAwareWindowInsets
import com.buxi.music.LocalPlayerConnection
import com.buxi.music.R
import com.buxi.music.constants.AppBarHeight
import com.buxi.music.db.entities.ArtistEntity
import com.buxi.music.extensions.togglePlayPause
import com.buxi.music.models.toMediaMetadata
import com.buxi.music.playback.queues.YouTubeQueue
import com.buxi.music.ui.component.AutoResizeText
import com.buxi.music.ui.component.FontSizeRange
import com.buxi.music.ui.component.IconButton
import com.buxi.music.ui.component.LocalMenuState
import com.buxi.music.ui.component.NavigationTitle
import com.buxi.music.ui.component.SongListItem
import com.buxi.music.ui.component.YouTubeGridItem
import com.buxi.music.ui.component.YouTubeListItem
import com.buxi.music.ui.component.shimmer.ButtonPlaceholder
import com.buxi.music.ui.component.shimmer.ListItemPlaceHolder
import com.buxi.music.ui.component.shimmer.ShimmerHost
import com.buxi.music.ui.component.shimmer.TextPlaceholder
import com.buxi.music.ui.menu.SongMenu
import com.buxi.music.ui.menu.YouTubeAlbumMenu
import com.buxi.music.ui.menu.YouTubeArtistMenu
import com.buxi.music.ui.menu.YouTubePlaylistMenu
import com.buxi.music.ui.menu.YouTubeSongMenu
import com.buxi.music.ui.utils.backToMain
import com.buxi.music.ui.utils.fadingEdge
import com.buxi.music.ui.utils.resize
import com.buxi.music.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer

@SuppressLint("ServiceCast")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()

    val lazyListState = rememberLazyListState()

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 200
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding =
            LocalPlayerAwareWindowInsets.current
                .add(
                    WindowInsets(
                        top = -WindowInsets.systemBars.asPaddingValues()
                            .calculateTopPadding() - AppBarHeight
                    )
                )
                .asPaddingValues(),
    ) {
        artistPage.let {
            if (artistPage != null) {
                item(key = "header") {
                    Column {
                        // Header con imagen de fondo estilo YouTube Music
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp)
                        ) {
                            // Imagen de fondo
                            AsyncImage(
                                model = artistPage.artist.thumbnail.resize(1200, 1000),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Gradiente oscuro en la parte inferior
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.7f),
                                                MaterialTheme.colorScheme.background
                                            ),
                                            startY = 0f,
                                            endY = 1200f
                                        )
                                    )
                            )

                            // Contenido sobre la imagen
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Imagen circular del artista (estilo YouTube Music)
                                AsyncImage(
                                    model = artistPage.artist.thumbnail.resize(200, 200),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(CircleShape)
                                )

                                // Nombre del artista
                                Text(
                                    text = artistPage.artist.title,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Botones de acción (estilo YouTube Music)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            // Botón de Suscribirse
                            Button(
                                onClick = {
                                    database.transaction {
                                        val artist = libraryArtist?.artist
                                        if (artist != null) {
                                            update(artist.toggleLike())
                                        } else {
                                            artistPage.artist.let {
                                                insert(
                                                    ArtistEntity(
                                                        id = it.id,
                                                        name = it.title,
                                                        channelId = it.channelId,
                                                        thumbnailUrl = it.thumbnail,
                                                    ).toggleLike()
                                                )
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (libraryArtist?.artist?.bookmarkedAt != null)
                                        MaterialTheme.colorScheme.surfaceVariant
                                    else
                                        MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(
                                    text = if (libraryArtist?.artist?.bookmarkedAt != null)
                                        stringResource(R.string.subscribed)
                                    else
                                        stringResource(R.string.subscribe),
                                    fontWeight = FontWeight.SemiBold,
                                    color = ThemeTextTertiary
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            // Botón de Mezclar (Shuffle)
                            artistPage.artist.shuffleEndpoint?.let { shuffleEndpoint ->
                                IconButton(
                                    onClick = {
                                        playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer,
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = stringResource(R.string.shuffle),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // Botón de Compartir
                            IconButton(
                                onClick = {
                                    artistPage.artist.shareLink?.let { link ->
                                        val clipboard =
                                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Artist Link", link)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.link),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                if (librarySongs.isNotEmpty()) {
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.from_your_library),
                            onClick = {
                                navController.navigate("artist/${viewModel.artistId}/songs")
                            },
                        )
                    }

                    items(
                        items = librarySongs,
                        key = { "local_${it.id}" },
                    ) { song ->
                        SongListItem(
                            song = song,
                            showInLibraryIcon = true,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        WatchEndpoint(
                                                            videoId = song.id,
                                                        ),
                                                        song.toMediaMetadata(),
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    )
                                    .animateItem(),
                        )
                    }
                }

                artistPage.sections.fastForEach { section ->
                    if (section.items.isNotEmpty()) {
                        item {
                            NavigationTitle(
                                title = section.title,
                                onClick =
                                    section.moreEndpoint?.let {
                                        {
                                            navController.navigate(
                                                "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}",
                                            )
                                        }
                                    },
                            )
                        }
                    }

                    if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                        items(
                            items = section.items,
                            key = { it.id },
                        ) { song ->
                            YouTubeListItem(
                                item = song as SongItem,
                                isActive = mediaMetadata?.id == song.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null,
                                        )
                                    }
                                },
                                modifier =
                                    Modifier
                                        .combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = song.id),
                                                            song.toMediaMetadata()
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        )
                                        .animateItem(),
                            )
                        }
                    } else {
                        item {
                            LazyRow {
                                items(
                                    items = section.items,
                                    key = { it.id },
                                ) { item ->
                                    YouTubeGridItem(
                                        item = item,
                                        isActive =
                                            when (item) {
                                                is SongItem -> mediaMetadata?.id == item.id
                                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                                else -> false
                                            },
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier =
                                            Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        when (item) {
                                                            is SongItem ->
                                                                playerConnection.playQueue(
                                                                    YouTubeQueue(
                                                                        WatchEndpoint(videoId = item.id),
                                                                        item.toMediaMetadata()
                                                                    ),
                                                                )

                                                            is AlbumItem -> {
                                                                try {
                                                                    if (item.id.isNotEmpty()) {
                                                                        navController.navigate("album/${item.id}")
                                                                    } else {
                                                                        Log.w(
                                                                            "ArtistScreen",
                                                                            "Album ID is empty"
                                                                        )
                                                                        Toast.makeText(
                                                                            context,
                                                                            "Error: Invalid album ID",
                                                                            Toast.LENGTH_SHORT
                                                                        ).show()
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Log.e(
                                                                        "ArtistScreen",
                                                                        "Navigation error to album: ${item.id}",
                                                                        e
                                                                    )
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Navigation error",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            }

                                                            is ArtistItem -> {
                                                                try {
                                                                    if (item.id.isNotEmpty()) {
                                                                        navController.navigate("artist/${item.id}")
                                                                    } else {
                                                                        Log.w(
                                                                            "ArtistScreen",
                                                                            "Artist ID is empty"
                                                                        )
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Log.e(
                                                                        "ArtistScreen",
                                                                        "Navigation error to artist: ${item.id}",
                                                                        e
                                                                    )
                                                                }
                                                            }

                                                            is PlaylistItem -> {
                                                                try {
                                                                    if (item.id.isNotEmpty()) {
                                                                        navController.navigate("online_playlist/${item.id}")
                                                                    } else {
                                                                        Log.w(
                                                                            "ArtistScreen",
                                                                            "Playlist ID is empty"
                                                                        )
                                                                    }
                                                                } catch (e: Exception) {
                                                                    Log.e(
                                                                        "ArtistScreen",
                                                                        "Navigation error to playlist: ${item.id}",
                                                                        e
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        menuState.show {
                                                            when (item) {
                                                                is SongItem ->
                                                                    YouTubeSongMenu(
                                                                        song = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is AlbumItem ->
                                                                    YouTubeAlbumMenu(
                                                                        albumItem = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is ArtistItem ->
                                                                    YouTubeArtistMenu(
                                                                        artist = item,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is PlaylistItem ->
                                                                    YouTubePlaylistMenu(
                                                                        playlist = item,
                                                                        coroutineScope = coroutineScope,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                            }
                                                        }
                                                    },
                                                )
                                                .animateItem(),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item(key = "shimmer") {
                    ShimmerHost {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3),
                        ) {
                            Spacer(
                                modifier =
                                    Modifier
                                        .shimmer()
                                        .background(MaterialTheme.colorScheme.onSurface)
                                        .fadingEdge(
                                            top =
                                                WindowInsets.systemBars
                                                    .asPaddingValues()
                                                    .calculateTopPadding() + AppBarHeight,
                                            bottom = 108.dp,
                                        ),
                            )
                            TextPlaceholder(
                                height = 56.dp,
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(horizontal = 48.dp),
                            )
                        }

                        Row(
                            modifier = Modifier.padding(12.dp),
                        ) {
                            ButtonPlaceholder(Modifier.weight(1f))

                            Spacer(Modifier.width(12.dp))

                            ButtonPlaceholder(Modifier.weight(1f))
                        }

                        repeat(6) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { if (!transparentAppBar) Text(artistPage?.artist?.title.orEmpty()) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        colors =
            if (transparentAppBar) {
                TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            } else {
                TopAppBarDefaults.topAppBarColors()
            },
    )
}
