package com.musifly.android.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.HighlightStyle
import com.musifly.android.ui.component.liquidglass.catalog.utils.rememberUISensor

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val isLiquidGlass = isLiquidGlassActive()
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 22.sp 
                ),
                // In Liquid Glass Light mode, use standard onMethod color (blackish) instead of primary
                color = if (isLiquidGlass && !isDarkTheme) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
            )
        }
        
        if (isLiquidGlass) {
            val uiSensor = rememberUISensor()
            val backdrop = rememberLayerBackdrop()
             
            val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

            Box(
                modifier = Modifier
                   .fillMaxWidth()
                   .padding(horizontal = 16.dp)
                   .drawBackdrop(
                       backdrop = backdrop,
                       shape = { RoundedCornerShape(26.dp) },
                       effects = {
                            vibrancy()
                            lens(30.dp.toPx(), 60.dp.toPx()) // Subtle lens
                       },
                       highlight = {
                           Highlight(
                               style = HighlightStyle.Default(
                                   angle = uiSensor.gravityAngle,
                                   falloff = 1.8f
                               )
                           )
                       },
                       onDrawSurface = {
                            if (isDarkTheme) {
                                val brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                                drawRect(brush)
                                drawRect(Color.White.copy(alpha = 0.05f))
                            } else {
                                // Light Mode Liquid Glass: White translucent "Milk Glass" look
                                drawRect(Color.White.copy(alpha = 0.85f))
                                // Optional subtle gradient for depth
                                val brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.1f),
                                        Color.White.copy(alpha = 0.0f)
                                    )
                                )
                                drawRect(brush)
                            }
                       }
                   )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
            }
        }
    }
}
