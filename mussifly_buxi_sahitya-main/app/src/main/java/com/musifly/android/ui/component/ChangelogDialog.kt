package com.musifly.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.musifly.android.utils.update.UpdateData

@Composable
fun ChangelogDialog(
    updateData: UpdateData?,
    onDismiss: () -> Unit
) {
    val brandOrange = Color(0xFFFF5722)
    val surfaceColor = Color(0xFF121212)

    val versionHistory = listOf(
        VersionInfo(
            "4.4.0", "Streamlined Experience", listOf(
                "Removed Chat Screen for faster performance",
                "Organized Search with Song/Album/Artist tabs",
                "Guest users can now login from Profile",
                "Simpler Settings without bottom navigation",
                "Fixed transparent toolbars & visual bugs",
                "Grouped player controls in Liquid Theme",
                "Fixed YouTube album crashes & local thumbnails",
                "Resolved notification & account deletion issues"
            )
        ),
        VersionInfo(
            "4.3.0", "Liquid Glass Perfection", listOf(
                "Enhanced Liquid Glass Theme visual improvements",
                "Added Mini Player height and corner radius controls",
                "New Navigation Bar width customization slider",
                "Fixed icon tinting issues in Settings",
                "Refined styling for Dialogs and Audio Effects",
                "General bug fixes and performance enhancements"
            )
        ),
        VersionInfo(
            "4.2.0", "Feature Expansion", listOf(
                "Added search short-cuts in all player layouts",
                " some crashes fixed",
                "Added splash screen and theme customization",
                "Added docker floating bar and customization",
                "Enhanced sound effects & AI Equalizer optimization",
                "Added new octapad preset and treble boost slider",
                "Improved streaming stability and reduced buffer lag",
                "Premium UI/UX refinements in Update Dialogs",
                "Minor bug fixes and performance improvements"
            )
        ),
        VersionInfo(
            "4.0.0", "The Glass Overhaul", listOf(
                "Introduced liquid glass & glassmorphism themes",
                "New Spotify-style premium player layout",
                "Integrated real-time lyrics synchronization",
                "Dynamic background colors based on album art",
                "Swipe gestures for player control"
            )
        ),
        VersionInfo(
            "3.5.0", "Performance Boost", listOf(
                "Optimized database queries for faster loading",
                "HD Thumbnail support for all tracks",
                "Added Backup & Restore feature for settings",
                "Improved offline mode reliability"
            )
        ),
        VersionInfo(
            "3.0.0", "Material You Evolution", listOf(
                "Complete migration to Material 3 Design",
                "Dynamic theme support (Android 12+)",
                "Redesigned Settings and About screens",
                "Direct Spotify playlist import feature"
            )
        ),
        VersionInfo(
            "2.5.0", "Social & Discovery", listOf(
                "Telegram & Community integration",
                "Global search for online streaming",
                "Artist bios and album discovery",
                "Added support for UPI & PayPal donations"
            )
        ),
        VersionInfo(
            "2.0.0", "The Core Refactor", listOf(
                "Completely rewritten in Kotlin & Jetpack Compose",
                "New SQLite database for playlist management",
                "Advanced notification controls",
                "Low-latency audio engine integration"
            )
        ),
        VersionInfo(
            "1.0.0", "The Birth", listOf(
                "Initial release of MussiFly",
                "Basic MP3 playback support",
                "Local library scanning",
                "Simple, ad-free music experience"
            )
        )
    )

    val (themeMode) = com.musifly.android.utils.rememberEnumPreference(com.musifly.android.constants.ThemeModeKey, defaultValue = com.musifly.android.constants.ThemeMode.DARK)
    val isLiquidGlass = themeMode == com.musifly.android.constants.ThemeMode.LIQUID_GLASS

    if (isLiquidGlass) {
        com.musifly.android.ui.component.liquidglass.LiquidGlassDialog(
            onDismiss = onDismiss,
            title = {
                 Text(
                    text = "Release History",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            },
            content = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    versionHistory.forEachIndexed { index, info ->
                        VersionSection(info, brandOrange, isLatest = (index == 0))
                        if (index < versionHistory.lastIndex) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            },
            buttons = {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = brandOrange)
                ) {
                    Text("Dismiss")
                }
            }
        )
    } else {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Release History",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer // Adaptive text color
                    )
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    versionHistory.forEachIndexed { index, info ->
                        VersionSection(info, MaterialTheme.colorScheme.primary, isLatest = (index == 0))
                        if (index < versionHistory.lastIndex) {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Bottom Action
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(
                            text = "Dismiss",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
    }
}

data class VersionInfo(val version: String, val tagline: String, val notes: List<String>)

@Composable
private fun VersionSection(info: VersionInfo, accent: Color, isLatest: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "v${info.version}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = if (isLatest) accent else MaterialTheme.colorScheme.onSurface
            )
            if (isLatest) {
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    color = accent.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "NEW",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = accent
                    )
                }
            }
        }
        
        Text(
            text = info.tagline,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                info.notes.forEach { note ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            color = accent,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
