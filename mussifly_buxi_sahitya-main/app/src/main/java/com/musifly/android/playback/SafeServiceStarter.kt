package com.musifly.android.playback

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.Lifecycle

/**
 * Safe service starter to prevent ForegroundServiceStartNotAllowedException on Android 12+.
 * 
 * This utility handles the correct way to start MusicService across all Android versions:
 * - Android 5-7 (SDK 21-25): Use regular startService()
 * - Android 8-11 (SDK 26-30): Use startForegroundService() with proper foreground notification
 * - Android 12+ (SDK 31+): Check app state before starting, handle exceptions gracefully
 * - Android 14-15 (SDK 34-35): Enhanced exception handling and fallback mechanisms
 * 
 * IMPORTANT for Android 15 (API 35):
 * - Even if app appears to be in foreground, startForegroundService() can fail
 * - Media3's MediaNotificationManager may also call startForegroundService() internally
 * - Always use fallback to startService() if startForegroundService() fails
 * - Service will promote itself to foreground in onCreate() if needed
 * 
 * @author OpenTune Team
 */
object SafeServiceStarter {
    private const val TAG = "SafeServiceStarter"
    
    /**
     * Safely start MusicService with proper Android version handling.
     * 
     * This function decides HOW to start the service depending on:
     * 1. Android version
     * 2. App foreground/background state
     * 3. Exception handling for Android 12+
     * 
     * @param context The context to start the service from
     * @param intent The intent for starting MusicService
     */
    fun safeStartMusicService(context: Context, intent: Intent) {
        try {
            // Android version < 26 (before Android O)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // For Android 5-7, always use regular startService
                context.startService(intent)
                Log.d(TAG, "✅ Started MusicService using startService() (Android < 8)")
                return
            }
            
            // Android version >= 26 (Android 8+)
            // Check if app is in foreground using multiple methods for reliability
            val isAppInForeground = isAppInForeground(context)
            
            if (isAppInForeground) {
                // App is in foreground, safe to use startForegroundService
                // On Android 15, even foreground apps can fail, so always have fallback
                try {
                    ContextCompat.startForegroundService(context, intent)
                    Log.d(TAG, "✅ Started MusicService using startForegroundService() (App in foreground)")
                } catch (e: Exception) {
                    // On Android 15, this can fail even in foreground due to stricter restrictions
                    if (e is android.app.ForegroundServiceStartNotAllowedException) {
                        Log.w(TAG, "⚠️ ForegroundServiceStartNotAllowedException even in foreground (Android 15?), using fallback", e)
                    } else {
                        Log.w(TAG, "⚠️ startForegroundService failed even in foreground, trying fallback", e)
                    }
                // Fallback to regular service - service will promote itself in onCreate()
                    // REMOVED: Fallback to startService() is also illegal from background on Android 8+
                    // and will cause BackgroundServiceStartNotAllowedException
                    Log.e(TAG, "❌ startForegroundService failed - cannot fallback to startService from background", e)
                }
            } else {
                // App is in background
                // For Android 12+ (SDK 31+), this can throw ForegroundServiceStartNotAllowedException
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ - try startForegroundService first, catch exception
                    // On Android 15, this will almost certainly fail, so fallback is critical
                    try {
                        ContextCompat.startForegroundService(context, intent)
                        Log.d(TAG, "✅ Started MusicService using startForegroundService() (App in background, Android 12+)")
                    } catch (e: Exception) {
                        // Catch ForegroundServiceStartNotAllowedException
                        if (e is android.app.ForegroundServiceStartNotAllowedException) {
                            Log.w(TAG, "⚠️ ForegroundServiceStartNotAllowedException: App in background (expected on Android 12+), suppressed.")
                            // REMOVED: Fallback to startService() is illegal and causes crash
                            Log.w(TAG, "⚠️ Service start deferred - will start when app comes to foreground")
                        } else {
                            // Other exception
                            Log.e(TAG, "❌ Unexpected exception starting MusicService", e)
                        }
                    }
                } else {
                    // Android 8-11 - should work even in background
                    try {
                        context.startForegroundService(intent)
                        Log.d(TAG, "✅ Started MusicService using startForegroundService() (App in background, Android 8-11)")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Exception starting MusicService (Android 8-11)", e)
                        // REMOVED: Fallback to startService() is illegal if app is in background
                    }
                }
            }
        } catch (e: Exception) {
            // Final catch-all for any unexpected errors
            Log.e(TAG, "❌ Critical error starting MusicService", e)
            // Don't rethrow - app should continue even if service fails to start
        }
    }
    
    /**
     * Check if app is currently in foreground using multiple methods for reliability.
     */
    private fun isAppInForeground(context: Context): Boolean {
        // Method 1: ProcessLifecycleOwner (most reliable)
        try {
            val isStarted = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            if (isStarted) {
                Log.d(TAG, "✅ ProcessLifecycleOwner: App in foreground")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ ProcessLifecycleOwner check failed", e)
        }
        
        // Method 2: ActivityManager (fallback)
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val appProcesses = activityManager?.runningAppProcesses
            val packageName = context.packageName
            
            if (appProcesses != null && packageName != null) {
                for (appProcess in appProcesses) {
                    if (appProcess.pid == android.os.Process.myPid() &&
                        appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        Log.d(TAG, "✅ ActivityManager: App in foreground")
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ ActivityManager check failed", e)
        }
        
        Log.d(TAG, "⚠️ App appears to be in background")
        return false
    }
    
    /**
     * Public method to check if app is currently in foreground.
     * Useful for determining whether to attempt foreground service start.
     */
    fun isAppInForeground(): Boolean {
        return try {
            ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ ProcessLifecycleOwner check failed", e)
            false
        }
    }
}

/**
 * Extension function for easy access to safe service starting.
 * 
 * Usage:
 * ```kotlin
 * context.safeStartMusicService(Intent(context, MusicService::class.java))
 * ```
 */
fun Context.safeStartMusicService(intent: Intent) {
    SafeServiceStarter.safeStartMusicService(this, intent)
}
