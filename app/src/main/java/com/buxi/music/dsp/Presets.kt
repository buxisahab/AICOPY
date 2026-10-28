package com.buxi.music.dsp

object Presets {
    // effect.param -> value (0..1 normalized where applicable)
    val presets: Map<String, Map<String, Float>> = mapOf(
        "Pop" to mapOf(
            "equalizer.band_0" to 0.6f,
            "equalizer.band_1" to 0.7f,
            "equalizer.band_2" to 0.5f,
            "equalizer.band_3" to 0.6f,
            "equalizer.band_4" to 0.7f,
            "bassBoost.strength" to 0.3f
        ),
        "Rock" to mapOf(
            "equalizer.band_0" to 0.7f,
            "equalizer.band_1" to 0.6f,
            "equalizer.band_2" to 0.5f,
            "equalizer.band_3" to 0.6f,
            "equalizer.band_4" to 0.7f,
            "bassBoost.strength" to 0.5f
        ),
        "EDM" to mapOf(
            "equalizer.band_0" to 0.8f,
            "equalizer.band_1" to 0.7f,
            "equalizer.band_2" to 0.6f,
            "equalizer.band_3" to 0.7f,
            "equalizer.band_4" to 0.8f,
            "bassBoost.strength" to 0.7f,
            "virtualizer.strength" to 0.4f
        ),
        "LoFi" to mapOf(
            "equalizer.band_0" to 0.4f,
            "equalizer.band_1" to 0.5f,
            "equalizer.band_2" to 0.5f,
            "equalizer.band_3" to 0.5f,
            "equalizer.band_4" to 0.4f
        ),
        "Cinematic" to mapOf(
            "virtualizer.strength" to 0.6f,
            "reverb.amount" to 0.6f
        ),
        "Speech" to mapOf(
            "equalizer.band_0" to 0.2f,
            "equalizer.band_1" to 0.4f,
            "equalizer.band_2" to 0.7f,
            "equalizer.band_3" to 0.4f,
            "equalizer.band_4" to 0.2f
        ),
        "Podcast" to mapOf(
            "compressor.threshold" to 0.6f,
            "compressor.ratio" to 0.5f
        ),
        "Bass-Heavy" to mapOf(
            "bassBoost.strength" to 0.9f,
            "equalizer.band_0" to 0.9f,
            "equalizer.band_1" to 0.7f
        )
    )

    private val userPresets: MutableMap<String, Map<String, Float>> = mutableMapOf()

    fun saveUserPreset(name: String, params: Map<String, Float>) {
        userPresets[name] = params
    }

    fun applyPreset(backend: DSPBackend, preset: String): Boolean {
        val map = userPresets[preset] ?: presets[preset] ?: return false
        map.forEach { (k, v) ->
            val parts = k.split('.')
            if (parts.size == 2) {
                val effect = EffectName.values().firstOrNull { it.wireName == parts[0] } ?: return@forEach
                backend.setParameter(effect, parts[1], v)
            }
        }
        return true
    }
}


