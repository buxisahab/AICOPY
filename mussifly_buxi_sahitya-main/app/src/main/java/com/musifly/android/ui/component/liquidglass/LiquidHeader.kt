package com.musifly.android.ui.component.liquidglass

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.musifly.android.R
import com.musifly.android.constants.PureBlackKey
import com.musifly.android.constants.FullScreenModeKey
import com.musifly.android.ui.component.liquidglass.backdrop.Backdrop
import com.musifly.android.ui.component.liquidglass.backdrop.drawBackdrop
import com.musifly.android.ui.component.liquidglass.backdrop.effects.blur
import com.musifly.android.ui.component.liquidglass.backdrop.effects.vibrancy
import com.musifly.android.ui.component.liquidglass.backdrop.effects.lens
import com.musifly.android.utils.rememberPreference

@Composable
fun LiquidHeader(
    navController: NavController,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    title: String? = null,
    showNotifications: Boolean = true,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    titleContent: @Composable (() -> Unit)? = null
) {
    val isLightTheme = !isSystemInDarkTheme()
    val pureBlack by rememberPreference(PureBlackKey, false)
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.4f)
                         else if (pureBlack) Color.Black.copy(0.4f)
                         else Color(0xFF121212).copy(0.4f)
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val (isFullScreen) = rememberPreference(FullScreenModeKey, defaultValue = false)
    val density = LocalDensity.current
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(0.dp) },
                effects = {
                    vibrancy()
                    blur(with(density) { 24.dp.toPx() })
                    lens(
                        refractionHeight = with(density) { 32.dp.toPx() },
                        refractionAmount = with(density) { 48.dp.toPx() },
                        chromaticAberration = true,
                        depthEffect = true
                    )
                },
                onDrawSurface = { drawRect(containerColor) }
            ),
        contentAlignment = Alignment.TopStart
    ) {
        Column {
            if (isFullScreen) {
                Spacer(modifier = Modifier.height(40.dp))
            }
            Spacer(modifier = Modifier.statusBarsPadding())
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides contentColor) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 20.dp)
                        .then(
                            if (isLightTheme) {
                                Modifier
                            } else {
                                // Plus blend mode for text dispersion in dark mode
                                Modifier.graphicsLayer { blendMode = BlendMode.Plus }
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        if (title == null && titleContent == null) {
                            Image(
                                painter = painterResource(R.drawable.sahitya_h),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        } else {
                            if (navigationIcon != null) {
                                navigationIcon()
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.arrow_back),
                                    contentDescription = "Back",
                                    tint = contentColor,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { navController.navigateUp() }
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            if (titleContent != null) {
                                titleContent()
                            } else {
                                Text(
                                    text = title ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showNotifications && title == null && titleContent == null) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .drawBackdrop(
                                        backdrop = backdrop,
                                        shape = { androidx.compose.foundation.shape.CircleShape },
                                        effects = {
                                            vibrancy()
                                            blur(with(density) { 8.dp.toPx() })
                                            lens(
                                                refractionHeight = with(density) { 32.dp.toPx() },
                                                refractionAmount = with(density) { 48.dp.toPx() },
                                                chromaticAberration = true,
                                                depthEffect = true
                                            )
                                        },
                                        onDrawSurface = { drawRect(containerColor) }
                                    )
                                    .clickable { navController.navigate("notifications") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.sahitya_notification),
                                    contentDescription = "Notifications",
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        actions()
                    }
                }
            }
        }
    }
}
