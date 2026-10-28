package com.musifly.android.ui.screens.library

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.R
import com.musifly.android.constants.CONTENT_TYPE_HEADER
import com.musifly.android.constants.CONTENT_TYPE_PLAYLIST
import com.musifly.android.constants.GridItemSize
import com.musifly.android.constants.GridItemsSizeKey
import com.musifly.android.constants.GridThumbnailHeight
import com.musifly.android.constants.InnerTubeCookieKey
import com.musifly.android.constants.LibraryViewType
import com.musifly.android.constants.PlaylistSortDescendingKey
import com.musifly.android.constants.PlaylistSortType
import com.musifly.android.constants.PlaylistSortTypeKey
import com.musifly.android.constants.PlaylistViewTypeKey
import com.musifly.android.constants.SongSortType
import com.musifly.android.constants.YtmSyncKey
import com.musifly.android.db.entities.Playlist
import com.musifly.android.db.entities.PlaylistEntity
import com.musifly.android.ui.component.BloomeeImportDialog
import com.musifly.android.ui.component.CreatePlaylistDialog
import com.musifly.android.ui.component.HideOnScrollFAB
import com.musifly.android.ui.component.LibraryPlaylistGridItem
import com.musifly.android.ui.component.LibraryPlaylistListItem
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.component.PlaylistGridItem
import com.musifly.android.ui.component.PlaylistListItem
import com.musifly.android.ui.component.SortHeader
import com.musifly.android.ui.component.SpotifyImportDialog
import com.musifly.android.utils.PlaylistImportExportService
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference
import com.musifly.android.viewmodels.LibraryPlaylistsViewModel
import com.musifly.innertube.utils.parseCookieString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.ui.component.liquidglass.LiquidGlassButton
import com.musifly.android.ui.component.liquidglass.LiquidGlassTitle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
    onNavigateToLocalMusic: () -> Unit = {},

    ) {
    val menuState = LocalMenuState.current
    LocalHapticFeedback.current

    val coroutineScope = rememberCoroutineScope()

    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSortTypeKey,
        PlaylistSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSortDescendingKey,
        true
    )
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.SMALL)

    // Get context and database in Composable context
    val context = LocalContext.current
    val database = LocalDatabase.current

    // Use ViewModel's allPlaylists Flow - same as LibrarySongsScreen uses viewModel.allSongs
    val playlists by viewModel.allPlaylists.collectAsState()

    val topSize by viewModel.topValue.collectAsState(initial = 50)
    
    // Trigger sync from Firestore when screen is composed to ensure data is synced
    // Same as LibrarySongsScreen does
    LaunchedEffect(Unit) {
        Timber.tag("LibraryPlaylistsScreen").d("🔄 LibraryPlaylistsScreen composed - triggering sync")
        viewModel.syncFromSupabase()
    }
    
    // Log when playlists change (this should trigger when Flow emits)
    LaunchedEffect(playlists) {
        Timber.tag("LibraryPlaylistsScreen").d("📊 Playlists Flow emitted: ${playlists.size} playlists")
        if (playlists.isNotEmpty()) {
            Timber.tag("LibraryPlaylistsScreen").d("📋 Playlist IDs: ${playlists.take(10).map { it.id }.joinToString()}${if (playlists.size > 10) "..." else ""}")
            Timber.tag("LibraryPlaylistsScreen").d("📋 Playlist names: ${playlists.take(10).map { it.playlist.name }.joinToString()}${if (playlists.size > 10) "..." else ""}")
        } else {
            Timber.tag("LibraryPlaylistsScreen").w("⚠️ Playlists list is EMPTY!")
        }
    }
    
    // Get liked songs count and thumbnails from database
    val likedSongsCount by database.likedSongsCount().collectAsState(initial = 0)
    
    // Get thumbnails from first 4 liked songs
    val likedSongsThumbnails = remember {
        mutableStateOf<List<String>>(emptyList())
    }
    
    // Update thumbnails when liked songs count changes
    LaunchedEffect(likedSongsCount) {
        withContext(Dispatchers.IO) {
            try {
                if (likedSongsCount > 0) {
                    val likedSongs = database.likedSongs(
                        SongSortType.CREATE_DATE,
                        true
                    ).first()
                    
                    val thumbnails = likedSongs.take(4).mapNotNull { song ->
                        song.song.thumbnailUrl?.takeIf { it.isNotEmpty() }
                    }
                    
                    likedSongsThumbnails.value = thumbnails
                } else {
                    likedSongsThumbnails.value = emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("LibraryPlaylistsScreen", "Error loading liked songs thumbnails: ${e.message}", e)
                likedSongsThumbnails.value = emptyList()
            }
        }
    }

    // Get liked playlist name (must be outside remember block)
    val likedPlaylistName = stringResource(R.string.liked)
    
    val likedPlaylist = remember(likedSongsCount, likedSongsThumbnails.value, likedPlaylistName) {
        Playlist(
            playlist = PlaylistEntity(
                id = "liked", // Use fixed ID for liked playlist
                name = likedPlaylistName
            ),
            songCount = likedSongsCount,
            thumbnails = likedSongsThumbnails.value,
        )
    }

    val topPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.my_top) + " $topSize"
            ),
            songCount = 0,
            thumbnails = emptyList(),
        )

    val cachePlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.cached_playlist)
            ),
            songCount = 0,
            thumbnails = emptyList(),
        )

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.sync()
            }
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showSpotifyImportDialog by rememberSaveable { mutableStateOf(false) }
    var showBloomeeImportDialog by rememberSaveable { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }
    
    if (showSpotifyImportDialog) {
        SpotifyImportDialog(
            onDismiss = { showSpotifyImportDialog = false },
            onImportComplete = { playlist ->
                // Playlist created successfully
            }
        )
    }
    
    if (showBloomeeImportDialog) {
        BloomeeImportDialog(
            onDismiss = { showBloomeeImportDialog = false },
            onImportComplete = {
                // Playlist imported successfully
            }
        )
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                        PlaylistSortType.NAME -> R.string.sort_by_name
                        PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                        PlaylistSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                    }
                },
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = pluralStringResource(
                    R.plurals.n_playlist,
                    playlists.size,
                    playlists.size
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp),
            ) {
                Icon(
                    painter =
                        painterResource(
                            when (viewType) {
                                LibraryViewType.LIST -> R.drawable.list
                                LibraryViewType.GRID -> R.drawable.grid_view
                            },
                        ),
                    contentDescription = null,
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        when (viewType) {
            LibraryViewType.LIST -> {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    item(
                        key = "downloadsButton",
                        contentType = { CONTENT_TYPE_HEADER },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("downloads")
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = "Downloads",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.offline),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    item(
                        key = "localMusicButton",
                        contentType = { CONTENT_TYPE_HEADER },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigateToLocalMusic()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = "Local Music",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Local Music",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    item(
                        key = "likedPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistListItem(
                            playlist = likedPlaylist,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("auto_playlist/liked")
                                    }
                                    .animateItem(),
                        )
                    }

                    item(
                        key = "TopPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistListItem(
                            playlist = topPlaylist,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("top_playlist/$topSize")
                                    }
                                    .animateItem(),
                        )
                    }

                    // Export Playlist Button
                    item(
                        key = "exportPlaylistsButton",
                        contentType = { CONTENT_TYPE_HEADER },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val allPlaylists = playlists
                                        if (allPlaylists.isNotEmpty()) {
                                            val firstPlaylist = allPlaylists.first()
                                            val result = PlaylistImportExportService.exportPlaylist(
                                                context = context,
                                                database = database,
                                                playlistId = firstPlaylist.id
                                            )
                                            withContext(Dispatchers.Main) {
                                                result.onSuccess { uri ->
                                                    val intent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        type = "application/json"
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, "Export Playlist"))
                                                }
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = stringResource(R.string.export),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.export),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    item(
                        key = "cachePlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistGridItem(
                            playlist = cachePlaylist,
                            fillMaxWidth = true,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("cache_playlist/cached")
                                        },
                                    )
                                    .animateItem(),
                            context = LocalContext.current // Pasamos el contexto actual para obtener la URI de la miniatura
                        )
                    }

                    playlists.let { playlists ->
                        if (playlists.isEmpty()) {
                            item {
                            }
                        }

                        items(
                            items = playlists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { playlist ->
                            LibraryPlaylistListItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                playlist = playlist,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }

                Box {
                    HideOnScrollFAB(
                        lazyListState = lazyListState,
                        icon = R.drawable.add,
                        onClick = {
                            showFabMenu = true
                        },
                    )
                    
                    if (showFabMenu) {
                        val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
                        val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

                        if (isLiquidGlass) {
                            LiquidGlassDialog(
                                onDismiss = { showFabMenu = false },
                                title = {
                                    LiquidGlassTitle(text = stringResource(R.string.options))
                                },
                                content = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        // 1. Create Playlist
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showFabMenu = false
                                                    showCreatePlaylistDialog = true
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.add),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.create_playlist),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }

                                        // 2. Export Playlist
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showFabMenu = false
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        val allPlaylists = playlists
                                                        if (allPlaylists.isNotEmpty()) {
                                                            val firstPlaylist = allPlaylists.first()
                                                            val result = PlaylistImportExportService.exportPlaylist(
                                                                context = context,
                                                                database = database,
                                                                playlistId = firstPlaylist.id
                                                            )
                                                            withContext(Dispatchers.Main) {
                                                                result.onSuccess { uri ->
                                                                    val intent = Intent().apply {
                                                                        action = Intent.ACTION_SEND
                                                                        type = "application/json"
                                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                    }
                                                                    context.startActivity(Intent.createChooser(intent, "Export Playlist"))
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.share),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "Export Playlist",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }

                                        // 3. Import from Spotify
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showFabMenu = false
                                                    showSpotifyImportDialog = true
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.playlist_import),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.import_from_spotify),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }

                                        // 4. Import Bloomee Playlist
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showFabMenu = false
                                                    showBloomeeImportDialog = true
                                                }
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.playlist_import),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = "Import Bloomee Playlist",
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                        }
                                    }
                                },
                                buttons = {
                                    LiquidGlassButton(
                                        onClick = { showFabMenu = false },
                                        text = stringResource(android.R.string.cancel)
                                    )
                                }
                            )
                        } else {
                            DropdownMenu(
                                expanded = showFabMenu,
                                onDismissRequest = { showFabMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.add),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(stringResource(R.string.create_playlist))
                                        }
                                    },
                                    onClick = {
                                        showFabMenu = false
                                        showCreatePlaylistDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.share),
                                                contentDescription = "Export",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Export Playlist",
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    },
                                    onClick = {
                                        showFabMenu = false
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val allPlaylists = playlists
                                            if (allPlaylists.isNotEmpty()) {
                                                val firstPlaylist = allPlaylists.first()
                                                val result = PlaylistImportExportService.exportPlaylist(
                                                    context = context,
                                                    database = database,
                                                    playlistId = firstPlaylist.id
                                                )
                                                withContext(Dispatchers.Main) {
                                                    result.onSuccess { uri ->
                                                        val intent = Intent().apply {
                                                            action = Intent.ACTION_SEND
                                                            type = "application/json"
                                                            putExtra(Intent.EXTRA_STREAM, uri)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(Intent.createChooser(intent, "Export Playlist"))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.playlist_import),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(stringResource(R.string.import_from_spotify))
                                        }
                                    },
                                    onClick = {
                                        showFabMenu = false
                                        showSpotifyImportDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { 
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.playlist_import),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text("Import Bloomee Playlist")
                                        }
                                    },
                                    onClick = {
                                        showFabMenu = false
                                        showBloomeeImportDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            LibraryViewType.GRID -> {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                        GridCells.Adaptive(
                            minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                        ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    item(
                        key = "downloadsButton",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = { CONTENT_TYPE_HEADER },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("downloads")
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = "Downloads",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.offline),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    item(
                        key = "localMusicButton",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = { CONTENT_TYPE_HEADER },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNavigateToLocalMusic()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.library_music),
                                contentDescription = "Local Music",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Local Music",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    item(
                        key = "likedPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistGridItem(
                            playlist = likedPlaylist,
                            fillMaxWidth = true,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/liked")
                                        },
                                    )
                                    .animateItem(),
                            context = LocalContext.current // Pasamos el contexto actual para obtener la URI de la miniatura
                        )
                    }

                    item(
                        key = "TopPlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistGridItem(
                            playlist = topPlaylist,
                            fillMaxWidth = true,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("top_playlist/$topSize")
                                        },
                                    )
                                    .animateItem(),
                            context = LocalContext.current // Pasamos el contexto actual para obtener la URI de la miniatura
                        )
                    }

                    // Export Playlist Button
                    item(
                        key = "exportPlaylistsButton",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = { CONTENT_TYPE_HEADER },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val allPlaylists = playlists
                                        if (allPlaylists.isNotEmpty()) {
                                            val firstPlaylist = allPlaylists.first()
                                            val result = PlaylistImportExportService.exportPlaylist(
                                                context = context,
                                                database = database,
                                                playlistId = firstPlaylist.id
                                            )
                                            withContext(Dispatchers.Main) {
                                                result.onSuccess { uri ->
                                                    val intent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        type = "application/json"
                                                        putExtra(Intent.EXTRA_STREAM, uri)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(Intent.createChooser(intent, "Export Playlist"))
                                                }
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = stringResource(R.string.export),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.export),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    item(
                        key = "cachePlaylist",
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) {
                        PlaylistGridItem(
                            playlist = cachePlaylist,
                            fillMaxWidth = true,
                            autoPlaylist = true,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("cache_playlist/cached")
                                        },
                                    )
                                    .animateItem(),
                            context = LocalContext.current // Pasamos el contexto actual para obtener la URI de la miniatura
                        )
                    }

                    playlists.let { playlists ->
                        if (playlists.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                            }
                        }

                        items(
                            items = playlists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) { playlist ->
                            LibraryPlaylistGridItem(
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                playlist = playlist,
                                modifier = Modifier.animateItem(),
                                context = LocalContext.current // Pasamos el contexto actual para obtener la URI de la miniatura
                            )
                        }
                    }
                }

                Box {
                    HideOnScrollFAB(
                        lazyListState = lazyGridState,
                        icon = R.drawable.add,
                        onClick = {
                            showFabMenu = true
                        },
                    )
                    
                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.add),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(stringResource(R.string.create_playlist))
                                }
                            },
                            onClick = {
                                showFabMenu = false
                                showCreatePlaylistDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.share),
                                        contentDescription = "Export",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Export Playlist",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            onClick = {
                                showFabMenu = false
                                coroutineScope.launch(Dispatchers.IO) {
                                    val allPlaylists = playlists
                                    if (allPlaylists.isNotEmpty()) {
                                        val firstPlaylist = allPlaylists.first()
                                        val result = PlaylistImportExportService.exportPlaylist(
                                            context = context,
                                            database = database,
                                            playlistId = firstPlaylist.id
                                        )
                                        withContext(Dispatchers.Main) {
                                            result.onSuccess { uri ->
                                                val intent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    type = "application/json"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, "Export Playlist"))
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.playlist_import),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(stringResource(R.string.import_from_spotify))
                                }
                            },
                            onClick = {
                                showFabMenu = false
                                showSpotifyImportDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.playlist_import),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text("Import Bloomee Playlist")
                                }
                            },
                            onClick = {
                                showFabMenu = false
                                showBloomeeImportDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}