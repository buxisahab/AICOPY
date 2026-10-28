package com.musifly.android.sync.supabase

import com.musifly.android.sync.firestore.FirestoreCachedDownloadsService
import com.musifly.android.sync.firestore.FirestoreLikesService
import com.musifly.android.sync.firestore.FirestorePlaylistService
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseMigrationService @Inject constructor(
    private val supabase: SupabaseClient,
    private val firestoreLikesService: FirestoreLikesService,
    private val firestorePlaylistService: FirestorePlaylistService,
    private val firestoreCachedDownloadsService: FirestoreCachedDownloadsService,
    private val supabaseLikesService: SupabaseLikesService,
    private val supabasePlaylistService: SupabasePlaylistService,
    private val supabaseCachedDownloadsService: SupabaseCachedDownloadsService
) {
    suspend fun migrateIfNeeded(userId: String) = withContext(Dispatchers.IO) {
        try {
            // 1. Check if already migrated
            val activityResult = supabase.from("user_activity").select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeSingleOrNull<SupabaseUserActivity>()

            if (activityResult?.isMigrated == true) {
                Timber.tag("SupabaseMigration").d("✅ User $userId already migrated. Skipping.")
                return@withContext
            }

            Timber.tag("SupabaseMigration").d("🚀 Starting migration for user $userId from Firestore to Supabase...")

            // 2. Migrate Liked Songs
            val likedSongsResult = firestoreLikesService.getAllLikedSongsWithMetadata(userId)
            likedSongsResult.fold(
                onSuccess = { likedSongs ->
                    if (likedSongs.isNotEmpty()) {
                        val supabaseLikes = likedSongs.map {
                            SupabaseLike(
                                userId = userId,
                                songId = it.songId,
                                title = it.title,
                                source = it.source,
                                thumbnailUrl = it.thumbnailUrl,
                                likedAt = formatTimestamp(it.addedAt)
                            )
                        }
                        supabaseLikesService.addLikes(supabaseLikes)
                        Timber.tag("SupabaseMigration").d("✅ Migrated ${likedSongs.size} likes")
                    }
                },
                onFailure = { e ->
                    Timber.tag("SupabaseMigration").e(e, "❌ Failed to fetch likes from Firestore")
                }
            )

            // 3. Migrate Playlists
            val playlistsResult = firestorePlaylistService.getAllPlaylists(userId)
            playlistsResult.fold(
                onSuccess = { playlists ->
                    playlists.forEach { playlist ->
                        // Create playlist in Supabase
                        supabasePlaylistService.createPlaylist(userId, playlist.playlistId, playlist.name)
                        
                        // Fetch songs for this playlist
                        val songsResult = firestorePlaylistService.getPlaylistSongs(userId, playlist.playlistId)
                        songsResult.fold(
                            onSuccess = { songs ->
                                if (songs.isNotEmpty()) {
                                    val supabaseSongs = songs.map {
                                        SupabasePlaylistSong(
                                            playlistId = playlist.playlistId,
                                            songId = it.songId,
                                            title = it.title,
                                            order = it.order,
                                            addedAt = formatTimestamp(it.addedAt)
                                        )
                                    }
                                    supabasePlaylistService.addSongsToPlaylist(playlist.playlistId, supabaseSongs)
                                }
                            },
                            onFailure = { e ->
                                Timber.tag("SupabaseMigration").e(e, "❌ Failed to fetch songs for playlist ${playlist.playlistId}")
                            }
                        )
                    }
                    Timber.tag("SupabaseMigration").d("✅ Migrated ${playlists.size} playlists")
                },
                onFailure = { e ->
                    Timber.tag("SupabaseMigration").e(e, "❌ Failed to fetch playlists from Firestore")
                }
            )

            // 4. Migrate Cached Downloads
            val cachedResult = firestoreCachedDownloadsService.getAllCachedDownloadsWithMetadata(userId)
            cachedResult.fold(
                onSuccess = { downloads ->
                    downloads.forEach { download ->
                        supabaseCachedDownloadsService.addCachedDownload(
                            userId = userId,
                            songId = download.songId,
                            title = download.title,
                            thumbnailUrl = download.thumbnailUrl,
                            source = download.source,
                            cachedAt = formatTimestamp(download.cachedAt)
                        )
                    }
                    Timber.tag("SupabaseMigration").d("✅ Migrated ${downloads.size} cached downloads")
                },
                onFailure = { e ->
                    Timber.tag("SupabaseMigration").e(e, "❌ Failed to fetch cached downloads from Firestore")
                }
            )

            // 5. Mark as migrated
            val activity = activityResult?.copy(isMigrated = true) ?: SupabaseUserActivity(
                userId = userId,
                userType = "logged_in",
                isMigrated = true
            )
            
            supabase.from("user_activity").upsert(listOf(activity)) {
                onConflict = "user_id"
            }
            Timber.tag("SupabaseMigration").d("✅ Migration completed and flag set for $userId")

        } catch (e: Exception) {
            Timber.tag("SupabaseMigration").e(e, "❌ Critical error during migration for $userId")
        }
    }

    private fun formatTimestamp(timestamp: Long?): String? {
        if (timestamp == null) return null
        return try {
            java.time.Instant.ofEpochMilli(timestamp).toString()
        } catch (e: Exception) {
            null
        }
    }
}
