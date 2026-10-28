package com.musifly.android.ui.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.MusicVideo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.musifly.android.importer.ExternalMediaImporter
import com.musifly.android.utils.UrlChecker
import com.musifly.android.LocalDatabase
import com.musifly.android.db.entities.PlaylistEntity
import com.musifly.android.db.DatabaseDao
import com.musifly.android.sync.repository.PlaylistRepository
import com.musifly.android.di.PlaylistRepositoryEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import android.widget.Toast
import android.util.Log
import java.time.LocalDateTime
import java.util.UUID

/**
 * Professional Import Type Button Component
 */
@Composable
private fun ImportTypeButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    // Detect Liquid Glass theme
    val (themeMode) = com.musifly.android.utils.rememberEnumPreference(
        com.musifly.android.constants.ThemeModeKey,
        defaultValue = com.musifly.android.constants.ThemeMode.DARK
    )
    val isLiquidGlass = themeMode == com.musifly.android.constants.ThemeMode.LIQUID_GLASS
    
    if (isLiquidGlass) {
        // Use LiquidGlassButton for Liquid Glass theme
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                onClick = onClick,
                text = label,
                modifier = Modifier.weight(1f),
                enabled = enabled
            )
        }
    } else {
        // Standard Material 3 design for other themes
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable(enabled = enabled) {
                    if (enabled) onClick()
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (enabled) 4.dp else 0.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = if (enabled) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                        }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon container with gradient background
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = if (enabled) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }
                            )
                            .shadow(
                                elevation = if (enabled) 4.dp else 0.dp,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = if (enabled) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(18.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                label.contains("Spotify") -> "Import from Spotify"
                                label.contains("YouTube Music") -> "Import from YouTube Music"
                                label.contains("YouTube") -> "Import from YouTube"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Professional Import Button Component
 */
@Composable
private fun ImportButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(enabled = enabled) {
                if (enabled) onClick()
            }
            .shadow(
                elevation = if (enabled) 2.dp else 0.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon on the left
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (enabled) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.onSecondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            
            // Label in the middle
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Checkbox on the right (when enabled)
            if (enabled) {
                androidx.compose.material3.Checkbox(
                    checked = true,
                    onCheckedChange = null,
                    enabled = false,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(20.dp))
            }
        }
    }
}

/**
 * Dialog for importing playlists from external sources
 * Converted from Bloomee Music Player (Flutter)
 */
@Composable
fun ImportPlaylistDialog(
    onDismiss: () -> Unit,
    onImportComplete: (List<com.musifly.android.models.MediaMetadata>) -> Unit = { }
) {
    var urlText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var importState by remember { mutableStateOf<ExternalMediaImporter.ImporterState?>(null) }
    var selectedImportType by remember { mutableStateOf<String?>(null) }
    var showUrlField by remember { mutableStateOf(false) }
    var importedSongs by remember { mutableStateOf<List<com.musifly.android.models.MediaMetadata>>(emptyList()) }
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    
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
    
    // Job to track and cancel ongoing import
    var importJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    // Songs are now saved directly in ExternalMediaImporter during import (like bloomee app)
    // No need for separate save function
    
    // Auto-detect URL type when URL is pasted
    val detectedUrlType = remember(urlText) {
        if (urlText.isNotEmpty() && UrlChecker.isUrl(urlText)) {
            UrlChecker.getUrlType(urlText)
        } else null
    }
    
    val (themeMode) = com.musifly.android.utils.rememberEnumPreference(
        com.musifly.android.constants.ThemeModeKey, 
        defaultValue = com.musifly.android.constants.ThemeMode.DARK
    )
    val isLiquidGlass = themeMode == com.musifly.android.constants.ThemeMode.LIQUID_GLASS

    if (isLiquidGlass) {
        com.musifly.android.ui.component.liquidglass.LiquidGlassDialog(
            onDismiss = {
                if (!isLoading) {
                    onDismiss()
                }
            },
            title = { Text("Import Playlist") },
            content = {
                // Content area - show either normal content OR progress, not both
                if (isLoading) {
                    // Progress indicator - full height when loading
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            LinearProgressIndicator(
                                progress = if (importState?.totalItems ?: 0 > 0) {
                                    (importState?.importedItems ?: 0).toFloat() / (importState?.totalItems ?: 1).toFloat()
                                } else 0f,
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = importState?.message ?: "Importing...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (importState?.totalItems ?: 0 > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${importState?.importedItems ?: 0} / ${importState?.totalItems ?: 0} items",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Scrollable content - only show when NOT loading
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                         // Header Subtitle
                        Text(
                            text = "Import from Spotify, YouTube & YouTube Music",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Step 1: Import Type Selection (if URL field not shown yet)
                        AnimatedVisibility(
                            visible = !showUrlField && selectedImportType == null && urlText.isEmpty(),
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Select Source",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Choose a platform to import from",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ImportTypeButton(
                                label = "Spotify",
                                icon = Icons.Default.MusicNote,
                                onClick = {
                                    selectedImportType = "Spotify"
                                    showUrlField = true
                                },
                                enabled = !isLoading
                            )
                            ImportTypeButton(
                                label = "YouTube",
                                icon = Icons.Default.VideoLibrary,
                                onClick = {
                                    selectedImportType = "YouTube"
                                    showUrlField = true
                                },
                                enabled = !isLoading
                            )
                            ImportTypeButton(
                                label = "YouTube Music",
                                icon = Icons.Default.MusicVideo,
                                onClick = {
                                    selectedImportType = "YouTube Music"
                                    showUrlField = true
                                },
                                enabled = !isLoading
                            )
                        }
                    }
                }
                
                // Hidden: OR section and Paste URL option removed
                
                // URL Input Field (shown when import type selected OR always available)
                AnimatedVisibility(
                    visible = showUrlField || urlText.isNotEmpty() || selectedImportType != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { 
                            urlText = it
                            errorMessage = null
                            if (it.isNotEmpty()) {
                                showUrlField = true
                            }
                        },
                        label = { 
                            Text(
                                when {
                                    selectedImportType?.contains("Spotify") == true -> "Spotify URL"
                                    selectedImportType?.contains("YouTube Music") == true -> "YouTube Music URL"
                                    selectedImportType?.contains("YouTube") == true -> "YouTube URL"
                                    detectedUrlType?.name?.contains("SPOTIFY") == true -> "Spotify URL detected"
                                    detectedUrlType?.name?.contains("YOUTUBE") == true -> "YouTube URL detected"
                                    else -> "Paste URL here"
                                },
                                fontWeight = FontWeight.Medium
                            )
                        },
                        placeholder = { 
                            Text(
                                when {
                                    selectedImportType?.contains("Spotify") == true -> "https://open.spotify.com/playlist/..."
                                    selectedImportType?.contains("YouTube Music") == true -> "https://music.youtube.com/playlist?list=..."
                                    selectedImportType?.contains("YouTube") == true -> "https://youtube.com/playlist?list=..."
                                    else -> "https://open.spotify.com/... or https://youtube.com/..."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    // Keyboard dismiss
                                }
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Link, 
                                    contentDescription = "URL",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (urlText.isNotEmpty()) {
                                    IconButton(onClick = { 
                                        urlText = ""
                                        selectedImportType = null
                                        showUrlField = false
                                        errorMessage = null
                                    }) {
                                        Icon(
                                            Icons.Default.Close, 
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            supportingText = {
                                when {
                                    detectedUrlType == UrlChecker.UrlType.SPOTIFY_PLAYLIST -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Spotify Playlist detected",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    detectedUrlType == UrlChecker.UrlType.SPOTIFY_ALBUM -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Spotify Album detected",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    detectedUrlType == UrlChecker.UrlType.SPOTIFY_TRACK -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Spotify Track detected",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    detectedUrlType == UrlChecker.UrlType.YOUTUBE_PLAYLIST -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "YouTube Playlist detected",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    detectedUrlType == UrlChecker.UrlType.YOUTUBE_MUSIC_PLAYLIST -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "YouTube Music Playlist detected",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    urlText.isNotEmpty() && detectedUrlType == UrlChecker.UrlType.OTHER -> {
                                        Text(
                                            text = "⚠ URL format not recognized",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    else -> null
                                }
                            }
                        )
                    }
                
                // Error message with proper spacing
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                
                // Import Button (shown when URL is pasted) - Show only ONE button based on detected URL type
                AnimatedVisibility(
                    visible = showUrlField && urlText.isNotEmpty() && !isLoading,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Spacing after URL input field (20dp minimum)
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Show only ONE import button based on detected URL type
                        when (detectedUrlType ?: UrlChecker.getUrlType(urlText)) {
                            UrlChecker.UrlType.SPOTIFY_TRACK -> {
                                ImportButton(
                                    label = "Import Track from Spotify",
                                    icon = Icons.Default.MusicNote,
                                    onClick = {
                                        importJob = scope.launch(Dispatchers.IO) {
                                            try {
                                                isLoading = true
                                                errorMessage = null
                                                importState = null
                                                val metadata = ExternalMediaImporter.importSpotifyTrack(urlText)
                                                withContext(Dispatchers.Main) {
                                                    if (metadata != null) {
                                                        Toast.makeText(context, "Track imported successfully!", Toast.LENGTH_SHORT).show()
                                                        onImportComplete(listOf(metadata))
                                                        isLoading = false
                                                        onDismiss()
                                                    } else {
                                                        errorMessage = "Failed to import track"
                                                        isLoading = false
                                                    }
                                                }
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = false
                                                    Toast.makeText(context, "Import cancelled", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    errorMessage = "Error: ${e.message}"
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading && urlText.isNotEmpty()
                                )
                            }
                            UrlChecker.UrlType.SPOTIFY_ALBUM -> {
                                ImportButton(
                                    label = "Import Album from Spotify",
                                    icon = Icons.Default.Album,
                                    onClick = {
                                        importJob = scope.launch(Dispatchers.IO) {
                                            try {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = true
                                                    errorMessage = null
                                                    importState = null
                                                }
                                                ExternalMediaImporter.importSpotifyAlbum(urlText, database)
                                                    .collect { state ->
                                                        withContext(Dispatchers.Main) {
                                                            importState = state
                                                            importedSongs = state.importedSongs
                                                        }
                                                        if (state.isDone && !state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(context, "Album imported: ${state.importedSongs.size} songs", Toast.LENGTH_LONG).show()
                                                                onImportComplete(state.importedSongs)
                                                                isLoading = false
                                                                onDismiss()
                                                            }
                                                        } else if (state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                errorMessage = state.message
                                                                isLoading = false
                                                            }
                                                        }
                                                    }
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = false
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    errorMessage = "Error: ${e.message}"
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading && urlText.isNotEmpty()
                                )
                            }
                            UrlChecker.UrlType.SPOTIFY_PLAYLIST -> {
                                ImportButton(
                                    label = "Import Playlist from Spotify",
                                    icon = Icons.Default.PlaylistPlay,
                                    onClick = {
                                        importJob = scope.launch(Dispatchers.IO) {
                                            try {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = true
                                                    errorMessage = null
                                                    importState = null
                                                }
                                                ExternalMediaImporter.importSpotifyPlaylist(urlText, database)
                                                    .collect { state ->
                                                        withContext(Dispatchers.Main) {
                                                            importState = state
                                                            importedSongs = state.importedSongs
                                                        }
                                                        if (state.isDone && !state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(context, "Playlist imported: ${state.importedSongs.size} songs", Toast.LENGTH_LONG).show()
                                                                onImportComplete(state.importedSongs)
                                                                isLoading = false
                                                                onDismiss()
                                                            }
                                                        } else if (state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                errorMessage = state.message
                                                                isLoading = false
                                                            }
                                                        }
                                                    }
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = false
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    errorMessage = "Error: ${e.message}"
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading && urlText.isNotEmpty()
                                )
                            }
                            UrlChecker.UrlType.YOUTUBE_PLAYLIST -> {
                                ImportButton(
                                    label = "Import Playlist from YouTube",
                                    icon = Icons.Default.VideoLibrary,
                                    onClick = {
                                        importJob = scope.launch(Dispatchers.IO) {
                                            try {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = true
                                                    errorMessage = null
                                                    importState = null
                                                }
                                                ExternalMediaImporter.importYouTubePlaylist(urlText, database)
                                                    .collect { state ->
                                                        withContext(Dispatchers.Main) {
                                                            importState = state
                                                            importedSongs = state.importedSongs
                                                        }
                                                        if (state.isDone && !state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(context, "Playlist imported: ${state.importedSongs.size} songs", Toast.LENGTH_LONG).show()
                                                                onImportComplete(state.importedSongs)
                                                                isLoading = false
                                                                onDismiss()
                                                            }
                                                        } else if (state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                errorMessage = state.message
                                                                isLoading = false
                                                            }
                                                        }
                                                    }
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = false
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    errorMessage = "Error: ${e.message}"
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading && urlText.isNotEmpty()
                                )
                            }
                            UrlChecker.UrlType.YOUTUBE_MUSIC_PLAYLIST -> {
                                ImportButton(
                                    label = "Import Playlist from YouTube Music",
                                    icon = Icons.Default.MusicVideo,
                                    onClick = {
                                        importJob = scope.launch(Dispatchers.IO) {
                                            try {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = true
                                                    errorMessage = null
                                                    importState = null
                                                }
                                                // YouTube Music playlists are handled same as YouTube playlists for now
                                                // but we pass a flag or just use the same importer
                                                ExternalMediaImporter.importYouTubePlaylist(urlText, database)
                                                    .collect { state ->
                                                        withContext(Dispatchers.Main) {
                                                            importState = state
                                                            importedSongs = state.importedSongs
                                                        }
                                                        if (state.isDone && !state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(context, "Playlist imported: ${state.importedSongs.size} songs", Toast.LENGTH_LONG).show()
                                                                onImportComplete(state.importedSongs)
                                                                isLoading = false
                                                                onDismiss()
                                                            }
                                                        } else if (state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                errorMessage = state.message
                                                                isLoading = false
                                                            }
                                                        }
                                                    }
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = false
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    errorMessage = "Error: ${e.message}"
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading && urlText.isNotEmpty()
                                )
                            }
                            else -> {
                                // Default/Fallback import button if type not strictly identified but URL exists
                                ImportButton(
                                    label = "Import from URL",
                                    icon = Icons.Default.Link,
                                    onClick = {
                                        // Generic import logic or show error
                                        errorMessage = "Please use a valid Spotify or YouTube URL"
                                    },
                                    enabled = !isLoading && urlText.isNotEmpty()
                                )
                            }
                        }
                    }
                }
            }
        }
        })
    } else {
    Dialog(
        onDismissRequest = {
            if (!isLoading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import Playlist",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Import from Spotify, YouTube & YouTube Music",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { if (!isLoading) onDismiss() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Content area - show either normal content OR progress, not both
                if (isLoading) {
                    // Progress indicator - full height when loading
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(56.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            LinearProgressIndicator(
                                progress = if (importState?.totalItems ?: 0 > 0) {
                                    (importState?.importedItems ?: 0).toFloat() / (importState?.totalItems ?: 1).toFloat()
                                } else 0f,
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = importState?.message ?: "Importing...",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (importState?.totalItems ?: 0 > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${importState?.importedItems ?: 0} / ${importState?.totalItems ?: 0} items",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Scrollable content - only show when NOT loading
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Step 1: Import Type Selection (if URL field not shown yet)
                        AnimatedVisibility(
                            visible = !showUrlField && selectedImportType == null && urlText.isEmpty(),
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Select Source",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Choose a platform to import from",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ImportTypeButton(
                                label = "Spotify",
                                icon = Icons.Default.MusicNote,
                                onClick = {
                                    selectedImportType = "Spotify"
                                    showUrlField = true
                                },
                                enabled = !isLoading
                            )
                            ImportTypeButton(
                                label = "YouTube",
                                icon = Icons.Default.VideoLibrary,
                                onClick = {
                                    selectedImportType = "YouTube"
                                    showUrlField = true
                                },
                                enabled = !isLoading
                            )
                            ImportTypeButton(
                                label = "YouTube Music",
                                icon = Icons.Default.MusicVideo,
                                onClick = {
                                    selectedImportType = "YouTube Music"
                                    showUrlField = true
                                },
                                enabled = !isLoading
                            )
                        }
                    }
                }
                
                AnimatedVisibility(
                    visible = !showUrlField && selectedImportType == null && urlText.isEmpty(),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                            Text(
                                text = "OR",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Paste URL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Auto-detect source from URL",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
                
                // URL Input Field (shown when import type selected OR always available)
                AnimatedVisibility(
                    visible = showUrlField || urlText.isNotEmpty() || selectedImportType != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { 
                            urlText = it
                            errorMessage = null
                            if (it.isNotEmpty()) {
                                showUrlField = true
                            }
                        },
                        label = { 
                            Text(
                                when {
                                    selectedImportType?.contains("Spotify") == true -> "Spotify URL"
                                    selectedImportType?.contains("YouTube Music") == true -> "YouTube Music URL"
                                    selectedImportType?.contains("YouTube") == true -> "YouTube URL"
                                    detectedUrlType?.name?.contains("SPOTIFY") == true -> "Spotify URL detected"
                                    detectedUrlType?.name?.contains("YOUTUBE") == true -> "YouTube URL detected"
                                    else -> "Paste URL here"
                                },
                                fontWeight = FontWeight.Medium
                            )
                        },
                        placeholder = { 
                            Text(
                                when {
                                    selectedImportType?.contains("Spotify") == true -> "https://open.spotify.com/playlist/..."
                                    selectedImportType?.contains("YouTube Music") == true -> "https://music.youtube.com/playlist?list=..."
                                    selectedImportType?.contains("YouTube") == true -> "https://youtube.com/playlist?list=..."
                                    else -> "https://open.spotify.com/... or https://youtube.com/..."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    // Keyboard dismiss
                                }
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Link, 
                                    contentDescription = "URL",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (urlText.isNotEmpty()) {
                                    IconButton(onClick = { 
                                        urlText = ""
                                        selectedImportType = null
                                        showUrlField = false
                                        errorMessage = null
                                    }) {
                                        Icon(
                                            Icons.Default.Close, 
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            supportingText = {
                                when {
                                    detectedUrlType == UrlChecker.UrlType.SPOTIFY_PLAYLIST -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Spotify Playlist detected",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    detectedUrlType == UrlChecker.UrlType.SPOTIFY_ALBUM -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Spotify Album detected",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    detectedUrlType == UrlChecker.UrlType.SPOTIFY_TRACK -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Spotify Track detected",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    detectedUrlType == UrlChecker.UrlType.YOUTUBE_PLAYLIST -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "YouTube Playlist detected",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    detectedUrlType == UrlChecker.UrlType.YOUTUBE_MUSIC_PLAYLIST -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "YouTube Music Playlist detected",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    urlText.isNotEmpty() && detectedUrlType == UrlChecker.UrlType.OTHER -> {
                                        Text(
                                            text = "⚠ URL format not recognized",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    else -> null
                                }
                            }
                        )
                    }
                
                // Error message with proper spacing
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                
                // Import Button (shown when URL is pasted) - Show only ONE button based on detected URL type
                AnimatedVisibility(
                    visible = showUrlField && urlText.isNotEmpty() && !isLoading,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Spacing after URL input field (20dp minimum)
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Show only ONE import button based on detected URL type
                        when (detectedUrlType ?: UrlChecker.getUrlType(urlText)) {
                            UrlChecker.UrlType.SPOTIFY_TRACK -> {
                                ImportButton(
                                    label = "Import Track from Spotify",
                                    icon = Icons.Default.MusicNote,
                                    onClick = {
                                        importJob = scope.launch(Dispatchers.IO) {
                                            try {
                                                isLoading = true
                                                errorMessage = null
                                                importState = null
                                                val metadata = ExternalMediaImporter.importSpotifyTrack(urlText)
                                                withContext(Dispatchers.Main) {
                                                    if (metadata != null) {
                                                        Toast.makeText(context, "Track imported successfully!", Toast.LENGTH_SHORT).show()
                                                        onImportComplete(listOf(metadata))
                                                        isLoading = false
                                                        onDismiss()
                                                    } else {
                                                        errorMessage = "Failed to import track"
                                                        isLoading = false
                                                    }
                                                }
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = false
                                                    Toast.makeText(context, "Import cancelled", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    errorMessage = "Error: ${e.message}"
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading && urlText.isNotEmpty()
                                )
                            }
                            UrlChecker.UrlType.SPOTIFY_ALBUM -> {
                                ImportButton(
                                    label = "Import Album from Spotify",
                                    icon = Icons.Default.Album,
                                    onClick = {
                                        importJob = scope.launch(Dispatchers.IO) {
                                            try {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = true
                                                    errorMessage = null
                                                    importState = null
                                                }
                                                ExternalMediaImporter.importSpotifyAlbum(urlText, database)
                                                    .collect { state ->
                                                        withContext(Dispatchers.Main) {
                                                            importState = state
                                                            importedSongs = state.importedSongs
                                                        }
                                                        if (state.isDone && !state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(context, "Album imported: ${state.importedSongs.size} songs", Toast.LENGTH_LONG).show()
                                                                onImportComplete(state.importedSongs)
                                                                isLoading = false
                                                                onDismiss()
                                                            }
                                                        } else if (state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                errorMessage = state.message
                                                                isLoading = false
                                                            }
                                                        }
                                                    }
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = false
                                                    Toast.makeText(context, "Import cancelled", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    errorMessage = "Error: ${e.message}"
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading && urlText.isNotEmpty()
                                )
                            }
                            UrlChecker.UrlType.SPOTIFY_PLAYLIST -> {
                                ImportButton(
                                    label = "Import Playlist from Spotify",
                                    icon = Icons.Default.PlaylistPlay,
                                    onClick = {
                                        importJob = scope.launch(Dispatchers.IO) {
                                            try {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = true
                                                    errorMessage = null
                                                    importState = null
                                                }
                                                ExternalMediaImporter.importSpotifyPlaylist(urlText, database, null, playlistRepository)
                                                    .collect { state ->
                                                        withContext(Dispatchers.Main) {
                                                            importState = state
                                                            importedSongs = state.importedSongs
                                                        }
                                                        if (state.isDone && !state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(context, "Playlist imported: ${state.importedSongs.size} songs", Toast.LENGTH_LONG).show()
                                                                onImportComplete(state.importedSongs)
                                                                isLoading = false
                                                                onDismiss()
                                                            }
                                                        } else if (state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                errorMessage = state.message
                                                                isLoading = false
                                                            }
                                                        }
                                                    }
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = false
                                                    Toast.makeText(context, "Import cancelled", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    errorMessage = "Error: ${e.message}"
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading && urlText.isNotEmpty()
                                )
                            }
                            UrlChecker.UrlType.YOUTUBE_PLAYLIST -> {
                                ImportButton(
                                    label = "Import Playlist from YouTube",
                                    icon = Icons.Default.VideoLibrary,
                                    onClick = {
                                        importJob = scope.launch(Dispatchers.IO) {
                                            try {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = true
                                                    errorMessage = null
                                                    importState = null
                                                }
                                                ExternalMediaImporter.importYouTubePlaylist(urlText, database, playlistRepository)
                                                    .collect { state ->
                                                        withContext(Dispatchers.Main) {
                                                            importState = state
                                                            importedSongs = state.importedSongs
                                                        }
                                                        if (state.isDone && !state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(context, "Playlist imported: ${state.importedSongs.size} songs", Toast.LENGTH_LONG).show()
                                                                onImportComplete(state.importedSongs)
                                                                isLoading = false
                                                                onDismiss()
                                                            }
                                                        } else if (state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                errorMessage = state.message
                                                                isLoading = false
                                                            }
                                                        }
                                                    }
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = false
                                                    Toast.makeText(context, "Import cancelled", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    errorMessage = "Error: ${e.message}"
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading && urlText.isNotEmpty()
                                )
                            }
                            UrlChecker.UrlType.YOUTUBE_MUSIC_PLAYLIST -> {
                                ImportButton(
                                    label = "Import Playlist from YouTube Music",
                                    icon = Icons.Default.MusicVideo,
                                    onClick = {
                                        importJob = scope.launch(Dispatchers.IO) {
                                            try {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = true
                                                    errorMessage = null
                                                    importState = null
                                                }
                                                ExternalMediaImporter.importYouTubeMusicPlaylist(urlText, database, playlistRepository)
                                                    .collect { state ->
                                                        withContext(Dispatchers.Main) {
                                                            importState = state
                                                            importedSongs = state.importedSongs
                                                        }
                                                        if (state.isDone && !state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(context, "Playlist imported: ${state.importedSongs.size} songs", Toast.LENGTH_LONG).show()
                                                                onImportComplete(state.importedSongs)
                                                                isLoading = false
                                                                onDismiss()
                                                            }
                                                        } else if (state.isFailed) {
                                                            withContext(Dispatchers.Main) {
                                                                errorMessage = state.message
                                                                isLoading = false
                                                            }
                                                        }
                                                    }
                                            } catch (e: kotlinx.coroutines.CancellationException) {
                                                withContext(Dispatchers.Main) {
                                                    isLoading = false
                                                    Toast.makeText(context, "Import cancelled", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    errorMessage = "Error: ${e.message}"
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading && urlText.isNotEmpty()
                                )
                            }
                            else -> {
                                // Unknown URL type - show error
                                Text(
                                    text = "Invalid or unsupported URL type",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        }
                    }
                }
                }
                
                // Fixed bottom buttons area - always visible
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Cancel buttons - Show "Cancel Import" when loading, "Cancel/Close" always
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cancel Import button (only when loading) - FIXED: Show when isLoading is true
                    if (isLoading) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    importJob?.cancel()
                                    importJob = null
                                    isLoading = false
                                    importState = null
                                    errorMessage = null
                                    Toast.makeText(context, "Import cancelled", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Log.e("ImportDialog", "Error cancelling import", e)
                                    isLoading = false
                                    importState = null
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(
                                "Cancel Import",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    // Cancel/Close button (always visible)
                    TextButton(
                        onClick = { 
                            // Allow dismissing dialog even during import (background import)
                            if (isLoading) {
                                // Don't cancel, just close - allow background import
                                onDismiss()
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            if (isLoading) "Close" else "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    // Cleanup on dismiss - cancel import job if dialog is dismissed during import
    DisposableEffect(Unit) {
        onDispose {
            // Don't cancel on dispose - allow background import to continue
        }
    }
}}
}