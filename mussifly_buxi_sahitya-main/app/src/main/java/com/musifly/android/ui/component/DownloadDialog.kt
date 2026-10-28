package com.musifly.android.ui.component

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.widget.MediaController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.musifly.android.LocalDatabase
import com.musifly.android.constants.AudioQuality
import com.musifly.android.models.MediaMetadata
import com.musifly.android.playback.ExoDownloadService
import com.musifly.android.utils.ExoDownloadHelper
import com.musifly.android.utils.NewPipeVideoDownloader
import com.musifly.android.utils.YTPlayerUtils
import com.musifly.android.streaming.NewPipeStreamFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import com.musifly.android.R
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.utils.rememberEnumPreference

// Helper function to show download notifications with clickable action
private fun showNotification(context: Context, title: String, message: String, progress: Int, downloadId: Long? = null) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    // Create intent to open Download Manager
    val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val pendingIntent = android.app.PendingIntent.getActivity(
        context,
        0,
        intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )
    
    val builder = NotificationCompat.Builder(context, "download_channel")
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(progress < 100)
        .setContentIntent(pendingIntent) // Make notification clickable
        .setAutoCancel(progress >= 100)
    
    if (progress < 100) {
        builder.setProgress(100, progress, false)
    }
    
    notificationManager.notify(downloadId?.toInt() ?: 1, builder.build())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadDialog(
    mediaMetadata: MediaMetadata?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var showVideoQualityOptions by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf("480p") }
    var downloadMessage by remember { mutableStateOf("") }
    var downloadProgress by remember { mutableStateOf(0) }
    var isVideoAvailable by remember { mutableStateOf(true) } // Enabled: Using Tubular
    var currentDownloadJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var useExoDownload by remember { mutableStateOf(true) } // Use System DownloadManager
    var useNewPipeDownloadSystem by remember { mutableStateOf(true) } // Use NewPipe download system with queue
    // Advanced mode removed - using simple NewPipeVideoDownloader only
    var currentDownloadId by remember { mutableStateOf<Long?>(null) }
    var downloadStage by remember { mutableStateOf("") }

    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS
    
    // Video selection dialog state
    var showVideoSelectionDialog by remember { mutableStateOf(false) }
    var videoSearchResults by remember { mutableStateOf<List<NewPipeStreamFinder.VideoSearchResult>>(emptyList()) }
    var selectedQualityForDownload by remember { mutableStateOf("") }
    var isSearchingVideos by remember { mutableStateOf(false) }
    var selectedVideoUrl by remember { mutableStateOf<String?>(null) }
    
    // Dynamic stream options
    var availableStreams by remember { mutableStateOf<List<NewPipeVideoDownloader.StreamOption>?>(null) }
    var isLoadingStreams by remember { mutableStateOf(false) }
    var streamFetchError by remember { mutableStateOf<String?>(null) }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "Unknown size"
        val mb = bytes / (1024.0 * 1024.0)
        if (mb >= 1024) {
             return "%.2f GB".format(mb / 1024.0)
        }
        return "%.1f MB".format(mb)
    }
    
    // Video preview dialog state
    var showVideoPreviewDialog by remember { mutableStateOf(false) }
    var previewVideoUrl by remember { mutableStateOf<String?>(null) }
    var previewVideoTitle by remember { mutableStateOf("") }
    var previewVideoStreamUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingPreview by remember { mutableStateOf(false) }
    
    // Check if video is available
    LaunchedEffect(mediaMetadata?.id) {
        mediaMetadata?.let {
            isVideoAvailable = true
        }
    }
    
    // Create notification channel
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "download_channel",
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Download progress notifications"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    // Video quality options (up to 8K)
    // Fetch streams when quality options are shown
    LaunchedEffect(showVideoQualityOptions, mediaMetadata) {
        if (showVideoQualityOptions && mediaMetadata != null && availableStreams == null) {
            isLoadingStreams = true
            streamFetchError = null
            
            try {
                val videoUrl = "https://www.youtube.com/watch?v=${mediaMetadata.id}"
                val result = NewPipeVideoDownloader.getAvailableStreams(videoUrl)
                
                if (result.isSuccess) {
                    availableStreams = result.getOrThrow()
                } else {
                    streamFetchError = result.exceptionOrNull()?.message ?: "Failed to fetch options"
                }
            } catch (e: Exception) {
                streamFetchError = e.message
            } finally {
                isLoadingStreams = false
            }
        }
    }
    
    fun cancelDownload() {
        currentDownloadJob?.cancel()
        currentDownloadId?.let { id ->
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.remove(id)
        }
        isDownloading = false
        downloadMessage = "Download cancelled"
        Toast.makeText(context, "Download cancelled", Toast.LENGTH_SHORT).show()
    }
    
    fun downloadWithExoManager(isAudio: Boolean, quality: String = "720") {
        if (mediaMetadata == null) return
        
        scope.launch {
            isDownloading = true
            downloadMessage = "Preparing download with DownloadManager..."
            downloadProgress = 0
            
            try {
                // Get stream URL in IO dispatcher to avoid NetworkOnMainThreadException
                val videoId = mediaMetadata.id ?: return@launch
                val streamUrl = withContext(Dispatchers.IO) {
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    
                    Timber.d("Getting stream URL for video ID: $videoId")
                    
                    val result = YTPlayerUtils.playerResponseForPlayback(
                        videoId = videoId,
                        playlistId = null,
                        audioQuality = AudioQuality.HIGH,
                        connectivityManager = connectivityManager
                    )
                    
                    if (result.isFailure) {
                        val error = result.exceptionOrNull()
                        Timber.e(error, "Failed to get player response")
                        throw Exception("Failed to get stream: ${error?.message ?: "Unknown error"}")
                    }
                    
                    val playbackData = result.getOrThrow()
                    playbackData.streamUrl
                }
                
                Timber.d("Got stream URL: ${streamUrl.take(100)}...")
                
                // Validate stream URL
                if (streamUrl.isBlank()) {
                    throw Exception("Stream URL is empty. Video may not be available.")
                }
                
                if (!streamUrl.startsWith("http")) {
                    throw Exception("Invalid stream URL format")
                }
                
                // Use Android DownloadManager
                val title = mediaMetadata.title ?: "Unknown"
                val downloadResult = ExoDownloadHelper.downloadWithManager(
                    context = context,
                    url = streamUrl,
                    title = title,
                    description = if (isAudio) "Audio download" else "Video download ($quality)",
                    isVideo = !isAudio,
                    onDownloadId = { id ->
                        currentDownloadId = id
                    }
                )
                
                downloadResult.onSuccess { downloadId ->
                    // Download started successfully
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "✅ Download started!\n\nTap notification to view progress in Download Manager",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    isDownloading = false
                    onDismiss()
                }.onFailure { error ->
                    Timber.e(error, "Download manager failed")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context, 
                            "Download failed: ${error.message}\n\nTry:\n• Check internet connection\n• Try again in a moment\n• Try a different song", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    isDownloading = false
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Download preparation failed")
                withContext(Dispatchers.Main) {
                    val errorMsg = when {
                        e.message?.contains("stream", ignoreCase = true) == true -> 
                            "Cannot get stream URL\n\nThis video may be:\n• Age-restricted\n• Region-blocked\n• Unavailable\n\nTry a different song"
                        e.message?.contains("network", ignoreCase = true) == true -> 
                            "Network error\n\nCheck your internet connection"
                        else -> 
                            "Download failed: ${e.message}\n\nTry again or select a different song"
                    }
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
                isDownloading = false
            }
        }
    }
    
    // Old downloadAudio() removed - using new implementation below
    
    fun downloadVideo(quality: String) {
        if (mediaMetadata == null) {
            Toast.makeText(context, "No song playing", Toast.LENGTH_SHORT).show()
            return
        }

        // Show video selection dialog first
        selectedQualityForDownload = quality
        showVideoSelectionDialog = true
        
        // Search for videos in background
        scope.launch {
            isSearchingVideos = true
            try {
                val title = mediaMetadata.title ?: "Unknown"
                val artist = mediaMetadata.artists?.firstOrNull()?.name
                
                Timber.tag("DownloadDialog").d("🔍 Searching for videos: $title by $artist")
                
                val searchResult = NewPipeStreamFinder.getAllSearchResults(
                    songTitle = title,
                    artistName = artist
                )
                
                if (searchResult.isSuccess) {
                    videoSearchResults = searchResult.getOrThrow()
                    Timber.tag("DownloadDialog").d("✅ Found ${videoSearchResults.size} videos")
                } else {
                    Timber.tag("DownloadDialog").e(searchResult.exceptionOrNull(), "Failed to search videos")
                    withContext(Dispatchers.Main) {
                        val errorMessage = searchResult.exceptionOrNull()?.message ?: "Unknown error"
                        val displayMessage = when {
                            errorMessage.contains("Android version", ignoreCase = true) || 
                            errorMessage.contains("URLEnc", ignoreCase = true) -> {
                                "❌ Video Search Not Available\n\n" +
                                "Your Android version (${Build.VERSION.RELEASE}) doesn't support video search.\n\n" +
                                "✅ You can still:\n" +
                                "• Download audio files\n" + 
                                "• Use direct video URLs"
                            }
                            else -> "❌ Could not search videos\n\n$errorMessage"
                        }
                        Toast.makeText(context, displayMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Timber.tag("DownloadDialog").e(e, "Video search error")
            } finally {
                isSearchingVideos = false
            }
        }
    }
    
    fun openVideoPreview(video: NewPipeStreamFinder.VideoSearchResult) {
        previewVideoUrl = video.url
        previewVideoTitle = video.title
        previewVideoStreamUrl = null
        isLoadingPreview = true
        showVideoPreviewDialog = true
        
        // Get video stream URL for preview (low quality for fast loading)
        scope.launch {
            try {
                val streamInfo = withContext(Dispatchers.IO) {
                    NewPipeVideoDownloader.extractVideoStreams(
                        videoUrl = video.url,
                        preferredQuality = NewPipeVideoDownloader.VideoQuality.Q_360P // Low quality for fast preview
                    )
                }
                
                if (streamInfo.isSuccess) {
                    val stream = streamInfo.getOrThrow()
                    previewVideoStreamUrl = stream.url
                    Timber.tag("DownloadDialog").d("✅ Preview stream URL obtained")
                } else {
                    Timber.tag("DownloadDialog").e(streamInfo.exceptionOrNull(), "Failed to get preview stream")
                }
            } catch (e: Exception) {
                Timber.tag("DownloadDialog").e(e, "Preview error")
            } finally {
                isLoadingPreview = false
            }
        }
    }
    
    fun startVideoDownload(videoUrl: String, quality: String) {
        if (mediaMetadata == null) return
        
        scope.launch {
            isDownloading = true
            downloadMessage = "Starting video download..."
            downloadProgress = 0
            
            try {
                val title = mediaMetadata.title ?: "Unknown"
                val artist = mediaMetadata.artists?.firstOrNull()?.name
                val thumbnailUrl = mediaMetadata.thumbnailUrl
                
                Timber.tag("DownloadDialog").d("📥 Starting download: $videoUrl")
                
                // Get video info for better metadata
                val videoInfo = withContext(Dispatchers.IO) {
                    NewPipeVideoDownloader.getVideoInfo(videoUrl).getOrNull()
                }
                
                // Use video info thumbnail if available, otherwise use metadata thumbnail
                val finalThumbnail = videoInfo?.thumbnailUrl ?: thumbnailUrl
                val finalArtist = videoInfo?.uploader ?: artist
                
                NewPipeVideoDownloader.downloadVideoWithNewPipe(
                    context = context,
                    videoUrl = videoUrl,
                    title = title,
                    quality = NewPipeVideoDownloader.VideoQuality.fromString(quality),
                    artist = finalArtist,
                    thumbnailUrl = finalThumbnail
                ).onSuccess {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "✅ Video Download Started!\n\n" +
                            "📹 Quality: $quality\n" +
                            "📁 Location: Musifly/video/\n\n" +
                            "Check Downloads screen for progress",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    isDownloading = false
                    showVideoSelectionDialog = false
                    onDismiss()
                }.onFailure { error ->
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "❌ Download failed: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    isDownloading = false
                }
            } catch (e: Exception) {
                Timber.tag("DownloadDialog").e(e, "Download error")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
                isDownloading = false
            }
        }
    }
    
    fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%d:%02d", minutes, secs)
        }
    }
    
    fun formatViewCount(count: Long): String {
        return when {
            count >= 1_000_000_000 -> String.format("%.1fB", count / 1_000_000_000.0)
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }
    }
    
    // ========================================================================
    // ✅ AUDIO DOWNLOAD (Unchanged - works perfectly)
    // Downloads audio-only stream (M4A) - NOT video
    // ========================================================================
    fun downloadAudio() {
        if (mediaMetadata == null) {
            Toast.makeText(context, "No song playing", Toast.LENGTH_SHORT).show()
            return
        }
        
        scope.launch {
            isDownloading = true
            downloadMessage = "Downloading audio..."
            downloadProgress = 0
            
            try {
                val title = mediaMetadata.title ?: "Unknown"
                val videoUrl = "https://www.youtube.com/watch?v=${mediaMetadata.id}"
                
                Timber.tag("DownloadDialog").d("🎵 Downloading AUDIO (not video): $title")
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "🎵 Downloading audio...\n$title",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Download audio using NewPipe
                val result = NewPipeVideoDownloader.downloadAudio(
                    context = context,
                    videoUrl = videoUrl,
                    title = title,
                    onDownloadId = { downloadId ->
                        currentDownloadId = downloadId
                    }
                )
                
                result.onSuccess { downloadId ->
                    Timber.tag("DownloadDialog").d("✅ Audio download started: $downloadId")
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "✅ Audio download started!\n\n" +
                            "Title: $title\n" +
                            "Type: Audio only (M4A)\n" +
                            "Location: Musifly/audio/\n\n" +
                            "Check notification for progress",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    isDownloading = false
                    onDismiss()
                }.onFailure { error ->
                    Timber.tag("DownloadDialog").e(error, "Audio download failed")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "❌ Audio download failed\n\n" +
                            "Error: ${error.message}\n\n" +
                            "Please try again later.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    isDownloading = false
                }
                
            } catch (e: Exception) {
                Timber.tag("DownloadDialog").e(e, "Audio download error")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "❌ Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                isDownloading = false
            }
        }
    }

    fun downloadToCache() {
        if (mediaMetadata == null) return

        scope.launch {
            isDownloading = true
            downloadMessage = "Preparing cache download..."
            downloadProgress = 0

            try {
                val videoId = mediaMetadata.id ?: return@launch
                val title = mediaMetadata.title ?: "Unknown"

                // Create DownloadRequest for ExoPlayer
                // We use videoId as URI because DownloadUtil's ResolvingDataSource will resolve the actual URL using the cache key (videoId)
                val downloadRequest = DownloadRequest.Builder(videoId, android.net.Uri.parse(videoId))
                    .setCustomCacheKey(videoId)
                    .setData(Util.getUtf8Bytes(title))
                    .build()

                database.transaction {
                    val existing = getSongById(mediaMetadata.id)
                    if (existing != null) {
                        update(existing.song.copy(
                            inLibrary = LocalDateTime.now(),
                            dateDownload = LocalDateTime.now()
                        ))
                    } else {
                        insert(mediaMetadata) {
                            it.copy(
                                inLibrary = LocalDateTime.now(),
                                dateDownload = LocalDateTime.now()
                            )
                        }
                    }
                }

                DownloadService.sendAddDownload(
                    context,
                    ExoDownloadService::class.java,
                    downloadRequest,
                    /* foreground= */ false
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "✅ Added to Cache Downloads", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
                isDownloading = false

            } catch (e: Exception) {
                Timber.e(e, "Cache download failed")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                isDownloading = false
            }
        }
    }

    // Video Preview Dialog
    if (showVideoPreviewDialog) {
        val onDismissRequest = { 
            if (!isLoadingPreview) {
                showVideoPreviewDialog = false
                previewVideoStreamUrl = null
            }
        }

        val dialogTitle: @Composable () -> Unit = {
            Column {
                Text(
                    text = "Video Preview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = previewVideoTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val videoPlayerContent: @Composable () -> Unit = {
            if (isLoadingPreview || previewVideoStreamUrl == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(
                            Color.Black,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Text(
                            text = "Loading preview...",
                            color = Color.White
                        )
                    }
                }
            } else {
                // Video Player
                // Video Player with ExoPlayer (Better support for streams)
                val context = LocalContext.current
                val exoPlayer = remember {
                    val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
                    val dataSourceFactory = DefaultHttpDataSource.Factory()
                        .setUserAgent(userAgent)
                        .setAllowCrossProtocolRedirects(true)
                    
                    ExoPlayer.Builder(context)
                        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                        .build()
                }
                
                // Manage Player Lifecycle
                DisposableEffect(Unit) {
                    onDispose {
                        exoPlayer.release()
                    }
                }
                
                // Load Media
                LaunchedEffect(previewVideoStreamUrl) {
                    if (previewVideoStreamUrl != null) {
                        val mediaItem = MediaItem.fromUri(previewVideoStreamUrl!!)
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setShowNextButton(false)
                            setShowPreviousButton(false)
                            controllerShowTimeoutMs = 3000
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                )
            }
        }

        val buttonsContent: @Composable RowScope.() -> Unit = {
            OutlinedButton(
                onClick = {
                    showVideoPreviewDialog = false
                    previewVideoStreamUrl = null
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Close")
            }
            Button(
                onClick = {
                    if (previewVideoUrl != null) {
                        showVideoPreviewDialog = false
                        startVideoDownload(previewVideoUrl!!, selectedQualityForDownload)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = previewVideoUrl != null && !isLoadingPreview
            ) {
                Icon(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download")
            }
        }

        if (isLiquidGlass) {
            LiquidGlassDialog(
                onDismiss = onDismissRequest,
                title = dialogTitle,
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        videoPlayerContent()
                    }
                },
                buttons = buttonsContent
            )
        } else {
            AlertDialog(
                onDismissRequest = onDismissRequest,
                title = dialogTitle,
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        videoPlayerContent()
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            buttonsContent()
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
    
    // Video Selection Dialog
    if (showVideoSelectionDialog) {
        val onDismissRequest = { 
            if (!isSearchingVideos && !isDownloading) {
                showVideoSelectionDialog = false
            }
        }
        
        val dialogTitle: @Composable () -> Unit = {
            Column {
                Text(
                    text = "Select Video to Download",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (mediaMetadata != null) {
                    Text(
                        text = "${mediaMetadata.title}${mediaMetadata.artists?.firstOrNull()?.let { " by ${it.name}" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        val dialogContent: @Composable ColumnScope.() -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSearchingVideos) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Searching for videos...")
                        }
                    }
                } else if (videoSearchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "No videos found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Try a different search or download audio instead",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Professional Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Select Video to Download",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${videoSearchResults.size} videos found • Tap to preview",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Quality: $selectedQualityForDownload",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                    
                    // Professional Video List
                    videoSearchResults.forEachIndexed { index, video ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedVideoUrl == video.url) 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else 
                                    MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail with Play Overlay
                                Box {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(video.thumbnailUrl ?: "")
                                            .crossfade(300)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(140.dp, 105.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    // Play button overlay
                                    Box(
                                        modifier = Modifier
                                            .size(140.dp, 105.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.3f))
                                            .clickable {
                                                openVideoPreview(video)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.play),
                                            contentDescription = "Preview",
                                            tint = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    
                                    // Duration badge
                                    if (video.duration > 0) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                        ) {
                                            Text(
                                                text = formatDuration(video.duration),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    
                                    // Best Match Badge
                                    if (index == 0 && video.score > 100) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(6.dp)
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.check),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = Color.White
                                                )
                                                Text(
                                                    text = "Best",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Video Info
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Title
                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 20.sp
                                    )
                                    
                                    // Channel with verified badge
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.person),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = video.uploaderName ?: "Unknown",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 12.sp
                                        )
                                        if (video.isVerified) {
                                            Icon(
                                                painter = painterResource(R.drawable.check),
                                                contentDescription = "Verified",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    // Views
                                    if (video.viewCount > 0) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.visibility),
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = formatViewCount(video.viewCount) + " views",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                                
                                // Action Buttons Column
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Preview Button
                                    IconButton(
                                        onClick = {
                                            openVideoPreview(video)
                                        },
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.play),
                                            contentDescription = "Preview",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    // Download Button
                                    IconButton(
                                        onClick = {
                                            startVideoDownload(video.url, selectedQualityForDownload)
                                        },
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.secondaryContainer,
                                                shape = CircleShape
                                            )
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.download),
                                            contentDescription = "Download",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        val buttonsContent: @Composable RowScope.() -> Unit = {
            TextButton(
                onClick = { 
                    if (!isSearchingVideos && !isDownloading) {
                        showVideoSelectionDialog = false
                    }
                }
            ) {
                Text("Cancel")
            }
        }

        if (isLiquidGlass) {
            LiquidGlassDialog(
                onDismiss = onDismissRequest,
                title = dialogTitle,
                content = dialogContent,
                buttons = buttonsContent
            )
        } else {
            AlertDialog(
                onDismissRequest = onDismissRequest,
                title = dialogTitle,
                text = { Column { dialogContent() } },
                confirmButton = { Row { buttonsContent() } }
            )
        }
    }
    
    val onDismissMain = { if (!isDownloading) onDismiss() }

    val mainDialogTitle: @Composable () -> Unit = {
        Text("Download Options")
    }

    val mainDialogContent: @Composable ColumnScope.() -> Unit = {
        if (isDownloading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (downloadProgress > 0) {
                    CircularProgressIndicator(progress = downloadProgress / 100f)
                    Text("${downloadProgress}%", style = MaterialTheme.typography.titleLarge)
                } else {
                    CircularProgressIndicator()
                }
                
                Text(downloadMessage.ifEmpty { "Downloading..." })
                Text(
                    text = "Saving to Music/Musifly folder",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else if (showVideoQualityOptions) {
            // Video Quality Selection Screen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Select Video Quality",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = mediaMetadata?.title ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Info message
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "💡 Automatically searches by title+artist to find the best official video",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Audio warning for high quality
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = "✅ 720p+ will automatically download video+audio and merge them using FFmpeg!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // "Wrong Video?" / Search Manual Option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { downloadVideo("720p") }, // Triggers search dialog
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Not the right video? Search for better version",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                if (isLoadingStreams) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Fetching video options...")
                        }
                    }
                } else if (streamFetchError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                            contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(R.drawable.error),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Failed to load options",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = streamFetchError ?: "Unknown error",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { availableStreams = null }, // Retry triggers LaunchedEffect
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                } else {
                    // Dynamic List of Streams
                    availableStreams?.forEach { stream ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stream.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = stream.format.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                        
                                        Text(
                                            text = formatSize(stream.size),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                // Actions: Preview and Download
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // PREVIEW BUTTON
                                    IconButton(
                                        onClick = {
                                            // Preview logic
                                            previewVideoUrl = null // No direct mp4 download url for preview dialog usually found here?
                                            // Actually videoUrl in stream is direct stream URL.
                                            previewVideoStreamUrl = stream.videoUrl
                                            previewVideoTitle = "${mediaMetadata?.title} (${stream.resolution})"
                                            isLoadingPreview = false
                                            showVideoPreviewDialog = true
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.play),
                                            contentDescription = "Preview",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    // DOWNLOAD BUTTON
                                    IconButton(
                                        onClick = {
                                            if (mediaMetadata != null) {
                                                isDownloading = true
                                                showVideoQualityOptions = false
                                                
                                                scope.launch {
                                                    NewPipeVideoDownloader.startStreamDownload(
                                                        context = context,
                                                        stream = stream,
                                                        title = mediaMetadata.title ?: "Unknown",
                                                        artist = mediaMetadata.artists?.firstOrNull()?.name,
                                                        thumbnailUrl = mediaMetadata.thumbnailUrl,
                                                        sourceVideoUrl = "https://www.youtube.com/watch?v=${mediaMetadata.id}"
                                                    )
                                                    
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "✅ Download started!", Toast.LENGTH_SHORT).show()
                                                        onDismiss()
                                                    }
                                                    isDownloading = false
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.secondaryContainer,
                                                shape = CircleShape
                                            )
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.download),
                                            contentDescription = "Download",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                TextButton(
                    onClick = { showVideoQualityOptions = false },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Back")
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = mediaMetadata?.title ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = mediaMetadata?.artists?.firstOrNull()?.name ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 3. OFFLINE LIBRARY
                Card(
                    onClick = { downloadToCache() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.done), // Using existing icon or closest
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Offline Library",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Save securely within app",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            painter = painterResource(R.drawable.chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "✓ Save songs for offline playback\n✓ High-quality audio\n✓ Seamless library integration",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    val mainButtonsContent: @Composable RowScope.() -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (isDownloading) {
                TextButton(onClick = { 
                    onDismiss()
                    Toast.makeText(context, "Download continues in background", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Background")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { cancelDownload() }) {
                    Text("Cancel Download")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }

    if (isLiquidGlass) {
        LiquidGlassDialog(
            onDismiss = onDismissMain,
            title = mainDialogTitle,
            content = mainDialogContent,
            buttons = mainButtonsContent
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismissMain,
            title = mainDialogTitle,
            text = { Column { mainDialogContent() } },
            confirmButton = {
                if (isDownloading) {
                    TextButton(onClick = { cancelDownload() }) {
                        Text("Cancel Download")
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                if (isDownloading) {
                    TextButton(onClick = { 
                        // Allow dialog to close but download continues in background
                        onDismiss()
                        Toast.makeText(context, "Download continues in background", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Background")
                    }
                }
            }
        )
    }
}
