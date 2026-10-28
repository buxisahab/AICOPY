package com.musifly.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.musifly.android.R
import com.musifly.android.viewmodels.ProfileViewModel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import com.google.firebase.auth.FirebaseAuth
import com.musifly.android.ui.component.shimmer.ShimmerHost
import com.musifly.android.ui.component.shimmer.ListItemPlaceHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    navController: NavController,
    userId: String,
    isFollowers: Boolean,
    viewModel: ProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val usersList by viewModel.categorizedUsersList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val followStates by viewModel.followStates.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val officialAccounts = listOf("Qev4DqlLiGeVXhIhBLaQWmHOmjq2", "RSdUGiWPitOuq6HXHL7lPXKGQwi1")
    val isAdmin = currentUserId in officialAccounts
    val isOwnProfile = userId == currentUserId
    
    val title = if (isFollowers) "Followers" else "Following"

    LaunchedEffect(userId, isFollowers) {
        if (isFollowers) {
            viewModel.getFollowersList(userId)
        } else {
            viewModel.getFollowingList(userId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    com.musifly.android.ui.component.AppIconButton(
                        onClick = { navController.navigateUp() },
                        onLongClick = null,
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading && usersList.isEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(10) {
                        ShimmerHost {
                            ListItemPlaceHolder(thumbnailShape = CircleShape)
                        }
                    }
                }
            } else if (usersList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.person),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No users found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(usersList) { item ->
                        when (item) {
                            is ProfileViewModel.UserListItem.Header -> {
                                Text(
                                    text = item.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            is ProfileViewModel.UserListItem.User -> {
                                val profile = item.profile
                                val isFollowing = followStates[profile.id] ?: false
                                val isSelf = profile.id == currentUserId

                                UserListItem(
                                    profile = profile,
                                    isFollowing = isFollowing,
                                    canFollow = !isSelf,
                                    canRemove = isFollowers && (isOwnProfile || isAdmin),
                                    onFollowClick = {
                                        viewModel.toggleFollowInList(profile.id)
                                    },
                                    onRemoveClick = {
                                        viewModel.removeFollower(profile.id, userId)
                                    },
                                    onClick = {
                                        navController.navigate("profile/${profile.id}")
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(
    profile: com.musifly.android.sync.supabase.SupabaseProfile,
    isFollowing: Boolean,
    canFollow: Boolean,
    canRemove: Boolean,
    onFollowClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(LocalContext.current)
                .data(profile.avatarUrl)
                .allowHardware(false)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.person)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.username ?: "User",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!profile.fullName.isNullOrBlank()) {
                Text(
                    text = profile.fullName!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (canFollow) {
                Button(
                    onClick = onFollowClick,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    colors = if (isFollowing) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (canRemove) {
                com.musifly.android.ui.component.AppIconButton(
                    onClick = onRemoveClick,
                    painter = painterResource(id = R.drawable.close),
                    contentDescription = "Remove Follower",
                    size = 32.dp,
                    iconSize = 20.dp
                )
            }
        }
    }
}
