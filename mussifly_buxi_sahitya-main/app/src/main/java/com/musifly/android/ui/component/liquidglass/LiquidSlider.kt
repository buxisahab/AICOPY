package com.musifly.android.ui.component.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.musifly.android.ui.component.liquidglass.backdrop.Backdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.layerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberCombinedBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.utils.DampedDragAnimation
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.blur
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.shadow.InnerShadow
import com.musifly.android.ui.component.liquidglass.backdrop.shadow.Shadow
import com.musifly.android.ui.component.liquidglass.shapes.Capsule
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LiquidSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    thumbEnabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val isLightTheme = !isSystemInDarkTheme()
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val trackColor =
        if (isLightTheme) Color(0xFF787878).copy(0.2f)
        else Color(0xFF787880).copy(0.36f)

    val trackBackdrop = rememberLayerBackdrop()

    BoxWithConstraints(
        modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = constraints.maxWidth

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        var isDragging by remember { mutableStateOf(false) }
        
        val dampedDragAnimation = remember(animationScope, valueRange) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                onDragStarted = {
                    isDragging = true
                },
                onDragStopped = {
                    isDragging = false
                    if (didDrag) {
                        onValueChange(targetValue)
                        didDrag = false
                        onValueChangeFinished?.invoke()
                    }
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) {
                        didDrag = dragAmount.x != 0f
                    }
                    val range = valueRange.endInclusive - valueRange.start
                    val delta = if (trackWidth > 0) range * (dragAmount.x / trackWidth) else 0f
                    val newValue = if (isLtr) (targetValue + delta).coerceIn(valueRange)
                                   else (targetValue - delta).coerceIn(valueRange)
                    onValueChange(newValue)
                }
            )
        }
        
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value() }
                .collectLatest { value ->
                    // Only update if not currently dragging
                    if (!isDragging && dampedDragAnimation.targetValue != value) {
                        dampedDragAnimation.updateValue(value)
                    }
                }
        }

        // Touch Target - High priority for user interaction
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(animationScope, trackWidth, valueRange) {
                    // Unified drag gesture that handles both tap and drag
                    var dragStartPosition: Offset? = null
                    var hasDragged = false
                    
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragStartPosition = offset
                            hasDragged = false
                            dampedDragAnimation.press()
                        },
                        onDragEnd = {
                            // If didn't drag much, treat as tap (jump to position)
                            if (!hasDragged && dragStartPosition != null) {
                                val position = dragStartPosition!!
                                val range = valueRange.endInclusive - valueRange.start
                                val delta = if (trackWidth > 0) range * (position.x / trackWidth) else 0f
                                val targetValue =
                                    (if (isLtr) valueRange.start + delta
                                    else valueRange.endInclusive - delta)
                                        .coerceIn(valueRange)
                                dampedDragAnimation.animateToValue(targetValue)
                                onValueChange(targetValue)
                                onValueChangeFinished?.invoke()
                            } else {
                                dampedDragAnimation.release()
                                // Drag release is handled by onDragStopped in DampedDragAnimation
                            }
                            isDragging = false
                            dragStartPosition = null
                            hasDragged = false
                        },
                        onDragCancel = {
                            isDragging = false
                            dragStartPosition = null
                            hasDragged = false
                            dampedDragAnimation.release()
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        hasDragged = true
                        val range = valueRange.endInclusive - valueRange.start
                        val delta = if (trackWidth > 0) range * (dragAmount / trackWidth) else 0f
                        val currentValue = dampedDragAnimation.targetValue
                        val newValue = if (isLtr) (currentValue + delta).coerceIn(valueRange)
                                      else (currentValue - delta).coerceIn(valueRange)
                        
                        // Immediately update visual state and external listener
                        dampedDragAnimation.snapTo(newValue)
                        onValueChange(newValue)
                    }
                }
        )

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(Capsule)
                    .background(trackColor)
                    .height(6f.dp)
                    .fillMaxWidth()
            )

            Box(
                Modifier
                    .clip(Capsule)
                    .background(accentColor)
                    .height(6f.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * dampedDragAnimation.progress).fastRoundToInt()
                        layout(width, placeable.height) {
                            placeable.place(0, 0)
                        }
                    }
            )
        }

        if (thumbEnabled) {
            Box(
                Modifier
                    .graphicsLayer {
                        translationX =
                            (-size.width / 2f + trackWidth * dampedDragAnimation.progress)
                                .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                    }
                    .then(dampedDragAnimation.modifier)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(
                            backdrop,
                            rememberBackdrop(trackBackdrop) { drawBackdrop ->
                                val progress = dampedDragAnimation.pressProgress
                                val scaleX = lerp(2f / 3f, 1f, progress)
                                val scaleY = lerp(0f, 1f, progress)
                                scale(scaleX, scaleY) {
                                    drawBackdrop()
                                }
                            }
                        ),
                        shape = { Capsule },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            blur(8f.dp.toPx() * (1f - progress))
                            lens(
                                10f.dp.toPx() * progress,
                                14f.dp.toPx() * progress,
                                chromaticAberration = true
                            )
                        },
                        highlight = {
                            val progress = dampedDragAnimation.pressProgress
                            Highlight.Ambient.copy(
                                width = Highlight.Ambient.width / 1.5f,
                                blurRadius = Highlight.Ambient.blurRadius / 1.5f,
                                alpha = progress
                            )
                        },
                        shadow = {
                            Shadow(
                                radius = 4f.dp,
                                color = Color.Black.copy(alpha = 0.05f)
                            )
                        },
                        innerShadow = {
                            val progress = dampedDragAnimation.pressProgress
                            InnerShadow(
                                radius = 4f.dp * progress,
                                alpha = progress
                            )
                        },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                            val velocity = dampedDragAnimation.velocity / 10f
                            scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                            scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            drawRect(Color.White.copy(alpha = 1f - progress))
                        }
                    )
                    .size(40f.dp, 24f.dp)
            )
        }
    }
}
