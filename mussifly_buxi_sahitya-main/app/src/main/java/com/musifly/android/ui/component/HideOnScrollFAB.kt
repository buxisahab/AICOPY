package com.musifly.android.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.ui.utils.isScrollingUp

@Composable
fun HideOnScrollFAB(
    visible: Boolean = true,
    lazyListState: LazyListState,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    val isScrollingUp = lazyListState.isScrollingUp()
    AnimatedVisibility(
        visible = visible && isScrollingUp,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            ),
    ) {
        AppFloatingActionButton(
            onClick = onClick,
        ) {
            val isLiquid = com.musifly.android.ui.component.liquidglass.isLiquidGlassActive()
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (isLiquid) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current
            )
        }
    }
}

@Composable
fun HideOnScrollFAB(
    visible: Boolean = true,
    lazyListState: LazyGridState,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    val isScrollingUp = lazyListState.isScrollingUp()
    AnimatedVisibility(
        visible = visible && isScrollingUp,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            ),
    ) {
        AppFloatingActionButton(
            onClick = onClick,
        ) {
            val isLiquid = com.musifly.android.ui.component.liquidglass.isLiquidGlassActive()
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (isLiquid) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current
            )
        }
    }
}

@Composable
fun HideOnScrollFAB(
    visible: Boolean = true,
    scrollState: ScrollState,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
) {
    val isScrollingUp = scrollState.isScrollingUp()
    AnimatedVisibility(
        visible = visible && isScrollingUp,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            ),
    ) {
        AppFloatingActionButton(
            onClick = onClick,
        ) {
            val isLiquid = com.musifly.android.ui.component.liquidglass.isLiquidGlassActive()
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (isLiquid) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.LocalContentColor.current
            )
        }
    }
}
