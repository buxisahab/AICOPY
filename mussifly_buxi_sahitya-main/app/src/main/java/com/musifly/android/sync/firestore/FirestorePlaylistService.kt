package com.musifly.android.sync.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.musifly.android.sync.local.UserPlaylistEntity
import com.musifly.android.sync.local.UserPlaylistSongEntity
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Service for managing playlists in Firestore
 * Handles CRUD operations for user's playlists
 */
/**
 * Data class for playlist song from Firestore
 */
data class FirestorePlaylistSong(
    val songId: String,
    val title: String?,
    val order: Int,
    val addedAt: Long? = null
)

class FirestorePlaylistService {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Get all playlists for current user
     */
    suspend fun getAllPlaylists(userId: String): Result<List<UserPlaylistEntity>> {
        return try {
            Timber.tag("FirestorePlaylistService").d("📥 Reading playlists from Firestore: users/$userId/playlists")
            
            // Ensure user document exists
            FirestoreAutoInitService.ensureUserDocument(userId).getOrThrow()
            
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("playlists")
                .get()
                .await()
            
            val playlists = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    // Handle createdAt as Timestamp or Long
                    val createdAt = when (val ts = data["createdAt"]) {
                        is com.google.firebase.Timestamp -> ts.toDate().time
                        is Long -> ts
                        else -> System.currentTimeMillis()
                    }
                    UserPlaylistEntity(
                        playlistId = doc.id,
                        name = data["name"] as? String ?: "",
                        createdAt = createdAt,
                        updatedAt = (data["updatedAt"] as? Long) ?: System.currentTimeMillis(),
                        isSynced = true,
                        userId = userId
                    )
                } catch (e: Exception) {
                    Timber.tag("FirestorePlaylistService").w(e, "Failed to parse playlist: ${doc.id}")
                    null
                }
            }
            
            Timber.tag("FirestorePlaylistService").d("✅ Retrieved ${playlists.size} playlists from Firestore for user: $userId")
            if (playlists.isNotEmpty()) {
                Timber.tag("FirestorePlaylistService").d("📋 Playlist names: ${playlists.take(5).map { it.name }.joinToString()}${if (playlists.size > 5) "..." else ""}")
            }
            Result.success(playlists)
        } catch (e: Exception) {
            Timber.tag("FirestorePlaylistService").e(e, "❌ Failed to get playlists from Firestore: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get playlist by ID
     */
    suspend fun getPlaylist(userId: String, playlistId: String): Result<UserPlaylistEntity?> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .get()
                .await()
            
            if (!snapshot.exists()) {
                return Result.success(null)
            }
            
            val data = snapshot.data ?: return Result.success(null)
            val playlist = UserPlaylistEntity(
                playlistId = snapshot.id,
                name = data["name"] as? String ?: "",
                createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis(),
                updatedAt = (data["updatedAt"] as? Long) ?: System.currentTimeMillis(),
                isSynced = true,
                userId = userId
            )
            
            Result.success(playlist)
        } catch (e: Exception) {
            Timber.tag("FirestorePlaylistService").e(e, "Failed to get playlist")
            Result.failure(e)
        }
    }
    
    /**
     * Get songs in a playlist
     */
    suspend fun getPlaylistSongs(userId: String, playlistId: String): Result<List<FirestorePlaylistSong>> {
        return try {
            val snapshot = firestore
                .collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .collection("songs")
                //.orderBy("order") // Removed strict DB ordering to ensure we get all docs even with schema mismatch
                .get()
                .await()
            
            // Sort in memory to handle both "order" and "position" (legacy)
            val sortedDocs = snapshot.documents.sortedBy { doc ->
                val data = doc.data
                // Try 'order' first, then 'position' (legacy), then default to 0
                (data?.get("order") as? Long)?.toInt() 
                    ?: (data?.get("order") as? Int)
                    ?: (data?.get("position") as? Long)?.toInt()
                    ?: (data?.get("position") as? Int)
                    ?: 0
            }
            
            val songs = sortedDocs.mapNotNull { doc ->
                val songId = doc.data?.get("songId") as? String ?: doc.id
                val title = doc.data?.get("title") as? String
                
                // Get order again for the object
                val data = doc.data
                val order = (data?.get("order") as? Long)?.toInt() 
                    ?: (data?.get("order") as? Int)
                    ?: (data?.get("position") as? Long)?.toInt()
                    ?: (data?.get("position") as? Int)
                    ?: 0
                    
                val addedAt = when (val ts = data?.get("addedAt")) {
                    is com.google.firebase.Timestamp -> ts.toDate().time
                    is Long -> ts
                    else -> null
                }
                    
                FirestorePlaylistSong(songId, title, order, addedAt)
            }
            
            Result.success(songs)
        } catch (e: Exception) {
            Timber.tag("FirestorePlaylistService").e(e, "Failed to get playlist songs")
            Result.failure(e)
        }
    }
    
    /**
     * Create a new playlist
     * CRITICAL: This is the source of truth for logged-in users
     */
    suspend fun createPlaylist(userId: String, playlist: UserPlaylistEntity): Result<Unit> {
        return try {
            val path = "users/$userId/playlists/${playlist.playlistId}"
            Timber.tag("FirestorePlaylistService").d("🔥 Firestore WRITE: $path")
            
            // Ensure user document exists
            FirestoreAutoInitService.ensureUserDocument(userId).getOrThrow()
            
            val playlistData = mapOf(
                "name" to playlist.name,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore
                .collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlist.playlistId)
                .set(playlistData, SetOptions.merge())
                .await()
            
            Timber.tag("FirestorePlaylistService").d("✅ SUCCESS: Playlist written to Firestore: $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestorePlaylistService").e(e, "❌ FAILED: Could not write playlist to Firestore: users/$userId/playlists/${playlist.playlistId}")
            Result.failure(e)
        }
    }
    
    /**
     * Update playlist name
     * CRITICAL: This is the source of truth for logged-in users
     */
    suspend fun updatePlaylistName(userId: String, playlistId: String, newName: String): Result<Unit> {
        return try {
            Timber.tag("FirestorePlaylistService").d("🔥 Updating playlist name in Firestore: users/$userId/playlists/$playlistId")
            
            firestore
                .collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .update(
                    mapOf(
                        "name" to newName,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            
            Timber.tag("FirestorePlaylistService").d("✅ SUCCESS: Playlist name updated in Firestore: users/$userId/playlists/$playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestorePlaylistService").e(e, "❌ FAILED: Could not update playlist name in Firestore: users/$userId/playlists/$playlistId")
            Result.failure(e)
        }
    }
    
    /**
     * Delete playlist
     * CRITICAL: This is the source of truth for logged-in users
     */
    suspend fun deletePlaylist(userId: String, playlistId: String): Result<Unit> {
        return try {
            val path = "users/$userId/playlists/$playlistId"
            Timber.tag("FirestorePlaylistService").d("🔥 Firestore DELETE: $path")
            
            // Delete all songs in playlist first
            val songsSnapshot = firestore
                .collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .collection("songs")
                .get()
                .await()
            
            val batch = firestore.batch()
            songsSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            // Delete playlist document
            batch.delete(
                firestore
                    .collection("users")
                    .document(userId)
                    .collection("playlists")
                    .document(playlistId)
            )
            
            batch.commit().await()
            Timber.tag("FirestorePlaylistService").d("✅ SUCCESS: Playlist deleted from Firestore: $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestorePlaylistService").e(e, "❌ FAILED: Could not delete playlist from Firestore: users/$userId/playlists/$playlistId")
            Result.failure(e)
        }
    }
    
    /**
     * Add song to playlist
     * CRITICAL: This is the source of truth for logged-in users
     */
    suspend fun addSongToPlaylist(
        userId: String, 
        playlistId: String, 
        songId: String, 
        order: Int,
        title: String? = null
    ): Result<Unit> {
        return try {
            val path = "users/$userId/playlists/$playlistId/songs/$songId"
            Timber.tag("FirestorePlaylistService").d("🔥 Firestore WRITE: $path")
            
            val songData = mutableMapOf<String, Any>(
                "songId" to songId,
                "order" to order,
                "addedAt" to com.google.firebase.Timestamp.now()
            )
            
            // Add optional title
            title?.let { songData["title"] = it }
            
            firestore
                .collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .collection("songs")
                .document(songId)
                .set(songData, SetOptions.merge())
                .await()
            
            Timber.tag("FirestorePlaylistService").d("✅ SUCCESS: Song added to playlist in Firestore: $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestorePlaylistService").e(e, "❌ FAILED: Could not add song to playlist in Firestore: users/$userId/playlists/$playlistId/songs/$songId")
            Result.failure(e)
        }
    }

    /**
     * Add multiple songs to playlist (Batch)
     * CRITICAL: Efficiently adds multiple songs using WriteBatch
     */
    suspend fun addSongsToPlaylist(
        userId: String,
        playlistId: String,
        songs: List<FirestorePlaylistSong>
    ): Result<Unit> {
        return try {
            Timber.tag("FirestorePlaylistService").d("🔥 Firestore BATCH WRITE: Adding ${songs.size} songs to users/$userId/playlists/$playlistId")

            // Process in chunks of 500 (Firestore batch limit)
            songs.chunked(500).forEach { chunk ->
                val batch = firestore.batch()
                
                chunk.forEach { song ->
                    val songRef = firestore
                        .collection("users")
                        .document(userId)
                        .collection("playlists")
                        .document(playlistId)
                        .collection("songs")
                        .document(song.songId)
                    
                    val songData = mutableMapOf<String, Any>(
                        "songId" to song.songId,
                        "order" to song.order,
                        "addedAt" to com.google.firebase.Timestamp.now()
                    )
                    
                    song.title?.let { songData["title"] = it }
                    
                    batch.set(songRef, songData, SetOptions.merge())
                }
                
                batch.commit().await()
            }
            
            Timber.tag("FirestorePlaylistService").d("✅ SUCCESS: ${songs.size} songs added to playlist in Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestorePlaylistService").e(e, "❌ FAILED: Could not batch add songs to playlist in Firestore")
            Result.failure(e)
        }
    }
    
    /**
     * Remove song from playlist
     * CRITICAL: This is the source of truth for logged-in users
     */
    suspend fun removeSongFromPlaylist(userId: String, playlistId: String, songId: String): Result<Unit> {
        return try {
            val path = "users/$userId/playlists/$playlistId/songs/$songId"
            Timber.tag("FirestorePlaylistService").d("🔥 Firestore DELETE: $path")
            
            firestore
                .collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .collection("songs")
                .document(songId)
                .delete()
                .await()
            
            Timber.tag("FirestorePlaylistService").d("✅ SUCCESS: Song removed from playlist in Firestore: $path")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestorePlaylistService").e(e, "❌ FAILED: Could not remove song from playlist in Firestore: users/$userId/playlists/$playlistId/songs/$songId")
            Result.failure(e)
        }
    }
    
    /**
     * Set all songs in playlist (replace existing)
     */
    suspend fun setPlaylistSongs(userId: String, playlistId: String, songIds: List<String>): Result<Unit> {
        return try {
            // Delete all existing songs
            val songsSnapshot = firestore
                .collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
                .collection("songs")
                .get()
                .await()
            
            val batch = firestore.batch()
            songsSnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            
            // Add new songs
            songIds.forEachIndexed { index, songId ->
                val songRef = firestore
                    .collection("users")
                    .document(userId)
                    .collection("playlists")
                    .document(playlistId)
                    .collection("songs")
                    .document(songId)
                
                val songData = mapOf(
                    "songId" to songId,
                    "order" to index,
                    "addedAt" to System.currentTimeMillis()
                )
                
                batch.set(songRef, songData, SetOptions.merge())
            }
            
            // Update playlist updatedAt
            val playlistRef = firestore
                .collection("users")
                .document(userId)
                .collection("playlists")
                .document(playlistId)
            
            batch.update(playlistRef, "updatedAt", System.currentTimeMillis())
            
            batch.commit().await()
            Timber.tag("FirestorePlaylistService").d("Set ${songIds.size} songs in playlist $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("FirestorePlaylistService").e(e, "Failed to set playlist songs")
            Result.failure(e)
        }
    }
}

