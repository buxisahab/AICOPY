package com.musifly.android.sync.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Service for managing cached downloads in Firestore
 * Handles CRUD operations for user's cached downloads
 * Only stores song IDs and metadata - actual audio files stay in local storage
 */
class FirestoreCachedDownloadsService {
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Data class for cached download with metadata
     * Matches Firestore schema: users/{uid}/cached_downloads/{songId}
     */
    data class CachedDownloadMetadata(
        val songId: String,
        val title: String?,
        val thumbnailUrl: String? = null,
        val source: String? = null, // e.g., "youtube", "jiosaavn"
        val cachedAt: Long? = null
    )
    
    /**
     * Get all cached downloads with metadata from Firestore
     * Returns both IDs and metadata for offline access
     */
    suspend fun getAllCachedDownloadsWithMetadata(userId: String): Result<List<CachedDownloadMetadata>> {
        return try {
            Timber.tag("FirestoreCachedDownloadsService").d("📥 Reading cached_downloads from Firestore: users/$userId/cached_downloads")
            
            // Ensure user document exists
            FirestoreAutoInitService.ensureUserDocument(userId).getOrThrow()
            
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("cached_downloads")
                .get()
                .await()
            
            val cachedDownloads = mutableListOf<CachedDownloadMetadata>()
            
            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: return@forEach
                    cachedDownloads.add(
                        CachedDownloadMetadata(
                            songId = data["songId"] as? String ?: doc.id,
                            title = data["title"] as? String,
                            thumbnailUrl = data["thumbnailUrl"] as? String,
                            source = data["source"] as? String,
                            cachedAt = (data["cachedAt"] as? com.google.firebase.Timestamp)?.toDate()?.time
                                ?: (data["cachedAt"] as? Long)
                        )
                    )
                } catch (e: Exception) {
                    Timber.tag("FirestoreCachedDownloadsService").w(e, "Failed to parse cached download: ${doc.id}")
                }
            }
            
            Timber.tag("FirestoreCachedDownloadsService").d("✅ Retrieved ${cachedDownloads.size} cached downloads from Firestore for user: $userId")
            if (cachedDownloads.isNotEmpty()) {
                Timber.tag("FirestoreCachedDownloadsService").d("📋 Song IDs: ${cachedDownloads.take(10).map { it.songId }.joinToString()}${if (cachedDownloads.size > 10) "..." else ""}")
            }
            Result.success(cachedDownloads)
        } catch (e: Exception) {
            Timber.tag("FirestoreCachedDownloadsService").e(e, "❌ Failed to get cached downloads from Firestore: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get all cached download IDs for current user (backward compatibility)
     */
    suspend fun getAllCachedDownloadIds(userId: String): Result<List<String>> {
        return getAllCachedDownloadsWithMetadata(userId).map { it.map { download -> download.songId } }
    }
    
    /**
     * Check if a song is cached
     */
    suspend fun isCached(userId: String, songId: String): Result<Boolean> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("cached_downloads")
                .document(songId)
                .get()
                .await()
            
            Result.success(snapshot.exists())
        } catch (e: Exception) {
            Timber.tag("FirestoreCachedDownloadsService").e(e, "Failed to check if song is cached")
            Result.failure(e)
        }
    }
    
    /**
     * Add a cached download (add to Firestore with metadata)
     * CRITICAL: This is the source of truth for logged-in users
     * Stores minimal metadata: songId, title (optional), source, cachedAt
     * Path: users/{uid}/cached_downloads/{songId}
     */
    suspend fun addCachedDownload(
        userId: String, 
        songId: String,
        title: String? = null,
        thumbnailUrl: String? = null,
        source: String? = null
    ): Result<Unit> {
        return try {
            val path = "users/$userId/cached_downloads/$songId"
            Timber.tag("FirestoreCachedDownloadsService").d("🔥 Firestore WRITE: $path")
            
            // Ensure user document exists
            FirestoreAutoInitService.ensureUserDocument(userId).getOrThrow()
            
            val cacheData = mutableMapOf<String, Any>(
                "songId" to songId,
                "cachedAt" to com.google.firebase.Timestamp.now()
            )
            
            // Add optional metadata
            title?.let { cacheData["title"] = it }
            thumbnailUrl?.let { cacheData["thumbnailUrl"] = it }
            source?.let { cacheData["source"] = it }
            
            firestore
                .collection("users")
                .document(userId)
                .collection("cached_downloads")
                .document(songId)
                .set(cacheData, SetOptions.merge())
                .await()
            
            Timber.tag("FirestoreCachedDownloadsService").d("✅ SUCCESS: Cached download written to Firestore: $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestoreCachedDownloadsService").e(e, "❌ FAILED: Could not write cached download to Firestore: users/$userId/cached_downloads/$songId")
            Result.failure(e)
        }
    }
    
    /**
     * Remove a cached download (delete from Firestore)
     * CRITICAL: This is the source of truth for logged-in users
     * Path: users/{uid}/cached_downloads/{songId}
     */
    suspend fun removeCachedDownload(userId: String, songId: String): Result<Unit> {
        return try {
            val path = "users/$userId/cached_downloads/$songId"
            Timber.tag("FirestoreCachedDownloadsService").d("🔥 Firestore DELETE: $path")
            
            firestore
                .collection("users")
                .document(userId)
                .collection("cached_downloads")
                .document(songId)
                .delete()
                .await()
            
            Timber.tag("FirestoreCachedDownloadsService").d("✅ SUCCESS: Cached download removed from Firestore: $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestoreCachedDownloadsService").e(e, "❌ FAILED: Could not remove cached download from Firestore: users/$userId/cached_downloads/$songId")
            Result.failure(e)
        }
    }
    
    /**
     * Sync local cached downloads to Firestore
     * Returns count of synced downloads
     */
    suspend fun syncLocalToFirestore(userId: String, songIds: List<String>): Result<Int> {
        return try {
            // Ensure user document exists
            FirestoreAutoInitService.ensureUserDocument(userId).getOrThrow()
            
            var syncedCount = 0
            val batch = firestore.batch()
            
            songIds.forEach { songId ->
                val cacheRef = firestore
                    .collection("users")
                    .document(userId)
                    .collection("cached_downloads")
                    .document(songId)
                
                val cacheData = mapOf(
                    "songId" to songId,
                    "cachedAt" to com.google.firebase.Timestamp.now()
                )
                
                batch.set(cacheRef, cacheData, SetOptions.merge())
                syncedCount++
            }
            
            batch.commit().await()
            Timber.tag("FirestoreCachedDownloadsService").d("🔥 Firestore WRITE: Synced $syncedCount cached downloads to Firestore")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Timber.tag("FirestoreCachedDownloadsService").e(e, "Failed to sync local cached downloads to Firestore")
            Result.failure(e)
        }
    }
}
