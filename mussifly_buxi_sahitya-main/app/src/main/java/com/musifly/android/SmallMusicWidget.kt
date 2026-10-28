package com.musifly.android

import android.content.Context
import android.widget.RemoteViews
import androidx.media3.common.Player
import com.musifly.android.ui.widgets.BaseMusicWidget

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Small music widget implementation (2x2).
 */
class SmallMusicWidget : BaseMusicWidget() {

    override fun getLayoutId(): Int = R.layout.widget_music_small

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
            
            // Note: Progress Bar is hidden in small layout, but we keep code safe or skip it.
            // Skipping strictly visual updates for hidden elements saves performace.
            
            // Update shuffle button state (Hidden in small layout but good to keep if valid)
            // val shuffleAlpha = if (player.shuffleModeEnabled) 1.0f else 0.5f
            // views.setFloat(R.id.widget_shuffle, "setAlpha", shuffleAlpha)

            // Update like button (Hidden in small widget)
            // ...

            // Load album artwork
            BaseMusicWidget.loadArtwork(context, views, R.id.widget_album_art, player, blur = false, SmallMusicWidget::class.java)

            // Set up button click handlers
            BaseMusicWidget.setOnClick(context, views, R.id.widget_play_pause, BaseMusicWidget.ACTION_PLAY_PAUSE, SmallMusicWidget::class.java)
            BaseMusicWidget.setOnClick(context, views, R.id.widget_next, BaseMusicWidget.ACTION_NEXT, SmallMusicWidget::class.java)
            BaseMusicWidget.setOnClick(context, views, R.id.widget_prev, BaseMusicWidget.ACTION_PREV, SmallMusicWidget::class.java)
            BaseMusicWidget.setOnClick(context, views, R.id.widget_root, BaseMusicWidget.ACTION_Open_APP, SmallMusicWidget::class.java)
            
            // App Icon opens app
            BaseMusicWidget.setOnClick(context, views, R.id.widget_logo, BaseMusicWidget.ACTION_Open_APP, SmallMusicWidget::class.java)
        }
    }
}
