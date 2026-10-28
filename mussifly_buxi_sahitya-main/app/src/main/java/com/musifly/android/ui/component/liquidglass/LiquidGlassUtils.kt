package com.musifly.android.ui.component.liquidglass

import androidx.compose.runtime.Composable
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.utils.rememberEnumPreference

/**
 * Helper to check if Liquid Glass theme is currently active
 * Use this throughout the app to conditionally apply liquid glass effects
 * 
 * Usage:
 * ```kotlin
 * if (isLiquidGlassActive()) {
 *     LiquidGlassComponent()
 * } else {
 *     StandardComponent()
 * }
 * ```
 */
@Composable
fun isLiquidGlassActive(): Boolean {
    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    return themeMode == ThemeMode.LIQUID_GLASS
}

/**
 * Execute a composable only when Liquid Glass theme is active
 * 
 * Usage:
 * ```kotlin
 * withLiquidGlass {
 *     // This code only runs when Liquid Glass is active
 *     Box(modifier = Modifier.liquidGlassProminent()) { }
 * }
 * ```
 */
@Composable
fun withLiquidGlass(content: @Composable () -> Unit) {
    if (isLiquidGlassActive()) {
        content()
    }
}

/**
 * Choose between liquid glass or standard composable based on theme
 * 
 * Usage:
 * ```kotlin
 * chooseLiquidGlass(
 *     liquidGlass = { LiquidGlassCard { } },
 *     standard = { Card { } }
 * )
 * ```
 */
@Composable
fun <T> chooseLiquidGlass(
    liquidGlass: @Composable () -> T,
    standard: @Composable () -> T
): T {
    return if (isLiquidGlassActive()) {
        liquidGlass()
    } else {
        standard()
    }
}
