package com.musifly.android.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musifly.android.ui.component.liquidglass.LiquidGlassButton
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.player.components.CircularGlassIconButton
import com.musifly.android.ui.player.components.CircularGlassButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.graphics.graphicsLayer

/**
 * A unified button component that conditionally renders a Liquid Glass button or a standard Material 3 button.
 * Use this for all standard text/icon-text buttons in the app to support theme switching.
 */
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    if (isLiquidGlassActive()) {
        LiquidGlassButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            isPrimary = isPrimary,
            content = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    content = content
                )
            }
        )
    } else {
        if (isPrimary) {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                contentPadding = contentPadding,
                content = content
            )
        } else {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                contentPadding = contentPadding,
                content = content
            )
        }
    }
}

/**
 * A unified icon button component that conditionally renders a Circular Liquid Glass button or a standard Material 3 IconButton.
 * Use this for standalone action icons (like Play, Shuffle, Back, Settings) to support theme switching.
 */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current,
    size: Dp = 48.dp, // Default touch target size
    iconSize: Dp = 24.dp,
    onLongClick: (() -> Unit)? = null
) {
    if (isLiquidGlassActive()) {
        CircularGlassIconButton(
            painter = painter,
            contentDescription = contentDescription,
            onClick = onClick,
            modifier = modifier, // Note: CircularGlassIconButton handles click internally
            onLongClick = onLongClick,
            buttonSize = size,
            iconSize = iconSize,
            tint = tint
        )
    } else {
        if (onLongClick != null) {
             com.musifly.android.ui.component.IconButton(
                onClick = onClick,
                onLongClick = onLongClick,
                modifier = modifier.size(size),
                enabled = enabled
            ) {
                 Icon(
                    painter = painter,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(iconSize)
                )
            }
        } else {
            IconButton(
                onClick = onClick,
                modifier = modifier.size(size), // Enforce size
                enabled = enabled
            ) {
                Icon(
                    painter = painter,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

/**
 * A unified Floating Action Button that conditionally renders a Circular Glass Button or a standard Material 3 FAB.
 */
@Composable
fun AppFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable () -> Unit
) {
    if (isLiquidGlassActive()) {
        CircularGlassButton(
            onClick = onClick,
            modifier = modifier,
            buttonSize = 56.dp
        ) { scale ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                content()
            }
        }
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = containerColor,
            contentColor = contentColor,
            content = content
        )
    }
}
