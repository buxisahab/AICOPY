package com.musifly.android.sync.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing user's liked songs locally
 * Separate from SongEntity.liked field to support Firebase sync
 */
@Entity(tableName = "user_liked_songs")
data class UserLikedSongEntity(
    @PrimaryKey val songId: String,
    val syncedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false // True if synced with Firestore
)
