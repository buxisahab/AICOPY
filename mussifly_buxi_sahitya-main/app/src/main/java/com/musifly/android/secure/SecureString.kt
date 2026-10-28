package com.musifly.android.secure

object SecureString {
    init {
        System.loadLibrary("musifly-secure")
    }

    // Obfuscated methods - intent is hidden
    
    // Direct action methods - String never returned to Java
    external fun openLink(context: android.content.Context, type: Int)
    
    // Only xy4 (Update URL) returns string because HttpFetcher needs it
    external fun xy4(): String 

    // Constants for types
    const val LINK_INSTA_APP = 1
    const val LINK_INSTA_WEB = 2
    const val LINK_TELEGRAM = 3
    const val LINK_UPDATE = 4 // Handled by xy4
    const val LINK_PAYPAL_COM = 5
    const val LINK_PAYPAL_ME = 6
    const val LINK_UPI_PAY = 7
    const val LINK_UPI_PAY_2 = 8
    const val LINK_UPI_PAY_3 = 9
    const val LINK_UPI_PAY_4 = 10
    const val LINK_OFFICIAL_WEBSITE = 11
    const val LINK_GITHUB_AVATAR = 12
    const val APP_NAME = 13
    const val TAGLINE = 14
    const val POWERED_BY = 15
    const val SUPABASE_URL = 16
    const val SUPABASE_KEY = 17
    const val FIREBASE_APP_ID = 18
    const val FIREBASE_API_KEY = 19
    const val FIREBASE_URL = 20
    const val FIREBASE_STORAGE_BUCKET = 21
    const val FIREBASE_PROJECT_ID = 22
    const val FIREBASE_WEB_CLIENT_ID = 23
    
    // Generic string getter
    external fun s(type: Int): String

    // Old methods removed to prevent Smali return type exposure
    // xy1, xy2, xy3, xy5, xy6 are gone. Use openLink(context, TYPE) instead.

    /**
     * Checks if an update is needed based on versions.
     * Moved to native for security.
     */
    external fun check(
        installedVersionCode: Int,
        installedVersionName: String,
        latestVersionCode: Int,
        latestVersionName: String,
        lastNotifiedVersionCode: Int,
        forceShow: Boolean
    ): Boolean
}
