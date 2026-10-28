package com.buxi.music.ui.screens.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buxi.music.auth.AuthManager
import com.buxi.music.auth.AuthResult
import com.buxi.music.auth.AuthState
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    
    val authState: StateFlow<AuthState> = authManager.authState
    
    fun getGoogleSignInIntent(): Intent {
        return authManager.getGoogleSignInClient().signInIntent
    }
    
    suspend fun signInWithGoogle(account: GoogleSignInAccount): AuthResult {
        return authManager.signInWithGoogle(account)
    }
    
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return authManager.signInWithEmail(email, password)
    }
    
    suspend fun signUpWithEmail(email: String, password: String, displayName: String? = null): AuthResult {
        return authManager.signUpWithEmail(email, password, displayName)
    }
    
    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }
    
    fun isLoggedIn(): Boolean = authManager.isLoggedIn()
}
