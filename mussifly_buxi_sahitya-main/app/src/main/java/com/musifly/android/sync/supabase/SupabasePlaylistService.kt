package com.musifly.android.sync.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabasePlaylistService @Inject constructor(
    private val supabase: SupabaseClient
) {
    
    companion object {
        const val TABLE_PLAYLISTS = "playlists"
        const val TABLE_PLAYLIST_SONGS = "playlist_songs"
    }

    suspend fun createPlaylist(
        userId: String,
        playlistId: String,
        name: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val playlist = SupabasePlaylist(
                id = playlistId,
                userId = userId,
                name = name
            )
            supabase.from(TABLE_PLAYLISTS).upsert(listOf(playlist)) {
                onConflict = "id"
            }
            Timber.tag("SupabasePlaylist").d("✅ Created/Updated playlist: $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabasePlaylist").e(e, "❌ Failed to create playlist: $playlistId")
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Cascade delete handles songs, but explicitly deleting logic via API if needed
            supabase.from(TABLE_PLAYLISTS).delete {
                filter {
                    eq("id", playlistId)
                }
            }
            Timber.tag("SupabasePlaylist").d("✅ Deleted playlist: $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabasePlaylist").e(e, "❌ Failed to delete playlist: $playlistId")
            Result.failure(e)
        }
    }

    suspend fun addSongsToPlaylist(
        playlistId: String,
        songs: List<SupabasePlaylistSong>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (songs.isEmpty()) return@withContext Result.success(Unit)
            
            // Upsert songs in batch
            supabase.from(TABLE_PLAYLIST_SONGS).upsert(songs) {
                onConflict = "playlist_id,song_id"
            }
            Timber.tag("SupabasePlaylist").d("✅ Added ${songs.size} songs to playlist: $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabasePlaylist").e(e, "❌ Failed to add songs to playlist: $playlistId")
            Result.failure(e)
        }
    }

    suspend fun removeSongFromPlaylist(
        playlistId: String,
        songId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from(TABLE_PLAYLIST_SONGS).delete {
                filter {
                    eq("playlist_id", playlistId)
                    eq("song_id", songId)
                }
            }
            Timber.tag("SupabasePlaylist").d("✅ Removed song $songId from playlist: $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabasePlaylist").e(e, "❌ Failed to remove song from playlist: $playlistId")
            Result.failure(e)
        }
    }

    suspend fun getUserPlaylists(userId: String): Result<List<SupabasePlaylist>> = withContext(Dispatchers.IO) {
        try {
            val playlists = supabase.from(TABLE_PLAYLISTS).select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<SupabasePlaylist>()
            Result.success(playlists)
        } catch (e: Exception) {
            Timber.tag("SupabasePlaylist").e(e, "❌ Failed to fetch user playlists")
            Result.failure(e)
        }
    }

    suspend fun getPlaylistSongs(playlistId: String): Result<List<SupabasePlaylistSong>> = withContext(Dispatchers.IO) {
        try {
            val songs = supabase.from(TABLE_PLAYLIST_SONGS).select {
                filter {
                    eq("playlist_id", playlistId)
                }
                order("order", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            }.decodeList<SupabasePlaylistSong>()
            Result.success(songs)
        } catch (e: Exception) {
            Timber.tag("SupabasePlaylist").e(e, "❌ Failed to fetch playlist songs: $playlistId")
            Result.failure(e)
        }
    }
}
