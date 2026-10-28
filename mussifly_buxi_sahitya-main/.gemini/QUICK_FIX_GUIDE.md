# Quick Fix Guide for Remaining Compilation Errors

All remaining errors are missing `saavnAPI` parameter. Here's exactly what to add at each location:

## 1. SaavnSongMenu.kt:130
**Error:** No value passed for parameter 'saavnAPI'
**Location:** Line 130 in the file itself (recursive call)
**Action:** This is likely in a preview or test function - need to check context

## 2. AlbumScreen.kt:311
**Error:** No value passed for parameter 'saavnAPI'
**Add:** `, saavnAPI = viewModel.saavnAPI`
**Note:** Need to ensure AlbumViewModel has saavnAPI injected

## 3. ArtistScreen.kt:324
**Error:** No value passed for parameter 'saavnAPI'
**Add:** `, saavnAPI = viewModel.saavnAPI`

## 4. ArtistScreen.kt:354
**Error:** No value passed for parameter 'saavnAPI'  
**Add:** `, saavnAPI = viewModel.saavnAPI`

## 5. ArtistScreen.kt:404
**Error:** No value passed for parameter 'saavnAPI'
**Add:** `, saavnAPI = viewModel.saavnAPI`

## 6. ArtistScreen.kt:437
**Error:** No value passed for parameter 'saavnAPI'
**Add:** `, saavnAPI = viewModel.saavnAPI`

## 7. OnlinePlaylistScreen.kt:374
**Error:** No value passed for parameter 'saavnAPI'
**Add:** `, saavnAPI = viewModel.saavnAPI`
**Note:** Need to ensure OnlinePlaylistViewModel has saavnAPI injected

## 8. LocalSearchScreen.kt:152, 157, 160
**Errors:** Unresolved reference 'id', 'toMediaMetadata', Unresolved label
**Issue:** These are related to SaavnSong handling - need to check the context

## 9. LocalSearchScreen.kt:169
**Error:** No value passed for parameter 'saavnAPI'
**Add:** `, saavnAPI = viewModel.saavnAPI`

## 10. LocalSearchScreen.kt:186
**Error:** Unresolved label
**Issue:** Related to click handling - need to check context

## 11. LocalSearchScreen.kt:199
**Error:** No value passed for parameter 'saavnAPI'
**Add:** `, saavnAPI = viewModel.saavnAPI`

## 12. OnlineSearchResult.kt:116
**Error:** No value passed for parameter 'saavnAPI'
**Add:** `, saavnAPI = viewModel.saavnAPI`

## 13. OnlineSearchResult.kt:126
**Error:** No value passed for parameter 'saavnAPI'
**Add:** `, saavnAPI = viewModel.saavnAPI`

## 14. OnlineSearchScreen.kt:282
**Error:** No value passed for parameter 'saavnAPI'
**Add:** `, saavnAPI = viewModel.saavnAPI`

## ViewModels that need saavnAPI injection:
- ✅ LocalSearchViewModel - Already has it
- ✅ OnlineSearchViewModel - Already has it
- ❓ ArtistViewModel - Need to check
- ❓ AlbumViewModel - Need to check  
- ❓ OnlinePlaylistViewModel - Need to check
- ❓ OnlineSearchResultViewModel - Need to check
