package com.musifly.android.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.R
import com.musifly.android.constants.AudioNormalizationKey
import com.musifly.android.constants.AudioQuality
import com.musifly.android.constants.AudioQualityKey
import com.musifly.android.constants.AutoLoadMoreKey
import com.musifly.android.constants.AutoSkipNextOnErrorKey
import com.musifly.android.constants.PersistentQueueKey
import com.musifly.android.constants.PlayerLayoutStyle
import com.musifly.android.constants.PlayerLayoutStyleKey
import com.musifly.android.constants.SimilarContent
import com.musifly.android.constants.SkipSilenceKey
import com.musifly.android.constants.SliderStyle
import com.musifly.android.constants.SliderStyleKey
import com.musifly.android.constants.StopMusicOnTaskClearKey
import com.musifly.android.constants.ThumbnailDisplayMode
import com.musifly.android.constants.ThumbnailDisplayModeKey
import com.musifly.android.constants.UseNewPipeStreamingKey
import com.musifly.android.streamingengines.StreamingEngineManager
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.ListDialog
import com.musifly.android.ui.component.PreferenceEntry

import com.musifly.android.ui.component.SettingsGroup
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.liquidGlassSubtle
import com.musifly.android.ui.component.liquidglass.AdaptiveSwitch
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    // Preferences
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = true
    )
    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(
        AutoLoadMoreKey,
        defaultValue = true
    )
    val (similarContentEnabled, similarContentEnabledChange) = rememberPreference(
        key = SimilarContent,
        defaultValue = true
    )
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(
        AutoSkipNextOnErrorKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = false
    )
    val (useNewPipeStreaming, onUseNewPipeStreamingChange) = rememberPreference(
        UseNewPipeStreamingKey,
        defaultValue = false
    )
    
    // Player Layout Style
    val (playerLayoutStyle, onPlayerLayoutStyleChange) = rememberEnumPreference(
        PlayerLayoutStyleKey,
        defaultValue = PlayerLayoutStyle.MUSSIFLY_PREMIUM
    )
    
    // Thumbnail Display Mode
    val (thumbnailDisplayMode, onThumbnailDisplayModeChange) = rememberEnumPreference(
        ThumbnailDisplayModeKey,
        defaultValue = ThumbnailDisplayMode.BLUR_HD
    )
    
    // Music Progress Style
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.DEFAULT
    )
    
    // Streaming Engine Preferences
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
    
    // Get country code from ContentSettings to check availability
    val (contentCountry, _) = rememberPreference(
        key = PreferenceKeys.ContentCountryKey,
        defaultValue = "system"
    )
    
    // Update StreamingEngineManager with country code
    val manager = remember { StreamingEngineManager.getInstance() }
    
    // Determine actual country code - Default to IN for testing
    val actualCountry = remember(contentCountry) {
        val systemLocale = java.util.Locale.getDefault().country
        when {
            contentCountry != "system" && contentCountry.isNotEmpty() -> contentCountry
            systemLocale.isNotEmpty() -> systemLocale
            else -> "IN" // Default to India for JioSaavn availability
        }
    }
    
    // Initialize country code and enabled engines
    LaunchedEffect(actualCountry, enableJioSaavn, enableYouTubeMusic, enableYouTubeVideo, enableSpotify) {
        manager.setCountryCode(actualCountry)
        manager.setEnabledEngines(
            jioSaavnEnabled = enableJioSaavn,
            youtubeMusicEnabled = enableYouTubeMusic,
            youtubeVideoEnabled = enableYouTubeVideo,
            spotifyEnabled = enableSpotify
        )
    }
    
    // Get available engines based on country and user preferences
    val availableEngines = remember(actualCountry, enableJioSaavn, enableYouTubeMusic, enableYouTubeVideo, enableSpotify) {
        manager.setCountryCode(actualCountry)
        manager.setEnabledEngines(
            jioSaavnEnabled = enableJioSaavn,
            youtubeMusicEnabled = enableYouTubeMusic,
            youtubeVideoEnabled = enableYouTubeVideo,
            spotifyEnabled = enableSpotify
        )
        manager.getAvailableEngines()
    }
    
    var showNewPipeInfoDialog by remember { mutableStateOf(false) }
    var showStreamingEnginesDialog by remember { mutableStateOf(false) }
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showPlayerLayoutDialog by remember { mutableStateOf(false) }
    var showSliderStyleDialog by remember { mutableStateOf(false) }
    var showThumbnailModeDialog by remember { mutableStateOf(false) }

    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    // Dialogs
    if (showNewPipeInfoDialog) {
        val dialogTitle: @Composable () -> Unit = { Text("NewPipe Streaming Engine") }
        val dialogContent: @Composable ColumnScope.() -> Unit = {
            Column {
                Text("🎵 Professional Audio Streaming\n")
                Text("✓ High-quality audio streams (M4A/Opus preferred)")
                Text("✓ Large buffer (25-60s) for uninterrupted playback")
                Text("✓ Smart caching system (100MB)")
                Text("✓ Automatic error recovery & fallback")
                Text("✓ No downloads required - stream like Spotify")
                Text("\n⚡ This provides the smoothest streaming experience with minimal interruptions.")
            }
        }
        val buttonsContent: @Composable RowScope.() -> Unit = {
            if (isLiquidGlass) {
                 com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                    onClick = { showNewPipeInfoDialog = false },
                    text = "Got it",
                    isPrimary = true
                 )
            } else {
                TextButton(onClick = { showNewPipeInfoDialog = false }) { Text("Got it") }
            }
        }

        if (isLiquidGlass) {
            LiquidGlassDialog(
                onDismiss = { showNewPipeInfoDialog = false },
                icon = { Icon(painter = painterResource(R.drawable.play_circle), contentDescription = null) },
                title = dialogTitle,
                content = dialogContent,
                buttons = buttonsContent
            )
        } else {
            AlertDialog(
                onDismissRequest = { showNewPipeInfoDialog = false },
                icon = { Icon(painter = painterResource(R.drawable.play_circle), contentDescription = null) },
                title = dialogTitle,
                text = { Column { dialogContent() } },
                confirmButton = { Row { buttonsContent() } }
            )
        }
    }

    // Audio Quality Dialog
    if (showAudioQualityDialog) {
        ListDialog(onDismiss = { showAudioQualityDialog = false }) {
            items(AudioQuality.values()) { value ->
                Row(
                   verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                   modifier = Modifier.fillMaxWidth().clickable { showAudioQualityDialog = false; onAudioQualityChange(value) }.padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                   Text(
                       text = when (value) {
                           AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                           AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                           AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                       },
                       style = MaterialTheme.typography.bodyLarge,
                       color = if (value == audioQuality) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                       modifier = Modifier.weight(1f)
                   )
                   
                   if (value == audioQuality) {
                       Icon(
                           painter = painterResource(R.drawable.check),
                           contentDescription = null,
                           tint = MaterialTheme.colorScheme.primary,
                           modifier = Modifier.size(24.dp)
                       )
                   }
                }
            }
        }
    }
    
    // Player Layout Dialog
    if (showPlayerLayoutDialog) {
        ListDialog(onDismiss = { showPlayerLayoutDialog = false }) {
            items(listOf(PlayerLayoutStyle.SPOTIFY, PlayerLayoutStyle.BLOOME, PlayerLayoutStyle.MUSSIFLY_PREMIUM)) { value ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showPlayerLayoutDialog = false; onPlayerLayoutStyleChange(value) }.padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = when (value) {
                            PlayerLayoutStyle.SPOTIFY -> stringResource(R.string.player_layout_spotify)
                            PlayerLayoutStyle.BLOOME -> "Bloome"
                            PlayerLayoutStyle.MUSSIFLY_PREMIUM -> "Mussifly Premium Player"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (value == playerLayoutStyle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (value == playerLayoutStyle) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Slider Style Dialog
    if (showSliderStyleDialog) {
        ListDialog(onDismiss = { showSliderStyleDialog = false }) {
             items(SliderStyle.values()) { value ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showSliderStyleDialog = false; onSliderStyleChange(value) }.padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = when (value) {
                            SliderStyle.DEFAULT -> "Default"
                            SliderStyle.SQUIGGLY -> "Squiggly"
                            SliderStyle.SLIM -> "Slim"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (value == sliderStyle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (value == sliderStyle) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Thumbnail Mode Dialog
    if (showThumbnailModeDialog) {
        ListDialog(onDismiss = { showThumbnailModeDialog = false }) {
             items(ThumbnailDisplayMode.values()) { value ->
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { showThumbnailModeDialog = false; onThumbnailDisplayModeChange(value) }.padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = when (value) {
                            ThumbnailDisplayMode.BLUR_HD -> "Instagram/Resso (Blur + HD)"
                            ThumbnailDisplayMode.CENTER_CROP -> "Center Crop (Immersive)"
                            ThumbnailDisplayMode.FIT_CENTER -> "Fit Center (Full Artwork)"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (value == thumbnailDisplayMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (value == thumbnailDisplayMode) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // Streaming Engines Dialog
    if (showStreamingEnginesDialog) {
        val dialogTitle: @Composable () -> Unit = { Text("Streaming Engines") }
        val dialogContent: @Composable ColumnScope.() -> Unit = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(text = "Select which streaming engines to use for searching and playback.", modifier = Modifier.padding(bottom = 16.dp))
                
                // JioSaavn Switch
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "JioSaavn (320kbps)", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(text = if (enableJioSaavn) "High quality streaming for Indian content." else "⚠️ Disabled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AdaptiveSwitch(checked = enableJioSaavn, onCheckedChange = { newValue -> onEnableJioSaavnChange(newValue); manager.setEnabledEngines(jioSaavnEnabled = newValue, youtubeMusicEnabled = enableYouTubeMusic, youtubeVideoEnabled = enableYouTubeVideo) })
                }
                
                // YouTube Music Switch
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "YouTube Music", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(text = if (enableYouTubeMusic) "Stream from YouTube Music" else "⚠️ Disabled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AdaptiveSwitch(checked = enableYouTubeMusic, onCheckedChange = { newValue -> onEnableYouTubeMusicChange(newValue); manager.setEnabledEngines(jioSaavnEnabled = enableJioSaavn, youtubeMusicEnabled = newValue, youtubeVideoEnabled = enableYouTubeVideo) })
                }
                
                // YouTube Video Switch
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "YouTube Video", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(text = if (enableYouTubeVideo) "Stream from YouTube videos" else "⚠️ Disabled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AdaptiveSwitch(checked = enableYouTubeVideo, onCheckedChange = { newValue -> onEnableYouTubeVideoChange(newValue); manager.setEnabledEngines(jioSaavnEnabled = enableJioSaavn, youtubeMusicEnabled = enableYouTubeMusic, youtubeVideoEnabled = newValue, spotifyEnabled = enableSpotify) })
                }
                
                // Spotify Switch
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Spotify", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(text = if (enableSpotify) "Search tracks from Spotify (metadata only)" else "⚠️ Disabled", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    AdaptiveSwitch(checked = enableSpotify, onCheckedChange = { newValue -> onEnableSpotifyChange(newValue); manager.setEnabledEngines(jioSaavnEnabled = enableJioSaavn, youtubeMusicEnabled = enableYouTubeMusic, youtubeVideoEnabled = enableYouTubeVideo, spotifyEnabled = newValue) })
                }
            }
        }
        val buttonsContent: @Composable RowScope.() -> Unit = {
            if (isLiquidGlass) {
                 com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                    onClick = { showStreamingEnginesDialog = false },
                    text = "Done",
                    isPrimary = true
                 )
            } else {
                TextButton(onClick = { showStreamingEnginesDialog = false }) { Text("Done") }
            }
        }

        if (isLiquidGlass) {
            LiquidGlassDialog(
                onDismiss = { showStreamingEnginesDialog = false },
                icon = { Icon(painter = painterResource(R.drawable.apps), contentDescription = null) },
                title = dialogTitle,
                content = dialogContent,
                buttons = buttonsContent
            )
        } else {
            AlertDialog(
                onDismissRequest = { showStreamingEnginesDialog = false },
                icon = { Icon(painter = painterResource(R.drawable.apps), contentDescription = null) },
                title = dialogTitle,
                text = { Column { dialogContent() } },
                confirmButton = { Row { buttonsContent() } }
            )
        }
    }

    val (liveAlertEnabled, onLiveAlertEnabledChange) = rememberPreference(
        com.musifly.android.constants.LiveAlertEnabledKey,
        defaultValue = true
    )
    val (verticalSwipeEnabled, onVerticalSwipeEnabledChange) = rememberPreference(
        com.musifly.android.constants.VerticalSwipeSongChangeKey,
        defaultValue = true
    )




    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(if (isLiquidGlass) Modifier.liquidGlassSubtle() else Modifier),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.player_and_audio)) },
                navigationIcon = {
                    com.musifly.android.ui.component.AppIconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = LocalPlayerAwareWindowInsets.current
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            
            // PLAYER & AUDIO GROUP
            SettingsGroup(
                title = stringResource(R.string.player)
            ) {
                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.sound_effect), null) },
                    iconBackgroundColor = Color(0xFF2196F3), // Blue
                    title = { Text(stringResource(R.string.audio_quality)) },
                    description = when (audioQuality) {
                             AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                             AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                             AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                    },
                    onClick = { showAudioQualityDialog = true }
                )
                

                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.view_quilt), null) },
                    iconBackgroundColor = Color(0xFF673AB7), // Deep Purple
                    title = { Text(stringResource(R.string.player_layout)) },
                    description = when (playerLayoutStyle) {
                           PlayerLayoutStyle.SPOTIFY -> stringResource(R.string.player_layout_spotify)
                           PlayerLayoutStyle.BLOOME -> "Bloome"
                           PlayerLayoutStyle.MUSSIFLY_PREMIUM -> "Mussifly Premium Player"
                           else -> ""
                    },
                    onClick = { showPlayerLayoutDialog = true }
                )
                

                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.linear_scale), null) },
                    iconBackgroundColor = Color(0xFF4CAF50), // Green
                    title = { Text(stringResource(R.string.music_progress_style)) },
                    description = when (sliderStyle) {
                            SliderStyle.DEFAULT -> "Default"
                            SliderStyle.SQUIGGLY -> "Squiggly"
                            SliderStyle.SLIM -> "Slime"
                    },
                    onClick = { showSliderStyleDialog = true }
                )
                

                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.crop), null) },
                    iconBackgroundColor = Color(0xFFFFA000), // Orange
                    title = { Text(stringResource(R.string.thumbnail_display_mode)) },
                    description = when (thumbnailDisplayMode) {
                            ThumbnailDisplayMode.BLUR_HD -> "Blur + HD"
                            ThumbnailDisplayMode.FIT_CENTER -> "FitCenter"
                            ThumbnailDisplayMode.CENTER_CROP -> "CenterCrop"
                    },
                    onClick = { showThumbnailModeDialog = true }
                )
            }
            
            // STREAMING & DSP
             SettingsGroup(
                 title = "Streaming & Audio Logic"
             ) {
                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.play_circle), null) },
                    iconBackgroundColor = Color(0xFFF44336), // Red
                    title = { Text(stringResource(R.string.newpipe_streaming_engine)) },
                    description = if (useNewPipeStreaming) "On" else "Off",
                    trailingContent = { 
                        AdaptiveSwitch(
                            checked = useNewPipeStreaming, 
                            onCheckedChange = { 
                                onUseNewPipeStreamingChange(it)
                                if (it) showNewPipeInfoDialog = true
                            }
                        ) 
                    },
                    onClick = { 
                        onUseNewPipeStreamingChange(!useNewPipeStreaming)
                         if (!useNewPipeStreaming) showNewPipeInfoDialog = true
                    }
                )
                

                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.apps), null) },
                    iconBackgroundColor = Color(0xFF9C27B0), // Purple
                    title = { Text(stringResource(R.string.manage_streaming_engines)) },
                    description = stringResource(R.string.streaming_engines_enabled, listOf(enableJioSaavn, enableYouTubeMusic, enableYouTubeVideo, enableSpotify).count { it }),
                    onClick = { showStreamingEnginesDialog = true }
                )
                

                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.equalizer), null) },
                    iconBackgroundColor = Color(0xFF00BCD4), // Cyan
                    title = { Text(stringResource(R.string.audio_normalization)) },
                    trailingContent = { AdaptiveSwitch(checked = audioNormalization, onCheckedChange = onAudioNormalizationChange) },
                    onClick = { onAudioNormalizationChange(!audioNormalization) }
                )
                

                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.fast_forward), null) },
                    iconBackgroundColor = Color(0xFFE91E63), // Pink
                    title = { Text(stringResource(R.string.skip_silence)) },
                    trailingContent = { AdaptiveSwitch(checked = skipSilence, onCheckedChange = onSkipSilenceChange) },
                    onClick = { onSkipSilenceChange(!skipSilence) }
                )
                

                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.tune), null) },
                    iconBackgroundColor = Color(0xFF607D8B), // Blue Grey
                    title = { Text(stringResource(R.string.dsp_settings)) },
                    onClick = { navController.navigate("settings/dsp") }
                )
                
                
                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.auto_awesome), null) },
                    iconBackgroundColor = Color(0xFF009688), // Teal
                    title = { Text(stringResource(R.string.audio_effects)) },
                    description = stringResource(R.string.audio_effects_desc),
                    onClick = { navController.navigate("settings/audio_effects") }
                )
            }

            // QUEUE & LISTS
            SettingsGroup(
                title = stringResource(R.string.queue)
            ) {
                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.save), null) },
                    iconBackgroundColor = Color(0xFF3F51B5), // Indigo
                    title = { Text(stringResource(R.string.persistent_queue)) },
                    trailingContent = { AdaptiveSwitch(checked = persistentQueue, onCheckedChange = onPersistentQueueChange) },
                    onClick = { onPersistentQueueChange(!persistentQueue) }
                )
                

                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.playlist_play), null) },
                    iconBackgroundColor = Color(0xFF795548), // Brown
                    title = { Text(stringResource(R.string.auto_load_more)) },
                    trailingContent = { AdaptiveSwitch(checked = autoLoadMore, onCheckedChange = onAutoLoadMoreChange) },
                    onClick = { onAutoLoadMoreChange(!autoLoadMore) }
                )
                

                 PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.auto_awesome), null) },
                    iconBackgroundColor = Color(0xFFFFC107), // Amber
                    title = { Text(stringResource(R.string.enable_similar_content)) },
                    trailingContent = { AdaptiveSwitch(checked = similarContentEnabled, onCheckedChange = similarContentEnabledChange) },
                    onClick = { similarContentEnabledChange(!similarContentEnabled) }
                )
                

                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.skip_next), null) },
                    iconBackgroundColor = Color(0xFF8BC34A), // Light Green
                    title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
                    trailingContent = { AdaptiveSwitch(checked = autoSkipNextOnError, onCheckedChange = onAutoSkipNextOnErrorChange) },
                    onClick = { onAutoSkipNextOnErrorChange(!autoSkipNextOnError) }
                )
            }
            
            // MISC
             SettingsGroup(
                title = stringResource(R.string.misc)
            ) {
                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.notification_on), null) },
                    iconBackgroundColor = Color(0xFFCDDC39), // Lime
                    title = { Text(stringResource(R.string.live_alert)) },
                    trailingContent = { AdaptiveSwitch(checked = liveAlertEnabled, onCheckedChange = onLiveAlertEnabledChange) },
                    onClick = { onLiveAlertEnabledChange(!liveAlertEnabled) }
                )
                

                PreferenceEntry(
                     icon = { Icon(painterResource(R.drawable.swipe_vertical), null) },
                     iconBackgroundColor = Color(0xFF000000), // Black
                     title = { Text(stringResource(R.string.vertical_swipe_song_change)) },
                     trailingContent = { AdaptiveSwitch(checked = verticalSwipeEnabled, onCheckedChange = onVerticalSwipeEnabledChange) },
                     onClick = { onVerticalSwipeEnabledChange(!verticalSwipeEnabled) }
                )
                

                PreferenceEntry(
                    icon = { Icon(painterResource(R.drawable.power_settings_new), null) },
                    iconBackgroundColor = Color(0xFFFF5722), // Deep Orange
                    title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                    trailingContent = { AdaptiveSwitch(checked = stopMusicOnTaskClear, onCheckedChange = onStopMusicOnTaskClearChange) },
                    onClick = { onStopMusicOnTaskClearChange(!stopMusicOnTaskClear) }
                )
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}
