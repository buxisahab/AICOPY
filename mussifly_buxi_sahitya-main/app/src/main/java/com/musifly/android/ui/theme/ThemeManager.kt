package com.musifly.android.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.musifly.android.constants.ThemeMode
import com.musifly.android.constants.ThemeModeKey
import com.musifly.android.extensions.toEnum
import com.musifly.android.utils.dataStore
import com.musifly.android.utils.get
import kotlinx.coroutines.flow.map

/**
 * ThemeManager utility class for managing theme preferences
 */
object ThemeManager {
    
    /**
     * Get the current theme mode from DataStore
     */
    fun getThemeMode(context: Context): ThemeMode {
        return context.dataStore[ThemeModeKey].toEnum(defaultValue = ThemeMode.DARK)
    }
    
    /**
     * Get theme mode as a Flow for reactive updates
     */
    fun getThemeModeFlow(context: Context) = context.dataStore.data.map { preferences ->
        preferences[ThemeModeKey]?.toEnum(defaultValue = ThemeMode.DARK) ?: ThemeMode.DARK
    }
    
    /**
     * Composable function to get current theme mode
     */
    @Composable
    fun rememberThemeMode(): ThemeMode {
        val context = LocalContext.current
        val themeMode by context.dataStore.data.map { preferences ->
            preferences[ThemeModeKey]?.toEnum(defaultValue = ThemeMode.DARK) ?: ThemeMode.DARK
        }.collectAsState(initial = ThemeMode.DARK)
        return themeMode
    }
    

    
    /**
     * Check if current theme is Light
     */
    fun isLight(context: Context): Boolean {
        return getThemeMode(context) == ThemeMode.LIGHT
    }
    
    /**
     * Check if current theme is Dark
     */
    fun isDark(context: Context): Boolean {
        return getThemeMode(context) == ThemeMode.DARK
    }
}
