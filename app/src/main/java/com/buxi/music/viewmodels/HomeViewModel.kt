package com.buxi.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buxi.music.innertube.YouTube
import com.buxi.music.innertube.models.PlaylistItem
import com.buxi.music.innertube.models.WatchEndpoint
import com.buxi.music.innertube.models.YTItem
import com.buxi.music.innertube.pages.ExplorePage
import com.buxi.music.innertube.pages.HomePage
import com.buxi.music.innertube.utils.completedLibraryPage
import com.buxi.music.db.MusicDatabase
import com.buxi.music.db.entities.Album
import com.buxi.music.db.entities.Artist
import com.buxi.music.db.entities.LocalItem
import com.buxi.music.db.entities.Playlist
import com.buxi.music.db.entities.Song
import com.buxi.music.models.SimilarRecommendation
import com.buxi.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)

    val quickPicks = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val homePage = MutableStateFlow<HomePage?>(null)
    val explorePage = MutableStateFlow<ExplorePage?>(null)
    val recentActivity = MutableStateFlow<List<YTItem>?>(null)
    val recentPlaylistsDb = MutableStateFlow<List<Playlist>?>(null)

    val allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allYtItems = MutableStateFlow<List<YTItem>>(emptyList())

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)

    private suspend fun load() {
        isLoading.value = true
        
        try {
            quickPicks.value = database.quickPicks()
                .first().shuffled().take(20)

            forgottenFavorites.value = database.forgottenFavorites()
                .first().shuffled().take(20)

            val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2
            val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5)
                .first().shuffled().take(10)
            val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2)
                .first().filter { it.album.thumbnailUrl != null }.shuffled().take(5)
            val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp)
                .first().filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }
                .shuffled().take(5)
            keepListening.value =
                (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()

            allLocalItems.value =
                (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
                    .filter { it is Song || it is Album }

            // Load YouTube data with proper error handling
            if (YouTube.cookie != null) {
                try {
                    YouTube.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess {
                        accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>()
                            .filterNot { it.id == "SE" }
                    }.onFailure {
                        reportException(it)
                    }
                } catch (e: Exception) {
                    reportException(e)
                }
            }

        // Similar to artists - with error handling
            val artistRecommendations = try {
                database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
                    .filter { it.artist.isYouTubeArtist }
                    .shuffled().take(3)
                    .mapNotNull {
                        val items = mutableListOf<YTItem>()
                        try {
                            YouTube.artist(it.id).onSuccess { page ->
                                items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                                items += page.sections.lastOrNull()?.items.orEmpty()
                            }
                        } catch (e: Exception) {
                            reportException(e)
                        }
                        SimilarRecommendation(
                            title = it,
                            items = items
                                .shuffled()
                                .ifEmpty { return@mapNotNull null }
                        )
                    }
            } catch (e: Exception) {
                reportException(e)
                emptyList()
            }
                
        // Similar to songs - with error handling
        val songRecommendations = try {
            database.mostPlayedSongs(fromTimeStamp, limit = 10).first()
                .filter { it.album != null }
                .shuffled().take(2)
                .mapNotNull { song ->
                    try {
                        val endpoint =
                            YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                                ?: return@mapNotNull null
                        val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                        SimilarRecommendation(
                            title = song,
                            items = (page.songs.shuffled().take(8) +
                                    page.albums.shuffled().take(4) +
                                    page.artists.shuffled().take(4) +
                                    page.playlists.shuffled().take(4))
                                .shuffled()
                                .ifEmpty { return@mapNotNull null }
                        )
                    } catch (e: Exception) {
                        reportException(e)
                        null
                    }
                }
        } catch (e: Exception) {
            reportException(e)
            emptyList()
        }
        similarRecommendations.value = (artistRecommendations + songRecommendations).shuffled()

        // Load home page with error handling
        try {
            YouTube.home().onSuccess { page ->
                homePage.value = page
            }.onFailure {
                reportException(it)
            }
        } catch (e: Exception) {
            reportException(e)
        }

        // Load explore page with error handling
        try {
            YouTube.explore().onSuccess { page ->
                val artists: Set<String>
                val favouriteArtists: Set<String>
                database.artistsBookmarkedByCreateDateAsc().first().let { list ->
                    artists = list.map(Artist::id).toHashSet()
                    favouriteArtists = list
                        .filter { it.artist.bookmarkedAt != null }
                        .map { it.id }
                        .toHashSet()
                }
                explorePage.value = page.copy(
                    newReleaseAlbums = page.newReleaseAlbums
                        .sortedBy { album ->
                            if (album.artists.orEmpty().any { it.id in favouriteArtists }) 0
                            else if (album.artists.orEmpty().any { it.id in artists }) 1
                            else 2
                        }
                )
            }.onFailure {
                reportException(it)
            }
        } catch (e: Exception) {
            reportException(e)
        }

        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty() +
                explorePage.value?.newReleaseAlbums.orEmpty()

        } catch (e: Exception) {
            reportException(e)
        } finally {
            isLoading.value = false
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load()
            isRefreshing.value = false
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            load()
        }
    }
}
