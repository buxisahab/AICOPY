package com.musifly.android.playback

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File

/**
 * Validates local audio files before passing to ExoPlayer
 * to prevent UnrecognizedInputFormatException and crashes.
 * 
 * Supported Audio Formats (via ExoPlayer DefaultExtractorsFactory):
 * - MP3 (.mp3) - MPEG Audio Layer 3
 * - AAC (.aac, .m4a) - Advanced Audio Coding
 * - FLAC (.flac) - Free Lossless Audio Codec
 * - OGG (.ogg, .oga) - Ogg Vorbis/Opus
 * - WAV (.wav) - Waveform Audio File
 * - OPUS (.opus) - Opus in Ogg/Matroska container
 * - AMR (.amr) - Adaptive Multi-Rate
 * - MP4 (.mp4, .m4a) - MPEG-4 Audio
 * - WEBM (.webm) - WebM Audio
 * - MKA (.mka, .mkv) - Matroska Audio
 * - AC3 (.ac3) - Dolby Digital
 * - EAC3 (.eac3) - Dolby Digital Plus
 * - DTS (.dts) - DTS Audio
 * - WMA (.wma) - Windows Media Audio (partial support)
 * - AIFF (.aiff, .aif) - Audio Interchange File Format
 */
object LocalFileValidator {
    private const val TAG = "LocalFileValidator"
    
    // Minimum file size: 1KB (corrupted files are often 0 bytes or very small)
    private const val MIN_FILE_SIZE = 1024L
    
    // Maximum file size: 500MB (sanity check)
    private const val MAX_FILE_SIZE = 500 * 1024 * 1024L
    
    // Supported audio MIME types
    private val SUPPORTED_MIME_TYPES = setOf(
        "audio/mpeg", // MP3
        "audio/mp3",
        "audio/aac", // AAC
        "audio/mp4", // M4A
        "audio/flac", // FLAC
        "audio/ogg", // OGG Vorbis
        "audio/opus", // Opus
        "audio/wav", // WAV
        "audio/x-wav",
        "audio/wave",
        "audio/vnd.wave",
        "audio/amr", // AMR
        "audio/3gpp",
        "audio/webm", // WebM
        "audio/x-matroska", // MKA/MKV
        "audio/ac3", // AC3
        "audio/eac3", // EAC3
        "audio/vnd.dts", // DTS
        "audio/vnd.dts.hd",
        "audio/x-ms-wma", // WMA
        "audio/aiff", // AIFF
        "audio/x-aiff"
    )
    
    data class ValidationResult(
        val isValid: Boolean,
        val reason: String? = null,
        val actualMimeType: String? = null,
        val duration: Long? = null,
        val hasEmbeddedArt: Boolean = false
    )
    
    /**
     * Validates a local audio file URI
     */
    fun validate(context: Context, uri: Uri): ValidationResult {
        try {
            // Convert URI to file path
            val filePath = when (uri.scheme) {
                "file" -> uri.path
                "content" -> {
                    // For content URIs, we can't easily get the file path
                    // so we'll try to validate using MediaMetadataRetriever directly
                    return validateWithRetriever(context, uri)
                }
                else -> return ValidationResult(false, "Unsupported URI scheme: ${uri.scheme}")
            }
            
            if (filePath == null) {
                return ValidationResult(false, "Could not extract file path from URI")
            }
            
            val file = File(filePath)
            
            // Check if file exists
            if (!file.exists()) {
                return ValidationResult(false, "File does not exist: $filePath")
            }
            
            // Check if readable
            if (!file.canRead()) {
                return ValidationResult(false, "File is not readable: $filePath")
            }
            
            // Check file size
            val fileSize = file.length()
            if (fileSize < MIN_FILE_SIZE) {
                return ValidationResult(false, "File too small (${fileSize}B), likely corrupted")
            }
            
            if (fileSize > MAX_FILE_SIZE) {
                return ValidationResult(false, "File too large (${fileSize}B), exceeds limit")
            }
            
            // Validate with MediaMetadataRetriever
            return validateWithRetriever(context, uri)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error validating file: ${uri}", e)
            return ValidationResult(false, "Validation error: ${e.message}")
        }
    }
    
    private fun validateWithRetriever(context: Context, uri: Uri): ValidationResult {
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            
            // Try to open the file with MediaMetadataRetriever
            when (uri.scheme) {
                "file" -> retriever.setDataSource(uri.path)
                "content" -> retriever.setDataSource(context, uri)
                else -> retriever.setDataSource(context, uri)
            }
            
            // Extract metadata to verify it's a valid audio file
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val hasArt = retriever.embeddedPicture != null
            
            // Check if it's an audio file
            if (mimeType == null || !mimeType.startsWith("audio/")) {
                return ValidationResult(
                    false, 
                    "Not an audio file (MIME: ${mimeType ?: "unknown"})"
                )
            }
            
            // Check if format is supported (warn but don't reject - ExoPlayer will try anyway)
            val isSupported = SUPPORTED_MIME_TYPES.contains(mimeType.lowercase())
            if (!isSupported) {
                Log.w(TAG, "⚠️ Uncommon MIME type: $mimeType - ExoPlayer will attempt playback")
                // Don't fail - ExoPlayer has additional format support we might not list
            }
            
            // Check if duration is valid (some corrupted files report 0 duration)
            if (duration == null || duration <= 0) {
                Log.w(TAG, "File has invalid duration: ${uri}, duration=$duration")
                // Don't fail here, some valid files might have this issue
                // but log it for debugging
            }
            
            Log.d(TAG, "✅ File validated: $uri (MIME: $mimeType, Duration: ${duration}ms, Art: $hasArt, Supported: $isSupported)")
            
            return ValidationResult(
                isValid = true,
                actualMimeType = mimeType,
                duration = duration,
                hasEmbeddedArt = hasArt
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "MediaMetadataRetriever failed for: $uri", e)
            return ValidationResult(
                false,
                "Cannot read audio metadata: ${e.message}"
            )
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }
    }
    
    /**
     * Extracts embedded album art from local file
     */
    fun extractEmbeddedArt(context: Context, uri: Uri): ByteArray? {
        Log.d(TAG, "🎨 Attempting to extract embedded art from: $uri")
        var retriever: MediaMetadataRetriever? = null
        try {
            retriever = MediaMetadataRetriever()
            
            // Set data source based on URI scheme
            when (uri.scheme) {
                "file" -> {
                    Log.d(TAG, "📁 Using file path: ${uri.path}")
                    retriever.setDataSource(uri.path)
                }
                "content" -> {
                    Log.d(TAG, "📦 Using content URI")
                    retriever.setDataSource(context, uri)
                }
                else -> {
                    Log.d(TAG, "🔗 Using generic URI")
                    retriever.setDataSource(context, uri)
                }
            }
            
            val embeddedPicture = retriever.embeddedPicture
            
            if (embeddedPicture != null && embeddedPicture.isNotEmpty()) {
                Log.d(TAG, "✅ Extracted embedded art: ${embeddedPicture.size} bytes")
            } else {
                Log.d(TAG, "ℹ️ No embedded art found in file")
            }
            
            return embeddedPicture
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error extracting embedded art from: $uri", e)
            return null
        } finally {
            try {
                retriever?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }
    }
    
    /**
     * Get list of supported audio format extensions
     */
    fun getSupportedFormats(): List<String> {
        return listOf(
            "mp3", "m4a", "aac", "flac", "ogg", "oga", "opus",
            "wav", "amr", "mp4", "webm", "mka", "mkv",
            "ac3", "eac3", "dts", "wma", "aiff", "aif"
        )
    }
    
    /**
     * Get user-friendly list of supported formats with descriptions
     */
    fun getSupportedFormatsWithNames(): Map<String, String> {
        return mapOf(
            "mp3" to "MP3 - MPEG Audio Layer 3",
            "m4a" to "M4A/AAC - Advanced Audio Coding",
            "aac" to "AAC - Advanced Audio Coding",
            "flac" to "FLAC - Free Lossless Audio Codec",
            "ogg" to "OGG - Ogg Vorbis",
            "oga" to "OGA - Ogg Audio",
            "opus" to "OPUS - Opus Audio",
            "wav" to "WAV - Waveform Audio",
            "amr" to "AMR - Adaptive Multi-Rate",
            "mp4" to "MP4 - MPEG-4 Audio",
            "webm" to "WEBM - WebM Audio",
            "mka" to "MKA - Matroska Audio",
            "mkv" to "MKV - Matroska Video (audio track)",
            "ac3" to "AC3 - Dolby Digital",
            "eac3" to "EAC3 - Dolby Digital Plus",
            "dts" to "DTS - DTS Audio",
            "wma" to "WMA - Windows Media Audio (partial)",
            "aiff" to "AIFF - Audio Interchange File Format",
            "aif" to "AIF - Audio Interchange Format"
        )
    }
    
    /**
     * Check if a file extension is supported
     */
    fun isFormatSupported(extension: String): Boolean {
        return getSupportedFormats().contains(extension.lowercase().removePrefix("."))
    }
}
