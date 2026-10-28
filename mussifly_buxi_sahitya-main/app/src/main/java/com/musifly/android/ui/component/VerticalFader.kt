package com.musifly.android.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A vertical fader (slider) that matches the "Sound Booster" neon aesthetic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerticalFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    trackColor: Color = Color(0xFF333333),
    progressColor: Color = Color(0xFFCCFF00), // Neon Green
    thumbColor: Color = Color(0xFFE0E0E0),
    width: Dp = 40.dp,
    height: Dp = 200.dp
) {
    // Custom vertical slider implementation
    // Standard Material3 Slider doesn't support vertical orientation natively without rotation tricks that mess up input.
    // However, the cleanest way to get proper touch handling is to rotate a standard slider.
    
    Layout(
        content = {
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                enabled = enabled,
                valueRange = valueRange,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = thumbColor,
                    activeTrackColor = progressColor,
                    inactiveTrackColor = trackColor,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                thumb = {
                    // Custom Thumb: A rectangular block like a physical fader
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(10.dp)
                            .graphicsLayer {
                                // Counter-rotate shadow/shape if needed, but since we rotate the whole component,
                                // the thumb draws relative to that.
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw fader knob
                            drawRoundRect(
                                color = thumbColor,
                                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                                size = Size(size.width, size.height)
                            )
                            // Add some grip lines
                            drawLine(
                                color = Color.Gray,
                                start = Offset(size.width / 2, size.height * 0.3f),
                                end = Offset(size.width / 2, size.height * 0.7f),
                                strokeWidth = 2f
                            )
                        }
                    }
                },
                track = { sliderState ->
                    // Custom Track to match the thin line + neon fill style
                    // The 'track' lambda in Material3 Slider is complex to customize perfectly while keeping logic.
                    // We'll rely on defaults but maybe override via colors/height.
                    // Actually, to get the "neon bar" look, standard track might be enough if we set height.
                    
                    SliderDefaults.Track(
                        colors = SliderDefaults.colors(
                            activeTrackColor = progressColor,
                            inactiveTrackColor = trackColor
                        ),
                        enabled = enabled,
                        sliderState = sliderState,
                        modifier = Modifier.height(6.dp) // Thicker track
                    )
                }
            )
        },
        measurePolicy = { measurables, constraints ->
            // Measure the slider as if it were horizontal, using the *height* constraint as its width
            val placeable = measurables.first().measure(
                Constraints(
                    minWidth = constraints.minHeight,
                    maxWidth = constraints.maxHeight,
                    minHeight = constraints.minWidth,
                    maxHeight = constraints.maxWidth
                )
            )

            // The component size is fixed to the requested width/height
            layout(constraints.maxWidth, constraints.maxHeight) {
                // Place the slider centered, but rotated -90 degrees
                placeable.placeWithLayer(
                    x = -(placeable.width - constraints.maxWidth) / 2,
                    y = -(placeable.height - constraints.maxHeight) / 2,
                    layerBlock = {
                        rotationZ = -90f
                        transformOrigin = TransformOrigin.Center
                    }
                )
            }
        },
        modifier = modifier
            .width(width)
            .height(height)
    )
}
