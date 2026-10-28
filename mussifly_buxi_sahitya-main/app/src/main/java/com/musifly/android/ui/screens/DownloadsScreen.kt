package com.musifly.android.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.content.Context
import android.content.Intent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalDownloadUtil
import com.musifly.android.LocalPlayerConnection
import dagger.hilt.android.EntryPointAccessors
import com.musifly.android.di.CachedDownloadsRepositoryEntryPoint
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import com.musifly.android.db.entities.Song

import com.musifly.android.extensions.toMediaItem
import com.musifly.android.extensions.togglePlayPause
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.playback.ExoDownloadService
import com.musifly.android.playback.queues.ListQueue
import com.musifly.android.ui.component.DownloadItem
import com.musifly.android.ui.component.AppIconButton
import com.musifly.android.ui.component.LocalMenuState
import com.musifly.android.ui.component.SongListItem

import com.musifly.android.ui.menu.SongMenu
import com.musifly.android.viewmodels.DownloadViewModel
import com.musifly.android.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Advanced unified downloads screen showing:
 * - Active downloads (custom download manager)
 * - Finished downloads (custom download manager)
 * - Cached/Offline songs (ExoPlayer downloads)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DownloadsScreen(
    navController: NavController,
    viewModel: DownloadViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    
    val pendingDownloads by viewModel.pendingDownloads.observeAsState(emptyList())
    val finishedDownloads by viewModel.finishedDownloads.observeAsState(emptyList())
    val isServiceBound by viewModel.isServiceBound.observeAsState(false)
    
    // ExoPlayer downloads - get all songs and downloads
    val allSongs = database.allSongs().collectAsState(initial = emptyList()).value
    val downloads = downloadUtil.downloads.collectAsState(initial = emptyMap()).value
    
    // Get active ExoPlayer downloads (downloading or queued)
    val activeExoDownloads = remember(downloads, allSongs) {
        downloads.values
            .filter { it.state == Download.STATE_DOWNLOADING || it.state == Download.STATE_QUEUED }
            .mapNotNull { download ->
                allSongs.find { it.id == download.request.id }?.let { song ->
                    Pair(song, download)
                }
            }
    }
    
    // Get cached download IDs from repository (synced from Firestore)
    val cachedDownloadIds = remember {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context,
                CachedDownloadsRepositoryEntryPoint::class.java
            )
            entryPoint.cachedDownloadsRepository().getAllCachedDownloadIds()
        } catch (e: Exception) {
            Timber.tag("DownloadsScreen").w(e, "Failed to get cached downloads repository")
            kotlinx.coroutines.flow.flowOf(emptyList<String>())
        }
    }.collectAsState(initial = emptyList<String>()).value.toSet()
    
    // Filter songs that are:
    // 1. Actually cached in ExoPlayer (STATE_COMPLETED), OR
    // 2. Synced from Supabase (in cached downloads repository)
    val cachedSongs = allSongs.filter { song ->
        val hasExoPlayerCache = downloads[song.id]?.state == Download.STATE_COMPLETED
        val isInRepository = song.id in cachedDownloadIds
        hasExoPlayerCache || isInRepository
    }
    
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    
    // HorizontalPager state with 3 pages (Active, Finished, Cached)

    val coroutineScope = rememberCoroutineScope()
    
    var showDeleteDialog by remember { mutableStateOf<Any?>(null) }
    
    // Multi-select state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf<Set<Long>>(emptySet()) } // Using timestamp as key
    
    // Syncing state for manual refresh
    var isSyncingFromSupabase by remember { mutableStateOf(false) }
    
    // Manual refresh function for cached songs
    val refreshCachedSongs: () -> Unit = {
        coroutineScope.launch(Dispatchers.IO) {
            isSyncingFromSupabase = true
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context,
                    CachedDownloadsRepositoryEntryPoint::class.java
                )
                val cachedDownloadsRepository = entryPoint.cachedDownloadsRepository()
                
                // Sync from Supabase
                cachedDownloadsRepository.syncFromSupabase().fold(
                    onSuccess = { count ->
                        Timber.tag("DownloadsScreen").d("✅ Manual refresh: Synced $count cached downloads from Supabase")
                        android.widget.Toast.makeText(
                            context,
                            "Synced $count songs from cloud",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { e ->
                        Timber.tag("DownloadsScreen").e(e, "❌ Manual refresh failed")
                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                context,
                                "Sync failed: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.tag("DownloadsScreen").e(e, "Error in manual refresh")
            } finally {
                isSyncingFromSupabase = false
            }
        }
    }
    
    // Bind to service on first composition
    LaunchedEffect(Unit) {
        viewModel.bindToService(context)
    }
    
    // Sync cached downloads from Firestore on screen open (for logged-in users)
    // Also sync existing local ExoPlayer cached downloads to Firestore
    LaunchedEffect(Unit) {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                context,
                CachedDownloadsRepositoryEntryPoint::class.java
            )
            val cachedDownloadsRepository = entryPoint.cachedDownloadsRepository()
            
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                // Step 1: Sync from Supabase (download to local)
                val syncedCount = cachedDownloadsRepository.syncFromSupabase().fold(
                    onSuccess = { count ->
                        Timber.tag("DownloadsScreen").d("✅ Synced $count cached downloads from Supabase")
                        count
                    },
                    onFailure = { e ->
                        Timber.tag("DownloadsScreen").e(e, "❌ Failed to sync cached downloads from Supabase")
                        0
                    }
                )
                
                // Step 1.5: Auto-download songs synced from Firestore that aren't locally cached
                // This ensures users can play offline after syncing
                if (syncedCount > 0) {
                    kotlinx.coroutines.delay(500) // Wait for downloads to load
                    val currentDownloads = downloadUtil.downloads.value
                    val cachedDownloadIdsFromRepo = cachedDownloadsRepository.getAllCachedDownloadIds().first().toSet()
                    
                    // Find songs that are in repository but not locally cached
                    val songsToDownload = cachedDownloadIdsFromRepo.filter { songId ->
                        val downloadState = currentDownloads[songId]?.state
                        downloadState != Download.STATE_COMPLETED && 
                        downloadState != Download.STATE_DOWNLOADING &&
                        downloadState != Download.STATE_QUEUED
                    }
                    
                    if (songsToDownload.isNotEmpty()) {
                        // Auto-download ALL songs for offline playback
                        Timber.tag("DownloadsScreen").d("📥 Auto-downloading ${songsToDownload.size} songs from Supabase sync...")
                        var downloadStarted = 0
                        
                        songsToDownload.forEach { songId ->
                            try {
                                // Get song from database
                                val song = database.getSongById(songId)
                                if (song != null) {
                                    // Convert to MediaMetadata for download
                                    val mediaMetadata = song.toMediaMetadata()
                                    
                                    // Ensure song is in database (upsert)
                                    database.transaction {
                                        upsert(song.song)
                                    }
                                    
                                    // CRITICAL: Pre-cache thumbnail for offline playback
                                    // This ensures thumbnail shows in mini player and Instagram layout when offline
                                    val thumbnailUrl = mediaMetadata.thumbnailUrl
                                    if (!thumbnailUrl.isNullOrEmpty()) {
                                        try {
                                            val imageLoader = coil.ImageLoader(context)
                                            val request = coil.request.ImageRequest.Builder(context)
                                                .data(thumbnailUrl)
                                                .memoryCacheKey(thumbnailUrl)
                                                .diskCacheKey(thumbnailUrl)
                                                .build()
                                            // Execute request to cache the image
                                            imageLoader.execute(request)
                                            Timber.tag("DownloadsScreen").d("✅ Pre-cached thumbnail for: ${mediaMetadata.title}")
                                        } catch (e: Exception) {
                                            Timber.tag("DownloadsScreen").w(e, "⚠️ Failed to pre-cache thumbnail for: ${mediaMetadata.title}")
                                        }
                                    }
                                    
                                    // Create download request
                                    val downloadRequest = DownloadRequest
                                        .Builder(songId, songId.toUri())
                                        .setCustomCacheKey(songId)
                                        .setData(mediaMetadata.title.toByteArray())
                                        .build()
                                    
                                    // Start download
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        DownloadService.sendAddDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            downloadRequest,
                                            false
                                        )
                                    }
                                    
                                    downloadStarted++
                                    Timber.tag("DownloadsScreen").d("✅ Started auto-download: ${mediaMetadata.title} ($songId)")
                                    
                                    // Small delay between downloads to avoid overwhelming the system
                                    kotlinx.coroutines.delay(200)
                                } else {
                                    Timber.tag("DownloadsScreen").w("⚠️ Song not found in database: $songId - skipping auto-download")
                                }
                            } catch (e: Exception) {
                                Timber.tag("DownloadsScreen").w(e, "⚠️ Failed to start auto-download for: $songId")
                            }
                        }
                        
                        if (downloadStarted > 0) {
                            Timber.tag("DownloadsScreen").d("✅ Started auto-downloading $downloadStarted songs")
                        }
                    }
                }
                
                // Step 2: Sync existing local ExoPlayer cached downloads to Supabase
                // This handles the case where user had cached downloads before logging in
                // Wait a bit for downloads to load, then check
                kotlinx.coroutines.delay(500)
                val currentDownloads = downloadUtil.downloads.value
                val completedDownloads = currentDownloads.filter { it.value.state == Download.STATE_COMPLETED }
                if (completedDownloads.isNotEmpty()) {
                    Timber.tag("DownloadsScreen").d("📤 Found ${completedDownloads.size} local cached downloads - syncing to Supabase...")
                    var syncedCount = 0
                    completedDownloads.forEach { (songId, _) ->
                        // Check if already in repository
                        val isInRepository = cachedDownloadsRepository.isCached(songId).first()
                        if (!isInRepository) {
                            // Add to repository (which will sync to Supabase if logged in)
                            cachedDownloadsRepository.addCachedDownload(songId).fold(
                                onSuccess = {
                                    syncedCount++
                                    Timber.tag("DownloadsScreen").d("✅ Synced local cached download to Supabase: $songId")
                                },
                                onFailure = { e ->
                                    Timber.tag("DownloadsScreen").w(e, "⚠️ Failed to sync local cached download: $songId")
                                }
                            )
                        }
                    }
                    if (syncedCount > 0) {
                        Timber.tag("DownloadsScreen").d("✅ Synced $syncedCount local cached downloads to Supabase")
                    }
                    
                    // CRITICAL: Pre-cache thumbnails for ALL existing cached songs
                    // This ensures thumbnails work offline for songs downloaded before this feature
                    Timber.tag("DownloadsScreen").d("📸 Pre-caching thumbnails for ${completedDownloads.size} cached songs...")
                    var cachedThumbnailCount = 0
                    completedDownloads.forEach { (songId, _) ->
                        try {
                            val song = database.getSongById(songId)
                            val thumbnailUrl = song?.song?.thumbnailUrl
                            if (!thumbnailUrl.isNullOrEmpty()) {
                                val imageLoader = coil.ImageLoader(context)
                                val request = coil.request.ImageRequest.Builder(context)
                                    .data(thumbnailUrl)
                                    .memoryCacheKey(thumbnailUrl)
                                    .diskCacheKey(thumbnailUrl)
                                    .build()
                                imageLoader.execute(request)
                                cachedThumbnailCount++
                            }
                        } catch (e: Exception) {
                            Timber.tag("DownloadsScreen").w(e, "⚠️ Failed to pre-cache thumbnail for: $songId")
                        }
                    }
                    if (cachedThumbnailCount > 0) {
                        Timber.tag("DownloadsScreen").d("✅ Pre-cached $cachedThumbnailCount thumbnails for offline playback")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("DownloadsScreen").w(e, "Failed to sync cached downloads (non-critical)")
        }
    }
    
    // EVENT BUS FIX: Listen for download state changes and trigger UI refresh
    // This ensures instant UI updates without screen refresh
    var eventTrigger by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        com.musifly.android.download.DownloadEventBus.stateChangeEvents.collect { event ->
            android.util.Log.d("DownloadsScreen", "📢 Event received: Mission ${event.missionId} → ${event.newState}")
            
            // Trigger recomposition by updating state
            eventTrigger++
            
            // Force refresh download lists from ViewModel
            viewModel.refreshDownloads()
        }
    }
    
    // Calculate total stats (will recompute when eventTrigger changes)
    val totalActiveDownloads = remember(pendingDownloads, eventTrigger) { pendingDownloads.size }
    val totalFinishedDownloads = remember(finishedDownloads, eventTrigger) { finishedDownloads.size }
    val totalCachedSongs = cachedSongs.size
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Offline Library")
                        Text(
                            text = "$totalCachedSongs songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    com.musifly.android.ui.component.AppIconButton(
                        onClick = { navController.navigateUp() },
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back"
                    )
                },
                actions = {
                    // Cached songs actions
                    if (cachedSongs.isNotEmpty()) {
                        com.musifly.android.ui.component.AppIconButton(
                            onClick = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = "Cached Songs",
                                        items = cachedSongs.map { it.toMediaItem() }
                                    )
                                )
                            },
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play All"
                        )
                    }
                    
                    // Refresh button - syncs from Firestore
                    if (isSyncingFromSupabase) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp).padding(12.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        com.musifly.android.ui.component.AppIconButton(
                            onClick = { refreshCachedSongs() },
                            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Refresh),
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Store selected song IDs for cached tab
            var selectedCachedSongs by remember { mutableStateOf<Set<String>>(emptySet()) }
            
            CachedSongsTab(
                songs = cachedSongs,
                downloads = downloads,
                isPlaying = isPlaying,
                mediaMetadata = mediaMetadata,
                navController = navController,
                isSyncing = isSyncingFromSupabase,
                onRefresh = refreshCachedSongs,
                isSelectionMode = isSelectionMode,
                selectedSongs = selectedCachedSongs,
                onSelectionChange = { songId, selected ->
                    selectedCachedSongs = if (selected) {
                        selectedCachedSongs + songId
                    } else {
                        selectedCachedSongs - songId
                    }
                },
                onLongPress = { song ->
                    isSelectionMode = true
                    selectedCachedSongs = setOf(song.id)
                    selectedItems = emptySet() // Clear other selection
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDeleteSong = { showDeleteDialog = it }
            )
            
            // Selection action bar for cached songs
            if (isSelectionMode && selectedCachedSongs.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    SelectionActionBar(
                        selectedCount = selectedCachedSongs.size,
                        onSelectAll = {
                            selectedCachedSongs = if (selectedCachedSongs.size == cachedSongs.size) {
                                emptySet()
                            } else {
                                cachedSongs.map { it.id }.toSet()
                            }
                        },
                        onDelete = {
                            // Delete multiple cached songs from Supabase and local storage
                            coroutineScope.launch(Dispatchers.IO) {
                                val songsToDelete = cachedSongs.filter { it.id in selectedCachedSongs }
                                
                                try {
                                    val entryPoint = EntryPointAccessors.fromApplication(
                                        context,
                                        CachedDownloadsRepositoryEntryPoint::class.java
                                    )
                                    val cachedDownloadsRepository = entryPoint.cachedDownloadsRepository()
                                    
                                    songsToDelete.forEach { song ->
                                        // Remove from Firestore + Room
                                        cachedDownloadsRepository.removeCachedDownload(song.id).fold(
                                            onSuccess = {
                                                Timber.tag("DownloadsScreen").d("✅ Removed from repository: ${song.id}")
                                            },
                                            onFailure = { e ->
                                                Timber.tag("DownloadsScreen").e(e, "❌ Failed to remove from repository: ${song.id}")
                                            }
                                        )
                                        
                                        // Remove ExoPlayer cache
                                        kotlinx.coroutines.withContext(Dispatchers.Main) {
                                            DownloadService.sendRemoveDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                song.id,
                                                false
                                            )
                                        }
                                    }
                                    
                                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Deleted ${songsToDelete.size} cached songs",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Timber.tag("DownloadsScreen").e(e, "Error deleting cached songs")
                                }
                                
                                selectedCachedSongs = emptySet()
                                isSelectionMode = false
                            }
                        },
                        onRename = {
                            // Not applicable for cached songs
                        },
                        onShare = {
                            // Not applicable for cached songs
                        },
                        onCancel = {
                            selectedCachedSongs = emptySet()
                            isSelectionMode = false
                        }
                    )
                }
            }
        }
    }
        
        // Delete confirmation dialog
        showDeleteDialog?.let { item ->
            when (item) {

                is Song -> {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = null },
                        icon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        title = {
                            Text(
                                text = "Remove from Cache?",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "⚠️ This will permanently delete the cached song from this device and your cloud account.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    text = "by ${item.artists.joinToString { it.name }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            // Removal is now synced automatically by using the repository properly
                                            // The DownloadViewModel doesn't handle Song entities directly, so we use repository here
                                            val entryPoint = EntryPointAccessors.fromApplication(
                                                context,
                                                CachedDownloadsRepositoryEntryPoint::class.java
                                            )
                                            val cachedDownloadsRepository = entryPoint.cachedDownloadsRepository()
                                            
                                            // Remove from repository (Supabase + Room)
                                            cachedDownloadsRepository.removeCachedDownload(item.id).fold(
                                                onSuccess = {
                                                    Timber.tag("DownloadsScreen").d("✅ Removed cached download from repository: ${item.id}")
                                                },
                                                onFailure = { e ->
                                                    Timber.tag("DownloadsScreen").e(e, "❌ Failed to remove cached download from repository: ${item.id}")
                                                }
                                            )
                                            
                                            // Then remove ExoPlayer cache
                                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                                DownloadService.sendRemoveDownload(
                                                    context,
                                                    ExoDownloadService::class.java,
                                                    item.id,
                                                    false
                                                )
                                                showDeleteDialog = null
                                            }
                                        } catch (e: Exception) {
                                            Timber.tag("DownloadsScreen").e(e, "Error removing cached download")
                                            // Still try to remove ExoPlayer cache even if repository fails
                                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                                DownloadService.sendRemoveDownload(
                                                    context,
                                                    ExoDownloadService::class.java,
                                                    item.id,
                                                    false
                                                )
                                                showDeleteDialog = null
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Delete Permanently")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
}
/**
 * Format file size for display
 */
private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CachedSongsTab(
    songs: List<Song>,
    downloads: Map<String, Download>,
    isPlaying: Boolean,
    mediaMetadata: com.musifly.android.models.MediaMetadata?,
    navController: NavController,
    isSyncing: Boolean,
    onRefresh: () -> Unit,
    isSelectionMode: Boolean,
    selectedSongs: Set<String>,
    onSelectionChange: (String, Boolean) -> Unit,
    onLongPress: (Song) -> Unit,
    onDeleteSong: (Song) -> Unit
) {
    // songs parameter is already filtered for cached/completed downloads
    val cachedSongs = songs
    
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (cachedSongs.isEmpty()) {
            EmptyState(
                icon = R.drawable.offline,
                message = "No cached songs",
                subtitle = "Songs downloaded via ExoPlayer or synced from Firestore will appear here"
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                item {
                    // Summary card with sync button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSelectionMode) {
                                        "${selectedSongs.size} of ${cachedSongs.size} selected"
                                    } else {
                                        pluralStringResource(
                                            R.plurals.n_song,
                                            cachedSongs.size,
                                            cachedSongs.size
                                        )
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isSyncing) "Syncing from cloud..." else "Ready for offline playback",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            
                            // Sync button (hide in selection mode)
                            if (!isSelectionMode) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else {
                                    com.musifly.android.ui.component.AppIconButton(
                                        onClick = onRefresh,
                                        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Refresh),
                                        contentDescription = "Sync from cloud",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
                
                items(cachedSongs, key = { it.id }) { song ->
                    val download = downloads[song.id]
                    val hasLocalCache = download?.state == Download.STATE_COMPLETED
                    val isSyncedOnly = !hasLocalCache // Synced from Firestore but no local cache
                    val isSelected = song.id in selectedSongs
                    
                    SongListItem(
                        song = song,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        showInLibraryIcon = true,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Selection checkbox
                                if (isSelectionMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            onSelectionChange(song.id, checked)
                                        }
                                    )
                                } else {
                                    // Download status icon - show different icon if synced but not cached locally
                                    if (isSyncedOnly) {
                                        // Show download icon with secondary color to indicate it needs re-download
                                        Icon(
                                            painter = painterResource(R.drawable.download),
                                            contentDescription = "Synced, needs download",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.offline),
                                            contentDescription = "Cached",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    // Open/Play button for cached media
                                    AppIconButton(
                                        onClick = {
                                            // Play the cached song
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = "Cached Songs",
                                                        items = cachedSongs.map { it.toMediaItem() },
                                                        startIndex = cachedSongs.indexOfFirst { it.id == song.id }
                                                    )
                                                )
                                            }
                                        },
                                        painter = if (song.id == mediaMetadata?.id && isPlaying) {
                                            painterResource(R.drawable.pause)
                                        } else {
                                            painterResource(R.drawable.play)
                                        },
                                        contentDescription = null
                                    )
                                    
                                    // Menu button
                                    AppIconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        },
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        onSelectionChange(song.id, song.id !in selectedSongs)
                                    } else {
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = "Cached Songs",
                                                    items = cachedSongs.map { it.toMediaItem() },
                                                    startIndex = cachedSongs.indexOfFirst { it.id == song.id }
                                                )
                                            )
                                        }
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        onLongPress(song)
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

/**
 * Selection action bar for multi-select mode
 */
@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Select All button
                AppIconButton(
                    onClick = onSelectAll,
                    painter = rememberVectorPainter(Icons.Default.SelectAll),
                    contentDescription = "Select All"
                )
                // Delete button - prominent with error color
                AppIconButton(
                    onClick = onDelete,
                    painter = rememberVectorPainter(Icons.Default.Delete),
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: Int,
    message: String,
    subtitle: String? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
