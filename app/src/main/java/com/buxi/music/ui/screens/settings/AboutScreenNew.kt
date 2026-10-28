package com.buxi.music.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlin.math.*

// Color palette
val kBackgroundColor = Color(0xFF100F14) // ThemeDark
val kPrimaryTextColor = Color.White
val kSecondaryTextColor = Color(0xFFC3B9CF)
val kCardBackgroundColor = Color(0xFF282032).copy(alpha = 0.18f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreenNew(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(kBackgroundColor)
    ) {
        // Background effects
        ParticleBackground()
        AnimatedWaveform()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Info Card
            InfoCard(context)
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Support Section
            SupportSection(context)
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer
            FooterSection()
        }
        
        // App Bar
        TopAppBar(
            title = { 
                Text(
                    "About",
                    color = kPrimaryTextColor,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = kPrimaryTextColor.copy(alpha = 0.8f)
                    )
                }
            },
            modifier = Modifier.background(Color.Transparent),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            scrollBehavior = scrollBehavior
        )
    }
}

@Composable
fun InfoCard(context: android.content.Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = kCardBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title with gradient
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Ai Music",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFEBD88),
                                Color(0xFFF17C98)
                            )
                        )
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                "Let AI tune your vibe.",
                style = MaterialTheme.typography.bodyLarge.copy(color = kSecondaryTextColor)
            )
            
            Spacer(modifier = Modifier.height(35.dp))
            
            // Handle section
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/buxi.sahab"))
                        ContextCompat.startActivity(context, intent, null)
                    }
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFB88C),
                                Color(0xFFF88A6B)
                            )
                        )
                    ),
                color = Color.Black.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFB88C),
                                        Color(0xFFF88A6B)
                                    )
                                )
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        "@buxi.sahab",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kPrimaryTextColor,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(35.dp))
            
            // Info pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InfoPill(
                    icon = Icons.Default.Email,
                    text = "Instagram",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ig.me/buxi.sahab"))
                        ContextCompat.startActivity(context, intent, null)
                    }
                )
                
                InfoPill(
                    icon = Icons.Default.Info,
                    text = "LinkedIn",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/sahadatbuxi"))
                        ContextCompat.startActivity(context, intent, null)
                    }
                )
            }
        }
    }
}

@Composable
fun InfoPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = kSecondaryTextColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodySmall.copy(color = kSecondaryTextColor)
            )
        }
    }
}

@Composable
fun SupportSection(context: android.content.Context) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Feel the beat, powered by Buxi.Sahab",
            style = MaterialTheme.typography.bodyMedium.copy(color = kSecondaryTextColor),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            "Let the music lift your spirit",
            style = MaterialTheme.typography.bodyMedium.copy(color = kSecondaryTextColor),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun FooterSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "All rights reserved buxi.sahab 2025",
            style = MaterialTheme.typography.bodySmall.copy(color = kSecondaryTextColor)
        )
    }
}

@Composable
fun AnimatedWaveform() {
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val paint = Paint().apply {
            strokeWidth = 3f
            strokeCap = StrokeCap.Round
            style = PaintingStyle.Stroke
        }
        
        val path = Path()
        val width = size.width
        val height = size.height
        
        for (x in -5..width.toInt() + 5 step 5) {
            val amp = height * 0.1f
            val y = height / 2 +
                   (amp) * sin(x * 0.015f + time * 2 * PI.toFloat()) +
                   (amp * 0.5f) * sin(x * 0.025f + time * 4 * PI.toFloat())
            
            if (x == -5) {
                path.moveTo(x.toFloat(), y)
            } else {
                path.lineTo(x.toFloat(), y)
            }
        }
        
        // Disabled pink color animation
        // paint.color = Color(0xFFE3729A)
        // drawPath(path, paint.color)
    }
}

@Composable
fun ParticleBackground() {
    val particles = remember { generateParticles() }
    val infiniteTransition = rememberInfiniteTransition()
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Disabled pink color background gradient
        // drawRect(
        //     brush = Brush.radialGradient(
        //         colors = listOf(
        //             Color(0xFF2A1726).copy(alpha = 0.6f),
        //             kBackgroundColor.copy(alpha = 0f)
        //         )
        //     )
        // )
        
        // Draw particles
        particles.forEach { particle ->
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = particle.radius,
                center = Offset(
                    particle.position.x * size.width,
                    particle.position.y * size.height
                )
            )
        }
    }
}

// Data classes and helper functions
data class Particle(
    var position: Offset,
    val radius: Float,
    val velocity: Offset,
    var lifespan: Float,
    val maxLifespan: Float,
    val isSharp: Boolean
)

private fun generateParticles(): List<Particle> {
    val random = java.util.Random()
    return List(40) {
        Particle(
            position = Offset(random.nextFloat(), random.nextFloat()),
            radius = random.nextFloat() * 1.5f + 0.5f,
            velocity = Offset(
                (random.nextFloat() - 0.5f) * 0.01f,
                -(random.nextFloat() * 0.02f + 0.002f)
            ),
            lifespan = random.nextFloat() * 8f + 4f,
            maxLifespan = random.nextFloat() * 8f + 4f,
            isSharp = random.nextFloat() > 0.4f
        )
    }
}
