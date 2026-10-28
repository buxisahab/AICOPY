package com.musifly.android.di

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.musifly.android.constants.MaxSongCacheSizeKey
import com.musifly.android.db.InternalDatabase
import com.musifly.android.db.MusicDatabase
import com.musifly.android.sync.local.LocalUserLikesDao
import com.musifly.android.sync.local.LocalUserPlaylistDao
import com.musifly.android.sync.local.LocalUserPlaylistSongDao
import com.musifly.android.sync.local.LocalUserCachedDownloadsDao
import com.musifly.android.sync.SyncManager
import com.musifly.android.sync.repository.LikesRepository
import com.musifly.android.sync.repository.PlaylistRepository
import com.musifly.android.sync.repository.CachedDownloadsRepository
import com.musifly.android.utils.dataStore
import com.musifly.android.utils.get
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * EntryPoint to access LikesRepository from Composables
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LikesRepositoryEntryPoint {
    fun likesRepository(): LikesRepository
}

/**
 * EntryPoint to access PlaylistRepository from Composables
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlaylistRepositoryEntryPoint {
    fun playlistRepository(): PlaylistRepository
}

/**
 * EntryPoint to access CachedDownloadsRepository from Composables
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CachedDownloadsRepositoryEntryPoint {
    fun cachedDownloadsRepository(): CachedDownloadsRepository
}

/**
 * EntryPoint to access BookmarksRepository from Composables
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BookmarksRepositoryEntryPoint {
    fun bookmarksRepository(): com.musifly.android.sync.repository.BookmarksRepository
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlayerCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCache

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MusicDatabase = InternalDatabase.newInstance(context)

    @Singleton
    @Provides
    fun provideDatabaseProvider(
        @ApplicationContext context: Context,
    ): DatabaseProvider = StandaloneDatabaseProvider(context)

    @Singleton
    @Provides
    @PlayerCache
    fun providePlayerCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): SimpleCache {
        val constructor = {
            SimpleCache(
                context.filesDir.resolve("exoplayer"),
                when (val cacheSize = context.dataStore[MaxSongCacheSizeKey] ?: 1024) {
                    -1 -> NoOpCacheEvictor()
                    else -> LeastRecentlyUsedCacheEvictor(cacheSize * 1024 * 1024L)
                },
                databaseProvider,
            )
        }
        constructor().release()
        return constructor()
    }

    @Singleton
    @Provides
    @DownloadCache
    fun provideDownloadCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): SimpleCache {
        val constructor = {
            SimpleCache(context.filesDir.resolve("download"), NoOpCacheEvictor(), databaseProvider)
        }
        constructor().release()
        return constructor()
    }
    
    // Firebase Sync DAOs
    @Singleton
    @Provides
    fun provideUserLikesDao(database: MusicDatabase): LocalUserLikesDao {
        return database.delegate.userLikesDao
    }
    
    @Singleton
    @Provides
    fun provideUserPlaylistDao(database: MusicDatabase): LocalUserPlaylistDao {
        return database.delegate.userPlaylistDao
    }
    
    @Singleton
    @Provides
    fun provideUserPlaylistSongDao(database: MusicDatabase): LocalUserPlaylistSongDao {
        return database.delegate.userPlaylistSongDao
    }
    
    @Singleton
    @Provides
    fun provideUserCachedDownloadsDao(database: MusicDatabase): LocalUserCachedDownloadsDao {
        return database.delegate.userCachedDownloadsDao
    }
    
    // Firebase Sync Services
    @Singleton
    @Provides
    fun provideFirestoreLikesService(): com.musifly.android.sync.firestore.FirestoreLikesService {
        return com.musifly.android.sync.firestore.FirestoreLikesService()
    }
    
    @Singleton
    @Provides
    fun provideFirestorePlaylistService(): com.musifly.android.sync.firestore.FirestorePlaylistService {
        return com.musifly.android.sync.firestore.FirestorePlaylistService()
    }
    
    @Singleton
    @Provides
    fun provideFirestoreCachedDownloadsService(): com.musifly.android.sync.firestore.FirestoreCachedDownloadsService {
        return com.musifly.android.sync.firestore.FirestoreCachedDownloadsService()
    }
    
    // Firebase Sync Repositories
    @Singleton
    @Provides
    fun provideLikesRepository(
        userLikesDao: LocalUserLikesDao,
        firestoreLikesService: com.musifly.android.sync.firestore.FirestoreLikesService,
        supabaseLikesService: com.musifly.android.sync.supabase.SupabaseLikesService,
        database: MusicDatabase
    ): LikesRepository {
        return LikesRepository(userLikesDao, firestoreLikesService, supabaseLikesService, database)
    }
    
    @Singleton
    @Provides
    fun providePlaylistRepository(
        userPlaylistDao: LocalUserPlaylistDao,
        userPlaylistSongDao: LocalUserPlaylistSongDao,
        firestorePlaylistService: com.musifly.android.sync.firestore.FirestorePlaylistService,
        supabasePlaylistService: com.musifly.android.sync.supabase.SupabasePlaylistService,
        database: MusicDatabase
    ): PlaylistRepository {
        return PlaylistRepository(
            userPlaylistDao, 
            userPlaylistSongDao, 
            firestorePlaylistService, 
            supabasePlaylistService,
            database
        )
    }
    
    @Singleton
    @Provides
    fun provideCachedDownloadsRepository(
        userCachedDownloadsDao: LocalUserCachedDownloadsDao,
        supabaseCachedDownloadsService: com.musifly.android.sync.supabase.SupabaseCachedDownloadsService,
        database: MusicDatabase
    ): CachedDownloadsRepository {
        return CachedDownloadsRepository(userCachedDownloadsDao, supabaseCachedDownloadsService, database)
    }

    @Singleton
    @Provides
    fun provideBookmarksRepository(
        database: MusicDatabase,
        supabaseBookmarksService: com.musifly.android.sync.supabase.SupabaseBookmarksService
    ): com.musifly.android.sync.repository.BookmarksRepository {
        return com.musifly.android.sync.repository.BookmarksRepository(database, supabaseBookmarksService)
    }
    
    @Singleton
    @Provides
    fun provideSyncManager(
        @ApplicationContext context: Context,
        likesRepository: LikesRepository,
        playlistRepository: PlaylistRepository,
        cachedDownloadsRepository: CachedDownloadsRepository,
        bookmarksRepository: com.musifly.android.sync.repository.BookmarksRepository,
        migrationService: com.musifly.android.sync.supabase.SupabaseMigrationService
    ): SyncManager {
        return SyncManager(
            context,
            likesRepository,
            playlistRepository,
            cachedDownloadsRepository,
            bookmarksRepository,
            migrationService
        )
    }

    @Singleton
    @Provides
    fun provideSupabaseMigrationService(
        supabase: io.github.jan.supabase.SupabaseClient,
        firestoreLikesService: com.musifly.android.sync.firestore.FirestoreLikesService,
        firestorePlaylistService: com.musifly.android.sync.firestore.FirestorePlaylistService,
        firestoreCachedDownloadsService: com.musifly.android.sync.firestore.FirestoreCachedDownloadsService,
        supabaseLikesService: com.musifly.android.sync.supabase.SupabaseLikesService,
        supabasePlaylistService: com.musifly.android.sync.supabase.SupabasePlaylistService,
        supabaseCachedDownloadsService: com.musifly.android.sync.supabase.SupabaseCachedDownloadsService
    ): com.musifly.android.sync.supabase.SupabaseMigrationService {
        return com.musifly.android.sync.supabase.SupabaseMigrationService(
            supabase,
            firestoreLikesService,
            firestorePlaylistService,
            firestoreCachedDownloadsService,
            supabaseLikesService,
            supabasePlaylistService,
            supabaseCachedDownloadsService
        )
    }

    @Singleton
    @Provides
    fun provideSaavnAPI(): com.musifly.android.streamingengines.saavn.SaavnAPI {
        return com.musifly.android.streamingengines.saavn.SaavnAPI()
    }
}
