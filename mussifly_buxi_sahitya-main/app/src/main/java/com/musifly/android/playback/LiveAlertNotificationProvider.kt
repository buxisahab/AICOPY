package com.musifly.android.playback

import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.musifly.android.R
import com.google.common.collect.ImmutableList

/**
 * Custom notification provider optimized for Live Alert/Dynamic Island display
 * on OnePlus, Samsung, and other compatible devices.
 */
@UnstableApi
class LiveAlertNotificationProvider(
    private val context: Context,
    notificationIdProvider: DefaultMediaNotificationProvider.NotificationIdProvider,
    private val channelId: String,
    channelNameResourceId: Int
) : MediaNotification.Provider {

    private val defaultProvider = DefaultMediaNotificationProvider(
        context,
        notificationIdProvider,
        channelId,
        channelNameResourceId
    )

    // Cache for artwork bitmap to prevent repeated decoding
    private var cachedArtworkData: ByteArray? = null
    private var cachedBitmap: Bitmap? = null

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<androidx.media3.session.CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        // Get the default notification first
        val defaultNotification = defaultProvider.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            onNotificationChangedCallback
        )

        val player = mediaSession.player
        val metadata = player.mediaMetadata

        // Build enhanced notification optimized for Live Alert
        // Note: OnePlus Live Alerts may not support custom compact views, so we rely on MediaStyle
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.opentune_monochrome)
            .setContentTitle(metadata.title)
            .setContentText(metadata.artist)
            .setSubText(metadata.albumTitle)
            .setLargeIcon(getLargeIcon(metadata))
            .setContentIntent(defaultNotification.notification.contentIntent)
            .setDeleteIntent(defaultNotification.notification.deleteIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT) // Critical for Live Alert
            .setOngoing(player.playWhenReady) // Only ongoing while playing/buffering
            .setPriority(NotificationCompat.PRIORITY_LOW) // Keep it low to avoid popping up too much
            .setColor(0xFF1DB954.toInt()) // Spotify green - helps with recognition
            .setColorized(true)

        // Add MediaStyle - critical for Live Alert recognition
        val mediaStyle = MediaStyle()
            .setShowActionsInCompactView(0, 1, 2) // Show all three actions
            .setShowCancelButton(false)

        builder.setStyle(mediaStyle)

        // SMART ACTION PRIORITIZATION: Prioritize controls most used by users
        // 1. Prev, 2. Play/Pause, 3. Next, 4. Like/Favorite, 5. Shuffle/Repeat
        val allActions = defaultNotification.notification.actions
        if (allActions != null) {
            val prioritizedActions = mutableListOf<android.app.Notification.Action>()
            
            // Step 1: Add core playback controls (usually indices 0, 1, 2)
            allActions.take(3).forEach { prioritizedActions.add(it) }
            
            // Step 2: Find "Like" or custom actions
            val likeAction = allActions.find { it.title?.toString()?.contains("Like", ignoreCase = true) == true }
            if (likeAction != null && prioritizedActions.size < 5) {
                if (!prioritizedActions.contains(likeAction)) prioritizedActions.add(likeAction)
            }
            
            // Step 3: Fill remaining slots up to 5
            allActions.forEach { action ->
                if (prioritizedActions.size < 5 && !prioritizedActions.contains(action)) {
                    prioritizedActions.add(action)
                }
            }
            
            // Add the prioritized actions to the builder
            prioritizedActions.forEach { action ->
                val compatAction = NotificationCompat.Action.Builder(
                    action.icon,
                    action.title,
                    action.actionIntent
                ).build()
                builder.addAction(compatAction)
            }
            
            android.util.Log.d("LiveAlertNotification", "✅ Added ${prioritizedActions.size} prioritized actions to notification")
        }

        return MediaNotification(
            defaultNotification.notificationId,
            builder.build()
        )
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: android.os.Bundle
    ): Boolean {
        return defaultProvider.handleCustomCommand(session, action, extras)
    }

    private fun getLargeIcon(metadata: androidx.media3.common.MediaMetadata): Bitmap? {
        val artworkData = metadata.artworkData
        
        if (artworkData == null) {
            cachedArtworkData = null
            cachedBitmap = null
            return ThumbnailProvider.getAppIconBitmap(context)
        }

        // Return cached bitmap if artwork data hasn't changed
        if (artworkData.contentEquals(cachedArtworkData)) {
            return cachedBitmap ?: ThumbnailProvider.getAppIconBitmap(context)
        }

        // Artwork changed or not cached, decode new bitmap
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                artworkData,
                0,
                artworkData.size
            )
            
            if (bitmap != null) {
                cachedArtworkData = artworkData
                cachedBitmap = bitmap
                bitmap
            } else {
                cachedArtworkData = null
                cachedBitmap = null
                ThumbnailProvider.getAppIconBitmap(context)
            }
        } catch (e: Exception) {
            android.util.Log.e("LiveAlertNotification", "Error decoding artwork", e)
            cachedArtworkData = null
            cachedBitmap = null
            ThumbnailProvider.getAppIconBitmap(context)
        }
    }
}

