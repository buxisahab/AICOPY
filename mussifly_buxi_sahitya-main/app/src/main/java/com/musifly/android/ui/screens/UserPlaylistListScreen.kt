package com.musifly.android.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.R
import com.musifly.android.sync.local.UserPlaylistEntity
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.ListItem
import com.musifly.android.ui.component.TextFieldDialog
import com.musifly.android.viewmodels.UserPlaylistViewModel
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import kotlinx.coroutines.launch

/**
 * Screen displaying user's playlists
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UserPlaylistListScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val viewModel: UserPlaylistViewModel = hiltViewModel()
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Playlists") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Playlist")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
        ) {
            when {
                playlists.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.playlist_play),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No playlists",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Create a playlist to organize your favorite songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Playlist")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = playlists,
                            key = { it.playlistId }
                        ) { playlist ->
                            PlaylistListItem(
                                playlist = playlist,
                                onPlaylistClick = {
                                    navController.navigate("user_playlist/${playlist.playlistId}")
                                },
                                onRenameClick = {
                                    showRenameDialog = playlist.playlistId
                                },
                                onDeleteClick = {
                                    showDeleteDialog = playlist.playlistId
                                },
                                modifier = Modifier.combinedClickable(
                                    onClick = {
                                        navController.navigate("user_playlist/${playlist.playlistId}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // Show options menu
                                    }
                                )
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Create Playlist Dialog
    if (showCreateDialog) {
        TextFieldDialog(
            modifier = Modifier,
            initialTextFieldValue = TextFieldValue(""),
            onDone = { text ->
                if (text.isNotBlank()) {
                    scope.launch {
                        viewModel.createPlaylist(text) { result ->
                            result.onSuccess {
                                showCreateDialog = false
                            }.onFailure {
                                // Show error
                            }
                        }
                    }
                } else {
                    showCreateDialog = false
                }
            },
            onDismiss = { showCreateDialog = false },
            singleLine = true,
            isInputValid = { it.isNotBlank() }
        )
    }
    
    // Rename Playlist Dialog
    showRenameDialog?.let { playlistId ->
        val playlist = playlists.find { it.playlistId == playlistId }
        if (playlist != null) {
            TextFieldDialog(
                modifier = Modifier,
                initialTextFieldValue = TextFieldValue(playlist.name),
                onDone = { text ->
                    if (text.isNotBlank() && text != playlist.name) {
                        scope.launch {
                            viewModel.renamePlaylist(playlistId, text) { result ->
                                result.onSuccess {
                                    showRenameDialog = null
                                }
                            }
                        }
                    } else {
                        showRenameDialog = null
                    }
                },
                onDismiss = { showRenameDialog = null },
                singleLine = true,
                isInputValid = { it.isNotBlank() }
            )
        }
    }
    
    // Delete Confirmation Dialog
    showDeleteDialog?.let { playlistId ->
        val playlist = playlists.find { it.playlistId == playlistId }
        if (playlist != null) {
            val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
            val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

            if (isLiquidGlass) {
                LiquidGlassDialog(
                    onDismiss = { showDeleteDialog = null },
                    title = { Text("Delete Playlist") },
                    content = { Text("Are you sure you want to delete \"${playlist.name}\"? This cannot be undone.") },
                    buttons = {
                        TextButton(onClick = { showDeleteDialog = null }) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = {
                                scope.launch {
                                    viewModel.deletePlaylist(playlistId) { result ->
                                        result.onSuccess {
                                            showDeleteDialog = null
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            } else {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = null },
                    title = { Text("Delete Playlist") },
                    text = { Text("Are you sure you want to delete \"${playlist.name}\"? This cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    viewModel.deletePlaylist(playlistId) { result ->
                                        result.onSuccess {
                                            showDeleteDialog = null
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
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

@Composable
private fun PlaylistListItem(
    playlist: UserPlaylistEntity,
    onPlaylistClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        title = playlist.name,
        subtitle = "Playlist • Updated ${java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(playlist.updatedAt))}",
        thumbnailContent = {
            // Empty thumbnail - can add playlist icon here if needed
        },
        trailingContent = {
            Row {
                IconButton(onClick = onRenameClick) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        modifier = modifier
    )
}
