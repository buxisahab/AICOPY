package com.musifly.android.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musifly.android.ui.theme.OpenTuneTheme
import com.musifly.android.BuildConfig
import android.util.Log

class CrashActivity : ComponentActivity() {
    companion object {
        private const val TAG = "CrashActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // CRITICAL: This is the crash_handler process - DO NOT initialize anything heavy
        // No Firebase, Firestore, Media, Audio, or background threads
        
        try {
            super.onCreate(savedInstanceState)
            
            // Get stack trace safely
            val stackTrace = try {
                intent?.getStringExtra("stack_trace") ?: "No stack trace available."
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get stack trace", e)
                "Failed to retrieve stack trace."
            }

            // Use minimal UI to prevent further crashes
            // Wrap in try-catch at Activity level (before setContent)
            try {
                setContent {
                    SafeCrashScreen(
                        stackTrace = stackTrace,
                        onSendReport = {
                            try {
                                sendCrashReport(stackTrace)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to send crash report", e)
                            }
                        }
                    )
                }
            } catch (e: Throwable) {
                // If setContent itself fails, use fallback TextView
                Log.e(TAG, "setContent failed in CrashActivity", e)
                try {
                    setContentView(android.widget.TextView(this@CrashActivity).apply {
                        text = "App crashed. Please restart."
                        setPadding(32, 32, 32, 32)
                    })
                } catch (e2: Throwable) {
                    // Even TextView failed, just log and exit
                    Log.e(TAG, "Even TextView failed", e2)
                    finish()
                }
            }
        } catch (e: Throwable) {
            // If onCreate itself crashes, just exit
            Log.e(TAG, "CrashActivity.onCreate failed", e)
            try {
                finish()
            } catch (e2: Throwable) {
                // finish() failed too
            }
            try {
                android.os.Process.killProcess(android.os.Process.myPid())
            } catch (e2: Throwable) {
                // killProcess failed, use System.exit
                System.exit(10)
            }
        }
    }

    private fun sendCrashReport(stackTrace: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("report.musifly@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Mussifly Crash Report - v${BuildConfig.VERSION_NAME}")
                putExtra(Intent.EXTRA_TEXT, "App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nDevice Info:\n${getDeviceInfo()}\n\nStack Trace:\n$stackTrace")
            }
            startActivity(Intent.createChooser(intent, "Send Crash Report"))
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to send crash report", e)
            // Don't crash if sending report fails
        }
    }

    private fun getDeviceInfo(): String {
        return "App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                "Brand: ${android.os.Build.BRAND}\n" +
                "Model: ${android.os.Build.MODEL}\n" +
                "Android Version: ${android.os.Build.VERSION.RELEASE}\n" +
                "SDK: ${android.os.Build.VERSION.SDK_INT}"
    }
}

/**
 * Safe wrapper composable that handles errors internally
 * This prevents crashes if Compose itself fails
 */
@Composable
fun SafeCrashScreen(stackTrace: String, onSendReport: () -> Unit) {
    // Use a simple error boundary approach
    // If OpenTuneTheme fails, we'll catch it at the Activity level
    OpenTuneTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            CrashScreen(stackTrace = stackTrace, onSendReport = onSendReport)
        }
    }
}

@Composable
fun CrashScreen(stackTrace: String, onSendReport: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Oops! Something went wrong.",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "The application has crashed. Please send us the crash report to help us fix the issue.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSendReport) {
            Text("Send Crash Report")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stackTrace,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}
