package com.musifly.android.ui.component.liquidglass.backdrop.shadow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight

@Immutable
data class Shadow(
    val radius: androidx.compose.ui.unit.Dp = 24f.dp,
    val offset: androidx.compose.ui.unit.DpOffset = androidx.compose.ui.unit.DpOffset(0f.dp, radius / 6f),
    val color: Color = Color.Black.copy(alpha = 0.1f),
    @androidx.annotation.FloatRange(from = 0.0, to = 1.0) val alpha: Float = 1f,
    val blendMode: BlendMode = DrawScope.DefaultBlendMode
) {

    companion object {

        @androidx.compose.runtime.Stable
        val Default: Shadow = Shadow()
    }
}
