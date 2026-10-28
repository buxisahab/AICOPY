package com.musifly.android.sync.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseCachedDownloadsService @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    suspend fun getAllCachedDownloads(userId: String): Result<List<SupabaseCachedDownload>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Timber.tag("SupabaseCachedDownloads").d("🔍 Fetching all cached downloads for user: $userId")
            val result = supabaseClient.from("cached_downloads")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<SupabaseCachedDownload>()
            Timber.tag("SupabaseCachedDownloads").d("✅ Fetched ${result.size} cached downloads")
            Result.success(result)
        } catch (e: Exception) {
            Timber.tag("SupabaseCachedDownloads").e(e, "❌ Error fetching cached downloads for $userId")
            Result.failure(e)
        }
    }

    suspend fun addCachedDownload(
        userId: String,
        songId: String,
        title: String? = null,
        thumbnailUrl: String? = null,
        source: String? = null,
        cachedAt: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Timber.tag("SupabaseCachedDownloads").d("➕ Adding cached download: $songId for user $userId")
            val cachedDownload = SupabaseCachedDownload(
                userId = userId,
                songId = songId,
                title = title,
                source = source,
                thumbnailUrl = thumbnailUrl,
                cachedAt = cachedAt
            )
            supabaseClient.from("cached_downloads").upsert(listOf(cachedDownload)) {
                onConflict = "user_id,song_id"
            }
            Timber.tag("SupabaseCachedDownloads").d("✅ Successfully added/updated cached download: $songId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabaseCachedDownloads").e(e, "❌ Error adding cached download: $songId for $userId")
            Result.failure(e)
        }
    }

    suspend fun removeCachedDownload(userId: String, songId: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            Timber.tag("SupabaseCachedDownloads").d("🗑️ removeCachedDownload - userId: $userId, songId: $songId")
            
            // Perform delete call
            supabaseClient.from("cached_downloads").delete {
                filter {
                    eq("user_id", userId)
                    eq("song_id", songId)
                }
            }
            
            Timber.tag("SupabaseCachedDownloads").d("✅ Supabase delete request successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabaseCachedDownloads").e(e, "❌ Error in removeCachedDownload: $songId")
            Result.failure(e)
        }
    }
}
