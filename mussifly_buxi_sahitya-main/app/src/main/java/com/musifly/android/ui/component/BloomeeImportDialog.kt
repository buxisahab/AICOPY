package com.musifly.android.ui.component

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.musifly.android.LocalDatabase
import com.musifly.android.R
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.ui.component.liquidglass.LiquidGlassButton
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.PlaylistImportExportService
import kotlinx.coroutines.launch

/**
 * mussifly Import Dialog
 * Allows users to import playlists from mussifly JSON files
 */
@Composable
fun BloomeeImportDialog(
    onDismiss: () -> Unit,
    onImportComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    
    var importState by remember { mutableStateOf(ImportState.INPUT) }
    var currentProgress by remember { mutableIntStateOf(0) }
    var totalTracks by remember { mutableIntStateOf(0) }
    var currentMessage by remember { mutableStateOf("") }
    var progressPercent by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf("") }
    var importedCount by remember { mutableIntStateOf(0) }
    var failedCount by remember { mutableIntStateOf(0) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            importState = ImportState.IMPORTING
            
            scope.launch {
                PlaylistImportExportService.importPlaylist(
                    context = context,
                    database = database,
                    uri = uri
                ).collect { state ->
                    currentProgress = state.importedItems
                    totalTracks = state.totalItems
                    currentMessage = state.message
                    importedCount = state.importedItems
                    failedCount = state.failedItems
                    progressPercent = if (state.totalItems > 0) {
                        state.importedItems.toFloat() / state.totalItems
                    } else 0f
                    
                    if (state.isDone) {
                        importState = ImportState.COMPLETE
                    } else if (state.isFailed) {
                        errorMessage = state.message
                        importState = ImportState.ERROR
                    }
                }
            }
        }
    }

    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS
    
    val dialogTitle: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.playlist_import),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = when (importState) {
                    ImportState.INPUT -> "Import mussifly Playlist"
                    ImportState.IMPORTING -> "Importing Playlist"
                    ImportState.COMPLETE -> "Import Complete"
                    ImportState.ERROR -> "Import Failed"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }

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
                        text = "Select a mussifly playlist JSON file to import.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // Info box
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
                                text = "Supported format:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "• mussifly JSON playlist files (*_mussiflyPlaylist.json)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Info text
                    Text(
                        text = "⚠️ Songs will be imported based on the metadata in the file. YouTube video IDs will be extracted from URLs.",
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
                                "Importing $currentProgress of $totalTracks" 
                            else 
                                "Reading file...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (currentMessage.isNotBlank()) {
                            Text(
                                text = currentMessage,
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
                        
                        if (failedCount > 0) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "$failedCount songs could not be imported",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
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
                            text = "Please check the file format and try again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    val confirmButtonContent: @Composable () -> Unit = {
        when (importState) {
            ImportState.INPUT -> {
                if (isLiquidGlass) {
                    LiquidGlassButton(
                        onClick = { filePickerLauncher.launch("application/json") },
                        text = "Select File",
                        isPrimary = true
                    )
                } else {
                    Button(
                        onClick = {
                            filePickerLauncher.launch("application/json")
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.playlist_import),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Select File")
                    }
                }
            }
            ImportState.IMPORTING -> { /* No button */ }
            ImportState.COMPLETE -> {
                if (isLiquidGlass) {
                    LiquidGlassButton(
                        onClick = {
                            onImportComplete()
                            onDismiss()
                        },
                        text = "Done",
                        isPrimary = true
                    )
                } else {
                    Button(
                        onClick = {
                            onImportComplete()
                            onDismiss()
                        }
                    ) {
                        Text("Done")
                    }
                }
            }
            ImportState.ERROR -> {
                if (isLiquidGlass) {
                    LiquidGlassButton(
                        onClick = {
                            importState = ImportState.INPUT
                            errorMessage = ""
                            selectedFileUri = null
                        },
                        text = "Try Again",
                        isPrimary = true
                    )
                } else {
                    Button(
                        onClick = {
                            importState = ImportState.INPUT
                            errorMessage = ""
                            selectedFileUri = null
                        }
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }

    val dismissButtonContent: @Composable () -> Unit = {
        if (importState == ImportState.INPUT || importState == ImportState.ERROR) {
            if (isLiquidGlass) {
                LiquidGlassButton(
                    onClick = onDismiss,
                    text = "Cancel"
                )
            } else {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
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
            title = dialogTitle,
            content = { dialogContent() },
            buttons = {
                dismissButtonContent()
                confirmButtonContent()
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = { 
                if (importState != ImportState.IMPORTING) {
                    onDismiss()
                }
            },
            title = dialogTitle,
            text = dialogContent,
            confirmButton = confirmButtonContent,
            dismissButton = dismissButtonContent
        )
    }
}
