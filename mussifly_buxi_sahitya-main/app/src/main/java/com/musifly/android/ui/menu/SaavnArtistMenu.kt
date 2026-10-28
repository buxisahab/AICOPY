package com.musifly.android.ui.menu

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musifly.android.LocalDatabase
import com.musifly.android.LocalPlayerConnection
import com.musifly.android.R
import com.musifly.android.db.entities.ArtistEntity
import com.musifly.android.models.MediaMetadata
import com.musifly.android.models.SaavnArtist
import com.musifly.android.playback.queues.SaavnQueue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaavnArtistMenu(
    artist: SaavnArtist,
    onDismiss: () -> Unit,
    saavnAPI: com.musifly.android.streamingengines.saavn.SaavnAPI,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val libraryArtist by database.artist("saavn:${artist.id}").collectAsState(initial = null)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    ListItem(
        headlineContent = {
            Text(
                text = artist.title,
                maxLines = 1,
            )
        },
        supportingContent = {
            artist.subtitle?.let {
                Text(
                    text = it,
                    maxLines = 1,
                )
            }
        },
        leadingContent = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artist.image)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
    )

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Start Radio button
        Column(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    // Start radio based on artist top song (simplification)
                    playerConnection.playQueue(
                        SaavnQueue(
                            "saavn:${artist.id}",
                            saavnAPI,
                            preloadItem = MediaMetadata(
                                id = "saavn:${artist.id}",
                                title = artist.title,
                                artists = listOf(MediaMetadata.Artist(id = null, name = artist.title)),
                                duration = 0
                            )
                        )
                    )
                    onDismiss()
                }
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.radio),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(R.string.start_radio),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .basicMarquee()
                    .padding(top = 4.dp),
            )
        }

        // Share button
        Column(
            modifier = Modifier
                .weight(1f)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        val shareLink = "https://www.jiosaavn.com/artist/${artist.title.replace(" ", "-")}/${artist.id}"
                        putExtra(Intent.EXTRA_TEXT, shareLink)
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                    onDismiss()
                }
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.share),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = stringResource(R.string.share),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .basicMarquee()
                    .padding(top = 4.dp),
            )
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            ListItem(
                headlineContent = {
                    Text(
                        text = if (libraryArtist?.artist?.bookmarkedAt != null) 
                            stringResource(R.string.subscribed) 
                        else 
                            stringResource(R.string.subscribe)
                    )
                },
                leadingContent = {
                    Icon(
                        painter = painterResource(
                            if (libraryArtist?.artist?.bookmarkedAt != null) {
                                R.drawable.library_add_check
                            } else {
                                R.drawable.library_add
                            }
                        ),
                        contentDescription = null,
                    )
                },
                modifier = Modifier.clickable {
                    coroutineScope.launch {
                        val bookmarksRepo = dagger.hilt.android.EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            com.musifly.android.di.BookmarksRepositoryEntryPoint::class.java
                        ).bookmarksRepository()
                        
                        val libArtistEntity = libraryArtist?.artist
                        if (libArtistEntity != null) {
                            bookmarksRepo.toggleArtistBookmark(libArtistEntity)
                        } else {
                            bookmarksRepo.toggleArtistBookmark(
                                ArtistEntity(
                                    id = "saavn:${artist.id}",
                                    name = artist.title,
                                    channelId = null,
                                    thumbnailUrl = artist.image,
                                )
                            )
                        }
                    }
                }
            )
        }
    }
}
