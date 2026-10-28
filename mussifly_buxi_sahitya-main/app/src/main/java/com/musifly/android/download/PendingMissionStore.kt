package com.musifly.android.download

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import java.io.File

/**
 * SQLite database for storing pending/paused downloads
 * This enables downloads to persist across app restarts
 */
class PendingMissionStore(private val context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val TAG = "PendingMissionStore"
        private const val DATABASE_NAME = "pending_downloads.db"
        private const val DATABASE_VERSION = 5  // Upgraded for songId support

        private const val TABLE_NAME = "pending_missions"
        private const val COL_ID = "_id"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_URLS = "urls"  // JSON array of URLs
        private const val COL_PATH = "path"
        private const val COL_TAG = "tag"   // audio or video
        private const val COL_KIND = "kind"
        private const val COL_LENGTH = "length"
        private const val COL_DONE = "done"
        private const val COL_THREAD_COUNT = "thread_count"
        private const val COL_TITLE = "title"
        private const val COL_ARTIST = "artist"
        private const val COL_THUMBNAIL_URL = "thumbnail_url"
        private const val COL_QUALITY = "quality"
        private const val COL_ERR_CODE = "err_code"
        private const val COL_ENQUEUED = "enqueued"
        private const val COL_NEAR_LENGTH = "near_length"
        private const val COL_MAX_RETRY = "max_retry"
        private const val COL_PAUSED_AT = "paused_at"  // Time when paused
        private const val COL_SAVED_DONE = "saved_done"  // Progress before pause
        private const val COL_SAVED_LENGTH = "saved_length"  // Length before pause
        private const val COL_IS_PAUSED = "is_paused"  // Paused state
        private const val COL_URI_STRING = "uri_string"  // Full URI string for MediaStore
        private const val COL_IS_MEDIASTORE = "is_mediastore"  // Whether this is a MediaStore URI
        private const val COL_SONG_ID = "song_id"  // ID of the song in the database
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER UNIQUE NOT NULL,
                $COL_URLS TEXT NOT NULL,
                $COL_PATH TEXT NOT NULL,
                $COL_TAG TEXT,
                $COL_KIND TEXT,
                $COL_LENGTH INTEGER DEFAULT 0,
                $COL_DONE INTEGER DEFAULT 0,
                $COL_THREAD_COUNT INTEGER DEFAULT 3,
                $COL_TITLE TEXT,
                $COL_ARTIST TEXT,
                $COL_THUMBNAIL_URL TEXT,
                $COL_QUALITY TEXT,
                $COL_ERR_CODE INTEGER DEFAULT 0,
                $COL_ENQUEUED INTEGER DEFAULT 1,
                $COL_NEAR_LENGTH INTEGER DEFAULT 0,
                $COL_MAX_RETRY INTEGER DEFAULT 3,
                $COL_PAUSED_AT INTEGER DEFAULT 0,
                $COL_SAVED_DONE INTEGER DEFAULT 0,
                $COL_SAVED_LENGTH INTEGER DEFAULT 0,
                $COL_IS_PAUSED INTEGER DEFAULT 0,
                $COL_URI_STRING TEXT,
                $COL_IS_MEDIASTORE INTEGER DEFAULT 0,
                $COL_SONG_ID TEXT
            )
        """.trimIndent()

        db.execSQL(createTable)
        Log.d(TAG, "Created pending missions table")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Upgrading database from $oldVersion to $newVersion")
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_SONG_ID TEXT")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to alter table, falling back to drop/create", e)
                db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
                onCreate(db)
            }
        } else {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
        }
    }

    /**
     * Save a pending mission to database
     */
    fun saveMission(mission: DownloadMission): Boolean {
        val db = writableDatabase

        try {
            // Convert URLs array to JSON
            val urlsJson = JSONArray(mission.urls.toList()).toString()

            // Determine if this is a MediaStore URI (content://) vs direct file (file://)
            val storageUri = mission.storage?.getUri()
            val isMediaStore = storageUri?.scheme == "content"
            val uriString = storageUri?.toString() ?: ""
            
            val values = ContentValues().apply {
                put(COL_TIMESTAMP, mission.timestamp)
                put(COL_URLS, urlsJson)
                put(COL_PATH, if (isMediaStore) "" else (storageUri?.path ?: ""))  // Only save path for file:// URIs
                put(COL_TAG, mission.storage?.getTag() ?: "")
                put(COL_KIND, mission.kind.toString())
                put(COL_LENGTH, mission.length)
                put(COL_DONE, mission.done)
                put(COL_THREAD_COUNT, mission.threadCount)
                put(COL_TITLE, mission.title)
                put(COL_ARTIST, mission.artist)
                put(COL_THUMBNAIL_URL, mission.thumbnailUrl)
                put(COL_QUALITY, mission.quality)
                put(COL_ERR_CODE, mission.errCode)
                put(COL_ENQUEUED, if (mission.enqueued) 1 else 0)
                put(COL_NEAR_LENGTH, mission.nearLength)
                put(COL_MAX_RETRY, mission.maxRetry)
                put(COL_PAUSED_AT, if (mission.isPaused) System.currentTimeMillis() else 0)
                put(COL_SAVED_DONE, mission.savedDone)
                put(COL_SAVED_LENGTH, mission.savedLength)
                put(COL_IS_PAUSED, if (mission.isPaused) 1 else 0)
                put(COL_URI_STRING, uriString)  // Save full URI for MediaStore support
                put(COL_IS_MEDIASTORE, if (isMediaStore) 1 else 0)
                put(COL_SONG_ID, mission.songId)
            }

            // Use REPLACE to update if exists, insert if not
            val result = db.insertWithOnConflict(
                TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )

            Log.d(TAG, "Saved mission: ${mission.title ?: mission.storage?.getName()} (timestamp: ${mission.timestamp}, done: ${mission.done}/${mission.length})")
            return result != -1L
        } catch (e: Exception) {
            Log.e(TAG, "Error saving mission", e)
            return false
        }
    }

    /**
     * Update mission progress
     */
    fun updateProgress(mission: DownloadMission) {
        val db = writableDatabase

        try {
            val values = ContentValues().apply {
                put(COL_DONE, mission.done)
                put(COL_LENGTH, mission.length)
                put(COL_ERR_CODE, mission.errCode)
            }

            db.update(
                TABLE_NAME,
                values,
                "$COL_TIMESTAMP = ?",
                arrayOf(mission.timestamp.toString())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating progress", e)
        }
    }

    /**
     * Load all pending missions from database
     */
    fun loadPendingMissions(): ArrayList<DownloadMission> {
        val missions = ArrayList<DownloadMission>()
        val db = readableDatabase

        try {
            val cursor = db.query(
                TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                "$COL_TIMESTAMP ASC"
            )

            cursor.use {
                while (it.moveToNext()) {
                    try {
                        val mission = DownloadMission()

                        mission.timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP))

                        // Parse URLs JSON array
                        val urlsJson = it.getString(it.getColumnIndexOrThrow(COL_URLS))
                        val urlsArray = JSONArray(urlsJson)
                        val urls = mutableListOf<String>()
                        for (i in 0 until urlsArray.length()) {
                            urls.add(urlsArray.getString(i))
                        }
                        mission.urls = urls.toTypedArray()

                        // Restore storage - try URI string first (MediaStore), then fall back to path (legacy)
                        val path = it.getString(it.getColumnIndexOrThrow(COL_PATH))
                        val tag = it.getString(it.getColumnIndexOrThrow(COL_TAG))
                        
                        // Try to get URI string and isMediaStore flag (may not exist in old DB)
                        val uriString = try {
                            it.getString(it.getColumnIndexOrThrow(COL_URI_STRING))
                        } catch (e: Exception) { null }
                        
                        val isMediaStore = try {
                            it.getInt(it.getColumnIndexOrThrow(COL_IS_MEDIASTORE)) == 1
                        } catch (e: Exception) { false }
                        
                        // Restore StoredFileHelper based on storage type
                        var storageRestored = false
                        if (!uriString.isNullOrEmpty() && isMediaStore) {
                            // MediaStore URI - use ContentResolver
                            try {
                                val uri = Uri.parse(uriString)
                                mission.storage = StoredFileHelper(context, uri, tag)
                                storageRestored = true
                                Log.d(TAG, "Restored MediaStore storage: $uriString")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to restore MediaStore URI: $uriString", e)
                            }
                        }
                        
                        if (!storageRestored && path.isNotEmpty()) {
                            // Legacy file path
                            val file = File(path)
                            mission.storage = StoredFileHelper(file, tag)
                            storageRestored = true
                            Log.d(TAG, "Restored file storage: $path")
                        }

                        mission.kind = it.getString(it.getColumnIndexOrThrow(COL_KIND)).firstOrNull() ?: 'v'
                        mission.length = it.getLong(it.getColumnIndexOrThrow(COL_LENGTH))
                        mission.done = it.getLong(it.getColumnIndexOrThrow(COL_DONE))
                        mission.threadCount = it.getInt(it.getColumnIndexOrThrow(COL_THREAD_COUNT))
                        mission.title = it.getString(it.getColumnIndexOrThrow(COL_TITLE))
                        mission.artist = it.getString(it.getColumnIndexOrThrow(COL_ARTIST))
                        mission.thumbnailUrl = it.getString(it.getColumnIndexOrThrow(COL_THUMBNAIL_URL))
                        mission.quality = it.getString(it.getColumnIndexOrThrow(COL_QUALITY))
                        mission.errCode = it.getInt(it.getColumnIndexOrThrow(COL_ERR_CODE))
                        mission.enqueued = it.getInt(it.getColumnIndexOrThrow(COL_ENQUEUED)) == 1
                        mission.nearLength = it.getLong(it.getColumnIndexOrThrow(COL_NEAR_LENGTH))
                        mission.maxRetry = it.getInt(it.getColumnIndexOrThrow(COL_MAX_RETRY))
                        
                        // Restore saved progress state
                        mission.savedDone = try {
                            it.getLong(it.getColumnIndexOrThrow(COL_SAVED_DONE))
                        } catch (e: Exception) { 0L }
                        
                        mission.savedLength = try {
                            it.getLong(it.getColumnIndexOrThrow(COL_SAVED_LENGTH))
                        } catch (e: Exception) { 0L }
                        
                        mission.isPaused = try {
                            it.getInt(it.getColumnIndexOrThrow(COL_IS_PAUSED)) == 1
                        } catch (e: Exception) { mission.done > 0 }

                        mission.songId = try {
                            it.getString(it.getColumnIndexOrThrow(COL_SONG_ID))
                        } catch (e: Exception) { null }

                        // Validate and add mission based on storage type
                        if (storageRestored && mission.storage != null) {
                            if (isMediaStore) {
                                // For MediaStore URIs, always add - we can't easily check if URI is still valid
                                // without context, and the download can be resumed
                                missions.add(mission)
                                Log.d(TAG, "Loaded MediaStore mission: ${mission.title} (${mission.done}/${mission.length})")
                            } else {
                                // For file paths, validate the file exists or parent directory exists
                                val file = mission.storage?.getUri()?.path?.let { p -> File(p) }
                                if (file != null) {
                                    if (file.exists()) {
                                        val stateFile = File(file.absolutePath + ".state")
                                        if (stateFile.exists()) {
                                            Log.d(TAG, "Found state file for: ${mission.title ?: file.name}")
                                        }
                                        missions.add(mission)
                                        Log.d(TAG, "Loaded pending mission: ${mission.title ?: file.name} (${mission.done}/${mission.length})")
                                    } else if (file.parentFile?.exists() == true) {
                                        // File doesn't exist but parent does - can still resume
                                        missions.add(mission)
                                        Log.d(TAG, "Loaded pending mission (file pending): ${mission.title}")
                                    } else {
                                        Log.d(TAG, "Skipping mission - path invalid: $path")
                                        deleteMission(mission.timestamp)
                                    }
                                } else {
                                    Log.d(TAG, "Skipping mission - no valid file path")
                                    deleteMission(mission.timestamp)
                                }
                            }
                        } else {
                            Log.d(TAG, "Skipping mission - storage not restored: ${mission.title}")
                            deleteMission(mission.timestamp)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading mission row", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading pending missions", e)
        }

        Log.d(TAG, "Loaded ${missions.size} pending missions")
        return missions
    }

    /**
     * Delete a mission from database
     */
    fun deleteMission(timestamp: Long): Boolean {
        val db = writableDatabase

        try {
            val rows = db.delete(
                TABLE_NAME,
                "$COL_TIMESTAMP = ?",
                arrayOf(timestamp.toString())
            )
            Log.d(TAG, "Deleted mission with timestamp: $timestamp (rows affected: $rows)")
            return rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting mission", e)
            return false
        }
    }

    /**
     * Delete a mission from database
     */
    fun deleteMission(mission: DownloadMission): Boolean {
        return deleteMission(mission.timestamp)
    }

    /**
     * Check if a mission exists
     */
    fun missionExists(timestamp: Long): Boolean {
        val db = readableDatabase

        try {
            val cursor = db.query(
                TABLE_NAME,
                arrayOf(COL_ID),
                "$COL_TIMESTAMP = ?",
                arrayOf(timestamp.toString()),
                null,
                null,
                null
            )

            val exists = cursor.count > 0
            cursor.close()
            return exists
        } catch (e: Exception) {
            Log.e(TAG, "Error checking mission existence", e)
            return false
        }
    }

    /**
     * Clear all pending missions
     */
    fun clearAll() {
        val db = writableDatabase

        try {
            db.delete(TABLE_NAME, null, null)
            Log.d(TAG, "Cleared all pending missions")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing missions", e)
        }
    }

    /**
     * Get count of pending missions
     */
    fun getCount(): Int {
        val db = readableDatabase

        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_NAME", null)
            cursor.use {
                if (it.moveToFirst()) {
                    return it.getInt(0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting count", e)
        }
        return 0
    }
}
