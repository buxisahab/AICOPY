package com.musifly.android.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.ArtistItem
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.ArtistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class ArtistSelectionViewModel @Inject constructor(
    private val database: MusicDatabase
) : ViewModel() {

    private val _artists = MutableStateFlow<List<ArtistItem>>(emptyList())
    val artists: StateFlow<List<ArtistItem>> = _artists.asStateFlow()

    private val _selectedArtistIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedArtistIds: StateFlow<Set<String>> = _selectedArtistIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadInitialArtists()
    }

    private fun loadInitialArtists() {
        viewModelScope.launch {
            _isLoading.value = true
            // Search for popular artists to provide a diverse initial selection
            val queries = listOf("Top Artists", "Hindi Artists", "Punjabi Artists", "Pop Artists")
            val allArtists = mutableListOf<ArtistItem>()
            
            queries.forEach { query ->
                YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST)
                    .onSuccess { result ->
                        allArtists.addAll(result.items.filterIsInstance<ArtistItem>())
                    }
            }
            
            // Deduplicate, shuffle for variety, and update state
            _artists.value = allArtists.distinctBy { it.id }.shuffled().take(30)
            _isLoading.value = false
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            searchArtists(query)
        } else if (query.isEmpty()) {
            loadInitialArtists()
        }
    }

    private fun searchArtists(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST)
                .onSuccess { result ->
                    _artists.value = result.items.filterIsInstance<ArtistItem>()
                }
            _isLoading.value = false
        }
    }

    fun toggleSelection(artistId: String) {
        val current = _selectedArtistIds.value
        if (current.contains(artistId)) {
            _selectedArtistIds.value = current - artistId
        } else {
            _selectedArtistIds.value = current + artistId
        }
    }

    fun saveSelectedArtists(onComplete: () -> Unit) {
        viewModelScope.launch {
            val selectedIds = _selectedArtistIds.value
            val selectedArtists = _artists.value.filter { selectedIds.contains(it.id) }
            
            selectedArtists.forEach { artistItem ->
                val artistEntity = ArtistEntity(
                    id = artistItem.id,
                    name = artistItem.title,
                    thumbnailUrl = artistItem.thumbnail,
                    channelId = artistItem.channelId,
                    bookmarkedAt = LocalDateTime.now()
                )
                database.insert(artistEntity)
            }
            onComplete()
        }
    }
}
