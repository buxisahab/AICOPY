package com.musifly.android.utils

import android.util.Log
import com.musifly.android.db.MusicDatabase
import com.musifly.android.db.entities.Album
import com.musifly.android.db.entities.Artist
import com.musifly.android.db.entities.Playlist
import com.musifly.android.db.entities.Song
import com.musifly.android.models.ListeningPattern
import com.musifly.android.models.MoodType
import com.musifly.android.models.RecommendationItem
import com.musifly.android.models.RecommendationSection
import com.musifly.android.models.RecommendationSectionType
import com.musifly.android.models.*
import com.musifly.android.streamingengines.saavn.SaavnAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Smart recommendation engine that analyzes listening history
 * and generates personalized recommendations
 */
object RecommendationEngine {
    private const val TAG = "RecommendationEngine"
    
    /**
     * Analyze listening history and extract patterns
     */
    suspend fun analyzeListeningPattern(
        database: MusicDatabase,
        days: Int = 30
    ): ListeningPattern = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val fromTimestamp = now - (days * 24 * 60 * 60 * 1000L)
        
        // Get most played artists
        val topArtists = database.mostPlayedArtists(fromTimestamp, limit = 20)
            .first()
            .map { artist -> artist to (artist.songCount) }
        
        // Get most played songs
        // Use mostPlayedSongsStats to get play time, then convert to Song
        val topSongsStats = database.mostPlayedSongsStats(fromTimestamp, limit = 50)
            .first()
        
        val topSongs = topSongsStats.mapNotNull { songWithStats ->
            database.song(songWithStats.id).first()?.let { song ->
                song to (songWithStats.timeListened ?: 0L)
            }
        }
        
        // Get recent songs (last 10)
        val recentSongs = database.events()
            .first()
            .take(10)
            .mapNotNull { it.song }
            .distinctBy { it.id }
        
        // Analyze genre patterns (simplified - can be enhanced)
        val genrePattern = analyzeGenrePattern(topSongs.map { it.first })
        
        // Determine dominant mood
        val dominantMood = determineMood(topSongs.map { it.first }, genrePattern)
        
        // Analyze tempo pattern
        val tempoPattern = analyzeTempoPattern(topSongs.map { it.first })
        
        Log.d(TAG, "📊 Listening pattern analyzed: ${topArtists.size} artists, ${topSongs.size} songs, mood: $dominantMood")
        
        ListeningPattern(
            topArtists = topArtists,
            topGenres = genrePattern,
            topSongs = topSongs,
            recentSongs = recentSongs,
            dominantMood = dominantMood,
            tempoPattern = tempoPattern
        )
    }
    
    /**
     * Generate Daily Mix based on listening history
     */
    suspend fun generateDailyMix(
        database: MusicDatabase,
        pattern: ListeningPattern,
        limit: Int = 20
    ): List<RecommendationItem> = withContext(Dispatchers.IO) {
        val mixSongs = mutableListOf<Song>()
        
        // Mix of top artists, recent songs, and similar songs
        val topArtistSongs = pattern.topArtists
            .take(3)
            .flatMap { (artist, _) ->
                try {
                    database.artistSongs(artist.id, com.musifly.android.constants.ArtistSongSortType.PLAY_TIME, descending = true)
                        .first()
                        .take(5)
                } catch (e: Exception) {
                    Log.w(TAG, "Error loading songs for artist ${artist.id}", e)
                    emptyList()
                }
            }
            .shuffled()
            .take(7)
        
        val recentSongs = pattern.recentSongs.take(5)
        val topSongs = pattern.topSongs.map { it.first }.take(8)
        
        mixSongs.addAll(topArtistSongs)
        mixSongs.addAll(recentSongs)
        mixSongs.addAll(topSongs)
        
        mixSongs
            .distinctBy { it.id }
            .shuffled()
            .take(limit)
            .map { it.toRecommendationItem() }
    }
    
    /**
     * Generate "Because You Like" recommendations
     */
    suspend fun generateBecauseYouLike(
        database: MusicDatabase,
        pattern: ListeningPattern,
        limit: Int = 15
    ): List<Pair<String, List<RecommendationItem>>> = withContext(Dispatchers.IO) {
        val recommendations = mutableListOf<Pair<String, List<RecommendationItem>>>()
        
        // Top artist recommendations
        pattern.topArtists.take(2).forEach { (artist, _) ->
            try {
                val similarSongs = database.artistSongs(artist.id, com.musifly.android.constants.ArtistSongSortType.PLAY_TIME, descending = true)
                    .first()
                    .shuffled()
                    .take(10)
                    .map { it.toRecommendationItem() }
                
                if (similarSongs.isNotEmpty()) {
                    recommendations.add("Because You Like ${artist.title}" to similarSongs)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error loading recommendations for artist ${artist.id}", e)
            }
        }
        
        recommendations
    }
    
    /**
     * Generate mood-based playlists
     */
    suspend fun generateMoodMix(
        database: MusicDatabase,
        mood: MoodType,
        limit: Int = 15
    ): List<RecommendationItem> = withContext(Dispatchers.IO) {
        // This is a simplified implementation
        // In a real app, you'd query songs by genre/tempo matching the mood
        val allSongs = database.allSongs().first()
        
        // Filter and shuffle based on mood (simplified logic)
        allSongs
            .shuffled()
            .take(limit)
            .map { it.toRecommendationItem() }
    }
    
    /**
     * Get recently played songs
     */
    suspend fun getRecentlyPlayed(
        database: MusicDatabase,
        limit: Int = 10
    ): List<RecommendationItem> = withContext(Dispatchers.IO) {
        database.events()
            .first()
            .take(limit * 2) // Take more to account for duplicates
            .mapNotNull { it.song }
            .distinctBy { it.id }
            .take(limit)
            .map { it.toRecommendationItem() }
    }
    
    /**
     * Get trending content from JioSaavn
     */
    suspend fun getTrendingContent(
        limit: Int = 20
    ): List<RecommendationItem> = withContext(Dispatchers.IO) {
        try {
            val saavnAPI = SaavnAPI()
            val trending = saavnAPI.getTopCharts(limit)
            trending
                .map { chart ->
                    val id = chart["id"] as? String ?: ""
                    val title = chart["title"] as? String ?: "Unknown"
                    val image = chart["image"] as? String ?: ""
                    val subtitle = chart["subtitle"] as? String ?: (chart["artist"] as? String ?: "")
                    val type = chart["type"] as? String ?: "album"
                    
                    RecommendationItem.PlaylistItem(
                        id = "saavn:$id",
                        title = title,
                        thumbnailUrl = image,
                        subtitle = subtitle,
                        type = type
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching trending content", e)
            emptyList()
        }
    }
    
    /**
     * Get recommended playlists from JioSaavn
     */
    suspend fun getRecommendedPlaylists(
        limit: Int = 15
    ): List<RecommendationItem> = withContext(Dispatchers.IO) {
        try {
            val saavnAPI = SaavnAPI()
            val playlists = saavnAPI.getFeaturedPlaylists(limit)
            playlists
                .map { playlist ->
                    val id = playlist["id"] as? String ?: ""
                    val title = playlist["title"] as? String ?: "Unknown"
                    val image = playlist["image"] as? String ?: ""
                    val subtitle = playlist["subtitle"] as? String ?: "Playlist"
                    
                    RecommendationItem.PlaylistItem(
                        id = "saavn:$id",
                        title = title,
                        thumbnailUrl = image,
                        subtitle = subtitle,
                        type = "playlist"
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching recommended playlists", e)
            emptyList()
        }
    }
    
    /**
     * Get recommended artists based on listening history
     */
    suspend fun getRecommendedArtists(
        database: MusicDatabase,
        pattern: ListeningPattern,
        limit: Int = 10
    ): List<RecommendationItem> = withContext(Dispatchers.IO) {
        pattern.topArtists
            .take(limit)
            .map { (artist, _) ->
                artist.toRecommendationItem()
            }
    }
    
    /**
     * Generate all recommendation sections
     */
    suspend fun generateAllSections(
        database: MusicDatabase
    ): List<RecommendationSection> = withContext(Dispatchers.IO) {
        val pattern = analyzeListeningPattern(database)
        val sections = mutableListOf<RecommendationSection>()
        
        // Daily Mix
        val dailyMix = generateDailyMix(database, pattern)
        if (dailyMix.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = RecommendationSectionType.DAILY_MIX,
                    title = "🎧 Your Daily Mix",
                    items = dailyMix,
                    shuffleEnabled = true
                )
            )
        }
        
        // Trending
        val trending = getTrendingContent()
        if (trending.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = RecommendationSectionType.TRENDING,
                    title = "🔥 Trending in India",
                    items = trending
                )
            )
        }
        
        // Because You Like
        val becauseYouLike = generateBecauseYouLike(database, pattern)
        becauseYouLike.forEach { (title, items) ->
            if (items.isNotEmpty()) {
                sections.add(
                    RecommendationSection(
                        type = RecommendationSectionType.BECAUSE_YOU_LIKE,
                        title = "💚 $title",
                        items = items
                    )
                )
            }
        }
        
        // Recently Played
        val recentlyPlayed = getRecentlyPlayed(database)
        if (recentlyPlayed.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = RecommendationSectionType.RECENTLY_PLAYED,
                    title = "⏱ Recently Played",
                    items = recentlyPlayed
                )
            )
        }
        
        // Mood Mix
        pattern.dominantMood?.let { mood ->
            val moodMix = generateMoodMix(database, mood)
            if (moodMix.isNotEmpty()) {
                val moodTitle = when (mood) {
                    MoodType.CHILL -> "😌 Chill Mix"
                    MoodType.ROMANTIC -> "💕 Romantic Mix"
                    MoodType.WORKOUT -> "💪 Workout Mix"
                    MoodType.PARTY -> "🎉 Party Mix"
                    MoodType.FOCUS -> "🎯 Focus Mix"
                    MoodType.SLEEP -> "🌙 Sleep Mix"
                }
                sections.add(
                    RecommendationSection(
                        type = RecommendationSectionType.MOOD_MIX,
                        title = moodTitle,
                        items = moodMix
                    )
                )
            }
        }
        
        // Recommended Playlists
        val recommendedPlaylists = getRecommendedPlaylists()
        if (recommendedPlaylists.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = RecommendationSectionType.RECOMMENDED_PLAYLISTS,
                    title = "📋 Recommended Playlists",
                    items = recommendedPlaylists
                )
            )
        }
        
        // Recommended Artists
        val recommendedArtists = getRecommendedArtists(database, pattern)
        if (recommendedArtists.isNotEmpty()) {
            sections.add(
                RecommendationSection(
                    type = RecommendationSectionType.RECOMMENDED_ARTISTS,
                    title = "🎤 Recommended Artists",
                    items = recommendedArtists
                )
            )
        }
        
        Log.d(TAG, "✅ Generated ${sections.size} recommendation sections")
        sections
    }
    
    // Helper functions
    
    private fun analyzeGenrePattern(songs: List<Song>): List<Pair<String, Int>> {
        // Simplified genre analysis - in real app, extract from metadata
        val genreCounts = mutableMapOf<String, Int>()
        songs.forEach { song ->
            // Extract genre from album name or use default
            val albumName = song.album?.title ?: song.song.albumName
            val genre = albumName?.let { extractGenreFromAlbum(it) } ?: "Unknown"
            genreCounts[genre] = (genreCounts[genre] ?: 0) + 1
        }
        return genreCounts.toList().sortedByDescending { it.second }
    }
    
    private fun extractGenreFromAlbum(albumName: String): String {
        // Simple keyword matching - can be enhanced
        val lower = albumName.lowercase()
        return when {
            lower.contains("romantic") || lower.contains("love") -> "Romantic"
            lower.contains("workout") || lower.contains("gym") -> "Workout"
            lower.contains("chill") || lower.contains("relax") -> "Chill"
            lower.contains("party") || lower.contains("dance") -> "Party"
            else -> "Pop"
        }
    }
    
    private fun determineMood(songs: List<Song>, genres: List<Pair<String, Int>>): MoodType? {
        if (genres.isEmpty()) return null
        
        val topGenre = genres.first().first.lowercase()
        return when {
            topGenre.contains("romantic") || topGenre.contains("love") -> MoodType.ROMANTIC
            topGenre.contains("workout") || topGenre.contains("gym") -> MoodType.WORKOUT
            topGenre.contains("chill") || topGenre.contains("relax") -> MoodType.CHILL
            topGenre.contains("party") || topGenre.contains("dance") -> MoodType.PARTY
            else -> null
        }
    }
    
    private fun analyzeTempoPattern(songs: List<Song>): String {
        // Simplified - in real app, analyze actual tempo from audio metadata
        return "mixed"
    }
}
