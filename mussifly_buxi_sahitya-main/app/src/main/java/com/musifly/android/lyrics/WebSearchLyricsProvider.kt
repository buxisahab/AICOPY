package com.musifly.android.lyrics

import android.content.Context
import android.util.Log
import com.musifly.android.constants.EnableWebSearchLyricsKey
import com.musifly.android.utils.dataStore
import com.musifly.android.utils.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Web Search-based Lyrics Provider
 * 
 * This provider searches for lyrics using multiple search engines
 * and extracts structured lyrics data from known lyrics websites.
 * 
 * Features:
 * - Searches multiple lyrics websites (Genius, AZLyrics, MetroLyrics, etc.)
 * - Extracts clean lyrics from HTML
 * - Validates extracted content
 * - Falls back to multiple sources
 */
object WebSearchLyricsProvider : LyricsProvider {
    override val name = "WebSearch"
    private const val TAG = "WebSearchLyrics"
    
    // Known lyrics websites with their extraction patterns
    private val lyricsWebsites = listOf(
        "genius.com",
        "azlyrics.com", 
        "metrolyrics.com",
        "lyrics.com",
        "songlyrics.com",
        "lyricsfreak.com"
    )
    
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableWebSearchLyricsKey] ?: false // Default disabled
    
    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching web for lyrics: $artist - $title")
            
            // Build search query
            val query = buildSearchQuery(title, artist)
            
            // Try to find lyrics from known websites
            val lyrics = searchLyricsWebsites(query, title, artist)
            
            if (lyrics != null && lyrics.isNotBlank()) {
                Log.i(TAG, "Found lyrics via web search for: $artist - $title")
                Result.success(lyrics)
            } else {
                Log.w(TAG, "No lyrics found via web search for: $artist - $title")
                Result.failure(Exception("No lyrics found via web search"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching web for lyrics", e)
            Result.failure(e)
        }
    }
    
    /**
     * Build search query optimized for lyrics
     */
    private fun buildSearchQuery(title: String, artist: String): String {
        val cleanTitle = title.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "").trim()
        val cleanArtist = artist.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "").trim()
        return "$cleanArtist $cleanTitle lyrics"
    }
    
    /**
     * Search for lyrics on known lyrics websites
     */
    private suspend fun searchLyricsWebsites(query: String, title: String, artist: String): String? {
        // Use DuckDuckGo Lite for searching (privacy-friendly, no API key needed)
        val searchUrl = "https://lite.duckduckgo.com/lite/?q=${URLEncoder.encode(query, "UTF-8")}"
        
        try {
            val searchResults = performHttpGet(searchUrl) ?: return null
            
            // Extract URLs from search results
            val urls = extractLyricsUrls(searchResults)
            
            // Try each URL to extract lyrics
            for (url in urls) {
                try {
                    val pageContent = performHttpGet(url) ?: continue
                    val extractedLyrics = extractLyricsFromPage(pageContent, url)
                    
                    if (extractedLyrics != null && extractedLyrics.length > 100) {
                        // Basic validation - lyrics should have multiple lines
                        if (extractedLyrics.split("\n").size > 5) {
                            return cleanLyrics(extractedLyrics)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to extract from $url", e)
                    continue
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
        }
        
        return null
    }
    
    /**
     * Extract lyrics URLs from search results
     */
    private fun extractLyricsUrls(html: String): List<String> {
        val urls = mutableListOf<String>()
        
        // Look for known lyrics website URLs in the HTML
        lyricsWebsites.forEach { website ->
            val pattern = Regex("href=\"(https?://[^\"]*$website[^\"]*)\"")
            pattern.findAll(html).forEach { match ->
                match.groupValues.getOrNull(1)?.let { url ->
                    // Filter out non-lyrics pages
                    if (!url.contains("artist") && !url.contains("search")) {
                        urls.add(url)
                    }
                }
            }
        }
        
        return urls.take(5) // Limit to first 5 URLs
    }
    
    /**
     * Extract lyrics from a webpage
     */
    private fun extractLyricsFromPage(html: String, url: String): String? {
        // Different extraction logic based on website
        return when {
            url.contains("genius.com") -> extractGeniusLyrics(html)
            url.contains("azlyrics.com") -> extractAZLyrics(html)
            url.contains("lyrics.com") -> extractGenericLyrics(html, "lyrics")
            url.contains("songlyrics.com") -> extractGenericLyrics(html, "lyrics")
            else -> extractGenericLyrics(html, "lyrics")
        }
    }
    
    /**
     * Extract lyrics from Genius.com
     */
    private fun extractGeniusLyrics(html: String): String? {
        // Look for Genius lyrics container
        val pattern = Regex("data-lyrics-container=\"true\"[^>]*>(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(html)
        
        return match?.groupValues?.getOrNull(1)?.let { content ->
            // Remove HTML tags
            content.replace(Regex("<[^>]*>"), "\n")
                .replace(Regex("\\[.*?\\]"), "") // Remove section labels like [Verse 1]
                .trim()
        }
    }
    
    /**
     * Extract lyrics from AZLyrics
     */
    private fun extractAZLyrics(html: String): String? {
        // AZLyrics typically has lyrics in a div without class/id
        val pattern = Regex("<!-- Usage of azlyrics.com content.*?-->(.*?)<!-- MxM banner -->", RegexOption.DOT_MATCHES_ALL)
        val match = pattern.find(html)
        
        return match?.groupValues?.getOrNull(1)?.let { content ->
            content.replace(Regex("<[^>]*>"), "\n").trim()
        }
    }
    
    /**
     * Generic lyrics extraction
     */
    private fun extractGenericLyrics(html: String, keyword: String): String? {
        // Try to find content with "lyrics" keyword in class or id
        val patterns = listOf(
            Regex("class=\"[^\"]*$keyword[^\"]*\"[^>]*>(.*?)</[^>]+>", RegexOption.DOT_MATCHES_ALL),
            Regex("id=\"[^\"]*$keyword[^\"]*\"[^>]*>(.*?)</[^>]+>", RegexOption.DOT_MATCHES_ALL)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) {
                return match.groupValues.getOrNull(1)?.let { content ->
                    content.replace(Regex("<[^>]*>"), "\n").trim()
                }
            }
        }
        
        return null
    }
    
    /**
     * Clean extracted lyrics
     */
    private fun cleanLyrics(lyrics: String): String {
        return lyrics
            .replace(Regex("\\n{3,}"), "\n\n") // Remove excessive line breaks
            .replace(Regex("[ \\t]+"), " ") // Normalize spaces
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&#39;"), "'")
            .trim()
    }
    
    /**
     * Perform HTTP GET request
     */
    private fun performHttpGet(urlString: String): String? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                return response.toString()
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTTP request failed for $urlString", e)
        } finally {
            connection?.disconnect()
        }
        
        return null
    }
}
