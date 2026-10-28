package com.buxi.music.audio

import android.util.Log

/**
 * Manages presets and manual controls for [DspAudioProcessor].
 * All effect changes are session-independent — never touches Android's AudioEffect API.
 */
class SessionIndependentDspManager(private val dsp: DspAudioProcessor) {

    enum class Preset {
        NONE, MUSIC, CINEMA, VOICE, ROCK, POP, JAZZ, CLASSIC,
        ELECTRONIC, SPATIAL, NIGHT, OCTAPAD, TREBLE, VOCAL_BOOST, BRIGHT
    }

    // ── Current manual state ──────────────────────────────────────────────────
    @Volatile var activePreset   = Preset.NONE; private set
    @Volatile var bassBoostDb    = 0f;  private set
    @Volatile var deepBassDb     = 0f;  private set
    @Volatile var trebleBoostPct = 0f;  private set
    @Volatile var virtualizerPct = 0f;  private set
    @Volatile var reverbWet      = 0f;  private set
    @Volatile var reverbRoom     = 0.5f;private set
    @Volatile var echoWet        = 0f;  private set
    @Volatile var echoDelayMs    = 300f;private set
    @Volatile var echoFeedback   = 0.35f;private set
    @Volatile var masterGainDb   = 0f;  private set
    @Volatile var fourBandOn     = false;private set
    private val fourBand = FloatArray(4)

    // ── Preset definitions ── [b0,b1,b2,b3,b4,b5,b6,b7,b8,b9] in dB ─────────
    private val presetEq: Map<Preset, FloatArray> = mapOf(
        Preset.NONE        to floatArrayOf(0f,  0f,  0f,  0f,  0f,  0f,  0f,  0f,  0f, 0f),
        Preset.MUSIC       to floatArrayOf(3f,  4f,  2f,  0f, -1f, -1f,  1f,  2f,  3f, 3f),
        Preset.CINEMA      to floatArrayOf(5f,  4f,  2f,  0f, -2f, -2f,  0f,  2f,  4f, 5f),
        Preset.VOICE       to floatArrayOf(-2f,-2f,  0f,  2f,  4f,  4f,  3f,  2f,  1f,-1f),
        Preset.ROCK        to floatArrayOf(5f,  4f,  3f,  0f, -2f, -2f,  2f,  3f,  4f, 4f),
        Preset.POP         to floatArrayOf(-1f, 2f,  4f,  4f,  2f,  0f, -1f, -1f, -1f,-1f),
        Preset.JAZZ        to floatArrayOf(4f,  3f,  1f,  2f, -1f, -1f,  0f,  1f,  3f, 3f),
        Preset.CLASSIC     to floatArrayOf(4f,  3f,  2f, -1f, -2f, -2f,  0f,  3f,  4f, 5f),
        Preset.ELECTRONIC  to floatArrayOf(5f,  4f,  0f, -3f, -2f,  0f,  1f,  3f,  4f, 5f),
        Preset.SPATIAL     to floatArrayOf(3f,  2f,  0f, -1f, -2f, -2f,  0f,  2f,  4f, 5f),
        Preset.NIGHT       to floatArrayOf(6f,  5f,  3f,  0f, -2f, -2f, -1f,  0f, -1f,-2f),
        Preset.OCTAPAD     to floatArrayOf(7f,  6f,  4f,  2f,  0f, -1f, -2f,  0f,  2f, 3f),
        Preset.TREBLE      to floatArrayOf(-2f,-2f, -1f,  0f,  1f,  2f,  3f,  5f,  6f, 7f),
        Preset.VOCAL_BOOST to floatArrayOf(-3f,-2f,  1f,  4f,  5f,  5f,  4f,  2f,  0f,-1f),
        Preset.BRIGHT      to floatArrayOf(-1f, 0f,  1f,  2f,  2f,  3f,  4f,  5f,  6f, 6f),
    )

    private val presetReverb: Map<Preset, Pair<Float,Float>> = mapOf(
        Preset.CINEMA   to Pair(0.15f, 0.75f),
        Preset.SPATIAL  to Pair(0.20f, 0.85f),
        Preset.CLASSIC  to Pair(0.10f, 0.65f),
    )

    private val presetVirt: Map<Preset, Float> = mapOf(
        Preset.SPATIAL  to 0.8f,
        Preset.CINEMA   to 0.5f,
        Preset.MUSIC    to 0.2f,
        Preset.ELECTRONIC to 0.4f,
    )

    // ── Core Logic ────────────────────────────────────────────────────────────
    fun applyPreset(p: Preset) {
        activePreset = p
        val eq = presetEq[p] ?: floatArrayOf()
        (0 until 10).forEach { i -> dsp.setEqBand(i, eq.getOrElse(i){0f}) }

        val pVirt = presetVirt[p] ?: 0f
        dsp.setVirtualizer(maxOf(virtualizerPct / 100f, pVirt))

        val (pWet, pRoom) = presetReverb[p] ?: Pair(0f, 0.5f)
        dsp.setReverb(maxOf(reverbWet / 100f, pWet), if (reverbWet / 100f > pWet) reverbRoom / 100f else pRoom)

        Log.d("DspManager", "Preset → $p")
    }

    fun setBassBoost(db: Float)        { bassBoostDb = db; dsp.setBassBoost(db) }
    fun setDeepBass(on: Boolean, db: Float) {
        deepBassDb = if (on) db else 0f
        dsp.setDeepBass(deepBassDb)
    }

    fun setTrebleBoost(pct: Float) {
        trebleBoostPct = pct.coerceIn(0f, 100f)
        // Apply shelf-like boost to high frequencies (bands 8 and 9)
        val db = (trebleBoostPct / 100f) * 12f
        dsp.setEqBand(8, db)
        dsp.setEqBand(9, db * 0.8f)
    }

    fun setVirtualizer(pct: Float) {
        virtualizerPct = pct.coerceIn(0f, 100f)
        val pVirt = presetVirt[activePreset] ?: 0f
        dsp.setVirtualizer(maxOf(virtualizerPct / 100f, pVirt))
    }

    fun setReverb(wet: Float, roomSize: Float) {
        reverbWet  = wet.coerceIn(0f, 100f)
        reverbRoom = roomSize.coerceIn(0f, 100f)
        val (pWet, pRoom) = presetReverb[activePreset] ?: Pair(0f, 0.5f)
        dsp.setReverb(maxOf(reverbWet / 100f, pWet), if (reverbWet / 100f > pWet) reverbRoom / 100f else pRoom)
    }

    fun setEcho(wet: Float, delayMs: Float, feedback: Float) {
        echoWet      = wet.coerceIn(0f, 100f)
        echoDelayMs  = delayMs.coerceIn(50f, 1000f)
        echoFeedback = feedback.coerceIn(0f, 95f)
        dsp.setEcho(echoWet / 100f, echoDelayMs, echoFeedback / 100f)
    }

    fun setMasterGain(db: Float) { masterGainDb = db.coerceIn(-12f, 12f); dsp.setGain(masterGainDb) }

    fun setFourBandEnabled(on: Boolean) { fourBandOn = on; applyFourBand() }
    fun setFourBandEq(band: Int, db: Int) {
        if (band in 0..3) { fourBand[band] = db.toFloat(); applyFourBand() }
    }

    private fun applyFourBand() {
        if (!fourBandOn) {
            val eq = presetEq[activePreset] ?: floatArrayOf()
            (0..3).forEach { i -> dsp.setEqBand(i, eq.getOrElse(i){0f}) }
        } else {
            val bandMap = intArrayOf(0, 2, 6, 9)
            for (i in 0..3) dsp.setEqBand(bandMap[i], fourBand[i])
        }
    }

    fun resetAll() {
        dsp.resetEffects()
        activePreset = Preset.NONE
        bassBoostDb=0f; deepBassDb=0f; trebleBoostPct=0f
        virtualizerPct=0f; reverbWet=0f; reverbRoom=50f
        echoWet=0f; echoDelayMs=300f; echoFeedback=35f
        masterGainDb=0f; fourBandOn=false; fourBand.fill(0f)
        Log.d("DspManager", "All effects reset")
    }
}
