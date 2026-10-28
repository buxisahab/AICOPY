package com.musifly.android.ui.component.liquidglass.backdrop.effects

import android.graphics.RenderEffect
import android.os.Build
import com.musifly.android.ui.component.liquidglass.backdrop.BackdropEffectScope

fun BackdropEffectScope.effect(effect: RenderEffect) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    val currentEffect = renderEffect
    renderEffect =
        if (currentEffect != null) {
            RenderEffect.createChainEffect(effect, currentEffect)
        } else {
            effect
        }
}
