package com.musifly.android.playback

import android.content.Context

class AiDspEngine private constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: AiDspEngine? = null

        fun getInstance(context: Context): AiDspEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AiDspEngine(context).also { INSTANCE = it }
            }
        }
    }
}
