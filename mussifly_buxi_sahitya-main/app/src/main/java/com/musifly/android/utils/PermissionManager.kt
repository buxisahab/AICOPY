package com.musifly.android.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Utility class for handling storage permissions across all Android versions
 * 
 * Android 13+ (API 33+): READ_MEDIA_AUDIO
 * Android 12 and below: READ_EXTERNAL_STORAGE
 */
object PermissionManager {
    
    /**
     * Get the required storage permission based on Android version
     */
    fun getStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    
    /**
     * Check if storage permission is granted
     */
    fun hasStoragePermission(context: Context): Boolean {
        val permission = getStoragePermission()
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request storage permission using modern ActivityResultLauncher
     */
    fun requestStoragePermission(launcher: ActivityResultLauncher<String>) {
        launcher.launch(getStoragePermission())
    }
    
    /**
     * Check if we should show permission rationale
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        val permission = getStoragePermission()
        return activity.shouldShowRequestPermissionRationale(permission)
    }
    
    /**
     * Open app settings page
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * Get user-friendly permission name for display
     */
    fun getPermissionName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "Audio Files Access"
        } else {
            "Storage Access"
        }
    }
    
    /**
     * Get permission rationale message
     */
    fun getPermissionRationale(): String {
        return "Musifly needs access to your audio files to play local music. " +
                "This permission allows the app to scan and play music stored on your device."
    }
    
    /**
     * All permissions needed for the app (can be extended)
     */
    fun getAllRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        return getAllRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
