package com.musifly.android.sync.repository

import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.AlbumEntity
import com.musifly.android.db.entities.ArtistEntity
import com.musifly.android.sync.UserSessionManager
import com.musifly.android.sync.supabase.SupabaseAlbumBookmark
import com.musifly.android.sync.supabase.SupabaseArtistBookmark
import com.musifly.android.sync.supabase.SupabaseBookmarksService
import com.musifly.android.constants.AlbumSortType
import com.musifly.android.constants.ArtistSortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarksRepository @Inject constructor(
    private val database: MusicDatabase,
    private val supabaseService: SupabaseBookmarksService
) {
    /**
     * Toggle album bookmark (liked status)
     */
    suspend fun toggleAlbumBookmark(album: AlbumEntity, artists: List<ArtistEntity> = emptyList()) {
        val now = LocalDateTime.now()
        val isBookmarked = album.bookmarkedAt != null
        val userId = UserSessionManager.getUserId()
        val isLoggedIn = UserSessionManager.isLoggedIn()

        Timber.tag("BookmarksRepository").d("🔖 toggleAlbumBookmark: albumId=${album.id}, title=${album.title}, isBookmarked=$isBookmarked, isLoggedIn=$isLoggedIn, userId=$userId")

        val updatedAlbum = if (isBookmarked) {
            album.copy(bookmarkedAt = null)
        } else {
            album.copy(bookmarkedAt = now)
        }

        // 1. Update local database
        database.transaction {
            update(updatedAlbum)
        }
        Timber.tag("BookmarksRepository").d("✅ Local DB updated for album: ${album.id}")

        // 2. Sync with Supabase if logged in
        if (isLoggedIn && userId != null) {
            withContext(Dispatchers.IO) {
                try {
                    if (isBookmarked) {
                        Timber.tag("BookmarksRepository").d("🗑️ Unbookmarking album in Supabase: ${album.id}")
                        val result = supabaseService.unbookmarkAlbum(userId, album.id)
                        result.onSuccess {
                            Timber.tag("BookmarksRepository").d("✅ Album unbookmarked in Supabase: ${album.id}")
                        }.onFailure { e ->
                            Timber.tag("BookmarksRepository").e(e, "❌ Failed to unbookmark album in Supabase: ${album.id}")
                        }
                    } else {
                        val artistName = artists.joinToString(", ") { it.name }
                        val bookmark = SupabaseAlbumBookmark(
                            userId = userId,
                            albumId = album.id,
                            title = album.title,
                            thumbnailUrl = album.thumbnailUrl,
                            artistName = artistName
                        )
                        Timber.tag("BookmarksRepository").d("➕ Bookmarking album in Supabase: ${album.id}, artists=$artistName")
                        val result = supabaseService.bookmarkAlbum(bookmark)
                        result.onSuccess {
                            Timber.tag("BookmarksRepository").d("✅ Album bookmarked in Supabase: ${album.id}")
                        }.onFailure { e ->
                            Timber.tag("BookmarksRepository").e(e, "❌ Failed to bookmark album in Supabase: ${album.id}")
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag("BookmarksRepository").e(e, "❌ Exception during Supabase sync for album: ${album.id}")
                }
            }
        } else {
            Timber.tag("BookmarksRepository").d("⚠️ Skipping Supabase sync (not logged in or no userId)")
        }

        // 3. YouTube sync (legacy logic from toggleLike)
        withContext(Dispatchers.IO) {
            try {
                if (album.playlistId != null) {
                    com.musifly.innertube.YouTube.likePlaylist(album.playlistId, !isBookmarked)
                    Timber.tag("BookmarksRepository").d("✅ YouTube playlist synced: ${album.playlistId}")
                }
            } catch (e: Exception) {
                Timber.tag("BookmarksRepository").w(e, "Failed to sync album bookmark to YouTube")
            }
        }
    }

    /**
     * Toggle artist bookmark
     */
    suspend fun toggleArtistBookmark(artist: ArtistEntity) {
        val now = LocalDateTime.now()
        val isBookmarked = artist.bookmarkedAt != null
        val userId = UserSessionManager.getUserId()
        val isLoggedIn = UserSessionManager.isLoggedIn()

        val updatedArtist = if (isBookmarked) {
            artist.copy(bookmarkedAt = null)
        } else {
            artist.copy(bookmarkedAt = now)
        }

        // 1. Update local database
        database.transaction {
            update(updatedArtist)
        }

        // 2. Sync with Supabase if logged in
        if (isLoggedIn && userId != null) {
            withContext(Dispatchers.IO) {
                if (isBookmarked) {
                    supabaseService.unbookmarkArtist(userId, artist.id)
                } else {
                    val bookmark = SupabaseArtistBookmark(
                        userId = userId,
                        artistId = artist.id,
                        name = artist.name,
                        thumbnailUrl = artist.thumbnailUrl
                    )
                    supabaseService.bookmarkArtist(bookmark)
                }
            }
        }

        // 3. YouTube sync (legacy logic)
        withContext(Dispatchers.IO) {
            try {
                val channelId = artist.channelId ?: com.musifly.innertube.YouTube.getChannelId(artist.id)
                if (channelId != null) {
                    com.musifly.innertube.YouTube.subscribeChannel(channelId, !isBookmarked)
                }
            } catch (e: Exception) {
                Timber.tag("BookmarksRepository").w(e, "Failed to sync artist subscription to YouTube")
            }
        }
    }

    /**
     * Toggle playlist bookmark
     */
    suspend fun togglePlaylistBookmark(playlist: com.musifly.android.db.entities.PlaylistEntity) {
        val now = LocalDateTime.now()
        val isBookmarked = playlist.bookmarkedAt != null
        val userId = UserSessionManager.getUserId()
        val isLoggedIn = UserSessionManager.isLoggedIn()

        Timber.tag("BookmarksRepository").d("🔖 togglePlaylistBookmark: playlistId=${playlist.id}, name=${playlist.name}, isBookmarked=$isBookmarked, isLoggedIn=$isLoggedIn, userId=$userId")

        val updatedPlaylist = if (isBookmarked) {
            playlist.copy(bookmarkedAt = null)
        } else {
            playlist.copy(bookmarkedAt = now)
        }

        // 1. Update local database
        database.transaction {
            update(updatedPlaylist)
        }
        Timber.tag("BookmarksRepository").d("✅ Local DB updated for playlist: ${playlist.id}")

        // 2. Sync with Supabase if logged in
        if (isLoggedIn && userId != null) {
            withContext(Dispatchers.IO) {
                try {
                    if (isBookmarked) {
                        Timber.tag("BookmarksRepository").d("🗑️ Unbookmarking playlist in Supabase: ${playlist.id}")
                        val result = supabaseService.unbookmarkPlaylist(userId, playlist.id)
                        result.onSuccess {
                            Timber.tag("BookmarksRepository").d("✅ Playlist unbookmarked in Supabase: ${playlist.id}")
                        }.onFailure { e ->
                            Timber.tag("BookmarksRepository").e(e, "❌ Failed to unbookmark playlist in Supabase: ${playlist.id}")
                        }
                    } else {
                        val bookmark = com.musifly.android.sync.supabase.SupabasePlaylistBookmark(
                            userId = userId,
                            playlistId = playlist.id,
                            title = playlist.name,
                            thumbnailUrl = null, // Playlists don't have thumbnails in current schema
                            browseId = playlist.browseId
                        )
                        Timber.tag("BookmarksRepository").d("➕ Bookmarking playlist in Supabase: ${playlist.id}")
                        val result = supabaseService.bookmarkPlaylist(bookmark)
                        result.onSuccess {
                            Timber.tag("BookmarksRepository").d("✅ Playlist bookmarked in Supabase: ${playlist.id}")
                        }.onFailure { e ->
                            Timber.tag("BookmarksRepository").e(e, "❌ Failed to bookmark playlist in Supabase: ${playlist.id}")
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag("BookmarksRepository").e(e, "❌ Exception during Supabase sync for playlist: ${playlist.id}")
                }
            }
        } else {
            Timber.tag("BookmarksRepository").d("⚠️ Skipping Supabase sync (not logged in or no userId)")
        }

        // 3. YouTube sync (legacy logic)
        withContext(Dispatchers.IO) {
            try {
                if (playlist.browseId != null) {
                    com.musifly.innertube.YouTube.likePlaylist(playlist.browseId, !isBookmarked)
                    Timber.tag("BookmarksRepository").d("✅ YouTube playlist synced: ${playlist.browseId}")
                }
            } catch (e: Exception) {
                Timber.tag("BookmarksRepository").w(e, "Failed to sync playlist bookmark to YouTube")
            }
        }
    }

    /**
     * Sync bookmarks from Supabase to local DB
     */
    suspend fun syncFromSupabase(): Result<Int> = withContext(Dispatchers.IO) {
        val userId = UserSessionManager.getUserId() ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
        
        try {
            var totalSynced = 0
            
            // SYNC ALBUMS
            val albumBookmarksResult = supabaseService.getUserAlbumBookmarks(userId)
            albumBookmarksResult.onSuccess { supabaseAlbums ->
                database.transaction {
                    supabaseAlbums.forEach { sbAlbum ->
                        val existingAlbum = getAlbumById(sbAlbum.albumId)
                        if (existingAlbum == null) {
                            // Create minimal album entry
                            val albumEntity = AlbumEntity(
                                id = sbAlbum.albumId,
                                title = sbAlbum.title ?: "Unknown Album",
                                thumbnailUrl = sbAlbum.thumbnailUrl,
                                bookmarkedAt = LocalDateTime.now(),
                                songCount = 0,
                                duration = 0
                            )
                            insert(albumEntity)
                            Timber.tag("BookmarksRepository").d("✅ Created album from SB: ${sbAlbum.albumId}")
                            totalSynced++
                        } else if (existingAlbum.album.bookmarkedAt == null) {
                            update(existingAlbum.album.copy(bookmarkedAt = LocalDateTime.now()))
                            Timber.tag("BookmarksRepository").d("✅ Marked album bookmarked from SB: ${sbAlbum.albumId}")
                            totalSynced++
                        }
                    }
                }
            }

            // SYNC ARTISTS
            val artistBookmarksResult = supabaseService.getUserArtistBookmarks(userId)
            artistBookmarksResult.onSuccess { supabaseArtists ->
                database.transaction {
                    supabaseArtists.forEach { sbArtist ->
                        val existingArtist = getArtistById(sbArtist.artistId)
                        if (existingArtist == null) {
                            val artistEntity = ArtistEntity(
                                id = sbArtist.artistId,
                                name = sbArtist.name ?: "Unknown Artist",
                                thumbnailUrl = sbArtist.thumbnailUrl,
                                bookmarkedAt = LocalDateTime.now()
                            )
                            insert(artistEntity)
                            Timber.tag("BookmarksRepository").d("✅ Created artist from SB: ${sbArtist.artistId}")
                            totalSynced++
                        } else if (existingArtist.artist.bookmarkedAt == null) {
                            update(existingArtist.artist.copy(bookmarkedAt = LocalDateTime.now()))
                            Timber.tag("BookmarksRepository").d("✅ Marked artist bookmarked from SB: ${sbArtist.artistId}")
                            totalSynced++
                        }
                    }
                }
            }

            Result.success(totalSynced)
        } catch (e: Exception) {
            Timber.tag("BookmarksRepository").e(e, "❌ Failed to sync bookmarks from Supabase")
            Result.failure(e)
        }
    }
    /**
     * Sync unsynced local bookmarks to Supabase
     * Uploads all local bookmarks to Supabase on login
     */
    suspend fun syncUnsyncedToSupabase(): Result<Int> = withContext(Dispatchers.IO) {
        if (!UserSessionManager.isLoggedIn()) {
            return@withContext Result.success(0)
        }
        
        val userId = UserSessionManager.getUserId() ?: return@withContext Result.failure(IllegalStateException("Not logged in"))
        
        try {
            var syncedCount = 0
            
            // 1. Get all local bookmarked albums
            val localAlbums = database.albumsLiked(com.musifly.android.constants.AlbumSortType.CREATE_DATE, false).first()
            localAlbums.forEach { album ->
                val artistName = album.artists.joinToString(", ") { it.name }
                val bookmark = SupabaseAlbumBookmark(
                    userId = userId,
                    albumId = album.id,
                    title = album.album.title,
                    thumbnailUrl = album.album.thumbnailUrl,
                    artistName = artistName
                )
                val result = supabaseService.bookmarkAlbum(bookmark)
                if (result.isSuccess) syncedCount++
            }
            
            // 2. Get all local bookmarked artists
            val localArtists = database.artistsBookmarked(com.musifly.android.constants.ArtistSortType.CREATE_DATE, false).first()
            localArtists.forEach { artist ->
                val bookmark = SupabaseArtistBookmark(
                    userId = userId,
                    artistId = artist.id,
                    name = artist.artist.name,
                    thumbnailUrl = artist.artist.thumbnailUrl
                )
                val result = supabaseService.bookmarkArtist(bookmark)
                if (result.isSuccess) syncedCount++
            }
            
            Timber.tag("BookmarksRepository").d("✅ Synced $syncedCount bookmarks to Supabase")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Timber.tag("BookmarksRepository").e(e, "❌ Failed to sync unsynced bookmarks to Supabase")
            Result.failure(e)
        }
    }

    /**
     * Clear all user bookmarks from local storage (called on logout)
     */
    suspend fun clearUserData() {
        try {
            Timber.tag("BookmarksRepository").d("🧹 Clearing all user bookmarks...")
            database.transaction {
                clearAllBookmarks()
            }
            Timber.tag("BookmarksRepository").i("✅ Cleared all local bookmarks")
        } catch (e: Exception) {
            Timber.tag("BookmarksRepository").e(e, "❌ Failed to clear user bookmarks")
        }
    }
}
