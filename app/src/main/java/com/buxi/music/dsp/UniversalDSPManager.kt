package com.buxi.music.dsp

import android.content.Context
import android.media.audiofx.AudioEffect
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

enum class PlayerType { EXOPLAYER, MEDIAPLAYER }

enum class PerformanceMode { HIGH_PERF, BALANCED, BATTERY_SAVER }

@Suppress("EnumEntryName")
enum class EffectName(val wireName: String) {
    equalizer("equalizer"),
    bassBoost("bassBoost"),
    reverb("reverb"),
    delay("delay"),
    compressor("compressor"),
    limiter("limiter"),
    pitchShift("pitchShift"),
    virtualizer("virtualizer")
}

interface DSPBackend {
    val name: String
    fun init(context: Context, playerType: PlayerType, audioSessionId: Int?): Boolean
    fun release()
    fun enable(effect: EffectName)
    fun disable(effect: EffectName)
    fun setParameter(effect: EffectName, key: String, value: Float)
    fun setPreset(preset: String): Boolean
    fun applyCustomFilter(type: String, coefficients: FloatArray): Boolean
    fun importSoundPack(context: Context, uri: android.net.Uri): String?
    fun setPerformanceMode(mode: PerformanceMode)
    fun supportedEffects(): Set<EffectName>
    fun diagnostics(): JSONObject
}

class UniversalDSPManager private constructor() {
    companion object {
        private const val TAG = "UniversalDSP"
        @Volatile private var INSTANCE: UniversalDSPManager? = null
        fun getInstance(): UniversalDSPManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UniversalDSPManager().also { INSTANCE = it }
            }
        }
    }

    private var backend: DSPBackend? = null
    // Use AndroidAudioEffects everywhere for maximum compatibility
    private var fallbackChain: List<(Context, PlayerType, Int?) -> DSPBackend> = listOf(
        { _, _, _ -> AndroidAudioEffectsBackend() }
    )

    private var lastInitContext: Context? = null
    private var lastPlayerType: PlayerType = PlayerType.EXOPLAYER
    private var lastAudioSessionId: Int? = null
    private var preferGlobalSession: Boolean = false

    fun init(context: Context, playerType: PlayerType, audioSessionId: Int? = null): Boolean {
        lastInitContext = context.applicationContext
        lastPlayerType = playerType
        
        // STEP 1: Verify Audio Session Handling
        val finalSessionId = if (preferGlobalSession) {
            Log.d(TAG, "Using global session (0) due to preferGlobalSession=true")
            0
        } else {
            if (audioSessionId == null) {
                Log.w(TAG, "No audio session ID provided and preferGlobalSession=false")
                if (preferGlobalSession) {
                    Log.d(TAG, "Falling back to global session (0)")
                    0
                } else {
                    Log.e(TAG, "No valid audio session ID available")
                    return false
                }
            } else {
                Log.d(TAG, "Using provided audio session ID: $audioSessionId")
                audioSessionId
            }
        }
        
        lastAudioSessionId = finalSessionId
        release()
        val chosen = selectBackend(context, playerType, finalSessionId)
        backend = chosen
        val ok = chosen.init(context, playerType, finalSessionId)
        Log.d(TAG, "init backend=${chosen.name} ok=$ok session=$finalSessionId")
        return ok
    }

    fun setCompatibilityPreferGlobalSession(enable: Boolean) {
        preferGlobalSession = enable
        val ctx = lastInitContext ?: return
        init(ctx, lastPlayerType, lastAudioSessionId)
    }

    private fun selectBackend(context: Context, playerType: PlayerType, audioSessionId: Int?): DSPBackend {
        // Old method: always Android AudioEffects
        return AndroidAudioEffectsBackend()
    }

    fun release() {
        try {
            backend?.release()
        } catch (_: Exception) {}
        backend = null
    }

    fun enableEffect(effectName: String) {
        val e = effectName.toEffectNameOrNull() ?: return
        Log.d(TAG, "enableEffect called for: $effectName")
        backend?.enable(e)
    }

    fun disableEffect(effectName: String) {
        val e = effectName.toEffectNameOrNull() ?: return
        backend?.disable(e)
    }

    fun setParameter(effectName: String, key: String, value: Float) {
        val e = effectName.toEffectNameOrNull() ?: return
        backend?.setParameter(e, key, value)
    }

    fun setPreset(presetName: String): Boolean {
        return backend?.setPreset(presetName) == true
    }

    fun saveUserPreset(name: String, params: Map<String, Float>) {
        Presets.saveUserPreset(name, params)
    }

    fun applyCustomFilter(filterType: String, coefficients: FloatArray): Boolean {
        return backend?.applyCustomFilter(filterType, coefficients) == true
    }

    fun importSoundPack(context: Context, uri: android.net.Uri): String? {
        return backend?.importSoundPack(context, uri)
    }

    fun setPerformanceMode(mode: PerformanceMode) {
        backend?.setPerformanceMode(mode)
    }

    fun acceptAIRecommendation(params: Map<String, Float>) {
        // Simple mapping: route keys as effect.param => value
        params.forEach { (k, v) ->
            val parts = k.split('.')
            if (parts.size == 2) {
                val effect = parts[0].toEffectNameOrNull() ?: return@forEach
                backend?.setParameter(effect, parts[1], v)
            }
        }
    }

    fun getDiagnostics(context: Context): JSONObject {
        val obj = JSONObject()
        val b = backend
        obj.put("device", android.os.Build.MODEL)
        obj.put("android", android.os.Build.VERSION.SDK_INT)
        obj.put("backend", b?.name ?: "none")
        obj.put("supportedEffects", JSONArray((b?.supportedEffects() ?: emptySet()).map { it.wireName }))
        obj.put("fallbackUsed", b !is NativeDSPBackend)
        obj.put("latencies", JSONObject().apply {
            put("estimatedProcessingMs", 0) // placeholder
        })
        obj.put("audioSessionId", lastAudioSessionId ?: JSONObject.NULL)
        obj.put("playerType", lastPlayerType.name)
        obj.put("backendDiagnostics", b?.diagnostics() ?: JSONObject())
        
        // STEP 6: Test and diagnose the implementation
        Log.d(TAG, "Diagnostics: backend=${b?.name}, session=$lastAudioSessionId, supported=${b?.supportedEffects()?.contains(EffectName.bassBoost)}")
        return obj
    }
    
    // Test method for BassBoost functionality
    fun testBassBoost(): Boolean {
        return try {
            Log.d(TAG, "Testing BassBoost functionality")
            enableEffect("bassBoost")
            setParameter("bassBoost", "strength", 0.5f)
            val diagnostics = backend?.diagnostics()
            val bassBoostWorking = diagnostics?.getBoolean("bassBoost") == true
            Log.d(TAG, "BassBoost test result: $bassBoostWorking")
            bassBoostWorking
        } catch (e: Exception) {
            Log.e(TAG, "BassBoost test failed", e)
            false
        }
    }
    
    // Comprehensive test for all audio effects
    fun testAllAudioEffects(): JSONObject {
        val results = JSONObject()
        // Map common names to backend-specific diagnostic keys
        val effectsMap = mapOf(
            "bassBoost" to "bassBoost",
            "equalizer" to "eq",
            "reverb" to "rv",
            "virtualizer" to "vz",
            "compressor" to "bassBoost" // Compressor is mapped to bassBoost in backend
        )
        
        effectsMap.forEach { (effectName, diagnosticKey) ->
            try {
                Log.d(TAG, "Testing $effectName (key: $diagnosticKey)")
                enableEffect(effectName)
                setParameter(effectName, "strength", 0.3f)
                val diagnostics = backend?.diagnostics()
                val isWorking = diagnostics?.optBoolean(diagnosticKey, false) ?: false
                results.put(effectName, isWorking)
                disableEffect(effectName)
                Log.d(TAG, "$effectName test result: $isWorking")
            } catch (e: Exception) {
                Log.e(TAG, "$effectName test failed", e)
                results.put(effectName, false)
            }
        }
        
        return results
    }

    private fun String.toEffectNameOrNull(): EffectName? {
        return EffectName.values().firstOrNull { it.wireName.equals(this, ignoreCase = true) }
    }
}


