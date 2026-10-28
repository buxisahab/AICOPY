package com.musifly.android.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musifly.android.R
import com.musifly.android.sync.supabase.SupabaseNotificationWithActor
import com.musifly.android.viewmodels.NewReleaseViewModel
import com.musifly.android.viewmodels.ProfileViewModel
import com.musifly.innertube.models.AlbumItem
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    
    val tabs = listOf(
        stringResource(R.string.activity),
        stringResource(R.string.new_releases)
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.notifications)) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.navigation_back)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
                
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> ActivityTab(navController)
                1 -> NewReleasesTab(navController)
            }
        }
    }
}

@Composable
private fun NewReleasesTab(
    navController: NavController,
    viewModel: NewReleaseViewModel = hiltViewModel()
) {
    val newReleases by viewModel.newReleaseAlbums.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            newReleases.isEmpty() -> {
                Text(
                    text = stringResource(R.string.no_new_releases),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = newReleases,
                        key = { it.id }
                    ) { album ->
                        NewReleaseItem(
                            album = album,
                            onClick = {
                                navController.navigate("album/${album.id}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityTab(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val pullRefreshState = rememberPullToRefreshState()
    
    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
        viewModel.markAllAsRead()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isLoading,
                onRefresh = { viewModel.loadNotifications() }
            )
    ) {
        if (notifications.isEmpty() && !isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No activity yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "When someone follows you, you'll see it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 32.dp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loadNotifications() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        } else {
            val followStates by viewModel.followStates.collectAsState()
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Clear All",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { viewModel.clearNotifications() }
                        )
                    }
                }
                
                items(
                    items = notifications,
                    key = { it.id ?: 0L }
                ) { notification ->
                    NotificationItem(
                        notification = notification,
                        isFollowing = followStates[notification.actorId] ?: false,
                        onActorClick = { actorId ->
                            navController.navigate("profile/$actorId")
                        },
                        onFollowClick = { actorId ->
                            viewModel.toggleFollowFromNotification(actorId)
                        },
                        onDelete = {
                            viewModel.deleteNotification(notification.id)
                        }
                    )
                }
            }
        }
        
        PullToRefreshDefaults.Indicator(
            state = pullRefreshState,
            isRefreshing = isLoading,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotificationItem(
    notification: SupabaseNotificationWithActor,
    isFollowing: Boolean,
    onActorClick: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onDelete: () -> Unit
) {
    val actor = notification.actor
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { actor?.id?.let { onActorClick(it) } },
                onLongClick = onDelete
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Picture
        AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(actor?.avatarUrl ?: R.drawable.person)
                .allowHardware(false)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.weight(1f)) {
            val annotatedText = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(actor?.fullName ?: actor?.username ?: "Someone")
                }
                append(" ")
                append(notification.message ?: "")
            }
            
            Text(
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = formatTimestamp(notification.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        if (notification.type == "follow") {
            Button(
                onClick = { actor?.id?.let { onFollowClick(it) } },
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) 
                        MaterialTheme.colorScheme.secondaryContainer 
                    else 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    contentColor = if (isFollowing)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatTimestamp(createdAt: String?): String {
    if (createdAt == null) return ""
    return try {
        val instant = Instant.parse(createdAt)
        val now = Instant.now()
        val zoneId = ZoneId.systemDefault()
        
        val minutes = ChronoUnit.MINUTES.between(instant, now)
        val hours = ChronoUnit.HOURS.between(instant, now)
        val days = ChronoUnit.DAYS.between(instant, now)

        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a").withZone(zoneId)
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(zoneId)
        
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago at ${timeFormatter.format(instant)}"
            hours < 24 -> "${hours}h ago at ${timeFormatter.format(instant)}"
            days == 1L -> "Yesterday at ${timeFormatter.format(instant)}"
            days < 7 -> "${days}d ago on ${DateTimeFormatter.ofPattern("EEEE").withZone(zoneId).format(instant)}"
            else -> "${dateFormatter.format(instant)} at ${timeFormatter.format(instant)}"
        }
    } catch (e: Exception) {
        ""
    }
}

@Composable
private fun NewReleaseItem(
    album: AlbumItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.thumbnail)
                    .allowHardware(false)
                    .build(),
                contentDescription = album.title,
                modifier = Modifier.size(56.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = album.title ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artists?.joinToString(", ") { it.name } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
