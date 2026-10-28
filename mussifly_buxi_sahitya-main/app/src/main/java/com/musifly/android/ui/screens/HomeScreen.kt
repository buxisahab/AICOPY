package com.musifly.android.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.AccountNameKey
import com.musifly.android.constants.GridThumbnailHeight
import com.musifly.android.constants.FullScreenModeKey
import com.musifly.android.constants.InnerTubeCookieKey
import com.musifly.android.constants.LiquidGlassNavBarHeightKey
import com.musifly.android.constants.ListItemHeight
import com.musifly.android.constants.ListThumbnailSize
import com.musifly.android.constants.PureBlackKey
import com.musifly.android.constants.ShowHomeTabsKey
import com.musifly.android.constants.ThumbnailCornerRadius
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.Album
import com.musifly.android.db.entities.Artist
import com.musifly.android.db.entities.LocalItem
import com.musifly.android.db.entities.Playlist
import com.musifly.android.db.entities.Song
import com.musifly.android.extensions.toMediaItem
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.ui.component.liquidglass.LiquidHeader
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.blur
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.layerBackdrop
import com.musifly.android.models.SaavnAlbum
import com.musifly.android.models.SaavnArtist
import com.musifly.android.models.SaavnChartEntry
import com.musifly.android.models.SaavnPlaylist
import com.musifly.android.models.SaavnSong
import com.musifly.android.models.MediaMetadata
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.playback.PlayerConnection
import com.musifly.android.playback.queues.ListQueue
import com.musifly.android.playback.queues.YouTubeQueue
import com.musifly.android.streamingengines.StreamSourceManager
import com.musifly.android.ui.component.AlbumGridItem
import com.musifly.android.ui.component.ArtistCard
import com.musifly.android.ui.component.ArtistGridItem
import com.musifly.android.ui.component.LiquidSourceTabs
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.component.MenuState
import com.musifly.android.ui.component.MiniSongTile
import com.musifly.android.ui.component.NavigationTitle
import com.musifly.android.ui.component.PlaylistCard
import com.musifly.android.ui.component.SaavnHomeSection
import com.musifly.android.ui.component.ShuffleButton
import com.musifly.android.ui.component.SongCard
import com.musifly.android.ui.component.SongGridItem
import com.musifly.android.ui.component.SongListItem
import com.musifly.android.ui.component.WelcomeDialog
import com.musifly.android.ui.component.YouTubeGridItem

import com.musifly.android.ui.component.shimmer.GridItemPlaceHolder
import com.musifly.android.ui.component.shimmer.ShimmerHost
import com.musifly.android.ui.component.shimmer.TextPlaceholder
import com.musifly.android.ui.menu.AlbumMenu
import com.musifly.android.ui.menu.ArtistMenu
import com.musifly.android.ui.menu.SongMenu
import com.musifly.android.ui.menu.YouTubeAlbumMenu
import com.musifly.android.ui.menu.YouTubeArtistMenu
import com.musifly.android.ui.menu.YouTubePlaylistMenu
import com.musifly.android.ui.menu.YouTubeSongMenu
import com.musifly.android.ui.utils.SnapLayoutInfoProvider
import com.musifly.android.utils.rememberPreference
import com.musifly.android.viewmodels.HomeViewModel
import com.musifly.android.viewmodels.StoryViewModel
import com.musifly.android.ui.component.StorySection
import com.musifly.innertube.models.AlbumItem
import com.musifly.innertube.models.ArtistItem
import com.musifly.innertube.models.PlaylistItem
import com.musifly.innertube.models.SongItem
import com.musifly.innertube.models.WatchEndpoint
import com.musifly.innertube.models.YTItem
import com.musifly.innertube.utils.parseCookieString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

// Top-level composable functions for grid items (no capturing of outer scope variables)
@Composable
fun LocalGridItemComposable(
    item: LocalItem,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    navController: NavController,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    currentSource: StreamSourceManager.StreamType,
    database: MusicDatabase,
) {
    when (item) {
        is Song -> SongGridItem(
            song = item,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (item.id == mediaMetadata?.id) {
                            playerConnection.player.togglePlayPause()
                        } else {
                            // CRITICAL: Use source-aware queue creation
                            Log.d("HomeScreen", "🎵 PlaybackSourceCheck: TOPBAR_SELECTED = ${currentSource.name}")
                            Log.d("HomeScreen", "🎵 Playing Song: ${item.title}, id: ${item.id}")
                            
                            val metadata = item.toMediaMetadata()
                            val queue = when (currentSource) {
                                StreamSourceManager.StreamType.YOUTUBE,
                                StreamSourceManager.StreamType.YT_MUSIC -> {
                                    Log.d("HomeScreen", "🎵 Creating YouTubeQueue for playback")
                                    YouTubeQueue.radio(metadata)
                                }
                                StreamSourceManager.StreamType.JIOSAAVN -> {
                                    Log.d("HomeScreen", "⚠️ WARNING: Local Song with JioSaavn source - using ListQueue")
                                    // Convert to MediaItem for ListQueue
                                    val mediaItem = metadata.toMediaItem()
                                    ListQueue(items = listOf(mediaItem))
                                }
                            }
                            
                            Log.d("HomeScreen", "🎵 PLAYBACK_ENGINE_USED = ${currentSource.name}")
                            playerConnection.playQueue(queue)
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(
                            HapticFeedbackType.LongPress,
                        )
                        menuState.show {
                            SongMenu(
                                originalSong = item,
                                navController = navController,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                ),
            isActive = item.id == mediaMetadata?.id,
            isPlaying = isPlaying,
        )

        is Album -> AlbumGridItem(
            album = item,
            isActive = item.id == mediaMetadata?.album?.id,
            isPlaying = isPlaying,
            coroutineScope = scope,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        navController.navigate("album/${item.id}")
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            AlbumMenu(
                                originalAlbum = item,
                                navController = navController,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                )
        )

        is Artist -> ArtistGridItem(
            artist = item,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        navController.navigate("artist/${item.id}")
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(
                            HapticFeedbackType.LongPress,
                        )
                        menuState.show {
                            ArtistMenu(
                                originalArtist = item,
                                coroutineScope = scope,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                ),
        )

        is Playlist -> {}
    }
}

@Composable
fun YtGridItemComposable(
    item: YTItem,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    navController: NavController,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    currentSource: StreamSourceManager.StreamType,
) {
    YouTubeGridItem(
        item = item,
        isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
        isPlaying = isPlaying,
        coroutineScope = scope,
        thumbnailRatio = 1f,
        modifier = Modifier
            .combinedClickable(
                onClick = {
                    // CRITICAL: Use source-aware playback based on currentSource parameter
                    Log.d("HomeScreen", "🎵 PlaybackSourceCheck: TOPBAR_SELECTED = ${currentSource.name}")
                    Log.d("HomeScreen", "🎵 Clicked YTItem: ${item.javaClass.simpleName}, id: ${(item as? SongItem)?.id ?: "N/A"}")
                    
                    when (item) {
                        is SongItem -> {
                            // Check if current source matches item type
                            when (currentSource) {
                                StreamSourceManager.StreamType.YOUTUBE,
                                StreamSourceManager.StreamType.YT_MUSIC -> {
                                    // YouTube/YT Music item - use YouTubeQueue
                                    val metadata = item.toMediaMetadata()
                                    Log.d("HomeScreen", "🎵 Creating YouTubeQueue for YouTube/YT Music item")
                                    Log.d("HomeScreen", "🎵 PLAYBACK_ENGINE_USED = ${currentSource.name}")
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            item.endpoint ?: WatchEndpoint(
                                                videoId = item.id
                                            ), metadata
                                        )
                                    )
                                }
                                StreamSourceManager.StreamType.JIOSAAVN -> {
                                    // User selected JioSaavn but clicked YouTube item - warn and use YouTube
                                    Log.w("HomeScreen", "⚠️ WARNING: User selected JioSaavn but clicked YouTube item. Playing with YouTube engine.")
                                    val metadata = item.toMediaMetadata()
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            item.endpoint ?: WatchEndpoint(
                                                videoId = item.id
                                            ), metadata
                                        )
                                    )
                                }
                            }
                        }

                        is AlbumItem -> navController.navigate("album/${item.id}")
                        is ArtistItem -> navController.navigate("artist/${item.id}")
                        is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        when (item) {
                            is SongItem -> YouTubeSongMenu(
                                song = item,
                                navController = navController,
                                onDismiss = menuState::dismiss
                            )

                            is AlbumItem -> YouTubeAlbumMenu(
                                albumItem = item,
                                navController = navController,
                                onDismiss = menuState::dismiss
                            )

                            is ArtistItem -> YouTubeArtistMenu(
                                artist = item,
                                onDismiss = menuState::dismiss
                            )

                            is PlaylistItem -> YouTubePlaylistMenu(
                                playlist = item,
                                coroutineScope = scope,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                }
            )
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    storyViewModel: StoryViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()

    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val allYtItems by viewModel.allYtItems.collectAsState()
    
    // JioSaavn data (collected at top level, not inside when block)
    // NEW: Structured JioSaavn home feed
    val saavnHomeFeed by viewModel.saavnHomeFeed.collectAsState()
    
    // Legacy fields (kept for backward compatibility)
    val saavnHomeData by viewModel.saavnHomeData.collectAsState()
    val saavnFeaturedPlaylists by viewModel.saavnFeaturedPlaylists.collectAsState()
    val saavnTopCharts by viewModel.saavnTopCharts.collectAsState()
    val saavnTrendingArtists by viewModel.saavnTrendingArtists.collectAsState()
    
    // Smart recommendation sections
    val recommendationSections by viewModel.recommendationSections.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by rememberPreference(AccountNameKey, "")
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    
    // Welcome Dialog State - Show until user joins channel
    val welcomeJoined by rememberPreference(com.musifly.android.constants.WelcomeJoinedKey, defaultValue = false)
    
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val localProfile by database.getProfileFlow(currentUserId).collectAsState(initial = null)
    val isPremium = localProfile?.isPremium ?: false

    // Only show dialog if user hasn't joined yet
    val shouldShowDialog = !welcomeJoined
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }
    
    // Monitor source changes using StateFlow for reactive updates
    val currentSource by StreamSourceManager.currentSourceFlow.collectAsState()
    
    // REMOVED: Redundant LaunchedEffect(currentSource) which caused double refresh loop
    // HomeViewModel.kt already registers a callback with StreamSourceManager to trigger refresh
    // This fixed the "laggy experience due to auto refresh everytime" issue
    
    // Observe unified home songs list for instant UI updates
    val homeSongs by viewModel.homeSongs.collectAsState()
    LaunchedEffect(homeSongs) {
        // Log when home songs update (for debugging)
        if (homeSongs.isNotEmpty()) {
            Log.d("HomeScreen", "📊 Home songs updated: ${homeSongs.size} items from ${currentSource.name}")
        }
    }

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
        contentAlignment = Alignment.TopStart
    ) {
        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
        val quickPicksSnapLayoutInfoProvider = remember(quickPicksLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = quickPicksLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }
        val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
            SnapLayoutInfoProvider(
                lazyGridState = forgottenFavoritesLazyGridState,
                positionInLayout = { layoutSize, itemSize ->
                    (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                }
            )
        }

        // Extract composable call in BoxWithConstraints scope to ensure proper @Composable context
        val playerAwarePaddingValues = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        val currentSource by StreamSourceManager.currentSourceFlow.collectAsState()
        val (navBarHeight) = rememberPreference(LiquidGlassNavBarHeightKey, defaultValue = 64f)
        val (showHomeTabs) = rememberPreference(ShowHomeTabsKey, defaultValue = false)
        val (isFullScreen) = rememberPreference(FullScreenModeKey, defaultValue = false)

        // Switched to global backdrop for consistent blurring with navigation bar
        val tabsBackdrop = rememberLayerBackdrop()

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // BACKDROP CAPTURE GROUP - Captures everything behind the header
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isLiquidGlassActive()) Modifier.layerBackdrop(tabsBackdrop) else Modifier)
            ) {
                // BACKGROUND - Only for Liquid Glass theme
                if (isLiquidGlassActive()) {
                    val isLightTheme = !androidx.compose.foundation.isSystemInDarkTheme()
                    val gradientColors = if (isLightTheme) {
                        listOf(
                            androidx.compose.ui.graphics.Color(0xFFF5F5F5),
                            androidx.compose.ui.graphics.Color(0xFFE8E8E8),
                            androidx.compose.ui.graphics.Color(0xFFDADADA)
                        )
                    } else {
                        listOf(
                            androidx.compose.ui.graphics.Color(0xFF0A0A0A),
                            androidx.compose.ui.graphics.Color(0xFF121212),
                            androidx.compose.ui.graphics.Color(0xFF1A1A1A),
                            androidx.compose.ui.graphics.Color(0xFF0F0A1E)
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush = Brush.verticalGradient(gradientColors))
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                state = lazylistState,
                contentPadding = PaddingValues(
                    start = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Start).asPaddingValues().calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.End).asPaddingValues().calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                    top = 112.dp + (if (isFullScreen) 40.dp else 0.dp),
                    bottom = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
                )
            ) {
                /*
                item {
                    StorySection(
                        viewModel = storyViewModel,
                        isPremium = isPremium,
                        currentUserAvatar = localProfile?.avatarUrl,
                        onStoryClick = { group ->
                            navController.navigate("story_view/${group.userId}")
                        },
                        onAddStory = { uri ->
                            val encodedUri = java.net.URLEncoder.encode(uri.toString(), java.nio.charset.StandardCharsets.UTF_8.toString())
                            navController.navigate("story_editor/$encodedUri")
                        }
                    )
                }
                */



                // ============================================================
                // CONDITIONAL UI BASED ON SOURCE
                // ============================================================
                // JioSaavn: Show JioSaavn-specific home screen
                // YouTube/YT Music: Show default home screen
                // ============================================================
                if (currentSource == StreamSourceManager.StreamType.JIOSAAVN) {
                    // 🎧 JioSaavn Home Feed - NEW STRUCTURED APPROACH
                    // Show structured home feed sections
                    Log.d("HomeScreen", "🎯 Rendering JioSaavn feed: ${saavnHomeFeed.size} sections")
                    saavnHomeFeed.forEachIndexed { sectionIndex, section ->
                        Log.d("HomeScreen", "   Rendering section $sectionIndex: '${section.title}' (${section.items.size} items)")
                        if (section.items.isEmpty()) {
                            Log.w("HomeScreen", "   ⚠️ Section '${section.title}' has 0 items - skipping UI render")
                            return@forEachIndexed
                        }
                        item {
                            SaavnHomeSection(
                                section = section,
                                onItemClick = { item ->
                                    when (item) {
                                        is SaavnSong -> {
                                            // Play song immediately
                                            scope.launch(Dispatchers.Main) {
                                                val metadata = item.toMediaMetadata()
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = item.title,
                                                        items = listOf(metadata.toMediaItem())
                                                    )
                                                )
                                            }
                                        }
                                        is SaavnAlbum -> {
                                            // Navigate to album screen
                                            navController.navigate("album/saavn:${item.id}")
                                        }
                                        is SaavnPlaylist -> {
                                            // Navigate to playlist screen
                                            navController.navigate("online_playlist/saavn:${item.id}")
                                        }
                                        is SaavnArtist -> {
                                            // Navigate to artist screen
                                            navController.navigate("artist/saavn:${item.id}")
                                        }
                                        is SaavnChartEntry -> {
                                            // Navigate based on chart item type
                                            when (val chartItem = item.item) {
                                                is SaavnAlbum -> navController.navigate("album/saavn:${chartItem.id}")
                                                is SaavnPlaylist -> navController.navigate("online_playlist/saavn:${chartItem.id}")
                                                is SaavnSong -> {
                                                    scope.launch(Dispatchers.Main) {
                                                        val metadata = chartItem.toMediaMetadata()
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = chartItem.title,
                                                                items = listOf(metadata.toMediaItem())
                                                            )
                                                        )
                                                    }
                                                }
                                                else -> {}
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    
                    // Quick Picks section (works for all sources)
                    Log.d("HomeScreen", "🎯 Quick Picks check: ${quickPicks?.size ?: 0} songs available")
                    quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                        Log.d("HomeScreen", "✅ Rendering Quick Picks section with ${quickPicks.size} songs")
                        item {
                            NavigationTitle(
                                title = stringResource(R.string.quick_picks),
                                modifier = Modifier.animateItem()
                            )
                        }

                        item {
                            LazyHorizontalGrid(
                                state = quickPicksLazyGridState,
                                rows = GridCells.Fixed(4),
                                flingBehavior = rememberSnapFlingBehavior(
                                    quickPicksSnapLayoutInfoProvider
                                ),
                                contentPadding = WindowInsets.systemBars
                                    .only(WindowInsetsSides.Horizontal)
                                    .asPaddingValues(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ListItemHeight * 4)
                                    .animateItem()
                            ) {
                                items(
                                    items = quickPicks,
                                    key = { it.id }
                                ) { originalSong ->
                                    // fetch song from database to keep updated
                                    val song by database.song(originalSong.id)
                                        .collectAsState(initial = originalSong)

                                    SongListItem(
                                        song = song!!,
                                        showInLibraryIcon = true,
                                        isActive = song!!.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song!!,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.more_vert),
                                                    contentDescription = null
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .width(horizontalLazyGridItemWidth)
                                            .combinedClickable(
                                                onClick = {
                                                    if (song!!.id == mediaMetadata?.id) {
                                                        playerConnection.player.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(
                                                            YouTubeQueue.radio(
                                                                song!!.toMediaMetadata()
                                                            )
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song!!,
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
                    
                    // Also show recommendation sections (smart recommendations based on history)
                    // 🎧 JioSaavn Smart Home UI - Show recommendation sections
                        recommendationSections.forEach { section ->
                            when (section.type) {
                                com.musifly.android.models.RecommendationSectionType.DAILY_MIX,
                                com.musifly.android.models.RecommendationSectionType.TRENDING,
                                com.musifly.android.models.RecommendationSectionType.BECAUSE_YOU_LIKE,
                                com.musifly.android.models.RecommendationSectionType.MOOD_MIX,
                                com.musifly.android.models.RecommendationSectionType.RECOMMENDED_PLAYLISTS -> {
                                    // Horizontal scrollable carousel
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = section.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (section.shuffleEnabled) {
                                                ShuffleButton(
                                                    onClick = {
                                                        // Shuffle Daily Mix
                                                        scope.launch(Dispatchers.IO) {
                                                            val songs = section.items
                                                                .filterIsInstance<com.musifly.android.models.RecommendationItem.SongItem>()
                                                                .mapNotNull { item ->
                                                                    database.song(item.id).first()
                                                                }
                                                            if (songs.isNotEmpty()) {
                                                                // Switch to Main thread for player operations
                                                                withContext(Dispatchers.Main) {
                                                                    playerConnection.playQueue(
                                                                        com.musifly.android.playback.queues.ListQueue(
                                                                            title = section.title,
                                                                            items = songs.shuffled()
                                                                                .map { it.toMediaItem() }
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    item {
                                        LazyRow(
                                            contentPadding = WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            items(section.items) { item ->
                                                when (item) {
                                                    is com.musifly.android.models.RecommendationItem.SongItem -> {
                                                        SongCard(
                                                            item = item,
                                                            onClick = {
                                                                scope.launch(Dispatchers.IO) {
                                                                    database.song(item.id).first()
                                                                        ?.let { song ->
                                                                            // Switch to Main thread for player operations
                                                                            withContext(Dispatchers.Main) {
                                                                                playerConnection.playQueue(
                                                                                    com.musifly.android.playback.queues.ListQueue(
                                                                                        title = section.title,
                                                                                        items = listOf(
                                                                                            song.toMediaItem()
                                                                                        )
                                                                                    )
                                                                                )
                                                                            }
                                                                        }
                                                                }
                                                            },
                                                            modifier = Modifier.padding(
                                                                start = if (section.items.indexOf(
                                                                        item
                                                                    ) == 0
                                                                ) 16.dp else 0.dp
                                                            )
                                                        )
                                                    }

                                                    is com.musifly.android.models.RecommendationItem.PlaylistItem -> {
                                                        PlaylistCard(
                                                            item = item,
                                                            onClick = {
                                                                if (item.id.startsWith("saavn:")) {
                                                                    navController.navigate("online_playlist/${item.id}")
                                                                } else {
                                                                    navController.navigate("playlist/${item.id}")
                                                                }
                                                            },
                                                            modifier = Modifier.padding(
                                                                start = if (section.items.indexOf(
                                                                        item
                                                                    ) == 0
                                                                ) 16.dp else 0.dp
                                                            )
                                                        )
                                                    }

                                                    is com.musifly.android.models.RecommendationItem.ArtistItem -> {
                                                        ArtistCard(
                                                            item = item,
                                                            onClick = {
                                                                navController.navigate("artist/${item.id}")
                                                            },
                                                            modifier = Modifier.padding(
                                                                start = if (section.items.indexOf(
                                                                        item
                                                                    ) == 0
                                                                ) 16.dp else 0.dp
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                com.musifly.android.models.RecommendationSectionType.RECENTLY_PLAYED -> {
                                    // Vertical list
                                    item {
                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .animateItem()
                                        )
                                    }

                                    items(section.items.filterIsInstance<com.musifly.android.models.RecommendationItem.SongItem>()) { item ->
                                        MiniSongTile(
                                            item = item,
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    database.song(item.id).first()?.let { song ->
                                                        // Switch to Main thread for player operations
                                                        withContext(Dispatchers.Main) {
                                                            playerConnection.playQueue(
                                                                com.musifly.android.playback.queues.ListQueue(
                                                                    title = section.title,
                                                                    items = listOf(song.toMediaItem())
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }

                                com.musifly.android.models.RecommendationSectionType.RECOMMENDED_ARTISTS -> {
                                    // Horizontal scrollable carousel
                                    item {
                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .animateItem()
                                        )
                                    }

                                    item {
                                        LazyRow(
                                            contentPadding = WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            items(section.items.filterIsInstance<com.musifly.android.models.RecommendationItem.ArtistItem>()) { item ->
                                                ArtistCard(
                                                    item = item,
                                                    onClick = {
                                                        navController.navigate("artist/${item.id}")
                                                    },
                                                    modifier = Modifier.padding(
                                                        start = if (section.items.indexOf(
                                                                item
                                                            ) == 0
                                                        ) 16.dp else 0.dp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                com.musifly.android.models.RecommendationSectionType.TOP_CHARTS -> {
                                    // Same as trending
                                    item {
                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                                .animateItem()
                                        )
                                    }

                                    item {
                                        LazyRow(
                                            contentPadding = WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            items(section.items.filterIsInstance<com.musifly.android.models.RecommendationItem.PlaylistItem>()) { item ->
                                                PlaylistCard(
                                                    item = item,
                                                    onClick = {
                                                        if (item.id.startsWith("saavn:")) {
                                                            if (item.type == "playlist") {
                                                                navController.navigate("online_playlist/${item.id}")
                                                            } else {
                                                                navController.navigate("album/${item.id}")
                                                            }
                                                        } else {
                                                            navController.navigate("playlist/${item.id}")
                                                        }
                                                    },
                                                    modifier = Modifier.padding(
                                                        start = if (section.items.indexOf(
                                                                item
                                                            ) == 0
                                                        ) 16.dp else 0.dp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Additional JioSaavn sections from API
                        // Top Charts
                        saavnTopCharts?.takeIf { it.isNotEmpty() }?.let { charts ->
                            item {
                                Text(
                                    text = "🔥 Top Charts",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .animateItem()
                                )
                            }

                            item {
                                LazyRow(
                                    contentPadding = WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.animateItem()
                                ) {
                                    items(charts) { chart ->
                                        val id = chart["id"] as? String ?: ""
                                        val title = chart["title"] as? String ?: "Unknown"
                                        val image = chart["image"] as? String ?: ""
                                        val subtitle =
                                            chart["subtitle"] as? String
                                                ?: (chart["artist"] as? String
                                                    ?: "")
                                        val type = chart["type"] as? String ?: "album"

                                        PlaylistCard(
                                            item = com.musifly.android.models.RecommendationItem.PlaylistItem(
                                                id = "saavn:$id",
                                                title = title,
                                                thumbnailUrl = image,
                                                subtitle = subtitle,
                                                type = type
                                            ),
                                            onClick = {
                                                if (type == "playlist") {
                                                    navController.navigate("online_playlist/saavn:$id")
                                                } else {
                                                    navController.navigate("album/saavn:$id")
                                                }
                                            },
                                            modifier = Modifier.padding(
                                                start = if (charts.indexOf(
                                                        chart
                                                    ) == 0
                                                ) 16.dp else 0.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Featured Playlists
                        saavnFeaturedPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                            item {
                                Text(
                                    text = "📋 Featured Playlists",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .animateItem()
                                )
                            }

                            item {
                                LazyRow(
                                    contentPadding = WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.animateItem()
                                ) {
                                    items(playlists) { playlist ->
                                        val id = playlist["id"] as? String ?: ""
                                        val title = playlist["title"] as? String ?: "Unknown"
                                        val image = playlist["image"] as? String ?: ""
                                        val subtitle = playlist["subtitle"] as? String ?: "Playlist"

                                        PlaylistCard(
                                            item = com.musifly.android.models.RecommendationItem.PlaylistItem(
                                                id = "saavn:$id",
                                                title = title,
                                                thumbnailUrl = image,
                                                subtitle = subtitle,
                                                type = "playlist"
                                            ),
                                            onClick = {
                                                navController.navigate("online_playlist/saavn:$id")
                                            },
                                            modifier = Modifier.padding(
                                                start = if (playlists.indexOf(
                                                        playlist
                                                    ) == 0
                                                ) 16.dp else 0.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Trending Artists
                        saavnTrendingArtists?.takeIf { it.isNotEmpty() }?.let { artists ->
                            item {
                                Text(
                                    text = "👥 Trending Artists",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .animateItem()
                                )
                            }

                            item {
                                LazyRow(
                                    contentPadding = WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.animateItem()
                                ) {
                                    items(artists) { artist ->
                                        val id = artist["id"] as? String ?: ""
                                        val name = artist["name"] as? String ?: "Unknown Artist"
                                        val image = artist["image"] as? String ?: ""
                                        val subtitle = artist["subtitle"] as? String ?: ""

                                        ArtistCard(
                                            item = com.musifly.android.models.RecommendationItem.ArtistItem(
                                                id = "saavn:$id",
                                                title = name,
                                                thumbnailUrl = image,
                                                subtitle = subtitle
                                            ),
                                            onClick = {
                                                // Navigate to artist screen with JioSaavn artist ID
                                                navController.navigate("artist/saavn:$id")
                                            },
                                            modifier = Modifier.padding(
                                                start = if (artists.indexOf(
                                                        artist
                                                    ) == 0
                                                ) 16.dp else 0.dp
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        // Home Data Sections (New Releases, etc.)
                        saavnHomeData?.forEach { section ->
                            val sectionTitle = section["title"] as? String ?: ""
                            val sectionItems =
                                section["items"] as? List<Map<String, Any>> ?: emptyList()
                            val sectionType = section["type"] as? String ?: ""

                            if (sectionItems.isNotEmpty()) {
                                item {
                                    Text(
                                        text = sectionTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .animateItem()
                                    )
                                }

                                item {
                                    LazyRow(
                                        contentPadding = WindowInsets.systemBars
                                            .only(WindowInsetsSides.Horizontal)
                                            .asPaddingValues(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.animateItem()
                                    ) {
                                        items(sectionItems) { item ->
                                            val id = item["id"] as? String ?: ""
                                            val title = item["title"] as? String ?: "Unknown"
                                            val image = item["image"] as? String ?: ""
                                            val subtitle = item["subtitle"] as? String
                                                ?: (item["artist"] as? String ?: "")
                                            val itemType = item["type"] as? String ?: sectionType

                                            PlaylistCard(
                                                item = com.musifly.android.models.RecommendationItem.PlaylistItem(
                                                    id = "saavn:$id",
                                                    title = title,
                                                    thumbnailUrl = image,
                                                    subtitle = subtitle,
                                                    type = itemType
                                                ),
                                                onClick = {
                                                    if (itemType == "playlist") {
                                                        navController.navigate("online_playlist/saavn:$id")
                                                    } else {
                                                        navController.navigate("album/saavn:$id")
                                                    }
                                                },
                                                modifier = Modifier.padding(
                                                    start = if (sectionItems.indexOf(
                                                            item
                                                        ) == 0
                                                    ) 16.dp else 0.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                } else {
                    // 📺 Default UI for YouTube/YT Music sources
                    // Show Quick Picks, Keep Listening, etc.

                        quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                            item {
                                NavigationTitle(
                                    title = stringResource(R.string.quick_picks),
                                    modifier = Modifier.animateItem()
                                )
                            }

                            item {
                                LazyHorizontalGrid(
                                    state = quickPicksLazyGridState,
                                    rows = GridCells.Fixed(4),
                                    flingBehavior = rememberSnapFlingBehavior(
                                        quickPicksSnapLayoutInfoProvider
                                    ),
                                    contentPadding = WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ListItemHeight * 4)
                                        .animateItem()
                                ) {
                                    items(
                                        items = quickPicks,
                                        key = { it.id }
                                    ) { originalSong ->
                                        // fetch song from database to keep updated
                                        val song by database.song(originalSong.id)
                                            .collectAsState(initial = originalSong)

                                        SongListItem(
                                            song = song!!,
                                            showInLibraryIcon = true,
                                            isActive = song!!.id == mediaMetadata?.id,
                                            isPlaying = isPlaying,
                                            trailingContent = {
                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            SongMenu(
                                                                originalSong = song!!,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.more_vert),
                                                        contentDescription = null
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .width(horizontalLazyGridItemWidth)
                                                .combinedClickable(
                                                    onClick = {
                                                        if (song!!.id == mediaMetadata?.id) {
                                                            playerConnection.player.togglePlayPause()
                                                        } else {
                                                            playerConnection.playQueue(
                                                                YouTubeQueue.radio(
                                                                    song!!.toMediaMetadata()
                                                                )
                                                            )
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        menuState.show {
                                                            SongMenu(
                                                                originalSong = song!!,
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

                        keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
                            item {
                                NavigationTitle(
                                    title = stringResource(R.string.keep_listening),
                                    modifier = Modifier.animateItem()
                                )
                            }

                            item {
                                val rows = if (keepListening.size > 6) 2 else 1
                                LazyHorizontalGrid(
                                    state = rememberLazyGridState(),
                                    rows = GridCells.Fixed(rows),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((GridThumbnailHeight + with(LocalDensity.current) {
                                            MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 +
                                                    MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
                                        }) * rows)
                                        .animateItem()
                                ) {
                                    items(keepListening) { item ->
                                        LocalGridItemComposable(
                                            item = item,
                                            mediaMetadata = mediaMetadata,
                                            isPlaying = isPlaying,
                                            playerConnection = playerConnection,
                                            menuState = menuState,
                                            navController = navController,
                                            haptic = haptic,
                                            scope = scope,
                                            currentSource = currentSource,
                                            database = database,
                                        )
                                    }
                                }
                            }
                        }

                        accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                            item {
                                NavigationTitle(
                                    label = stringResource(R.string.your_ytb_playlists),
                                    title = accountName,
                                    thumbnail = {
                                        if (url != null) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(url)
                                                    .diskCachePolicy(CachePolicy.ENABLED)
                                                    .diskCacheKey(url)
                                                    .crossfade(true)
                                                    .allowHardware(false)
                                                    .build(),
                                                placeholder = painterResource(id = R.drawable.person),
                                                error = painterResource(id = R.drawable.person),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(ListThumbnailSize)
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(id = R.drawable.person),
                                                contentDescription = null,
                                                modifier = Modifier.size(ListThumbnailSize)
                                            )
                                        }
                                    },
                                    onClick = {
                                        navController.navigate(Screens.Profile.route)
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }


                            item {
                                LazyRow(
                                    contentPadding = WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                                    modifier = Modifier.animateItem()
                                ) {
                                    items(
                                        items = accountPlaylists,
                                        key = { it.id },
                                    ) { item ->
                                        YtGridItemComposable(
                                            item = item,
                                            mediaMetadata = mediaMetadata,
                                            isPlaying = isPlaying,
                                            playerConnection = playerConnection,
                                            menuState = menuState,
                                            navController = navController,
                                            haptic = haptic,
                                            scope = scope,
                                            currentSource = currentSource,
                                        )
                                    }
                                }
                            }
                        }

                        // Show content based on current source
                        when (StreamSourceManager.currentSource) {
                            StreamSourceManager.StreamType.YOUTUBE,
                            StreamSourceManager.StreamType.YT_MUSIC -> {
                                // YouTube/YouTube Music content
                                // Similar Recommendations (only for YouTube sources)
                                similarRecommendations?.forEach {
                                    item {
                                        NavigationTitle(
                                            label = stringResource(R.string.similar_to),
                                            title = it.title.title,
                                            thumbnail = it.title.thumbnailUrl?.let { thumbnailUrl ->
                                                {
                                                    val shape =
                                                        if (it.title is Artist) CircleShape else RoundedCornerShape(
                                                            ThumbnailCornerRadius
                                                        )
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(thumbnailUrl)
                                                            .allowHardware(false)
                                                            .build(),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(ListThumbnailSize)
                                                            .clip(shape)
                                                    )
                                                }
                                            },
                                            onClick = {
                                                when (it.title) {
                                                    is Song -> navController.navigate("album/${it.title.album!!.id}")
                                                    is Album -> navController.navigate("album/${it.title.id}")
                                                    is Artist -> navController.navigate("artist/${it.title.id}")
                                                    is Playlist -> {}
                                                }
                                            },
                                            modifier = Modifier.animateItem()
                                        )
                                    }

                                    item {
                                        LazyRow(
                                            contentPadding = WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            items(it.items) { item ->
                                                YtGridItemComposable(
                                                    item = item,
                                                    mediaMetadata = mediaMetadata,
                                                    isPlaying = isPlaying,
                                                    playerConnection = playerConnection,
                                                    menuState = menuState,
                                                    navController = navController,
                                                    haptic = haptic,
                                                    scope = scope,
                                                    currentSource = currentSource,
                                                )
                                            }
                                        }
                                    }
                                }

                                // YouTube Home Page Sections
                                homePage?.sections?.forEach {
                                    item {
                                        NavigationTitle(
                                            title = it.title,
                                            label = it.label,
                                            thumbnail = it.thumbnail?.let { thumbnailUrl ->
                                                {
                                                    val shape =
                                                        if (it.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(
                                                            ThumbnailCornerRadius
                                                        )
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(thumbnailUrl)
                                                            .allowHardware(false)
                                                            .build(),
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(ListThumbnailSize)
                                                            .clip(shape)
                                                    )
                                                }
                                            },
                                            modifier = Modifier.animateItem()
                                        )
                                    }

                                    item {
                                        LazyRow(
                                            contentPadding = WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            items(it.items) { item ->
                                                YtGridItemComposable(
                                                    item = item,
                                                    mediaMetadata = mediaMetadata,
                                                    isPlaying = isPlaying,
                                                    playerConnection = playerConnection,
                                                    menuState = menuState,
                                                    navController = navController,
                                                    haptic = haptic,
                                                    scope = scope,
                                                    currentSource = currentSource,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            StreamSourceManager.StreamType.JIOSAAVN -> {
                                // JioSaavn content (using already collected StateFlows from top level)
                                // Featured Playlists Section
                                saavnFeaturedPlaylists?.takeIf { it.isNotEmpty() }
                                    ?.let { playlists ->
                                        item {
                                            NavigationTitle(
                                                title = "Featured Playlists",
                                                modifier = Modifier.animateItem()
                                            )
                                        }

                                        item {
                                            LazyRow(
                                                contentPadding = WindowInsets.systemBars
                                                    .only(WindowInsetsSides.Horizontal)
                                                    .asPaddingValues(),
                                                modifier = Modifier.animateItem()
                                            ) {
                                                items(playlists) { playlist ->
                                                    val playlistId = playlist["id"] as? String ?: ""
                                                    val title =
                                                        playlist["title"] as? String ?: "Unknown"
                                                    val image = playlist["image"] as? String ?: ""
                                                    val subtitle =
                                                        playlist["subtitle"] as? String ?: ""

                                                    // Simple card for JioSaavn playlist
                                                    Box(
                                                        modifier = Modifier
                                                            .width(GridThumbnailHeight)
                                                            .padding(end = 8.dp)
                                                            .clip(
                                                                RoundedCornerShape(
                                                                    ThumbnailCornerRadius
                                                                )
                                                            )
                                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                                            .combinedClickable(
                                                                onClick = {
                                                                    // Navigate to JioSaavn playlist
                                                                    navController.navigate("online_playlist/saavn:$playlistId")
                                                                }
                                                            )
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(8.dp)
                                                        ) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current)
                                                                    .data(image.takeIf { it.isNotEmpty() })
                                                                    .allowHardware(false)
                                                                    .build(),
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(GridThumbnailHeight)
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                                placeholder = painterResource(R.drawable.music_note),
                                                                error = painterResource(R.drawable.music_note)
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Text(
                                                                text = title,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (subtitle.isNotEmpty()) {
                                                                Text(
                                                                    text = subtitle,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                                        alpha = 0.7f
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                // Top Charts Section
                                saavnTopCharts?.takeIf { it.isNotEmpty() }?.let { topCharts ->
                                    item {
                                        NavigationTitle(
                                            title = "Top Charts",
                                            modifier = Modifier.animateItem()
                                        )
                                    }

                                    item {
                                        LazyRow(
                                            contentPadding = WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                            modifier = Modifier.animateItem()
                                        ) {
                                            items(topCharts) { chart ->
                                                val chartId = chart["id"] as? String ?: ""
                                                val title = chart["title"] as? String ?: "Unknown"
                                                val image = chart["image"] as? String ?: ""
                                                val subtitle = chart["subtitle"] as? String
                                                    ?: (chart["artist"] as? String ?: "")
                                                val chartType = chart["type"] as? String ?: "album"

                                                // Card for Top Charts
                                                Box(
                                                    modifier = Modifier
                                                        .width(GridThumbnailHeight)
                                                        .padding(end = 8.dp)
                                                        .clip(
                                                            RoundedCornerShape(
                                                                ThumbnailCornerRadius
                                                            )
                                                        )
                                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                                        .combinedClickable(
                                                            onClick = {
                                                                try {
                                                                    android.util.Log.d(
                                                                        "HomeScreen",
                                                                        "🖱️ Top chart clicked - type: $chartType, id: $chartId, title: $title"
                                                                    )
                                                                    if (chartId.isEmpty()) {
                                                                        android.util.Log.e(
                                                                            "HomeScreen",
                                                                            "❌ Cannot navigate: chartId is empty!"
                                                                        )
                                                                        return@combinedClickable
                                                                    }
                                                                    when (chartType) {
                                                                        "playlist" -> {
                                                                            android.util.Log.d(
                                                                                "HomeScreen",
                                                                                "📋 Navigating to playlist: saavn:$chartId"
                                                                            )
                                                                            navController.navigate("online_playlist/saavn:$chartId")
                                                                        }

                                                                        "album" -> {
                                                                            android.util.Log.d(
                                                                                "HomeScreen",
                                                                                "📀 Navigating to album: saavn:$chartId"
                                                                            )
                                                                            navController.navigate("album/saavn:$chartId")
                                                                        }

                                                                        else -> {
                                                                            android.util.Log.w(
                                                                                "HomeScreen",
                                                                                "⚠️ Unknown chart type: $chartType, treating as album"
                                                                            )
                                                                            // Fallback: treat unknown type as album
                                                                            navController.navigate("album/saavn:$chartId")
                                                                        }
                                                                    }
                                                                } catch (e: Exception) {
                                                                    android.util.Log.e(
                                                                        "HomeScreen",
                                                                        "❌ Error navigating: ${e.message}",
                                                                        e
                                                                    )
                                                                }
                                                            }
                                                        )
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(8.dp)
                                                    ) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(LocalContext.current)
                                                                .data(image.takeIf { it.isNotEmpty() })
                                                                .allowHardware(false)
                                                                .build(),
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(GridThumbnailHeight)
                                                                .clip(RoundedCornerShape(8.dp)),
                                                            placeholder = painterResource(R.drawable.music_note),
                                                            error = painterResource(R.drawable.music_note)
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = title,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (subtitle.isNotEmpty()) {
                                                            Text(
                                                                text = subtitle,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                                    alpha = 0.7f
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Home Data Sections
                                saavnHomeData?.forEach { section ->
                                    val sectionTitle = section["title"] as? String ?: ""
                                    val sectionItems =
                                        section["items"] as? List<Map<String, Any>> ?: emptyList()

                                    if (sectionItems.isNotEmpty()) {
                                        item {
                                            NavigationTitle(
                                                title = sectionTitle,
                                                modifier = Modifier.animateItem()
                                            )
                                        }

                                        item {
                                            LazyRow(
                                                contentPadding = WindowInsets.systemBars
                                                    .only(WindowInsetsSides.Horizontal)
                                                    .asPaddingValues(),
                                                modifier = Modifier.animateItem()
                                            ) {
                                                items(sectionItems) { item ->
                                                    val itemId = item["id"] as? String ?: ""
                                                    val itemTitle =
                                                        item["title"] as? String ?: "Unknown"
                                                    val itemImage = item["image"] as? String ?: ""
                                                    val itemSubtitle = item["subtitle"] as? String
                                                        ?: (item["artist"] as? String ?: "")
                                                    val itemType =
                                                        item["type"] as? String ?: "playlist"

                                                    // Simple card for JioSaavn items
                                                    Box(
                                                        modifier = Modifier
                                                            .width(GridThumbnailHeight)
                                                            .padding(end = 8.dp)
                                                            .clip(
                                                                RoundedCornerShape(
                                                                    ThumbnailCornerRadius
                                                                )
                                                            )
                                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                                            .combinedClickable(
                                                                onClick = {
                                                                    try {
                                                                        android.util.Log.d(
                                                                            "HomeScreen",
                                                                            "🖱️ Home section item clicked - type: $itemType, id: $itemId, title: $itemTitle"
                                                                        )
                                                                        if (itemId.isEmpty()) {
                                                                            android.util.Log.e(
                                                                                "HomeScreen",
                                                                                "❌ Cannot navigate: itemId is empty!"
                                                                            )
                                                                            return@combinedClickable
                                                                        }
                                                                        when (itemType) {
                                                                            "playlist" -> {
                                                                                android.util.Log.d(
                                                                                    "HomeScreen",
                                                                                    "📋 Navigating to playlist: saavn:$itemId"
                                                                                )
                                                                                navController.navigate(
                                                                                    "online_playlist/saavn:$itemId"
                                                                                )
                                                                            }

                                                                            "album" -> {
                                                                                android.util.Log.d(
                                                                                    "HomeScreen",
                                                                                    "📀 Navigating to album: saavn:$itemId"
                                                                                )
                                                                                navController.navigate(
                                                                                    "album/saavn:$itemId"
                                                                                )
                                                                            }

                                                                            else -> {
                                                                                android.util.Log.w(
                                                                                    "HomeScreen",
                                                                                    "⚠️ Unknown item type: $itemType, treating as album"
                                                                                )
                                                                                // Fallback: treat unknown type as album
                                                                                navController.navigate(
                                                                                    "album/saavn:$itemId"
                                                                                )
                                                                            }
                                                                        }
                                                                    } catch (e: Exception) {
                                                                        android.util.Log.e(
                                                                            "HomeScreen",
                                                                            "❌ Error navigating: ${e.message}",
                                                                            e
                                                                        )
                                                                    }
                                                                }
                                                            )
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(8.dp)
                                                        ) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current)
                                                                    .data(itemImage.takeIf { it.isNotEmpty() })
                                                                    .allowHardware(false)
                                                                    .build(),
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(GridThumbnailHeight)
                                                                    .clip(RoundedCornerShape(8.dp)),
                                                                placeholder = painterResource(R.drawable.music_note),
                                                                error = painterResource(R.drawable.music_note)
                                                            )
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Text(
                                                                text = itemTitle,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (itemSubtitle.isNotEmpty()) {
                                                                Text(
                                                                    text = itemSubtitle,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                                        alpha = 0.7f
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        explorePage?.newReleaseAlbums?.let { newReleaseAlbums ->
                            item {
                                NavigationTitle(
                                    title = stringResource(R.string.new_release_albums),
                                    onClick = {
                                        navController.navigate("new_release")
                                    },
                                    modifier = Modifier.animateItem()
                                )
                            }

                            item {
                                LazyRow(
                                    contentPadding = WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                                    modifier = Modifier.animateItem()
                                ) {
                                    items(
                                        items = newReleaseAlbums,
                                        key = { it.id }
                                    ) { album ->
                                        YouTubeGridItem(
                                            item = album,
                                            isActive = mediaMetadata?.album?.id == album.id,
                                            isPlaying = isPlaying,
                                            coroutineScope = scope,
                                            modifier = Modifier
                                                .combinedClickable(
                                                    onClick = {
                                                        navController.navigate("album/${album.id}")
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        menuState.show {
                                                            YouTubeAlbumMenu(
                                                                albumItem = album,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss
                                                            )
                                                        }
                                                    }
                                                )
                                                .animateItem()
                                        )
                                    }
                                }
                            }
                        }

                        if (isLoading) {
                            item {
                                ShimmerHost(
                                    modifier = Modifier.animateItem()
                                ) {
                                    TextPlaceholder(
                                        height = 36.dp,
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .width(250.dp),
                                    )
                                    LazyRow {
                                        items(4) {
                                            GridItemPlaceHolder()
                                        }
                                    }
                                }
                            }
                        }

                        forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                            item {
                                NavigationTitle(
                                    title = stringResource(R.string.forgotten_favorites),
                                    modifier = Modifier.animateItem()
                                )
                            }

                            item {
                                // take min in case list size is less than 4
                                val rows = min(4, forgottenFavorites.size)
                                LazyHorizontalGrid(
                                    state = forgottenFavoritesLazyGridState,
                                    rows = GridCells.Fixed(rows),
                                    flingBehavior = rememberSnapFlingBehavior(
                                        forgottenFavoritesSnapLayoutInfoProvider
                                    ),
                                    contentPadding = WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(ListItemHeight * rows)
                                        .animateItem()
                                ) {
                                    items(
                                        items = forgottenFavorites,
                                        key = { it.id }
                                    ) { originalSong ->
                                        val song by database.song(originalSong.id)
                                            .collectAsState(initial = originalSong)

                                        SongListItem(
                                            song = song!!,
                                            showInLibraryIcon = true,
                                            isActive = song!!.id == mediaMetadata?.id,
                                            isPlaying = isPlaying,
                                            modifier = Modifier
                                                .width(horizontalLazyGridItemWidth)
                                                .combinedClickable(
                                                    onClick = {
                                                        if (song!!.id == mediaMetadata?.id) {
                                                            playerConnection.player.togglePlayPause()
                                                        } else {
                                                            playerConnection.playQueue(
                                                                YouTubeQueue.radio(
                                                                    song!!.toMediaMetadata()
                                                                )
                                                            )
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        menuState.show {
                                                            SongMenu(
                                                                originalSong = song!!,
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
                    } // Close else block
                } // Close LazyColumn
            } // Close Backdrop Capture Box
                
                // CUSTOM FLOATING HEADER
                if (isLiquidGlassActive()) {
                    LiquidHeader(
                        navController = navController,
                        backdrop = tabsBackdrop,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 0.dp) // Edge-to-edge
                    )
                }

                // Indicator and HideOnScrollFAB in Box scope for proper @Composable context
                Indicator(
                    isRefreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = (112.dp + 15.dp)) // Position 15dp below active top elements
                )
                
                // Floating source tabs removed
            } // Close Box (was BoxWithConstraints)
        
        // Welcome Dialog - Shows until user joins channel
        if (shouldShowDialog) {
            WelcomeDialog(
                onDismiss = {
                    // Dismiss dialog (don't save - user can see it again next time)
                },
                onJoinChannel = {
                    // Dialog will automatically close when welcomeJoined becomes true
                    // because shouldShowDialog will become false
                }
            )
        }
    }
}

