package com.musifly.android

import android.content.Context
import android.widget.RemoteViews
import androidx.media3.common.Player
import com.musifly.android.ui.widgets.BaseMusicWidget

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Universal music widget implementation designed for broad compatibility.
 */
class UniversalMusicWidget : BaseMusicWidget() {

    override fun getLayoutId(): Int = R.layout.widget_universal_music

    override fun onUpdateViews(context: Context, views: RemoteViews, player: Player) {
        updateViews(context, views, player)
    }

    companion object {
        fun updateViews(context: Context, views: RemoteViews, player: Player) {
            // Update song title
            val title = player.mediaMetadata.title?.toString() ?: context.getString(R.string.song_title)
            views.setTextViewText(R.id.widget_song_title, title)

            // Update artist name
            val artist = player.mediaMetadata.artist?.toString() ?: context.getString(R.string.artist_name)
            views.setTextViewText(R.id.widget_artist, artist)

            // Update play/pause button
            val playPauseIcon = if (player.isPlaying) {
                R.drawable.pause
            } else {
                R.drawable.play
            }
            views.setImageViewResource(R.id.widget_play_pause, playPauseIcon)
            
            // Update Progress Bar
            val duration = player.duration
            val position = player.currentPosition
            if (duration > 0) {
                 views.setProgressBar(R.id.widget_progress_bar, duration.toInt(), position.toInt(), false)
            } else {
                 views.setProgressBar(R.id.widget_progress_bar, 100, 0, false)
            }

            // Update shuffle button state
            val shuffleAlpha = if (player.shuffleModeEnabled) 1.0f else 0.5f
            views.setFloat(R.id.widget_shuffle, "setAlpha", shuffleAlpha)

            // Update like button - show filled red heart when liked
            val liked = (player.currentMediaItem?.localConfiguration?.tag as? com.musifly.android.models.MediaMetadata)?.liked == true
            val likeIcon = if (liked) R.drawable.favorite else R.drawable.favorite_border
            
            views.setImageViewResource(R.id.widget_like, likeIcon)
            
            // Apply red color when liked using ColorStateList for better compatibility
            val tintColor = if (liked) android.graphics.Color.RED else android.graphics.Color.WHITE
            views.setInt(R.id.widget_like, "setColorFilter", tintColor)
            views.setInt(R.id.widget_like, "setImageAlpha", 255) // Ensure full opacity

            // Load album artwork
            BaseMusicWidget.loadArtwork(context, views, R.id.widget_album_art, player, blur = false, UniversalMusicWidget::class.java)

            // Set up button click handlers
            BaseMusicWidget.setOnClick(context, views, R.id.widget_play_pause, BaseMusicWidget.ACTION_PLAY_PAUSE, UniversalMusicWidget::class.java)
            BaseMusicWidget.setOnClick(context, views, R.id.widget_next, BaseMusicWidget.ACTION_NEXT, UniversalMusicWidget::class.java)
            BaseMusicWidget.setOnClick(context, views, R.id.widget_prev, BaseMusicWidget.ACTION_PREV, UniversalMusicWidget::class.java)
            BaseMusicWidget.setOnClick(context, views, R.id.widget_shuffle, BaseMusicWidget.ACTION_SHUFFLE, UniversalMusicWidget::class.java)
            BaseMusicWidget.setOnClick(context, views, R.id.widget_like, BaseMusicWidget.ACTION_LIKE, UniversalMusicWidget::class.java)
            BaseMusicWidget.setOnClick(context, views, R.id.widget_root, BaseMusicWidget.ACTION_Open_APP, UniversalMusicWidget::class.java)
            
            // App Icon opens app
            BaseMusicWidget.setOnClick(context, views, R.id.widget_logo, BaseMusicWidget.ACTION_Open_APP, UniversalMusicWidget::class.java)
            
            // Allow clicking progress bar to open app as "seek" is not supported directly but opening app allows seeking
            BaseMusicWidget.setOnClick(context, views, R.id.widget_progress_bar, BaseMusicWidget.ACTION_Open_APP, UniversalMusicWidget::class.java)
        }
    }
}
