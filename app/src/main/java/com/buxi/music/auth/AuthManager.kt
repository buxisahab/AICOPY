package com.buxi.music.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class AuthResult {
    data class Success(val user: FirebaseUser?) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    private var googleSignInClient: GoogleSignInClient? = null

    // Register a callback to sync profile to Supabase after login
    var onAuthSuccess: (() -> Unit)? = null
    
    init {
        // Initialize auth state
        auth.currentUser?.let { user ->
            _authState.value = AuthState.Authenticated(user)
            _currentUser.value = user
        } ?: run {
            _authState.value = AuthState.Unauthenticated
        }
        
        // Listen for auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            firebaseAuth.currentUser?.let { user ->
                _authState.value = AuthState.Authenticated(user)
                _currentUser.value = user
            } ?: run {
                _authState.value = AuthState.Unauthenticated
                _currentUser.value = null
            }
        }
    }
    
    /**
     * Get Google Sign-In client
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        if (googleSignInClient == null) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("168164058701-rgfhdm5jsll9b2tgki3v04s4bm1dd2tv.apps.googleusercontent.com")
                .requestEmail()
                .build()
            
            googleSignInClient = GoogleSignIn.getClient(context, gso)
        }
        return googleSignInClient!!
    }
    
    /**
     * Sign in with Google
     */
    suspend fun signInWithGoogle(account: GoogleSignInAccount): AuthResult {
        return try {
            _authState.value = AuthState.Loading
            
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            
            result.user?.let { user ->
                _authState.value = AuthState.Authenticated(user)
                _currentUser.value = user
                Log.d("AuthManager", "Google sign-in successful: ${user.email}")
                onAuthSuccess?.invoke()   // ← sync to Supabase
                AuthResult.Success(user)
            } ?: run {
                _authState.value = AuthState.Error("Sign-in failed")
                AuthResult.Error("Sign-in failed")
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Google sign-in error", e)
            _authState.value = AuthState.Error(e.message ?: "Unknown error")
            AuthResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            _authState.value = AuthState.Loading
            
            val result = auth.signInWithEmailAndPassword(email, password).await()
            
            result.user?.let { user ->
                _authState.value = AuthState.Authenticated(user)
                _currentUser.value = user
                Log.d("AuthManager", "Email sign-in successful: ${user.email}")
                onAuthSuccess?.invoke()   // ← sync to Supabase
                AuthResult.Success(user)
            } ?: run {
                _authState.value = AuthState.Error("Sign-in failed")
                AuthResult.Error("Sign-in failed")
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Email sign-in error", e)
            val errorMessage = when {
                e.message?.contains("password") == true -> "Invalid password"
                e.message?.contains("email") == true -> "Invalid email"
                e.message?.contains("user") == true -> "User not found"
                else -> e.message ?: "Sign-in failed"
            }
            _authState.value = AuthState.Error(errorMessage)
            AuthResult.Error(errorMessage)
        }
    }
    
    /**
     * Sign up with email and password
     */
    suspend fun signUpWithEmail(email: String, password: String, displayName: String? = null): AuthResult {
        return try {
            _authState.value = AuthState.Loading
            
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            
            result.user?.let { user ->
                // Update display name if provided
                displayName?.let {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(it)
                        .build()
                    user.updateProfile(profileUpdates).await()
                }
                
                _authState.value = AuthState.Authenticated(user)
                _currentUser.value = user
                Log.d("AuthManager", "Email sign-up successful: ${user.email}")
                onAuthSuccess?.invoke()   // ← sync + auto-follow devs
                AuthResult.Success(user)
            } ?: run {
                _authState.value = AuthState.Error("Sign-up failed")
                AuthResult.Error("Sign-up failed")
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Email sign-up error", e)
            val errorMessage = when {
                e.message?.contains("already in use") == true -> "Email already in use"
                e.message?.contains("weak password") == true -> "Password too weak"
                e.message?.contains("invalid email") == true -> "Invalid email format"
                else -> e.message ?: "Sign-up failed"
            }
            _authState.value = AuthState.Error(errorMessage)
            AuthResult.Error(errorMessage)
        }
    }
    
    /**
     * Sign out
     */
    suspend fun signOut() {
        try {
            auth.signOut()
            googleSignInClient?.signOut()?.await()
            _authState.value = AuthState.Unauthenticated
            _currentUser.value = null
            Log.d("AuthManager", "Sign-out successful")
        } catch (e: Exception) {
            Log.e("AuthManager", "Sign-out error", e)
        }
    }
    
    /**
     * Delete account
     */
    suspend fun deleteAccount(): AuthResult {
        return try {
            val user = auth.currentUser ?: return AuthResult.Error("No user logged in")
            user.delete().await()
            _authState.value = AuthState.Unauthenticated
            _currentUser.value = null
            Log.d("AuthManager", "Account deleted successfully")
            AuthResult.Success(user)
        } catch (e: Exception) {
            Log.e("AuthManager", "Account deletion error", e)
            AuthResult.Error(e.message ?: "Failed to delete account")
        }
    }
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Log.d("AuthManager", "Password reset email sent to: $email")
            AuthResult.Success(null)
        } catch (e: Exception) {
            Log.e("AuthManager", "Password reset error", e)
            AuthResult.Error(e.message ?: "Failed to send reset email")
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(displayName: String? = null, photoUrl: String? = null): AuthResult {
        return try {
            val user = auth.currentUser ?: return AuthResult.Error("No user logged in")
            
            val profileUpdates = UserProfileChangeRequest.Builder().apply {
                displayName?.let { setDisplayName(it) }
                photoUrl?.let { setPhotoUri(android.net.Uri.parse(it)) }
            }.build()
            
            user.updateProfile(profileUpdates).await()
            _currentUser.value = user
            Log.d("AuthManager", "Profile updated successfully")
            AuthResult.Success(user)
        } catch (e: Exception) {
            Log.e("AuthManager", "Profile update error", e)
            AuthResult.Error(e.message ?: "Failed to update profile")
        }
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean = auth.currentUser != null
    
    /**
     * Get current user ID
     */
    fun getUserId(): String? = auth.currentUser?.uid
    
    /**
     * Get current user email
     */
    fun getUserEmail(): String? = auth.currentUser?.email
    
    /**
     * Get current user display name
     */
    fun getUserDisplayName(): String? = auth.currentUser?.displayName
    
    /**
     * Get current user photo URL
     */
    fun getUserPhotoUrl(): String? = auth.currentUser?.photoUrl?.toString()
}
