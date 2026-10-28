package com.musifly.android.utils.update

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper class for testing and debugging the update system
 * 
 * Use these methods to manually test the update system
 */
object UpdateTestHelper {
    private const val TAG = "UpdateTestHelper"
    
    /**
     * Test JSON fetch and log the result
     */
    suspend fun testJsonFetch(context: Context) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧪 Testing JSON fetch from: ${UpdateChecker.UPDATE_JSON_URL}")
            val jsonString = HttpFetcher.fetchJson(UpdateChecker.UPDATE_JSON_URL, context)
            
            if (jsonString != null) {
                Log.d(TAG, "✅ JSON fetched successfully")
                Log.d(TAG, "JSON content: $jsonString")
                
                val updateData = UpdateData.fromJson(jsonString)
                if (updateData != null) {
                    Log.d(TAG, "✅ JSON parsed successfully")
                    Log.d(TAG, "Latest version: ${updateData.latestVersionName} (${updateData.latestVersionCode})")
                    Log.d(TAG, "Notification enabled: ${updateData.notificationEnabled}")
                    Log.d(TAG, "Message ID: ${updateData.messageId}")
                } else {
                    Log.e(TAG, "❌ Failed to parse JSON")
                }
            } else {
                Log.e(TAG, "❌ Failed to fetch JSON")
                
                // Check cache
                val cached = UpdatePrefs.getCachedJson(context)
                if (cached != null) {
                    Log.d(TAG, "📦 Using cached JSON: $cached")
                } else {
                    Log.e(TAG, "❌ No cached JSON available")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in test: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Test update check and log the result
     */
    suspend fun testUpdateCheck(context: Context) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🧪 Testing update check...")
            val updateData = UpdateChecker.checkForUpdate(context)
            
            if (updateData != null) {
                Log.d(TAG, "✅ Update available!")
                Log.d(TAG, "Version: ${updateData.latestVersionName} (${updateData.latestVersionCode})")
                Log.d(TAG, "Title: ${updateData.updateTitle}")
                Log.d(TAG, "Description: ${updateData.updateDescription}")
                Log.d(TAG, "Download URL: ${updateData.downloadUrl}")
            } else {
                Log.d(TAG, "ℹ️ No update available")
                
                // Check why
                val jsonString = HttpFetcher.fetchJson(UpdateChecker.UPDATE_JSON_URL, context)
                if (jsonString != null) {
                    val data = UpdateData.fromJson(jsonString)
                    if (data != null) {
                        val installedVersion = com.musifly.android.BuildConfig.VERSION_CODE
                        val lastNotified = UpdatePrefs.getLastVersionNotified(context)
                        
                        Log.d(TAG, "Installed version: $installedVersion")
                        Log.d(TAG, "Latest version: ${data.latestVersionCode}")
                        Log.d(TAG, "Last notified: $lastNotified")
                        
                        if (installedVersion >= data.latestVersionCode) {
                            Log.d(TAG, "ℹ️ App is already up to date")
                        } else if (data.latestVersionCode <= lastNotified) {
                            Log.d(TAG, "ℹ️ Already notified about this version")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in test: ${e.message}", e)
            e.printStackTrace()
        }
    }
    
    /**
     * Clear all update preferences (for testing)
     */
    fun clearAll(context: Context) {
        UpdatePrefs.clearAll(context)
        Log.d(TAG, "🧹 Cleared all update preferences")
    }
    
    /**
     * Force show notification (for testing)
     * REMOVED: JSON notification feature has been removed
     */
    // fun forceShowNotification(context: Context, title: String, message: String, link: String) {
    //     // Notification feature removed
    // }
}
