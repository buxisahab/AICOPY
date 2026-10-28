package com.musifly.android.ui.player

// import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi // Removed
// JossRedFallbackKey import removed - fake fallback mechanism
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.media3.common.C
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.common.util.Util
import java.time.LocalDateTime
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalDownloadUtil
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.DarkModeKey
import com.musifly.android.constants.DefaultSmallButtonsShape
import com.musifly.android.constants.HapticFeedbackEnabledKey
import com.musifly.android.constants.HapticFeedbackIntensity
import com.musifly.android.constants.HapticFeedbackIntensityKey
import com.musifly.android.constants.PlayerBackgroundStyle
import com.musifly.android.constants.PlayerBackgroundStyleKey
import com.musifly.android.constants.PlayerButtonsStyle
import com.musifly.android.constants.PlayerButtonsStyleKey
import com.musifly.android.constants.PlayerHorizontalPadding
import com.musifly.android.constants.PlayerLayoutStyle
import com.musifly.android.constants.PlayerLayoutStyleKey
import com.musifly.android.constants.PlayerTextAlignmentKey
import com.musifly.android.constants.PureBlackKey
import com.musifly.android.constants.ShowLyricsKey
import com.musifly.android.constants.SliderStyle
import com.musifly.android.constants.SliderStyleKey
import com.musifly.android.constants.SmallButtonsShapeKey
import com.musifly.android.constants.UseNewPipeStreamingKey
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.extensions.toggleRepeatMode
import com.musifly.android.models.MediaMetadata
import com.musifly.android.playback.ExoDownloadService
import com.musifly.android.secure.SecureString
import com.musifly.android.streamingengines.StreamingEngineManager
import com.musifly.android.ui.component.BottomSheet
import com.musifly.android.ui.component.BottomSheetState

import com.musifly.android.ui.component.DownloadButton
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.component.PlayerSliderTrack
import com.musifly.android.ui.component.rememberBottomSheetState
import com.musifly.android.ui.menu.PlayerMenu
import com.musifly.android.ui.player.layouts.BloomePlayerLayout
import com.musifly.android.ui.player.layouts.MussiflyPremiumPlayerLayout
import com.musifly.android.ui.player.layouts.PlayerActions
import com.musifly.android.ui.player.layouts.PlayerState
import com.musifly.android.ui.player.layouts.SpotifyPlayerLayout
import com.musifly.android.ui.screens.settings.DarkMode
import com.musifly.android.ui.screens.settings.PlayerTextAlignment
import com.musifly.android.ui.screens.settings.PreferenceKeys
import com.musifly.android.ui.theme.extractGradientColors
import com.musifly.android.utils.dataStore
import com.musifly.android.utils.getSmallButtonShape
import com.musifly.android.utils.makeTimeString
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import kotlin.math.roundToInt
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.Backdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.LayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.layerBackdrop
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.AdaptiveSwitch
import com.musifly.android.ui.screens.settings.AudioEffectsContent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    onOpenFullscreenLyrics: () -> Unit, // NUEVO PARÁMETRO
    onActivateSearch: (() -> Unit)? = null, // NEW: Search activation callback
    backdrop: LayerBackdrop? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()

    val clipboardManager = LocalClipboardManager.current

    var showFullscreenLyrics by remember { mutableStateOf(false) }
    


    // Liquid Glass Backdrop
    val isLiquidGlass = isLiquidGlassActive()
    val activeBackdrop = backdrop ?: (if (isLiquidGlass) rememberLayerBackdrop() else null)
    
    // Double tap to like states
    var showCenterLikeAnimation by remember { mutableStateOf(false) }
    
    val centerLikeScale by animateFloatAsState(
        targetValue = if (showCenterLikeAnimation) 1.2f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "centerLikeScale"
    )
    
    val centerLikeAlpha by animateFloatAsState(
        targetValue = if (showCenterLikeAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "centerLikeAlpha"
    )
    
    // Handle double tap animation lifecycle
    LaunchedEffect(showCenterLikeAnimation) {
        if (showCenterLikeAnimation) {
            delay(800) // Animation duration
            showCenterLikeAnimation = false
        }
    }

    val playerConnection = LocalPlayerConnection.current ?: return






    val playerTextAlignment by rememberEnumPreference(
        PlayerTextAlignmentKey,
        PlayerTextAlignment.CENTER
    )

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    
    // Player Layout Style (Instagram Reels, Spotify, Apple, Resso)
    val playerLayoutStyle by rememberEnumPreference(
        key = PlayerLayoutStyleKey,
        defaultValue = PlayerLayoutStyle.MUSSIFLY_PREMIUM
    )
    
    val useNewPipeStreaming by rememberPreference(
        key = UseNewPipeStreamingKey,
        defaultValue = false
    )
    
    // JossRed preference removed - fake fallback mechanism
    

    // Streaming Engine Preferences (for search engines)
    val (enableJioSaavn, onEnableJioSaavnChange) = rememberPreference(
        key = PreferenceKeys.EnableJioSaavnKey,
        defaultValue = true
    )
    val (enableYouTubeMusic, onEnableYouTubeMusicChange) = rememberPreference(
        key = PreferenceKeys.EnableYouTubeMusicKey,
        defaultValue = true
    )
    val (enableYouTubeVideo, onEnableYouTubeVideoChange) = rememberPreference(
        key = PreferenceKeys.EnableYouTubeVideoKey,
        defaultValue = true
    )
    val (enableSpotify, onEnableSpotifyChange) = rememberPreference(
        key = PreferenceKeys.EnableSpotifyKey,
        defaultValue = true
    )
    
    val streamingManager = remember { StreamingEngineManager.getInstance() }
    
    // Initialize enabled engines
    LaunchedEffect(enableJioSaavn, enableYouTubeMusic, enableYouTubeVideo, enableSpotify) {
        streamingManager.setEnabledEngines(
            jioSaavnEnabled = enableJioSaavn,
            youtubeMusicEnabled = enableYouTubeMusic,
            youtubeVideoEnabled = enableYouTubeVideo,
            spotifyEnabled = enableSpotify
        )
    }
    
    // Haptic Feedback Settings
    val hapticEnabled by rememberPreference(
        key = HapticFeedbackEnabledKey,
        defaultValue = true
    )
    val hapticIntensity by rememberEnumPreference(
        key = HapticFeedbackIntensityKey,
        defaultValue = HapticFeedbackIntensity.MEDIUM
    )
    
    // Initialize Vibrator Service
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    
    // Haptic Feedback Helper Function
    fun performHapticFeedback(type: String = "light") {
        if (!hapticEnabled || hapticIntensity == HapticFeedbackIntensity.OFF) return
        
        val duration = when (type) {
            "light" -> when (hapticIntensity) {
                HapticFeedbackIntensity.LIGHT -> 5L
                HapticFeedbackIntensity.MEDIUM -> 10L
                HapticFeedbackIntensity.STRONG -> 15L
                else -> return
            }
            "medium" -> when (hapticIntensity) {
                HapticFeedbackIntensity.LIGHT -> 15L
                HapticFeedbackIntensity.MEDIUM -> 25L
                HapticFeedbackIntensity.STRONG -> 35L
                else -> return
            }
            "strong" -> when (hapticIntensity) {
                HapticFeedbackIntensity.LIGHT -> 25L
                HapticFeedbackIntensity.MEDIUM -> 40L
                HapticFeedbackIntensity.STRONG -> 60L
                else -> return
            }
            else -> 10L
        }
        
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(
                        VibrationEffect.createOneShot(
                            duration,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(duration)
                }
            }
        } catch (e: Exception) {
            // Silently fail if haptic feedback is not available
        }
    }

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
        }
    val backgroundColor = if (useBlackBackground && state.value > state.collapsedBound) {
        lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    var showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SQUIGGLY)
    
    // Reset showLyrics when switching to Instagram layout to prevent auto-showing lyrics
    LaunchedEffect(playerLayoutStyle) {
        if (playerLayoutStyle == PlayerLayoutStyle.INSTAGRAM && showLyrics) {
            showLyrics = false
        }
    }

    // Use PlayerConnection's StateFlows directly to avoid redundant updates
    val position by playerConnection.currentPosition.collectAsState(initial = 0L)
    val duration by playerConnection.duration.collectAsState(initial = C.TIME_UNSET)
    
    // Persistent manual seek position to prevent the "ugly" jump during network latency
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    
    // Sync slider override with actual player position updates to ensure smooth finish
    LaunchedEffect(position) {
        sliderPosition?.let { target ->
            // If player reports position within 2s of target, we consider the seek confirmed
            if (kotlin.math.abs(position - target) < 2000L) {
                sliderPosition = null
            }
        }
    }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }

    var changeColor by remember {
        mutableStateOf(false)
    }



    // Animaciones para efectos de fondo
    var backgroundImageUrl by remember { mutableStateOf<String?>(null) }
    val blurRadius by animateDpAsState(
        targetValue = if (state.isExpanded && playerBackground == PlayerBackgroundStyle.BLUR) 150.dp else 0.dp,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "blurRadius"
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (state.isExpanded && playerBackground != PlayerBackgroundStyle.DEFAULT) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "backgroundAlpha"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = when {
            !state.isExpanded -> 0f
            playerBackground == PlayerBackgroundStyle.BLUR -> 0.3f
            playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2 -> 0.2f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "overlayAlpha"
    )


    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.SECONDARY
    )
    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    LaunchedEffect(mediaMetadata, playerBackground) {
        // Actualizar URL de imagen para transiciones suaves
        backgroundImageUrl = mediaMetadata?.thumbnailUrl

        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.BLUR) {
            gradientColors = listOf(Color.Black, Color.Black)
        }
        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.GRADIENT) {
            gradientColors = listOf(Color.Black, Color.Black)
        } else if (playerBackground == PlayerBackgroundStyle.GRADIENT) {
            // Debounce to prevent excessive computations during rapid changes
            delay(100)
            withContext(Dispatchers.IO) {
                val result =
                    (
                            ImageLoader(context)
                                .execute(
                                    ImageRequest
                                        .Builder(context)
                                        .data(mediaMetadata?.thumbnailUrl)
                                        .allowHardware(false) // Disable hardware acceleration for safety
                                        .memoryCacheKey(mediaMetadata?.id) // Use memory cache
                                        .diskCacheKey(mediaMetadata?.id) // Use disk cache
                                        .build(),
                                ).drawable as? BitmapDrawable
                            )?.bitmap?.extractGradientColors()

                result?.let {
                    gradientColors = it
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val changeBound = state.expandedBound / 3

    val TextBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            PlayerBackgroundStyle.BLUR -> Color.White
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    changeColor = true
                    Color.Black
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    changeColor = true
                    Color.White
                } else {
                    changeColor = false
                    Color.White
                }
            }
        }

    val icBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR -> Color.Black
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    changeColor = true
                    Color.White
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    changeColor = true
                    Color.Black
                } else {
                    changeColor = false
                    Color.Black
                }
            }
        }

    val (textButtonColor, iconButtonColor) = when (playerButtonsStyle) {
        PlayerButtonsStyle.DEFAULT -> Pair(TextBackgroundColor, icBackgroundColor)
        PlayerButtonsStyle.SECONDARY -> Pair(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        )
    }


    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var showAudioEffectsDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    if (showSleepTimerDialog) {
        if (isLiquidGlass) {
            com.musifly.android.ui.component.liquidglass.LiquidGlassDialog(
                onDismiss = { showSleepTimerDialog = false },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = null
                    )
                },
                title = { Text(stringResource(R.string.sleep_timer)) },
                content = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.minute,
                                sleepTimerValue.roundToInt(),
                                sleepTimerValue.roundToInt()
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        com.musifly.android.ui.component.liquidglass.LiquidSlider(
                            value = { sleepTimerValue },
                            onValueChange = { sleepTimerValue = it },
                            valueRange = 5f..120f,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                },
                buttons = {
                    com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                         onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                         },
                         text = stringResource(android.R.string.ok),
                         modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        },
                        text = stringResource(R.string.end_of_song),
                        modifier = Modifier.weight(1f)
                    )
                }
            )
        } else {
            AlertDialog(
                properties = DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = { showSleepTimerDialog = false },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = null
                    )
                },
                title = { Text(stringResource(R.string.sleep_timer)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                        },
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSleepTimerDialog = false },
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.minute,
                                sleepTimerValue.roundToInt(),
                                sleepTimerValue.roundToInt()
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        Slider(
                            value = sleepTimerValue,
                            onValueChange = { sleepTimerValue = it },
                            valueRange = 5f..120f,
                            steps = (120 - 5) / 5 - 1,
                        )

                        OutlinedButton(
                            onClick = {
                                showSleepTimerDialog = false
                                playerConnection.service.sleepTimer.start(-1)
                            },
                        ) {
                            Text(stringResource(R.string.end_of_song))
                        }
                    }
                },
            )
        }
    }

    if (showAudioEffectsDialog) {
        if (isLiquidGlass) {
            com.musifly.android.ui.component.liquidglass.LiquidGlassDialog(
                onDismiss = { showAudioEffectsDialog = false },
                content = {
                    AudioEffectsContent(
                        isDialog = true,
                        onBack = { showAudioEffectsDialog = false }
                    )
                }
            )
        } else {
            Dialog(
                onDismissRequest = { showAudioEffectsDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp), // Increased width
                    shape = RoundedCornerShape(16.dp), // Reduced corners
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    AudioEffectsContent(
                        isDialog = true,
                        onBack = { showAudioEffectsDialog = false }
                    )
                }
            }
        }
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }


    val smallButtonsShapeState = rememberPreference(
        key = SmallButtonsShapeKey,
        defaultValue = DefaultSmallButtonsShape
    )

    val smallButtonShape = remember(smallButtonsShapeState.value) {
        getSmallButtonShape(smallButtonsShapeState.value)
    }

    // Instagram Reels-style: transparent buttons without background
    val smallButtonModifier = @Composable {
        Modifier
            .size(42.dp)
    }

    // Position and duration are now managed by PlayerConnection StateFlows
    // No need for redundant updates here - PlayerConnection already throttles to 1000ms

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    var showDetailsDialog by rememberSaveable {
        mutableStateOf(false)
    }
    
    var showStreamingSelectorDialog by remember {
        mutableStateOf(false)
    }

    if (showDetailsDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showDetailsDialog = false },
            containerColor = if (useBlackBackground) Color.Black else AlertDialogDefaults.containerColor,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDetailsDialog = false },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    listOf(
                        stringResource(R.string.song_title) to mediaMetadata?.title,
                        stringResource(R.string.song_artists) to mediaMetadata?.artists?.joinToString { it.name },
                        stringResource(R.string.media_id) to mediaMetadata?.id,
                        "Itag" to currentFormat?.itag?.toString(),
                        stringResource(R.string.mime_type) to currentFormat?.mimeType,
                        stringResource(R.string.codecs) to currentFormat?.codecs,
                        stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                        stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                        stringResource(R.string.loudness) to currentFormat?.loudnessDb?.let { "$it dB" },
                        stringResource(R.string.volume) to "${(playerConnection.player.volume * 100).toInt()}%",
                        stringResource(R.string.file_size) to
                                currentFormat?.contentLength?.let {
                                    Formatter.formatShortFileSize(
                                        context,
                                        it
                                    )
                                },
                    ).forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            modifier =
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(displayText))
                                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                ),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
        )
    }
    
    // Streaming Engine Selector Dialog
    if (showStreamingSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showStreamingSelectorDialog = false },
            containerColor = if (useBlackBackground) Color.Black else AlertDialogDefaults.containerColor,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.media3_icon_circular_play),
                    contentDescription = null
                )
            },
            title = { Text("Streaming Engines") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Streaming Quality",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    // NewPipe Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (useNewPipeStreaming)
                                    Color(0xFF4CAF50).copy(alpha = 0.2f)
                                else
                                    Color.Transparent
                            )
                            .border(
                                width = 2.dp,
                                color = if (useNewPipeStreaming)
                                    Color(0xFF4CAF50)
                                else
                                    Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                performHapticFeedback("medium")
                                coroutineScope.launch {
                                    context.dataStore.edit { preferences ->
                                        preferences[UseNewPipeStreamingKey] = true
                                    }
                                    showStreamingSelectorDialog = false
                                    Toast
                                        .makeText(
                                            context,
                                            "✅ Switched to NewPipe Streaming",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                        Column {
                            Text(
                                text = "NewPipe",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "High quality, large buffers, smart caching",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    // YouTube Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (!useNewPipeStreaming)
                                    Color(0xFFFF5722).copy(alpha = 0.2f)
                                else
                                    Color.Transparent
                            )
                            .border(
                                width = 2.dp,
                                color = if (!useNewPipeStreaming)
                                    Color(0xFFFF5722)
                                else
                                    Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                performHapticFeedback("medium")
                                coroutineScope.launch {
                                    context.dataStore.edit { preferences ->
                                        preferences[UseNewPipeStreamingKey] = false
                                    }
                                    showStreamingSelectorDialog = false
                                    Toast
                                        .makeText(
                                            context,
                                            "▶️ Switched to YouTube Streaming",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5722))
                        )
                        Column {
                            Text(
                                text = "YouTube",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Standard streaming, reliable",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    // JossRed Fallback UI removed - fake fallback mechanism

                    
                    // Divider
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    
                    // Search Engines Section
                    Text(
                        text = "Search Engines",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Text(
                        text = "Select which engines to use for searching songs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // JioSaavn Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "JioSaavn (320kbps)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (enableJioSaavn) "High quality streaming for Indian content" else "⚠️ Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        AdaptiveSwitch(
                            checked = enableJioSaavn,
                            onCheckedChange = { newValue ->
                                onEnableJioSaavnChange(newValue)
                                streamingManager.setEnabledEngines(
                                    jioSaavnEnabled = newValue,
                                    youtubeMusicEnabled = enableYouTubeMusic,
                                    youtubeVideoEnabled = enableYouTubeVideo,
                                    spotifyEnabled = enableSpotify
                                )
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    // YouTube Music Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "YouTube Music",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (enableYouTubeMusic) "Stream from YouTube Music" else "⚠️ Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        AdaptiveSwitch(
                            checked = enableYouTubeMusic,
                            onCheckedChange = { newValue ->
                                onEnableYouTubeMusicChange(newValue)
                                streamingManager.setEnabledEngines(
                                    jioSaavnEnabled = enableJioSaavn,
                                    youtubeMusicEnabled = newValue,
                                    youtubeVideoEnabled = enableYouTubeVideo,
                                    spotifyEnabled = enableSpotify
                                )
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    // YouTube Video Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "YouTube Video",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (enableYouTubeVideo) "Stream from YouTube videos" else "⚠️ Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        AdaptiveSwitch(
                            checked = enableYouTubeVideo,
                            onCheckedChange = { newValue ->
                                onEnableYouTubeVideoChange(newValue)
                                streamingManager.setEnabledEngines(
                                    jioSaavnEnabled = enableJioSaavn,
                                    youtubeMusicEnabled = enableYouTubeMusic,
                                    youtubeVideoEnabled = newValue,
                                    spotifyEnabled = enableSpotify
                                )
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    // Spotify Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Spotify",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (enableSpotify) "Search tracks from Spotify" else "⚠️ Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        AdaptiveSwitch(
                            checked = enableSpotify,
                            onCheckedChange = { newValue ->
                                onEnableSpotifyChange(newValue)
                                streamingManager.setEnabledEngines(
                                    jioSaavnEnabled = enableJioSaavn,
                                    youtubeMusicEnabled = enableYouTubeMusic,
                                    youtubeVideoEnabled = enableYouTubeVideo,
                                    spotifyEnabled = newValue
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStreamingSelectorDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
    


    var showQueueSheet by rememberSaveable { mutableStateOf(false) }
    
    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = 0.dp,
            expandedBound = state.expandedBound,
        )
    
    // Monitor queue sheet state and close when dismissed
    LaunchedEffect(queueSheetState.value) {
        if (queueSheetState.value == queueSheetState.dismissedBound) {
            showQueueSheet = false
        }
    }

    val bottomSheetBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT ->
            MaterialTheme.colorScheme.surfaceContainer
        else ->
            if (useBlackBackground) Color.Black
            else MaterialTheme.colorScheme.surfaceContainer
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
             modifier = Modifier
                 .fillMaxSize()
                 .then(Modifier)
        ) {
            BottomSheet(
            state = state,
            modifier = modifier,
            background = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bottomSheetBackgroundColor)
                ) {
                    when (playerBackground) {
                        PlayerBackgroundStyle.BLUR -> {
                            AnimatedContent(
                                targetState = mediaMetadata?.thumbnailUrl,
                                transitionSpec = {
                                    fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                                },
                                label = "blurBackground"
                            ) { thumbnailUrl ->
                                if (thumbnailUrl != null) {
                                    Box(modifier = Modifier.alpha(backgroundAlpha)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(thumbnailUrl)
                                                .size(100, 100)
                                                .allowHardware(false)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .blur(if (useDarkTheme) 150.dp else 100.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f))
                                        )
                                    }
                                }
                            }
                        }

                        PlayerBackgroundStyle.GRADIENT -> {
                            AnimatedContent(
                                targetState = gradientColors,
                                transitionSpec = {
                                    fadeIn(tween(800)).togetherWith(fadeOut(tween(800)))
                                },
                                label = "gradientBackground"
                            ) { colors ->
                                if (colors.isNotEmpty()) {
                                    val gradientColorStops = if (colors.size >= 3) {
                                        arrayOf(
                                            0.0f to colors[0],
                                            0.5f to colors[1],
                                            1.0f to colors[2]
                                        )
                                    } else {
                                        arrayOf(
                                            0.0f to colors[0],
                                            0.6f to colors[0].copy(alpha = 0.7f),
                                            1.0f to Color.Black
                                        )
                                    }
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .alpha(backgroundAlpha)
                                            .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                            .background(Color.Black.copy(alpha = 0.2f))
                                    )
                                }
                            }
                        }

                        else -> {
                            PlayerBackgroundStyle.DEFAULT
                        }
                    }
                }
            },
            onDismiss = {
                playerConnection.service.clearAutomix()
                playerConnection.player.stop()
                playerConnection.player.clearMediaItems()
            },
            collapsedContent = {
                MiniPlayer(
                    backdrop = activeBackdrop,
                    position = position,
                    duration = duration,
                )
            },
        ) {
            val interactionButtons: @Composable (MediaMetadata, Boolean, Color) -> Unit =
                { mediaMetadata, isVertical, contentColor ->
                    val buttons = @Composable {
                        // Instagram Reels-style button with circular semi-transparent background
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                                .clickable {
                                    playerConnection.service.startRadioSeamlessly()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.radio),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(contentColor),
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                                .clickable {
                                    if (download?.state == Download.STATE_COMPLETED) {
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            mediaMetadata.id,
                                            false,
                                        )
                                    } else {
                                        database.transaction {
                                            insert(mediaMetadata)
                                        }
                                        val downloadRequest =
                                            DownloadRequest
                                                .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                                .setCustomCacheKey(mediaMetadata.id)
                                                .setData(mediaMetadata.title.toByteArray())
                                                .build()
                                        DownloadService.sendAddDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            downloadRequest,
                                            false,
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (download?.state == Download.STATE_DOWNLOADING) {
                                val progress = download!!.percentDownloaded / 100f
                                val animatedProgress by animateFloatAsState(
                                    targetValue = progress,
                                    animationSpec = tween(
                                        durationMillis = 300,
                                        easing = FastOutSlowInEasing
                                    ),
                                    label = "downloadProgress"
                                )

                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                ) {
                                    val strokeWidth = 3.dp.toPx()
                                    val radius = (size.minDimension - strokeWidth) / 2
                                    val center = Offset(size.width / 2, size.height / 2)

                                    // Círculo de fondo (gris)
                                    drawCircle(
                                        color = contentColor.copy(alpha = 0.3f),
                                        radius = radius,
                                        center = center,
                                        style = Stroke(width = strokeWidth)
                                    )

                                    // Círculo de progreso
                                    if (animatedProgress > 0f) {
                                        drawArc(
                                            color = contentColor,
                                            startAngle = -90f,
                                            sweepAngle = 360f * animatedProgress,
                                            useCenter = false,
                                            topLeft = Offset(
                                                center.x - radius,
                                                center.y - radius
                                            ),
                                            size = Size(radius * 2, radius * 2),
                                            style = Stroke(
                                                width = strokeWidth,
                                                cap = StrokeCap.Round
                                            )
                                        )
                                    }
                                }
                            }

                            val iconResource = when (download?.state) {
                                Download.STATE_COMPLETED -> R.drawable.offline
                                Download.STATE_DOWNLOADING -> R.drawable.pause
                                Download.STATE_FAILED -> R.drawable.error
                                else -> R.drawable.download
                            }

                            val iconAlpha by animateFloatAsState(
                                targetValue = if (download?.state == Download.STATE_DOWNLOADING) 0.8f else 1f,
                                animationSpec = tween(durationMillis = 200),
                                label = "iconAlpha"
                            )

                            val iconScale by animateFloatAsState(
                                targetValue = if (download?.state == Download.STATE_DOWNLOADING) 0.9f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "iconScale"
                            )

                            Image(
                                painter = painterResource(iconResource),
                                contentDescription = when (download?.state) {
                                    Download.STATE_COMPLETED -> "Downloaded"
                                    Download.STATE_DOWNLOADING -> "Downloading..."
                                    Download.STATE_FAILED -> "Download failed"
                                    else -> "Download"
                                },
                                colorFilter = ColorFilter.tint(
                                    when (download?.state) {
                                        Download.STATE_FAILED -> MaterialTheme.colorScheme.error
                                        else -> contentColor
                                    }
                                ),
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(iconScale)
                                    .alpha(iconAlpha),
                            )

                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                                .clickable {
                                    performHapticFeedback("light")
                                    if (sleepTimerEnabled) {
                                        playerConnection.service.sleepTimer.clear()
                                    } else {
                                        showSleepTimerDialog = true
                                    }
                                },
                        ) {
                            AnimatedContent(
                                label = "sleepTimer",
                                targetState = sleepTimerEnabled,
                            ) { sleepTimerEnabled ->
                                if (sleepTimerEnabled) {
                                    Text(
                                        text = makeTimeString(sleepTimerTimeLeft),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = onBackgroundColor,
                                        maxLines = 1,
                                        modifier = Modifier.basicMarquee(),
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(R.drawable.bedtime),
                                        colorFilter = ColorFilter.tint(contentColor),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                                .clickable {
                                    showQueueSheet = true
                                    queueSheetState.expand()
                                },
                        ) {
                            Image(
                                painter = painterResource(R.drawable.list),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(contentColor),
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.3f))
                                .clickable {
                                    menuState.show {
                                        PlayerMenu(
                                            mediaMetadata = mediaMetadata,
                                            navController = navController,
                                            playerBottomSheetState = state,
                                            onShowDetailsDialog = { showDetailsDialog = true },
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                        ) {
                            Image(
                                painter = painterResource(R.drawable.more_horiz),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(contentColor),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (isVertical) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(12.dp)
                        ) {
                            buttons()
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(12.dp)
                        ) {
                            buttons()
                        }
                    }
                }

            val controlsContent: @Composable ColumnScope.(MediaMetadata, Color) -> Unit =
                { mediaMetadata, contentColor ->
                    val playPauseRoundness by animateDpAsState(
                        targetValue = if (isPlaying) 24.dp else 36.dp,
                        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
                        label = "playPauseRoundness",
                    )



                    Spacer(Modifier.height(6.dp))

                    when (sliderStyle) {
                        SliderStyle.DEFAULT -> {
                            Slider(
                                value = (sliderPosition ?: position).toFloat(),
                                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                onValueChange = {
                                    sliderPosition = it.toLong()
                                },
                                onValueChangeFinished = {
                                    sliderPosition?.let {
                                        playerConnection.player.seekTo(it)
                                    }
                                },
                                colors = SliderDefaults.colors(
                                    activeTrackColor = contentColor,
                                    inactiveTrackColor = Color.Gray,
                                    activeTickColor = contentColor,
                                    inactiveTickColor = Color.Gray,
                                    thumbColor = contentColor
                                ),
                                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                            )
                        }

                        SliderStyle.SQUIGGLY -> {
                            SquigglySlider(
                                value = (sliderPosition ?: position).toFloat(),
                                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                onValueChange = {
                                    sliderPosition = it.toLong()
                                },
                                onValueChangeFinished = {
                                    sliderPosition?.let {
                                        playerConnection.player.seekTo(it)
                                    }
                                },
                                colors = SliderDefaults.colors(
                                    activeTrackColor = contentColor,
                                    inactiveTrackColor = Color.Gray,
                                    activeTickColor = contentColor,
                                    inactiveTickColor = Color.Gray,
                                    thumbColor = contentColor
                                ),
                                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                                squigglesSpec =
                                    SquigglySlider.SquigglesSpec(
                                        amplitude = if (isPlaying) (2.dp).coerceAtLeast(2.dp) else 0.dp,
                                        strokeWidth = 3.dp,
                                    ),
                            )
                        }

                        SliderStyle.SLIM -> {
                            Slider(
                                value = (sliderPosition ?: position).toFloat(),
                                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                onValueChange = {
                                    sliderPosition = it.toLong()
                                },
                                onValueChangeFinished = {
                                    sliderPosition?.let {
                                        playerConnection.player.seekTo(it)
                                    }
                                },
                                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                track = { sliderState ->
                                    PlayerSliderTrack(
                                        sliderState = sliderState,
                                        colors = SliderDefaults.colors(
                                            activeTrackColor = contentColor,
                                            inactiveTrackColor = Color.Gray,
                                            activeTickColor = contentColor,
                                            inactiveTickColor = Color.Gray
                                        )
                                    )
                                },
                                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PlayerHorizontalPadding + 4.dp),
                    ) {
                        Text(
                            text = makeTimeString(sliderPosition ?: position),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextBackgroundColor.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Playback controls hidden as per request
                    Spacer(Modifier.height(32.dp))
                }

            // Efectos de fondo animados
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Fondo con imagen difuminada
                AnimatedVisibility(
                    visible = playerBackground == PlayerBackgroundStyle.BLUR && backgroundImageUrl != null,
                    enter = fadeIn(tween(600)),
                    exit = fadeOut(tween(400))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(backgroundImageUrl)
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(blurRadius)
                            .alpha(backgroundAlpha)
                    )
                }

                // Fondo con gradiente animado
                AnimatedVisibility(
                    visible = playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2,
                    enter = fadeIn(tween(800)),
                    exit = fadeOut(tween(600))
                ) {
                    val animatedGradientColors = gradientColors.map { color ->
                        androidx.compose.animation.animateColorAsState(
                            targetValue = color,
                            animationSpec = tween(
                                durationMillis = 1000,
                                easing = FastOutSlowInEasing
                            ),
                            label = "gradientColor"
                        ).value
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(backgroundAlpha)
                            .background(
                                Brush.verticalGradient(
                                    colors = if (animatedGradientColors.isNotEmpty()) animatedGradientColors else gradientColors
                                )
                            )
                    )
                }

                // Overlay oscuro animado
                AnimatedVisibility(
                    visible = overlayAlpha > 0f,
                    enter = fadeIn(tween(500)),
                    exit = fadeOut(tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = overlayAlpha))
                    )
                }

                // Gradient Scrim (Top and Bottom Shadow)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.9f),
                                    0.3f to Color.Transparent,
                                    0.6f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 1.0f)
                                )
                            )
                        )
                )
            }
            when (LocalConfiguration.current.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    Row(
                        modifier =
                            Modifier
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                .padding(bottom = 48.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f),
                        ) {
                            val screenWidth = LocalConfiguration.current.screenWidthDp
                            val thumbnailSize = (screenWidth * 0.4).dp

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Texto "Reproduciendo desde:"
                                val queueTitle by playerConnection.queueTitle.collectAsState()
                                AnimatedVisibility(
                                    visible = !queueTitle.isNullOrEmpty(),
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(bottom = 45.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.playing_from),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = onBackgroundColor.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )

                                        Text(
                                            text = queueTitle.orEmpty(),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = onBackgroundColor,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .padding(horizontal = 16.dp)
                                                .basicMarquee()
                                        )
                                    }
                                }

                                Thumbnail(
                                    sliderPositionProvider = { sliderPosition },
                                    onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                                    modifier = Modifier.size(thumbnailSize)
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .windowInsetsPadding(
                                        WindowInsets.systemBars.only(
                                            WindowInsetsSides.Top
                                        )
                                    ),
                        ) {
                            Spacer(Modifier.weight(1f))

                            mediaMetadata?.let {
                                controlsContent(it, TextBackgroundColor)
                                // Add buttons back for landscape
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    interactionButtons(it, false, iconButtonColor)
                                }
                            }

                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                else -> {
                    // Create PlayerState and PlayerActions for alternate layouts
                    
                    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "")
                        .collectAsState(initial = null)

                    val currentPlayerState = PlayerState(
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        position = position,
                        duration = duration,
                        isLiked = currentSong?.song?.liked ?: false,
                        canSkipPrevious = canSkipPrevious,
                        canSkipNext = canSkipNext,
                        repeatMode = repeatMode,
                        shuffleModeEnabled = playerConnection.player.shuffleModeEnabled,
                        playbackState = playbackState,
                        download = download
                    )
                    
                    // Download dialog state
                    var showDownloadDialogForLayout by remember { mutableStateOf(false) }
                    
                    val currentPlayerActions = PlayerActions(
                        onPlayPause = { playerConnection.player.togglePlayPause() },
                        onNext = { playerConnection.player.seekToNext() },
                        onPrevious = { playerConnection.player.seekToPrevious() },
                        onSeek = { pos -> playerConnection.player.seekTo(pos) },
                        onToggleLike = { playerConnection.toggleLike() },
                        onToggleRepeat = { playerConnection.player.toggleRepeatMode() },
                        onToggleShuffle = { 
                            playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                        },
                        onOpenQueue = { 
                            showQueueSheet = true
                            queueSheetState.expand()
                        },
                        onOpenMenu = {
                            mediaMetadata?.let { meta ->
                                menuState.show {
                                    PlayerMenu(
                                        mediaMetadata = meta,
                                        navController = navController,
                                        playerBottomSheetState = state,
                                        onShowDetailsDialog = { showDetailsDialog = true },
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            }
                        },
                        onOpenLyrics = onOpenFullscreenLyrics,
                        onOpenSleepTimer = { showSleepTimerDialog = true },
                        onDownload = {
                            mediaMetadata?.let { meta ->
                                coroutineScope.launch {
                                    if (download == null || download!!.state == Download.STATE_FAILED) {
                                        // START CACHED DOWNLOAD
                                        database.transaction {
                                            val existing = getSongById(meta.id)
                                            if (existing != null) {
                                                update(existing.song.copy(
                                                    inLibrary = LocalDateTime.now(),
                                                    dateDownload = LocalDateTime.now()
                                                ))
                                            } else {
                                                insert(meta) {
                                                    it.copy(
                                                        inLibrary = LocalDateTime.now(),
                                                        dateDownload = LocalDateTime.now()
                                                    )
                                                }
                                            }
                                        }

                                        val downloadRequest = DownloadRequest.Builder(meta.id, meta.id.toUri())
                                            .setCustomCacheKey(meta.id)
                                            .setData(Util.getUtf8Bytes(meta.title))
                                            .build()

                                        DownloadService.sendAddDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            downloadRequest,
                                            /* foreground= */ false
                                        )
                                        performHapticFeedback("heavy")
                                        Toast.makeText(context, "✅ Added to Offline Library", Toast.LENGTH_SHORT).show()
                                    } else if (download!!.state == Download.STATE_COMPLETED) {
                                        // PERMANENT DELETE (as requested)
                                        try {
                                            // 1. Remove from Repository (App Logic + Cloud Sync)
                                            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                                                context,
                                                com.musifly.android.di.CachedDownloadsRepositoryEntryPoint::class.java
                                            )
                                            val cachedDownloadsRepository = entryPoint.cachedDownloadsRepository()
                                            
                                            launch(Dispatchers.IO) {
                                                cachedDownloadsRepository.removeCachedDownload(meta.id)
                                            }

                                            // 2. Remove from ExoPlayer (File System)
                                            DownloadService.sendRemoveDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                meta.id,
                                                false
                                            )
                                            
                                            performHapticFeedback("medium")
                                            Toast.makeText(context, "🗑️ Removed from Offline Library", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            // Fallback if repository fails (just remove file)
                                            DownloadService.sendRemoveDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                meta.id,
                                                false
                                            )
                                            Toast.makeText(context, "Removed from device", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        // CANCEL DOWNLOAD (In progress)
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            meta.id,
                                            false
                                        )
                                        Toast.makeText(context, "Download cancelled", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onOpenDownloads = {
                            coroutineScope.launch {
                                state.collapseSoft()
                            }
                            navController.navigate("downloads")
                        },
                        onOpenSoundSettings = { showAudioEffectsDialog = true },
                        onOpenSearch = {
                            // Use the passed callback if available, otherwise just collapse
                            onActivateSearch?.invoke() ?: run {
                                coroutineScope.launch {
                                    state.collapseSoft()
                                }
                            }
                        },
                        onSwitchPlayer = {
                            // TODO: Show player layout selection dialog
                        }
                    )
                    

                    
                    val effectiveLayoutStyle = playerLayoutStyle
                    
                    // Switch between player layouts based on user preference (or forced for JioSaavn)
                    when (effectiveLayoutStyle) {
                        PlayerLayoutStyle.SPOTIFY -> {
                            SpotifyPlayerLayout(
                                playerState = currentPlayerState,
                                playerActions = currentPlayerActions,
                                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                                onCollapse = {
                                    state.collapseSoft()  // Use public method
                                },
                                sliderStyle = sliderStyle,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        PlayerLayoutStyle.BLOOME -> {
                            BloomePlayerLayout(
                                playerState = currentPlayerState,
                                playerActions = currentPlayerActions,
                                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                                sliderStyle = sliderStyle,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        PlayerLayoutStyle.MUSSIFLY_PREMIUM -> {
                            MussiflyPremiumPlayerLayout(
                                playerState = currentPlayerState,
                                playerActions = currentPlayerActions,
                                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                                onCollapse = { state.collapseSoft() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        // Default: Instagram Reels style (existing implementation)
                        else -> {
                    // Instagram Reels-style: Track scroll offset for entire screen scrolling
                    var screenScrollOffset by remember { mutableFloatStateOf(0f) }
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    
                    // Use derivedStateOf to reduce recompositions
                    val animatedOffset by remember {
                        androidx.compose.runtime.derivedStateOf {
                            screenScrollOffset.roundToInt()
                        }
                    }
                    
                    // Direct offset for Instagram Reels-style scrolling - optimized with derivedStateOf
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { androidx.compose.ui.unit.IntOffset(0, animatedOffset) }
                    ) {
                        // 1. Thumbnail (Full Screen) - passes scroll offset to parent
                        Thumbnail(
                            sliderPositionProvider = { sliderPosition },
                            onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 120.dp),
                            onScrollOffsetChanged = { offset ->
                                // Only update if change is significant (reduces recompositions)
                                val diff = kotlin.math.abs(screenScrollOffset - offset)
                                if (diff > 0.5f) {
                                    screenScrollOffset = offset
                                }
                            }
                        )
                        
                        // Center Like Animation (Instagram Reels style)
                        if (centerLikeScale > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.favorite),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .scale(centerLikeScale)
                                        .alpha(centerLikeAlpha)
                                )
                            }
                        }

                        // Status Bar Gradient Shadow
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.8f),
                                            Color.Black.copy(alpha = 0.6f),
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Top Nav Bar - Scroll with content (Instagram Reels style)
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .graphicsLayer {
                                    // Scroll with thumbnail (30% dampened like content)
                                    translationY = screenScrollOffset * 0.3f
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                        ) {
                            // App Name and Song Title with fade-in animation (takes available space)
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(800)),
                                exit = fadeOut(tween(400)),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "MusiFly",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )

                                    // Song Title below app name
                                    AnimatedContent(
                                        targetState = mediaMetadata?.title,
                                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                                        label = "topBarTitleAnimation",
                                    ) { title ->
                                        Text(
                                            text = title ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .basicMarquee()
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            // Right side: Streaming indicator + Menu button (fixed size, won't collapse)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                // Streaming source indicator hidden per request

                                
                                // Sound Effects button removed - depends on system equalizer
                                
                                // Three Dots Menu Button
                                IconButton(
                                    onClick = {
                                        performHapticFeedback("light")
                                        menuState.show {
                                            PlayerMenu(
                                                mediaMetadata = mediaMetadata,
                                                navController = navController,
                                                playerBottomSheetState = state,
                                                onShowDetailsDialog = { showDetailsDialog = true },
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_horiz),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Shadow Gradient - Scroll with content
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(250.dp)
                                .graphicsLayer {
                                    // Scroll with content
                                    translationY = screenScrollOffset * 0.3f
                                }
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Black.copy(alpha = 0.6f),
                                            Color.Black.copy(alpha = 0.85f),
                                            Color.Black.copy(alpha = 0.95f)
                                        )
                                    )
                                )
                        )

                        // 2. Bottom Info Section (Instagram Reels Style) - Scroll with content
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .graphicsLayer {
                                    // Scroll with content
                                    translationY = screenScrollOffset * 0.3f
                                }
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                                .padding(bottom = 8.dp),
                            color = Color.Transparent
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                // Artist Info Row
                                Row(
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp)
                                        .padding(bottom = 8.dp)
                                ) {
                                    // Circular Avatar with Instagram Story-style gradient ring
                                    var rotation by remember { mutableFloatStateOf(0f) }

                                    LaunchedEffect(Unit) {
                                        while (isActive) {
                                            rotation = (rotation + 1f) % 360f
                                            delay(5000) // Reduced update frequency to improve performance
                                        }
                                    }

                                    Box(
                                        modifier = Modifier.size(52.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Gradient ring using Canvas
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .scale(
                                                    animateFloatAsState(
                                                        targetValue = 1f,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        ),
                                                        label = "ringScale"
                                                    ).value
                                                )
                                        ) {
                                            val strokeWidth = 3.dp.toPx()
                                            val radius = (size.minDimension - strokeWidth) / 2
                                            val center = Offset(size.width / 2, size.height / 2)

                                            // Instagram gradient colors
                                            val gradientColors = listOf(
                                                Color(0xFFC13584), // Purple
                                                Color(0xFFE1306C), // Pink
                                                Color(0xFFFD1D1D), // Red
                                                Color(0xFFF77737), // Orange
                                                Color(0xFFFCAF45), // Yellow
                                                Color(0xFFC13584)  // Purple (loop)
                                            )

                                            drawCircle(
                                                brush = Brush.sweepGradient(
                                                    colors = gradientColors,
                                                    center = center
                                                ),
                                                radius = radius,
                                                center = center,
                                                style = Stroke(width = strokeWidth)
                                            )
                                        }

                                        // White inner border
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .border(
                                                    2.dp,
                                                    Color.Black.copy(alpha = 0.1f),
                                                    CircleShape
                                                )
                                        ) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(mediaMetadata?.thumbnailUrl)
                                                    .allowHardware(false)
                                                    .build(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Song and Artist Info
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Song Title (truncated)
                                        Text(
                                            text = mediaMetadata?.title ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 14.sp
                                        )

                                        // Artist Name
                                        Text(
                                            text = mediaMetadata?.artists?.joinToString { it.name }
                                                ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.9f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 12.sp
                                        )
                                        
                                        // Stream Engine Indicator (Clickable to switch)
                                        val streamEngine by playerConnection.streamEngine.collectAsState()
                                        val availableEngines = remember { mutableStateOf<List<String>>(emptyList()) }
                                        var showEngineMenu by remember { mutableStateOf(false) }
                                        val coroutineScope = rememberCoroutineScope()
                                        
                                        LaunchedEffect(mediaMetadata?.id) {
                                            // Update available engines when media changes
                                            availableEngines.value = playerConnection.getAvailableEngines()
                                        }
                                        
                                        streamEngine?.let { engine ->
                                            val (textColor, displayText) = when (engine) {
                                                "YouTube" -> Color(0xFFFF0000) to "🔰 Source: YouTube"
                                                "YouTube Music" -> Color(0xFFFF005D) to "🎵 Source: YouTube Music"
                                                "JioSaavn" -> Color(0xFF4CAF50) to "🎧 Source: JioSaavn"
                                                "Local" -> Color(0xFF2196F3) to "💾 Source: Local"
                                                else -> Color.White.copy(alpha = 0.7f) to "❓ Source: $engine"
                                            }
                                            
                                            // Make it clickable only if multiple engines available
                                            val isClickable = availableEngines.value.size > 1
                                            
                                            Box {
                                                Text(
                                                    text = displayText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = textColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier
                                                        .padding(top = 2.dp)
                                                        .then(
                                                            if (isClickable) {
                                                                Modifier.clickable { 
                                                                    showEngineMenu = true 
                                                                }
                                                            } else {
                                                                Modifier
                                                            }
                                                        )
                                                )
                                                
                                                // Dropdown menu for switching engines
                                                if (isClickable && availableEngines.value.isNotEmpty()) {
                                                    DropdownMenu(
                                                        expanded = showEngineMenu,
                                                        onDismissRequest = { showEngineMenu = false },
                                                        modifier = Modifier.background(
                                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                                        )
                                                    ) {
                                                        availableEngines.value.forEach { availableEngine ->
                                                            val (optionColor, optionText, optionIcon) = when (availableEngine) {
                                                                "YouTube" -> Triple(Color(0xFFFF0000), "🔰 YouTube", "YouTube")
                                                                "YouTube Music" -> Triple(Color(0xFFFF005D), "🎵 YouTube Music", "YouTube Music")
                                                                "JioSaavn" -> Triple(Color(0xFF4CAF50), "🎧 JioSaavn", "JioSaavn")
                                                                else -> Triple(Color.White.copy(alpha = 0.7f), "❓ $availableEngine", availableEngine)
                                                            }
                                                            
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text(
                                                                        text = optionText,
                                                                        color = if (availableEngine == engine) {
                                                                            optionColor
                                                                        } else {
                                                                            MaterialTheme.colorScheme.onSurface
                                                                        },
                                                                        fontWeight = if (availableEngine == engine) {
                                                                            FontWeight.Bold
                                                                        } else {
                                                                            FontWeight.Normal
                                                                        }
                                                                    )
                                                                },
                                                                onClick = {
                                                                    showEngineMenu = false
                                                                    if (availableEngine != engine) {
                                                                        coroutineScope.launch {
                                                                            playerConnection.switchStreamingEngine(optionIcon)
                                                                        }
                                                                    }
                                                                },
                                                                leadingIcon = if (availableEngine == engine) {
                                                                    {
                                                                        Icon(
                                                                            painter = painterResource(R.drawable.check),
                                                                            contentDescription = null,
                                                                            tint = optionColor,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                } else {
                                                                    null
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Follow Button (Instagram Gradient)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFFC13584), // Purple
                                                        Color(0xFFE1306C), // Pink
                                                        Color(0xFFFD1D1D), // Red
                                                        Color(0xFFF77737)  // Orange
                                                    )
                                                )
                                            )
                                            .clickable {
                                                performHapticFeedback("light")
                                                SecureString.openLink(context, SecureString.LINK_INSTA_APP)
                                            }                                   .padding(horizontal = 16.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = "Follow",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                // Progress Bar with Time Stamps (Instagram Reels Style)
                                mediaMetadata?.let {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp)
                                            .padding(bottom = 12.dp)
                                    ) {
                                        // Time stamps above slider
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 4.dp)
                                        ) {
                                            Text(
                                                text = makeTimeString(sliderPosition ?: position),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = makeTimeString(duration),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp
                                            )
                                        }

                                        // Progress Slider with style support
                                        when (sliderStyle) {
                                            SliderStyle.DEFAULT -> {
                                                Slider(
                                                    value = (sliderPosition ?: position).toFloat(),
                                                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                                    onValueChange = { sliderPosition = it.toLong() },
                                                    onValueChangeFinished = {
                                                        performHapticFeedback("light")
                                                        sliderPosition?.let {
                                                            playerConnection.player.seekTo(it)
                                                        }
                                                    },
                                                    colors = SliderDefaults.colors(
                                                        activeTrackColor = Color.White,
                                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                                        thumbColor = Color.White
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(16.dp)
                                                )
                                            }
                                            SliderStyle.SQUIGGLY -> {
                                                SquigglySlider(
                                                    value = (sliderPosition ?: position).toFloat(),
                                                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                                    onValueChange = { sliderPosition = it.toLong() },
                                                    onValueChangeFinished = {
                                                        performHapticFeedback("light")
                                                        sliderPosition?.let {
                                                            playerConnection.player.seekTo(it)
                                                        }
                                                    },
                                                    colors = SliderDefaults.colors(
                                                        activeTrackColor = Color.White,
                                                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                                        thumbColor = Color.White
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(16.dp),
                                                    squigglesSpec = SquigglySlider.SquigglesSpec(
                                                        amplitude = if (isPlaying) 2.dp else 0.dp,
                                                        strokeWidth = 3.dp
                                                    )
                                                )
                                            }
                                            SliderStyle.SLIM -> {
                                                Slider(
                                                    value = (sliderPosition ?: position).toFloat(),
                                                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                                    onValueChange = { sliderPosition = it.toLong() },
                                                    onValueChangeFinished = {
                                                        performHapticFeedback("light")
                                                        sliderPosition?.let {
                                                            playerConnection.player.seekTo(it)
                                                        }
                                                    },
                                                    thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                                    track = { sliderState ->
                                                        PlayerSliderTrack(
                                                            sliderState = sliderState,
                                                            colors = SliderDefaults.colors(
                                                                activeTrackColor = Color.White,
                                                                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                                                activeTickColor = Color.White,
                                                                inactiveTickColor = Color.White.copy(alpha = 0.3f)
                                                            )
                                                        )
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Interaction Buttons (Right Side - Instagram Reels Style) - Scroll with content
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .graphicsLayer {
                                    // Scroll with content
                                    translationY = screenScrollOffset * 0.3f
                                }
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                                .padding(end = 10.dp, bottom = 140.dp),
                            color = Color.Transparent
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(20.dp), // Instagram-style spacing
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Queue Button
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.list),
                                        contentDescription = "Queue",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                performHapticFeedback("light")
                                                showQueueSheet = true
                                                queueSheetState.expand()
                                            }
                                    )
                                    Text(
                                        text = "Queue",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Like Button (Heart) with enhanced animation
                                val isLiked = currentSong?.song?.liked ?: false
                                var showSideLikeAnimation by remember { mutableStateOf(false) }

                                val likeScale by animateFloatAsState(
                                    targetValue = if (showSideLikeAnimation) 1.3f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "likeScale"
                                )

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                                        ),
                                        contentDescription = "Like",
                                        tint = if (isLiked) Color(0xFFFF1744) else Color.White,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .scale(likeScale)
                                            .clickable {
                                                performHapticFeedback("medium")
                                                showSideLikeAnimation = true
                                                playerConnection.toggleLike()
                                            }
                                    )

                                    LaunchedEffect(showSideLikeAnimation) {
                                        if (showSideLikeAnimation) {
                                            delay(400)
                                            showSideLikeAnimation = false
                                        }
                                    }

                                    Text(
                                        text = "Like",
                                        color = if (isLiked) Color(0xFFFF1744) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Sound Effects Button
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.sound_effect),
                                        contentDescription = "Audio Effects",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                performHapticFeedback("light")
                                                currentPlayerActions.onOpenSoundSettings()
                                            }
                                    )
                                    Text(
                                        text = "Effects",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Download Button
                                var showDownloadDialog by remember { mutableStateOf(false) }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    DownloadButton(
                                        download = currentPlayerState.download,
                                        onClick = {
                                            performHapticFeedback("light")
                                            showDownloadDialog = true
                                        },
                                        onLongClick = {
                                            coroutineScope.launch {
                                                state.collapseSoft()
                                            }
                                            navController.navigate("downloads")
                                        },
                                        modifier = Modifier.size(32.dp),
                                        iconSize = 32.dp,
                                        tint = Color.White
                                    )
                                    Text(
                                        text = "Download",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                // Download Options Dialog
                                if (showDownloadDialog) {
                                    com.musifly.android.ui.component.DownloadDialog(
                                        mediaMetadata = mediaMetadata,
                                        onDismiss = { showDownloadDialog = false }
                                    )
                                }

                                // Timer/Sleep Timer Button
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.bedtime),
                                        contentDescription = "Timer",
                                        tint = if (sleepTimerEnabled) Color(0xFF4CAF50) else Color.White,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                if (sleepTimerEnabled) {
                                                    playerConnection.service.sleepTimer.clear()
                                                } else {
                                                    showSleepTimerDialog = true
                                                }
                                            }
                                    )
                                    Text(
                                        text = "Timer",
                                        color = if (sleepTimerEnabled) Color(0xFF4CAF50) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                        } // End of PlayerLayoutStyle.INSTAGRAM
                    } // End of when (playerLayoutStyle)
                } // End of else -> (portrait mode)
            } // End of when (orientation)

            if (showQueueSheet) {
                Queue(
                    state = queueSheetState,
                    playerBottomSheetState = state,
                    navController = navController,
                    backgroundColor =
                        if (useBlackBackground) {
                            Color.Black
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    onBackgroundColor = onBackgroundColor,
                    textBackgroundColor = TextBackgroundColor,
                )
            }
            
        }
    }
}


}