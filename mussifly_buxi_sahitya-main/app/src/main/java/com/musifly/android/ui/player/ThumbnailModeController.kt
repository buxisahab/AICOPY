package com.musifly.android.ui.player

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.musifly.android.constants.ThumbnailDisplayMode
import com.musifly.android.constants.ThumbnailDisplayModeKey
import com.musifly.android.utils.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThumbnailModeController(private val context: Context) {

    val currentMode: Flow<ThumbnailDisplayMode> = context.dataStore.data
        .map { preferences ->
            val modeName = preferences[ThumbnailDisplayModeKey]
            if (modeName != null) {
                try {
                    ThumbnailDisplayMode.valueOf(modeName)
                } catch (e: IllegalArgumentException) {
                    ThumbnailDisplayMode.BLUR_HD
                }
            } else {
                ThumbnailDisplayMode.BLUR_HD
            }
        }

    suspend fun setMode(mode: ThumbnailDisplayMode) {
        context.dataStore.edit { preferences ->
            preferences[ThumbnailDisplayModeKey] = mode.name
        }
    }
}
