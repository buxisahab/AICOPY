package com.musifly.android.sync.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity for storing songs in user playlists
 * Maps songs to user playlists with position
 */
@Entity(
    tableName = "user_playlist_songs",
    foreignKeys = [
        ForeignKey(
            entity = UserPlaylistEntity::class,
            parentColumns = ["playlistId"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["songId"])
    ]
)
data class UserPlaylistSongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: String,
    val songId: String,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
