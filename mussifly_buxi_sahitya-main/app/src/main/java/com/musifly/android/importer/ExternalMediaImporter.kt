package com.musifly.android.importer

import android.util.Log
import com.musifly.android.models.MediaMetadata
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.streamingengines.spotify.SpotifyAPI
import com.musifly.android.utils.UrlChecker
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.PlaylistEntity
import com.musifly.android.sync.repository.PlaylistRepository
import com.musifly.android.sync.UserSessionManager
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.SongItem
import com.musifly.innertube.models.YTItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

/**
 * External Media Importer
 * Converted from Bloomee Music Player (Flutter)
 * 
 * Features:
 * - Import playlists from Spotify
 * - Import albums from Spotify
 * - Import playlists from YouTube
 * - Import playlists from YouTube Music
 * - Import single tracks from Spotify, YouTube, and YouTube Music
 */
object ExternalMediaImporter {
    private const val TAG = "ExternalMediaImporter"
    
    private val spotifyAPI = SpotifyAPI()
    
    /**
     * Import state data class
     */
    data class ImporterState(
        val totalItems: Int = 0,
        val importedItems: Int = 0,
        val failedItems: Int = 0,
        val isDone: Boolean = false,
        val isFailed: Boolean = false,
        val message: String = "",
        val importedSongs: List<MediaMetadata> = emptyList(),
        val playlistTitle: String = ""
    )
    
    /**
     * Import YouTube playlist
     */
    fun importYouTubePlaylist(
        url: String, 
        database: MusicDatabase,
        playlistRepository: PlaylistRepository? = null
    ): Flow<ImporterState> = flow {
        try {
            val playlistId = UrlChecker.extractYoutubePlaylistId(url)
            if (playlistId == null) {
                emit(ImporterState(
                    isFailed = true,
                    message = "Invalid YouTube playlist URL"
                ))
                return@flow
            }
            
            Log.d(TAG, "Importing YouTube playlist: $playlistId")
            
            emit(ImporterState(
                message = "Importing Playlist...with ID: $playlistId"
            ))
            
            val result = YouTube.playlist(playlistId).getOrNull()
            if (result == null) {
                emit(ImporterState(
                    isFailed = true,
                    message = "Failed to fetch playlist"
                ))
                return@flow
            }
            
            val playlistTitle = result.playlist?.title ?: "Unknown Playlist"
            val songs = result.songs?.toMutableList() ?: mutableListOf()
            
            // Handle continuation
            var continuation = result.songsContinuation
            while (continuation != null) {
                val continuationResult = YouTube.playlistContinuation(continuation).getOrNull()
                if (continuationResult == null) break
                continuationResult.songs?.let { songs.addAll(it) }
                continuation = continuationResult.continuation
            }
            
            val totalItems = songs.size
            
            // Create playlist (use PlaylistRepository if logged in for Firestore sync)
            val playlistName = "${playlistTitle} (YouTube)"
            val dbPlaylistId: String
            
            if (UserSessionManager.isLoggedIn() && playlistRepository != null) {
                // Use PlaylistRepository to create user playlist (syncs to Firestore)
                val createResult = playlistRepository.createPlaylist(playlistName)
                dbPlaylistId = createResult.getOrElse {
                    // Fallback to local-only if Firestore creation fails
                    Timber.tag(TAG).w(it, "Failed to create playlist in Firestore, creating locally")
                    val fallbackPlaylist = PlaylistEntity(
                        name = playlistName,
                        createdAt = LocalDateTime.now(),
                        lastUpdateTime = LocalDateTime.now(),
                        isEditable = true,
                        bookmarkedAt = LocalDateTime.now()
                    )
                    withContext(Dispatchers.IO) {
                        database.transaction {
                            insert(fallbackPlaylist)
                        }
                    }
                    fallbackPlaylist.id
                }
                Timber.tag(TAG).d("✅ Created user playlist in Firestore: $dbPlaylistId - $playlistName")
            } else {
                // Guest mode: Create local playlist only
                val newPlaylist = PlaylistEntity(
                    name = playlistName,
                    createdAt = LocalDateTime.now(),
                    lastUpdateTime = LocalDateTime.now(),
                    isEditable = true,
                    bookmarkedAt = LocalDateTime.now()
                )
                withContext(Dispatchers.IO) {
                    database.transaction {
                        insert(newPlaylist)
                    }
                }
                dbPlaylistId = newPlaylist.id
                Timber.tag(TAG).d("✅ Created local playlist (guest mode): $dbPlaylistId - $playlistName")
            }
            Log.d(TAG, "Created playlist: $playlistName (ID: $dbPlaylistId)")
            
            // Wait a bit for transaction to complete
            kotlinx.coroutines.delay(200)
            
            // Get the playlist with proper error handling
            var playlist = database.playlist(dbPlaylistId).firstOrNull()
            var retryCount = 0
            while (playlist == null && retryCount < 5) {
                kotlinx.coroutines.delay(100)
                try {
                    playlist = database.playlist(dbPlaylistId).firstOrNull()
                } catch (e: Exception) {
                    Log.e(TAG, "Error retrieving playlist: ${e.message}", e)
                }
                retryCount++
            }
            
            emit(ImporterState(
                totalItems = totalItems,
                message = "Importing Playlist: $playlistName"
            ))
            
            val importedSongsList = mutableListOf<MediaMetadata>()
            var importedCount = 0
            
            if (playlist != null) {
                for (song in songs) {
                    try {
                        // Null safety checks
                        val title = song.title ?: "Unknown Title"
                        val metadata = song.toMediaMetadata()
                        
                        // Insert song to database with error handling
                        try {
                            database.insert(metadata)
                            importedSongsList.add(metadata)
                            importedCount++
                            
                            // Song added to local DB only (batch sync at end)
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Error inserting song '$title': ${e.message}", e)
                            // Continue with next song instead of crashing
                        }
                        
                        // Emit progress update
                        emit(ImporterState(
                            totalItems = totalItems,
                            importedItems = importedCount,
                            message = "$importedCount/$totalItems - $title",
                            importedSongs = importedSongsList.toList()
                        ))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing song: ${e.message}", e)
                        // Continue with next song instead of crashing
                    }
                }
                
                // Batch add songs to playlist (Firestore sync + Local/Main tables)
                if (importedSongsList.isNotEmpty()) {
                    try {
                        val songIds = importedSongsList.mapNotNull { it.id }
                        if (songIds.isNotEmpty()) {
                            if (UserSessionManager.isLoggedIn() && playlistRepository != null) {
                                // Batch sync to Firestore
                                playlistRepository.addSongsToPlaylist(dbPlaylistId, songIds).fold(
                                    onSuccess = {
                                        Timber.tag(TAG).d("✅ Batch added ${songIds.size} songs to Firestore playlist: $playlistName")
                                    },
                                    onFailure = { e ->
                                        Timber.tag(TAG).w(e, "⚠️ Failed to batch add songs to Firestore playlist: $playlistName")
                                    }
                                )
                            } else {
                                // Guest mode or no repo - direct DB add
                                database.addSongToPlaylist(playlist, songIds)
                                Log.d(TAG, "Added ${songIds.size} songs to local playlist: $playlistName")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding songs to playlist: ${e.message}", e)
                        emit(ImporterState(
                            totalItems = totalItems,
                            importedItems = importedCount,
                            isDone = true,
                            message = "Imported ${importedCount} songs but failed to add to playlist: ${e.message}",
                            importedSongs = importedSongsList.toList(),
                            playlistTitle = playlistName
                        ))
                        return@flow
                    }
                }
            } else {
                Log.e(TAG, "Failed to retrieve playlist after creation")
                emit(ImporterState(
                    isFailed = true,
                    message = "Failed to retrieve playlist after creation"
                ))
                return@flow
            }
            
            emit(ImporterState(
                totalItems = totalItems,
                importedItems = importedCount,
                isDone = true,
                message = "Imported Playlist: $playlistName",
                importedSongs = importedSongsList.toList(),
                playlistTitle = playlistName
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error importing YouTube playlist: ${e.message}", e)
            emit(ImporterState(
                isFailed = true,
                message = "Failed to import Playlist: ${e.message}"
            ))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Import YouTube Music playlist
     */
    fun importYouTubeMusicPlaylist(
        url: String, 
        database: MusicDatabase,
        playlistRepository: PlaylistRepository? = null
    ): Flow<ImporterState> = flow {
        try {
            val playlistId = UrlChecker.extractYTMusicPlaylistId(url)
            if (playlistId == null) {
                emit(ImporterState(
                    isFailed = true,
                    message = "Invalid YouTube Music playlist URL"
                ))
                return@flow
            }
            
            Log.d(TAG, "Importing YouTube Music playlist: $playlistId")
            
            emit(ImporterState(
                message = "Importing Playlist...with ID: $playlistId"
            ))
            
            // YouTube Music playlists are handled the same way as YouTube playlists
            val result = YouTube.playlist(playlistId).getOrNull()
            if (result == null) {
                emit(ImporterState(
                    isFailed = true,
                    message = "Failed to fetch playlist"
                ))
                return@flow
            }
            
            val playlistTitle = result.playlist?.title ?: "Unknown Playlist"
            val songs = result.songs?.toMutableList() ?: mutableListOf()
            
            // Handle continuation
            var continuation = result.songsContinuation
            while (continuation != null) {
                val continuationResult = YouTube.playlistContinuation(continuation).getOrNull()
                if (continuationResult == null) break
                continuationResult.songs?.let { songs.addAll(it) }
                continuation = continuationResult.continuation
            }
            
            val totalItems = songs.size
            
            // Create playlist (use PlaylistRepository if logged in for Firestore sync)
            val playlistName = "${playlistTitle} (YouTube Music)"
            val dbPlaylistId: String
            
            if (UserSessionManager.isLoggedIn() && playlistRepository != null) {
                // Use PlaylistRepository to create user playlist (syncs to Firestore)
                val createResult = playlistRepository.createPlaylist(playlistName)
                dbPlaylistId = createResult.getOrElse {
                    // Fallback to local-only if Firestore creation fails
                    Timber.tag(TAG).w(it, "Failed to create playlist in Firestore, creating locally")
                    val fallbackPlaylist = PlaylistEntity(
                        name = playlistName,
                        createdAt = LocalDateTime.now(),
                        lastUpdateTime = LocalDateTime.now(),
                        isEditable = true,
                        bookmarkedAt = LocalDateTime.now()
                    )
                    withContext(Dispatchers.IO) {
                        database.transaction {
                            insert(fallbackPlaylist)
                        }
                    }
                    fallbackPlaylist.id
                }
                Timber.tag(TAG).d("✅ Created user playlist in Firestore: $dbPlaylistId - $playlistName")
            } else {
                // Guest mode: Create local playlist only
                val newPlaylist = PlaylistEntity(
                    name = playlistName,
                    createdAt = LocalDateTime.now(),
                    lastUpdateTime = LocalDateTime.now(),
                    isEditable = true,
                    bookmarkedAt = LocalDateTime.now()
                )
                withContext(Dispatchers.IO) {
                    database.transaction {
                        insert(newPlaylist)
                    }
                }
                dbPlaylistId = newPlaylist.id
                Timber.tag(TAG).d("✅ Created local playlist (guest mode): $dbPlaylistId - $playlistName")
            }
            Log.d(TAG, "Created playlist: $playlistName (ID: $dbPlaylistId)")
            
            // Wait a bit for transaction to complete
            kotlinx.coroutines.delay(200)
            
            // Get the playlist with proper error handling
            var playlist = database.playlist(dbPlaylistId).firstOrNull()
            var retryCount = 0
            while (playlist == null && retryCount < 5) {
                kotlinx.coroutines.delay(100)
                try {
                    playlist = database.playlist(dbPlaylistId).firstOrNull()
                } catch (e: Exception) {
                    Log.e(TAG, "Error retrieving playlist: ${e.message}", e)
                }
                retryCount++
            }
            
            emit(ImporterState(
                totalItems = totalItems,
                message = "Importing Playlist: $playlistName"
            ))
            
            val importedSongsList = mutableListOf<MediaMetadata>()
            var importedCount = 0
            
            if (playlist != null) {
                for (song in songs) {
                    try {
                        // Null safety checks
                        val title = song.title ?: "Unknown Title"
                        val metadata = song.toMediaMetadata()
                        
                        // Insert song to database with error handling
                        try {
                            database.insert(metadata)
                            importedSongsList.add(metadata)
                            importedCount++
                            
                            // Song added to local DB only (batch sync at end)
                            
                        } catch (e: Exception) {
                            Log.e(TAG, "Error inserting song '$title': ${e.message}", e)
                            // Continue with next song instead of crashing
                        }
                        
                        // Emit progress update
                        emit(ImporterState(
                            totalItems = totalItems,
                            importedItems = importedCount,
                            message = "$importedCount/$totalItems - $title",
                            importedSongs = importedSongsList.toList()
                        ))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing song: ${e.message}", e)
                        // Continue with next song instead of crashing
                    }
                }
                
                // Batch add songs to playlist (Firestore sync + Local/Main tables)
                if (importedSongsList.isNotEmpty()) {
                    try {
                        val songIds = importedSongsList.mapNotNull { it.id }
                        if (songIds.isNotEmpty()) {
                            if (UserSessionManager.isLoggedIn() && playlistRepository != null) {
                                // Batch sync to Firestore
                                playlistRepository.addSongsToPlaylist(dbPlaylistId, songIds).fold(
                                    onSuccess = {
                                        Timber.tag(TAG).d("✅ Batch added ${songIds.size} songs to Firestore playlist: $playlistName")
                                    },
                                    onFailure = { e ->
                                        Timber.tag(TAG).w(e, "⚠️ Failed to batch add songs to Firestore playlist: $playlistName")
                                    }
                                )
                            } else {
                                // Guest mode or no repo - direct DB add
                                database.addSongToPlaylist(playlist, songIds)
                                Log.d(TAG, "Added ${songIds.size} songs to local playlist: $playlistName")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding songs to playlist: ${e.message}", e)
                        emit(ImporterState(
                            totalItems = totalItems,
                            importedItems = importedCount,
                            isDone = true,
                            message = "Imported ${importedCount} songs but failed to add to playlist: ${e.message}",
                            importedSongs = importedSongsList.toList(),
                            playlistTitle = playlistName
                        ))
                        return@flow
                    }
                }
            } else {
                Log.e(TAG, "Failed to retrieve playlist after creation")
                emit(ImporterState(
                    isFailed = true,
                    message = "Failed to retrieve playlist after creation"
                ))
                return@flow
            }
            
            emit(ImporterState(
                totalItems = totalItems,
                importedItems = importedCount,
                isDone = true,
                message = "Imported Playlist: $playlistName",
                importedSongs = importedSongsList.toList(),
                playlistTitle = playlistName
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error importing YouTube Music playlist: ${e.message}", e)
            emit(ImporterState(
                isFailed = true,
                message = "Failed to import Playlist: ${e.message}"
            ))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Import Spotify playlist
     */
    fun importSpotifyPlaylist(
        url: String, 
        database: MusicDatabase, 
        playlistId: String? = null,
        playlistRepository: PlaylistRepository? = null
    ): Flow<ImporterState> = flow {
        try {
            val finalPlaylistId = playlistId ?: UrlChecker.extractSpotifyPlaylistId(url)
            if (finalPlaylistId == null) {
                emit(ImporterState(
                    isFailed = true,
                    message = "Invalid Spotify playlist URL"
                ))
                return@flow
            }
            
            Log.d(TAG, "Importing Spotify playlist: $finalPlaylistId")
            
            emit(ImporterState(
                message = "Getting Spotify playlist..."
            ))
            
            val data = withContext(Dispatchers.IO) {
                spotifyAPI.getPlaylistDetails(finalPlaylistId)
            }
            
            if (data.containsKey("error")) {
                emit(ImporterState(
                    isFailed = true,
                    message = "Failed to get playlist: ${data["error"]}"
                ))
                return@flow
            }
            
            val originalTitle = data["title"] as? String ?: "Unknown Playlist"
            val playlistTitle = "Spotify Playlist - $originalTitle"
            val tracks = data["songs"] as? List<Map<String, Any>> ?: emptyList()
            
            if (tracks.isEmpty()) {
                emit(ImporterState(
                    isFailed = true,
                    message = "Playlist is empty!!"
                ))
                return@flow
            }
            
            val totalItems = tracks.size
            
            // Create playlist (use PlaylistRepository if logged in for Firestore sync)
            val dbPlaylistId: String
            try {
                if (UserSessionManager.isLoggedIn() && playlistRepository != null) {
                    // Use PlaylistRepository to create user playlist (syncs to Firestore)
                    val createResult = playlistRepository.createPlaylist(playlistTitle)
                    dbPlaylistId = createResult.getOrElse {
                        // Fallback to local-only if Firestore creation fails
                        Timber.tag(TAG).w(it, "Failed to create playlist in Firestore, creating locally")
                        val fallbackPlaylist = PlaylistEntity(
                            name = playlistTitle,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true
                        )
                        withContext(Dispatchers.IO) {
                            database.transaction {
                                insert(fallbackPlaylist)
                            }
                        }
                        fallbackPlaylist.id
                    }
                    Timber.tag(TAG).d("✅ Created user playlist in Firestore: $dbPlaylistId - $playlistTitle")
                } else {
                    // Guest mode: Create local playlist only
                    dbPlaylistId = UUID.randomUUID().toString()
                    database.transaction {
                        insert(
                            PlaylistEntity(
                                id = dbPlaylistId,
                                name = playlistTitle,
                                bookmarkedAt = LocalDateTime.now(),
                                isEditable = true
                            )
                        )
                    }
                    Log.d(TAG, "Created local playlist (guest mode): $playlistTitle (ID: $dbPlaylistId)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating playlist: ${e.message}", e)
                emit(ImporterState(
                    isFailed = true,
                    message = "Failed to create playlist: ${e.message}"
                ))
                return@flow
            }
            
            // Wait a bit for transaction to complete
            kotlinx.coroutines.delay(200)
            
            // Get the playlist with proper error handling
            var playlist = database.playlist(dbPlaylistId).firstOrNull()
            var retryCount = 0
            while (playlist == null && retryCount < 5) {
                kotlinx.coroutines.delay(100)
                try {
                    playlist = database.playlist(dbPlaylistId).firstOrNull()
                } catch (e: Exception) {
                    Log.e(TAG, "Error retrieving playlist: ${e.message}", e)
                }
                retryCount++
            }
            
            val importedSongsList = mutableListOf<MediaMetadata>()
            var importedCount = 0
            
            if (playlist != null) {
                // Convert Spotify tracks to MediaMetadata and search for YouTube equivalents
                for ((index, track) in tracks.withIndex()) {
                    try {
                        val title = track["title"] as? String ?: ""
                        val artist = track["artist"] as? String ?: ""
                        
                        if (title.isNotEmpty()) {
                            try {
                                // Search for track on YouTube Music with error handling
                                val searchQuery = "$title $artist".trim()
                                val searchResult = YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                
                                val firstResult = searchResult?.items?.firstOrNull()
                                if (firstResult is SongItem) {
                                    try {
                                        val metadata = firstResult.toMediaMetadata()
                                        
                                        // Insert song to database with error handling
                                        try {
                                            database.insert(metadata)
                                            importedSongsList.add(metadata)
                                            importedCount++
                                            
                                        // Song added to local DB only (batch sync at end)
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error inserting song '$title': ${e.message}", e)
                                            // Continue with next song
                                        }
                                        
                                        // Emit progress update
                                        val songTitle = firstResult.title ?: title
                                        emit(ImporterState(
                                            totalItems = totalItems,
                                            importedItems = importedCount,
                                            message = "Importing($importedCount/$totalItems): $songTitle",
                                            importedSongs = importedSongsList.toList()
                                        ))
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error converting song to metadata: ${e.message}", e)
                                        // Continue with next song
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error searching for track '$title': ${e.message}", e)
                                // Continue with next song instead of crashing
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing track: ${e.message}", e)
                        // Continue with next song instead of crashing
                    }
                }
                
                // Batch add songs to playlist (Firestore sync + Local/Main tables)
                if (importedSongsList.isNotEmpty()) {
                    try {
                        val songIds = importedSongsList.mapNotNull { it.id }
                        if (songIds.isNotEmpty()) {
                            if (UserSessionManager.isLoggedIn() && playlistRepository != null) {
                                // Batch sync to Firestore and Local Cache
                                playlistRepository.addSongsToPlaylist(dbPlaylistId, songIds).fold(
                                    onSuccess = {
                                        Timber.tag(TAG).d("✅ Batch added ${songIds.size} songs to Firestore playlist: $playlistTitle")
                                    },
                                    onFailure = { e ->
                                        Timber.tag(TAG).w(e, "⚠️ Failed to batch add songs to Firestore playlist: $playlistTitle")
                                    }
                                )
                            } else {
                                // Guest mode or no repo - direct DB add
                                database.addSongToPlaylist(playlist, songIds)
                                Log.d(TAG, "Added ${songIds.size} songs to local playlist: $playlistTitle")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding songs to playlist: ${e.message}", e)
                        emit(ImporterState(
                            totalItems = totalItems,
                            importedItems = importedCount,
                            isDone = true,
                            message = "Imported ${importedCount} songs but failed to add to playlist: ${e.message}",
                            importedSongs = importedSongsList.toList(),
                            playlistTitle = playlistTitle
                        ))
                        return@flow
                    }
                }
            } else {
                Log.e(TAG, "Failed to retrieve playlist after creation")
                emit(ImporterState(
                    isFailed = true,
                    message = "Failed to retrieve playlist after creation"
                ))
                return@flow
            }
            
            emit(ImporterState(
                totalItems = totalItems,
                importedItems = importedCount,
                isDone = true,
                message = "Imported Playlist: $playlistTitle",
                importedSongs = importedSongsList.toList(),
                playlistTitle = playlistTitle
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error importing Spotify playlist: ${e.message}", e)
            emit(ImporterState(
                isFailed = true,
                message = "Failed to import Playlist: ${e.message}"
            ))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Import Spotify album
     */
    fun importSpotifyAlbum(url: String, database: MusicDatabase, albumId: String? = null): Flow<ImporterState> = flow {
        try {
            val finalAlbumId = albumId ?: UrlChecker.extractSpotifyAlbumId(url)
            if (finalAlbumId == null) {
                emit(ImporterState(
                    isFailed = true,
                    message = "Invalid Spotify album URL"
                ))
                return@flow
            }
            
            Log.d(TAG, "Importing Spotify album: $finalAlbumId")
            
            emit(ImporterState(
                message = "Getting Spotify album..."
            ))
            
            val data = withContext(Dispatchers.IO) {
                spotifyAPI.getAlbumDetails(finalAlbumId)
            }
            
            if (data.containsKey("error")) {
                emit(ImporterState(
                    isFailed = true,
                    message = "Failed to get album: ${data["error"]}"
                ))
                return@flow
            }
            
            val originalTitle = data["title"] as? String ?: "Unknown Album"
            val albumTitle = "Spotify Album - $originalTitle"
            val tracks = data["songs"] as? List<Map<String, Any>> ?: emptyList()
            
            if (tracks.isEmpty()) {
                emit(ImporterState(
                    isFailed = true,
                    message = "Album is empty!!"
                ))
                return@flow
            }
            
            val totalItems = tracks.size
            
            // Create playlist in database (like bloomee app does)
            val dbPlaylistId = UUID.randomUUID().toString()
            try {
                database.transaction {
                    insert(
                        PlaylistEntity(
                            id = dbPlaylistId,
                            name = albumTitle,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true
                        )
                    )
                }
                Log.d(TAG, "Created playlist: $albumTitle (ID: $dbPlaylistId)")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating playlist: ${e.message}", e)
                emit(ImporterState(
                    isFailed = true,
                    message = "Failed to create playlist: ${e.message}"
                ))
                return@flow
            }
            
            // Wait a bit for transaction to complete
            kotlinx.coroutines.delay(200)
            
            // Get the playlist with proper error handling
            var playlist = database.playlist(dbPlaylistId).firstOrNull()
            var retryCount = 0
            while (playlist == null && retryCount < 5) {
                kotlinx.coroutines.delay(100)
                try {
                    playlist = database.playlist(dbPlaylistId).firstOrNull()
                } catch (e: Exception) {
                    Log.e(TAG, "Error retrieving playlist: ${e.message}", e)
                }
                retryCount++
            }
            
            val importedSongsList = mutableListOf<MediaMetadata>()
            var importedCount = 0
            
            if (playlist != null) {
                // Convert Spotify tracks to MediaMetadata and search for YouTube equivalents
                for (track in tracks) {
                    try {
                        val title = track["title"] as? String ?: ""
                        val artist = track["artist"] as? String ?: ""
                        
                        if (title.isNotEmpty()) {
                            try {
                                // Search for track on YouTube Music with error handling
                                val searchQuery = "$title $artist".trim()
                                val searchResult = YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                
                                val firstResult = searchResult?.items?.firstOrNull()
                                if (firstResult is SongItem) {
                                    try {
                                        val metadata = firstResult.toMediaMetadata()
                                        
                                        // Insert song to database with error handling
                                        try {
                                            database.insert(metadata)
                                            importedSongsList.add(metadata)
                                            importedCount++
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error inserting song '$title': ${e.message}", e)
                                            // Continue with next song
                                        }
                                        
                                        // Emit progress update
                                        val songTitle = firstResult.title ?: title
                                        emit(ImporterState(
                                            totalItems = totalItems,
                                            importedItems = importedCount,
                                            message = "Importing($importedCount/$totalItems): $songTitle",
                                            importedSongs = importedSongsList.toList()
                                        ))
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error converting song to metadata: ${e.message}", e)
                                        // Continue with next song
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error searching for track '$title': ${e.message}", e)
                                // Continue with next song instead of crashing
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing track: ${e.message}", e)
                        // Continue with next song instead of crashing
                    }
                }
                
                // Add all songs to playlist at once with error handling
                if (importedSongsList.isNotEmpty()) {
                    try {
                        val songIds = importedSongsList.mapNotNull { it.id }
                        if (songIds.isNotEmpty()) {
                            database.addSongToPlaylist(playlist, songIds)
                            Log.d(TAG, "Added ${songIds.size} songs to playlist: $albumTitle")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding songs to playlist: ${e.message}", e)
                        emit(ImporterState(
                            totalItems = totalItems,
                            importedItems = importedCount,
                            isDone = true,
                            message = "Imported ${importedCount} songs but failed to add to playlist: ${e.message}",
                            importedSongs = importedSongsList.toList(),
                            playlistTitle = albumTitle
                        ))
                        return@flow
                    }
                }
            } else {
                Log.e(TAG, "Failed to retrieve playlist after creation")
                emit(ImporterState(
                    isFailed = true,
                    message = "Failed to retrieve playlist after creation"
                ))
                return@flow
            }
            
            emit(ImporterState(
                totalItems = totalItems,
                importedItems = importedCount,
                isDone = true,
                message = "Imported Album: $albumTitle",
                importedSongs = importedSongsList.toList(),
                playlistTitle = albumTitle
            ))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error importing Spotify album: ${e.message}", e)
            emit(ImporterState(
                isFailed = true,
                message = "Failed to import Album: ${e.message}"
            ))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Import single track from Spotify
     */
    suspend fun importSpotifyTrack(url: String): MediaMetadata? = withContext(Dispatchers.IO) {
        try {
            val trackId = UrlChecker.extractSpotifyTrackId(url)
            if (trackId == null) {
                Log.e(TAG, "Invalid Spotify track URL")
                return@withContext null
            }
            
            val data = spotifyAPI.getTrackDetails(trackId)
            if (data.containsKey("error")) {
                Log.e(TAG, "Failed to get track: ${data["error"]}")
                return@withContext null
            }
            
            val title = data["title"] as? String ?: ""
            val artist = data["artist"] as? String ?: ""
            
            if (title.isNotEmpty()) {
                // Search for track on YouTube Music
                val searchQuery = "$title $artist".trim()
                val searchResult = YouTube.search(searchQuery, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                
                val firstResult = searchResult?.items?.firstOrNull()
                if (firstResult is com.musifly.innertube.models.SongItem) {
                    return@withContext firstResult.toMediaMetadata()
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error importing Spotify track: ${e.message}", e)
            null
        }
    }
    
    /**
     * Import single track from YouTube
     */
    suspend fun importYouTubeTrack(url: String): MediaMetadata? = withContext(Dispatchers.IO) {
        try {
            val videoId = UrlChecker.extractVideoId(url)
            if (videoId == null) {
                Log.e(TAG, "Invalid YouTube URL")
                return@withContext null
            }
            
            // Use YouTube search - FILTER_VIDEO may return SongItem or other types
            val searchResult = YouTube.search(videoId, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
            val firstResult = searchResult?.items?.firstOrNull()
            
            // Check if it's a SongItem before converting
            return@withContext if (firstResult is SongItem) {
                firstResult.toMediaMetadata()
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error importing YouTube track: ${e.message}", e)
            null
        }
    }
    
    /**
     * Import single track from YouTube Music
     */
    suspend fun importYouTubeMusicTrack(url: String): MediaMetadata? = withContext(Dispatchers.IO) {
        try {
            val videoId = UrlChecker.extractYTMusicId(url)
            if (videoId == null) {
                Log.e(TAG, "Invalid YouTube Music URL")
                return@withContext null
            }
            
            // Use YouTube Music search
            val searchResult = YouTube.search(videoId, YouTube.SearchFilter.FILTER_SONG).getOrNull()
            val firstResult = searchResult?.items?.firstOrNull()
            
            // Check if it's a SongItem before converting
            return@withContext if (firstResult is SongItem) {
                firstResult.toMediaMetadata()
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error importing YouTube Music track: ${e.message}", e)
            null
        }
    }
}
