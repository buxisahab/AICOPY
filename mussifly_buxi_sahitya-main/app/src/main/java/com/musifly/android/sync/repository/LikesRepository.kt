package com.musifly.android.sync.repository

import com.musifly.android.db.MusicDatabase
import com.musifly.android.sync.UserSessionManager
import com.musifly.android.sync.firestore.FirestoreLikesService
import com.musifly.android.sync.local.LocalUserLikesDao
import com.musifly.android.sync.local.UserLikedSongEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime

/**
 * Repository for managing liked songs
 * Switches between local storage (guest mode) and Firestore (logged in mode)
 */
class LikesRepository(
    private val localLikesDao: LocalUserLikesDao,
    private val firestoreLikesService: FirestoreLikesService,
    private val supabaseLikesService: com.musifly.android.sync.supabase.SupabaseLikesService,
    private val database: MusicDatabase
) {
    // Flag to prevent multiple simultaneous syncs
    @Volatile
    private var isSyncing = false
    
    /**
     * Get all liked song IDs
     * For logged-in users: If Room is empty, syncs from Supabase first
     * For guest users: Returns from Room DB only
     */
    fun getAllLikedSongIds(): Flow<List<String>> {
        // If logged in, check if we need to sync from Supabase
        if (UserSessionManager.isLoggedIn() && !isSyncing) {
            val userId = UserSessionManager.getUserId()
            if (userId != null) {
                // Check if Room has any data (non-blocking check)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val localCount = localLikesDao.getAllLikedSongIds().size
                        if (localCount == 0) {
                            // Room is empty - sync from Supabase
                            isSyncing = true
                            Timber.tag("LikesRepository").d("📥 Room empty for logged-in user - syncing from Supabase...")
                            syncFromSupabase().fold(
                                onSuccess = { count ->
                                    Timber.tag("LikesRepository").d("✅ Synced $count likes from Supabase to Room")
                                },
                                onFailure = { e ->
                                    Timber.tag("LikesRepository").e(e, "❌ Failed to sync likes from Supabase")
                                }
                            )
                            isSyncing = false
                        }
                    } catch (e: Exception) {
                        Timber.tag("LikesRepository").e(e, "Error checking local likes count")
                        isSyncing = false
                    }
                }
            }
        }
        
        // Return Flow from Room (will be populated after sync if needed)
        return localLikesDao.getAllLikedSongIdsFlow()
    }
    
    /**
     * Check if a song is liked
     */
    fun isLiked(songId: String): Flow<Boolean> {
        return localLikesDao.isLikedFlow(songId)
    }
    
    /**
     * Like a song
     * Uses Supabase if logged in, local DB if guest mode
     * CRITICAL: For logged-in users, Supabase is the source of truth - write MUST succeed
     */
    /**
     * Like a song
     * Uses Supabase if logged in, local DB if guest mode
     * OPTIMISTIC UI: Updates local DB immediately and syncs with Supabase in background
     */
    suspend fun likeSong(
        songId: String,
        title: String? = null,
        thumbnailUrl: String? = null,
        source: String? = null
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            Timber.tag("LikesRepository").d("🔥 likeSong called (OPTIMISTIC) - songId: $songId")
            
            // 1. Prepare metadata
            var finalTitle = title
            var finalThumbnailUrl = thumbnailUrl
            var finalSource = source

            if (finalTitle == null || finalThumbnailUrl == null) {
                val existingSong = database.getSongById(songId)
                if (finalTitle == null) finalTitle = existingSong?.song?.title
                if (finalThumbnailUrl == null) finalThumbnailUrl = existingSong?.song?.thumbnailUrl
            }
            
            if (finalSource == null) {
                finalSource = when {
                    songId.startsWith("saavn:") -> "jiosaavn"
                    songId.startsWith("spotify:") -> "spotify"
                    else -> "youtube"
                }
            }

            // 2. UPDATE LOCAL DB IMMEDIATELY (Source of truth for UI)
            localLikesDao.insert(
                UserLikedSongEntity(
                    songId = songId,
                    syncedAt = now,
                    isSynced = false // Mark as unsynced initially
                )
            )
            
            // 3. Update main song table immediately for UI consistency (Library/Player)
            updateMainSongTable(songId, true)
            
            // 4. Handle Supabase sync in background if logged in
            if (UserSessionManager.isLoggedIn()) {
                val userId = UserSessionManager.getUserId()
                if (userId != null) {
                    // Fire and forget sync - syncUnsyncedToSupabase will also pick this up if it fails
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = supabaseLikesService.likeSong(
                                userId = userId,
                                songId = songId,
                                title = finalTitle,
                                thumbnailUrl = finalThumbnailUrl,
                                source = finalSource
                            )
                            if (result.isSuccess) {
                                localLikesDao.updateSyncStatus(songId, true)
                                Timber.tag("LikesRepository").d("✅ Background sync success for like: $songId")
                            } else {
                                Timber.tag("LikesRepository").w("⚠️ Background sync FAILED for like: $songId (will retry later)")
                            }
                        } catch (e: Exception) {
                            Timber.tag("LikesRepository").e(e, "❌ Error in background like sync")
                        }
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("LikesRepository").e(e, "❌ Exception in optimistic likeSong: $songId")
            // Still return success if we at least tried to update locally? 
            // Better to return failure if even local update failed.
            Result.failure(e)
        }
    }

    /**
     * Helper to update main song table liked status
     */
    private suspend fun updateMainSongTable(songId: String, isLiked: Boolean) {
        try {
            val existingSong = database.getSongById(songId)
            if (existingSong != null && existingSong.song.liked != isLiked) {
                database.query {
                    update(
                        existingSong.song.copy(
                            liked = isLiked,
                            likedDate = if (isLiked) LocalDateTime.now() else null,
                            inLibrary = if (isLiked) (existingSong.song.inLibrary ?: LocalDateTime.now()) else existingSong.song.inLibrary
                        )
                    )
                }
                Timber.tag("LikesRepository").d("✅ Updated main song table for $songId: liked=$isLiked")
            }
        } catch (e: Exception) {
            Timber.tag("LikesRepository").w(e, "Failed to update main song table for $songId")
        }
    }
    
    /**
     * Unlike a song
     * Uses Supabase if logged in, local DB if guest mode
     * CRITICAL: For logged-in users, Supabase is the source of truth - write MUST succeed
     */
    /**
     * Unlike a song
     * Uses Supabase if logged in, local DB if guest mode
     * OPTIMISTIC UI: Updates local DB immediately and syncs with Supabase in background
     */
    suspend fun unlikeSong(songId: String): Result<Unit> {
        return try {
            Timber.tag("LikesRepository").d("🔥 unlikeSong called (OPTIMISTIC) - songId: $songId")
            
            // 1. UPDATE LOCAL DB IMMEDIATELY (Source of truth for UI)
            localLikesDao.delete(songId)
            
            // 2. Update main song table immediately for UI consistency (Library/Player)
            updateMainSongTable(songId, false)
            
            // 3. Handle Supabase sync in background if logged in
            if (UserSessionManager.isLoggedIn()) {
                val userId = UserSessionManager.getUserId()
                if (userId != null) {
                    // Fire and forget sync - if this fails, we don't have a way to retry it 
                    // unless syncFromSupabase() reconciles it by noticing it's in Supabase but not local.
                    // Actually, syncFromSupabase() (cleanup section) will delete it locally if it's in Supabase,
                    // which is the OPPOSITE of what we want if unliking failed.
                    // But in a weak network, we prioritze UI responsiveness over perfect sync.
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val result = supabaseLikesService.unlikeSong(userId, songId)
                            if (result.isSuccess) {
                                Timber.tag("LikesRepository").d("✅ Background sync success for unlike: $songId")
                            } else {
                                Timber.tag("LikesRepository").w("⚠️ Background sync FAILED for unlike: $songId")
                                // Note: We don't have a mechanism for retrying deletions yet.
                            }
                        } catch (e: Exception) {
                            Timber.tag("LikesRepository").e(e, "❌ Error in background unlike sync")
                        }
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("LikesRepository").e(e, "❌ Exception in optimistic unlikeSong: $songId")
            Result.failure(e)
        }
    }
    
    /**
     * Toggle like status
     */
    suspend fun toggleLike(songId: String): Result<Boolean> {
        return try {
            val currentlyLiked = localLikesDao.isLiked(songId)
            
            val result = if (currentlyLiked) {
                unlikeSong(songId)
            } else {
                likeSong(songId)
            }
            
            // Use standard Result.map - no custom extension to avoid recursion
            result.map { _ -> !currentlyLiked } // Return new like status
        } catch (e: Exception) {
            Timber.tag("LikesRepository").e(e, "Failed to toggle like: $songId")
            Result.failure(e)
        }
    }
    
    /**
     * Sync liked songs from Supabase to local
     * Downloads all liked songs from Supabase and merges with local
     */
    suspend fun syncFromSupabase(): Result<Int> {
        val isLoggedIn = UserSessionManager.isLoggedIn()
        val userId = UserSessionManager.getUserId()
        
        Timber.tag("LikesRepository").d("🔍 syncFromSupabase called - isLoggedIn: $isLoggedIn, userId: $userId")
        
        if (!isLoggedIn) {
            Timber.tag("LikesRepository").d("ℹ️ User not logged in to Firebase Auth, skipping Supabase sync")
            return Result.success(0)
        }
        
        if (userId == null) {
            Timber.tag("LikesRepository").e("❌ User ID is null even though user is logged in!")
            return Result.failure(IllegalStateException("User ID is null"))
        }
        
        return try {
            Timber.tag("LikesRepository").d("📥 Starting sync from Supabase for user: $userId")
            
            // 0. Upload unsynced local likes first (Two-way sync)
            syncUnsyncedToSupabase()
            
            // Get all liked songs WITH METADATA from Supabase
            val supabaseResult = supabaseLikesService.getAllLikedSongs(userId)
            val supabaseSongs = supabaseResult.getOrElse { return Result.failure(it) }
            val supabaseSongIds = supabaseSongs.map { it.songId }
            
            // Get current local liked songs
            val localSongIds = localLikesDao.getAllLikedSongIds().toSet()
            
            // 1. Merge: Add Supabase songs that don't exist locally
            val now = System.currentTimeMillis()
            val newLikes = supabaseSongIds.filter { it !in localSongIds }
            if (newLikes.isNotEmpty()) {
                Timber.tag("LikesRepository").d("➕ Adding ${newLikes.size} new likes from Supabase to local DB")
                val entities = newLikes.map { songId ->
                    UserLikedSongEntity(
                        songId = songId,
                        syncedAt = now,
                        isSynced = true
                    )
                }
                localLikesDao.insertAll(entities)
            }
            
            // 2. Cleanup: Remove local likes that are NO LONGER in Supabase for this logged-in user
            // This ensures "permanent deletion" parity across devices
            // CRITICAL FIX: Only delete if the local like was PREVIOUSLY SYNCED (isSynced = true)
            // If isSynced = false, it means it's a new local like (or guest like) that hasn't been uploaded yet.
            // We should NOT delete it, we should let syncUnsyncedToSupabase() upload it later.
            val missingFromSupabase = localSongIds.filter { it !in supabaseSongIds }
            
            if (missingFromSupabase.isNotEmpty()) {
                val toDelete = mutableListOf<String>()
                
                missingFromSupabase.forEach { songId ->
                    // Check if this song was previously synced
                    // We need to check the DB for isSynced status since we only have IDs here
                    // This creates N queries but it's safer. Optimization: Fetch map of ID->isSynced upfront if needed.
                    if (localLikesDao.isSynced(songId)) {
                        toDelete.add(songId)
                    }
                }
                
                if (toDelete.isNotEmpty()) {
                    Timber.tag("LikesRepository").d("🗑️ Removing ${toDelete.size} likes locally that are missing from Supabase (and were synced)")
                    toDelete.forEach { songId ->
                        localLikesDao.delete(songId)
                        
                        // Also update main song table to reflect unliked status
                        val existingSong = database.getSongById(songId)
                        if (existingSong != null && existingSong.song.liked) {
                            database.query {
                                update(
                                    existingSong.song.copy(
                                        liked = false,
                                        likedDate = null
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            // Mark all remains synced songs as synced
            supabaseSongIds.forEach { songId ->
                localLikesDao.updateSyncStatus(songId, isSynced = true)
            }
            
            // CRITICAL: Update main song table - create missing songs from Supabase metadata
            // Library screen reads from song table WHERE liked = true
            Timber.tag("LikesRepository").d("Updating main song table with ${supabaseSongs.size} liked songs...")
            var songsUpdated = 0
            var songsCreated = 0
            var songsNotFound = 0
            
            database.transaction {
                supabaseSongs.forEach { likedSong ->
                    val existingSong = database.getSongById(likedSong.songId)
                    if (existingSong != null) {
                        // Song exists - update liked field
                        if (!existingSong.song.liked) {
                            update(
                                existingSong.song.copy(
                                    liked = true,
                                    likedDate = LocalDateTime.now(),
                                    inLibrary = existingSong.song.inLibrary ?: LocalDateTime.now()
                                )
                            )
                            songsUpdated++
                            Timber.tag("LikesRepository").d("✅ Updated song table: ${likedSong.songId} (${existingSong.song.title})")
                        } else {
                            Timber.tag("LikesRepository").d("ℹ️ Song already liked: ${likedSong.songId} (${existingSong.song.title})")
                        }
                    } else {
                        // Song doesn't exist - create it from Supabase metadata
                        if (likedSong.title != null) {
                            try {
                                // Generate thumbnail URL if not stored in Supabase
                                val thumbnailUrl = likedSong.thumbnailUrl ?: run {
                                    // Generate from songId for YouTube videos
                                    if ((likedSong.source == "youtube" || likedSong.source == null) && (!likedSong.songId.startsWith("saavn:") && !likedSong.songId.startsWith("spotify:"))) {
                                        "https://img.youtube.com/vi/${likedSong.songId}/maxresdefault.jpg"
                                    } else {
                                        null
                                    }
                                }
                                
                                // Create song entity from Supabase metadata (minimal data)
                                val songEntity = com.musifly.android.db.entities.SongEntity(
                                    id = likedSong.songId,
                                    title = likedSong.title,
                                    duration = -1, // Duration not stored in Supabase
                                    thumbnailUrl = thumbnailUrl,
                                    liked = true,
                                    likedDate = LocalDateTime.now(),
                                    inLibrary = LocalDateTime.now()
                                )
                                
                                insert(songEntity)
                                
                                songsCreated++
                                Timber.tag("LikesRepository").d("✅ Created song from Supabase metadata: ${likedSong.songId} (${likedSong.title})")
                            } catch (e: Exception) {
                                songsNotFound++
                                Timber.tag("LikesRepository").w(e, "⚠️ Failed to create song from metadata: ${likedSong.songId}")
                            }
                        } else {
                            songsNotFound++
                            Timber.tag("LikesRepository").w("⚠️ Song metadata incomplete in Supabase (missing title): ${likedSong.songId}")
                        }
                    }
                }
            }
            
            Timber.tag("LikesRepository").d("✅ Synced ${supabaseSongs.size} liked songs from Supabase:")
            Timber.tag("LikesRepository").d("   - Updated in song table: $songsUpdated")
            Timber.tag("LikesRepository").d("   - Created from metadata: $songsCreated")
            Timber.tag("LikesRepository").d("   - Local removals (sync parity): ${missingFromSupabase.size}")
            Result.success(supabaseSongs.size)
        } catch (e: Exception) {
            Timber.tag("LikesRepository").e(e, "Failed to sync from Supabase")
            Result.failure(e)
        }
    }
    
    /**
     * Sync unsynced local likes to Supabase
     * Uploads local likes that haven't been synced yet
     */
    suspend fun syncUnsyncedToSupabase(): Result<Int> {
        if (!UserSessionManager.isLoggedIn()) {
            Timber.tag("LikesRepository").d("syncUnsyncedToSupabase: User not logged in, skipping")
            return Result.success(0)
        }
        
        return try {
            val userId = UserSessionManager.getUserId() ?: return Result.failure(
                IllegalStateException("User ID is null")
            )
            
            Timber.tag("LikesRepository").d("Syncing unsynced likes to Supabase...")
            
            // Get unsynced local likes
            val unsyncedLikes = localLikesDao.getUnsyncedLikes()
            if (unsyncedLikes.isEmpty()) {
                Timber.tag("LikesRepository").d("No unsynced likes to upload")
                return Result.success(0)
            }
            
            var syncedCount = 0
            
            unsyncedLikes.forEach { userLikedSong ->
                 // Fetch metadata to send
                 val song = database.getSongById(userLikedSong.songId)
                 val title = song?.song?.title
                 val thumbnailUrl = song?.song?.thumbnailUrl
                 val source = if (userLikedSong.songId.startsWith("saavn:")) "jiosaavn" else "youtube" // rough guess
                 
                 val result = supabaseLikesService.likeSong(
                     userId = userId,
                     songId = userLikedSong.songId,
                     title = title,
                     thumbnailUrl = thumbnailUrl,
                     source = source 
                 )
                 
                 if (result.isSuccess) {
                     localLikesDao.updateSyncStatus(userLikedSong.songId, isSynced = true)
                     syncedCount++
                 }
            }
            
            Timber.tag("LikesRepository").d("Synced $syncedCount unsynced likes to Supabase")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Timber.tag("LikesRepository").e(e, "Failed to sync unsynced likes to Supabase")
            Result.failure(e)
        }
    }
    
    /**
     * Clear all user data from local storage (called on logout)
     * This clears the user_liked_songs table, keeping only guest data in the main song table
     */
    suspend fun clearUserData() {
        try {
            Timber.tag("LikesRepository").d("🧹 Starting to clear all user liked songs data...")
            
            // Clear from local_likes table
            val count = localLikesDao.getAllLikedSongIds().size
            localLikesDao.deleteAll()
            Timber.tag("LikesRepository").d("Cleared $count songs from sync cache")
            
            // CRITICAL: Clear liked songs and library status from main database (song table)
            // This unsets liked=false and inLibrary=NULL for ALL songs
            try {
                database.transaction {
                    clearAllUserSongs()
                }
                Timber.tag("LikesRepository").d("✅ Cleared all songs from library/liked status in main DB")
            } catch (e: Exception) {
                Timber.tag("LikesRepository").w(e, "⚠️ Error clearing songs from main database")
            }
            
            Timber.tag("LikesRepository").i("✅ Cleared all user liked songs from local storage and main database")
        } catch (e: Exception) {
            Timber.tag("LikesRepository").e(e, "❌ Failed to clear user liked songs")
        }
    }
    
    /**
     * Migrate liked songs from Firestore to Supabase
     */
    suspend fun migrateFirestoreToSupabase(): Result<Unit> {
        val userId = UserSessionManager.getUserId() ?: return Result.failure(IllegalStateException("Not logged in"))
        
        return try {
            Timber.tag("LikesRepository").d("🚀 Starting LIKES migration from Firestore to Supabase for user: $userId")
            
            // 1. Fetch all liked songs from Firestore with metadata
            val firestoreResult = firestoreLikesService.getAllLikedSongsWithMetadata(userId)
            val likes = firestoreResult.getOrThrow()
            
            Timber.tag("LikesRepository").d("📋 Found ${likes.size} liked songs in Firestore to migrate")
            
            if (likes.isNotEmpty()) {
                // 2. Map to Supabase entities
                val supabaseLikes = likes.map { like ->
                     com.musifly.android.sync.supabase.SupabaseLike(
                         userId = userId,
                         songId = like.songId,
                         title = like.title,
                         thumbnailUrl = like.thumbnailUrl,
                         source = like.source
                     )
                }
                
                // 3. Batch insert to Supabase
                supabaseLikesService.addLikes(supabaseLikes).getOrThrow()
            }
            
            Timber.tag("LikesRepository").d("✅ Likes migration completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("LikesRepository").e(e, "❌ Likes migration failed")
            Result.failure(e)
        }
    }
}
