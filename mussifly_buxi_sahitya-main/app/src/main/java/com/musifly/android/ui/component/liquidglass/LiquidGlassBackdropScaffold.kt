package com.musifly.android.ui.component.liquidglass

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.layerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.graphics.luminance

/**
 * Liquid Glass Backdrop Scaffold
 * Provides a backdrop background similar to AndroidLiquidGlass settings screens
 * Creates a gradient background that can be blurred by child components
 */

@Composable
fun LiquidGlassBackdropScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(backdrop: com.musifly.android.ui.component.liquidglass.backdrop.backdrops.LayerBackdrop) -> Unit
) {
    // Check luminance to respect app-specific theme overrides (not just system)
    val isLightTheme = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() > 0.5f
    val backdrop = rememberLayerBackdrop()

    // Create gradient background matching AndroidLiquidGlass style
    val gradientColors = if (isLightTheme) {
        listOf(
            Color(0xFFF9F9F9), // Very light gray (almost white)
            Color(0xFFF2F2F7), // iOS/Telegram Settings Gray
            Color(0xFFECECEC)  // Slight shadow at bottom
        )
    } else {
        listOf(
            Color(0xFF000000),
            Color(0xFF0A0A0A),
            Color(0xFF121212),
            Color(0xFF0F0A1E)
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Background gradient with backdrop layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop)
                .background(
                    brush = Brush.verticalGradient(gradientColors)
                )
        )

        // Content with backdrop access
        content(backdrop)
    }
}

