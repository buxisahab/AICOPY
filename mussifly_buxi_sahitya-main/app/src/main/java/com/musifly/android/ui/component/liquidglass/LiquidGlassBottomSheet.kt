package com.musifly.android.ui.component.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musifly.android.constants.*
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.layerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.blur
import com.musifly.android.ui.component.liquidglass.backdrop.effects.colorControls
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.shadow.Shadow

/**
 * Liquid Glass themed Bottom Sheet
 * Applies floating glass effect to bottom sheet surface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidGlassBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    shape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    containerColor: Color = Color.Transparent,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = Color.Unspecified,
    dragHandle: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val isLightTheme = !isSystemInDarkTheme()

    val actualScrimColor = if (scrimColor != Color.Unspecified) {
        scrimColor
    } else {
        if (isLightTheme) Color(0xFF29293A).copy(0.23f) else Color(0xFF121212).copy(0.56f)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = Color.Transparent,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        scrimColor = actualScrimColor,
        dragHandle = dragHandle
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val surfaceColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f) else Color(0xFF121212).copy(0.4f)
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
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
                        shadow = { Shadow.Default },
                        onDrawSurface = { drawRect(surfaceColor) }
                    )
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                content()
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
