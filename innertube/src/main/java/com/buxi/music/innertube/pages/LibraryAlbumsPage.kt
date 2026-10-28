package com.buxi.music.innertube.pages

import com.buxi.music.innertube.models.Album
import com.buxi.music.innertube.models.AlbumItem
import com.buxi.music.innertube.models.Artist
import com.buxi.music.innertube.models.ArtistItem
import com.buxi.music.innertube.models.MusicResponsiveListItemRenderer
import com.buxi.music.innertube.models.MusicTwoRowItemRenderer
import com.buxi.music.innertube.models.PlaylistItem
import com.buxi.music.innertube.models.SongItem
import com.buxi.music.innertube.models.YTItem
import com.buxi.music.innertube.models.oddElements
import com.buxi.music.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
