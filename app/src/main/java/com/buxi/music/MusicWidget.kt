package com.buxi.music

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.Player
import coil.ImageLoader
import coil.request.ImageRequest
import com.buxi.music.playback.PlayerConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MusicWidget : AppWidgetProvider() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        try {
            Log.d("MusicWidget", "onUpdate called with ${appWidgetIds.size} widgets")
            appWidgetIds.forEach { appWidgetId ->
                updateWidget(context, appWidgetManager, appWidgetId)
            }
            startProgressUpdater(context)
        } catch (e: Exception) {
            Log.e("MusicWidget", "Error in onUpdate", e)
        }
    }

    override fun onEnabled(context: Context) {
        try {
            Log.d("MusicWidget", "onEnabled called")
            startProgressUpdater(context)
        } catch (e: Exception) {
            Log.e("MusicWidget", "Error in onEnabled", e)
        }
    }

    override fun onDisabled(context: Context) {
        try {
            Log.d("MusicWidget", "onDisabled called")
            stopProgressUpdater()
        } catch (e: Exception) {
            Log.e("MusicWidget", "Error in onDisabled", e)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            super.onReceive(context, intent)
            Log.d("MusicWidget", "onReceive called with action: ${intent.action}")
            
            when (intent.action) {
                ACTION_PLAY_PAUSE -> {
                    try {
                        PlayerConnection.instance?.togglePlayPause()
                        updateAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e("MusicWidget", "Error in ACTION_PLAY_PAUSE", e)
                    }
                }

                ACTION_PREV -> {
                    try {
                        PlayerConnection.instance?.seekToPrevious()
                        updateAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e("MusicWidget", "Error in ACTION_PREV", e)
                    }
                }

                ACTION_NEXT -> {
                    try {
                        PlayerConnection.instance?.seekToNext()
                        updateAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e("MusicWidget", "Error in ACTION_NEXT", e)
                    }
                }

                ACTION_UPDATE -> {
                    try {
                        updateAllWidgets(context)
                    } catch (e: Exception) {
                        Log.e("MusicWidget", "Error in ACTION_UPDATE", e)
                    }
                }
            }
                    } catch (e: Exception) {
            Log.e("MusicWidget", "Error in onReceive", e)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_music)
            
            // Set up click listeners
            val playPauseIntent = Intent(context, MusicWidget::class.java).apply {
                action = ACTION_PLAY_PAUSE
            }
            val playPausePendingIntent = PendingIntent.getBroadcast(
                context, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePendingIntent)

            val prevIntent = Intent(context, MusicWidget::class.java).apply {
                action = ACTION_PREV
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_prev, prevPendingIntent)

            val nextIntent = Intent(context, MusicWidget::class.java).apply {
                action = ACTION_NEXT
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_next, nextPendingIntent)

            // Update widget content
            updateWidgetContent(context, views)
            
            appWidgetManager.updateAppWidget(appWidgetId, views)
                    } catch (e: Exception) {
            Log.e("MusicWidget", "Error updating widget", e)
        }
    }

    private fun updateWidgetContent(context: Context, views: RemoteViews) {
        try {
            val playerConnection = PlayerConnection.instance
            if (playerConnection != null) {
                // Update play/pause button based on player state
                val isPlaying = playerConnection.player.playWhenReady
                val playPauseIcon = if (isPlaying) R.drawable.pause else R.drawable.play
                views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)
                
                // Get current media item
                val currentMediaItem = playerConnection.player.currentMediaItem
                if (currentMediaItem != null) {
                    val mediaMetadata = currentMediaItem.mediaMetadata
                    views.setTextViewText(R.id.widget_title, mediaMetadata.title ?: "Unknown Title")
                    views.setTextViewText(R.id.widget_artist, mediaMetadata.artist ?: "Unknown Artist")
                    
                    // Load album art if available
                    val artworkUri = mediaMetadata.artworkUri
                    if (artworkUri != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val imageLoader = ImageLoader(context)
                                val request = ImageRequest.Builder(context)
                                    .data(artworkUri)
                                    .build()
                                val drawable = imageLoader.execute(request).drawable
                                val bitmap = drawable?.toBitmap(200, 200)
                                if (bitmap != null) {
                                    views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                                }
                    } catch (e: Exception) {
                                Log.e("MusicWidget", "Error loading album art", e)
                            }
                        }
                    }
                } else {
                    views.setTextViewText(R.id.widget_title, "No song playing")
                    views.setTextViewText(R.id.widget_artist, "")
                }
            } else {
                views.setTextViewText(R.id.widget_title, "No music service")
                views.setTextViewText(R.id.widget_artist, "")
                views.setImageViewResource(R.id.widget_play_pause, R.drawable.play)
            }
        } catch (e: Exception) {
            Log.e("MusicWidget", "Error updating widget content", e)
        }
    }

    private fun startProgressUpdater(context: Context) {
        try {
            runnable = object : Runnable {
                override fun run() {
                try {
                    updateAllWidgets(context)
                        handler.postDelayed(this, 1000) // Update every second
                } catch (e: Exception) {
                    Log.e("MusicWidget", "Error in progress updater", e)
                    }
                }
            }
            handler.post(runnable)
        } catch (e: Exception) {
            Log.e("MusicWidget", "Error starting progress updater", e)
        }
    }

    private fun stopProgressUpdater() {
        try {
            handler.removeCallbacks(runnable)
        } catch (e: Exception) {
            Log.e("MusicWidget", "Error stopping progress updater", e)
        }
    }

    private fun updateAllWidgets(context: Context) {
        try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MusicWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            appWidgetIds.forEach { appWidgetId ->
                updateWidget(context, appWidgetManager, appWidgetId)
                            }
                        } catch (e: Exception) {
            Log.e("MusicWidget", "Error updating all widgets", e)
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.buxi.music.widget.PLAY_PAUSE"
        const val ACTION_PREV = "com.buxi.music.widget.PREV"
        const val ACTION_NEXT = "com.buxi.music.widget.NEXT"
        const val ACTION_UPDATE = "com.buxi.music.widget.UPDATE"
    }
}
