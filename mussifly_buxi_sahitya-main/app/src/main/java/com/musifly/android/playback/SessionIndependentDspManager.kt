package com.musifly.android.playback

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Session-Independent DSP Manager
 * Implements "Production-Grade" Audio Effects Logic
 */
class SessionIndependentDspManager(private val dspProcessor: DspAudioProcessor) {

    companion object {
        private const val TAG = "SessionIndependentDspManager"
    }

    // Updated Enums to match Professional AudioEffectsManager
    enum class Preset {
        NONE,
        OCTAPAD,       // Punchy bass and sharp attack
        MUSIC,  // Balanced
        CINEMA, // V-shaped, Wide
        VOICE,  // Vocal focus
        SPATIAL,// Immersive
        NIGHT,  // Reduced dynamics
        ROCK,   // Punchy
        POP,    // Vocal/Treble
        JAZZ,   // Warm
        CLASSIC,// Air
        ELECTRONIC, // Deep Bass
        TREBLE,      // Crisp highs emphasis
        TREBLE_BOOST, // High frequency focus
        BRIGHT,      // Crisp and clear
        VOCAL_BOOST  // Enhanced clarity for voices
    }

    private val _currentPreset = MutableStateFlow(Preset.NONE)
    val currentPreset: StateFlow<Preset> = _currentPreset
    
    // ... (rest of the file until applyEffects switch case)

    // ... (rest of the file until applyEffects switch case)

    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled

    // State variables
    private var deepBassOn = false
    private var trebleBoostEnabled = false
    private var trebleBoostStrength = 0
    private var surround3dEnabled = false
    private var surround3dStrength = 0
    private var manualVirtualizerStrength = 0.0f
    private var fourBandEnabled = false
    private val fourBandLevels = IntArray(4) { 0 } // Low, Mid, High-Mid, High
    private var activePreset = Preset.NONE


    fun setEnabled(enabled: Boolean) {
        _enabled.value = enabled
        dspProcessor.setEnabled(enabled)
        Log.d(TAG, "DSP ${if (enabled) "enabled" else "disabled"}")
    }

    fun setMasterGain(gainDb: Float) {
        val clamped = gainDb.coerceIn(-20f, 20f)
        dspProcessor.setGain(clamped)
    }

    fun setBassBoost(gainDb: Float) {
        // Just passthrough or ignore if we want to rely on the EQ logic strictly
        dspProcessor.setBassBoost(gainDb)
    }

    // ============================================================================================
    // PROFESSIONAL LOGIC IMPLEMENTATION
    // ============================================================================================

    fun setDeepBass(gainDb: Float) {
        // We treat any gain > 0 as "Enabled"
        deepBassOn = gainDb > 0

        // Strict Snippet Compliance: Deep Bass is handled via EQ Loop (< 80Hz +8dB)
        // Disable separate filter to avoid double-processing or "bass booster" effect
        dspProcessor.setDeepBass(0f)

        Log.d(TAG, "Deep Bass (EQ Mode) enabled: $deepBassOn")
        applyEffects()
    }

    fun setTrebleBoost(enabled: Boolean, strength: Int) {
        trebleBoostEnabled = enabled
        trebleBoostStrength = strength
        Log.d(TAG, "Treble Boost enabled: $enabled, strength: $strength")
        applyEffects()
    }

    fun setSurround3d(enabled: Boolean, strength: Int) {
        surround3dEnabled = enabled
        surround3dStrength = strength
        Log.d(TAG, "Surround 3D enabled: $enabled, strength: $strength")
        applyEffects()
    }

    fun setFourBandEnabled(enabled: Boolean) {
        fourBandEnabled = enabled
        Log.d(TAG, "4-Band EQ enabled: $enabled")
        applyEffects()
    }

    fun setFourBandEq(band: Int, level: Int) {
        if (band in 0..3) {
            fourBandLevels[band] = level
            Log.d(TAG, "4-Band EQ $band set to $level")
            if (fourBandEnabled) applyEffects()
        }
    }

    fun setEqBand(band: Int, gainDb: Float) {
        // Direct manual override
        dspProcessor.setEqBand(band, gainDb)
    }

    fun applyPreset(preset: Preset) {
        activePreset = preset
        _currentPreset.value = preset
        Log.d(TAG, "Applied preset: $preset")
        applyEffects()
    }

    fun resetAll() {
        deepBassOn = false
        trebleBoostEnabled = false
        trebleBoostStrength = 0
        surround3dEnabled = false
        surround3dStrength = 0
        for (i in fourBandLevels.indices) fourBandLevels[i] = 0
        activePreset = Preset.NONE
        _currentPreset.value = Preset.NONE
        dspProcessor.resetEffects()
        Log.d(TAG, "All effects reset")
    }

    fun setVirtualizer(strength: Float) {
        manualVirtualizerStrength = strength
        Log.d(TAG, "Virtualizer strength set to $strength")
        applyEffects()
    }

    /**
     * Core logic engine - maps user settings to 10-band EQ
     * Based on: "Production-Grade Audio Effects Manager" logic
     */
    private fun applyEffects() {
        val numBands = dspProcessor.getEqBandCount()

        // --- 0. Preset-Specific Effects (Spatializer) ---
        var spatialStrength = when (activePreset) {
            Preset.CINEMA -> 0.6f
            Preset.SPATIAL -> 0.9f
            Preset.ELECTRONIC -> 0.2f
            else -> 0.0f
        }
        
        // Blend Presets + Manual Virtualizer + 3D Surround
        var finalVirtualizer = spatialStrength
        
        // 1. Manual Virtualizer (Spatial Audio Slider)
        finalVirtualizer = maxOf(finalVirtualizer, manualVirtualizerStrength)

        // 2. 3D Surround (Immersion Slider)
        if (surround3dEnabled) {
             val s3dFactor = surround3dStrength / 1000f // 0.0 to 1.0
             // 3D Surround adds 80% effect max
             finalVirtualizer = maxOf(finalVirtualizer, s3dFactor * 0.8f) 
        }

        // Apply final combined strength
        dspProcessor.setVirtualizer(finalVirtualizer)

        for (i in 0 until numBands) {
            val centerFreq = dspProcessor.getEqBandFrequency(i)
            var bandGainDb = 0f

            // 1. Preset Logic (Derived from User Snippet MilliBels)
            // Range +/- 15dB approx
            val presetGain = when (activePreset) {
                Preset.MUSIC -> {
                    if (centerFreq < 100) 3.0f
                    else if (centerFreq > 8000) 3.0f
                    else 0f
                }
                Preset.CINEMA -> {
                    if (centerFreq < 150) 6.0f
                    else if (centerFreq in 200f..2000f) -3.0f
                    else if (centerFreq > 4000) 4.5f
                    else 0f
                }
                Preset.VOICE -> {
                    if (centerFreq < 200) -6.0f
                    else if (centerFreq in 500f..4000f) 6.0f
                    else 0f
                }
                Preset.SPATIAL -> {
                    if (centerFreq in 200f..800f) -3.0f
                    else if (centerFreq > 6000) 3.0f
                    else 0f
                }
                Preset.NIGHT -> {
                    if (centerFreq < 100) -6.0f
                    else if (centerFreq > 8000) -6.0f
                    else 0f
                }
                Preset.ROCK -> {
                    // Classic V-Shape
                    if (centerFreq < 100) 6.0f
                    else if (centerFreq > 6000) 4.5f
                    else -3.0f
                }
                Preset.POP -> {
                    // Mid-forward for vocals
                    if (centerFreq in 400f..3000f) 3.0f
                    else if (centerFreq > 5000) 1.5f
                    else 0f
                }
                Preset.JAZZ -> {
                    if (centerFreq in 200f..600f) 3.0f
                    else if (centerFreq > 8000) 1.5f
                    else 0f
                }
                Preset.CLASSIC -> {
                    if (centerFreq > 4000) 4.5f
                    else 0f
                }
                Preset.ELECTRONIC -> {
                    // Deep energetic sub-bass
                    if (centerFreq < 100) 9.0f
                    else if (centerFreq > 8000) 6.0f
                    else -1.5f
                }
                Preset.TREBLE -> {
                    // Crisp highs emphasis
                    if (centerFreq > 6000) 8.0f
                    else if (centerFreq > 2000) 4.0f
                    else if (centerFreq < 200) -3.0f
                    else 0f
                }
                Preset.OCTAPAD -> {
                    // Octapad Style: Tight punchy bass + distinct snap
                    if (centerFreq in 60f..120f) 8.0f       // The main "Thump"
                    else if (centerFreq < 60f) 4.0f         // Sub support
                    else if (centerFreq in 3000f..5000f) 5.0f // The "Snap" / Attack
                    else if (centerFreq in 200f..800f) -3.0f  // Cut mud/boxiness
                    else if (centerFreq > 8000f) 2.0f       // Air
                    else 0f
                }
                else -> 0f
            }

            bandGainDb += presetGain

            // 2. Deep Bass Logic - Professional Sub-bass enhancement
            // Snippet: "Target < 80Hz ... + 800"
            // Implementation: We use a shelf-like curve on the lowest two bands
            if (deepBassOn) {
                if (centerFreq < 40) bandGainDb += 12.0f   // True Sub-Bass "Rumble"
                else if (centerFreq < 70) bandGainDb += 9.0f // Tight Bass "Thump"
                else if (centerFreq < 100) bandGainDb += 4.5f // Upper Bass Fill
            }

            // 2b. Treble Boost Logic
            if (trebleBoostEnabled) {
                if (centerFreq > 4000) {
                    val boostFactor = trebleBoostStrength / 1000f // 0.0 to 1.0
                    val maxBoost = 6.0f
                    bandGainDb += maxBoost * boostFactor
                } else if (centerFreq > 2000) {
                    val boostFactor = trebleBoostStrength / 1000f // 0.0 to 1.0
                    val maxBoost = 3.0f
                    bandGainDb += maxBoost * boostFactor
                }
            }
            
            // 2c. 3D Surround EQ Logic (HRTF Simulation)
            // Creating a "Phantom Center" and "Wide Air"
            if (surround3dEnabled) {
                val strength = surround3dStrength / 1000f // 0.0 to 1.0
                
                if (centerFreq > 10000) {
                     // Air Boost: Adds verticality/space (+4dB max)
                     bandGainDb += 4.0f * strength
                } else if (centerFreq in 3000f..5000f) {
                     // Presence Limit: Slight cut to push vocals "back" into the mix for depth (-2dB max)
                     // or Slight boost for clarity? Usually cuts create depth.
                     // Let's do a slight cut for "Distance" effect
                     bandGainDb -= 2.0f * strength
                } else if (centerFreq in 200f..600f) {
                     // Mud Cut: Cleans up the "Boxiness" (-3dB max)
                     bandGainDb -= 3.0f * strength
                }
            }

            // 3. 4-Band EQ Logic - Optimized range distribution
            // Band 0: Bass (40-250Hz)
            // Band 1: Mids (250-2000Hz)
            // Band 2: High-Mid (2000-8000Hz)
            // Band 3: Highs (8000-16000Hz)
            if (fourBandEnabled) {
                val userGainLevel = if (centerFreq < 250) fourBandLevels[0]
                else if (centerFreq < 2000) fourBandLevels[1]
                else if (centerFreq < 8000) fourBandLevels[2]
                else fourBandLevels[3]

                // PREMIUM PUNCH SCALING: 
                // Matches the feel of the old code (1.2dB per unit = +12dB at full)
                // New smart limiter handles this cleanly
                val userGainDb = userGainLevel * 1.2f
                bandGainDb += userGainDb
            }

            // 4. Professional Harmonization: "Space for Sub-bass"
            // When Deep Bass is enabled, we heavily attenuate mids and highs (vocal/treble range)
            // This prevents intermodulation distortion and makes the sub-bass feel more "professional" and "isolated"
            if (deepBassOn) {
                if (centerFreq in 400.0f..3000.0f) {
                    // Attenuate mid-range (Vocals) slightly (-3.0dB) to avoid "thinning" while keeping clarity
                    bandGainDb -= 3.0f
                } else if (centerFreq > 4000.0f) {
                    // Attenuate highs (-2.5dB) just enough to prevent "sibilance" during high bass
                    bandGainDb -= 2.5f
                }
            }

            // 5. Loudness Compensation: "Native Make-up Gain"
            // When Deep Bass is on, we add a global boost (+2.0dB) to compensate for mid-cuts
            // This keeps the "Energy" of the track high.
            if (deepBassOn) {
                bandGainDb += 2.0f
            }
            
            // 6. Treble-Specific Presets Logic
            val extraTrebleGain = when (activePreset) {
                Preset.TREBLE_BOOST -> {
                    if (centerFreq > 4000) 7.5f
                    else if (centerFreq > 2000) 3.0f
                    else 0f
                }
                Preset.BRIGHT -> {
                    if (centerFreq > 8000) 6.0f
                    else if (centerFreq in 2000f..8000f) 2.5f
                    else -1.5f // Slight bass cut for brightness
                }
                Preset.VOCAL_BOOST -> {
                    if (centerFreq in 500f..4000f) 5.0f
                    else if (centerFreq < 200) -3.0f // Cut low end to clear vocals
                    else 0f
                }
                else -> 0f
            }
            bandGainDb += extraTrebleGain

            // 6. Safety Cap: Prevent extreme boosts that cause harsh limiting or "fat" distortion
            // We clamp the total boost to a professional limit (+14.5dB)
            val clampedGainDb = bandGainDb.coerceIn(-15.0f, 14.5f)

            // Apply to DSP
            dspProcessor.setEqBand(i, clampedGainDb)
        }
    }

    fun getEqBandCount(): Int = dspProcessor.getEqBandCount()
    fun getEqBandFrequency(band: Int): Float = dspProcessor.getEqBandFrequency(band)
    fun getEqBandGain(band: Int): Float = dspProcessor.getEqBandGain(band)
}
