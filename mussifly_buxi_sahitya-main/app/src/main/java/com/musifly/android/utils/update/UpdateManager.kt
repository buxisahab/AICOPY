package com.musifly.android.utils.update

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.util.Log

object UpdateManager {
    private const val UPDATE_WORK_TAG = "musifly_update_work"
    
    // Last checked timestamp in memory (to avoid too frequent checks on resume)
    // Initialize with 0 to allow check on first run, or current time to skip immediate first run
    private var lastCheckedTime: Long = 0
    private const val MIN_CHECK_INTERVAL_MS = 60000 // 1 minute between checks on resume

    @Composable
    fun CheckForUpdateDialog(forceShow: Boolean = false, checkOnResume: Boolean = false) {
        val context = LocalContext.current
        var updateData by remember { mutableStateOf<UpdateData?>(null) }
        
        // This effect runs once when the composable enters composition
        // It's the primary check for updates when the app starts
        LaunchedEffect(Unit) {
            val currentTime = System.currentTimeMillis()
            updateData = UpdateChecker.checkForUpdate(context, forceShow)
            lastCheckedTime = currentTime
        }
        
        // This effect can optionally run when app resumes (re-enters composition)
        // Set checkOnResume=true only for debugging/testing
        if (checkOnResume) {
            LaunchedEffect(Unit) {
                // Determine if enough time passed since last check to avoid spamming
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastCheckedTime > MIN_CHECK_INTERVAL_MS) {
                    val newData = UpdateChecker.checkForUpdate(context, forceShow)
                    if (newData != null) {
                        updateData = newData
                    }
                    lastCheckedTime = currentTime
                }
            }
        }
        
        if (updateData != null) {
            UpdateDialog(
                updateData = updateData!!,
                onDismiss = { 
                    updateData = null 
                    // Mark as notified when dismissed so it doesn't show again effectively
                    // For persistent nagging, modify this logic
                    updateData?.let { data ->
                         UpdateChecker.markUpdateAsNotified(context, data.latestVersionCode)
                    }
                },
                onUpdateClick = {
                    // Update click action
                }
            )
        }
    }
    
    fun InitializeUpdateSystem(context: Context) {
         try {
            Log.d("UpdateManager", "Initializing update worker")
            
            // Create constraints - requires internet
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            // Build periodic request - check every 12 hours
            // Android minimum is 15 minutes, but we don't need that frequency
            val updateRequest = PeriodicWorkRequestBuilder<UpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag(UPDATE_WORK_TAG)
                .build()
            
            // Enqueue unique periodic work - keeps existing schedule if already active
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "MusiflyUpdateCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                updateRequest
            )
            
            Log.d("UpdateManager", "Update worker scheduled successfully")
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to schedule update worker", e)
        }
    }
    
    // Composable-friendly helper that gets context automatically
    @Composable
    fun InitializeUpdateSystem() {
        val context = LocalContext.current
        LaunchedEffect(Unit) {
            InitializeUpdateSystem(context)
        }
    }
    
    fun clearUpdatePreferences(context: Context) {
        UpdatePrefs.clearAll(context)
    }
}
