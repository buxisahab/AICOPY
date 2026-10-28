package com.musifly.android.utils.update

import org.json.JSONObject

/**
 * Data class representing the update/notification JSON structure
 * 
 * This class parses and holds all fields from the GitHub RAW JSON response
 */
data class UpdateData(
    // Version info
    val latestVersionCode: Int,
    val latestVersionName: String,
    
    // Update dialog info
    val updateTitle: String,
    val updateDescription: String,
    val downloadUrl: String,
    val forceUpdate: Boolean,
    
    // Notification info
    val notificationEnabled: Boolean,
    val notificationTitle: String,
    val notificationMessage: String,
    val notificationLink: String,
    val notificationSound: String, // Custom sound URI or raw resource name (optional)
    
    // Notification control
    val notificationIntervalDays: Int,
    val maxNotificationsPerDay: Int,
    
    // Message tracking
    val messageId: String,
    val autoPush: Boolean
) {
    companion object {
        /**
         * Parse JSON string into UpdateData object
         * Uses safe defaults if fields are missing
         */
        fun fromJson(jsonString: String): UpdateData? {
            return try {
                android.util.Log.d("UpdateData", "Parsing JSON: ${jsonString.take(500)}...")
                val json = JSONObject(jsonString)
                
                // Parse version code - check multiple possible field names
                val versionCode = when {
                    json.has("latest_version_code") -> json.getInt("latest_version_code")
                    json.has("version_code") -> json.getInt("version_code")
                    json.has("versionCode") -> json.getInt("versionCode")
                    else -> {
                        android.util.Log.w("UpdateData", "⚠️ No version_code field found in JSON, trying optInt with default 0")
                        json.optInt("latest_version_code", 0)
                    }
                }
                
                // Parse version name - check multiple possible field names
                val versionName = when {
                    json.has("latest_version_name") -> json.getString("latest_version_name")
                    json.has("version_name") -> json.getString("version_name")
                    json.has("versionName") -> json.getString("versionName")
                    json.has("version") -> json.getString("version")
                    else -> {
                        android.util.Log.w("UpdateData", "⚠️ No version_name field found in JSON, using empty string")
                        json.optString("latest_version_name", "")
                    }
                }
                
                android.util.Log.d("UpdateData", "📦 Parsed version code: $versionCode")
                android.util.Log.d("UpdateData", "📦 Parsed version name: $versionName")
                
                val updateData = UpdateData(
                    latestVersionCode = versionCode,
                    latestVersionName = versionName,
                    
                    updateTitle = json.optString("update_title", "Update Available"),
                    updateDescription = json.optString("update_description", "A new version is available."),
                    downloadUrl = json.optString("download_url", ""),
                    forceUpdate = json.optBoolean("force_update", false),
                    
                    notificationEnabled = json.optBoolean("notification_enabled", false),
                    notificationTitle = json.optString("notification_title", "Update Available"),
                    notificationMessage = json.optString("notification_message", "Tap to see what's new!"),
                    notificationLink = json.optString("notification_link", ""),
                    notificationSound = json.optString("notification_sound", ""), // Optional: raw resource name or URI
                    
                    notificationIntervalDays = json.optInt("notification_interval_days", 1).coerceAtLeast(1),
                    maxNotificationsPerDay = json.optInt("max_notifications_per_day", 5).coerceAtLeast(1),
                    
                    messageId = json.optString("message_id", ""),
                    autoPush = json.optBoolean("auto_push", true)
                )
                
                android.util.Log.d("UpdateData", "✅ JSON parsed successfully:")
                android.util.Log.d("UpdateData", "   Version: ${updateData.latestVersionName} (${updateData.latestVersionCode})")
                android.util.Log.d("UpdateData", "   Update Title: ${updateData.updateTitle}")
                android.util.Log.d("UpdateData", "   Download URL: ${updateData.downloadUrl}")
                android.util.Log.d("UpdateData", "   Force Update: ${updateData.forceUpdate}")
                android.util.Log.d("UpdateData", "   Notification enabled: ${updateData.notificationEnabled}")
                android.util.Log.d("UpdateData", "   Message ID: ${updateData.messageId}")
                
                updateData
            } catch (e: Exception) {
                android.util.Log.e("UpdateData", "❌ Failed to parse JSON: ${e.message}", e)
                android.util.Log.e("UpdateData", "JSON content: ${jsonString.take(1000)}")
                e.printStackTrace()
                null
            }
        }
    }
}
