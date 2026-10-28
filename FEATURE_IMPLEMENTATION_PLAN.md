# AI Music App - Feature & UI Enhancement Implementation Plan

## Overview
This document outlines the implementation plan for 9 major feature enhancements to the AI Music app.

## Features to Implement

### 1. ✅ Explore Tab → Smart Search Shortcut (PRIORITY: HIGH)
**Description:** Double-tap on Explore tab opens Search tab directly
**Files to Modify:**
- `MainActivity.kt` - Add double-tap detection logic in NavigationBar

**Implementation Steps:**
1. Add state variable to track last tap time on Explore tab
2. Implement double-tap detection (< 300ms between taps)
3. Navigate to search when double-tap detected
4. Add haptic feedback for better UX

**Estimated Time:** 1-2 hours

---

### 2. ✅ Voice Search in Search Tab (PRIORITY: HIGH)
**Description:** Add voice search with premium animated UI
**Files to Create:**
- `VoiceSearchManager.kt` - Handle speech recognition
- `VoiceSearchAnimation.kt` - Premium waveform/pulse animation

**Files to Modify:**
- `TopSearch.kt` - Add voice search button
- `MainActivity.kt` - Integrate voice search

**Implementation Steps:**
1. Request RECORD_AUDIO permission
2. Implement SpeechRecognizer integration
3. Create animated voice detection UI (waveform/pulse)
4. Add listening, detecting, and processing states
5. Convert speech to text and trigger search

**Estimated Time:** 4-6 hours

---

### 3. ✅ Settings Icon Visibility Fix (PRIORITY: MEDIUM)
**Description:** Settings icon only visible on Profile tab
**Files to Modify:**
- `MainActivity.kt` - Conditional rendering of settings icon

**Implementation Steps:**
1. Check current route in TopAppBar actions
2. Only show settings icon when route == "library" (Profile tab)
3. Remove settings icon from other tabs

**Estimated Time:** 30 minutes

---

### 4. ✅ Profile Tab Cleanup (PRIORITY: LOW)
**Description:** Remove Playlists, Songs, Albums, Artists tags from Profile
**Files to Modify:**
- `LibraryScreen.kt` - Remove filter chips
- `LibraryMixScreen.kt` - Simplify UI

**Implementation Steps:**
1. Remove ChipsRow component from LibraryScreen
2. Show only LibraryMixScreen content
3. Clean up navigation between filters

**Estimated Time:** 1 hour

---

### 5. ✅ Listening History System (PRIORITY: HIGH)
**Description:** Track and display listening history date-wise
**Files to Create:**
- `ListeningHistory.kt` (Entity) - Database model
- `HistoryDao.kt` - Database access
- `HistoryScreen.kt` - UI for history
- `HistoryViewModel.kt` - Business logic

**Files to Modify:**
- `MusicDatabase.kt` - Add HistoryDao
- `PlayerConnection.kt` - Track song plays
- `LibraryMixScreen.kt` - Add History section

**Implementation Steps:**
1. Create database entity with: songId, playedAt, songDetails
2. Add DAO with date-wise queries
3. Track every song play in PlayerConnection
4. Create UI with date headers (Today, Yesterday, etc.)
5. Add between Liked and Downloaded sections

**Estimated Time:** 6-8 hours

---

### 6. ✅ Premium Login & Signup (PRIORITY: CRITICAL)
**Description:** Firebase Authentication with Google Sign-In and Email/Password
**Files to Create:**
- `AuthManager.kt` - Firebase auth wrapper
- `LoginSignupScreen.kt` - Premium login UI
- `UserProfile.kt` (Entity) - User data model
- `ProfileRepository.kt` - User data management

**Files to Modify:**
- `build.gradle.kts` - Add Firebase dependencies
- `MainActivity.kt` - Check auth state
- `LibraryMixScreen.kt` - Show login prompt

**Dependencies to Add:**
```kotlin
// Firebase
implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
implementation("com.google.firebase:firebase-database-ktx:20.3.1")
implementation("com.google.gms:google-services:4.4.0")
implementation("com.google.android.gms:play-services-auth:20.7.0")
```

**Implementation Steps:**
1. Add Firebase to project (google-services.json)
2. Create beautiful login/signup screen matching app theme
3. Implement Google Sign-In flow
4. Implement Email/Password signup
5. Make login optional - allow guest mode
6. Fetch Google profile data when signing in
7. Store user profile locally and in Firebase

**Estimated Time:** 10-12 hours

---

### 7. ✅ Profile Management (PRIORITY: HIGH)
**Description:** User profile with edit capabilities and persistent profile picture
**Files to Create:**
- `ProfileEditScreen.kt` - Edit profile UI
- `ProfileImageManager.kt` - Handle profile picture storage

**Files to Modify:**
- `LibraryMixScreen.kt` - Add profile section at top
- `AvatarPreferenceManager.kt` - Extend for profile pictures

**Implementation Steps:**
1. Create profile section UI (as per reference image)
2. Add click handler to open edit screen
3. Allow editing: name, email, profile picture
4. Save profile picture to internal storage
5. Persist across app restarts
6. Sync with Firebase when logged in

**Estimated Time:** 6-8 hours

---

### 8. ✅ Firebase Realtime Database Integration (PRIORITY: CRITICAL)
**Description:** Sync user data across devices
**Files to Create:**
- `FirebaseSync.kt` - Sync manager
- `SyncWorker.kt` - Background sync worker

**Files to Modify:**
- All relevant DAOs - Add sync flags
- `PlayerConnection.kt` - Sync on changes

**Data to Sync:**
- Liked songs
- Listening history
- Library
- Top 50
- Downloads metadata
- Playlists
- Albums
- Saved artists

**Implementation Steps:**
1. Design Firebase database structure
2. Create sync manager with conflict resolution
3. Implement upload/download logic
4. Add sync on login
5. Add periodic background sync
6. Handle offline mode gracefully
7. Restore complete state on re-login

**Database Structure:**
```
users/
  {userId}/
    liked_songs/
    history/
    playlists/
    albums/
    artists/
    downloads/
    top_50/
    profile/
```

**Estimated Time:** 12-15 hours

---

### 9. ✅ Smart Download System (PRIORITY: MEDIUM)
**Description:** Controlled download with 30MB session limit
**Files to Create:**
- `DownloadSessionManager.kt` - Track download quota
- `DownloadCacheValidator.kt` - Validate cached files

**Files to Modify:**
- `DownloadUtil.kt` - Add session tracking
- `DownloadManager.kt` - Implement quota logic

**Implementation Steps:**
1. Track downloaded bytes per session
2. Check if song is already cached before downloading
3. Implement 30MB per session limit
4. Download silently in background
5. Show subtle progress indicator
6. Notify when quota reached
7. Reset quota on app restart or after 24 hours

**Estimated Time:** 4-6 hours

---

## Total Estimated Time: 45-60 hours

## Implementation Priority Order:
1. **Phase 1 (Core Auth & Sync):** Features 6, 8 (Login + Firebase)
2. **Phase 2 (User Features):** Features 5, 7 (History + Profile)
3. **Phase 3 (UX Improvements):** Features 1, 2, 3 (Search shortcuts + Voice)
4. **Phase 4 (Polish):** Features 4, 9 (Cleanup + Downloads)

## Prerequisites:
1. Firebase project setup
2. google-services.json file
3. SHA-1 certificate for Google Sign-In
4. RECORD_AUDIO permission in manifest
5. Internet permission (already present)

## Testing Checklist:
- [ ] Double-tap Explore opens Search
- [ ] Voice search works with animations
- [ ] Settings icon only on Profile tab
- [ ] Profile tab shows clean UI
- [ ] History tracks all plays date-wise
- [ ] Login/Signup works (Google + Email)
- [ ] Profile edit saves and persists
- [ ] Firebase syncs all data
- [ ] Download quota enforced
- [ ] Offline mode works
- [ ] Re-login restores state

## Notes:
- All UI should match existing dark theme
- Animations should be smooth (60fps)
- Error handling for network failures
- Graceful degradation when offline
- Security: Never expose Firebase keys in code
