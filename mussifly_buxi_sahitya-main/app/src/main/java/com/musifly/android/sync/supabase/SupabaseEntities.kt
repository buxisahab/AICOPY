package com.musifly.android.sync.supabase

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabasePlaylist(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SupabasePlaylistSong(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("song_id") val songId: String,
    val title: String? = null,
    val order: Int,
    @SerialName("added_at") val addedAt: String? = null
)

@Serializable
data class SupabaseLike(
    @SerialName("user_id") val userId: String,
    @SerialName("song_id") val songId: String,
    val title: String? = null,
    val source: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("liked_at") val likedAt: String? = null
)

@Serializable
data class SupabaseUserActivity(
    @SerialName("user_id") val userId: String,
    @SerialName("user_type") val userType: String,
    @SerialName("last_active_at") val lastActiveAt: String? = null,
    @SerialName("is_migrated") val isMigrated: Boolean = false
)

@Serializable
data class SupabaseAlbumBookmark(
    @SerialName("user_id") val userId: String,
    @SerialName("album_id") val albumId: String,
    val title: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("artist_name") val artistName: String? = null,
    @SerialName("bookmarked_at") val bookmarked_at: String? = null
)

@Serializable
data class SupabaseArtistBookmark(
    @SerialName("user_id") val userId: String,
    @SerialName("artist_id") val artistId: String,
    val name: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("bookmarked_at") val bookmarked_at: String? = null
)

@Serializable
data class SupabasePlaylistBookmark(
    @SerialName("user_id") val userId: String,
    @SerialName("playlist_id") val playlistId: String,
    val title: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("browse_id") val browseId: String? = null,
    @SerialName("bookmarked_at") val bookmarked_at: String? = null
)

@Serializable
data class SupabaseCachedDownload(
    @SerialName("user_id") val userId: String,
    @SerialName("song_id") val songId: String,
    val title: String? = null,
    val source: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("cached_at") val cachedAt: String? = null
)

@Serializable
data class SupabaseProfile(
    val id: String,
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("is_premium") val isPremium: Boolean = false
)

@Serializable
data class SupabaseProfileUpsert(
    val id: String,
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_premium") val isPremium: Boolean = false
)

@Serializable
data class SupabaseFollower(
    val id: Long? = null,
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabaseFollowerWithProfile(
    val id: Long? = null,
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String,
    @SerialName("created_at") val createdAt: String? = null,
    val profile: SupabaseProfile? = null
)

@Serializable
data class SupabaseFollowingWithProfile(
    val id: Long? = null,
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String,
    @SerialName("created_at") val createdAt: String? = null,
    val profile: SupabaseProfile? = null
)

@Serializable
data class SupabaseNotification(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("actor_id") val actorId: String? = null,
    val type: String,
    val title: String? = null,
    val message: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SupabaseNotificationWithActor(
    val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("actor_id") val actorId: String? = null,
    val type: String,
    val title: String? = null,
    val message: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    val actor: SupabaseProfile? = null
)

@Serializable
data class SupabaseStory(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("media_url") val mediaUrl: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expires_at: String? = null,
    val caption: String? = null,
    @SerialName("is_video") val isVideo: Boolean = false,
    val duration: Long? = null,
    @SerialName("music_id") val musicId: String? = null,
    @SerialName("start_time") val startTime: Long? = null,
    @SerialName("end_time") val endTime: Long? = null,
    @SerialName("trim_start") val trimStart: Long = 0,
    @SerialName("trim_end") val trimEnd: Long? = null
)

@Serializable
data class SupabaseStoryWithProfile(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("media_url") val mediaUrl: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("expires_at") val expires_at: String? = null,
    val caption: String? = null,
    @SerialName("is_video") val isVideo: Boolean = false,
    val duration: Long? = null,
    @SerialName("music_id") val musicId: String? = null,
    @SerialName("start_time") val startTime: Long? = null,
    @SerialName("end_time") val endTime: Long? = null,
    @SerialName("trim_start") val trimStart: Long = 0,
    @SerialName("trim_end") val trimEnd: Long? = null,
    val profile: SupabaseProfile? = null
)

@Serializable
data class SupabaseStoryView(
    val id: String,
    @SerialName("story_id") val storyId: String,
    @SerialName("viewer_user_id") val viewerUserId: String,
    @SerialName("viewed_at") val viewedAt: String,
    val viewer: SupabaseProfile? = null
)
