package com.buxi.music.ui.component

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.buxi.music.R
import com.buxi.music.models.MediaMetadata
import com.buxi.music.ui.theme.ThemeAccent
import com.buxi.music.ui.theme.ThemeCard
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.theme.ThemeTextPrimary
import com.buxi.music.ui.theme.ThemeTextSecondary
import com.buxi.music.ui.theme.ThemeTextTertiary
import com.buxi.music.utils.Toast

/**
 * Lyric Card Theme options
 */
enum class QuoteCardTheme(val title: String, val brush: Brush, val textColor: Color) {
    CRIMSON_BEAT(
        "Crimson",
        Brush.linearGradient(listOf(Color(0xFFE50914), Color(0xFF4A0012), Color(0xFF100F14))),
        Color.White
    ),
    NEON_AURORA(
        "Aurora",
        Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF00D4FF), Color(0xFF0A0E27))),
        Color.White
    ),
    SUNSET_VIBE(
        "Sunset",
        Brush.linearGradient(listOf(Color(0xFFFF6B00), Color(0xFFFF2D55), Color(0xFF1F0824))),
        Color.White
    ),
    OBSIDIAN_GLASS(
        "Obsidian",
        Brush.linearGradient(listOf(Color(0xFF22222A), Color(0xFF141418), Color(0xFF0C0C0E))),
        Color.White
    )
}

/**
 * Signature Resso-Style Lyric Quote Card Creator.
 * Resso was widely celebrated for its viral Lyric Quote feature,
 * enabling users to pick lyric lines and generate beautiful styled quote cards to share.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RessoLyricQuoteDialog(
    mediaMetadata: MediaMetadata?,
    lyrics: List<String>,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val context = LocalContext.current
    val songTitle = mediaMetadata?.title ?: "Unknown Song"
    val artistName = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown Artist"

    // Default sample lyrics if track has no synced lyrics
    val displayLyrics = remember(lyrics) {
        if (lyrics.isNotEmpty()) lyrics else listOf(
            "Every melody whispers memories of you,",
            "Lost in the rhythm of the city night,",
            "Turn up the music, let the sound take flight,",
            "Some songs stay with us forever."
        )
    }

    val selectedIndices = remember { mutableStateListOf<Int>(0) }
    var selectedThemeIndex by remember { mutableIntStateOf(0) }
    val currentTheme = QuoteCardTheme.values()[selectedThemeIndex]

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .clip(RoundedCornerShape(28.dp))
            .background(ThemeDark)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ThemeAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.format_quote),
                            contentDescription = null,
                            tint = ThemeAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Lyric Quote",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ThemeTextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Close",
                        tint = ThemeTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Quote Card Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(currentTheme.brush)
                    .shadow(12.dp, RoundedCornerShape(20.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Quote icon & AI Music watermark
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "“",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = currentTheme.textColor.copy(alpha = 0.7f),
                            lineHeight = 28.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ThemeAccent)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "AI Music • Vibe",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Selected Lyrics text
                    val quoteText = selectedIndices.sorted()
                        .mapNotNull { displayLyrics.getOrNull(it) }
                        .joinToString("\n")

                    Text(
                        text = if (quoteText.isNotBlank()) quoteText else "Select lyrics below to create quote",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Italic,
                        color = currentTheme.textColor,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Song info bottom row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(3.dp, 24.dp)
                                .clip(CircleShape)
                                .background(ThemeAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = songTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = currentTheme.textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.labelSmall,
                                color = currentTheme.textColor.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Card Style Theme selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuoteCardTheme.values().forEachIndexed { index, theme ->
                    val isSelected = selectedThemeIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) ThemeAccent.copy(alpha = 0.25f) else ThemeCard
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) ThemeAccent else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedThemeIndex = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = theme.title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) ThemeAccent else ThemeTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Select lines to quote (max 4):",
                style = MaterialTheme.typography.labelMedium,
                color = ThemeTextTertiary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lyrics selection list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ThemeCard)
                    .padding(8.dp)
            ) {
                itemsIndexed(displayLyrics) { index, line ->
                    val isSelected = index in selectedIndices
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) ThemeAccent.copy(alpha = 0.18f) else Color.Transparent
                            )
                            .clickable {
                                if (isSelected) {
                                    if (selectedIndices.size > 1) selectedIndices.remove(index)
                                } else {
                                    if (selectedIndices.size < 4) {
                                        selectedIndices.add(index)
                                    } else {
                                        Toast.makeText(context, "Maximum 4 lines allowed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) ThemeAccent else Color.White.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) ThemeTextPrimary else ThemeTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ThemeTextSecondary)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val quoteLines = selectedIndices.sorted()
                            .mapNotNull { displayLyrics.getOrNull(it) }
                            .joinToString("\n")
                        val shareMessage = "“$quoteLines”\n\n— $songTitle by $artistName\nShared from AI Music"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareMessage)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Lyric Quote"))
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemeAccent)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
