package com.musifly.android.ui.component

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.musifly.android.R

import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey

/**
 * Professional Storage Permission Dialog
 * Shows on first app launch with a safe, reassuring message
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoragePermissionDialog(
    onDismiss: () -> Unit,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    
    // Track permission request state to prevent re-triggering
    var permissionRequested by remember { mutableStateOf(false) }
    
    // Function to check current permission status
    fun checkStoragePermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ (API 33+)
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-12 (API 23-32)
                val readGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                
                val writeGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    // Android 9 and below - WRITE permission is required
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    // Android 10-12 - WRITE permission not needed with scoped storage
                    true
                }
                
                readGranted && writeGranted
            }
            else -> true // Android 5 and below - permissions granted at install
        }
    }
    
    // Check initial permission status
    val hasStoragePermission = remember { checkStoragePermission() }
    
    // Track permission state dynamically
    var currentPermissionState by remember { mutableStateOf(hasStoragePermission) }
    
    // Permission launcher for Android 13+
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionRequested = false // Reset for next potential request
        val allGranted = permissions.values.all { it }
        currentPermissionState = checkStoragePermission() // Re-check actual permission state
        if (allGranted) {
            onPermissionGranted()
            onDismiss()
        }
        // If denied, dialog stays visible for user to try again
    }
    
    // Permission launcher for Android 6-12
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionRequested = false // Reset for next potential request
        val allGranted = permissions.values.all { it }
        currentPermissionState = checkStoragePermission() // Re-check actual permission state
        if (allGranted) {
            onPermissionGranted()
            onDismiss()
        }
        // If denied, dialog stays visible for user to try again
    }
    
    // If already granted, dismiss immediately and mark as completed
    LaunchedEffect(hasStoragePermission) {
        if (hasStoragePermission) {
            currentPermissionState = true
            onPermissionGranted()
            onDismiss()
        }
    }
    
    // Show dialog only if permission is not granted
    // Dialog will stay visible until permission is granted
    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    if (!currentPermissionState) {
    if (isLiquidGlass) {
        LiquidGlassDialog(
            onDismiss = { /* No dismiss */ },
            icon = {
                 Icon(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                 Text(
                    text = "Storage Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🔒 Your Privacy is Protected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Text(
                                text = "We need storage permission to:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Column(
                                modifier = Modifier.padding(start = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PermissionItem("📥", "Save downloaded music and videos to your device")
                                PermissionItem("🎵", "Access your music library for offline playback")
                                PermissionItem("💾", "Manage your downloaded content safely")
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Text(
                                text = "✅ We only access files you download or select. Your personal files remain private and secure.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Text(
                        text = "This permission is essential for the app to function properly. You can change this later in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            buttons = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (permissionRequested) return@Button
                            permissionRequested = true
                            
                            when {
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                                    mediaPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_MEDIA_AUDIO,
                                            Manifest.permission.READ_MEDIA_VIDEO
                                        )
                                    )
                                }
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                                    val permissions = mutableListOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    }
                                    storagePermissionLauncher.launch(permissions.toTypedArray())
                                }
                                else -> {
                                    onPermissionGranted()
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Allow Storage Access", fontWeight = FontWeight.SemiBold)
                    }
                    
                    TextButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Maybe Later")
                    }
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = {
                // Don't allow dismissing by clicking outside
                // User must either grant permission or click "Maybe Later"
            },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Storage Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Professional, safe message
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🔒 Your Privacy is Protected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Text(
                                text = "We need storage permission to:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Column(
                                modifier = Modifier.padding(start = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PermissionItem(
                                    icon = "📥",
                                    text = "Save downloaded music and videos to your device"
                                )
                                PermissionItem(
                                    icon = "🎵",
                                    text = "Access your music library for offline playback"
                                )
                                PermissionItem(
                                    icon = "💾",
                                    text = "Manage your downloaded content safely"
                                )
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            Text(
                                text = "✅ We only access files you download or select. Your personal files remain private and secure.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Text(
                        text = "This permission is essential for the app to function properly. You can change this later in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Prevent duplicate permission requests
                        if (permissionRequested) return@Button
                        permissionRequested = true
                        
                        when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                                // Android 13+ (API 33+)
                                mediaPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_MEDIA_AUDIO,
                                        Manifest.permission.READ_MEDIA_VIDEO
                                    )
                                )
                            }
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                                // Android 6-12 (API 23-32)
                                val permissions = mutableListOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                // Add WRITE permission for Android 9 and below
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                                storagePermissionLauncher.launch(permissions.toTypedArray())
                            }
                            else -> {
                                // Android 5 and below - permissions granted at install
                                onPermissionGranted()
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Allow Storage Access", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // User clicked "Maybe Later" - dismiss dialog but don't mark as completed
                        // Dialog will show again next time app opens if permission still not granted
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Maybe Later")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
    }
}

@Composable
private fun PermissionItem(
    icon: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

