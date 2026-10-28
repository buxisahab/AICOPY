package com.buxi.music.dsp

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log
import org.json.JSONObject

class NativeDSPBackend : DSPBackend {
    override val name: String = "native"
    private var inited = false

    override fun init(context: Context, playerType: PlayerType, audioSessionId: Int?): Boolean {
        // Probe for native libs (placeholder). In a real implementation, loadLibrary and JNI init.
        inited = try {
            // System.loadLibrary("superpowered_dsp")
            false // default to false until native is integrated
        } catch (_: Throwable) { false }
        return inited
    }

    override fun release() { inited = false }
    override fun enable(effect: EffectName) {}
    override fun disable(effect: EffectName) {}
    override fun setParameter(effect: EffectName, key: String, value: Float) {}
    override fun setPreset(preset: String): Boolean = false
    override fun applyCustomFilter(type: String, coefficients: FloatArray): Boolean = false
    override fun importSoundPack(context: Context, uri: android.net.Uri): String? = null
    override fun setPerformanceMode(mode: PerformanceMode) {}
    override fun supportedEffects(): Set<EffectName> = emptySet()
    override fun diagnostics(): JSONObject = JSONObject().put("nativeAvailable", inited)
}

class JavaDSPBackend : DSPBackend {
    override val name: String = "java"
    private var inited = false

    override fun init(context: Context, playerType: PlayerType, audioSessionId: Int?): Boolean {
        // Placeholder for TarsosDSP or custom Java pipeline; mark available for now
        inited = true
        return true
    }

    override fun release() { inited = false }
    override fun enable(effect: EffectName) {}
    override fun disable(effect: EffectName) {}
    override fun setParameter(effect: EffectName, key: String, value: Float) {}
    override fun setPreset(preset: String): Boolean = Presets.applyPreset(this, preset)
    override fun applyCustomFilter(type: String, coefficients: FloatArray): Boolean = true
    override fun importSoundPack(context: Context, uri: android.net.Uri): String? = null
    override fun setPerformanceMode(mode: PerformanceMode) {}
    override fun supportedEffects(): Set<EffectName> = EffectName.values().toSet()
    override fun diagnostics(): JSONObject = JSONObject().put("javaAvailable", inited)
}

class AndroidAudioEffectsBackend : DSPBackend {
    override val name: String = "android_audiofx"
    private var eq: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var rv: PresetReverb? = null
    private var vz: Virtualizer? = null
    private var session: Int = 0
    private var appContext: Context? = null
    private val TAG = "AndroidAudioEffectsBackend"
    
    // Track active effects for proper management
    private val activeEffects = mutableSetOf<EffectName>()

    override fun init(context: Context, playerType: PlayerType, audioSessionId: Int?): Boolean {
        appContext = context.applicationContext
        session = audioSessionId ?: 0
        Log.d(TAG, "init called with session=$session, playerType=$playerType")
        
        // Lazy creation: verify we can create at least one effect; try provided session then fallback 0
        return try {
            // Probe Equalizer with given session; if fails, retry with global 0
            try {
                eq?.release(); eq = null
                eq = Equalizer(0, session).apply { enabled = false }
                Log.d(TAG, "Equalizer created successfully with session=$session")
            } catch (e: Throwable) {
                Log.w(TAG, "EQ attach failed on session=$session, retry with global session 0", e)
                session = 0
                eq?.release(); eq = null
                eq = Equalizer(0, session).apply { enabled = false }
                Log.d(TAG, "Equalizer created with fallback session=0")
            }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "init failed", t)
            false
        }
    }

    override fun release() {
        Log.d(TAG, "release called")
        try { eq?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { rv?.release() } catch (_: Exception) {}
        try { vz?.release() } catch (_: Exception) {}
        eq = null
        bassBoost = null
        rv = null
        vz = null
    }

    override fun enable(effect: EffectName) {
        Log.d(TAG, "enable called for effect: ${effect.wireName}")
        activeEffects.add(effect)
        
        try {
            when (effect) {
                EffectName.equalizer -> {
                    if (eq == null) {
                        Log.d(TAG, "Creating Equalizer with session=$session")
                        eq = Equalizer(0, session)
                    }
                    eq?.enabled = true
                    Log.d(TAG, "Equalizer enabled")
                }
                EffectName.bassBoost -> {
                    // Maximum BassBoost implementation with extreme intensity
                    if (bassBoost == null) {
                        Log.d(TAG, "Creating BassBoost with session=$session")
                        bassBoost = try {
                            BassBoost(0, session).apply {
                                setStrength(1000) // Maximum strength for extreme bass
                                enabled = true
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to create BassBoost with session=$session", e)
                            null
                        }
                        Log.d(TAG, "BassBoost creation ${if (bassBoost != null) "succeeded" else "failed"}")
                    } else {
                        Log.d(TAG, "BassBoost already exists, enabling")
                        bassBoost?.enabled = true
                    }
                }
                EffectName.reverb -> {
                    if (rv == null) {
                        Log.d(TAG, "Creating Reverb with session=$session")
                        rv = PresetReverb(0, session)
                    }
                    rv?.enabled = true
                    Log.d(TAG, "Reverb enabled (for rain effect)")
                }
                EffectName.virtualizer -> {
                    if (vz == null) {
                        Log.d(TAG, "Creating Virtualizer with session=$session")
                        vz = Virtualizer(0, session)
                    }
                    vz?.enabled = true
                    Log.d(TAG, "Virtualizer enabled (for thunder effect)")
                }
                EffectName.compressor, EffectName.limiter, EffectName.delay, EffectName.pitchShift -> {
                    // These effects are not supported by Android AudioEffects
                    // Map to closest available effect
                    when (effect) {
                        EffectName.compressor -> {
                            // Use BassBoost for compression-like effect
                            if (bassBoost == null) {
                                bassBoost = BassBoost(0, session)
                            }
                            bassBoost?.enabled = true
                            Log.d(TAG, "Compressor mapped to BassBoost")
                        }
                        EffectName.limiter -> {
                            // Use Reverb for limiting effect
                            if (rv == null) {
                                rv = PresetReverb(0, session)
                            }
                            rv?.enabled = true
                            Log.d(TAG, "Limiter mapped to Reverb")
                        }
                        else -> {
                            Log.d(TAG, "Effect ${effect.wireName} not supported, using fallback")
                        }
                    }
                }
                else -> {
                    Log.d(TAG, "Unknown effect: ${effect.wireName}")
                }
            }
            
            // Log active effects for debugging
            Log.d(TAG, "Active effects: ${activeEffects.map { it.wireName }}")
            
        } catch (e: Throwable) {
            Log.w(TAG, "enable ${effect.wireName} failed on session=$session", e)
            // Don't retry with session 0 to avoid conflicts
        }
    }

    override fun disable(effect: EffectName) {
        Log.d(TAG, "disable called for effect: ${effect.wireName}")
        activeEffects.remove(effect)
        
        when (effect) {
            EffectName.equalizer -> {
                eq?.enabled = false
                Log.d(TAG, "Equalizer disabled")
            }
            EffectName.bassBoost -> {
                bassBoost?.enabled = false
                Log.d(TAG, "BassBoost disabled")
            }
            EffectName.reverb -> {
                rv?.enabled = false
                Log.d(TAG, "Reverb disabled (rain effect)")
            }
            EffectName.virtualizer -> {
                vz?.enabled = false
                Log.d(TAG, "Virtualizer disabled (thunder effect)")
            }
            EffectName.compressor -> {
                // Disable mapped BassBoost
                bassBoost?.enabled = false
                Log.d(TAG, "Compressor (BassBoost) disabled")
            }
            EffectName.limiter -> {
                // Disable mapped Reverb
                rv?.enabled = false
                Log.d(TAG, "Limiter (Reverb) disabled")
            }
            else -> {
                Log.d(TAG, "Unknown effect to disable: ${effect.wireName}")
            }
        }
        
        // Log remaining active effects for debugging
        Log.d(TAG, "Remaining active effects: ${activeEffects.map { it.wireName }}")
    }

    override fun setParameter(effect: EffectName, key: String, value: Float) {
        Log.d(TAG, "setParameter called: effect=${effect.wireName}, key=$key, value=$value")
        
        when (effect) {
            EffectName.equalizer -> {
                if (eq == null) {
                    Log.d(TAG, "Equalizer not initialized, enabling first")
                    enable(EffectName.equalizer)
                }
                if (key.startsWith("band_")) {
                    val numBands = try { eq?.numberOfBands?.toInt() ?: 5 } catch (_: Throwable) { 5 }
                    val req = key.removePrefix("band_").toIntOrNull() ?: return
                    val idx = req.coerceIn(0, numBands - 1).toShort()
                    
                    try {
                        val range = eq!!.bandLevelRange
                        // Convert 0-1 value to dB range (-1500 to +1500 millibels)
                        val level = (range[0] + (range[1] - range[0]) * value).toInt().toShort()
                        eq!!.setBandLevel(idx, level)
                        Log.d(TAG, "Equalizer band $idx set to $level (range: ${range[0]} to ${range[1]})")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to set equalizer band $idx", e)
                    }
                } else {
                    Log.w(TAG, "Unknown equalizer parameter: $key")
                }
            }
            EffectName.bassBoost -> {
                if (bassBoost == null) {
                    Log.d(TAG, "BassBoost not initialized, enabling first")
                    enable(EffectName.bassBoost)
                }
                if (key == "strength") {
                    // Maximum bass boost calculation for extreme intensity
                    val enhancedValue = (value * 2.0f).coerceIn(0f, 1f) // Double the input value for maximum bass
                    val strength = (enhancedValue * 1000f).toInt().toShort()
                    Log.d(TAG, "Setting BassBoost strength: $value -> enhanced: $enhancedValue -> strength: $strength (MAXIMUM BASS)")
                    bassBoost?.setStrength(strength)
                } else {
                    Log.w(TAG, "Unknown BassBoost parameter: $key")
                }
            }
            EffectName.reverb -> {
                if (rv == null) {
                    enable(EffectName.reverb)
                }
                if (key == "amount" || key == "level") {
                    // Map reverb amount to preset
                    val preset = when {
                        value < 0.2f -> PresetReverb.PRESET_NONE
                        value < 0.4f -> PresetReverb.PRESET_SMALLROOM
                        value < 0.6f -> PresetReverb.PRESET_MEDIUMROOM
                        value < 0.8f -> PresetReverb.PRESET_LARGEROOM
                        else -> PresetReverb.PRESET_LARGEROOM // Use LARGEROOM as highest preset
                    }
                    rv?.setPreset(preset)
                    Log.d(TAG, "Reverb preset set to $preset for value $value")
                }
            }
            EffectName.virtualizer -> {
                if (vz == null) {
                    enable(EffectName.virtualizer)
                }
                if (key == "strength") {
                    val strength = (value * 1000f).toInt().toShort()
                    vz?.setStrength(strength)
                    Log.d(TAG, "Virtualizer strength set to $strength")
                }
            }
            EffectName.compressor -> {
                // Map compressor to BassBoost
                if (bassBoost == null) {
                    enable(EffectName.compressor)
                }
                if (key == "threshold" || key == "strength") {
                    val strength = (value * 1000f).toInt().toShort()
                    bassBoost?.setStrength(strength)
                    Log.d(TAG, "Compressor (BassBoost) strength set to $strength")
                }
            }
            EffectName.limiter -> {
                // Map limiter to Reverb
                if (rv == null) {
                    enable(EffectName.limiter)
                }
                if (key == "threshold" || key == "level") {
                    val preset = when {
                        value < 0.3f -> PresetReverb.PRESET_NONE
                        value < 0.6f -> PresetReverb.PRESET_SMALLROOM
                        else -> PresetReverb.PRESET_MEDIUMROOM
                    }
                    rv?.setPreset(preset)
                    Log.d(TAG, "Limiter (Reverb) preset set to $preset")
                }
            }
            else -> {
                Log.w(TAG, "Unknown effect for parameter setting: ${effect.wireName}")
            }
        }
    }

    override fun setPreset(preset: String): Boolean {
        // Enable required effects first, then apply parameters
        val params = Presets.presets[preset] ?: return false
        val effectsToEnable = params.keys.mapNotNull { key ->
            val eff = key.substringBefore('.')
            EffectName.values().firstOrNull { it.wireName == eff }
        }.toSet()

        effectsToEnable.forEach { enable(it) }

        params.forEach { (k, v) ->
            val parts = k.split('.')
            if (parts.size == 2) {
                val e = EffectName.values().firstOrNull { it.wireName == parts[0] } ?: return@forEach
                setParameter(e, parts[1], v)
            }
        }
        return true
    }

    override fun applyCustomFilter(type: String, coefficients: FloatArray): Boolean {
        // Not supported in fallback
        return false
    }

    override fun importSoundPack(context: Context, uri: android.net.Uri): String? = null
    override fun setPerformanceMode(mode: PerformanceMode) {}
    override fun supportedEffects(): Set<EffectName> = setOf(
        EffectName.equalizer, EffectName.bassBoost, EffectName.reverb, EffectName.virtualizer
    )
    
    override fun diagnostics(): JSONObject = JSONObject().apply {
        put("session", session)
        put("eq", eq != null)
        put("bassBoost", bassBoost != null)
        put("rv", rv != null)
        put("vz", vz != null)
        put("activeEffects", activeEffects.map { it.wireName })
        put("bassBoostEnabled", bassBoost?.enabled ?: false)
        put("eqEnabled", eq?.enabled ?: false)
        put("reverbEnabled", rv?.enabled ?: false)
        put("virtualizerEnabled", vz?.enabled ?: false)
        
        if (bassBoost != null) {
            try {
                put("bassBoostStrength", bassBoost!!.getRoundedStrength())
            } catch (e: Exception) {
                put("bassBoostStrength", "unknown")
            }
        }
        
        Log.d(TAG, "Diagnostics: session=$session, activeEffects=${activeEffects.map { it.wireName }}, bassBoost=${bassBoost != null}, enabled=${bassBoost?.enabled}")
    }
}


