package com.musifly.android.playback

import kotlinx.coroutines.flow.StateFlow

class BackgroundAudioProcessor(
    dspEngine: AiDspEngine,
    isAppInBackground: StateFlow<Boolean>,
    isScreenOff: StateFlow<Boolean>
) {
    fun setEnabled(enabled: Boolean) {
        // Implementation stub
    }

    fun shutdown() {
        // Shutdown logic
    }
}
