package com.buxi.music.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buxi.music.auth.AuthManager
import com.buxi.music.data.remote.ProfileRepository
import com.buxi.music.data.remote.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Loaded(val profile: UserProfile) : ProfileUiState()
    data class Error(val msg: String) : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val auth: AuthManager
) : ViewModel() {

    val currentUid: String? get() = auth.getUserId()

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private val _followers = MutableStateFlow<List<UserProfile>>(emptyList())
    val followers: StateFlow<List<UserProfile>> = _followers.asStateFlow()

    private val _following = MutableStateFlow<List<UserProfile>>(emptyList())
    val following: StateFlow<List<UserProfile>> = _following.asStateFlow()

    private val _isFollowTarget = MutableStateFlow(false)
    val isFollowTarget: StateFlow<Boolean> = _isFollowTarget.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _toast = MutableSharedFlow<String>()
    val toast: SharedFlow<String> = _toast.asSharedFlow()

    // Load own profile (or target uid for viewing other users)
    fun loadProfile(targetUid: String? = null) {
        val uid = targetUid ?: currentUid ?: return
        viewModelScope.launch {
            try {
                _state.value = ProfileUiState.Loading
                val profile = repo.fetchProfile(uid)
                _state.value = ProfileUiState.Loaded(profile!!)
                
                // Load followers and following with error handling
                try {
                    _followers.value = repo.fetchFollowers(uid)
                } catch (e: Exception) {
                    // Keep empty list on error
                }
                
                try {
                    _following.value = repo.fetchFollowing(uid)
                } catch (e: Exception) {
                    // Keep empty list on error
                }
                
            } catch (e: Exception) {
                _state.value = ProfileUiState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    // Called after Firebase login/signup to sync profile into Supabase
    fun ensureProfileSynced() {
        val uid   = auth.getUserId()       ?: return
        val email = auth.getUserEmail()
        val name  = auth.getUserDisplayName()
        val photo = auth.getUserPhotoUrl()
        viewModelScope.launch {
            val p = repo.upsertProfile(uid, email, name, photo)
            p?.let { _state.value = ProfileUiState.Loaded(it) }
        }
    }

    fun loadSocialCounts(uid: String? = null) {
        val target = uid ?: currentUid ?: return
        viewModelScope.launch {
            try {
                _followers.value = repo.fetchFollowers(target)
                _following.value = repo.fetchFollowing(target)
            } catch (e: Exception) {
                // Ignore errors to prevent crash
            }
        }
    }

    fun checkFollowState(targetUid: String) {
        val me = currentUid ?: return
        viewModelScope.launch {
            _isFollowTarget.value = repo.isFollowing(me, targetUid)
        }
    }

    fun toggleFollow(targetUid: String) {
        val me = currentUid ?: return
        viewModelScope.launch {
            val currently = _isFollowTarget.value
            val ok = if (currently) repo.unfollow(me, targetUid)
                     else repo.follow(me, targetUid)
            if (ok) {
                _isFollowTarget.value = !currently
                loadSocialCounts(); loadSocialCounts(targetUid)
            }
        }
    }

    // Edit profile
    fun saveProfile(displayName: String, bio: String) {
        val uid = currentUid ?: return
        if (displayName.isBlank()) { viewModelScope.launch { _toast.emit("Name cannot be empty") }; return }
        viewModelScope.launch {
            _isSaving.value = true
            val updated = repo.updateProfile(uid, displayName.trim(), bio.trim())
            if (updated != null) {
                _state.value = ProfileUiState.Loaded(updated)
                _toast.emit("Profile saved ✓")
            } else _toast.emit("Failed to save profile")
            _isSaving.value = false
        }
    }

    fun uploadAvatar(imageUri: Uri) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _isSaving.value = true
            val url = repo.uploadAvatar(uid, imageUri)
            if (url != null) {
                repo.updateAvatarUrl(uid, url)
                loadProfile()
                _toast.emit("Avatar updated ✓")
            } else _toast.emit("Failed to upload image")
            _isSaving.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
