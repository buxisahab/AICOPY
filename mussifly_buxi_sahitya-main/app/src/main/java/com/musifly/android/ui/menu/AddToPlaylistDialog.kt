package com.musifly.android.ui.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.musifly.innertube.YouTube
import com.musifly.innertube.utils.parseCookieString
import com.musifly.android.LocalDatabase
import com.musifly.android.R
import com.musifly.android.constants.InnerTubeCookieKey
import com.musifly.android.constants.ListThumbnailSize
import com.musifly.android.db.entities.Playlist
import com.musifly.android.sync.repository.PlaylistRepository
import com.musifly.android.sync.UserSessionManager
import com.musifly.android.ui.component.CreatePlaylistDialog
import dagger.hilt.android.EntryPointAccessors
import com.musifly.android.di.PlaylistRepositoryEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import com.musifly.android.ui.component.DefaultDialog
import com.musifly.android.ui.component.ListDialog
import com.musifly.android.ui.component.ListItem
import com.musifly.android.ui.component.PlaylistListItem
import com.musifly.android.utils.rememberPreference
import timber.log.Timber

@Composable
fun AddToPlaylistDialog(
    isVisible: Boolean,
    allowSyncing: Boolean = true,
    initialTextFieldValue: String? = null,
    onGetSong: suspend (Playlist) -> List<String>, // list of song ids. Songs should be inserted to database in this function.
    onDismiss: () -> Unit,
) {
    val database = LocalDatabase.current
    val context = androidx.compose.ui.platform.LocalContext.current
    // Use SupervisorJob to ensure coroutine continues even after composition is removed
    val coroutineScope = remember {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
    var playlists by remember {
        mutableStateOf(emptyList<Playlist>())
    }
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    var showCreatePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showDuplicateDialog by remember {
        mutableStateOf(false)
    }
    var selectedPlaylist by remember {
        mutableStateOf<Playlist?>(null)
    }
    var songIds by remember {
        mutableStateOf<List<String>?>(null) // list is not saveable
    }
    var duplicates by remember {
        mutableStateOf(emptyList<String>())
    }
    
    // Get PlaylistRepository for Firestore sync (if logged in)
    val playlistRepository = remember {
        try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                PlaylistRepositoryEntryPoint::class.java
            ).playlistRepository()
        } catch (e: Exception) {
            null
        }
    }

    // Refresh playlists when create dialog is dismissed (new playlist might have been created)
    LaunchedEffect(showCreatePlaylistDialog) {
        if (!showCreatePlaylistDialog) {
            // Dialog was dismissed - refresh playlists
            kotlinx.coroutines.delay(500) // Small delay to allow Firestore write to complete
        }
    }
    
    LaunchedEffect(Unit) {
        // Combine old playlists (from playlist table) and new user playlists (from user_playlists table)
        if (UserSessionManager.isLoggedIn() && playlistRepository != null) {
            combine(
                database.editablePlaylistsByCreateDateAsc(),
                playlistRepository.getAllPlaylists()
            ) { oldPlaylists, userPlaylists ->
                // Convert UserPlaylistEntity to Playlist format
                val convertedUserPlaylists = userPlaylists.map { userPlaylist ->
                    val createdAt = try {
                        LocalDateTime.ofEpochSecond(
                            userPlaylist.createdAt / 1000,
                            0,
                            ZoneOffset.UTC
                        )
                    } catch (e: Exception) {
                        LocalDateTime.now()
                    }
                    
                    val updatedAt = try {
                        LocalDateTime.ofEpochSecond(
                            userPlaylist.updatedAt / 1000,
                            0,
                            ZoneOffset.UTC
                        )
                    } catch (e: Exception) {
                        LocalDateTime.now()
                    }
                    
                    val playlistEntity = com.musifly.android.db.entities.PlaylistEntity(
                        id = userPlaylist.playlistId,
                        name = userPlaylist.name,
                        createdAt = createdAt,
                        lastUpdateTime = updatedAt,
                        isEditable = true,
                        bookmarkedAt = LocalDateTime.now()
                    )
                    
                    Playlist(
                        playlist = playlistEntity,
                        songCount = 0,
                        thumbnails = emptyList()
                    )
                }
                
                // Combine both lists and sort by creation date (newest first)
                (oldPlaylists + convertedUserPlaylists).sortedByDescending { 
                    it.playlist.createdAt ?: LocalDateTime.MIN 
                }
            }.collect {
                playlists = it
            }
        } else {
            // Guest mode or no repository - use old logic
            database.editablePlaylistsByCreateDateAsc().collect {
                playlists = it.asReversed()
            }
        }
    }

    if (isVisible) {
        ListDialog(
            onDismiss = onDismiss,
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.create_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.add),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize)
                        )
                    },
                    modifier = Modifier.clickable {
                        showCreatePlaylistDialog = true
                    }
                )
            }

            items(playlists) { playlist ->
                PlaylistListItem(
                    playlist = playlist,
                    modifier = Modifier.clickable {
                        selectedPlaylist = playlist
                        coroutineScope.launch {
                            if (songIds == null) {
                                songIds = onGetSong(playlist)
                            }
                            duplicates = database.playlistDuplicates(playlist.id, songIds!!)
                            if (duplicates.isNotEmpty()) {
                                showDuplicateDialog = true
                            } else {
                                onDismiss()
                                
                                // Check if this is a user playlist (starts with "UP") or old playlist
                                val isUserPlaylist = playlist.playlist.id.startsWith("UP")
                                val isYouTubePlaylist = playlist.playlist.browseId != null
                                
                                if (isUserPlaylist && playlistRepository != null) {
                                    // User playlist - use repository to add to Firestore
                                    // Add songs sequentially to ensure proper order and error handling
                                    songIds?.forEach { songId ->
                                        val result = playlistRepository.addSongToPlaylist(playlist.playlist.id, songId)
                                        result.fold(
                                            onSuccess = {
                                                Timber.tag("AddToPlaylistDialog").d("✅ Song added to playlist in Firestore: ${playlist.playlist.id}/$songId")
                                            },
                                            onFailure = { e ->
                                                Timber.tag("AddToPlaylistDialog").e(e, "❌ Failed to add song to playlist in Firestore: ${playlist.playlist.id}/$songId")
                                                // Log error but continue with other songs
                                            }
                                        )
                                    }
                                } else {
                                    // Old playlist - add to local DB
                                    // Note: Old playlists are local-only, not synced to Firestore
                                    database.addSongToPlaylist(playlist, songIds!!)
                                    Timber.tag("AddToPlaylistDialog").d("✅ Songs added to old playlist (local DB only): ${playlist.playlist.id}, songs: ${songIds!!.size}")

                                    if (isYouTubePlaylist) {
                                        playlist.playlist.browseId?.let { plist ->
                                            songIds?.forEach {
                                                YouTube.addToPlaylist(plist, it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    // duplicate songs warning
    if (showDuplicateDialog) {
        DefaultDialog(
            title = { Text(stringResource(R.string.duplicates)) },
            buttons = {
                TextButton(
                    onClick = {
                        showDuplicateDialog = false
                        onDismiss()
                        val playlist = selectedPlaylist!!
                        val isUserPlaylist = playlist.playlist.id.startsWith("UP")
                        val filteredSongIds = songIds!!.filter { !duplicates.contains(it) }
                        
                        coroutineScope.launch {
                            if (isUserPlaylist && playlistRepository != null) {
                                // User playlist - use repository to add to Firestore
                                filteredSongIds.forEach { songId ->
                                    val result = playlistRepository.addSongToPlaylist(playlist.playlist.id, songId)
                                    result.fold(
                                        onSuccess = {
                                            Timber.tag("AddToPlaylistDialog").d("✅ Song added to playlist in Firestore: ${playlist.playlist.id}/$songId")
                                        },
                                        onFailure = { e ->
                                            Timber.tag("AddToPlaylistDialog").e(e, "❌ Failed to add song to playlist in Firestore: ${playlist.playlist.id}/$songId")
                                        }
                                    )
                                }
                            } else {
                                // Old playlist or guest mode - use old logic
                                database.transaction {
                                    addSongToPlaylist(playlist, filteredSongIds)
                                }
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.skip_duplicates))
                }

                TextButton(
                    onClick = {
                        showDuplicateDialog = false
                        onDismiss()
                        val playlist = selectedPlaylist!!
                        val isUserPlaylist = playlist.playlist.id.startsWith("UP")
                        
                        coroutineScope.launch {
                            if (isUserPlaylist && playlistRepository != null) {
                                // User playlist - use repository to add to Firestore
                                songIds!!.forEach { songId ->
                                    val result = playlistRepository.addSongToPlaylist(playlist.playlist.id, songId)
                                    result.fold(
                                        onSuccess = {
                                            Timber.tag("AddToPlaylistDialog").d("✅ Song added to playlist in Firestore: ${playlist.playlist.id}/$songId")
                                        },
                                        onFailure = { e ->
                                            Timber.tag("AddToPlaylistDialog").e(e, "❌ Failed to add song to playlist in Firestore: ${playlist.playlist.id}/$songId")
                                        }
                                    )
                                }
                            } else {
                                // Old playlist or guest mode - use old logic
                                database.transaction {
                                    addSongToPlaylist(playlist, songIds!!)
                                }
                            }
                        }
                    }
                ) {
                    Text(stringResource(R.string.add_anyway))
                }

                TextButton(
                    onClick = {
                        showDuplicateDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showDuplicateDialog = false
            }
        ) {
            Text(
                text = if (duplicates.size == 1) {
                    stringResource(R.string.duplicates_description_single)
                } else {
                    stringResource(R.string.duplicates_description_multiple, duplicates.size)
                },
                textAlign = TextAlign.Start,
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}