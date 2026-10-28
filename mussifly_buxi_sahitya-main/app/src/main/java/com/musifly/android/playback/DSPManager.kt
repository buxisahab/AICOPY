package com.musifly.android.playback

import android.content.Context

class DSPManager private constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: DSPManager? = null

        fun getInstance(context: Context): DSPManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DSPManager(context).also { INSTANCE = it }
            }
        }
    }

    fun loadSettings() {
        // Stub
    }

    fun release() {
        // Cleanup resources
    }
}
