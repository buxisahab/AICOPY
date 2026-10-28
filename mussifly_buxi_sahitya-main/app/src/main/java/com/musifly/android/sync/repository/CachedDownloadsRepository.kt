package com.musifly.android.sync.repository

import com.musifly.android.db.MusicDatabase
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.sync.UserSessionManager
import com.musifly.android.sync.supabase.SupabaseCachedDownloadsService
import com.musifly.android.sync.local.LocalUserCachedDownloadsDao
import com.musifly.android.sync.local.UserCachedDownloadEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Repository for managing cached downloads
 * Switches between local storage (guest mode) and Supabase (logged in mode)
 * Only stores song IDs and metadata - actual audio files stay in local storage
 */
class CachedDownloadsRepository(
    private val localCachedDownloadsDao: LocalUserCachedDownloadsDao,
    private val supabaseCachedDownloadsService: SupabaseCachedDownloadsService,
    private val database: MusicDatabase
) {
    // Flag to prevent multiple simultaneous syncs
    @Volatile
    private var isSyncing = false
    
    /**
     * Get all cached download IDs
     * For logged-in users: If Room is empty, syncs from Firestore first
     * For guest users: Returns from Room DB only
     */
    fun getAllCachedDownloadIds(): Flow<List<String>> {
        // If logged in, check if we need to sync from Firestore
        if (UserSessionManager.isLoggedIn() && !isSyncing) {
            val userId = UserSessionManager.getUserId()
            if (userId != null) {
                // Check if Room has any data (non-blocking check)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val localCount = localCachedDownloadsDao.getAllCachedDownloadIds().size
                        if (localCount == 0) {
                            // Room is empty - sync from Supabase
                            isSyncing = true
                            Timber.tag("CachedDownloadsRepository").d("📥 Room empty for logged-in user - syncing from Supabase...")
                            syncFromSupabase().fold(
                                onSuccess = { count ->
                                    Timber.tag("CachedDownloadsRepository").d("✅ Synced $count cached downloads from Supabase to Room")
                                },
                                onFailure = { e ->
                                    Timber.tag("CachedDownloadsRepository").e(e, "❌ Failed to sync cached downloads from Supabase")
                                }
                            )
                            isSyncing = false
                        }
                    } catch (e: Exception) {
                        Timber.tag("CachedDownloadsRepository").e(e, "Error checking local cached downloads count")
                        isSyncing = false
                    }
                }
            }
        }
        
        // Return Flow from Room (will be populated after sync if needed)
        return localCachedDownloadsDao.getAllCachedDownloadIdsFlow()
    }
    
    /**
     * Check if a song is cached
     */
    fun isCached(songId: String): Flow<Boolean> {
        return localCachedDownloadsDao.isCachedFlow(songId)
    }
    
    /**
     * Add a cached download
     * Uses Firestore if logged in, local DB if guest mode
     * CRITICAL: For logged-in users, Firestore is the source of truth - write MUST succeed
     */
    suspend fun addCachedDownload(songId: String): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            Timber.tag("CachedDownloadsRepository").d("🔥 addCachedDownload called - songId: $songId")
            
            // Fetch current song metadata to store in Firestore (for offline access)
            val existingSong = database.getSongById(songId)
            val title = existingSong?.song?.title
            val thumbnailUrl = existingSong?.song?.thumbnailUrl
            
            // Detect source from songId (youtube, jiosaavn, etc.)
            val source = when {
                songId.startsWith("saavn:") -> "jiosaavn"
                songId.startsWith("spotify:") -> "spotify"
                else -> "youtube" // Default to youtube for YouTube video IDs
            }
            
            if (UserSessionManager.isLoggedIn()) {
                // Logged in: Supabase is PRIMARY source of truth
                val userId = UserSessionManager.getUserId() ?: return Result.failure(
                    IllegalStateException("User ID is null - cannot add cached download")
                )
                
                Timber.tag("CachedDownloadsRepository").d("✅ User logged in (uid: $userId) - writing to Supabase FIRST")
                
                // CRITICAL: Write to Supabase FIRST (source of truth) with minimal metadata
                val supabaseResult = supabaseCachedDownloadsService.addCachedDownload(
                    userId = userId,
                    songId = songId,
                    title = title,
                    thumbnailUrl = thumbnailUrl,
                    source = source
                )
                
                if (supabaseResult.isSuccess) {
                    // Supabase write succeeded - now cache locally for offline access
                    localCachedDownloadsDao.insert(
                        UserCachedDownloadEntity(
                            songId = songId,
                            syncedAt = now,
                            cachedAt = now,
                            isSynced = true
                        )
                    )
                    
                    Timber.tag("CachedDownloadsRepository").d("✅ Cached download added to Supabase and cached locally: $songId")
                    Result.success(Unit)
                } else {
                    // Supabase write failed - log error and return failure
                    val error = supabaseResult.exceptionOrNull() ?: Exception("Unknown Supabase error")
                    Timber.tag("CachedDownloadsRepository").e(error, "❌ Supabase write FAILED for cached download: $songId")
                    Result.failure(error)
                }
            } else {
                // Guest mode: Use local DB only
                localCachedDownloadsDao.insert(
                    UserCachedDownloadEntity(
                        songId = songId,
                        syncedAt = now,
                        cachedAt = now,
                        isSynced = false // Not synced in guest mode
                    )
                )
                Timber.tag("CachedDownloadsRepository").d("✅ Guest mode - Cached download added to local DB only: $songId")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.tag("CachedDownloadsRepository").e(e, "❌ Exception in addCachedDownload: $songId")
            Result.failure(e)
        }
    }
    
    /**
     * Remove a cached download
     * Uses Firestore if logged in, local DB if guest mode
     * CRITICAL: For logged-in users, Firestore is the source of truth - write MUST succeed
     * Also deletes the local audio file
     */
    suspend fun removeCachedDownload(songId: String): Result<Unit> {
        return try {
            Timber.tag("CachedDownloadsRepository").d("🗑️ removeCachedDownload called - songId: $songId")
            
            if (UserSessionManager.isLoggedIn()) {
                val userId = UserSessionManager.getUserId() ?: run {
                    Timber.tag("CachedDownloadsRepository").e("❌ User ID is null - cannot remove from Supabase")
                    return Result.failure(IllegalStateException("User ID is null"))
                }
                
                Timber.tag("CachedDownloadsRepository").d("🌐 User logged in (uid: $userId) - Attempting Supabase deletion FIRST")
                
                val supabaseResult = supabaseCachedDownloadsService.removeCachedDownload(userId, songId)
                
                if (supabaseResult.isSuccess) {
                    Timber.tag("CachedDownloadsRepository").d("✅ Supabase deletion successful - now removing from local DB")
                    localCachedDownloadsDao.delete(songId)
                    Timber.tag("CachedDownloadsRepository").i("✅ Cached download permanently removed: $songId")
                    Result.success(Unit)
                } else {
                    val error = supabaseResult.exceptionOrNull() ?: Exception("Unknown Supabase error")
                    Timber.tag("CachedDownloadsRepository").e(error, "❌ Supabase delete FAILED - aborting local deletion to maintain sync: $songId")
                    Result.failure(error)
                }
            } else {
                Timber.tag("CachedDownloadsRepository").d("👤 Guest mode - removing from local DB only: $songId")
                localCachedDownloadsDao.delete(songId)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.tag("CachedDownloadsRepository").e(e, "❌ Critical exception in removeCachedDownload: $songId")
            Result.failure(e)
        }
    }
    
    /**
     * Sync cached downloads from Firestore to local
     * Downloads all cached downloads from Firestore and merges with local
     */
    suspend fun syncFromSupabase(): Result<Int> {
        val isLoggedIn = UserSessionManager.isLoggedIn()
        val userId = UserSessionManager.getUserId()
        
        Timber.tag("CachedDownloadsRepository").d("🔍 syncFromSupabase called - isLoggedIn: $isLoggedIn, userId: $userId")
        
        if (!isLoggedIn) {
            Timber.tag("CachedDownloadsRepository").d("📥 Sync skipped: User not logged in")
            return Result.success(0)
        }
        
        if (userId == null) {
            Timber.tag("CachedDownloadsRepository").e("❌ Sync failed: User ID is null despite being logged in")
            return Result.failure(IllegalStateException("User ID is null"))
        }
        
        return try {
            Timber.tag("CachedDownloadsRepository").d("📥 Starting sync from Supabase for user: $userId")
            
            // Get all cached downloads WITH METADATA from Supabase
            val supabaseResult = supabaseCachedDownloadsService.getAllCachedDownloads(userId)
            val supabaseDownloads = supabaseResult.getOrElse { 
                Timber.tag("CachedDownloadsRepository").e(it, "❌ Failed to fetch from Supabase")
                return Result.failure(it) 
            }
            val supabaseSongIds = supabaseDownloads.map { it.songId }
            
            Timber.tag("CachedDownloadsRepository").d("📥 Fetched ${supabaseDownloads.size} items from Supabase")
            
            // Get current local cached downloads
            val localSongIds = localCachedDownloadsDao.getAllCachedDownloadIds().toSet()
            
            // Merge: Add Supabase downloads that don't exist locally
            val now = System.currentTimeMillis()
            val newDownloads = supabaseSongIds.filter { it !in localSongIds }
            if (newDownloads.isNotEmpty()) {
                Timber.tag("CachedDownloadsRepository").d("📥 Adding ${newDownloads.size} new downloads to local repository")
                val entities = newDownloads.map { songId ->
                    UserCachedDownloadEntity(
                        songId = songId,
                        syncedAt = now,
                        cachedAt = now, // We don't store cachedAt in Supabase yet, defaulting to now
                        isSynced = true
                    )
                }
                localCachedDownloadsDao.insertAll(entities)
            }
            
            // Mark all synced downloads as synced
            supabaseSongIds.forEach { songId ->
                localCachedDownloadsDao.updateSyncStatus(songId, isSynced = true)
            }
            
            // CRITICAL: Create song entries in main database from Supabase metadata
            // This ensures songs appear in the UI even if they don't exist locally
            Timber.tag("CachedDownloadsRepository").d("Creating song entries from Supabase metadata...")
            var songsCreated = 0
            var songsUpdated = 0
            var createdSongs = 0
            
            database.transaction {
                supabaseDownloads.forEach { cachedDownload ->
                    val existingSong = database.getSongById(cachedDownload.songId)
                    
                    if (existingSong == null && cachedDownload.title != null) {
                        // Song doesn't exist - create it from Supabase metadata
                        try {
                            Timber.tag("CachedDownloadsRepository").d("📥 Re-creating song metadata for: ${cachedDownload.title}")
                            // Generate thumbnail URL if not stored in Supabase
                            val thumbnailUrl = cachedDownload.thumbnailUrl ?: run {
                                // Generate from songId for YouTube videos
                                if (cachedDownload.source == "youtube" || (!cachedDownload.songId.startsWith("saavn:") && !cachedDownload.songId.startsWith("spotify:"))) {
                                    "https://img.youtube.com/vi/${cachedDownload.songId}/maxresdefault.jpg"
                                } else {
                                    null
                                }
                            }
                            
                            // Create song entity from Supabase metadata (minimal data)
                            val songEntity = com.musifly.android.db.entities.SongEntity(
                                id = cachedDownload.songId,
                                title = cachedDownload.title,
                                duration = -1, // Duration not stored in Supabase
                                thumbnailUrl = thumbnailUrl,
                                liked = false,
                                inLibrary = null // Not in library, just cached
                            )
                            
                            insert(songEntity)
                            songsCreated++
                            Timber.tag("CachedDownloadsRepository").d("✅ Created song from Supabase metadata: ${cachedDownload.songId} (${cachedDownload.title})")
                        } catch (e: Exception) {
                            Timber.tag("CachedDownloadsRepository").w(e, "⚠️ Failed to create song from metadata: ${cachedDownload.songId}")
                        }
                    } else if (existingSong != null) {
                        songsUpdated++
                        Timber.tag("CachedDownloadsRepository").d("ℹ️ Song already exists: ${cachedDownload.songId}")
                    }
                }
            }
            
            Timber.tag("CachedDownloadsRepository").d("✅ Synced ${supabaseDownloads.size} cached downloads from Supabase:")
            Timber.tag("CachedDownloadsRepository").d("   - New downloads added: ${newDownloads.size}")
            Timber.tag("CachedDownloadsRepository").d("   - Songs created: $songsCreated")
            Timber.tag("CachedDownloadsRepository").d("   - Songs already existed: $songsUpdated")
            Timber.tag("CachedDownloadsRepository").d("   - Total synced: ${supabaseDownloads.size}")
            Result.success(supabaseDownloads.size)
        } catch (e: Exception) {
            Timber.tag("CachedDownloadsRepository").e(e, "Failed to sync from Supabase")
            Result.failure(e)
        }
    }
    
    /**
     * Sync unsynced local cached downloads to Firestore
     * Uploads local cached downloads that haven't been synced yet
     */
    suspend fun syncUnsyncedToSupabase(): Result<Int> {
        if (!UserSessionManager.isLoggedIn()) {
            Timber.tag("CachedDownloadsRepository").d("syncUnsyncedToSupabase: User not logged in, skipping")
            return Result.success(0)
        }
        
        return try {
            val userId = UserSessionManager.getUserId() ?: return Result.failure(
                IllegalStateException("User ID is null")
            )
            
            Timber.tag("CachedDownloadsRepository").d("Syncing unsynced cached downloads to Supabase...")
            
            // Get unsynced local cached downloads
            val unsyncedDownloads = localCachedDownloadsDao.getUnsyncedDownloads()
            if (unsyncedDownloads.isEmpty()) {
                Timber.tag("CachedDownloadsRepository").d("No unsynced cached downloads to upload")
                return Result.success(0)
            }
            
            var syncedCount = 0
            unsyncedDownloads.forEach { download ->
                // Fetch metadata
                val song = database.getSongById(download.songId)
                val source = when {
                    download.songId.startsWith("saavn:") -> "jiosaavn"
                    download.songId.startsWith("spotify:") -> "spotify"
                    else -> "youtube"
                }
                
                val result = supabaseCachedDownloadsService.addCachedDownload(
                    userId = userId,
                    songId = download.songId,
                    title = song?.song?.title,
                    thumbnailUrl = song?.song?.thumbnailUrl,
                    source = source
                )
                
                if (result.isSuccess) {
                    syncedCount++
                    localCachedDownloadsDao.updateSyncStatus(download.songId, isSynced = true)
                }
            }
            
            Timber.tag("CachedDownloadsRepository").d("Synced $syncedCount unsynced cached downloads to Supabase")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Timber.tag("CachedDownloadsRepository").e(e, "Failed to sync unsynced cached downloads to Supabase")
            Result.failure(e)
        }
    }
    
    /**
     * Get all synced songs for auto-download
     * Returns a list of MediaMetadata for songs in cached downloads
     */
    suspend fun getCachedSongsForDownload(): List<com.musifly.android.models.MediaMetadata> {
        return try {
            val songIds = localCachedDownloadsDao.getAllCachedDownloadIds()
            val songs = mutableListOf<com.musifly.android.models.MediaMetadata>()
            
            songIds.forEach { songId ->
                try {
                    val songEntity = database.getSongById(songId)
                    if (songEntity != null) {
                        songs.add(songEntity.toMediaMetadata())
                    }
                } catch (e: Exception) {
                    Timber.tag("CachedDownloadsRepository").w(e, "⚠️ Error fetching song details for download: $songId")
                }
            }
            songs
        } catch (e: Exception) {
            Timber.tag("CachedDownloadsRepository").e(e, "❌ Error getting synced songs for download")
            emptyList()
        }
    }

    /**
     * Clear all user data from local storage (called on logout)
     * This clears the user_cached_downloads table
     */
    suspend fun clearUserData() {
        try {
            Timber.tag("CachedDownloadsRepository").d("🧹 Starting to clear all user cached downloads data...")
            
            // Clear from local_cached_downloads table
            localCachedDownloadsDao.deleteAll()
            
            Timber.tag("CachedDownloadsRepository").i("✅ Cleared all user cached downloads from local storage")
        } catch (e: Exception) {
            Timber.tag("CachedDownloadsRepository").e(e, "❌ Failed to clear user cached downloads")
        }
    }
}
