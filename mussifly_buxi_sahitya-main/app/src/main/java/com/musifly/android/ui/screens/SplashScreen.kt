package com.musifly.android.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musifly.android.R
import com.musifly.android.secure.SecureString
import kotlin.random.Random

/**
 * Instant Start Premium Splash Screen
 * Features:
 * - Zero launch delay
 * - Official App Icon (MussiFly)
 * - Optimized Ocean Bubble particles (Canvas)
 * - Deep Dark Music Aesthetic
 */
@Composable
fun SplashScreen() {
    // No "LaunchedEffect delay" - animations start at 0s
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    
    // Bubble Animation State
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    // Scaling/Glow animation for logo
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Pre-calculated particles for performance
    val particles = remember {
        List(40) { // Increased to 40 bubbles
            ParticleData(
                xSeed = Random.nextFloat(),
                ySeed = Random.nextFloat(),
                size = Random.nextFloat() * 25f + 10f, // Increased size (10px to 35px)
                speed = Random.nextFloat() * 0.3f + 0.2f, // Slightly slower, more floating feel
                alpha = Random.nextFloat() * 0.4f + 0.3f // More visible (30% to 70% alpha)
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF050505) // Deepest Black
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF050505),
                            Color(0xFF0A0F16), // Very subtle navy tint
                            Color(0xFF000000)
                        )
                    )
                )
        ) {
            // --- Bubble Particle Layer (Canvas for Performance) ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                particles.forEach { p ->
                    // Flowing upwards
                    val y = (h * (p.ySeed - (time * p.speed))) % h
                    val finalY = if (y < 0) y + h else y
                    
                    // Sway movement
                    val sway = (Math.sin(time.toDouble() * Math.PI * 2 + p.xSeed * 5) * 40).toFloat()
                    val finalX = (w * p.xSeed) + sway

                    drawCircle(
                        color = Color.White.copy(alpha = p.alpha),
                        radius = p.size,
                        center = Offset(finalX, finalY)
                    )
                }
            }

            // Main Content (Centered)
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Background Glow behind icon
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Ambient Blue Glow
                    Canvas(modifier = Modifier.size(240.dp)) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF00B0FF).copy(alpha = 0.15f), Color.Transparent)
                            ),
                            radius = size.minDimension / 2
                        )
                    }

                    // Official App Icon (MussiFly)
                    Image(
                        painter = painterResource(id = R.mipmap.ic_musifly_launcher_foreground),
                        contentDescription = "MusiFly",
                        modifier = Modifier
                            .size(160.dp)
                            .scale(pulseScale)
                            .clip(CircleShape) // IMPORTANT: Clips the foreground to avoid square edges
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // App Name branding
                Text(
                    text = SecureString.s(SecureString.APP_NAME),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 46.sp,
                        letterSpacing = 6.sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Professional Tagline
                Text(
                    text = SecureString.s(SecureString.TAGLINE),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp
                    ),
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            // Footer branding
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = SecureString.s(SecureString.POWERED_BY),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color.White.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Minimalist Accent
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(2.dp)
                        .background(Color(0xFFFF5722)) // Signature Orange
                )
            }
        }
    }
}

private data class ParticleData(
    val xSeed: Float,
    val ySeed: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)
