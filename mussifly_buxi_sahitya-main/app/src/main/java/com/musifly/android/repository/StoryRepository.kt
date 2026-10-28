package com.musifly.android.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.musifly.android.di.ApplicationScope
import com.musifly.android.models.Story
import com.musifly.android.models.UserStoryGroup
import com.musifly.android.sync.supabase.SupabaseStory
import com.musifly.android.sync.supabase.SupabaseStoryView
import com.musifly.android.sync.supabase.SupabaseStoryWithProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val externalScope: CoroutineScope
) {
    private val _stories = MutableStateFlow<List<UserStoryGroup>>(emptyList())
    val stories = _stories.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    suspend fun uploadStory(
        imageUri: Uri, 
        caption: String? = null,
        duration: Long? = null,
        musicId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Result<Unit> {
        // We use externalScope so the upload survives screen rotation/navigation
        val job = Job()
        externalScope.launch(Dispatchers.IO + job) {
            try {
                _isUploading.value = true
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw IllegalStateException("User not logged in")

                val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                val isVideo = mimeType.startsWith("video/")
                
                // 0. Use passed duration or calculate for video
                var finalDuration = duration ?: 0L
                
                if (isVideo) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, imageUri)
                        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        val durationMs = durationStr?.toLong() ?: 0L
                        if (durationMs > 90_000) { // 90 seconds
                            throw IllegalArgumentException("video must be under 90 seconds")
                        }
                        finalDuration = durationMs
                    } finally {
                        retriever.release()
                    }
                }

                // 1. Read and compress image (if not video)
                var bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Failed to read file")

                // Smart Compression for images only
                if (!isVideo && bytes.size > 1024 * 1024) {
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
                        Timber.tag("StoryRepository").e(e, "Compression failed")
                    }
                }

                val storyId = UUID.randomUUID().toString()
                val extension = if (isVideo) "mp4" else "jpg"
                val fileName = "$userId/$storyId.$extension"
                val bucket = supabase.storage.from("stories")

                // 2. Upload to Storage
                bucket.upload(path = fileName, data = bytes) {
                    upsert = true
                }

                val mediaUrl = bucket.publicUrl(fileName)

                // 3. Save to Database
                if (isVideo && finalDuration > 15_000L) {
                    // Virtual Splitting
                    val totalDuration = finalDuration
                    val segmentDuration = 15_000L
                    var currentStart = 0L
                    
                    while (currentStart < totalDuration) {
                        val currentEnd = minOf(currentStart + segmentDuration, totalDuration)
                        
                        val storyFragment = SupabaseStory(
                            userId = userId,
                            mediaUrl = mediaUrl,
                            caption = caption,
                            isVideo = isVideo,
                            duration = totalDuration,
                            musicId = musicId,
                            startTime = startTime,
                            endTime = endTime,
                            trimStart = currentStart,
                            trimEnd = currentEnd
                        )
                        supabase.from("stories").insert(storyFragment)
                        currentStart += segmentDuration
                    }
                } else {
                    // Single Story
                    val story = SupabaseStory(
                        userId = userId,
                        mediaUrl = mediaUrl,
                        caption = caption,
                        isVideo = isVideo,
                        duration = finalDuration,
                        musicId = musicId,
                        startTime = startTime,
                        endTime = endTime,
                        trimStart = 0,
                        trimEnd = finalDuration
                    )
                    supabase.from("stories").insert(story)
                }

                Timber.tag("StoryRepository").d("Story uploaded successfully")
                loadStories() // Refresh shared state
            } catch (e: Exception) {
                Timber.tag("StoryRepository").e(e, "Error uploading story: $imageUri")
            } finally {
                _isUploading.value = false
            }
        }
        return Result.success(Unit) // Immediate success return to UI, background handles the rest
    }

    suspend fun loadStories() {
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                    _stories.value = emptyList()
                    return@withContext
                }
                
                // 1. Get following IDs
                val following = profileRepository.getFollowing(currentUserId)
                val userIds: List<String> = following.map { it.followingId } + currentUserId

                // 2. Fetch stories from these users that haven't expired
                val now = Instant.now().toString()
                val supabaseStories = supabase.from("stories")
                    .select(columns = Columns.raw("*, profile:user_id(*)")) {
                        filter {
                            isIn("user_id", userIds)
                            gt("expires_at", now)
                        }
                        order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }.decodeList<SupabaseStoryWithProfile>()

                // 2. Fetch my views
                val storyIds = supabaseStories.map { it.id }
                val myViews = if (storyIds.isNotEmpty()) {
                     supabase.from("story_views").select {
                        filter {
                            eq("viewer_user_id", currentUserId)
                            isIn("story_id", storyIds)
                        }
                    }.decodeList<SupabaseStoryView>().map { it.storyId }.toSet()
                } else {
                    emptySet()
                }

                // 3. Group by user
                val groups = supabaseStories.groupBy { it.userId }.map { (userId, stories) ->
                    val profile = stories.first().profile
                    UserStoryGroup(
                        userId = userId,
                        username = profile?.username ?: profile?.fullName ?: "User",
                        userImageUrl = profile?.avatarUrl,
                        stories = stories.map { s ->
                            Story(
                                id = s.id,
                                userId = s.userId,
                                username = profile?.username ?: profile?.fullName,
                                userImageUrl = profile?.avatarUrl,
                                mediaUrl = s.mediaUrl,
                                createdAt = s.createdAt,
                                expiresAt = s.expires_at,
                                caption = s.caption,
                                isVideo = s.isVideo,
                                isViewed = s.id in myViews,
                                duration = s.duration,
                                musicId = s.musicId,
                                startTime = s.startTime,
                                endTime = s.endTime,
                                trimStart = s.trimStart,
                                trimEnd = s.trimEnd
                            )
                        }
                    )
                }.sortedByDescending { it.userId == currentUserId } // Current user story first
                
                _stories.value = groups
            } catch (e: Exception) {
                Timber.e(e, "Error fetching stories")
                _stories.value = emptyList()
            }
        }
    }

    suspend fun getStories(): List<UserStoryGroup> {
        loadStories()
        return _stories.value
    }

    suspend fun deleteStory(storyId: String, mediaUrl: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Delete from Storage
                try {
                    val path = mediaUrl.substringAfter("/stories/")
                    supabase.storage.from("stories").delete(path)
                } catch (e: Exception) {
                    Timber.tag("StoryRepository").e(e, "Failed to delete storage file, proceeding with DB deletion")
                }

                // 2. Delete from Database
                supabase.from("stories").delete {
                    filter {
                        eq("id", storyId)
                    }
                }
                loadStories() // Refresh shared state
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "Error deleting story")
                Result.failure(e)
            }
        }
    }


    suspend fun viewStory(storyId: String) {
        withContext(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
                // Check if already viewed to avoid unnecessary network call (though Unique constraint handles it)
                // We'll just insert and ignore conflict
                val view = SupabaseStoryView(
                    id = UUID.randomUUID().toString(),
                    storyId = storyId,
                    viewerUserId = currentUserId,
                    viewedAt = Instant.now().toString()
                )
                supabase.from("story_views").upsert(view) {
                    onConflict = "story_id, viewer_user_id"
                }
                loadStories() // Refresh 
            } catch (e: Exception) {
                Timber.e(e, "Error viewing story")
            }
        }
    }

    suspend fun getStoryViewers(storyId: String): List<SupabaseStoryView> {
        return withContext(Dispatchers.IO) {
             try {
                supabase.from("story_views")
                    .select(columns = Columns.raw("*, viewer:viewer_user_id(*)")) {
                        filter {
                            eq("story_id", storyId)
                        }
                        order("viewed_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }.decodeList<SupabaseStoryView>()
            } catch (e: Exception) {
                Timber.e(e, "Error fetching viewers")
                emptyList()
            }
        }
    }
}
