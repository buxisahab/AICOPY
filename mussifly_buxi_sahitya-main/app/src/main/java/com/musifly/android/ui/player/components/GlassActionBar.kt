package com.musifly.android.ui.player.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.fastCoerceAtMost
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.HighlightStyle
import com.musifly.android.ui.component.liquidglass.backdrop.utils.InteractiveHighlight
import com.musifly.android.ui.component.liquidglass.catalog.utils.rememberUISensor
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun GlassActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope
        )
    }
    
    val uiSensor = rememberUISensor()
    val backdrop = rememberLayerBackdrop()

    Row(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(percent = 50) },
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
                    // Adaptive scaling for liquid feel - slightly less scale for bar
                    val scale = lerp(1f, 1f + 8.dp.toPx() / size.height, progress)

                    val maxOffset = size.minDimension
                    val initialDerivative = 0.15f
                    val offset = interactiveHighlight.offset
                    val limitX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                    val limitY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)
                    
                    translationX = limitX
                    translationY = limitY

                    val maxDragScale = 8.dp.toPx() / size.height
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
            .then(interactiveHighlight.modifier)
            .then(interactiveHighlight.gestureModifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}
