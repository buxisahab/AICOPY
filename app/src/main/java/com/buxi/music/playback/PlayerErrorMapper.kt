package com.buxi.music.playback

import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import java.io.EOFException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * PlayerErrorMapper - Maps ExoPlayer PlaybackException to user-friendly error categories
 * Migrated from Musifly for AI Music app
 */
object PlayerErrorMapper {
    private const val TAG = "PlayerErrorMapper"
    
    enum class ErrorCategory {
        NETWORK_ERROR,      // Network connection issues
        URL_EXPIRED,        // Stream URL has expired (YouTube/JioSaavn)
        UNSUPPORTED_FORMAT, // Audio format not supported
        SERVER_BAD_RESPONSE,// Server returned error (403, 404, 500, etc.)
        CORRUPTED_CACHE,    // Cached file is corrupted
        PERMISSION_ERROR,   // Storage/permission denied
        UNKNOWN             // Truly unknown error
    }
    
    fun categorizeError(error: PlaybackException, streamType: String? = null): ErrorCategory {
        val errorCode = error.errorCode
        val errorMessage = error.message?.lowercase() ?: ""
        val cause = error.cause
        
        if (isCorruptedCacheError(error, errorCode, errorMessage, cause)) return ErrorCategory.CORRUPTED_CACHE
        if (isExpiredUrlError(error, errorCode, errorMessage, cause)) return ErrorCategory.URL_EXPIRED
        if (isNetworkError(error, errorCode, errorMessage, cause)) return ErrorCategory.NETWORK_ERROR
        if (isUnsupportedFormatError(error, errorCode, errorMessage)) return ErrorCategory.UNSUPPORTED_FORMAT
        if (isServerBadResponseError(error, errorCode, errorMessage, cause)) return ErrorCategory.SERVER_BAD_RESPONSE
        
        if (errorCode == PlaybackException.ERROR_CODE_IO_NO_PERMISSION ||
            errorMessage.contains("permission") || errorMessage.contains("access denied")) {
            return ErrorCategory.PERMISSION_ERROR
        }
        
        return ErrorCategory.UNKNOWN
    }
    
    private fun isNetworkError(error: PlaybackException, errorCode: Int, errorMessage: String, cause: Throwable?): Boolean {
        if (cause is EOFException) return false
        return errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                (errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED && cause !is EOFException) ||
                cause is UnknownHostException || cause is ConnectException || cause is SocketTimeoutException ||
                errorMessage.contains("network") || errorMessage.contains("connection") || errorMessage.contains("timeout")
    }
    
    private fun isExpiredUrlError(error: PlaybackException, errorCode: Int, errorMessage: String, cause: Throwable?): Boolean {
        val hasVarintError = generateSequence(cause) { it.cause }.any { it.message?.contains("varint", true) == true }
        return hasVarintError || errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                errorMessage.contains("expired") || errorMessage.contains("signature") ||
                (cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode == 403)
    }
    
    private fun isCorruptedCacheError(error: PlaybackException, errorCode: Int, errorMessage: String, cause: Throwable?): Boolean {
        val hasEofException = generateSequence(cause) { it.cause }.any { it is EOFException }
        return hasEofException || errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ||
                errorMessage.contains("eof") || errorMessage.contains("corrupted")
    }
    
    private fun isUnsupportedFormatError(error: PlaybackException, errorCode: Int, errorMessage: String): Boolean {
        val cause = error.cause
        val isParserExceptionWithAtom = cause?.javaClass?.simpleName == "ParserException" &&
                (cause.message?.contains("Skipping atom with length") == true || errorMessage.contains("2147483647"))
        return isParserExceptionWithAtom || errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED
    }
    
    private fun isServerBadResponseError(error: PlaybackException, errorCode: Int, errorMessage: String, cause: Throwable?): Boolean {
        return errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                errorCode == PlaybackException.ERROR_CODE_REMOTE_ERROR ||
                (cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode in 400..599)
    }
    
    fun isRecoverable(errorCategory: ErrorCategory): Boolean {
        return errorCategory == ErrorCategory.NETWORK_ERROR || 
               errorCategory == ErrorCategory.URL_EXPIRED || 
               errorCategory == ErrorCategory.CORRUPTED_CACHE || 
               errorCategory == ErrorCategory.SERVER_BAD_RESPONSE
    }
    
    fun getUserFriendlyMessage(errorCategory: ErrorCategory, streamType: String? = null): String {
        return when (errorCategory) {
            ErrorCategory.NETWORK_ERROR -> "Network issue. Tap to retry"
            ErrorCategory.URL_EXPIRED -> "Stream expired. Refreshing..."
            ErrorCategory.CORRUPTED_CACHE -> "Cache issue. Retrying..."
            ErrorCategory.UNSUPPORTED_FORMAT -> "Audio format not supported"
            ErrorCategory.SERVER_BAD_RESPONSE -> "Stream unavailable. Trying alternative..."
            else -> "Playback error. Tap to retry"
        }
    }
}
