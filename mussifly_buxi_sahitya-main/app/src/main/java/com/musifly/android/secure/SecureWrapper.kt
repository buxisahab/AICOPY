package com.musifly.android.secure

import com.musifly.android.secure.StringDecode
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString

/**
 * Wrapper class that remains in normal DEX (NOT Dex2C processed).
 * 
 * This class provides a bridge between Dex2C-unsafe code (using Ktor, etc.)
 * and Dex2C-safe code (pure string manipulation).
 * 
 * CRITICAL: This class must NOT be Dex2C processed.
 * It uses Ktor URLBuilder which has dependencies that are not Dex2C-safe.
 */
object SecureWrapper {
    
    /**
     * Wrapper for nSigDecode that can be called from anywhere.
     * The actual implementation is in StringDecode (Dex2C-safe).
     */
    fun nSigDecode(n: String): String {
        return StringDecode.nSigDecode(n)
    }
    
    /**
     * Wrapper for sigDecode that can be called from anywhere.
     * The actual implementation is in StringDecode (Dex2C-safe).
     */
    fun sigDecode(input: String): String {
        return StringDecode.sigDecode(input)
    }
    
    /**
     * Wrapper for createUrl that bridges Ktor dependencies with secure logic.
     * The URL building logic uses Ktor (not Dex2C-safe), but the signature
     * decoding uses secure functions (Dex2C-safe).
     */
    fun createUrl(
        url: String? = null,
        cipher: String? = null,
    ): String? {
        val resUrl: URLBuilder
        var signature = ""
        var signatureParam = "sig"
        
        if (cipher != null) {
            val params = parseQueryString(cipher)
            signature = params["s"] ?: return null
            signatureParam = params["sp"] ?: return null
            resUrl = params["url"]?.let { URLBuilder(it) } ?: return null
        } else {
            resUrl = url?.let { URLBuilder(it) } ?: return null
        }
        
        val n = resUrl.parameters["n"]
        if (n != null) {
            resUrl.parameters["n"] = StringDecode.nSigDecode(n.toString())
        }
        
        if (cipher != null) {
            resUrl.parameters[signatureParam] = StringDecode.sigDecode(signature)
        }
        
        resUrl.parameters["c"] = "ANDROID_MUSIC"
        
        return resUrl.toString()
    }
}
