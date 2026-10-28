package com.musifly.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.navigation.NavController
import com.musifly.android.LocalPlayerAwareWindowInsets
import com.musifly.android.R
import com.musifly.android.constants.*
import com.musifly.android.playback.AudioEffectsManager
import com.musifly.android.playback.PlayerConnection
import com.musifly.android.ui.component.IconButton
import com.musifly.android.ui.component.PreferenceEntry
import com.musifly.android.ui.component.SettingsGroup
import com.musifly.android.ui.component.VerticalFader
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.liquidGlassSubtle
import com.musifly.android.ui.component.liquidglass.*
import com.musifly.android.ui.utils.backToMain
import com.musifly.android.utils.rememberEnumPreference
import com.musifly.android.utils.rememberPreference


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    AudioEffectsContent(
        onBack = { navController.navigateUp() },
        onLongBack = { navController.backToMain() },
        scrollBehavior = scrollBehavior
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsContent(
    onBack: () -> Unit,
    onLongBack: (() -> Unit)? = null, // Optional for usage in Dialog where long press isn't needed
    scrollBehavior: TopAppBarScrollBehavior? = null,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null, // Alternate name for onBack
    isDialog: Boolean = false
) {
    // Adapter for calls using 'onClose' which effectively means 'onBack' in dialog context
    val effectiveOnBack = onClose ?: onBack

    val context = LocalContext.current
    val isLiquidGlass = isLiquidGlassActive() // Enable liquid glass in dialog too!

    // Preferences - Audio Effects State
    val (aiDspEnabled, onAiDspEnabledChange) = rememberPreference(
        AiDspEnabledKey,
        defaultValue = true
    )
    
    val (bassBoostEnabled, onBassBoostEnabledChange) = rememberPreference(
        BassBoostEnabledKey,
        defaultValue = false
    )
    val (bassBoostStrength, onBassBoostStrengthChange) = rememberPreference(
        BassBoostStrengthKey,
        defaultValue = 0
    )

    val (trebleBoostEnabled, onTrebleBoostEnabledChange) = rememberPreference(
        TrebleBoostEnabledKey,
        defaultValue = false
    )
    val (trebleBoostStrength, onTrebleBoostStrengthChange) = rememberPreference(
        TrebleBoostStrengthKey,
        defaultValue = 0
    )

    val (surround3dEnabled, onSurround3dEnabledChange) = rememberPreference(
        Surround3dEnabledKey,
        defaultValue = false
    )
    val (surround3dStrength, onSurround3dStrengthChange) = rememberPreference(
        Surround3dStrengthKey,
        defaultValue = 0
    )
    
    val (virtualizerEnabled, onVirtualizerEnabledChange) = rememberPreference(
        VirtualizerEnabledKey,
        defaultValue = false
    )
    val (virtualizerStrength, onVirtualizerStrengthChange) = rememberPreference(
        VirtualizerStrengthKey,
        defaultValue = 0
    )
    
    val (loudnessEnabled, onLoudnessEnabledChange) = rememberPreference(
        VolumeBoostEnabledKey,
        defaultValue = false
    )
    val (loudnessGain, onLoudnessGainChange) = rememberPreference(
        VolumeBoostGainKey,
        defaultValue = 0
    )
    
    val (dspPreset, onDspPresetChange) = rememberEnumPreference(
        key = DolbyAtmosModeKey,
        defaultValue = AudioEffectsManager.DspPreset.NONE
    )

    // Sync UI changes with AudioEffectsManager
    val effectsManager = PlayerConnection.instance?.service?.audioEffectsManager
    
    // ⚠️ DEPRECATED: Direct effect calls removed - DataStore observers in MusicService handle everything
    // The UI only needs to update DataStore preferences, MusicService will apply effects automatically
    
    LaunchedEffect(aiDspEnabled) {
        PlayerConnection.instance?.service?.aiDspController?.setEnabled(aiDspEnabled)
    }
    
    // Presets still work through old manager temporarily (will be migrated)
    LaunchedEffect(dspPreset) {
        // Note: This uses old AudioEffectsManager.DspPreset enum
        // Session-independent DSP uses SessionIndependentDspManager.Preset
        // For now, DataStore observer in MusicService handles the conversion
    }
    
    // ✅ Bass Boost, Virtualizer, Loudness are now handled by DataStore observers in MusicService
    // No need to call effectsManager methods directly - just update preferences!
    // The MusicService observers will automatically apply to SessionIndependentDspManager
    
    // Deep Bass and EQ preferences
    val (deepBassEnabled, onDeepBassEnabledChange) = rememberPreference(
        key = booleanPreferencesKey("deep_bass_enabled_v1"),
        defaultValue = false
    )
    
    val (equalizerEnabled, onEqualizerEnabledChange) = rememberPreference(
        key = EqualizerEnabledKey,
        defaultValue = false
    )

    // 4-Band EQ Levels
    val (eqBand0, onEqBand0Change) = rememberPreference(intPreferencesKey("four_band_eq_level_0"), 0)
    val (eqBand1, onEqBand1Change) = rememberPreference(intPreferencesKey("four_band_eq_level_1"), 0)
    val (eqBand2, onEqBand2Change) = rememberPreference(intPreferencesKey("four_band_eq_level_2"), 0)
    val (eqBand3, onEqBand3Change) = rememberPreference(intPreferencesKey("four_band_eq_level_3"), 0)

    val currentEqLevels = listOf(eqBand0, eqBand1, eqBand2, eqBand3)
    val currentEqSetters = listOf(onEqBand0Change, onEqBand1Change, onEqBand2Change, onEqBand3Change)
    
    // ✅ Deep Bass and 4-Band EQ are now handled by DataStore observers in MusicService
    // No need to call methods directly - just update preferences!
    // The MusicService observers will automatically apply to SessionIndependentDspManager

    val sessionId by effectsManager?.getSessionIdStateFlow()?.collectAsState(initial = 0) ?: remember { mutableStateOf(0) }
    
    val resetAll = {
        onAiDspEnabledChange(true)
        onBassBoostEnabledChange(false)
        onBassBoostStrengthChange(0)
        onTrebleBoostEnabledChange(false)
        onTrebleBoostStrengthChange(0)
        onSurround3dEnabledChange(false)
        onSurround3dStrengthChange(0)
        onVirtualizerEnabledChange(false)
        onVirtualizerStrengthChange(0)
        onLoudnessEnabledChange(false)
        onLoudnessGainChange(0)
        onDspPresetChange(AudioEffectsManager.DspPreset.NONE)
        onDeepBassEnabledChange(false)
        onEqualizerEnabledChange(false)
        onEqBand0Change(0)
        onEqBand1Change(0)
        onEqBand2Change(0)
        onEqBand3Change(0)
    }

    if (isDialog) {
        if (isLiquidGlass) {
            // Direct Render Mode (No additional Dialog Window)
            Column(
                modifier = modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
               // Title
               LiquidGlassTitle("AI sound effects")

               // Content
               Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start
                ) {
                    AudioEffectsScrollableContent(
                        effectsManager = effectsManager,
                        aiDspEnabled = aiDspEnabled,
                        onAiDspEnabledChange = onAiDspEnabledChange,
                        dspPreset = dspPreset,
                        onDspPresetChange = onDspPresetChange,
                        bassBoostEnabled = bassBoostEnabled,
                        onBassBoostEnabledChange = onBassBoostEnabledChange,
                        bassBoostStrength = bassBoostStrength,
                        onBassBoostStrengthChange = onBassBoostStrengthChange,
                        trebleBoostEnabled = trebleBoostEnabled,
                        onTrebleBoostEnabledChange = onTrebleBoostEnabledChange,
                        trebleBoostStrength = trebleBoostStrength,
                        onTrebleBoostStrengthChange = onTrebleBoostStrengthChange,
                        surround3dEnabled = surround3dEnabled,
                        onSurround3dEnabledChange = onSurround3dEnabledChange,
                        surround3dStrength = surround3dStrength,
                        onSurround3dStrengthChange = onSurround3dStrengthChange,
                        virtualizerEnabled = virtualizerEnabled,
                        onVirtualizerEnabledChange = onVirtualizerEnabledChange,
                        virtualizerStrength = virtualizerStrength,
                        onVirtualizerStrengthChange = onVirtualizerStrengthChange,
                        loudnessEnabled = loudnessEnabled,
                        onLoudnessEnabledChange = onLoudnessEnabledChange,
                        loudnessGain = loudnessGain,
                        onLoudnessGainChange = onLoudnessGainChange,
                        showSessionDisplay = false,
                        deepBassEnabled = deepBassEnabled,
                        onDeepBassEnabledChange = onDeepBassEnabledChange,
                        equalizerEnabled = equalizerEnabled,
                        onEqualizerEnabledChange = onEqualizerEnabledChange,
                        eqLevels = currentEqLevels,
                        onEqLevelChange = currentEqSetters,
                        isLiquidGlass = true
                    )
                }
                
                // Footer Buttons
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassButton(
                         onClick = resetAll,
                         text = "Reset",
                         backgroundColor = Color(0xFFFF8D28),
                         modifier = Modifier.weight(0.4f)
                    )
                    
                    LiquidGlassButton(
                         onClick = effectiveOnBack,
                         text = "Close",
                         isPrimary = true,
                         modifier = Modifier.weight(0.6f)
                    )
                }
            }
        } else {
            // Standard Material 3 Dialog
            Column(modifier = modifier.fillMaxWidth()) {
                // Fixed Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI sound effects",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(color = DividerDefaults.color)
                
                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    AudioEffectsScrollableContent(
                        effectsManager = effectsManager,
                        aiDspEnabled = aiDspEnabled,
                        onAiDspEnabledChange = onAiDspEnabledChange,
                        dspPreset = dspPreset,
                        onDspPresetChange = onDspPresetChange,
                        bassBoostEnabled = bassBoostEnabled,
                        onBassBoostEnabledChange = onBassBoostEnabledChange,
                        bassBoostStrength = bassBoostStrength,
                        onBassBoostStrengthChange = onBassBoostStrengthChange,
                        trebleBoostEnabled = trebleBoostEnabled,
                        onTrebleBoostEnabledChange = onTrebleBoostEnabledChange,
                        trebleBoostStrength = trebleBoostStrength,
                        onTrebleBoostStrengthChange = onTrebleBoostStrengthChange,
                        surround3dEnabled = surround3dEnabled,
                        onSurround3dEnabledChange = onSurround3dEnabledChange,
                        surround3dStrength = surround3dStrength,
                        onSurround3dStrengthChange = onSurround3dStrengthChange,
                        virtualizerEnabled = virtualizerEnabled,
                        onVirtualizerEnabledChange = onVirtualizerEnabledChange,
                        virtualizerStrength = virtualizerStrength,
                        onVirtualizerStrengthChange = onVirtualizerStrengthChange,
                        loudnessEnabled = loudnessEnabled,
                        onLoudnessEnabledChange = onLoudnessEnabledChange,
                        loudnessGain = loudnessGain,
                        onLoudnessGainChange = onLoudnessGainChange,
                        showSessionDisplay = false,
                        deepBassEnabled = deepBassEnabled,
                        onDeepBassEnabledChange = onDeepBassEnabledChange,
                        equalizerEnabled = equalizerEnabled,
                        onEqualizerEnabledChange = onEqualizerEnabledChange,
                        eqLevels = currentEqLevels,
                        onEqLevelChange = currentEqSetters,
                        isLiquidGlass = false
                    )
                }
                
                HorizontalDivider(color = DividerDefaults.color)

                // Fixed Bottom Footer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = resetAll) {
                        Text("Reset Defaults")
                    }

                    Button(onClick = effectiveOnBack) {
                        Text("Close")
                    }
                }
            }
        }
    } else {
        // Normal Full Screen Mode
        Scaffold(
            modifier = modifier
                .then(if (scrollBehavior != null) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier)
                .then(if (isLiquidGlass) Modifier.liquidGlassSubtle() else Modifier),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.ai_sound_effects)) },
                    navigationIcon = {
                        com.musifly.android.ui.component.AppIconButton(
                            onClick = effectiveOnBack,
                            onLongClick = onLongBack,
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
                AudioEffectsScrollableContent(
                    effectsManager = effectsManager,
                    aiDspEnabled = aiDspEnabled,
                    onAiDspEnabledChange = onAiDspEnabledChange,
                    dspPreset = dspPreset,
                    onDspPresetChange = onDspPresetChange,
                    bassBoostEnabled = bassBoostEnabled,
                    onBassBoostEnabledChange = onBassBoostEnabledChange,
                    bassBoostStrength = bassBoostStrength,
                    onBassBoostStrengthChange = onBassBoostStrengthChange,
                    trebleBoostEnabled = trebleBoostEnabled,
                    onTrebleBoostEnabledChange = onTrebleBoostEnabledChange,
                    trebleBoostStrength = trebleBoostStrength,
                    onTrebleBoostStrengthChange = onTrebleBoostStrengthChange,
                    surround3dEnabled = surround3dEnabled,
                    onSurround3dEnabledChange = onSurround3dEnabledChange,
                    surround3dStrength = surround3dStrength,
                    onSurround3dStrengthChange = onSurround3dStrengthChange,
                    virtualizerEnabled = virtualizerEnabled,
                    onVirtualizerEnabledChange = onVirtualizerEnabledChange,
                    virtualizerStrength = virtualizerStrength,
                    onVirtualizerStrengthChange = onVirtualizerStrengthChange,
                    loudnessEnabled = loudnessEnabled,
                    onLoudnessEnabledChange = onLoudnessEnabledChange,
                    loudnessGain = loudnessGain,
                    onLoudnessGainChange = onLoudnessGainChange,
                    
                     // New Params
                    deepBassEnabled = deepBassEnabled,
                    onDeepBassEnabledChange = onDeepBassEnabledChange,
                    equalizerEnabled = equalizerEnabled,
                    onEqualizerEnabledChange = onEqualizerEnabledChange,
                    eqLevels = currentEqLevels,
                    onEqLevelChange = currentEqSetters,
                    isLiquidGlass = isLiquidGlass
                )
            }
        }
    }
}

@Composable
private fun AudioEffectsScrollableContent(
    effectsManager: AudioEffectsManager?,
    aiDspEnabled: Boolean,
    onAiDspEnabledChange: (Boolean) -> Unit,
    dspPreset: AudioEffectsManager.DspPreset,
    onDspPresetChange: (AudioEffectsManager.DspPreset) -> Unit,
    bassBoostEnabled: Boolean,
    onBassBoostEnabledChange: (Boolean) -> Unit,
    bassBoostStrength: Int,
    onBassBoostStrengthChange: (Int) -> Unit,
    trebleBoostEnabled: Boolean,
    onTrebleBoostEnabledChange: (Boolean) -> Unit,
    trebleBoostStrength: Int,
    onTrebleBoostStrengthChange: (Int) -> Unit,
    surround3dEnabled: Boolean,
    onSurround3dEnabledChange: (Boolean) -> Unit,
    surround3dStrength: Int,
    onSurround3dStrengthChange: (Int) -> Unit,
    virtualizerEnabled: Boolean,
    onVirtualizerEnabledChange: (Boolean) -> Unit,
    virtualizerStrength: Int,
    onVirtualizerStrengthChange: (Int) -> Unit,
    loudnessEnabled: Boolean,
    onLoudnessEnabledChange: (Boolean) -> Unit,
    loudnessGain: Int,
    onLoudnessGainChange: (Int) -> Unit,
    // New Params for Deep Bass & EQ
    deepBassEnabled: Boolean,
    onDeepBassEnabledChange: (Boolean) -> Unit,
    equalizerEnabled: Boolean,
    onEqualizerEnabledChange: (Boolean) -> Unit,
    eqLevels: List<Int>,
    onEqLevelChange: List<(Int) -> Unit>,
    
    showSessionDisplay: Boolean = true,
    isLiquidGlass: Boolean = false
) {

    @Composable
    fun AdaptiveSlider(
        value: Float,
        onValueChange: (Float) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        modifier: Modifier = Modifier
    ) {
        if (isLiquidGlass) {
            LiquidSlider(
                value = { value },
                onValueChange = onValueChange,
                valueRange = valueRange,
                visibilityThreshold = 0.001f,
                modifier = modifier
            )
        } else {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = modifier
            )
        }
    }

    Column {

        
        // AI DSP & PRESETS
        SettingsGroup(title = stringResource(R.string.intelligent_processing)) {
            PreferenceEntry(
                icon = { Icon(painterResource(R.drawable.auto_awesome), null) },
                iconBackgroundColor = Color(0xFF4CAF50), // Green
                title = { Text(stringResource(R.string.ai_dsp)) },
                description = stringResource(R.string.ai_dsp_desc),
                trailingContent = {
                    AdaptiveSwitch(checked = aiDspEnabled, onCheckedChange = onAiDspEnabledChange)
                },
                onClick = { onAiDspEnabledChange(!aiDspEnabled) }
            )
            
            
            // DSP Presets
            Column(Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.audio_preset),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioEffectsManager.DspPreset.values().forEach { preset ->
                        FilterChip(
                            selected = dspPreset == preset,
                            onClick = { onDspPresetChange(preset) },
                            label = { 
                                Text(
                                    preset.name.lowercase()
                                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                                ) 
                            }
                        )
                    }
                }
            }
        }
        
        // DEEP BASS & EQ
        SettingsGroup(title = stringResource(R.string.creative_equalizer)) {
            // Deep Bass (Extra Sub)
            PreferenceEntry(
                icon = { Icon(painterResource(R.drawable.music_note), null) },
                iconBackgroundColor = Color(0xFF2196F3), // Blue
                title = { Text(stringResource(R.string.deep_bass)) },
                description = stringResource(R.string.deep_bass_desc),
                trailingContent = {
                    AdaptiveSwitch(checked = deepBassEnabled, onCheckedChange = onDeepBassEnabledChange)
                },
                onClick = { onDeepBassEnabledChange(!deepBassEnabled) }
            )
            
            
            // 4-Band Divider and Toggle
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                   modifier = Modifier.fillMaxWidth(),
                   verticalAlignment = Alignment.CenterVertically,
                   horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.four_band_equalizer),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AdaptiveSwitch(
                        checked = equalizerEnabled,
                        onCheckedChange = onEqualizerEnabledChange
                    )
                }
                
                if (equalizerEnabled) {
                    Spacer(Modifier.height(16.dp))
                    
                    val bands = listOf(
                        stringResource(R.string.eq_band_low),
                        stringResource(R.string.eq_band_mid),
                        stringResource(R.string.eq_band_high_mid),
                        stringResource(R.string.eq_band_high)
                    )
                    
                    Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bands.forEachIndexed { index, label ->
                            val level = eqLevels.getOrElse(index) { 0 }
                            val onLevelChange = onEqLevelChange.getOrElse(index) { {} }
                            
                            // Local state for smooth dragging without constant IO/DSP updates
                            var currentDragValue by remember(level) { mutableStateOf(level.toFloat()) }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                VerticalFader(
                                    value = currentDragValue,
                                    onValueChange = { currentDragValue = it },
                                    onValueChangeFinished = { onLevelChange(currentDragValue.toInt()) },
                                    valueRange = -10f..10f,
                                    steps = 20, // Integers -10 to 10
                                    width = 40.dp,
                                    height = 180.dp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    color = if (isLiquidGlass) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface 
                                )
                                Text(
                                    text = "${if(currentDragValue > 0) "+" else ""}${currentDragValue.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.equalizer_disabled_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // BASS & SPATIAL
            SettingsGroup(title = stringResource(R.string.enhancements)) {
                // Bass Boost
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF673AB7).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(R.drawable.equalizer), null, tint = Color(0xFF673AB7), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.bass_boost), style = MaterialTheme.typography.titleMedium,
                                color = if (isLiquidGlass) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (bassBoostEnabled) stringResource(R.string.strength_value, bassBoostStrength) else stringResource(R.string.off),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLiquidGlass) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(checked = bassBoostEnabled, onCheckedChange = onBassBoostEnabledChange)
                    }
                    
                    if (bassBoostEnabled) {
                        AdaptiveSlider(
                            value = bassBoostStrength.toFloat(),
                            onValueChange = { onBassBoostStrengthChange(it.toInt()) },
                            valueRange = 0f..300f,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

    
                // Treble Boost
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE91E63).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(R.drawable.graphic_eq), null, tint = Color(0xFFE91E63), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.treble_boost), style = MaterialTheme.typography.titleMedium, 
                                color = if (isLiquidGlass) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (trebleBoostEnabled) stringResource(R.string.strength_value, trebleBoostStrength) else stringResource(R.string.off),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLiquidGlass) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(checked = trebleBoostEnabled, onCheckedChange = onTrebleBoostEnabledChange)
                    }
                    
                    if (trebleBoostEnabled) {
                        AdaptiveSlider(
                            value = trebleBoostStrength.toFloat(),
                            onValueChange = { onTrebleBoostStrengthChange(it.toInt()) },
                            valueRange = 0f..1000f,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                    
                // 3D Surround
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFF9800).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(R.drawable.auto_awesome), null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.surround_3d), style = MaterialTheme.typography.titleMedium, 
                                color = if (isLiquidGlass) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (surround3dEnabled) stringResource(R.string.immersion_value, surround3dStrength) else stringResource(R.string.off),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLiquidGlass) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(checked = surround3dEnabled, onCheckedChange = onSurround3dEnabledChange)
                    }
                    
                    if (surround3dEnabled) {
                        AdaptiveSlider(
                            value = surround3dStrength.toFloat(),
                            onValueChange = { onSurround3dStrengthChange(it.toInt()) },
                            valueRange = 0f..1000f,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            stringResource(R.string.surround_3d_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                    
                // Virtualizer
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF009688).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(R.drawable.view_quilt), null, tint = Color(0xFF009688), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.spatial_audio), style = MaterialTheme.typography.titleMedium, 
                                color = if (isLiquidGlass) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (virtualizerEnabled) stringResource(R.string.spaciousness_value, virtualizerStrength) else stringResource(R.string.off),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLiquidGlass) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(checked = virtualizerEnabled, onCheckedChange = onVirtualizerEnabledChange)
                    }
                    
                    if (virtualizerEnabled) {
                        AdaptiveSlider(
                            value = virtualizerStrength.toFloat(),
                            onValueChange = { onVirtualizerStrengthChange(it.toInt()) },
                            valueRange = 0f..1000f,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                    
                // Loudness
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF607D8B).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(R.drawable.tune), null, tint = Color(0xFF607D8B), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.loudness_enhancer), style = MaterialTheme.typography.titleMedium, 
                                color = if (isLiquidGlass) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (loudnessEnabled) stringResource(R.string.gain_value, loudnessGain) else stringResource(R.string.off),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLiquidGlass) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AdaptiveSwitch(checked = loudnessEnabled, onCheckedChange = onLoudnessEnabledChange)
                    }
                    
                    if (loudnessEnabled) {
                        AdaptiveSlider(
                            value = loudnessGain.toFloat(),
                            onValueChange = { onLoudnessGainChange(it.toInt()) },
                            valueRange = 0f..100f,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Note about hardware support
        Text(
            stringResource(R.string.audio_effects_note),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(16.dp))
    }
}
