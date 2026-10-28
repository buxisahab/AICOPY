package com.musifly.android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.musifly.innertube.utils.parseCookieString
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.HistorySource
import com.musifly.android.constants.InnerTubeCookieKey
import com.musifly.android.db.entities.EventWithSong
import com.musifly.android.extensions.metadata
import com.musifly.android.extensions.toMediaItem
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.playback.queues.ListQueue
import com.musifly.android.playback.queues.YouTubeQueue
import com.musifly.android.ui.component.ChipsRow
import com.musifly.android.ui.component.HideOnScrollFAB
import com.musifly.android.ui.component.AppIconButton
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.component.NavigationTitle
import com.musifly.android.ui.component.SongListItem
import com.musifly.android.ui.component.YouTubeListItem
import com.musifly.android.ui.menu.SelectionMediaMetadataMenu
import com.musifly.android.ui.menu.SongMenu
import com.musifly.android.ui.menu.YouTubeSongMenu
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.utils.rememberPreference
import com.musifly.android.viewmodels.DateAgo
import com.musifly.android.viewmodels.HistoryViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var selection by remember {
        mutableStateOf(false)
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
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
    } else if (selection) {
        BackHandler {
            selection = false
        }
    }

    val historySource by viewModel.historySource.collectAsState()
    val events by viewModel.events.collectAsState()
    val historyPage by viewModel.historyPage

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val historyTitle = stringResource(R.string.history)
    val todayString = stringResource(R.string.today)
    val yesterdayString = stringResource(R.string.yesterday)
    val thisWeekString = stringResource(R.string.this_week)
    val lastWeekString = stringResource(R.string.last_week)

    fun getDateAgoString(dateAgo: DateAgo): String {
        return when (dateAgo) {
            DateAgo.Today -> todayString
            DateAgo.Yesterday -> yesterdayString
            DateAgo.ThisWeek -> thisWeekString
            DateAgo.LastWeek -> lastWeekString
            is DateAgo.Other -> dateAgo.date.format(DateTimeFormatter.ofPattern("yyyy/MM"))
        }
    }

    class WrappedHistoryItem(val item: EventWithSong) {
        var isSelected by mutableStateOf(false)
    }

    val filteredEvents = remember(events, query) {
        if (query.text.isEmpty()) {
            events
        } else {
            events.mapValues { (_, songs) ->
                songs.filter { event ->
                    event.song.song.title.contains(query.text, ignoreCase = true) ||
                            event.song.artists.any {
                                it.name.contains(
                                    query.text,
                                    ignoreCase = true
                                )
                            }
                }
            }.filterValues { it.isNotEmpty() }
        }
    }

    val filteredRemoteContent = remember(historyPage, query) {
        if (query.text.isEmpty()) {
            historyPage?.sections
        } else {
            historyPage?.sections?.map { section ->
                section.copy(
                    songs = section.songs.filter { song ->
                        song.title.contains(query.text, ignoreCase = true) ||
                                song.artists.any { it.name.contains(query.text, ignoreCase = true) }
                    }
                )
            }?.filter { it.songs.isNotEmpty() }
        }
    }

    val wrappedItems = remember(filteredEvents) {
        filteredEvents.flatMap { it.value }.map { WrappedHistoryItem(it) }
    }.toMutableStateList()

    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (selection) {
                        val count = wrappedItems.count { it.isSelected }
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
                    } else {
                        Text(historyTitle)
                    }
                },
                navigationIcon = {
                    AppIconButton(
                        onClick = {
                            when {
                                isSearching -> {
                                    isSearching = false
                                    query = TextFieldValue()
                                }

                                selection -> {
                                    selection = false
                                }

                                else -> {
                                    navController.navigateUp()
                                }
                            }
                        },
                        onLongClick = {
                            if (!isSearching && !selection) {
                                navController.backToMain()
                            }
                        },
                        painter = painterResource(
                            if (selection) R.drawable.close else R.drawable.arrow_back
                        ),
                        contentDescription = null
                    )
                },
                actions = {
                    if (selection) {
                        val count = wrappedItems.count { it.isSelected }
                        AppIconButton(
                            onClick = {
                                if (count == wrappedItems.size) {
                                    wrappedItems.forEach { it.isSelected = false }
                                } else {
                                    wrappedItems.forEach { it.isSelected = true }
                                }
                            },
                            painter = painterResource(
                                if (count == wrappedItems.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                        AppIconButton(
                            onClick = {
                                menuState.show {
                                    SelectionMediaMetadataMenu(
                                        songSelection = wrappedItems
                                            .filter { it.isSelected }
                                            .map { it.item.song.toMediaItem().metadata!! },
                                        onDismiss = menuState::dismiss,
                                        clearAction = { selection = false },
                                        currentItems = emptyList()
                                    )
                                }
                            },
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    } else if (!isSearching) {
                        AppIconButton(
                            onClick = { isSearching = true },
                            painter = painterResource(R.drawable.search),
                            contentDescription = null
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            HideOnScrollFAB(
                visible = if (historySource == HistorySource.REMOTE) {
                    filteredRemoteContent?.any { it.songs.isNotEmpty() } == true
                } else {
                    wrappedItems.isNotEmpty()
                },
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                onClick = {
                    if (historySource == HistorySource.REMOTE && historyPage != null) {
                        val songs = filteredRemoteContent?.flatMap { it.songs } ?: emptyList()
                        if (songs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = historyTitle,
                                    items = songs.map { it.toMediaItem() }.shuffled()
                                )
                            )
                        }
                    } else {
                        playerConnection.playQueue(
                            ListQueue(
                                title = historyTitle,
                                items = wrappedItems.map { it.item.song.toMediaItem() }.shuffled()
                            )
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val bottomPadding = LocalPlayerAwareWindowInsets.current
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()

        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            item {
                ChipsRow(
                    chips = if (isLoggedIn) listOf(
                        HistorySource.LOCAL to stringResource(R.string.local_history),
                        HistorySource.REMOTE to stringResource(R.string.remote_history),
                    ) else {
                        listOf(HistorySource.LOCAL to stringResource(R.string.local_history))
                    },
                    currentValue = historySource,
                    onValueUpdate = {
                        viewModel.historySource.value = it
                        if (it == HistorySource.REMOTE) {
                            viewModel.fetchRemoteHistory()
                        }
                    }
                )
            }

            if (historySource == HistorySource.REMOTE && isLoggedIn) {
                filteredRemoteContent?.forEach { section ->
                    stickyHeader {
                        NavigationTitle(
                            title = section.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }

                    items(
                        items = section.songs,
                        key = { "${section.title}_${it.id}" }
                    ) { song ->
                        YouTubeListItem(
                            item = song,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                AppIconButton(
                                    onClick = {
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    },
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(song.toMediaMetadata())
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
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
            } else {
                filteredEvents.forEach { (dateAgo, events) ->
                    stickyHeader {
                        NavigationTitle(
                            title = getDateAgoString(dateAgo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }

                    itemsIndexed(
                        items = wrappedItems,
                    ) { index, wrappedItem ->
                        val event = wrappedItem.item
                        SongListItem(
                            song = event.song,
                            isActive = event.song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            isSelected = wrappedItem.isSelected && selection,

                            trailingContent = {
                                AppIconButton(
                                    onClick = {
                                        if (!selection) {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = event.song,
                                                    event = event.event,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    },
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (!selection) {
                                            if (event.song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = getDateAgoString(dateAgo),
                                                        items = wrappedItems.map { it.item.song.toMediaItem() },
                                                        startIndex = index
                                                    )
                                                )
                                            }
                                        } else {
                                            wrappedItem.isSelected = !wrappedItem.isSelected
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                            wrappedItems.forEach { it.isSelected = false }
                                            wrappedItem.isSelected = true
                                        }
                                    }
                                )
                                .animateItem()
                        )
                    }
                }
            }
        }
    }
}