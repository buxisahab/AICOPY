package com.musifly.android.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.musifly.android.LocalDatabase
import com.musifly.android.R
import com.musifly.android.auth.AuthService
import com.musifly.android.ui.component.liquidglass.LiquidGlassCard
import com.musifly.android.ui.component.liquidglass.isLiquidGlassActive
import com.musifly.android.ui.component.shimmer.ShimmerHost
import com.musifly.android.ui.component.shimmer.TextPlaceholder
import com.musifly.android.viewmodels.ProfileViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.musifly.android.LocalPlayerAwareWindowInsets
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeautifulAccountScreen(
    navController: NavController,
    showBackButton: Boolean = true,
    userId: String? = null,
    profileViewModel: ProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val context = LocalContext.current
    val authService = remember { AuthService.getInstance(context) }
    val firebaseAuth = FirebaseAuth.getInstance()
    val currentUser = firebaseAuth.currentUser
    val isLoggedIn = currentUser != null
    
    val database = LocalDatabase.current
    val isLiquidGlass = isLiquidGlassActive()
    
    // Stats State (Local)
    val songCount by remember(database) { database.songsByRowIdAsc().map { it.size } }.collectAsState(initial = 0)
    val playlistCount by remember(database) { database.allEditablePlaylistsByCreateDateAsc().map { it.size } }.collectAsState(initial = 0)
    val artistCount by remember(database) { database.artistsInAA().map { it.size } }.collectAsState(initial = 0)
    val likedSongsCount by remember(database) { database.likedSongsCount() }.collectAsState(initial = 0)
    
    // Recently played
    val recentSongsCount by remember(database) {
        database.events().map { events ->
            events.map { it.song.id }.distinct().size
        }
    }.collectAsState(initial = 0)
    
    // Profile State
    val isProfileLoading by profileViewModel.isLoading.collectAsState()
    val uploadSuccess by profileViewModel.uploadSuccess.collectAsState()
    val uploadError by profileViewModel.errorMessage.collectAsState()
    
    val currentProfileState by profileViewModel.currentProfile.collectAsState()
    val viewedProfileState by profileViewModel.viewedProfile.collectAsState()
    
    // Determine which profile to show
    val currentProfile = if (userId != null) viewedProfileState else currentProfileState
    val isSelf = userId == null || userId == currentUser?.uid
    
    val isFollowing by profileViewModel.isFollowing.collectAsState()
    val userStats by profileViewModel.userStats.collectAsState()
    
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    // Google Sign In & Logout Handlers
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            isLoading = true
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val user = authService.handleSignInResult(account)
                if (user != null) {
                    try {
                        val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.musifly.android.sync.SyncManagerEntryPoint::class.java
                        )
                        val syncManager = com.musifly.android.sync.SyncManager(
                            context.applicationContext,
                            entryPoint.likesRepository(),
                            entryPoint.playlistRepository(),
                            entryPoint.cachedDownloadsRepository(),
                            entryPoint.bookmarksRepository(),
                            entryPoint.migrationService()
                        )
                        syncManager.syncAllUserData(scope) { }
                        profileViewModel.loadCurrentProfile()
                    } catch (e: Exception) {
                        Timber.tag("BeautifulAccountScreen").e(e, "Sync failed")
                    }
                    navController.navigateUp()
                } else {
                    errorMessage = "Sign in failed"
                }
            } catch (e: ApiException) {
                errorMessage = "Sign in failed"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun handleGoogleSignIn() {
        val intent = authService.getSignInIntent()
        if (intent != null) googleSignInLauncher.launch(intent)
    }
    
    fun handleLogout() {
        scope.launch(Dispatchers.IO) {
            try {
                isLoading = true
                val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(context.applicationContext, com.musifly.android.sync.SyncManagerEntryPoint::class.java)
                entryPoint.likesRepository().clearUserData()
                entryPoint.playlistRepository().clearUserData()
                entryPoint.bookmarksRepository().clearUserData()
                entryPoint.cachedDownloadsRepository().clearUserData()
                database.clearListenHistory()
                database.clearSearchHistory()
                authService.signOut()
                withContext(Dispatchers.Main) { navController.navigateUp() }
            } catch (e: Exception) {
                Timber.e(e, "Logout failed")
            } finally {
                isLoading = false
            }
        }
    }

    // Load Data
    LaunchedEffect(userId) {
        if (userId != null) {
            profileViewModel.loadProfile(userId)
        } else if (isLoggedIn) {
            profileViewModel.loadCurrentProfile()
            currentUser?.uid?.let { profileViewModel.loadUserStats(it) }
        }
    }
    
    val deleteSuccess by profileViewModel.deleteSuccess.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Dialog & Error Effects
    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            showEditProfileDialog = false
            profileViewModel.resetState()
            android.widget.Toast.makeText(context, "Profile updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    LaunchedEffect(deleteSuccess) {
        if (deleteSuccess) {
             android.widget.Toast.makeText(context, "Account deleted successfully", android.widget.Toast.LENGTH_LONG).show()
             // Clear local data and logout using existing logic
             handleLogout()
        }
    }

    LaunchedEffect(uploadError) {
        uploadError?.let {
            android.widget.Toast.makeText(context, "Error: $it", android.widget.Toast.LENGTH_LONG).show()
            profileViewModel.resetState()
        }
    }

    // Pull to Refresh State
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = isProfileLoading // Use profile loading state for refresh indicator
    
    val onRefresh: () -> Unit = {
        scope.launch {
            if (userId != null) {
                profileViewModel.loadProfile(userId)
            } else if (isLoggedIn) {
                profileViewModel.loadCurrentProfile()
                currentUser?.uid?.let { profileViewModel.loadUserStats(it) }
            }
        }
    }

    // --- UI Structure ---
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isSelf && !isLoggedIn) {
                                "Guest"
                            } else if (isProfileLoading && currentProfile?.username == null) {
                                "" // Handled by shimmer if we want, but TopAppBar title is tricky
                            } else {
                                currentProfile?.username ?: "User"
                            },
                             style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (isProfileLoading && currentProfile?.username == null) {
                            ShimmerHost(modifier = Modifier.width(100.dp).height(24.dp)) {
                                TextPlaceholder(height = 20.dp)
                            }
                        }
                        if (currentProfile?.username != null) { 
                             // Spacer(Modifier.width(4.dp))
                             // Icon(Icons.Default.CheckCircle, null, tint = Blue, modifier = Modifier.size(16.dp)) 
                        }
                    }
                },
                navigationIcon = {
                    if (showBackButton) {
                        com.musifly.android.ui.component.AppIconButton(
                            onClick = { navController.navigateUp() },
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    com.musifly.android.ui.component.AppIconButton(
                        onClick = { if (isSelf) navController.navigate("settings") },
                        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Default.MoreVert),
                        contentDescription = "Menu"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullToRefresh(
                    isRefreshing = isRefreshing,
                    state = pullToRefreshState,
                    onRefresh = onRefresh
                )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Header Row (Avatar + Stats)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        val photoUrl = if (isSelf) {
                             if(isLoggedIn) (currentProfile?.avatarUrl ?: currentUser?.photoUrl) else null
                        } else {
                            currentProfile?.avatarUrl
                        }
                        
                        if (isProfileLoading && photoUrl == null) {
                            ShimmerHost(
                                modifier = Modifier
                                    .size(86.dp)
                                    .clip(CircleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                ) {
                                    if (photoUrl != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(photoUrl)
                                                .allowHardware(false)
                                                .build(),
                                            contentDescription = "Profile",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(R.drawable.person),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().padding(16.dp),
                                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                                // Plus icon for story (optional, if self)
                                if (isSelf && isLoggedIn) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(24.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        // Stats
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Playlists/Posts
                            if (isProfileLoading && userStats == null && isSelf) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    ShimmerHost(modifier = Modifier.width(40.dp).height(20.dp)) { Box(modifier = Modifier.fillMaxSize().background(Color.Gray)) }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Posts", fontSize = 12.sp)
                                }
                            } else {
                                StatColumn(
                                    count = if (isSelf) playlistCount.toString() else "0", 
                                    label = "Playlists" // Mapped "Posts" -> "Playlists"
                                )
                            }
                            
                            // Followers
                            val effectiveUserId = userId ?: currentUser?.uid
                            
                            if (isProfileLoading && userStats == null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    ShimmerHost(modifier = Modifier.width(40.dp).height(20.dp)) { Box(modifier = Modifier.fillMaxSize().background(Color.Gray)) }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Followers", fontSize = 12.sp)
                                }
                            } else {
                                StatColumn(
                                    count = (userStats?.followersCount ?: 0).toString(),
                                    label = "Followers",
                                    onClick = { 
                                        if ((isLoggedIn || userId != null) && effectiveUserId != null) {
                                            navController.navigate("user_list/$effectiveUserId/true")
                                        }
                                    }
                                )
                            }

                            // Following
                            if (isProfileLoading && userStats == null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    ShimmerHost(modifier = Modifier.width(40.dp).height(20.dp)) { Box(modifier = Modifier.fillMaxSize().background(Color.Gray)) }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Following", fontSize = 12.sp)
                                }
                            } else {
                                StatColumn(
                                    count = (userStats?.followingCount ?: 0).toString(),
                                    label = "Following",
                                    onClick = { 
                                        if ((isLoggedIn || userId != null) && effectiveUserId != null) {
                                            navController.navigate("user_list/$effectiveUserId/false")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // 2. Bio Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        if (isProfileLoading && currentProfile?.fullName == null) {
                            ShimmerHost(modifier = Modifier.width(150.dp).height(20.dp)) {
                                TextPlaceholder(height = 14.sp.value.dp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            ShimmerHost(modifier = Modifier.fillMaxWidth(0.7f).height(40.dp)) {
                                TextPlaceholder(height = 14.sp.value.dp)
                                Spacer(modifier = Modifier.height(4.dp))
                                TextPlaceholder(height = 14.sp.value.dp)
                            }
                        } else {
                            Text(
                                text = if (isLoggedIn || userId != null) (currentProfile?.fullName ?: "User") else "Guest User",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (!currentProfile?.bio.isNullOrBlank()) {
                                Text(
                                    text = currentProfile!!.bio!!,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        // Default Bio Link (Placeholder)
                       // Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                       //     Icon(painterResource(R.drawable.link), null, modifier = Modifier.size(14.dp).rotate(-45f))
                       //     Text(" m.youtube.com/...", color = Color(0xFF00376B))
                       // }
                    }
                    
                    // 3. Buttons Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSelf) {
                            if (isLoggedIn) {
                                ProfileButton(
                                    text = "Edit profile",
                                    modifier = Modifier.weight(1f),
                                    onClick = { showEditProfileDialog = true }
                                )
                                ProfileButton(
                                    text = "Share profile",
                                    modifier = Modifier.weight(1f),
                                    onClick = { /* Share intent */ }
                                )
                            } else {
                                Button(
                                    onClick = { handleGoogleSignIn() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Sign in with Google")
                                }
                            }
                        } else {
                            // Follow Button
                            val isFollowingUser = isFollowing
                            val officialAccounts = listOf("Qev4DqlLiGeVXhIhBLaQWmHOmjq2", "RSdUGiWPitOuq6HXHL7lPXKGQwi1")
                            val isOfficialAccount = userId in officialAccounts
                            
                            Button(
                                onClick = { 
                                    if (isLoggedIn) {
                                        if (isOfficialAccount && isFollowingUser) {
                                            android.widget.Toast.makeText(context, "You cannot unfollow official accounts", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            userId?.let { profileViewModel.toggleFollow(it) } 
                                        }
                                    } else {
                                        android.widget.Toast.makeText(context, "Please login to follow", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !(isOfficialAccount && isFollowingUser), // Optional: visually disable it too
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowingUser) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isFollowingUser) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (isFollowingUser) "Following" else "Follow",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (isFollowingUser) {
                                    Icon(androidx.compose.material.icons.Icons.Default.KeyboardArrowDown, null)
                                }
                            }
                            

                        }
                    }
                    
                    if (showEditProfileDialog && isLoggedIn) {
                        EditProfileDialog(
                            initialFullName = currentProfile?.fullName ?: currentUser?.displayName ?: "",
                            initialUsername = currentProfile?.username ?: "",
                            initialBio = currentProfile?.bio ?: "",
                            email = currentUser?.email,
                            currentPhotoUrl = currentProfile?.avatarUrl ?: currentUser?.photoUrl?.toString(),
                            onDismiss = { showEditProfileDialog = false },
                            onSave = { fullName, username, bio, uri ->
                                profileViewModel.updateProfile(username.takeIf { it.isNotBlank() }, fullName.takeIf { it.isNotBlank() }, bio, uri)
                            },
                            isProfileLoading = isProfileLoading,
                            profileViewModel = profileViewModel
                        )
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Delete Account") },
                            text = { 
                                Text("Are you sure you want to permanently delete your account? This action cannot be undone and all your data (playlists, history, etc.) will be lost.") 
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDeleteDialog = false
                                        profileViewModel.deleteAccount()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // 4. Content Area (Menu Cards from previous implementation, styled simpler)
                    if (isSelf) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        
                        // List of items
                        SimpleMenuItem(R.drawable.favorite, "Liked Songs", "$likedSongsCount songs") { navController.navigate("liked_songs") }
                        SimpleMenuItem(R.drawable.history, "Recently Played", "$recentSongsCount songs") { navController.navigate("history") }
                        SimpleMenuItem(R.drawable.settings, "Settings", "") { navController.navigate("settings") }
                        SimpleMenuItem(R.drawable.info, "About", "") { navController.navigate("settings/about") }
                        SimpleMenuItem(R.drawable.group, "Community", "") { navController.navigate("settings/community") }
                        if (isLoggedIn) {
                             SimpleMenuItem(R.drawable.logout, "Logout", "Sign out") { handleLogout() }
                             SimpleMenuItem(R.drawable.delete, "Delete Account", "Permanently delete account") { showDeleteDialog = true }
                        }
                    } else {
                        // Viewing other profile
                        HorizontalDivider(modifier = Modifier.padding(top=12.dp))
                        
                        // If we had their playlists, we'd show grid here.
                        // For now, show empty state or "No playlists yet"
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No posts yet", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }
                        }
                    }

                    // Add dynamic bottom padding to account for the navigation bar
                    val playerAwareInsets: WindowInsets = LocalPlayerAwareWindowInsets.current
                    val density = LocalDensity.current
                    val bottomPadding = with(density) { 
                        playerAwareInsets.getBottom(this).toDp() 
                    }
                    Spacer(Modifier.height(bottomPadding + 16.dp)) // Extra 16dp for breathing room
                }
            }

            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun StatColumn(count: String, label: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Text(
            text = count,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun ProfileButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant, // Gray
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant // Black/White
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp) // Compact
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SimpleMenuItem(icon: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            if (subtitle.isNotEmpty()) Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun EditProfileDialog(
    initialFullName: String,
    initialUsername: String,
    initialBio: String,
    email: String?,
    currentPhotoUrl: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, android.net.Uri?) -> Unit,
    isProfileLoading: Boolean,
    profileViewModel: ProfileViewModel
) {
    var fullName by remember { mutableStateOf(initialFullName) }
    var username by remember { mutableStateOf(initialUsername) }
    var bio by remember { mutableStateOf(initialBio) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    // Username checking state
    val usernameAvailability by profileViewModel.usernameAvailability.collectAsState()
    
    // Debounce username check
    LaunchedEffect(username) {
        if (username != initialUsername) {
            delay(500)
            if (username.length >= 3) {
                profileViewModel.checkUsername(username)
            }
        }
    }
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                // Profile Picture
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .clickable { 
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(selectedImageUri)
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else if (currentPhotoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentPhotoUrl)
                                .allowHardware(false)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.person),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    // Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                            contentDescription = "Change Photo",
                            tint = Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = username,
                    onValueChange = { 
                        // Instagram-like regex: lowercase, numbers, underscores, periods
                        if (it.all { char -> char.isLetterOrDigit() || char == '_' || char == '.' }) {
                            username = it.lowercase()
                        }
                    },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (username != initialUsername && username.length >= 3) {
                            when (usernameAvailability) {
                                true -> Text("Available", color = Color(0xFF4CAF50))
                                false -> Text("Username taken", color = MaterialTheme.colorScheme.error)
                                null -> Text("Checking...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Bio Field
                OutlinedTextField(
                    value = bio,
                    onValueChange = { 
                        // Character count check
                        if (it.length <= 180) {
                            bio = it 
                        }
                    },
                    label = { Text("Bio") },
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            text = "${bio.length}/180",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                )

                
                if (!email.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { },
                        label = { Text("Email") },
                        enabled = false,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        confirmButton = {
            val isUsernameValid = username == initialUsername || (usernameAvailability == true && username.length >= 3)
            Button(
                onClick = { onSave(fullName, username, bio, selectedImageUri) },
                enabled = !isProfileLoading && isUsernameValid
            ) {
                if (isProfileLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProfileLoading
            ) {
                Text("Cancel")
            }
        }
    )
}



@Composable

fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onGranted: () -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                onGranted()
            } else {
                onDismiss()
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stay Updated") },
        text = {
            Text("Enable notifications to know when someone follows you back or interacts with your profile. We value your privacy and only send important updates.")
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onGranted()
                    }
                }
            ) {
                Text("Allow Notifications")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        },
        icon = { Icon(painterResource(R.drawable.notification_on), null) }
    )
}

@Composable
private fun EnhancedMenuCard(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isLiquidGlass: Boolean,
    isPrimary: Boolean = false
) {
    if (isLiquidGlass) {
        LiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            onClick = onClick,
            cornerRadius = 20f,
            surfaceAlpha = if (isPrimary) 0.3f else 0.15f,
            content = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon Container
                    Box(
                        modifier = Modifier
                            .size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(18.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    
                    Icon(
                        painter = painterResource(R.drawable.navigate_next),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )
    } else {
        val containerColor = when {
            isPrimary -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = if (isPrimary) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
                )
                .clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Container
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(18.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                Icon(
                    painter = painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun EnhancedStatCard(value: String, label: String, isLiquidGlass: Boolean) {
    // Removed background and shadow as requested
    if (isLiquidGlass) {
        LiquidGlassCard(
            modifier = Modifier
                .padding(8.dp)
                .size(100.dp), // Fixed size for consistency
            cornerRadius = 20f,
            surfaceAlpha = 0.15f,
            content = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = value,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
