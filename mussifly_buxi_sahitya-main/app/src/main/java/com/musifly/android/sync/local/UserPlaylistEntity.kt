package com.musifly.android.sync.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing user's custom playlists locally
 * Separate from PlaylistEntity to support Firebase sync for user-created playlists
 */
@Entity(tableName = "user_playlists")
data class UserPlaylistEntity(
    @PrimaryKey val playlistId: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false, // True if synced with Firestore
    val userId: String? = null // Firebase user ID if synced
)
