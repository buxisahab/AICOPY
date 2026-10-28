package com.musifly.android.utils.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Simple HTTP client for fetching JSON from GitHub RAW URL
 * 
 * Features:
 * - GET request to specified URL
 * - Returns cached JSON if network request fails
 * - Handles errors gracefully
 */
object HttpFetcher {
    private const val TAG = "HttpFetcher"
    private const val CONNECT_TIMEOUT = 10000 // 10 seconds
    private const val READ_TIMEOUT = 10000 // 10 seconds
    
    /**
     * Fetch JSON from URL
     * 
     * @param urlString The GitHub RAW URL to fetch from
     * @param context Context for accessing cached data
     * @return JSON string, or null if fetch failed and no cache available
     */
    suspend fun fetchJson(urlString: String, context: android.content.Context): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            Log.d(TAG, "Attempting to fetch JSON from: $urlString")
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "MusiFly-UpdateChecker/1.0")
            connection.setRequestProperty("Cache-Control", "no-cache")
            
            Log.d(TAG, "Connecting to URL...")
            val responseCode = connection.responseCode
            Log.d(TAG, "HTTP response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line).append("\n")
                }
                reader.close()
                
                val jsonString = response.toString().trim()
                
                if (jsonString.isEmpty()) {
                    Log.w(TAG, "Empty JSON response received")
                    return@withContext getCachedJson(context)
                }
                
                // Cache the successful response
                UpdatePrefs.setCachedJson(context, jsonString)
                
                Log.d(TAG, "✅ Successfully fetched update JSON (${jsonString.length} chars)")
                Log.d(TAG, "JSON preview: ${jsonString.take(200)}...")
                return@withContext jsonString
            } else {
                Log.w(TAG, "❌ HTTP request failed with code: $responseCode")
                val errorStream = connection.errorStream
                if (errorStream != null) {
                    val errorResponse = BufferedReader(InputStreamReader(errorStream)).readText()
                    Log.w(TAG, "Error response: $errorResponse")
                }
                // Fallback to cache
                return@withContext getCachedJson(context)
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "❌ Network error: Unknown host - ${e.message}")
            return@withContext getCachedJson(context)
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "❌ Network error: Connection timeout - ${e.message}")
            return@withContext getCachedJson(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching JSON: ${e.message}", e)
            e.printStackTrace()
            // Fallback to cache on any error
            return@withContext getCachedJson(context)
        } finally {
            // Ensure connection is closed
            try {
                connection?.disconnect()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    /**
     * Get cached JSON if available
     */
    private fun getCachedJson(context: android.content.Context): String? {
        val cached = UpdatePrefs.getCachedJson(context)
        if (cached != null) {
            Log.d(TAG, "Using cached JSON")
        } else {
            Log.w(TAG, "No cached JSON available")
        }
        return cached
    }
}
