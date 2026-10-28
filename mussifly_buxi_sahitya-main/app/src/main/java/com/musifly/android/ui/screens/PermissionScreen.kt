package com.musifly.android.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.musifly.android.R
import com.musifly.android.utils.PermissionManager

// Beats-Music theme colors (Spotify-style)
private val BeatsThemeColor = Color(0xFF000000) // Pure black background
private val BeatsAccentColor = Color(0xFF1DB954) // Spotify green
private val BeatsPrimaryText = Color(0xFFFFFFFF) // Pure white
private val BeatsSecondaryText = Color(0xFFB3B3B3) // Light gray (white70)

@Composable
fun PermissionScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPrefs = remember { 
        context.getSharedPreferences("musifly_prefs", android.content.Context.MODE_PRIVATE) 
    }
    
    var storageGranted by remember { 
        mutableStateOf(PermissionManager.hasStoragePermission(context))
    }
    
    var batteryGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) 
                    as android.os.PowerManager
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        )
    }
    
    // Check permissions on launch
    LaunchedEffect(Unit) {
        storageGranted = PermissionManager.hasStoragePermission(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) 
                as android.os.PowerManager
            batteryGranted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
    }
    
    // Storage permission launcher
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        storageGranted = isGranted || PermissionManager.hasStoragePermission(context)
    }
    
    // Battery optimization launcher
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Re-check battery optimization status after returning
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) 
                as android.os.PowerManager
            batteryGranted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
    }
    
    fun requestStoragePermission() {
        val permission = PermissionManager.getStoragePermission()
        storagePermissionLauncher.launch(permission)
    }
    
    fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            try {
                batteryOptimizationLauncher.launch(intent)
            } catch (e: Exception) {
                // Fallback to app settings
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS.also { action ->
                    val settingsIntent = Intent(action).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(settingsIntent)
                }
            }
        }
    }
    
    fun continueToLogin() {
        if (storageGranted) {
            // Save that permissions have been seen
            sharedPrefs.edit().putBoolean("seen_permission", true).apply()
            
            navController.navigate("google_login") {
                popUpTo("permission") { inclusive = true }
            }
        } else {
            // Show snackbar that storage permission is required (Beats-Music style)
            // Note: You might want to show a Snackbar here using Scaffold's snackbarHostState
        }
    }
    
    // Prevent back navigation on permission screen (Beats-Music style)
    BackHandler(enabled = true) {
        // Block back navigation - user must complete permissions
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BeatsThemeColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .safeDrawingPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .background(BeatsThemeColor),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // Shield icon (Beats-Music style)
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = BeatsAccentColor
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = "Permissions",
                    style = androidx.compose.ui.text.TextStyle(
                        color = BeatsPrimaryText,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Subtitle
                Text(
                    text = "To provide the best music experience, we need a few permissions.",
                    style = androidx.compose.ui.text.TextStyle(
                        color = BeatsSecondaryText,
                        fontSize = 16.sp
                    )
                )
                
                Spacer(modifier = Modifier.height(40.dp))
                
                // Storage Permission Tile
                PermissionTile(
                    icon = Icons.Default.MusicNote,
                    title = "Music & Audio",
                    subtitle = "To access and play your local music files.",
                    isGranted = storageGranted,
                    onTap = { 
                        if (!storageGranted) {
                            requestStoragePermission()
                        } else {
                            // Open settings if needed
                            PermissionManager.openAppSettings(context)
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Battery Optimization Tile
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PermissionTile(
                        icon = Icons.Default.BatteryFull,
                        title = "Battery",
                        subtitle = "Disable optimization for uninterrupted background play.",
                        isGranted = batteryGranted,
                        onTap = { 
                            if (!batteryGranted) {
                                requestBatteryOptimization()
                            } else {
                                // Open battery settings
                                try {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    PermissionManager.openAppSettings(context)
                                }
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Continue Button (Beats-Music style - green background)
                Button(
                    onClick = { continueToLogin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BeatsAccentColor,
                        contentColor = BeatsThemeColor
                    ),
                    enabled = storageGranted
                ) {
                    Text(
                        text = "Continue",
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BeatsThemeColor
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun PermissionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted, onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = if (isGranted) {
            BorderStroke(
                width = 1.dp,
                color = BeatsAccentColor
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                // Icon container
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isGranted) {
                                BeatsAccentColor.copy(alpha = 0.2f)
                            } else {
                                Color.White.copy(alpha = 0.1f)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isGranted) {
                            BeatsAccentColor
                        } else {
                            BeatsPrimaryText
                        }
                    )
                }
                
                // Text content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = androidx.compose.ui.text.TextStyle(
                            color = BeatsPrimaryText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = subtitle,
                        style = androidx.compose.ui.text.TextStyle(
                            color = BeatsSecondaryText,
                            fontSize = 12.sp
                        )
                    )
                }
                
                // Right side indicator
                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Granted",
                        tint = BeatsAccentColor
                    )
                } else {
                    Text(
                        text = "Allow",
                        style = androidx.compose.ui.text.TextStyle(
                            color = BeatsAccentColor,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
