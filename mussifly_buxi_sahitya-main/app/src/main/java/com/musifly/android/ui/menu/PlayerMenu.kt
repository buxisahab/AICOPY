package com.musifly.android.ui.menu

import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.WatchEndpoint
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalDownloadUtil
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.ListItemHeight
import com.musifly.android.constants.ListThumbnailSize
import com.musifly.android.constants.ThumbnailCornerRadius
import com.musifly.android.models.MediaMetadata
import com.musifly.android.playback.ExoDownloadService
import com.musifly.android.playback.queues.YouTubeQueue
import com.musifly.android.ui.component.BottomSheetState
import com.musifly.android.ui.component.DownloadGridMenu
import com.musifly.android.ui.component.GridMenu
import com.musifly.android.ui.component.GridMenuItem
import com.musifly.android.ui.component.ListDialog
import com.musifly.android.ui.component.ListItem
import com.musifly.android.utils.joinByBullet
import com.musifly.android.utils.makeTimeString
import com.musifly.android.streamingengines.StreamingEngineManager
import com.musifly.android.streamingengines.models.SourceEngine
import com.musifly.android.ui.screens.settings.PreferenceKeys
import com.musifly.android.utils.rememberPreference
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.layout.heightIn
import com.musifly.android.utils.rememberEnumPreference
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import com.musifly.android.constants.PlayerLayoutStyle
import com.musifly.android.constants.PlayerLayoutStyleKey
import com.musifly.android.ui.component.liquidglass.AdaptiveSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    isQueueTrigger: Boolean? = false,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id)
        .collectAsState(initial = null)

    val artists =
        remember(mediaMetadata.artists) {
            mediaMetadata.artists.filter { it.id != null }
        }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(mediaMetadata)
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }
            listOf(mediaMetadata.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        }
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier =
                        Modifier
                            .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            item {
                ListItem(
                    title = mediaMetadata.title,
                    thumbnailContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(mediaMetadata.thumbnailUrl)
                                    .allowHardware(false)
                                    .build(),
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    subtitle =
                        joinByBullet(
                            mediaMetadata.artists.joinToString { it.name },
                            makeTimeString(mediaMetadata.duration * 1000L),
                        ),
                )
            }
        }
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                        Modifier
                            .fillParentMaxWidth()
                            .height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                playerBottomSheetState.collapseSoft()
                                onDismiss()
                            }
                            .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(
            onDismiss = { showPitchTempoDialog = false },
        )
    }

    // Streaming Engines Dialog State
    var showStreamingEnginesDialog by rememberSaveable {
        mutableStateOf(false)
    }

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

    val manager = remember { StreamingEngineManager.getInstance() }

    // Initialize enabled engines
    LaunchedEffect(enableJioSaavn, enableYouTubeMusic, enableYouTubeVideo, enableSpotify) {
        manager.setEnabledEngines(
            jioSaavnEnabled = enableJioSaavn,
            youtubeMusicEnabled = enableYouTubeMusic,
            youtubeVideoEnabled = enableYouTubeVideo,
            spotifyEnabled = enableSpotify
        )
    }

    // Player Switch Dialog State
    var showPlayerSwitchDialog by rememberSaveable {
        mutableStateOf(false)
    }
    
    // Current player layout preference
    val (playerLayoutStyle, onPlayerLayoutStyleChange) = rememberEnumPreference(
        PlayerLayoutStyleKey,
        defaultValue = PlayerLayoutStyle.MUSSIFLY_PREMIUM
    )

    // Check if liquid glass theme is enabled for dialogs
    val (themeModeForDialogs) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlassForDialogs = themeModeForDialogs == ThemeMode.LIQUID_GLASS

    // Streaming Engines Dialog
    if (showStreamingEnginesDialog) {
        if (isLiquidGlassForDialogs) {
            com.musifly.android.ui.component.liquidglass.LiquidGlassDialog(
                onDismiss = { showStreamingEnginesDialog = false },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null
                    )
                },
                title = { Text("Streaming Engines") },
                content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Select which streaming engines to use for searching and playback.",
                        modifier = Modifier.padding(bottom = 16.dp)
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (enableJioSaavn) {
                                    "High quality streaming for Indian content."
                                } else {
                                    "⚠️ Disabled"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(
                            checked = enableJioSaavn,
                            onCheckedChange = { newValue ->
                                onEnableJioSaavnChange(newValue)
                                manager.setEnabledEngines(
                                    jioSaavnEnabled = newValue,
                                    youtubeMusicEnabled = enableYouTubeMusic,
                                    youtubeVideoEnabled = enableYouTubeVideo,
                                    spotifyEnabled = enableSpotify
                                )
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (enableYouTubeMusic) "Stream from YouTube Music" else "⚠️ Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(
                            checked = enableYouTubeMusic,
                            onCheckedChange = { newValue ->
                                onEnableYouTubeMusicChange(newValue)
                                manager.setEnabledEngines(
                                    jioSaavnEnabled = enableJioSaavn,
                                    youtubeMusicEnabled = newValue,
                                    youtubeVideoEnabled = enableYouTubeVideo,
                                    spotifyEnabled = enableSpotify
                                )
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (enableYouTubeVideo) "Stream from YouTube videos" else "⚠️ Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(
                            checked = enableYouTubeVideo,
                            onCheckedChange = { newValue ->
                                onEnableYouTubeVideoChange(newValue)
                                manager.setEnabledEngines(
                                    jioSaavnEnabled = enableJioSaavn,
                                    youtubeMusicEnabled = enableYouTubeMusic,
                                    youtubeVideoEnabled = newValue,
                                    spotifyEnabled = enableSpotify
                                )
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (enableSpotify) "Search tracks from Spotify" else "⚠️ Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(
                            checked = enableSpotify,
                            onCheckedChange = { newValue ->
                                onEnableSpotifyChange(newValue)
                                manager.setEnabledEngines(
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
                buttons = {
                    TextButton(onClick = { showStreamingEnginesDialog = false }) {
                        Text("Done")
                    }
                }
            )
        } else {
        AlertDialog(
            onDismissRequest = { showStreamingEnginesDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null
                )
            },
            title = { Text("Streaming Engines") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Select which streaming engines to use for searching and playback.",
                        modifier = Modifier.padding(bottom = 16.dp)
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (enableJioSaavn) {
                                    "High quality streaming for Indian content."
                                } else {
                                    "⚠️ Disabled"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(
                            checked = enableJioSaavn,
                            onCheckedChange = { newValue ->
                                onEnableJioSaavnChange(newValue)
                                manager.setEnabledEngines(
                                    jioSaavnEnabled = newValue,
                                    youtubeMusicEnabled = enableYouTubeMusic,
                                    youtubeVideoEnabled = enableYouTubeVideo,
                                    spotifyEnabled = enableSpotify
                                )
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (enableYouTubeMusic) "Stream from YouTube Music" else "⚠️ Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(
                            checked = enableYouTubeMusic,
                            onCheckedChange = { newValue ->
                                onEnableYouTubeMusicChange(newValue)
                                manager.setEnabledEngines(
                                    jioSaavnEnabled = enableJioSaavn,
                                    youtubeMusicEnabled = newValue,
                                    youtubeVideoEnabled = enableYouTubeVideo,
                                    spotifyEnabled = enableSpotify
                                )
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (enableYouTubeVideo) "Stream from YouTube videos" else "⚠️ Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(
                            checked = enableYouTubeVideo,
                            onCheckedChange = { newValue ->
                                onEnableYouTubeVideoChange(newValue)
                                manager.setEnabledEngines(
                                    jioSaavnEnabled = enableJioSaavn,
                                    youtubeMusicEnabled = enableYouTubeMusic,
                                    youtubeVideoEnabled = newValue,
                                    spotifyEnabled = enableSpotify
                                )
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
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
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (enableSpotify) "Search tracks from Spotify" else "⚠️ Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(
                            checked = enableSpotify,
                            onCheckedChange = { newValue ->
                                onEnableSpotifyChange(newValue)
                                manager.setEnabledEngines(
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
                TextButton(onClick = { showStreamingEnginesDialog = false }) {
                    Text("Done")
                }
            }
        )
        }
    }

    // List of available layout styles with their titles and descriptions
    val layoutOptions = remember {
        listOf(
            PlayerLayoutStyle.INSTAGRAM to ("Instagram Reels" to "Vertical swipe style with right sidebar"),
            PlayerLayoutStyle.SPOTIFY to ("Spotify" to "Classic Spotify-style player"),
            PlayerLayoutStyle.BLOOME to ("Bloome" to "Tabbed player with lyrics"),
            PlayerLayoutStyle.MUSSIFLY_PREMIUM to ("Mussifly Premium Player" to "Default Mussifly Premium Layout")
        )
    }

    // Player Switch Dialog
    if (showPlayerSwitchDialog) {
        if (isLiquidGlassForDialogs) {
            com.musifly.android.ui.component.liquidglass.LiquidGlassDialog(
                onDismiss = { showPlayerSwitchDialog = false },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.swap_vert),
                        contentDescription = null
                    )
                },
                title = { Text(stringResource(R.string.player_layout)) },
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Choose your preferred player layout",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        layoutOptions.forEach { (layout, texts) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPlayerLayoutStyleChange(layout)
                                        // We don't call onDismiss() here immediately to allow the preference update to complete
                                        // and to let the user see the selection change in the dialog.
                                        // The user can then click anywhere outside or 'Cancel' to close.
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (playerLayoutStyle == layout) 
                                            R.drawable.radio_button_checked 
                                        else 
                                            R.drawable.radio_button_unchecked
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column {
                                    Text(
                                        text = texts.first,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (playerLayoutStyle == layout) 
                                            FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = texts.second,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (layout != PlayerLayoutStyle.MUSSIFLY_PREMIUM) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                },
                buttons = {
                    TextButton(onClick = { showPlayerSwitchDialog = false }) {
                        Text("Done") // Changed from Cancel to Done for better UX when item remains selected
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showPlayerSwitchDialog = false },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.swap_vert),
                        contentDescription = null
                    )
                },
                title = { Text(stringResource(R.string.player_layout)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Choose your preferred player layout",
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        layoutOptions.forEach { (layout, texts) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onPlayerLayoutStyleChange(layout)
                                        // Keeping onDismiss() here for material dialog as it's the existing behavior
                                        // but removed for Liquid Glass to ensure reactivity works better there.
                                        showPlayerSwitchDialog = false
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (playerLayoutStyle == layout) 
                                            R.drawable.radio_button_checked 
                                        else 
                                            R.drawable.radio_button_unchecked
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Column {
                                    Text(
                                        text = texts.first,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (playerLayoutStyle == layout) 
                                            FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = texts.second,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (layout != PlayerLayoutStyle.MUSSIFLY_PREMIUM) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlayerSwitchDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    if (isQueueTrigger != true) {
        // State to track if audio is muted
        var isMuted by remember { mutableStateOf(false) }

        // Store the volume before muting to restore later
        var previousVolume by remember { mutableFloatStateOf(playerVolume.value) }

        // Check if liquid glass theme is enabled
        val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
        val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

        if (isLiquidGlass) {
            LiquidGlassDialog(
                onDismiss = onDismiss,
                content = {
                    GridMenu(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                              /* Volume bar removed as per request */

                        }

                        GridMenuItem(
                            icon = R.drawable.radio,
                            title = R.string.start_radio,
                        ) {
                            playerConnection.playQueue(
                                YouTubeQueue(
                                    WatchEndpoint(videoId = mediaMetadata.id),
                                    mediaMetadata
                                )
                            )
                            onDismiss()
                        }
                        GridMenuItem(
                            icon = R.drawable.playlist_add,
                            title = R.string.add_to_playlist,
                        ) {
                            showChoosePlaylistDialog = true
                        }
                        DownloadGridMenu(
                            state = download?.state,
                            onDownload = {
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
                            },
                            onRemoveDownload = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                                        context.applicationContext,
                                        com.musifly.android.di.CachedDownloadsRepositoryEntryPoint::class.java
                                    )
                                    val cachedRepo = entryPoint.cachedDownloadsRepository()
                                    cachedRepo.removeCachedDownload(mediaMetadata.id)

                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        DownloadService.sendRemoveDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            mediaMetadata.id,
                                            false,
                                        )
                                    }
                                }
                            },
                        )
                        // Player Layout - First position for easy access
                        GridMenuItem(
                            icon = R.drawable.swap_vert,
                            title = R.string.player_layout,
                        ) {
                            showPlayerSwitchDialog = true
                        }
                        if (librarySong?.song?.inLibrary != null) {
                            GridMenuItem(
                                icon = R.drawable.library_add_check,
                                title = R.string.remove_from_library,
                            ) {
                                database.query {
                                    inLibrary(mediaMetadata.id, null)
                                }
                            }
                        } else {
                            GridMenuItem(
                                icon = R.drawable.library_add,
                                title = R.string.add_to_library,
                            ) {
                                database.transaction {
                                    insert(mediaMetadata)
                                    inLibrary(mediaMetadata.id, LocalDateTime.now())
                                }
                            }
                        }
                        if (artists.isNotEmpty()) {
                            GridMenuItem(
                                icon = R.drawable.artist,
                                title = R.string.view_artist,
                            ) {
                                if (mediaMetadata.artists.size == 1) {
                                    navController.navigate("artist/${mediaMetadata.artists[0].id}")
                                    playerBottomSheetState.collapseSoft()
                                    onDismiss()
                                } else {
                                    showSelectArtistDialog = true
                                }
                            }
                        }
                        if (mediaMetadata.album != null) {
                            GridMenuItem(
                                icon = R.drawable.album,
                                title = R.string.view_album,
                            ) {
                                navController.navigate("album/${mediaMetadata.album.id}")
                                playerBottomSheetState.collapseSoft()
                                onDismiss()
                            }
                        }
                        GridMenuItem(
                            icon = R.drawable.share,
                            title = R.string.share,
                        ) {
                            val intent =
                                Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                                    )
                                }
                            context.startActivity(Intent.createChooser(intent, null))
                            onDismiss()
                        }
                        GridMenuItem(
                            icon = R.drawable.info,
                            title = R.string.details,
                        ) {
                            onShowDetailsDialog()
                            onDismiss()
                        }
                        
                        GridMenuItem(
                            icon = R.drawable.tune,
                            title = R.string.advanced,
                        ) {
                            showPitchTempoDialog = true
                        }
                        GridMenuItem(
                            icon = R.drawable.music_note,
                            title = R.string.streaming_engines,
                        ) {
                            showStreamingEnginesDialog = true
                        }
                    }
                }
            )
        } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            dragHandle = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    // Drag handle
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                /* Volume bar removed as per request */


                // Grid de opciones
                GridMenu(
                    contentPadding = PaddingValues(
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding() + 16.dp
                    ),
                ) {
                    GridMenuItem(
                        icon = R.drawable.radio,
                        title = R.string.start_radio,
                    ) {
                        playerConnection.playQueue(
                            YouTubeQueue(
                                WatchEndpoint(videoId = mediaMetadata.id),
                                mediaMetadata
                            )
                        )
                        onDismiss()
                    }
                    GridMenuItem(
                        icon = R.drawable.playlist_add,
                        title = R.string.add_to_playlist,
                    ) {
                        showChoosePlaylistDialog = true
                    }
                    DownloadGridMenu(
                        state = download?.state,
                        onDownload = {
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
                        },
                        onRemoveDownload = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                                    context.applicationContext,
                                    com.musifly.android.di.CachedDownloadsRepositoryEntryPoint::class.java
                                )
                                val cachedRepo = entryPoint.cachedDownloadsRepository()
                                cachedRepo.removeCachedDownload(mediaMetadata.id)

                                kotlinx.coroutines.withContext(Dispatchers.Main) {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        mediaMetadata.id,
                                        false,
                                    )
                                }
                            }
                        },
                    )
                    // Player Layout - First position for easy access
                    GridMenuItem(
                        icon = R.drawable.swap_vert,
                        title = R.string.player_layout,
                    ) {
                        showPlayerSwitchDialog = true
                    }
                    if (librarySong?.song?.inLibrary != null) {
                        GridMenuItem(
                            icon = R.drawable.library_add_check,
                            title = R.string.remove_from_library,
                        ) {
                            database.query {
                                inLibrary(mediaMetadata.id, null)
                            }
                        }
                    } else {
                        GridMenuItem(
                            icon = R.drawable.library_add,
                            title = R.string.add_to_library,
                        ) {
                            database.transaction {
                                insert(mediaMetadata)
                                inLibrary(mediaMetadata.id, LocalDateTime.now())
                            }
                        }
                    }
                    if (artists.isNotEmpty()) {
                        GridMenuItem(
                            icon = R.drawable.artist,
                            title = R.string.view_artist,
                        ) {
                            if (mediaMetadata.artists.size == 1) {
                                navController.navigate("artist/${mediaMetadata.artists[0].id}")
                                playerBottomSheetState.collapseSoft()
                                onDismiss()
                            } else {
                                showSelectArtistDialog = true
                            }
                        }
                    }
                    if (mediaMetadata.album != null) {
                        GridMenuItem(
                            icon = R.drawable.album,
                            title = R.string.view_album,
                        ) {
                            navController.navigate("album/${mediaMetadata.album.id}")
                            playerBottomSheetState.collapseSoft()
                            onDismiss()
                        }
                    }
                    GridMenuItem(
                        icon = R.drawable.share,
                        title = R.string.share,
                    ) {
                        val intent =
                            Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                                )
                            }
                        context.startActivity(Intent.createChooser(intent, null))
                        onDismiss()
                    }
                    GridMenuItem(
                        icon = R.drawable.info,
                        title = R.string.details,
                    ) {
                        onShowDetailsDialog()
                        onDismiss()
                    }
                    // Equalizer removed - depends on system apps that may not be available
                    
                    GridMenuItem(
                        icon = R.drawable.tune,
                        title = R.string.advanced,
                    ) {
                        showPitchTempoDialog = true
                    }
                    GridMenuItem(
                        icon = R.drawable.music_note,
                        title = R.string.streaming_engines,
                    ) {
                        showStreamingEnginesDialog = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TempoPitchDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableIntStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters =
            PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
    }

    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    if (isLiquidGlass) {
        com.musifly.android.ui.component.liquidglass.LiquidGlassDialog(
            onDismiss = onDismiss,
            title = { Text(stringResource(R.string.tempo_and_pitch)) },
            buttons = {
                TextButton(
                    onClick = {
                        tempo = 1f
                        transposeValue = 0
                        updatePlaybackParameters()
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            content = {
                Column {
                    ValueAdjuster(
                        icon = R.drawable.speed,
                        currentValue = tempo,
                        values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                        onValueUpdate = {
                            tempo = it
                            updatePlaybackParameters()
                        },
                        valueText = { "x$it" },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    ValueAdjuster(
                        icon = R.drawable.discover_tune,
                        currentValue = transposeValue,
                        values = (-12..12).toList(),
                        onValueUpdate = {
                            transposeValue = it
                            updatePlaybackParameters()
                        },
                        valueText = { "${if (it > 0) "+" else ""}$it" },
                    )
                }
            }
        )
    } else {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = onDismiss,
            title = {
                Text(stringResource(R.string.tempo_and_pitch))
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        tempo = 1f
                        transposeValue = 0
                        updatePlaybackParameters()
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                Column {
                    ValueAdjuster(
                        icon = R.drawable.speed,
                        currentValue = tempo,
                        values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                        onValueUpdate = {
                            tempo = it
                            updatePlaybackParameters()
                        },
                        valueText = { "x$it" },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    ValueAdjuster(
                        icon = R.drawable.discover_tune,
                        currentValue = transposeValue,
                        values = (-12..12).toList(),
                        onValueUpdate = {
                            transposeValue = it
                            updatePlaybackParameters()
                        },
                        valueText = { "${if (it > 0) "+" else ""}$it" },
                    )
                }
            },
        )
    }
}

@Composable
fun <T> ValueAdjuster(
    @DrawableRes icon: Int,
    currentValue: T,
    values: List<T>,
    onValueUpdate: (T) -> Unit,
    valueText: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.remove),
                contentDescription = null,
            )
        }

        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp),
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = null,
            )
        }
    }
}