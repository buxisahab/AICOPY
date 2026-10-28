package com.buxi.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.buxi.music.LocalPlayerAwareWindowInsets
import com.buxi.music.R
import com.buxi.music.ui.theme.ThemeAccent
import com.buxi.music.ui.theme.ThemeCard
import com.buxi.music.ui.theme.ThemeDark
import com.buxi.music.ui.theme.ThemeTextPrimary
import com.buxi.music.ui.theme.ThemeTextTertiary
import androidx.compose.foundation.layout.asPaddingValues

// ---------------------------------------------------------------------------
// Data models
// ---------------------------------------------------------------------------

private data class ExploreGenre(val id: String, val label: String)

private data class ExploreChannel(
    val id: String,
    val title: String,
    val songCount: Int,
    val artworkUrl: String?,
    val artworkColor: Color,
    val genre: String,
)

// ---------------------------------------------------------------------------
// Static seed data (used when no network API is wired up)
// ---------------------------------------------------------------------------

private val ALL_GENRES = listOf(
    ExploreGenre("all", "All"),
    ExploreGenre("pop", "Pop"),
    ExploreGenre("hiphop", "Hip Hop"),
    ExploreGenre("rnb", "R&B"),
    ExploreGenre("kpop", "K-Pop"),
    ExploreGenre("electronic", "Electronic"),
    ExploreGenre("jazz", "Jazz"),
    ExploreGenre("rock", "Rock"),
    ExploreGenre("latin", "Latin"),
    ExploreGenre("indie", "Indie"),
    ExploreGenre("classical", "Classical"),
)

private val FAKE_CHANNELS = listOf(
    ExploreChannel("ch1", "Today's Hits", 52, null, Color(0xFFE50914), "pop"),
    ExploreChannel("ch2", "Rap Radar", 38, null, Color(0xFF1DB954), "hiphop"),
    ExploreChannel("ch3", "R&B Soul", 29, null, Color(0xFF8B5CF6), "rnb"),
    ExploreChannel("ch4", "K-Pop Station", 61, null, Color(0xFFEC4899), "kpop"),
    ExploreChannel("ch5", "EDM Rush", 44, null, Color(0xFF00D4FF), "electronic"),
    ExploreChannel("ch6", "Jazz Café", 23, null, Color(0xFFF59E0B), "jazz"),
    ExploreChannel("ch7", "Rock Anthems", 57, null, Color(0xFFDC2626), "rock"),
    ExploreChannel("ch8", "Latin Vibes", 36, null, Color(0xFFD97706), "latin"),
    ExploreChannel("ch9", "Indie Spotlight", 19, null, Color(0xFF6366F1), "indie"),
    ExploreChannel("ch10", "Classical Mix", 31, null, Color(0xFF0EA5E9), "classical"),
    ExploreChannel("ch11", "Pop Rising", 48, null, Color(0xFFF43F5E), "pop"),
    ExploreChannel("ch12", "Trap Nation", 66, null, Color(0xFF7C3AED), "hiphop"),
    ExploreChannel("ch13", "Neo Soul", 25, null, Color(0xFFD946EF), "rnb"),
    ExploreChannel("ch14", "BTS World", 40, null, Color(0xFF06B6D4), "kpop"),
)

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RessoExploreScreen(navController: NavController) {
    val bgColor = Color(0xFF0D0D1A)
    var selectedGenre by remember { mutableStateOf("all") }

    val visibleChannels = remember(selectedGenre) {
        if (selectedGenre == "all") FAKE_CHANNELS
        else FAKE_CHANNELS.filter { it.genre == selectedGenre }
    }

    val playerAwareInsets = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Explore",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ThemeTextPrimary
            )
            IconButton(onClick = { navController.navigate("search/") }) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = "Search",
                    tint = ThemeTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ── Genre pill tabs ──────────────────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ALL_GENRES) { genre ->
                GenrePill(
                    label = genre.label,
                    selected = selectedGenre == genre.id,
                    onClick = { selectedGenre = genre.id }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Channel grid ─────────────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                bottom = playerAwareInsets.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(visibleChannels, key = { it.id }) { channel ->
                ChannelCard(
                    channel = channel,
                    onClick = {
                        navController.navigate("online_playlist/${channel.id}")
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Genre pill
// ---------------------------------------------------------------------------

@Composable
private fun GenrePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) ThemeAccent else ThemeCard
    val textColor = if (selected) Color.White else ThemeTextTertiary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ---------------------------------------------------------------------------
// Channel card
// ---------------------------------------------------------------------------

@Composable
private fun ChannelCard(channel: ExploreChannel, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        // Artwork / solid color background
        if (channel.artworkUrl != null) {
            AsyncImage(
                model = channel.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(channel.artworkColor)
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Text labels at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = channel.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${channel.songCount} songs",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp
            )
        }
    }
}
