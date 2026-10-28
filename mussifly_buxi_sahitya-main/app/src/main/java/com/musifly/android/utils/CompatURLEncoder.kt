package com.musifly.android.utils

import android.os.Build
import java.net.URLEncoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * URLEncoder utility that works across all Android versions
 * Handles the difference between URLEncoder.encode(String, String) and URLEncoder.encode(String, Charset)
 */
object CompatURLEncoder {
    
    /**
     * Encode a string using UTF-8 encoding, compatible with all Android versions
     */
    fun encodeUTF8(value: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+) supports Charset parameter
                URLEncoder.encode(value, StandardCharsets.UTF_8)
            } else {
                // Android 12 and below use String parameter
                @Suppress("DEPRECATION")
                URLEncoder.encode(value, "UTF-8")
            }
        } catch (e: Exception) {
            // Fallback to deprecated method if new one fails
            @Suppress("DEPRECATION")
            URLEncoder.encode(value, "UTF-8")
        }
    }
    
    /**
     * Encode a string using specified charset, compatible with all Android versions
     */
    fun encode(value: String, charset: Charset): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ supports Charset parameter
                URLEncoder.encode(value, charset)
            } else {
                // Android 12 and below use String parameter
                @Suppress("DEPRECATION")
                URLEncoder.encode(value, charset.name())
            }
        } catch (e: Exception) {
            // Fallback to deprecated method
            @Suppress("DEPRECATION")
            URLEncoder.encode(value, charset.name())
        }
    }
}
