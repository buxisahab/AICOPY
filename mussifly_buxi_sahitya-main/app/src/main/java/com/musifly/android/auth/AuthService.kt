package com.musifly.android.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Authentication Service for Google Sign-In with Firebase
 * Handles Google authentication and Firebase user management
 */
class AuthService private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "AuthService"
        
        @Volatile
        private var instance: AuthService? = null
        
        fun getInstance(context: Context): AuthService {
            return instance ?: synchronized(this) {
                instance ?: AuthService(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    
    // Google Sign-In Client - will be initialized when Firebase is configured
    private var googleSignInClient: GoogleSignInClient? = null
    
    /**
     * Initialize Google Sign-In Client with Web Client ID
     * This must be called after Firebase is initialized
     * 
     * @param webClientId The Firebase Web Client ID from google-services.json
     */
    fun initialize(webClientId: String) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
        Timber.tag(TAG).d("Google Sign-In initialized")
    }
    
    /**
     * Get current Firebase user
     */
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser
    
    /**
     * Check if user is signed in
     */
    fun isSignedIn(): Boolean = firebaseAuth.currentUser != null
    
    /**
     * Sign in with Google
     * Returns the Firebase user on success, null on failure or cancellation
     */
    suspend fun signInWithGoogle(): FirebaseUser? {
        return try {
            val client = googleSignInClient
                ?: throw IllegalStateException("Google Sign-In not initialized. Call initialize() first.")
            
            // Launch Google Sign-In
            val account = client.signInIntent
            // Note: The actual sign-in intent should be handled in Activity/Fragment
            // This method should be called from an Activity context that can startActivityForResult
            Timber.tag(TAG).w("signInWithGoogle() called but requires Activity context for intent")
            null
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during Google Sign-In")
            null
        }
    }
    
    /**
     * Handle Google Sign-In result from Activity
     * Call this from onActivityResult or Activity Result API
     */
    suspend fun handleSignInResult(account: GoogleSignInAccount?): FirebaseUser? {
        return try {
            if (account == null) {
                Timber.tag(TAG).d("Google Sign-In cancelled or failed")
                return null
            }
            
            // Get ID token from Google account
            val idToken = account.idToken
                ?: throw IllegalStateException("ID token is null")
            
            // Create Firebase credential
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            
            // Sign in to Firebase
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
            
            Timber.tag(TAG).d("Sign in successful: ${user?.email}")
            user
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error signing in to Firebase")
            null
        }
    }
    
    /**
     * Sign out from Firebase and Google
     */
    suspend fun signOut() {
        try {
            firebaseAuth.signOut()
            googleSignInClient?.signOut()?.await()
            Timber.tag(TAG).d("Signed out successfully")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error signing out")
        }
    }
    
    /**
     * Get Google Sign-In Intent
     * Use this to start the Google Sign-In flow
     */
    fun getSignInIntent(): android.content.Intent? {
        return googleSignInClient?.signInIntent
    }
}
