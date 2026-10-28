package com.musifly.android.ui.screens.playlist

import com.musifly.android.ui.component.liquidglass.LiquidHeader
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.layerBackdrop
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import androidx.compose.ui.graphics.Brush

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.musifly.innertube.models.SongItem
import com.musifly.innertube.models.WatchEndpoint
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.AlbumThumbnailSize
import com.musifly.android.constants.HideExplicitKey
import com.musifly.android.constants.ThumbnailCornerRadius
import com.musifly.android.db.entities.PlaylistEntity
import com.musifly.android.db.entities.PlaylistSongMap
import com.musifly.android.extensions.metadata
import com.musifly.android.extensions.toMediaItem
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.models.toMediaMetadataFromSaavnMap
import com.musifly.android.models.MediaMetadata
import com.musifly.android.playback.queues.YouTubeQueue
import com.musifly.android.playback.queues.ListQueue
import com.musifly.android.ui.component.MediaMetadataListItem
import com.musifly.android.ui.component.AutoResizeText
import com.musifly.android.ui.component.FontSizeRange
import com.musifly.android.ui.component.LocalMenuState

import com.musifly.android.ui.component.YouTubeListItem
import com.musifly.android.ui.component.shimmer.ButtonPlaceholder
import com.musifly.android.ui.component.shimmer.ListItemPlaceHolder
import com.musifly.android.ui.component.shimmer.ShimmerHost
import com.musifly.android.ui.component.shimmer.TextPlaceholder
import com.musifly.android.ui.menu.SelectionMediaMetadataMenu
import com.musifly.android.ui.menu.SaavnSongMenu
import com.musifly.android.ui.menu.YouTubePlaylistMenu
import com.musifly.android.ui.menu.YouTubeSongMenu
import com.musifly.android.ui.utils.ItemWrapper
import com.musifly.android.utils.rememberPreference
import com.musifly.android.viewmodels.OnlinePlaylistViewModel

import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TopAppBarDefaults
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val context = LocalContext.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    
    // JioSaavn playlist data
    val saavnPlaylistData by viewModel.saavnPlaylistData.collectAsState()
    val saavnPlaylistSongs by viewModel.saavnPlaylistSongs.collectAsState()
    
    // Determine if we're showing a JioSaavn playlist
    val isSaavnPlaylist = saavnPlaylistData != null
    // Get playlist image URL to use as fallback for songs without individual images
    val playlistImageUrl = saavnPlaylistData?.get("image") as? String
    val saavnSongsMetadata = remember(saavnPlaylistSongs, playlistImageUrl) {
        saavnPlaylistSongs.map { songMap ->
            songMap.toMediaMetadataFromSaavnMap(albumImageUrl = playlistImageUrl)
        }
    }
    val wrappedSaavnSongs = remember(saavnSongsMetadata) {
        saavnSongsMetadata.map { metadata -> ItemWrapper(metadata) }.toMutableList()
    }

    var selection by remember {
        mutableStateOf(false)
    }
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    
    val filteredSaavnSongs = remember(saavnSongsMetadata, query) {
        if (query.text.isEmpty()) {
            saavnSongsMetadata.mapIndexed { index, metadata -> index to metadata }
        } else {
            saavnSongsMetadata
                .mapIndexed { index, metadata -> index to metadata }
                .filter { (_, metadata) ->
                    metadata.title.contains(query.text, ignoreCase = true) ||
                    metadata.artists.any { it.name.contains(query.text, ignoreCase = true) }
                }
        }
    }
    val filteredSongs =
        remember(songs, query) {
            if (query.text.isEmpty()) {
                songs.mapIndexed { index, song -> index to song }
            } else {
                songs
                    .mapIndexed { index, song -> index to song }
                    .filter { (_, song) ->
                        song.title.contains(query.text, ignoreCase = true) ||
                                song.artists.fastAny {
                                    it.name.contains(
                                        query.text,
                                        ignoreCase = true
                                    )
                                }
                    }
            }
        }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    }








    LaunchedEffect(playlist?.id) {
        // Restaurar posición si cambias de playlist
        if (playlist?.id != null) {
            lazyListState.scrollToItem(0)
        }
    }

    val showTopBarTitle by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0
        }
    }

    // Calcular el padding del contenido considerando WindowInsets
    val contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
        .asPaddingValues()
    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { item -> ItemWrapper(item) }.toMutableList()
    }
    
    val backdrop = rememberLayerBackdrop()



    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isLiquidGlassActive()) Modifier.layerBackdrop(backdrop) else Modifier)
        ) {
            // BACKGROUND - Support for both themes
            if (isLiquidGlassActive()) {
                val isLightTheme = !androidx.compose.foundation.isSystemInDarkTheme()
                val gradientColors = if (isLightTheme) {
                    listOf(
                        Color(0xFFF5F5F5),
                        Color(0xFFE8E8E8),
                        Color(0xFFDADADA)
                    )
                } else {
                    listOf(
                        Color(0xFF0A0A0A),
                        Color(0xFF121212),
                        Color(0xFF1A1A1A),
                        Color(0xFF0F0A1E)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = Brush.verticalGradient(gradientColors))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                )
            }

            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().let {
                    PaddingValues(
                        start = it.calculateStartPadding(LayoutDirection.Ltr).coerceAtLeast(0.dp),
                        end = it.calculateEndPadding(LayoutDirection.Ltr).coerceAtLeast(0.dp),
                        top = (it.calculateTopPadding() + 64.dp).coerceAtLeast(0.dp),
                        bottom = it.calculateBottomPadding().coerceAtLeast(0.dp)
                    )
                },
            ) {
                // Check if we should display JioSaavn playlist
                if (isSaavnPlaylist && saavnPlaylistData != null && saavnSongsMetadata.isNotEmpty()) {
                    // Display JioSaavn Playlist UI (same structure as YouTube playlist but with different data)
                    val playlistTitle = saavnPlaylistData?.get("title") as? String ?: "Unknown Playlist"
                    val playlistImage = saavnPlaylistData?.get("image") as? String ?: ""
                    val playlistCreator = saavnPlaylistData?.get("creator") as? String ?: "JioSaavn"
                    val songCount = saavnPlaylistSongs.size
                    
                    if (!isSearching) {
                        item {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .animateItem(),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(AlbumThumbnailSize)
                                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                            .fillMaxWidth(),
                                    ) {
                                        AsyncImage(
                                            model = coil.request.ImageRequest.Builder(context)
                                                .data(playlistImage)
                                                .allowHardware(false)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                                            placeholder = painterResource(R.drawable.music_note),
                                            error = painterResource(R.drawable.music_note)
                                        )
                                    }
                                    
                                    Spacer(Modifier.width(16.dp))
                                    
                                    Column(
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        AutoResizeText(
                                            text = playlistTitle,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSizeRange = FontSizeRange(16.sp, 22.sp),
                                        )
                                        
                                        Text(
                                            text = playlistCreator,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Normal,
                                        )
                                        
                                        Text(
                                            text = "$songCount songs",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Normal,
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(12.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Button(
                                            onClick = {
                                                val mediaItems = saavnSongsMetadata.map { it.toMediaItem() }
                                                playerConnection.playQueue(
                                                    ListQueue(items = mediaItems, title = playlistTitle)
                                                )
                                            },
                                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
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
                                        
                                        OutlinedButton(
                                            onClick = {
                                                val mediaItems = saavnSongsMetadata.shuffled().map { it.toMediaItem() }
                                                playerConnection.playQueue(
                                                    ListQueue(items = mediaItems, title = "$playlistTitle (Shuffled)")
                                                )
                                            },
                                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
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
                    }
                    
                    // Display JioSaavn Songs
                    items(
                        items = filteredSaavnSongs,
                        key = { (_, metadata) -> metadata.id },
                    ) { (index, metadata) ->
                        val metadataWrapper = wrappedSaavnSongs.find { it.item.id == metadata.id }
                        MediaMetadataListItem(
                            mediaMetadata = metadata,
                            isActive = metadata.id == mediaMetadata?.id,
                            isPlaying = isPlaying && metadata.id == mediaMetadata?.id,
                            isSelected = metadataWrapper?.isSelected == true && selection,
                            trailingContent = {
                                IconButton(
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
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
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
                                                    ListQueue(items = mediaItems, title = playlistTitle, startIndex = index)
                                                )
                                            }
                                        } else {
                                            metadataWrapper?.isSelected = !(metadataWrapper?.isSelected ?: false)
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                        }
                                        wrappedSaavnSongs.forEach { it.isSelected = false }
                                        metadataWrapper?.isSelected = true
                                    },
                                )
                                .alpha(if (hideExplicit && metadata.explicit) 0.3f else 1f)
                                .animateItem(),
                        )
                    }
                } else {
                    playlist.let { playlist ->
                        if (playlist != null) {
                            if (!isSearching) {
                            item {
                                Column(
                                    modifier =
                                        Modifier
                                            .padding(12.dp)
                                            .animateItem(),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(AlbumThumbnailSize)
                                                .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                                .fillMaxWidth(),
                                        ) {
                                            AsyncImage(
                                                model = coil.request.ImageRequest.Builder(context)
                                                    .data(playlist.thumbnail)
                                                    .allowHardware(false)
                                                    .build(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                                            )
                                        }

                                        Spacer(Modifier.width(16.dp))

                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            AutoResizeText(
                                                text = playlist.title,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSizeRange = FontSizeRange(16.sp, 22.sp),
                                            )

                                            playlist.author?.let { artist ->
                                                Text(
                                                    buildAnnotatedString {
                                                        withStyle(
                                                            style =
                                                                MaterialTheme.typography.titleMedium
                                                                    .copy(
                                                                        fontWeight = FontWeight.Normal,
                                                                        color = MaterialTheme.colorScheme.onBackground,
                                                                    ).toSpanStyle(),
                                                        ) {
                                                            if (artist.id != null) {
                                                                val link =
                                                                    LinkAnnotation.Clickable(artist.id!!) {
                                                                        navController.navigate("artist/${artist.id!!}")
                                                                    }
                                                                withLink(link) {
                                                                    append(artist.name)
                                                                }
                                                            } else {
                                                                append(artist.name)
                                                            }
                                                        }
                                                    },
                                                )
                                            }

                                            playlist.songCountText?.let { songCountText ->
                                                Text(
                                                    text = songCountText,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Normal,
                                                )
                                            }

                                            Row {
                                                if (playlist.id != "LM") {
                                                    IconButton(
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                val bookmarksRepo = dagger.hilt.android.EntryPointAccessors.fromApplication(
                                                                    context.applicationContext,
                                                                    com.musifly.android.di.BookmarksRepositoryEntryPoint::class.java
                                                                ).bookmarksRepository()

                                                                if (dbPlaylist?.playlist == null) {
                                                                    // Create playlist entity
                                                                    val entity = PlaylistEntity(
                                                                        name = playlist.title,
                                                                        browseId = playlist.id,
                                                                        isEditable = playlist.isEditable,
                                                                        playEndpointParams = playlist.playEndpoint?.params,
                                                                        shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                                                        radioEndpointParams = playlist.radioEndpoint?.params,
                                                                        bookmarkedAt = LocalDateTime.now()
                                                                    )
                                                                    
                                                                    // Insert in transaction
                                                                    database.transaction {
                                                                        insert(entity)
                                                                        songs.map(SongItem::toMediaMetadata)
                                                                            .onEach(::insert)
                                                                            .mapIndexed { index, song ->
                                                                                PlaylistSongMap(
                                                                                    songId = song.id,
                                                                                    playlistId = entity.id,
                                                                                    position = index
                                                                                )
                                                                            }
                                                                            .forEach(::insert)
                                                                    }
                                                                    
                                                                    // Sync to Supabase (outside transaction)
                                                                    // Use the entity we created (it has the ID)
                                                                    bookmarksRepo.togglePlaylistBookmark(entity)
                                                                } else {
                                                                    bookmarksRepo.togglePlaylistBookmark(dbPlaylist!!.playlist)
                                                                }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(
                                                                if (dbPlaylist?.playlist?.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border
                                                            ),
                                                            contentDescription = null,
                                                            tint = if (dbPlaylist?.playlist?.bookmarkedAt != null) MaterialTheme.colorScheme.error else LocalContentColor.current
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            YouTubePlaylistMenu(
                                                                playlist = playlist,
                                                                songs = songs,
                                                                coroutineScope = coroutineScope,
                                                                onDismiss = menuState::dismiss,
                                                                selectAction = { selection = true },
                                                                canSelect = true,
                                                            )
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.more_vert),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                                                Button(
                                                    onClick = {
                                                        playerConnection.playQueue(
                                                            YouTubeQueue(
                                                                shuffleEndpoint
                                                            )
                                                        )
                                                    },
                                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.shuffle),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                                    )
                                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                                    Text(stringResource(R.string.shuffle))
                                                }
                                        }

                                        playlist.radioEndpoint?.let { radioEndpoint ->
                                                OutlinedButton(
                                                    onClick = {
                                                        playerConnection.playQueue(
                                                            YouTubeQueue(
                                                                radioEndpoint
                                                            )
                                                        )
                                                    },
                                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                                    modifier = Modifier.weight(1f),
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.radio),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(ButtonDefaults.IconSize),
                                                    )
                                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                                    Text(stringResource(R.string.radio))
                                                }
                                        }
                                    }
                                }
                            }
                        }



                        items(
                            items = wrappedSongs,
                        ) { song ->
                            YouTubeListItem(
                                item = song.item.second,
                                isActive = mediaMetadata?.id == song.item.second.id,
                                isPlaying = isPlaying,
                                isSelected = song.isSelected && selection,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song.item.second,
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
                                            enabled = !hideExplicit || !song.item.second.explicit,
                                            onClick = {
                                                if (!selection) {
                                                    if (song.item.second.id == mediaMetadata?.id) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        playerConnection.service.getAutomix(
                                                            playlistId = playlist!!.id
                                                        )
                                                        playerConnection.playQueue(
                                                            YouTubeQueue(
                                                                song.item.second.endpoint
                                                                    ?: WatchEndpoint(
                                                                        videoId =
                                                                            song.item.second
                                                                                .id,
                                                                    ),
                                                                song.item.second.toMediaMetadata(),
                                                            ),
                                                        )
                                                    }
                                                } else {
                                                    song.isSelected = !song.isSelected
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (!selection) {
                                                    selection = true
                                                }
                                                wrappedSongs.forEach { it.isSelected = false }
                                                song.isSelected = true
                                            },
                                        )
                                        .alpha(if (hideExplicit && song.item.second.explicit) 0.3f else 1f)
                                        .animateItem(),
                            )
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
        }
    }

        if (isLiquidGlassActive()) {
            LiquidHeader(
                navController = navController,
                backdrop = backdrop,
                title = null,
                titleContent = {
                    if (selection) {
                        val count = if (isSaavnPlaylist) wrappedSaavnSongs.count { it.isSelected } else wrappedSongs.count { it.isSelected }
                        Text(
                            text = pluralStringResource(R.plurals.n_song, count, count),
                            style = MaterialTheme.typography.titleLarge
                        )
                    } else if (isSearching) {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    } else if (showTopBarTitle) {
                        Text(
                            if (isSaavnPlaylist) {
                                (saavnPlaylistData?.get("title") as? String) ?: "Unknown Playlist"
                            } else {
                                playlist?.title.orEmpty()
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
                            if (isSearching) {
                                isSearching = false
                                query = TextFieldValue()
                            } else if (selection) {
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
                        val count = if (isSaavnPlaylist) wrappedSaavnSongs.count { it.isSelected } else wrappedSongs.count { it.isSelected }
                        val currentWrappedSongs = if (isSaavnPlaylist) wrappedSaavnSongs else wrappedSongs
                        IconButton(
                            onClick = {
                                if (count == currentWrappedSongs.size) {
                                    currentWrappedSongs.forEach { it.isSelected = false }
                                } else {
                                    currentWrappedSongs.forEach { it.isSelected = true }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (count == currentWrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                                ),
                                contentDescription = null
                            )
                        }
                        IconButton(
                            onClick = {
                                menuState.show {
                                    if (isSaavnPlaylist) {
                                        SelectionMediaMetadataMenu(
                                            songSelection = wrappedSaavnSongs.filter { it.isSelected }.map { it.item },
                                            onDismiss = menuState::dismiss,
                                            clearAction = { selection = false },
                                            currentItems = emptyList()
                                        )
                                    } else {
                                        SelectionMediaMetadataMenu(
                                            songSelection = wrappedSongs.filter { it.isSelected }
                                                .map { it.item.second.toMediaItem().metadata!! },
                                            onDismiss = menuState::dismiss,
                                            clearAction = { selection = false },
                                            currentItems = emptyList()
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
                    } else {
                        if (!isSearching) {
                            IconButton(
                                onClick = { isSearching = true }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.search),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            )
        } else {
            TopAppBar(
                title = {
                    if (selection) {
                        val count = if (isSaavnPlaylist) wrappedSaavnSongs.count { it.isSelected } else wrappedSongs.count { it.isSelected }
                        Text(
                            text = pluralStringResource(R.plurals.n_song, count, count),
                            style = MaterialTheme.typography.titleLarge
                        )
                    } else if (isSearching) {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                    } else if (showTopBarTitle) {
                        Text(
                            if (isSaavnPlaylist) {
                                (saavnPlaylistData?.get("title") as? String) ?: "Unknown Playlist"
                            } else {
                                playlist?.title.orEmpty()
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
                            if (isSearching) {
                                isSearching = false
                                query = TextFieldValue()
                            } else if (selection) {
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
                        val count = if (isSaavnPlaylist) wrappedSaavnSongs.count { it.isSelected } else wrappedSongs.count { it.isSelected }
                        val currentWrappedSongs = if (isSaavnPlaylist) wrappedSaavnSongs else wrappedSongs
                        IconButton(
                            onClick = {
                                if (count == currentWrappedSongs.size) {
                                    currentWrappedSongs.forEach { it.isSelected = false }
                                } else {
                                    currentWrappedSongs.forEach { it.isSelected = true }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (count == currentWrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                                ),
                                contentDescription = null
                            )
                        }
                        IconButton(
                            onClick = {
                                menuState.show {
                                    if (isSaavnPlaylist) {
                                        SelectionMediaMetadataMenu(
                                            songSelection = wrappedSaavnSongs.filter { it.isSelected }.map { it.item },
                                            onDismiss = menuState::dismiss,
                                            clearAction = { selection = false },
                                            currentItems = emptyList()
                                        )
                                    } else {
                                        SelectionMediaMetadataMenu(
                                            songSelection = wrappedSongs.filter { it.isSelected }
                                                .map { it.item.second.toMediaItem().metadata!! },
                                            onDismiss = menuState::dismiss,
                                            clearAction = { selection = false },
                                            currentItems = emptyList()
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
                    } else {
                        if (!isSearching) {
                            IconButton(
                                onClick = { isSearching = true }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.search),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                .align(Alignment.BottomCenter),
        )
    }
}
