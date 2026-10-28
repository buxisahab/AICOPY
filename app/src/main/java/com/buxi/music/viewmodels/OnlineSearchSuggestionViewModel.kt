package com.buxi.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buxi.music.innertube.YouTube
import com.buxi.music.innertube.models.YTItem
import com.buxi.music.innertube.models.filterExplicit
import com.buxi.music.constants.HideExplicitKey
import com.buxi.music.db.MusicDatabase
import com.buxi.music.db.entities.SearchHistory
import com.buxi.music.utils.dataStore
import com.buxi.music.utils.get
import com.buxi.music.jiosaavn.JioSaavn
import com.buxi.music.innertube.models.SongItem
import com.buxi.music.innertube.models.Artist
import com.buxi.music.innertube.models.Album
import com.buxi.music.innertube.models.WatchEndpoint
import com.buxi.music.constants.SearchSource
import com.buxi.music.constants.SearchSourceKey
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
    private val _viewState = MutableStateFlow(SearchSuggestionViewState())
    val viewState = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            query
                .flatMapLatest { query ->
                    if (query.isEmpty()) {
                        database.searchHistory().map { history ->
                            SearchSuggestionViewState(
                                history = history,
                            )
                        }
                    } else {
                        val searchSource = context.dataStore.get(SearchSourceKey, SearchSource.YOUTUBE.name)
                        
                        if (searchSource == SearchSource.JIOSAAVN.name) {
                            val result = JioSaavn.searchSongs(query).getOrNull()
                            database.searchHistory(query).map { it.take(3) }.map { history ->
                                SearchSuggestionViewState(
                                    history = history,
                                    suggestions = emptyList(), // JioSaavn doesn't have suggestions API yet in the snippet
                                    items = result?.results?.map { song ->
                                        SongItem(
                                            id = "js:" + song.id,
                                            title = song.realTitle,
                                            artists = song.primary_artists?.split(",")?.map { Artist(it.trim(), null) } ?: emptyList(),
                                            album = Album(song.album, song.albumid ?: ""),
                                            duration = song.duration?.toIntOrNull(),
                                            thumbnail = song.image.replace("150x150", "500x500"),
                                            explicit = song.explicit_content == "1",
                                            endpoint = WatchEndpoint(videoId = "js:" + song.id)
                                        )
                                    }?.filterExplicit(context.dataStore.get(HideExplicitKey, false)).orEmpty()
                                )
                            }
                        } else {
                            val result = YouTube.searchSuggestions(query).getOrNull()
                            database
                                .searchHistory(query)
                                .map { it.take(3) }
                                .map { history ->
                                    SearchSuggestionViewState(
                                        history = history,
                                        suggestions =
                                            result
                                                ?.queries
                                                ?.filter { query ->
                                                    history.none { it.query == query }
                                                }.orEmpty(),
                                        items =
                                            result
                                                ?.recommendedItems
                                                ?.filterExplicit(
                                                    context.dataStore.get(
                                                        HideExplicitKey,
                                                        false,
                                                    ),
                                                ).orEmpty(),
                                    )
                                }
                        }
                    }
                }.collect {
                    _viewState.value = it
                }
        }
    }
}

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
)
