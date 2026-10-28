package com.musifly.android.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.R
import com.musifly.android.constants.ChipSortTypeKey
import com.musifly.android.constants.LibraryFilter
import dagger.hilt.android.EntryPointAccessors
import com.musifly.android.sync.UserSessionManager


import com.musifly.android.sync.repository.PlaylistRepository
import com.musifly.android.sync.repository.SyncStatus
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Warning
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import com.musifly.android.ui.component.ChipsRow
import com.musifly.android.ui.component.ImportPlaylistDialog
import com.musifly.android.ui.component.VerticalFastScroller
import com.musifly.android.ui.LocalMusicActivity
import com.musifly.android.utils.rememberEnumPreference
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

    val lazyListState = rememberLazyListState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Import dialog state
    var showImportPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    
    // Sync state
    val userPlaylistViewModel = hiltViewModel<com.musifly.android.viewmodels.UserPlaylistViewModel>()
    val syncStatus by userPlaylistViewModel.syncStatus.collectAsState()
    
    // Handle Sync Status side effects (Snackbars)
    LaunchedEffect(syncStatus) {
        when (val status = syncStatus) {
            is SyncStatus.Success -> {
                snackbarHostState.showSnackbar(status.message)
            }
            is SyncStatus.Error -> {
                snackbarHostState.showSnackbar(status.message)
            }
            else -> {}
        }
    }
    
    // Get PlaylistRepository
    val playlistRepository = remember {
        try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                com.musifly.android.di.PlaylistRepositoryEntryPoint::class.java
            ).playlistRepository()
        } catch (e: Exception) {
            null
        }
    }
    
    // Check if user is logged in
    val isLoggedIn: Boolean = remember { UserSessionManager.isLoggedIn() }
    
    // Automatic sync on screen load for logged-in users
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
             userPlaylistViewModel.syncFromSupabase()
        }
    }
    
    // Sync function
    // Sync function
    val syncToSupabase: () -> Unit = {
        if (playlistRepository == null || !isLoggedIn) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Please login to sync data")
            }
        } else {
            // Trigger sync via ViewModel or Repository directly if exposed
            // Since we need to call suspend function, launch locally
            coroutineScope.launch {
                userPlaylistViewModel.syncFromSupabase() // Sync FROM Supabase first to get latest
                // Then optionally sync TO Supabase if needed, but repository usually handles logic
                // For now, let's assume one button triggers the main sync flow
            }
        }
    }

    val filterContent = @Composable {
        ChipsRow(
            chips =
                listOf(
                    LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                    LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                    LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                ),
            currentValue = filterType,
            onValueUpdate = {
                filterType =
                    if (filterType == it) {
                        LibraryFilter.LIBRARY
                    } else {
                        it
                    }
            },
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sync Status Blocking Dialog
            if (syncStatus is SyncStatus.Syncing) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { /* Prevent dismissal */ },
                    title = { Text("Syncing Data") },
                    text = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                            Text("Please wait while we sync your playlists...")
                        }
                    },
                    confirmButton = {},
                    dismissButton = {},
                    properties = androidx.compose.ui.window.DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    )
                )
            }
            
            Box(modifier = Modifier.weight(1f)) {
                VerticalFastScroller(
                    listState = lazyListState,
                    topContentPadding = 16.dp,
                    endContentPadding = 0.dp
                )
                {
                    when (filterType) {
                        LibraryFilter.LIBRARY -> LibraryMixScreen(
                            navController = navController,
                            filterContent = filterContent,
                            onNavigateToLocalMusic = { 
                                // Start LocalMusicActivity instead of changing state
                                context.startActivity(LocalMusicActivity.createIntent(context))
                            }
                        )
                        LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(
                            navController = navController,
                            filterContent = filterContent,
                            onNavigateToLocalMusic = { 
                                // Start LocalMusicActivity instead of changing state
                                context.startActivity(LocalMusicActivity.createIntent(context))
                            }
                        )
                        LibraryFilter.SONGS -> LibrarySongsScreen(
                            navController,
                            { filterType = LibraryFilter.LIBRARY })
        
                        LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                            navController,
                            { filterType = LibraryFilter.LIBRARY })
        
                        LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                            navController,
                            { filterType = LibraryFilter.LIBRARY })
                        
                        LibraryFilter.LOCAL -> {
                            // This case should not happen since we use a separate activity now
                            // But we need it for exhaustive when expression
                            // If somehow LOCAL is set, redirect to LIBRARY
                            LibraryMixScreen(
                                navController = navController,
                                filterContent = filterContent,
                                onNavigateToLocalMusic = { 
                                    context.startActivity(LocalMusicActivity.createIntent(context))
                                }
                            )
                        }
                    }
                }
            }
        }
        

        
        // FAB Buttons - WhatsApp style at bottom right
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Sync to Supabase Button (only show if logged in)
            if (isLoggedIn) {
                com.musifly.android.ui.component.AppFloatingActionButton(
                    onClick = syncToSupabase,
                    modifier = Modifier.padding(bottom = 16.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    val isLiquid = com.musifly.android.ui.component.liquidglass.isLiquidGlassActive()
                    val tint = if (isLiquid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
                    
                    when (syncStatus) {
                        is SyncStatus.Syncing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = tint
                            )
                        }
                        is SyncStatus.Success -> {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Synced",
                                tint = tint
                            )
                        }
                        is SyncStatus.Error -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Sync Failed",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync from Supabase",
                                tint = tint
                            )
                        }
                    }
                }
            }
            
            // Import Playlist FAB Button
            com.musifly.android.ui.component.AppFloatingActionButton(
                onClick = { showImportPlaylistDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                val isLiquid = com.musifly.android.ui.component.liquidglass.isLiquidGlassActive()
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.import_from_external),
                    tint = if (isLiquid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Snackbar for sync messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
        )
    }
    
    // Import Playlist Dialog
    if (showImportPlaylistDialog) {
        ImportPlaylistDialog(
            onDismiss = { showImportPlaylistDialog = false },
            onImportComplete = { /* Handle imported items */ }
        )
    }
}
