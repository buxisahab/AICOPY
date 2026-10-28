package com.musifly.android.ui.component.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.graphicsLayer
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.blur
import com.musifly.android.ui.component.liquidglass.backdrop.effects.colorControls
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.Backdrop
import com.musifly.android.ui.component.liquidglass.shapes.Capsule
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.fastCoerceAtMost
import com.musifly.android.ui.component.liquidglass.backdrop.utils.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

val LocalBackdrop = compositionLocalOf<Backdrop?> { null }

@Composable
fun LiquidGlassDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    buttons: (@Composable RowScope.() -> Unit)? = null,
    bottomBar: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val isLightTheme = !isSystemInDarkTheme()
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f) else Color(0xFF121212).copy(0.4f)
    val dimColor = if (isLightTheme) Color(0xFF29293A).copy(0.23f) else Color(0xFF121212).copy(0.56f)
    
    // Try to get the backdrop from the parent (Activity/Screen level) if provided via LocalBackdrop.
    // Otherwise, create a new one (which will likely be empty in a separate Dialog window).
    val backdrop = LocalBackdrop.current ?: rememberLayerBackdrop()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        ),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalBackdrop provides backdrop
        ) {
            Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dimColor)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 40.dp) // Maximized width
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        enabled = true,
                        onClick = {}
                    )
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(48.dp) },
                        effects = {
                            colorControls(
                                brightness = if (isLightTheme) 0.2f else 0f, 
                                saturation = 1.5f
                            )
                            blur(if (isLightTheme) 16.dp.toPx() else 8.dp.toPx())
                            lens(
                                refractionHeight = 24.dp.toPx(), 
                                refractionAmount = 48.dp.toPx(), 
                                depthEffect = true
                            )
                        },
                        highlight = { Highlight.Plain },
                        onDrawSurface = { drawRect(containerColor) }
                    )
            ) {
                Column(
                    modifier = modifier.fillMaxWidth()
                ) {
                    // Title Padding: 28, 24, 28, 12
                    if (title != null) {
                        Box(modifier = Modifier.padding(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 12.dp)) {
                            
                            val contentColor = if (isLightTheme) Color.Black else Color.White
                            ProvideTextStyle(
                                androidx.compose.ui.text.TextStyle(
                                    color = contentColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            ) { 
                                title() 
                            }
                        }
                    } else if (icon != null) {
                        Box(modifier = Modifier.padding(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 12.dp)) {
                            icon()
                        }
                    }
                    
                    // Content Padding: 24, 12, 24, 12
                    val contentModifier = if (isLightTheme) Modifier else Modifier.graphicsLayer { blendMode = androidx.compose.ui.graphics.BlendMode.Plus }
                    
                    Column(
                        modifier = Modifier
                            .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 12.dp)
                            .then(contentModifier)
                    ) {
                        val contentColor = if (isLightTheme) Color.Black else Color.White
                        ProvideTextStyle(
                            androidx.compose.ui.text.TextStyle(
                                color = contentColor.copy(0.68f),
                                fontSize = 15.sp
                            )
                        ) {
                             content()
                        }
                    }

                    // Buttons Padding: 24, 12, 24, 24
                    if (buttons != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = if (bottomBar == null) 24.dp else 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            content = buttons
                        )
                    }

                    if (bottomBar != null) {
                        Row(
                             modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp),
                             verticalAlignment = Alignment.CenterVertically,
                             content = bottomBar
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun LiquidGlassTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = if (!androidx.compose.foundation.isSystemInDarkTheme()) Color.Black else Color.White,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Start,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    backgroundColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val backdrop = LocalBackdrop.current ?: rememberLayerBackdrop()
    
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    
    // Base container color from reference
    val baseContainerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f) else Color(0xFF121212).copy(0.4f)
    
    // Final button background color
    val buttonColor = backgroundColor ?: if (isPrimary) {
        accentColor.copy(alpha = 0.8f)
    } else {
        baseContainerColor.copy(0.2f)
    }
    
    val textColor = if (isPrimary || backgroundColor != null) Color.White else contentColor

    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(
            animationScope = animationScope
        )
    }

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule },
                effects = {
                    blur(if (isLightTheme) 4.dp.toPx() else 8.dp.toPx())
                    lens(12.dp.toPx(), 24.dp.toPx())
                    if (isPrimary) {
                        colorControls(saturation = 1.2f)
                    }
                },
                layerBlock = if (enabled) {
                    {
                        val width = size.width
                        val height = size.height
                        
                        // Guard against zero size to prevent NaN/Inf in transformations
                        if (width > 0f && height > 0f) {
                            val progress = interactiveHighlight.pressProgress
                            val scale = lerp(1f, 1f + 12.dp.toPx() / height, progress)

                            val maxOffset = size.minDimension
                            val initialDerivative = 0.12f
                            val offset = interactiveHighlight.offset
                            
                            if (maxOffset > 0f) {
                                translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                                translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)
                            }

                            val maxDragScale = 12.dp.toPx() / height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX =
                                scale +
                                        maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                        (width / height).fastCoerceAtMost(1f)
                            scaleY =
                                scale +
                                        maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                        (height / width).fastCoerceAtMost(1f)
                        }
                    }
                } else {
                    null
                },
                onDrawSurface = {
                    drawRect(buttonColor)
                }
            )
            .clickable(
                interactionSource = null, 
                indication = null, 
                enabled = enabled, 
                onClick = onClick
            )
            .then(
                if (enabled) {
                    Modifier
                        .then(interactiveHighlight.modifier)
                        .then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                }
            )
            .height(48.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Provide LocalContentColor for children
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides textColor
        ) {
            content()
        }
    }
}

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    backgroundColor: Color? = null
) {
    LiquidGlassButton(
        onClick = onClick,
        modifier = modifier,
        isPrimary = isPrimary,
        enabled = enabled,
        backgroundColor = backgroundColor
    ) {
        androidx.compose.foundation.text.BasicText(
            text = text,
            style = androidx.compose.ui.text.TextStyle(
                color = androidx.compose.material3.LocalContentColor.current,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
