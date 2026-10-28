package com.musifly.android.local

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.musifly.android.models.LocalSong
import com.musifly.android.utils.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loader class for scanning local music files from device storage using MediaStore
 */
class LocalMusicLoader(private val context: Context) {
    
    companion object {
        private const val TAG = "LocalMusicLoader"
        
        // Supported audio file formats
        private val SUPPORTED_FORMATS = setOf(
            "audio/mpeg",       // mp3
            "audio/mp4",        // m4a, aac
            "audio/x-m4a",      // m4a
            "audio/wav",        // wav
            "audio/x-wav",      // wav
            "audio/aac",        // aac
            "audio/flac",       // flac
            "audio/ogg",        // ogg
            "audio/x-flac"      // flac
        )
        
        // MediaStore projection - columns to fetch
        private val PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,                // File path
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.BITRATE
        )
        
        // Selection criteria - only music files
        private const val SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        
        // Sort order - by title A-Z
        private const val SORT_ORDER = "${MediaStore.Audio.Media.TITLE} ASC"
    }
    
    /**
     * Load all local music files from device storage
     * This function runs on background thread (Dispatchers.IO)
     * 
     * @return List of LocalSong objects
     */
    suspend fun loadLocalMusic(): List<LocalSong> = withContext(Dispatchers.IO) {
        // Check permission first
        if (!PermissionManager.hasStoragePermission(context)) {
            Log.w(TAG, "Storage permission not granted")
            return@withContext emptyList<LocalSong>()
        }
        
        val songs = mutableListOf<LocalSong>()
        val contentResolver: ContentResolver = context.contentResolver
        
        try {
            // Query MediaStore for audio files
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                PROJECTION,
                SELECTION,
                null,
                SORT_ORDER
            )?.use { cursor ->
                // Get column indices
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val bitrateColumn = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                
                Log.d(TAG, "Found ${cursor.count} audio files")
                
                // Iterate through all songs
                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "Unknown Title"
                        val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                        val album = cursor.getString(albumColumn) ?: "Unknown Album"
                        val path = cursor.getString(dataColumn) ?: continue
                        val duration = cursor.getLong(durationColumn)
                        val size = cursor.getLong(sizeColumn)
                        val albumId = cursor.getLong(albumIdColumn)
                        val dateAdded = cursor.getLong(dateAddedColumn)
                        val mimeType = cursor.getString(mimeTypeColumn) ?: "audio/mpeg"
                        val bitrate = if (bitrateColumn >= 0) cursor.getInt(bitrateColumn) else 0
                        
                        // Filter by supported formats
                        if (!SUPPORTED_FORMATS.contains(mimeType.lowercase())) {
                            Log.d(TAG, "Skipping unsupported format: $mimeType for $title")
                            continue
                        }
                        
                        // Generate album art URI
                        val albumArtUri = Uri.parse("content://media/external/audio/albumart/$albumId")
                        
                        // Create LocalSong object
                        val localSong = LocalSong(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            path = path,
                            duration = duration,
                            size = size,
                            albumId = albumId,
                            albumArtUri = albumArtUri,
                            dateAdded = dateAdded,
                            mimeType = mimeType,
                            bitrate = bitrate
                        )
                        
                        songs.add(localSong)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing song from cursor", e)
                    }
                }
            }
            
            Log.i(TAG, "Successfully loaded ${songs.size} local songs")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local music", e)
        }
        
        return@withContext songs
    }
    
    /**
     * Load local music by album
     */
    suspend fun loadMusicByAlbum(albumId: Long): List<LocalSong> = withContext(Dispatchers.IO) {
        if (!PermissionManager.hasStoragePermission(context)) {
            return@withContext emptyList<LocalSong>()
        }
        
        val selection = "$SELECTION AND ${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())
        
        val songs = mutableListOf<LocalSong>()
        val contentResolver: ContentResolver = context.contentResolver
        
        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                PROJECTION,
                selection,
                selectionArgs,
                MediaStore.Audio.Media.TRACK + " ASC"
            )?.use { cursor ->
                // Same parsing logic as loadLocalMusic
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val bitrateColumn = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                
                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "Unknown Title"
                        val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                        val album = cursor.getString(albumColumn) ?: "Unknown Album"
                        val path = cursor.getString(dataColumn) ?: continue
                        val duration = cursor.getLong(durationColumn)
                        val size = cursor.getLong(sizeColumn)
                        val dbAlbumId = cursor.getLong(albumIdColumn)
                        val dateAdded = cursor.getLong(dateAddedColumn)
                        val mimeType = cursor.getString(mimeTypeColumn) ?: "audio/mpeg"
                        val bitrate = if (bitrateColumn >= 0) cursor.getInt(bitrateColumn) else 0
                        
                        if (!SUPPORTED_FORMATS.contains(mimeType.lowercase())) {
                            continue
                        }
                        
                        val albumArtUri = Uri.parse("content://media/external/audio/albumart/$dbAlbumId")
                        
                        songs.add(
                            LocalSong(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                path = path,
                                duration = duration,
                                size = size,
                                albumId = dbAlbumId,
                                albumArtUri = albumArtUri,
                                dateAdded = dateAdded,
                                mimeType = mimeType,
                                bitrate = bitrate
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing song from cursor", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading music by album", e)
        }
        
        return@withContext songs
    }
    
    /**
     * Search local music by query
     */
    suspend fun searchLocalMusic(query: String): List<LocalSong> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            return@withContext loadLocalMusic()
        }
        
        if (!PermissionManager.hasStoragePermission(context)) {
            return@withContext emptyList<LocalSong>()
        }
        
        val selection = "$SELECTION AND (" +
                "${MediaStore.Audio.Media.TITLE} LIKE ? OR " +
                "${MediaStore.Audio.Media.ARTIST} LIKE ? OR " +
                "${MediaStore.Audio.Media.ALBUM} LIKE ?)"
        
        val searchPattern = "%$query%"
        val selectionArgs = arrayOf(searchPattern, searchPattern, searchPattern)
        
        val songs = mutableListOf<LocalSong>()
        val contentResolver: ContentResolver = context.contentResolver
        
        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                PROJECTION,
                selection,
                selectionArgs,
                SORT_ORDER
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val bitrateColumn = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                
                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: "Unknown Title"
                        val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                        val album = cursor.getString(albumColumn) ?: "Unknown Album"
                        val path = cursor.getString(dataColumn) ?: continue
                        val duration = cursor.getLong(durationColumn)
                        val size = cursor.getLong(sizeColumn)
                        val dbAlbumId = cursor.getLong(albumIdColumn)
                        val dateAdded = cursor.getLong(dateAddedColumn)
                        val mimeType = cursor.getString(mimeTypeColumn) ?: "audio/mpeg"
                        val bitrate = if (bitrateColumn >= 0) cursor.getInt(bitrateColumn) else 0
                        
                        if (!SUPPORTED_FORMATS.contains(mimeType.lowercase())) {
                            continue
                        }
                        
                        val albumArtUri = Uri.parse("content://media/external/audio/albumart/$dbAlbumId")
                        
                        songs.add(
                            LocalSong(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                path = path,
                                duration = duration,
                                size = size,
                                albumId = dbAlbumId,
                                albumArtUri = albumArtUri,
                                dateAdded = dateAdded,
                                mimeType = mimeType,
                                bitrate = bitrate
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing song from cursor", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching local music", e)
        }
        
        return@withContext songs
    }
}
