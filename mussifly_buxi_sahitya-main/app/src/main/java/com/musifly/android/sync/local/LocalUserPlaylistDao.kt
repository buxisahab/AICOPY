package com.musifly.android.sync.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user playlists (Firebase-synced)
 */
@Dao
interface LocalUserPlaylistDao {
    
    @Query("SELECT * FROM user_playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<UserPlaylistEntity>>
    
    @Query("SELECT * FROM user_playlists ORDER BY updatedAt DESC")
    suspend fun getAllPlaylistsSync(): List<UserPlaylistEntity>
    
    @Query("SELECT * FROM user_playlists WHERE playlistId = :playlistId")
    suspend fun getPlaylist(playlistId: String): UserPlaylistEntity?
    
    @Query("SELECT * FROM user_playlists WHERE playlistId = :playlistId")
    fun getPlaylistFlow(playlistId: String): Flow<UserPlaylistEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: UserPlaylistEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(playlists: List<UserPlaylistEntity>)
    
    @Query("DELETE FROM user_playlists WHERE playlistId = :playlistId")
    suspend fun delete(playlistId: String)
    
    @Query("DELETE FROM user_playlists")
    suspend fun deleteAll()
    
    @Query("UPDATE user_playlists SET name = :name, updatedAt = :updatedAt WHERE playlistId = :playlistId")
    suspend fun updateName(playlistId: String, name: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE user_playlists SET isSynced = :isSynced, updatedAt = :updatedAt WHERE playlistId = :playlistId")
    suspend fun updateSyncStatus(playlistId: String, isSynced: Boolean, updatedAt: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM user_playlists WHERE isSynced = 0")
    suspend fun getUnsyncedPlaylists(): List<UserPlaylistEntity>
    
    @Query("SELECT COUNT(*) FROM user_playlists")
    suspend fun getPlaylistsCount(): Int
}

/**
 * DAO for songs in user playlists
 */
@Dao
interface LocalUserPlaylistSongDao {
    
    @Query("SELECT * FROM user_playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistSongs(playlistId: String): List<UserPlaylistSongEntity>
    
    @Query("SELECT songId FROM user_playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistSongIds(playlistId: String): List<String>
    
    @Query("SELECT songId FROM user_playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getPlaylistSongIdsFlow(playlistId: String): Flow<List<String>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM user_playlist_songs WHERE playlistId = :playlistId AND songId = :songId)")
    suspend fun isSongInPlaylist(playlistId: String, songId: String): Boolean
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlistSong: UserPlaylistSongEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(playlistSongs: List<UserPlaylistSongEntity>)
    
    @Query("DELETE FROM user_playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun delete(playlistId: String, songId: String)
    
    @Query("DELETE FROM user_playlist_songs WHERE playlistId = :playlistId")
    suspend fun deleteAllFromPlaylist(playlistId: String)
    
    @Query("SELECT COUNT(*) FROM user_playlist_songs WHERE playlistId = :playlistId")
    suspend fun getPlaylistSongCount(playlistId: String): Int
    
    @Query("SELECT MAX(position) FROM user_playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxPosition(playlistId: String): Int?
    
    @Transaction
    suspend fun addSongToPlaylist(playlistId: String, songId: String) {
        val maxPos = getMaxPosition(playlistId) ?: -1
        insert(UserPlaylistSongEntity(
            playlistId = playlistId,
            songId = songId,
            position = maxPos + 1
        ))
    }
    
    @Transaction
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String) {
        delete(playlistId, songId)
        // Reorder remaining songs
        val remaining = getPlaylistSongs(playlistId)
        remaining.forEachIndexed { index, entity ->
            insert(entity.copy(position = index))
        }
    }
    
    @Transaction
    suspend fun setPlaylistSongs(playlistId: String, songIds: List<String>) {
        deleteAllFromPlaylist(playlistId)
        val songs = songIds.mapIndexed { index, songId ->
            UserPlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                position = index
            )
        }
        insertAll(songs)
    }

    @Query("SELECT DISTINCT songId FROM user_playlist_songs")
    suspend fun getAllUserPlaylistSongIds(): List<String>
}
