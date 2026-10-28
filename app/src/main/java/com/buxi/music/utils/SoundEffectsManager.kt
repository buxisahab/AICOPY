package com.buxi.music.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log

class SoundEffectsManager(context: Context) {
    private val TAG = "SoundFX"
    private val appContext = context.applicationContext
    private var soundPool: SoundPool? = null
    private var rainSoundId: Int = 0
    private var rainStreamId: Int = 0
    private var thunderSoundId: Int = 0
    private var thunderStreamId: Int = 0
    private var isInitialized = false
    
    companion object {
        @Volatile
        private var instance: SoundEffectsManager? = null

        fun getInstance(context: Context): SoundEffectsManager {
            return instance ?: synchronized(this) {
                instance ?: SoundEffectsManager(context).also { instance = it }
            }
        }
    }

    init {
        initializeSoundPool()
    }
    
    private fun initializeSoundPool() {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            soundPool = SoundPool.Builder()
                .setMaxStreams(4) // Increased for better performance
                .setAudioAttributes(attrs)
                .build()

            // Try to load sound files with multiple fallback options
            loadSoundFiles()
            isInitialized = true
            Log.d(TAG, "SoundEffectsManager initialized successfully")
        } catch (t: Throwable) {
            Log.e(TAG, "SoundEffectsManager init failed", t)
            isInitialized = false
        }
    }
    
    private fun loadSoundFiles() {
        // Try multiple possible sound file names
        val rainFiles = listOf("rain", "rain_sound", "rain_effect", "ambient_rain")
        val thunderFiles = listOf("thunder", "thunder_sound", "thunder_effect", "ambient_thunder")
        
        // Load rain sound
        for (fileName in rainFiles) {
            val rainRes = appContext.resources.getIdentifier(fileName, "raw", appContext.packageName)
            if (rainRes != 0) {
                rainSoundId = soundPool!!.load(appContext, rainRes, 1)
                Log.d(TAG, "Rain sound loaded: $fileName (ID: $rainSoundId)")
                break
            }
        }
        
        // Load thunder sound
        for (fileName in thunderFiles) {
            val thunderRes = appContext.resources.getIdentifier(fileName, "raw", appContext.packageName)
            if (thunderRes != 0) {
                thunderSoundId = soundPool!!.load(appContext, thunderRes, 1)
                Log.d(TAG, "Thunder sound loaded: $fileName (ID: $thunderSoundId)")
                break
            }
        }
        
        // Log status
        if (rainSoundId == 0) {
            Log.w(TAG, "No rain sound file found in raw resources")
        }
        if (thunderSoundId == 0) {
            Log.w(TAG, "No thunder sound file found in raw resources")
        }
    }

    fun setRainLevel(level01: Float) {
        if (!isInitialized) {
            Log.w(TAG, "SoundEffectsManager not initialized, cannot set rain level")
            return
        }
        
        val sp = soundPool ?: return
        if (rainSoundId == 0) {
            Log.w(TAG, "Rain sound not loaded, cannot play rain effect")
            return
        }
        
        val clamped = level01.coerceIn(0f, 1f)
        // Increased volume - minimum 0.4f, maximum 1.0f for louder rain sound
        val vol = if (clamped <= 0f) 0f else (0.4f + clamped * 0.6f).coerceAtMost(1f)
        
        Log.d(TAG, "setRainLevel: level=$level01, clamped=$clamped, volume=$vol")
        
        if (clamped <= 0f) {
            if (rainStreamId != 0) {
                try { 
                    sp.stop(rainStreamId)
                    Log.d(TAG, "Rain sound stopped")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop rain sound", e)
                }
                rainStreamId = 0
            }
            return
        }
        
        if (rainStreamId == 0) {
            // loop forever (-1) - rain plays continuously while applied
            try {
                rainStreamId = sp.play(rainSoundId, vol, vol, 1, -1, 1f)
                Log.d(TAG, "Rain sound started (stream ID: $rainStreamId) - LOOPING")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play rain sound", e)
            }
        } else {
            try {
                sp.setVolume(rainStreamId, vol, vol)
                Log.d(TAG, "Rain sound volume updated to $vol")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set rain sound volume", e)
            }
        }
    }

    fun playThunder(level01: Float) {
        if (!isInitialized) {
            Log.w(TAG, "SoundEffectsManager not initialized, cannot set thunder level")
            return
        }
        
        val sp = soundPool ?: return
        if (thunderSoundId == 0) {
            Log.w(TAG, "Thunder sound not loaded, cannot play thunder effect")
            return
        }
        
        val clamped = level01.coerceIn(0f, 1f)
        val vol = if (clamped <= 0f) 0f else (0.4f + clamped * 0.8f).coerceAtMost(1f)
        
        Log.d(TAG, "playThunder: level=$level01, clamped=$clamped, volume=$vol")
        
        if (clamped <= 0f) {
            if (thunderStreamId != 0) {
                try { 
                    sp.stop(thunderStreamId)
                    Log.d(TAG, "Thunder sound stopped")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to stop thunder sound", e)
                }
                thunderStreamId = 0
            }
            return
        }
        
        if (thunderStreamId == 0) {
            try {
                thunderStreamId = sp.play(thunderSoundId, vol, vol, 1, -1, 1f)
                Log.d(TAG, "Thunder sound started (stream ID: $thunderStreamId)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play thunder sound", e)
            }
        } else {
            try {
                sp.setVolume(thunderStreamId, vol, vol)
                Log.d(TAG, "Thunder sound volume updated to $vol")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set thunder sound volume", e)
            }
        }
    }

    // Convenience alias for continuous control
    fun setThunderLevel(level01: Float) = playThunder(level01)
    
    // Check if sound effects are available
    fun isRainSoundAvailable(): Boolean {
        return isInitialized && rainSoundId != 0
    }
    
    fun isThunderSoundAvailable(): Boolean {
        return isInitialized && thunderSoundId != 0
    }
    
    fun getStatus(): String {
        return "SoundEffectsManager Status:\n" +
                "Initialized: $isInitialized\n" +
                "Rain Sound ID: $rainSoundId\n" +
                "Thunder Sound ID: $thunderSoundId\n" +
                "Rain Stream ID: $rainStreamId\n" +
                "Thunder Stream ID: $thunderStreamId"
    }

    fun release() {
        try {
            soundPool?.release()
            Log.d(TAG, "SoundEffectsManager released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release SoundEffectsManager", e)
        }
        soundPool = null
        rainStreamId = 0
        rainSoundId = 0
        thunderSoundId = 0
        thunderStreamId = 0
        isInitialized = false
    }
}


