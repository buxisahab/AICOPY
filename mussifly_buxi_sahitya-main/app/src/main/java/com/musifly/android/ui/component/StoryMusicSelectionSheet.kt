package com.musifly.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musifly.android.models.LocalSong
import com.musifly.android.models.MediaMetadata
import com.musifly.android.models.toMediaMetadata
import com.musifly.android.viewmodels.LocalMusicViewModel
import com.musifly.android.viewmodels.OnlineSearchSuggestionViewModel
import com.musifly.innertube.models.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryMusicSelectionSheet(
    onDismiss: () -> Unit,
    onMusicSelected: (MediaMetadata) -> Unit,
    localViewModel: LocalMusicViewModel = hiltViewModel(),
    onlineViewModel: OnlineSearchSuggestionViewModel = hiltViewModel()
) {
    var searchSource by remember { mutableStateOf("Online") } // "Online" or "Local"
    val localSongs by localViewModel.displayedSongs.collectAsState()
    val localSearchQuery by localViewModel.searchQuery.collectAsState()
    
    val onlineViewState by onlineViewModel.viewState.collectAsState()
    val onlineSearchQuery by onlineViewModel.query.collectAsState()
    
    // Initial load for local
    LaunchedEffect(Unit) {
        localViewModel.loadLocalMusic()
    }

    // Adapt to App Theme
    val sheetBackground = MaterialTheme.colorScheme.surface
    val searchBarColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(sheetBackground)
            .padding(top = 8.dp)
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(onSurface.copy(alpha = 0.2f))
                .align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Search Bar (Instagram style: Pill-shaped, dynamic theme background)
        // Using BasicTextField for more control and to avoid the padding issues of Material3 TextField
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(searchBarColor)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                BasicTextField(
                    value = if (searchSource == "Online") onlineSearchQuery else localSearchQuery,
                    onValueChange = { newValue ->
                        if (searchSource == "Online") {
                            onlineViewModel.query.value = newValue
                        } else {
                            localViewModel.searchMusic(newValue)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = onSurface,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(onSurface),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    decorationBox = { innerTextField ->
                        if ((if (searchSource == "Online") onlineSearchQuery else localSearchQuery).isEmpty()) {
                            Text(
                                "Search music", 
                                color = onSurface.copy(alpha = 0.4f),
                                fontSize = 15.sp 
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Categories (Instagram style: Chips/Pill buttons)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                CategoryChip(
                    text = "For you",
                    selected = searchSource == "Online",
                    onClick = { searchSource = "Online" }
                )
            }
            item {
                CategoryChip(
                    text = "Local",
                    selected = searchSource == "Local",
                    onClick = { searchSource = "Local" }
                )
            }
            item {
                CategoryChip(text = "Trending", selected = false, onClick = { })
            }
            item {
                CategoryChip(text = "Saved", selected = false, onClick = { })
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (searchSource == "Online") {
                val songItems = onlineViewState.items.filterIsInstance<SongItem>()
                items(songItems) { songItem ->
                    val metadata = songItem.toMediaMetadata()
                    MusicItemRow(
                        title = songItem.title,
                        artist = songItem.artists.joinToString { it.name },
                        thumbnail = songItem.thumbnail,
                        durationInSec = songItem.duration ?: 0,
                        onClick = { onMusicSelected(metadata) }
                    )
                }
            } else {
                items(localSongs) { localSong ->
                    MusicItemRow(
                        title = localSong.title,
                        artist = localSong.artist,
                        thumbnail = localSong.albumArtUri.toString(),
                        durationInSec = (localSong.duration / 1000).toInt(),
                        onClick = { 
                            onMusicSelected(localSong.toMediaMetadata(localSong.albumArtUri?.toString()))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.surfaceVariant
    val onSelected = MaterialTheme.colorScheme.onPrimary
    val onUnselected = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) selectedColor else unselectedColor,
        modifier = Modifier.height(34.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (selected) onSelected else onUnselected,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MusicItemRow(
    title: String,
    artist: String,
    thumbnail: String?,
    durationInSec: Int,
    onClick: () -> Unit
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(thumbnail)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val durationText = String.format("%d:%02d", durationInSec / 60, durationInSec % 60)
                Text(
                    text = "$artist \u2022 $durationText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp
                )
            }
        }
        
        // Bookmark icon
        IconButton(onClick = { /* Save logic */ }) {
            Icon(
                imageVector = Icons.Default.BookmarkBorder,
                contentDescription = "Save",
                tint = onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
