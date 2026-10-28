package com.musifly.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.musifly.android.download.*
import com.musifly.android.ui.component.AppButton
import com.musifly.android.ui.component.AppIconButton
import androidx.compose.ui.graphics.vector.rememberVectorPainter

/**
 * Advanced Downloads Screen - UI for advanced download features
 * 
 * Shows:
 * - Batch download controls
 * - Speed limiter settings
 * - Download statistics
 * - Scheduled downloads
 * - Smart resume settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedDownloadsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // State
    var selectedTab by remember { mutableStateOf(0) }
    var showSpeedLimitDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced Downloads") },
                navigationIcon = {
                    AppIconButton(
                        onClick = onNavigateBack,
                        painter = rememberVectorPainter(Icons.Default.ArrowBack),
                        contentDescription = "Back"
                    )
                },
                actions = {
                    AppIconButton(
                        onClick = { showStatsDialog = true },
                        painter = rememberVectorPainter(Icons.Default.Analytics),
                        contentDescription = "Statistics"
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Batch") },
                    icon = { Icon(Icons.Default.Download, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Speed") },
                    icon = { Icon(Icons.Default.Speed, null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Schedule") },
                    icon = { Icon(Icons.Default.Schedule, null) }
                )
            }
            
            // Tab Content
            when (selectedTab) {
                0 -> BatchDownloadTab()
                1 -> SpeedLimiterTab { showSpeedLimitDialog = true }
                2 -> ScheduleTab { showScheduleDialog = true }
            }
        }
        
        // Dialogs
        if (showSpeedLimitDialog) {
            SpeedLimitDialog(onDismiss = { showSpeedLimitDialog = false })
        }
        
        if (showScheduleDialog) {
            ScheduleDialog(onDismiss = { showScheduleDialog = false })
        }
        
        if (showStatsDialog) {
            StatisticsDialog(onDismiss = { showStatsDialog = false })
        }
    }
}

/**
 * Batch Download Tab
 */
@Composable
fun BatchDownloadTab() {
    var batchStats by remember { mutableStateOf<BatchDownloadManager.BatchStats?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Batch Queue Status
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Batch Queue",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                batchStats?.let { stats ->
                    BatchStatRow("Total", stats.total.toString())
                    BatchStatRow("Queued", stats.queued.toString())
                    BatchStatRow("Active", stats.active.toString())
                    BatchStatRow("Completed", stats.completed.toString())
                    BatchStatRow("Failed", stats.failed.toString())
                    
                    if (stats.total > 0) {
                        LinearProgressIndicator(
                            progress = stats.completedPercentage / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        )
                        
                        Text(
                            text = "${stats.completedPercentage.toInt()}% Complete",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } ?: run {
                    Text(
                        text = "No active batch downloads",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Batch Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppButton(
                onClick = { /* Pause batch */ },
                modifier = Modifier.weight(1f),
                isPrimary = false
            ) {
                Icon(Icons.Default.Pause, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pause All")
            }
            
            AppButton(
                onClick = { /* Resume batch */ },
                modifier = Modifier.weight(1f),
                isPrimary = true
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Resume All")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = { /* Cancel batch */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Cancel, null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Cancel All")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Priority Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "ℹ️ Batch Download Features",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
                        • Downloads up to 3 songs simultaneously
                        • Priority-based queue (URGENT > HIGH > NORMAL > LOW)
                        • Automatic retry on failure (3 attempts)
                        • Exponential backoff between retries
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun BatchStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Speed Limiter Tab
 */
@Composable
fun SpeedLimiterTab(onShowDialog: () -> Unit) {
    var currentSpeed by remember { mutableStateOf("Unlimited") }
    var currentLimit by remember { mutableStateOf("Unlimited") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Current Speed
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Current Speed",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = currentSpeed,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Speed Limit: $currentLimit",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Speed Limit Button
        AppButton(
            onClick = onShowDialog,
            modifier = Modifier.fillMaxWidth(),
            isPrimary = true
        ) {
            Icon(Icons.Default.Settings, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Change Speed Limit")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Speed Limit Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "ℹ️ Speed Limiting Benefits",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
                        • Prevents bandwidth hogging
                        • Allows other apps to use network
                        • Saves battery on mobile data
                        • Smooth token bucket algorithm
                        • Real-time speed monitoring
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Schedule Tab
 */
@Composable
fun ScheduleTab(onShowDialog: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Scheduled Downloads List
        Text(
            text = "Scheduled Downloads",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "No scheduled downloads",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Schedule New Download
        AppButton(
            onClick = onShowDialog,
            modifier = Modifier.fillMaxWidth(),
            isPrimary = true
        ) {
            Icon(Icons.Default.Schedule, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Schedule Download")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick Schedule Buttons
        Text(
            text = "Quick Schedule",
            style = MaterialTheme.typography.titleSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppButton(
                onClick = { /* Schedule for tonight */ },
                modifier = Modifier.weight(1f),
                isPrimary = false
            ) {
                Text("Tonight 2 AM")
            }
            
            AppButton(
                onClick = { /* Schedule for tomorrow */ },
                modifier = Modifier.weight(1f),
                isPrimary = false
            ) {
                Text("Tomorrow 6 AM")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Schedule Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "ℹ️ Smart Scheduling Features",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = """
                        • Schedule for specific time
                        • WiFi-only downloads
                        • Battery threshold check
                        • Recurring schedules (daily/weekly/monthly)
                        • Automatic alarm management
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Speed Limit Dialog
 */
@Composable
fun SpeedLimitDialog(onDismiss: () -> Unit) {
    val speedOptions = SpeedLimiter.getSpeedLimitOptions()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Speed Limit") },
        text = {
            LazyColumn {
                items(speedOptions) { (label, value) ->
                    TextButton(
                        onClick = {
                            // Set speed limit
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Schedule Dialog
 */
@Composable
fun ScheduleDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Download") },
        text = {
            Column {
                Text("Select time for download:")
                Spacer(modifier = Modifier.height(16.dp))
                
                // Time presets
                DownloadScheduler.SchedulePreset.values().forEach { preset ->
                    TextButton(
                        onClick = {
                            // Schedule with this preset
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${preset.hourOfDay}:00")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Statistics Dialog
 */
@Composable
fun StatisticsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val statistics = remember { DownloadStatistics(context) }
    val summary = remember { statistics.getSummary() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📊 Download Statistics") },
        text = {
            Column {
                StatRow("Total Downloads", summary.totalDownloads.toString())
                StatRow("Successful", summary.successfulDownloads.toString())
                StatRow("Failed", summary.failedDownloads.toString())
                StatRow("Success Rate", "${"%.1f".format(summary.successRate)}%")
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                StatRow("Total Data", formatBytes(summary.totalDataDownloaded))
                StatRow("Average Speed", formatSpeed(summary.averageSpeed))
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                StatRow("Today", summary.todayDownloads.toString())
                StatRow("This Week", summary.weekDownloads.toString())
                StatRow("This Month", summary.monthDownloads.toString())
                
                if (summary.mostPopularQuality != null) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    StatRow("Popular Quality", summary.mostPopularQuality)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// Helper functions
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

private fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 1024 -> "${bytesPerSecond} B/s"
        bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
        else -> "%.1f MB/s".format(bytesPerSecond / (1024.0 * 1024.0))
    }
}
