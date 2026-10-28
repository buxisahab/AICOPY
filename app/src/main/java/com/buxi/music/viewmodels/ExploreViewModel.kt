package com.buxi.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buxi.music.innertube.YouTube
import com.buxi.music.innertube.models.filterExplicit
import com.buxi.music.innertube.pages.ExplorePage
import com.buxi.music.constants.HideExplicitKey
import com.buxi.music.db.MusicDatabase
import com.buxi.music.utils.dataStore
import com.buxi.music.utils.get
import com.buxi.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    val explorePage = MutableStateFlow<ExplorePage?>(null)

    private suspend fun load() {
        try {
            YouTube
                .explore()
                .onSuccess { page ->
                    try {
                        val artists: MutableMap<Int, String> = mutableMapOf()
                        val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                        database.allArtistsByPlayTime().first().let { list ->
                            var favIndex = 0
                            for ((artistsIndex, artist) in list.withIndex()) {
                                artists[artistsIndex] = artist.id
                                if (artist.artist.bookmarkedAt != null) {
                                    favouriteArtists[favIndex] = artist.id
                                    favIndex++
                                }
                            }
                        }
                        explorePage.value =
                            page.copy(
                                newReleaseAlbums =
                                    page.newReleaseAlbums
                                        .sortedBy { album ->
                                            val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                            val firstArtistKey =
                                                artistIds.firstNotNullOfOrNull { artistId ->
                                                    if (artistId in favouriteArtists.values) {
                                                        favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                                    } else {
                                                        artists.entries.firstOrNull { it.value == artistId }?.key
                                                    }
                                                } ?: Int.MAX_VALUE
                                            firstArtistKey
                                        }.filterExplicit(context.dataStore.get(HideExplicitKey, false)),
                            )
                    } catch (e: Exception) {
                        reportException(e)
                        // Still set the original page if sorting fails
                        explorePage.value = page
                    }
                }.onFailure {
                    reportException(it)
                }
        } catch (e: Exception) {
            reportException(e)
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            load()
        }
    }
}
