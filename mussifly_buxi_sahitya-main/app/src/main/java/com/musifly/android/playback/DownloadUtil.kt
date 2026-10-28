package com.musifly.android.playback

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.musifly.innertube.YouTube
import com.musifly.android.constants.AudioQuality
import com.musifly.android.constants.AudioQualityKey
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.FormatEntity
import com.musifly.android.di.DownloadCache
import com.musifly.android.di.PlayerCache
import com.musifly.android.utils.YTPlayerUtils
import com.musifly.android.utils.enumPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.Executor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: SimpleCache,
    @PlayerCache val playerCache: SimpleCache,
    private val cachedDownloadsRepository: com.musifly.android.sync.repository.CachedDownloadsRepository,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val songUrlCache = HashMap<String, Pair<String, Long>>()
    
    // Mutex to prevent concurrent cached download adds (critical for Firestore)
    private val cachedDownloadMutex = Mutex()
    
    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        OkHttpClient
                            .Builder()
                            .proxy(YouTube.proxy)
                            .build(),
                    ),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val length = if (dataSpec.length >= 0) dataSpec.length else 1

            if (playerCache.isCached(mediaId, dataSpec.position, length)) {
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            val playedFormat = runBlocking(Dispatchers.IO) { database.format(mediaId).first() }
            val playbackData = runBlocking(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                )
            }.getOrThrow()
            val format = playbackData.format

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength!!,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        playbackUrl = playbackData.streamUrl
                    ),
                )
            }

            val streamUrl = playbackData.streamUrl.let {
                // Specify range to avoid YouTube's throttling
                "${it}&range=0-${format.contentLength ?: 10000000}"
            }

            songUrlCache[mediaId] =
                streamUrl to System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
            dataSpec.withUri(streamUrl.toUri())
        }
    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run)
        ).apply {
            maxParallelDownloads = 3
            addListener(
                ExoDownloadService.TerminalStateNotificationHelper(
                    context = context,
                    notificationHelper = downloadNotificationHelper,
                    nextNotificationId = ExoDownloadService.NOTIFICATION_ID + 1,
                ),
            )
        }
    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    private val downloadSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    init {
        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
        downloadManager.addListener(
            object : DownloadManager.Listener {
                override fun onDownloadChanged(
                    downloadManager: DownloadManager,
                    download: Download,
                    finalException: Exception?,
                ) {
                    val previousState = downloads.value[download.request.id]?.state
                    val currentState = download.state
                    val songId = download.request.id
                    
                    downloads.update { map ->
                        map.toMutableMap().apply {
                            set(songId, download)
                        }
                    }
                    
                    // Handle download completion - add to Firestore with mutex to prevent concurrent writes
                    if (previousState != Download.STATE_COMPLETED && currentState == Download.STATE_COMPLETED) {
                        Timber.tag("DownloadUtil").d("✅ Download completed: $songId - adding to cached downloads repository")
                        downloadSyncScope.launch {
                            // Use mutex to ensure sequential processing (critical for Firestore)
                            cachedDownloadMutex.withLock {
                                cachedDownloadsRepository.addCachedDownload(songId).fold(
                                    onSuccess = {
                                        Timber.tag("DownloadUtil").d("✅ Successfully added cached download to repository: $songId")
                                    },
                                    onFailure = { e ->
                                        Timber.tag("DownloadUtil").e(e, "❌ Failed to add cached download to repository: $songId")
                                    }
                                )
                            }
                        }
                    }
                }

                override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                    val songId = download.request.id
                    downloads.update { map ->
                        map.toMutableMap().apply {
                            remove(songId)
                        }
                    }
                    
                    // Remove from Supabase if download is removed
                    Timber.tag("DownloadUtil").d("🔥 Download removed: $songId - removing from cached downloads repository")
                    downloadSyncScope.launch {
                        cachedDownloadsRepository.removeCachedDownload(songId)
                    }
                }
            },
        )
    }
}
