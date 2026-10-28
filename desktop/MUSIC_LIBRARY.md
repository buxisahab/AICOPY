# 🎵 Music Library - Complete Song List

## ✅ Available Songs (12 Popular Tracks)

### Quick Picks Section:

1. **Never Gonna Give You Up** - Rick Astley
   - Album: Whenever You Need Somebody
   - Duration: 3:33

2. **Blinding Lights** - The Weeknd
   - Album: After Hours
   - Duration: 3:20

3. **Shape of You** - Ed Sheeran
   - Album: ÷ (Divide)
   - Duration: 3:54

4. **Levitating** - Dua Lipa
   - Album: Future Nostalgia
   - Duration: 3:23

5. **Starboy** - The Weeknd ft. Daft Punk
   - Album: Starboy
   - Duration: 3:50

6. **Save Your Tears** - The Weeknd
   - Album: After Hours
   - Duration: 3:35

7. **Peaches** - Justin Bieber ft. Daniel Caesar
   - Album: Justice
   - Duration: 3:18

8. **Stay** - The Kid LAROI & Justin Bieber
   - Album: F*ck Love 3: Over You
   - Duration: 2:21

9. **Anti-Hero** - Taylor Swift
   - Album: Midnights
   - Duration: 3:21

10. **As It Was** - Harry Styles
    - Album: Harry's House
    - Duration: 2:47

11. **Flowers** - Miley Cyrus
    - Album: Endless Summer Vacation
    - Duration: 3:20

12. **Unholy** - Sam Smith & Kim Petras
    - Album: Gloria
    - Duration: 2:36

### Trending Now Section:

1. **Anti-Hero** - Taylor Swift
2. **As It Was** - Harry Styles
3. **Flowers** - Miley Cyrus
4. **Unholy** - Sam Smith & Kim Petras

### Forgotten Favorites Section:

1. **Someone Like You** - Adele
   - Album: 21
   - Duration: 4:45

2. **Rolling in the Deep** - Adele
   - Album: 21
   - Duration: 3:48

## 🎯 Total: 14 Unique Songs

All songs are **real YouTube videos** that can be played!

## 🔄 How It Works:

### Current Implementation:
1. **Fallback Data**: 14 curated popular songs
2. **Real YouTube URLs**: Each song has actual YouTube video ID
3. **High-Quality Thumbnails**: Official artwork from YouTube

### Future (Real API):
1. **YouTube Music API**: Fetch millions of songs
2. **Personalized**: Based on your listening history
3. **Dynamic**: Always fresh content
4. **Search**: Find any song on YouTube Music

## 🎵 Song Categories:

### Pop:
- Blinding Lights, Shape of You, Levitating, Flowers, Peaches

### Hip-Hop/R&B:
- Starboy, Stay, Unholy

### Singer-Songwriter:
- Anti-Hero, As It Was, Someone Like You, Rolling in the Deep

### Classic:
- Never Gonna Give You Up

## 📊 Artists Featured:

1. **The Weeknd** (3 songs)
   - Blinding Lights
   - Starboy
   - Save Your Tears

2. **Adele** (2 songs)
   - Someone Like You
   - Rolling in the Deep

3. **Ed Sheeran** (1 song)
4. **Dua Lipa** (1 song)
5. **Justin Bieber** (2 songs)
6. **Taylor Swift** (1 song)
7. **Harry Styles** (1 song)
8. **Miley Cyrus** (1 song)
9. **Sam Smith** (1 song)
10. **Rick Astley** (1 song)

## 🎨 Features:

- ✅ **Album Artwork**: High-quality thumbnails
- ✅ **Artist Names**: Proper artist credits
- ✅ **Album Info**: Album names included
- ✅ **Duration**: Song lengths displayed
- ✅ **Playable**: All songs work with YouTube

## 🚀 To Add More Songs:

### Option 1: Manual (Easy)
Edit `src/services/YouTubeMusicAPI.js` and add more songs to `getFallbackData()`:

```javascript
{
  id: 'YOUTUBE_VIDEO_ID',
  title: 'Song Title',
  artist: 'Artist Name',
  album: 'Album Name',
  thumbnail: 'https://i.ytimg.com/vi/VIDEO_ID/maxresdefault.jpg',
  duration: 200, // seconds
  url: 'https://www.youtube.com/watch?v=VIDEO_ID',
  videoId: 'VIDEO_ID',
  type: 'song'
}
```

### Option 2: Real API (Advanced)
The code already has YouTube Music API integration that will:
- Fetch real-time data from YouTube Music
- Get personalized recommendations
- Access millions of songs
- Update automatically

## 📝 Notes:

- All songs use **real YouTube video IDs**
- Thumbnails are **official YouTube artwork**
- Songs are **playable** (requires internet)
- **No copyright issues** - streaming from YouTube
- **Same as Android app** - uses YouTube Music

---

**Total Songs Available: 14 Popular Tracks**
**More can be added easily or use real API for millions!** 🎵
