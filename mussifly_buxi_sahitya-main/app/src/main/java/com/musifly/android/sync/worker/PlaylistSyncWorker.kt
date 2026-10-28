package com.musifly.android.sync.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.musifly.android.sync.repository.PlaylistRepository
import com.musifly.android.sync.SyncManagerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber

/**
 * Worker for syncing playlists between local DB and Firestore
 * Runs periodically to keep data in sync
 */
class PlaylistSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val playlistRepository: PlaylistRepository by lazy {
        EntryPointAccessors.fromApplication(
            context,
            SyncManagerEntryPoint::class.java
        ).playlistRepository()
    }
    
    override suspend fun doWork(): Result {
        return try {
            val auth = FirebaseAuth.getInstance()
            
            if (auth.currentUser == null) {
                Timber.tag("PlaylistSyncWorker").d("User not logged in, skipping sync")
                return Result.success()
            }
            
            Timber.tag("PlaylistSyncWorker").d("Starting playlist sync")
            
            // Step 1: Pull latest from Supabase and merge with local
            val pullResult = playlistRepository.syncFromSupabase()
            pullResult.fold(
                onSuccess = { count ->
                    Timber.tag("PlaylistSyncWorker").d("Pulled $count playlists from Supabase")
                },
                onFailure = { error ->
                    Timber.tag("PlaylistSyncWorker").e(error, "Failed to pull playlists from Supabase")
                }
            )
            
            // Step 2: Push unsynced local changes to Supabase
            val pushResult = playlistRepository.syncUnsyncedToSupabase()
            pushResult.fold(
                onSuccess = { count ->
                    Timber.tag("PlaylistSyncWorker").d("Pushed $count unsynced playlists to Supabase")
                },
                onFailure = { error ->
                    Timber.tag("PlaylistSyncWorker").e(error, "Failed to push unsynced playlists to Supabase")
                }
            )
            
            // Step 3: Trigger auto-download for synced songs - DISABLED
            // User feedback: Auto-download triggers unwanted video downloads via NewPipe
            /*
            try {
                val syncedSongs = playlistRepository.getSyncedSongsForDownload()
                if (syncedSongs.isNotEmpty()) {
                    Timber.tag("PlaylistSyncWorker").d("Triggering auto-download for ${syncedSongs.size} synced songs")
                    val downloader = com.musifly.android.download.SmartMusicDownloader(applicationContext)
                    
                    syncedSongs.forEach { song ->
                        // Construct URL assuming ID is YouTube Video ID (standard for this app's playlists)
                        // This avoids search overhead
                        val videoUrl = "https://www.youtube.com/watch?v=${song.id}"
                        val artistName = song.artists.firstOrNull()?.name
                        
                        try {
                            // Resolve stream and start download in foreground service
                            // This checks if file exists first to avoid duplicates
                            downloader.startDownloadInService(
                                videoUrl = videoUrl,
                                title = song.title,
                                artist = artistName,
                                thumbnailUrl = song.thumbnailUrl
                            )
                        } catch (e: Exception) {
                            Timber.tag("PlaylistSyncWorker").e(e, "Failed to start download for ${song.title}")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag("PlaylistSyncWorker").e(e, "Error during auto-download trigger")
            }
            */
            
            Timber.tag("PlaylistSyncWorker").d("Playlist sync completed")
            Result.success()
        } catch (e: Exception) {
            Timber.tag("PlaylistSyncWorker").e(e, "Error in playlist sync worker")
            Result.retry() // Retry on failure
        }
    }
}
