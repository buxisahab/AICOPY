package com.musifly.android.models

import kotlinx.serialization.Serializable

@Serializable
data class Story(
    val id: String,
    val userId: String,
    val username: String? = null,
    val userImageUrl: String? = null,
    val mediaUrl: String,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val caption: String? = null,
    val isVideo: Boolean = false,
    val isViewed: Boolean = false,
    val duration: Long? = null,
    val musicId: String? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val trimStart: Long = 0,
    val trimEnd: Long? = null
)

data class UserStoryGroup(
    val userId: String,
    val username: String,
    val userImageUrl: String?,
    val stories: List<Story>,
    val hasUnviewed: Boolean = stories.any { !it.isViewed }
)
