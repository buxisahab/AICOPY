package com.musifly.android.sync.usecases

import com.musifly.android.sync.repository.PlaylistRepository

/**
 * Use case for removing a song from a playlist
 */
class RemoveSongFromPlaylistUseCase(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlistId: String, songId: String): Result<Unit> {
        return playlistRepository.removeSongFromPlaylist(playlistId, songId)
    }
}
