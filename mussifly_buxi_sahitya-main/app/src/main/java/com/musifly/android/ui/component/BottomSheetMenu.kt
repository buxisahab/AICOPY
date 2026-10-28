package com.musifly.android.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import com.musifly.android.ui.utils.top
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.utils.rememberEnumPreference
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.blur
import com.musifly.android.ui.component.liquidglass.backdrop.effects.colorControls
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.LocalBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop

val LocalMenuState = compositionLocalOf { MenuState() }

@Stable
class MenuState(
    isVisible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var isVisible by mutableStateOf(isVisible)
    var content by mutableStateOf(content)

    fun show(content: @Composable ColumnScope.() -> Unit) {
        isVisible = true
        this.content = content
    }

    fun dismiss() {
        isVisible = false
    }
}

@Composable
fun BottomSheetMenu(
    modifier: Modifier = Modifier,
    state: MenuState,
    background: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation),
) {
    val focusManager = LocalFocusManager.current
    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    if (isLiquidGlass) {
        if (state.isVisible) {
            LiquidGlassDialog(
                onDismiss = { state.dismiss() },
                content = { state.content(this) }
            )
        }
        LaunchedEffect(state.isVisible) {
            if (state.isVisible) {
                focusManager.clearFocus()
            }
        }
        return
    }

    AnimatedVisibility(
        visible = state.isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        BackHandler {
            state.dismiss()
        }

        Spacer(
            modifier =
                Modifier
                    .pointerInput(Unit) {
                        detectTapGestures {
                            state.dismiss()
                        }
                    }
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                    .fillMaxSize(),
        )
    }

    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                    .padding(top = 48.dp)
                    .clip(ShapeDefaults.Large.top())
                    .background(background),
        ) {
            state.content(this)
        }
    }

    LaunchedEffect(state.isVisible) {
        if (state.isVisible) {
            focusManager.clearFocus()
        }
    }
}
