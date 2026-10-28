package com.buxi.music

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.buxi.music.utils.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import com.buxi.music.models.MediaMetadata
import com.buxi.music.innertube.YouTube
import com.buxi.music.innertube.models.SongItem
import com.buxi.music.innertube.models.WatchEndpoint
import com.buxi.music.constants.AppBarHeight
import com.buxi.music.constants.DarkModeKey
import com.buxi.music.constants.DefaultOpenTabKey
import com.buxi.music.constants.DisableScreenshotKey
import com.buxi.music.constants.DynamicThemeKey
import com.buxi.music.constants.MiniPlayerHeight
import com.buxi.music.constants.NavigationBarAnimationSpec
import com.buxi.music.constants.NavigationBarHeight
import com.buxi.music.constants.PauseSearchHistoryKey
import com.buxi.music.constants.PlayerBackgroundStyle
import com.buxi.music.constants.PlayerBackgroundStyleKey
import com.buxi.music.constants.PureBlackKey
import com.buxi.music.constants.SearchSource
import com.buxi.music.constants.ThemeColor
import com.buxi.music.constants.ThemeColorKey
import com.buxi.music.constants.SearchSourceKey
import com.buxi.music.constants.SlimNavBarKey
import com.buxi.music.constants.StopMusicOnTaskClearKey
import com.buxi.music.db.MusicDatabase
import com.buxi.music.db.entities.SearchHistory
import com.buxi.music.extensions.toEnum
import com.buxi.music.models.toMediaMetadata
import com.buxi.music.playback.DownloadUtil
import com.buxi.music.playback.MusicService
import com.buxi.music.playback.MusicService.MusicBinder
import com.buxi.music.playback.PlayerConnection
import com.buxi.music.playback.queues.YouTubeQueue
import com.buxi.music.ui.component.BottomSheetMenu
import com.buxi.music.ui.component.IconButton
import com.buxi.music.ui.component.LocalMenuState
import com.buxi.music.ui.component.LocaleManager
import com.buxi.music.ui.component.TopSearch
import com.buxi.music.ui.component.VoiceSearchDialog
import com.buxi.music.ui.component.VoiceSearchState
import com.buxi.music.ui.component.rememberBottomSheetState
import com.buxi.music.ui.component.rememberVoiceSearchManager
import com.buxi.music.ui.component.shimmer.ShimmerTheme
import com.buxi.music.ui.menu.YouTubeSongMenu
import com.buxi.music.ui.player.BottomSheetPlayer
import com.buxi.music.ui.screens.Screens
import com.buxi.music.ui.screens.navigationBuilder
import com.buxi.music.ui.screens.search.LocalSearchScreen
import com.buxi.music.ui.screens.search.OnlineSearchScreen
import com.buxi.music.ui.screens.settings.DarkMode
import com.buxi.music.ui.screens.settings.NavigationTab
import com.buxi.music.ui.theme.ColorSaver
import com.buxi.music.ui.theme.DefaultThemeColor
import com.buxi.music.ui.theme.AiMusicTheme
import com.buxi.music.ui.theme.getThemeColor
import com.buxi.music.ui.theme.ThemeBackground
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.theme.ThemeWhite
import com.buxi.music.ui.theme.ThemeAccent
import com.buxi.music.ui.theme.ThemeTextPrimary
import com.buxi.music.ui.theme.ThemeTextTertiary
import com.buxi.music.ui.theme.extractThemeColor
import com.buxi.music.ui.utils.appBarScrollBehavior
import com.buxi.music.ui.utils.backToMain
import com.buxi.music.ui.utils.resetHeightOffset
import com.buxi.music.utils.SyncUtils
import com.buxi.music.utils.dataStore
import com.buxi.music.utils.get
import com.buxi.music.utils.rememberEnumPreference
import com.buxi.music.utils.rememberPreference
import com.buxi.music.utils.reportException
import com.buxi.music.utils.checkForUpdate
import com.buxi.music.utils.UpdateManager
import com.buxi.music.dsp.UniversalDSPManager
import com.buxi.music.dsp.PlayerType
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import com.buxi.music.ui.component.AvatarPreferenceManager
import com.buxi.music.ui.component.AvatarSelection
import com.buxi.music.ui.component.Lyrics

// El codigo original de la aplicacion pertenece a : Arturo Cervantes Galindo (Arturo254) Cualquier parecido es copia y pega de mi codigo original

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                if (service is MusicBinder) {
                    playerConnection =
                        PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    private var latestVersionName by mutableStateOf("2.0.5")

    override fun onStart() {
        super.onStart()
        startService(Intent(this, MusicService::class.java))
        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onResume() {
        super.onResume()
        // Audio effects are handled automatically by MusicService
    }
    
    override fun onStop() {
        unbindService(serviceConnection)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (dataStore.get(
                StopMusicOnTaskClearKey,
                false
            ) && playerConnection?.isPlaying?.value == true && isFinishing
        ) {
            stopService(Intent(this, MusicService::class.java))
            unbindService(serviceConnection)
            playerConnection = null
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleManager.getInstance(newBase).applyLocaleToContext(newBase)
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Check for app updates
        lifecycleScope.launch {
            checkForUpdate()
        }

        lifecycleScope.launch {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    if (it) {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE,
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                }
        }

        intent?.let { handlevideoIdIntent(it) }

        // Enable global-session compatibility for Android 14/15 and OEMs that block per-session effects (Old DSP - Commented out)
        // try { UniversalDSPManager.getInstance().setCompatibilityPreferGlobalSession(true) } catch (_: Exception) {}

        setContent {

            var showFullscreenLyrics by remember { mutableStateOf(false) }

            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = false)
            val selectedThemeColor by rememberEnumPreference(ThemeColorKey, defaultValue = ThemeColor.VIBRANT)
            // Force dark theme always - disable system theme detection
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON)

            val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
            // Force dark theme always - ignore system theme completely
            val useDarkTheme = remember { true }
            LaunchedEffect(useDarkTheme) {
                this@MainActivity.setSystemBarAppearance(useDarkTheme)
            }
            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            LaunchedEffect(enableDynamicTheme) {
                if (!enableDynamicTheme) {
                    themeColor = getThemeColor(selectedThemeColor)
                    return@LaunchedEffect
                }
                val currentPlayerConnection = playerConnection
                if (currentPlayerConnection == null) {
                    themeColor = getThemeColor(selectedThemeColor)
                    return@LaunchedEffect
                }
                currentPlayerConnection.service.currentMediaMetadata.collectLatest { song ->
                    themeColor =
                        if (song != null) {
                            withContext(Dispatchers.IO) {
                                val result =
                                    imageLoader.execute(
                                        ImageRequest
                                            .Builder(this@MainActivity)
                                            .data(song.thumbnailUrl)
                                            .allowHardware(false) // pixel access is not supported on Config#HARDWARE bitmaps
                                            .build(),
                                    )
                                (result.drawable as? BitmapDrawable)?.bitmap?.extractThemeColor()
                                    ?: getThemeColor(selectedThemeColor)
                            }
                        } else {
                            getThemeColor(selectedThemeColor)
                        }
                }
            }

            AiMusicTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = themeColor,
            ) {
                // New theme background with proper text hierarchy
                ThemeBackground(isDarkTheme = useDarkTheme) {
                    
                @SuppressLint("UnusedBoxWithConstraintsScope")
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(ThemeDark),
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }

                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    var previousTab by rememberSaveable { mutableStateOf<String?>("home") }

                    val navigationItems = remember { Screens.MainScreens }
                    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                    val defaultOpenTab =
                        remember {
                            dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                        }
                            val tabOpenedFromShortcut =
                                remember {
                                    when (intent?.action) {
                                        MainActivity.ACTION_LIBRARY -> NavigationTab.LIBRARY
                                        MainActivity.ACTION_EXPLORE -> NavigationTab.EXPLORE
                                        else -> null
                                    }
                                }

                    val topLevelScreens =
                        listOf(
                            Screens.ForYou.route,
                            Screens.Explore.route,
                            Screens.Profile.route,
                            "settings",
                        )

                    val (query, onQueryChange) =
                        rememberSaveable(stateSaver = TextFieldValue.Saver) {
                            mutableStateOf(TextFieldValue())
                        }

                    var active by rememberSaveable {
                        mutableStateOf(false)
                    }

                    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }

                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.YOUTUBE)

                    val searchBarFocusRequester = remember { FocusRequester() }

                    val onSearch: (String) -> Unit = {
                        if (it.isNotEmpty()) {
                            onActiveChange(false)
                            navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                            if (dataStore[PauseSearchHistoryKey] != true) {
                                database.query {
                                    insert(SearchHistory(query = it))
                                }
                            }
                        }
                    }

                    var openSearchImmediately: Boolean by remember {
                        mutableStateOf(intent?.action == MainActivity.ACTION_SEARCH)
                    }

                    val isSearchRoute = navBackStackEntry?.destination?.route?.startsWith("search/") == true

                    val shouldShowSearchBar =
                        remember(active, navBackStackEntry) {
                            active ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true
                        }

                    val shouldShowNavigationBar =
                        remember(navBackStackEntry, active) {
                            navBackStackEntry?.destination?.route == null ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } &&
                                    !active
                        }

                    val navigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar) NavigationBarHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = "",
                    )

                    val playerBottomSheetState =
                        rememberBottomSheetState(
                            dismissedBound = 0.dp,
                            collapsedBound = bottomInset + (if (shouldShowNavigationBar) NavigationBarHeight else 0.dp) + MiniPlayerHeight,
                            expandedBound = maxHeight,
                        )

                    val playerAwareWindowInsets =
                        remember(
                            bottomInset,
                            shouldShowNavigationBar,
                            playerBottomSheetState.isDismissed
                        ) {
                            var bottom = bottomInset
                            if (shouldShowNavigationBar) bottom += NavigationBarHeight
                            if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                            windowsInsets
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                                .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                        }

                    appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    val searchBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )
                    val topAppBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )

                    LaunchedEffect(navBackStackEntry) {
                        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            val searchQuery =
                                withContext(Dispatchers.IO) {
                                    if (navBackStackEntry
                                            ?.arguments
                                            ?.getString(
                                                "query",
                                            )!!
                                            .contains(
                                                "%",
                                            )
                                    ) {
                                        navBackStackEntry?.arguments?.getString(
                                            "query",
                                        )!!
                                    } else {
                                        URLDecoder.decode(
                                            navBackStackEntry?.arguments?.getString("query")!!,
                                            "UTF-8"
                                        )
                                    }
                                }
                            onQueryChange(
                                TextFieldValue(
                                    searchQuery,
                                    TextRange(searchQuery.length)
                                )
                            )
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                            onQueryChange(TextFieldValue())
                        }
                        searchBarScrollBehavior.state.resetHeightOffset()
                        topAppBarScrollBehavior.state.resetHeightOffset()
                    }
                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                            searchBarFocusRequester.requestFocus()
                        }
                    }

                    LaunchedEffect(playerConnection) {
                        try {
                            val player = playerConnection?.player ?: return@LaunchedEffect
                            if (player.currentMediaItem == null) {
                                if (!playerBottomSheetState.isDismissed) {
                                    playerBottomSheetState.dismiss()
                                }
                            } else {
                                if (playerBottomSheetState.isDismissed) {
                                    playerBottomSheetState.collapseSoft()
                                }
                            }
                        } catch (e: Exception) {
                            // Handle state management errors gracefully
                            try {
                                if (playerBottomSheetState.isDismissed) {
                                    playerBottomSheetState.collapseSoft()
                                }
                            } catch (e2: Exception) {
                                // Ignore secondary errors
                            }
                        }
                    }

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        try {
                            val player =
                                playerConnection?.player ?: return@DisposableEffect onDispose { }
                            val listener =
                                object : Player.Listener {
                                    override fun onMediaItemTransition(
                                        mediaItem: MediaItem?,
                                        reason: Int,
                                    ) {
                                        try {
                                            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                                mediaItem != null &&
                                                playerBottomSheetState.isDismissed
                                            ) {
                                                playerBottomSheetState.collapseSoft()
                                            }
                                        } catch (e: Exception) {
                                            // Handle transition errors gracefully
                                        }
                                    }
                                }
                            player.addListener(listener)
                            onDispose {
                                try {
                                    player.removeListener(listener)
                                } catch (e: Exception) {
                                    // Ignore cleanup errors
                                }
                            }
                        } catch (e: Exception) {
                            // Handle initialization errors
                            onDispose { }
                        }
                    }

                    var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(navBackStackEntry) {
                        shouldShowTopBar =
                            !active && navBackStackEntry?.destination?.route in topLevelScreens && navBackStackEntry?.destination?.route != "settings"
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember {
                        mutableStateOf(null)
                    }
                    DisposableEffect(Unit) {
                        val listener =
                            Consumer<Intent> { intent ->
                                val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)
                                    ?.toUri() ?: return@Consumer
                                when (val path = uri.pathSegments.firstOrNull()) {
                                    "playlist" ->
                                        uri.getQueryParameter("list")?.let { playlistId ->
                                            if (playlistId.startsWith("OLAK5uy_")) {
                                                coroutineScope.launch {
                                                    YouTube
                                                        .albumSongs(playlistId)
                                                        .onSuccess { songs ->
                                                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                                                navController.navigate("album/$browseId")
                                                            }
                                                        }.onFailure {
                                                            reportException(it)
                                                        }
                                                }
                                            } else {
                                                navController.navigate("online_playlist/$playlistId")
                                            }
                                        }

                                    "browse" ->
                                        uri.lastPathSegment?.let { browseId ->
                                            navController.navigate("album/$browseId")
                                        }

                                    "channel", "c" ->
                                        uri.lastPathSegment?.let { artistId ->
                                            navController.navigate("artist/$artistId")
                                        }

                                    else ->
                                        when {
                                            path == "watch" -> uri.getQueryParameter("v")
                                            uri.host == "youtu.be" -> path
                                            else -> null
                                        }?.let { videoId ->
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    YouTube.queue(listOf(videoId))
                                                }.onSuccess {
                                                    playerConnection?.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = it.firstOrNull()?.id),
                                                            it.firstOrNull()?.toMediaMetadata()
                                                        )
                                                    )
                                                }.onFailure {
                                                    reportException(it)
                                                }
                                            }
                                        }
                                }
                            }

                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }

                    val currentTitle = remember(navBackStackEntry) {
                        when (navBackStackEntry?.destination?.route) {
                            Screens.ForYou.route -> R.string.for_you
                            Screens.Explore.route -> R.string.explore
                            Screens.Profile.route -> R.string.profile
                            else -> null
                        }
                    }

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides contentColorFor(ThemeDark),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils,
                    ) {
                        Scaffold(
                            topBar = {
                                val playerBackground by rememberEnumPreference(
                                    key = PlayerBackgroundStyleKey,
                                    defaultValue = PlayerBackgroundStyle.DEFAULT
                                )

                                if (shouldShowTopBar) {
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        // Capa base con color de fondo siempre visible
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(ThemeDark)
                                        )

                                        // Validación más segura para el background
                                        val safeSelectedValue = when {
                                            playerBackground == PlayerBackgroundStyle.BLUR &&
                                                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                                                PlayerBackgroundStyle.DEFAULT // Sin blur en versiones < Android 12 (S)
                                            }

                                            else -> playerBackground
                                        }

                                        // Only show blur if safeSelectedValue is BLUR
                                        if (safeSelectedValue == PlayerBackgroundStyle.BLUR) {
                                            val playerConnection = LocalPlayerConnection.current

                                            // Safer verification of playerConnection
                                            playerConnection?.let { connection ->
                                                val mediaMetadata by connection.mediaMetadata.collectAsState()

                                                mediaMetadata?.thumbnailUrl?.let { imageUrl ->
                                                    AsyncImage(
                                                        model = imageUrl,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.FillBounds,
                                                        modifier = Modifier
                                                            .matchParentSize()
                                                            .blur(35.dp)
                                                            .alpha(0.6f)
                                                            .drawWithContent {
                                                                drawContent()
                                                                drawRect(
                                                                    brush = Brush.verticalGradient(
                                                                        colors = listOf(
                                                                            Color.Black.copy(alpha = 0.5f),
                                                                            Color.Transparent
                                                                        ),
                                                                        startY = 0f,
                                                                        endY = size.height * 0.6f
                                                                    ),
                                                                    blendMode = BlendMode.DstIn
                                                                )
                                                            },
                                                        onError = { error ->
                                                            // Log del error sin crashear la app
                                                            Log.w(
                                                                "PlayerBackground",
                                                                "Error loading background image: ${error.result.throwable.message}"
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        TopAppBar(
                                            title = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Image(
                                                        painter = painterResource(R.drawable.opentune),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(27.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = stringResource(R.string.app_name),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            },

                                            actions = {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val context = LocalContext.current

                                                    // Update notification button - only show when version mismatch
                                                    var showUpdateDialog by remember { mutableStateOf(false) }
                                                    var hasUpdateAvailable by remember { mutableStateOf(false) }
                                                    var userDismissedDialog by remember { mutableStateOf(false) }
                                                    
                                                    // Check for updates once on app start only (no polling loop)
                                                    LaunchedEffect(Unit) {
                                                        try {
                                                            val updateConfig = UpdateManager.getUpdateConfig()
                                                            if (updateConfig != null) {
                                                                val currentVersion = UpdateManager.getCurrentAppVersion(this@MainActivity)
                                                                hasUpdateAvailable = UpdateManager.shouldShowUpdateDialog(currentVersion, updateConfig.version_name)
                                                                if (hasUpdateAvailable && !userDismissedDialog) {
                                                                    showUpdateDialog = true
                                                                } else if (!hasUpdateAvailable) {
                                                                    showUpdateDialog = false
                                                                    userDismissedDialog = false
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("UpdateCheck", "Error checking for updates", e)
                                                        }
                                                    }
                                                    
                                                    // Show notification icon only when update is available
                                                    if (hasUpdateAvailable) {
                                                        IconButton(
                                                            onClick = { showUpdateDialog = true }
                                                        ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.notification_off),
                                                            contentDescription = "Update Available",
                                                            tint = ThemeAccent
                                                        )
                                                        }
                                                    }
                                                    
                                                    // Modern Update Dialog Component
                                                    if (showUpdateDialog) {
                                                        androidx.compose.ui.window.Dialog(
                                                            onDismissRequest = { 
                                                                // Only allow dismiss if show_later_button is true
                                                                if (UpdateManager.getUpdateConfig()?.show_later_button == true) {
                                                                    showUpdateDialog = false
                                                                }
                                                            },
                                                            properties = androidx.compose.ui.window.DialogProperties(
                                                                dismissOnBackPress = UpdateManager.getUpdateConfig()?.show_later_button == true,
                                                                dismissOnClickOutside = false,
                                                                usePlatformDefaultWidth = false
                                                            )
                                                        ) {
                                                            androidx.compose.material3.Surface(
                                                                modifier = Modifier
                                                                    .fillMaxWidth(0.95f),
                                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                                                color = Color(0xFF16151B),
                                                                shadowElevation = 16.dp
                                                            ) {
                                                                Column(
                                                                    modifier = Modifier.padding(24.dp),
                                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                                ) {
                                                                    // Get update config first
                                                                    val updateConfig = UpdateManager.getUpdateConfig()
                                                                    
                                                                    // Title with icon
                                                                    Column(
                                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                                    ) {
                                                                        Icon(
                                                                            painter = painterResource(R.drawable.notification_off),
                                                                            contentDescription = null,
                                                                            tint = ThemeAccent,
                                                                            modifier = Modifier.size(32.dp)
                                                                        )
                                                                        Text(
                                                                            text = updateConfig?.title ?: "",
                                                                            style = MaterialTheme.typography.headlineSmall,
                                                                            color = ThemeTextPrimary,
                                                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                                        )
                                                                    }
                                                                    
                                                                    Spacer(modifier = Modifier.height(16.dp))
                                                                    
                                                                    // Message
                                                                    Text(
                                                                        text = updateConfig?.message ?: "",
                                                                        style = MaterialTheme.typography.bodyMedium,
                                                                        color = ThemeTextTertiary,
                                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                                                        lineHeight = 20.sp
                                                                    )
                                                                    
                                                                    Spacer(modifier = Modifier.height(24.dp))
                                                                    
                                                                    // Buttons
                                                                    Row(
                                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                        modifier = Modifier.align(Alignment.End)
                                                                    ) {
                                                                        // Show Later button only if show_later_button is true
                                                                        if (updateConfig?.show_later_button == true) {
                                                                            androidx.compose.material3.TextButton(
                                                                                onClick = { showUpdateDialog = false },
                                                                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                                                                    contentColor = ThemeTextTertiary
                                                                                )
                                                                            ) {
                                                                                Text(
                                                                                    text = updateConfig?.later_text ?: "",
                                                                                    style = MaterialTheme.typography.labelLarge
                                                                                )
                                                                            }
                                                                        }
                                                                        
                                                                        androidx.compose.material3.TextButton(
                                                                            onClick = { 
                                                                                showUpdateDialog = false
                                                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateConfig?.update_link ?: ""))
                                                                                context.startActivity(intent)
                                                                            },
                                                                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                                                                contentColor = ThemeAccent
                                                                            )
                                                                        ) {
                                                                            Text(
                                                                                text = updateConfig?.update_now_text ?: "",
                                                                                style = MaterialTheme.typography.labelLarge
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    IconButton(
                                                        onClick = { onActiveChange(true) }
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.search),
                                                            contentDescription = stringResource(R.string.search),
                                                            tint = ThemeTextPrimary
                                                        )
                                                    }

                                                    // Settings icon only visible on Profile tab
                                                    if (navBackStackEntry?.destination?.route == "library") {
                                                        IconButton(
                                                            onClick = {
                                                                try {
                                                                    navController.navigate("settings")
                                                                } catch (e: Exception) {
                                                                    logErrorToDownloads(context, e)
                                                                    // Error logging only
                                                                }
                                                            }
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.settings),
                                                                contentDescription = stringResource(R.string.settings),
                                                                tint = ThemeTextPrimary
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            scrollBehavior = searchBarScrollBehavior,
                                            colors = TopAppBarDefaults.topAppBarColors(
                                                containerColor = ThemeDark,
                                                scrolledContainerColor = ThemeDark,
                                                titleContentColor = ThemeTextPrimary,
                                                navigationIconContentColor = ThemeTextPrimary,
                                                actionIconContentColor = ThemeTextPrimary
                                            )
                                        )
                                    }
                                }

                                // Safer verification for the route
                                if (active || isSearchRoute) {
                                    TopSearch(
                                        query = query,
                                        onQueryChange = onQueryChange,
                                        onSearch = onSearch,
                                        active = active,
                                        onActiveChange = onActiveChange,
                                        placeholder = {
                                            Text(
                                                text = stringResource(
                                                    when (searchSource) {
                                                        SearchSource.LOCAL -> R.string.search_library
                                                        SearchSource.YOUTUBE -> R.string.search_yt_music
                                                        SearchSource.JIOSAAVN -> R.string.search_yt_music
                                                    }
                                                ),
                                            )
                                        },
                                        leadingIcon = {
                                            val currentRoute = navBackStackEntry?.destination?.route
                                            val isInNavigationItems =
                                                navigationItems.fastAny { it.route == currentRoute }

                                            IconButton(
                                                onClick = {
                                                    when {
                                                        active -> onActiveChange(false)
                                                        !isInNavigationItems -> {
                                                            try {
                                                                navController.navigateUp()
                                                            } catch (e: Exception) {
                                                                Log.e(
                                                                    "Navigation",
                                                                    "Error navigating up",
                                                                    e
                                                                )
                                                            }
                                                        }

                                                        else -> onActiveChange(true)
                                                    }
                                                },
                                                onLongClick = {
                                                    when {
                                                        active -> { /* No action */
                                                        }

                                                        !isInNavigationItems -> {
                                                            try {
                                                                navController.backToMain()
                                                            } catch (e: Exception) {
                                                                Log.e(
                                                                    "Navigation",
                                                                    "Error navigating to main",
                                                                    e
                                                                )
                                                            }
                                                        }

                                                        else -> { /* No action */
                                                        }
                                                    }
                                                },
                                            ) {
                                                Icon(
                                                    painterResource(
                                                        if (active || !isInNavigationItems) {
                                                            R.drawable.arrow_back
                                                        } else {
                                                            R.drawable.search
                                                        }
                                                    ),
                                                    contentDescription = stringResource(
                                                        if (active || !isInNavigationItems) {
                                                            R.string.back
                                                        } else {
                                                            R.string.search
                                                        }
                                                    ),
                                                )
                                            }
                                        },
                                        trailingIcon = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (active) {
                                                    // Voice Search Integration
                                                    var showVoiceDialog by remember { mutableStateOf(false) }
                                                    var voiceSearchState by remember { mutableStateOf(VoiceSearchState.IDLE) }
                                                    val voiceSearchManager = rememberVoiceSearchManager()
                                                    val context = LocalContext.current
                                                    
                                                    // Permission launcher
                                                    val permissionLauncher = rememberLauncherForActivityResult(
                                                        contract = ActivityResultContracts.RequestPermission()
                                                    ) { isGranted ->
                                                        if (isGranted) {
                                                            showVoiceDialog = true
                                                            voiceSearchManager.startListening(
                                                                onResult = { result ->
                                                                    showVoiceDialog = false
                                                                    onQueryChange(TextFieldValue(result, TextRange(result.length)))
                                                                    onSearch(result)
                                                                },
                                                                onStateChange = { state ->
                                                                    voiceSearchState = state
                                                                },
                                                                onError = { error ->
                                                                    showVoiceDialog = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                    
                                                    // Voice search dialog
                                                    if (showVoiceDialog) {
                                                        VoiceSearchDialog(
                                                            state = voiceSearchState,
                                                            onDismiss = {
                                                                showVoiceDialog = false
                                                                voiceSearchManager.cancel()
                                                            }
                                                        )
                                                    }
                                                    
                                                    // Voice search button
                                                    IconButton(
                                                        onClick = {
                                                            if (voiceSearchManager.hasPermission()) {
                                                                showVoiceDialog = true
                                                                voiceSearchManager.startListening(
                                                                    onResult = { result ->
                                                                        showVoiceDialog = false
                                                                        onQueryChange(TextFieldValue(result, TextRange(result.length)))
                                                                        onSearch(result)
                                                                    },
                                                                    onStateChange = { state ->
                                                                        voiceSearchState = state
                                                                    },
                                                                    onError = { error ->
                                                                        showVoiceDialog = false
                                                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                                                    }
                                                                )
                                                            } else {
                                                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.mic),
                                                            contentDescription = "Voice Search",
                                                            tint = ThemeAccent
                                                        )
                                                    }
                                                    
                                                    if (query.text.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = {
                                                                onQueryChange(TextFieldValue(""))
                                                            },
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(R.drawable.close),
                                                                contentDescription = null,
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            searchSource = searchSource.toggle()
                                                        },
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(
                                                                when (searchSource) {
                                                                    SearchSource.LOCAL -> R.drawable.library_music
                                                                    SearchSource.YOUTUBE -> R.drawable.language
                                                                    SearchSource.JIOSAAVN -> R.drawable.language
                                                                }
                                                            ),
                                                            contentDescription = stringResource(
                                                                when (searchSource) {
                                                                    SearchSource.LOCAL -> R.string.search_online
                                                                    SearchSource.YOUTUBE -> R.string.search_library
                                                                    SearchSource.JIOSAAVN -> R.string.search_library
                                                                }
                                                            ),
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .focusRequester(searchBarFocusRequester)
                                            .align(Alignment.TopCenter)
                                            .fillMaxWidth(),
                                        focusRequester = searchBarFocusRequester
                                    ) {
                                        Crossfade(
                                            targetState = searchSource,
                                            label = "search_content_transition",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(
                                                    bottom = if (!playerBottomSheetState.isDismissed) {
                                                        MiniPlayerHeight
                                                    } else {
                                                        0.dp
                                                    }
                                                )
                                                .navigationBarsPadding(),
                                        ) { currentSearchSource ->
                                            when (currentSearchSource) {
                                                SearchSource.LOCAL -> LocalSearchScreen(
                                                    query = query.text,
                                                    navController = navController,
                                                    onDismiss = { onActiveChange(false) },
                                                    pureBlack = pureBlack,
                                                )

                                                SearchSource.YOUTUBE, SearchSource.JIOSAAVN -> OnlineSearchScreen(
                                                    query = query.text,
                                                    onQueryChange = onQueryChange,
                                                    navController = navController,
                                                    onSearch = { searchQuery ->
                                                        try {
                                                            val encodedQuery = URLEncoder.encode(
                                                                searchQuery,
                                                                "UTF-8"
                                                            )
                                                            navController.navigate("search/$encodedQuery")

                                                            // Verificar preferencias antes de guardar historial
                                                            if (dataStore[PauseSearchHistoryKey] != true) {
                                                                database.query {
                                                                    insert(SearchHistory(query = searchQuery))
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e(
                                                                "SearchNavigation",
                                                                "Error navigating to search: ${e.message}",
                                                                e
                                                            )
                                                        }
                                                    },
                                                    onDismiss = { onActiveChange(false) },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        ) { _ ->
                // Main content container
                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(
                                if (isSearchRoute || active) {
                                    searchBarScrollBehavior.nestedScrollConnection
                                } else {
                                    topAppBarScrollBehavior.nestedScrollConnection
                                }
                            )
                            .background(ThemeDark),
                        topBar = {
                            // TopAppBar/Search is handled above in the content Column
                        },
                        // bottomBar moved outside Scaffold to avoid double bottom padding
                    ) { padding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = when (defaultOpenTab) {
                                    NavigationTab.HOME -> Screens.ForYou.route
                                    NavigationTab.EXPLORE -> Screens.Explore.route
                                    NavigationTab.LIBRARY -> Screens.Profile.route
                                },
                            ) {
                                navigationBuilder(
                                    navController,
                                    topAppBarScrollBehavior,
                                    latestVersionName
                                )
                            }
                        }
                    }

                    // Floating Player and other overlays
                    BottomSheetPlayer(
                        state = playerBottomSheetState,
                        navController = navController,
                        onOpenFullscreenLyrics = {
                            showFullscreenLyrics = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (shouldShowNavigationBar) {
                        NavigationBar(
                            containerColor = ThemeDark,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .height(navigationBarHeight)
                        ) {
                            navigationItems.forEach { item ->
                                val isSelected = navBackStackEntry?.destination?.route == item.route
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        if (!isSelected) {
                                            previousTab = navBackStackEntry?.destination?.route
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(if (isSelected) item.iconIdActive else item.iconIdInactive),
                                            contentDescription = stringResource(item.titleId)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(item.titleId),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ThemeAccent,
                                        selectedTextColor = ThemeAccent,
                                        unselectedIconColor = ThemeTextTertiary,
                                        unselectedTextColor = ThemeTextTertiary,
                                        indicatorColor = Color.Transparent
                                    )
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showFullscreenLyrics,
                        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(150)) + fadeIn(animationSpec = tween(150)),
                        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
                    ) {
                        Lyrics(
                            sliderPositionProvider = { null },
                            onNavigateBack = { showFullscreenLyrics = false },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    BottomSheetMenu(
                        state = LocalMenuState.current,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    /*sharedSong?.let { song ->
                        playerConnection?.let {
                            Dialog(
                                onDismissRequest = { sharedSong = null },
                                properties = DialogProperties(usePlatformDefaultWidth = false),
                            ) {
                                SharedSongDialog(
                                    song = song,
                                    playerConnection = it,
                                    onDismiss = { sharedSong = null }
                                )
                            }
                        }
                    }*/

                    LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                        if (shouldShowSearchBar && openSearchImmediately) {
                            onActiveChange(true)
                            try {
                                delay(100)
                                searchBarFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                            openSearchImmediately = false
                        }
                    }
                } // Box at 1280
            } // Scaffold at 740
        } // CompositionLocalProvider
        } // BoxWithConstraints
        } // ThemeBackground
            } // AiMusicTheme
        } // setContent
    } // onCreate

    private fun navigateToScreen(navController: NavHostController, screen: Screens) {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    private fun handlevideoIdIntent(intent: Intent) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        when {
            uri.pathSegments.firstOrNull() == "watch" -> uri.getQueryParameter("v")
            uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
            else -> null
        }?.let { videoId ->
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { YouTube.queue(listOf(videoId)) }.onSuccess {
                    playerConnection?.playQueue(
                        YouTubeQueue(
                            WatchEndpoint(videoId = it.firstOrNull()?.id),
                            it.firstOrNull()?.toMediaMetadata()
                        )
                    )
                }.onFailure { reportException(it) }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        window.navigationBarColor = ThemeDark.toArgb()
    }

    companion object {
        const val ACTION_SEARCH = "com.buxi.music.action.SEARCH"
        const val ACTION_EXPLORE = "com.buxi.music.action.EXPLORE"
        const val ACTION_LIBRARY = "com.buxi.music.action.LIBRARY"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection = staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }

@Composable
fun NotificationPermissionPreference() {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }

    val checkNotificationPermission = remember {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }

    LaunchedEffect(Unit) {
        permissionGranted = checkNotificationPermission()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = checkNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SwitchPreference(
        title = { Text(stringResource(R.string.notification)) },
        icon = {
            Icon(
                painter = painterResource(id = if (permissionGranted) R.drawable.notification_on else R.drawable.notification_off),
                contentDescription = null
            )
        },
        checked = permissionGranted,
        onCheckedChange = { checked ->
            if (checked && !permissionGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openNotificationSettings(context)
                }
            } else if (!checked && permissionGranted) {
                openNotificationSettings(context)
            }
        }
    )
}

@Composable
fun SwitchPreference(
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        color = ThemeDark
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                CompositionLocalProvider(LocalContentColor provides if (enabled) ThemeTextPrimary else ThemeTextPrimary.copy(alpha = 0.38f)) {
                    icon()
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                CompositionLocalProvider(LocalContentColor provides if (enabled) ThemeTextPrimary else ThemeTextPrimary.copy(alpha = 0.38f)) {
                    title()
                }
                subtitle?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    CompositionLocalProvider(LocalContentColor provides if (enabled) ThemeTextTertiary else ThemeTextTertiary.copy(alpha = 0.38f)) {
                        it()
                    }
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/Arturo254/OpenTune/releases/latest")
        val connection = url.openConnection()
        connection.connect()
        val json = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(json)
        return@withContext jsonObject.getString("tag_name")
    } catch (e: Exception) {
        return@withContext null
    }
}

fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    val remote = remoteVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val current = currentVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(remote.size, current.size)) {
        val r = remote.getOrNull(i) ?: 0
        val c = current.getOrNull(i) ?: 0
        if (r > c) return true
        if (r < c) return false
    }
    return false
}

fun logErrorToDownloads(context: Context, e: Exception) {
    val fileName = "error_log_${
        SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss",
            Locale.getDefault()
        ).format(Date())
    }.txt"
    val logText = "Exception: ${e.message}\nStacktrace:\n${e.stackTrace.joinToString("\n")}";
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri =
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return
            resolver.openOutputStream(uri)?.use { it.write(logText.toByteArray()) }
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        } else {
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            File(downloadsDir, fileName).writeBytes(logText.toByteArray())
        }
    } catch (ex: Exception) {
        Log.e("ErrorLog", "Failed to save error log", ex)
    }
}
