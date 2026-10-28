# Remaining Fixes for SaavnSongMenu and SaavnArtistMenu

## Files that need saavnAPI parameter added to SaavnSongMenu calls:

1. LocalSearchScreen.kt - Lines 166, 196
2. OnlineSearchScreen.kt - Line 279
3. OnlineSearchResult.kt - Line 113
4. OnlinePlaylistScreen.kt - Line 371
5. ArtistScreen.kt - Lines 324, 401, 434
6. AlbumScreen.kt - Line 308

## Files that need saavnAPI parameter added to SaavnArtistMenu calls:

1. OnlineSearchResult.kt - Line 119
2. ArtistScreen.kt - Line 352

## Strategy:
- For LocalSearchScreen: use viewModel.saavnAPI
- For OnlineSearchScreen: use viewModel.saavnAPI
- For OnlineSearchResult: need to add saavnAPI to OnlineSearchViewModel and pass it
- For ArtistScreen: need to check if viewModel has saavnAPI or inject it
- For AlbumScreen: need to check if viewModel has saavnAPI or inject it
- For OnlinePlaylistScreen: need to check if viewModel has saavnAPI or inject it
