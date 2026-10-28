package com.musifly.android.recommendations

import android.util.Log
import com.musifly.android.db.DatabaseDao
import com.musifly.android.db.entities.EventWithSong
import com.musifly.android.db.entities.Song
import com.musifly.android.models.SaavnAlbum
import com.musifly.android.models.SaavnArtist
import com.musifly.android.models.SaavnHomeSection
import com.musifly.android.models.SaavnItem
import com.musifly.android.models.SaavnPlaylist
import com.musifly.android.models.SaavnSectionType
import com.musifly.android.models.SaavnSong
import com.musifly.android.models.toSaavnSong
import com.musifly.android.models.toSaavnAlbum
import com.musifly.android.models.toSaavnPlaylist
import com.musifly.android.models.toSaavnArtist
import com.musifly.android.streamingengines.saavn.SaavnAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * AI-Powered Recommendation Engine for JioSaavn
 * 
 * Generates:
 * - Daily Mixes (4 types: Artist, Mood, Language, Genre)
 * - Personalized Recommendations
 * - Listening Pattern Analysis
 */
class RecommendationEngine(
    private val database: DatabaseDao,
    private val saavnAPI: SaavnAPI
) {
    companion object {
        private const val TAG = "RecommendationEngine"
        private const val RECENT_SONGS_LIMIT = 50
        private const val DAILY_MIX_SONGS = 30
        private const val RECOMMENDATION_LIMIT = 40
    }

    /**
     * Listening pattern data class
     */
    data class ListeningPattern(
        val topArtists: List<Pair<String, Int>>, // artist name -> play count
        val topGenres: List<Pair<String, Int>>, // genre -> play count
        val topLanguages: List<Pair<String, Int>>, // language -> play count
        val moodDistribution: Map<String, Int>, // mood -> play count
        val timePreference: String, // "morning", "afternoon", "evening", "night"
        val recentSongs: List<Song>
    )

    /**
     * Analyze user's listening pattern from recent history
     */
    suspend fun analyzeListeningPattern(): ListeningPattern = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 Analyzing listening pattern...")
            
            // Get recent events (last 50 songs)
            val sevenDaysAgo = LocalDateTime.now().minusDays(7).toInstant(ZoneOffset.UTC).toEpochMilli()
            val recentEvents = database.events().first()
                .filter { 
                    it.event.timestamp.toInstant(ZoneOffset.UTC).toEpochMilli() >= sevenDaysAgo 
                }
                .take(RECENT_SONGS_LIMIT)
            
            Log.d(TAG, "📊 Found ${recentEvents.size} recent listening events")
            
            // If no recent history, return empty pattern (will use trending as fallback)
            if (recentEvents.isEmpty()) {
                Log.w(TAG, "⚠️ No recent listening history found - will use trending content")
                return@withContext ListeningPattern(emptyList(), emptyList(), emptyList(), emptyMap(), "night", emptyList())
            }
            
            // Extract artist frequency
            val artistFrequency = mutableMapOf<String, Int>()
            val genreFrequency = mutableMapOf<String, Int>()
            val languageFrequency = mutableMapOf<String, Int>()
            val moodFrequency = mutableMapOf<String, Int>()
            val hourDistribution = mutableMapOf<Int, Int>() // hour -> count
            
            recentEvents.forEach { eventWithSong ->
                val song = eventWithSong.song
                val timestamp = eventWithSong.event.timestamp
                
                // Count artists
                song.artists.forEach { artist ->
                    artistFrequency[artist.name] = artistFrequency.getOrDefault(artist.name, 0) + 1
                }
                
                // Extract genre from song metadata (if available)
                // For now, we'll infer from album or use default
                val genre = extractGenre(song)
                genreFrequency[genre] = genreFrequency.getOrDefault(genre, 0) + 1
                
                // Extract language (from album or infer from title)
                val language = extractLanguage(song)
                languageFrequency[language] = languageFrequency.getOrDefault(language, 0) + 1
                
                // Extract mood (infer from genre or metadata)
                val mood = extractMood(song, genre)
                moodFrequency[mood] = moodFrequency.getOrDefault(mood, 0) + 1
                
                // Track time preference
                val hour = timestamp.hour
                hourDistribution[hour] = hourDistribution.getOrDefault(hour, 0) + 1
            }
            
            // Determine time preference
            val timePreference = when {
                hourDistribution.getOrDefault(6, 0) + hourDistribution.getOrDefault(7, 0) + 
                hourDistribution.getOrDefault(8, 0) + hourDistribution.getOrDefault(9, 0) > 
                hourDistribution.values.maxOrNull() ?: 0 -> "morning"
                hourDistribution.getOrDefault(12, 0) + hourDistribution.getOrDefault(13, 0) + 
                hourDistribution.getOrDefault(14, 0) > 
                hourDistribution.values.maxOrNull() ?: 0 -> "afternoon"
                hourDistribution.getOrDefault(18, 0) + hourDistribution.getOrDefault(19, 0) + 
                hourDistribution.getOrDefault(20, 0) > 
                hourDistribution.values.maxOrNull() ?: 0 -> "evening"
                else -> "night"
            }
            
            val topArtists = artistFrequency.toList().sortedByDescending { it.second }.take(10)
            val topGenres = genreFrequency.toList().sortedByDescending { it.second }.take(10)
            val topLanguages = languageFrequency.toList().sortedByDescending { it.second }.take(5)
            
            Log.d(TAG, "✅ Analysis complete:")
            Log.d(TAG, "   Top Artists: ${topArtists.size}")
            Log.d(TAG, "   Top Genres: ${topGenres.size}")
            Log.d(TAG, "   Top Languages: ${topLanguages.size}")
            Log.d(TAG, "   Time Preference: $timePreference")
            
            ListeningPattern(
                topArtists = topArtists,
                topGenres = topGenres,
                topLanguages = topLanguages,
                moodDistribution = moodFrequency,
                timePreference = timePreference,
                recentSongs = recentEvents.map { it.song }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error analyzing listening pattern: ${e.message}", e)
            ListeningPattern(emptyList(), emptyList(), emptyList(), emptyMap(), "night", emptyList())
        }
    }

    /**
     * Generate Daily Mixes (4 types)
     */
    suspend fun generateDailyMixes(pattern: ListeningPattern): List<SaavnHomeSection> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🎵 Generating Daily Mixes...")
            val mixes = mutableListOf<SaavnHomeSection>()
            
            // If no listening pattern, generate default mixes from trending
            if (pattern.recentSongs.isEmpty()) {
                Log.d(TAG, "📊 No listening history - generating default mixes from trending")
                // Generate default mixes using trending content
                val trending = saavnAPI.getTrendingSongs(DAILY_MIX_SONGS)
                val trendingSongs = trending.mapNotNull { it.toSaavnSong() }.take(DAILY_MIX_SONGS)
                
                if (trendingSongs.isNotEmpty()) {
                    mixes.add(SaavnHomeSection(
                        title = "Daily Mix 1",
                        type = SaavnSectionType.MIX,
                        items = trendingSongs
                    ))
                    mixes.add(SaavnHomeSection(
                        title = "Daily Mix 2",
                        type = SaavnSectionType.MIX,
                        items = trendingSongs.take(DAILY_MIX_SONGS)
                    ))
                }
                return@withContext mixes
            }
            
            // Daily Mix 1: Artist-based
            if (pattern.topArtists.isNotEmpty()) {
                val artistMix = generateArtistBasedMix(pattern)
                artistMix?.let { mixes.add(it) }
            }
            
            // Daily Mix 2: Mood-based
            if (pattern.moodDistribution.isNotEmpty()) {
                val moodMix = generateMoodBasedMix(pattern)
                moodMix?.let { mixes.add(it) }
            }
            
            // Daily Mix 3: Language-based
            if (pattern.topLanguages.isNotEmpty()) {
                val languageMix = generateLanguageBasedMix(pattern)
                languageMix?.let { mixes.add(it) }
            }
            
            // Daily Mix 4: Genre-based
            if (pattern.topGenres.isNotEmpty()) {
                val genreMix = generateGenreBasedMix(pattern)
                genreMix?.let { mixes.add(it) }
            }
            
            Log.d(TAG, "✅ Generated ${mixes.size} Daily Mixes")
            mixes
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating daily mixes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Generate AI-powered personalized recommendations
     */
    suspend fun generateAIRecommendations(pattern: ListeningPattern): List<SaavnHomeSection> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🤖 Generating AI recommendations...")
            val sections = mutableListOf<SaavnHomeSection>()
            
            // Section 1: "Because you listened to [Top Artist]"
            if (pattern.topArtists.isNotEmpty()) {
                val topArtist = pattern.topArtists.first().first
                val artistSection = generateArtistRecommendationSection(topArtist)
                artistSection?.let { sections.add(it) }
            }
            
            // Section 2: Language-based picks
            if (pattern.topLanguages.isNotEmpty()) {
                val topLanguage = pattern.topLanguages.first().first
                val languageSection = generateLanguageRecommendationSection(topLanguage)
                languageSection?.let { sections.add(it) }
            }
            
            // Section 3: Time-based recommendations
            val timeSection = generateTimeBasedRecommendations(pattern.timePreference)
            timeSection?.let { sections.add(it) }
            
            // Section 4: Genre-based recommendations
            if (pattern.topGenres.isNotEmpty()) {
                val topGenre = pattern.topGenres.first().first
                val genreSection = generateGenreRecommendationSection(topGenre)
                genreSection?.let { sections.add(it) }
            }
            
            // Section 5: "Recommended For You (AI)"
            val generalRecommendations = generateGeneralRecommendations(pattern)
            generalRecommendations?.let { sections.add(it) }
            
            Log.d(TAG, "✅ Generated ${sections.size} AI recommendation sections")
            sections
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating AI recommendations: ${e.message}", e)
            emptyList()
        }
    }

    // ============ DAILY MIX GENERATORS ============

    private suspend fun generateArtistBasedMix(pattern: ListeningPattern): SaavnHomeSection? {
        try {
            val topArtists = pattern.topArtists.take(5).map { it.first }
            val artistNames = topArtists.joinToString(", ")
            
            Log.d(TAG, "🎤 Generating Artist Mix for: $artistNames")
            
            // Search for songs by top artists
            val allSongs = mutableListOf<SaavnSong>()
            for (artist in topArtists.take(3)) { // Limit to 3 artists to avoid too many API calls
                try {
                    val searchResults = saavnAPI.fetchSearchResults("$artist songs", count = 10)
                    val resultsList = searchResults["songs"] as? List<Map<String, Any>> ?: emptyList()
                    val songs = resultsList
                        .mapNotNull { item ->
                            try {
                                item.toSaavnSong()
                            } catch (e: Exception) {
                                Log.w(TAG, "⚠️ Error converting song: ${e.message}")
                                null
                            }
                        }
                    allSongs.addAll(songs)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error searching for artist $artist: ${e.message}")
                }
            }
            
            if (allSongs.isEmpty()) {
                // Fallback: Use trending songs
                val trending = saavnAPI.getTrendingSongs(DAILY_MIX_SONGS)
                allSongs.addAll(trending.mapNotNull { it.toSaavnSong() })
            }
            
            if (allSongs.isEmpty()) return null
            
            // Get top artist images for cover
            val coverImage = getTopArtistImage(topArtists.firstOrNull() ?: "")
            
            return SaavnHomeSection(
                title = "Daily Mix 1",
                type = SaavnSectionType.MIX,
                items = allSongs.take(DAILY_MIX_SONGS)
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating artist mix: ${e.message}", e)
            return null
        }
    }

    private suspend fun generateMoodBasedMix(pattern: ListeningPattern): SaavnHomeSection? {
        try {
            val topMood = pattern.moodDistribution.toList()
                .sortedByDescending { it.second }
                .firstOrNull()?.first ?: "happy"
            
            Log.d(TAG, "😊 Generating Mood Mix for: $topMood")
            
            // Search for mood-based playlists or songs
            val moodQueries = mapOf(
                "happy" to listOf("happy songs", "upbeat music", "feel good"),
                "sad" to listOf("sad songs", "emotional music", "melancholic"),
                "energetic" to listOf("workout music", "energetic songs", "pump up"),
                "calm" to listOf("calm music", "relaxing songs", "peaceful")
            )
            
            val queries = moodQueries[topMood.lowercase()] ?: listOf("trending songs")
            val allSongs = mutableListOf<SaavnSong>()
            
            for (query in queries.take(2)) {
                try {
                    val results = saavnAPI.fetchSearchResults(query, count = 15)
                    val resultsList = results["songs"] as? List<Map<String, Any>> ?: emptyList()
                    val songs = resultsList
                        .mapNotNull { it.toSaavnSong() }
                    allSongs.addAll(songs)
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Error searching mood query: ${e.message}")
                }
            }
            
            if (allSongs.isEmpty()) {
                val trending = saavnAPI.getTrendingSongs(DAILY_MIX_SONGS)
                allSongs.addAll(trending.mapNotNull { it.toSaavnSong() })
            }
            
            if (allSongs.isEmpty()) return null
            
            val moodTitle = topMood.replaceFirstChar { it.uppercaseChar() }
            return SaavnHomeSection(
                title = "Daily Mix 2 - $moodTitle",
                type = SaavnSectionType.MIX,
                items = allSongs.take(DAILY_MIX_SONGS)
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating mood mix: ${e.message}", e)
            return null
        }
    }

    private suspend fun generateLanguageBasedMix(pattern: ListeningPattern): SaavnHomeSection? {
        try {
            val topLanguage = pattern.topLanguages.firstOrNull()?.first ?: "Hindi"
            
            Log.d(TAG, "🌍 Generating Language Mix for: $topLanguage")
            
            // Search for language-specific songs
            val query = "$topLanguage songs"
            val results = saavnAPI.fetchSearchResults(query, count = DAILY_MIX_SONGS)
            val resultsList = results["songs"] as? List<Map<String, Any>> ?: emptyList()
            val songs = resultsList
                .mapNotNull { it.toSaavnSong() }
            
            if (songs.isEmpty()) {
                val trending = saavnAPI.getTrendingSongs(DAILY_MIX_SONGS)
                return SaavnHomeSection(
                    title = "Daily Mix 3 - $topLanguage",
                    type = SaavnSectionType.MIX,
                    items = trending.mapNotNull { it.toSaavnSong() }.take(DAILY_MIX_SONGS)
                )
            }
            
            return SaavnHomeSection(
                title = "Daily Mix 3 - $topLanguage",
                type = SaavnSectionType.MIX,
                items = songs.take(DAILY_MIX_SONGS)
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating language mix: ${e.message}", e)
            return null
        }
    }

    private suspend fun generateGenreBasedMix(pattern: ListeningPattern): SaavnHomeSection? {
        try {
            val topGenre = pattern.topGenres.firstOrNull()?.first ?: "Romantic"
            
            Log.d(TAG, "🎸 Generating Genre Mix for: $topGenre")
            
            // Search for genre-specific content
            val query = "$topGenre songs"
            val results = saavnAPI.fetchSearchResults(query, count = DAILY_MIX_SONGS)
            val resultsList = results["songs"] as? List<Map<String, Any>> ?: emptyList()
            val songs = resultsList
                .mapNotNull { it.toSaavnSong() }
            
            if (songs.isEmpty()) {
                val trending = saavnAPI.getTrendingSongs(DAILY_MIX_SONGS)
                return SaavnHomeSection(
                    title = "Daily Mix 4 - $topGenre",
                    type = SaavnSectionType.MIX,
                    items = trending.mapNotNull { it.toSaavnSong() }.take(DAILY_MIX_SONGS)
                )
            }
            
            return SaavnHomeSection(
                title = "Daily Mix 4 - $topGenre",
                type = SaavnSectionType.MIX,
                items = songs.take(DAILY_MIX_SONGS)
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating genre mix: ${e.message}", e)
            return null
        }
    }

    // ============ AI RECOMMENDATION GENERATORS ============

    private suspend fun generateArtistRecommendationSection(artistName: String): SaavnHomeSection? {
        try {
            Log.d(TAG, "🎤 Generating recommendations for artist: $artistName")
            
            // Search for artist's songs and similar artists
            val results = saavnAPI.fetchSearchResults(artistName, count = 20)
            val resultsList = results["songs"] as? List<Map<String, Any>> ?: emptyList()
            val songs = resultsList
                .mapNotNull { it.toSaavnSong() }
                .take(20)
            
            if (songs.isEmpty()) return null
            
            return SaavnHomeSection(
                title = "Because you listened to $artistName",
                type = SaavnSectionType.TRENDING,
                items = songs
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating artist recommendations: ${e.message}", e)
            return null
        }
    }

    private suspend fun generateLanguageRecommendationSection(language: String): SaavnHomeSection? {
        try {
            val results = saavnAPI.fetchSearchResults("$language songs", count = 20)
            val resultsList = results["songs"] as? List<Map<String, Any>> ?: emptyList()
            val songs = resultsList
                .mapNotNull { it.toSaavnSong() }
                .take(20)
            
            if (songs.isEmpty()) return null
            
            return SaavnHomeSection(
                title = "Your $language Picks",
                type = SaavnSectionType.TRENDING,
                items = songs
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating language recommendations: ${e.message}", e)
            return null
        }
    }

    private suspend fun generateTimeBasedRecommendations(timePreference: String): SaavnHomeSection? {
        try {
            val queries = mapOf(
                "morning" to "morning songs",
                "afternoon" to "afternoon music",
                "evening" to "evening songs",
                "night" to "night music"
            )
            
            val query = queries[timePreference] ?: "trending songs"
            val results = saavnAPI.fetchSearchResults(query, count = 20)
            val resultsList = results["songs"] as? List<Map<String, Any>> ?: emptyList()
            val songs = resultsList
                .mapNotNull { it.toSaavnSong() }
                .take(20)
            
            if (songs.isEmpty()) {
                val trending = saavnAPI.getTrendingSongs(20)
            val timeTitle = timePreference.replaceFirstChar { it.uppercaseChar() }
            return SaavnHomeSection(
                title = "Your $timeTitle Vibes",
                type = SaavnSectionType.TRENDING,
                items = trending.mapNotNull { it.toSaavnSong() }.take(20)
            )
            }
            
            val timeTitle = timePreference.replaceFirstChar { it.uppercaseChar() }
            return SaavnHomeSection(
                title = "Your $timeTitle Vibes",
                type = SaavnSectionType.TRENDING,
                items = songs
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating time-based recommendations: ${e.message}", e)
            return null
        }
    }

    private suspend fun generateGenreRecommendationSection(genre: String): SaavnHomeSection? {
        try {
            val results = saavnAPI.fetchSearchResults("$genre songs", count = 20)
            val resultsList = results["songs"] as? List<Map<String, Any>> ?: emptyList()
            val songs = resultsList
                .mapNotNull { it.toSaavnSong() }
                .take(20)
            
            if (songs.isEmpty()) return null
            
            return SaavnHomeSection(
                title = "Your $genre Mix",
                type = SaavnSectionType.MIX,
                items = songs
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating genre recommendations: ${e.message}", e)
            return null
        }
    }

    private suspend fun generateGeneralRecommendations(pattern: ListeningPattern): SaavnHomeSection? {
        try {
            // Combine multiple factors for general recommendations
            val trending = saavnAPI.getTrendingSongs(RECOMMENDATION_LIMIT)
            val songs = trending.mapNotNull { it.toSaavnSong() }.take(20)
            
            if (songs.isEmpty()) return null
            
            return SaavnHomeSection(
                title = "Recommended For You (AI)",
                type = SaavnSectionType.TRENDING,
                items = songs
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error generating general recommendations: ${e.message}", e)
            return null
        }
    }

    // ============ HELPER FUNCTIONS ============

    private fun extractGenre(song: Song): String {
        // Try to extract genre from album or song metadata
        // For now, use common genres based on keywords
        val title = song.title.lowercase()
        val albumName = song.album?.title?.lowercase() ?: ""
        
        return when {
            title.contains("romantic") || albumName.contains("romantic") -> "Romantic"
            title.contains("party") || albumName.contains("party") -> "Party"
            title.contains("workout") || albumName.contains("workout") -> "Workout"
            title.contains("lofi") || albumName.contains("lofi") -> "Lofi"
            title.contains("sad") || albumName.contains("sad") -> "Sad"
            title.contains("happy") || albumName.contains("happy") -> "Happy"
            else -> "Pop" // Default
        }
    }

    private fun extractLanguage(song: Song): String {
        // Infer language from title or metadata
        val title = song.title
        
        return when {
            title.matches(Regex(".*[\\u0900-\\u097F].*")) -> "Hindi" // Devanagari script
            title.matches(Regex(".*[\\u0A00-\\u0A7F].*")) -> "Punjabi" // Gurmukhi script
            title.matches(Regex(".*[\\u0980-\\u09FF].*")) -> "Bengali"
            title.matches(Regex(".*[\\u0C80-\\u0CFF].*")) -> "Kannada"
            title.matches(Regex(".*[\\u0D00-\\u0D7F].*")) -> "Malayalam"
            title.matches(Regex(".*[\\u0B80-\\u0BFF].*")) -> "Tamil"
            title.matches(Regex(".*[\\u0C00-\\u0C7F].*")) -> "Telugu"
            else -> "English"
        }
    }

    private fun extractMood(song: Song, genre: String): String {
        // Infer mood from genre or song characteristics
        val title = song.title.lowercase()
        
        return when {
            genre == "Happy" || title.contains("happy") || title.contains("celebration") -> "happy"
            genre == "Sad" || title.contains("sad") || title.contains("emotional") -> "sad"
            genre == "Workout" || title.contains("pump") || title.contains("energy") -> "energetic"
            genre == "Lofi" || title.contains("calm") || title.contains("peace") -> "calm"
            else -> "happy" // Default
        }
    }

    private suspend fun getTopArtistImage(artistName: String): String {
        return try {
            val results = saavnAPI.fetchSearchResults(artistName, count = 1)
            val resultsList = results["songs"] as? List<Map<String, Any>> ?: emptyList()
            resultsList.firstOrNull()?.get("image") as? String ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
