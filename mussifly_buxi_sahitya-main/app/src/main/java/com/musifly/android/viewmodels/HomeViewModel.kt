package com.musifly.android.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.innertube.YouTube
import com.musifly.innertube.models.PlaylistItem
import com.musifly.innertube.models.WatchEndpoint
import com.musifly.innertube.models.YTItem
import com.musifly.innertube.pages.ExplorePage
import com.musifly.innertube.pages.HomePage
import com.musifly.innertube.utils.completedLibraryPage
import com.musifly.android.streamingengines.StreamSourceManager
import com.musifly.android.streamingengines.StreamingEngineManager
import com.musifly.android.streamingengines.saavn.SaavnAPI
import com.musifly.android.db.MusicDatabase
import android.util.Log
import com.musifly.android.db.entities.Album
import com.musifly.android.db.entities.Artist
import com.musifly.android.db.entities.LocalItem
import com.musifly.android.db.entities.Playlist
import com.musifly.android.db.entities.Song
import com.musifly.android.models.SaavnHomeSection
import com.musifly.android.models.SimilarRecommendation
import com.musifly.android.utils.reportException
import com.musifly.android.utils.RecommendationEngine as UtilsRecommendationEngine
import com.musifly.android.recommendations.RecommendationEngine
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
    
    // JioSaavn home data - NEW STRUCTURED APPROACH
    val saavnHomeFeed = MutableStateFlow<List<SaavnHomeSection>>(emptyList())
    
    // Legacy fields (kept for backward compatibility during transition)
    val saavnHomeData = MutableStateFlow<List<Map<String, Any>>?>(null) // List of sections
    val saavnFeaturedPlaylists = MutableStateFlow<List<Map<String, Any>>?>(null)
    val saavnTopCharts = MutableStateFlow<List<Map<String, Any>>?>(null) // Top charts
    val saavnTrendingArtists = MutableStateFlow<List<Map<String, Any>>?>(null) // Trending artists
    
    // Unified home songs list - this is what UI observes for instant updates
    // When source changes, this list is cleared and repopulated with new source's songs
    val homeSongs = MutableStateFlow<List<Any>>(emptyList()) // Can hold SongItem, Map<String,Any> for JioSaavn, etc.

    val accountName = MutableStateFlow("Guest")
    val accountImageUrl = MutableStateFlow<String?>(null)
    
    // Smart recommendation sections
    val recommendationSections = MutableStateFlow<List<com.musifly.android.models.RecommendationSection>>(emptyList())

    /**
     * Load home content from current source
     * Called after source change to populate new data
     */
    fun loadHomeFromSource() {
        viewModelScope.launch(Dispatchers.IO) {
            load(force = true)
        }
    }

    private suspend fun load(force: Boolean = false) {
        // REAL-TIME SAFE: Skip heavy work during playback unless forced
        if (!force && com.musifly.android.playback.PlaybackPriorityManager.shouldPauseBackgroundWork()) {
            Log.d("HomeViewModel", "⏸️ Skipping home refresh - playback priority mode active")
            return
        }
        
        isLoading.value = true

        // CRITICAL: Clear ALL old data FIRST before loading new source
        clearAllHomeData()

        // Load based on current source - each source has separate, independent loader
        when (StreamSourceManager.currentSource) {
            StreamSourceManager.StreamType.YOUTUBE -> loadYouTubeHome(force)
            StreamSourceManager.StreamType.YT_MUSIC -> loadYouTubeMusicHome(force)
            StreamSourceManager.StreamType.JIOSAAVN -> loadJioSaavnHome(force)
        }
        
        // Load smart recommendations (works for all sources) - skip during playback unless forced
        if (force || !com.musifly.android.playback.PlaybackPriorityManager.shouldPauseBackgroundWork()) {
            loadRecommendations()
        }
        
        // Update unified home songs list based on current source
        updateHomeSongsList()

        isLoading.value = false
    }
    
    /**
     * Load smart recommendation sections based on listening history
     */
    private suspend fun loadRecommendations() {
        try {
            // Use the old RecommendationEngine from utils for general recommendations
            // (This is separate from the JioSaavn-specific recommendations in loadJioSaavnHome)
            val sections = UtilsRecommendationEngine.generateAllSections(database)
            recommendationSections.value = sections
            Log.d("HomeViewModel", "✅ Loaded ${sections.size} recommendation sections")
        } catch (e: Exception) {
            reportException(e)
            Log.e("HomeViewModel", "❌ Failed to load recommendations: ${e.message}", e)
            recommendationSections.value = emptyList()
        }
    }
    
    /**
     * Update unified homeSongs list based on current source
     * This is what UI observes for instant updates
     */
    private fun updateHomeSongsList() {
        when (StreamSourceManager.currentSource) {
            StreamSourceManager.StreamType.YOUTUBE,
            StreamSourceManager.StreamType.YT_MUSIC -> {
                // Flatten all YouTube items into single list
                val allItems = mutableListOf<Any>()
                homePage.value?.sections?.forEach { section ->
                    allItems.addAll(section.items)
                }
                explorePage.value?.newReleaseAlbums?.let { allItems.addAll(it) }
                similarRecommendations.value?.forEach { rec ->
                    allItems.addAll(rec.items)
                }
                homeSongs.value = allItems
                Log.d("HomeViewModel", "📊 Updated homeSongs: ${allItems.size} YouTube items")
            }
            StreamSourceManager.StreamType.JIOSAAVN -> {
                // Combine JioSaavn sections into single list
                val allItems = mutableListOf<Any>()
                saavnFeaturedPlaylists.value?.let { allItems.addAll(it) }
                saavnTopCharts.value?.let { allItems.addAll(it) } // Add top charts
                saavnHomeData.value?.forEach { section ->
                    val items = section["items"] as? List<Map<String, Any>> ?: emptyList()
                    allItems.addAll(items)
                }
                homeSongs.value = allItems
                Log.d("HomeViewModel", "📊 Updated homeSongs: ${allItems.size} JioSaavn items (${saavnTopCharts.value?.size ?: 0} top charts)")
            }
        }
    }
    
    /**
     * Clear all home data to prevent old cached data from showing
     * This is called BEFORE loading new source data
     * CRITICAL: Must clear homeSongs StateFlow to force UI update
     */
    fun clearHomeCache() {
        Log.d("HomeViewModel", "🧹 Clearing home cache - wiping all source-specific data")
        
        // Clear unified home songs list FIRST (forces UI update)
        homeSongs.value = emptyList()
        
        // Clear YouTube data
        homePage.value = null
        explorePage.value = null
        accountPlaylists.value = null
        similarRecommendations.value = null
        allYtItems.value = emptyList()
        
        // Clear JioSaavn data
        saavnHomeFeed.value = emptyList()
        saavnHomeData.value = null
        saavnFeaturedPlaylists.value = null
        saavnTopCharts.value = null
        saavnTrendingArtists.value = null
        
        // Clear recommendation sections (will be regenerated)
        recommendationSections.value = emptyList()
        
        // Keep local items (Quick Picks, Forgotten Favorites, Keep Listening)
        // These are database-based and don't depend on source
    }
    
    /**
     * Private clear function (called from load())
     */
    private fun clearAllHomeData() {
        clearHomeCache()
    }
    
    /**
     * Load YouTube home content (default)
     * INDEPENDENT implementation - only loads YouTube data
     */
    private suspend fun loadYouTubeHome(force: Boolean) {
        // REAL-TIME SAFE: Skip heavy work during playback unless forced
        if (!force && com.musifly.android.playback.PlaybackPriorityManager.shouldPauseBackgroundWork()) {
            Log.d("HomeViewModel", "⏸️ Skipping YouTube home load - playback priority mode active")
            return
        }
        
        Log.d("HomeViewModel", "📥 Loading YouTube home content...")
        
        // Clear JioSaavn-specific data first (already cleared in clearAllHomeData, but ensure)
        saavnHomeData.value = null
        saavnFeaturedPlaylists.value = null
        saavnTopCharts.value = null

        // Load Quick Picks with fallback
        try {
            val standardQuickPicks = database.quickPicks()
                .first().shuffled().take(20)
            
            quickPicks.value = if (standardQuickPicks.isNotEmpty()) {
                standardQuickPicks
            } else {
                Log.d("HomeViewModel", "⚠️ YouTube Quick Picks empty, using fallback")
                database.mostPlayedSongs(
                    System.currentTimeMillis() - 86400000 * 30L, // Last 30 days
                    limit = 20
                ).first().shuffled()
            }
            Log.d("HomeViewModel", "🎵 Quick Picks loaded for YouTube: ${quickPicks.value?.size ?: 0} songs")
        } catch (e: Exception) {
            Log.e("HomeViewModel", "❌ Failed to load Quick Picks", e)
            quickPicks.value = emptyList()
        }

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

        if (YouTube.cookie != null) {
            YouTube.library("FEmusic_liked_playlists").completedLibraryPage().onSuccess {
                accountPlaylists.value = it.items.filterIsInstance<PlaylistItem>()
                    .filterNot { it.id == "SE" }
            }.onFailure {
                reportException(it)
            }
        }

        // Similar to artists
        val artistRecommendations =
            database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
                .filter { it.artist.isYouTubeArtist }
                .shuffled().take(3)
                .mapNotNull {
                    val items = mutableListOf<YTItem>()
                    YouTube.artist(it.id).onSuccess { page ->
                        items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                        items += page.sections.lastOrNull()?.items.orEmpty()
                    }
                    SimilarRecommendation(
                        title = it,
                        items = items
                            .shuffled()
                            .ifEmpty { return@mapNotNull null }
                    )
                }
        // Similar to songs
        val songRecommendations =
            database.mostPlayedSongs(fromTimeStamp, limit = 10).first()
                .filter { it.album != null }
                .shuffled().take(2)
                .mapNotNull { song ->
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
                }
        similarRecommendations.value = (artistRecommendations + songRecommendations).shuffled()

        YouTube.home().onSuccess { page ->
            homePage.value = page
        }.onFailure {
            reportException(it)
        }

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

        allYtItems.value = similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                homePage.value?.sections?.flatMap { it.items }.orEmpty() +
                explorePage.value?.newReleaseAlbums.orEmpty()

        Log.d("HomeViewModel", "✅ YouTube home loaded: ${homePage.value?.sections?.size ?: 0} sections, ${allYtItems.value.size} items")
        
        // Note: homeSongs will be updated by updateHomeSongsList() called from load()
    }
    
    /**
     * Load YouTube Music home content
     * INDEPENDENT implementation - same as YouTube for now, can be customized later
     */
    private suspend fun loadYouTubeMusicHome(force: Boolean) {
        Log.d("HomeViewModel", "📥 Loading YouTube Music home content...")
        // YouTube Music uses same API as YouTube but filtered for music content
        loadYouTubeHome(force) // Same implementation for now, can be customized later
    }
    
    /**
     * Load JioSaavn home content with AI-powered recommendations
     * NEW: Complete recommendation pipeline with Daily Mixes, AI recommendations, and trending sections
     */
    private suspend fun loadJioSaavnHome(force: Boolean) {
        // REAL-TIME SAFE: Skip heavy analysis during playback unless forced
        if (!force && com.musifly.android.playback.PlaybackPriorityManager.shouldPauseBackgroundWork()) {
            Log.d("HomeViewModel", "⏸️ Skipping JioSaavn home load - playback priority mode active")
            return
        }
        
        try {
            Log.d("HomeViewModel", "🎯 Starting JioSaavn home feed load with AI recommendations...")
            val saavnAPI = SaavnAPI()
            val recommendationEngine = RecommendationEngine(database, saavnAPI)
            
            // Load Quick Picks with fallback
            // 1. Try standard Quick Picks (recommendations from related songs)
            val standardQuickPicks = database.quickPicks()
                .first().shuffled().take(20)
            
            // 2. If empty (common for JioSaavn users as related map isn't populated), 
            // fallback to most played/recent songs
            quickPicks.value = if (standardQuickPicks.isNotEmpty()) {
                standardQuickPicks
            } else {
                Log.d("HomeViewModel", "⚠️ Standard Quick Picks empty, using fallback")
                database.mostPlayedSongs(
                    System.currentTimeMillis() - 86400000 * 30L, // Last 30 days
                    limit = 20
                ).first().shuffled()
            }
            Log.d("HomeViewModel", "🎵 Quick Picks loaded for JioSaavn: ${quickPicks.value?.size ?: 0} songs")
            
            forgottenFavorites.value = database.forgottenFavorites()
                .first().shuffled().take(20)
            Log.d("HomeViewModel", "💭 Forgotten Favorites loaded: ${forgottenFavorites.value?.size ?: 0} songs")
            
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
            Log.d("HomeViewModel", "🔄 Keep Listening loaded: ${keepListening.value?.size ?: 0} items")
            
            allLocalItems.value =
                (quickPicks.value.orEmpty() + forgottenFavorites.value.orEmpty() + keepListening.value.orEmpty())
                    .filter { it is Song || it is Album }
            
            // IMPORTANT: Data already cleared in clearAllHomeData() - don't reuse old lists
            
            // ============ RECOMMENDATION PIPELINE ============
            // Step 1: Analyze listening pattern - SKIP during playback unless forced
            if (!force && com.musifly.android.playback.PlaybackPriorityManager.shouldPauseBackgroundWork()) {
                Log.d("HomeViewModel", "⏸️ Skipping listening pattern analysis - playback active")
                return
            }
            Log.d("HomeViewModel", "📊 Step 1: Analyzing listening pattern...")
            val listeningPattern = recommendationEngine.analyzeListeningPattern()
            
            // Step 2: Generate Daily Mixes - SKIP during playback unless forced
            if (!force && com.musifly.android.playback.PlaybackPriorityManager.shouldPauseBackgroundWork()) {
                Log.d("HomeViewModel", "⏸️ Skipping Daily Mixes generation - playback active")
                return
            }
            Log.d("HomeViewModel", "🎵 Step 2: Generating Daily Mixes...")
            val dailyMixes = recommendationEngine.generateDailyMixes(listeningPattern)
            
            // Step 3: Generate AI Recommendations - SKIP during playback unless forced
            if (!force && com.musifly.android.playback.PlaybackPriorityManager.shouldPauseBackgroundWork()) {
                Log.d("HomeViewModel", "⏸️ Skipping AI recommendations - playback active")
                return
            }
            Log.d("HomeViewModel", "🤖 Step 3: Generating AI recommendations...")
            val aiRecommendations = recommendationEngine.generateAIRecommendations(listeningPattern)
            
            // Step 4: Fetch Trending & Editorial Sections - SKIP during playback unless forced
            if (!force && com.musifly.android.playback.PlaybackPriorityManager.shouldPauseBackgroundWork()) {
                Log.d("HomeViewModel", "⏸️ Skipping trending sections fetch - playback active")
                return
            }
            Log.d("HomeViewModel", "📊 Step 4: Fetching trending & editorial sections...")
            val trendingSections = saavnAPI.fetchTrendingAndEditorialSections()
            
            // Step 5: Combine all sections (Personalized first, then Trending)
            Log.d("HomeViewModel", "🔗 Step 5: Combining all sections...")
            val allSections = mutableListOf<SaavnHomeSection>()
            
            // Add Daily Mixes first (personalized)
            allSections.addAll(dailyMixes)
            Log.d("HomeViewModel", "   ✅ Added ${dailyMixes.size} Daily Mixes")
            
            // Add AI Recommendations (personalized)
            allSections.addAll(aiRecommendations)
            Log.d("HomeViewModel", "   ✅ Added ${aiRecommendations.size} AI recommendation sections")
            
            // Add Trending & Editorial sections (below personalized)
            allSections.addAll(trendingSections)
            Log.d("HomeViewModel", "   ✅ Added ${trendingSections.size} trending/editorial sections")
            
            // CRITICAL: Only filter out sections that are truly empty (items.isEmpty())
            val validSections = allSections.filter { section ->
                val isValid = section.items.isNotEmpty()
                if (!isValid) {
                    Log.w("HomeViewModel", "⚠️ Filtering out empty section: '${section.title}' (0 items)")
                }
                isValid
            }
            
            Log.d("HomeViewModel", "🎯 Final Saavn Section Count = ${validSections.size}")
            Log.d("HomeViewModel", "   📊 Breakdown:")
            Log.d("HomeViewModel", "      - Daily Mixes: ${dailyMixes.size}")
            Log.d("HomeViewModel", "      - AI Recommendations: ${aiRecommendations.size}")
            Log.d("HomeViewModel", "      - Trending/Editorial: ${trendingSections.size}")
            
            saavnHomeFeed.value = validSections
            
            // Also populate legacy fields for backward compatibility
            val allPlaylists = mutableListOf<Map<String, Any>>()
            val allCharts = mutableListOf<Map<String, Any>>()
            val allArtists = mutableListOf<Map<String, Any>>()
            val legacySections = mutableListOf<Map<String, Any>>()
            
            validSections.forEach { section ->
                when (section.type) {
                    com.musifly.android.models.SaavnSectionType.FEATURED,
                    com.musifly.android.models.SaavnSectionType.PLAYLIST,
                    com.musifly.android.models.SaavnSectionType.MIX,
                    com.musifly.android.models.SaavnSectionType.ORIGINALS -> {
                        section.items.forEach { item ->
                            if (item is com.musifly.android.models.SaavnPlaylist) {
                                allPlaylists.add(mapOf(
                                    "id" to item.id,
                                    "title" to item.title,
                                    "image" to item.image,
                                    "subtitle" to (item.subtitle ?: ""),
                                    "songCount" to (item.songCount ?: "0"),
                                    "type" to "playlist"
                                ))
                            }
                        }
                    }
                    com.musifly.android.models.SaavnSectionType.CHART -> {
                        section.items.forEach { item ->
                            if (item is com.musifly.android.models.SaavnChartEntry) {
                                val chartItem = item.item
                                allCharts.add(mapOf(
                                    "id" to chartItem.id,
                                    "title" to chartItem.title,
                                    "image" to chartItem.image,
                                    "subtitle" to (chartItem.subtitle ?: ""),
                                    "type" to "album",
                                    "rank" to item.rank
                                ))
                            }
                        }
                    }
                    com.musifly.android.models.SaavnSectionType.ARTIST -> {
                        section.items.forEach { item ->
                            if (item is com.musifly.android.models.SaavnArtist) {
                                allArtists.add(mapOf(
                                    "id" to item.id,
                                    "name" to item.title,
                                    "image" to item.image,
                                    "subtitle" to (item.subtitle ?: ""),
                                    "type" to "artist"
                                ))
                            }
                        }
                    }
                    else -> {
                        // Convert other sections to legacy format
                        val sectionItems = section.items.map { item ->
                            when (item) {
                                is com.musifly.android.models.SaavnSong -> mapOf(
                                    "id" to item.id,
                                    "title" to item.title,
                                    "image" to item.image,
                                    "subtitle" to (item.subtitle ?: item.artist),
                                    "artist" to item.artist,
                                    "album" to (item.album ?: ""),
                                    "type" to "song"
                                )
                                is com.musifly.android.models.SaavnAlbum -> mapOf(
                                    "id" to item.id,
                                    "title" to item.title,
                                    "image" to item.image,
                                    "subtitle" to (item.subtitle ?: item.artist),
                                    "artist" to item.artist,
                                    "type" to "album"
                                )
                                else -> emptyMap<String, Any>()
                            }
                        }.filter { it.isNotEmpty() }
                        
                        if (sectionItems.isNotEmpty()) {
                            legacySections.add(mapOf(
                                "title" to section.title,
                                "type" to section.type.name.lowercase(),
                                "items" to sectionItems
                            ))
                        }
                    }
                }
            }
            
            // Update legacy fields
            saavnFeaturedPlaylists.value = allPlaylists.take(20)
            saavnTopCharts.value = allCharts.take(20)
            saavnTrendingArtists.value = allArtists.take(20)
            saavnHomeData.value = legacySections
            
            Log.d("HomeViewModel", "✅ JioSaavn home loaded: ${validSections.size} structured sections, ${allPlaylists.size} playlists, ${allCharts.size} charts, ${allArtists.size} artists")
            
            // Note: homeSongs will be updated by updateHomeSongsList() called from load()
        } catch (e: Exception) {
            reportException(e)
            Log.e("HomeViewModel", "❌ Failed to load JioSaavn home: ${e.message}", e)
            // Ensure empty lists on error
            saavnHomeFeed.value = emptyList()
            saavnHomeData.value = emptyList()
            saavnFeaturedPlaylists.value = emptyList()
            saavnTopCharts.value = emptyList()
            saavnTrendingArtists.value = emptyList()
            homeSongs.value = emptyList()
        }
    }

    /**
     * Force refresh - clears old data and loads fresh content from current source
     * This is called when source changes to ensure no cached data remains
     */
    fun refresh() {
        // REAL-TIME SAFE: Force refresh even during playback when user explicitly requests it

        if (isRefreshing.value) {
            Log.d("HomeViewModel", "⚠️ Refresh already in progress, skipping")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            Log.d("HomeViewModel", "🔄 Starting refresh for source: ${StreamSourceManager.currentSource}")
            load(force = true)
            isRefreshing.value = false
            Log.d("HomeViewModel", "✅ Refresh complete for source: ${StreamSourceManager.currentSource}")
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // REAL-TIME SAFE: Skip initial load during playback
            if (!com.musifly.android.playback.PlaybackPriorityManager.shouldPauseBackgroundWork()) {
                load()
            } else {
                Log.d("HomeViewModel", "⏸️ Skipping initial load - playback priority mode active")
            }
        }
        
        // Register callback to handle source changes from StreamSourceManager
        // This ensures instant reload when source changes
        StreamSourceManager.registerSourceChangeCallback { newSource ->
            Log.d("HomeViewModel", "🔄 Source change callback received in HomeViewModel: ${newSource.name}")
            // REAL-TIME SAFE: Always reload on source change (user explicit action)
            // CRITICAL: Clear cache FIRST, then reload
            // This must happen immediately (no delay)
            clearHomeCache()
            loadHomeFromSource()
        }
    }
}