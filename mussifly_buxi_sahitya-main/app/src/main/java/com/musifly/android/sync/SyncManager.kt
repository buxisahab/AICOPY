package com.musifly.android.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.musifly.android.sync.firestore.FirestoreAutoInitService
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.musifly.android.playback.ExoDownloadService
import com.musifly.android.sync.repository.LikesRepository
import com.musifly.android.sync.repository.CachedDownloadsRepository
import com.musifly.android.sync.repository.PlaylistRepository
import com.musifly.android.sync.repository.BookmarksRepository
import com.musifly.android.sync.worker.LikesSyncWorker
import com.musifly.android.sync.worker.PlaylistSyncWorker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Manager for sync operations (Firestore removed - local storage only)
 * Sync methods are no-ops but kept for API compatibility
 */
class SyncManager(
    private val context: Context,
    private val likesRepository: LikesRepository,
    private val playlistRepository: PlaylistRepository,
    private val cachedDownloadsRepository: CachedDownloadsRepository,
    private val bookmarksRepository: BookmarksRepository,
    private val migrationService: com.musifly.android.sync.supabase.SupabaseMigrationService
) {
    private val auth = FirebaseAuth.getInstance()
    
    // Lazy initialization to avoid crash if WorkManager is not initialized yet
    private val workManager: WorkManager? by lazy {
        try {
            WorkManager.getInstance(context)
        } catch (e: IllegalStateException) {
            Timber.tag("SyncManager").w(e, "WorkManager not initialized, periodic sync will be disabled")
            null
        }
    }
    
    companion object {
        private const val LIKES_SYNC_WORK_NAME = "likes_sync_work"
        private const val PLAYLIST_SYNC_WORK_NAME = "playlist_sync_work"
        private const val SYNC_INTERVAL_HOURS = 4L // Sync every 4 hours
    }
    
    /**
     * Sync all user data from Supabase
     * Called on login
     * Downloads from Supabase and uploads local unsynced data
     */
    fun syncAllUserData(
        scope: CoroutineScope,
        onComplete: (Result<SyncResult>) -> Unit = {}
    ) {
        // Use SupervisorJob + CoroutineExceptionHandler to prevent crashes
        val exceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
            Timber.tag("SyncManager").e(throwable, "Uncaught exception in sync coroutine - preventing crash")
            onComplete(Result.failure(throwable))
        }
        scope.launch(Dispatchers.IO + kotlinx.coroutines.SupervisorJob() + exceptionHandler) {
            try {
                if (auth.currentUser == null) {
                    Timber.tag("SyncManager").d("No user logged in, skipping sync")
                    onComplete(Result.success(SyncResult(0, 0, 0, 0)))
                    return@launch
                }
                
                Timber.tag("SyncManager").d("Starting full user data sync for user: ${auth.currentUser?.uid}")
                
                // Step 0: Migrate from Firestore to Supabase if needed
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    Timber.tag("SyncManager").d("Checking if migration from Firestore is needed for $userId...")
                    migrationService.migrateIfNeeded(userId)
                }
                
                // Step 1: Upload local unsynced data to Supabase first
                // Wrap in try-catch to prevent crashes from affecting sync
                Timber.tag("SyncManager").d("Uploading unsynced local data to Supabase...")
                val uploadLikesResult = try {
                    likesRepository.syncUnsyncedToSupabase()
                } catch (e: Exception) {
                    Timber.tag("SyncManager").e(e, "Error uploading unsynced likes")
                    Result.failure(e)
                }
                val uploadPlaylistsResult = try {
                    playlistRepository.syncUnsyncedToSupabase()
                } catch (e: Exception) {
                    Timber.tag("SyncManager").e(e, "Error uploading unsynced playlists to Supabase")
                    Result.failure(e)
                }
                
                // Add Bookmarks Sync To Supabase
                val uploadBookmarksResult = try {
                    bookmarksRepository.syncUnsyncedToSupabase()
                } catch (e: Exception) {
                    Timber.tag("SyncManager").e(e, "Error uploading unsynced bookmarks to Supabase")
                    Result.failure(e)
                }

                // Add Cached Downloads Sync To Supabase
                val uploadCachedDownloadsResult = try {
                    cachedDownloadsRepository.syncUnsyncedToSupabase()
                } catch (e: Exception) {
                    Timber.tag("SyncManager").e(e, "Error uploading unsynced cached downloads to Supabase")
                    Result.failure(e)
                }
                
                uploadLikesResult.onSuccess {
                    Timber.tag("SyncManager").d("Uploaded $it unsynced likes to Supabase")
                }.onFailure { e ->
                    Timber.tag("SyncManager").w(e, "Failed to upload unsynced likes")
                }
                
                uploadPlaylistsResult.onSuccess {
                    Timber.tag("SyncManager").d("Uploaded $it unsynced playlists to Supabase")
                }.onFailure { e ->
                    Timber.tag("SyncManager").w(e, "Failed to upload unsynced playlists")
                }

                uploadBookmarksResult.onSuccess {
                    Timber.tag("SyncManager").d("Uploaded $it unsynced bookmarks to Supabase")
                }.onFailure { e ->
                    Timber.tag("SyncManager").w(e, "Failed to upload unsynced bookmarks")
                }

                uploadCachedDownloadsResult.onSuccess {
                    Timber.tag("SyncManager").d("Uploaded $it unsynced cached downloads to Supabase")
                }.onFailure { e ->
                    Timber.tag("SyncManager").w(e, "Failed to upload unsynced cached downloads")
                }
                
                // Step 2: Download data from Supabase (may overwrite local if newer)
                // Wrap in try-catch to prevent crashes from affecting sync
                Timber.tag("SyncManager").d("Downloading data from Supabase...")
                val likesResult = try {
                    likesRepository.syncFromSupabase()
                } catch (e: Exception) {
                    Timber.tag("SyncManager").e(e, "Error downloading likes from Supabase")
                    Result.failure(e)
                }
                val playlistsResult = try {
                    playlistRepository.syncFromSupabase()
                } catch (e: Exception) {
                    Result.failure(e)
                }

                // Step 2.1: Sync Bookmarks (Albums/Artists) from Supabase
                Timber.tag("SyncManager").d("Downloading bookmarks from Supabase...")
                val bookmarksResult = try {
                    bookmarksRepository.syncFromSupabase()
                } catch (e: Exception) {
                    Timber.tag("SyncManager").e(e, "Error downloading bookmarks from Supabase")
                    Result.failure(e)
                }

                // Step 3: Sync cached downloads (Parallel attempt for efficiency)
                Timber.tag("SyncManager").d("Downloading cached downloads from Supabase...")
                val cachedDownloadsResult = try {
                    val result = cachedDownloadsRepository.syncFromSupabase()
                    
                    // Trigger actual downloads for these cached songs
                    result.onSuccess {
                         try {
                            triggerCachedAutoDownload()
                        } catch (e: Exception) {
                            Timber.tag("SyncManager").e(e, "Error triggering cached downloads")
                        }
                    }
                    result
                } catch (e: Exception) {
                    Timber.tag("SyncManager").e(e, "Error downloading cached downloads from Supabase")
                    Result.failure(e)
                }
                
                
                // AUTO-DOWNLOAD DISABLED: Downloads should only happen when user manually clicks download
                // Supabase sync is only for syncing playlist metadata, not for triggering downloads
                // if (playlistsResult.isSuccess) {
                //     try {
                //         triggerAutoDownload()
                //     } catch (e: Exception) {
                //         Timber.tag("SyncManager").e(e, "Error triggering auto-download")
                //     }
                // }
                
                
                val likedSongsCount = likesResult.getOrNull() ?: 0
                val playlistsCount = playlistsResult.getOrNull() ?: 0
                val bookmarksCount = bookmarksResult.getOrNull() ?: 0
                val cachedDownloadsCount = cachedDownloadsResult.getOrNull() ?: 0
                
                Timber.tag("SyncManager").d("Sync complete: Downloaded $likedSongsCount likes, $playlistsCount playlists, $bookmarksCount bookmarks, $cachedDownloadsCount cached downloads")
                
                // Schedule periodic sync workers (non-blocking, errors won't affect sync result)
                try {
                    schedulePeriodicSync()
                } catch (e: Exception) {
                    Timber.tag("SyncManager").w(e, "Failed to schedule periodic sync (non-critical)")
                    // Don't fail the sync operation if scheduling fails
                }
                
                onComplete(Result.success(SyncResult(likedSongsCount, playlistsCount, bookmarksCount, cachedDownloadsCount)))
            } catch (e: Exception) {
                Timber.tag("SyncManager").e(e, "Failed to sync user data")
                onComplete(Result.failure(e))
            }
        }
    }
    
    /**
     * Trigger auto-download for cached songs
     */
    private suspend fun triggerCachedAutoDownload() {
        try {
            val cachedSongs = cachedDownloadsRepository.getCachedSongsForDownload()
            if (cachedSongs.isNotEmpty()) {
                Timber.tag("SyncManager").d("Triggering auto-cache for ${cachedSongs.size} cached songs")
                
                cachedSongs.forEach { song ->
                    try {
                        val downloadRequest = DownloadRequest
                            .Builder(song.id, song.id.toUri())
                            .setCustomCacheKey(song.id)
                            .setData(song.title.toByteArray())
                            .build()
                        
                        DownloadService.sendAddDownload(
                            context,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                        Timber.tag("SyncManager").d("✅ Added to cache: ${song.title}")
                    } catch (e: Exception) {
                        Timber.tag("SyncManager").e(e, "Failed to start cache for ${song.title}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("SyncManager").e(e, "Error in cached auto-download logic")
        }
    }

    /**
     * Trigger auto-download for synced songs (playlists)
     */
    private suspend fun triggerAutoDownload() {
        try {
            val syncedSongs = playlistRepository.getSyncedSongsForDownload()
            if (syncedSongs.isNotEmpty()) {
                Timber.tag("SyncManager").d("Triggering auto-cache for ${syncedSongs.size} synced songs from playlists")
                
                syncedSongs.forEach { song ->
                    try {
                        val downloadRequest = DownloadRequest
                            .Builder(song.id, song.id.toUri())
                            .setCustomCacheKey(song.id)
                            .setData(song.title.toByteArray())
                            .build()
                        
                        DownloadService.sendAddDownload(
                            context,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                        Timber.tag("SyncManager").d("✅ Added to cache from playlist: ${song.title}")
                    } catch (e: Exception) {
                        Timber.tag("SyncManager").e(e, "Failed to start cache for playlist song ${song.title}")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("SyncManager").e(e, "Error in playlist auto-download logic")
        }
    }
    
    /**
     * Schedule periodic background sync
     */
    private fun schedulePeriodicSync() {
        workManager?.let { wm ->
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            // Schedule likes sync
            val likesSyncWork = PeriodicWorkRequestBuilder<LikesSyncWorker>(
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            
            wm.enqueueUniquePeriodicWork(
                LIKES_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                likesSyncWork
            )
            
            // Schedule playlist sync
            val playlistSyncWork = PeriodicWorkRequestBuilder<PlaylistSyncWorker>(
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            
            wm.enqueueUniquePeriodicWork(
                PLAYLIST_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                playlistSyncWork
            )
            
            Timber.tag("SyncManager").d("Scheduled periodic sync workers")
        } ?: run {
            Timber.tag("SyncManager").w("Cannot schedule periodic sync: WorkManager not initialized")
        }
    }
    
    /**
     * Cancel periodic sync (called on logout)
     */
    fun cancelPeriodicSync() {
        workManager?.let { wm ->
            wm.cancelUniqueWork(LIKES_SYNC_WORK_NAME)
            wm.cancelUniqueWork(PLAYLIST_SYNC_WORK_NAME)
            Timber.tag("SyncManager").d("Cancelled periodic sync workers")
        } ?: run {
            Timber.tag("SyncManager").w("Cannot cancel periodic sync: WorkManager not initialized")
        }
    }
    
    /**
     * Manually trigger sync (from UI)
     */
    fun triggerManualSync(
        scope: CoroutineScope,
        onComplete: (Result<SyncResult>) -> Unit = {}
    ) {
        syncAllUserData(scope, onComplete)
    }
}

/**
 * Result of sync operation
 */
data class SyncResult(
    val likedSongsCount: Int,
    val playlistsCount: Int,
    val bookmarksCount: Int,
    val cachedDownloadsCount: Int
)
