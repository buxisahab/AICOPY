@file:OptIn(ExperimentalCoroutinesApi::class)

package com.musifly.android.viewmodels

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.musifly.innertube.YouTube
import com.musifly.android.constants.AlbumFilter
import com.musifly.android.constants.AlbumFilterKey
import com.musifly.android.constants.AlbumSortDescendingKey
import com.musifly.android.constants.AlbumSortType
import com.musifly.android.constants.AlbumSortTypeKey
import com.musifly.android.constants.ArtistFilter
import com.musifly.android.constants.ArtistFilterKey
import com.musifly.android.constants.ArtistSongSortDescendingKey
import com.musifly.android.constants.ArtistSongSortType
import com.musifly.android.constants.ArtistSongSortTypeKey
import com.musifly.android.constants.ArtistSortDescendingKey
import com.musifly.android.constants.ArtistSortType
import com.musifly.android.constants.ArtistSortTypeKey
import com.musifly.android.constants.LibraryFilter
import com.musifly.android.constants.PlaylistSortDescendingKey
import com.musifly.android.constants.PlaylistSortType
import com.musifly.android.constants.PlaylistSortTypeKey
import com.musifly.android.constants.SongFilter
import com.musifly.android.constants.SongFilterKey
import com.musifly.android.constants.SongSortDescendingKey
import com.musifly.android.constants.SongSortType
import com.musifly.android.constants.SongSortTypeKey
import com.musifly.android.constants.TopSize
import com.musifly.android.db.MusicDatabase
import com.musifly.android.extensions.reversed
import com.musifly.android.extensions.toEnum
import com.musifly.android.playback.DownloadUtil
import com.musifly.android.utils.SyncUtils
import com.musifly.android.utils.dataStore
import com.musifly.android.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.Collator
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LibrarySongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    private val syncUtils: SyncUtils,
    private val likesRepository: com.musifly.android.sync.repository.LikesRepository,
) : ViewModel() {
    init {
        Timber.tag("LibrarySongsViewModel").d("🚀 LibrarySongsViewModel initialized")
        
        // For logged-in users, trigger sync from Firestore on ViewModel init
        viewModelScope.launch(Dispatchers.IO) {
            val isLoggedIn = com.musifly.android.sync.UserSessionManager.isLoggedIn()
            val userId = com.musifly.android.sync.UserSessionManager.getUserId()
            
            Timber.tag("LibrarySongsViewModel").d("🔍 Checking login status - isLoggedIn: $isLoggedIn, userId: $userId")
            
            if (isLoggedIn && userId != null) {
                try {
                    Timber.tag("LibrarySongsViewModel").d("📥 Logged-in user detected (uid: $userId) - syncing likes from Supabase...")
                    val result = likesRepository.syncFromSupabase()
                    result.fold(
                        onSuccess = { count ->
                            Timber.tag("LibrarySongsViewModel").d("✅ Synced $count likes from Supabase to local DB")
                        },
                        onFailure = { e ->
                            Timber.tag("LibrarySongsViewModel").e(e, "❌ Failed to sync likes from Supabase: ${e.message}")
                        }
                    )
                } catch (e: Exception) {
                    Timber.tag("LibrarySongsViewModel").e(e, "❌ Exception syncing likes from Supabase: ${e.message}")
                }
            } else {
                Timber.tag("LibrarySongsViewModel").d("ℹ️ User not logged in to Firebase Auth - skipping Supabase sync")
            }
        }
    }
    
    val allSongs =
        context.dataStore.data
            .map {
                Triple(
                    it[SongFilterKey].toEnum(SongFilter.LIKED),
                    it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                    (it[SongSortDescendingKey] ?: true),
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filter, sortType, descending) ->
                when (filter) {
                    SongFilter.LIBRARY -> database.songs(sortType, descending)
                    SongFilter.LIKED -> database.likedSongs(sortType, descending)
                    SongFilter.DOWNLOADED ->
                        downloadUtil.downloads.flatMapLatest { downloads ->
                            database
                                .allSongs()
                                .flowOn(Dispatchers.IO)
                                .map { songs ->
                                    songs.filter {
                                        downloads[it.id]?.state == Download.STATE_COMPLETED
                                    }
                                }.map { songs ->
                                    when (sortType) {
                                        SongSortType.CREATE_DATE -> songs.sortedBy {
                                            downloads[it.id]?.updateTimeMs ?: 0L
                                        }

                                        SongSortType.NAME -> songs.sortedBy { it.song.title }
                                        SongSortType.ARTIST -> {
                                            val collator =
                                                Collator.getInstance(Locale.getDefault())
                                            collator.strength = Collator.PRIMARY
                                            songs
                                                .sortedWith(
                                                    compareBy(collator) { song ->
                                                        song.artists.joinToString(
                                                            "",
                                                        ) { it.name }
                                                    },
                                                ).groupBy { it.album?.title }
                                                .flatMap { (_, songsByAlbum) ->
                                                    songsByAlbum.sortedBy { album ->
                                                        album.artists.joinToString(
                                                            "",
                                                        ) { it.name }
                                                    }
                                                }
                                        }

                                        SongSortType.PLAY_TIME -> songs.sortedBy { it.song.totalPlayTime }
                                    }.reversed(descending)
                                }
                        }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val likedSongsCount = database.likedSongs(SongSortType.NAME, false)
        .map { it.size }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val librarySongsCount = database.songs(SongSortType.NAME, false)
        .map { it.size }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val downloadedSongsCount = downloadUtil.downloads
        .map { downloads -> 
            // We need to check database for these IDs to ensure they are songs
            // But for count, just checking completed downloads is usually enough
            downloads.values.count { it.state == Download.STATE_COMPLETED }
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun syncLikedSongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLikedSongs() }
    }

    fun syncLibrarySongs() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncLibrarySongs() }
    }
    
    /**
     * Trigger Firestore sync for logged-in users
     * Called when library screen opens to ensure data is synced
     */
    fun syncFromSupabase() {
        viewModelScope.launch(Dispatchers.IO) {
            if (com.musifly.android.sync.UserSessionManager.isLoggedIn()) {
                Timber.tag("LibrarySongsViewModel").d("🔄 Manual sync triggered from UI")
                likesRepository.syncFromSupabase().fold(
                    onSuccess = { count ->
                        Timber.tag("LibrarySongsViewModel").d("✅ Manual sync completed: $count likes")
                    },
                    onFailure = { e ->
                        Timber.tag("LibrarySongsViewModel").e(e, "❌ Manual sync failed")
                    }
                )
            }
        }
    }
}

@HiltViewModel
class LibraryArtistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allArtists =
        context.dataStore.data
            .map {
                Triple(
                    it[ArtistFilterKey].toEnum(ArtistFilter.LIKED),
                    it[ArtistSortTypeKey].toEnum(ArtistSortType.CREATE_DATE),
                    it[ArtistSortDescendingKey] ?: true,
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filter, sortType, descending) ->
                when (filter) {
                    ArtistFilter.LIBRARY -> database.artists(sortType, descending)
                    ArtistFilter.LIKED -> database.artistsBookmarked(sortType, descending)
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncArtistsSubscriptions() }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allArtists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null || Duration.between(
                            it.lastUpdateTime,
                            LocalDateTime.now()
                        ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryAlbumsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
    private val bookmarksRepository: com.musifly.android.sync.repository.BookmarksRepository,
) : ViewModel() {
    val allAlbums =
        context.dataStore.data
            .map {
                Triple(
                    it[AlbumFilterKey].toEnum(AlbumFilter.LIKED),
                    it[AlbumSortTypeKey].toEnum(AlbumSortType.CREATE_DATE),
                    it[AlbumSortDescendingKey] ?: true,
                )
            }.distinctUntilChanged()
            .flatMapLatest { (filter, sortType, descending) ->
                when (filter) {
                    AlbumFilter.LIBRARY -> database.albums(sortType, descending)
                    AlbumFilter.LIKED -> database.albumsLiked(sortType, descending)
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { 
            syncUtils.syncLikedAlbums()
            bookmarksRepository.syncFromSupabase()
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val isLoggedIn = com.musifly.android.sync.UserSessionManager.isLoggedIn()
            if (isLoggedIn) {
                bookmarksRepository.syncFromSupabase()
            }
            
            allAlbums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryPlaylistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
    private val playlistRepository: com.musifly.android.sync.repository.PlaylistRepository,
) : ViewModel() {
    init {
        Timber.tag("LibPlaylistsVM").d("🚀 LibraryPlaylistsViewModel initialized")
        
        // For logged-in users, trigger sync from Firestore on ViewModel init
        // This ensures playlists are synced when the screen opens
        viewModelScope.launch(Dispatchers.IO) {
            val isLoggedIn = com.musifly.android.sync.UserSessionManager.isLoggedIn()
            val userId = com.musifly.android.sync.UserSessionManager.getUserId()
            
            Timber.tag("LibPlaylistsVM").d("🔍 Checking login status - isLoggedIn: $isLoggedIn, userId: $userId")
            
            // Check local database first to see if playlists exist
            try {
                val localPlaylists = playlistRepository.getAllPlaylists().first()
                Timber.tag("LibPlaylistsVM").d("📊 Local user playlists count: ${localPlaylists.size}")
                if (localPlaylists.isNotEmpty()) {
                    Timber.tag("LibPlaylistsVM").d("📋 Local playlist names: ${localPlaylists.map { it.name }.joinToString()}")
                }
            } catch (e: Exception) {
                Timber.tag("LibPlaylistsVM").w(e, "Failed to check local playlists")
            }
            
            if (isLoggedIn && userId != null) {
                try {
                    Timber.tag("LibPlaylistsVM").d("📥 Logged-in user detected (uid: $userId) - syncing playlists from Supabase...")
                    val result = playlistRepository.syncFromSupabase()
                    result.fold(
                        onSuccess = { count ->
                            Timber.tag("LibPlaylistsVM").d("✅ Synced $count playlists from Supabase to local DB")
                            // Verify sync worked
                            val afterSync = playlistRepository.getAllPlaylists().first()
                            Timber.tag("LibPlaylistsVM").d("📊 User playlists after sync: ${afterSync.size}")
                            if (afterSync.isNotEmpty()) {
                                Timber.tag("LibPlaylistsVM").d("📋 Playlist names after sync: ${afterSync.map { it.name }.joinToString()}")
                            }
                        },
                        onFailure = { e ->
                            Timber.tag("LibPlaylistsVM").e(e, "❌ Failed to sync playlists from Supabase: ${e.message}")
                        }
                    )
                } catch (e: Exception) {
                    Timber.tag("LibPlaylistsVM").e(e, "❌ Exception syncing playlists from Supabase: ${e.message}")
                }
            } else {
                Timber.tag("LibPlaylistsVM").d("ℹ️ User not logged in to Firebase Auth - skipping Supabase sync")
            }
        }
    }
    
    // Simple Flow like LibrarySongsViewModel - directly combine Room Flows
    // Use flatMapLatest to combine both sources, similar to how liked songs work
    val allPlaylists =
        context.dataStore.data
            .onStart { 
                Timber.tag("LibPlaylistsVM").d("🚀 DataStore Flow started")
            }
            .map {
                it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CREATE_DATE) to (it[PlaylistSortDescendingKey]
                    ?: true)
            }.distinctUntilChanged()
            .onEach { (sortType, descending) ->
                Timber.tag("LibPlaylistsVM").d("🔄 DataStore changed - sortType: $sortType, descending: $descending")
            }
            .flatMapLatest { (sortType, descending) ->
                Timber.tag("LibPlaylistsVM").d("🔄 allPlaylists Flow triggered - sortType: $sortType, descending: $descending")
                
                // User playlists are now in main playlist table with bookmarkedAt set (like liked songs)
                // Just query the database directly - no need to combine
                database.allPlaylists(sortType, descending)
                    .onStart { 
                        Timber.tag("LibPlaylistsVM").d("📥 database.allPlaylists Flow started")
                        emit(emptyList()) 
                    }
                    .onEach { 
                        Timber.tag("LibPlaylistsVM").d("📥 database.allPlaylists emitted: ${it.size} playlists")
                        if (it.isNotEmpty()) {
                            Timber.tag("LibPlaylistsVM").d("📋 Playlist names: ${it.take(10).map { p -> p.playlist.name }.joinToString()}${if (it.size > 10) "..." else ""}")
                        }
                    }
            }
            .onStart { 
                Timber.tag("LibPlaylistsVM").d("🚀 allPlaylists Flow STARTED - will emit when collected")
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()) // Use Eagerly to ensure it starts immediately

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) { syncUtils.syncSavedPlaylists() }
    }
    
    /**
     * Manually trigger sync from Firestore for playlists.
     * Called when library screen opens to ensure data is synced
     * Same as LibrarySongsViewModel.syncFromFirestore()
     */
    fun syncFromSupabase() {
        viewModelScope.launch(Dispatchers.IO) {
            if (com.musifly.android.sync.UserSessionManager.isLoggedIn()) {
                Timber.tag("LibPlaylistsVM").d("🔄 Manual sync triggered from UI")
                playlistRepository.syncFromSupabase().fold(
                    onSuccess = { count ->
                        Timber.tag("LibPlaylistsVM").d("✅ Manual sync completed: $count playlists")
                        // Room Flow will automatically emit when data changes, no need for manual trigger
                    },
                    onFailure = { e ->
                        Timber.tag("LibPlaylistsVM").e(e, "❌ Manual sync failed")
                    }
                )
            }
        }
    }

    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
}

@HiltViewModel
class ArtistSongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId = savedStateHandle.get<String>("artistId")!!
    val artist =
        database
            .artist(artistId)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val songs =
        context.dataStore.data
            .map {
                it[ArtistSongSortTypeKey].toEnum(ArtistSongSortType.CREATE_DATE) to (it[ArtistSongSortDescendingKey]
                    ?: true)
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending) ->
                database.artistSongs(artistId, sortType, descending)
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@HiltViewModel
class LibraryMixViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
    var artists =
        database
            .artistsBookmarked(
                ArtistSortType.CREATE_DATE,
                true,
            ).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var albums = database.albumsLiked(AlbumSortType.CREATE_DATE, true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var playlists = database.playlists(PlaylistSortType.CREATE_DATE, true)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            albums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            artists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null ||
                                Duration.between(
                                    it.lastUpdateTime,
                                    LocalDateTime.now(),
                                ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryViewModel
@Inject
constructor() : ViewModel() {
    private val curScreen = mutableStateOf(LibraryFilter.LIBRARY)
    val filter: MutableState<LibraryFilter> = curScreen
}
