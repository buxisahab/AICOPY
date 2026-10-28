# Remaining saavnAPI Parameter Additions

## Files Still Need Fixing:

### 1. AlbumScreen.kt:311
**File:** `d:\mussifly_final_project\app\src\main\java\com\musifly\android\ui\screens\AlbumScreen.kt`
**Line:** 311
**Change:** Add `, saavnAPI = viewModel.saavnAPI` to the SaavnSongMenu call

### 2. ArtistScreen.kt - Multiple locations
**File:** `d:\mussifly_final_project\app\src\main\java\com\musifly\android\ui\screens\artist\ArtistScreen.kt`
**Lines:** 324, 354, 404, 437
**Change:** Add `, saavnAPI = viewModel.saavnAPI` to each SaavnSongMenu or SaavnArtistMenu call

### 3. OnlinePlaylistScreen.kt:374
**File:** `d:\mussifly_final_project\app\src\main\java\com\musifly\android\ui\screens\playlist\OnlinePlaylistScreen.kt`
**Line:** 374
**Change:** Add `, saavnAPI = viewModel.saavnAPI` to the SaavnSongMenu call

### 4. OnlineSearchResult.kt - Multiple locations
**File:** `d:\mussifly_final_project\app\src\main\java\com\musifly\android\ui\screens\search\OnlineSearchResult.kt`
**Lines:** 116, 126
**Change:** Add `, saavnAPI = viewModel.saavnAPI` to each SaavnSongMenu or SaavnArtistMenu call

### 5. OnlineSearchScreen.kt:282
**File:** `d:\mussifly_final_project\app\src\main\java\com\musifly\android\ui\screens\search\OnlineSearchScreen.kt`
**Line:** 282
**Change:** Add `, saavnAPI = viewModel.saavnAPI` to the SaavnSongMenu call

## Summary of Changes Made:
✅ ArtistViewModel - Added saavnAPI injection
✅ AlbumViewModel - Added saavnAPI injection
✅ OnlinePlaylistViewModel - Added saavnAPI injection
✅ LocalSearchViewModel - Already had saavnAPI
✅ OnlineSearchViewModel - Already had saavnAPI
✅ OnlineSearchSuggestionViewModel - Made saavnAPI public
✅ SaavnArtistMenu - Added saavnAPI parameter
✅ SaavnSongMenu - Added saavnAPI parameter
✅ LocalSearchScreen - Fixed key function, renamed local variable, added saavnAPI parameters
✅ OnlineSearchScreen - Added saavnAPI to SaavnQueue.radio and SaavnArtistMenu calls
✅ SaavnSongMenu.kt - Added saavnAPI to internal SaavnQueue.radio call
