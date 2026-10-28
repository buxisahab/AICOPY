package com.musifly.android.sync.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user cached downloads (Firebase-synced)
 */
@Dao
interface LocalUserCachedDownloadsDao {
    
    @Query("SELECT * FROM user_cached_downloads ORDER BY cachedAt DESC")
    fun getAllCachedDownloads(): Flow<List<UserCachedDownloadEntity>>
    
    @Query("SELECT songId FROM user_cached_downloads")
    suspend fun getAllCachedDownloadIds(): List<String>
    
    @Query("SELECT songId FROM user_cached_downloads")
    fun getAllCachedDownloadIdsFlow(): Flow<List<String>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_cached_downloads WHERE songId = :songId)")
    suspend fun isCached(songId: String): Boolean
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_cached_downloads WHERE songId = :songId)")
    fun isCachedFlow(songId: String): Flow<Boolean>
    
    @Query("SELECT * FROM user_cached_downloads WHERE songId = :songId")
    suspend fun getCachedDownload(songId: String): UserCachedDownloadEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cachedDownload: UserCachedDownloadEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cachedDownloads: List<UserCachedDownloadEntity>)
    
    @Query("DELETE FROM user_cached_downloads WHERE songId = :songId")
    suspend fun delete(songId: String)
    
    @Query("DELETE FROM user_cached_downloads")
    suspend fun deleteAll()
    
    @Query("UPDATE user_cached_downloads SET isSynced = :isSynced WHERE songId = :songId")
    suspend fun updateSyncStatus(songId: String, isSynced: Boolean)
    
    @Query("SELECT * FROM user_cached_downloads WHERE isSynced = 0")
    suspend fun getUnsyncedDownloads(): List<UserCachedDownloadEntity>
    
    @Query("SELECT COUNT(*) FROM user_cached_downloads")
    suspend fun getCachedDownloadsCount(): Int
    
    @Transaction
    suspend fun toggleCache(songId: String, isCached: Boolean) {
        if (isCached) {
            insert(UserCachedDownloadEntity(songId = songId, isSynced = false))
        } else {
            delete(songId)
        }
    }
}
