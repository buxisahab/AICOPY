package com.buxi.music.ui.player

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import com.buxi.music.models.MediaMetadata
import com.buxi.music.playback.PlayerConnection

// Paging + prefetch parameters
private const val WIDTH_FACTOR = 0.62f
private const val PREFETCH_RANGE_PAGES = 1                  // prefetch ±1 page
private const val VIEW_CACHE_SIZE = 3                       // recycle cache (Compose keeps composition alive within bounds)
private const val CROSSFADE_MS = 200L

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerPlayer(
    playerConnection: PlayerConnection,
    songs: List<MediaMetadata>,
    currentSongIndex: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = currentSongIndex) { songs.size }
    val context = LocalContext.current

    var currentPage by remember { mutableStateOf(currentSongIndex) }

    // Lightweight UI metadata cache so upcoming page binds instantly
    data class UiModel(val title: String?, val artistsText: String?, val thumbnailUrl: String?)
    val metadataCache = remember { mutableStateMapOf<Int, UiModel>() }

    fun cacheUiFor(index: Int) {
        if (index in songs.indices && metadataCache[index] == null) {
            val s = songs[index]
            metadataCache[index] = UiModel(
                title = s.title,
                artistsText = s.artists.joinToString { it.name },
                thumbnailUrl = s.thumbnailUrl
            )
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val newPage = pagerState.currentPage
        if (newPage != currentPage) {
            currentPage = newPage
            onPageChange(newPage)
        }
        // Ensure cache for current and neighbors
        cacheUiFor(newPage)
        for (i in (newPage - PREFETCH_RANGE_PAGES)..(newPage + PREFETCH_RANGE_PAGES)) {
            if (i != newPage && i in songs.indices) {
                cacheUiFor(i)
                songs[i].thumbnailUrl?.let { url ->
                    ImageLoader(context).enqueue(
                        ImageRequest.Builder(context)
                            .data(url)
                            .build()
                    )
                }
            }
        }
    }

    LaunchedEffect(currentSongIndex) {
        if (currentSongIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(currentSongIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val song = songs[page]
            val isCurrentPage = page == pagerState.currentPage

            // Preload adjacent thumbnails for this page range
            LaunchedEffect(page) {
                val start = maxOf(0, page - PREFETCH_RANGE_PAGES)
                val end = minOf(songs.size - 1, page + PREFETCH_RANGE_PAGES)

                for (i in start..end) {
                    if (i != page) {
                        cacheUiFor(i)
                        songs[i].thumbnailUrl?.let { url ->
                            ImageLoader(context).enqueue(
                                ImageRequest.Builder(context)
                                    .data(url)
                                    .build()
                            )
                        }
                    }
                }
            }

            SongPage(
                song = song,
                isCurrentPage = isCurrentPage,
                playerConnection = playerConnection,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun SongPage(
    song: MediaMetadata,
    isCurrentPage: Boolean,
    playerConnection: PlayerConnection,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // TODO: Replace with actual player UI content
    }
}
