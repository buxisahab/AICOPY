package com.musifly.android.sync.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.musifly.android.sync.repository.LikesRepository
import com.musifly.android.sync.SyncManagerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import timber.log.Timber

/**
 * Worker for syncing liked songs between local DB and Firestore
 * Runs periodically to keep data in sync
 */
class LikesSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val likesRepository: LikesRepository by lazy {
        EntryPointAccessors.fromApplication(
            context,
            SyncManagerEntryPoint::class.java
        ).likesRepository()
    }
    
    override suspend fun doWork(): Result {
        return try {
            val auth = FirebaseAuth.getInstance()
            
            if (auth.currentUser == null) {
                Timber.tag("LikesSyncWorker").d("User not logged in, skipping sync")
                return Result.success()
            }
            
            Timber.tag("LikesSyncWorker").d("Starting likes sync")
            
            // Step 1: Pull latest from Supabase and merge with local
            val pullResult = likesRepository.syncFromSupabase()
            pullResult.fold(
                onSuccess = { count ->
                    Timber.tag("LikesSyncWorker").d("Pulled $count liked songs from Supabase")
                },
                onFailure = { error ->
                    Timber.tag("LikesSyncWorker").e(error, "Failed to pull likes from Supabase")
                }
            )
            
            // Step 2: Push unsynced local changes to Supabase
            val pushResult = likesRepository.syncUnsyncedToSupabase()
            pushResult.fold(
                onSuccess = { count ->
                    Timber.tag("LikesSyncWorker").d("Pushed $count unsynced likes to Supabase")
                },
                onFailure = { error ->
                    Timber.tag("LikesSyncWorker").e(error, "Failed to push unsynced likes")
                }
            )
            
            Timber.tag("LikesSyncWorker").d("Likes sync completed")
            Result.success()
        } catch (e: Exception) {
            Timber.tag("LikesSyncWorker").e(e, "Error in likes sync worker")
            Result.retry() // Retry on failure
        }
    }
}
