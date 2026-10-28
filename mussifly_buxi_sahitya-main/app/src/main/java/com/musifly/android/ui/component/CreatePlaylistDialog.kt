package com.musifly.android.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.musifly.innertube.YouTube
import com.musifly.android.LocalDatabase
import com.musifly.android.R
import com.musifly.android.db.entities.PlaylistEntity
import com.musifly.android.sync.UserSessionManager
import com.musifly.android.sync.repository.PlaylistRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import com.musifly.android.ui.component.liquidglass.AdaptiveSwitch

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val database = LocalDatabase.current
    val context = androidx.compose.ui.platform.LocalContext.current
    // Use SupervisorJob to ensure coroutine continues even after composition is removed
    val coroutineScope = remember {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
    var syncedPlaylist by remember { mutableStateOf(false) }
    
    // Get PlaylistRepository for Firestore sync (if logged in)
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
    
    TextFieldDialog(
        icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
        title = { Text(text = stringResource(R.string.create_playlist)) },
        initialTextFieldValue = TextFieldValue(initialTextFieldValue ?: ""),
        onDismiss = onDismiss,
        onDone = { playlistName ->
            coroutineScope.launch {
                // If user is logged in, use PlaylistRepository (stores to Firestore)
                if (UserSessionManager.isLoggedIn() && playlistRepository != null) {
                    val result = playlistRepository.createPlaylist(playlistName)
                    result.fold(
                        onSuccess = { playlistId ->
                            android.util.Log.d("CreatePlaylistDialog", "✅ Playlist created in Firestore: $playlistId")
                        },
                        onFailure = { e ->
                            android.util.Log.e("CreatePlaylistDialog", "❌ Failed to create playlist in Firestore", e)
                        }
                    )
                } else {
                    // Guest mode or old-style playlist: Create YouTube playlist if synced
                    val browseId = if (syncedPlaylist)
                        YouTube.createPlaylist(playlistName)
                    else null
                    database.query {
                        insert(
                            PlaylistEntity(
                                name = playlistName,
                                browseId = browseId?.toString(),
                                bookmarkedAt = LocalDateTime.now(),
                                isEditable = true,
                            )
                        )
                    }
                }
            }
        },
        extraContent = {
            if (allowSyncing) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 40.dp)
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.sync_playlist),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.allows_for_sync_witch_youtube),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        AdaptiveSwitch(
                            checked = syncedPlaylist,
                            onCheckedChange = {
                                syncedPlaylist = !syncedPlaylist
                            },
                        )
                    }
                }
            }
        }
    )
}
