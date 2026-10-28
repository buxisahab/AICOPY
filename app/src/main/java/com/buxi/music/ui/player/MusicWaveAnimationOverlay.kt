package com.buxi.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.buxi.music.models.MediaMetadata
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun MusicWaveAnimationOverlay(
    mediaMetadata: MediaMetadata?,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_animation")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )
    
    val waveIntensity by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_intensity"
    )
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(500)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Square thumbnail overlay - Center positioned
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mediaMetadata?.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Song thumbnail",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Music wave animation around the square thumbnail
            Canvas(
                modifier = Modifier
                    .size(280.dp)
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val radius = 100.dp.toPx()
                
                // Draw multiple wave layers with golden yellow color
                repeat(3) { layer ->
                    val layerAlpha = (1f - layer * 0.25f) * waveIntensity * 0.8f
                    val layerOffset = layer * 8.dp.toPx()
                    
                    // Top wave
                    drawWavePath(
                        centerX = centerX,
                        centerY = centerY - radius - layerOffset,
                        width = radius * 2,
                        waveOffset = waveOffset + layer * 0.3f,
                        intensity = waveIntensity,
                        alpha = layerAlpha,
                        isHorizontal = true
                    )
                    
                    // Bottom wave
                    drawWavePath(
                        centerX = centerX,
                        centerY = centerY + radius + layerOffset,
                        width = radius * 2,
                        waveOffset = waveOffset + layer * 0.3f,
                        intensity = waveIntensity,
                        alpha = layerAlpha,
                        isHorizontal = true
                    )
                    
                    // Left wave
                    drawWavePath(
                        centerX = centerX - radius - layerOffset,
                        centerY = centerY,
                        width = radius * 2,
                        waveOffset = waveOffset + layer * 0.3f,
                        intensity = waveIntensity,
                        alpha = layerAlpha,
                        isHorizontal = false
                    )
                    
                    // Right wave
                    drawWavePath(
                        centerX = centerX + radius + layerOffset,
                        centerY = centerY,
                        width = radius * 2,
                        waveOffset = waveOffset + layer * 0.3f,
                        intensity = waveIntensity,
                        alpha = layerAlpha,
                        isHorizontal = false
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawWavePath(
    centerX: Float,
    centerY: Float,
    width: Float,
    waveOffset: Float,
    intensity: Float,
    alpha: Float,
    isHorizontal: Boolean
) {
    val path = Path()
    val steps = 40
    val waveHeight = 18f * intensity
    
    for (i in 0..steps) {
        val progress = i.toFloat() / steps
        val offset = (progress - 0.5f) * width
        
        val wave = waveHeight * sin((progress * 6 * PI) + (waveOffset * 2 * PI)).toFloat()
        
        val x = if (isHorizontal) centerX + offset else centerX + wave
        val y = if (isHorizontal) centerY + wave else centerY + offset
        
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    
    // Draw the wave path with golden yellow color
    drawPath(
        path = path,
        color = Color(0xFFFFD700).copy(alpha = alpha),
        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
    )
    
    // Add glowing dots along the wave
    for (i in 0..steps step 4) {
        val progress = i.toFloat() / steps
        val offset = (progress - 0.5f) * width
        val wave = waveHeight * sin((progress * 6 * PI) + (waveOffset * 2 * PI)).toFloat()
        
        val x = if (isHorizontal) centerX + offset else centerX + wave
        val y = if (isHorizontal) centerY + wave else centerY + offset
        
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = alpha * 0.9f),
            radius = 3f,
            center = Offset(x, y)
        )
    }
}

