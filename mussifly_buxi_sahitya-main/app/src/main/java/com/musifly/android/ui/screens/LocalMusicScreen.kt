package com.musifly.android.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.models.LocalSong
import com.musifly.android.playback.queues.ListQueue
import com.musifly.android.ui.component.shimmer.ShimmerHost
import com.musifly.android.ui.component.shimmer.TextPlaceholder
import com.musifly.android.utils.PermissionManager
import com.musifly.android.viewmodels.LocalMusicViewModel
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey

/**
 * Local Music Screen - Display all local audio files from device storage
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LocalMusicScreen(
    navController: NavController,
    onDeselect: () -> Unit = {},
    viewModel: LocalMusicViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    
    val allSongs by viewModel.allSongs.collectAsState()
    val displayedSongs by viewModel.displayedSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val lazyListState = rememberLazyListState()
    
    // Permission handling
    val hasPermission = PermissionManager.hasStoragePermission(context)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.loadLocalMusic()
        }
    }
    
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // Handle system back button press - intercept before NavController handles it
    // This prevents NavController from trying to pop a non-existent destination
    // LocalMusicScreen is state-based, not navigation-based, so we handle back manually
    BackHandler(enabled = true) {
        // Immediately call onDeselect to change state and hide this screen
        // This prevents NavController from trying to handle the back press
        onDeselect()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Music") },
                navigationIcon = {
                    IconButton(onClick = onDeselect) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back to Library"
                        )
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = {
                            if (hasPermission) {
                                viewModel.refresh()
                            } else {
                                showPermissionDialog = true
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.replay),
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (displayedSongs.isNotEmpty() && !isLoading) {
                FloatingActionButton(
                    onClick = {
                        playerConnection.playQueue(
                            ListQueue(
                                title = "Local Music",
                                items = displayedSongs.map { it.toMediaItem(context) }.shuffled()
                            )
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = "Shuffle all"
                    )
                }
            }
        }
    ) { paddingValues ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchMusic(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search local music...") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchMusic("") }) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = "Clear"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
            
            // Content
            when {
                // Permission not granted
                !hasPermission -> {
                    PermissionRequiredView(
                        onRequestPermission = {
                            permissionLauncher.launch(PermissionManager.getStoragePermission())
                        },
                        onOpenSettings = {
                            PermissionManager.openAppSettings(context)
                        }
                    )
                }
                
                // Loading
                isLoading -> {
                    LoadingView()
                }
                
                // No songs found
                displayedSongs.isEmpty() && !isLoading -> {
                    EmptyStateView(
                        searchQuery = searchQuery,
                        onScanAgain = { viewModel.refresh() }
                    )
                }
                
                // Songs list
                else -> {
                    LocalMusicList(
                        songs = displayedSongs,
                        lazyListState = lazyListState,
                        onSongClick = { song, index ->
                            playerConnection.playQueue(
                                ListQueue(
                                    title = "Local Music",
                                    items = displayedSongs.map { it.toMediaItem(context) },
                                    startIndex = index
                                )
                            )
                        }
                    )
                }
            }
        }
    }
    
    // Permission Dialog
    if (showPermissionDialog) {
        PermissionRationaleDialog(
            onDismiss = { showPermissionDialog = false },
            onConfirm = {
                showPermissionDialog = false
                permissionLauncher.launch(PermissionManager.getStoragePermission())
            }
        )
    }
}

/**
 * List of local songs
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalMusicList(
    songs: List<LocalSong>,
    lazyListState: LazyListState,
    onSongClick: (LocalSong, Int) -> Unit
) {
    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) {
        items(
            items = songs,
            key = { it.id }
        ) { song ->
            val index = songs.indexOf(song)
            LocalSongListItem(
                song = song,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onSongClick(song, index) },
                        onLongClick = {
                            // TODO: Show menu
                        }
                    )
                    .animateItemPlacement()
            )
        }
    }
}

private fun Modifier.animateItemPlacement(): Modifier = this

/**
 * Individual song list item
 */
@Composable
private fun LocalSongListItem(
    song: LocalSong,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Art
        AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(song.albumArtUri)
                .allowHardware(false)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            placeholder = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher),
            error = rememberAsyncImagePainter(com.musifly.android.R.mipmap.ic_musifly_launcher)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Song Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "${song.getFormattedDuration()} • ${song.getFormattedSize()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        
        // More button
        IconButton(onClick = { /* TODO: Show menu */ }) {
            Icon(
                painter = painterResource(R.drawable.more_vert),
                contentDescription = "More options"
            )
        }
    }
}

/**
 * Loading state with shimmer effect
 */
@Composable
private fun LoadingView() {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(10) {
            ShimmerHost {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        TextPlaceholder()
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        TextPlaceholder()
                        Spacer(modifier = Modifier.height(4.dp))
                        TextPlaceholder()
                        Spacer(modifier = Modifier.height(4.dp))
                        TextPlaceholder()
                    }
                }
            }
        }
    }
}

/**
 * Empty state view
 */
@Composable
private fun EmptyStateView(
    searchQuery: String,
    onScanAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.music_note),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (searchQuery.isEmpty()) "No local music found" else "No results",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (searchQuery.isEmpty()) 
                "Add music files to your device or check storage permission" 
            else 
                "Try a different search query",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (searchQuery.isEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            
            FilledTonalButton(onClick = onScanAgain) {
                Icon(
                    painter = painterResource(R.drawable.replay),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Again")
            }
        }
    }
}

/**
 * Permission required view
 */
@Composable
private fun PermissionRequiredView(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.security),
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Storage Permission Required",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = PermissionManager.getPermissionRationale(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        FilledTonalButton(onClick = onRequestPermission) {
            Icon(
                painter = painterResource(R.drawable.security),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Grant Permission")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextButton(onClick = onOpenSettings) {
            Text("Open Settings")
        }
    }
}

/**
 * Permission rationale dialog
 */
@Composable
private fun PermissionRationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    if (isLiquidGlass) {
        LiquidGlassDialog(
            onDismiss = onDismiss,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.security),
                    contentDescription = null
                )
            },
            title = {
                Text(PermissionManager.getPermissionName())
            },
            content = {
                Text(PermissionManager.getPermissionRationale())
            },
            buttons = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                TextButton(onClick = onConfirm) {
                    Text("Grant")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.security),
                    contentDescription = null
                )
            },
            title = {
                Text(PermissionManager.getPermissionName())
            },
            text = {
                Text(PermissionManager.getPermissionRationale())
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Grant")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
