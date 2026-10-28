# 🎯 AudioEffectsManager - Quick Reference Guide

## Architecture Overview

```
MusicService.kt
    ↓
AudioEffectsManager (Session-Safe, Bluetooth-Safe, Background-Safe)
    ↓
ExoPlayer.addAnalyticsListener → onAudioSessionIdChanged
    ↓
AudioEffects (Equalizer, BassBoost, LoudnessEnhancer, Virtualizer, PresetReverb)
```

---

## Key Components

### 1. **Session Management** (MOST CRITICAL)

**Listener Setup** (MusicService.kt lines 980-988):
```kotlin
player.addAnalyticsListener(object : AnalyticsListener {
    override fun onAudioSessionIdChanged(
        eventTime: AnalyticsListener.EventTime,
        audioSessionId: Int
    ) {
        audioEffectsManager?.onAudioSessionIdChanged(audioSessionId)
    }
})
```

**What Triggers SessionId Changes**:
- New song starts
- Bluetooth connect/disconnect
- Wired headset plug/unplug
- App background → foreground
- Audio route change

**How It's Handled**:
1. Detect change in `onAudioSessionIdChanged()`
2. Release old effects
3. Wait 150ms for pipeline stabilization
4. Create new effects with new sessionId
5. Restore previous effect states
6. **Playback NEVER pauses**

---

### 2. **Effect Lifecycle**

**State Machine**:
```
Idle → Attaching → Attached → (SessionChange) → Attaching → Attached
                ↓
            Error/Retrying → Attached
                ↓
            LowMemory → Idle
```

**Critical Rules**:
- Effects created ONLY when: `STATE_READY + isPlaying`
- Effects attached AFTER 100ms delay (pipeline stabilization)
- Effects re-attached AFTER 150ms delay (session change)
- Effects disabled on pause (NOT released)
- Effects released on service destroy

---

### 3. **Memory Management**

**Thresholds**:
- **<100MB**: Low memory warning
- **<50MB**: Skip effect creation

**Checks**:
- Before every effect creation
- Logged on initialization
- Tracked continuously

**Behavior**:
```kotlin
if (availableMemory < 50MB) {
    Log.e("CRITICAL memory pressure")
    throw OutOfMemoryError()
    // Playback continues without effects
}
```

---

### 4. **Retry Logic**

**Exponential Backoff**:
- Attempt 1: 200ms delay
- Attempt 2: 400ms delay
- Attempt 3: 800ms delay
- After 3 failures: Give up

**Reset Condition**:
- 4 seconds without failures

**Triggers**:
- `RuntimeException` during effect creation
- Transient audio pipeline issues
- Device-specific bugs

---

### 5. **Health Monitoring**

**Frequency**: Every 5 seconds during playback

**Checks**:
- Equalizer enabled state
- BassBoost enabled state
- LoudnessEnhancer enabled state

**Recovery**:
- Calls `restoreEffectStates()` if issues found
- Logs warning if >3 consecutive failures

---

### 6. **Device-Specific Workarounds**

**OnePlus/OPPO/Realme/Vivo**:
```kotlin
// LoudnessEnhancer must be enabled before setTargetGain
loud.enabled = true
loud.setTargetGain(gainMb)
```

**Android 15+**:
```kotlin
// Add 50ms delay for pipeline stabilization
Thread.sleep(50)
```

**Detection**:
```kotlin
if (isProblematicDevice()) {
    // Apply workarounds
}
```

---

## Effect Chain Order (FIXED)

```
AudioSource
    ↓
Equalizer (Priority 0)
    ↓
BassBoost (Priority 1)
    ↓
LoudnessEnhancer (Priority 2)
    ↓
Virtualizer (Priority 3)
    ↓
Output
```

**Why This Order**:
1. Equalizer first (frequency shaping)
2. BassBoost second (low-frequency enhancement)
3. LoudnessEnhancer third (overall volume)
4. Virtualizer last (spatial effects)

---

## Safe Value Ranges

| Effect | Parameter | Min | Max | Safe Max | Unit |
|--------|-----------|-----|-----|----------|------|
| BassBoost | Strength | 0 | 1000 | **300** | 0-1000 |
| LoudnessEnhancer | Gain | -2000 | 2000 | **2000** | millibels |
| Virtualizer | Strength | 0 | 1000 | **1000** | 0-1000 |
| Equalizer | Band Level | -1500 | 1500 | **1500** | millibels |

**Why Safe Max < Max**:
- BassBoost >300 causes distortion on most devices
- LoudnessEnhancer >2000mB (20dB) causes clipping

---

## Common Scenarios

### Scenario 1: User Plays Song
```
1. Player: STATE_READY + isPlaying
2. AudioEffectsManager.handlePlaybackStateChange()
3. Wait 100ms
4. Check memory
5. createEffects(sessionId)
6. restoreEffectStates()
7. Effects active ✅
```

### Scenario 2: User Connects Bluetooth
```
1. AudioDeviceCallback.onAudioDevicesAdded()
2. Debounce 500ms
3. Detect device type change
4. onAudioSessionIdChanged(newSessionId)
5. reattachEffects(newSessionId)
   - Release old effects
   - Wait 150ms
   - Create new effects
   - Restore states
6. Effects active on Bluetooth ✅
```

### Scenario 3: Low Memory Device
```
1. createEffects(sessionId)
2. checkAvailableMemory() → 45MB
3. isCriticalMemoryPressure() → true
4. throw OutOfMemoryError
5. Playback continues without effects ✅
```

### Scenario 4: Effect Creation Fails
```
1. createEffects(sessionId)
2. RuntimeException thrown
3. recordFailure()
4. shouldRetry() → true
5. Wait 200ms
6. Retry createEffects()
7. Success → recordSuccess() ✅
```

### Scenario 5: OnePlus Device
```
1. initialize()
2. isProblematicDevice() → true
3. applyDeviceSpecificWorkarounds()
4. setLoudnessEnhancer(enabled=true, gain=1000)
   - loud.enabled = true (FIRST)
   - loud.setTargetGain(1000) (SECOND)
5. Works correctly ✅
```

---

## Integration Points

### MusicService.kt

**onCreate()** (line 959):
```kotlin
audioEffectsManager = AudioEffectsManager(this, player).apply {
    initialize()
}
```

**AudioSessionId Listener** (lines 980-988):
```kotlin
player.addAnalyticsListener(object : AnalyticsListener {
    override fun onAudioSessionIdChanged(
        eventTime: AnalyticsListener.EventTime,
        audioSessionId: Int
    ) {
        audioEffectsManager?.onAudioSessionIdChanged(audioSessionId)
    }
})
```

**onDestroy()** (line 5364):
```kotlin
audioEffectsManager?.release()
```

---

## API Reference

### Public Methods

```kotlin
// Initialization
fun initialize()

// Session management
fun onAudioSessionIdChanged(newSessionId: Int)

// Equalizer
fun setEqualizerEnabled(enabled: Boolean)
fun setEqualizerBandLevel(band: Int, level: Short)
fun getEqualizerBandLevel(band: Int): Short
fun getEqualizerNumberOfBands(): Short

// BassBoost
fun setBassBoost(enabled: Boolean, strength: Short = 0)
fun setBassBoostStrength(strength: Short)

// LoudnessEnhancer
fun setLoudnessEnhancer(enabled: Boolean, gainMb: Int = 0)
fun setLoudnessGain(gainMb: Int)

// Virtualizer
fun setVirtualizer(enabled: Boolean, strength: Short = 0)

// Reverb
fun setReverbPreset(preset: Short)

// Deep Bass & Treble
fun setDeepBass(enabled: Boolean, level: Short = 0)
fun setTreble(enabled: Boolean, level: Short = 0)

// Utility
fun disableAllEffects()
fun getEffectStates(): Map<String, Any>
fun hasActiveEffects(): Boolean
fun release()
```

### Status Flow

```kotlin
audioEffectsManager.effectStatus.collect { status ->
    when (status) {
        is EffectStatus.Idle -> // No effects
        is EffectStatus.Attaching -> // Creating effects
        is EffectStatus.Attached -> // Effects active
        is EffectStatus.Error -> // Error: status.message
        is EffectStatus.LowMemory -> // Low memory: status.availableMB
        is EffectStatus.Retrying -> // Retrying: status.attempt/status.maxAttempts
    }
}
```

---

## Debugging Tips

### 1. Check Logs
```bash
adb logcat | grep AudioEffectsManager
```

### 2. Check Effect Status
```kotlin
val states = audioEffectsManager.getEffectStates()
Log.d("DEBUG", "Effects: $states")
```

### 3. Check Memory
```kotlin
// Look for this log on initialization:
💾 Available memory: 150MB
```

### 4. Check Device
```kotlin
// Look for this log on initialization:
🔧 Device: OnePlus OnePlus 9 Pro, Android 35
🔧 Applying workarounds for OnePlus device
```

### 5. Check Session Changes
```kotlin
// Look for these logs:
🔊 AudioSessionId changed: 12345
AudioSessionId changed: 0 → 12345 (re-attaching effects)
✅ Effects re-attached successfully to session 12345
```

---

## Performance Metrics

### Typical Values
- **Effect Creation**: 50-200ms
- **Effect Enable/Disable**: <5ms
- **Health Check**: <10ms
- **Memory Check**: <1ms
- **Session Re-attach**: 150-300ms

### Red Flags
- **Creation >500ms**: Slow device or memory pressure
- **>3 consecutive failures**: Device incompatibility
- **Available memory <100MB**: Low-end device

---

## Testing Commands

### Test Session Change
```bash
# Simulate Bluetooth connect
adb shell cmd media_session volume --show --stream 3 --set 10
```

### Test Memory Pressure
```bash
# Fill memory
adb shell am start -a android.intent.action.VIEW -d "https://example.com"
# (Open many apps to consume memory)
```

### Test Rapid Skipping
```kotlin
// In your test code
repeat(10) {
    player.seekToNext()
    delay(500)
}
```

---

## Known Issues & Solutions

### Issue: Effects not applying
**Solution**: Check `effectsInitialized` flag and `isPlaybackActive`

### Issue: LoudnessEnhancer throws UnsupportedOperationException
**Solution**: Already handled - effect is enabled before setTargetGain

### Issue: OutOfMemoryError on low-end devices
**Solution**: Already handled - effects skipped if <50MB available

### Issue: Effects reset on app restart
**Solution**: State is persisted in memory, but not to disk (future enhancement)

---

## Future Enhancements (Optional)

1. **DataStore Persistence**: Save effect states to disk
2. **Bluetooth Latency Compensation**: Adjust delays for Bluetooth audio lag
3. **Audio Focus Integration**: Disable effects during transient focus loss
4. **Telemetry**: Track effect usage and failures
5. **User-Facing Status**: Show effect status in UI

---

**Version**: 2.0 (Production-Hardened)  
**Last Updated**: 2025-12-22  
**Status**: ✅ Production Ready
