package com.musifly.android.ui.screens.artist

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import com.musifly.android.playback.queues.SaavnQueue
import com.musifly.android.models.MediaMetadata
import com.musifly.android.extensions.toMediaItem
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import com.musifly.android.ui.component.AppIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.musifly.innertube.models.AlbumItem
import com.musifly.innertube.models.ArtistItem
import com.musifly.innertube.models.PlaylistItem
import com.musifly.innertube.models.SongItem
import com.musifly.innertube.models.WatchEndpoint
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.AppBarHeight
import com.musifly.android.db.entities.ArtistEntity
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.playback.queues.YouTubeQueue
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.component.NavigationTitle
import com.musifly.android.ui.component.SongListItem
import com.musifly.android.ui.component.YouTubeGridItem
import com.musifly.android.ui.component.YouTubeListItem
import com.musifly.android.ui.component.shimmer.ButtonPlaceholder
import com.musifly.android.ui.component.shimmer.ListItemPlaceHolder
import com.musifly.android.ui.component.shimmer.ShimmerHost
import com.musifly.android.ui.component.shimmer.TextPlaceholder
import com.musifly.android.models.SaavnArtist
import com.musifly.android.models.toMediaMetadataFromSaavnMap
import com.musifly.android.models.toSaavnArtist
import com.musifly.android.ui.menu.SaavnArtistMenu
import com.musifly.android.ui.menu.SaavnSongMenu
import com.musifly.android.ui.menu.SongMenu
import com.musifly.android.ui.menu.YouTubeAlbumMenu
import com.musifly.android.ui.menu.YouTubeArtistMenu
import com.musifly.android.ui.menu.YouTubePlaylistMenu
import com.musifly.android.ui.menu.YouTubeSongMenu
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.ui.utils.fadingEdge
import com.musifly.android.ui.utils.resize
import com.musifly.android.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch

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
    
    // JioSaavn artist data
    val saavnArtistData by viewModel.saavnArtistData.collectAsState()
    val isSaavnArtist = viewModel.isSaavnArtist

    val lazyListState = rememberLazyListState()

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current
            .add(
                WindowInsets(
                    top = -WindowInsets.systemBars.asPaddingValues()
                        .calculateTopPadding() - AppBarHeight
                )
            )
            .asPaddingValues(),
    ) {
        // ============ JioSaavn Artist ============
        if (isSaavnArtist && saavnArtistData != null) {
            val artistName = saavnArtistData!!["name"] as? String ?: "Unknown Artist"
            // Try multiple image sources
            val artistImage = (saavnArtistData!!["image_url"] as? String)?.takeIf { it.isNotEmpty() }
                ?: (saavnArtistData!!["image"] as? String)?.takeIf { it.isNotEmpty() }
                ?: ""
            val topSongs = saavnArtistData!!["topSongs"] as? List<Map<String, Any>> ?: emptyList()
            val topAlbums = saavnArtistData!!["topAlbums"] as? List<Map<String, Any>> ?: emptyList()
            
            android.util.Log.d("ArtistScreen", "🎤 JioSaavn Artist loaded: $artistName")
            android.util.Log.d("ArtistScreen", "   Image: ${if (artistImage.isNotEmpty()) "✅ ($artistImage)" else "❌"}")
            android.util.Log.d("ArtistScreen", "   Top Songs: ${topSongs.size}")
            android.util.Log.d("ArtistScreen", "   Top Albums: ${topAlbums.size}")
            
            // Artist Header
            item(key = "saavn_header") {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(context)
                                .data(artistImage)
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                        )
                        
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                shadowElevation = 8.dp,
                                modifier = Modifier.size(100.dp)
                            ) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(artistImage)
                                        .allowHardware(false)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Saavn Artist Actions Row
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            // Subscribed/Subscribe Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val bookmarksRepo = dagger.hilt.android.EntryPointAccessors.fromApplication(
                                            context.applicationContext,
                                            com.musifly.android.di.BookmarksRepositoryEntryPoint::class.java
                                        ).bookmarksRepository()
                                        
                                        val artist = libraryArtist?.artist
                                        if (artist != null) {
                                            bookmarksRepo.toggleArtistBookmark(artist)
                                        } else {
                                            bookmarksRepo.toggleArtistBookmark(
                                                ArtistEntity(
                                                    id = "saavn:${saavnArtistData!!["id"] as? String ?: ""}",
                                                    name = artistName,
                                                    channelId = null,
                                                    thumbnailUrl = artistImage,
                                                )
                                            )
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (libraryArtist?.artist?.bookmarkedAt != null)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else
                                        MaterialTheme.colorScheme.primary,
                                    contentColor = if (libraryArtist?.artist?.bookmarkedAt != null)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (libraryArtist?.artist?.bookmarkedAt != null)
                                            R.drawable.library_add_check
                                        else
                                            R.drawable.library_add
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (libraryArtist?.artist?.bookmarkedAt != null)
                                        stringResource(R.string.subscribed)
                                    else
                                        stringResource(R.string.subscribe),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            // Shuffle Button
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                AppIconButton(
                                    onClick = {
                                        val mediaItems = topSongs.map { it.toMediaMetadataFromSaavnMap().toMediaItem() }
                                        playerConnection.playQueue(
                                            com.musifly.android.playback.queues.ListQueue(
                                                items = mediaItems.shuffled(),
                                                title = "$artistName (Shuffled)"
                                            )
                                        )
                                    },
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = stringResource(R.string.shuffle),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    size = 44.dp
                                )
                            }

                            // Start Radio Button
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                AppIconButton(
                                    onClick = {
                                        playerConnection.playQueue(
                                            SaavnQueue.radio(
                                                MediaMetadata(
                                                    id = "saavn:${saavnArtistData!!["id"] as? String ?: ""}",
                                                    title = artistName,
                                                    artists = listOf(MediaMetadata.Artist(id = null, name = artistName)),
                                                    duration = 0
                                                ),
                                                viewModel.saavnAPI
                                            )
                                        )
                                    },
                                    painter = painterResource(R.drawable.radio),
                                    contentDescription = stringResource(R.string.start_radio),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    size = 44.dp
                                )
                            }

                            // More/Menu Button
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                AppIconButton(
                                    onClick = {
                                        menuState.show {
                                            SaavnArtistMenu(
                                                artist = saavnArtistData!!.toSaavnArtist(),
                                                onDismiss = menuState::dismiss,
                                                saavnAPI = viewModel.saavnAPI
                                            )
                                        }
                                    },
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    size = 44.dp
                                )
                            }
                        }
                    }
                }
            }
            
            // Top Songs
            if (topSongs.isNotEmpty()) {
                item {
                    NavigationTitle(title = "Top Songs")
                }
                
                items(topSongs.size) { index ->
                    val song = topSongs[index]
                    val songId = song["id"] as? String ?: ""
                    val songTitle = song["title"] as? String ?: "Unknown"
                    val songImage = song["image"] as? String ?: ""
                    val songArtist = song["artist"] as? String ?: ""
                    
                    YouTubeListItem(
                        item = SongItem(
                            id = songId,
                            title = songTitle,
                            artists = listOf(com.musifly.innertube.models.Artist(name = songArtist, id = null)),
                            album = null,
                            duration = (song["duration"] as? Int) ?: 0,
                            thumbnail = songImage,
                            explicit = false
                        ),
                        isActive = songId == mediaMetadata?.id || "saavn:$songId" == mediaMetadata?.id,
                        isPlaying = isPlaying && (songId == mediaMetadata?.id || "saavn:$songId" == mediaMetadata?.id),
                        trailingContent = {
                            AppIconButton(
                                onClick = {
                                    menuState.show {
                                        SaavnSongMenu(
                                            mediaMetadata = song.toMediaMetadataFromSaavnMap(),
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                            saavnAPI = viewModel.saavnAPI
                                        )
                                    }
                                },
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier
                            .combinedClickable(
                                onClick = {
                                    if (songId == mediaMetadata?.id || "saavn:$songId" == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        val mediaItems = topSongs.map { it.toMediaMetadataFromSaavnMap().toMediaItem() }
                                        playerConnection.playQueue(
                                            com.musifly.android.playback.queues.ListQueue(
                                                items = mediaItems,
                                                title = "$artistName - Top Songs",
                                                startIndex = index
                                            )
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        SaavnSongMenu(
                                            mediaMetadata = song.toMediaMetadataFromSaavnMap(),
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                            saavnAPI = viewModel.saavnAPI
                                        )
                                    }
                                }
                            )
                            .animateItem()
                    )
                }
            }
            
            // Top Albums
            if (topAlbums.isNotEmpty()) {
                item {
                    NavigationTitle(title = "Top Albums")
                }
                
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(topAlbums.size) { index ->
                            val album = topAlbums[index]
                            val albumId = album["id"] as? String ?: ""
                            val albumTitle = album["title"] as? String ?: "Unknown"
                            val albumImage = album["image"] as? String ?: ""
                            
                            YouTubeGridItem(
                                item = AlbumItem(
                                    browseId = "saavn:$albumId",
                                    playlistId = "saavn:$albumId",
                                    id = "saavn:$albumId",
                                    title = albumTitle,
                                    artists = emptyList(),
                                    year = null,
                                    thumbnail = albumImage,
                                    explicit = false
                                ),
                                isActive = false,
                                isPlaying = false,
                                coroutineScope = coroutineScope,
                                modifier = Modifier.combinedClickable(
                                    onClick = {
                                        navController.navigate("album/saavn:$albumId")
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
        // ============ YouTube Artist ============
        else {
            if (artistPage != null) {
                item(key = "header") {
                    Column {
                        // Hero Header con imagen de fondo desvanecida
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                        ) {
                            // Imagen de fondo con desvanecimiento
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                AsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(artistPage.artist.thumbnail.resize(1200, 1000))
                                        .allowHardware(false)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Transparent,
                                                    MaterialTheme.colorScheme.surface
                                                ),
                                                startY = 0f,
                                                endY = 1100f
                                            )
                                        )
                                )
                            }

                            // Gradiente adicional para mejor legibilidad
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.2f),
                                                Color.Black.copy(alpha = 0.4f),
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                MaterialTheme.colorScheme.surface
                                            ),
                                            startY = 0f,
                                            endY = 1400f
                                        )
                                    )
                            )

                            // Contenido sobre la imagen
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 20.dp, vertical = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar circular del artista
                                Surface(
                                    shape = CircleShape,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.size(110.dp)
                                ) {
                                    AsyncImage(
                                        model = coil.request.ImageRequest.Builder(context)
                                            .data(artistPage.artist.thumbnail.resize(220, 220))
                                            .allowHardware(false)
                                            .build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Nombre del artista con sombra sutil
                                Text(
                                    text = artistPage.artist.title,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Barra de acciones principales
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // Botón Suscribirse/Suscrito
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val bookmarksRepo = dagger.hilt.android.EntryPointAccessors.fromApplication(
                                                context.applicationContext,
                                                com.musifly.android.di.BookmarksRepositoryEntryPoint::class.java
                                            ).bookmarksRepository()
                                            
                                            val artist = libraryArtist?.artist
                                            if (artist != null) {
                                                bookmarksRepo.toggleArtistBookmark(artist)
                                            } else {
                                                artistPage.artist.let {
                                                    bookmarksRepo.toggleArtistBookmark(
                                                        ArtistEntity(
                                                            id = it.id,
                                                            name = it.title,
                                                            channelId = it.channelId,
                                                            thumbnailUrl = it.thumbnail,
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (libraryArtist?.artist?.bookmarkedAt != null)
                                            MaterialTheme.colorScheme.secondaryContainer
                                        else
                                            MaterialTheme.colorScheme.primary,
                                        contentColor = if (libraryArtist?.artist?.bookmarkedAt != null)
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        else
                                            MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (libraryArtist?.artist?.bookmarkedAt != null)
                                                R.drawable.library_add_check
                                            else
                                                R.drawable.library_add
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (libraryArtist?.artist?.bookmarkedAt != null)
                                            stringResource(R.string.subscribed)
                                        else
                                            stringResource(R.string.subscribe),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(Modifier.weight(1f))

                                // Botón Shuffle
                                artistPage.artist.shuffleEndpoint?.let { shuffleEndpoint ->
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        AppIconButton(
                                            onClick = {
                                                playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                                            },
                                            painter = painterResource(R.drawable.shuffle),
                                            contentDescription = stringResource(R.string.shuffle),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            size = 44.dp
                                        )
                                    }
                                }

                                // Botón Compartir
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    AppIconButton(
                                        onClick = {
                                            artistPage.artist.shareLink?.let { link ->
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Artist Link", link)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(
                                                    context,
                                                    R.string.link_copied,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        painter = painterResource(R.drawable.link),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        size = 44.dp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Canciones de la biblioteca
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
                                AppIconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    WatchEndpoint(videoId = song.id),
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

                // Secciones del artista
                artistPage.sections.fastForEach { section ->
                    if (section.items.isNotEmpty()) {
                        item {
                            NavigationTitle(
                                title = section.title,
                                onClick = section.moreEndpoint?.let {
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
                                    AppIconButton(
                                        onClick = {
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                },
                                modifier = Modifier
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
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    items = section.items,
                                    key = { it.id },
                                ) { item ->
                                    YouTubeGridItem(
                                        item = item,
                                        isActive = when (item) {
                                            is SongItem -> mediaMetadata?.id == item.id
                                            is AlbumItem -> mediaMetadata?.album?.id == item.id
                                            else -> false
                                        },
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier = Modifier
                                            .combinedClickable(
                                                onClick = {
                                                    when (item) {
                                                        is SongItem -> playerConnection.playQueue(
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
                                                                    Log.w("ArtistScreen", "Album ID is empty")
                                                                    Toast.makeText(
                                                                        context,
                                                                        "Error: Invalid album ID",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                }
                                                            } catch (e: Exception) {
                                                                Log.e("ArtistScreen", "Navigation error to album: ${item.id}", e)
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
                                                                    Log.w("ArtistScreen", "Artist ID is empty")
                                                                }
                                                            } catch (e: Exception) {
                                                                Log.e("ArtistScreen", "Navigation error to artist: ${item.id}", e)
                                                            }
                                                        }
                                                        is PlaylistItem -> {
                                                            try {
                                                                if (item.id.isNotEmpty()) {
                                                                    navController.navigate("online_playlist/${item.id}")
                                                                } else {
                                                                    Log.w("ArtistScreen", "Playlist ID is empty")
                                                                }
                                                            } catch (e: Exception) {
                                                                Log.e("ArtistScreen", "Navigation error to playlist: ${item.id}", e)
                                                            }
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        when (item) {
                                                            is SongItem -> YouTubeSongMenu(
                                                                song = item,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                            is AlbumItem -> YouTubeAlbumMenu(
                                                                albumItem = item,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                            is ArtistItem -> YouTubeArtistMenu(
                                                                artist = item,
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                            is PlaylistItem -> YouTubePlaylistMenu(
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
                // Estado de carga
                item(key = "shimmer") {
                    ShimmerHost {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3),
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .shimmer()
                                    .background(MaterialTheme.colorScheme.onSurface)
                                    .fadingEdge(
                                        top = WindowInsets.systemBars
                                            .asPaddingValues()
                                            .calculateTopPadding() + AppBarHeight,
                                        bottom = 108.dp,
                                    ),
                            )
                            TextPlaceholder(
                                height = 56.dp,
                                modifier = Modifier
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
        } // End else (YouTube artist)
    }

    // TopAppBar con transición suave
    TopAppBar(
        title = {
            if (!transparentAppBar) {
                Text(
                    text = artistPage?.artist?.title.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            AppIconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = null,
            )
        },
        colors = if (transparentAppBar) {
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        } else {
            TopAppBarDefaults.topAppBarColors()
        },
        scrollBehavior = scrollBehavior
    )
    }
}