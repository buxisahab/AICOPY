package com.musifly.android.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.R
import com.musifly.android.constants.ButtonSoundEnabledKey
import com.musifly.android.constants.HapticFeedbackEnabledKey
import com.musifly.android.constants.HapticFeedbackIntensity
import com.musifly.android.constants.HapticFeedbackIntensityKey
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.ListDialog
import com.musifly.android.ui.component.SettingsCategory
import com.musifly.android.ui.component.SettingsCategoryItem
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.liquidGlassSubtle
import com.musifly.android.ui.component.liquidglass.AdaptiveSwitch
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.musifly.android.ui.component.PreferenceEntry
import com.musifly.android.ui.component.PreferenceGroupTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DSPSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val (buttonSoundEnabled, onButtonSoundEnabledChange) = rememberPreference(
        ButtonSoundEnabledKey,
        defaultValue = true
    )
    val (hapticEnabled, onHapticEnabledChange) = rememberPreference(
        HapticFeedbackEnabledKey,
        defaultValue = true
    )
    val (hapticIntensity, onHapticIntensityChange) = rememberEnumPreference(
        HapticFeedbackIntensityKey,
        defaultValue = HapticFeedbackIntensity.MEDIUM
    )
    
    // DSP dialog state removed - feature has been removed
    var showHapticIntensityDialog by remember { androidx.compose.runtime.mutableStateOf(false) }

    fun playButtonSound() {
        if (buttonSoundEnabled) {
            scope.launch {
                try {
                    val toneGen = android.media.ToneGenerator(
                        android.media.AudioManager.STREAM_MUSIC,
                        50 
                    )
                    toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 50)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        toneGen.release()
                    }, 100)
                } catch (e: Exception) {
                }
            }
        }
    }
    
    // DSP dialog removed - feature has been removed
    
    if (showHapticIntensityDialog) {
        ListDialog(onDismiss = { showHapticIntensityDialog = false }) {
            items(HapticFeedbackIntensity.values()) { value ->
                Row(
                   verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                   modifier = Modifier.fillMaxWidth().clickable { 
                       playButtonSound()
                       showHapticIntensityDialog = false
                       onHapticIntensityChange(value) 
                   }.padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                   Text(
                       text = when (value) {
                           HapticFeedbackIntensity.OFF -> "⚪ Off"
                           HapticFeedbackIntensity.LIGHT -> "🟢 Light (5-25ms)"
                           HapticFeedbackIntensity.MEDIUM -> "🟡 Medium (10-40ms)"
                           HapticFeedbackIntensity.STRONG -> "🔴 Strong (15-60ms)"
                       },
                       style = MaterialTheme.typography.bodyLarge,
                       color = if (value == hapticIntensity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                       modifier = Modifier.weight(1f)
                   )
                   
                   androidx.compose.material3.RadioButton(
                       selected = value == hapticIntensity,
                       onClick = null,
                       colors = androidx.compose.material3.RadioButtonDefaults.colors(
                           selectedColor = MaterialTheme.colorScheme.primary,
                           unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                       )
                   )
                }
            }
        }
    }

    val isLiquidGlass = isLiquidGlassActive()

    Column(
        Modifier
            .then(if (isLiquidGlass) Modifier.liquidGlassSubtle() else Modifier)
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        Spacer(Modifier.height(16.dp))
        
        // Sound Effects category removed - feature has been removed from app

        
        Spacer(Modifier.height(16.dp))
        
        // Haptics & Feedback Group
        SettingsCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                SettingsCategoryItem(
                    icon = painterResource(R.drawable.vibrate),
                    title = { Text(stringResource(R.string.haptic_feedback)) },
                    iconBackgroundColor = Color(0xFF4CAF50), // Green
                    description = { Text("Feel tactile responses depending on interaction") },
                    trailingContent = { 
                        AdaptiveSwitch(
                            checked = hapticEnabled, 
                            onCheckedChange = {
                                playButtonSound()
                                onHapticEnabledChange(it)
                            }
                        ) 
                    },
                    onClick = { 
                        playButtonSound()
                        onHapticEnabledChange(!hapticEnabled) 
                    }
                ),
                // Only show intensity if haptics enabled - wait, SettingsCategory takes a list.
                // We can't conditionally add to list easily inside the function call, so we build list separately or use if inside `items = listOf(...)` but `listOf` elements must be non-null.
                // Kotlin's `listOfNotNull` works if we return null.
            ).let { list ->
                 if (hapticEnabled) {
                     list + SettingsCategoryItem(
                        icon = painterResource(R.drawable.tune),
                        title = { Text(stringResource(R.string.haptic_intensity)) },
                        iconBackgroundColor = Color(0xFF2196F3), // Blue
                        description = { 
                            Text(when(hapticIntensity) {
                                HapticFeedbackIntensity.OFF -> "Off"
                                HapticFeedbackIntensity.LIGHT -> "Light"
                                HapticFeedbackIntensity.MEDIUM -> "Medium"
                                HapticFeedbackIntensity.STRONG -> "Strong"
                            }) 
                        },
                        onClick = { 
                           playButtonSound()
                           showHapticIntensityDialog = true 
                        }
                     )
                 } else list
            } + listOf(
                 SettingsCategoryItem(
                    icon = painterResource(R.drawable.volume_up),
                    title = { Text(stringResource(R.string.button_sound)) },
                    iconBackgroundColor = Color(0xFF9C27B0), // Purple
                    description = { Text(stringResource(R.string.button_sound_desc)) },
                    trailingContent = { AdaptiveSwitch(checked = buttonSoundEnabled, onCheckedChange = onButtonSoundEnabledChange) },
                    onClick = { onButtonSoundEnabledChange(!buttonSoundEnabled) }
                )
            )
        )
        
        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.dsp_settings)) },
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
}
