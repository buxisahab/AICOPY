package com.buxi.music.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_download")
data class DeviceDownloadEntity(
    @PrimaryKey val songId: String,
    val filePath: String,
    val downloadedAt: Long = System.currentTimeMillis()
)
