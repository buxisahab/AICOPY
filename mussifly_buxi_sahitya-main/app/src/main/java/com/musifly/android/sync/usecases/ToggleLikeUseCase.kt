package com.musifly.android.sync.usecases

import com.musifly.android.sync.repository.LikesRepository

/**
 * Use case for toggling like status of a song
 */
class ToggleLikeUseCase(
    private val likesRepository: LikesRepository
) {
    suspend operator fun invoke(songId: String): Result<Boolean> {
        return likesRepository.toggleLike(songId)
    }
}
