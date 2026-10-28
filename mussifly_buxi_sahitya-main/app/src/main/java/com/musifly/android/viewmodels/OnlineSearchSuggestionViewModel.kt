package com.musifly.android.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.YTItem
import com.musifly.innertube.models.SongItem
import com.musifly.innertube.models.AlbumItem
import com.musifly.innertube.models.ArtistItem
import com.musifly.innertube.models.PlaylistItem
import com.musifly.innertube.models.Album
import com.musifly.innertube.models.Artist
import com.musifly.innertube.models.filterExplicit
import com.musifly.android.constants.HideExplicitKey
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.SearchHistory
import com.musifly.android.utils.dataStore
import com.musifly.android.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    database: MusicDatabase,
    val saavnAPI: com.musifly.android.streamingengines.saavn.SaavnAPI,
) : ViewModel() {
    val query = MutableStateFlow("")
    private val _viewState = MutableStateFlow(SearchSuggestionViewState())
    val viewState = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            query
                .debounce(300L)
                .distinctUntilChanged()
                .flatMapLatest { text: String ->
                    if (text.isEmpty()) {
                        // Fetch trending songs for "For you" when empty
                        val trendingState = MutableStateFlow<List<YTItem>>(emptyList())
                        launch {
                           try {
                               val trending = withContext(Dispatchers.IO) {
                                   saavnAPI.getTrendingSongs(20)
                               }
                               trendingState.value = trending.map { it.toYTItem("song") }
                           } catch (e: Exception) {
                               Timber.e(e, "Failed to fetch trending songs")
                           }
                        }

                        database.searchHistory().combine(trendingState) { history, trendingItems ->
                            SearchSuggestionViewState(
                                history = history,
                                items = trendingItems
                            )
                        }
                    } else {
                    val networkData = MutableStateFlow(
                        Triple<com.musifly.innertube.models.SearchSuggestions?, Map<String, Any>?, List<String>>(
                            null, null, emptyList()
                        )
                    )

                        launch {
                            val ytResult = withContext(Dispatchers.IO) {
                                YouTube.searchSuggestions(text).getOrNull()
                            }
                        networkData.update { current ->
                            Triple(ytResult, current.second, current.third)
                        }
                        }
                        launch {
                            val saavnResults = withContext(Dispatchers.IO) {
                                try {
                                    saavnAPI.fetchGlobalSearchResults(text)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        networkData.update { current ->
                            Triple(current.first, saavnResults, current.third)
                        }
                        }
                        launch {
                            val saavnSug = withContext(Dispatchers.IO) {
                                try {
                                    saavnAPI.getSearchSuggestions(text)
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                        networkData.update { current ->
                            Triple(current.first, current.second, saavnSug)
                        }
                        }

                        database
                            .searchHistory(text)
                        .combine(networkData) { history: List<SearchHistory>, (yt: com.musifly.innertube.models.SearchSuggestions?, saavnR: Map<String, Any>?, saavnS: List<String>) ->
                                val combinedSuggestions = (yt?.queries.orEmpty() + saavnS)
                                    .distinct()
                                    .filter { suggestion ->
                                        history.none { it.query == suggestion }
                                    }

                                val saavnSongs = (saavnR?.get("songs") as? List<Map<String, Any>>).orEmpty()
                                    .map { it.toYTItem("song") }
                                val saavnAlbums = (saavnR?.get("albums") as? List<Map<String, Any>>).orEmpty()
                                    .map { it.toYTItem("album") }
                                val saavnArtists = (saavnR?.get("artists") as? List<Map<String, Any>>).orEmpty()
                                    .map { it.toYTItem("artist") }
                                val saavnPlaylists = (saavnR?.get("playlists") as? List<Map<String, Any>>).orEmpty()
                                    .map { it.toYTItem("playlist") }

                                SearchSuggestionViewState(
                                    history = history.take(3),
                                    suggestions = combinedSuggestions,
                                    items =
                                        (yt?.recommendedItems.orEmpty() + saavnSongs + saavnAlbums + saavnArtists + saavnPlaylists)
                                            .distinctBy { it.id }
                                            .filterExplicit(
                                                context.dataStore.get(
                                                    HideExplicitKey,
                                                    false,
                                                ),
                                            ),
                                )
                            }
                    }
                }.collect { state: SearchSuggestionViewState ->
                    _viewState.value = state
                }
        }
    }

    private fun Map<String, Any>.toYTItem(type: String): YTItem {
        val id = this["id"] as? String ?: ""
        val saavnId = if (id.startsWith("saavn:")) id else "saavn:$id"
        val title = this["title"] as? String ?: this["name"] as? String ?: "Unknown"
        val thumbnail = this["image"] as? String ?: ""

        return when (type) {
            "song" -> SongItem(
                id = saavnId,
                title = title,
                thumbnail = thumbnail,
                artists = listOf(Artist(name = this["artist"] as? String ?: "Unknown", id = null)),
                album = (this["album"] as? String)?.takeIf { it.isNotEmpty() }?.let { Album(name = it, id = "") },
                duration = when (val durationValue = this["durationSeconds"]) {
                    is Int -> durationValue
                    is String -> durationValue.toIntOrNull()
                    else -> 0
                },
                explicit = false
            )
            "album" -> AlbumItem(
                browseId = saavnId,
                playlistId = "",
                title = title,
                artists = (this["artist"] as? String)?.let { listOf(Artist(name = it, id = null)) },
                thumbnail = thumbnail,
                year = (this["year"] as? String)?.toIntOrNull()
            )
            "artist" -> ArtistItem(
                id = saavnId,
                title = title,
                thumbnail = thumbnail,
                shuffleEndpoint = null,
                radioEndpoint = null
            )
            "playlist" -> PlaylistItem(
                id = saavnId,
                title = title,
                author = (this["artist"] as? String)?.let { Artist(name = it, id = null) },
                songCountText = (this["songsCount"] as? String) ?: (this["subtitle"] as? String),
                thumbnail = thumbnail,
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null
            )
            else -> SongItem(
                id = saavnId,
                title = title,
                thumbnail = thumbnail,
                artists = emptyList(),
                explicit = false
            )
        }
    }
}

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
)
