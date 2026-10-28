package com.musifly.android.ui.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import com.musifly.android.ui.component.DownloadButton
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberCombinedBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.utils.InteractiveHighlight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.fastCoerceAtMost
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.HighlightStyle
import com.musifly.android.ui.component.liquidglass.backdrop.rememberBackdrop
import com.musifly.android.ui.component.liquidglass.catalog.utils.rememberUISensor

/**
 * Circular Glass Download Button with iOS Control Center-style glass effects
 * Wraps the standard DownloadButton with liquid glass styling
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CircularGlassDownloadButton(
    download: Download?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    buttonSize: Dp = 56.dp,
    iconSize: Dp = 24.dp,
    tint: Color = Color.White
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
                    val progress = interactiveHighlight.pressProgress
                    val refHeight = lerp(35f, 45f, progress).dp.toPx()
                    val refAmount = lerp(70f, 90f, progress).dp.toPx()
                    lens(refHeight, refAmount, depthEffect = true, chromaticAberration = true)
                },
                layerBlock = {
                    val width = size.width
                    val height = size.height

                    val progress = interactiveHighlight.pressProgress
                    val scale = lerp(1f, 1f + 14.dp.toPx() / size.height, progress)

                    val maxOffset = size.minDimension
                    val initialDerivative = 0.15f
                    val offset = interactiveHighlight.offset
                    translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                    translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)

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
        DownloadButton(
            download = download, 
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    val s = lerp(1f, 0.95f, interactiveHighlight.pressProgress)
                    scaleX = s
                    scaleY = s
                },
            iconSize = iconSize,
            tint = tint,
            onClick = {}, // Handled by parent container
            onLongClick = {} // Handled by parent container
        )
    }
}
