package com.musifly.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.Album
import com.musifly.android.db.entities.Artist
import com.musifly.android.db.entities.LocalItem
import com.musifly.android.db.entities.Playlist
import com.musifly.android.db.entities.Song
import com.musifly.android.models.toSaavnSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LocalSearchViewModel
@Inject
constructor(
    private val database: MusicDatabase,
    val saavnAPI: com.musifly.android.streamingengines.saavn.SaavnAPI,
) : ViewModel() {
    val query = MutableStateFlow("")
    val filter = MutableStateFlow(LocalFilter.ALL)

    val result =
        combine(query, filter) { query, filter ->
            query to filter
        }.flatMapLatest { (query, filter) ->
            if (query.isEmpty()) {
                flowOf(LocalSearchResult("", filter, emptyMap()))
            } else {
                val saavnResultsFlow = if (filter == LocalFilter.ALL || filter == LocalFilter.SONG) {
                    kotlinx.coroutines.flow.flow {
                        val results = try {
                            saavnAPI.fetchSearchResults(query, 5)["songs"] as? List<Map<String, Any>>
                        } catch (e: Exception) {
                            null
                        }
                        emit(results?.map { it.toSaavnSong() } ?: emptyList<com.musifly.android.models.SaavnSong>())
                    }
                } else {
                    flowOf(emptyList<com.musifly.android.models.SaavnSong>())
                }

                when (filter) {
                    LocalFilter.ALL ->
                        combine(
                            database.searchSongs(query, PREVIEW_SIZE),
                            database.searchAlbums(query, PREVIEW_SIZE),
                            database.searchArtists(query, PREVIEW_SIZE),
                            database.searchPlaylists(query, PREVIEW_SIZE),
                            saavnResultsFlow
                        ) { songs, albums, artists, playlists, saavnSongs ->
                            songs + albums + artists + playlists + saavnSongs
                        }

                    LocalFilter.SONG -> 
                        combine(database.searchSongs(query), saavnResultsFlow) { local, saavn ->
                            local + saavn
                        }
                    LocalFilter.SAAVN_SONG -> saavnResultsFlow
                    LocalFilter.ALBUM -> database.searchAlbums(query)
                    LocalFilter.ARTIST -> database.searchArtists(query)
                    LocalFilter.PLAYLIST -> database.searchPlaylists(query)
                }.map { list ->
                    LocalSearchResult(
                        query = query,
                        filter = filter,
                        map =
                            list.groupBy {
                                when (it) {
                                    is Song -> LocalFilter.SONG
                                    is Album -> LocalFilter.ALBUM
                                    is Artist -> LocalFilter.ARTIST
                                    is Playlist -> LocalFilter.PLAYLIST
                                    is com.musifly.android.models.SaavnSong -> LocalFilter.SAAVN_SONG
                                    else -> LocalFilter.ALL
                                }
                            },
                    )
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            LocalSearchResult("", filter.value, emptyMap())
        )

    companion object {
        const val PREVIEW_SIZE = 3
    }
}

enum class LocalFilter {
    ALL,
    SONG,
    ALBUM,
    ARTIST,
    PLAYLIST,
    SAAVN_SONG,
}

data class LocalSearchResult(
    val query: String,
    val filter: LocalFilter,
    val map: Map<LocalFilter, List<Any>>,
)
