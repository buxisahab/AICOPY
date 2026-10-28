package com.musifly.android.download

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

/**
 * SQLite database for storing finished downloads
 * Adapted from NewPipe's FinishedMissionStore
 */
class FinishedMissionStore(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    
    companion object {
        private const val TAG = "FinishedMissionStore"
        private const val DATABASE_NAME = "downloads.db"
        private const val DATABASE_VERSION = 2
        
        private const val TABLE_NAME = "finished_missions"
        private const val COL_ID = "_id"
        private const val COL_NAME = "name"
        private const val COL_PATH = "path"
        private const val COL_URL = "url"
        private const val COL_KIND = "kind"
        private const val COL_LENGTH = "length"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_SONG_ID = "song_id"
    }
    
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_PATH TEXT NOT NULL,
                $COL_URL TEXT,
                $COL_KIND TEXT,
                $COL_LENGTH INTEGER,
                $COL_TIMESTAMP INTEGER,
                $COL_SONG_ID TEXT
            )
        """.trimIndent()
        
        db.execSQL(createTable)
    }
    
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Upgrading finished missions database from $oldVersion to $newVersion")
        if (oldVersion < 2) {
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
     * Load all finished missions from database
     */
    fun loadFinishedMissions(): ArrayList<FinishedMission> {
        val missions = ArrayList<FinishedMission>()
        val db = readableDatabase
        
        try {
            val cursor = db.query(
                TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                "$COL_TIMESTAMP DESC"
            )
            
            cursor.use {
                while (it.moveToNext()) {
                    try {
                        val mission = FinishedMission()
                        
                        val path = it.getString(it.getColumnIndexOrThrow(COL_PATH))
                        mission.storage = StoredFileHelper(
                            java.io.File(path),
                            it.getString(it.getColumnIndexOrThrow(COL_KIND))
                        )
                        
                        mission.source = it.getString(it.getColumnIndexOrThrow(COL_URL))
                        mission.length = it.getLong(it.getColumnIndexOrThrow(COL_LENGTH))
                        mission.timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP))
                        mission.kind = it.getString(it.getColumnIndexOrThrow(COL_KIND)).firstOrNull() ?: 'v'
                        mission.songId = try {
                            it.getString(it.getColumnIndexOrThrow(COL_SONG_ID))
                        } catch (e: Exception) { null }
                        
                        missions.add(mission)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading mission", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading finished missions", e)
        }
        
        return missions
    }
    
    /**
     * Add a finished mission to database
     */
    fun addFinishedMission(mission: DownloadMission) {
        val db = writableDatabase
        
        try {
            val values = ContentValues().apply {
                put(COL_NAME, mission.storage?.getName())
                put(COL_PATH, mission.storage?.getUri()?.path)
                put(COL_URL, mission.source)
                put(COL_KIND, mission.kind.toString())
                put(COL_LENGTH, mission.length)
                put(COL_TIMESTAMP, mission.timestamp)
                put(COL_SONG_ID, mission.songId)
            }
            
            db.insert(TABLE_NAME, null, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding finished mission", e)
        }
    }
    
    /**
     * Delete a mission from database
     */
    fun deleteMission(mission: Mission) {
        val db = writableDatabase
        
        try {
            db.delete(
                TABLE_NAME,
                "$COL_TIMESTAMP = ?",
                arrayOf(mission.timestamp.toString())
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting mission", e)
        }
    }
}
