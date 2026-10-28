package com.musifly.android.ui.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.musifly.android.MusicWidget

/**
 * Helper object to update all widgets when playback state changes.
 * This resolves compilation errors in MusicService where WidgetUpdateHelper is referenced.
 */
object WidgetUpdateHelper {
    /**
     * Sends a broadcast to force update all music widgets.
     * This relies on BaseMusicWidget handling the ACTION_FORCE_UPDATE intent.
     */
    fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MusicWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                // Send explicit broadcast to widget with widget IDs
                val intent = Intent(context, MusicWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdateHelper", "Failed to update widgets", e)
        }
    }

    /**
     * Updates all widgets directly with the provided player instance.
     * This ensures widgets are updated even if the app is in background/killed and PlayerConnection is unavailable.
     */
    /**
     * Updates all widgets directly with the provided player instance.
     * This ensures widgets are updated even if the app is in background/killed and PlayerConnection is unavailable.
     */
    fun updateAllWidgets(context: Context, player: androidx.media3.common.Player) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Update Classic MusicWidget
        try {
            val componentName = ComponentName(context, MusicWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val views = android.widget.RemoteViews(context.packageName, com.musifly.android.R.layout.widget_music)
                MusicWidget.updateViews(context, views, player)
                appWidgetManager.updateAppWidget(appWidgetIds, views)
                android.util.Log.d("WidgetUpdateHelper", "Updated ${appWidgetIds.size} Classic widgets")
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdateHelper", "Failed to update Classic widgets", e)
        }

        // Update UniversalMusicWidget
        try {
            val componentName = ComponentName(context, com.musifly.android.UniversalMusicWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val views = android.widget.RemoteViews(context.packageName, com.musifly.android.R.layout.widget_universal_music)
                com.musifly.android.UniversalMusicWidget.updateViews(context, views, player)
                appWidgetManager.updateAppWidget(appWidgetIds, views)
                android.util.Log.d("WidgetUpdateHelper", "Updated ${appWidgetIds.size} Universal widgets")
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdateHelper", "Failed to update Universal widgets", e)
        }
        
        // Update SmallMusicWidget
        try {
            val componentName = ComponentName(context, com.musifly.android.SmallMusicWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val views = android.widget.RemoteViews(context.packageName, com.musifly.android.R.layout.widget_music_small)
                com.musifly.android.SmallMusicWidget.updateViews(context, views, player)
                appWidgetManager.updateAppWidget(appWidgetIds, views)
                android.util.Log.d("WidgetUpdateHelper", "Updated ${appWidgetIds.size} Small widgets")
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdateHelper", "Failed to update Small widgets", e)
        }
        
        // Update BarMusicWidget
        try {
            val componentName = ComponentName(context, com.musifly.android.BarMusicWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val views = android.widget.RemoteViews(context.packageName, com.musifly.android.R.layout.widget_music_bar)
                com.musifly.android.BarMusicWidget.updateViews(context, views, player)
                appWidgetManager.updateAppWidget(appWidgetIds, views)
                android.util.Log.d("WidgetUpdateHelper", "Updated ${appWidgetIds.size} Bar widgets")
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdateHelper", "Failed to update Bar widgets", e)
        }
    }
}
