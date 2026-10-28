package com.musifly.android.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musifly.android.LocalDatabase
import com.musifly.android.R
import com.musifly.android.db.entities.PlaylistEntity
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.ui.component.liquidglass.LiquidGlassButton
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.SpotifyImportManager
import com.musifly.android.utils.SpotifyPlaylistScraper
import com.musifly.android.sync.repository.PlaylistRepository
import com.musifly.android.di.PlaylistRepositoryEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Import state enum
 */
enum class ImportState {
    INPUT,
    IMPORTING,
    COMPLETE,
    ERROR
}

/**
 * Spotify Import Dialog
 * Allows users to import playlists from Spotify by pasting a link
 */
@Composable
fun SpotifyImportDialog(
    onDismiss: () -> Unit,
    onImportComplete: (PlaylistEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
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
    
    var spotifyLink by remember { mutableStateOf("") }
    var importState by remember { mutableStateOf(ImportState.INPUT) }
    var currentProgress by remember { mutableIntStateOf(0) }
    var totalTracks by remember { mutableIntStateOf(0) }
    var currentTrackName by remember { mutableStateOf("") }
    var progressPercent by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf("") }
    var importedCount by remember { mutableIntStateOf(0) }
    var createdPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var failedTracks by remember { mutableStateOf<List<SpotifyPlaylistScraper.SpotifyTrack>>(emptyList()) }
    
    val isValidLink = SpotifyPlaylistScraper.isValidSpotifyPlaylistLink(spotifyLink)
    
    val (themeMode) = rememberEnumPreference(
        ThemeModeKey, 
        defaultValue = ThemeMode.DARK
    )
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    // Shared content logic
    val dialogContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (importState) {
                ImportState.INPUT -> {
                    // Instructions
                    Text(
                        text = "Paste a Spotify playlist link below. Public playlists only.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Input field with paste button
                    OutlinedTextField(
                        value = spotifyLink,
                        onValueChange = { spotifyLink = it },
                        label = { Text("Spotify Playlist Link") },
                        placeholder = { Text("https://open.spotify.com/playlist/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    clipboardManager.getText()?.text?.let { 
                                        spotifyLink = it 
                                    }
                                }
                            ) {
                                Text("Paste")
                            }
                        },
                        isError = spotifyLink.isNotBlank() && !isValidLink,
                        supportingText = {
                            if (spotifyLink.isNotBlank() && !isValidLink) {
                                Text(
                                    "Invalid Spotify playlist link",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                    
                    // Supported formats info
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Supported formats:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "• https://open.spotify.com/playlist/...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• spotify:playlist:...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Info text
                    Text(
                        text = "⚠️ Songs will be matched using YouTube search. Some tracks may not be found exactly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                ImportState.IMPORTING -> {
                    // Progress UI
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Text(
                            text = if (totalTracks > 0) 
                                "Importing track $currentProgress of $totalTracks" 
                            else 
                                "Fetching playlist...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (currentTrackName.isNotBlank()) {
                            Text(
                                text = currentTrackName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        if (totalTracks > 0) {
                            LinearProgressIndicator(
                                progress = { progressPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                            )
                            
                            Text(
                                text = "${(progressPercent * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Text(
                            text = "Please wait, this may take a few minutes...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                ImportState.COMPLETE -> {
                    // Success UI
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check_circle),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        
                        Text(
                            text = "Successfully Imported!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "$importedCount of $totalTracks songs imported",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (createdPlaylist != null) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Playlist created:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = createdPlaylist!!.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        if (failedTracks.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${failedTracks.size} tracks could not be found:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    failedTracks.take(5).forEach { track ->
                                        Text(
                                            text = "• ${track.title} - ${track.artist}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (failedTracks.size > 5) {
                                        Text(
                                            text = "...and ${failedTracks.size - 5} more",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                ImportState.ERROR -> {
                    // Error UI
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        
                        Text(
                            text = "Import Failed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        
                        Text(
                            text = "Please check the link and try again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // Shared buttons logic
    val importButtonAction = {
        if (isValidLink) {
            importState = ImportState.IMPORTING
            
            scope.launch {
                val callback = object : SpotifyImportManager.ImportCallback {
                    override fun onProgress(current: Int, total: Int, currentTrack: String) {
                        scope.launch(Dispatchers.Main) {
                            currentProgress = current
                            totalTracks = total
                            currentTrackName = currentTrack
                            progressPercent = if (total > 0) current.toFloat() / total else 0f
                        }
                    }
                    
                    override fun onTrackImported(track: SpotifyPlaylistScraper.SpotifyTrack, success: Boolean) {
                    }
                    
                    override fun onComplete(playlist: PlaylistEntity, imported: Int, total: Int) {
                        scope.launch(Dispatchers.Main) {
                            createdPlaylist = playlist
                            importedCount = imported
                            totalTracks = total
                            importState = ImportState.COMPLETE
                        }
                    }
                    
                    override fun onError(error: String) {
                        scope.launch(Dispatchers.Main) {
                            errorMessage = error
                            importState = ImportState.ERROR
                        }
                    }
                }
                
                val result = SpotifyImportManager.importPlaylist(
                    playlistUrl = spotifyLink,
                    database = database,
                    playlistRepository = playlistRepository,
                    callback = callback
                )
                
                if (result.success && result.playlist != null) {
                    failedTracks = result.failedTracks
                    withContext(Dispatchers.Main) {
                        onImportComplete(result.playlist)
                    }
                }
            }
        }
    }
    
    val buttonsContent: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLiquidGlass) {
                // Liquid Glass Buttons
                if (importState == ImportState.INPUT) {
                    LiquidGlassButton(onClick = onDismiss, text = "Cancel")
                } else if (importState == ImportState.ERROR) {
                    LiquidGlassButton(onClick = onDismiss, text = "Close")
                }
                
                Spacer(Modifier.width(8.dp))
                
                when (importState) {
                    ImportState.INPUT -> {
                        LiquidGlassButton(
                            onClick = { importButtonAction() },
                            text = "Import",
                            isPrimary = true,
                            enabled = isValidLink
                        )
                    }
                    ImportState.IMPORTING -> {}
                    ImportState.COMPLETE -> {
                        LiquidGlassButton(
                            onClick = { onDismiss() },
                            text = "Done",
                            isPrimary = true
                        )
                    }
                    ImportState.ERROR -> {
                        LiquidGlassButton(
                            onClick = {
                                importState = ImportState.INPUT
                                errorMessage = ""
                            },
                            text = "Try Again",
                            isPrimary = true
                        )
                    }
                }
            } else {
                // Standard Material buttons
                if (importState == ImportState.INPUT) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                } else if (importState == ImportState.ERROR) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                
                Spacer(Modifier.width(8.dp))
                
                when (importState) {
                    ImportState.INPUT -> {
                        Button(
                            onClick = { importButtonAction() },
                            enabled = isValidLink
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Import")
                        }
                    }
                    ImportState.IMPORTING -> {}
                    ImportState.COMPLETE -> {
                        Button(onClick = { onDismiss() }) {
                            Text("Done")
                        }
                    }
                    ImportState.ERROR -> {
                        Button(
                            onClick = {
                                importState = ImportState.INPUT
                                errorMessage = ""
                            }
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }

    if (isLiquidGlass) {
        LiquidGlassDialog(
            onDismiss = {
                if (importState != ImportState.IMPORTING) {
                    onDismiss()
                }
            },
            title = {
                 Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Spotify icon placeholder (green circle)
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1DB954) // Spotify green
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = when (importState) {
                            ImportState.INPUT -> "Import from Spotify"
                            ImportState.IMPORTING -> "Importing Playlist"
                            ImportState.COMPLETE -> "Import Complete"
                            ImportState.ERROR -> "Import Failed"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            content = {
                dialogContent()
            },
            buttons = {
                buttonsContent()
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = { 
                if (importState != ImportState.IMPORTING) {
                    onDismiss()
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Spotify icon placeholder (green circle)
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1DB954) // Spotify green
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = when (importState) {
                            ImportState.INPUT -> "Import from Spotify"
                            ImportState.IMPORTING -> "Importing Playlist"
                            ImportState.COMPLETE -> "Import Complete"
                            ImportState.ERROR -> "Import Failed"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                dialogContent()
            },
            confirmButton = {
                // Confirm button logic from buttonsContent
                when (importState) {
                    ImportState.INPUT -> {
                        Button(
                            onClick = { importButtonAction() },
                            enabled = isValidLink
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Import")
                        }
                    }
                    ImportState.IMPORTING -> {
                        // No button during import
                    }
                    ImportState.COMPLETE -> {
                        Button(
                            onClick = {
                                onDismiss()
                            }
                        ) {
                            Text("Done")
                        }
                    }
                    ImportState.ERROR -> {
                        Button(
                            onClick = {
                                importState = ImportState.INPUT
                                errorMessage = ""
                            }
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            },
            dismissButton = {
                // Dismiss button logic from buttonsContent
                if (importState == ImportState.INPUT) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                } else if (importState == ImportState.ERROR) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        )
    }
}
