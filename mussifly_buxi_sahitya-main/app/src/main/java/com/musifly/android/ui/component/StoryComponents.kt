package com.musifly.android.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musifly.android.R
import com.musifly.android.models.UserStoryGroup
import com.musifly.android.viewmodels.StoryViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable

@Composable
fun StorySection(
    viewModel: StoryViewModel,
    isPremium: Boolean,
    currentUserAvatar: String?,
    onStoryClick: (UserStoryGroup) -> Unit,
    onAddStory: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val stories by viewModel.stories.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    val myStoryGroup = stories.find { it.userId == currentUserId }
    val otherStories = stories.filter { it.userId != currentUserId }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { onAddStory(it) }
    }

    Column(modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            item {
                MyStoryItem(
                    isUploading = isUploading,
                    avatarUrl = currentUserAvatar,
                    storyGroup = myStoryGroup,
                    onAddClick = {
                        if (isPremium) {
                            launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageAndVideo))
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "these feature only for mussifly premium users",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    onViewClick = { group -> onStoryClick(group) }
                )
            }

            items(otherStories, key = { it.userId }) { group ->
                StoryItem(
                    group = group,
                    onClick = { onStoryClick(group) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyStoryItem(
    isUploading: Boolean,
    avatarUrl: String?,
    storyGroup: UserStoryGroup?,
    onAddClick: () -> Unit,
    onViewClick: (UserStoryGroup) -> Unit
) {
    val ringBrush = when {
        storyGroup == null || storyGroup.stories.isEmpty() -> null
        storyGroup.hasUnviewed -> Brush.sweepGradient(
            listOf(Color(0xFF00E5FF), Color(0xFF00B0FF), Color(0xFF00E5FF))
        )
        else -> Brush.linearGradient(listOf(Color.Gray, Color.Gray))
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .combinedClickable(
                onClick = { 
                    if (storyGroup != null && storyGroup.stories.isNotEmpty()) onViewClick(storyGroup) else onAddClick() 
                },
                onLongClick = onAddClick
            )
            .padding(end = 4.dp)
    ) {
        Box(modifier = Modifier.size(68.dp)) {
            // Ring
            if (ringBrush != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, ringBrush, CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .align(Alignment.Center)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .error(R.drawable.person)
                        .placeholder(R.drawable.person)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Plus button overlay (Instagram style - always allow adding)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF25D366)) // WhatsApp Green
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .align(Alignment.BottomEnd)
                    .clickable { onAddClick() } // Direct click on plus also works
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Story",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp).align(Alignment.Center)
                )
            }

            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }
        }
        Text(
            text = "My Story",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StoryItem(
    group: UserStoryGroup,
    onClick: () -> Unit
) {
    val ringBrush = if (group.hasUnviewed) {
        Brush.sweepGradient(
            listOf(
                Color(0xFF00E5FF),
                Color(0xFF00E5FF),
                Color(0xFF00B0FF),
                Color(0xFF00E5FF)
            )
        )
    } else {
        Brush.linearGradient(listOf(Color.Gray, Color.Gray))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .size(width = 80.dp, height = 100.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .border(2.dp, ringBrush, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(group.userImageUrl)
                    .crossfade(true)
                    .error(R.drawable.person)
                    .placeholder(R.drawable.person)
                    .build(),
                contentDescription = group.username,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = group.username,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}
