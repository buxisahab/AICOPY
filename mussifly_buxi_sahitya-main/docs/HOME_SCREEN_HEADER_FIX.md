# 🎯 HOME SCREEN HEADER FIX - ACTION NEEDED

## ❌ **Current Issue:**
The MusiFly app name/logo, settings button, and search button are still scrolling with content in the HomeScreen.

## 🔍 **What Needs to be Done:**

The main app header (logo + buttons) is likely defined as an `item` inside the `LazyColumn` (around lines 570-1200). It needs to be moved to the fixed header Column along with the source tabs.

## 📍 **Current Structure:**
```kotlin
Column {  // Main container
    Column {  // FIXED - Source tabs (YouTube/YT Music/JioSaavn)
        Row { /* tabs */ }
    }
    
    LazyColumn {  // SCROLLABLE
        item { /* ⚠️ App header HERE - scrolls */ }
        item { /* Source tabs - ALREADY MOVED */ }
        // ... content
    }
}
```

## ✅ **Target Structure:**
```kotlin
Column {  // Main container
    Column {  // FIXED HEADER - Won't scroll
        Row { /* App logo + buttons */ }  // ⭐ MOVE HERE
        Row { /* Source tabs */ }  // ✅ Already here
    }
    
    LazyColumn {  // SCROLLABLE CONTENT
        // ... content only
    }
}
```

## 🔧 **Steps to Fix:**

### **1. Find the App Header:**
Search for one of these in `HomeScreen.kt line 570-1200`:
- `item {` containing app name/title
- `IconButton` for settings
- `IconButton` for search  
- Text("MusiFly") or similar
- Top app bar or navigation header

### **2. Move It:**
Cut that entire `item { }` block and paste it INSIDE the fixed header Column (before the source tabs Row).

### **3. Location:**
Add it in `HomeScreen.kt` around line 505-512 (inside the fixed Column).

## 📝 **Example Code Location:**

Insert app header here (line ~507):
```kotlin
Column(  // Line 506 - Fixed header
    modifier = Modifier
        .windowInsetsPadding(...)
        .fillMaxWidth()
        .then(if (isLiquidGlass) Modifier.liquidGlassSubtle() else Modifier)
) {
    // ⭐ ADD APP HEADER HERE (logo + buttons)
    
    // Source tabs (already here at line ~515)
    Row { /* tabs */ }
}
```

---

## 🎨 **Alternative Quick Fix:**

If the header is complex, you can use `stickyHeader` in LazyColumn:

```kotlin
LazyColumn {
    stickyHeader {
        // App header - logo + buttons
        Row { /* header content */ }
    }
    
    // Rest of content
}
```

---

##  📊 **Progress:**

- ✅ Source tabs - FIXED and have liquid glass
- ⏳ App header (logo + buttons) - NEEDS TO BE FIXED
- ✅ Content - Scrolls correctly

---

**File**: `HomeScreen.kt`  
**Lines to check**: 570-1200 (find app header item)  
**Move to**: Lines 505-512 (fixed header Column)

---

**Once app header is moved, entire top section (logo + buttons + tabs) will be fixed with liquid glass background!** ✨
