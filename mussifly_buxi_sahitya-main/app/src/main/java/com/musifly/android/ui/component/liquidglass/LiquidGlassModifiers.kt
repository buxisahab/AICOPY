package com.musifly.android.ui.component.liquidglass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.musifly.android.constants.*
import com.musifly.android.ui.component.liquidglass.backdrop.Backdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.*
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.HighlightStyle
import com.musifly.android.ui.component.liquidglass.backdrop.shadow.InnerShadow
import com.musifly.android.ui.component.liquidglass.backdrop.shadow.Shadow
import com.musifly.android.utils.rememberPreference

/**
 * Extension function to apply liquid glass effect to any Modifier
 * Usage: Modifier.liquidGlass()
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: Backdrop? = null,
    cornerRadius: Float = 0f,
    surfaceAlpha: Float = 0.05f,
    enableHighlight: Boolean = false,
    enableShadow: Boolean = false,
    blurMultiplier: Float = 1f,
    lensMultiplier: Float = 1f,
    saturation: Float? = null,
    highlightIntensity: Float? = null
): Modifier {
    val (blurRadius) = rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 3f)
    val (lensHeight) = rememberPreference(LiquidGlassLensHeightKey, defaultValue = 43f)
    val (lensAmount) = rememberPreference(LiquidGlassLensAmountKey, defaultValue = -43f)
    val (vibrancy) = rememberPreference(LiquidGlassVibrancyKey, defaultValue = true)
    
    // NEW DETAILED PREFERENCES
    val (chromaticAberration) = rememberPreference(LiquidGlassChromaticAberrationKey, defaultValue = true)
    val (chromaticAberrationAmount) = rememberPreference(LiquidGlassChromaticAberrationAmountKey, defaultValue = 1.0f)
    val (enableDispersion) = rememberPreference(LiquidGlassEnableDispersionKey, defaultValue = true)
    val (eccentricFactor) = rememberPreference(LiquidGlassEccentricFactorKey, defaultValue = 0.37f)
    val (bleedBlurRadius) = rememberPreference(LiquidGlassBleedBlurRadiusKey, defaultValue = 26f)
    val (enableUnsafeValues) = rememberPreference(LiquidGlassEnableUnsafeValuesKey, defaultValue = false)
    
    val (brightness) = rememberPreference(LiquidGlassBrightnessKey, defaultValue = 0f)
    val (contrast) = rememberPreference(LiquidGlassContrastKey, defaultValue = 1f)
    val (prefSaturation) = rememberPreference(LiquidGlassSaturationKey, defaultValue = 1f)
    val (exposure) = rememberPreference(LiquidGlassExposureKey, defaultValue = 0f)
    val (gamma) = rememberPreference(LiquidGlassGammaKey, defaultValue = 1f)
    
    val (prefHighlightIntensity) = rememberPreference(LiquidGlassHighlightIntensityKey, defaultValue = 0.5f)
    val (highlightAngle) = rememberPreference(LiquidGlassHighlightAngleKey, defaultValue = 45f)
    val (highlightFalloff) = rememberPreference(LiquidGlassHighlightFalloffKey, defaultValue = 1f)
    val (innerShadowRadius) = rememberPreference(LiquidGlassInnerShadowRadiusKey, defaultValue = 0f)
    val (innerShadowAlpha) = rememberPreference(LiquidGlassInnerShadowAlphaKey, defaultValue = 0f)
    val (depthEffect) = rememberPreference(LiquidGlassDepthEffectKey, defaultValue = false)
    
    val actualBackdrop = backdrop ?: rememberLayerBackdrop()

    // Resolve overrides
    val effectiveSaturation = saturation ?: if (vibrancy) prefSaturation * 1.5f else prefSaturation
    val effectiveHighlightIntensity = highlightIntensity ?: prefHighlightIntensity

    return this.drawBackdrop(
        backdrop = actualBackdrop,
        shape = { RoundedCornerShape(cornerRadius.dp) },
        effects = {
            colorControls(
                brightness = brightness,
                contrast = contrast,
                saturation = effectiveSaturation
            )
            exposureAdjustment(exposure)
            gammaAdjustment(gamma)

            blur((blurRadius * blurMultiplier).dp.toPx())
            if (lensMultiplier > 0f) {
                lens(
                    refractionHeight = lensHeight.dp.toPx() * 0.5f * lensMultiplier,
                    refractionAmount = (lensAmount * lensMultiplier).dp.toPx(),
                    depthEffect = depthEffect,
                    chromaticAberration = chromaticAberration
                )
            }
        },
        highlight = if (enableHighlight) ({ 
            Highlight(
                style = HighlightStyle.Default(
                    intensity = effectiveHighlightIntensity,
                    angle = highlightAngle,
                    falloff = highlightFalloff
                )
            )
        }) else null,
        innerShadow = if (innerShadowRadius > 0f && innerShadowAlpha > 0f) ({
            InnerShadow(
                radius = innerShadowRadius.dp,
                color = Color.Black.copy(alpha = innerShadowAlpha)
            )
        }) else null,
        shadow = if (enableShadow) ({ Shadow.Default }) else null,
        onDrawSurface = {
            if (surfaceAlpha > 0f) {
                drawRect(Color.Black.copy(surfaceAlpha))
            }
        }
    )
}

/**
 * Lighter variant for subtle background effects
 */
@Composable
fun Modifier.liquidGlassSubtle(): Modifier {
    return this.liquidGlass(
        surfaceAlpha = 0.03f,
        blurMultiplier = 0.5f,
        lensMultiplier = 0.3f
    )
}

/**
 * Stronger variant for prominent UI elements
 */
@Composable
fun Modifier.liquidGlassProminent(
    cornerRadius: Float = 16f
): Modifier {
    return this.liquidGlass(
        cornerRadius = cornerRadius,
        surfaceAlpha = 0.25f,
        enableHighlight = true,
        enableShadow = true,
        blurMultiplier = 1.2f,
        lensMultiplier = 1.0f
    )
}
