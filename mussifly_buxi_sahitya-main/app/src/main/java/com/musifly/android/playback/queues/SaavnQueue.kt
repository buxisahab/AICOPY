package com.musifly.android.playback.queues

import androidx.media3.common.MediaItem
import com.musifly.android.extensions.toMediaItem
import com.musifly.android.models.MediaMetadata
import com.musifly.android.models.toMediaMetadataFromSaavnMap
import com.musifly.android.streamingengines.saavn.SaavnAPI
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class SaavnQueue(
    private val seedSongId: String,
    private val saavnAPI: SaavnAPI,
    override val preloadItem: MediaMetadata? = null,
) : Queue {
    private var items: List<MediaItem> = emptyList()
    private var fetched = false

    override suspend fun getInitialStatus(): Queue.Status = withContext(IO) {
        val results = saavnAPI.getSongRecommendations(seedSongId.removePrefix("saavn:"))
        val mediaMetadataList = results.map { it.toMediaMetadataFromSaavnMap() }
        items = mediaMetadataList.map { it.toMediaItem() }
        fetched = true
        
        Queue.Status(
            title = "Radio",
            items = items,
            mediaItemIndex = 0,
        )
    }

    override fun hasNextPage(): Boolean = false

    override suspend fun nextPage(): List<MediaItem> = throw UnsupportedOperationException()

    companion object {
        fun radio(song: MediaMetadata, saavnAPI: SaavnAPI) = SaavnQueue(song.id, saavnAPI, song)
    }
}
