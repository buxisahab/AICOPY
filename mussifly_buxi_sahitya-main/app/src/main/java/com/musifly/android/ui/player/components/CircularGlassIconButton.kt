package com.musifly.android.ui.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberCombinedBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.HighlightStyle
import com.musifly.android.ui.component.liquidglass.backdrop.rememberBackdrop
import com.musifly.android.ui.component.liquidglass.catalog.utils.rememberUISensor
import com.musifly.android.ui.component.liquidglass.backdrop.utils.InteractiveHighlight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.fastCoerceAtMost
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import com.musifly.android.ui.component.liquidglass.backdrop.rememberBackdrop
import com.musifly.android.ui.component.liquidglass.catalog.utils.rememberUISensor

/**
 * Circular glass icon button with iOS Control Center-style effects
 * Used across all player layouts when Liquid Glass theme is active
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CircularGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    buttonSize: Dp = 56.dp,
    content: @Composable BoxScope.(contentScale: Float) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope
        )
    }
    
    val uiSensor = rememberUISensor()
    val backdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier
            .size(buttonSize)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    // Adaptive Lensing based on interaction
                    val progress = interactiveHighlight.pressProgress
                    val refHeight = lerp(35f, 45f, progress).dp.toPx()
                    val refAmount = lerp(70f, 90f, progress).dp.toPx()
                    lens(refHeight, refAmount, depthEffect = true, chromaticAberration = true)
                },
                layerBlock = {
                    val width = size.width
                    val height = size.height

                    val progress = interactiveHighlight.pressProgress
                    // Adaptive scaling for liquid feel
                    val scale = lerp(1f, 1f + 14.dp.toPx() / size.height, progress)

                    val maxOffset = size.minDimension
                    val initialDerivative = 0.15f
                    val offset = interactiveHighlight.offset
                    val limitX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                    val limitY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)
                    
                    translationX = limitX
                    translationY = limitY

                    val maxDragScale = 14.dp.toPx() / size.height
                    val offsetAngle = atan2(offset.y, offset.x)
                    scaleX =
                        scale +
                                maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                (width / height).fastCoerceAtMost(1f)
                    scaleY =
                        scale +
                                maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                (height / width).fastCoerceAtMost(1f)
                },
                highlight = {
                    Highlight(
                        style = HighlightStyle.Default(
                            angle = uiSensor.gravityAngle,
                            falloff = lerp(2f, 1.5f, interactiveHighlight.pressProgress)
                        )
                    )
                },
                onDrawSurface = {
                    // Surface brightens slightly on press
                    val alpha = lerp(0.12f, 0.2f, interactiveHighlight.pressProgress)
                    drawRect(Color.White.copy(alpha = alpha))
                }
            )
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                }
            )
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier),
        contentAlignment = Alignment.Center
    ) {
        val pressProgress = interactiveHighlight.pressProgress
        val contentScale = lerp(1f, 0.95f, pressProgress)
        content(contentScale)
    }
}

@Composable
fun CircularGlassIconButton(
    painter: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    iconSize: Dp = 24.dp,
    buttonSize: Dp = 56.dp,
    tint: Color = Color.White
) {
    CircularGlassButton(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        buttonSize = buttonSize
    ) { contentScale ->
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                }
        )
    }
}
