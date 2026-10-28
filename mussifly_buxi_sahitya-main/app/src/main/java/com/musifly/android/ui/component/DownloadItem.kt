package com.musifly.android.ui.component

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musifly.android.download.DownloadMission
import com.musifly.android.download.FinishedMission
import com.musifly.android.download.Mission
import com.musifly.android.download.StoredFileHelper
import java.io.File

/**
 * Download item UI component with REAL-TIME progress updates
 */
@Composable
fun DownloadItem(
    mission: Mission,
    priority: Int = 0,
    retryCount: Int = 0,
    networkType: String? = null,
    peakSpeed: Long = 0,
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onDelete: () -> Unit = {},
    onRetry: () -> Unit = {},
    onSetPriority: (Int) -> Unit = {},
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {},
    onRename: () -> Unit = {},
    onShare: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val maxRetries = 3
    val downloadMission = mission as? DownloadMission

    // Reactive state for instant UI updates
    val isCurrentlyRunning = remember(downloadMission?.running, downloadMission?.isPaused, mission.finished) {
        downloadMission?.running == true && downloadMission.isPaused == false && !mission.finished
    }
    
    val isCurrentlyPaused = remember(downloadMission?.isPaused, downloadMission?.running, mission.finished) {
        downloadMission?.isPaused == true && downloadMission.running == false && !mission.finished
    }
    
    val canResume = remember(mission.finished, downloadMission?.running, downloadMission?.isPaused) {
        !mission.finished && downloadMission?.running == false && downloadMission?.isPaused == true
    }

    // Real-time progress state
    var progress by remember { mutableStateOf(mission.getProgress()) }
    var isRunning by remember { mutableStateOf(mission.running) }
    var isPausedState by remember { mutableStateOf(downloadMission?.isPaused ?: false) }
    var doneBytes by remember { mutableStateOf(mission.done) }
    var totalBytes by remember { mutableStateOf(mission.length) }
    var timestamp by remember { mutableStateOf(mission.timestamp) }
    var speed by remember { mutableStateOf<String?>(null) }
    var eta by remember { mutableStateOf<String?>(null) }
    
    // ✅ BUG FIX: LISTEN FOR STATE CHANGES (PAUSE/RESUME EVENTS)
    LaunchedEffect(mission.timestamp) {
        com.musifly.android.download.DownloadEventBus.stateChangeEvents.collect { event ->
            if (event.missionId == mission.timestamp) {
                // Update UI state INSTANTLY when pause/resume happens
                when (event.newState) {
                    com.musifly.android.download.DownloadEventBus.DownloadState.DOWNLOADING -> {
                        isRunning = true
                        isPausedState = false
                    }
                    com.musifly.android.download.DownloadEventBus.DownloadState.PAUSED -> {
                        isRunning = false
                        isPausedState = true
                    }
                    com.musifly.android.download.DownloadEventBus.DownloadState.FINISHED -> {
                        isRunning = false
                        isPausedState = false
                    }
                    com.musifly.android.download.DownloadEventBus.DownloadState.ERROR -> {
                        isRunning = false
                        isPausedState = false
                    }
                    else -> {}
                }
            }
        }
    }

    // ✅ REAL-TIME PROGRESS EVENT LISTENER
    LaunchedEffect(mission.timestamp) {
        com.musifly.android.download.DownloadEventBus.progressEvents.collect { event ->
            if (event.missionId == mission.timestamp) {
                // Update state INSTANTLY
                doneBytes = event.downloadedBytes
                totalBytes = event.totalBytes
                progress = event.progress
                
                // Calculate speed and ETA
                val elapsedSeconds = (System.currentTimeMillis() - timestamp) / 1000
                if (elapsedSeconds > 0) {
                    val bytesPerSecond = doneBytes / elapsedSeconds
                    speed = formatBytes(bytesPerSecond)
                    
                    if (totalBytes > doneBytes && bytesPerSecond > 0) {
                        val remainingBytes = totalBytes - doneBytes
                        val remainingSeconds = remainingBytes / bytesPerSecond
                        eta = formatTime(remainingSeconds)
                    } else {
                        eta = null
                    }
                } else {
                    speed = "calculating..."
                    eta = null
                }
            }
        }
    }

    // Initial state from mission object
    LaunchedEffect(mission) {
        progress = mission.getProgress()
        isRunning = mission.running
        isPausedState = downloadMission?.isPaused ?: false
        doneBytes = mission.done
        totalBytes = mission.length
        timestamp = mission.timestamp
    }

    // Thumbnail handling
    val thumbnailUrl = remember(mission) {
        when {
            mission is DownloadMission && mission.thumbnailUrl != null -> mission.thumbnailUrl
            mission is FinishedMission && mission.thumbnailUrl != null -> mission.thumbnailUrl
            mission is FinishedMission && isVideoFile(mission.storage) -> null
            else -> null
        }
    }
    
    var extractedThumbnail by remember { mutableStateOf<android.net.Uri?>(null) }
    LaunchedEffect(mission) {
        if (mission is FinishedMission && thumbnailUrl == null && isVideoFile(mission.storage)) {
            val videoUri = mission.storage?.getUri()?.takeIf { it != android.net.Uri.EMPTY }
            if (videoUri != null) {
                extractedThumbnail = extractVideoThumbnail(context, videoUri)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(
                if (isSelectionMode) {
                    Modifier
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .combinedClickable(
                            onClick = onClick,
                            onLongClick = onLongPress
                        )
                } else {
                    Modifier.combinedClickable(
                        onClick = { /* Handled by buttons */ },
                        onLongClick = onLongPress
                    )
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.size(24.dp)
                )
            }

            // Thumbnail
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                when {
                    thumbnailUrl != null -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(thumbnailUrl)
                                .crossfade(300)
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    extractedThumbnail != null -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(extractedThumbnail)
                                .crossfade(300)
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    mission is DownloadMission -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                    else -> {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = when {
                        mission is DownloadMission && mission.title != null -> mission.title!!
                        mission is FinishedMission && mission.title != null -> mission.title!!
                        else -> mission.storage?.getName() ?: "Unknown"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Artist
                if ((mission is DownloadMission && mission.artist != null) || 
                    (mission is FinishedMission && mission.artist != null)) {
                    Text(
                        text = (mission as? DownloadMission)?.artist ?: 
                               (mission as? FinishedMission)?.artist ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (mission) {
                    is DownloadMission -> {
                        // Progress bar - UPDATES IN REAL-TIME
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when {
                                        isRunning -> "Downloading..."
                                        mission.isFinished() -> "Complete"
                                        mission.isCorrupt() -> "Failed"
                                        else -> "Paused"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        isRunning -> MaterialTheme.colorScheme.primary
                                        mission.isCorrupt() -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )

                                // Size - UPDATES IN REAL-TIME
                                Text(
                                    text = "${formatBytes(doneBytes)} / ${formatBytes(totalBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Speed and ETA - UPDATES IN REAL-TIME
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (isRunning && speed != null) {
                                        Text(
                                            text = "⚡ $speed/s",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    if (isRunning && eta != null) {
                                        Text(
                                            text = "⏱️ $eta",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    
                                    if (totalBytes > 0) {
                                        Text(
                                            text = "${progress.toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Action buttons - REACTIVE STATE (BUG FIX: Use local state)
                            Row {
                                if (isRunning) {
                                    IconButton(onClick = onPause) {
                                        Icon(Icons.Default.Pause, "Pause")
                                    }
                                } else if (mission.isCorrupt()) {
                                    IconButton(
                                        onClick = onRetry,
                                        enabled = retryCount < maxRetries
                                    ) {
                                        Icon(Icons.Default.Refresh, "Retry")
                                    }
                                } else if (!mission.isFinished()) {
                                    IconButton(onClick = onResume) {
                                        Icon(Icons.Default.PlayArrow, "Resume")
                                    }
                                }
                                
                                IconButton(onClick = onDelete) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        "Cancel",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    is FinishedMission -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Complete",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(
                                    text = formatBytes(mission.length),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        mission.storage?.let { storage ->
                                            openDownloadedFile(context, storage)
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.PlayCircle,
                                        "Open file",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(onClick = onDelete) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openDownloadedFile(context: Context, storage: StoredFileHelper) {
    try {
        val uri = storage.getUri()
        if (uri == android.net.Uri.EMPTY) {
            Toast.makeText(context, "File URI not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (storage.isDirect() && !storage.existsAsFile()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_LONG).show()
            return
        }

        val mimeType = getMimeTypeFromExtension(storage.getType())
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            try {
                context.startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(context, "No app found to open this file", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun getMimeTypeFromExtension(extension: String): String {
    return when (extension.lowercase()) {
        "mp4", "mkv", "webm", "avi", "mov", "wmv", "flv" -> "video/*"
        "mp3", "m4a", "opus", "ogg", "wav", "flac", "aac" -> "audio/*"
        "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "image/*"  
        "pdf" -> "application/pdf"
        "txt" -> "text/plain"
        "zip", "rar", "7z" -> "application/zip"
        else -> "*/*"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}

private fun formatTime(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}

private fun extractVideoThumbnail(context: Context, videoUri: android.net.Uri): android.net.Uri? {
    return try {
        val retriever = android.media.MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val bitmap = retriever.getFrameAtTime(1000000)
        retriever.release()

        if (bitmap != null) {
            val cacheDir = context.cacheDir
            val thumbnailFile = File(cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
            thumbnailFile.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                thumbnailFile
            )
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

private fun isVideoFile(storage: StoredFileHelper?): Boolean {
    val fileName = storage?.getName()?.lowercase() ?: return false
    return fileName.endsWith(".mp4") || fileName.endsWith(".mkv") ||
            fileName.endsWith(".webm") || fileName.endsWith(".avi") ||
            fileName.endsWith(".mov") || fileName.endsWith(".wmv")
}

private fun isAudioFile(storage: StoredFileHelper?): Boolean {
    val fileName = storage?.getName()?.lowercase() ?: return false
    return fileName.endsWith(".mp3") || fileName.endsWith(".m4a") ||
            fileName.endsWith(".opus") || fileName.endsWith(".ogg") ||
            fileName.endsWith(".wav") || fileName.endsWith(".flac") ||
            fileName.endsWith(".aac")
}