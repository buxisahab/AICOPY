import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!
const SUPABASE_SERVICE_ROLE_KEY = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const FCM_SERVER_KEY = Deno.env.get('FCM_SERVER_KEY')!

interface FollowNotificationPayload {
    follower_id: string
    followed_id: string
    action: 'follow' | 'unfollow'
}

Deno.serve(async (req) => {
    try {
        const payload: FollowNotificationPayload = await req.json()
        console.log('Received payload:', payload)

        // Only send notification for follow action
        if (payload.action !== 'follow') {
            return new Response(JSON.stringify({ message: 'Skipped: not a follow action' }), {
                headers: { 'Content-Type': 'application/json' },
                status: 200,
            })
        }

        const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY)

        // Get follower's profile (the person who followed)
        const { data: followerProfile, error: followerError } = await supabase
            .from('profiles')
            .select('username, full_name, avatar_url')
            .eq('id', payload.follower_id)
            .single()

        if (followerError || !followerProfile) {
            console.error('Error fetching follower profile:', followerError)
            throw new Error('Follower profile not found')
        }

        // Get followed user's FCM token
        const { data: followedProfile, error: followedError } = await supabase
            .from('profiles')
            .select('fcm_token')
            .eq('id', payload.followed_id)
            .single()

        if (followedError || !followedProfile || !followedProfile.fcm_token) {
            console.log('No FCM token found for followed user')
            return new Response(JSON.stringify({ message: 'No FCM token available' }), {
                headers: { 'Content-Type': 'application/json' },
                status: 200,
            })
        }

        const displayName = followerProfile.full_name
            ? `${followerProfile.username} (${followerProfile.full_name})`
            : followerProfile.username || 'Someone';

        // Send FCM notification
        const fcmPayload = {
            to: followedProfile.fcm_token,
            priority: 'high',
            notification: {
                title: 'New follower',
                body: `${displayName} started following you.`,
                icon: followerProfile.avatar_url || '',
                sound: 'default',
                click_action: 'FLUTTER_NOTIFICATION_CLICK',
            },
            data: {
                type: 'follow',
                actor_id: payload.follower_id,
                actor_name: displayName,
                actor_avatar_url: followerProfile.avatar_url || '',
                title: 'New follower',
                message: `${displayName} started following you.`,
            },
        }

        const fcmResponse = await fetch('https://fcm.googleapis.com/fcm/send', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `key=${FCM_SERVER_KEY}`,
            },
            body: JSON.stringify(fcmPayload),
        })

        const fcmResult = await fcmResponse.json()
        console.log('FCM Response:', fcmResult)

        if (!fcmResponse.ok) {
            throw new Error(`FCM request failed: ${JSON.stringify(fcmResult)}`)
        }

        return new Response(JSON.stringify({
            success: true,
            fcm_result: fcmResult
        }), {
            headers: { 'Content-Type': 'application/json' },
            status: 200,
        })

    } catch (error) {
        console.error('Error:', error)
        return new Response(JSON.stringify({
            error: error.message
        }), {
            headers: { 'Content-Type': 'application/json' },
            status: 500,
        })
    }
})
