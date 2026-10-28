package com.musifly.android.sync.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseLikesService @Inject constructor(
    private val supabase: SupabaseClient
) {
    
    companion object {
        const val TABLE_LIKES = "likes"
    }

    suspend fun likeSong(
        userId: String,
        songId: String,
        title: String?,
        thumbnailUrl: String?,
        source: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val like = SupabaseLike(
                userId = userId,
                songId = songId,
                title = title,
                thumbnailUrl = thumbnailUrl,
                source = source
            )
            
            supabase.from(TABLE_LIKES).upsert(listOf(like)) {
                onConflict = "user_id,song_id"
            }
            Timber.tag("SupabaseLikes").d("✅ Liked song: $songId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabaseLikes").e(e, "❌ Failed to like song: $songId")
            Result.failure(e)
        }
    }

    suspend fun unlikeSong(
        userId: String,
        songId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabase.from(TABLE_LIKES).delete {
                filter {
                    eq("user_id", userId)
                    eq("song_id", songId)
                }
            }
            Timber.tag("SupabaseLikes").d("✅ Unliked song: $songId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabaseLikes").e(e, "❌ Failed to unlike song: $songId")
            Result.failure(e)
        }
    }
    
    suspend fun getAllLikedSongs(userId: String): Result<List<SupabaseLike>> = withContext(Dispatchers.IO) {
        try {
            val likes = supabase.from(TABLE_LIKES).select {
                filter {
                    eq("user_id", userId)
                }
            }.decodeList<SupabaseLike>()
            Result.success(likes)
        } catch (e: Exception) {
            Timber.tag("SupabaseLikes").e(e, "❌ Failed to fetch liked songs")
            Result.failure(e)
        }
    }
    
    // Batch helper for migration
    suspend fun addLikes(likes: List<SupabaseLike>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (likes.isEmpty()) return@withContext Result.success(Unit)
            
            supabase.from(TABLE_LIKES).upsert(likes) {
                 onConflict = "user_id,song_id"
            }
            Timber.tag("SupabaseLikes").d("✅ Batch created ${likes.size} likes")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("SupabaseLikes").e(e, "❌ Failed to batch create likes")
            Result.failure(e)
        }
    }
}
