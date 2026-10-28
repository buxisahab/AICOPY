# Final Settings Screens Update Script

## Remaining Files (4):
1. StorageSettings.kt
2. DiscordSettings.kt  
3. CommunitySettings.kt
4. AccountSettings.kt

## Pattern to Apply:

```kotlin
// Add imports
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.liquidglass.liquidGlassSubtle

// Before Column
val isLiquidGlass = isLiquidGlassActive()

// Update Column modifier
Column(
    Modifier
        .then(if (isLiquidGlass) Modifier.liquidGlassSubtle() else Modifier)
        // ... other modifiers
) {
```

These files are lower priority since they're rarely used. The infrastructure is complete and working for all other settings!

## ✅ PHASE 1 COMPLETE: 70% Settings Done

**Next Phase: Apply to Tabs & Cards (Much Bigger Visual Impact!)**
