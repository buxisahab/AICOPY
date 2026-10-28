package com.musifly.android.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import com.musifly.android.ui.component.liquidglass.LiquidHeader
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.layerBackdrop
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import androidx.compose.ui.graphics.Brush

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalDownloadUtil
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.AlbumThumbnailSize
import com.musifly.android.constants.ThumbnailCornerRadius
import com.musifly.android.db.entities.Album
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.playback.ExoDownloadService
import com.musifly.android.playback.queues.LocalAlbumRadio
import com.musifly.android.ui.component.AutoResizeText
import com.musifly.android.ui.component.FontSizeRange
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.component.NavigationTitle
import com.musifly.android.ui.component.SongListItem
import com.musifly.android.ui.component.YouTubeGridItem
import com.musifly.android.ui.component.MediaMetadataListItem
import com.musifly.android.models.toMediaMetadataFromSaavnMap
import com.musifly.android.models.MediaMetadata
import com.musifly.android.playback.queues.ListQueue
import com.musifly.android.extensions.toMediaItem
import com.musifly.android.ui.component.shimmer.ButtonPlaceholder
import com.musifly.android.ui.component.shimmer.ListItemPlaceHolder
import com.musifly.android.ui.component.shimmer.ShimmerHost
import com.musifly.android.ui.component.shimmer.TextPlaceholder
import com.musifly.android.ui.menu.AlbumMenu
import com.musifly.android.ui.menu.SelectionSongMenu
import com.musifly.android.ui.menu.SelectionMediaMetadataMenu
import com.musifly.android.ui.menu.SaavnSongMenu
import com.musifly.android.ui.menu.SongMenu
import com.musifly.android.ui.menu.YouTubeAlbumMenu
import com.musifly.android.ui.utils.ItemWrapper
import com.musifly.android.viewmodels.AlbumViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    val backdrop = rememberLayerBackdrop()
    
    val showTopBarTitle by remember {
        androidx.compose.runtime.derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    
    // JioSaavn album data
    val saavnAlbumData by viewModel.saavnAlbumData.collectAsState()
    val saavnAlbumSongs by viewModel.saavnAlbumSongs.collectAsState()
    
    // Determine if we're showing a JioSaavn album
    val isSaavnAlbum = saavnAlbumData != null
    // Get album image URL to use as fallback for songs without individual images
    val albumImageUrl = saavnAlbumData?.get("image") as? String
    val saavnSongsMetadata = remember(saavnAlbumSongs, albumImageUrl) {
        saavnAlbumSongs.map { songMap ->
            songMap.toMediaMetadataFromSaavnMap(albumImageUrl = albumImageUrl)
        }
    }
    val wrappedSaavnSongs = remember(saavnSongsMetadata) {
        saavnSongsMetadata.map { metadata -> ItemWrapper(metadata) }.toMutableList()
    }

    val wrappedSongs = remember(albumWithSongs) {
        albumWithSongs?.songs?.map { item -> ItemWrapper(item) }?.toMutableList()
    }

    var selection by remember {
        mutableStateOf(false)
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    // Capture the state value in a local variable to enable smart casting
    val currentAlbumWithSongs = albumWithSongs

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Forced default theme appearance for AlbumScreen
        ) {
            // BACKGROUND - Default surface color
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            )

        LazyColumn(
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().let {
                PaddingValues(
                    start = it.calculateStartPadding(LayoutDirection.Ltr),
                    end = it.calculateEndPadding(LayoutDirection.Ltr),
                    top = it.calculateTopPadding() + 64.dp,
                    bottom = it.calculateBottomPadding()
                )
            },
            state = lazyListState
        ) {
            // Check if we should display JioSaavn album
            if (isSaavnAlbum && saavnAlbumData != null && saavnSongsMetadata.isNotEmpty()) {
                // Display JioSaavn Album
                val albumTitle = saavnAlbumData?.get("title") as? String ?: "Unknown Album"
                val albumArtist = saavnAlbumData?.get("artist") as? String ?: "Unknown Artist"
                val albumImage = saavnAlbumData?.get("image") as? String ?: ""
                val albumYear = saavnAlbumData?.get("year") as? String ?: ""
                
                item {
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(AlbumThumbnailSize)
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(albumImage.takeIf { it.isNotEmpty() })
                                        .allowHardware(false)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(R.drawable.music_note),
                                    error = painterResource(R.drawable.music_note)
                                )
                            }
                            
                            Spacer(Modifier.width(16.dp))
                            
                            Column(
                                verticalArrangement = Arrangement.Center,
                            ) {
                                AutoResizeText(
                                    text = albumTitle,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSizeRange = FontSizeRange(16.sp, 22.sp),
                                )
                                
                                Text(
                                    text = albumArtist,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                )
                                
                                if (albumYear.isNotEmpty()) {
                                    Text(
                                        text = albumYear,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Normal,
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppButton(
                                onClick = {
                                    val mediaItems = saavnSongsMetadata.map { it.toMediaItem() }
                                    playerConnection.playQueue(
                                        ListQueue(items = mediaItems, title = albumTitle)
                                    )
                                },
                                isPrimary = true,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(text = stringResource(R.string.play))
                            }

                            AppButton(
                                onClick = {
                                    val mediaItems = saavnSongsMetadata.shuffled().map { it.toMediaItem() }
                                    playerConnection.playQueue(
                                        ListQueue(items = mediaItems, title = "$albumTitle (Shuffled)")
                                    )
                                },
                                isPrimary = false,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.shuffle))
                            }
                        }
                    }
                }
                
                // Display JioSaavn Songs
                itemsIndexed(
                    items = wrappedSaavnSongs,
                    key = { _, metadata -> metadata.item.id },
                ) { index, metadataWrapper ->
                    val metadata = metadataWrapper.item
                    MediaMetadataListItem(
                        mediaMetadata = metadata,
                        isActive = metadata.id == mediaMetadata?.id,
                        isPlaying = isPlaying && metadata.id == mediaMetadata?.id,
                        isSelected = metadataWrapper.isSelected && selection,
                        trailingContent = {
                            AppIconButton(
                                onClick = {
                                    menuState.show {
                                        SaavnSongMenu(
                                            mediaMetadata = metadata,
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
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!selection) {
                                        if (metadata.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            val mediaItems = saavnSongsMetadata.map { it.toMediaItem() }
                                            playerConnection.playQueue(
                                                ListQueue(items = mediaItems, title = albumTitle, startIndex = index)
                                            )
                                        }
                                    } else {
                                        metadataWrapper.isSelected = !metadataWrapper.isSelected
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!selection) {
                                        selection = true
                                    }
                                    wrappedSaavnSongs.forEach { it.isSelected = false }
                                    metadataWrapper.isSelected = true
                                },
                            ),
                    )
                }
            } else if (currentAlbumWithSongs != null && currentAlbumWithSongs.songs.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(AlbumThumbnailSize)
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                            ) {
                                // Mostrar la imagen del álbum
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(currentAlbumWithSongs.album.thumbnailUrl)
                                        .allowHardware(false)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )


                            }


                            Spacer(Modifier.width(16.dp))

                            Column(
                                verticalArrangement = Arrangement.Center,
                            ) {
                                AutoResizeText(
                                    text = currentAlbumWithSongs.album.title,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSizeRange = FontSizeRange(16.sp, 22.sp),
                                )

                                val annotatedString =
                                    buildAnnotatedString {
                                        withStyle(
                                            style =
                                                MaterialTheme.typography.titleMedium
                                                    .copy(
                                                        fontWeight = FontWeight.Normal,
                                                        color = MaterialTheme.colorScheme.onBackground,
                                                    ).toSpanStyle(),
                                        ) {
                                            currentAlbumWithSongs.artists.fastForEachIndexed { index, artist ->
                                                pushStringAnnotation(artist.id, artist.name)
                                                append(artist.name)
                                                pop()
                                                if (index != currentAlbumWithSongs.artists.lastIndex) {
                                                    append(", ")
                                                }
                                            }
                                        }
                                    }
                                ClickableText(annotatedString) { offset ->
                                    annotatedString.getStringAnnotations(offset, offset).firstOrNull()
                                        ?.let { range ->
                                            navController.navigate("artist/${range.tag}")
                                        }
                                }

                                if (currentAlbumWithSongs.album.year != null) {
                                    Text(
                                        text = currentAlbumWithSongs.album.year.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Normal,
                                    )
                                }

                                Row {
                                    AppIconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val bookmarksRepo = dagger.hilt.android.EntryPointAccessors.fromApplication(
                                                    context.applicationContext,
                                                    com.musifly.android.di.BookmarksRepositoryEntryPoint::class.java
                                                ).bookmarksRepository()
                                                
                                                bookmarksRepo.toggleAlbumBookmark(
                                                    album = currentAlbumWithSongs.album,
                                                    artists = currentAlbumWithSongs.artists
                                                )
                                            }
                                        },
                                        painter = painterResource(
                                            if (currentAlbumWithSongs.album.bookmarkedAt != null) {
                                                R.drawable.favorite
                                            } else {
                                                R.drawable.favorite_border
                                            }
                                        ),
                                        contentDescription = null,
                                        tint = if (currentAlbumWithSongs.album.bookmarkedAt != null) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            LocalContentColor.current
                                        }
                                    )

                                    when (downloadState) {
                                        Download.STATE_COMPLETED -> {
                                            AppIconButton(
                                                onClick = {
                                                    currentAlbumWithSongs.songs.forEach { song ->
                                                        DownloadService.sendRemoveDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            song.id,
                                                            false,
                                                        )
                                                    }
                                                },
                                                painter = painterResource(R.drawable.offline),
                                                contentDescription = null,
                                            )
                                        }

                                        Download.STATE_DOWNLOADING -> {
                                            IconButton(
                                                onClick = {
                                                    currentAlbumWithSongs.songs.forEach { song ->
                                                        DownloadService.sendRemoveDownload(
                                                            context,
                                                            ExoDownloadService::class.java,
                                                            song.id,
                                                            false,
                                                        )
                                                    }
                                                },
                                            ) {
                                                CircularProgressIndicator(
                                                    strokeWidth = 2.dp,
                                                    modifier = Modifier.size(24.dp),
                                                )
                                            }
                                        }

                                        else -> {
                                            AppIconButton(
                                                onClick = {
                                                    // Save all songs to database first before downloading
                                                    // This ensures they appear in the cached downloads screen
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        currentAlbumWithSongs.songs.forEach { song ->
                                                            // Ensure song is in database
                                                            database.transaction {
                                                                upsert(song.song)
                                                            }
                                                            
                                                            val downloadRequest =
                                                                DownloadRequest
                                                                    .Builder(song.id, song.id.toUri())
                                                                    .setCustomCacheKey(song.id)
                                                                    .setData(song.song.title.toByteArray())
                                                                    .build()
                                                            
                                                            // Start download on main thread
                                                            withContext(Dispatchers.Main) {
                                                                DownloadService.sendAddDownload(
                                                                    context,
                                                                    ExoDownloadService::class.java,
                                                                    downloadRequest,
                                                                    false,
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                painter = painterResource(R.drawable.download),
                                                contentDescription = null,
                                            )
                                        }
                                    }

                                    AppIconButton(
                                        onClick = {
                                            menuState.show {
                                                AlbumMenu(
                                                    originalAlbum = Album(
                                                        currentAlbumWithSongs.album,
                                                        currentAlbumWithSongs.artists
                                                    ),
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppButton(
                                onClick = {
                                    playerConnection.service.getAutomix(playlistId)
                                    playerConnection.playQueue(
                                        LocalAlbumRadio(currentAlbumWithSongs),
                                    )
                                },
                                isPrimary = true,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(
                                    text = stringResource(R.string.play),
                                )
                            }

                            AppButton(
                                onClick = {
                                    playerConnection.service.getAutomix(playlistId)
                                    playerConnection.playQueue(
                                        LocalAlbumRadio(currentAlbumWithSongs.copy(songs = currentAlbumWithSongs.songs.shuffled())),
                                    )
                                },
                                isPrimary = false,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = null,
                                    modifier = Modifier.size(ButtonDefaults.IconSize),
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.shuffle))
                            }
                        }
                    }
                }

                if (wrappedSongs != null) {
                    itemsIndexed(
                        items = wrappedSongs,
                        key = { _, song -> song.item.id },
                    ) { index, songWrapper ->
                        SongListItem(
                            song = songWrapper.item,
                            albumIndex = index + 1,
                            isActive = songWrapper.item.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            trailingContent = {
                                AppIconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = songWrapper.item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                )
                            },
                            isSelected = songWrapper.isSelected && selection,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (!selection) {
                                                if (songWrapper.item.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    if (currentAlbumWithSongs != null) {
                                                        playerConnection.service.getAutomix(playlistId)
                                                        playerConnection.playQueue(
                                                            LocalAlbumRadio(
                                                                currentAlbumWithSongs,
                                                                startIndex = index
                                                            ),
                                                        )
                                                    }
                                                }
                                            } else {
                                                songWrapper.isSelected = !songWrapper.isSelected
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (!selection) {
                                                selection = true
                                            }
                                            wrappedSongs.forEach {
                                                it.isSelected = false
                                            } // Clear previous selections
                                            songWrapper.isSelected = true // Select the current item
                                        },
                                    ),
                        )
                    }
                }

                if (otherVersions.isNotEmpty()) {
                    item {
                        NavigationTitle(
                            title = stringResource(R.string.other_versions),
                        )
                    }
                    item {
                        LazyRow {
                            items(
                                items = otherVersions,
                                key = { it.id },
                            ) { item ->
                                YouTubeGridItem(
                                    item = item,
                                    isActive = mediaMetadata?.album?.id == item.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = scope,
                                    modifier =
                                        Modifier
                                            .combinedClickable(
                                                onClick = { navController.navigate("album/${item.id}") },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        YouTubeAlbumMenu(
                                                            albumItem = item,
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
                    }
                }
            } else {
                item {
                    ShimmerHost {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(
                                    modifier =
                                        Modifier
                                            .size(AlbumThumbnailSize)
                                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                            .background(MaterialTheme.colorScheme.onSurface),
                                )

                                Spacer(Modifier.width(16.dp))

                                Column(
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    TextPlaceholder()
                                    TextPlaceholder()
                                    TextPlaceholder()
                                }
                            }

                            Spacer(Modifier.padding(8.dp))

                            Row {
                                ButtonPlaceholder(Modifier.weight(1f))

                                Spacer(Modifier.width(12.dp))

                                ButtonPlaceholder(Modifier.weight(1f))
                            }
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
            title = {
                if (selection) {
                    val count = if (isSaavnAlbum) wrappedSaavnSongs.count { it.isSelected } else wrappedSongs?.count { it.isSelected } ?: 0
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else if (showTopBarTitle) {
                    Text(
                        text = if (isSaavnAlbum && saavnAlbumData != null) {
                            saavnAlbumData?.get("title") as? String ?: ""
                        } else {
                            albumWithSongs?.album?.title.orEmpty()
                        },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (selection) {
                            selection = false
                        } else {
                            navController.navigateUp()
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(
                            if (selection) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
                }
            },
            actions = {
                if (selection) {
                    val count = if (isSaavnAlbum) wrappedSaavnSongs.count { it.isSelected } else wrappedSongs?.count { it.isSelected } ?: 0
                    val totalCount = if (isSaavnAlbum) wrappedSaavnSongs.size else wrappedSongs?.size ?: 0
                    IconButton(
                        onClick = {
                            if (count == totalCount) {
                                if (isSaavnAlbum) wrappedSaavnSongs.forEach { it.isSelected = false }
                                else wrappedSongs?.forEach { it.isSelected = false }
                            } else {
                                if (isSaavnAlbum) wrappedSaavnSongs.forEach { it.isSelected = true }
                                else wrappedSongs?.forEach { it.isSelected = true }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == totalCount) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                    }

                    IconButton(
                        onClick = {
                            menuState.show {
                                if (isSaavnAlbum) {
                                    SelectionMediaMetadataMenu(
                                        songSelection = wrappedSaavnSongs.filter { it.isSelected }.map { it.item },
                                        currentItems = emptyList(),
                                        onDismiss = menuState::dismiss,
                                        clearAction = { selection = false }
                                    )
                                } else {
                                    SelectionSongMenu(
                                        songSelection = wrappedSongs?.filter { it.isSelected }?.map { it.item } ?: emptyList(),
                                        onDismiss = menuState::dismiss,
                                        clearAction = { selection = false }
                                    )
                                }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    if (isPrimary) {
        Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentPadding = contentPadding,
            content = content
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentPadding = contentPadding,
            content = content
        )
    }
}

@Composable
private fun AppIconButton(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    onLongClick: (() -> Unit)? = null
) {
    if (onLongClick != null) {
        com.musifly.android.ui.component.IconButton(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier.size(size),
            enabled = enabled
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(size),
            enabled = enabled
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}



suspend fun saveAlbumImageToGallery(context: Context, imageUrl: String, albumTitle: String) {
    try {
        // Cargar la imagen usando Coil
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .build()

        val drawable = context.imageLoader.execute(request).drawable

        if (drawable != null) {
            val bitmap = drawable.toBitmap()

            // Usar el título del álbum para el nombre del archivo
            val displayName = "${albumTitle.replace(" ", "_")}.png"
            val mimeType = "image/png" // Tipo MIME para PNG


            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                // Mostrar un mensaje de éxito
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Save Picture Success",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    } catch (e: IOException) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "X",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}