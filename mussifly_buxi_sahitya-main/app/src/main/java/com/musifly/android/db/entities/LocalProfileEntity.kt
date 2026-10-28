package com.musifly.android.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_profile")
data class LocalProfileEntity(
    @PrimaryKey val id: String,
    val username: String?,
    val fullName: String?,
    val avatarUrl: String?,
    val bio: String?,
    val followersCount: Long,
    val followingCount: Long,
    val lastUpdated: Long = System.currentTimeMillis(),
    val lastSeen: Long? = null, // Timestamp in milliseconds
    @ColumnInfo(defaultValue = "0") val isPremium: Boolean = false
)
