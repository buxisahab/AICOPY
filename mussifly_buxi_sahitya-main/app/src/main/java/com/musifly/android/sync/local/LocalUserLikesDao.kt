package com.musifly.android.sync.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user liked songs (Firebase-synced)
 */
@Dao
interface LocalUserLikesDao {
    
    @Query("SELECT * FROM user_liked_songs ORDER BY syncedAt DESC")
    fun getAllLikedSongs(): Flow<List<UserLikedSongEntity>>
    
    @Query("SELECT songId FROM user_liked_songs")
    suspend fun getAllLikedSongIds(): List<String>
    
    @Query("SELECT songId FROM user_liked_songs")
    fun getAllLikedSongIdsFlow(): Flow<List<String>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_liked_songs WHERE songId = :songId)")
    suspend fun isLiked(songId: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_liked_songs WHERE songId = :songId)")
    fun isLikedFlow(songId: String): Flow<Boolean>

    @Query("SELECT isSynced FROM user_liked_songs WHERE songId = :songId")
    suspend fun isSynced(songId: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(likedSong: UserLikedSongEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(likedSongs: List<UserLikedSongEntity>)
    
    @Query("DELETE FROM user_liked_songs WHERE songId = :songId")
    suspend fun delete(songId: String)
    
    @Query("DELETE FROM user_liked_songs")
    suspend fun deleteAll()
    
    @Query("UPDATE user_liked_songs SET isSynced = :isSynced WHERE songId = :songId")
    suspend fun updateSyncStatus(songId: String, isSynced: Boolean)
    
    @Query("SELECT * FROM user_liked_songs WHERE isSynced = 0")
    suspend fun getUnsyncedLikes(): List<UserLikedSongEntity>
    
    @Query("SELECT COUNT(*) FROM user_liked_songs")
    suspend fun getLikedSongsCount(): Int
    
    @Transaction
    suspend fun toggleLike(songId: String, isLiked: Boolean) {
        if (isLiked) {
            insert(UserLikedSongEntity(songId = songId, isSynced = false))
        } else {
            delete(songId)
        }
    }
}
