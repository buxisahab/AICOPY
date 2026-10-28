package com.musifly.android.utils.update

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences helper for storing update and notification related data
 * 
 * Stores:
 * - lastMessageId: Last notification message ID received
 * - lastNotificationTime: Timestamp of last notification sent
 * - notificationsSentToday: Count of notifications sent today
 * - lastResetDay: Day when counter was last reset (for daily limit)
 * - lastVersionNotified: Last version code that was shown in update dialog
 * - cachedJsonString: Cached JSON response for offline support
 */
object UpdatePrefs {
    private const val PREFS_NAME = "musifly_update_prefs"
    
    // Preference keys
    private const val KEY_LAST_MESSAGE_ID = "last_message_id"
    private const val KEY_LAST_NOTIFICATION_TIME = "last_notification_time"
    private const val KEY_NOTIFICATIONS_SENT_TODAY = "notifications_sent_today"
    private const val KEY_LAST_RESET_DAY = "last_reset_day"
    private const val KEY_LAST_VERSION_NOTIFIED = "last_version_notified"
    private const val KEY_CACHED_JSON = "cached_json_string"
    private const val KEY_LAST_NOTIFICATION_CONTENT_HASH = "last_notification_content_hash"
    
    /**
     * Get SharedPreferences instance
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get last message ID that was processed
     */
    fun getLastMessageId(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_MESSAGE_ID, null)
    }
    
    /**
     * Save last message ID
     */
    fun setLastMessageId(context: Context, messageId: String) {
        getPrefs(context).edit().putString(KEY_LAST_MESSAGE_ID, messageId).apply()
    }
    
    /**
     * Get timestamp of last notification sent (milliseconds since epoch)
     */
    fun getLastNotificationTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_NOTIFICATION_TIME, 0L)
    }
    
    /**
     * Save timestamp of last notification
     */
    fun setLastNotificationTime(context: Context, timestamp: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_NOTIFICATION_TIME, timestamp).apply()
    }
    
    /**
     * Get count of notifications sent today
     */
    fun getNotificationsSentToday(context: Context): Int {
        return getPrefs(context).getInt(KEY_NOTIFICATIONS_SENT_TODAY, 0)
    }
    
    /**
     * Increment notification count for today
     */
    fun incrementNotificationsSentToday(context: Context) {
        val currentCount = getNotificationsSentToday(context)
        getPrefs(context).edit().putInt(KEY_NOTIFICATIONS_SENT_TODAY, currentCount + 1).apply()
    }
    
    /**
     * Get the day when counter was last reset (format: YYYY-MM-DD)
     */
    fun getLastResetDay(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_RESET_DAY, "") ?: ""
    }
    
    /**
     * Set the reset day
     */
    fun setLastResetDay(context: Context, day: String) {
        getPrefs(context).edit().putString(KEY_LAST_RESET_DAY, day).apply()
    }
    
    /**
     * Reset daily notification counter
     */
    fun resetDailyCounter(context: Context, currentDay: String) {
        getPrefs(context).edit()
            .putInt(KEY_NOTIFICATIONS_SENT_TODAY, 0)
            .putString(KEY_LAST_RESET_DAY, currentDay)
            .apply()
    }
    
    /**
     * Get last version code that was shown in update dialog
     */
    fun getLastVersionNotified(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_VERSION_NOTIFIED, 0)
    }
    
    /**
     * Save last version code that was shown
     */
    fun setLastVersionNotified(context: Context, versionCode: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_VERSION_NOTIFIED, versionCode).apply()
    }
    
    /**
     * Get cached JSON string
     */
    fun getCachedJson(context: Context): String? {
        return getPrefs(context).getString(KEY_CACHED_JSON, null)
    }
    
    /**
     * Save cached JSON string
     */
    fun setCachedJson(context: Context, jsonString: String) {
        getPrefs(context).edit().putString(KEY_CACHED_JSON, jsonString).apply()
    }
    
    /**
     * Get last notification content hash (for detecting content changes)
     */
    fun getLastNotificationContentHash(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_NOTIFICATION_CONTENT_HASH, null)
    }
    
    /**
     * Save last notification content hash
     */
    fun setLastNotificationContentHash(context: Context, hash: String) {
        getPrefs(context).edit().putString(KEY_LAST_NOTIFICATION_CONTENT_HASH, hash).apply()
    }
    
    /**
     * Clear all update preferences (for testing/reset)
     */
    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
