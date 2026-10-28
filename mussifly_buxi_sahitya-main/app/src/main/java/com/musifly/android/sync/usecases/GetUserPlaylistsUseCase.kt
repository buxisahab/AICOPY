package com.musifly.android.sync.usecases

import com.musifly.android.sync.local.UserPlaylistEntity
import com.musifly.android.sync.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting user playlists
 */
class GetUserPlaylistsUseCase(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(): Flow<List<UserPlaylistEntity>> {
        return playlistRepository.getAllPlaylists()
    }
}
