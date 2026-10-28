package com.musifly.android.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
// import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi // Removed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.R
import com.musifly.android.constants.*
import com.musifly.android.ui.component.EnumListPreference
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.ListPreference
import com.musifly.android.ui.component.PreferenceEntry
import com.musifly.android.ui.component.SwitchPreference
import com.musifly.android.ui.component.SliderPreference
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.liquidGlassSubtle
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference
import com.musifly.android.ui.component.SettingsGroup
import com.musifly.android.ui.component.liquidglass.LiquidGlassDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import com.musifly.android.ui.theme.DefaultThemeColor
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.Dp
import kotlin.math.roundToInt
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {

    // DARK MODE
    val (darkMode, onDarkModeChange) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )

    val (pureBlack, onPureBlackChange) = rememberPreference(
        PureBlackKey,
        defaultValue = false
    )

    val (fullScreenMode, onFullScreenModeChange) = rememberPreference(
        FullScreenModeKey,
        defaultValue = false
    )

    val (playerTextAlignment, onPlayerTextAlignmentChange) =
        rememberEnumPreference(
            PlayerTextAlignmentKey,
            defaultValue = PlayerTextAlignment.CENTER,
        )

    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )

    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.CENTER
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)

    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )

    val (rotateBackground, onRotateBackgroundChange) = rememberPreference(
        key = RotateBackgroundKey,
        defaultValue = false
    )

    // Grid Size default SMALL + UI hidden
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.SMALL
    )


    val (slimNav, onSlimNavChange) = rememberPreference(
        SlimNavBarKey,
        defaultValue = true
    )

    val (appTheme, onAppThemeChange) = rememberEnumPreference(
        AppThemeKey,
        defaultValue = AppTheme.DEFAULT
    )


    // Theme Mode Selector (Light / Dark / Liquid Glass)
    val (themeMode, onThemeModeChange) = rememberEnumPreference(
        ThemeModeKey,
        defaultValue = ThemeMode.DARK
    )

    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme =
        if (darkMode == DarkMode.AUTO) isSystemDark else darkMode == DarkMode.ON



    val isLiquidGlass = isLiquidGlassActive()
    
    // Safety check: Reset theme to DARK if LIQUID_GLASS is selected on unsupported Android versions (< 13)
    LaunchedEffect(themeMode) {
        if (themeMode == ThemeMode.LIQUID_GLASS && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onThemeModeChange(ThemeMode.DARK)
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .then(if (isLiquidGlass) Modifier.liquidGlassSubtle() else Modifier),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.appearance)) },
                navigationIcon = {
                    com.musifly.android.ui.component.AppIconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surface
                )
            )
        },
        contentWindowInsets = LocalPlayerAwareWindowInsets.current
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // THEME GROUP
            SettingsGroup(title = stringResource(R.string.theme)) {
                EnumListPreference(
                    title = { Text(stringResource(R.string.dark_theme)) },
                    icon = { Icon(painterResource(R.drawable.dark_mode), null) },
                    iconBackgroundColor = Color(0xFF2196F3), // Blue
                    selectedValue = darkMode,
                    onValueSelected = onDarkModeChange,
                    valueText = {
                        when (it) {
                            DarkMode.ON -> stringResource(R.string.dark_theme_on)
                            DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                            DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                        }
                    },
                )


                // Theme Mode Picker (Light / Dark / Liquid Glass)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val availableThemeModes = listOf(ThemeMode.DARK, ThemeMode.LIQUID_GLASS)

                    EnumListPreference(
                        title = { Text(stringResource(R.string.theme_style)) },
                        icon = { Icon(painterResource(R.drawable.palette), null) },
                        iconBackgroundColor = Color(0xFF673AB7), // Deep Purple
                        selectedValue = if (themeMode !in availableThemeModes) ThemeMode.DARK else themeMode,
                        values = availableThemeModes,
                        onValueSelected = onThemeModeChange,
                        valueText = {
                            when (it) {
                                ThemeMode.DARK -> stringResource(R.string.default_theme)
                                ThemeMode.LIQUID_GLASS -> stringResource(R.string.liquid_glass_premium)
                                else -> ""
                            }
                        },
                    )
                }


                if (themeMode != ThemeMode.LIQUID_GLASS) {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.pure_black_oled)) },
                        description = stringResource(R.string.pure_black_desc),
                        icon = { Icon(painterResource(R.drawable.dark_mode), null) },
                        iconBackgroundColor = Color(0xFF000000), // Black
                        checked = pureBlack,
                        onCheckedChange = onPureBlackChange,
                        isEnabled = useDarkTheme
                    )
                    
                }

                SwitchPreference(
                    title = { Text(stringResource(R.string.full_screen_mode)) },
                    description = stringResource(R.string.full_screen_mode_desc),
                    icon = { Icon(painterResource(R.drawable.fullscreen), null) },
                    iconBackgroundColor = Color(0xFF00BCD4), // Cyan
                    checked = fullScreenMode,
                    onCheckedChange = onFullScreenModeChange
                )


                // App Theme Picker (Color Scheme) hidden as requested
            }

            // PLAYER GROUP
            SettingsGroup(title = stringResource(R.string.player)) {
                // Player Text Alignment
                EnumListPreference(
                    title = { Text(stringResource(R.string.player_text_alignment)) },
                    icon = {
                        Icon(
                            painter = painterResource(
                                if (playerTextAlignment == PlayerTextAlignment.CENTER)
                                    R.drawable.format_align_center
                                else R.drawable.format_align_left
                            ),
                            contentDescription = null
                        )
                    },
                    iconBackgroundColor = Color(0xFF4CAF50), // Green
                    selectedValue = playerTextAlignment,
                    onValueSelected = onPlayerTextAlignmentChange,
                    valueText = {
                        when (it) {
                            PlayerTextAlignment.SIDED -> stringResource(R.string.sided)
                            PlayerTextAlignment.CENTER -> stringResource(R.string.center)
                        }
                    }
                )


                // Lyrics Position
                EnumListPreference(
                    title = { Text(stringResource(R.string.lyrics_text_position)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    iconBackgroundColor = Color(0xFFE91E63), // Pink
                    selectedValue = lyricsPosition,
                    onValueSelected = onLyricsPositionChange,
                    valueText = {
                        when (it) {
                            LyricsPosition.LEFT -> stringResource(R.string.left)
                            LyricsPosition.CENTER -> stringResource(R.string.center)
                            LyricsPosition.RIGHT -> stringResource(R.string.right)
                        }
                    }
                )


                SwitchPreference(
                    title = { Text(stringResource(R.string.lyrics_click_change)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    iconBackgroundColor = Color(0xFFFF9800), // Deep Orange
                    checked = lyricsClick,
                    onCheckedChange = onLyricsClickChange,
                )


                if (themeMode != ThemeMode.LIQUID_GLASS) {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                        icon = { Icon(painterResource(R.drawable.swipe), null) },
                        iconBackgroundColor = Color(0xFF009688), // Teal
                        checked = swipeThumbnail,
                        onCheckedChange = onSwipeThumbnailChange,
                    )

                }

                SwitchPreference(
                    title = { Text(stringResource(R.string.Rotatelyricsbackground)) },
                    icon = { Icon(painterResource(R.drawable.album), null) },
                    iconBackgroundColor = Color(0xFF3F51B5), // Indigo
                    checked = rotateBackground,
                    onCheckedChange = onRotateBackgroundChange
                )

                // Liquid Glass Mini Player Customization
                if (themeMode == ThemeMode.LIQUID_GLASS) {
                    val (miniHeight, onMiniHeightChange) = rememberPreference(
                        LiquidGlassMiniPlayerHeightKey,
                        defaultValue = 50f
                    )
                    val (miniCorners, onMiniCornersChange) = rememberPreference(
                        LiquidGlassMiniPlayerCornerRadiusKey,
                        defaultValue = 15f
                    )


                    SliderPreference(
                        title = { Text(stringResource(R.string.mini_player_height)) },
                        description = stringResource(R.string.mini_player_height_desc),
                        icon = { Icon(painterResource(R.drawable.height), null) },
                        iconBackgroundColor = Color(0xFF9C27B0), // Purple
                        value = miniHeight,
                        onValueChange = onMiniHeightChange,
                        valueRange = 50f..100f,
                        steps = 50,
                        valueText = { "${it.toInt()} dp" }
                    )


                    SliderPreference(
                        title = { Text(stringResource(R.string.mini_player_corners)) },
                        description = stringResource(R.string.mini_player_corners_desc),
                        icon = { Icon(painterResource(R.drawable.rounded_corner), null) },
                        iconBackgroundColor = Color(0xFF8BC34A), // Light Green
                        value = miniCorners,
                        onValueChange = onMiniCornersChange,
                        valueRange = 0f..50f,
                        steps = 50,
                        valueText = { "${it.toInt()} dp" }
                    )
                }
            }


            
            // NAVIGATION BAR GROUP
            SettingsGroup(title = stringResource(R.string.navigation_bar)) {
                EnumListPreference(
                    title = { Text(stringResource(R.string.default_open_tab)) },
                    icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                    iconBackgroundColor = Color(0xFF795548), // Brown
                    selectedValue = defaultOpenTab,
                    onValueSelected = onDefaultOpenTabChange,
                    valueText = {
                        when (it) {
                            NavigationTab.HOME -> stringResource(R.string.home)
                            NavigationTab.EXPLORE -> stringResource(R.string.explore)
                            NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                        }
                    }
                )


                SwitchPreference(
                    title = { Text(stringResource(R.string.slim_navbar)) },
                    description = stringResource(R.string.slim_navbar_desc),
                    icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                    iconBackgroundColor = Color(0xFF607D8B), // Blue Grey
                    checked = slimNav,
                    onCheckedChange = onSlimNavChange
                )
                
                // Liquid Glass Navigation Bar Width Customization
                if (themeMode == ThemeMode.LIQUID_GLASS) {
                    val (navWidth, onNavWidthChange) = rememberPreference(
                        LiquidGlassNavBarWidthKey,
                        defaultValue = 1.0f
                    )


                    SliderPreference(
                        title = { Text(stringResource(R.string.bar_width)) },
                        description = stringResource(R.string.bar_width_desc),
                        icon = { Icon(painterResource(R.drawable.sahitya_slider), null) },
                        iconBackgroundColor = Color(0xFFFFC107), // Amber
                        value = navWidth,
                        onValueChange = onNavWidthChange,
                        valueRange = 0.5f..1.0f,
                        steps = 10,
                        valueText = { "${(it * 100).toInt()}%" }
                    )
                }
                
                // Liquid Glass Navigation Customization
                
                // Floating Dock Customization (Only for Standard Theme)
                if (themeMode != ThemeMode.LIQUID_GLASS) {
                    val (enableDocker, onEnableDockerChange) = rememberPreference(
                        EnableFloatingDockerKey,
                        defaultValue = true
                    )
                
                    val (navWidthPercent, onNavWidthPercentChange) = rememberPreference(
                        FloatingNavBarWidthPercentKey,
                        defaultValue = 1.0f // Default 100% width
                    )
                    
                    val (navHeightDp, onNavHeightDpChange) = rememberPreference(
                        FloatingNavBarHeightDpKey,
                        defaultValue = 64f // Default shorter height for floating bar
                    )
                    
                    
                    SwitchPreference(
                        title = { Text(stringResource(R.string.floating_bar)) },
                        description = stringResource(R.string.floating_bar_desc),
                        icon = { Icon(painterResource(R.drawable.dock), null) },
                        iconBackgroundColor = Color(0xFFCDDC39), // Lime
                        checked = enableDocker,
                        onCheckedChange = onEnableDockerChange
                    )
                    
                    if (enableDocker) {
                        
                        SliderPreference(
                            title = { Text(stringResource(R.string.bar_width)) },
                            description = stringResource(R.string.floating_bar_width_desc),
                            icon = { Icon(painterResource(R.drawable.sahitya_slider), null) },
                            iconBackgroundColor = Color(0xFFFFC107), // Amber
                            value = navWidthPercent,
                            onValueChange = onNavWidthPercentChange,
                            valueRange = 0.5f..1.0f,
                            steps = 10,
                            valueText = { "${(it * 100).toInt()}%" }
                        )
                        
                        
                        SliderPreference(
                            title = { Text(stringResource(R.string.bar_height)) },
                            description = stringResource(R.string.bar_height_desc),
                            icon = { Icon(painterResource(R.drawable.height), null) },
                            iconBackgroundColor = Color(0xFFFF5722), // Deep Orange
                            value = navHeightDp,
                            onValueChange = onNavHeightDpChange,
                            valueRange = 60f..120f,
                            steps = 12,
                            valueText = { "${it.toInt()} dp" }
                        )
                    }
                }
            }


        }
    }
}



enum class DarkMode { ON, OFF, AUTO }
enum class NavigationTab { HOME, EXPLORE, LIBRARY }
enum class LyricsPosition { LEFT, CENTER, RIGHT }
enum class PlayerTextAlignment { SIDED, CENTER }

val MaterialColors = listOf(
    Color(0xFFF44336), Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
    Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4),
    Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39),
    Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722),
    Color(0xFF795548), Color(0xFF9E9E9E), Color(0xFF607D8B), Color(0xFF000000), Color(0xFFFFFFFF)
)

@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Int,
    onColorSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    includeAuto: Boolean = false
) {
    // Current Color State
    val initialC = if (initialColor == 0) Color.Transparent else Color(initialColor)
    var red by remember { mutableStateOf(if (initialColor == 0) 0f else initialC.red) }
    var green by remember { mutableStateOf(if (initialColor == 0) 0f else initialC.green) }
    var blue by remember { mutableStateOf(if (initialColor == 0) 0f else initialC.blue) }

    val currentColor = Color(red, green, blue)

    val (themeMode) = rememberEnumPreference(ThemeModeKey, defaultValue = ThemeMode.DARK)
    val isLiquidGlass = themeMode == ThemeMode.LIQUID_GLASS

    val dialogTitle: @Composable () -> Unit = { Text(title) }
    val dialogContent: @Composable ColumnScope.() -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (includeAuto && initialColor == 0 && red == 0f && green == 0f && blue == 0f) MaterialTheme.colorScheme.surfaceVariant else currentColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                if (includeAuto && initialColor == 0 && red == 0f && green == 0f && blue == 0f) {
                    Text(stringResource(R.string.auto_default), style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            if (includeAuto) {
                PreferenceEntry(
                    title = { Text(stringResource(R.string.use_auto_default)) },
                    description = stringResource(R.string.use_auto_desc),
                    onClick = { 
                        onColorSelected(0)
                    }
                )
                HorizontalDivider()
            }

            // Title for Manual Selection
            Text(stringResource(R.string.custom_rgb_selection), style = MaterialTheme.typography.labelLarge)

            // Red Slider
            Column {
                Text(stringResource(R.string.red_value, (red * 255).roundToInt()))
                Slider(
                    value = red,
                    onValueChange = { red = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Red,
                        activeTrackColor = Color.Red.copy(alpha = 0.7f)
                    )
                )
            }

            // Green Slider
            Column {
                Text(stringResource(R.string.green_value, (green * 255).roundToInt()))
                Slider(
                    value = green,
                    onValueChange = { green = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Green,
                        activeTrackColor = Color.Green.copy(alpha = 0.7f)
                    )
                )
            }

            // Blue Slider
            Column {
                Text(stringResource(R.string.blue_value, (blue * 255).roundToInt()))
                Slider(
                    value = blue,
                    onValueChange = { blue = it },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Blue,
                        activeTrackColor = Color.Blue.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
    val buttonsContent: @Composable RowScope.() -> Unit = {
        if (isLiquidGlass) {
             com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                onClick = onDismissRequest,
                text = stringResource(R.string.cancel)
             )
             androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
             com.musifly.android.ui.component.liquidglass.LiquidGlassButton(
                onClick = { onColorSelected(currentColor.toArgb()) },
                text = stringResource(R.string.action_select),
                isPrimary = true
             )
        } else {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
            TextButton(
                onClick = {
                    onColorSelected(currentColor.toArgb())
                }
            ) { Text(stringResource(R.string.action_select)) }
        }
    }

    if (isLiquidGlass) {
        LiquidGlassDialog(
            onDismiss = onDismissRequest,
            title = dialogTitle,
            content = dialogContent,
            buttons = buttonsContent
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = dialogTitle,
            text = { Column { dialogContent() } },
            confirmButton = {
                TextButton(
                    onClick = {
                        onColorSelected(currentColor.toArgb())
                    }
                ) { Text(stringResource(R.string.action_select)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
