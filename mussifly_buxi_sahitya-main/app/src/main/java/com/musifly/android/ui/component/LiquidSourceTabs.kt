package com.musifly.android.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.RowScope
import com.musifly.android.constants.*
import com.musifly.android.streamingengines.StreamSourceManager
import com.musifly.android.ui.component.liquidglass.backdrop.Backdrop
import com.musifly.android.ui.component.liquidglass.catalog.components.LiquidBottomTab
import com.musifly.android.ui.component.liquidglass.catalog.components.LiquidBottomTabs
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.utils.rememberPreference

@Composable

fun LiquidSourceTabs(
    currentSource: StreamSourceManager.StreamType,
    backdrop: Backdrop,
    modifier: Modifier = Modifier
) {
    val isLiquidGlass = isLiquidGlassActive()
    
    if (!isLiquidGlass) {
        // Simple fallback when liquid glass is off
        SimpleSourceTabs(currentSource, modifier)
        return
    }
    
    // Use LiquidBottomTabs component for source selection  
    val sourceTabs = StreamSourceManager.StreamType.entries
    var internalSelectedIndex by remember { mutableIntStateOf(sourceTabs.indexOf(currentSource)) }
    var lastSourceChangeTime by remember { mutableStateOf(0L) }
    
    // Sync external changes to internal state
    LaunchedEffect(currentSource) {
        internalSelectedIndex = sourceTabs.indexOf(currentSource)
    }
    
    LiquidBottomTabs(
        tabsCount = sourceTabs.size,
        selectedTabIndex = { internalSelectedIndex },
        onTabSelected = { index ->
            // Only handle animation, not source change
            internalSelectedIndex = index
        },
        backdrop = backdrop,
        modifier = modifier
    ) {
        for ((index, source) in sourceTabs.withIndex()) {
            val sourceColor = Color(StreamSourceManager.getSourceColor(source))
            val isSelected = internalSelectedIndex == index
            
            LiquidBottomTab(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    // Prevent rapid double-clicks (debounce)
                    if (currentTime - lastSourceChangeTime > 100L) {
                        lastSourceChangeTime = currentTime
                        internalSelectedIndex = index
                        StreamSourceManager.onSourceChange(source)
                    }
                }
            ) {
                Text(
                    text = when(source) {
                        StreamSourceManager.StreamType.YOUTUBE -> "YouTube"
                        StreamSourceManager.StreamType.YT_MUSIC -> "YT Music"
                        StreamSourceManager.StreamType.JIOSAAVN -> "JioSaavn"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) sourceColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}


@Composable
private fun SimpleSourceTabs(
    currentSource: StreamSourceManager.StreamType,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StreamSourceManager.StreamType.entries.forEach { source ->
            val isSelected = currentSource == source

            OutlinedButton(
                onClick = {
                    if (currentSource != source) {
                        StreamSourceManager.onSourceChange(source)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                colors = if (isSelected) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                border = if (isSelected) null else BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(50), // Standard Pill Shape
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text(
                    text = StreamSourceManager.getDisplayName(source),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
