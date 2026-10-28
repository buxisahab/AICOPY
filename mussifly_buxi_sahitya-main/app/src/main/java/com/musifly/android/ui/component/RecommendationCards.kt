package com.musifly.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musifly.android.R
import com.musifly.android.models.RecommendationItem
import com.musifly.android.utils.HdThumbnailUtil

/**
 * Song Card Component - Rounded 12px, AlbumArt full width, Title + Artist below
 */
@Composable
fun SongCard(
    item: RecommendationItem.SongItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        // Album Art
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .shadow(4.dp, RoundedCornerShape(12.dp))
        ) {
            val thumbnailUrl = HdThumbnailUtil.getBestThumbnailUrl(
                com.musifly.android.models.MediaMetadata(
                    id = item.id,
                    title = item.title,
                    artists = item.artists.map { artistName ->
                        com.musifly.android.models.MediaMetadata.Artist(
                            id = null,
                            name = artistName
                        )
                    },
                    duration = item.duration,
                    thumbnailUrl = item.thumbnailUrl
                )
            ) ?: item.thumbnailUrl
            
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(thumbnailUrl)
                    .crossfade(300)
                    .memoryCacheKey(thumbnailUrl)
                    .diskCacheKey(thumbnailUrl)
                    .allowHardware(false)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.opentune_monochrome),
                error = painterResource(R.drawable.opentune_monochrome),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp
        )
        
        // Artist
        if (item.subtitle != null) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Playlist Card Component - Larger square art, Subtitle: Playlist / Album type
 */
@Composable
fun PlaylistCard(
    item: RecommendationItem.PlaylistItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(180.dp)
            .clickable(onClick = onClick)
    ) {
        // Large square art
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .shadow(4.dp, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(item.thumbnailUrl)
                    .crossfade(300)
                    .memoryCacheKey(item.thumbnailUrl)
                    .diskCacheKey(item.thumbnailUrl)
                    .allowHardware(false)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.opentune_monochrome),
                error = painterResource(R.drawable.opentune_monochrome),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        )
        
        // Subtitle (Playlist / Album type)
        Text(
            text = item.subtitle ?: item.type.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
    }
}

/**
 * Artist Card Component - Circular image with subtle shadow
 */
@Composable
fun ArtistCard(
    item: RecommendationItem.ArtistItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Circular image
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .shadow(8.dp, CircleShape)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(item.thumbnailUrl)
                    .crossfade(300)
                    .memoryCacheKey(item.thumbnailUrl)
                    .diskCacheKey(item.thumbnailUrl)
                    .allowHardware(false)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.opentune_monochrome),
                error = painterResource(R.drawable.opentune_monochrome),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Artist name
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp
        )
    }
}

/**
 * Mini Song Tile - Compact vertical list item for Recently Played
 */
@Composable
fun MiniSongTile(
    item: RecommendationItem.SongItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Small thumbnail
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            val thumbnailUrl = HdThumbnailUtil.getBestThumbnailUrl(
                com.musifly.android.models.MediaMetadata(
                    id = item.id,
                    title = item.title,
                    artists = item.artists.map { artistName ->
                        com.musifly.android.models.MediaMetadata.Artist(
                            id = null,
                            name = artistName
                        )
                    },
                    duration = item.duration,
                    thumbnailUrl = item.thumbnailUrl
                )
            ) ?: item.thumbnailUrl
            
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(thumbnailUrl)
                    .crossfade(300)
                    .memoryCacheKey(thumbnailUrl)
                    .diskCacheKey(thumbnailUrl)
                    .allowHardware(false)
                    .build(),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.opentune_monochrome),
                error = painterResource(R.drawable.opentune_monochrome),
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Title and Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
            
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Shuffle button for Daily Mix section
 */
@Composable
fun ShuffleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (com.musifly.android.ui.component.liquidglass.isLiquidGlassActive()) {
        com.musifly.android.ui.player.components.CircularGlassIconButton(
            painter = painterResource(R.drawable.shuffle),
            contentDescription = "Shuffle",
            onClick = onClick,
            buttonSize = 48.dp,
            iconSize = 24.dp,
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Icon(
                painter = painterResource(R.drawable.shuffle),
                contentDescription = "Shuffle",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
