package com.musifly.android.sync.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity for storing user's cached/downloaded songs locally
 * Separate from ExoPlayer's download tracking to support Firebase sync
 * Only stores song IDs and metadata - actual audio files stay in local storage
 */
@Entity(tableName = "user_cached_downloads")
data class UserCachedDownloadEntity(
    @PrimaryKey val songId: String,
    val syncedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false, // True if synced with Firestore
    val cachedAt: Long = System.currentTimeMillis() // When the song was cached locally
)
