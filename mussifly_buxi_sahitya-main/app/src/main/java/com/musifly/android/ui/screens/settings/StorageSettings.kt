package com.musifly.android.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.imageLoader
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.constants.CustomDownloadDirectoryKey
import com.musifly.android.constants.MaxImageCacheSizeKey
import com.musifly.android.constants.MaxSongCacheSizeKey
import com.musifly.android.constants.ThumbnailCornerRadius
import com.musifly.android.db.entities.Song
import com.musifly.android.extensions.tryOrNull
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.ListPreference
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.liquidGlassSubtle
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.ui.utils.formatFileSize
import com.musifly.android.utils.dataStore
import com.musifly.android.utils.rememberPreference
import com.musifly.android.viewmodels.HistoryViewModel
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextOverflow
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StorageSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val imageDiskCache = context.imageLoader.diskCache ?: return
    val playerCache = LocalPlayerConnection.current?.service?.playerCache ?: return
    val downloadCache = LocalPlayerConnection.current?.service?.downloadCache ?: return

    val coroutineScope = rememberCoroutineScope()
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = -1
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = -1
    )
    
    val (customDownloadDirectory, onCustomDownloadDirectoryChange) = rememberPreference(
        key = CustomDownloadDirectoryKey,
        defaultValue = ""
    )

    var imageCacheSize by remember { mutableLongStateOf(imageDiskCache.size) }
    var playerCacheSize by remember { mutableLongStateOf(tryOrNull { playerCache.cacheSpace } ?: 0) }
    var downloadCacheSize by remember { mutableLongStateOf(tryOrNull { downloadCache.cacheSpace } ?: 0) }

    val animatedImageCacheSize by animateFloatAsState(
        targetValue = if (maxImageCacheSize == -1) 0f
        else (imageCacheSize.toFloat() / (maxImageCacheSize * 1024 * 1024L)).coerceIn(0f, 1f),
        label = "imageCacheProgress",
    )

    val animatedPlayerCacheSize by animateFloatAsState(
        targetValue = if (maxSongCacheSize == -1) 0f
        else (playerCacheSize.toFloat() / (maxSongCacheSize * 1024 * 1024L)).coerceIn(0f, 1f),
        label = "playerCacheProgress",
    )
    val animatedDownloadCacheSize by animateFloatAsState(
        targetValue = if (downloadCacheSize == 0L) 0f else 1f,
        label = "downloadCacheProgress",
    )

    var showCachedSongsSheet by remember { mutableStateOf(false) }
    
    // Directory picker launcher
    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // Take persistable URI permission
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    
                    // Try to get the path from URI
                    // For file:// URIs, we can extract the path directly
                    // For content:// URIs (SAF), we'll try to extract path from URI
                    val path = if (uri.scheme == "file") {
                        uri.path ?: uri.toString()
                    } else {
                        // For content:// URIs, try to extract path
                        // This works for some cases but not all SAF URIs
                        try {
                            // Try to get path from URI path segment
                            val uriPath = uri.path
                            if (uriPath != null && uriPath.startsWith("/tree/")) {
                                // Extract path from /tree/primary:path format
                                val pathPart = uriPath.substringAfter("/tree/")
                                if (pathPart.contains(":")) {
                                    val (storage, rest) = pathPart.split(":", limit = 2)
                                    if (storage == "primary") {
                                        "/storage/emulated/0/$rest"
                                    } else {
                                        "/storage/$storage/$rest"
                                    }
                                } else {
                                    uriPath
                                }
                            } else {
                                uriPath ?: uri.toString()
                            }
                        } catch (e: Exception) {
                            // Fallback to URI string
                            uri.toString()
                        }
                    }
                    
                    // Save the path to preferences
                    context.dataStore.edit { preferences ->
                        preferences[CustomDownloadDirectoryKey] = path
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(1000) // Throttle to 1 second to prevent UI freeze
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache) {
        while (isActive) {
            delay(1000) // Throttle to 1 second to prevent UI freeze
            playerCacheSize = tryOrNull { playerCache.cacheSpace } ?: 0
        }
    }
    LaunchedEffect(downloadCache) {
        while (isActive) {
            delay(1000) // Throttle to 1 second to prevent UI freeze
            downloadCacheSize = tryOrNull { downloadCache.cacheSpace } ?: 0
        }
    }

    val isLiquidGlass = isLiquidGlassActive()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .then(if (isLiquidGlass) Modifier.liquidGlassSubtle() else Modifier)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Descargas
            StorageCard(
                title = stringResource(R.string.downloaded_songs),
                icon = R.drawable.download,
                usedSize = downloadCacheSize,
                maxSize = null,
                progress = if (downloadCacheSize > 0) animatedDownloadCacheSize else 0f,
                onClearClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            downloadCache.keys.toList().forEach { key ->
                                tryOrNull { downloadCache.removeResource(key) }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                onManageClick = null
            )
            
            // Download Directory Settings
            DownloadDirectoryCard(
                currentDirectory = customDownloadDirectory,
                onSelectDirectory = { directoryPickerLauncher.launch(null) },
                onResetDirectory = {
                    coroutineScope.launch(Dispatchers.IO) {
                        context.dataStore.edit { preferences ->
                            preferences.remove(CustomDownloadDirectoryKey)
                        }
                    }
                }
            )

            // Caché de canciones
            StorageCard(
                title = stringResource(R.string.song_cache),
                icon = R.drawable.music_note,
                usedSize = playerCacheSize,
                maxSize = if (maxSongCacheSize == -1) -1L else maxSongCacheSize * 1024 * 1024L,
                progress = animatedPlayerCacheSize,
                onClearClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            playerCache.keys.toList().forEach { key ->
                                tryOrNull { playerCache.removeResource(key) }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                onManageClick = { showCachedSongsSheet = true },
                extraContent = {
                    ListPreference(
                        title = { Text(stringResource(R.string.max_cache_size)) },
                        icon = { Icon(painterResource(R.drawable.save), null) },
                        iconBackgroundColor = Color(0xFF3F51B5), // Indigo
                        selectedValue = maxSongCacheSize,
                        values = listOf(-1, 128, 256, 512, 1024, 2048, 4096, 8192),
                        valueText = {
                            if (it == -1) stringResource(R.string.unlimited)
                            else formatFileSize(it * 1024 * 1024L)
                        },
                        onValueSelected = onMaxSongCacheSizeChange,
                    )
                }
            )

            // Caché de imágenes
            StorageCard(
                title = stringResource(R.string.image_cache),
                icon = R.drawable.image,
                usedSize = imageCacheSize,
                maxSize = if (maxImageCacheSize == -1) -1L else maxImageCacheSize * 1024 * 1024L,
                progress = animatedImageCacheSize,
                onClearClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            imageDiskCache.clear()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                onManageClick = null,
                extraContent = {
                    ListPreference(
                        title = { Text(stringResource(R.string.max_cache_size)) },
                        icon = { Icon(painterResource(R.drawable.save), null) },
                        iconBackgroundColor = Color(0xFF673AB7), // Deep Purple
                        selectedValue = maxImageCacheSize,
                        values = listOf(-1, 128, 256, 512, 1024, 2048, 4096, 8192),
                        valueText = {
                            if (it == -1) stringResource(R.string.unlimited)
                            else formatFileSize(it * 1024 * 1024L)
                        },
                        onValueSelected = onMaxImageCacheSizeChange,
                    )
                }
            )
        }

        TopAppBar(
            title = { Text(stringResource(R.string.storage)) },
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
    }

    // Bottom Sheet para gestionar canciones en caché
    if (showCachedSongsSheet) {
        CachedSongsBottomSheet(
            playerCache = playerCache,
            viewModel = viewModel,
            onDismiss = { showCachedSongsSheet = false }
        )
    }
}

@Composable
private fun StorageCard(
    title: String,
    icon: Int,
    usedSize: Long,
    maxSize: Long?,
    progress: Float,
    onClearClick: () -> Unit,
    onManageClick: (() -> Unit)?,
    extraContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con icono y título
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when(icon) {
                                R.drawable.download -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                R.drawable.music_note -> Color(0xFF2196F3).copy(alpha = 0.1f)
                                R.drawable.image -> Color(0xFFE91E63).copy(alpha = 0.1f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = when(icon) {
                            R.drawable.download -> Color(0xFF4CAF50)
                            R.drawable.music_note -> Color(0xFF2196F3)
                            R.drawable.image -> Color(0xFFE91E63)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Barra de progreso
            if (usedSize > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatFileSize(usedSize),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (maxSize != null) {
                            Text(
                                text = if (maxSize == -1L) stringResource(R.string.unlimited)
                                else formatFileSize(maxSize),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.size_used, formatFileSize(0)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Contenido extra (ListPreference)
            extraContent?.invoke()

            // Botones de acción
            if (usedSize > 0) {


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onManageClick != null) {
                        ActionButton(
                            text = stringResource(R.string.manage),
                            icon = R.drawable.settings,
                            onClick = onManageClick,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ActionButton(
                        text = stringResource(R.string.clear),
                        icon = R.drawable.delete,
                        onClick = onClearClick,
                        modifier = Modifier.weight(1f),
                        isDestructive = true
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isDestructive)
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (isDestructive)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (isDestructive)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoilApi::class)
@Composable
private fun CachedSongsBottomSheet(
    playerCache: androidx.media3.datasource.cache.Cache,
    viewModel: HistoryViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val events by viewModel.events.collectAsState()

    // Obtener IDs de canciones en caché
    val cachedSongIds = remember(playerCache) {
        playerCache.keys.map { it.toString() }.toSet()
    }

    // Obtener canciones completas desde el historial (similar a CachePlaylistScreen)
    val cachedSongs = remember(events, cachedSongIds) {
        events.values.flatten()
            .map { it.song }
            .distinctBy { it.id }
            .filter { it.id in cachedSongIds }
    }

    // Obtener tamaños de caché - MEJORA: Filtrar canciones con tamaño 0
    val cachedSongsWithSize = remember(cachedSongs, playerCache) {
        cachedSongs.mapNotNull { song ->
            val size = tryOrNull {
                playerCache.getCachedBytes(song.id, 0, Long.MAX_VALUE)
            } ?: 0L

            if (size > 0) {
                CachedSongInfo(song = song, size = size)
            } else null
        }.sortedByDescending { it.size }
    }

    var displayedSongs by remember { mutableStateOf(cachedSongsWithSize) }

    LaunchedEffect(cachedSongsWithSize) {
        displayedSongs = cachedSongsWithSize
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.cached_playlist),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.n_song,
                            displayedSongs.size,
                            displayedSongs.size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (displayedSongs.isNotEmpty()) {
                    com.musifly.android.ui.component.AppIconButton(
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    // MEJORA: Conversión a lista antes de iterar
                                    playerCache.keys.toList().forEach { key ->
                                        tryOrNull { playerCache.removeResource(key) }
                                    }
                                    // MEJORA: Actualización del UI con withContext
                                    withContext(Dispatchers.Main) {
                                        displayedSongs = emptyList()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        painter = painterResource(R.drawable.delete),
                        contentDescription = stringResource(R.string.clear_all_downloads),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Lista de canciones
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedSongs, key = { it.song.id }) { songInfo ->
                    CachedSongItem(
                        songInfo = songInfo,
                        onDeleteClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    // MEJORA: Búsqueda correcta de keys por canción
                                    val keysToRemove = playerCache.keys.filter { key ->
                                        key.contains(songInfo.song.id)
                                    }

                                    keysToRemove.forEach { key ->
                                        tryOrNull { playerCache.removeResource(key) }
                                    }

                                    // MEJORA: Actualización del UI con withContext
                                    withContext(Dispatchers.Main) {
                                        displayedSongs = displayedSongs.filter {
                                            it.song.id != songInfo.song.id
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DownloadDirectoryCard(
    currentDirectory: String,
    onSelectDirectory: () -> Unit,
    onResetDirectory: () -> Unit
) {
    val defaultPath = "/storage/emulated/0/Music/MusiFly/videos"
    val displayPath = if (currentDirectory.isEmpty()) {
        defaultPath
    } else {
        currentDirectory
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFF9800).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.storage),
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.download_directory),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Current directory display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.current_directory),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = displayPath,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Buttons

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (currentDirectory.isNotEmpty()) {
                    ActionButton(
                        text = stringResource(R.string.reset_to_default),
                        icon = R.drawable.restore,
                        onClick = onResetDirectory,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                ActionButton(
                    text = stringResource(R.string.choose_directory),
                    icon = R.drawable.storage,
                    onClick = onSelectDirectory,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CachedSongItem(
    songInfo: CachedSongInfo,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(songInfo.song.thumbnailUrl)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
            )

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = songInfo.song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = songInfo.song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = formatFileSize(songInfo.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Delete button
            IconButton(
                onClick = onDeleteClick,
                onLongClick = { /* No action on long click */ }
            ) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = stringResource(R.string.clear_song_cache),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private data class CachedSongInfo(
    val song: Song,
    val size: Long
)