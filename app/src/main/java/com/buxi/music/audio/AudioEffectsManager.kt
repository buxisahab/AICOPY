package com.buxi.music.audio

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.media.audiofx.EnvironmentalReverb
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Date

class AudioEffectsManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var audioSessionId: Int = 0
    private var isSessionActive = false
    private var isOEMOverrideDetected = false
    private var lastSessionChangeTime = 0L
    
    // Audio effects
    private var bassBoostEffect: BassBoost? = null
    private var virtualizerEffect: Virtualizer? = null
    private var equalizerEffect: Equalizer? = null
    private var reverbEffect: PresetReverb? = null
    private var loudnessEnhancerEffect: LoudnessEnhancer? = null
    private var environmentalReverbEffect: EnvironmentalReverb? = null
    
    // Effect states
    private var bassBoostEnabled = false
    private var bassBoost = 0f
    private var is3DEnabled = false
    private var bassLevel = 50f
    private var midLevel = 50f
    private var trebleLevel = 50f
    private var reverbEnabled = false
    private var reverbPreset = PresetReverb.PRESET_NONE.toInt()
    private var loudnessGain = 0f
    private var echoStrength = 0f
    
    // Shared preferences for persistence
    private val sharedPrefs = context.getSharedPreferences("audio_effects", Context.MODE_PRIVATE)
    private val crashPrefs = context.getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
    
    // Crash logging
    private val crashLogDir = File(context.filesDir, "crash_logs")
    
    init {
        setupCrashHandler()
        loadSettings()
        startEffectPersistence()
        detectOEMAudioEngines()
    }
    
    fun setAudioSessionId(sessionId: Int) {
        if (audioSessionId != sessionId) {
            Log.d("AudioEffectsManager", "Audio session changed from $audioSessionId to $sessionId")
            audioSessionId = sessionId
            lastSessionChangeTime = System.currentTimeMillis()
            
            // Try multiple session IDs for better device compatibility
            val sessionIds = listOf(sessionId, 0, 1, 2, 3, 4, 5)
            var effectsInitialized = false
            
            for (testSessionId in sessionIds) {
                try {
                    this.audioSessionId = testSessionId
                    initializeEffects()
                    if (verifyEffectsWorking()) {
                        effectsInitialized = true
                        Log.d("AudioEffectsManager", "Effects initialized with session ID: $testSessionId")
                        break
                    }
                } catch (e: Exception) {
                    Log.w("AudioEffectsManager", "Failed to initialize with session ID: $testSessionId", e)
                    continue
                }
            }
            
            if (!effectsInitialized) {
                Log.w("AudioEffectsManager", "Failed to initialize effects with any session ID")
            }
            
            // Reattach effects with delay for OEM override
            if (isOEMOverrideDetected) {
                scope.launch(Dispatchers.IO) {
                    delay(500) // Wait for OEM effects to settle
                    reattachAllEffects()
                }
            }
        }
    }
    
    // Verify that effects are actually working
    private fun verifyEffectsWorking(): Boolean {
        return try {
            bassBoostEffect?.let { effect ->
                effect.strengthSupported
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    // Enhanced crash handler setup
    private fun setupCrashHandler() {
        if (!crashLogDir.exists()) {
            crashLogDir.mkdirs()
        }
        
        // Check for crash loop prevention
        val lastCrash = crashPrefs.getLong("lastCrashTime", 0L)
        if (System.currentTimeMillis() - lastCrash < 5000) {
            Log.w("AudioEffectsManager", "Crash loop detected, clearing crash data")
            crashPrefs.edit().clear().apply()
        }
        
        // Set up global crash handler
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            logCrash(throwable)
            val prefs = context.getSharedPreferences("crash_prefs", Context.MODE_PRIVATE)
            prefs.edit().putLong("lastCrashTime", System.currentTimeMillis()).apply()
            
            // Restart app safely
            val intent = Intent(context, com.buxi.music.MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
    
    // Enhanced crash logging
    private fun logCrash(e: Throwable) {
        try {
            val logFile = File(crashLogDir, "crash_${System.currentTimeMillis()}.txt")
            val logContent = buildString {
                appendLine("Time: ${Date()}")
                appendLine("Message: ${e.message}")
                appendLine("Audio Session ID: $audioSessionId")
                appendLine("OEM Override Detected: $isOEMOverrideDetected")
                appendLine("Bass Boost Enabled: $bassBoostEnabled")
                appendLine("3D Enabled: $is3DEnabled")
                appendLine("StackTrace:")
                appendLine(Log.getStackTraceString(e))
            }
            logFile.writeText(logContent)
            Log.e("AudioEffectsManager", "Crash logged to: ${logFile.absolutePath}")
        } catch (logError: Exception) {
            Log.e("AudioEffectsManager", "Failed to log crash", logError)
        }
    }
    
    // Detect OEM audio engines
    private fun detectOEMAudioEngines() {
        val packageManager = context.packageManager
        val oemPackages = listOf(
            "com.dolby.dax3", // Dolby Atmos
            "com.dolby.dax2", // Dolby Audio
            "com.samsung.android.soundalive", // Samsung SoundAlive
            "com.samsung.android.soundalive_plus", // Samsung SoundAlive Plus
            "com.dirac.dirac", // Dirac
            "com.dirac.dirac.one", // Dirac One
            "com.realsound.realsound", // RealSound
            "com.sony.soundenhancement", // Sony Sound Enhancement
            "com.lg.sound", // LG Sound
            "com.huawei.soundplus", // Huawei Sound Plus
            "com.xiaomi.sound", // Xiaomi Sound
            "com.oneplus.sound" // OnePlus Sound
        )
        
        isOEMOverrideDetected = oemPackages.any { packageName ->
            try {
                packageManager.getPackageInfo(packageName, 0)
                Log.d("AudioEffectsManager", "OEM audio engine detected: $packageName")
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
        
        Log.d("AudioEffectsManager", "OEM override detection: $isOEMOverrideDetected")
    }
    
    // Enhanced effect reattachment
    private fun reattachAllEffects() {
        Log.d("AudioEffectsManager", "Reattaching all effects to session $audioSessionId")
        
        try {
            bassBoostEffect?.let { effect ->
                try {
                    effect.enabled = bassBoostEnabled
                    if (bassBoostEnabled) {
                        effect.setStrength(1000.toShort()) // Maximum bass boost strength
                    }
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to reattach BassBoost", e)
                    logCrash(e)
                }
            }
            
            virtualizerEffect?.let { effect ->
                try {
                    effect.enabled = is3DEnabled
                    if (is3DEnabled) {
                        effect.setStrength(1000.toShort())
                    }
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to reattach Virtualizer", e)
                    logCrash(e)
                }
            }
            
            equalizerEffect?.let { effect ->
                try {
                    effect.enabled = true
                    val numBands = effect.numberOfBands.toInt()
                    if (numBands >= 3) {
                        effect.setBandLevel(0, ((bassLevel - 50) * 30).toInt().toShort())
                        effect.setBandLevel((numBands / 2).toShort(), ((midLevel - 50) * 30).toInt().toShort())
                        effect.setBandLevel((numBands - 1).toShort(), ((trebleLevel - 50) * 30).toInt().toShort())
                    }
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to reattach Equalizer", e)
                    logCrash(e)
                }
            }
            
            loudnessEnhancerEffect?.let { effect ->
                try {
                    effect.enabled = true
                    effect.setTargetGain((loudnessGain * 10).toInt())
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to reattach LoudnessEnhancer", e)
                    logCrash(e)
                }
            }
            
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error during effect reattachment", e)
            logCrash(e)
        }
    }
    
    private fun loadSettings() {
        bassBoostEnabled = sharedPrefs.getBoolean("bass_boost_enabled", false)
        bassBoost = sharedPrefs.getFloat("bass_boost", 0f)
        is3DEnabled = sharedPrefs.getBoolean("is_3d", false)
        bassLevel = sharedPrefs.getFloat("bass_level", 50f)
        midLevel = sharedPrefs.getFloat("mid_level", 50f)
        trebleLevel = sharedPrefs.getFloat("treble_level", 50f)
        reverbEnabled = sharedPrefs.getBoolean("reverb_enabled", false)
        reverbPreset = sharedPrefs.getInt("reverb_preset", PresetReverb.PRESET_NONE.toInt())
        loudnessGain = sharedPrefs.getFloat("loudness_gain", 0f)
        echoStrength = sharedPrefs.getFloat("echo_strength", 0f)
    }
    
    private fun saveSettings() {
        sharedPrefs.edit()
            .putBoolean("bass_boost_enabled", bassBoostEnabled)
            .putFloat("bass_boost", bassBoost)
            .putBoolean("is_3d", is3DEnabled)
            .putFloat("bass_level", bassLevel)
            .putFloat("mid_level", midLevel)
            .putFloat("treble_level", trebleLevel)
            .putBoolean("reverb_enabled", reverbEnabled)
            .putInt("reverb_preset", reverbPreset)
            .putFloat("loudness_gain", loudnessGain)
            .putFloat("echo_strength", echoStrength)
            .apply()
    }
    
    private fun initializeEffects() {
        Log.d("AudioEffectsManager", "Initializing audio effects for session $audioSessionId")
        
        try {
            // Release existing effects first
            releaseEffects()
            
            // Initialize Bass Boost with enhanced error handling
            bassBoostEffect = createBassBoost()
            
            // Initialize Virtualizer
            virtualizerEffect = createVirtualizer()
            
            // Initialize Equalizer
            equalizerEffect = createEqualizer()
            
            // Initialize Reverb
            reverbEffect = createReverb()
            
            // Initialize Loudness Enhancer
            loudnessEnhancerEffect = createLoudnessEnhancer()
            
            // Initialize Environmental Reverb
            environmentalReverbEffect = createEnvironmentalReverb()
            
            // Apply all current settings
            reapplyAllEffects()
            
            Log.d("AudioEffectsManager", "Audio effects initialized successfully for session $audioSessionId")
            
            // Schedule reattachment for OEM override
            if (isOEMOverrideDetected) {
                scope.launch(Dispatchers.IO) {
                    delay(1000) // Wait for OEM effects to settle
                    reattachAllEffects()
                }
            }
            
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Failed to initialize audio effects", e)
            logCrash(e)
        }
    }
    
    // Enhanced effect creation with better error handling
    private fun createBassBoost(): BassBoost? {
        return try {
            if (audioSessionId > 0) {
                BassBoost(0, audioSessionId).apply {
                    try {
                        enabled = bassBoostEnabled
                        setStrength(1000.toShort()) // Maximum bass boost strength
                        Log.d("AudioEffectsManager", "BassBoost created successfully for session $audioSessionId")
                    } catch (e: Exception) {
                        Log.e("AudioEffectsManager", "Failed to configure BassBoost", e)
                        logCrash(e)
                    }
                }
            } else {
                Log.w("AudioEffectsManager", "Invalid audio session ID for BassBoost")
                null
            }
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Failed to create BassBoost", e)
            logCrash(e)
            null
        }
    }
    
    private fun createVirtualizer(): Virtualizer? {
        return try {
            if (audioSessionId > 0) {
                Virtualizer(0, audioSessionId).apply {
                    try {
                        enabled = is3DEnabled
                        setStrength(if (is3DEnabled) 1000.toShort() else 0.toShort())
                        Log.d("AudioEffectsManager", "Virtualizer created successfully for session $audioSessionId")
                    } catch (e: Exception) {
                        Log.e("AudioEffectsManager", "Failed to configure Virtualizer", e)
                        logCrash(e)
                    }
                }
            } else {
                Log.w("AudioEffectsManager", "Invalid audio session ID for Virtualizer")
                null
            }
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Failed to create Virtualizer", e)
            logCrash(e)
            null
        }
    }
    
    private fun createEqualizer(): Equalizer? {
        return try {
            if (audioSessionId > 0) {
                Equalizer(0, audioSessionId).apply {
                    try {
                        enabled = true
                        val numBands = numberOfBands.toInt()
                        if (numBands >= 3) {
                            setBandLevel(0, ((bassLevel - 50) * 30).toInt().toShort())
                            setBandLevel((numBands / 2).toShort(), ((midLevel - 50) * 30).toInt().toShort())
                            setBandLevel((numBands - 1).toShort(), ((trebleLevel - 50) * 30).toInt().toShort())
                        }
                        Log.d("AudioEffectsManager", "Equalizer created successfully for session $audioSessionId")
                    } catch (e: Exception) {
                        Log.e("AudioEffectsManager", "Failed to configure Equalizer", e)
                        logCrash(e)
                    }
                }
            } else {
                Log.w("AudioEffectsManager", "Invalid audio session ID for Equalizer")
                null
            }
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Failed to create Equalizer", e)
            logCrash(e)
            null
        }
    }
    
    private fun createLoudnessEnhancer(): LoudnessEnhancer? {
        return try {
            if (audioSessionId > 0) {
                LoudnessEnhancer(audioSessionId).apply {
                    try {
                        enabled = true
                        setTargetGain((loudnessGain * 10).toInt())
                        Log.d("AudioEffectsManager", "LoudnessEnhancer created successfully for session $audioSessionId")
                    } catch (e: Exception) {
                        Log.e("AudioEffectsManager", "Failed to configure LoudnessEnhancer", e)
                        logCrash(e)
                    }
                }
            } else {
                Log.w("AudioEffectsManager", "Invalid audio session ID for LoudnessEnhancer")
                null
            }
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Failed to create LoudnessEnhancer", e)
            logCrash(e)
            null
        }
    }
    
    // Enhanced effect persistence with better error handling
    private fun startEffectPersistence() {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // Re-apply all effects to ensure they stay active
                    reapplyAllEffects()
                    
                    // Check for session changes
                    if (System.currentTimeMillis() - lastSessionChangeTime > 10000) {
                        // Reattach effects every 10 seconds if no recent session changes
                        reattachAllEffects()
                    }
                    
                    delay(3000) // Check every 3 seconds
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Error in effect persistence", e)
                    logCrash(e)
                    delay(5000) // Wait longer on error
                }
            }
        }
    }
    
    // Enhanced reapply with better error handling
    private fun reapplyAllEffects() {
        try {
            bassBoostEffect?.let { effect ->
                try {
                    if (bassBoostEnabled) {
                        effect.enabled = true
                        effect.setStrength(1000.toShort()) // Maximum bass boost strength
                    } else {
                        effect.enabled = false
                    }
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to reapply BassBoost", e)
                    logCrash(e)
                }
            }
            
            virtualizerEffect?.let { effect ->
                try {
                    if (is3DEnabled) {
                        effect.enabled = true
                        effect.setStrength(1000.toShort())
                    } else {
                        effect.enabled = false
                    }
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to reapply Virtualizer", e)
                    logCrash(e)
                }
            }
            
            equalizerEffect?.let { effect ->
                try {
                    effect.enabled = true
                    val numBands = effect.numberOfBands.toInt()
                    if (numBands >= 3) {
                        effect.setBandLevel(0, ((bassLevel - 50) * 30).toInt().toShort())
                        effect.setBandLevel((numBands / 2).toShort(), ((midLevel - 50) * 30).toInt().toShort())
                        effect.setBandLevel((numBands - 1).toShort(), ((trebleLevel - 50) * 30).toInt().toShort())
                    }
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to reapply Equalizer", e)
                    logCrash(e)
                }
            }
            
            loudnessEnhancerEffect?.let { effect ->
                try {
                    effect.enabled = true
                    effect.setTargetGain((loudnessGain * 10).toInt())
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to reapply LoudnessEnhancer", e)
                    logCrash(e)
                }
            }
            
            reverbEffect?.let { effect ->
                try {
                    if (reverbEnabled) {
                        effect.enabled = true
                        effect.preset = reverbPreset.toShort()
                    } else {
                        effect.enabled = false
                    }
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to reapply Reverb", e)
                    logCrash(e)
                }
            }
            
            environmentalReverbEffect?.let { effect ->
                try {
                    if (echoStrength > 0) {
                        effect.enabled = true
                        effect.setDecayTime((echoStrength * 50).toInt())
                    } else {
                        effect.enabled = false
                    }
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to reapply EnvironmentalReverb", e)
                    logCrash(e)
                }
            }
            
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error during effect reapplication", e)
            logCrash(e)
        }
    }
    
    // Enhanced release with better error handling
    private fun releaseEffects() {
        try {
            bassBoostEffect?.release()
            virtualizerEffect?.release()
            equalizerEffect?.release()
            reverbEffect?.release()
            loudnessEnhancerEffect?.release()
            environmentalReverbEffect?.release()
            
            bassBoostEffect = null
            virtualizerEffect = null
            equalizerEffect = null
            reverbEffect = null
            loudnessEnhancerEffect = null
            environmentalReverbEffect = null
            
            Log.d("AudioEffectsManager", "Audio effects released successfully")
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error releasing effects", e)
            logCrash(e)
        }
    }
    
    // Crash log sharing functionality
    fun shareCrashLogs() {
        try {
            val latestFile = crashLogDir.listFiles()?.maxByOrNull { it.lastModified() }
            latestFile?.let { logFile ->
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    logFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Send Crash Log"))
            } ?: run {
                Log.w("AudioEffectsManager", "No crash logs found to share")
            }
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Failed to share crash logs", e)
            logCrash(e)
        }
    }
    
    // Get crash log count
    fun getCrashLogCount(): Int {
        return crashLogDir.listFiles()?.size ?: 0
    }
    
    // Clear crash logs
    fun clearCrashLogs() {
        try {
            crashLogDir.listFiles()?.forEach { it.delete() }
            Log.d("AudioEffectsManager", "Crash logs cleared")
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Failed to clear crash logs", e)
            logCrash(e)
        }
    }
    
    private fun createReverb(): PresetReverb? {
        if (audioSessionId <= 0) {
            Log.w("AudioEffectsManager", "Invalid audio session ID for Reverb")
            return null
        }
        return try {
            PresetReverb(0, audioSessionId).apply {
                try {
                    enabled = reverbEnabled
                    preset = reverbPreset.toShort()
                    Log.d("AudioEffectsManager", "Reverb created successfully for session $audioSessionId")
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to configure Reverb", e)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Initialization error: Reverb not supported or session invalid ($audioSessionId)", e)
            null
        }
    }
    
    private fun createEnvironmentalReverb(): EnvironmentalReverb? {
        if (audioSessionId <= 0) {
            Log.w("AudioEffectsManager", "Invalid audio session ID for EnvironmentalReverb")
            return null
        }
        return try {
            EnvironmentalReverb(0, audioSessionId).apply {
                try {
                    enabled = echoStrength > 0
                    setDecayTime((echoStrength * 50).toInt())
                    Log.d("AudioEffectsManager", "EnvironmentalReverb created successfully for session $audioSessionId")
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to configure EnvironmentalReverb", e)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Initialization error: EnvironmentalReverb not supported or session invalid ($audioSessionId)", e)
            null
        }
    }
    
    // Enhanced public methods with better error handling
    fun setBassBoost(enabled: Boolean, strength: Float) {
        try {
            bassBoostEnabled = enabled
            bassBoost = strength
            bassBoostEffect?.let { effect ->
                try {
                    effect.enabled = enabled
                    effect.setStrength(1000.toShort()) // Maximum bass boost strength
                    Log.d("AudioEffectsManager", "BassBoost updated: enabled=$enabled, strength=$strength")
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to update BassBoost", e)
                    logCrash(e)
                }
            }
            saveSettings()
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error in setBassBoost", e)
            logCrash(e)
        }
    }
    
    fun set3DEnabled(enabled: Boolean) {
        try {
            is3DEnabled = enabled
            virtualizerEffect?.let { effect ->
                try {
                    effect.enabled = enabled
                    effect.setStrength(if (enabled) 1000.toShort() else 0.toShort())
                    Log.d("AudioEffectsManager", "3D Virtualizer updated: enabled=$enabled")
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to update Virtualizer", e)
                    logCrash(e)
                }
            }
            saveSettings()
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error in set3DEnabled", e)
            logCrash(e)
        }
    }
    
    fun setEqualizer(bass: Float, mid: Float, treble: Float) {
        try {
            bassLevel = bass
            midLevel = mid
            trebleLevel = treble
            equalizerEffect?.let { effect ->
                try {
                    val numBands = effect.numberOfBands.toInt()
                    if (numBands >= 3) {
                        effect.setBandLevel(0, ((bass - 50) * 30).toInt().toShort())
                        effect.setBandLevel((numBands / 2).toShort(), ((mid - 50) * 30).toInt().toShort())
                        effect.setBandLevel((numBands - 1).toShort(), ((treble - 50) * 30).toInt().toShort())
                    }
                    Log.d("AudioEffectsManager", "Equalizer updated: bass=$bass, mid=$mid, treble=$treble")
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to update Equalizer", e)
                    logCrash(e)
                }
            }
            saveSettings()
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error in setEqualizer", e)
            logCrash(e)
        }
    }
    
    fun setReverb(enabled: Boolean, preset: Int) {
        try {
            reverbEnabled = enabled
            reverbPreset = preset
            reverbEffect?.let { effect ->
                try {
                    effect.enabled = enabled
                    effect.preset = preset.toShort()
                    Log.d("AudioEffectsManager", "Reverb updated: enabled=$enabled, preset=$preset")
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to update Reverb", e)
                    logCrash(e)
                }
            }
            saveSettings()
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error in setReverb", e)
            logCrash(e)
        }
    }
    
    fun setLoudnessGain(gain: Float) {
        try {
            loudnessGain = gain
            loudnessEnhancerEffect?.let { effect ->
                try {
                    effect.enabled = true
                    effect.setTargetGain((gain * 10).toInt())
                    Log.d("AudioEffectsManager", "LoudnessEnhancer updated: gain=$gain")
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to update LoudnessEnhancer", e)
                    logCrash(e)
                }
            }
            saveSettings()
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error in setLoudnessGain", e)
            logCrash(e)
        }
    }
    
    fun setEchoStrength(strength: Float) {
        try {
            echoStrength = strength
            environmentalReverbEffect?.let { effect ->
                try {
                    effect.enabled = strength > 0
                    effect.setDecayTime((strength * 50).toInt())
                    Log.d("AudioEffectsManager", "EnvironmentalReverb updated: strength=$strength")
                } catch (e: Exception) {
                    Log.e("AudioEffectsManager", "Failed to update EnvironmentalReverb", e)
                    logCrash(e)
                }
            }
            saveSettings()
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error in setEchoStrength", e)
            logCrash(e)
        }
    }
    
    // Enhanced release method
    fun release() {
        try {
            releaseEffects()
            Log.d("AudioEffectsManager", "AudioEffectsManager released successfully")
        } catch (e: Exception) {
            Log.e("AudioEffectsManager", "Error releasing AudioEffectsManager", e)
            logCrash(e)
        }
    }
    
    // Get current effect states
    fun getBassBoostState() = Pair(bassBoostEnabled, bassBoost)
    fun get3DState() = is3DEnabled
    fun getEqualizerState() = Triple(bassLevel, midLevel, trebleLevel)
    fun getReverbState() = Pair(reverbEnabled, reverbPreset)
    fun getLoudnessGain() = loudnessGain
    fun getEchoStrength() = echoStrength
}
