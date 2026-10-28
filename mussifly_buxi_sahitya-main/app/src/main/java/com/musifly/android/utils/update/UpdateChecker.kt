package com.musifly.android.utils.update

import android.content.Context
import android.util.Log
import com.musifly.android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Notification-related imports removed
import com.musifly.android.secure.SecureString

/**
 * Main update checker that compares versions and manages update logic
 * 
 * Responsibilities:
 * - Fetch and parse update JSON
 * - Compare installed version with latest version
 * - Check if update dialog should be shown
 * - Track which version was last notified
 */
object UpdateChecker {
    private const val TAG = "UpdateChecker"
    
    // GitHub RAW URL for the update JSON file
    // Replace this with your actual GitHub RAW URL

    // GitHub RAW URL for the update JSON file
    // Securely retrieved from native code
    val UPDATE_JSON_URL get() = SecureString.xy4()
    
    /**
     * Check for updates and return update data if update is available
     * 
     * @param context Application context
     * @param forceShow If true, ignores "already notified" check and shows dialog even if version was shown before
     * @return UpdateData if update is needed, null otherwise
     */
    suspend fun checkForUpdate(context: Context, forceShow: Boolean = false): UpdateData? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching JSON from: $UPDATE_JSON_URL (forceShow=$forceShow)")
            
            // Fetch JSON from GitHub RAW
            val jsonString = HttpFetcher.fetchJson(UPDATE_JSON_URL, context)
            if (jsonString == null) {
                Log.w(TAG, "Failed to fetch JSON (null returned)")
                return@withContext null
            }
            
            Log.d(TAG, "JSON fetched successfully, length: ${jsonString.length}")
            
            // Parse JSON
            val updateData = UpdateData.fromJson(jsonString)
            if (updateData == null) {
                Log.w(TAG, "Failed to parse JSON")
                return@withContext null
            }
            
            Log.d(TAG, "JSON parsed successfully. Latest version: ${updateData.latestVersionName} (${updateData.latestVersionCode})")
            
            // Get installed version code
            val installedVersionCode = BuildConfig.VERSION_CODE
            val installedVersionName = BuildConfig.VERSION_NAME
            Log.d(TAG, "📱 Installed version: $installedVersionName (code: $installedVersionCode)")
            Log.d(TAG, "🌐 Latest version from JSON: ${updateData.latestVersionName} (code: ${updateData.latestVersionCode})")
            
            // Validate version code from JSON
            if (updateData.latestVersionCode <= 0) {
                Log.e(TAG, "❌ Invalid version code in JSON: ${updateData.latestVersionCode}")
                Log.e(TAG, "💡 Make sure JSON has 'latest_version_code' field with a positive integer value")
                return@withContext null
            }
            
            // Native update check logic
            val lastNotifiedVersionCode = UpdatePrefs.getLastVersionNotified(context)
            
            val isUpdateNeeded = SecureString.check(
                installedVersionCode,
                installedVersionName,
                updateData.latestVersionCode,
                updateData.latestVersionName,
                lastNotifiedVersionCode,
                forceShow
            )
            
            if (isUpdateNeeded) {
                Log.d(TAG, "✅ Update needed (confirmed by native logic)")
                return@withContext updateData
            } else {
                Log.d(TAG, "ℹ️ No update needed (checked by native logic)")
            }
            
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking for update: ${e.message}", e)
            e.printStackTrace()
            return@withContext null
        }
    }
    
    /**
     * Mark update as notified (so we don't show it again)
     */
    fun markUpdateAsNotified(context: Context, versionCode: Int) {
        UpdatePrefs.setLastVersionNotified(context, versionCode)
        Log.d(TAG, "Marked version $versionCode as notified")
    }
    
    // Notification-related functions removed - JSON notification feature has been removed
}
