package com.buxi.music.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt

/**
 * DspAudioProcessor: Mastering-Grade Audio Engine with Echo & Reverb.
 * 
 * Includes:
 * 1. Subsonic Purification (<22Hz cut)
 * 2. 10-Band EQ (TDF-II)
 * 3. Bass Boost & Deep Bass (Low Shelf)
 * 4. Stereo Virtualizer
 * 5. Simple Reverb (Room sim)
 * 6. Feedback Echo
 * 7. Mastering Limiter & Dithering
 */
@UnstableApi
class DspAudioProcessor : AudioProcessor {
    
    companion object {
        private const val TAG = "DspAudioProcessor"
        private const val EQ_BAND_COUNT = 10
        private val EQ_FREQUENCIES = floatArrayOf(
            31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
        )
        private const val EQ_Q = 0.7071067811865476f.toFloat()
        
        private const val SUBSONIC_FREQ = 22f
        private const val BASS_BOOST_FREQ = 75f
        private const val DEEP_BASS_FREQ = 42f
        
        private const val MAX_ECHO_DELAY_MS = 1000f
    }
    
    private var inputAudioFormat: AudioProcessor.AudioFormat = AudioProcessor.AudioFormat.NOT_SET
    @Volatile private var isInitialized = false
    @Volatile private var enabled = true
    
    @Volatile private var masterGain = 1.0f
    @Volatile private var bassBoostGain = 0.0f
    @Volatile private var deepBassGain = 0.0f
    private val eqBandGains = FloatArray(EQ_BAND_COUNT) { 0.0f }
    private val eqBandGainsLock = Any()
    @Volatile private var activeBandsMask = 0
    @Volatile private var bassBoostActive = false
    @Volatile private var deepBassActive = false
    @Volatile private var virtualizerStrength = 0.0f

    // Echo & Reverb Params
    @Volatile private var echoWet = 0.0f
    @Volatile private var echoDelayMs = 300f
    @Volatile private var echoFeedback = 0.35f
    @Volatile private var reverbWet = 0.0f
    @Volatile private var reverbRoomSize = 0.5f

    // Buffers for Echo/Reverb
    private var echoBuffer: Array<FloatArray>? = null
    private var echoWriteIndex = 0
    private var reverbBuffer: Array<FloatArray>? = null
    private var reverbWriteIndex = 0

    // Limiter State
    private var envelopeGain = 1.0f
    private val attackConst = 0.9999f
    private val releaseConst = 0.00035f
    private var ditherSeed = 0.0f
    
    private data class BiquadCoeffs(
        var b0: Double = 0.0, var b1: Double = 0.0, var b2: Double = 0.0,
        var a1: Double = 0.0, var a2: Double = 0.0
    )
    
    private data class BiquadState(
        var s1: Double = 0.0, var s2: Double = 0.0,
        val coeffs: BiquadCoeffs = BiquadCoeffs()
    )
    
    private var eqFilters: Array<Array<BiquadState>>? = null
    private var bassFilters: Array<BiquadState>? = null
    private var deepBassFilters: Array<BiquadState>? = null
    private var subsonicFilters: Array<BiquadState>? = null
    
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    
    override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && 
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        if (this.inputAudioFormat == inputAudioFormat) return inputAudioFormat
        this.inputAudioFormat = inputAudioFormat
        initializeFilters()
        initializeDelayBuffers()
        isInitialized = true
        return inputAudioFormat
    }
    
    override fun isActive(): Boolean = inputAudioFormat != AudioProcessor.AudioFormat.NOT_SET
    
    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!isInitialized) return
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return
        if (!enabled) {
            outputBuffer = inputBuffer.duplicate().order(ByteOrder.nativeOrder())
            inputBuffer.position(inputBuffer.limit())
            return
        }
        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else buffer.clear()
        processSamples(inputBuffer, buffer)
        buffer.flip()
        outputBuffer = buffer
    }

    override fun getOutput(): ByteBuffer {
        val output = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return output
    }
    
    override fun queueEndOfStream() { inputEnded = true }
    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER
    override fun flush() { outputBuffer = AudioProcessor.EMPTY_BUFFER; inputEnded = false; resetFilterStates() }
    override fun reset() { flush(); buffer = AudioProcessor.EMPTY_BUFFER; inputAudioFormat = AudioProcessor.AudioFormat.NOT_SET; isInitialized = false }

    private fun processSamples(input: ByteBuffer, output: ByteBuffer) {
        val isFloat = inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val channelCount = inputAudioFormat.channelCount
        val sampleRate = inputAudioFormat.sampleRate
        val count = input.remaining() / (channelCount * (if (isFloat) 4 else 2))
        
        val head = calculateHeadroom()
        val totalGain = masterGain * head
        val currentMask = activeBandsMask
        val currentBass = bassBoostActive
        val currentDeep = deepBassActive
        val currentVirt = virtualizerStrength
        
        val curEchoWet = echoWet
        val curEchoDelaySamples = (echoDelayMs * sampleRate / 1000f).toInt()
        val curEchoFb = echoFeedback
        val curReverbWet = reverbWet
        val curReverbRoom = reverbRoomSize

        for (i in 0 until count) {
            if (channelCount == 2) {
                var l = if (isFloat) input.float else input.short.toFloat() / 32768f
                var r = if (isFloat) input.float else input.short.toFloat() / 32768f

                // 1. Subsonic Purification
                l = applySubsonic(l, 0); r = applySubsonic(r, 1)

                // 2. EQ & Bass
                if (currentMask != 0) { l = applyFilteredEq(l, 0, currentMask); r = applyFilteredEq(r, 1, currentMask) }
                if (currentBass) { l = applyBassBoost(l, 0); r = applyBassBoost(r, 1) }
                if (currentDeep) { l = applyDeepBass(l, 0); r = applyDeepBass(r, 1) }

                // 3. Image Mastering (Virtualizer)
                if (currentVirt > 0) {
                    val m = (l + r) * 0.5f; val s = (l - r) * 0.5f
                    val ns = s * (1.0f + currentVirt * 0.45f)
                    l = m + ns; r = m - ns
                }

                // 4. Echo
                if (curEchoWet > 0) {
                    l = applyEcho(l, 0, curEchoDelaySamples, curEchoWet, curEchoFb)
                    r = applyEcho(r, 1, curEchoDelaySamples, curEchoWet, curEchoFb)
                }

                // 5. Reverb (Simple Room Sim)
                if (curReverbWet > 0) {
                    l = applyReverb(l, 0, curReverbWet, curReverbRoom, sampleRate)
                    r = applyReverb(r, 1, curReverbWet, curReverbRoom, sampleRate)
                }

                // 6. Gain & Limiter
                l *= totalGain; r *= totalGain
                val pk = max(kotlin.math.abs(l), kotlin.math.abs(r))
                val target = if (pk > 0.95f) 0.95f / pk else 1.0f
                if (target < envelopeGain) envelopeGain = target else envelopeGain += (target - envelopeGain) * releaseConst
                
                l *= envelopeGain; r *= envelopeGain

                // 7. Dither & Put
                if (isFloat) { output.putFloat(l); output.putFloat(r) } 
                else { output.putShort(ditherAndClamp(l)); output.putShort(ditherAndClamp(r)) }
            } else {
                // Mono fallback
                for (ch in 0 until channelCount) {
                    var s = if (isFloat) input.float else input.short.toFloat() / 32768f
                    s *= totalGain
                    if (isFloat) output.putFloat(s) else output.putShort((s.coerceIn(-1f, 1f) * 32767f).toInt().toShort())
                }
            }
        }
    }

    private fun applyEcho(s: Float, ch: Int, delaySamples: Int, wet: Float, fb: Float): Float {
        val buf = echoBuffer ?: return s
        if (ch >= buf.size) return s
        val cBuf = buf[ch]
        val readIdx = (echoWriteIndex - delaySamples + cBuf.size) % cBuf.size
        val delayed = cBuf[readIdx]
        cBuf[echoWriteIndex] = s + delayed * fb
        if (ch == buf.size - 1) echoWriteIndex = (echoWriteIndex + 1) % cBuf.size
        return s * (1f - wet) + delayed * wet
    }

    private fun applyReverb(s: Float, ch: Int, wet: Float, room: Float, sr: Int): Float {
        val buf = reverbBuffer ?: return s
        if (ch >= buf.size) return s
        val cBuf = buf[ch]
        // Very simplified room sim using multi-tap delay
        val delaySamples = (45f * room * sr / 1000f).toInt().coerceAtLeast(10)
        val readIdx = (reverbWriteIndex - delaySamples + cBuf.size) % cBuf.size
        val delayed = cBuf[readIdx]
        cBuf[reverbWriteIndex] = s + delayed * (0.4f + 0.3f * room)
        if (ch == buf.size - 1) reverbWriteIndex = (reverbWriteIndex + 1) % cBuf.size
        return s * (1f - wet) + delayed * wet
    }

    private fun calculateHeadroom(): Float {
        val mx = synchronized(eqBandGainsLock) { eqBandGains.maxOrNull() ?: 0f }
        val bst = max(mx, max(bassBoostGain, deepBassGain))
        return if (bst > 0) Math.pow(10.0, -min(24.0, bst.toDouble() * 0.55) / 20.0).toFloat() else 1.0f
    }

    private fun ditherAndClamp(x: Float): Short {
        val r = (Math.random().toFloat() * 2f - 1f) / 32768.0f
        val out = x + (r - ditherSeed); ditherSeed = r
        return (out.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
    }

    private fun applySubsonic(s: Float, ch: Int): Float = subsonicFilters?.let { if (ch < it.size) applyBiquad(s, it[ch]) else s } ?: s
    private fun applyFilteredEq(s: Float, ch: Int, mask: Int): Float {
        val filters = eqFilters ?: return s
        if (ch >= filters.size) return s
        var out = s
        for (i in 0 until EQ_BAND_COUNT) if ((mask and (1 shl i)) != 0) out = applyBiquad(out, filters[ch][i])
        return out
    }
    private fun applyBassBoost(s: Float, ch: Int): Float = bassFilters?.let { if (ch < it.size) applyBiquad(s, it[ch]) else s } ?: s
    private fun applyDeepBass(s: Float, ch: Int): Float = deepBassFilters?.let { if (ch < it.size) applyBiquad(s, it[ch]) else s } ?: s

    private fun applyBiquad(input: Float, state: BiquadState): Float {
        val cf = state.coeffs; val inD = input.toDouble()
        val out = cf.b0 * inD + state.s1
        state.s1 = cf.b1 * inD - cf.a1 * out + state.s2
        state.s2 = cf.b2 * inD - cf.a2 * out
        return out.toFloat()
    }

    private fun initializeFilters() {
        val sr = inputAudioFormat.sampleRate.toFloat()
        val count = inputAudioFormat.channelCount
        if (sr <= 0 || count <= 0) return
        subsonicFilters = Array(count) { BiquadState().apply { updateHighPassCoeffs(coeffs, SUBSONIC_FREQ, 0.707f, sr) }}
        eqFilters = Array(count) { ch -> Array(EQ_BAND_COUNT) { i -> BiquadState().apply { updatePeakingEqCoeffs(coeffs, EQ_FREQUENCIES[i], EQ_Q, getEqBandGain(i), sr) }}}
        bassFilters = Array(count) { BiquadState().apply { updateLowShelfCoeffs(coeffs, BASS_BOOST_FREQ, 0.707f, bassBoostGain, sr) }}
        deepBassFilters = Array(count) { BiquadState().apply { updateLowShelfCoeffs(coeffs, DEEP_BASS_FREQ, 0.707f, deepBassGain, sr) }}
    }

    private fun initializeDelayBuffers() {
        val sr = inputAudioFormat.sampleRate
        val count = inputAudioFormat.channelCount
        if (sr <= 0 || count <= 0) return
        echoBuffer = Array(count) { FloatArray((MAX_ECHO_DELAY_MS * sr / 1000f).toInt()) }
        reverbBuffer = Array(count) { FloatArray((100f * sr / 1000f).toInt()) } // 100ms room buffer
        echoWriteIndex = 0
        reverbWriteIndex = 0
    }

    private fun resetFilterStates() {
        subsonicFilters?.forEach { it.s1 = 0.0; it.s2 = 0.0 }
        eqFilters?.forEach { it.forEach { s -> s.s1 = 0.0; s.s2 = 0.0 }}
        bassFilters?.forEach { it.s1 = 0.0; it.s2 = 0.0 }
        deepBassFilters?.forEach { it.s1 = 0.0; it.s2 = 0.0 }
        envelopeGain = 1.0f
        echoBuffer?.forEach { it.fill(0f) }
        reverbBuffer?.forEach { it.fill(0f) }
    }

    private fun updateHighPassCoeffs(c: BiquadCoeffs, f: Float, q: Float, sr: Float) {
        val omega = 2.0 * Math.PI * f / sr; val sn = sin(omega); val cs = cos(omega)
        val alpha = sn / (2.0 * q); val a0 = 1.0 + alpha
        c.b0 = ((1.0 + cs) / 2.0) / a0; c.b1 = -(1.0 + cs) / a0; c.b2 = ((1.0 + cs) / 2.0) / a0
        c.a1 = (-2.0 * cs) / a0; c.a2 = (1.0 - alpha) / a0
    }

    private fun updatePeakingEqCoeffs(c: BiquadCoeffs, f: Float, q: Float, g: Float, sr: Float) {
        val a = Math.pow(10.0, g / 40.0); val omega = 2.0 * Math.PI * f / sr
        val sn = sin(omega); val cs = cos(omega); val alpha = sn / (2.0 * q); val a0 = 1.0 + alpha / a
        c.b0 = (1.0 + alpha * a) / a0; c.b1 = (-2.0 * cs) / a0; c.b2 = (1.0 - alpha * a) / a0
        c.a1 = (-2.0 * cs) / a0; c.a2 = (1.0 - alpha / a) / a0
    }

    private fun updateLowShelfCoeffs(c: BiquadCoeffs, f: Float, q: Float, g: Float, sr: Float) {
        val a = Math.pow(10.0, g / 40.0); val omega = 2.0 * Math.PI * f / sr
        val sn = sin(omega); val cs = cos(omega); val alpha = sn / (2.0 * q); val sa = sqrt(a)
        val a0 = (a + 1.0) + (a - 1.0) * cs + 2.0 * sa * alpha
        c.b0 = (a * ((a + 1.0) - (a - 1.0) * cs + 2.0 * sa * alpha)) / a0
        c.b1 = (2.0 * a * ((a - 1.0) - (a + 1.0) * cs)) / a0
        c.b2 = (a * ((a + 1.0) - (a - 1.0) * cs - 2.0 * sa * alpha)) / a0
        c.a1 = (-2.0 * ((a - 1.0) + (a + 1.0) * cs)) / a0
        c.a2 = ((a + 1.0) + (a - 1.0) * cs - 2.0 * sa * alpha) / a0
    }

    fun setEnabled(e: Boolean) { this.enabled = e }
    fun setGain(g: Float) { masterGain = Math.pow(10.0, g.toDouble() / 20.0).toFloat() }
    fun setEqBand(b: Int, g: Float) {
        if (b !in 0 until EQ_BAND_COUNT) return
        val clp = g.coerceIn(-15f, 15f)
        synchronized(eqBandGainsLock) { eqBandGains[b] = clp; if (clp != 0f) activeBandsMask = activeBandsMask or (1 shl b) else activeBandsMask = activeBandsMask and (1 shl b).inv() }
        val sr = inputAudioFormat.sampleRate.toFloat()
        if (sr > 0) eqFilters?.forEach { if (b < it.size) updatePeakingEqCoeffs(it[b].coeffs, EQ_FREQUENCIES[b], EQ_Q, clp, sr) }
    }
    fun setBassBoost(g: Float) {
        bassBoostGain = g.coerceIn(0f, 15f); bassBoostActive = bassBoostGain > 0.1f
        val sr = inputAudioFormat.sampleRate.toFloat(); if (sr > 0) bassFilters?.forEach { updateLowShelfCoeffs(it.coeffs, BASS_BOOST_FREQ, 0.707f, bassBoostGain, sr) }
    }
    fun setDeepBass(g: Float) {
        deepBassGain = g.coerceIn(0f, 12f); deepBassActive = deepBassGain > 0.1f
        val sr = inputAudioFormat.sampleRate.toFloat(); if (sr > 0) deepBassFilters?.forEach { updateLowShelfCoeffs(it.coeffs, DEEP_BASS_FREQ, 0.707f, deepBassGain, sr) }
    }
    fun setVirtualizer(s: Float) { virtualizerStrength = s.coerceIn(0f, 1f) }
    fun setEcho(wet: Float, delayMs: Float, feedback: Float) {
        this.echoWet = wet.coerceIn(0f, 1f)
        this.echoDelayMs = delayMs.coerceIn(50f, MAX_ECHO_DELAY_MS)
        this.echoFeedback = feedback.coerceIn(0f, 0.95f)
    }
    fun setReverb(wet: Float, room: Float) {
        this.reverbWet = wet.coerceIn(0f, 1f)
        this.reverbRoomSize = room.coerceIn(0f, 1f)
    }

    fun getEqBandGain(b: Int): Float = synchronized(eqBandGainsLock) { if (b in 0 until EQ_BAND_COUNT) eqBandGains[b] else 0f }
    fun getEqBandFrequency(b: Int): Float = if (b in 0 until EQ_FREQUENCIES.size) EQ_FREQUENCIES[b] else 0f
    fun getEqBandCount(): Int = EQ_BAND_COUNT
    
    fun resetEffects() {
        masterGain = 1.0f; bassBoostGain = 0.0f; deepBassGain = 0.0f; virtualizerStrength = 0.0f
        echoWet = 0f; reverbWet = 0f; bassBoostActive = false; deepBassActive = false; activeBandsMask = 0
        synchronized(eqBandGainsLock) { for (i in eqBandGains.indices) eqBandGains[i] = 0.0f }
        initializeFilters()
        initializeDelayBuffers()
    }
}
