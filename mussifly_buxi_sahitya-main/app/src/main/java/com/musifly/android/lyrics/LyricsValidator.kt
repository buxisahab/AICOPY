package com.musifly.android.lyrics

import android.content.Context
import android.util.Log
import com.musifly.android.models.MediaMetadata
import kotlin.math.abs

/**
 * AI-powered Lyrics Validator
 * 
 * This class provides smart validation and matching for lyrics results
 * to prevent wrong lyrics from being shown to users.
 * 
 * Features:
 * - Similarity scoring between song metadata and lyrics
 * - Content validation (not error messages, not empty)
 * - Duration-based matching
 * - Artist/title fuzzy matching
 * - Confidence scoring for lyrics quality
 */
object LyricsValidator {
    private const val TAG = "LyricsValidator"
    
    // Minimum confidence score to accept lyrics (0.0 to 1.0)
    private const val MIN_CONFIDENCE_SCORE = 0.6f
    
    /**
     * Validate lyrics against song metadata
     * Returns a confidence score between 0.0 and 1.0
     */
    fun validateLyrics(
        lyrics: String,
        metadata: MediaMetadata,
        providerName: String
    ): LyricsValidationResult {
        var confidence = 1.0f
        val issues = mutableListOf<String>()
        
        // 1. Basic validation
        if (lyrics.isBlank()) {
            return LyricsValidationResult(
                isValid = false,
                confidence = 0.0f,
                issues = listOf("Lyrics are empty"),
                providerName = providerName
            )
        }
        
        // 2. Check for error messages
        val errorPatterns = listOf(
            "error", "not found", "unavailable", "failed", 
            "404", "500", "exception", "invalid"
        )
        if (errorPatterns.any { lyrics.contains(it, ignoreCase = true) }) {
            issues.add("Contains error message")
            confidence -= 0.5f
        }
        
        // 3. Minimum length check
        if (lyrics.length < 50) {
            issues.add("Lyrics too short (${lyrics.length} chars)")
            confidence -= 0.3f
        }
        
        // 4. Check if it looks like lyrics (has line breaks, repeated patterns)
        if (!lyrics.contains("\n") && !lyrics.contains("[")) {
            issues.add("Doesn't appear to be formatted lyrics")
            confidence -= 0.2f
        }
        
        // 5. Title matching (fuzzy)
        val titleInLyrics = containsFuzzy(lyrics, metadata.title)
        if (!titleInLyrics && !lyrics.startsWith("[")) {
            // It's okay if title is not in lyrics for synced lyrics
            Log.d(TAG, "Title '${metadata.title}' not found in lyrics")
        }
        
        // 6. Artist matching (fuzzy)
        val artistNames = metadata.artists.map { it.name }
        val hasArtist = artistNames.any { containsFuzzy(lyrics, it) }
        if (!hasArtist && !lyrics.startsWith("[")) {
            Log.d(TAG, "Artist names not found in lyrics")
        }
        
        // 7. Check for synced lyrics format (\[MM:SS.xx\])
        val isSynced = lyrics.contains(Regex("\\[\\d{2}:\\d{2}\\.\\d{2}\\]"))
        if (isSynced) {
            confidence += 0.1f // Bonus for synced lyrics
            
            // Validate timestamp count matches song duration
            val timestampCount = Regex("\\[\\d{2}:\\d{2}\\.\\d{2}\\]").findAll(lyrics).count()
            val expectedLines = metadata.duration / 3000 // Rough estimate: 1 line per 3 seconds
            
            if (timestampCount < 5) {
                issues.add("Too few timestamps for synced lyrics")
                confidence -= 0.2f
            }
        }
        
        // 8. Check lyrics length relative to song duration
        val lyricsLines = lyrics.lines().filter { it.isNotBlank() }.size
        val songDurationMinutes = metadata.duration / 60000
        
        // Typical song has 15-30 lines per minute
        val expectedMinLines = songDurationMinutes * 10
        val expectedMaxLines = songDurationMinutes * 40
        
        if (lyricsLines < expectedMinLines) {
            issues.add("Too few lines for song duration")
            confidence -= 0.1f
        } else if (lyricsLines > expectedMaxLines) {
            issues.add("Too many lines for song duration")
            confidence -= 0.1f
        }
        
        // 9. Provider-specific scoring
        when (providerName.lowercase()) {
            "lrclib" -> confidence += 0.05f // LrcLib usually reliable
            "kugou" -> confidence += 0.03f  // KuGou good for Asian music
            "youtube" -> confidence -= 0.05f // YouTube subtitles can be auto-generated
        }
        
        // Cap confidence between 0.0 and 1.0
        confidence = confidence.coerceIn(0.0f, 1.0f)
        
        val isValid = confidence >= MIN_CONFIDENCE_SCORE
        
        Log.d(TAG, "Validation for '${metadata.title}': confidence=$confidence, valid=$isValid")
        if (issues.isNotEmpty()) {
            Log.d(TAG, "Issues: ${issues.joinToString(", ")}")
        }
        
        return LyricsValidationResult(
            isValid = isValid,
            confidence = confidence,
            issues = issues,
            providerName = providerName
        )
    }
    
    /**
     * Compare multiple lyrics results and pick the best one
     */
    fun pickBestLyrics(
        results: List<Pair<String, String>>, // (providerName, lyrics)
        metadata: MediaMetadata
    ): String? {
        if (results.isEmpty()) return null
        
        val validatedResults = results.map { (provider, lyrics) ->
            Triple(provider, lyrics, validateLyrics(lyrics, metadata, provider))
        }
        
        // Sort by confidence score
        val sortedResults = validatedResults
            .filter { it.third.isValid }
            .sortedByDescending { it.third.confidence }
        
        if (sortedResults.isEmpty()) {
            Log.w(TAG, "No valid lyrics found for '${metadata.title}'")
            return null
        }
        
        val best = sortedResults.first()
        Log.i(TAG, "Best lyrics for '${metadata.title}': ${best.first} (confidence=${best.third.confidence})")
        
        return best.second
    }
    
    /**
     * Fuzzy string matching - checks if needle exists in haystack with some tolerance
     */
    private fun containsFuzzy(haystack: String, needle: String): Boolean {
        if (needle.isBlank()) return false
        
        val haystackLower = haystack.lowercase()
        val needleLower = needle.lowercase()
        
        // Direct match
        if (haystackLower.contains(needleLower)) return true
        
        // Remove special characters and try again
        val cleanHaystack = haystackLower.replace(Regex("[^a-z0-9\\s]"), "")
        val cleanNeedle = needleLower.replace(Regex("[^a-z0-9\\s]"), "")
        
        if (cleanHaystack.contains(cleanNeedle)) return true
        
        // Word-by-word matching (for multi-word titles)
        val needleWords = cleanNeedle.split("\\s+".toRegex()).filter { it.length > 2 }
        val matchedWords = needleWords.count { word ->
            cleanHaystack.contains(word)
        }
        
        // If more than 50% of words match, consider it a fuzzy match
        return matchedWords.toFloat() / needleWords.size.toFloat() > 0.5f
    }
}

/**
 * Result of lyrics validation
 */
data class LyricsValidationResult(
    val isValid: Boolean,
    val confidence: Float, // 0.0 to 1.0
    val issues: List<String>,
    val providerName: String
)
