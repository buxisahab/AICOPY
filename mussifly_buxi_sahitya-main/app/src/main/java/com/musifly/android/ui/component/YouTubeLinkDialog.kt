package com.musifly.android.ui.component

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musifly.android.utils.NewPipeVideoDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import timber.log.Timber

import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey

/**
 * Dialog shown when user shares a YouTube link to the app
 * Offers to download video or audio with quality selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeLinkDialog(
    youtubeUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }
    var videoTitle by remember { mutableStateOf("Loading...") }
    var selectedQuality by remember { mutableStateOf("720p") }
    var downloadType by remember { mutableStateOf("video") } // "video" or "audio"
    
    val qualityOptions = listOf("144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p")
    
    // Load video info
    LaunchedEffect(youtubeUrl) {
        loadingMessage = "Fetching video info..."
        try {
            withContext(Dispatchers.IO) {
                val streamInfo = StreamInfo.getInfo(
                    ServiceList.YouTube,
                    youtubeUrl
                )
                videoTitle = streamInfo.name ?: "Unknown Video"
                Timber.tag("YouTubeLinkDialog").d("Loaded video: $videoTitle")
            }
        } catch (e: Exception) {
            Timber.tag("YouTubeLinkDialog").e(e, "Failed to load video info")
            videoTitle = "Video"
        }
    }
    
    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    val dialogContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Video title
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = videoTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            // Download type selector
            Text(
                text = "Download Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = downloadType == "video",
                    onClick = { downloadType = "video" },
                    label = { Text("Video (MP4)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.VideoFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                
                FilterChip(
                    selected = downloadType == "audio",
                    onClick = { downloadType = "audio" },
                    label = { Text("Audio (M4A)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.AudioFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Quality selector (only for video)
            if (downloadType == "video") {
                Text(
                    text = "Video Quality",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    qualityOptions.chunked(4).forEach { row ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { quality ->
                                FilterChip(
                                    selected = selectedQuality == quality,
                                    onClick = { selectedQuality = quality },
                                    label = { Text(quality, fontSize = 12.sp) },
                                    modifier = Modifier.width(70.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Loading indicator
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Text(
                        text = loadingMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    val confirmButtonContent: @Composable () -> Unit = {
        Button(
            onClick = {
                if (isLoading) return@Button
                
                scope.launch {
                    isLoading = true
                    
                    try {
                        if (downloadType == "video") {
                            loadingMessage = "Preparing video download..."
                            
                            // Use NewPipe video downloader
                            val quality = NewPipeVideoDownloader.VideoQuality.fromString(selectedQuality)
                            val result = NewPipeVideoDownloader.downloadVideoWithNewPipe(
                                context = context,
                                videoUrl = youtubeUrl,
                                title = videoTitle,
                                quality = quality
                            )
                            
                            result.onSuccess {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "✅ Video download started!\nQuality: $selectedQuality",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                onDismiss()
                            }.onFailure { error ->
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "❌ Download failed: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                isLoading = false
                            }
                        } else {
                            loadingMessage = "Preparing audio download..."
                            
                            // Use NewPipe audio downloader
                            val result = NewPipeVideoDownloader.downloadAudio(
                                context = context,
                                videoUrl = youtubeUrl,
                                title = videoTitle,
                                onDownloadId = {}
                            )
                            
                            result.onSuccess {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "✅ Audio download started!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                onDismiss()
                            }.onFailure { error ->
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "❌ Download failed: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                isLoading = false
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag("YouTubeLinkDialog").e(e, "Download failed")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "❌ Error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download")
        }
    }

    val dismissButtonContent: @Composable () -> Unit = {
        TextButton(
            onClick = onDismiss,
            enabled = !isLoading
        ) {
            Text("Cancel")
        }
    }

    if (isLiquidGlass) {
        LiquidGlassDialog(
            onDismiss = { if (!isLoading) onDismiss() },
            icon = {
                 Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                 Text(
                    text = "Download from YouTube",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            content = { dialogContent() },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    dismissButtonContent()
                    Spacer(modifier = Modifier.width(8.dp))
                    confirmButtonContent()
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = { if (!isLoading) onDismiss() },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Download from YouTube",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = { dialogContent() },
            confirmButton = { confirmButtonContent() },
            dismissButton = { dismissButtonContent() }
        )
    }
}
