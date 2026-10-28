package com.buxi.music.ui.component

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.buxi.music.R
import com.buxi.music.models.MediaMetadata
import com.buxi.music.ui.theme.ThemeCard
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.theme.ThemeTextPrimary
import com.buxi.music.ui.theme.ThemeTextSecondary
import com.buxi.music.ui.theme.ThemeTextTertiary

private val RessoCoralAccent = Color(0xFFFF4D67)

/**
 * Resso-style Song More Options Sheet (Matches playing_dialog_more.xml)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RessoSongMoreSheet(
    mediaMetadata: MediaMetadata?,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenAudioQuality: () -> Unit,
    onOpenAudioEffects: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onViewArtist: () -> Unit,
    onViewAlbum: () -> Unit,
    onHideSong: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ThemeDark,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp
                )
                .verticalScroll(rememberScrollState())
        ) {
            // Track Info Header
            if (mediaMetadata != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(mediaMetadata.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaMetadata.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemeTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mediaMetadata.artists.joinToString { it.name },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ThemeTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Options List
            RessoMoreOptionRow(
                icon = if (isLiked) R.drawable.favorite else R.drawable.favorite_border,
                iconTint = if (isLiked) RessoCoralAccent else ThemeTextPrimary,
                title = if (isLiked) "Remove from Liked Songs" else "Like this song",
                onClick = {
                    onToggleLike()
                    onDismiss()
                }
            )

            RessoMoreOptionRow(
                icon = R.drawable.add,
                title = "Add to playlist",
                onClick = {
                    onAddToPlaylist()
                    onDismiss()
                }
            )

            RessoMoreOptionRow(
                icon = R.drawable.bedtime,
                title = "Sleep timer",
                onClick = {
                    onDismiss()
                    onOpenSleepTimer()
                }
            )

            RessoMoreOptionRow(
                icon = R.drawable.speed,
                title = "Playback speed",
                onClick = {
                    onDismiss()
                    onOpenSpeed()
                }
            )

            RessoMoreOptionRow(
                icon = R.drawable.graphic_eq,
                title = "Audio quality (Lossless/VIP)",
                onClick = {
                    onDismiss()
                    onOpenAudioQuality()
                }
            )

            RessoMoreOptionRow(
                icon = R.drawable.tune,
                title = "Equalizer & Audio effects",
                onClick = {
                    onDismiss()
                    onOpenAudioEffects()
                }
            )

            RessoMoreOptionRow(
                icon = R.drawable.share,
                title = "Share song",
                onClick = {
                    mediaMetadata?.let {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, "Listening to ${it.title} by ${it.artists.joinToString { a -> a.name }} on AI Music https://music.youtube.com/watch?v=${it.id}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Track"))
                    }
                    onDismiss()
                }
            )

            if (mediaMetadata?.artists?.isNotEmpty() == true) {
                RessoMoreOptionRow(
                    icon = R.drawable.artist,
                    title = "View artist: ${mediaMetadata.artists.firstOrNull()?.name ?: ""}",
                    onClick = {
                        onViewArtist()
                        onDismiss()
                    }
                )
            }

            if (mediaMetadata?.album != null) {
                RessoMoreOptionRow(
                    icon = R.drawable.album,
                    title = "View album: ${mediaMetadata.album.title}",
                    onClick = {
                        onViewAlbum()
                        onDismiss()
                    }
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            RessoMoreOptionRow(
                icon = R.drawable.block,
                iconTint = Color(0xFFFF6B6B),
                title = "Hide this song / artist",
                onClick = {
                    onDismiss()
                    onHideSong()
                }
            )
        }
    }
}

@Composable
private fun RessoMoreOptionRow(
    icon: Int,
    title: String,
    iconTint: Color = ThemeTextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (iconTint != ThemeTextPrimary) iconTint else ThemeTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

