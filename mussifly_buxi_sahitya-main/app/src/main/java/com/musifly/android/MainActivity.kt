package com.musifly.android

// StoragePermissionDialog removed - using PermissionScreen instead
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.musifly.android.constants.AppBarHeight
import com.musifly.android.constants.AppTheme
import com.musifly.android.constants.AppThemeKey
import com.musifly.android.constants.CustomPrimaryColorKey
import com.musifly.android.constants.CustomSecondaryColorKey
import com.musifly.android.constants.DarkModeKey
import com.musifly.android.constants.DefaultOpenTabKey
import com.musifly.android.constants.DynamicThemeKey
import com.musifly.android.constants.EnableFloatingDockerKey
import com.musifly.android.constants.FirstLaunchCompletedKey
import com.musifly.android.constants.FloatingNavBarHeightDpKey
import com.musifly.android.constants.FloatingNavBarWidthPercentKey
import com.musifly.android.constants.FullScreenModeKey
import com.musifly.android.constants.LiquidGlassNavBarHeightKey
import com.musifly.android.constants.MiniPlayerHeight
import com.musifly.android.constants.NavigationBarAnimationSpec
import com.musifly.android.constants.NavigationBarHeight
import com.musifly.android.constants.PauseSearchHistoryKey
import com.musifly.android.constants.PlayerBackgroundStyle
import com.musifly.android.constants.PlayerBackgroundStyleKey
import com.musifly.android.constants.PureBlackKey
import com.musifly.android.constants.SearchSource
import com.musifly.android.constants.SearchSourceKey
import com.musifly.android.constants.SlimNavBarKey
import com.musifly.android.constants.StopMusicOnTaskClearKey
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.SearchHistory
import com.musifly.android.extensions.toEnum
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.playback.DownloadUtil
import com.musifly.android.playback.MusicService
import com.musifly.android.playback.MusicService.MusicBinder
import com.musifly.android.playback.PlayerConnection
import com.musifly.android.playback.queues.YouTubeQueue
import com.musifly.android.repository.ProfileRepository
import com.musifly.android.sync.supabase.SupabaseTelemetryService
import com.musifly.android.ui.component.AvatarPreferenceManager
import com.musifly.android.ui.component.AvatarSelection
import com.musifly.android.ui.component.BottomSheetMenu
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.component.LocaleManager
import com.musifly.android.ui.component.Lyrics
import com.musifly.android.ui.component.SwitchPreference
import com.musifly.android.ui.component.TopSearch
import com.musifly.android.ui.component.YouTubeLinkDialog
import com.musifly.android.ui.component.liquidglass.LocalBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.layerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.catalog.components.LiquidBottomNavigation
import com.musifly.android.ui.component.rememberBottomSheetState
import com.musifly.android.ui.component.shimmer.ShimmerTheme
import com.musifly.android.ui.menu.YouTubeSongMenu
import com.musifly.android.ui.player.BottomSheetPlayer
import com.musifly.android.ui.screens.Screens
import com.musifly.android.ui.screens.navigationBuilder
import com.musifly.android.ui.screens.search.LocalSearchScreen
import com.musifly.android.ui.screens.search.OnlineSearchScreen
import com.musifly.android.ui.screens.settings.DarkMode
import com.musifly.android.ui.screens.settings.NavigationTab
import com.musifly.android.ui.theme.ColorSaver
import com.musifly.android.ui.theme.DefaultThemeColor
import com.musifly.android.ui.theme.OpenTuneTheme
import com.musifly.android.ui.theme.extractThemeColor
import com.musifly.android.ui.utils.appBarScrollBehavior
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.ui.utils.resetHeightOffset
import com.musifly.android.utils.SyncUtils
import com.musifly.android.utils.Updater
import com.musifly.android.utils.dataStore
import com.musifly.android.utils.get
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference
import com.musifly.android.utils.rememberVoiceSearchHelper
import com.musifly.android.utils.reportException
import com.musifly.android.utils.update.UpdateManager
import com.musifly.android.viewmodels.NewReleaseViewModel
import com.musifly.android.viewmodels.ProfileViewModel
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.SongItem
import com.musifly.innertube.models.WatchEndpoint
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.days


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

    @Inject
    lateinit var supabaseTelemetryService: SupabaseTelemetryService

    @Inject
    lateinit var profileRepository: ProfileRepository

    private val notificationRedirect = MutableStateFlow<Pair<String, String>?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Timber.tag("MainActivity").d("Notification permission granted")
            syncFcmToken()
        } else {
            Timber.tag("MainActivity").d("Notification permission denied")
        }
    }

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    
    // Track previous notification permission state to detect changes
    private var previousNotificationPermissionState: Boolean? = null
    
    private var isServiceBound = false

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

    private var latestVersionName by mutableStateOf("1.0.0")

    private fun setFullScreen(enable: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (enable) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onStart() {
        super.onStart()
        // Removed: safeStartMusicService(serviceIntent)
        // We only bind the service here. MusicService will promote itself to foreground 
        // automatically when playback actually starts, avoiding the ugly initial notification.
        bindService(
            Intent(this, MusicService::class.java),
            serviceConnection,
            BIND_AUTO_CREATE
        )
        isServiceBound = true
    }

    override fun onResume() {
        super.onResume()
        // Handle notification permission changes when user returns from system settings
        // Only rebind if permission status actually CHANGED (not every time onResume is called)
        val currentPermissionState = notificationPermissionGranted()
        
        // Check if permission state changed
        val permissionChanged = previousNotificationPermissionState != null && 
                                previousNotificationPermissionState != currentPermissionState
        
        // Update stored state
        previousNotificationPermissionState = currentPermissionState
        
        // Only rebind if permission was just granted (changed from false to true)
        if (permissionChanged && currentPermissionState) {
            try {
                Log.d("MainActivity", "🔄 Notification permission just granted (was denied), rebinding playback notification")
                // Access service through playerConnection if available
                playerConnection?.service?.forceRebindPlaybackNotification()
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to rebind notification after permission grant", e)
            }
        } else if (currentPermissionState && previousNotificationPermissionState == null) {
            // First time checking - permission is granted, but don't rebind yet
            // Media3 notification should already be working
            Log.d("MainActivity", "✅ Notification permission granted (first check), Media3 notification should be active")
        }
        
        // Notification checking removed - no longer checking for JSON notifications
        syncFcmToken()
        
        // Update last_seen timestamp for online status tracking
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            lifecycleScope.launch {
                profileRepository.updateLastSeen(userId)
            }
        }

    }

    /**
     * Check if notification permission is granted.
     * For Android 13+ (Tiramisu): checks both POST_NOTIFICATIONS permission and NotificationManager state.
     * For Android 12 and below: checks NotificationManager.areNotificationsEnabled().
     * 
     * @return true if notifications are enabled and permission is granted (if required)
     */
    private fun notificationPermissionGranted(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // First check if notifications are enabled at the system level
        val notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true // Pre-Android 7.0, always enabled
        }
        
        if (!notificationsEnabled) {
            return false
        }
        
        // For Android 13+ (Tiramisu), also check POST_NOTIFICATIONS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            return permissionGranted && notificationsEnabled
        }
        
        // For Android 12 and below, notifications are always allowed if enabled
        return notificationsEnabled
    }

    override fun onStop() {
        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Pause playback when app is being destroyed (user fully closed the app)
        // REMOVED: Unconditional pause prevents background playback
        /*
        if (isFinishing && playerConnection != null) {
            try {
                // Force pause - player should never auto-resume on next app start
                playerConnection?.player?.pause()
                playerConnection?.player?.playWhenReady = false
                Log.d("MainActivity", "🛑 Pausing playback on app destroy")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error pausing playback on destroy", e)
            }
        }
        */
        
        if (dataStore.get(
                StopMusicOnTaskClearKey,
                false
            ) && playerConnection?.isPlaying?.value == true && isFinishing
        ) {
            stopService(Intent(this, MusicService::class.java))
            if (isServiceBound) {
                unbindService(serviceConnection)
                isServiceBound = false
            }
            playerConnection = null
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun syncFcmToken() {
        if (FirebaseAuth.getInstance().currentUser == null) return
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.tag("MainActivity").w(task.exception, "Fetching FCM registration token failed")
                return@addOnCompleteListener
            }
            val token = task.result
            lifecycleScope.launch {
                profileRepository.updateFcmToken(token)
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            LocaleManager.getInstance(newBase).applyLocaleToContext(newBase)
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent) {
        val screen = intent.getStringExtra("screen")
        val actorId = intent.getStringExtra("actor_id") ?: intent.getStringExtra("userId") ?: ""
        if (screen != null) {
            notificationRedirect.value = Pair(screen, actorId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Request Notification Permission and Sync FCM Token
        checkNotificationPermission()
        lifecycleScope.launch {
            delay(2000) // Wait for Firebase and network to be ready
            syncFcmToken()
        }
        handleNotificationIntent(intent)


        lifecycleScope.launch {
            dataStore.data
                .map { it[FullScreenModeKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    setFullScreen(it)
                }
        }

        // Record User Activity for Analytics (Supabase Dashboard)
        lifecycleScope.launch(Dispatchers.IO) {
            val isGuest = com.musifly.android.sync.UserSessionManager.isGuest()
            val userId = if (isGuest) {
                // Use Android ID for guests
                Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown_guest"
            } else {
                com.musifly.android.sync.UserSessionManager.getUserId() ?: "unknown_user"
            }
            supabaseTelemetryService.recordActivity(userId, isGuest)
        }



        setContent {
            LaunchedEffect(Unit) {
                if (System.currentTimeMillis() - Updater.lastCheckTime > 1.days.inWholeMilliseconds) {
                    Updater.getLatestVersionName().onSuccess {
                        latestVersionName = it
                    }
                }
            }

            var showFullscreenLyrics by remember { mutableStateOf(false) }

            // Check if this is first launch and permission status
            val firstLaunchCompleted = rememberPreference(FirstLaunchCompletedKey, defaultValue = false)
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            // Storage permission dialog removed as requested
            
            // Check for first launch flow (Beats-Music style)
            val sharedPrefs = remember { 
                context.getSharedPreferences("musifly_prefs", Context.MODE_PRIVATE) 
            }
            var seenPermission by remember { 
                mutableStateOf(sharedPrefs.getBoolean("seen_permission", false)) 
            }
            var loginSkipped by remember { 
                mutableStateOf(sharedPrefs.getBoolean("login_skipped", false)) 
            }
            
            // Listen for changes in SharedPreferences to update states
            LaunchedEffect(Unit) {
                // Check initial state and update if needed
                seenPermission = sharedPrefs.getBoolean("seen_permission", false)
                loginSkipped = sharedPrefs.getBoolean("login_skipped", false)
                
                // Also set up a listener for changes (via polling or SharedPreferences listener)
                // For now, we'll check on app resume/launch
            }
            
            // Check storage permission status
            val hasStoragePermission = remember {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                        // Android 13+ (API 33+)
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_MEDIA_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_MEDIA_VIDEO
                        ) == PackageManager.PERMISSION_GRANTED
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                        // Android 6-12 (API 23-32)
                        val readGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                        
                        val writeGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            // Android 9 and below - WRITE permission is required
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            // Android 10-12 - WRITE permission not needed with scoped storage
                            true
                        }
                        
                        readGranted && writeGranted
                    }
                    else -> true // Android 5 and below - permissions granted at install
                }
            }
            
            LaunchedEffect(hasStoragePermission) {
                // Storage permission dialog removed - permissions handled by PermissionScreen
                // Mark first launch as completed if permission is granted
                if (hasStoragePermission && !firstLaunchCompleted.value) {
                        scope.launch {
                            context.dataStore.edit {
                                it[FirstLaunchCompletedKey] = true
                            }
                        }
                }
            }

            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = false)
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
            val appTheme by rememberEnumPreference(AppThemeKey, defaultValue = AppTheme.DEFAULT)
            val themeMode by rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)

            val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            
            // Theme mode determines the actual dark/light state
            val useDarkTheme = remember(themeMode, darkTheme, isSystemInDarkTheme) {
                when (themeMode) {
                    ThemeMode.LIGHT -> false
                    else -> {
                        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
                    }
                }
            }
            LaunchedEffect(useDarkTheme, themeMode) {
                setSystemBarAppearance(useDarkTheme, themeMode)
            }
            
            // Initialize update system (start periodic worker)
            UpdateManager.InitializeUpdateSystem()
            
            // Notification checking removed - no longer checking for JSON notifications
            
            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme, appTheme) {
                val playerConnection = playerConnection
                // Don't use dynamic theme if Flutter Music theme is selected
                if (!enableDynamicTheme || playerConnection == null || appTheme == AppTheme.FLUTTER_MUSIC) {
                    themeColor = if (appTheme == AppTheme.FLUTTER_MUSIC) {
                        com.musifly.android.ui.theme.FlutterMusicAccent2
                    } else {
                        DefaultThemeColor
                    }
                    return@LaunchedEffect
                }
                playerConnection.service.currentMediaMetadata.collectLatest { song ->
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
                                    ?: DefaultThemeColor
                            }
                        } else {
                            DefaultThemeColor
                        }
                }
            }

            val customPrimaryColorInt by rememberPreference(CustomPrimaryColorKey, defaultValue = DefaultThemeColor.toArgb())
            val customSecondaryColorInt by rememberPreference(CustomSecondaryColorKey, defaultValue = 0)

            OpenTuneTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = themeColor,
                customPrimaryColor = Color(customPrimaryColorInt),
                customSecondaryColor = if (customSecondaryColorInt != 0) Color(customSecondaryColorInt) else null,
                appTheme = appTheme,
                themeMode = themeMode,
            ) {
                // Splash Screen Logic
                // Shows for a brief moment then reveals app
                var showSplash by remember { mutableStateOf(true) }
                
                LaunchedEffect(Unit) {
                    delay(1200) // Reduced from 2500ms for faster start
                    showSplash = false
                }

                if (showSplash) {
                    com.musifly.android.ui.screens.SplashScreen()
                } else {
                    // Check for updates and show dialog if available
                    // MUST be inside OpenTuneTheme to get proper theme colors (dark/light theme support)
                    // Set forceShow=true for testing - shows dialog even if version was already notified
                    // Set checkOnResume=true to re-check when app resumes (useful for testing JSON changes)
                    // For production, set both to false
                    UpdateManager.CheckForUpdateDialog(
                        forceShow = false,  // Set to true to force show dialog for testing (bypasses "already notified" check)
                        checkOnResume = false  // Re-check when app resumes (helps test JSON changes)
                    )
                    
                    BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                    val bottomInsetDp = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()


                    val navController = rememberNavController()

                    LaunchedEffect(navController) {
                        notificationRedirect.collectLatest { redirect ->
                            if (redirect != null) {
                                val (screen, id) = redirect
                                when (screen) {
                                    "profile" -> navController.navigate("profile/$id")
                                    "notifications" -> navController.navigate("notifications")
                                }
                                notificationRedirect.value = null
                            }
                        }
                    }

                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val (previousTab) = rememberSaveable { mutableStateOf("home") }

                    val backdrop = rememberLayerBackdrop()

                    val navigationItems = remember { Screens.MainScreens }
                    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = true)
                    val defaultOpenTab =
                        remember {
                            dataStore[DefaultOpenTabKey].toEnum(defaultValue = NavigationTab.HOME)
                        }
                    val tabOpenedFromShortcut =
                        remember {
                            when (intent?.action) {
                                ACTION_LIBRARY -> NavigationTab.LIBRARY
                                ACTION_EXPLORE -> NavigationTab.EXPLORE
                                else -> null
                            }
                        }

                    val topLevelScreens =
                        listOf(
                            Screens.Home.route,
                            Screens.Explore.route,
                            Screens.Library.route,
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

                    val (searchSource, onSearchSourceChange) = rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)
                    var showGuestSearchDialog by remember { mutableStateOf(false) }

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
                        mutableStateOf(intent?.action == ACTION_SEARCH)
                    }

                    val shouldShowSearchBar =
                        remember(active, navBackStackEntry) {
                            active ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true
                        }

                    val shouldShowNavigationBar =
                        remember(navBackStackEntry, active) {
                            val route = navBackStackEntry?.destination?.route
                            
                            (route == null ||
                                    navigationItems.fastAny { it.route == route }) &&
                                    !active
                        }

                    val (enableDocker) = rememberPreference(
                        EnableFloatingDockerKey,
                        defaultValue = true
                    )
                    
                    val (navWidthPercent) = rememberPreference(
                        FloatingNavBarWidthPercentKey,
                        defaultValue = 1.0f
                    )
                    
                    val (navHeightDp) = rememberPreference(
                        FloatingNavBarHeightDpKey,
                        defaultValue = 64f // Default shorter height for floating bar
                    )

                    val (isFullScreen) = rememberPreference(
                        FullScreenModeKey,
                        defaultValue = false
                    )

                    val (liquidNavBarHeightPref) = rememberPreference(
                        LiquidGlassNavBarHeightKey,
                        defaultValue = 64f
                    )
                    
                    val navigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar) {
                            val extraPadding = if (isFullScreen) 16.dp else 0.dp
                            if (themeMode == ThemeMode.LIQUID_GLASS) {
                                liquidNavBarHeightPref.dp + 24.dp + extraPadding
                            } else {
                                if (enableDocker) {
                                    navHeightDp.dp + 20.dp + extraPadding
                                } else {
                                    NavigationBarHeight + extraPadding
                                }
                            }
                        } else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = "",
                    )

                    val playerBottomSheetState =
                        rememberBottomSheetState(
                            dismissedBound = 0.dp,
                            collapsedBound = bottomInset + navigationBarHeight + MiniPlayerHeight,
                            expandedBound = maxHeight,
                        )

                    val profileViewModel = hiltViewModel<ProfileViewModel>()

                    val playerAwareWindowInsets =
                        remember(
                            bottomInset,
                            navigationBarHeight, // Changed from shouldShowNavigationBar check to value
                            playerBottomSheetState.isDismissed,
                            navBackStackEntry
                        ) {
                            var bottom = bottomInset
                            bottom = (bottom + navigationBarHeight).coerceAtLeast(0.dp) // Always add the calculated height
                            
                            // Check if player is hidden on this screen (Settings & Profile)
                            val currentRoute = navBackStackEntry?.destination?.route
                            val isPlayerHidden = currentRoute?.startsWith("settings") == true || currentRoute == Screens.Profile.route
                            
                            if (!playerBottomSheetState.isDismissed && !isPlayerHidden) {
                                bottom = (bottom + MiniPlayerHeight).coerceAtLeast(0.dp)
                            }
                            
                            windowsInsets
                                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                                .add(WindowInsets(top = AppBarHeight.coerceAtLeast(0.dp), bottom = bottom.coerceAtLeast(0.dp)))
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

                    // CRITICAL FIX: Watch mediaMetadata flow to show mini-player
                    // This handles async restoration where metadata arrives AFTER UI init
                    LaunchedEffect(playerConnection) {
                        playerConnection?.mediaMetadata?.collect { metadata ->
                            if (metadata != null) {
                                Log.d("MainActivity", "🎵 Metadata update: ${metadata.title} - Ensuring MiniPlayer visible")
                                if (playerBottomSheetState.isDismissed) {
                                    Log.d("MainActivity", "   • MiniPlayer was dismissed -> Collapsing soft to show")
                                    playerBottomSheetState.collapseSoft()
                                }
                            } else {
                                Log.d("MainActivity", "∅ Metadata null")
                                // Optional: Dismiss if truly no media, but be careful of transient nulls
                                // if (!playerBottomSheetState.isDismissed) {
                                //    playerBottomSheetState.dismiss()
                                // }
                            }
                        }
                    }

                    // Keep generic listener for other events if needed, but the Flow above handles visibility best
                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player = playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener = object : Player.Listener {
                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                if (mediaItem != null && playerBottomSheetState.isDismissed) {
                                    Log.d("MainActivity", "🎵 Media transition event -> Showing MiniPlayer")
                                    playerBottomSheetState.collapseSoft()
                                }
                            }
                            
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                // Ensure visible on PLAYING/PAUSED state if we have items
                                if (player.mediaItemCount > 0 && playerBottomSheetState.isDismissed) {
                                     playerBottomSheetState.collapseSoft()
                                }
                            }
                        }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }
                    
                    LaunchedEffect(navBackStackEntry, active, themeMode) {
                        val route = navBackStackEntry?.destination?.route
                        shouldShowTopBar =
                            !active && 
                            route in topLevelScreens && 
                            route != "settings" && 
                            route != Screens.Library.route &&
                            route != Screens.Profile.route &&
                            !(themeMode == ThemeMode.LIQUID_GLASS && route == Screens.Home.route)
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember {
                        mutableStateOf(null)
                    }
                    
                    // YouTube link download dialog state
                    var showYouTubeLinkDialog by remember { mutableStateOf(false) }
                    var youtubeLinkUrl by remember { mutableStateOf("") }

                    // Unified intent handler
                    val handleIntent = remember {
                        { intent: Intent ->
                            Log.d("MainActivity", "Handling intent: action=${intent.action}, data=${intent.data}, extras=${intent.extras?.getString(Intent.EXTRA_TEXT)?.take(100)}")
                            
                            // Handle route navigation from extras
                            intent.getStringExtra("route")?.let { route ->
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                return@remember
                            }
                            
                            // Extract URL from shared text - handle cases where text contains more than just URL
                            val uri = intent.data ?: run {
                                val sharedText = intent.extras?.getString(Intent.EXTRA_TEXT)
                                if (sharedText != null) {
                                    // Try to extract YouTube URL from shared text using regex
                                    // Matches: youtube.com/watch?v=ID, youtube.com/shorts/ID, youtu.be/ID, music.youtube.com/watch?v=ID
                                    val youtubeUrlPattern = Regex(
                                        """(https?://)?(www\.|m\.)?(youtube\.com|youtu\.be|music\.youtube\.com)(/shorts/[a-zA-Z0-9_-]{11}|/watch\?v=[a-zA-Z0-9_-]{11}|/[a-zA-Z0-9_-]{11})(\S*)""",
                                        RegexOption.IGNORE_CASE
                                    )
                                    val matchResult = youtubeUrlPattern.find(sharedText)
                                    if (matchResult != null) {
                                        // Build proper URL with https:// scheme
                                        var extractedUrl = matchResult.value.trim()
                                        // Ensure URL has proper scheme
                                        if (!extractedUrl.startsWith("http://") && !extractedUrl.startsWith("https://")) {
                                            extractedUrl = "https://$extractedUrl"
                                        }
                                        // Remove any trailing characters that aren't part of URL
                                        extractedUrl = extractedUrl.split(Regex("[\\s\\n\\r]"))[0]
                                        try {
                                            extractedUrl.toUri()
                                        } catch (e: Exception) {
                                            Log.w("MainActivity", "Failed to parse extracted URL: $extractedUrl", e)
                                            null
                                        }
                                    } else {
                                        // No YouTube URL pattern found, try parsing the whole text as URL
                                        try {
                                            val trimmed = sharedText.trim()
                                            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                                                trimmed.toUri()
                                            } else {
                                                null
                                            }
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                } else null
                            }
                            
                            if (uri != null) {
                                // Check if this is a SEND intent (share from another app)
                                val isSharedLink = intent.action == Intent.ACTION_SEND
                                
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

                                    else -> {
                                        // Extract video ID from various YouTube URL formats
                                        val videoId = when {
                                            path == "watch" -> uri.getQueryParameter("v")
                                            path == "shorts" -> uri.lastPathSegment  // YouTube Shorts: /shorts/VIDEO_ID
                                            uri.host == "youtu.be" -> path  // Short URLs: youtu.be/VIDEO_ID
                                            uri.host?.contains("youtube") == true && path?.length == 11 -> path // Direct video ID path
                                            else -> null
                                        }
                                        
                                        Log.d("MainActivity", "Parsed YouTube URL - host: ${uri.host}, path: $path, videoId: $videoId, uri: $uri")
                                        
                                        videoId?.let { id ->
                                            // If link is shared from another app, show download dialog
                                            if (isSharedLink) {
                                                youtubeLinkUrl = uri.toString()
                                                showYouTubeLinkDialog = true
                                                Log.d("MainActivity", "Opening download dialog for: $youtubeLinkUrl")
                                            } else {
                                                // If opened directly in app, play it
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
                                }
                            }
                        }
                    }

                    // Handle initial intent (Cold Start)
                    LaunchedEffect(Unit) {
                        intent?.let { handleIntent(it) }
                    }
                    
                    // Handle new intents (App already running)
                    DisposableEffect(Unit) {
                        val listener = Consumer<Intent> { intent -> handleIntent(intent) }
                        addOnNewIntentListener(listener)
                        onDispose { removeOnNewIntentListener(listener) }
                    }

                    val currentTitle = remember(navBackStackEntry) {
                        when (navBackStackEntry?.destination?.route) {
                            Screens.Home.route -> R.string.home
                            Screens.Explore.route -> R.string.explore
                            Screens.Library.route -> R.string.filter_library
                            else -> null
                        }
                    }
                    val baseBg = if (pureBlack && useDarkTheme) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                    val insetBg = if (playerBottomSheetState.progress > 0f) Color.Transparent else baseBg

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils,
                        LocalBackdrop provides backdrop,
                    ) {
                        val configuration = LocalConfiguration.current
                        val isTabletLandscape = configuration.screenWidthDp >= 600 &&
                                configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                        val shouldShowBottomNav = shouldShowNavigationBar && !isTabletLandscape && playerBottomSheetState.value <= playerBottomSheetState.collapsedBound

                        Box(modifier = Modifier.fillMaxSize()) {
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
                                                .then(
                                                    Modifier.background(MaterialTheme.colorScheme.background)
                                                )
                                        )

                                        // Validación más segura para el background
                                        val safeSelectedValue = when {
                                            playerBackground == PlayerBackgroundStyle.BLUR &&
                                                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                                                PlayerBackgroundStyle.DEFAULT // Sin blur en versiones < Android 12 (S)
                                            }

                                            else -> playerBackground
                                        }

                                        // Solo mostrar blur si safeSelectedValue es BLUR
                                        if (safeSelectedValue == PlayerBackgroundStyle.BLUR) {
                                            val playerConnection = LocalPlayerConnection.current

                                            // Verificación más segura del playerConnection
                                            playerConnection?.let { connection ->
                                                val mediaMetadata by connection.mediaMetadata.collectAsState()

                                                mediaMetadata?.thumbnailUrl?.let { imageUrl ->
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(imageUrl)
                                                            .allowHardware(false)
                                                            .build(),
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
                                                        painter = painterResource(R.drawable.sahitya_h),
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
                                                    val newReleaseViewModel: NewReleaseViewModel = hiltViewModel()
                                                    val hasNewReleases by newReleaseViewModel.hasNewReleases.collectAsState()
                                                    
                                                    // Profile ViewModel for user notifications
                                                    val profileViewModel: ProfileViewModel = hiltViewModel()
                                                    val hasUnreadNotifications by profileViewModel.hasUnreadNotifications.collectAsState()
                                                    
                                                    // Refresh notifications whenever we are on a main screen to stay updated
                                                    val currentRoute = navBackStackEntry?.destination?.route
                                                    LaunchedEffect(currentRoute) {
                                                        if (FirebaseAuth.getInstance().currentUser != null && 
                                                            currentRoute in listOf(Screens.Home.route, Screens.Explore.route, Screens.Library.route)
                                                        ) {
                                                            profileViewModel.loadNotifications()
                                                        }
                                                    }

                                                    // User Notifications Button (Follow notifications) - Hide on Explore screen
                                                    if (navBackStackEntry?.destination?.route != Screens.Explore.route) {
                                                        Box {
                                                            if (themeMode == ThemeMode.LIQUID_GLASS) {
                                                                // Liquid Glass circular background button
                                                                val userNotifBackdrop = rememberLayerBackdrop()
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(48.dp)
                                                                        .drawBackdrop(
                                                                            backdrop = userNotifBackdrop,
                                                                            shape = { CircleShape },
                                                                            effects = {
                                                                                vibrancy()
                                                                                lens(35.dp.toPx(), 70.dp.toPx(), depthEffect = true, chromaticAberration = true)
                                                                            },
                                                                            highlight = { Highlight.Plain },
                                                                            onDrawSurface = {
                                                                                drawRect(Color.White.copy(alpha = 0.12f))
                                                                            }
                                                                        )
                                                                        .clickable {
                                                                            navController.navigate("notifications")
                                                                        },
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        painter = painterResource(R.drawable.sahitya_notification),
                                                                        contentDescription = "User Notifications",
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(24.dp)
                                                                    )
                                                                }
                                                            } else {
                                                                // Standard Material IconButton
                                                                IconButton(
                                                                    onClick = {
                                                                        navController.navigate("notifications")
                                                                    }
                                                                ) {
                                                                    Icon(
                                                                        painter = painterResource(R.drawable.sahitya_notification),
                                                                        contentDescription = "User Notifications",
                                                                        tint = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                }
                                                            }

                                                            // Badge for unread notifications
                                                            val unreadCount by profileViewModel.unreadNotificationsCount.collectAsState()
                                                            if (unreadCount > 0) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .align(Alignment.TopEnd)
                                                                        .offset(x = 4.dp, y = (-4).dp)
                                                                        .size(18.dp)
                                                                        .clip(CircleShape)
                                                                        .background(Color.Red),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                                                        color = Color.White,
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        fontWeight = FontWeight.ExtraBold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Voice search helper
                                                    val voiceSearchHelper = rememberVoiceSearchHelper()
                                                    
                                                    // Get permission error string
                                                    val permissionErrorString = stringResource(R.string.voice_search_error_permissions)
                                                    
                                                    // Permission launcher for microphone
                                                    val requestPermissionLauncher = rememberLauncherForActivityResult(
                                                        contract = ActivityResultContracts.RequestPermission()
                                                    ) { isGranted ->
                                                        if (isGranted) {
                                                            // Start voice search
                                                            voiceSearchHelper.startVoiceSearch(
                                                                onResult = { result ->
                                                                    onQueryChange(TextFieldValue(result, TextRange(result.length)))
                                                                    onActiveChange(true)
                                                                    onSearch(result)
                                                                },
                                                                onError = { error ->
                                                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                        } else {
                                                            Toast.makeText(
                                                                context,
                                                                permissionErrorString,
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                    
                                                    // Search icon removed as requested




                                                }
                                            },
                                            // scrollBehavior = searchBarScrollBehavior,  // Removed to keep header fixed
                                            colors = TopAppBarDefaults.topAppBarColors(
                                                containerColor = Color.Transparent,
                                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                } else {
                                    // Toolbar for backing (Settings, Library, etc.)
                                    val route = navBackStackEntry?.destination?.route
                                    val isSettings = route == "settings"
                                    val isLibrary = route == Screens.Library.route
                                    
                                    if (isSettings || isLibrary) {
                                        val title = if (isSettings) stringResource(R.string.settings) 
                                                    else stringResource(R.string.filter_library)
                                        
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .matchParentSize()
                                                    .background(MaterialTheme.colorScheme.background)
                                            )
                                            TopAppBar(
                                                title = { 
                                                    Text(
                                                        text = title,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onSurface // Ensure readable title
                                                    ) 
                                                },
                                                navigationIcon = {
                                                    IconButton(onClick = { navController.navigateUp() }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.arrow_back),
                                                            contentDescription = "Back",
                                                            tint = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                },
                                                colors = TopAppBarDefaults.topAppBarColors(
                                                    containerColor = Color.Transparent
                                                )
                                            )
                                        }
                                    }
                                }

                                // Verificación más segura para la ruta
                                val isSearchRoute =
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true

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
                                                        SearchSource.ONLINE -> R.string.search_yt_music
                                                        SearchSource.SAAVN -> R.string.search_jiosaavn
                                                        SearchSource.USERS -> R.string.search_users
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
                                            // Voice search helper for search bar
                                            val voiceSearchHelperInBar = rememberVoiceSearchHelper()
                                            val contextInBar = LocalContext.current
                                            
                                            // Get permission error string
                                            val permissionErrorStringInBar = stringResource(R.string.voice_search_error_permissions)
                                            
                                            // Permission launcher for microphone in search bar
                                            val requestPermissionLauncherInBar = rememberLauncherForActivityResult(
                                                contract = ActivityResultContracts.RequestPermission()
                                            ) { isGranted ->
                                                if (isGranted) {
                                                    voiceSearchHelperInBar.startVoiceSearch(
                                                        onResult = { result ->
                                                            onQueryChange(TextFieldValue(result, TextRange(result.length)))
                                                            onSearch(result)
                                                        },
                                                        onError = { error ->
                                                            Toast.makeText(contextInBar, error, Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                } else {
                                                    Toast.makeText(
                                                        contextInBar,
                                                        permissionErrorStringInBar,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                            
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (active) {
                                                    // Voice search button
                                                    IconButton(
                                                        onClick = {
                                                            if (ContextCompat.checkSelfPermission(
                                                                    contextInBar,
                                                                    Manifest.permission.RECORD_AUDIO
                                                                ) == PackageManager.PERMISSION_GRANTED
                                                            ) {
                                                                voiceSearchHelperInBar.startVoiceSearch(
                                                                    onResult = { result ->
                                                                        onQueryChange(TextFieldValue(result, TextRange(result.length)))
                                                                        onSearch(result)
                                                                    },
                                                                    onError = { error ->
                                                                        Toast.makeText(contextInBar, error, Toast.LENGTH_SHORT).show()
                                                                    }
                                                                )
                                                            } else {
                                                                requestPermissionLauncherInBar.launch(Manifest.permission.RECORD_AUDIO)
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.sahitya_mic),
                                                            contentDescription = stringResource(R.string.voice_search),
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    
                                                    // User Search Button
                                                    IconButton(
                                                        onClick = {
                                                            if (searchSource == SearchSource.USERS) {
                                                                onSearchSourceChange(SearchSource.ONLINE)
                                                            } else {
                                                                if (FirebaseAuth.getInstance().currentUser == null) {
                                                                    showGuestSearchDialog = true
                                                                } else {
                                                                    onSearchSourceChange(SearchSource.USERS)
                                                                }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = if (searchSource == SearchSource.USERS) Icons.Default.MusicNote else Icons.Default.PersonSearch, 
                                                            contentDescription = if (searchSource == SearchSource.USERS) "Search Music" else "Search Users",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(24.dp)
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
                                                    // Removed Source Toggle Icon
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
                                                    pureBlack = pureBlack && useDarkTheme,
                                                )

                                                SearchSource.SAAVN,
                                                SearchSource.ONLINE -> OnlineSearchScreen(
                                                    query = query.text,
                                                    onQueryChange = onQueryChange,
                                                    navController = navController,
                                                    onSearch = { queryStr ->
                                                        try {
                                                            val encodedQuery = java.net.URLEncoder.encode(queryStr, "UTF-8")
                                                            navController.navigate("search/${currentSearchSource.name}/$encodedQuery")
                                                            onActiveChange(false)
                                                            if (dataStore[PauseSearchHistoryKey] != true) {
                                                                database.query {
                                                                    insert(SearchHistory(query = queryStr))
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("SearchNavigation", "Error navigating: ${e.message}", e)
                                                        }
                                                    },
                                                    onDismiss = { onActiveChange(false) },
                                                )

                                                SearchSource.USERS -> com.musifly.android.ui.screens.search.UserSearchScreen(
                                                    query = query.text,
                                                    navController = navController,
                                                    onDismiss = { onActiveChange(false) }
                                                )
                                            }
                                        }
                                    }
                                }
                            },



                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
                                    .then(
                                        if (themeMode != ThemeMode.LIQUID_GLASS)
                                            Modifier.background(MaterialTheme.colorScheme.surface)
                                        else Modifier.layerBackdrop(backdrop)
                                    )
                        ) {
                            var transitionDirection =
                                AnimatedContentTransitionScope.SlideDirection.Left

                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                if (navigationItems.fastAny { it.route == previousTab }) {
                                    val curIndex = navigationItems.indexOf(
                                        navigationItems.fastFirstOrNull {
                                            it.route == navBackStackEntry?.destination?.route
                                        }
                                    )

                                    val prevIndex = navigationItems.indexOf(
                                        navigationItems.fastFirstOrNull {
                                            it.route == previousTab
                                        }
                                    )

                                    if (prevIndex > curIndex)
                                        AnimatedContentTransitionScope.SlideDirection.Right.also {
                                            transitionDirection = it
                                        }
                                }
                            }

                            // Check Firebase authentication state
                            val authService = remember { 
                                com.musifly.android.auth.AuthService.getInstance(this@MainActivity) 
                            }
                            var isUserLoggedIn by remember { mutableStateOf(authService.isSignedIn()) }
                            
                            // CRITICAL: Mandatory Username Check
                            LaunchedEffect(isUserLoggedIn) {
                                if (isUserLoggedIn) {
                                    val currentUser = FirebaseAuth.getInstance().currentUser
                                    if (currentUser != null) {
                                        try {
                                            // 1. Check local cache FIRST (instant)
                                            val cachedProfile = profileRepository.getCachedProfile(currentUser.uid)
                                            if (cachedProfile != null && !cachedProfile.username.isNullOrBlank()) {
                                                Timber.d("Mandatory Username Check: Local cache hit, username exists.")
                                                return@LaunchedEffect
                                            }

                                            // 2. Only check network if not found in cache or missing username
                                            // Delay slightly to ensure NavHost has set the graph
                                            delay(500) 
                                            val profile = profileRepository.getProfile(currentUser.uid)
                                            if (profile == null || profile.username.isNullOrBlank()) {
                                                Timber.d("Mandatory Username Check: Missing username in Supabase, redirecting to creation")
                                                navController.navigate(Screens.UsernameCreation.route) {
                                                    popUpTo(navController.graph.startDestinationId) {
                                                        inclusive = true
                                                    }
                                                    launchSingleTop = true
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Timber.e(e, "Failed to check mandatory username")
                                        }
                                    }
                                }
                            }
                            
                            // Update login state when seenPermission, loginSkipped changes
                            LaunchedEffect(seenPermission, loginSkipped) {
                                isUserLoggedIn = authService.isSignedIn()
                            }
                            
                            // Also refresh login state when returning from login screen
                            LaunchedEffect(navBackStackEntry) {
                                val currentIsLoggedIn = authService.isSignedIn()
                                if (currentIsLoggedIn != isUserLoggedIn) {
                                    isUserLoggedIn = currentIsLoggedIn
                                }
                            }
                            
                            // Determine start destination based on first launch flow (Beats-Music style)
                            // CRITICAL: Show login screen if user hasn't logged in AND hasn't skipped (guest mode)
                            // Allow guest mode (loginSkipped = true) to access main app
                            // CRITICAL: Calculate only once on first composition - never recalculate
                            // This prevents login screen from disappearing when user manually navigates there
                            val initialSeenPermission = remember { seenPermission }
                            val initialLoginSkipped = remember { loginSkipped }
                            val initialIsUserLoggedIn = remember { isUserLoggedIn }
                            val initialTabOpenedFromShortcut = remember { tabOpenedFromShortcut }
                            val initialDefaultOpenTab = remember { defaultOpenTab }
                            
                            val initialDestination = remember {
                                // Calculate only once - never recalculate even if state changes
                                when {
                                    !initialSeenPermission -> "permission" // Show permission screen first
                                    // Show login screen only if user hasn't logged in AND hasn't skipped (guest mode)
                                    !initialLoginSkipped && !initialIsUserLoggedIn -> "google_login" // Show login screen
                                    else -> when (initialTabOpenedFromShortcut ?: initialDefaultOpenTab) {
                                        NavigationTab.HOME -> Screens.Home.route
                                        NavigationTab.EXPLORE -> Screens.Explore.route
                                        NavigationTab.LIBRARY -> Screens.Library.route
                                        }
                                }
                            }
                            
                            // CRITICAL: Prevent automatic navigation away from login screen if user manually navigated there
                            // If user is on login screen (whether guest or not), keep them there until they login
                            // This allows guest users to login if they want to
                            val currentRoute = navBackStackEntry?.destination?.route
                            val isOnLoginScreen = currentRoute == "google_login" || currentRoute == "login"
                            
                            LaunchedEffect(isOnLoginScreen, isUserLoggedIn) {
                                // If user manually navigated to login screen, keep them there until they login
                                // Don't navigate away even if they're in guest mode - they should be able to login
                                if (isOnLoginScreen && !isUserLoggedIn) {
                                    // User is on login screen and not logged in
                                    // Don't allow automatic navigation away - they must login
                                    // This applies to both regular users and guest users who want to login
                                }
                            }

                            NavHost(
                                navController = navController,
                                startDestination = initialDestination,

                                enterTransition = {
                                    if (initialState.destination.route in topLevelScreens &&
                                        targetState.destination.route in topLevelScreens
                                    ) {
                                        fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                    } else {
                                        fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                                                slideInHorizontally(
                                                    initialOffsetX = { it },
                                                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                                                )
                                    }
                                },

                                exitTransition = {
                                    if (initialState.destination.route in topLevelScreens &&
                                        targetState.destination.route in topLevelScreens
                                    ) {
                                        fadeOut(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                    } else {
                                        fadeOut(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                                                slideOutHorizontally(
                                                    targetOffsetX = { -it / 5 },
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                    }
                                },

                                popEnterTransition = {
                                    if ((initialState.destination.route in topLevelScreens ||
                                                initialState.destination.route?.startsWith("search/") == true) &&
                                        targetState.destination.route in topLevelScreens
                                    ) {
                                        fadeIn(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                    } else {
                                        fadeIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) +
                                                slideInHorizontally(
                                                    initialOffsetX = { -it },
                                                    animationSpec = spring(stiffness = Spring.StiffnessLow)
                                                )
                                    }
                                },

                                popExitTransition = {
                                    if ((initialState.destination.route in topLevelScreens ||
                                                initialState.destination.route?.startsWith("search/") == true) &&
                                        targetState.destination.route in topLevelScreens
                                    ) {
                                        fadeOut(spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                    } else {
                                        fadeOut(spring(dampingRatio = Spring.DampingRatioLowBouncy)) +
                                                slideOutHorizontally(
                                                    targetOffsetX = { it / 5 },
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                                )
                                    }
                                },

                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .nestedScroll(
                                        if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                            navBackStackEntry?.destination?.route?.startsWith("search/") == true
                                        ) {
                                            searchBarScrollBehavior.nestedScrollConnection
                                        } else {
                                            topAppBarScrollBehavior.nestedScrollConnection
                                        }
                                    ),
                            ) {
                                navigationBuilder(
                                    navController,
                                    topAppBarScrollBehavior,
                                    latestVersionName
                                )
                            }
                        }
                    }

                        // Show/Hide Logic
                        // variables defined at top of scope (line 1116)

                         // PLAYER & LYRICS (Rendered FIRST so Nav Bar is on top)
                        Box {
                            // Hide player on Settings (all sub-screens) and Profile screens
                            val currentRoute = navBackStackEntry?.destination?.route
                            val hidePlayer = currentRoute?.startsWith("settings") == true || currentRoute == Screens.Profile.route

                            if (!hidePlayer) {
                                BottomSheetPlayer(
                                    state = playerBottomSheetState,
                                    navController = navController,
                                    backdrop = backdrop,
                                    onOpenFullscreenLyrics = {
                                        showFullscreenLyrics = true
                                    },
                                    onActivateSearch = {
                                        // Activate search like bottom search icon
                                        coroutineScope.launch {
                                            playerBottomSheetState.collapseSoft()
                                        }
                                        onActiveChange(true)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            AnimatedVisibility(
                                visible = showFullscreenLyrics,
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(300)
                                ) + fadeIn(animationSpec = tween(300)),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(300)
                                ) + fadeOut(animationSpec = tween(300))
                            ) {
                                Lyrics(
                                    sliderPositionProvider = {
                                        // Obtener posición del slider desde el player connection
                                        null // o la lógica que tengas para obtener la posición del slider
                                    },
                                    onNavigateBack = {
                                        showFullscreenLyrics = false
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        if (shouldShowBottomNav) {
                            // Get ViewModel for notification badge
                            val viewModel: NewReleaseViewModel = hiltViewModel()
                            val hasNewReleases by viewModel.hasNewReleases.collectAsState()
                            val context = LocalContext.current
                            
                            // ═══════════════════════════════════════════════════════
                            // NAVIGATION BAR (Standard or Liquid Glass)
                            // ═══════════════════════════════════════════════════════

                            var lastTapTime by remember { mutableLongStateOf(0L) }
                            var lastTappedIcon by remember { mutableStateOf<Int?>(null) }
                            var navigateToExplore by remember { mutableStateOf(false) }
                            var exploreTapCount by remember { mutableIntStateOf(0) }

                            val onScreenClick: (Screens) -> Unit = { screen ->
                                val currentTapTime = System.currentTimeMillis()
                                val timeSinceLastTap = currentTapTime - lastTapTime

                                lastTapTime = currentTapTime

                                if (screen.titleId == R.string.explore) {
                                    // Directly open search bar on Explore tap
                                    onActiveChange(true)
                                    // Focus on search bar
                                    try {
                                        searchBarFocusRequester.requestFocus()
                                    } catch (e: Exception) {
                                        Log.e("SearchBar", "Error focusing search bar", e)
                                    }
                                } else {
                                    exploreTapCount = 0
                                    lastTappedIcon = screen.titleId
                                    val isSelected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true
                                    if (isSelected) {
                                        // Scroll to top en la pantalla actual
                                        navController.currentBackStackEntry?.savedStateHandle?.set(
                                            "scrollToTop",
                                            true
                                        )
                                        coroutineScope.launch {
                                            try {
                                                searchBarScrollBehavior.state.resetHeightOffset()
                                            } catch (e: Exception) {
                                                Log.e(
                                                    "ScrollBehavior",
                                                    "Error resetting scroll",
                                                    e
                                                )
                                            }
                                        }
                                    } else {
                                        try {
                                            navigateToScreen(
                                                navController,
                                                screen
                                            )
                                        } catch (e: Exception) {
                                            Log.e(
                                                "Navigation",
                                                "Error navigating to screen",
                                                e
                                            )
                                        }
                                    }
                                }
                            }

                            if (themeMode == ThemeMode.LIQUID_GLASS) {
                                // Get system navigation bar height to position above it
                                val navigationBarInsets = WindowInsets.systemBars
                                    .only(WindowInsetsSides.Bottom)
                                val density = LocalDensity.current
                                val systemNavBarHeight = with(density) {
                                    navigationBarInsets.asPaddingValues().calculateBottomPadding()
                                }



                                // Hide bottom navigation bar on settings screen
                                // Calculate slide offset for smooth hide/show animation
                                // Using the outer navigationBarHeight (animated) to drive the slide
                                val targetH = liquidNavBarHeightPref.dp + 24.dp
                                val totalHideOffsetPx = with(density) { (bottomInset + targetH).toPx() }
                                val currentH = with(density) { navigationBarHeight.toPx() }
                                val targetHPx = with(density) { targetH.toPx() }
                                
                                val slideOffsetY = if (targetHPx > 0) {
                                    ((1f - (currentH / targetHPx)) * totalHideOffsetPx).roundToInt()
                                } else {
                                    totalHideOffsetPx.roundToInt()
                                }

                                Box(
                                    modifier = Modifier
                                        .offset { 
                                            IntOffset(
                                                0, 
                                                slideOffsetY
                                            ) 
                                        }
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = systemNavBarHeight + (if (isFullScreen) 24.dp else 8.dp)) // Lift above system nav bar
                                ) {
                                    LiquidBottomNavigation(
                                        navController = navController,
                                        navigationItems = navigationItems,
                                        backdrop = backdrop,
                                        onNavigate = onScreenClick
                                    )
                                }
                            } else {
                                NavigationBar(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .then(
                                            if (enableDocker) {
                                                Modifier
                                                    .navigationBarsPadding()
                                                    .padding(
                                                        bottom = if (isFullScreen) 24.dp else 12.dp,
                                                        start = 12.dp,
                                                        end = 12.dp
                                                    )
                                                    .fillMaxWidth(navWidthPercent)
                                                    .height(navHeightDp.dp)
                                                    .shadow(8.dp, RoundedCornerShape(32.dp))
                                                    .background(
                                                        color = if (pureBlack && useDarkTheme) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                                        shape = RoundedCornerShape(32.dp)
                                                    )
                                                    .clip(RoundedCornerShape(32.dp))
                                            } else {
                                                Modifier
                                                    .fillMaxWidth()
                                                    .shadow(8.dp)
                                            }
                                        )
                                        .offset {
                                            if (navigationBarHeight == 0.dp) {
                                                IntOffset(
                                                    x = 0,
                                                    y = (bottomInset + (if (enableDocker) navHeightDp.dp else NavigationBarHeight) + 100.dp).roundToPx(),
                                                )
                                            } else {
                                                IntOffset(x = 0, y = 0)
                                            }
                                        },
                                    containerColor = Color.Transparent,
                                    contentColor = if (pureBlack && useDarkTheme) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    windowInsets = WindowInsets(0, 0, 0, 0)
                                ) {
                                    if (enableDocker) {
                                        Spacer(Modifier.width(12.dp))
                                    }

                                    // Get ViewModel for notification badge
                                    val viewModel: NewReleaseViewModel = hiltViewModel()
                                    val hasNewReleases by viewModel.hasNewReleases.collectAsState()

                                    navigationItems.fastForEach { screen ->
                                        val isSelected =
                                            navBackStackEntry?.destination?.hierarchy?.any {
                                                it.route == screen.route
                                            } == true

                                        NavigationBarItem(
                                            selected = isSelected,
                                            icon = {
                                                val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
                                                val currentUser = firebaseAuth.currentUser
                                                
                                                if (screen.route == Screens.Profile.route && currentUser?.photoUrl != null) {
                                                    coil.compose.AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(currentUser.photoUrl)
                                                            .allowHardware(false)
                                                            .build(),
                                                        contentDescription = stringResource(screen.titleId),
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .border(
                                                                width = if (isSelected) 2.dp else 1.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                                shape = CircleShape
                                                            ),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        painter = painterResource(
                                                            id = if (isSelected) {
                                                                screen.iconIdActive
                                                            } else {
                                                                screen.iconIdInactive
                                                            }
                                                        ),
                                                        contentDescription = stringResource(screen.titleId),
                                                    )
                                                }
                                            },
                                            label = if (!slimNav) {
                                                {
                                                    Text(
                                                        text = stringResource(screen.titleId),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            } else null,
                                            onClick = { onScreenClick(screen) },
                                        )
                                    }
                                    if (enableDocker) {
                                        Spacer(Modifier.width(12.dp))
                                    }
                                }

                                // Removed background strip (Solid Box) - Docker should be floating
                                // Box( ... background(insetBg) ...) -> REMOVED
                            }
                        }
                    


                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        sharedSong?.let { song ->
                            playerConnection?.let {
                                Dialog(
                                    onDismissRequest = { sharedSong = null },
                                    properties = DialogProperties(usePlatformDefaultWidth = false),
                                ) {
                                    Surface(
                                        modifier = Modifier.padding(24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = AlertDialogDefaults.containerColor,
                                        tonalElevation = AlertDialogDefaults.TonalElevation,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = { sharedSong = null },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // YouTube link download dialog
                        if (showYouTubeLinkDialog && youtubeLinkUrl.isNotEmpty()) {
                            YouTubeLinkDialog(
                                youtubeUrl = youtubeLinkUrl,
                                onDismiss = {
                                    showYouTubeLinkDialog = false
                                    youtubeLinkUrl = ""
                                }
                            )
                        }


                        
                        // Storage Permission Dialog removed - permissions handled by PermissionScreen


                    }

                    LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                        if (shouldShowSearchBar && openSearchImmediately) {
                            onActiveChange(true)
                            try {
                                delay(100)
                                searchBarFocusRequester.requestFocus()
                            } catch (_: Exception) {
                            }
                            openSearchImmediately = false
                        }
                    }

                    if (showGuestSearchDialog) {
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showGuestSearchDialog = false },
                            title = {
                                Text(text = "Login Required")
                            },
                            text = {
                                Text(text = "Please log in to search for users and friends.")
                            },
                            confirmButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        showGuestSearchDialog = false
                                        navController.navigate(Screens.Profile.route)
                                    }
                                ) {
                                    Text("Log In")
                                }
                            },
                            dismissButton = {
                                androidx.compose.material3.TextButton(
                                    onClick = { showGuestSearchDialog = false }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                    }
                }
            }
        }
    }

    private fun navigateToScreen(
        navController: NavHostController,
        screen: Screens
    ) {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
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

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean, themeMode: ThemeMode) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        
        // Status Bar Color
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        } else {
            window.statusBarColor = Color.Transparent.toArgb()
        }

        // Navigation Bar Color
        // Fix for Liquid Theme: User reported it was transparent, so we set it to a solid color (Surface/Black)
        if (themeMode == ThemeMode.LIQUID_GLASS) {
             // For Liquid Glass, avoid transparency to prevent content scrolling visibility issues behind the system bar
             // Use a solid color compatible with the theme (Black for dark mode, Scrim for light)
             window.navigationBarColor = if (isDark) Color.Black.toArgb() else Color(0xFFF5F5F5).toArgb() // Using light grey for light mode
             
             // Ensure contrast symbols
             WindowCompat.getInsetsController(window, window.decorView.rootView).isAppearanceLightNavigationBars = !isDark
        } else {
            // Default behavior (Transparent for edge-to-edge)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.navigationBarColor = Color.Transparent.toArgb()
            } else {
                window.navigationBarColor =
                    (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
            }
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.musifly.android.action.SEARCH"
        const val ACTION_EXPLORE = "com.musifly.android.action.EXPLORE"
        const val ACTION_LIBRARY = "com.musifly.android.action.LIBRARY"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection =
    staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }

@Composable
fun NotificationPermissionPreference() {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(false) }

    // Función para verificar permisos extraída para mejor legibilidad
    val checkNotificationPermission = remember {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED // Usar PackageManager en lugar de PermissionChecker
            } else {
                // Para versiones anteriores, verificar si las notificaciones están habilitadas
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        // Opcional: agregar callback para manejar el resultado
        if (!isGranted) {
            // Manejar caso cuando el usuario rechaza el permiso
            Log.d("NotificationPermission", "Permiso de notificaciones denegado")
        }
    }

    // Verificar permisos al inicializar y cuando la app vuelve al foreground
    LaunchedEffect(Unit) {
        permissionGranted = checkNotificationPermission()
    }

    // Escuchar cambios cuando la app vuelve del background
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = checkNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    SwitchPreference(
        title = { Text(stringResource(R.string.notification)) },
        icon = {
            Icon(
                painter = painterResource(
                    id = if (permissionGranted) R.drawable.notification_on
                    else R.drawable.notification_off
                ),
                contentDescription = stringResource(
                    if (permissionGranted) R.string.notifications_enabled
                    else R.string.notifications_disabled
                )
            )
        },
        checked = permissionGranted,
        onCheckedChange = { checked ->
            when {
                checked && !permissionGranted -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Para versiones anteriores, dirigir a configuración
                        openNotificationSettings(context)
                    }
                }

                !checked && permissionGranted -> {
                    // Si el usuario intenta desactivar, dirigir a configuración del sistema
                    openNotificationSettings(context)
                }
            }
        }
    )
}


// Función auxiliar para abrir configuración de notificaciones
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
    } catch (e: ActivityNotFoundException) {
        Log.e("NotificationSettings", "No se pudo abrir configuración de notificaciones", e)
        // Fallback: abrir configuración general
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
        e.printStackTrace()
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

@Composable
fun ProfileIconWithUpdateBadge(
    currentVersion: String,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)
    var showUpdateBadge by remember { mutableStateOf(false) }
    val updatedOnClick = rememberUpdatedState(onProfileClick)

    // Control seguro de updates
    LaunchedEffect(currentVersion) {
        try {
            val latestVersion = withContext(Dispatchers.IO) { checkForUpdates() }
            showUpdateBadge = latestVersion?.let { isNewerVersion(it, currentVersion) } ?: false
        } catch (e: Exception) {
            Timber.tag("ProfileIcon").e("Error checking for updates: ${e.message}")
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                try {
                    updatedOnClick.value()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    )
    {
        // Avatar usando el nuevo sistema
        Box(contentAlignment = Alignment.Center) {
            when (currentSelection) {
                is AvatarSelection.Custom -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data((currentSelection as AvatarSelection.Custom).uri.toUri())
                                    .allowHardware(false)
                                    .crossfade(true)
                                    .error(R.drawable.person)
                                    .placeholder(R.drawable.person)
                                    .build(),
                        contentDescription = "Avatar personalizado",
                        modifier = modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                is AvatarSelection.DiceBear -> {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data((currentSelection as AvatarSelection.DiceBear).url)
                            .allowHardware(false)
                            .crossfade(true)
                            .error(R.drawable.person)
                            .placeholder(R.drawable.person)
                            .build(),
                        contentDescription = "Avatar DiceBear",
                        modifier = modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                else -> {
                    Icon(
                        painter = painterResource(R.drawable.person),
                        contentDescription = "Avatar predeterminado",
                        modifier = modifier
                    )
                }
            }

            // Badge de actualización
            if (showUpdateBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.update),
                        contentDescription = "Actualización disponible",
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.Center)
                    )
                }
            }

        }
    }
}