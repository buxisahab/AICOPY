package com.musifly.android.secure

/**
 * Dex2C-safe URL building functions.
 * 
 * CRITICAL: This package contains ONLY pure Java/Kotlin logic:
 * - No Android Context usage
 * - No threading (no coroutines, no synchronized blocks)
 * - No callbacks
 * - No reflection
 * - No blocking calls
 * - All methods are fast and return immediately
 * 
 * These functions can be safely converted to native C code via Dex2C.
 */
object UrlBuilder {
    
    /**
     * Build YouTube streaming URL with signature decoding.
     * 
     * NOTE: This function uses URLBuilder from Ktor which may have dependencies.
     * For full Dex2C safety, consider implementing a pure Java URL builder.
     * 
     * @param url Base URL (optional)
     * @param cipher Cipher string containing signature parameters (optional)
     * @return Decoded URL string or null if invalid
     */
    fun createUrl(
        url: String? = null,
        cipher: String? = null,
        parseQueryString: (String) -> Map<String, String>,
        createUrlBuilder: (String) -> Any?,
        getUrlBuilderParams: (Any) -> MutableMap<String, String>,
        urlBuilderToString: (Any) -> String
    ): String? {
        var signature = ""
        var signatureParam = "sig"
        
        val resUrl: Any?
        if (cipher != null) {
            val params = parseQueryString(cipher)
            signature = params["s"] ?: return null
            signatureParam = params["sp"] ?: return null
            val urlStr = params["url"] ?: return null
            resUrl = createUrlBuilder(urlStr) ?: return null
        } else {
            resUrl = url?.let { createUrlBuilder(it) } ?: return null
        }
        
        val params = getUrlBuilderParams(resUrl)
        val n = params["n"]
        if (n != null) {
            params["n"] = StringDecode.nSigDecode(n)
        }
        
        if (cipher != null) {
            params[signatureParam] = StringDecode.sigDecode(signature)
        }
        
        params["c"] = "ANDROID_MUSIC"
        
        return urlBuilderToString(resUrl)
    }
}
