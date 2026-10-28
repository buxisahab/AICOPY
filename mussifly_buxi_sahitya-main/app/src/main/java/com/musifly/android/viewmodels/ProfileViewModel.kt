package com.musifly.android.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musifly.android.repository.ProfileRepository
import com.musifly.android.sync.supabase.SupabaseProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _uploadSuccess = MutableStateFlow(false)
    val uploadSuccess: StateFlow<Boolean> = _uploadSuccess
    
    private val _usernameAvailability = MutableStateFlow<Boolean?>(null) 
    val usernameAvailability: StateFlow<Boolean?> = _usernameAvailability

    private val _searchResults = MutableStateFlow<List<SupabaseProfile>>(emptyList())
    val searchResults: StateFlow<List<SupabaseProfile>> = _searchResults
    
    private val _currentProfile = MutableStateFlow<SupabaseProfile?>(null)
    val currentProfile: StateFlow<SupabaseProfile?> = _currentProfile

    private val _viewedProfile = MutableStateFlow<SupabaseProfile?>(null)
    val viewedProfile: StateFlow<SupabaseProfile?> = _viewedProfile

    data class UserStats(
        val followersCount: Long = 0,
        val followingCount: Long = 0
    )

    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing

    private val _usersList = MutableStateFlow<List<SupabaseProfile>>(emptyList())
    val usersList: StateFlow<List<SupabaseProfile>> = _usersList

    private val _notifications = MutableStateFlow<List<com.musifly.android.sync.supabase.SupabaseNotificationWithActor>>(emptyList())
    val notifications: StateFlow<List<com.musifly.android.sync.supabase.SupabaseNotificationWithActor>> = _notifications

    private val _followStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val followStates: StateFlow<Map<String, Boolean>> = _followStates

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState

    sealed class UserListItem {
        data class Header(val title: String) : UserListItem()
        data class User(val profile: SupabaseProfile, val followerId: String) : UserListItem()
    }

    private val _categorizedUsersList = MutableStateFlow<List<UserListItem>>(emptyList())
    val categorizedUsersList: StateFlow<List<UserListItem>> = _categorizedUsersList

    init {
        observeLocalProfile()
        loadCurrentProfile()
    }

    private fun observeLocalProfile() {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                profileRepository.getLocalProfile(user.uid).collectLatest { local ->
                    if (local != null) {
                        // Update basic info
                        _currentProfile.value = SupabaseProfile(
                            id = local.id,
                            username = local.username,
                            fullName = local.fullName,
                            avatarUrl = local.avatarUrl,
                            bio = local.bio
                        )
                        // Update stats
                        _userStats.value = UserStats(
                            followersCount = local.followersCount,
                            followingCount = local.followingCount
                        )
                        // Once we have local data, we're not "loading" the initial screen anymore
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    fun loadCurrentProfile() {
        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    // Start sync in background, UI will update via Flow
                    profileRepository.syncProfile(user.uid)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading current profile")
            } finally {
                // If we get here and still "loading" (e.g. no local data), stop loading
                _isLoading.value = false
            }
        }
    }

    fun checkUsername(username: String) {
        viewModelScope.launch {
            if (username.length < 3) {
                _usernameAvailability.value = null
                return@launch
            }
            
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val profile = _currentProfile.value ?: try { profileRepository.getProfile(currentUser.uid) } catch (e: Exception) { null }
                if (profile?.username == username) {
                    _usernameAvailability.value = true // It's available to US
                    return@launch
                }
            }


            // Setloading/checking state before network call
            _usernameAvailability.value = null
            val available = profileRepository.isUsernameAvailable(username)
            _usernameAvailability.value = available
        }
    }
    
    fun searchUsers(query: String) {
        viewModelScope.launch {
            if (FirebaseAuth.getInstance().currentUser == null) {
                _searchResults.value = emptyList()
                return@launch
            }
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            val results = profileRepository.searchUsers(query)
            _searchResults.value = results
        }
    }



    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _viewedProfile.value = null 
            try {
                // For other profiles, we still fetch directly but could also cache
                _viewedProfile.value = profileRepository.getProfile(userId)
                loadUserStats(userId)
            } catch (e: Exception) {
                _errorMessage.value = e.message
                Timber.tag("ProfileViewModel").e(e, "Error loading profile")
            } finally {
                _isLoading.value = false
            }
        }
    }




    fun loadUserStats(userId: String) {
        viewModelScope.launch {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val isSelf = userId == currentUserId
            
            // Do not reset stats to null if we already have local data (prevents "zero on weak network")
            if (_userStats.value == null || !isSelf) {
                _isLoading.value = true
            }
            
            try {
                _isFollowing.value = false

                val followers = profileRepository.getFollowersCount(userId)
                val following = profileRepository.getFollowingCount(userId)
                _userStats.value = UserStats(followers, following)
                
                // Check if current user is following this user
                 val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                 if (currentUserId != null && currentUserId != userId) {
                     _isFollowing.value = profileRepository.isFollowing(userId)
                 }
            } catch (e: Exception) {
                Timber.e(e, "Error loading user stats")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch {
            try {
                if (_isFollowing.value) {
                    val result = profileRepository.unfollowUser(targetUserId)
                    result.onSuccess {
                        _isFollowing.value = false
                        // Update stats locally for immediate feedback
                        _userStats.value = _userStats.value?.let { 
                            it.copy(followersCount = (it.followersCount - 1).coerceAtLeast(0)) 
                        }
                    }.onFailure { e ->
                        _errorMessage.value = "Failed to unfollow: ${e.message}"
                        Timber.tag("ProfileViewModel").e(e, "Unfollow failed")
                    }
                } else {
                    val result = profileRepository.followUser(targetUserId)
                    result.onSuccess {
                        _isFollowing.value = true
                        // Update stats locally
                        _userStats.value = _userStats.value?.let { 
                            it.copy(followersCount = it.followersCount + 1) 
                        }
                        
                        // Notification is now handled by Supabase database trigger for robustness
                    }.onFailure { e ->
                        _errorMessage.value = "Failed to follow: ${e.message}"
                        Timber.tag("ProfileViewModel").e(e, "Follow failed")
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Unexpected error: ${e.message}"
                Timber.tag("ProfileViewModel").e(e, "Exception in toggleFollow")
            }
        }
    }


    fun getFollowersList(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (FirebaseAuth.getInstance().currentUser == null) {
                    _categorizedUsersList.value = emptyList()
                    return@launch
                }
                
                val followersWithProfile = profileRepository.getFollowers(userId)
                val categorized = groupUsersByTime(followersWithProfile.map { 
                    it.profile to it.createdAt 
                })
                
                _categorizedUsersList.value = categorized
                
                // Fetch follow status for these users
                val userIds = followersWithProfile.mapNotNull { it.profile?.id }
                if (userIds.isNotEmpty()) {
                    _followStates.value = _followStates.value + profileRepository.getFollowStates(userIds)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading followers list")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun groupUsersByTime(users: List<Pair<SupabaseProfile?, String?>>): List<UserListItem> {
        val today = mutableListOf<SupabaseProfile>()
        val thisWeek = mutableListOf<SupabaseProfile>()
        val earlier = mutableListOf<SupabaseProfile>()

        val now = java.time.Instant.now()
        val oneDayAgo = now.minus(1, java.time.temporal.ChronoUnit.DAYS)
        val oneWeekAgo = now.minus(7, java.time.temporal.ChronoUnit.DAYS)

        users.forEach { (profile, createdAt) ->
            if (profile != null) {
                val createdInstant = try {
                    java.time.Instant.parse(createdAt)
                } catch (e: Exception) {
                    java.time.Instant.MIN
                }

                when {
                    createdInstant.isAfter(oneDayAgo) -> today.add(profile)
                    createdInstant.isAfter(oneWeekAgo) -> thisWeek.add(profile)
                    else -> earlier.add(profile)
                }
            }
        }

        val result = mutableListOf<UserListItem>()
        if (today.isNotEmpty()) {
            result.add(UserListItem.Header("Today"))
            result.addAll(today.map { UserListItem.User(it, it.id) })
        }
        if (thisWeek.isNotEmpty()) {
            result.add(UserListItem.Header("This Week"))
            result.addAll(thisWeek.map { UserListItem.User(it, it.id) })
        }
        if (earlier.isNotEmpty()) {
            if (result.isNotEmpty()) result.add(UserListItem.Header("Earlier"))
            result.addAll(earlier.map { UserListItem.User(it, it.id) })
        }
        return result
    }

    fun removeFollower(followerId: String, targetUserId: String) {
        viewModelScope.launch {
            try {
                val result = profileRepository.removeFollower(followerId, targetUserId)
                if (result.isSuccess) {
                    // Update list locally
                    _usersList.value = _usersList.value.filter { it.id != followerId }
                    // Update stats
                    _userStats.value = _userStats.value?.let { 
                        it.copy(followersCount = (it.followersCount - 1).coerceAtLeast(0)) 
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error removing follower")
            }
        }
    }

    fun getFollowingList(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (FirebaseAuth.getInstance().currentUser == null) {
                    _categorizedUsersList.value = emptyList()
                    return@launch
                }
                
                val followingWithProfile = profileRepository.getFollowing(userId)
                val categorized = groupUsersByTime(followingWithProfile.map { 
                    it.profile to it.createdAt 
                })
                
                _categorizedUsersList.value = categorized
                
                val userIds = followingWithProfile.mapNotNull { it.profile?.id }
                if (userIds.isNotEmpty()) {
                    _followStates.value = _followStates.value + profileRepository.getFollowStates(userIds)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading following list")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFollowInList(targetUserId: String) {
        viewModelScope.launch {
            toggleFollowFromNotification(targetUserId)
            // If we are looking at our own stats, update them
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (_userStats.value != null && currentUserId != null) {
                // If we are looking at someone else's profile, toggleFollow already updates it.
                // This function is for list items. 
            }
        }
    }


    fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetchedNotifications = profileRepository.getNotifications()
                
                // Deduplicate by actor_id and type to avoid spamming "followed/unfollowed"
                // Keep only the most recent notification for each actor-type combo
                val deduplicated = fetchedNotifications
                    .distinctBy { "${it.actorId}_${it.type}" }
                
                _notifications.value = deduplicated
                
                // Load follow states for all actors in the list efficiently
                val actorIds = deduplicated.mapNotNull { it.actorId }.distinct()
                _followStates.value = profileRepository.getFollowStates(actorIds)
                
            } catch (e: Exception) {
                Timber.tag("ProfileViewModel").e(e, "Failed to load notifications")
            } finally {
                _isLoading.value = false
            }
        }
    }

    val hasUnreadNotifications: StateFlow<Boolean> = _notifications.map { notifications ->
        notifications.any { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val unreadNotificationsCount: StateFlow<Int> = _notifications.map { notifications ->
        notifications.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)


    fun toggleFollowFromNotification(targetUserId: String) {
        viewModelScope.launch {
            val currentlyFollowing = _followStates.value[targetUserId] ?: false
            try {
                if (currentlyFollowing) {
                    val result = profileRepository.unfollowUser(targetUserId)
                    result.onSuccess {
                        _followStates.value = _followStates.value + (targetUserId to false)
                    }
                } else {
                    val result = profileRepository.followUser(targetUserId)
                    result.onSuccess {
                        _followStates.value = _followStates.value + (targetUserId to true)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling follow from notification")
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            _notifications.value.filter { !it.isRead && it.id != null }.forEach { 
                profileRepository.markNotificationAsRead(it.id!!)
            }
            _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        }
    }

    fun markAsRead(notificationId: Long?) {
        if (notificationId == null) return
        viewModelScope.launch {
            profileRepository.markNotificationAsRead(notificationId)
            // Optimistically update list
            _notifications.value = _notifications.value.map {
                if (it.id == notificationId) it.copy(isRead = true) else it
            }
        }
    }

    fun deleteNotification(notificationId: Long?) {
        if (notificationId == null) return
        viewModelScope.launch {
            // Optimistically remove from list
            _notifications.value = _notifications.value.filter { it.id != notificationId }
            profileRepository.deleteNotification(notificationId)
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            _notifications.value = emptyList()
            profileRepository.clearAllNotifications()
        }
    }

    fun syncFcmToken() {
        viewModelScope.launch {
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Timber.tag("ProfileViewModel").w(task.exception, "Fetching FCM registration token failed")
                            return@addOnCompleteListener
                        }
                        val token = task.result
                        Timber.tag("ProfileViewModel").d("FCM Token: $token")
                        viewModelScope.launch {
                            profileRepository.updateFcmToken(token)
                        }
                    }
            } catch (e: Exception) {
                Timber.tag("ProfileViewModel").e(e, "Error syncing FCM token")
            }
        }
    }


    fun deleteAccount() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _deleteSuccess.value = false
            
            val result = profileRepository.deleteAccount()
            
            result.onSuccess {
                _deleteSuccess.value = true
                Timber.tag("ProfileViewModel").d("Account deleted successfully")
            }.onFailure { e ->
                _errorMessage.value = e.message
                Timber.tag("ProfileViewModel").e(e, "Failed to delete account")
            }
            
            _isLoading.value = false
        }
    }


    fun updateProfile(username: String?, fullName: String?, bio: String?, imageUri: Uri?) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            _isLoading.value = true
            
            val result = profileRepository.updateProfile(username, fullName, bio, imageUri)
            
            result.onSuccess {
                _profileState.value = ProfileState.Success
                _uploadSuccess.value = true
                loadCurrentProfile()
            }.onFailure { e ->
                _profileState.value = ProfileState.Error(e.message ?: "Unknown error")
                _errorMessage.value = e.message
            }
            _isLoading.value = false
        }
    }
    fun resetState() {
        _errorMessage.value = null
        _uploadSuccess.value = false
        _deleteSuccess.value = false
        _profileState.value = ProfileState.Idle
        _usernameAvailability.value = null
        _searchResults.value = emptyList()
    }
}

sealed interface ProfileState {
    data object Idle : ProfileState
    data object Loading : ProfileState
    data object Success : ProfileState
    data class Error(val message: String) : ProfileState
}
