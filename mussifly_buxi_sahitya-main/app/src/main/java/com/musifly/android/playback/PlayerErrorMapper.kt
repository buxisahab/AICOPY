package com.musifly.android.playback

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import java.io.EOFException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * PlayerErrorMapper - Maps ExoPlayer PlaybackException to user-friendly error categories
 * 
 * This utility categorizes playback errors to enable intelligent retry logic and
 * user-friendly error messages, replacing generic "Unknown error: Bad stream player response"
 * 
 * ERROR ORIGIN ANALYSIS:
 * - "Bad stream player response" comes from YTPlayerUtils.kt when all YouTube clients fail
 * - This can happen due to: expired URLs, network issues, server errors, or format problems
 * 
 * STREAM TYPES SUPPORTED:
 * - YouTube / YouTube Music (via NewPipe/Innertube)
 * - JioSaavn (via custom API)
 * - Local files (FileDataSource)
 */
object PlayerErrorMapper {
    private const val TAG = "PlayerErrorMapper"
    
    /**
     * Error categories for playback errors
     */
    enum class ErrorCategory {
        NETWORK_ERROR,      // Network connection issues (timeout, no internet, etc.)
        URL_EXPIRED,        // Stream URL has expired (YouTube/JioSaavn signed URLs)
        UNSUPPORTED_FORMAT, // Audio format not supported by device
        SERVER_BAD_RESPONSE,// Server returned error (403, 404, 500, etc.)
        CORRUPTED_CACHE,    // Cached file is corrupted/incomplete
        PERMISSION_ERROR,   // Storage/permission denied
        UNKNOWN             // Truly unknown error (use only when mapping fails)
    }
    
    /**
     * Maps a PlaybackException to an ErrorCategory
     * 
     * @param error The PlaybackException from ExoPlayer
     * @param streamType Optional: "youtube", "saavn", "local" for better error context
     * @return ErrorCategory indicating the type of error
     */
    fun categorizeError(error: PlaybackException, streamType: String? = null): ErrorCategory {
        val errorCode = error.errorCode
        val errorMessage = error.message?.lowercase() ?: ""
        val cause = error.cause
        
        Log.d(TAG, "Categorizing error: code=$errorCode, message=$errorMessage, streamType=$streamType")
        
        // CRITICAL: Check for corrupted cache FIRST (before network errors)
        // EOFException and malformed container errors should be detected early
        // to avoid misclassifying cache corruption as network issues
        if (isCorruptedCacheError(error, errorCode, errorMessage, cause)) {
            Log.d(TAG, "→ Categorized as CORRUPTED_CACHE")
            return ErrorCategory.CORRUPTED_CACHE
        }
        
        // Check for expired URL errors (common with YouTube/JioSaavn signed URLs)
        if (isExpiredUrlError(error, errorCode, errorMessage, cause)) {
            Log.d(TAG, "→ Categorized as URL_EXPIRED")
            return ErrorCategory.URL_EXPIRED
        }
        
        // Check for network-related errors (after cache/URL checks)
        if (isNetworkError(error, errorCode, errorMessage, cause)) {
            Log.d(TAG, "→ Categorized as NETWORK_ERROR")
            return ErrorCategory.NETWORK_ERROR
        }
        
        // Check for unsupported format
        if (isUnsupportedFormatError(error, errorCode, errorMessage)) {
            Log.d(TAG, "→ Categorized as UNSUPPORTED_FORMAT")
            return ErrorCategory.UNSUPPORTED_FORMAT
        }
        
        // Check for server errors (HTTP 403, 404, 500, etc.)
        if (isServerBadResponseError(error, errorCode, errorMessage, cause)) {
            Log.d(TAG, "→ Categorized as SERVER_BAD_RESPONSE")
            return ErrorCategory.SERVER_BAD_RESPONSE
        }
        
        // Check for permission errors
        if (errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION ||
            errorMessage.contains("permission", ignoreCase = true) ||
            errorMessage.contains("access denied", ignoreCase = true)) {
            Log.d(TAG, "→ Categorized as PERMISSION_ERROR")
            return ErrorCategory.PERMISSION_ERROR
        }
        
        // Default to unknown if we can't categorize
        Log.w(TAG, "→ Could not categorize error, defaulting to UNKNOWN")
        return ErrorCategory.UNKNOWN
    }
    
    /**
     * Check if error indicates a network issue
     * NOTE: This should be called AFTER checking for corrupted cache errors,
     * since EOFException can have ERROR_CODE_IO_UNSPECIFIED but is cache-related
     */
    private fun isNetworkError(
        error: PlaybackException,
        errorCode: Int,
        errorMessage: String,
        cause: Throwable?
    ): Boolean {
        // Exclude EOFException - that's a cache corruption issue, not network
        if (cause is EOFException) {
            return false
        }
        
        // Media3 valid error codes: ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        // ERROR_CODE_IO_UNSPECIFIED can indicate network issues, but only if not EOFException
        return errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                (errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED && cause !is EOFException) ||
                cause is UnknownHostException ||
                cause is ConnectException ||
                cause is SocketTimeoutException ||
                errorMessage.contains("network", ignoreCase = true) ||
                errorMessage.contains("connection", ignoreCase = true) ||
                errorMessage.contains("timeout", ignoreCase = true) ||
                errorMessage.contains("unreachable", ignoreCase = true) ||
                errorMessage.contains("no route to host", ignoreCase = true) ||
                (cause?.message?.contains("network", ignoreCase = true) == true)
    }
    
    /**
     * Check if error indicates an expired URL
     * YouTube/JioSaavn URLs often have expiration tokens and can become invalid
     */
    private fun isExpiredUrlError(
        error: PlaybackException,
        errorCode: Int,
        errorMessage: String,
        cause: Throwable?
    ): Boolean {
        // "varint" errors in YouTube streams often indicate expired/invalid URLs
        val hasVarintError = generateSequence(cause) { it.cause }
            .any { it.message?.contains("varint", ignoreCase = true) == true }
        
        return hasVarintError ||
                errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                errorMessage.contains("varint", ignoreCase = true) ||
                errorMessage.contains("expired", ignoreCase = true) ||
                errorMessage.contains("invalid token", ignoreCase = true) ||
                errorMessage.contains("signature", ignoreCase = true) ||
                // HTTP 403 often means expired URL for YouTube
                // Note: Only InvalidResponseCodeException has responseCode property
                (cause is HttpDataSource.InvalidResponseCodeException &&
                        (cause as HttpDataSource.InvalidResponseCodeException).responseCode == 403) ||
                (cause?.message?.contains("403", ignoreCase = false) == true)
    }
    
    /**
     * Check if error indicates corrupted cache
     * EOFException typically occurs when reading incomplete/corrupted cache files
     * ArrayIndexOutOfBoundsException with negative index indicates buffer corruption from corrupted cache
     */
    private fun isCorruptedCacheError(
        error: PlaybackException,
        errorCode: Int,
        errorMessage: String,
        cause: Throwable?
    ): Boolean {
        // Check for EOFException in the cause chain (may be nested)
        val hasEofException = generateSequence(cause) { it.cause }
            .any { it is EOFException }
        
        // Check for ArrayIndexOutOfBoundsException with negative index (indicates buffer corruption)
        // This happens when DefaultExtractorInput tries to read corrupted cached data
        // Negative srcPos like "-2141876520" is a clear sign of corrupted buffer from bad cache
        val hasNegativeIndexException = generateSequence(cause) { it.cause }
            .any { exception ->
                exception is ArrayIndexOutOfBoundsException &&
                (exception.message?.contains("srcPos=-", ignoreCase = true) == true ||
                 exception.message?.contains("dstPos=-", ignoreCase = true) == true ||
                 exception.message?.contains("updatepeekbuffer", ignoreCase = true) == true ||
                 exception.message?.matches(Regex(".*srcPos=-\\d+.*", RegexOption.IGNORE_CASE)) == true ||
                 exception.message?.matches(Regex(".*dstPos=-\\d+.*", RegexOption.IGNORE_CASE)) == true)
            }
        
        return hasEofException ||
                hasNegativeIndexException ||
                errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                errorCode == PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE ||
                errorMessage.contains("eof", ignoreCase = true) ||
                errorMessage.contains("corrupted", ignoreCase = true) ||
                errorMessage.contains("incomplete", ignoreCase = true) ||
                errorMessage.contains("malformed", ignoreCase = true) ||
                errorMessage.contains("skipping atom", ignoreCase = true) ||
                errorMessage.contains("arrayindexoutofboundsexception", ignoreCase = true) ||
                errorMessage.contains("srcpos=-", ignoreCase = true) ||
                errorMessage.contains("updatepeekbuffer", ignoreCase = true) ||
                (cause?.javaClass?.simpleName == "ArrayIndexOutOfBoundsException" &&
                 cause.message?.contains("srcPos=-", ignoreCase = true) == true)
    }
    
    /**
     * Check if error indicates unsupported format
     */
    private fun isUnsupportedFormatError(
        error: PlaybackException,
        errorCode: Int,
        errorMessage: String
    ): Boolean {
        val cause = error.cause
        // Check for ParserException with "Skipping atom with length > 2147483647" (MP4 container issue)
        val isParserExceptionWithAtom = cause?.javaClass?.simpleName == "ParserException" &&
                (cause.message?.contains("Skipping atom with length", ignoreCase = true) == true ||
                 cause.message?.contains("2147483647", ignoreCase = false) == true ||
                 errorMessage.contains("Skipping atom with length", ignoreCase = true) ||
                 errorMessage.contains("2147483647", ignoreCase = false))
        
        return isParserExceptionWithAtom ||
                errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ||
                errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                errorMessage.contains("unsupported", ignoreCase = true) ||
                errorMessage.contains("format", ignoreCase = true) ||
                errorMessage.contains("codec", ignoreCase = true) ||
                errorMessage.contains("decoder", ignoreCase = true)
    }
    
    /**
     * Check if error indicates server returned bad response
     */
    private fun isServerBadResponseError(
        error: PlaybackException,
        errorCode: Int,
        errorMessage: String,
        cause: Throwable?
    ): Boolean {
        return errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                errorCode == PlaybackException.ERROR_CODE_REMOTE_ERROR ||
                errorMessage.contains("bad stream", ignoreCase = true) ||
                errorMessage.contains("player response", ignoreCase = true) ||
                errorMessage.contains("server error", ignoreCase = true) ||
                // Note: Only InvalidResponseCodeException has responseCode property
                (cause is HttpDataSource.InvalidResponseCodeException &&
                        ((cause as HttpDataSource.InvalidResponseCodeException).responseCode in 400..599))
    }
    
    /**
     * Check if an error is recoverable (should retry automatically)
     */
    fun isRecoverable(errorCategory: ErrorCategory): Boolean {
        return when (errorCategory) {
            ErrorCategory.NETWORK_ERROR,
            ErrorCategory.URL_EXPIRED,
            ErrorCategory.CORRUPTED_CACHE,
            ErrorCategory.SERVER_BAD_RESPONSE -> true
            ErrorCategory.UNSUPPORTED_FORMAT,
            ErrorCategory.PERMISSION_ERROR,
            ErrorCategory.UNKNOWN -> false
        }
    }
    
    /**
     * Get user-friendly error message for display
     * 
     * @param errorCategory The categorized error
     * @param streamType Optional: "youtube", "saavn", "local" for context
     * @return User-friendly error message
     */
    fun getUserFriendlyMessage(errorCategory: ErrorCategory, streamType: String? = null): String {
        return when (errorCategory) {
            ErrorCategory.NETWORK_ERROR -> "Network connection issue. Tap to retry"
            ErrorCategory.URL_EXPIRED -> {
                when (streamType?.lowercase()) {
                    "youtube", "youtube music" -> "Stream expired. Refreshing..."
                    "saavn", "jiosaavn" -> "Stream expired. Refreshing..."
                    else -> "Stream expired. Tap to retry"
                }
            }
            ErrorCategory.CORRUPTED_CACHE -> "Cache issue detected. Clearing and retrying..."
            ErrorCategory.UNSUPPORTED_FORMAT -> "Audio format not supported"
            ErrorCategory.SERVER_BAD_RESPONSE -> {
                when (streamType?.lowercase()) {
                    "youtube", "youtube music" -> "Stream unavailable. Trying alternative..."
                    "saavn", "jiosaavn" -> "Song temporarily unavailable. Tap to retry"
                    else -> "Streaming issue. Tap to retry"
                }
            }
            ErrorCategory.PERMISSION_ERROR -> "Permission denied. Please check app settings"
            ErrorCategory.UNKNOWN -> "Playback error. Tap to retry"
        }
    }
    
    /**
     * Get detailed error info for logging (masks sensitive data like tokens)
     */
    fun getDetailedErrorInfo(error: PlaybackException, streamUrl: String? = null): String {
        val maskedUrl = streamUrl?.let { url ->
            // Mask query parameters that might contain tokens
            try {
                val uri = android.net.Uri.parse(url)
                val scheme = uri.scheme ?: ""
                val host = uri.host ?: ""
                val path = uri.path ?: ""
                // Don't include query params in logs (may contain sensitive tokens)
                "$scheme://$host$path[?query_masked]"
            } catch (e: Exception) {
                "[URL parsing failed]"
            }
        } ?: "[No URL]"
        
        return buildString {
            append("ErrorCode: ${error.errorCodeName}")
            append(", Category: ${categorizeError(error)}")
            append(", Message: ${error.message?.take(100)}")
            append(", StreamURL: $maskedUrl")
            error.cause?.let {
                append(", Cause: ${it.javaClass.simpleName}")
                append(": ${it.message?.take(100)}")
            }
        }
    }
}
