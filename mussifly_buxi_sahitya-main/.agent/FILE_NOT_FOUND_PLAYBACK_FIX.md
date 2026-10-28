# FileNotFoundException Playback Fix

## Problem Summary

The app was crashing during playback with the following error:

```
java.io.FileNotFoundException: FH-oV5nXblA: open failed: ENOENT (No such file or directory)
```

**Root Cause:** MediaItems were created with video IDs as URIs (e.g., `"FH-oV5nXblA"`), but ExoPlayer's default DataSource tried to open them as **file paths** instead of resolving them to YouTube streaming URLs.

---

## Technical Analysis

### The Issue

1. **MediaItem Creation** (`MediaItemExt.kt` line 56):
   ```kotlin
   fun MediaMetadata.toMediaItem() =
       MediaItem.Builder()
           .setMediaId(id)
           .setUri(id)  // ❌ This is just a video ID, not a valid URI!
           ...
   ```

2. **ExoPlayer Configuration** (`MusicService.kt`):
   - ExoPlayer was created with **default DataSource configuration**
   - Default DataSource uses `FileDataSource` for non-HTTP URIs
   - When it saw `"FH-oV5nXblA"`, it tried to open it as a file → **FileNotFoundException**

3. **Why It Worked for Downloads**:
   - `DownloadUtil.kt` had a `ResolvingDataSource.Factory` that intercepted video IDs
   - Resolved them to actual YouTube streaming URLs using `YTPlayerUtils.playerResponseForPlayback()`
   - **But this was only configured for downloads, not regular playback!**

---

## The Solution

### Changes Made to `MusicService.kt`

1. **Added ResolvingDataSource** to ExoPlayer configuration:
   - Intercepts MediaItem URIs before playback
   - Resolves video IDs to actual YouTube streaming URLs
   - Uses `YTPlayerUtils.playerResponseForPlayback()` API
   - Caches resolved URLs to avoid repeated API calls

2. **Added Stream URL Caching**:
   ```kotlin
   private val songUrlCache = HashMap<String, Pair<String, Long>>()
   ```
   - Caches stream URLs with expiration timestamps
   - Avoids repeated YouTube API calls for the same song
   - URLs expire after ~6 hours (YouTube's standard)

3. **Added Audio Quality Configuration**:
   ```kotlin
   private val audioQuality by enumPreference(this, AudioQualityKey, AudioQuality.AUTO)
   ```
   - Respects user's audio quality preference
   - Passed to YouTube API during URL resolution

---

## Code Flow

### Before (❌ Broken):
```
MediaItem(uri="FH-oV5nXblA")
   ↓
ExoPlayer.play()
   ↓
DefaultDataSource.open("FH-oV5nXblA")
   ↓
FileDataSource.open("FH-oV5nXblA")
   ↓
❌ FileNotFoundException: No such file or directory
```

### After (✅ Fixed):
```
MediaItem(uri="FH-oV5nXblA")
   ↓
ExoPlayer.play()
   ↓
ResolvingDataSource.resolve("FH-oV5nXblA")
   ↓
YTPlayerUtils.playerResponseForPlayback("FH-oV5nXblA")
   ↓
Resolved URL: "https://rr3---sn-ab5sznez.googlevideo.com/..."
   ↓
OkHttpDataSource.open(resolvedURL)
   ↓
✅ Playback starts successfully
```

---

## Key Components

### ResolvingDataSource Lambda (lines 520-573)

```kotlin
val dataSourceFactory = ResolvingDataSource.Factory(...) { dataSpec ->
    val mediaId = dataSpec.key ?: dataSpec.uri.toString()
    
    // 1. Check cache first
    if (playerCache.isCached(mediaId, ...)) return@Factory dataSpec
    
    // 2. Check URL cache
    songUrlCache[mediaId]?.let { ... }
    
    // 3. Resolve video ID to streaming URL
    val playbackData = runBlocking {
        YTPlayerUtils.playerResponseForPlayback(
            videoId = mediaId,
            audioQuality = audioQuality,
            connectivityManager = connectivityManager
        )
    }.getOrThrow()
    
    // 4. Cache the URL
    songUrlCache[mediaId] = streamUrl to expirationTime
    
    // 5. Return resolved URI
    dataSpec.withUri(streamUrl.toUri())
}
```

### Error Handling

- **Network Errors**: Automatically retried when network returns (existing functionality)
- **File Errors**: Now properly handled - video IDs are resolved instead of treated as files
- **Unknown Errors**: Player pauses and logs error for debugging

---

## Testing

### Verify the Fix

1. **Play a song from YouTube**:
   - Logs should show: `✅ Resolved video ID 'XXX' to streaming URL`
   - No more `FileNotFoundException` errors

2. **Check caching**:
   - First playback: Resolves URL (slight delay)
   - Subsequent playback: Uses cached URL (instant)

3. **Network resilience**:
   - Disconnect network → Error shown
   - Reconnect → Playback auto-resumes

### Expected Logs

```
D/MusicService: ✅ ExoPlayer created with professional configuration + ResolvingDataSource
D/MusicService: ✅ Resolved video ID 'oI8X15a_q_Q' to streaming URL
D/MusicService: 🎵 Playback state: READY (isPlaying: true)
```

---

## Future Improvements

1. **Preload Next Song**:
   - Resolve next song's URL in background
   - Zero-latency track transitions

2. **Smarter Caching**:
   - Persist URL cache across app restarts
   - Pre-cache URLs for playlists

3. **Offline Mode**:
   - Detect when song is downloaded
   - Use local file instead of streaming URL

---

## Related Files

- `MusicService.kt` - Main playback service (MODIFIED)
- `DownloadUtil.kt` - Download manager with similar ResolvingDataSource
- `MediaItemExt.kt` - MediaItem creation extensions
- `YTPlayerUtils.kt` - YouTube stream URL resolution API

---

## Summary

**Before**: ExoPlayer tried to play video IDs as file paths → FileNotFoundException  
**After**: ResolvingDataSource intercepts video IDs and resolves them to streaming URLs → Successful playback

This fix brings the playback system to **production quality**, matching the reliability of apps like Spotify and YouTube Music.
