package com.musifly.android.sync.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseBookmarksService @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        const val TABLE_ALBUM_BOOKMARKS = "album_bookmarks"
        const val TABLE_ARTIST_BOOKMARKS = "artist_bookmarks"
    }

    // --- Album Bookmarks ---

    suspend fun bookmarkAlbum(bookmark: SupabaseAlbumBookmark): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from(TABLE_ALBUM_BOOKMARKS).upsert(bookmark) {
                onConflict = "user_id,album_id"
            }
            Timber.tag("SupabaseBookmarks").d("✅ Bookmarked album in Supabase: ${bookmark.albumId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabaseBookmarks").e(e, "❌ Failed to bookmark album: ${bookmark.albumId}")
            Result.failure(e)
        }
    }

    suspend fun unbookmarkAlbum(userId: String, albumId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from(TABLE_ALBUM_BOOKMARKS).delete {
                filter {
                    eq("user_id", userId)
                    eq("album_id", albumId)
                }
            }
            Timber.tag("SupabaseBookmarks").d("✅ Unbookmarked album in Supabase: $albumId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabaseBookmarks").e(e, "❌ Failed to unbookmark album: $albumId")
            Result.failure(e)
        }
    }

    suspend fun getUserAlbumBookmarks(userId: String): Result<List<SupabaseAlbumBookmark>> = withContext(Dispatchers.IO) {
        try {
            val bookmarks = supabase.from(TABLE_ALBUM_BOOKMARKS).select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<SupabaseAlbumBookmark>()
            Result.success(bookmarks)
        } catch (e: Exception) {
            Timber.tag("SupabaseBookmarks").e(e, "❌ Failed to fetch user album bookmarks")
            Result.failure(e)
        }
    }

    // --- Artist Bookmarks ---

    suspend fun bookmarkArtist(bookmark: SupabaseArtistBookmark): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from(TABLE_ARTIST_BOOKMARKS).upsert(bookmark) {
                onConflict = "user_id,artist_id"
            }
            Timber.tag("SupabaseBookmarks").d("✅ Bookmarked artist in Supabase: ${bookmark.artistId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabaseBookmarks").e(e, "❌ Failed to bookmark artist: ${bookmark.artistId}")
            Result.failure(e)
        }
    }

    suspend fun unbookmarkArtist(userId: String, artistId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from(TABLE_ARTIST_BOOKMARKS).delete {
                filter {
                    eq("user_id", userId)
                    eq("artist_id", artistId)
                }
            }
            Timber.tag("SupabaseBookmarks").d("✅ Unbookmarked artist in Supabase: $artistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabaseBookmarks").e(e, "❌ Failed to unbookmark artist: $artistId")
            Result.failure(e)
        }
    }

    suspend fun getUserArtistBookmarks(userId: String): Result<List<SupabaseArtistBookmark>> = withContext(Dispatchers.IO) {
        try {
            val bookmarks = supabase.from(TABLE_ARTIST_BOOKMARKS).select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<SupabaseArtistBookmark>()
            Result.success(bookmarks)
        } catch (e: Exception) {
            Timber.tag("SupabaseBookmarks").e(e, "❌ Failed to fetch user artist bookmarks")
            Result.failure(e)
        }
    }

    // --- Playlist Bookmarks ---

    suspend fun bookmarkPlaylist(bookmark: SupabasePlaylistBookmark): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("playlist_bookmarks").upsert(bookmark) {
                onConflict = "user_id,playlist_id"
            }
            Timber.tag("SupabaseBookmarks").d("✅ Bookmarked playlist in Supabase: ${bookmark.playlistId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabaseBookmarks").e(e, "❌ Failed to bookmark playlist: ${bookmark.playlistId}")
            Result.failure(e)
        }
    }

    suspend fun unbookmarkPlaylist(userId: String, playlistId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from("playlist_bookmarks").delete {
                filter {
                    eq("user_id", userId)
                    eq("playlist_id", playlistId)
                }
            }
            Timber.tag("SupabaseBookmarks").d("✅ Unbookmarked playlist in Supabase: $playlistId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabaseBookmarks").e(e, "❌ Failed to unbookmark playlist: $playlistId")
            Result.failure(e)
        }
    }

    suspend fun getUserPlaylistBookmarks(userId: String): Result<List<SupabasePlaylistBookmark>> = withContext(Dispatchers.IO) {
        try {
            val bookmarks = supabase.from("playlist_bookmarks").select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<SupabasePlaylistBookmark>()
            Result.success(bookmarks)
        } catch (e: Exception) {
            Timber.tag("SupabaseBookmarks").e(e, "❌ Failed to fetch user playlist bookmarks")
            Result.failure(e)
        }
    }
}
