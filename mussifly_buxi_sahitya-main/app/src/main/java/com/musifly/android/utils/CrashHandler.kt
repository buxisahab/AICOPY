package com.musifly.android.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Process
import android.util.Log
import com.musifly.android.ui.CrashActivity
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()
    
    companion object {
        private const val TAG = "CrashHandler"
        private const val PREFS_NAME = "crash_guard"
        private const val KEY_CRASH_COUNT = "crash_count"
        private const val KEY_LAST_CRASH_TIME = "last_crash_time"
        private const val MAX_CRASHES = 3
        private const val CRASH_RESET_TIME_MS = 60_000L // 1 minute
    }
    
    /**
     * Get crash guard preferences (safe, no Firebase/Media dependencies)
     */
    private fun getCrashGuardPrefs(): SharedPreferences {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get crash guard prefs", e)
            // Return a no-op SharedPreferences if we can't get real one
            object : SharedPreferences {
                override fun getAll(): Map<String, *> = emptyMap<String, Any?>()
                override fun getString(key: String, defValue: String?) = defValue
                override fun getStringSet(key: String, defValues: Set<String>?) = defValues
                override fun getInt(key: String, defValue: Int) = defValue
                override fun getLong(key: String, defValue: Long) = defValue
                override fun getFloat(key: String, defValue: Float) = defValue
                override fun getBoolean(key: String, defValue: Boolean) = defValue
                override fun contains(key: String) = false
                override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
                    override fun putString(key: String, value: String?) = this
                    override fun putStringSet(key: String, values: Set<String>?) = this
                    override fun putInt(key: String, value: Int) = this
                    override fun putLong(key: String, value: Long) = this
                    override fun putFloat(key: String, value: Float) = this
                    override fun putBoolean(key: String, value: Boolean) = this
                    override fun remove(key: String) = this
                    override fun clear() = this
                    override fun commit() = true
                    override fun apply() {}
                }
                override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
                override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
            }
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Check if we're in a crash loop
            val prefs = getCrashGuardPrefs()
            val now = System.currentTimeMillis()
            val lastCrashTime = prefs.getLong(KEY_LAST_CRASH_TIME, 0)
            val crashCount = prefs.getInt(KEY_CRASH_COUNT, 0)
            
            // Reset crash count if enough time has passed
            val newCrashCount = if (now - lastCrashTime > CRASH_RESET_TIME_MS) {
                1
            } else {
                crashCount + 1
            }
            
            // Update crash count
            try {
                prefs.edit()
                    .putInt(KEY_CRASH_COUNT, newCrashCount)
                    .putLong(KEY_LAST_CRASH_TIME, now)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update crash count", e)
            }
            
            // If too many crashes, just exit without showing UI
            if (newCrashCount > MAX_CRASHES) {
                Log.e(TAG, "Too many crashes ($newCrashCount), exiting without UI to prevent loop")
                Process.killProcess(Process.myPid())
                exitProcess(10)
                return
            }
            
            val stringWriter = StringWriter()
            val printWriter = PrintWriter(stringWriter)
            throwable.printStackTrace(printWriter)
            val stackTrace = stringWriter.toString()

            val intent = Intent(context, CrashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra("stack_trace", stackTrace)
            }
            
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start CrashActivity", e)
                // If we can't start CrashActivity, just exit
                Process.killProcess(Process.myPid())
                exitProcess(10)
                return
            }

            // Give UI time to show, then kill process
            Thread.sleep(100)
            Process.killProcess(Process.myPid())
            exitProcess(10)
        } catch (e: Throwable) {
            // If crash handler itself crashes, use default handler
            Log.e(TAG, "CrashHandler failed", e)
            defaultHandler?.uncaughtException(thread, throwable)
            try {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            } catch (e2: Throwable) {
                // Last resort: system exit
                System.exit(10)
            }
        }
    }
}
