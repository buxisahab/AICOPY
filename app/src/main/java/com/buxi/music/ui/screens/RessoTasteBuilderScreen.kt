package com.buxi.music.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.buxi.music.R
import com.buxi.music.ui.theme.ThemeDark

private val RessoCoral = Color(0xFFFF4D67)

data class GenreBubble(
    val name: String,
    val icon: String,
    val color1: Color,
    val color2: Color
)

/**
 * Resso Taste Builder (Matches user_activity_taste_builder.xml)
 * Allows users to customize their recommendation algorithm by selecting music genres and vibes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RessoTasteBuilderScreen(
    navController: NavController,
    onFinished: () -> Unit = { navController.popBackStack() }
) {
    val selectedGenres = remember { mutableStateListOf<String>() }

    val genres = remember {
        listOf(
            GenreBubble("Pop", "✨", Color(0xFFFF5E7E), Color(0xFFFF9966)),
            GenreBubble("Hip-Hop", "🎤", Color(0xFF654EA3), Color(0xFFEAAFC8)),
            GenreBubble("R&B", "🎷", Color(0xFF8E2DE2), Color(0xFF4A00E0)),
            GenreBubble("Electronic / EDM", "⚡", Color(0xFF00C6FF), Color(0xFF0072FF)),
            GenreBubble("Bollywood", "🎬", Color(0xFFFF416C), Color(0xFFFF4B2B)),
            GenreBubble("Punjabi", "🥁", Color(0xFFF7971E), Color(0xFFFFD200)),
            GenreBubble("Lo-Fi & Chill", "☕", Color(0xFF43C6AC), Color(0xFF191654)),
            GenreBubble("Rock", "🎸", Color(0xFFE52D27), Color(0xFFB31217)),
            GenreBubble("K-Pop", "💜", Color(0xFFDA22FF), Color(0xFF9733EE)),
            GenreBubble("Indie & Acoustic", "🍃", Color(0xFF11998E), Color(0xFF38EF7D)),
            GenreBubble("Classical", "🎻", Color(0xFF7F7FD5), Color(0xFF86A8E7)),
            GenreBubble("Metal", "🤘", Color(0xFF232526), Color(0xFF414345)),
            GenreBubble("Reggae", "🌴", Color(0xFF00B4DB), Color(0xFF0083B0)),
            GenreBubble("Workout Beats", "🔥", Color(0xFFFF0844), Color(0xFFFFB199))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                )
                .padding(horizontal = 20.dp)
        ) {
            // Top Bar with Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onFinished) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                TextButton(onClick = onFinished) {
                    Text(
                        text = "Skip",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Subtitle
            Text(
                text = "Taste Builder",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Select 3 or more genres you vibe with. Your For You feed will adapt to your taste.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bubble Grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    genres.forEach { genre ->
                        val isSelected = selectedGenres.contains(genre.name)
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1.0f,
                            label = "scale"
                        )

                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(listOf(genre.color1, genre.color2))
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF1E1E2E),
                                                Color(0xFF252538)
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable {
                                    if (isSelected) {
                                        selectedGenres.remove(genre.name)
                                    } else {
                                        selectedGenres.add(genre.name)
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = genre.icon,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = genre.name,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Continue CTA
            val canProceed = selectedGenres.size >= 3
            val countText = if (selectedGenres.size < 3) {
                "Pick ${3 - selectedGenres.size} more"
            } else {
                "${selectedGenres.size} selected"
            }

            Button(
                onClick = onFinished,
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RessoCoral,
                    disabledContainerColor = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = if (canProceed) "Start Listening" else countText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

