# Follow Notification System - Setup Guide

यह guide आपको बताएगी कि कैसे follow/unfollow पर **system push notifications** भेजें (जैसे Instagram में)।

## 📋 Prerequisites

1. ✅ Supabase project setup
2. ✅ Firebase Cloud Messaging (FCM) setup
3. ✅ FCM Server Key (Firebase Console से)

## 🚀 Setup Steps

### Step 1: Deploy Edge Function

```bash
cd d:\mussifly_final_project
supabase functions deploy send-follow-notification
```

### Step 2: Set Environment Variables

Supabase Dashboard में जाएं:
1. Settings → Edge Functions
2. Add secrets:
   - `FCM_SERVER_KEY`: आपकी Firebase Server Key

### Step 3: Enable pg_net Extension

Supabase SQL Editor में run करें:

```sql
CREATE EXTENSION IF NOT EXISTS pg_net;
```

### Step 4: Configure Database Settings

Supabase SQL Editor में run करें (अपनी values replace करें):

```sql
-- Replace with your actual Supabase project URL
ALTER DATABASE postgres SET app.supabase_url = 'https://your-project-ref.supabase.co';

-- Replace with your actual anon key (from Supabase Dashboard → Settings → API)
ALTER DATABASE postgres SET app.supabase_anon_key = 'your-anon-key-here';
```

### Step 5: Run Migration

Supabase SQL Editor में `follow_notification_trigger_with_fcm.sql` file का content run करें।

## 🎯 How It Works

```
User A follows User B
    ↓
Database trigger fires
    ↓
1. Creates notification in database (in-app)
    ↓
2. Calls Edge Function (async)
    ↓
3. Edge Function fetches User B's FCM token
    ↓
4. Sends push notification via FCM
    ↓
5. User B's phone shows system notification 🔔
```

## 📱 Testing

1. User A को login करें
2. User B को follow करें
3. User B के phone पर notification दिखना चाहिए

## 🔍 Debugging

### Check Edge Function Logs:
```bash
supabase functions logs send-follow-notification
```

### Check if FCM token is saved:
```sql
SELECT id, name, fcm_token FROM profiles WHERE id = 'user-id';
```

### Test Edge Function manually:
```bash
curl -X POST 'https://your-project.supabase.co/functions/v1/send-follow-notification' \
  -H 'Authorization: Bearer YOUR_ANON_KEY' \
  -H 'Content-Type: application/json' \
  -d '{
    "follower_id": "follower-user-id",
    "followed_id": "followed-user-id",
    "action": "follow"
  }'
```

## ⚠️ Important Notes

1. **FCM Token**: User को app open करना होगा ताकि FCM token save हो
2. **Permissions**: Android notification permission होनी चाहिए
3. **Internet**: Push notification के लिए internet connection जरूरी है
4. **Rate Limiting**: FCM की rate limits का ध्यान रखें

## 🎨 Notification Customization

Edge Function में customize करें:
- `notification.title`: Notification का title
- `notification.body`: Notification का message
- `notification.icon`: User की profile photo
- `data`: Extra data (deep linking के लिए)

## 📊 Production Checklist

- [ ] Edge Function deployed
- [ ] FCM_SERVER_KEY set
- [ ] pg_net extension enabled
- [ ] Database settings configured
- [ ] Migration applied
- [ ] Tested with real users
- [ ] Error logging setup
- [ ] Rate limiting considered

## 🐛 Common Issues

**Issue**: Notification नहीं आ रहा
- Check: FCM token saved है?
- Check: Edge Function logs में errors?
- Check: Internet connection?
- Check: Notification permission granted?

**Issue**: Edge Function timeout
- Solution: pg_net async call use करें (already implemented)

**Issue**: Wrong notification content
- Check: Follower profile में name है?
- Check: Edge Function में correct data भेज रहे हैं?
