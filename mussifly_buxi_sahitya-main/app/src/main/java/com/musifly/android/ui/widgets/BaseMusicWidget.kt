package com.musifly.android.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import coil.ImageLoader
import coil.request.ImageRequest
import com.musifly.android.R
import com.musifly.android.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseMusicWidget : AppWidgetProvider() {

    abstract fun getLayoutId(): Int
    abstract fun onUpdateViews(context: Context, views: RemoteViews, player: Player)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        android.util.Log.d("MusicWidget", "onUpdate called with ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            android.util.Log.d("MusicWidget", "Updating widget ID: $appWidgetId")
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        android.util.Log.d("MusicWidget", "onReceive called with action: ${intent.action}")
        
        when (intent.action) {
            ACTION_PLAY_PAUSE, ACTION_PREV, ACTION_NEXT, ACTION_SHUFFLE, ACTION_LIKE -> {
                android.util.Log.d("MusicWidget", "Widget button clicked: ${intent.action}")
                // Start service with the action
                val serviceIntent = Intent(context, com.musifly.android.playback.MusicService::class.java).apply {
                    action = intent.action
                    putExtra("user_initiated", true) // Mark as user-initiated to prevent auto-pause on start
                }
                
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    android.util.Log.d("MusicWidget", "Service started successfully with user_initiated=true")
                } catch (e: Exception) {
                    android.util.Log.e("BaseMusicWidget", "Failed to start service", e)
                }
                
                // Direct cleanup or minor UI update if needed, but DO NOT call onUpdate()
                // because it might reset the widget to default state if PlayerConnection is null.
                // The service will call WidgetUpdateHelper.updateAllWidgets() with the valid player.
            }
            ACTION_FORCE_UPDATE, AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                android.util.Log.d("MusicWidget", "Force update or system update requested")
                val manager = AppWidgetManager.getInstance(context)
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    ?: manager.getAppWidgetIds(ComponentName(context, this::class.java))
                android.util.Log.d("MusicWidget", "Updating ${ids.size} widgets")
                onUpdate(context, manager, ids)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        android.util.Log.d("MusicWidget", "updateAppWidget called for ID: $appWidgetId")
        val views = RemoteViews(context.packageName, getLayoutId())
        val player = PlayerConnection.instance?.player

        if (player != null) {
            android.util.Log.d("MusicWidget", "Player available, updating widget views")
            onUpdateViews(context, views, player)
        } else {
            android.util.Log.w("MusicWidget", "Player is null, widget will show default state")
            // Set default text when no player
            views.setTextViewText(R.id.widget_song_title, "No music playing")
            views.setTextViewText(R.id.widget_artist, "Tap to start")
            
            // Set up all buttons even in empty state
            val widgetClass = this::class.java
            BaseMusicWidget.setOnClick(context, views, R.id.widget_play_pause, ACTION_PLAY_PAUSE, widgetClass)
            BaseMusicWidget.setOnClick(context, views, R.id.widget_next, ACTION_NEXT, widgetClass)
            BaseMusicWidget.setOnClick(context, views, R.id.widget_prev, ACTION_PREV, widgetClass)
            BaseMusicWidget.setOnClick(context, views, R.id.widget_root, ACTION_Open_APP, widgetClass)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
        android.util.Log.d("MusicWidget", "Widget $appWidgetId updated successfully")
    }

    // Removed instance methods setOnClick and loadArtwork to force usage of Companion methods



    companion object {
        const val ACTION_PLAY_PAUSE = "com.musifly.android.widget.ACTION_PLAY_PAUSE"
        const val ACTION_PREV = "com.musifly.android.widget.ACTION_PREV"
        const val ACTION_NEXT = "com.musifly.android.widget.ACTION_NEXT"
        const val ACTION_SHUFFLE = "com.musifly.android.widget.ACTION_SHUFFLE"
        const val ACTION_LIKE = "com.musifly.android.widget.ACTION_LIKE"
        const val ACTION_FORCE_UPDATE = "com.musifly.android.widget.ACTION_FORCE_UPDATE"
        const val ACTION_Open_APP = "com.musifly.android.widget.ACTION_OPEN_APP"
        const val ACTION_STATE_CHANGED = "com.musifly.android.widget.ACTION_STATE_CHANGED"
        const val ACTION_UPDATE_PROGRESS = "com.musifly.android.widget.ACTION_UPDATE_PROGRESS"

        fun setOnClick(context: Context, views: RemoteViews, viewId: Int, action: String, widgetClass: Class<*>) {
            val pendingIntent = if (action == ACTION_Open_APP) {
                // Open main activity
                val intent = Intent(context, com.musifly.android.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                PendingIntent.getActivity(
                    context, viewId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
                )
            } else {
                // Widget action broadcast
                val intent = Intent(context, widgetClass).apply { this.action = action }
                PendingIntent.getBroadcast(
                    context, viewId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
                )
            }
            views.setOnClickPendingIntent(viewId, pendingIntent)
        }

        fun loadArtwork(context: Context, views: RemoteViews, viewId: Int, player: Player, blur: Boolean = false, widgetClass: Class<*>) {
            val uri = player.mediaMetadata.artworkUri
            if (uri != null) {
                 CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val request = ImageRequest.Builder(context)
                            .data(uri)
                            //.transformations(if (blur) listOf(BlurTransformation(context, 25f)) else emptyList()) // Coil blur if needed
                            .size(300, 300) 
                            .build()
                        val drawable = ImageLoader(context).execute(request).drawable
                        val bitmap = drawable?.toBitmap()
                        
                        if (bitmap != null) {
                            // TODO: Apply software blur if 'blur' is true and not using transformations
                            views.setImageViewBitmap(viewId, bitmap)
                            AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(
                                 AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, widgetClass)), 
                                 views
                            )
                        }
                    } catch (e: Exception) {
                        views.setImageViewResource(viewId, R.drawable.music_note)
                    }
                 }
            } else {
                 views.setImageViewResource(viewId, R.drawable.music_note)
            }
        }
    }
}
