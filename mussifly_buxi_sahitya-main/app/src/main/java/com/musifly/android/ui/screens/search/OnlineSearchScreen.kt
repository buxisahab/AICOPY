package com.musifly.android.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.musifly.innertube.models.AlbumItem
import com.musifly.innertube.models.ArtistItem
import com.musifly.innertube.models.PlaylistItem
import com.musifly.innertube.models.SongItem
import com.musifly.innertube.models.WatchEndpoint
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.SuggestionItemHeight
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.playback.queues.YouTubeQueue
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.component.YouTubeListItem
import com.musifly.android.ui.menu.YouTubeAlbumMenu
import com.musifly.android.ui.menu.YouTubeArtistMenu
import com.musifly.android.ui.menu.YouTubePlaylistMenu
import com.musifly.android.ui.menu.YouTubeSongMenu
import com.musifly.android.viewmodels.OnlineSearchSuggestionViewModel
import com.musifly.android.constants.*
import com.musifly.android.ui.component.LocalSearchBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.blur
import com.musifly.android.ui.component.liquidglass.backdrop.effects.colorControls
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.shadow.Shadow
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference
import com.musifly.android.constants.LiquidGlassBlurRadiusKey
import com.musifly.android.constants.LiquidGlassLensHeightKey
import com.musifly.android.constants.LiquidGlassLensAmountKey
import com.musifly.android.constants.LiquidGlassVibrancyKey
import com.musifly.android.constants.LiquidGlassChromaticAberrationKey
import com.musifly.android.constants.LiquidGlassChromaticAberrationAmountKey
import com.musifly.android.constants.LiquidGlassEnableDispersionKey
import com.musifly.android.constants.LiquidGlassCornerRadiusKey
import kotlinx.coroutines.flow.drop

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    navController: NavController,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val viewState by viewModel.viewState.collectAsState()

    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS
    val backdrop = LocalSearchBackdrop.current
    
    val lazyListState = rememberLazyListState()

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Songs", "Albums", "Artists")

    val filteredItems = remember(viewState.items, selectedCategory) {
        if (selectedCategory == "All") {
            viewState.items
        } else {
            viewState.items.filter { item ->
                when (selectedCategory) {
                    "Songs" -> item is SongItem
                    "Albums" -> item is AlbumItem
                    "Artists" -> item is ArtistItem
                    else -> true
                }
            }
        }
    }

    val categorizedResults = remember(filteredItems, selectedCategory) {
        if (selectedCategory != "All") {
            if (filteredItems.isEmpty()) emptyMap() else mapOf(selectedCategory to filteredItems)
        } else {
            if (filteredItems.isEmpty()) {
                emptyMap()
            } else {
                val topCount = minOf(4, filteredItems.size)
                mapOf(
                    "Top result" to filteredItems.take(topCount),
                    "Other" to filteredItems.drop(topCount)
                ).filterValues { it.isNotEmpty() }
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
        ),
    ) {
        if (query.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = null
                        )
                    }
                }
            }
        }

        if (viewState.history.isNotEmpty() && query.isEmpty()) {
            item {
                Text(
                    text = (stringResource(R.string.SearchHistory)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(
                items = viewState.history,
                key = { it.query },
            ) { history ->
                SuggestionItem(
                    query = history.query,
                    online = false,
                    onClick = {
                        onSearch(history.query)
                        onDismiss()
                    },
                    onDelete = {
                        database.query {
                            delete(history)
                        }
                    },
                    onFillTextField = {
                        onQueryChange(
                            TextFieldValue(
                                text = history.query,
                                selection = TextRange(history.query.length),
                            ),
                        )
                    },
                    modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                )
            }
        }

        if (viewState.suggestions.isNotEmpty() && query.isNotEmpty()) {
            item {
                Text(
                    text = (stringResource(R.string.Sujestions)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = viewState.suggestions) { query ->
                        HorizontalSuggestionChip(
                            query = query,
                            onClick = {
                                onSearch(query)
                                onDismiss()
                            },
                            onFillTextField = {
                                onQueryChange(
                                    TextFieldValue(
                                        text = query,
                                        selection = TextRange(query.length),
                                    ),
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (filteredItems.isNotEmpty() && query.isNotEmpty()) {
            categorizedResults.forEach { (category, items) ->
                item {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(
                    items = items,
                    key = { it.id },
                ) { item ->
                    val isSaavn = item.id.startsWith("saavn:")
                    YouTubeListItem(
                        item = item,
                        isActive =
                            when (item) {
                                is SongItem -> mediaMetadata?.id == item.id
                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                else -> false
                            },
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .then(
                                run {
                                    val shape = RoundedCornerShape(16.dp)
                                    if (isLiquidGlass && backdrop != null) {
                                        Modifier
                                            .drawBackdrop(
                                                backdrop = backdrop,
                                                shape = { shape },
                                                effects = {
                                                    colorControls(0.1f, 0.1f)
                                                    vibrancy()
                                                }
                                            )
                                            .clip(shape)
                                    } else {
                                        Modifier
                                            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
                                            .clip(shape)
                                    }
                                }
                            )
                            .combinedClickable(
                                onClick = {
                                    when (item) {
                                        is SongItem -> {
                                            if (item.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                if (isSaavn) {
                                                    playerConnection.playQueue(
                                                        com.musifly.android.playback.queues.SaavnQueue.radio(item.toMediaMetadata(), viewModel.saavnAPI)
                                                    )
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = item.id),
                                                            item.toMediaMetadata()
                                                        ),
                                                    )
                                                }
                                                onDismiss()
                                            }
                                        }

                                        is AlbumItem -> {
                                            navController.navigate("album/${item.id}")
                                            onDismiss()
                                        }

                                        is ArtistItem -> {
                                            navController.navigate("artist/${item.id}")
                                            onDismiss()
                                        }

                                        is PlaylistItem -> {
                                            navController.navigate("online_playlist/${item.id}")
                                            onDismiss()
                                        }
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        if (isSaavn) {
                                            when (item) {
                                                is SongItem ->
                                                    com.musifly.android.ui.menu.SaavnSongMenu(
                                                        mediaMetadata = item.toMediaMetadata(),
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                        saavnAPI = viewModel.saavnAPI,
                                                    )
                                                is ArtistItem ->
                                                    com.musifly.android.ui.menu.SaavnArtistMenu(
                                                        artist = com.musifly.android.models.SaavnArtist(
                                                            id = item.id.removePrefix("saavn:"),
                                                            title = item.title,
                                                            image = item.thumbnail ?: "",
                                                            subtitle = null
                                                        ),
                                                        onDismiss = menuState::dismiss,
                                                        saavnAPI = viewModel.saavnAPI
                                                    )
                                                else -> {}
                                            }
                                        } else {
                                            when (item) {
                                                is SongItem ->
                                                    YouTubeSongMenu(
                                                        song = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )

                                                is AlbumItem ->
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )

                                                is ArtistItem ->
                                                    YouTubeArtistMenu(
                                                        artist = item,
                                                        onDismiss = menuState::dismiss,
                                                    )

                                                is PlaylistItem ->
                                                    YouTubePlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                            }
                                        }
                                    }
                                },
                            )
                            .animateItem()
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
) {
    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS
    val backdrop = LocalSearchBackdrop.current
    
    val (lensHeight) = rememberPreference(LiquidGlassLensHeightKey, defaultValue = 0.2f)
    val (lensAmount) = rememberPreference(LiquidGlassLensAmountKey, defaultValue = 0.2f)
    val (chromaticAberration) = rememberPreference(LiquidGlassChromaticAberrationKey, defaultValue = true)
    val (chromaticAberrationAmount) = rememberPreference(LiquidGlassChromaticAberrationAmountKey, defaultValue = 0f)
    val (enableDispersion) = rememberPreference(LiquidGlassEnableDispersionKey, defaultValue = false)
    val (cornerRadius) = rememberPreference(LiquidGlassCornerRadiusKey, defaultValue = 0.5f)
    
    val shape = RoundedCornerShape(18.dp * cornerRadius)
    
    // Control Center style container color (matching AndroidLiquidGlass)
    val containerColor = Color.Black.copy(0.05f)
    
    Surface(
        shape = shape,
        color = Color.Transparent, // Color is handled by backdrop or background modifier
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(
                if (isLiquidGlass && backdrop != null) {
                    Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = {
                                vibrancy()
                                val minDimension = size.minDimension
                                lens(
                                    refractionHeight = lensHeight * minDimension * 0.5f,
                                    refractionAmount = lensAmount * minDimension,
                                    depthEffect = true,
                                    chromaticAberration = chromaticAberration && (chromaticAberrationAmount > 0f || enableDispersion)
                                )
                            },
                            highlight = { Highlight.Plain },
                            shadow = { Shadow.Default },
                            onDrawSurface = {
                                drawRect(containerColor)
                            }
                        )
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant, shape)
                }
            )
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(SuggestionItemHeight)
                .padding(horizontal = 12.dp)
        ) {
                Icon(
                    painterResource(if (online) R.drawable.search else R.drawable.history),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp)
                        .alpha(0.7f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = query,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!online) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.alpha(0.7f),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onFillTextField,
                    modifier = Modifier.alpha(0.7f),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_top_left),
                        contentDescription = "Fill",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
    }
}

@Composable
fun HorizontalSuggestionChip(
    query: String,
    onClick: () -> Unit,
    onFillTextField: () -> Unit,
) {
    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS
    val backdrop = LocalSearchBackdrop.current
    
    val shape = RoundedCornerShape(24.dp)
    val containerColor = if (isLiquidGlass) Color.White.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant
    
    Surface(
        color = Color.Transparent,
        shape = shape,
        modifier = Modifier
            .then(
                if (isLiquidGlass && backdrop != null) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            lens(12.dp.toPx(), 24.dp.toPx(), depthEffect = true)
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                } else {
                    Modifier.background(containerColor, shape)
                }
            )
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Text(
                text = query,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            IconButton(
                onClick = onFillTextField,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_top_left),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
