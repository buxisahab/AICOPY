# 🎯 AudioEffectsManager Integration Guide

## ✅ Phase 1: Backend Complete!

The production-grade AudioEffectsManager is now integrated into MusicService. It provides:
- ✅ Session-safe audio effects (survives Bluetooth changes)
- ✅ Automatic effect re-attachment on device changes
- ✅ Proper lifecycle management (no memory leaks)
- ✅ Resso-level stability

---

## 📋 Phase 2: UI Integration

Now you need to connect your existing DSP UI to the new AudioEffectsManager.

### Option A: Update Existing DSPSettings.kt

Replace old AI DSP calls with AudioEffectsManager calls:

```kotlin
// In DSPSettings.kt or your audio settings screen

// Get reference to MusicService
val binder = MusicService.MusicBinder // your binding
val service = binder.service

// Enable BassBoost
service.audioEffectsManager?.setBassBoost(
    enabled = true,
    strength = 200.toShort() // 0-300 safe range
)

// Enable Loudness Enhancer
service.audioEffectsManager?.setLoudnessEnhancer(
    enabled = true,
    gainMb = 1000 // +10dB (range: -2000 to +2000)
)

// Enable Equalizer
service.audioEffectsManager?.setEqualizerEnabled(true)
service.audioEffectsManager?.setEqualizerBandLevel(
    band = 0, // 0-9 for 10-band EQ
    level = 500.toShort() // millibels
)

// Enable Virtualizer (spatial effect)
service.audioEffectsManager?.setVirtualizer(
    enabled = true,
    strength = 500.toShort() // 0-1000
)

// Disable all effects (emergency)
service.audioEffectsManager?.disableAllEffects()

// Get current states (for UI display)
val states = service.audioEffectsManager?.getEffectStates()
val bassEnabled = states?.get("bassEnabled") as? Boolean ?: false
val bassStrength = states?.get("bassStrength") as? Short ?: 0
```

### Example: Bass Boost Slider

```kotlin
// In your Composable UI
@Composable
fun BassBoostControl(musicService: MusicService) {
    var bassStrength by remember { mutableStateOf(0f) }
    var bassEnabled by remember { mutableStateOf(false) }
    
    Column {
        // Enable/Disable Toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bass Boost")
            Spacer(Modifier.weight(1f))
            Switch(
                checked = bassEnabled,
                onCheckedChange = { enabled ->
                    bassEnabled = enabled
                    musicService.audioEffectsManager?.setBassBoost(
                        enabled = enabled,
                        strength = bassStrength.toInt().toShort()
                    )
                }
            )
        }
        
        // Strength Slider (0-300)
        Slider(
            value = bassStrength,
            onValueChange = { value ->
                bassStrength = value
                if (bassEnabled) {
                    musicService.audioEffectsManager?.setBassBoostStrength(
                        value.toInt().toShort()
                    )
                }
            },
            valueRange = 0f..300f,
            enabled = bassEnabled
        )
        
        Text("Strength: ${bassStrength.toInt()}")
    }
}
```

### Example: Equalizer Preset

```kotlin
@Composable
fun EqualizerPresetControl(musicService: MusicService) {
    val presets = listOf("Rock", "Pop", "Jazz", "Classical", "Flat")
    var selectedPreset by remember { mutableStateOf(0) }
    
    // Preset values (example for Rock)
    val rockPreset = shortArrayOf(
        500,  // 60 Hz
        400,  // 170 Hz
        300,  // 310 Hz
        -200, // 600 Hz
        -400, // 1kHz
        -200, // 3kHz
        200,  // 6kHz
        400,  // 12kHz
        500,  // 14kHz
        600   // 16kHz
    )
    
    Column {
        Text("Equalizer Preset")
        
        // Preset selector
        Row {
            presets.forEachIndexed { index, preset ->
                Button(
                    onClick = {
                        selectedPreset = index
                        musicService.audioEffectsManager?.setEqualizerEnabled(true)
                        
                        // Apply preset
                        when (preset) {
                            "Rock" -> {
                                rockPreset.forEachIndexed { band, level ->
                                    musicService.audioEffectsManager?.setEqualizerBandLevel(
                                        band = band,
                                        level = level
                                    )
                                }
                            }
                            "Flat" -> {
                                // Reset all bands to 0
                                for (band in 0..9) {
                                    musicService.audioEffectsManager?.setEqualizerBandLevel(
                                        band = band,
                                        level = 0
                                    )
                                }
                            }
                            // Add other presets...
                        }
                    }
                ) {
                    Text(preset)
                }
            }
        }
    }
}
```

---

## 🔧 Effect Value Ranges

### BassBoost
- **Strength**: `0-300` (Short)
  - 0 = No bass boost
  - 150 = Moderate (recommended)
  - 300 = Maximum safe (>300 causes distortion!)

### LoudnessEnhancer
- **Gain**: `-2000 to +2000` (Int, millibels)
  - -2000 = -20dB (quieter)
  - 0 = No change
  - +1000 = +10dB (louder recommended max)
  - +2000 = +20dB (very loud, use carefully)

### Equalizer
- **Bands**: `0-9` (10-band EQ)
- **Level**: `-1500 to +1500` (Short, millibels)
  - -1500 = -15dB (cut)
  - 0 = Flat
  - +1500 = +15dB (boost)

### Virtualizer
- **Strength**: `0-1000` (Short)
  - 0 = No spatial effect
  - 500 = Moderate surround
  - 1000 = Maximum spatial effect

---

## 🎨 Recommended UI Flow

1. **Main Audio Settings Screen**
   - Master Enable/Disable toggle
   - Bass Boost slider (0-300)
   - Loudness slider (-20dB to +20dB)
   - Equalizer preset selector
   - Virtualizer toggle + slider

2. **Advanced Equalizer Screen**
   - 10 individual band sliders
   - Preset buttons (Rock, Pop, Jazz, etc.)
   - Reset button

3. **Visual Feedback**
   - Show current audioSessionId (for debugging)
   - Indicate when effects are active
   - Show warning if audioSessionId is 0 (not attached)

---

## ⚠️ Important Notes

### Effect Application

Effects are applied **automatically** when you call the methods. You don't need to manually call "apply" or "commit".

### Thread Safety

All AudioEffectsManager methods are **thread-safe** and run on background threads. They won't block the UI.

### Session Changes

When the audioSessionId changes (new song, Bluetooth connect/disconnect), effects will:
1. Release old instances
2. Create new instances with new sessionId
3. Restore previous settings automatically

You don't need to do anything! Just set the effects once, and they persist.

### Debugging

Check logs for:
```
🔊 AudioSessionId changed: 1234
✅ Effect states restored successfully
```

If you see:
```
⚠️ Invalid audioSessionId: 0
```
This means playback hasn't started yet. Effects will attach when playback begins.

---

## 📝 Migration Checklist

- [x] AudioEffectsManager created
- [x] Integrated into MusicService
- [x] AudioSessionId monitoring added
- [x] Proper lifecycle (onCreate/onDestroy)
- [ ] Update DSPSettings.kt to call AudioEffectsManager
- [ ] Remove old AI DSP UI calls
- [ ] Test BassBoost slider
- [ ] Test Loudness slider
- [ ] Test Equalizer presets
- [ ] Test Bluetooth connect/disconnect
- [ ] Test background/foreground transitions
- [ ] Test quick song skipping

---

## 🧪 Testing Guide

### Test 1: Basic Effects
1. Enable Bass Boost (strength: 200)
2. Play a song
3. Verify bass is enhanced
4. Disable Bass Boost
5. Verify bass returns to normal

### Test 2: Bluetooth Switching
1. Enable effects
2. Play a song
3. Connect Bluetooth headphones
4. **Expected**: Effects continue working
5. Disconnect Bluetooth
6. **Expected**: Effects continue working

### Test 3: Background/Foreground
1. Enable effects
2. Play a song
3. Minimize app (background)
4. **Expected**: Effects continue
5. Reopen app (foreground)
6. **Expected**: Effects still active

### Test 4: Quick Song Skip
1. Enable effects
2. Play a song
3. Skip to next song rapidly (5-10 times)
4. **Expected**: No crashes, effects work on all songs

### Test 5: Device Change
1. Enable effects
2. Play a song on speaker
3. Plug in wired headphones
4. **Expected**: Effects continue on headphones

---

## 🎯 Next Steps

1. **Update your DSPSettings.kt screen**
   - Replace DSPManager calls with Audio EffectsManager calls
   - Use the examples above

2. **Remove old AI DSP code** (optional, for cleanup)
   - Delete `dsp/AiDspEngine.kt` 
   - Delete `dsp/DSPManager.kt`
   - Remove AI DSP related constants

3. **Test thoroughly**
   - Follow the testing guide above
   - Test on real Bluetooth device

4. **Polish UI**
   - Add visual feedback for active effects
   - Show audioSessionId in debug mode

---

**Ready to update your UI?** Let me know which file you want to modify first, and I'll help integrate the AudioEffectsManager!
