# Firestore Like Sync - Complete Implementation

## समस्या (Problem)
**User jab song like karta hai, vo Firestore par save nahi ho raha tha**

जब user app data clear करता है या दूसरे device पर login करता है, तो उसकी liked songs गायब हो जाती हैं।

**Why**: Likes केवल local database (`SongEntity.liked`) में save हो रहे थे, Firestore में नहीं।

---

## समाधान (Solution)

### Firestore Sync Architecture

App में **dual-storage system** है:

1. **Local Database** (`SongEntity.liked` + `UserLikedSongEntity`)
   - Fast, offline access
   - Primary UI data source

2. **Firestore** (`users/{uid}/liked_songs/{songId}`)
   - Cloud backup
   - Cross-device sync
   - Survives app reinstalls

### Integration in `toggleLike()`

```kotlin
fun toggleLike() {
    // 1. Update local database (SongEntity.liked)
    val updatedSong = song.song.toggleLike()
    database.query { update(updatedSong) }
    
    // 2. ✅ NEW: Sync to Firestore + user_liked_songs table
    val likesRepository = EntryPointAccessors
        .fromApplication(this, SyncManagerEntryPoint::class.java)
        .likesRepository()
    
    likesRepository.toggleLike(songId).fold(
        onSuccess = { isNowLiked ->
            Log.d(TAG, "🔥 Firestore sync successful")
        },
        onFailure = { error ->
            Log.w(TAG, "⚠️ Firestore sync failed (non-critical)")
            // Local database already updated, UI still works
        }
    )
}
```

---

## LikesRepository Flow

### When User Likes a Song:

```kotlin
likesRepository.toggleLike(songId)
  ↓
Check if currently liked
  ↓
If NOT liked → likeSong(songId):
  ├─ Logged in?
  │  ├─ YES: 
  │  │   ├─ Write to Firestore (PRIMARY)
  │  │   │   └─ users/{uid}/liked_songs/{songId}
  │  │   └─ Write to local (user_liked_songs table)
  │  │       └─ isSynced = true
  │  └─ NO (Guest):
  │      └─ Write to local only
  │          └─ isSynced = false
  └─ Update SongEntity.liked = true
```

### Firestore Document Structure:

```javascript
users/{userId}/liked_songs/{songId} = {
  songId: "m6Y8xEfyXTs",
  title: "Main Yahaan Hoon",
  thumbnailUrl: "https://...",
  source: "youtube",  // or "jiosaavn"
  addedAt: Timestamp
}
```

---

## क्या होता है अब (What Happens Now)

### Scenario 1: User Likes Song (Logged In)

```log
D/MusicService: ❤️ Toggling like for: Main Yahaan Hoon (ID: m6Y8xEfyXTs)
D/MusicService: 📝 Creating song in database: Main Yahaan Hoon
D/LikesRepository: 🔥 likeSong called - songId: m6Y8xEfyXTs
D/LikesRepository: 🔥 Firestore is PRIMARY - Writing to Firestore first...
D/FirestoreLikesService: 🔥 Firestore WRITE: Added like: m6Y8xEfyXTs
D/LikesRepository: ✅ Firestore write successful - adding to local DB
D/MusicService: 🔥 Firestore sync successful - like status: true
D/MusicService: ✅ Song Main Yahaan Hoon liked ❤️
```

### Scenario 2: App Data Cleared + Relogin

```log
D/SyncManager: Starting sync for user: abc123xyz
D/LikesRepository: 📥 Downloading liked songs from Firestore...
D/FirestoreLikesService: 📥 Reading liked_songs from Firestore
D/LikesRepository: ✅ Downloaded 25 liked songs from Firestore
D/LikesRepository: 📝 Creating 18 songs in local database...
D/SyncManager: ✅ Sync complete: Downloaded 25 likes
```

### Scenario 3: Offline Like (Will Sync Later)

```log
D/MusicService: ❤️ Toggling like for: Song Name
D/LikesRepository: 🔥 likeSong called
D/LikesRepository: ⚠️ Firestore write failed - Network error
D/LikesRepository: 💾 Fallback: Saved to local DB (isSynced = false)
D/MusicService: ⚠️ Firestore sync failed (non-critical)
D/MusicService: ✅ Song liked locally

// Later when online:
D/LikesSyncWorker: Syncing unsynced likes to Firestore...
D/LikesSyncWorker: Pushed 1 unsynced likes to Firestore
```

---

## Database Tables

### 1. `song` table (Main song data)
```sql
CREATE TABLE song (
  id TEXT PRIMARY KEY,
  title TEXT,
  liked BOOLEAN,           -- ← UI shows this
  likedDate DATETIME,
  ...
)
```

### 2. `user_liked_songs` table (Firestore sync tracking)
```sql
CREATE TABLE user_liked_songs (
  songId TEXT PRIMARY KEY,
  syncedAt INTEGER,
  isSynced BOOLEAN         -- ← false if pending Firestore sync
)
```

### 3. Firestore Collection
```
users/{userId}/liked_songs/{songId}
  ├─ songId: string
  ├─ title: string
  ├─ thumbnailUrl: string
  ├─ source: string
  └─ addedAt: Timestamp
```

---

## फायदे (Benefits)

1. ✅ **Cross-Device Sync**: एक device पर like करो, सभी devices पर दिखे
2. ✅ **App Reinstall Safety**: App delete/reinstall के बाद भी likes वापस आ जाएं
3. ✅ **Cloud Backup**: Firestore में permanent backup
4. ✅ **Offline Support**: Offline में भी like कर सकते हैं, बाद में sync होगा
5. ✅ **Guest Mode**: Guest users के लिए भी local likes काम करेंगे

---

## Testing

### Test 1: Like a Song (Logged In)
1. Login करें
2. कोई song play करें
3. Like button दबाएं
4. Check logcat:
   ```
   D/MusicService: 🔥 Firestore sync successful
   ```
5. Firestore console में देखें: `users/{uid}/liked_songs` में entry है

### Test 2: Cross-Device Sync
1. Device A पर song like करें
2. Device B पर same account से login करें
3. Check: Song "Liked Songs" में दिखना चाहिए

### Test 3: App Reinstall
1. कुछ songs like करें
2. App uninstall करें
3. Reinstall करें और login करें
4. Check: सभी liked songs वापस आ जानी चाहिए

---

## Code Files Modified

- **`MusicService.kt`** - Added Firestore sync in `toggleLike()`
- Uses **`LikesRepository`** - Handles Firestore + local DB
- Uses **`FirestoreLikesService`** - Direct Firestore operations
- Uses **`UserLikedSongEntity`** - Sync tracking table

---

## सारांश (Summary)

**पहले**: Like → केवल local database  
**अब**: Like → local database + Firestore (if logged in)  
**परिणाम**: Likes अब permanent हैं, cross-device sync होते हैं! 🔥❤️✅
