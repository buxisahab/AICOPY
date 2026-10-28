package com.buxi.music.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URL

/**
 * UpdateManager handles app update notifications by fetching configuration from a remote JSON file.
 * 
 * Features:
 * - Fetches update configuration from remote JSON
 * - Compares app version with remote version
 * - Shows themed Material Design dialog
// * - Handles errors gracefully
 * - Supports custom button texts and links
 */
object UpdateManager {
    
    private const val TAG = "UpdateManager"
    private const val CONFIG_URL = "https://pastebin.com/raw/Y5gH7Cyz"
    
    /**
     * Data class representing the update configuration from JSON
     */
    @Serializable
    data class UpdateConfig(
        val version_name: String,
        val title: String,
        val message: String,
        val update_now_text: String,
        val later_text: String,
        val show_later_button: Boolean,
        val update_link: String
    )
    
    /**
     * Main function to check for updates and show dialog if needed
     * 
     * @param activity The activity context to show the dialog
     */
    suspend fun checkForUpdate(activity: Activity) {
        try {
            Log.d(TAG, "Starting update check...")
            
            // Fetch update configuration from remote JSON
            val config = fetchUpdateConfig()
            Log.d(TAG, "Fetched config: $config")
            
            if (config != null) {
                // Get current app version
                val currentVersion = getCurrentAppVersion(activity)
                Log.d(TAG, "Current app version: $currentVersion")
                
                // Compare versions
                if (shouldShowUpdateDialog(currentVersion, config.version_name)) {
                    Log.d(TAG, "Update available: Current: $currentVersion, Remote: ${config.version_name}")
                    // Store config for Compose dialog
                    updateConfig = config
                    Log.d(TAG, "Update config stored: $updateConfig")
                } else {
                    Log.d(TAG, "App is up to date. Current: $currentVersion, Remote: ${config.version_name}")
                }
            } else {
                Log.d(TAG, "No config fetched - check network or JSON URL")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            // Fail silently - don't crash the app
        }
    }
    
    // Store the update config for Compose dialog
    private var updateConfig: UpdateConfig? = null
    
    // State to trigger recomposition
    private var configUpdated = false
    
    /**
     * Gets the current update config (for Compose dialog)
     */
    fun getUpdateConfig(): UpdateConfig? = updateConfig
    
    /**
     * Clears the update config after dialog is shown
     */
    fun clearUpdateConfig() {
        updateConfig = null
    }
    
    /**
     * Force refresh the update configuration from remote JSON
     * This will immediately fetch the latest JSON and update the config
     */
    suspend fun refreshUpdateConfig() {
        try {
            Log.d(TAG, "Force refreshing update config...")
            val freshConfig = fetchUpdateConfig()
            if (freshConfig != null) {
                updateConfig = freshConfig
                configUpdated = !configUpdated // Trigger recomposition
                Log.d(TAG, "Update config refreshed: $freshConfig")
            } else {
                Log.w(TAG, "Failed to refresh update config")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing update config", e)
        }
    }
    
    
    /**
     * Gets current app version from package manager
     */
    fun getCurrentAppVersion(activity: Activity): String {
        return try {
            val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app version", e)
            "0.0.0"
        }
    }
    
    /**
     * Checks if update dialog should be shown based on version comparison
     */
    fun shouldShowUpdateDialog(currentVersion: String, remoteVersion: String): Boolean {
        return try {
            Log.d(TAG, "Version comparison: Current='$currentVersion', Remote='$remoteVersion'")
            
            // Show dialog only if current version is different from remote version
            // If versions are same, no dialog should show
            val shouldShow = currentVersion != remoteVersion
            
            if (shouldShow) {
                Log.d(TAG, "✅ UPDATE NEEDED: Current version ($currentVersion) is different from latest ($remoteVersion)")
            } else {
                Log.d(TAG, "✅ NO UPDATE NEEDED: Current version ($currentVersion) matches latest ($remoteVersion)")
            }
            
            return shouldShow
        } catch (e: Exception) {
            Log.e(TAG, "Error comparing versions", e)
            false
        }
    }
    
    /**
     * Fetches update configuration from remote JSON
     */
    private suspend fun fetchUpdateConfig(): UpdateConfig? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonString = URL(CONFIG_URL).readText()
                Json.decodeFromString<UpdateConfig>(jsonString)
            } catch (e: IOException) {
                Log.e(TAG, "Network error fetching update config", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing update config", e)
                null
            }
        }
    }
    
    /**
     * Opens the update link in browser or Play Store
     */
    fun openUpdateLink(activity: Activity, updateLink: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateLink))
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening update link", e)
        }
    }
}


/**
 * Extension function to easily check for updates from any Activity
 */
suspend fun Activity.checkForUpdate() {
    UpdateManager.checkForUpdate(this)
}

