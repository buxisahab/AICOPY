package com.musifly.android.repository

import android.content.Context
import android.net.Uri
import com.musifly.android.sync.supabase.SupabaseProfile
import com.musifly.android.sync.supabase.SupabaseProfileUpsert
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.*
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber
import java.time.Instant
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.LocalProfileEntity
import kotlinx.coroutines.flow.Flow

@Singleton
class ProfileRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val database: MusicDatabase,
    @ApplicationContext private val context: Context
) {

    /**
     * Checks if a username is already taken by another user.
     * Returns true if available, false if taken.
     */
    suspend fun isUsernameAvailable(username: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check if any user has this username
                // Note: We use countOrNull() which returns Long?
                val count = supabase.from("profiles")
                    .select {
                        count(Count.EXACT)
                        // Use head inside the builder if supported or rely on count(EXACT)
                        // If head is a property of the builder:
                        // head = true
                        filter {
                            eq("username", username)
                        }
                    }.countOrNull()

                // If count is null or 0, it means no rows found -> username is available
                // If count > 0, user with username exists
                count == null || count == 0L
            } catch (e: Exception) {
                Timber.e(e, "Error checking username availability")
                // Fail safe
                false 
            }
        }
    }

    suspend fun getProfile(userId: String): SupabaseProfile? {
        return withContext(Dispatchers.IO) {
            try {
                val profile = supabase.from("profiles")
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }.decodeSingleOrNull<SupabaseProfile>()
                
                // Update local DB if we got a profile
                if (profile != null) {
                    val existing = database.getProfile(userId)
                    val followers = existing?.followersCount ?: 0L
                    val following = existing?.followingCount ?: 0L
                    
                    database.upsertProfile(profile.toLocalEntity(followers, following))
                }
                
                profile
            } catch (e: Exception) {
                Timber.e(e, "Error fetching profile, falling back to local database")
                // Fallback to local database if network fails
                database.getProfile(userId)?.toSupabaseProfile()
            }
        }
    }

    /**
     * Get profile from local database only - fast and offline-ready.
     */
    suspend fun getCachedProfile(userId: String): SupabaseProfile? {
        return withContext(Dispatchers.IO) {
            database.getProfile(userId)?.toSupabaseProfile()
        }
    }

    fun getLocalProfile(userId: String): Flow<LocalProfileEntity?> {
        return database.getProfileFlow(userId)
    }

    suspend fun syncProfile(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val profile = getProfile(userId)
                if (profile != null) {
                    val followers = getFollowersCount(userId)
                    val following = getFollowingCount(userId)
                    database.upsertProfile(profile.toLocalEntity(followers, following))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error syncing profile for $userId")
            }
        }
    }

    private fun SupabaseProfile.toLocalEntity(followers: Long, following: Long): LocalProfileEntity {
        val lastSeenMillis = lastSeen?.let {
            try {
                Instant.parse(it).toEpochMilli()
            } catch (e: Exception) {
                null
            }
        }
        
        return LocalProfileEntity(
            id = id,
            username = username,
            fullName = fullName,
            avatarUrl = avatarUrl,
            bio = bio,
            followersCount = followers,
            followingCount = following,
            lastSeen = lastSeenMillis,
            isPremium = isPremium
        )
    }

    fun LocalProfileEntity.toSupabaseProfile(): SupabaseProfile {
        return SupabaseProfile(
            id = id,
            username = username,
            fullName = fullName,
            avatarUrl = avatarUrl,
            bio = bio,
            isPremium = isPremium
        )
    }

    suspend fun searchUsers(query: String): List<SupabaseProfile> {
        return withContext(Dispatchers.IO) {
            if (query.length < 3) return@withContext emptyList()
            try {
                supabase.from("profiles")
                    .select {
                        filter {
                            or {
                                ilike("username", "%$query%")
                                ilike("full_name", "%$query%")
                            }
                        }
                        limit(20)
                    }.decodeList<SupabaseProfile>()
            } catch (e: Exception) {
                Timber.e(e, "Error searching users")
                emptyList()
            }
        }
    }

    /**
     * Updates the user profile (username, fullName and/or photo).
     */
    suspend fun updateProfile(username: String?, fullName: String?, bio: String?, imageUri: Uri?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser 
                    ?: throw IllegalStateException("User not logged in")

                val userId = firebaseUser.uid
                var photoUrl: String? = null

                // 1. Upload new image if provided
                if (imageUri != null) {
                    var bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("Failed to read image file")
                    
                    // Smart Compression Logic
                    val maxFileSize = 1024 * 1024
                    
                    if (bytes.size > maxFileSize) {
                        try {
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            val stream = java.io.ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                            
                            val compressedBytes = stream.toByteArray()
                            if (compressedBytes.size < bytes.size) {
                                bytes = compressedBytes
                            }
                            bitmap.recycle() 
                        } catch (e: Exception) {
                            Timber.tag("ProfileRepository").e(e, "Image compression failed, uploading original")
                        }
                    }
                    
                    val fileName = "$userId.png" 
                    val bucketName = "avatars"
                    val bucket = supabase.storage.from(bucketName)
                    
                    bucket.upload(path = fileName, data = bytes) {
                        upsert = true
                    }
                    
                    val rawUrl = bucket.publicUrl(fileName)
                    photoUrl = "$rawUrl?v=${System.currentTimeMillis()}"
                }

                // 2. Update Firebase User Profile 
                val display = if (!fullName.isNullOrBlank()) fullName else username
                
                val profileUpdates = UserProfileChangeRequest.Builder().apply {
                    if (!display.isNullOrBlank()) {
                        displayName = display
                    }
                    if (photoUrl != null) {
                        photoUri = Uri.parse(photoUrl)
                    }
                }.build()

                firebaseUser.updateProfile(profileUpdates).await()

                // 3. Update Supabase 'profiles' table
                val currentProfile = try { getProfile(userId) } catch(e: Exception) { null }
                val finalUsername = if (!username.isNullOrBlank()) username else currentProfile?.username
                val finalFullName = if (!fullName.isNullOrBlank()) fullName else currentProfile?.fullName
                val finalPhotoUrl = photoUrl ?: currentProfile?.avatarUrl
                val finalBio = if (bio != null) bio else currentProfile?.bio

                // We use Upsert DTO to avoid nulling out created_at
                val upsertProfile = SupabaseProfileUpsert(
                    id = userId,
                    username = finalUsername,
                    fullName = finalFullName,
                    avatarUrl = finalPhotoUrl,
                    bio = finalBio,
                    updatedAt = try { Instant.now().toString() } catch (e: Error) { null }, // API 26+ safe catch
                    isPremium = currentProfile?.isPremium ?: false
                )
                
                supabase.from("profiles").upsert(upsertProfile) {
                    onConflict = "id"
                }
                Timber.tag("ProfileRepository").d("Updated Supabase profiles table")

                // Update Local DB
                val existing = database.getProfile(userId)
                database.upsertProfile(
                    LocalProfileEntity(
                        id = userId,
                        username = finalUsername,
                        fullName = finalFullName,
                        avatarUrl = finalPhotoUrl,
                        bio = finalBio,
                        followersCount = existing?.followersCount ?: 0,
                        followingCount = existing?.followingCount ?: 0,
                        isPremium = existing?.isPremium ?: false
                    )
                )

                Result.success(Unit)
            } catch (e: Exception) {
                Timber.tag("ProfileRepository").e(e, "Error updating profile")
                Result.failure(e)
            }
        }
    }

    suspend fun updateFcmToken(token: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid 
                    ?: throw IllegalStateException("User not logged in")

                // Changed from Upsert to Update to prevent creation of incomplete profiles
                // Upsert would trigger default/random username generation on new rows
                val updateTime = try { java.time.Instant.now().toString() } catch (e: Throwable) { null }
                
                supabase.from("profiles").update({
                    set("fcm_token", token)
                     if (updateTime != null) set("updated_at", updateTime)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }
                Timber.tag("ProfileRepository").d("Updated FCM token for user $userId")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.tag("ProfileRepository").e(e, "Error updating FCM token")
                Result.failure(e)
            }
        }
    }

    suspend fun updateLastSeen(userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val now = Instant.now().toString()
                
                supabase.from("profiles").update({
                    set("last_seen", now)
                }) {
                    filter {
                        eq("id", userId)
                    }
                }
                Timber.tag("ProfileRepository").d("Updated last_seen for user $userId")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.tag("ProfileRepository").e(e, "Error updating last_seen")
                Result.failure(e)
            }
        }
    }

    // Follow / Unfollow Logic

    suspend fun followUser(targetUserId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: throw IllegalStateException("Not logged in")
                Timber.tag("ProfileRepository").d("Attempting to follow user: $targetUserId from $currentUserId")
                
                val follower = com.musifly.android.sync.supabase.SupabaseFollower(
                    followerId = currentUserId,
                    followingId = targetUserId
                )
                
                supabase.from("followers").insert(follower)
                Timber.tag("ProfileRepository").d("Follow successful")
                
                // Update local following count for current user
                updateLocalFollowingCount(currentUserId, 1)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.tag("ProfileRepository").e(e, "Error following user")
                Result.failure(e)
            }
        }
    }

    suspend fun unfollowUser(targetUserId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: throw IllegalStateException("Not logged in")
                Timber.tag("ProfileRepository").d("Attempting to unfollow user: $targetUserId from $currentUserId")
                
                supabase.from("followers").delete {
                    filter {
                        eq("follower_id", currentUserId)
                        eq("following_id", targetUserId)
                    }
                }
                
                Timber.tag("ProfileRepository").d("Unfollow successful")
                
                // Update local following count for current user
                updateLocalFollowingCount(currentUserId, -1)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.tag("ProfileRepository").e(e, "Error unfollowing user")
                Result.failure(e)
            }
        }
    }

    suspend fun removeFollower(followerId: String, targetUserId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: throw IllegalStateException("Not logged in")
                val officialAccounts = listOf("Qev4DqlLiGeVXhIhBLaQWmHOmjq2", "RSdUGiWPitOuq6HXHL7lPXKGQwi1")
                val isAdmin = currentUserId in officialAccounts
                
                val followingId = if (isAdmin) targetUserId else currentUserId
                
                supabase.from("followers").delete {
                    filter {
                        eq("follower_id", followerId)
                        eq("following_id", followingId)
                    }
                }
                
                Timber.tag("ProfileRepository").d("Follower removal successful (Target: $followingId, Follower: $followerId)")
                
                // Update local followers count for the target user (could be self or another user if admin)
                updateLocalFollowersCount(followingId, -1)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.tag("ProfileRepository").e(e, "Error removing follower")
                Result.failure(e)
            }
        }
    }

    suspend fun isFollowing(targetUserId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext false
                
                val count = supabase.from("followers").select {
                    count(Count.EXACT)
                    filter {
                        eq("follower_id", currentUserId)
                        eq("following_id", targetUserId)
                    }
                }.countOrNull()
                
                count != null && count > 0
            } catch (e: Exception) {
                Timber.e(e, "Error checking follow status")
                false
            }
        }
    }

    /**
     * Efficiently fetches follow status for multiple users in a single query.
     */
    suspend fun getFollowStates(targetUserIds: List<String>): Map<String, Boolean> {
        if (targetUserIds.isEmpty()) return emptyMap()
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext emptyMap()
                
                // Fetch all follow rows where follower is current user and following is in the list
                val follows = supabase.from("followers").select {
                    filter {
                        eq("follower_id", currentUserId)
                        isIn("following_id", targetUserIds)
                    }
                }.decodeList<com.musifly.android.sync.supabase.SupabaseFollower>()
                
                val followMap = targetUserIds.associateWith { false }.toMutableMap()
                follows.forEach { follow ->
                    followMap[follow.followingId] = true
                }
                followMap
            } catch (e: Exception) {
                Timber.e(e, "Error fetching multiple follow statuses")
                emptyMap()
            }
        }
    }

    private suspend fun updateLocalFollowingCount(userId: String, delta: Long) {
        withContext(Dispatchers.IO) {
            try {
                val existing = database.getProfile(userId)
                if (existing != null) {
                    val newCount = (existing.followingCount + delta).coerceAtLeast(0)
                    database.upsertProfile(existing.copy(followingCount = newCount))
                    Timber.tag("ProfileRepository").d("Updated local following count to $newCount")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating local following count")
            }
        }
    }

    private suspend fun updateLocalFollowersCount(userId: String, delta: Long) {
        withContext(Dispatchers.IO) {
            try {
                val existing = database.getProfile(userId)
                if (existing != null) {
                    val newCount = (existing.followersCount + delta).coerceAtLeast(0)
                    database.upsertProfile(existing.copy(followersCount = newCount))
                    Timber.tag("ProfileRepository").d("Updated local followers count to $newCount")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating local followers count")
            }
        }
    }

    suspend fun getFollowersCount(userId: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                supabase.from("followers").select {
                    count(Count.EXACT)
                    filter {
                        eq("following_id", userId)
                    }
                }.countOrNull() ?: 0L
            } catch (e: Exception) {
                Timber.e(e, "Error getting followers count")
                // Return existing local count if available instead of 0
                database.getProfile(userId)?.followersCount ?: 0L
            }
        }
    }

    suspend fun getFollowingCount(userId: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                supabase.from("followers").select {
                    count(Count.EXACT)
                    filter {
                        eq("follower_id", userId)
                    }
                }.countOrNull() ?: 0L
            } catch (e: Exception) {
                Timber.e(e, "Error getting following count")
                // Return existing local count if available instead of 0
                database.getProfile(userId)?.followingCount ?: 0L
            }
        }
    }

    suspend fun getFollowers(userId: String): List<com.musifly.android.sync.supabase.SupabaseFollowerWithProfile> {
        return withContext(Dispatchers.IO) {
            try {
                // Use join to fetch follower profiles and order by creation date
                supabase.from("followers").select(columns = Columns.raw("*, profile:follower_id(*)")) {
                    filter {
                        eq("following_id", userId)
                    }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }.decodeList<com.musifly.android.sync.supabase.SupabaseFollowerWithProfile>()
            } catch (e: Exception) {
                Timber.e(e, "Error getting followers list")
                emptyList()
            }
        }
    }

    suspend fun getFollowing(userId: String): List<com.musifly.android.sync.supabase.SupabaseFollowingWithProfile> {
        return withContext(Dispatchers.IO) {
            try {
                // Use join to fetch following profiles and order by creation date
                supabase.from("followers").select(columns = Columns.raw("*, profile:following_id(*)")) {
                    filter {
                        eq("follower_id", userId)
                    }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }.decodeList<com.musifly.android.sync.supabase.SupabaseFollowingWithProfile>()
            } catch (e: Exception) {
                Timber.e(e, "Error getting following list")
                emptyList()
            }
        }
    }

    suspend fun createNotification(recipientId: String, type: String, title: String, message: String) {
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
                
                val notification = com.musifly.android.sync.supabase.SupabaseNotification(
                    userId = recipientId,
                    actorId = currentUserId,
                    type = type,
                    title = title,
                    message = message
                )
                
                supabase.from("notifications").insert(notification)
            } catch (e: Exception) {
                Timber.e(e, "Error creating notification")
            }
        }
    }

    suspend fun getNotifications(): List<com.musifly.android.sync.supabase.SupabaseNotificationWithActor> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext emptyList()
                val oneWeekAgo = Instant.now().minus(7, java.time.temporal.ChronoUnit.DAYS).toString()
                
                // 1. Permanent cleanup: Delete all notifications for this user that are older than 7 days
                try {
                    supabase.from("notifications").delete {
                        filter {
                            eq("user_id", currentUserId)
                            lt("created_at", oneWeekAgo)
                        }
                    }
                    Timber.tag("ProfileRepository").d("Cleaned up old notifications")
                } catch (e: Exception) {
                    Timber.tag("ProfileRepository").e(e, "Error during auto-cleanup of old notifications")
                }

                // 2. Fetch remaining notifications
                supabase.from("notifications").select(columns = Columns.raw("*, actor:actor_id(*)")) {
                    filter {
                        eq("user_id", currentUserId)
                    }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(50)
                }.decodeList<com.musifly.android.sync.supabase.SupabaseNotificationWithActor>()
            } catch (e: Exception) {
                Timber.e(e, "Error fetching notifications")
                emptyList()
            }
        }
    }

    suspend fun markNotificationAsRead(notificationId: Long) {
        withContext(Dispatchers.IO) {
            try {
                 supabase.from("notifications").update({
                     set("is_read", true)
                 }) {
                     filter {
                         eq("id", notificationId)
                     }
                 }
            } catch (e: Exception) {
                Timber.e(e, "Error updating notification")
            }
        }
    }

    suspend fun deleteNotification(notificationId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabase.from("notifications").delete {
                    filter {
                        eq("id", notificationId)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error deleting notification")
                Result.failure(e)
            }
        }
    }


    suspend fun clearAllNotifications(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext Result.failure(Exception("Not logged in"))
                supabase.from("notifications").delete {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error clearing notifications")
                Result.failure(e)
            }
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val user = FirebaseAuth.getInstance().currentUser 
                    ?: return@withContext Result.failure(Exception("Not logged in"))
                val userId = user.uid

                // 1. First attempt to delete from Firebase Auth.
                // This is the most likely step to fail (requires recent login).
                // If it fails, we haven't touched the Supabase data yet.
                try {
                    user.delete().await()
                    Timber.tag("ProfileRepository").d("Deleted Firebase user $userId")
                } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                    Timber.tag("ProfileRepository").e("Re-authentication required for deletion")
                    return@withContext Result.failure(Exception("Re-authentication required. Please log out and log back in to delete your account."))
                }

                // 2. Delete Avatar from Storage
                try {
                    supabase.storage.from("avatars").delete("$userId.png")
                    Timber.tag("ProfileRepository").d("Deleted avatar for $userId")
                } catch (e: Exception) {
                    // Log but continue - user might not have an avatar
                    Timber.tag("ProfileRepository").w(e, "Failed to delete avatar (or none existed)")
                }

                // 3. Delete from Supabase 'profiles' table
                // This permanently removes the username and profile data
                // All related data (likes, playlists) will cascade delete
                try {
                    supabase.from("profiles").delete {
                        filter {
                            eq("id", userId)
                        }
                    }
                    Timber.tag("ProfileRepository").d("Deleted Supabase profile for $userId")
                } catch (e: Exception) {
                    Timber.tag("ProfileRepository").e(e, "Failed to delete Supabase profile")
                    // Note: Firebase user is already gone, so we must try to finish cleanup or user is orphaned
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Timber.tag("ProfileRepository").e(e, "Error during account deletion")
                Result.failure(e)
            }
        }
    }

}
