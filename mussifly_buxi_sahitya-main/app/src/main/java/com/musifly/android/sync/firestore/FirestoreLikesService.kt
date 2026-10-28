package com.musifly.android.sync.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Service for managing liked songs in Firestore
 * Handles CRUD operations for user's liked songs
 */
class FirestoreLikesService {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Data class for liked song with metadata
     * Matches Firestore schema: users/{uid}/liked_songs/{songId}
     */
    data class LikedSongMetadata(
        val songId: String,
        val title: String?,
        val thumbnailUrl: String? = null,
        val source: String? = null, // e.g., "youtube", "jiosaavn"
        val addedAt: Long? = null
    )
    
    /**
     * Get all liked songs with metadata from Firestore
     * Returns both IDs and metadata for offline access
     */
    suspend fun getAllLikedSongsWithMetadata(userId: String): Result<List<LikedSongMetadata>> {
        return try {
            Timber.tag("FirestoreLikesService").d("📥 Reading liked_songs from Firestore: users/$userId/liked_songs")
            
            // Ensure user document exists
            FirestoreAutoInitService.ensureUserDocument(userId).getOrThrow()
            
            // Try new collection first
            val newSnapshot = firestore
                .collection("users")
                .document(userId)
                .collection("liked_songs")
                .get()
                .await()
            
            val likedSongs = mutableListOf<LikedSongMetadata>()
            
            // Parse new collection
            newSnapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: return@forEach
                    likedSongs.add(
                        LikedSongMetadata(
                            songId = data["songId"] as? String ?: doc.id,
                            title = data["title"] as? String,
                            thumbnailUrl = data["thumbnailUrl"] as? String,
                            source = data["source"] as? String,
                            addedAt = (data["addedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
                                ?: (data["addedAt"] as? Long)
                        )
                    )
                } catch (e: Exception) {
                    Timber.tag("FirestoreLikesService").w(e, "Failed to parse liked song: ${doc.id}")
                }
            }
            
            // If new collection is empty, try old collection (migration)
            if (likedSongs.isEmpty()) {
                Timber.tag("FirestoreLikesService").d("📥 New collection empty, checking old 'likes' collection for migration...")
                try {
                    val oldSnapshot = firestore
                        .collection("users")
                        .document(userId)
                        .collection("likes")
                        .get()
                        .await()
                    
                    Timber.tag("FirestoreLikesService").d("📥 Found ${oldSnapshot.documents.size} songs in old 'likes' collection - migrating...")
                    
                    // Migrate from old collection to new
                    val batch = firestore.batch()
                    oldSnapshot.documents.forEach { doc ->
                        try {
                            val data = doc.data ?: return@forEach
                            val songId = data["songId"] as? String ?: doc.id
                            val title = data["title"] as? String
                            val source = data["source"] as? String ?: "youtube"
                            
                            // Add to new collection
                            val newRef = firestore
                                .collection("users")
                                .document(userId)
                                .collection("liked_songs")
                                .document(songId)
                            
                            val newData = mutableMapOf<String, Any>(
                                "songId" to songId,
                                "addedAt" to com.google.firebase.Timestamp.now()
                            )
                            title?.let { newData["title"] = it }
                            source?.let { newData["source"] = it }
                            
                            batch.set(newRef, newData, SetOptions.merge())
                            
                            // Add to result list
                            likedSongs.add(
                                LikedSongMetadata(
                                    songId = songId,
                                    title = title,
                                    thumbnailUrl = data["thumbnailUrl"] as? String,
                                    source = source,
                                    addedAt = System.currentTimeMillis()
                                )
                            )
                        } catch (e: Exception) {
                            Timber.tag("FirestoreLikesService").w(e, "Failed to migrate song: ${doc.id}")
                        }
                    }
                    
                    // Commit migration batch
                    if (oldSnapshot.documents.isNotEmpty()) {
                        batch.commit().await()
                        Timber.tag("FirestoreLikesService").d("✅ Migrated ${oldSnapshot.documents.size} songs from 'likes' to 'liked_songs'")
                    }
                } catch (e: Exception) {
                    Timber.tag("FirestoreLikesService").w(e, "Failed to read old 'likes' collection (may not exist)")
                }
            }
            
            Timber.tag("FirestoreLikesService").d("✅ Retrieved ${likedSongs.size} liked songs from Firestore for user: $userId")
            if (likedSongs.isNotEmpty()) {
                Timber.tag("FirestoreLikesService").d("📋 Song IDs: ${likedSongs.take(10).map { it.songId }.joinToString()}${if (likedSongs.size > 10) "..." else ""}")
            }
            Result.success(likedSongs)
        } catch (e: Exception) {
            Timber.tag("FirestoreLikesService").e(e, "❌ Failed to get liked songs from Firestore: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get all liked song IDs for current user (backward compatibility)
     */
    suspend fun getAllLikedSongIds(userId: String): Result<List<String>> {
        return getAllLikedSongsWithMetadata(userId).map { it.map { song -> song.songId } }
    }
    
    /**
     * Check if a song is liked
     */
    suspend fun isLiked(userId: String, songId: String): Result<Boolean> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("liked_songs")
                .document(songId)
                .get()
                .await()
            
            Result.success(snapshot.exists())
        } catch (e: Exception) {
            Timber.tag("FirestoreLikesService").e(e, "Failed to check if song is liked")
            Result.failure(e)
        }
    }
    
    /**
     * Like a song (add to Firestore with metadata)
     * CRITICAL: This is the source of truth for logged-in users
     * Stores minimal metadata: songId, title (optional), source, addedAt
     * Path: users/{uid}/liked_songs/{songId}
     */
    suspend fun likeSong(
        userId: String, 
        songId: String,
        title: String? = null,
        thumbnailUrl: String? = null,
        source: String? = null
    ): Result<Unit> {
        return try {
            val path = "users/$userId/liked_songs/$songId"
            Timber.tag("FirestoreLikesService").d("🔥 Firestore WRITE: $path")
            
            // Ensure user document exists
            FirestoreAutoInitService.ensureUserDocument(userId).getOrThrow()
            
            val likeData = mutableMapOf<String, Any>(
                "songId" to songId,
                "addedAt" to com.google.firebase.Timestamp.now()
            )
            
            // Add optional metadata
            title?.let { likeData["title"] = it }
            thumbnailUrl?.let { likeData["thumbnailUrl"] = it }
            source?.let { likeData["source"] = it }
            
            firestore
                .collection("users")
                .document(userId)
                .collection("liked_songs")
                .document(songId)
                .set(likeData, SetOptions.merge())
                .await()
            
            Timber.tag("FirestoreLikesService").d("✅ SUCCESS: Liked song written to Firestore: $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestoreLikesService").e(e, "❌ FAILED: Could not write like to Firestore: users/$userId/liked_songs/$songId")
            Result.failure(e)
        }
    }
    
    /**
     * Unlike a song (remove from Firestore)
     * CRITICAL: This is the source of truth for logged-in users
     * Path: users/{uid}/liked_songs/{songId}
     */
    suspend fun unlikeSong(userId: String, songId: String): Result<Unit> {
        return try {
            val path = "users/$userId/liked_songs/$songId"
            Timber.tag("FirestoreLikesService").d("🔥 Firestore DELETE: $path")
            
            firestore
                .collection("users")
                .document(userId)
                .collection("liked_songs")
                .document(songId)
                .delete()
                .await()
            
            Timber.tag("FirestoreLikesService").d("✅ SUCCESS: Unlike written to Firestore: $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestoreLikesService").e(e, "❌ FAILED: Could not remove like from Firestore: users/$userId/liked_songs/$songId")
            Result.failure(e)
        }
    }
    
    /**
     * Sync all liked songs from Firestore to local
     * Returns list of song IDs (backward compatibility)
     */
    suspend fun syncAllToLocal(userId: String): Result<List<String>> {
        return getAllLikedSongIds(userId)
    }
    
    /**
     * Sync local liked songs to Firestore
     * Returns count of synced songs
     */
    suspend fun syncLocalToFirestore(userId: String, songIds: List<String>): Result<Int> {
        return try {
            // Ensure user document exists
            FirestoreAutoInitService.ensureUserDocument(userId).getOrThrow()
            
            var syncedCount = 0
            val batch = firestore.batch()
            
            songIds.forEach { songId ->
                val likeRef = firestore
                    .collection("users")
                    .document(userId)
                    .collection("liked_songs")
                    .document(songId)
                
                val likeData = mapOf(
                    "songId" to songId,
                    "addedAt" to com.google.firebase.Timestamp.now()
                )
                
                batch.set(likeRef, likeData, SetOptions.merge())
                syncedCount++
            }
            
            batch.commit().await()
            Timber.tag("FirestoreLikesService").d("🔥 Firestore WRITE: Synced $syncedCount liked songs to Firestore")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Timber.tag("FirestoreLikesService").e(e, "Failed to sync local likes to Firestore")
            Result.failure(e)
        }
    }
}

