package com.buxi.music.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.buxi.music.LocalDatabase
import com.buxi.music.LocalPlayerConnection
import com.buxi.music.R
import com.buxi.music.extensions.togglePlayPause
import com.buxi.music.ui.theme.ThemeAccent
import com.buxi.music.ui.theme.ThemeAccentBright
import com.buxi.music.ui.theme.ThemeCard
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.theme.ThemeTextPrimary
import com.buxi.music.ui.theme.ThemeTextSecondary
import com.buxi.music.ui.theme.ThemeTextTertiary
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Resso-Style Frosted Glass Floating Capsule Mini-Player.
 * Recreates Resso's modern music player bar:
 * - Floating rounded capsule above bottom navigation
 * - Rotating vinyl / rounded cover with active playing waves
 * - Animated mini rhythm bars
 * - Instant favorite toggle & glowing Play/Pause button
 * - Seamless expand to full Resso experience
 */
@Composable
fun RessoMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    onExpand: (() -> Unit)? = null
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val database = LocalDatabase.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()

    val librarySong by playerConnection.currentSong.collectAsState(initial = null)
    val isFavorite = librarySong?.song?.inLibrary != null
    val heartScale = remember { Animatable(1f) }

    // Vinyl rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )

    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 12.dp)
            .shadow(16.dp, RoundedCornerShape(34.dp), spotColor = ThemeAccent.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(34.dp))
            .clickable { onExpand?.invoke() },
        color = Color(0xFF1E1D24).copy(alpha = 0.94f),
        shape = RoundedCornerShape(34.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(
                    Color.White.copy(alpha = 0.15f),
                    ThemeAccent.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.1f)
                )
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vinyl Disc / Album Artwork with animated rotation
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .rotate(if (isPlaying) rotation else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(mediaMetadata?.thumbnailUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                    // Vinyl center spindle hole
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ThemeDark)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title, Artist and Animated rhythm bars
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = mediaMetadata?.title ?: "No track playing",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPlaying) {
                            RessoMiniEqualizerBars(modifier = Modifier.size(14.dp, 10.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = mediaMetadata?.artists?.joinToString { it.name } ?: "Unknown artist",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeTextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Favorite Heart Button
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            heartScale.animateTo(1.35f, spring(dampingRatio = 0.4f))
                            heartScale.animateTo(1f, spring(dampingRatio = 0.6f))
                        }
                        mediaMetadata?.let { song ->
                            database.query {
                                inLibrary(song.id, if (isFavorite) null else LocalDateTime.now())
                            }
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .scale(heartScale.value)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isFavorite) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = "Favorite",
                        tint = if (isFavorite) ThemeAccent else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Circular Play / Pause Button with Accent Glow
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ThemeAccent)
                        .clickable { playerConnection.player.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (playbackState == Player.STATE_BUFFERING) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Bottom Glowing Gradient Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(ThemeAccent, ThemeAccentBright, Color(0xFF00D4FF))
                            )
                        )
                )
            }
        }
    }
}

/**
 * Animated Mini Soundwave Equalizer Bars for mini-player
 */
@Composable
private fun RessoMiniEqualizerBars(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "mini_eq")
    val h1 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )

    Canvas(modifier = modifier) {
        val barWidth = 2.dp.toPx()
        val spacing = 2.dp.toPx()
        val maxHeight = size.height

        val heights = listOf(h1, h2, h3)
        heights.forEachIndexed { i, h ->
            val x = i * (barWidth + spacing) + barWidth / 2
            val barH = maxHeight * h
            val yTop = maxHeight - barH
            drawLine(
                color = ThemeAccent,
                start = Offset(x, maxHeight),
                end = Offset(x, yTop),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
