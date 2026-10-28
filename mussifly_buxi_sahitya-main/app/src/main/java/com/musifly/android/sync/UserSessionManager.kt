package com.musifly.android.sync

import com.google.firebase.auth.FirebaseAuth
import com.musifly.android.auth.AuthService

/**
 * Centralized session manager for user login state
 * Provides consistent access to user authentication status across the app
 */
object UserSessionManager {
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Check if user is in guest mode (not logged in)
     */
    fun isGuest(): Boolean {
        return auth.currentUser == null
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * Get current user ID
     * Returns null if user is not logged in
     */
    fun getUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Get current user email
     * Returns null if user is not logged in
     */
    fun getUserEmail(): String? {
        return auth.currentUser?.email
    }
    
    /**
     * Get current user display name
     * Returns null if user is not logged in
     */
    fun getUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }
}

