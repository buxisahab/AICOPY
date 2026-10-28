package com.musifly.android.ui.player

import android.annotation.SuppressLint
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.ListItemHeight
import com.musifly.android.constants.NavigationBarHeight
import com.musifly.android.constants.QueueEditLockKey
import com.musifly.android.constants.ShowLyricsKey
import com.musifly.android.extensions.metadata
import com.musifly.android.extensions.move
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.extensions.toggleRepeatMode
import com.musifly.android.models.MediaMetadata
import com.musifly.android.ui.component.BottomSheet
import com.musifly.android.ui.component.BottomSheetState
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.component.MediaMetadataListItem
import com.musifly.android.ui.menu.PlayerMenu
import com.musifly.android.ui.menu.SelectionMediaMetadataMenu
import com.musifly.android.utils.makeTimeString
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.graphicsLayer
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.blur
import com.musifly.android.ui.component.liquidglass.backdrop.effects.colorControls
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.Backdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.layerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberCombinedBackdrop
import com.musifly.android.ui.component.BottomSheetMenu
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy


@SuppressLint("UnrememberedMutableState", "StringFormatInvalid")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Queue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onBackgroundColor: Color,
    textBackgroundColor: Color,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val menuState = LocalMenuState.current

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    val selectedSongs: MutableList<MediaMetadata> = mutableStateListOf()
    val selectedItems: MutableList<Timeline.Window> = mutableStateListOf()
    var selection by remember {
        mutableStateOf(false)
    }

    var showDetailsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var locked by rememberPreference(QueueEditLockKey, defaultValue = true)

    var showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    val snackbarHostState = remember { SnackbarHostState() }
    var dismissJob: Job? by remember { mutableStateOf(null) }

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerValue by remember { mutableStateOf(30f) }
    val sleepTimerEnabled = remember(
        playerConnection.service.sleepTimer.triggerTime,
        playerConnection.service.sleepTimer.pauseWhenSongEnd
    ) {
        playerConnection.service.sleepTimer.isActive
    }
    var sleepTimerTimeLeft by remember { mutableStateOf(0L) }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                }
                delay(1000L)
            }
        }
    }

    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    if (showDetailsDialog) {
        val dialogTitle: @Composable () -> Unit = {
            Text(
                text = stringResource(R.string.details),
                style = MaterialTheme.typography.titleLarge
            )
        }
        val dialogContent: @Composable ColumnScope.() -> Unit = {
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
        }
        val buttonsContent: @Composable RowScope.() -> Unit = {
            TextButton(
                onClick = { showDetailsDialog = false },
            ) {
                Text(stringResource(android.R.string.ok))
            }
        }

        if (isLiquidGlass) {
            LiquidGlassDialog(
                onDismiss = { showDetailsDialog = false },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                    )
                },
                title = dialogTitle,
                content = dialogContent,
                buttons = buttonsContent
            )
        } else {
            AlertDialog(
                properties = DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = { showDetailsDialog = false },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                    )
                },
                confirmButton = { Row { buttonsContent() } },
                text = { Column { dialogContent() } },
            )
        }
    }

    val queueTitle by playerConnection.queueTitle.collectAsState()
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val automix by playerConnection.service.automixItems.collectAsState()
    val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
    val queueLength =
        remember(queueWindows) {
            queueWindows.sumOf { it.mediaItem.metadata!!.duration }
        }

    val backdrop = if (isLiquidGlass) rememberLayerBackdrop() else null
    val coroutineScope = rememberCoroutineScope()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

    val headerItems = 1
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onMove = { from, to ->
            if (to.index >= headerItems && from.index >= headerItems) {
                mutableQueueWindows.move(
                    from.index - headerItems,
                    to.index - headerItems,
                )

                val safeFrom = (from.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
                val safeTo = (to.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)

                if (!playerConnection.player.shuffleModeEnabled) {
                    playerConnection.player.moveMediaItem(safeFrom, safeTo)
                } else {
                    playerConnection.player.setShuffleOrder(
                        DefaultShuffleOrder(
                            queueWindows
                                .map { it.firstPeriodIndex }
                                .toMutableList()
                                .move(safeFrom, safeTo)
                                .toIntArray(),
                            System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }
    )

    val headerUI: @Composable () -> Unit = {
        Column {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                Text(
                    text = queueTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                AnimatedVisibility(
                    visible = !selection,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                ) {
                    Row {
                        IconButton(
                            onClick = { locked = !locked },
                            modifier = Modifier.padding(horizontal = 6.dp),
                        ) {
                            Icon(
                                painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                                contentDescription = null,
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.n_song,
                            queueWindows.size,
                            queueWindows.size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Text(
                        text = makeTimeString(queueLength * 1000L),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            AnimatedVisibility(
                visible = selection,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier = Modifier.height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val count = selectedSongs.size
                    IconButton(onClick = { selection = false }) {
                        Icon(painter = painterResource(R.drawable.close), contentDescription = null)
                    }
                    Text(
                        text = stringResource(R.string.elements_selected, count),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (count == mutableQueueWindows.size) {
                                selectedSongs.clear()
                                selectedItems.clear()
                            } else {
                                queueWindows
                                    .filter { it.mediaItem.metadata!! !in selectedSongs }
                                    .forEach {
                                        selectedSongs.add(it.mediaItem.metadata!!)
                                        selectedItems.add(it)
                                    }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(if (count == mutableQueueWindows.size) R.drawable.deselect else R.drawable.select_all),
                            contentDescription = null,
                        )
                    }

                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = selectedSongs,
                                    onDismiss = menuState::dismiss,
                                    clearAction = {
                                        selectedSongs.clear()
                                        selectedItems.clear()
                                    },
                                    currentItems = selectedItems,
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = LocalContentColor.current,
                        )
                    }
                }
            }
        }
    }

    val footerUI: @Composable RowScope.() -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(
                            if (playerConnection.player.shuffleModeEnabled) playerConnection.player.currentMediaItemIndex else 0,
                        )
                    }.invokeOnCompletion {
                        playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null,
                    modifier = Modifier.alpha(if (shuffleModeEnabled) 1f else 0.5f),
                )
            }

            IconButton(
                onClick = { state.collapseSoft() },
            ) {
                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = null,
                )
            }

            IconButton(
                onClick = playerConnection.player::toggleRepeatMode,
            ) {
                Icon(
                    painter = painterResource(
                        when (repeatMode) {
                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> throw IllegalStateException()
                        }
                    ),
                    contentDescription = null,
                    modifier = Modifier.alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                )
            }
        }
    }


    LaunchedEffect(queueWindows) {
        mutableQueueWindows.apply {
            clear()
            addAll(queueWindows)
        }
    }

    LaunchedEffect(mutableQueueWindows) {
        if (currentWindowIndex != -1) {
            lazyListState.scrollToItem(currentWindowIndex)
        }
    }

    val innerContent: @Composable BoxScope.() -> Unit = {
        val listBackdrop = rememberLayerBackdrop()
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                        if (isLiquidGlass) {
                            Modifier
                        } else {
                            Modifier.background(backgroundColor)
                        }
                    ),
        ) {
            val flingBehavior = ScrollableDefaults.flingBehavior()

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (isLiquidGlass) Modifier.layerBackdrop(listBackdrop) else Modifier)
            ) {
                LazyColumn(
                    state = lazyListState,
                    contentPadding =
                        WindowInsets.systemBars
                            .add(
                                WindowInsets(
                                    top = ListItemHeight + 8.dp,
                                    bottom = ListItemHeight + 8.dp + NavigationBarHeight,
                                ),
                            ).asPaddingValues(),
                    flingBehavior = flingBehavior,
                    modifier =
                        Modifier
                             .then(if (isLiquidGlass) Modifier else Modifier.background(backgroundColor))
                            .nestedScroll(state.preUpPostDownNestedScrollConnection),
                ) {
                item {
                    Spacer(
                        modifier =
                            Modifier
                                .animateContentSize()
                                .height(if (selection) 48.dp else 0.dp),
                    )
                }

                itemsIndexed(
                    items = mutableQueueWindows,
                    key = { _, item -> item.uid.hashCode() },
                ) { index, window ->
                    ReorderableItem(
                        state = reorderableState, 
                        key = window.uid.hashCode(),
                    ) {
                        val currentItem by rememberUpdatedState(window)
                        val currentContext by rememberUpdatedState(context)
                        val dismissBoxState =
                            rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance ->
                                    totalDistance
                                },
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.StartToEnd ||
                                        dismissValue == SwipeToDismissBoxValue.EndToStart
                                    ) {
                                        playerConnection.player.removeMediaItem(currentItem.firstPeriodIndex)
                                        dismissJob?.cancel()
                                        val removedMessage = currentContext.getString(
                                            R.string.removed_song_from_playlist,
                                            currentItem.mediaItem.metadata?.title ?: "Unknown",
                                        )
                                        val undoLabel = currentContext.getString(R.string.undo)
                                        dismissJob =
                                            coroutineScope.launch {
                                                val snackbarResult =
                                                    snackbarHostState.showSnackbar(
                                                        message = removedMessage,
                                                        actionLabel = undoLabel,
                                                        duration = SnackbarDuration.Short,
                                                    )
                                                if (snackbarResult == SnackbarResult.ActionPerformed) {
                                                    playerConnection.player.addMediaItem(currentItem.mediaItem)
                                                    playerConnection.player.moveMediaItem(
                                                        mutableQueueWindows.size,
                                                        currentItem.firstPeriodIndex,
                                                    )
                                                }
                                            }
                                    }
                                    true
                                },
                            )

                        val content: @Composable () -> Unit = {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                MediaMetadataListItem(
                                    mediaMetadata = window.mediaItem.metadata!!,
                                    isSelected = selection && window.mediaItem.metadata!! in selectedSongs,
                                    isActive = index == currentWindowIndex,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        if (!locked) {
                                            IconButton(
                                                onClick = { },
                                                modifier =
                                                    Modifier.draggableHandle(),
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.drag_handle),
                                                    contentDescription = null,
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    SelectionMediaMetadataMenu(
                                                        songSelection = selectedSongs,
                                                        onDismiss = menuState::dismiss,
                                                        clearAction = {
                                                            selectedSongs.clear()
                                                            selectedItems.clear()
                                                        },
                                                        currentItems = selectedItems,
                                                    )
                                                }
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                                tint = LocalContentColor.current,
                                            )
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (selection) {
                                                        if (window.mediaItem.metadata!! in selectedSongs) {
                                                            selectedSongs.remove(window.mediaItem.metadata!!)
                                                            selectedItems.remove(currentItem)
                                                        } else {
                                                            selectedSongs.add(window.mediaItem.metadata!!)
                                                            selectedItems.add(currentItem)
                                                        }
                                                    } else {
                                                        if (index == currentWindowIndex) {
                                                            playerConnection.player.togglePlayPause()
                                                        } else {
                                                            playerConnection.player.seekToDefaultPosition(
                                                                window.firstPeriodIndex,
                                                            )
                                                            playerConnection.player.playWhenReady =
                                                                true
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    if (!selection) {
                                                        selection = true
                                                    }
                                                    selectedSongs.clear()
                                                    selectedSongs.add(window.mediaItem.metadata!!)
                                                },
                                            ),
                                )
                            }
                        }

                        if (locked) {
                            content()
                        } else {
                            SwipeToDismissBox(
                                state = dismissBoxState,
                                backgroundContent = {},
                            ) {
                                content()
                            }
                        }
                    }
                }
                
                if (automix.isNotEmpty()) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        )

                        ItemWithGlowingIcon()
                    }

                    itemsIndexed(
                        items = automix,
                        key = { _, it -> it.mediaId },
                    ) { index, item ->
                        Row(
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            MediaMetadataListItem(
                                mediaMetadata = item.metadata!!,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            playerConnection.service.playNextAutomix(
                                                item,
                                                index,
                                            )
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.playlist_play),
                                            contentDescription = null,
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            playerConnection.service.addToQueueAutomix(
                                                item,
                                                index,
                                            )
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.queue_music),
                                            contentDescription = null,
                                        )
                                    }
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                menuState.show {
                                                    PlayerMenu(
                                                        mediaMetadata = item.metadata,
                                                        navController = navController,
                                                        playerBottomSheetState = playerBottomSheetState,
                                                        isQueueTrigger = true,
                                                        onShowDetailsDialog = {
                                                            showDetailsDialog = true
                                                        },
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
        }
        }

        if (!isLiquidGlass) {
            Column(
                modifier =
                    Modifier
                        .windowInsetsPadding(
                            WindowInsets.systemBars
                                .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        )
                        .height(64.dp)
                        .fillMaxWidth()
                        .then(
                            if (isLiquidGlass && backdrop != null) {
                                val isLightTheme = !isSystemInDarkTheme()
                                val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f) else Color(0xFF121212).copy(0.4f)
                                Modifier.drawBackdrop(
                                    backdrop = rememberCombinedBackdrop(backdrop, listBackdrop),
                                    shape = { androidx.compose.foundation.shape.CircleShape },
                                    effects = {
                                        colorControls(
                                            brightness = if (isLightTheme) 0.2f else 0f,
                                            saturation = 1.5f
                                        )
                                        blur(if (isLightTheme) 16.dp.toPx() else 8.dp.toPx())
                                        lens(
                                            refractionHeight = 24.dp.toPx(),
                                            refractionAmount = 48.dp.toPx(),
                                            depthEffect = true
                                        )
                                    },
                                    highlight = { Highlight.Plain },
                                    onDrawSurface = { drawRect(containerColor) }
                                )
                            } else {
                                Modifier.background(
                                    MaterialTheme.colorScheme
                                        .secondaryContainer
                                        .copy(alpha = 0.90f),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                            }
                        ),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = queueTitle.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    AnimatedVisibility(
                        visible = !selection,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                    ) {
                        Row {
                            IconButton(
                                onClick = { locked = !locked },
                                modifier = Modifier.padding(horizontal = 6.dp),
                            ) {
                                Icon(
                                    painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                                    contentDescription = null,
                                )
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.n_song,
                                queueWindows.size,
                                queueWindows.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        Text(
                            text = makeTimeString(queueLength * 1000L),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = selection,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .height(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val count = selectedSongs.size
                        IconButton(
                            onClick = {
                                selection = false
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                            )
                        }
                        Text(
                            text = stringResource(R.string.elements_selected, count),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (count == mutableQueueWindows.size) {
                                    selectedSongs.clear()
                                    selectedItems.clear()
                                } else {
                                    queueWindows
                                        .filter { it.mediaItem.metadata!! !in selectedSongs }
                                        .forEach {
                                            selectedSongs.add(it.mediaItem.metadata!!)
                                            selectedItems.add(it)
                                        }
                                }
                            },
                        ) {
                            Icon(
                                painter =
                                    painterResource(
                                        if (count == mutableQueueWindows.size) {
                                            R.drawable.deselect
                                        } else {
                                            R.drawable.select_all
                                        },
                                    ),
                                contentDescription = null,
                            )
                        }

                        IconButton(
                            onClick = {
                                menuState.show {
                                    SelectionMediaMetadataMenu(
                                        songSelection = selectedSongs,
                                        onDismiss = menuState::dismiss,
                                        clearAction = {
                                            selectedSongs.clear()
                                            selectedItems.clear()
                                        },
                                        currentItems = selectedItems,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                                tint = LocalContentColor.current,
                            )
                        }
                    }
                }
            }
        }

        if (!isLiquidGlass) {
            androidx.compose.foundation.layout.Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(
                            WindowInsets.systemBars
                                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                        )
                        .height(64.dp)
                        .fillMaxWidth()
                        .then(
                            if (isLiquidGlass && backdrop != null) {
                                val isLightTheme = !isSystemInDarkTheme()
                                val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f) else Color(0xFF121212).copy(0.4f)
                                Modifier.drawBackdrop(
                                    backdrop = rememberCombinedBackdrop(backdrop, listBackdrop),
                                    shape = { androidx.compose.foundation.shape.CircleShape },
                                    effects = {
                                        colorControls(
                                            brightness = if (isLightTheme) 0.2f else 0f,
                                            saturation = 1.5f
                                        )
                                        blur(if (isLightTheme) 16.dp.toPx() else 8.dp.toPx())
                                        lens(
                                            refractionHeight = 24.dp.toPx(),
                                            refractionAmount = 48.dp.toPx(),
                                            depthEffect = true
                                        )
                                    },
                                    highlight = { Highlight.Plain },
                                    onDrawSurface = { drawRect(containerColor) }
                                )
                            } else {
                                Modifier.background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(
                                        alpha = 0.8f
                                    ),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                            }
                        )
                        .clickable {
                            state.collapseSoft()
                        }
                        .padding(horizontal = 12.dp),
            ) {
                IconButton(
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = {
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(
                                if (playerConnection.player.shuffleModeEnabled) playerConnection.player.currentMediaItemIndex else 0,
                            )
                        }.invokeOnCompletion {
                                playerConnection.player.shuffleModeEnabled =
                                    !playerConnection.player.shuffleModeEnabled
                            }
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = null,
                        modifier = Modifier.alpha(if (shuffleModeEnabled) 1f else 0.5f),
                    )
                }

                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                )

                IconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = playerConnection.player::toggleRepeatMode,
                ) {
                    Icon(
                        painter =
                            painterResource(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    else -> throw IllegalStateException()
                                },
                            ),
                        contentDescription = null,
                        modifier = Modifier.alpha(if (repeatMode == Player.REPEAT_MODE_OFF) 0.5f else 1f),
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .padding(
                        bottom =
                            ListItemHeight +
                                    WindowInsets.systemBars
                                        .asPaddingValues()
                                        .calculateBottomPadding(),
                    )
                    .align(Alignment.BottomCenter),
        )
    }

    if (isLiquidGlass) {
        LiquidGlassDialog(
            onDismiss = { state.collapseSoft() },
            title = headerUI,
            bottomBar = footerUI,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                ) {
                    innerContent()
                    BottomSheetMenu(
                        state = LocalMenuState.current,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        )
    } else {
        BottomSheet(
            state = state,
            background = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                )
            },
            modifier = modifier,
            collapsedContent = {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(
                                WindowInsets.systemBars
                                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                            ),
                ) {
                    IconButton(onClick = { state.expandSoft() }) {
                        Icon(
                            painter = painterResource(R.drawable.expand_less),
                            tint = onBackgroundColor,
                            contentDescription = null,
                        )
                    }
                }
            },
        ) {
            innerContent()
        }
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun ItemWithGlowingIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Box {
                Icon(
                    painter = painterResource(R.drawable.ia_icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(30.dp)
                        .scale(scale)
                        .alpha(alpha)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = stringResource(R.string.similar_content),
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}