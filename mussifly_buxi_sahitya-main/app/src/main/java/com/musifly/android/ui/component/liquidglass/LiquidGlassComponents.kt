package com.musifly.android.ui.component.liquidglass

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.musifly.android.constants.*
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.blur
import com.musifly.android.ui.component.liquidglass.backdrop.effects.colorControls
import com.musifly.android.ui.component.liquidglass.backdrop.effects.exposureAdjustment
import com.musifly.android.ui.component.liquidglass.backdrop.effects.gammaAdjustment
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.shadow.InnerShadow
import com.musifly.android.utils.rememberPreference

/**
 * Liquid Glass themed Tab Row
 * Applies floating glass effect to tab container and indicator
 */
@Composable
fun LiquidGlassTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    edgePadding: Dp = 52.dp,
    indicator: @Composable (tabPositions: List<TabPosition>) -> Unit = { tabPositions ->
        LiquidGlassTabIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
        )
    },
    divider: @Composable () -> Unit = {},
    tabs: @Composable () -> Unit
) {
    // Read user preferences
    val (blurRadius) = rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 3f)
    val (lensHeight) = rememberPreference(LiquidGlassLensHeightKey, defaultValue = 43f)
    val (lensAmount) = rememberPreference(LiquidGlassLensAmountKey, defaultValue = -43f)
    val (vibrancy) = rememberPreference(LiquidGlassVibrancyKey, defaultValue = true)
    
    val (brightness) = rememberPreference(LiquidGlassBrightnessKey, defaultValue = 0f)
    val (contrast) = rememberPreference(LiquidGlassContrastKey, defaultValue = 1f)
    val (saturation) = rememberPreference(LiquidGlassSaturationKey, defaultValue = 1f)
    val (exposure) = rememberPreference(LiquidGlassExposureKey, defaultValue = 0f)
    val (gamma) = rememberPreference(LiquidGlassGammaKey, defaultValue = 1f)

    val backdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(0.dp) },
                effects = {
                    colorControls(
                        brightness = brightness,
                        contrast = contrast,
                        saturation = if (vibrancy) saturation * 1.5f else saturation
                    )
                    exposureAdjustment(exposure)
                    gammaAdjustment(gamma)
                    blur(blurRadius.dp.toPx() * 0.5f)
                },
                onDrawSurface = {
                    drawRect(Color.Black.copy(0.15f))
                }
            )
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.fillMaxWidth(),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            edgePadding = edgePadding,
            indicator = indicator,
            divider = divider,
            tabs = tabs
        )
    }
}

/**
 * Liquid Glass themed Tab Indicator
 * Shows a glassy indicator under the selected tab
 */
@Composable
fun LiquidGlassTabIndicator(
    modifier: Modifier = Modifier
) {
    val (lensHeight) = rememberPreference(LiquidGlassLensHeightKey, defaultValue = 43f)
    val (lensAmount) = rememberPreference(LiquidGlassLensAmountKey, defaultValue = -43f)
    val (depthEffect) = rememberPreference(LiquidGlassDepthEffectKey, defaultValue = false)
    val (chromaticAberration) = rememberPreference(LiquidGlassChromaticAberrationKey, defaultValue = true)
    val (chromaticAberrationAmount) = rememberPreference(LiquidGlassChromaticAberrationAmountKey, defaultValue = 1.0f)
    val (eccentricFactor) = rememberPreference(LiquidGlassEccentricFactorKey, defaultValue = 0.37f)
    
    val backdrop = rememberLayerBackdrop()
    val primaryColor = MaterialTheme.colorScheme.primary // Capture color outside lambda

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(topStart = 1.5.dp, topEnd = 1.5.dp) },
                effects = {
                    lens(
                        refractionHeight = lensHeight.dp.toPx() * 0.3f,
                        refractionAmount = lensAmount.dp.toPx() * 0.5f,
                        depthEffect = depthEffect,
                        chromaticAberration = chromaticAberration
                    )
                },
                highlight = { Highlight.Default },
                onDrawSurface = {
                    drawRect(primaryColor.copy(0.8f)) // Use captured color
                }
            )
    )
}

/**
 * Liquid Glass themed Card/Surface
 * Reusable component for any card or surface element
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Float = 16f,
    surfaceAlpha: Float = 0.25f,
    content: @Composable ColumnScope.() -> Unit
) {
    val (blurRadius) = rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 3f)
    val (lensHeight) = rememberPreference(LiquidGlassLensHeightKey, defaultValue = 43f)
    val (lensAmount) = rememberPreference(LiquidGlassLensAmountKey, defaultValue = -43f)
    val (vibrancy) = rememberPreference(LiquidGlassVibrancyKey, defaultValue = true)
    
    val (brightness) = rememberPreference(LiquidGlassBrightnessKey, defaultValue = 0f)
    val (contrast) = rememberPreference(LiquidGlassContrastKey, defaultValue = 1f)
    val (saturation) = rememberPreference(LiquidGlassSaturationKey, defaultValue = 1f)
    val (exposure) = rememberPreference(LiquidGlassExposureKey, defaultValue = 0f)
    val (gamma) = rememberPreference(LiquidGlassGammaKey, defaultValue = 1f)
    
    val (depthEffect) = rememberPreference(LiquidGlassDepthEffectKey, defaultValue = false)
    val (chromaticAberration) = rememberPreference(LiquidGlassChromaticAberrationKey, defaultValue = true)
    val (chromaticAberrationAmount) = rememberPreference(LiquidGlassChromaticAberrationAmountKey, defaultValue = 1.0f)
    val (eccentricFactor) = rememberPreference(LiquidGlassEccentricFactorKey, defaultValue = 0.37f)
    val (innerShadowRadius) = rememberPreference(LiquidGlassInnerShadowRadiusKey, defaultValue = 0f)
    val (innerShadowAlpha) = rememberPreference(LiquidGlassInnerShadowAlphaKey, defaultValue = 0f)

    val backdrop = rememberLayerBackdrop()
    
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp)) // Clip ripple to shape
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        onClick = onClick
                    )
                } else Modifier
            )
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(cornerRadius.dp) },
                effects = {
                    colorControls(
                        brightness = brightness,
                        contrast = contrast,
                        saturation = if (vibrancy) saturation * 1.5f else saturation
                    )
                    exposureAdjustment(exposure)
                    gammaAdjustment(gamma)
                    blur(blurRadius.dp.toPx())
                    lens(
                        refractionHeight = lensHeight.dp.toPx() * 0.4f,
                        refractionAmount = lensAmount.dp.toPx() * 0.6f,
                        depthEffect = depthEffect,
                        chromaticAberration = chromaticAberration
                    )
                },
                highlight = { Highlight.Plain },
                innerShadow = if (innerShadowRadius > 0f && innerShadowAlpha > 0f) ({
                    InnerShadow(
                        radius = innerShadowRadius.dp,
                        color = Color.Black.copy(alpha = innerShadowAlpha)
                    )
                }) else null,
                onDrawSurface = {
                    drawRect(Color.Black.copy(surfaceAlpha))
                }
            )
            .padding(12.dp)
    ) {
        content()
    }
}

/**
 * Adaptive Switch that toggles between Material 3 Switch and Liquid Glass Toggle
 */
@Composable
fun AdaptiveSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (isLiquidGlassActive()) {
        LiquidToggle(
            selected = { checked },
            onSelect = onCheckedChange,
            modifier = modifier
        )
    } else {
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = modifier,
            enabled = enabled,
            thumbContent = {
                androidx.compose.material3.Icon(
                    painter = androidx.compose.ui.res.painterResource(
                        id = if (checked) com.musifly.android.R.drawable.check else com.musifly.android.R.drawable.close
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize),
                )
            }
        )
    }
}
