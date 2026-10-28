package com.musifly.android.ui.component.liquidglass.catalog.components

import com.musifly.android.R
import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.musifly.android.ui.component.liquidglass.backdrop.Backdrop
import com.musifly.android.ui.screens.Screens

@Composable
fun LiquidBottomNavigation(
    navController: NavController,
    navigationItems: List<Screens>,
    backdrop: Backdrop,
    onNavigate: (Screens) -> Unit,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val externalSelectedIndex = navigationItems.indexOfFirst { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }.let { if (it == -1) 0 else it }
    
    // Internal state for immediate tab switching animation
    var internalSelectedIndex by remember { mutableIntStateOf(externalSelectedIndex) }
    var lastNavigateTime by remember { mutableStateOf(0L) }
    
    // Sync external navigation changes to internal state
    LaunchedEffect(externalSelectedIndex) {
        internalSelectedIndex = externalSelectedIndex
    }

    val (slimNav) = com.musifly.android.utils.rememberPreference(com.musifly.android.constants.SlimNavBarKey, defaultValue = false)
    val showLabels = !slimNav
    val (widthPercent) = com.musifly.android.utils.rememberPreference(com.musifly.android.constants.LiquidGlassNavBarWidthKey, defaultValue = 1.0f)

    val isLightTheme = !androidx.compose.foundation.isSystemInDarkTheme()
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val inactiveColor = if (isLightTheme) Color.Black.copy(0.4f) else Color.White.copy(0.4f)

    LiquidBottomTabs(
        tabsCount = navigationItems.size,
        selectedTabIndex = { internalSelectedIndex },
        onTabSelected = { index ->
            onNavigate(navigationItems[index])
        },
        backdrop = backdrop,
        modifier = modifier
            .fillMaxWidth(widthPercent)
            .padding(horizontal = 8.dp)
    ) {
        navigationItems.forEachIndexed { index, screen ->
            val isSelected = internalSelectedIndex == index
            LiquidBottomTab(
                onClick = {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastNavigateTime > 500L) {
                        lastNavigateTime = currentTime
                        onNavigate(screen)
                    }
                }
            ) {
                val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val currentUser = firebaseAuth.currentUser

                val isActiveLayer = LocalLiquidTabActiveLayer.current
                val iconTint = if (isActiveLayer || isSelected) accentColor else inactiveColor
                val textTint = if (isActiveLayer || isSelected) accentColor else inactiveColor

                if (screen.route == Screens.Profile.route && currentUser?.photoUrl != null) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(currentUser.photoUrl)
                                .allowHardware(false)
                                .build(),
                            contentDescription = stringResource(screen.titleId),
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) accentColor else inactiveColor,
                                    shape = CircleShape
                                ),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.person),
                            error = painterResource(R.drawable.person)
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(
                            id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                        ),
                        contentDescription = stringResource(screen.titleId),
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (showLabels) {
                    Text(
                        text = stringResource(screen.titleId),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = textTint,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

