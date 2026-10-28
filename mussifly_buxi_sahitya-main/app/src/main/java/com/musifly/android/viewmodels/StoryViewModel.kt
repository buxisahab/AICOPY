package com.musifly.android.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.android.models.UserStoryGroup
import com.musifly.android.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val storyRepository: StoryRepository,
    private val profileRepository: com.musifly.android.repository.ProfileRepository
) : ViewModel() {

    val stories = storyRepository.stories
    
    private val _currentUser = MutableStateFlow<com.musifly.android.db.entities.LocalProfileEntity?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val isUploading = storyRepository.isUploading

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
            loadStories()
            viewModelScope.launch {
                profileRepository.getLocalProfile(user.uid).collect {
                    _currentUser.value = it
                }
            }
        }
        // No need to clear local stories here, Repository handles it
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        FirebaseAuth.getInstance().currentUser?.let { user ->
            viewModelScope.launch {
                profileRepository.getLocalProfile(user.uid).collect {
                    _currentUser.value = it
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }

    fun loadStories() {
        viewModelScope.launch {
            _isRefreshing.value = true
            storyRepository.loadStories()
            _isRefreshing.value = false
        }
    }

    fun uploadStory(
        uri: Uri, 
        caption: String? = null,
        duration: Long? = null,
        musicId: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ) {
        viewModelScope.launch {
            val result = storyRepository.uploadStory(uri, caption, duration, musicId, startTime, endTime)
            if (!result.isSuccess) {
                Timber.e(result.exceptionOrNull(), "Story upload failed")
            }
        }
    }

    fun deleteStory(storyId: String, mediaUrl: String) {
        viewModelScope.launch {
            storyRepository.deleteStory(storyId, mediaUrl)
        }
    }

    fun viewStory(storyId: String) {
        viewModelScope.launch {
            storyRepository.viewStory(storyId)
        }
    }

    suspend fun getStoryViewers(storyId: String): List<com.musifly.android.sync.supabase.SupabaseStoryView> {
        return storyRepository.getStoryViewers(storyId)
    }
}
