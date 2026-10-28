package com.musifly.android.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Instagram Reels-style double-tap like animation
 * 
 * Features:
 * - 3D pink heart with gradient (pink to magenta)
 * - Smooth scale animation: 0% → 130% → 90% → 100% → 0%
 * - Glossy shine effect
 * - Particle burst (8-10 glowing particles)
 * - Soft glow around heart
 * - Total duration: 0.65s - 0.75s
 * - Transparent background
 */
@Composable
fun InstagramLikeAnimation(
    visible: Boolean,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var animationPhase by remember { mutableStateOf(AnimationPhase.IDLE) }
    
    // Scale animation with multiple phases
    val scale by animateFloatAsState(
        targetValue = when (animationPhase) {
            AnimationPhase.IDLE -> 0f
            AnimationPhase.POP_IN -> 1.3f
            AnimationPhase.SQUASH -> 0.9f
            AnimationPhase.SETTLE -> 1.0f
            AnimationPhase.FADE_OUT -> 0f
        },
        animationSpec = when (animationPhase) {
            AnimationPhase.POP_IN -> tween(durationMillis = 120, easing = FastOutSlowInEasing)
            AnimationPhase.SQUASH -> tween(durationMillis = 80, easing = LinearEasing)
            AnimationPhase.SETTLE -> tween(durationMillis = 100, easing = FastOutSlowInEasing)
            AnimationPhase.FADE_OUT -> tween(durationMillis = 250, easing = FastOutLinearInEasing)
            else -> tween(0)
        },
        label = "heartScale"
    )
    
    // Alpha animation for fade out
    val alpha by animateFloatAsState(
        targetValue = if (animationPhase == AnimationPhase.FADE_OUT) 0f else 1f,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "heartAlpha"
    )
    
    // Particle animations
    val particleStates = remember { List(10) { ParticleState() } }
    
    LaunchedEffect(visible) {
        if (visible) {
            // Phase 1: Pop in (0% → 130%) - 120ms
            animationPhase = AnimationPhase.POP_IN
            delay(120)
            
            // Phase 2: Squash (130% → 90%) - 80ms
            animationPhase = AnimationPhase.SQUASH
            delay(80)
            
            // Phase 3: Settle (90% → 100%) - 100ms
            animationPhase = AnimationPhase.SETTLE
            delay(100)
            
            // Phase 4: Hold at full size - 350ms
            delay(350)
            
            // Phase 5: Fade out (100% → 0%) - 250ms
            animationPhase = AnimationPhase.FADE_OUT
            delay(250)
            
            // Reset and notify end
            animationPhase = AnimationPhase.IDLE
            onAnimationEnd()
        } else {
            animationPhase = AnimationPhase.IDLE
        }
    }
    
    // Trigger particles during pop-in phase
    LaunchedEffect(animationPhase) {
        if (animationPhase == AnimationPhase.POP_IN) {
            particleStates.forEachIndexed { index, particle ->
                val angle = (360f / particleStates.size) * index
                particle.trigger(angle)
            }
        }
    }
    
    if (visible || scale > 0.01f) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Draw particles and heart
            Canvas(modifier = Modifier.size(200.dp)) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                
                // Draw particles
                particleStates.forEach { particle ->
                    drawParticle(particle, centerX, centerY, alpha)
                }
                
                // Draw heart with glow, gradient, and shine
                drawInstagramHeart(
                    centerX = centerX,
                    centerY = centerY,
                    scale = scale,
                    alpha = alpha
                )
            }
        }
    }
}

/**
 * Animation phases for the heart animation
 */
private enum class AnimationPhase {
    IDLE,
    POP_IN,
    SQUASH,
    SETTLE,
    FADE_OUT
}

/**
 * State for individual particle
 */
private class ParticleState {
    var progress by mutableStateOf(0f)
    var angle by mutableStateOf(0f)
    var isActive by mutableStateOf(false)
    
    suspend fun trigger(particleAngle: Float) {
        angle = particleAngle
        isActive = true
        progress = 0f
        
        // Animate particle outward
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        ) { value, _ ->
            progress = value
        }
        
        isActive = false
    }
}

/**
 * Draw a single particle with glow effect
 */
private fun DrawScope.drawParticle(
    particle: ParticleState,
    centerX: Float,
    centerY: Float,
    globalAlpha: Float
) {
    if (!particle.isActive || particle.progress == 0f) return
    
    val maxDistance = 100f
    val distance = particle.progress * maxDistance
    val angleRad = particle.angle * (PI / 180f).toFloat()
    
    val x = centerX + cos(angleRad) * distance
    val y = centerY + sin(angleRad) * distance
    
    // Particle fades as it moves outward
    val particleAlpha = (1f - particle.progress) * globalAlpha
    
    // Particle color - pink to magenta
    val particleColor = Color(0xFFFF1493).copy(alpha = particleAlpha)
    
    // Draw glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                particleColor.copy(alpha = particleAlpha * 0.6f),
                Color.Transparent
            ),
            center = Offset(x, y),
            radius = 12f
        ),
        radius = 12f,
        center = Offset(x, y)
    )
    
    // Draw particle core
    drawCircle(
        color = particleColor,
        radius = 4f,
        center = Offset(x, y)
    )
}

/**
 * Draw Instagram-style 3D heart with gradient, glow, and glossy shine
 */
private fun DrawScope.drawInstagramHeart(
    centerX: Float,
    centerY: Float,
    scale: Float,
    alpha: Float
) {
    if (scale <= 0f) return
    
    val baseSize = 75f * scale
    
    // Create heart path
    val heartPath = Path().apply {
        val scaledCenterX = centerX
        val scaledCenterY = centerY
        
        // Heart shape coordinates (scaled)
        moveTo(scaledCenterX, scaledCenterY + baseSize * 0.3f)
        
        // Left curve
        cubicTo(
            scaledCenterX - baseSize * 0.7f, scaledCenterY - baseSize * 0.1f,
            scaledCenterX - baseSize * 0.8f, scaledCenterY - baseSize * 0.6f,
            scaledCenterX, scaledCenterY - baseSize * 0.4f
        )
        
        // Right curve
        cubicTo(
            scaledCenterX + baseSize * 0.8f, scaledCenterY - baseSize * 0.6f,
            scaledCenterX + baseSize * 0.7f, scaledCenterY - baseSize * 0.1f,
            scaledCenterX, scaledCenterY + baseSize * 0.3f
        )
        
        close()
    }
    
    // Layer 1: Outer glow (soft pink glow)
    drawPath(
        path = heartPath,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFF1493).copy(alpha = alpha * 0.4f),
                Color(0xFFFF69B4).copy(alpha = alpha * 0.2f),
                Color.Transparent
            ),
            center = Offset(centerX, centerY),
            radius = baseSize * 1.5f
        )
    )
    
    // Layer 2: Main heart body (pink to magenta gradient)
    drawPath(
        path = heartPath,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFF69B4).copy(alpha = alpha), // Pink
                Color(0xFFFF1493).copy(alpha = alpha), // Deep Pink
                Color(0xFFC71585).copy(alpha = alpha)  // Medium Violet Red (Magenta)
            ),
            startY = centerY - baseSize * 0.5f,
            endY = centerY + baseSize * 0.5f
        )
    )
    
    // Layer 3: 3D depth effect (subtle shadow on bottom-right)
    val shadowPath = Path().apply {
        addPath(heartPath)
    }
    drawPath(
        path = shadowPath,
        brush = Brush.radialGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = alpha * 0.15f)
            ),
            center = Offset(centerX + baseSize * 0.2f, centerY + baseSize * 0.2f),
            radius = baseSize * 0.8f
        ),
        blendMode = BlendMode.Multiply
    )
    
    // Layer 4: Glossy shine effect (top-left highlight)
    val shinePath = Path().apply {
        val shineX = centerX - baseSize * 0.25f
        val shineY = centerY - baseSize * 0.3f
        
        addOval(
            androidx.compose.ui.geometry.Rect(
                left = shineX - baseSize * 0.2f,
                top = shineY - baseSize * 0.15f,
                right = shineX + baseSize * 0.2f,
                bottom = shineY + baseSize * 0.15f
            )
        )
    }
    
    // Clip shine to heart shape
    clipPath(heartPath) {
        rotate(degrees = -25f, pivot = Offset(centerX, centerY)) {
            drawPath(
                path = shinePath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = alpha * 0.6f),
                        Color.White.copy(alpha = alpha * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(centerX - baseSize * 0.25f, centerY - baseSize * 0.3f),
                    radius = baseSize * 0.3f
                ),
                blendMode = BlendMode.Screen
            )
        }
    }
    
    // Layer 5: Soft inner glow
    clipPath(heartPath) {
        drawPath(
            path = heartPath,
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha * 0.2f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY - baseSize * 0.2f),
                radius = baseSize * 0.6f
            ),
            blendMode = BlendMode.Screen
        )
    }
}
