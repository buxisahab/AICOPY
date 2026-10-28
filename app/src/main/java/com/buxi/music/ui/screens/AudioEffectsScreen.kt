package com.buxi.music.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.buxi.music.LocalPlayerConnection
import com.buxi.music.R
import com.buxi.music.audio.SessionIndependentDspManager
import com.buxi.music.utils.SoundEffectsManager
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
// Preset chip metadata (same as AudioEffectsDialog)
// ─────────────────────────────────────────────────────────────────────────────
private data class EffectPreset(
    val preset: SessionIndependentDspManager.Preset,
    val label: String
)

private val EFFECT_PRESETS = listOf(
    EffectPreset(SessionIndependentDspManager.Preset.NONE,        "Flat"),
    EffectPreset(SessionIndependentDspManager.Preset.MUSIC,       "Music"),
    EffectPreset(SessionIndependentDspManager.Preset.CINEMA,      "Cinema"),
    EffectPreset(SessionIndependentDspManager.Preset.VOICE,       "Voice"),
    EffectPreset(SessionIndependentDspManager.Preset.ROCK,        "Rock"),
    EffectPreset(SessionIndependentDspManager.Preset.POP,         "Pop"),
    EffectPreset(SessionIndependentDspManager.Preset.JAZZ,        "Jazz"),
    EffectPreset(SessionIndependentDspManager.Preset.CLASSIC,     "Classic"),
    EffectPreset(SessionIndependentDspManager.Preset.ELECTRONIC,  "EDM"),
    EffectPreset(SessionIndependentDspManager.Preset.SPATIAL,     "Spatial"),
    EffectPreset(SessionIndependentDspManager.Preset.NIGHT,       "Night"),
    EffectPreset(SessionIndependentDspManager.Preset.TREBLE,      "Treble"),
    EffectPreset(SessionIndependentDspManager.Preset.VOCAL_BOOST, "Vocal+"),
    EffectPreset(SessionIndependentDspManager.Preset.BRIGHT,      "Bright"),
)

// ─────────────────────────────────────────────────────────────────────────────
// Shared preferences key
// ─────────────────────────────────────────────────────────────────────────────
private const val DSP_PREFS = "dsp_v3"

private val accentRed = Color(0xFFE5223B)
private val accentPink = Color(0xFFD13080)

// ─────────────────────────────────────────────────────────────────────────────
// AudioEffectsScreen  (full-screen, reference-image accurate)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AudioEffectsScreen(onClose: () -> Unit) {
    val context     = LocalContext.current
    val playerConn  = LocalPlayerConnection.current
    val dsp         = playerConn?.service?.getDspManager()
    val metadata    by playerConn?.mediaMetadata?.collectAsState()
        ?: remember { mutableStateOf(null) }

    val sp  = remember { context.getSharedPreferences(DSP_PREFS, Context.MODE_PRIVATE) }
    val sfx = remember { SoundEffectsManager(context) }

    // ── Persisted DSP state ───────────────────────────────────────────────────
    fun spStr(k: String, d: String) = sp.getString(k, d) ?: d
    fun spF  (k: String, d: Float)  = sp.getFloat(k, d)

    var preset by rememberSaveable {
        mutableStateOf(
            runCatching {
                SessionIndependentDspManager.Preset.valueOf(spStr("preset", "NONE"))
            }.getOrDefault(SessionIndependentDspManager.Preset.NONE)
        )
    }
    var bassDb     by rememberSaveable { mutableStateOf(spF("bass",  0f)) }
    var deepBassDb by rememberSaveable { mutableStateOf(spF("deep",  0f)) }
    var treblePct  by rememberSaveable { mutableStateOf(spF("treble",0f)) }
    var virtPct    by rememberSaveable { mutableStateOf(spF("virt",  0f)) }
    var masterDb   by rememberSaveable { mutableStateOf(spF("master",0f)) }
    var reverbWet  by rememberSaveable { mutableStateOf(spF("rv_wet",0f)) }
    var reverbRoom by rememberSaveable { mutableStateOf(spF("rv_room",50f)) }
    var echoWet    by rememberSaveable { mutableStateOf(spF("ec_wet",0f)) }
    var echoDelay  by rememberSaveable { mutableStateOf(spF("ec_delay",300f)) }
    var echoFb     by rememberSaveable { mutableStateOf(spF("ec_fb",35f)) }
    var rainPct    by rememberSaveable { mutableStateOf(spF("rain",  0f)) }

    // Visual-only toggles (Musical Flashlight / Vibrate / Color Strobe)
    var flashlightOn  by rememberSaveable { mutableStateOf(false) }
    var vibrateOn     by rememberSaveable { mutableStateOf(false) }
    var strobeOn      by rememberSaveable { mutableStateOf(false) }

    fun save() = sp.edit()
        .putString("preset", preset.name)
        .putFloat("bass", bassDb)        .putFloat("deep", deepBassDb)
        .putFloat("treble", treblePct)   .putFloat("virt", virtPct)
        .putFloat("master", masterDb)
        .putFloat("rv_wet", reverbWet)   .putFloat("rv_room", reverbRoom)
        .putFloat("ec_wet", echoWet)     .putFloat("ec_delay", echoDelay) .putFloat("ec_fb", echoFb)
        .putFloat("rain", rainPct)
        .apply()

    // ── Apply effects on change ───────────────────────────────────────────────
    LaunchedEffect(preset)                   { dsp?.applyPreset(preset); save() }
    LaunchedEffect(bassDb)                   { dsp?.setBassBoost(bassDb); save() }
    LaunchedEffect(deepBassDb)               { dsp?.setDeepBass(deepBassDb > 0f, deepBassDb); save() }
    LaunchedEffect(treblePct)                { dsp?.setTrebleBoost(treblePct); save() }
    LaunchedEffect(virtPct)                  { dsp?.setVirtualizer(virtPct); save() }
    LaunchedEffect(masterDb)                 { dsp?.setMasterGain(masterDb); save() }
    LaunchedEffect(reverbWet, reverbRoom)    { dsp?.setReverb(reverbWet, reverbRoom); save() }
    LaunchedEffect(echoWet, echoDelay, echoFb) { dsp?.setEcho(echoWet, echoDelay, echoFb); save() }
    LaunchedEffect(rainPct)                  { sfx.setRainLevel(rainPct / 100f); save() }

    LaunchedEffect(Unit) {
        dsp?.applyPreset(preset)
        dsp?.setBassBoost(bassDb); dsp?.setDeepBass(deepBassDb > 0f, deepBassDb)
        dsp?.setTrebleBoost(treblePct)
        dsp?.setVirtualizer(virtPct); dsp?.setMasterGain(masterDb)
        dsp?.setReverb(reverbWet, reverbRoom)
        dsp?.setEcho(echoWet, echoDelay, echoFb)
        sfx.setRainLevel(rainPct / 100f)
    }

    fun resetAll() {
        preset = SessionIndependentDspManager.Preset.NONE
        bassDb=0f; deepBassDb=0f; treblePct=0f; virtPct=0f; masterDb=0f
        reverbWet=0f; reverbRoom=50f; echoWet=0f; echoDelay=300f; echoFb=35f; rainPct=0f
        dsp?.resetAll(); sfx.setRainLevel(0f); save()
    }

    // ── Haptic helper ─────────────────────────────────────────────────────────
    fun hapticTick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(18, 80))
        }
    }

    // ── Background colours ────────────────────────────────────────────────────
    val bgDark    = Color(0xFF0C0C1A).copy(alpha = 0.95f)
    val bgMid     = Color(0xFF14102B).copy(alpha = 0.95f)

    // ── UI ────────────────────────────────────────────────────────────────────
    // Root container with premium glassmorphism background
    Surface(
        modifier = Modifier.fillMaxSize().zIndex(100f),
        color = Color(0xFF100F14) // Match ThemeDark
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0C0C1A),
                            Color(0xFF14102B),
                            Color(0xFF100A20)
                        )
                    )
                )
        ) {
            // Background animated blobs for premium feel
            AmbientGlowBlobs()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // ── TOP BAR ──────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onClose() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Text(
                        "Audio Effects",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.weight(1f))
                    // Reset button
                    IconButton(onClick = { resetAll() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset", tint = Color.White.copy(0.7f))
                    }
                }

                // ── SCROLLABLE BODY ───────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                // ── 1. SONG ARTWORK PILL + BASS SLIDER ────────────────────────
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SongArtworkPill(
                        thumbnailUrl = metadata?.thumbnailUrl,
                        title        = metadata?.title,
                        artist       = metadata?.artists?.joinToString { it.name }
                    )

                    // Hugging Bass Slider
                    Box(
                        modifier = Modifier
                            .padding(top = 220.dp) // Aligns slider with bottom curve center
                    ) {
                        BassArcSlider(
                            value    = bassDb,
                            maxValue = 15f,
                            label    = "Bass Boost",
                            onChange = { newVal ->
                                bassDb = newVal
                                hapticTick()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(130.dp)) // Buffer for the downward arc

                // ── 3. PRESET CHIPS ───────────────────────────────────────────
                EffectSectionCard(title = "Presets") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(EFFECT_PRESETS) { info ->
                            EffectPresetChip(info, selected = preset == info.preset) {
                                preset = info.preset
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── 4. EQUALIZER CARD ─────────────────────────────────────────
                EffectSectionCard(title = "Equalizer") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SliderRowWithLabel("Bass", bassDb, 0f..15f, { "+${it.toInt()} dB" }) {
                            bassDb = it; hapticTick()
                        }
                        SliderRowWithLabel("Deep Bass", deepBassDb, 0f..12f, { "+${it.toInt()} dB" }) {
                            deepBassDb = it
                        }
                        SliderRowWithLabel("Treble", treblePct, 0f..100f, { "${it.toInt()}%" }) {
                            treblePct = it
                        }
                        SliderRowWithLabel("Master Gain", masterDb, -12f..12f, {
                            "${if (it >= 0) "+" else ""}${it.toInt()} dB"
                        }) { masterDb = it }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── 5. SPATIAL EFFECTS ────────────────────────────────────────
                EffectSectionCard(title = "3D Virtualizer / Surround") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SliderRowWithLabel("3D Virtualizer", virtPct, 0f..100f, { "${it.toInt()}%" }) {
                            virtPct = it
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── 6. REVERB ─────────────────────────────────────────────────
                EffectSectionCard(title = "Reverb") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SliderRowWithLabel("Reverb Wet", reverbWet, 0f..100f, {
                            if (it == 0f) "Off" else "${it.toInt()}%"
                        }) { reverbWet = it }
                        if (reverbWet > 0f) {
                            SliderRowWithLabel("Room Size", reverbRoom, 0f..100f, { "${it.toInt()}%" }) {
                                reverbRoom = it
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── 7. ECHO / DELAY ───────────────────────────────────────────
                EffectSectionCard(title = "Echo / Delay") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SliderRowWithLabel("Echo Wet", echoWet, 0f..100f, {
                            if (it == 0f) "Off" else "${it.toInt()}%"
                        }) { echoWet = it }
                        if (echoWet > 0f) {
                            SliderRowWithLabel("Delay", echoDelay, 50f..800f, { "${it.toInt()} ms" }) {
                                echoDelay = it
                            }
                            SliderRowWithLabel("Feedback", echoFb, 0f..85f, { "${it.toInt()}%" }) {
                                echoFb = it
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── 8. LOUDNESS ENHANCER (maps to masterDb in DSP) ───────────
                EffectSectionCard(title = "Loudness Enhancer") {
                    SliderRowWithLabel("Loudness Gain", masterDb, -12f..12f, {
                        "${if (it >= 0) "+" else ""}${it.toInt()} dB"
                    }) { masterDb = it }
                }

                Spacer(Modifier.height(12.dp))

                // ── 9. RAIN AMBIENT ───────────────────────────────────────────
                EffectSectionCard(title = "Rain Ambient") {
                    SliderRowWithLabel("Rain Intensity", rainPct, 0f..100f, {
                        if (it == 0f) "Off" else "${it.toInt()}%"
                    }) { rainPct = it }
                }

                Spacer(Modifier.height(20.dp))

                // ── 10. ICON TOGGLE ROW (Musical Flashlight / Vibrate / Strobe)
                Text(
                    "Visual Effects",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    EffectIconToggle(
                        iconRes       = R.drawable.ic_musical_flashlight,
                        label         = "Musical\nFlashlight",
                        checked       = flashlightOn,
                        accentColor   = accentRed,
                        onToggle      = { flashlightOn = it }
                    )
                    EffectIconToggle(
                        iconRes       = R.drawable.ic_vibrate,
                        label         = "Vibrate",
                        checked       = vibrateOn,
                        accentColor   = accentPink,
                        onToggle      = { vibrateOn = it }
                    )
                    EffectIconToggle(
                        iconRes       = R.drawable.ic_color_strobe,
                        label         = "Color\nStrobe",
                        checked       = strobeOn,
                        accentColor   = accentRed,
                        onToggle      = { strobeOn = it }
                    )
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}
}

// ─────────────────────────────────────────────────────────────────────────────
// Song Artwork Pill  — red/pink gradient container with artwork
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SongArtworkPill(
    thumbnailUrl: String?,
    title:  String?,
    artist: String?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pill_anim")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier
            .padding(horizontal = 40.dp)
            .fillMaxWidth()
    ) {
        // Red gradient pill behind artwork
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(310.dp)
                .graphicsLayer {
                    shadowElevation = 50f
                    shape = RoundedCornerShape(
                        bottomStart = 110.dp, bottomEnd = 110.dp,
                        topStart = 20.dp, topEnd = 20.dp
                    )
                    clip = true
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE5223B),
                            Color(0xFFD13080),
                            Color(0xFFAA1558).copy(alpha = glowAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(
                        bottomStart = 110.dp, bottomEnd = 110.dp,
                        topStart = 20.dp, topEnd = 20.dp
                    )
                )
        ) {
            // Artwork fills the pill
            AsyncImage(
                model            = thumbnailUrl,
                contentDescription = "Album Art",
                contentScale     = ContentScale.Crop,
                modifier         = Modifier.fillMaxSize()
            )

            // Bottom fade so it blends into bg
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xFF0C0C1A).copy(alpha = 0.8f))
                        )
                    )
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Half-Circular Bass Arc Slider
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BassArcSlider(
    value:    Float,
    maxValue: Float,
    label:    String,
    onChange: (Float) -> Unit
) {
    val fraction   = (value / maxValue).coerceIn(0f, 1f)

    // Glow pulse when active
    val infiniteTransition = rememberInfiniteTransition(label = "bass_glow")
    val thumbGlow by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "thumb_g"
    )

    val trackColor  = Color.White.copy(alpha = 0.15f)
    val activeColor = Color(0xFFE5223B)
    val thumbColor  = Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(220.dp) // Match thumbnail width
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(220.dp)
                .height(130.dp)
        ) {
            // ── Inverted Arc canvas (Downward) ───────────────────────────────
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val width  = size.width.toFloat()
                            val cx = width / 2f
                            val cy = 0f // Center at top to hug thumbnail bottom
                            val dx = change.position.x - cx
                            val dy = change.position.y - cy  
                            
                            // angle in arc (0 is right, 90 is bottom, 180 is left)
                            var angle = atan2(dy, dx).toDegrees()
                            angle = angle.coerceIn(0f, 180f)
                            val newFrac = 1f - (angle / 180f) // 180 -> 0 (min), 0 -> 1 (max)
                            onChange((newFrac * maxValue).coerceIn(0f, maxValue))
                        }
                    }
            ) {
                val w  = size.width
                val cx = w / 2f
                val cy = 0f  // Top center
                val r  = w / 2f // Radius 110dp
                val strokeW  = 6.dp.toPx()
                val sweepFrac = fraction

                // Background arc (0 to 180 clockwise = right -> bottom -> left)
                drawArc(
                    color      = trackColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter  = false,
                    topLeft    = Offset(cx - r, cy - r),
                    size       = Size(r * 2, r * 2),
                    style      = Stroke(width = strokeW, cap = StrokeCap.Round)
                )

                // Active arc (From left toward right)
                if (sweepFrac > 0f) {
                    drawArc(
                        brush      = Brush.linearGradient(
                            listOf(Color(0xFFE5223B), Color(0xFFD13080))
                        ),
                        startAngle = 180f,
                        sweepAngle = -180f * sweepFrac,
                        useCenter  = false,
                        topLeft    = Offset(cx - r, cy - r),
                        size       = Size(r * 2, r * 2),
                        style      = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // Thumb dot
                val thumbAngleRad = (180f - 180f * sweepFrac) * (PI / 180f)
                val thumbX = cx + r * kotlin.math.cos(thumbAngleRad).toFloat()
                val thumbY = cy + r * kotlin.math.sin(thumbAngleRad).toFloat()

                // Thumb glow
                if (value > 0f) {
                    drawCircle(
                        color  = activeColor.copy(alpha = 0.35f),
                        radius = thumbGlow,
                        center = Offset(thumbX, thumbY)
                    )
                }
                drawCircle(
                    color  = thumbColor,
                    radius = 8.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )
                drawCircle(
                    color  = activeColor,
                    radius = 4.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )
            }
        } // Missing brace added

        Spacer(Modifier.height(8.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text  = "+${value.toInt()} dB",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(0.6f)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Effect Section Card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EffectSectionCard(
    title:   String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape       = RoundedCornerShape(20.dp),
        color       = Color(0xFF1A1530),
        tonalElevation = 0.dp,
        modifier    = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFFE5223B)
            )
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Slider row with min/max/value label
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SliderRowWithLabel(
    name:     String,
    value:    Float,
    range:    ClosedFloatingPointRange<Float>,
    labelFn:  (Float) -> String,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(0.75f))
            Text(labelFn(value), style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE5223B), fontWeight = FontWeight.Bold)
        }
        Slider(
            value          = value,
            onValueChange  = onChange,
            valueRange     = range,
            modifier       = Modifier.fillMaxWidth(),
            colors         = SliderDefaults.colors(
                thumbColor       = Color.White,
                activeTrackColor = Color(0xFFE5223B),
                inactiveTrackColor = Color.White.copy(0.12f)
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Preset Chip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EffectPresetChip(
    info:     EffectPreset,
    selected: Boolean,
    onClick:  () -> Unit
) {
    val bg by animateColorAsState(
        if (selected) Color(0xFFE5223B) else Color(0xFF252040),
        animationSpec = tween(200), label = "chip_bg"
    )
    val fg = if (selected) Color.White else Color.White.copy(0.65f)
    Surface(
        onClick = onClick,
        shape   = RoundedCornerShape(50),
        color   = bg,
        modifier = Modifier.height(34.dp)
    ) {
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                info.label,
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color      = fg
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Icon Toggle Button (Musical Flashlight / Vibrate / Color Strobe)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EffectIconToggle(
    iconRes:    Int,
    label:      String,
    checked:    Boolean,
    accentColor: Color,
    onToggle:   (Boolean) -> Unit
) {
    val scale by animateFloatAsState(
        if (checked) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "icon_scale"
    )
    val bgColor by animateColorAsState(
        if (checked) accentColor.copy(0.22f) else Color(0xFF1C1830),
        animationSpec = tween(200), label = "icon_bg"
    )
    val borderColor by animateColorAsState(
        if (checked) accentColor else Color.White.copy(0.1f),
        animationSpec = tween(200), label = "icon_border"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .width(96.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(1.5.dp, borderColor, CircleShape)
                .clickable { onToggle(!checked) }
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = if (checked) accentColor else Color.White.copy(0.65f),
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            label,
            style     = MaterialTheme.typography.labelSmall,
            color     = if (checked) accentColor else Color.White.copy(0.55f),
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Ambient background glow blobs (decorative)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AmbientGlowBlobs() {
    val infiniteTransition = rememberInfiniteTransition(label = "blobs")
    val blob1 by infiniteTransition.animateFloat(
        0.3f, 0.55f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val blob2 by infiniteTransition.animateFloat(
        0.2f, 0.45f,
        infiniteRepeatable(tween(5500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b2"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFE5223B).copy(blob1), Color.Transparent),
                center = Offset(size.width * 0.8f, size.height * 0.15f),
                radius = 280f
            ),
            radius = 280f,
            center = Offset(size.width * 0.8f, size.height * 0.15f)
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFF6B21A8).copy(blob2), Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.55f),
                radius = 220f
            ),
            radius = 220f,
            center = Offset(size.width * 0.15f, size.height * 0.55f)
        )
    }
}

// ── Extension: Float to degrees ───────────────────────────────────────────────
private fun Float.toDegrees() = (this * 180f / PI.toFloat())
