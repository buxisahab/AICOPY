package com.musifly.android.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.offline.Download
import com.musifly.android.R
import com.musifly.android.constants.LiquidGlassBlurRadiusKey
import com.musifly.android.constants.LiquidGlassLensAmountKey
import com.musifly.android.constants.LiquidGlassLensHeightKey
import com.musifly.android.constants.LiquidGlassVibrancyKey
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.ui.component.liquidglass.backdrop.backdrops.rememberLayerBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.blur
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.highlight.Highlight
import com.musifly.android.ui.component.liquidglass.backdrop.shadow.Shadow
import com.musifly.android.utils.makeTimeString
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference

val GridMenuItemHeight = 108.dp

@Composable
fun GridMenu(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier,
        contentPadding = contentPadding,
        content = content
    )
}

fun LazyGridScope.GridMenuItem(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    tint: @Composable () -> Color = { LocalContentColor.current },
    @StringRes title: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) = GridMenuItem(
    modifier = modifier,
    icon = {
        Icon(
            painter = painterResource(icon),
            tint = tint(),
            contentDescription = null
        )
    },
    title = title,
    enabled = enabled,
    onClick = onClick
)

fun LazyGridScope.GridMenuItem(
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
    @StringRes title: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    item {
        GridMenuItemContent(
            modifier = modifier,
            icon = icon,
            title = title,
            enabled = enabled,
            onClick = onClick
        )
    }
}

@Composable
private fun GridMenuItemContent(
    modifier: Modifier = Modifier,
    icon: @Composable BoxScope.() -> Unit,
    @StringRes title: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    // Check if liquid glass theme is enabled
    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS
    
    // Read liquid glass preferences
    val (blurRadius) = rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 8f)
    val (lensHeight) = rememberPreference(LiquidGlassLensHeightKey, defaultValue = 0.2f)
    val (lensAmount) = rememberPreference(LiquidGlassLensAmountKey, defaultValue = 0.2f)
    val (vibrancy) = rememberPreference(LiquidGlassVibrancyKey, defaultValue = true)
    
    val backdrop = rememberLayerBackdrop()
    val containerColor = Color.Black.copy(0.05f)

        Column(
            modifier = modifier
                .clip(ShapeDefaults.Large)
                .height(GridMenuItemHeight)
            .then(
                if (isLiquidGlass) {
                    Modifier
                        .background(
                            color = containerColor,
                            shape = ShapeDefaults.Large
                        )
                } else {
                    Modifier
                }
            )
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .alpha(if (enabled) 1f else 0.5f)
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
                content = icon
            )
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) {
                        MaterialTheme.typography.labelLarge.lineHeight.toDp() * 2
                    })
            )
    }
}


fun LazyGridScope.DownloadGridMenu(
    @Download.State state: Int?,
    onRemoveDownload: () -> Unit,
    onDownload: () -> Unit,
) {
    when (state) {
        Download.STATE_COMPLETED -> {
            GridMenuItem(
                icon = R.drawable.offline,
                title = R.string.remove_download,
                onClick = onRemoveDownload
            )
        }

        Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
            GridMenuItem(
                icon = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                },
                title = R.string.downloading,
                onClick = onRemoveDownload
            )
        }

        else -> {
            GridMenuItem(
                icon = R.drawable.download,
                title = R.string.download,
                onClick = onDownload
            )
        }
    }
}

fun LazyGridScope.SleepTimerGridMenu(
    modifier: Modifier = Modifier,
    sleepTimerTimeLeft: Long,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    item {
        SleepTimerGridMenuItemContent(
            modifier = modifier,
            sleepTimerTimeLeft = sleepTimerTimeLeft,
            enabled = enabled,
            onClick = onClick
        )
    }
}

@Composable
private fun SleepTimerGridMenuItemContent(
    modifier: Modifier = Modifier,
    sleepTimerTimeLeft: Long,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    // Check if liquid glass theme is enabled
    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS
    
    // Read liquid glass preferences
    val (blurRadius) = rememberPreference(LiquidGlassBlurRadiusKey, defaultValue = 8f)
    val (lensHeight) = rememberPreference(LiquidGlassLensHeightKey, defaultValue = 0.2f)
    val (lensAmount) = rememberPreference(LiquidGlassLensAmountKey, defaultValue = 0.2f)
    val (vibrancy) = rememberPreference(LiquidGlassVibrancyKey, defaultValue = true)
    
    // Container color matching AndroidLiquidGlass ControlCenter style
    val containerColor = Color.Black.copy(0.05f)

        Column(
            modifier = modifier
                .clip(ShapeDefaults.Large)
                .height(GridMenuItemHeight)
            .then(
                if (isLiquidGlass) {
                    Modifier
                        .background(
                            color = containerColor,
                            shape = ShapeDefaults.Large
                        )
                } else {
                    Modifier
                }
            )
                .clickable(
                    onClick = onClick
                )
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
                content = {
                    Icon(
                        painterResource(R.drawable.bedtime),
                        contentDescription = null,
                        modifier = Modifier.alpha(if (enabled) 1f else 0.5f)
                    )
                }
            )
            Text(
                text = if (enabled) makeTimeString(sleepTimerTimeLeft) else stringResource(
                    id = R.string.sleep_timer
                ),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
    }
}
