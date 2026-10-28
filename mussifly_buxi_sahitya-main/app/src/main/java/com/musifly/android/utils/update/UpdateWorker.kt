package com.musifly.android.utils.update

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager worker that periodically checks for updates and notifications
 * 
 * Runs every 6 hours (as per requirements) and:
 * - Fetches update JSON from GitHub RAW
 * - Checks for update availability
 * - Sends notifications if conditions are met
 * - Respects daily limits and intervals
 */
class UpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "UpdateWorker"
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 UpdateWorker started")
            
            // Fetch and parse update JSON
            val jsonString = HttpFetcher.fetchJson(
                UpdateChecker.UPDATE_JSON_URL,
                applicationContext
            )
            
            if (jsonString == null) {
                Log.w(TAG, "⚠️ No JSON data available (offline or fetch failed)")
                // Don't retry immediately - use success so worker can continue on schedule
                return@withContext Result.success()
            }
            
            val updateData = UpdateData.fromJson(jsonString)
            if (updateData == null) {
                Log.e(TAG, "❌ Failed to parse JSON")
                return@withContext Result.success() // Don't retry on parse errors
            }
            
            Log.d(TAG, "✅ JSON parsed. Notification enabled: ${updateData.notificationEnabled}")
            
            // Notification checking removed - JSON notification feature has been removed
            // Only update checking remains active
            
            Log.d(TAG, "✅ UpdateWorker completed successfully")
            return@withContext Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ UpdateWorker failed: ${e.message}", e)
            e.printStackTrace()
            // Return success so worker continues on schedule (don't spam retries)
            return@withContext Result.success()
        }
    }
}

/**
 * Helper object to initialize and manage the periodic worker
 */
object UpdateWorkerManager {
    private const val TAG = "UpdateWorkerManager"
    private const val WORK_NAME = "musifly_update_checker"
    
    /**
     * Start periodic update checking with WorkManager
     * Checks every 6 hours as per requirements
     */
    fun startPeriodicCheck(context: Context) {
        try {
            // Try to get WorkManager instance, but handle if not initialized
            val workManager = try {
                androidx.work.WorkManager.getInstance(context)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "WorkManager not initialized, skipping periodic update check (non-critical)")
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get WorkManager instance: ${e.message}", e)
                return
            }
            
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()
            
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<UpdateWorker>(
                6, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()
            
            workManager.apply {
                // Use unique work to replace existing work if any
                enqueueUniquePeriodicWork(
                    WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
            }
            
            Log.d(TAG, "Periodic update check started (every 6 hours)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start periodic update check (non-critical): ${e.message}", e)
            // Don't let this error propagate - it's non-critical
        }
    }
    
    /**
     * Cancel periodic update checking
     */
    fun stopPeriodicCheck(context: Context) {
        try {
            val workManager = try {
                androidx.work.WorkManager.getInstance(context)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "WorkManager not initialized, cannot stop periodic update check")
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get WorkManager instance: ${e.message}", e)
                return
            }
            
            workManager.cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Periodic update check stopped")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop periodic update check (non-critical): ${e.message}", e)
        }
    }
}
