package com.musifly.android.lyrics

import android.content.Context
import android.util.LruCache
import com.musifly.android.constants.PreferredLyricsProvider
import com.musifly.android.constants.PreferredLyricsProviderKey
import com.musifly.android.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.musifly.android.extensions.toEnum
import com.musifly.android.models.MediaMetadata
import com.musifly.android.utils.dataStore
import com.musifly.android.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private var lyricsProviders =
        listOf(
            LrcLibLyricsProvider,
            KuGouLyricsProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider
        )
    val preferred =
        context.dataStore.data
            .map {
                it[PreferredLyricsProviderKey].toEnum(PreferredLyricsProvider.LRCLIB)
            }.distinctUntilChanged()
            .map {
                lyricsProviders =
                    if (it == PreferredLyricsProvider.LRCLIB) {
                        listOf(
                            LrcLibLyricsProvider,
                            KuGouLyricsProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                    } else {
                        listOf(
                            KuGouLyricsProvider,
                            LrcLibLyricsProvider,
                            YouTubeSubtitleLyricsProvider,
                            YouTubeLyricsProvider
                        )
                    }
            }
    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    // Separate cache for successful lyrics to avoid wrong lyrics being served
    private val successCache = LruCache<String, String>(MAX_CACHE_SIZE)

    suspend fun getLyrics(mediaMetadata: MediaMetadata): String {
        // Check success cache first (contains verified good lyrics)
        val cachedSuccess = successCache.get(mediaMetadata.id)
        if (cachedSuccess != null && cachedSuccess != LYRICS_NOT_FOUND) {
            return cachedSuccess
        }
        
        // Check general cache
        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null && cached.lyrics != LYRICS_NOT_FOUND) {
            // Validate cached lyrics
            val validation = LyricsValidator.validateLyrics(cached.lyrics, mediaMetadata, cached.providerName)
            if (validation.isValid) {
                return cached.lyrics
            }
        }
        
        // Collect results from all providers
        val allResults = mutableListOf<Pair<String, String>>()
        var lastError: Throwable? = null
        
        lyricsProviders.forEach { provider ->
            if (provider.isEnabled(context)) {
                try {
                    provider
                        .getLyrics(
                            mediaMetadata.id,
                            mediaMetadata.title,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.duration,
                        ).onSuccess { lyrics ->
                            // Basic validation
                            if (lyrics.isNotBlank() && 
                                lyrics != LYRICS_NOT_FOUND &&
                                !lyrics.contains("Error", ignoreCase = true) &&
                                lyrics.length > 10) {
                                
                                // Add to results for AI validation
                                allResults.add(Pair(provider.name, lyrics))
                            }
                        }.onFailure {
                            lastError = it
                            reportException(it)
                        }
                } catch (e: Exception) {
                    lastError = e
                    reportException(e)
                    // Continue to next provider
                }
            }
        }
        
        // Use AI validator to pick the best lyrics
        if (allResults.isNotEmpty()) {
            val bestLyrics = LyricsValidator.pickBestLyrics(allResults, mediaMetadata)
            if (bestLyrics != null) {
                // Cache the best result
                successCache.put(mediaMetadata.id, bestLyrics)
                val providerName = allResults.first { it.second == bestLyrics }.first
                val result = LyricsResult(providerName, bestLyrics)
                cache.put(mediaMetadata.id, listOf(result))
                return bestLyrics
            }
        }
        
        // If all providers failed, log the last error
        lastError?.let { reportException(it) }
        
        return LYRICS_NOT_FOUND
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        callback: (LyricsResult) -> Unit,
    ) {
        val cacheKey = "$songArtists-$songTitle".replace(" ", "").lowercase()
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }
        val allResult = mutableListOf<LyricsResult>()
        lyricsProviders.forEach { provider ->
            if (provider.isEnabled(context)) {
                try {
                    provider.getAllLyrics(mediaId, songTitle, songArtists, duration) { lyrics ->
                        // Validate lyrics before adding to results
                        if (lyrics.isNotBlank() && 
                            lyrics != LYRICS_NOT_FOUND &&
                            lyrics.length > 10) {
                            val result = LyricsResult(provider.name, lyrics)
                            allResult += result
                            callback(result)
                        }
                    }
                } catch (e: Exception) {
                    reportException(e)
                    // Continue to next provider
                }
            }
        }
        
        // Only cache if we got results
        if (allResult.isNotEmpty()) {
            cache.put(cacheKey, allResult)
        }
    }

    companion object {
        private const val MAX_CACHE_SIZE = 50 // Increased from 3 to 50 for better caching
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
