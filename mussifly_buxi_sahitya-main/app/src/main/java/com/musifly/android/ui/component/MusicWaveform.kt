package com.musifly.android.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.musifly.android.models.MediaMetadata
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.DrawScope

@Composable
fun MusicWaveform(
    metadata: MediaMetadata,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val density = LocalDensity.current
    val barPixelHeights = remember(metadata.id) {
        FloatArray(100) { (20..70).random().toFloat() * density.density }
    }
    
    val barWidthPx = with(density) { 3.dp.toPx() }
    
    Canvas(modifier = modifier) {
        val boxHeight = size.height
        val halfHeight = boxHeight / 2f
        val boxWidth = size.width
        val spacing = boxWidth / barPixelHeights.size
        
        for (i in barPixelHeights.indices) {
            val x = i * spacing + (spacing / 2f)
            val h = barPixelHeights[i]
            drawLine(
                color = color,
                start = Offset(x, halfHeight - h/2),
                end = Offset(x, halfHeight + h/2),
                strokeWidth = barWidthPx,
                cap = StrokeCap.Round
            )
        }
    }
}
