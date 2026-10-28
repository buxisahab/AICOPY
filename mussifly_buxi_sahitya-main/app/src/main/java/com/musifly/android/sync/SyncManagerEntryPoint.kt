package com.musifly.android.sync

import com.musifly.android.sync.repository.LikesRepository
import com.musifly.android.sync.repository.PlaylistRepository
import com.musifly.android.sync.repository.BookmarksRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Entry point for accessing sync repositories from non-Hilt contexts
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncManagerEntryPoint {
    fun likesRepository(): LikesRepository
    fun playlistRepository(): PlaylistRepository
    fun cachedDownloadsRepository(): com.musifly.android.sync.repository.CachedDownloadsRepository
    fun bookmarksRepository(): BookmarksRepository
    fun migrationService(): com.musifly.android.sync.supabase.SupabaseMigrationService
}
