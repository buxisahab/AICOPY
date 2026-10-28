package com.musifly.android.ui.player.layouts

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.db.entities.LyricsEntity
import com.musifly.android.ui.component.Lyrics

/**
 * Unified Lyrics Overlay for all Player Layouts
 * 
 * This component provides consistent lyrics behavior across all player layouts:
 * - Instagram Reels style
 * - Spotify style
 * - Bloome style
 * - Musix style
 * - Modern Vertical style
 * 
 * Features:
 * - Long press to toggle lyrics
 * - "No lyrics available" message when not found
 * - Synced lyrics display
 * - Consistent UI across all layouts
 * 
 * Usage:
 * ```kotlin
 * LyricsOverlay(
 *     modifier = Modifier.fillMaxSize(),
 *     overlayColor = Color.Black.copy(alpha = 0.5f), // Optional
 *     showCloseButton = true, // Optional
 *     alignmentOnImage = Alignment.BottomCenter // Optional
 * )
 * ```
 */
@Composable
fun LyricsOverlay(
    modifier: Modifier = Modifier,
    overlayColor: Color = Color.Black.copy(alpha = 0.5f),
    showCloseButton: Boolean = true,
    closeButtonAlignment: Alignment = Alignment.TopEnd,
    messageAlignment: Alignment = Alignment.BottomCenter,
    enableLongPress: Boolean = true,
    onLyricsToggled: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    
    // Lyrics state
    val lyricsEntity by playerConnection?.currentLyrics?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }
    val hasLyrics = !lyrics.isNullOrEmpty() && 
                    lyrics != LyricsEntity.LYRICS_NOT_FOUND && 
                    lyrics != "No lyrics found"
    
    var showLyricsContent by remember { mutableStateOf(false) }
    
    // Notify parent about lyrics state
    LaunchedEffect(showLyricsContent) {
        onLyricsToggled?.invoke(showLyricsContent)
    }
    
    Box(
        modifier = modifier
            .then(
                if (enableLongPress) {
                    Modifier.pointerInput(hasLyrics) {
                        detectTapGestures(
                            onLongPress = {
                                if (hasLyrics) {
                                    showLyricsContent = !showLyricsContent
                                }
                            }
                        )
                    }
                } else Modifier
            )
    ) {
        Crossfade(
            targetState = showLyricsContent && hasLyrics,
            animationSpec = tween(durationMillis = 300),
            label = "LyricsOverlayCrossfade"
        ) { showLyrics ->
            if (showLyrics) {
                // Show lyrics overlay with proper sync
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                ) {
                    // Lyrics component with REAL-TIME position for sync
                    Lyrics(
                        sliderPositionProvider = {
                            // CRITICAL: Pass REAL-TIME player position for proper sync!
                            playerConnection?.player?.currentPosition
                        },
                        onNavigateBack = null, // Not fullscreen
                        modifier = Modifier.fillMaxSize(),
                        forceShow = true // Force show lyrics in overlay
                    )
                    
                    // Close button with animation
                    if (showCloseButton) {
                        Box(
                            modifier = Modifier
                                .align(closeButtonAlignment)
                                .padding(16.dp)
                        ) {
                            IconButton(
                                onClick = { showLyricsContent = false },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        overlayColor,
                                        shape = CircleShape
                                    )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = "Close lyrics",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Show hint/message
                Box(
                    modifier = Modifier
                        .align(messageAlignment)
                        .padding(16.dp)
                ) {
                    if (hasLyrics) {
                        // Hint: Long press for lyrics - with fade animation
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = androidx.compose.animation.fadeIn(
                                animationSpec = tween(durationMillis = 500)
                            ),
                            exit = androidx.compose.animation.fadeOut(
                                animationSpec = tween(durationMillis = 300)
                            )
                        ) {
                            Text(
                                text = "Long press for lyrics",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(
                                        overlayColor,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        // No lyrics available message - with scale animation
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = androidx.compose.animation.scaleIn(
                                animationSpec = tween(durationMillis = 400),
                                initialScale = 0.8f
                            ) + androidx.compose.animation.fadeIn(
                                animationSpec = tween(durationMillis = 400)
                            ),
                            exit = androidx.compose.animation.scaleOut(
                                animationSpec = tween(durationMillis = 300)
                            ) + androidx.compose.animation.fadeOut(
                                animationSpec = tween(durationMillis = 300)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(
                                        overlayColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.music_note),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "No lyrics available",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lightweight lyrics state provider
 * Use this to check lyrics availability without rendering the full overlay
 */
@Composable
fun rememberLyricsState(): LyricsState {
    val playerConnection = LocalPlayerConnection.current
    val lyricsEntity by playerConnection?.currentLyrics?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }
    val lyrics = remember(lyricsEntity) { lyricsEntity?.lyrics?.trim() }
    val hasLyrics = !lyrics.isNullOrEmpty() && 
                    lyrics != LyricsEntity.LYRICS_NOT_FOUND && 
                    lyrics != "No lyrics found"
    
    return LyricsState(
        hasLyrics = hasLyrics,
        lyrics = lyrics,
        isSynced = lyrics?.startsWith("[") == true
    )
}

/**
 * Data class representing lyrics state
 */
data class LyricsState(
    val hasLyrics: Boolean,
    val lyrics: String?,
    val isSynced: Boolean
)
