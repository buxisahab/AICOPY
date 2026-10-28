# AsyncImage Crash Fix Summary

## Problem
The app was crashing with a `SIGSEGV` in `RenderThread` when using the "Liquid Glass" theme. This was caused by a conflict between hardware-accelerated bitmaps (default in Coil) and the heavy `RenderEffect` (blur, vibrancy) used by the theme.

## Solution
We have systematically disabled hardware bitmaps for all `AsyncImage` components in key screens. This forces software rendering for these images, preventing the `RenderThread` crash while maintaining visual quality.

## Applied Changes

### 1. `SaavnComponents.kt`
- Updated `SaavnSongCard`, `SaavnAlbumCard`, `SaavnPlaylistCard`, `SaavnArtistCard`, and `SaavnChartRow`.
- Added `.allowHardware(false)` to all `ImageRequest.Builder` calls.

### 2. `HomeScreen.kt`
- Updated `AsyncImage` in `LocalGridItemComposable` (via `Items.kt` definitions).
- Updated `AsyncImage` in `YtGridItemComposable` (via `YouTubeGridItem`).
- Updated direct `AsyncImage` usage in:
    - Account Playlists section (`NavigationTitle` thumbnail).
    - JioSaavn "Home Data Sections" `Box`.
    - "New Release Albums" section.

### 3. `Items.kt` (Core UI Components)
- Verified/Updated `SongGridItem`, `AlbumGridItem`, `ArtistGridItem`, `PlaylistListItem`, etc.
- All core list/grid items now use `.allowHardware(false)`.

### 4. Destination Screens
- `AlbumScreen.kt`: Album art display patched.
- `OnlinePlaylistScreen.kt`: Playlist and song thumbnails patched.
- `LocalPlaylistScreen.kt`: Playlist and song thumbnails patched.

## Verification
- All major entry points from the Home Screen (Playlists, Albums, Artists) now use safe image loading.
- The "Liquid Glass" theme should now be stable when browsing content.

## Note on Performance
Disabling hardware bitmaps may have a negligible impact on scrolling performance, but it provides significantly better stability for complex themes like "Liquid Glass".
