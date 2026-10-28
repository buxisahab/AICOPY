# Audio Effects System Documentation

## Overview

The Audio Effects system in Buxi Music provides advanced audio processing capabilities including bass boost, 3D virtualization, equalization, reverb, loudness enhancement, and environmental effects. This document describes the current implementation as of the latest version.

---

## Architecture

### Core Component: `AudioEffectsManager`

**Location:** `app/src/main/java/com/buxi/music/audio/AudioEffectsManager.kt`

The `AudioEffectsManager` class is the central component that manages all audio effects in the application. It handles:
- Effect initialization and lifecycle management
- Audio session management
- Effect persistence across playback sessions
- OEM audio engine detection and compatibility
- Crash logging and recovery

### Key Dependencies

```kotlin
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.media.audiofx.EnvironmentalReverb
```

---

## Supported Audio Effects

### 1. **Bass Boost**
- **Type:** `BassBoost`
- **Purpose:** Enhances low-frequency audio content
- **Strength:** Maximum (1000/1000)
- **Control:** On/Off toggle with strength parameter

### 2. **Virtualizer (3D Audio)**
- **Type:** `Virtualizer`
- **Purpose:** Creates spatial audio effect for headphone listening
- **Strength:** Maximum (1000/1000) when enabled
- **Control:** On/Off toggle
- **Note:** Deprecated in newer Android versions but still functional

### 3. **Equalizer**
- **Type:** `Equalizer`
- **Purpose:** Frequency-based audio adjustment
- **Bands:** 3-band control (Bass, Mid, Treble)
- **Range:** -50 to +50 (mapped to -1500dB to +1500dB)
- **Control:** Individual sliders for each frequency band

### 4. **Preset Reverb**
- **Type:** `PresetReverb`
- **Purpose:** Adds reverb effect to audio
- **Presets:** Standard Android reverb presets
- **Control:** On/Off with preset selection

### 5. **Loudness Enhancer**
- **Type:** `LoudnessEnhancer`
- **Purpose:** Increases perceived loudness without distortion
- **Range:** 0 to 100 (mapped to 0-1000 millibels)
- **Control:** Gain slider

### 6. **Environmental Reverb (Echo)**
- **Type:** `EnvironmentalReverb`
- **Purpose:** Advanced reverb with decay time control
- **Range:** 0 to 100 (mapped to 0-5000ms decay time)
- **Control:** Strength slider

---

## Audio Session Management

### Session ID Handling

The system uses a sophisticated multi-session approach to ensure compatibility across different devices:

```kotlin
fun setAudioSessionId(sessionId: Int) {
    val sessionIds = listOf(sessionId, 0, 1, 2, 3, 4, 5)
    var effectsInitialized = false
    
    for (testSessionId in sessionIds) {
        try {
            this.audioSessionId = testSessionId
            initializeEffects()
            if (verifyEffectsWorking()) {
                effectsInitialized = true
                break
            }
        } catch (e: Exception) {
            continue
        }
    }
}
```

**Why Multiple Session IDs?**
- Different Android devices and OEMs use different audio session ID schemes
- Session ID 0 is often a global session
- IDs 1-5 are fallback options for specific device configurations
- The system tries each until it finds one that works

### Session Verification

```kotlin
private fun verifyEffectsWorking(): Boolean {
    return try {
        bassBoostEffect?.let { effect ->
            effect.strengthSupported
        } ?: false
    } catch (e: Exception) {
        false
    }
}
```

---

## OEM Audio Engine Detection

### Supported OEM Engines

The system detects and adapts to the following OEM audio engines:

| OEM | Package Name | Audio System |
|-----|--------------|--------------|
| Dolby | `com.dolby.dax3`, `com.dolby.dax2` | Dolby Atmos/Audio |
| Samsung | `com.samsung.android.soundalive`, `com.samsung.android.soundalive_plus` | SoundAlive |
| Dirac | `com.dirac.dirac`, `com.dirac.dirac.one` | Dirac HD Sound |
| Sony | `com.sony.soundenhancement` | ClearAudio+ |
| LG | `com.lg.sound` | LG Sound |
| Huawei | `com.huawei.soundplus` | Histen |
| Xiaomi | `com.xiaomi.sound` | Mi Sound |
| OnePlus | `com.oneplus.sound` | Dolby Atmos |

### OEM Override Handling

When an OEM audio engine is detected:

```kotlin
if (isOEMOverrideDetected) {
    scope.launch(Dispatchers.IO) {
        delay(500) // Wait for OEM effects to settle
        reattachAllEffects()
    }
}
```

**Why This Matters:**
- OEM audio engines can override or conflict with standard Android audio effects
- The system waits for OEM effects to initialize before applying custom effects
- Effects are reattached periodically to maintain control

---

## Error Handling & Recovery

### Graceful Degradation

The current implementation uses a **fail-safe** approach:

```kotlin
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
            } catch (e: Exception) {
                Log.e("AudioEffectsManager", "Failed to configure Reverb", e)
            }
        }
    } catch (e: Exception) {
        Log.e("AudioEffectsManager", "Initialization error: Reverb not supported", e)
        null
    }
}
```

**Key Points:**
- If an effect fails to initialize, it returns `null` instead of crashing
- The app continues to function with other working effects
- Errors are logged for debugging but don't affect user experience

### Common Error Codes

| Error Code | Meaning | Solution |
|------------|---------|----------|
| -3 (`BAD_VALUE`) | Effect not supported on device | Gracefully skip effect |
| -38 (`DEAD_OBJECT`) | Audio session terminated | Reinitialize with new session |
| -1 (`ERROR`) | Generic initialization failure | Try alternative session IDs |

### Crash Logging System

```kotlin
private fun logCrash(e: Throwable) {
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
}
```

**Features:**
- Automatic crash log creation in `files/crash_logs/`
- Detailed context including audio session state
- Shareable logs for debugging
- Crash loop prevention

---

## Effect Persistence

### Automatic Reapplication

The system runs a background coroutine to maintain effects:

```kotlin
private fun startEffectPersistence() {
    scope.launch(Dispatchers.IO) {
        while (isActive) {
            try {
                reapplyAllEffects()
                
                if (System.currentTimeMillis() - lastSessionChangeTime > 10000) {
                    reattachAllEffects()
                }
                
                delay(3000) // Check every 3 seconds
            } catch (e: Exception) {
                Log.e("AudioEffectsManager", "Error in effect persistence", e)
                delay(5000) // Wait longer on error
            }
        }
    }
}
```

**Why This Is Needed:**
- Some devices reset effects when switching tracks
- OEM audio engines can override custom effects
- Bluetooth/wired headphone changes can reset audio sessions
- Background apps may interfere with audio effects

### Settings Persistence

All effect settings are saved to SharedPreferences:

```kotlin
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
```

Settings are restored on app restart.

---

## Public API

### Setting Bass Boost

```kotlin
fun setBassBoost(enabled: Boolean, strength: Float)
```

**Parameters:**
- `enabled`: Whether bass boost is active
- `strength`: Boost strength (0-100, currently unused as max strength is always applied)

### Setting 3D Audio

```kotlin
fun set3DEnabled(enabled: Boolean)
```

**Parameters:**
- `enabled`: Whether 3D virtualization is active

### Setting Equalizer

```kotlin
fun setEqualizer(bass: Float, mid: Float, treble: Float)
```

**Parameters:**
- `bass`: Bass level (0-100, mapped to -1500dB to +1500dB)
- `mid`: Mid-range level (0-100)
- `treble`: Treble level (0-100)

### Setting Reverb

```kotlin
fun setReverb(enabled: Boolean, preset: Int)
```

**Parameters:**
- `enabled`: Whether reverb is active
- `preset`: Reverb preset (0-6, see `PresetReverb` constants)

### Setting Loudness

```kotlin
fun setLoudnessGain(gain: Float)
```

**Parameters:**
- `gain`: Loudness gain (0-100, mapped to 0-1000 millibels)

### Setting Echo

```kotlin
fun setEchoStrength(strength: Float)
```

**Parameters:**
- `strength`: Echo decay time (0-100, mapped to 0-5000ms)

### Cleanup

```kotlin
fun release()
```

Releases all audio effects and cleans up resources.

---

## Integration with MusicService

The `AudioEffectsManager` is initialized in `MusicService`:

```kotlin
private lateinit var audioEffectsManager: AudioEffectsManager

override fun onCreate() {
    super.onCreate()
    audioEffectsManager = AudioEffectsManager(this, scope)
}

// When audio session changes
player.addListener(object : Player.Listener {
    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        audioEffectsManager.setAudioSessionId(audioSessionId)
    }
})
```

---

## Known Limitations

### 1. **Virtualizer Deprecation**
- `Virtualizer` is deprecated in Android API 33+
- Still functional but may be removed in future Android versions
- Consider migrating to `Spatializer` API in the future

### 2. **Device-Specific Behavior**
- Some devices don't support all effects
- OEM audio engines may limit or override effects
- Effect quality varies by device hardware

### 3. **Effect Availability**
- Not all Android devices support all audio effects
- The system gracefully handles unsupported effects by skipping them

### 4. **Performance Impact**
- Multiple active effects can increase CPU usage
- Battery consumption may increase with effects enabled
- Minimal impact on modern devices

---

## Troubleshooting

### Effects Not Working

**Possible Causes:**
1. Device doesn't support the specific effect
2. OEM audio engine is overriding effects
3. Invalid audio session ID
4. Bluetooth device with its own audio processing

**Solutions:**
1. Check crash logs in `files/crash_logs/`
2. Try disabling OEM audio enhancements in device settings
3. Restart playback to reinitialize audio session
4. Disable and re-enable effects

### Effects Reset After Track Change

**Cause:** Some devices reset effects on track boundaries

**Solution:** The persistence system automatically reapplies effects every 3 seconds

### Crashes on Specific Devices

**Cause:** Unsupported audio effect or incompatible audio session

**Solution:** The current implementation prevents crashes by catching all exceptions and gracefully degrading

---

## Future Improvements

### Planned Enhancements

1. **Spatializer API Integration**
   - Replace deprecated `Virtualizer` with modern `Spatializer`
   - Support for head tracking on compatible devices

2. **Custom Equalizer Presets**
   - User-defined EQ presets
   - Genre-based automatic EQ adjustment

3. **Advanced Reverb Controls**
   - Room size adjustment
   - Diffusion control
   - Pre-delay settings

4. **Effect Profiles**
   - Save/load complete effect configurations
   - Share effect profiles with other users

5. **Visualizer Integration**
   - Real-time audio spectrum visualization
   - Waveform display

---

## Technical Notes

### Thread Safety

All effect operations run on the IO dispatcher:

```kotlin
scope.launch(Dispatchers.IO) {
    // Effect operations
}
```

### Memory Management

Effects are properly released to prevent memory leaks:

```kotlin
private fun releaseEffects() {
    bassBoostEffect?.release()
    virtualizerEffect?.release()
    equalizerEffect?.release()
    reverbEffect?.release()
    loudnessEnhancerEffect?.release()
    environmentalReverbEffect?.release()
    
    // Set to null for garbage collection
    bassBoostEffect = null
    virtualizerEffect = null
    // ... etc
}
```

### Crash Loop Prevention

```kotlin
val lastCrash = crashPrefs.getLong("lastCrashTime", 0L)
if (System.currentTimeMillis() - lastCrash < 5000) {
    Log.w("AudioEffectsManager", "Crash loop detected, clearing crash data")
    crashPrefs.edit().clear().apply()
}
```

---

## Conclusion

The current AudioEffectsManager implementation provides a robust, device-compatible audio effects system with:
- ✅ Graceful error handling
- ✅ OEM audio engine compatibility
- ✅ Automatic effect persistence
- ✅ Comprehensive crash logging
- ✅ Multi-session fallback support
- ✅ No-crash guarantee through defensive programming

The system prioritizes **stability over features**, ensuring that even if some effects fail, the app continues to function normally.
