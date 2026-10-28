package com.musifly.android.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import android.util.Log
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.PlaylistEntity
import com.musifly.android.db.entities.PlaylistSong
import com.musifly.android.db.entities.PlaylistSongMap
import com.musifly.android.db.entities.Song
import com.musifly.android.db.entities.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.util.UUID

/**
 * Service for importing and exporting playlists in mussifly JSON format.
 * 
 * mussifly format structure:
 * {
 *   "_meta": {
 *     "generated_by": "Musifly - Open Source Music Player",
 *     "version": "v1.0.0+100",
 *     "exportedAt": "2024-01-01T00:00:00.000Z",
 *     "note": "..."
 *   },
 *   "playlistName": "My Playlist",
 *   "mediaRanks": {...},
 *   "mediaItems": [
 *     {
 *       "title": "Song Title",
 *       "duration": 180,
 *       "permaURL": "https://...",
 *       "streamingURL": "https://...",
 *       "album": "Album Name",
 *       "artist": "Artist Name",
 *       "artURL": "https://..."
 *     }
 *   ]
 * }
 */
object PlaylistImportExportService {
    private const val TAG = "PlaylistImportExport"
    
    /**
     * Import state for progress tracking
     */
    data class ImportState(
        val totalItems: Int = 0,
        val importedItems: Int = 0,
        val failedItems: Int = 0,
        val isDone: Boolean = false,
        val isFailed: Boolean = false,
        val message: String = ""
    )
    
    /**
     * Import playlist from Bloomee JSON file
     */
    fun importPlaylist(
        context: Context,
        database: MusicDatabase,
        uri: Uri
    ): Flow<ImportState> = flow {
        try {
            emit(ImportState(
                message = "Reading file..."
            ))
            
            // Read JSON from URI
            val jsonString = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
            } ?: run {
                emit(ImportState(
                    isFailed = true,
                    message = "Failed to read file"
                ))
                return@flow
            }
            
            // Parse JSON
            val playlistJson = JSONObject(jsonString)
            
            // Validate structure
            if (!playlistJson.has("playlistName") || !playlistJson.has("mediaItems")) {
                emit(ImportState(
                    isFailed = true,
                    message = "Invalid mussifly playlist format"
                ))
                return@flow
            }
            
            val playlistName = playlistJson.getString("playlistName")
            val mediaItemsArray = playlistJson.getJSONArray("mediaItems")
            val totalItems = mediaItemsArray.length()
            
            if (totalItems == 0) {
                emit(ImportState(
                    isFailed = true,
                    message = "Playlist is empty"
                ))
                return@flow
            }
            
            emit(ImportState(
                totalItems = totalItems,
                message = "Importing playlist: $playlistName"
            ))
            
            // Check if playlist already exists and generate unique name
            var finalPlaylistName = playlistName
            var playlistExists = withContext(Dispatchers.IO) {
                database.playlistsByNameAsc().first().any { it.playlist.name == finalPlaylistName }
            }
            
            var counter = 1
            while (playlistExists) {
                finalPlaylistName = "${playlistName}_$counter"
                playlistExists = withContext(Dispatchers.IO) {
                    database.playlistsByNameAsc().first().any { it.playlist.name == finalPlaylistName }
                }
                counter++
            }
            
            // Create playlist
            val newPlaylist = PlaylistEntity(
                name = finalPlaylistName,
                createdAt = LocalDateTime.now(),
                lastUpdateTime = LocalDateTime.now(),
                isEditable = true,
                bookmarkedAt = LocalDateTime.now() // Important: Set this so playlist shows in library
            )
            
            withContext(Dispatchers.IO) {
                database.query { insert(newPlaylist) }
            }
            
            Log.d(TAG, "Created playlist: ${newPlaylist.id} - $finalPlaylistName")
            
            // Import songs
            var importedCount = 0
            var failedCount = 0
            
            for (i in 0 until totalItems) {
                try {
                    val mediaItemJson = mediaItemsArray.getJSONObject(i)
                    
                    // Extract song data from mussifly format
                    val title = mediaItemJson.getString("title")
                    val artist = mediaItemJson.optString("artist", "Unknown Artist")
                    val album = mediaItemJson.optString("album", "")
                    val duration = mediaItemJson.optInt("duration", 0)
                    val permaURL = mediaItemJson.optString("permaURL", "")
                    val artURL = mediaItemJson.optString("artURL", "")
                    
                    // Extract video ID from permaURL (YouTube format)
                    val videoId = extractVideoIdFromUrl(permaURL) ?: UUID.randomUUID().toString()
                    
                    // Create or find song in database
                    val songId = videoId
                    val songEntity = withContext(Dispatchers.IO) {
                        database.song(songId).firstOrNull()?.song
                    } ?: run {
                        // Create new song entity
                        SongEntity(
                            id = songId,
                            title = title,
                            duration = duration,
                            thumbnailUrl = artURL.takeIf { it.isNotEmpty() },
                            albumName = album.takeIf { it.isNotEmpty() },
                            inLibrary = LocalDateTime.now()
                        )
                    }
                    
                    // Insert song and add to playlist in a single transaction
                    // This ensures foreign key constraints are satisfied
                    // OnConflictStrategy.IGNORE will handle duplicate songs gracefully
                    withContext(Dispatchers.IO) {
                        database.transaction {
                            // Insert song (will ignore if already exists due to OnConflictStrategy.IGNORE)
                            insert(songEntity)
                            
                            // Add song to playlist
                            // The foreign key constraint will be satisfied because:
                            // 1. If song is new, it was just inserted above
                            // 2. If song exists, OnConflictStrategy.IGNORE means insert succeeded (no-op)
                            insert(
                                PlaylistSongMap(
                                    playlistId = newPlaylist.id,
                                    songId = songId,
                                    position = importedCount
                                )
                            )
                        }
                    }
                    
                    importedCount++
                    
                    emit(ImportState(
                        totalItems = totalItems,
                        importedItems = importedCount,
                        failedItems = failedCount,
                        message = "Imported $importedCount/$totalItems: $title"
                    ))
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to import song at index $i: ${e.message}", e)
                    failedCount++
                    emit(ImportState(
                        totalItems = totalItems,
                        importedItems = importedCount,
                        failedItems = failedCount,
                        message = "Failed to import song $i"
                    ))
                }
            }
            
            emit(ImportState(
                totalItems = totalItems,
                importedItems = importedCount,
                failedItems = failedCount,
                isDone = true,
                message = "Imported playlist: $finalPlaylistName ($importedCount songs)"
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error importing playlist: ${e.message}", e)
            emit(ImportState(
                isFailed = true,
                message = "Import failed: ${e.message}"
            ))
        }
    }
    
    /**
     * Export playlist to mussifly JSON format
     */
    suspend fun exportPlaylist(
        context: Context,
        database: MusicDatabase,
        playlistId: String,
        exportFolderUri: Uri? = null
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val playlist = database.playlist(playlistId).firstOrNull()
                ?: return@withContext Result.failure(Exception("Playlist not found"))
            
            val playlistSongs = database.playlistSongs(playlistId).first()
            
            if (playlistSongs.isEmpty()) {
                return@withContext Result.failure(Exception("Playlist is empty"))
            }
            
            // Build JSON object
            val playlistJson = JSONObject().apply {
                // Meta information
                put("_meta", JSONObject().apply {
                    put("generated_by", "Musifly - Open Source Music Player")
                    put("version", "1.0.0")
                    put("exportedAt", LocalDateTime.now().toString())
                    put("note", "This file is automatically generated by Musifly and is intended solely for importing playlists within the application.")
                })
                
                put("playlistName", playlist.playlist.name)
                put("mediaRanks", JSONObject()) // Empty for now
                
                // Media items
                val mediaItemsArray = JSONArray()
                playlistSongs.forEachIndexed { index, playlistSong ->
                    val song = playlistSong.song
                    val mediaItem = JSONObject().apply {
                        put("title", song.song.title)
                        put("duration", song.song.duration)
                        put("permaURL", "https://music.youtube.com/watch?v=${song.song.id}")
                        put("streamingURL", song.song.id) // Use song ID as streaming URL
                        put("album", song.song.albumName ?: "")
                        put("artist", song.artists.joinToString(", ") { it.name })
                        put("artURL", song.song.thumbnailUrl ?: "")
                    }
                    mediaItemsArray.put(mediaItem)
                }
                put("mediaItems", mediaItemsArray)
            }
            
            // Write to file
            val fileName = "${playlist.playlist.name.replace(Regex("[^a-zA-Z0-9]"), "_")}_mussiflyPlaylist.json"
            val jsonString = playlistJson.toString(2)
            
            // Save to user-selected folder or default Downloads folder
            val savedFile = if (exportFolderUri != null) {
                // User selected a custom folder - use SAF (Storage Access Framework)
                try {
                    val treeDocumentId = android.provider.DocumentsContract.getTreeDocumentId(exportFolderUri)
                    val fileUri = android.provider.DocumentsContract.createDocument(
                        context.contentResolver,
                        exportFolderUri,
                        "application/json",
                        fileName
                    )
                    
                    if (fileUri != null) {
                        context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                            outputStream.write(jsonString.toByteArray())
                        }
                        Log.d(TAG, "Saved playlist to custom folder: $fileUri")
                        fileUri
                    } else {
                        // Fallback to default location
                        Log.w(TAG, "Failed to create document in custom folder, using default location")
                        saveToDefaultLocation(context, fileName, jsonString)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save to custom folder: ${e.message}", e)
                    // Fallback to default location
                    saveToDefaultLocation(context, fileName, jsonString)
                }
            } else {
                // Save to default Downloads folder
                saveToDefaultLocation(context, fileName, jsonString)
            }
            
            Result.success(savedFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting playlist: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Save file to default Downloads/MusiFly folder
     */
    private fun saveToDefaultLocation(context: Context, fileName: String, jsonString: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+) - Use MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MusiFly")
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )
            
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }
                Log.d(TAG, "Saved playlist to Downloads: $uri")
                uri
            } else {
                // Fallback to cache directory
                val file = File(context.cacheDir, fileName)
                file.writeText(jsonString)
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }
        } else {
            // Android 9 and below - Use traditional file system
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val musiflyDir = File(downloadsDir, "MusiFly")
            if (!musiflyDir.exists()) {
                musiflyDir.mkdirs()
            }
            
            val file = File(musiflyDir, fileName)
            file.writeText(jsonString)
            Log.d(TAG, "Saved playlist to Downloads: ${file.absolutePath}")
            
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }
    }
    
    /**
     * Extract YouTube video ID from URL
     */
    private fun extractVideoIdFromUrl(url: String): String? {
        if (url.isEmpty()) return null
        
        return when {
            url.contains("youtube.com/watch?v=") -> {
                url.substringAfter("v=").substringBefore("&")
            }
            url.contains("youtu.be/") -> {
                url.substringAfter("youtu.be/").substringBefore("?")
            }
            url.contains("music.youtube.com/watch?v=") -> {
                url.substringAfter("v=").substringBefore("&")
            }
            else -> {
                // If it's already a video ID (11 characters)
                if (url.length == 11 && url.matches(Regex("[a-zA-Z0-9_-]+"))) {
                    url
                } else null
            }
        }
    }
}

