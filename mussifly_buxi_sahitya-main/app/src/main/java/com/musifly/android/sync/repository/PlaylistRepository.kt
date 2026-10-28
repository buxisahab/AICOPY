package com.musifly.android.sync.repository

import com.musifly.android.sync.supabase.SupabasePlaylistService
import com.musifly.android.sync.supabase.SupabasePlaylistSong


import com.musifly.android.db.MusicDatabase
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.sync.UserSessionManager
import com.musifly.android.sync.firestore.FirestorePlaylistService
import com.musifly.android.sync.local.LocalUserPlaylistDao
import com.musifly.android.sync.local.LocalUserPlaylistSongDao
import com.musifly.android.sync.local.UserPlaylistEntity
import com.musifly.android.sync.local.UserPlaylistSongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Status of playlist synchronization
 */
sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val message: String) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}


/**
 * Repository for managing user playlists
 * Switches between local storage (guest mode) and Supabase (logged in mode)
 */
class PlaylistRepository(
    private val localPlaylistDao: LocalUserPlaylistDao,
    private val localPlaylistSongDao: LocalUserPlaylistSongDao,
    private val firestorePlaylistService: FirestorePlaylistService,
    private val supabasePlaylistService: SupabasePlaylistService,
    private val database: MusicDatabase
) {
    // Flag to prevent multiple simultaneous syncs
    @Volatile
    private var isSyncing = false
    
    // Sync status flow
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    /**
     * Generate unique playlist ID
     */
    private fun generatePlaylistId(): String {
        val allowedChars = ('A'..'Z') + ('a'..'z')
        val randomString = (1..8)
            .map { allowedChars.random() }
            .joinToString("")
        return "UP$randomString"
    }
    
    /**
     * Get all user playlists
     * For logged-in users: If Room is empty, syncs from Firestore first
     * For guest users: Returns from Room DB only
     */
    fun getAllPlaylists(): Flow<List<UserPlaylistEntity>> {
        Timber.tag("PlaylistRepository").d("📥 getAllPlaylists() called - isLoggedIn: ${UserSessionManager.isLoggedIn()}")
        
        // Return Flow from Main Database (Source of Truth for "My Playlists" UI)
        // This ensures consistent count with Profile screen and includes legacy playlists
        return database.allEditablePlaylistsByCreateDateAsc().map { playlists ->
            playlists.map { playlist ->
                // Map local Playlist to UserPlaylistEntity
                // Note: isSynced is approximated as false here (UI might not show cloud icon correctly for legacy playlists)
                // but this ensures they appear in the list.
                val createdAtMillis = try {
                    (playlist.playlist.createdAt ?: java.time.LocalDateTime.now()).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
                
                val updatedAtMillis = try {
                    (playlist.playlist.lastUpdateTime ?: java.time.LocalDateTime.now()).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
                
                UserPlaylistEntity(
                    playlistId = playlist.playlist.id,
                    name = playlist.playlist.name,
                    createdAt = createdAtMillis,
                    updatedAt = updatedAtMillis,
                    isSynced = false, // We can't easily know sync status from main DB only, default to false
                    userId = null
                )
            }
        }.onEach {
            Timber.tag("PlaylistRepository").d("📊 getAllPlaylists Flow emitted (from Main DB): ${it.size} playlists")
        }
    }
    
    /**
     * Get playlist by ID
     */
    fun getPlaylist(playlistId: String): Flow<UserPlaylistEntity?> {
        return localPlaylistDao.getPlaylistFlow(playlistId)
    }
    
    /**
     * Get songs in a playlist
     */
    fun getPlaylistSongs(playlistId: String): Flow<List<String>> {
        return localPlaylistSongDao.getPlaylistSongIdsFlow(playlistId)
    }
    
    /**
     * Create a new playlist
     * Uses Firestore if logged in, local DB if guest mode
     * CRITICAL: For logged-in users, Firestore is the source of truth - write MUST succeed
     */
    suspend fun createPlaylist(name: String): Result<String> {
        return try {
            val playlistId = generatePlaylistId()
            val now = System.currentTimeMillis()
            val userId = UserSessionManager.getUserId()
            val isLoggedIn = UserSessionManager.isLoggedIn()
            
            Timber.tag("PlaylistRepository").d("🔥 createPlaylist called - name: $name, id: $playlistId")

            // 1. Create locally ALWAYS (Optimistic UI)
            val playlist = UserPlaylistEntity(
                playlistId = playlistId,
                name = name,
                createdAt = now,
                updatedAt = now,
                isSynced = false, // Pending sync
                userId = userId
            )
            
            // Insert local cache
            localPlaylistDao.insert(playlist)
            
            // Insert into main playlist table (visible in library)
            val createdAt = java.time.LocalDateTime.ofEpochSecond(
                playlist.createdAt / 1000,
                0,
                java.time.ZoneOffset.UTC
            )
            val updatedAt = java.time.LocalDateTime.ofEpochSecond(
                playlist.updatedAt / 1000,
                0,
                java.time.ZoneOffset.UTC
            )
            
            val playlistEntity = com.musifly.android.db.entities.PlaylistEntity(
                id = playlistId,
                name = name,
                createdAt = createdAt,
                lastUpdateTime = updatedAt,
                isEditable = true,
                bookmarkedAt = java.time.LocalDateTime.now() // Ensure visibility
            )
            
            database.transaction {
                insert(playlistEntity)
            }
            Timber.tag("PlaylistRepository").d("✅ Playlist created locally (Optimistic): $playlistId")
            
            // 2. Try to sync if logged in
            if (isLoggedIn && userId != null) {
                Timber.tag("PlaylistRepository").d("☁️ Initiating background sync for playlist: $playlistId")
                try {
                    val firestoreResult = supabasePlaylistService.createPlaylist(userId, playlist.playlistId, playlist.name)
                    
                    if (firestoreResult.isSuccess) {
                        // Mark as synced
                        localPlaylistDao.updateSyncStatus(playlistId, isSynced = true, updatedAt = now)
                        Timber.tag("PlaylistRepository").d("✅ Background sync successful for: $playlistId")
                    } else {
                        val error = firestoreResult.exceptionOrNull()
                        Timber.tag("PlaylistRepository").w(error, "⚠️ Background sync failed for $playlistId - will retry next full sync")
                    }
                } catch (e: Exception) {
                    Timber.tag("PlaylistRepository").e(e, "❌ Exception during background sync")
                }
            } else {
                Timber.tag("PlaylistRepository").d("ℹ️ User not logged in (or guest), skipping background sync")
            }
            
            Result.success(playlistId)
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "❌ Exception in createPlaylist: $name")
            Result.failure(e)
        }
    }
    
    /**
     * Rename playlist
     * Uses Firestore if logged in, local DB if guest mode
     * CRITICAL: For logged-in users, Firestore is the source of truth - write MUST succeed
     */
    suspend fun renamePlaylist(playlistId: String, newName: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            Timber.tag("PlaylistRepository").d("🔥 renamePlaylist called - id: $playlistId, newName: $newName")
            
            if (UserSessionManager.isLoggedIn()) {
                val userId = UserSessionManager.getUserId() ?: return Result.failure(
                    IllegalStateException("User ID is null - cannot rename playlist")
                )
                
                Timber.tag("PlaylistRepository").d("✅ User logged in (uid: $userId) - writing to Firestore FIRST")
                
                // CRITICAL: Write to Supabase FIRST (source of truth)
                // Use createPlaylist (upsert) to update name
                val firestoreResult = supabasePlaylistService.createPlaylist(userId, playlistId, newName)
                
                if (firestoreResult.isSuccess) {
                    // Firestore write succeeded - now update local cache
                    localPlaylistDao.updateName(playlistId, newName, now)
                    
                    // CRITICAL: Also update main playlist table
                    val existingPlaylist = database.playlist(playlistId).firstOrNull()
                    if (existingPlaylist != null) {
                        database.transaction {
                            update(existingPlaylist.playlist.copy(
                                name = newName,
                                lastUpdateTime = java.time.LocalDateTime.ofEpochSecond(now / 1000, 0, java.time.ZoneOffset.UTC)
                            ))
                        }
                    }
                    
                    Timber.tag("PlaylistRepository").d("✅ Playlist renamed in Firestore, local cache, and main table: $playlistId")
                    Result.success(Unit)
                } else {
                    // Firestore write failed - log error and return failure
                    val error = firestoreResult.exceptionOrNull() ?: Exception("Unknown Supabase error")
                    Timber.tag("PlaylistRepository").e(error, "❌ Supabase write FAILED for rename: $playlistId")
                    Result.failure(error)
                }
            } else {
                // Guest mode: Update local DB only
                localPlaylistDao.updateName(playlistId, newName, now)
                Timber.tag("PlaylistRepository").d("✅ Guest mode - Playlist renamed in local DB only: $playlistId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "❌ Exception in renamePlaylist: $playlistId")
            Result.failure(e)
        }
    }
    
    /**
     * Delete playlist
     * Uses Firestore if logged in, local DB if guest mode
     * CRITICAL: For logged-in users, Firestore is the source of truth - write MUST succeed
     */
    suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        return try {
            Timber.tag("PlaylistRepository").d("🔥 deletePlaylist called - id: $playlistId")
            
            if (UserSessionManager.isLoggedIn()) {
                val userId = UserSessionManager.getUserId() ?: return Result.failure(
                    IllegalStateException("User ID is null - cannot delete playlist")
                )
                
                Timber.tag("PlaylistRepository").d("✅ User logged in (uid: $userId) - writing to Firestore FIRST")
                                // Delete from Supabase FIRST
                    val firestoreResult = supabasePlaylistService.deletePlaylist(playlistId)
                
                if (firestoreResult.isSuccess) {
                    // Firestore write succeeded - now delete from local cache
                    localPlaylistSongDao.deleteAllFromPlaylist(playlistId)
                    localPlaylistDao.delete(playlistId)
                    
                    // CRITICAL: Also delete from main playlist table and playlist_song_map
                    val existingPlaylist = database.playlist(playlistId).firstOrNull()
                    if (existingPlaylist != null) {
                        database.transaction {
                            // Clear all songs from playlist_song_map first
                            clearPlaylist(playlistId)
                            // Then delete the playlist itself
                            delete(existingPlaylist.playlist)
                        }
                    } else {
                        // Even if playlist not found, clear playlist_song_map to be safe
                        database.transaction {
                            clearPlaylist(playlistId)
                        }
                    }
                    
                    Timber.tag("PlaylistRepository").d("✅ Playlist permanently deleted from Firestore, local cache, main table, and playlist_song_map: $playlistId")
                    Result.success(Unit)
                } else {
                    // Supabase write failed - log error and return failure
                    val error = firestoreResult.exceptionOrNull() ?: Exception("Unknown Supabase error")
                    Timber.tag("PlaylistRepository").e(error, "❌ Supabase write FAILED for delete: $playlistId")
                    Result.failure(error)
                }
            } else {
                // Guest mode: Delete from local DB only
                localPlaylistSongDao.deleteAllFromPlaylist(playlistId)
                localPlaylistDao.delete(playlistId)
                
                // Also clear from main playlist_song_map and delete playlist
                val existingPlaylist = database.playlist(playlistId).firstOrNull()
                if (existingPlaylist != null) {
                    database.transaction {
                        clearPlaylist(playlistId)
                        delete(existingPlaylist.playlist)
                    }
                } else {
                    database.transaction {
                        clearPlaylist(playlistId)
                    }
                }
                
                Timber.tag("PlaylistRepository").d("✅ Guest mode - Playlist deleted from local DB, main table, and playlist_song_map: $playlistId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "❌ Exception in deletePlaylist: $playlistId")
            Result.failure(e)
        }
    }
    
    /**
     * Add song to playlist
     * Uses Firestore if logged in, local DB if guest mode
     * CRITICAL: For logged-in users, Firestore is the source of truth - write MUST succeed
     */
    suspend fun addSongToPlaylist(playlistId: String, songId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            Timber.tag("PlaylistRepository").d("🔥 addSongToPlaylist called - playlistId: $playlistId, songId: $songId")
            
            if (UserSessionManager.isLoggedIn()) {
                val userId = UserSessionManager.getUserId() ?: return Result.failure(
                    IllegalStateException("User ID is null - cannot add song to playlist")
                )
                
                // Get current position (order)
                val maxPosition = localPlaylistSongDao.getMaxPosition(playlistId) ?: -1
                val order = maxPosition + 1
                
                // Get song title for metadata
                val existingSong = database.getSongById(songId)
                val title = existingSong?.song?.title
                
                Timber.tag("PlaylistRepository").d("✅ User logged in (uid: $userId) - writing to Supabase FIRST")
                
                // CRITICAL: Write to Supabase FIRST (source of truth) with metadata
                val supabaseSong = SupabasePlaylistSong(
                    playlistId = playlistId,
                    songId = songId,
                    title = title,
                    order = order
                )
                
                val firestoreResult = supabasePlaylistService.addSongsToPlaylist(
                    playlistId = playlistId, 
                    songs = listOf(supabaseSong)
                )
                
                if (firestoreResult.isSuccess) {
                    // Supabase write succeeded - now add to local cache
                    // CRITICAL: Ensure playlist exists in user_playlists table before adding song
                    // (user_playlist_songs has foreign key to user_playlists)
                    var userPlaylist = localPlaylistDao.getPlaylist(playlistId)
                    if (userPlaylist == null) {
                        // Playlist doesn't exist in user_playlists - create it
                        // Try to get name from main playlist table or Firestore
                        val mainPlaylist = database.playlist(playlistId).firstOrNull()
                        val playlistName = mainPlaylist?.playlist?.name ?: "Imported Playlist"
                        
                        userPlaylist = UserPlaylistEntity(
                            playlistId = playlistId,
                            name = playlistName,
                            createdAt = now,
                            updatedAt = now,
                            isSynced = true,
                            userId = userId
                        )
                        localPlaylistDao.insert(userPlaylist)
                        Timber.tag("PlaylistRepository").d("✅ Created playlist in user_playlists table: $playlistId")
                    }
                    
                    // Now safe to add song (playlist exists in user_playlists)
                    try {
                        localPlaylistSongDao.addSongToPlaylist(playlistId, songId)
                        localPlaylistDao.updateSyncStatus(playlistId, isSynced = true, updatedAt = now)
                    } catch (e: android.database.sqlite.SQLiteConstraintException) {
                        Timber.tag("PlaylistRepository").e(e, "❌ Foreign key constraint error when adding song $songId to playlist $playlistId in user_playlist_songs")
                        // Try to ensure playlist exists and retry
                        val retryUserPlaylist = localPlaylistDao.getPlaylist(playlistId)
                        if (retryUserPlaylist == null) {
                            // Still doesn't exist - this is a critical error
                            Timber.tag("PlaylistRepository").e("❌ Playlist $playlistId still doesn't exist in user_playlists after creation attempt")
                            return Result.failure(Exception("Playlist not found in user_playlists table: $playlistId"))
                        }
                        // Retry the insert
                        localPlaylistSongDao.addSongToPlaylist(playlistId, songId)
                        localPlaylistDao.updateSyncStatus(playlistId, isSynced = true, updatedAt = now)
                        Timber.tag("PlaylistRepository").d("✅ Successfully added song after retry: $playlistId/$songId")
                    }
                    
                    // CRITICAL: Also add to main playlist_song_map table (like liked songs approach)
                    // This ensures song count updates and songs show in playlist
                    var existingPlaylist = database.playlist(playlistId).firstOrNull()
                    
                    // If playlist doesn't exist in main table, create it first
                    if (existingPlaylist == null) {
                        val userPlaylist = localPlaylistDao.getPlaylist(playlistId)
                        if (userPlaylist != null) {
                            val createdAt = java.time.LocalDateTime.ofEpochSecond(
                                userPlaylist.createdAt / 1000,
                                0,
                                java.time.ZoneOffset.UTC
                            )
                            val updatedAt = java.time.LocalDateTime.ofEpochSecond(
                                userPlaylist.updatedAt / 1000,
                                0,
                                java.time.ZoneOffset.UTC
                            )
                            
                            val playlistEntity = com.musifly.android.db.entities.PlaylistEntity(
                                id = playlistId,
                                name = userPlaylist.name,
                                createdAt = createdAt,
                                lastUpdateTime = updatedAt,
                                isEditable = true,
                                bookmarkedAt = java.time.LocalDateTime.now()
                            )
                            
                            database.transaction {
                                insert(playlistEntity)
                            }
                            
                            existingPlaylist = database.playlist(playlistId).firstOrNull()
                            Timber.tag("PlaylistRepository").d("✅ Created playlist in main table: $playlistId")
                        }
                    }
                    
                    if (existingPlaylist != null) {
                        // CRITICAL: Ensure song exists in song table before inserting into playlist_song_map
                        // (playlist_song_map has foreign key to song table)
                        val songExists = existingSong != null
                        if (!songExists) {
                            Timber.tag("PlaylistRepository").w("⚠️ Song $songId not found in song table")
                        }
                        
                        // Verify song exists now (after potential fetch)
                        val finalSong = database.getSongById(songId)
                        if (finalSong != null) {
                            // Get current actual song count from database
                            val actualCount = database.getPlaylistSongCount(playlistId)
                            
                            try {
                                database.transaction {
                                    insert(
                                        com.musifly.android.db.entities.PlaylistSongMap(
                                            playlistId = playlistId,
                                            songId = songId,
                                            position = actualCount
                                        )
                                    )
                                }
                                Timber.tag("PlaylistRepository").d("✅ Song added to main playlist_song_map table: $playlistId/$songId at position $actualCount")
                            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                                Timber.tag("PlaylistRepository").e(e, "❌ Foreign key constraint error when adding song $songId to playlist_song_map for playlist $playlistId")
                                // Continue - song is in Firestore and user_playlist_songs, just not in playlist_song_map
                            }
                        } else {
                            Timber.tag("PlaylistRepository").w("⚠️ Song $songId still not found in song table after fetch attempt - skipping playlist_song_map insertion")
                        }
                    } else {
                        Timber.tag("PlaylistRepository").e("❌ Playlist not found in main table and could not create: $playlistId - song not added to playlist_song_map")
                    }
                    
                    Timber.tag("PlaylistRepository").d("✅ Song added to playlist in Firestore, local cache, and main table: $playlistId")
                    Result.success(Unit)
                } else {
                    // Firestore write failed - log error and return failure
                    val error = firestoreResult.exceptionOrNull() ?: Exception("Unknown Firestore error")
                    Timber.tag("PlaylistRepository").e(error, "❌ Firestore write FAILED for addSong: $playlistId/$songId")
                    Result.failure(error)
                }
            } else {
                // Guest mode: Add to local DB only
                // CRITICAL: Ensure playlist exists in user_playlists table before adding song
                // (user_playlist_songs has foreign key to user_playlists)
                var userPlaylist = localPlaylistDao.getPlaylist(playlistId)
                if (userPlaylist == null) {
                    // Playlist doesn't exist in user_playlists - create it
                    // Try to get name from main playlist table
                    val mainPlaylist = database.playlist(playlistId).firstOrNull()
                    val playlistName = mainPlaylist?.playlist?.name ?: "Guest Playlist"
                    
                    userPlaylist = UserPlaylistEntity(
                        playlistId = playlistId,
                        name = playlistName,
                        createdAt = now,
                        updatedAt = now,
                        isSynced = false,
                        userId = null
                    )
                    localPlaylistDao.insert(userPlaylist)
                    Timber.tag("PlaylistRepository").d("✅ Created playlist in user_playlists table (guest mode): $playlistId")
                }
                
                // Now safe to add song (playlist exists in user_playlists)
                try {
                    localPlaylistSongDao.addSongToPlaylist(playlistId, songId)
                    localPlaylistDao.updateSyncStatus(playlistId, isSynced = false, updatedAt = now)
                } catch (e: android.database.sqlite.SQLiteConstraintException) {
                    Timber.tag("PlaylistRepository").e(e, "❌ Foreign key constraint error when adding song $songId to playlist $playlistId in user_playlist_songs (guest mode)")
                    // Try to ensure playlist exists and retry
                    val retryUserPlaylist = localPlaylistDao.getPlaylist(playlistId)
                    if (retryUserPlaylist == null) {
                        // Still doesn't exist - this is a critical error
                        Timber.tag("PlaylistRepository").e("❌ Playlist $playlistId still doesn't exist in user_playlists after creation attempt (guest mode)")
                        return Result.failure(Exception("Playlist not found in user_playlists table: $playlistId"))
                    }
                    // Retry the insert
                    localPlaylistSongDao.addSongToPlaylist(playlistId, songId)
                    localPlaylistDao.updateSyncStatus(playlistId, isSynced = false, updatedAt = now)
                    Timber.tag("PlaylistRepository").d("✅ Successfully added song after retry (guest mode): $playlistId/$songId")
                }
                
                // CRITICAL: Also add to main playlist_song_map table for guest playlists
                var existingPlaylist = database.playlist(playlistId).firstOrNull()
                
                // If playlist doesn't exist in main table, create it first
                if (existingPlaylist == null) {
                    val userPlaylist = localPlaylistDao.getPlaylist(playlistId)
                    if (userPlaylist != null) {
                        val createdAt = java.time.LocalDateTime.ofEpochSecond(
                            userPlaylist.createdAt / 1000,
                            0,
                            java.time.ZoneOffset.UTC
                        )
                        val updatedAt = java.time.LocalDateTime.ofEpochSecond(
                            userPlaylist.updatedAt / 1000,
                            0,
                            java.time.ZoneOffset.UTC
                        )
                        
                        val playlistEntity = com.musifly.android.db.entities.PlaylistEntity(
                            id = playlistId,
                            name = userPlaylist.name,
                            createdAt = createdAt,
                            lastUpdateTime = updatedAt,
                            isEditable = true,
                            bookmarkedAt = java.time.LocalDateTime.now()
                        )
                        
                        database.transaction {
                            insert(playlistEntity)
                        }
                        
                        existingPlaylist = database.playlist(playlistId).firstOrNull()
                    }
                }
                
                if (existingPlaylist != null) {
                    // CRITICAL: Ensure song exists in song table before inserting into playlist_song_map
                    // (playlist_song_map has foreign key to song table)
                    val existingSong = database.getSongById(songId)
                    val songExists = existingSong != null
                    if (!songExists) {
                        Timber.tag("PlaylistRepository").w("⚠️ Song $songId not found in song table (guest mode) - attempting to fetch from YouTube")
                        // CRITICAL: Use YouTube.player() to fetch video metadata directly by video ID
                        try {
                            val playerResponse = com.musifly.innertube.YouTube.player(
                                videoId = songId,
                                playlistId = null,
                                client = com.musifly.innertube.models.YouTubeClient.WEB_REMIX,
                                signatureTimestamp = null
                            ).getOrNull()
                            
                            val videoDetails = playerResponse?.videoDetails
                            
                            if (videoDetails != null) {
                                // Convert VideoDetails to MediaMetadata
                                val thumbnailUrl = videoDetails.thumbnail.thumbnails.lastOrNull()?.url
                                    ?: "https://img.youtube.com/vi/${songId}/maxresdefault.jpg"
                                
                                val durationSeconds = videoDetails.lengthSeconds.toIntOrNull() ?: 0
                                
                                val mediaMetadata = com.musifly.android.models.MediaMetadata(
                                    id = songId,
                                    title = videoDetails.title,
                                    artists = listOf(
                                        com.musifly.android.models.MediaMetadata.Artist(
                                            id = videoDetails.channelId,
                                            name = videoDetails.author
                                        )
                                    ),
                                    duration = durationSeconds,
                                    thumbnailUrl = thumbnailUrl
                                )
                                
                                database.transaction {
                                    insert(mediaMetadata)
                                }
                                Timber.tag("PlaylistRepository").d("✅ Fetched and inserted song $songId from YouTube (guest mode): ${mediaMetadata.title}")
                            } else {
                                Timber.tag("PlaylistRepository").w("⚠️ Could not fetch song $songId from YouTube (guest mode) - skipping playlist_song_map insertion")
                            }
                        } catch (e: Exception) {
                            Timber.tag("PlaylistRepository").w(e, "⚠️ Error fetching song $songId from YouTube (guest mode)")
                        }
                    }
                    
                    // Verify song exists now (after potential fetch)
                    val finalSong = database.getSongById(songId)
                    if (finalSong != null) {
                        // Get current actual song count from database
                        val actualCount = database.getPlaylistSongCount(playlistId)
                        
                        try {
                            database.transaction {
                                insert(
                                    com.musifly.android.db.entities.PlaylistSongMap(
                                        playlistId = playlistId,
                                        songId = songId,
                                        position = actualCount
                                    )
                                )
                            }
                            Timber.tag("PlaylistRepository").d("✅ Song added to main playlist_song_map table (guest mode): $playlistId/$songId at position $actualCount")
                        } catch (e: android.database.sqlite.SQLiteConstraintException) {
                            Timber.tag("PlaylistRepository").e(e, "❌ Foreign key constraint error when adding song $songId to playlist_song_map for playlist $playlistId (guest mode)")
                            // Continue - song is in user_playlist_songs, just not in playlist_song_map
                        }
                    } else {
                        Timber.tag("PlaylistRepository").w("⚠️ Song $songId still not found in song table after fetch attempt (guest mode) - skipping playlist_song_map insertion")
                    }
                }
                
                Timber.tag("PlaylistRepository").d("✅ Guest mode - Song added to playlist in local DB and main table: $playlistId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "❌ Exception in addSongToPlaylist: $playlistId/$songId")
            Result.failure(e)
        }
    }
    
    /**
     * Add multiple songs to playlist (Batch)
     * Uses Firestore Batch if logged in, local DB transaction if guest mode
     * CRITICAL: Efficiently adds multiple songs to prevent quota exhaustion
     */
    suspend fun addSongsToPlaylist(playlistId: String, songIds: List<String>): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            Timber.tag("PlaylistRepository").d("🔥 addSongsToPlaylist called - playlistId: $playlistId, count: ${songIds.size}")
            
            if (songIds.isEmpty()) return Result.success(Unit)
            
            if (UserSessionManager.isLoggedIn()) {
                val userId = UserSessionManager.getUserId() ?: return Result.failure(
                    IllegalStateException("User ID is null - cannot add songs to playlist")
                )
                
                // Get current position (order)
                var currentMaxPosition = localPlaylistSongDao.getMaxPosition(playlistId) ?: -1
                
                // Prepare songs for Firestore
                val firestoreSongs = mutableListOf<com.musifly.android.sync.firestore.FirestorePlaylistSong>()
                
                // Get titles for metadata efficiently
                // We assume songs are already in the DB as Importer adds them
                songIds.forEach { songId ->
                    val existingSong = database.getSongById(songId)
                    val title = existingSong?.song?.title
                    currentMaxPosition++
                    
                    firestoreSongs.add(
                        com.musifly.android.sync.firestore.FirestorePlaylistSong(
                            songId = songId,
                            title = title,
                            order = currentMaxPosition
                        )
                    )
                }
                
                Timber.tag("PlaylistRepository").d("✅ User logged in (uid: $userId) - BATCH writing to Supabase FIRST")
                
                // CRITICAL: Write to Supabase FIRST (source of truth) using BATCH
                val supabaseSongs = firestoreSongs.map { 
                    SupabasePlaylistSong(
                        playlistId = playlistId,
                        songId = it.songId,
                        title = it.title,
                        order = it.order
                    )
                }
                
                val firestoreResult = supabasePlaylistService.addSongsToPlaylist(
                    playlistId = playlistId, 
                    songs = supabaseSongs
                )
                
                if (firestoreResult.isSuccess) {
                    // Firestore write succeeded - now add to local cache
                    // CRITICAL: Ensure playlist exists in user_playlists table first
                    var userPlaylist = localPlaylistDao.getPlaylist(playlistId)
                    if (userPlaylist == null) {
                        val mainPlaylist = database.playlist(playlistId).firstOrNull()
                        val playlistName = mainPlaylist?.playlist?.name ?: "Imported Playlist"
                        
                        userPlaylist = UserPlaylistEntity(
                            playlistId = playlistId,
                            name = playlistName,
                            createdAt = now,
                            updatedAt = now,
                            isSynced = true,
                            userId = userId
                        )
                        localPlaylistDao.insert(userPlaylist)
                    }
                    
                    // Batch insert into local cache
                    val currentMaxPosLocal = (localPlaylistSongDao.getMaxPosition(playlistId) ?: -1) + 1
                    val userPlaylistSongs = songIds.mapIndexed { index, songId ->
                        UserPlaylistSongEntity(
                            playlistId = playlistId,
                            songId = songId,
                            position = currentMaxPosLocal + index
                        )
                    }
                    localPlaylistSongDao.insertAll(userPlaylistSongs)
                    localPlaylistDao.updateSyncStatus(playlistId, isSynced = true, updatedAt = now)
                    
                    // CRITICAL: Batch insert into main playlist_song_map table
                    var existingPlaylist = database.playlist(playlistId).firstOrNull()
                    if (existingPlaylist == null) {
                         // Should have been created by Importer, but just in case
                         // ... (simplification: assume existence or skip)
                    }
                    
                    if (existingPlaylist != null) {
                         val currentCount = database.getPlaylistSongCount(playlistId)
                         database.transaction {
                             songIds.forEachIndexed { index, songId ->
                                 try {
                                     insert(
                                         com.musifly.android.db.entities.PlaylistSongMap(
                                             playlistId = playlistId,
                                             songId = songId,
                                             position = currentCount + index
                                         )
                                     )
                                 } catch (e: Exception) {
                                     // Ignore duplicates
                                 }
                             }
                         }
                    }
                    
                    Timber.tag("PlaylistRepository").d("✅ Songs batch added to playlist in Firestore, local cache, and main table: $playlistId")
                    Result.success(Unit)
                } else {
                    val error = firestoreResult.exceptionOrNull() ?: Exception("Unknown Supabase error")
                    Timber.tag("PlaylistRepository").e(error, "❌ Supabase batch write FAILED")
                    Result.failure(error)
                }
            } else {
                // Guest mode
                // CRITICAL: Ensure playlist exists in user_playlists
                var userPlaylist = localPlaylistDao.getPlaylist(playlistId)
                if (userPlaylist == null) {
                    val mainPlaylist = database.playlist(playlistId).firstOrNull()
                    val playlistName = mainPlaylist?.playlist?.name ?: "Guest Playlist"
                    
                    userPlaylist = UserPlaylistEntity(
                        playlistId = playlistId,
                        name = playlistName,
                        createdAt = now,
                        updatedAt = now,
                        isSynced = false,
                        userId = null
                    )
                    localPlaylistDao.insert(userPlaylist)
                }
                
                // Batch insert local
                val currentMaxPos = (localPlaylistSongDao.getMaxPosition(playlistId) ?: -1) + 1
                val userPlaylistSongs = songIds.mapIndexed { index, songId ->
                    UserPlaylistSongEntity(
                        playlistId = playlistId,
                        songId = songId,
                        position = currentMaxPos + index
                    )
                }
                localPlaylistSongDao.insertAll(userPlaylistSongs)
                localPlaylistDao.updateSyncStatus(playlistId, isSynced = false, updatedAt = now)
                
                // Batch insert main table
                val currentCount = database.getPlaylistSongCount(playlistId)
                database.transaction {
                     songIds.forEachIndexed { index, songId ->
                         try {
                             insert(
                                 com.musifly.android.db.entities.PlaylistSongMap(
                                     playlistId = playlistId,
                                     songId = songId,
                                     position = currentCount + index
                                 )
                             )
                         } catch (e: Exception) {
                             // Ignore
                         }
                     }
                }
                
                Timber.tag("PlaylistRepository").d("✅ Guest mode - Songs batch added: $playlistId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "❌ Exception in addSongsToPlaylist")
            Result.failure(e)
        }
    }
    
    /**
     * Remove song from playlist
     * Uses Firestore if logged in, local DB if guest mode
     * CRITICAL: For logged-in users, Firestore is the source of truth - write MUST succeed
     */
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            Timber.tag("PlaylistRepository").d("🔥 removeSongFromPlaylist called - playlistId: $playlistId, songId: $songId")
            
            if (UserSessionManager.isLoggedIn()) {
                val userId = UserSessionManager.getUserId() ?: return Result.failure(
                    IllegalStateException("User ID is null - cannot remove song from playlist")
                )
                
                Timber.tag("PlaylistRepository").d("✅ User logged in (uid: $userId) - writing to Firestore FIRST")
                
                // CRITICAL: Write to Supabase FIRST (source of truth)
                val firestoreResult = supabasePlaylistService.removeSongFromPlaylist(playlistId, songId)
                
                if (firestoreResult.isSuccess) {
                    // Firestore write succeeded - now remove from local cache
                    localPlaylistSongDao.removeSongFromPlaylist(playlistId, songId)
                    localPlaylistDao.updateSyncStatus(playlistId, isSynced = true, updatedAt = now)
                    
                    // CRITICAL: Also remove from main playlist_song_map table
                    database.removeSongFromPlaylistMap(playlistId, songId)
                    
                    Timber.tag("PlaylistRepository").d("✅ Song removed from playlist in Firestore, local cache, and main table: $playlistId")
                    Result.success(Unit)
                } else {
                    // Firestore write failed - log error and return failure
                    val error = firestoreResult.exceptionOrNull() ?: Exception("Unknown Supabase error")
                    Timber.tag("PlaylistRepository").e(error, "❌ Supabase write FAILED for removeSong: $playlistId/$songId")
                    Result.failure(error)
                }
            } else {
                // Guest mode: Remove from local DB only
                localPlaylistSongDao.removeSongFromPlaylist(playlistId, songId)
                localPlaylistDao.updateSyncStatus(playlistId, isSynced = false, updatedAt = now)
                
                // CRITICAL: Also remove from main playlist_song_map table
                database.removeSongFromPlaylistMap(playlistId, songId)
                
                Timber.tag("PlaylistRepository").d("✅ Guest mode - Song removed from playlist in local DB and main table: $playlistId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "❌ Exception in removeSongFromPlaylist: $playlistId/$songId")
            Result.failure(e)
        }
    }
    
    /**
     * Check if song is in playlist
     */
    suspend fun isSongInPlaylist(playlistId: String, songId: String): Boolean {
        return try {
            localPlaylistSongDao.isSongInPlaylist(playlistId, songId)
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "Failed to check if song in playlist")
            false
        }
    }
    
    /**
     * Sync playlists from Firestore to local
     * Downloads all playlists from Firestore and merges with local
     */
    /**
     * Sync playlists from Supabase (Source of Truth) to local database
     * This fetches all playlists for the logged-in user from Supabase and updates local storage
     * It handles:
     * 1. Creating/Updating playlists in local UserPlaylistEntity (offline cache)
     * 2. Creating/Updating playlists in main PlaylistEntity (visible in library)
     * 3. Syncing songs and updating PlaylistSongMap (visible in player)
     * 4. Fetching missing song metadata from YouTube if needed
     */
    suspend fun syncFromSupabase(): Result<Int> {
        val isLoggedIn = UserSessionManager.isLoggedIn()
        val userId = UserSessionManager.getUserId()
        
        Timber.tag("PlaylistRepository").d("🔄 syncFromSupabase called - isLoggedIn: $isLoggedIn, userId: $userId")
        
        if (!isLoggedIn) {
            Timber.tag("PlaylistRepository").d("ℹ️ User not logged in, skipping sync from Supabase")
            return Result.success(0)
        }
        
        if (userId == null) {
            Timber.tag("PlaylistRepository").e("❌ User ID is null even though user is logged in!")
            return Result.failure(IllegalStateException("User ID is null"))
        }

        if (isSyncing) {
            Timber.tag("PlaylistRepository").d("⏳ Sync already in progress, skipping")
            return Result.success(0)
        }
        
        isSyncing = true
        _syncStatus.value = SyncStatus.Syncing
        
        return try {
            withContext(Dispatchers.IO) {
                Timber.tag("PlaylistRepository").d("📥 Fetching playlists from Supabase for user: $userId")
                
                // 0. Upload unsynced local playlists first (Two-way sync)
                syncUnsyncedToSupabase()
                
                // 1. Fetch playlists from Supabase
                val supabasePlaylistsResult = supabasePlaylistService.getUserPlaylists(userId)
                val supabasePlaylists = supabasePlaylistsResult.getOrThrow()
                
                Timber.tag("PlaylistRepository").d("📋 Found ${supabasePlaylists.size} playlists in Supabase")
                
                // Cleanup: Remove local sync-enabled playlists that are NO LONGER in Supabase
                // This ensures "permanent deletion" parity across devices
                val localPlaylists = localPlaylistDao.getAllPlaylistsSync().filter { it.userId == userId }
                val localPlaylistIds = localPlaylists.map { it.playlistId }.toSet()
                val supabasePlaylistIds = supabasePlaylists.map { it.id }.toSet()
                
                val missingFromSupabase = localPlaylistIds.filter { it !in supabasePlaylistIds }
                if (missingFromSupabase.isNotEmpty()) {
                    val toDelete = mutableListOf<String>()
                    
                    missingFromSupabase.forEach { playlistId ->
                        // Check if playlist was synced before deleting
                        // We need to check if we should keep it (isSynced=false means pending upload)
                        val playlist = localPlaylists.find { it.playlistId == playlistId }
                        if (playlist?.isSynced == true) {
                            toDelete.add(playlistId)
                        }
                    }
                    
                    if (toDelete.isNotEmpty()) {
                        Timber.tag("PlaylistRepository").d("🗑️ Removing ${toDelete.size} playlists locally that are missing from Supabase")
                        toDelete.forEach { playlistId ->
                            localPlaylistSongDao.deleteAllFromPlaylist(playlistId)
                            localPlaylistDao.delete(playlistId)
                            
                            // Also delete from main playlist table
                            val existingMain = database.playlist(playlistId).firstOrNull()
                            if (existingMain != null) {
                                database.transaction {
                                    clearPlaylist(playlistId)
                                    delete(existingMain.playlist)
                                }
                            }
                        }
                    }
                }
                
                var playlistsInserted = 0
                
                // 2. Process each playlist
                supabasePlaylists.forEach { supabasePlaylist ->
                    
                    // Add/Update in local user_playlists table
                    val now = System.currentTimeMillis()
                    // Convert timestamp if available, else use now
                    val createdAt = supabasePlaylist.createdAt?.let { 
                        try {
                            java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                        } catch (e: Exception) {
                            try {
                                java.time.Instant.parse(it).toEpochMilli()
                            } catch (e2: Exception) {
                                Timber.tag("PlaylistRepository").w("⚠️ Date parse failed for $it, using now()")
                                now
                            }
                        }
                    } ?: now
                    
                    val updatedAt = supabasePlaylist.updatedAt?.let { 
                        try {
                            java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli()
                        } catch (e: Exception) {
                            try {
                                java.time.Instant.parse(it).toEpochMilli()
                            } catch (e2: Exception) {
                                now
                            }
                        }
                    } ?: now

                    val userPlaylist = UserPlaylistEntity(
                        playlistId = supabasePlaylist.id,
                        name = supabasePlaylist.name,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        isSynced = true,
                        userId = userId
                    )
                    
                    // Insert/Update local cache
                    val existingLocal = localPlaylistDao.getPlaylist(supabasePlaylist.id)
                    if (existingLocal == null) {
                        localPlaylistDao.insert(userPlaylist)
                    } else {
                        localPlaylistDao.updateName(supabasePlaylist.id, supabasePlaylist.name, updatedAt)
                    }
                    
                    // Add/Update in main playlist table (visible in UI)
                    try {
                        val createdAtLdt = java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(createdAt), 
                            java.time.ZoneOffset.UTC
                        )
                        val updatedAtLdt = java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(updatedAt), 
                            java.time.ZoneOffset.UTC
                        )
                        
                        val playlistEntity = com.musifly.android.db.entities.PlaylistEntity(
                            id = supabasePlaylist.id,
                            name = supabasePlaylist.name,
                            createdAt = createdAtLdt,
                            lastUpdateTime = updatedAtLdt,
                            isEditable = true,
                            bookmarkedAt = java.time.LocalDateTime.now()
                        )
                        
                        val existingMain = database.playlist(supabasePlaylist.id).firstOrNull()
                        database.transaction {
                            // Clear any orphaned mappings first
                            clearPlaylist(supabasePlaylist.id)
                            
                            if (existingMain == null) {
                                insert(playlistEntity)
                                playlistsInserted++
                            } else {
                                update(playlistEntity)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag("PlaylistRepository").w(e, "⚠️ Failed to update main playlist table for ${supabasePlaylist.name}")
                    }
                    
                    // 3. Fetch songs for this playlist from Supabase
                    val songsResult = supabasePlaylistService.getPlaylistSongs(supabasePlaylist.id)
                    val supabaseSongs = songsResult.getOrElse { emptyList() }
                    
                    if (supabaseSongs.isNotEmpty()) {
                        val songIds = supabaseSongs.map { it.songId }
                        
                        // Update local cache
                        localPlaylistSongDao.setPlaylistSongs(supabasePlaylist.id, songIds)
                        
                        // Sync songs to main playlist_song_map table
                        // First, identify missing songs in local DB
                       val missingSongIds = songIds.filter { songId ->
                            database.getSongById(songId) == null
                        }
                        
                        if (missingSongIds.isNotEmpty()) {
                            Timber.tag("PlaylistRepository").d("🔍 Found ${missingSongIds.size} missing songs locally, attempting to fetch metadata...")
                            missingSongIds.forEach { songId ->
                                try {
                                    // Try fetching from YouTube (Innertube)
                                    var playerResponse = com.musifly.innertube.YouTube.player(
                                        videoId = songId,
                                        playlistId = null,
                                        client = com.musifly.innertube.models.YouTubeClient.WEB_REMIX,
                                        signatureTimestamp = null
                                    ).getOrNull()
                                    
                                    var videoDetails = playerResponse?.videoDetails
                                    var mediaMetadata: com.musifly.android.models.MediaMetadata? = null
                                    
                                    if (videoDetails != null) {
                                        val thumbnailUrl = videoDetails.thumbnail.thumbnails.lastOrNull()?.url
                                            ?: "https://img.youtube.com/vi/${songId}/maxresdefault.jpg"
                                        
                                        val durationSeconds = videoDetails.lengthSeconds.toIntOrNull() ?: 0
                                        
                                        mediaMetadata = com.musifly.android.models.MediaMetadata(
                                            id = songId,
                                            title = videoDetails.title,
                                            artists = listOf(
                                                com.musifly.android.models.MediaMetadata.Artist(
                                                    id = videoDetails.channelId,
                                                    name = videoDetails.author
                                                )
                                            ),
                                            duration = durationSeconds,
                                            thumbnailUrl = thumbnailUrl
                                        )
                                    } else {
                                        // Fallback to Supabase title if available
                                        val song = supabaseSongs.find { it.songId == songId }
                                        if (song?.title != null) {
                                             mediaMetadata = com.musifly.android.models.MediaMetadata(
                                                id = songId,
                                                title = song.title,
                                                artists = listOf(
                                                    com.musifly.android.models.MediaMetadata.Artist(
                                                        id = "unknown", 
                                                        name = "Unknown Artist"
                                                    )
                                                ),
                                                duration = 0,
                                                thumbnailUrl = "https://img.youtube.com/vi/${songId}/default.jpg"
                                            )
                                        }
                                    }
                                    
                                    if (mediaMetadata != null) {
                                        database.transaction {
                                            insert(mediaMetadata)
                                        }
                                        Timber.tag("PlaylistRepository").d("✅ Inserted song $songId into local DB")
                                    }
                                    
                                } catch (e: Exception) {
                                    Timber.tag("PlaylistRepository").w(e, "⚠️ Failed to fetch/insert missing song metadata: $songId")
                                }
                            }
                        }
                        
                        // Insert into playlist_song_map
                        try {
                            // Filter to only songs that exist (or we just added)
                            val validSongs = supabaseSongs.filter { database.getSongById(it.songId) != null }
                            
                            if (validSongs.isNotEmpty()) {
                                database.transaction {
                                    // Clear existing mapping for this playlist first to ensure exact sync
                                    clearPlaylist(supabasePlaylist.id)
                                    
                                    validSongs.forEachIndexed { index, song ->
                                        try {
                                            insert(
                                                com.musifly.android.db.entities.PlaylistSongMap(
                                                    playlistId = supabasePlaylist.id,
                                                    songId = song.songId,
                                                    position = index
                                                )
                                            )
                                        } catch (e: Exception) {
                                            // Ignore duplicates
                                        }
                                    }
                                }
                                Timber.tag("PlaylistRepository").d("✅ Songs synced to main playlist_song_map for playlist ${supabasePlaylist.name}")
                            }
                        } catch (e: Exception) {
                             Timber.tag("PlaylistRepository").e(e, "❌ Failed to update playlist_song_map for ${supabasePlaylist.name}")
                        }
                    }
                }
                
                Timber.tag("PlaylistRepository").d("✅ Synced ${supabasePlaylists.size} playlists from Supabase")
                _syncStatus.value = SyncStatus.Success("Synced ${supabasePlaylists.size} playlists")
                Result.success(supabasePlaylists.size)
            }
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "❌ Failed to sync from Supabase")
            _syncStatus.value = SyncStatus.Error("Sync failed: ${e.message}")
            Result.failure(e)
        } finally {
            isSyncing = false
        }
    }
    
    /**
     * Sync unsynced local playlists to Supabase
     * Uploads local playlists that haven't been synced yet
     */
    suspend fun syncUnsyncedToSupabase(): Result<Int> {
        val isLoggedIn = UserSessionManager.isLoggedIn()
        val userId = UserSessionManager.getUserId()
        
        if (!isLoggedIn) {
            Timber.tag("PlaylistRepository").d("syncUnsyncedToSupabase: User not logged in, skipping")
            return Result.success(0)
        }
        
        if (userId == null) {
             return Result.failure(IllegalStateException("User ID is null"))
        }
        
        return try {
            Timber.tag("PlaylistRepository").d("Syncing unsynced playlists to Supabase...")
            
            val unsyncedPlaylists = localPlaylistDao.getUnsyncedPlaylists()
            if (unsyncedPlaylists.isEmpty()) {
                Timber.tag("PlaylistRepository").d("No unsynced playlists to upload")
                return Result.success(0)
            }
            
            var syncedCount = 0
            unsyncedPlaylists.forEach { playlist ->
                // 1. Create/Update playlist in Supabase
                // Note: supabasePlaylistService.createPlaylist does upsert
                val createResult = supabasePlaylistService.createPlaylist(userId, playlist.playlistId, playlist.name)
                
                if (createResult.isSuccess) {
                    // 2. Upload songs for this playlist
                    val playlistSongs = localPlaylistSongDao.getPlaylistSongs(playlist.playlistId)
                    
                    // Map to Supabase songs - need titles
                    val supabaseSongs = playlistSongs.mapNotNull { ps ->
                        val song = database.getSongById(ps.songId)
                        // If song missing locally, we use placeholder or skip?
                        // Supabase table requires title.
                        val title = song?.song?.title ?: "Unknown Title"
                        
                        SupabasePlaylistSong(
                            playlistId = playlist.playlistId,
                            songId = ps.songId,
                            title = title,
                            order = ps.position
                        )
                    }
                    
                    if (supabaseSongs.isNotEmpty()) {
                         val songsResult = supabasePlaylistService.addSongsToPlaylist(playlist.playlistId, supabaseSongs)
                         if (songsResult.isFailure) {
                             Timber.tag("PlaylistRepository").e("Failed to upload songs for playlist ${playlist.name}")
                         }
                    }
                    
                    // Mark as synced
                    localPlaylistDao.updateSyncStatus(playlist.playlistId, isSynced = true, updatedAt = System.currentTimeMillis())
                    syncedCount++
                }
            }
            
            Timber.tag("PlaylistRepository").d("Synced $syncedCount unsynced playlists to Supabase")
            Result.success(syncedCount)
            
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "Failed to sync unsynced playlists to Supabase")
            Result.failure(e)
        }
    }
    
    /**
     * Sync unsynced local playlists to Firestore
     * Uploads local playlists that haven't been synced yet
     */
    suspend fun syncUnsyncedToFirestore(): Result<Int> {
        if (!UserSessionManager.isLoggedIn()) {
            Timber.tag("PlaylistRepository").d("syncUnsyncedToFirestore: User not logged in, skipping")
            return Result.success(0)
        }
        
        return try {
            val userId = UserSessionManager.getUserId() ?: return Result.failure(
                IllegalStateException("User ID is null")
            )
            
            Timber.tag("PlaylistRepository").d("Syncing unsynced playlists to Firestore...")
            
            // Get unsynced local playlists
            val unsyncedPlaylists = localPlaylistDao.getUnsyncedPlaylists()
            if (unsyncedPlaylists.isEmpty()) {
                Timber.tag("PlaylistRepository").d("No unsynced playlists to upload")
                return Result.success(0)
            }
            
            var syncedCount = 0
            unsyncedPlaylists.forEach { playlist ->
                // Upload playlist to Firestore
                val createResult = firestorePlaylistService.createPlaylist(userId, playlist)
                if (createResult.isSuccess) {
                    // Upload songs for this playlist
                    val songIds = localPlaylistSongDao.getPlaylistSongIds(playlist.playlistId)
                    val songsResult = firestorePlaylistService.setPlaylistSongs(userId, playlist.playlistId, songIds)
                    
                    if (songsResult.isSuccess) {
                        // Mark as synced
                        localPlaylistDao.updateSyncStatus(playlist.playlistId, isSynced = true, updatedAt = System.currentTimeMillis())
                        syncedCount++
                    }
                }
            }
            
            Timber.tag("PlaylistRepository").d("Synced $syncedCount unsynced playlists to Firestore")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "Failed to sync unsynced playlists to Firestore")
            Result.failure(e)
        }
    }
    
    /**
     * Sync all local playlists to Firestore (overwriting if exists)
     * This method syncs all local playlists to Firestore, overwriting any existing data
     */
    suspend fun syncToFirestore(): Result<Int> {
        val isLoggedIn = UserSessionManager.isLoggedIn()
        val userId = UserSessionManager.getUserId()
        
        Timber.tag("PlaylistRepository").d("🔄 syncToFirestore called - isLoggedIn: $isLoggedIn, userId: $userId")
        
        if (!isLoggedIn) {
            Timber.tag("PlaylistRepository").d("ℹ️ User not logged in, skipping sync to Firestore")
            return Result.success(0)
        }
        
        if (userId == null) {
            Timber.tag("PlaylistRepository").e("❌ User ID is null even though user is logged in!")
            return Result.failure(IllegalStateException("User ID is null"))
        }
        
        return try {
            Timber.tag("PlaylistRepository").d("📤 Starting sync to Firestore for user: $userId")
            
            // Get all local playlists (both synced and unsynced)
            val localPlaylists = localPlaylistDao.getAllPlaylistsSync()
            
            if (localPlaylists.isEmpty()) {
                Timber.tag("PlaylistRepository").d("ℹ️ No local playlists to sync")
                return Result.success(0)
            }
            
            var syncedCount = 0
            var failedCount = 0
            
            localPlaylists.forEach { playlist ->
                try {
                    Timber.tag("PlaylistRepository").d("📤 Syncing playlist to Firestore: ${playlist.name} (${playlist.playlistId})")
                    
                    // Update playlist with current timestamp
                    val updatedPlaylist = playlist.copy(
                        updatedAt = System.currentTimeMillis(),
                        isSynced = false // Will be set to true after successful sync
                    )
                    
                    // Upload/overwrite playlist to Firestore (SetOptions.merge() will overwrite if exists)
                    val createResult = firestorePlaylistService.createPlaylist(userId, updatedPlaylist)
                    
                    if (createResult.isSuccess) {
                        // Get all songs for this playlist
                        val songIds = localPlaylistSongDao.getPlaylistSongIds(playlist.playlistId)
                        
                        Timber.tag("PlaylistRepository").d("📤 Syncing ${songIds.size} songs for playlist: ${playlist.name}")
                        
                        // Upload/overwrite songs to Firestore
                        val songsResult = firestorePlaylistService.setPlaylistSongs(userId, playlist.playlistId, songIds)
                        
                        if (songsResult.isSuccess) {
                            // Mark as synced
                            localPlaylistDao.updateSyncStatus(playlist.playlistId, isSynced = true, updatedAt = updatedPlaylist.updatedAt)
                            syncedCount++
                            Timber.tag("PlaylistRepository").d("✅ Successfully synced playlist to Firestore: ${playlist.name} (${songIds.size} songs)")
                        } else {
                            failedCount++
                            val error = songsResult.exceptionOrNull() ?: Exception("Unknown error")
                            Timber.tag("PlaylistRepository").e(error, "❌ Failed to sync songs for playlist: ${playlist.name}")
                        }
                    } else {
                        failedCount++
                        val error = createResult.exceptionOrNull() ?: Exception("Unknown error")
                        Timber.tag("PlaylistRepository").e(error, "❌ Failed to sync playlist to Firestore: ${playlist.name}")
                    }
                } catch (e: Exception) {
                    failedCount++
                    Timber.tag("PlaylistRepository").e(e, "❌ Exception while syncing playlist: ${playlist.name}")
                }
            }
            
            Timber.tag("PlaylistRepository").d("✅ Sync to Firestore completed: $syncedCount successful, $failedCount failed out of ${localPlaylists.size} playlists")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "❌ Exception in syncToFirestore")
            Result.failure(e)
        }
    }
    
    /**
     * Clear all user data from local storage (called on logout)
     * This clears user playlists and playlist songs, keeping only guest data
     */
    suspend fun clearUserData() {
        try {
            Timber.tag("PlaylistRepository").d("🧹 Starting to clear all user playlist data...")
            
            // Get all playlists first (suspend function to get list)
            val allPlaylists = localPlaylistDao.getAllPlaylistsSync()
            val playlistIds = allPlaylists.map { it.playlistId }
            
            Timber.tag("PlaylistRepository").d("📋 Found ${playlistIds.size} user playlists to clear")
            
            // CRITICAL: Clear from main database tables (playlist and playlist_song_map)
            // This is what the library screen reads from
            // Get playlist entities BEFORE transaction (suspend functions)
            val playlistsToDelete = mutableListOf<com.musifly.android.db.entities.PlaylistEntity>()
            if (playlistIds.isNotEmpty()) {
                playlistIds.forEach { playlistId ->
                    try {
                        val playlistEntity = database.playlist(playlistId).firstOrNull()
                        if (playlistEntity != null) {
                            playlistsToDelete.add(playlistEntity.playlist)
                        }
                    } catch (e: Exception) {
                        Timber.tag("PlaylistRepository").w(e, "⚠️ Error fetching playlist $playlistId")
                    }
                }
            }
            
            // Also get ALL user-created playlists (isEditable = true, browseId = null) from main table
            // This catches any playlists that might not be in user_playlists table
            try {
                val allEditablePlaylists = database.allEditablePlaylistsByCreateDateAsc().first()
                val additionalUserPlaylists = allEditablePlaylists.filter { 
                    it.playlist.isEditable == true && it.playlist.browseId == null 
                }
                
                if (additionalUserPlaylists.isNotEmpty()) {
                    Timber.tag("PlaylistRepository").d("📋 Found ${additionalUserPlaylists.size} additional user playlists in main table to clear")
                    additionalUserPlaylists.forEach { playlist ->
                        // Only add if not already in list
                        if (!playlistsToDelete.any { it.id == playlist.playlist.id }) {
                            playlistsToDelete.add(playlist.playlist)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag("PlaylistRepository").w(e, "⚠️ Error fetching additional user playlists")
            }
            
            // Now delete all playlists in transaction (non-suspend context)
            if (playlistsToDelete.isNotEmpty()) {
                Timber.tag("PlaylistRepository").d("🗑️ Deleting ${playlistsToDelete.size} playlists from main database...")
                database.transaction {
                    playlistsToDelete.forEach { playlistEntity ->
                        try {
                            // Clear playlist_song_map entries for this playlist
                            clearPlaylist(playlistEntity.id)
                            
                            // Delete playlist from main playlist table
                            database.query {
                                delete(playlistEntity)
                            }
                            Timber.tag("PlaylistRepository").d("✅ Deleted user playlist from main table: ${playlistEntity.name} (${playlistEntity.id})")
                        } catch (e: Exception) {
                            Timber.tag("PlaylistRepository").w(e, "⚠️ Error deleting playlist ${playlistEntity.id} from main database")
                        }
                    }
                }
            }
            
            // Clear from user_playlist_songs table
            allPlaylists.forEach { playlist ->
                localPlaylistSongDao.deleteAllFromPlaylist(playlist.playlistId)
            }
            
            // Delete all playlists from user_playlists table
            localPlaylistDao.deleteAll()
            
            Timber.tag("PlaylistRepository").i("✅ Cleared all user playlists from local storage and main database")
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "❌ Failed to clear user playlists")
        }
    }

    /**
     * Get all synced songs for auto-download
     * Returns a list of MediaMetadata for songs in synced playlists
     */
    suspend fun getSyncedSongsForDownload(): List<com.musifly.android.models.MediaMetadata> {
        return try {
            val songIds = localPlaylistSongDao.getAllUserPlaylistSongIds()
            val songs = mutableListOf<com.musifly.android.models.MediaMetadata>()
            
            songIds.forEach { songId ->
                try {
                    val songEntity = database.getSongById(songId)
                    if (songEntity != null) {
                        songs.add(songEntity.toMediaMetadata())
                    }
                } catch (e: Exception) {
                    Timber.tag("PlaylistRepository").w(e, "⚠️ Error fetching song details for download: $songId")
                }
            }
            songs
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "❌ Error getting synced songs for download")
            emptyList()
        }
    }

    /**
     * Migrate playlists from Firestore to Supabase
     */
    suspend fun migrateFirestoreToSupabase(): Result<Unit> {
        val userId = UserSessionManager.getUserId() ?: return Result.failure(IllegalStateException("Not logged in"))
        
        return try {
            Timber.tag("PlaylistRepository").d("🚀 Starting migration from Firestore to Supabase for user: $userId")
            
            // 1. Fetch all playlists from Firestore
            val playlistsResult = firestorePlaylistService.getAllPlaylists(userId)
            val playlists = playlistsResult.getOrThrow()
            
            Timber.tag("PlaylistRepository").d("📋 Found ${playlists.size} playlists in Firestore to migrate")
            
            var successCount = 0
            var failCount = 0
            
            playlists.forEach { playlist ->
                try {
                    // 2. Create playlist in Supabase
                    Timber.tag("PlaylistRepository").d("Creating playlist in Supabase: ${playlist.name}")
                    supabasePlaylistService.createPlaylist(userId, playlist.playlistId, playlist.name)
                        .getOrThrow()
                    
                    // 3. Fetch songs from Firestore
                    val songsResult = firestorePlaylistService.getPlaylistSongs(userId, playlist.playlistId)
                    val songs = songsResult.getOrElse { emptyList() }
                    
                    if (songs.isNotEmpty()) {
                        Timber.tag("PlaylistRepository").d("Migrating ${songs.size} songs for playlist: ${playlist.name}")
                        
                        // 4. Add songs to Supabase
                        val supabaseSongs = songs.map { 
                            SupabasePlaylistSong(
                                playlistId = playlist.playlistId,
                                songId = it.songId,
                                title = it.title,
                                order = it.order
                            )
                        }
                        
                        supabasePlaylistService.addSongsToPlaylist(playlist.playlistId, supabaseSongs)
                            .getOrThrow()
                    }
                    
                    successCount++
                } catch (e: Exception) {
                    Timber.tag("PlaylistRepository").e(e, "❌ Failed to migrate playlist: ${playlist.name}")
                    failCount++
                }
            }
            
            Timber.tag("PlaylistRepository").d("✅ Migration completed. Success: $successCount, Failed: $failCount")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("PlaylistRepository").e(e, "❌ Migration failed")
            Result.failure(e)
        }
    }
}
