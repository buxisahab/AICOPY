package com.musifly.android.ui.component

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DigitalPotentiometer(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    unit: String = "dB"
) {
    // Mapping: 
    // Angle 135 -> Min
    // Angle 405 (45) -> Max
    // Sweep = 270 degrees
    
    val currentAngle = remember(value) {
        val normalized = (value - range.start) / (range.endInclusive - range.start)
        135f + normalized * 270f
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val indicatorColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                var dragStartAngle = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                         // calculate initial angle if needed
                    },
                    onDrag = { change, dragAmount ->
                        // Sensitivity: vertical drag moves value
                        // -dragAmount.y means moving up increases value
                        val sensitivity = 0.005f // % per pixel
                        val rangeSpan = range.endInclusive - range.start
                        val delta = (-dragAmount.y * sensitivity * rangeSpan)
                        val newValue = (value + delta).coerceIn(range.start, range.endInclusive)
                        onValueChange(newValue)
                        change.consume()
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val radius = minOf(w, h) / 2 * 0.8f
        val strokeWidth = radius * 0.15f

        // Draw Track (Background Arc)
        drawArc(
            color = trackColor.copy(alpha=0.3f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Draw Progress (Active Arc)
        val sweep = currentAngle - 135f
        if (sweep > 0) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(primaryColor.copy(alpha=0.5f), primaryColor),
                    center = Offset(cx, cy)
                ),
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Draw Ticks
        for (i in 0..10) {
            val angle = (135f + (270f * i / 10)) * (PI / 180)
            val rStart = radius * 1.15f
            val rEnd = radius * 1.3f
            drawLine(
                color = if (i.toFloat()/10 <= (value - range.start)/(range.endInclusive - range.start)) primaryColor else trackColor,
                start = Offset((cx + cos(angle) * rStart).toFloat(), (cy + sin(angle) * rStart).toFloat()),
                end = Offset((cx + cos(angle) * rEnd).toFloat(), (cy + sin(angle) * rEnd).toFloat()),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )
        }
        
        // Draw Indicator Dot on the Ring
        val indAngleRad = currentAngle * (PI / 180)
        drawCircle(
            color = indicatorColor,
            radius = strokeWidth * 0.8f,
            center = Offset(
                (cx + cos(indAngleRad) * radius).toFloat(),
                (cy + sin(indAngleRad) * radius).toFloat()
            )
        )
    }
}

fun valueToAngle(value: Float, range: ClosedFloatingPointRange<Float>): Float {
    return 0f // Helper stub
}
