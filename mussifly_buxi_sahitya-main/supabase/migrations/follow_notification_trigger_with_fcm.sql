-- ============================================================================
-- FOLLOW NOTIFICATION SYSTEM - PRODUCTION READY
-- ============================================================================
-- This trigger handles follow/unfollow events and:
-- 1. Creates/deletes in-app notifications in the notifications table
-- 2. Calls Edge Function to send FCM push notifications
-- ============================================================================

-- Drop existing trigger and function if they exist
DROP TRIGGER IF EXISTS on_follower_change ON followers;
DROP FUNCTION IF EXISTS handle_follower_change();

-- Create the trigger function
CREATE OR REPLACE FUNCTION handle_follower_change()
RETURNS TRIGGER AS $$
DECLARE
    follower_name TEXT;
    edge_function_url TEXT;
    supabase_anon_key TEXT;
BEGIN
    -- Get the follower's details from profiles table
    SELECT 
        CASE 
            WHEN full_name IS NOT NULL AND full_name != '' THEN username || ' (' || full_name || ')'
            ELSE username
        END INTO follower_name
    FROM profiles
    WHERE id = NEW.follower_id OR id = OLD.follower_id
    LIMIT 1;

    -- Set Edge Function URL (replace with your actual Supabase project URL)
    edge_function_url := current_setting('app.supabase_url', true) || '/functions/v1/send-follow-notification';
    supabase_anon_key := current_setting('app.supabase_anon_key', true);

    IF (TG_OP = 'INSERT') THEN
        -- Someone followed: Create notification
        INSERT INTO notifications (user_id, type, message, actor_id, is_read, created_at)
        VALUES (
            NEW.followed_id,
            'follow',
            follower_name || ' started following you',
            NEW.follower_id,
            FALSE,
            NOW()
        )
        ON CONFLICT DO NOTHING;

        -- Call Edge Function to send FCM push notification (async, non-blocking)
        PERFORM net.http_post(
            url := edge_function_url,
            headers := jsonb_build_object(
                'Content-Type', 'application/json',
                'Authorization', 'Bearer ' || supabase_anon_key
            ),
            body := jsonb_build_object(
                'follower_id', NEW.follower_id,
                'followed_id', NEW.followed_id,
                'action', 'follow'
            )
        );

        RETURN NEW;

    ELSIF (TG_OP = 'DELETE') THEN
        -- Someone unfollowed: Delete notification
        DELETE FROM notifications
        WHERE user_id = OLD.followed_id
          AND actor_id = OLD.follower_id
          AND type = 'follow';

        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create the trigger
CREATE TRIGGER on_follower_change
    AFTER INSERT OR DELETE ON followers
    FOR EACH ROW
    EXECUTE FUNCTION handle_follower_change();

-- Grant necessary permissions
GRANT EXECUTE ON FUNCTION handle_follower_change() TO authenticated;
GRANT EXECUTE ON FUNCTION handle_follower_change() TO anon;

-- ============================================================================
-- SETUP INSTRUCTIONS:
-- ============================================================================
-- 1. Deploy the Edge Function:
--    supabase functions deploy send-follow-notification
--
-- 2. Set environment variables in Supabase Dashboard:
--    - FCM_SERVER_KEY: Your Firebase Cloud Messaging server key
--
-- 3. Set database configuration (run these in Supabase SQL Editor):
--    ALTER DATABASE postgres SET app.supabase_url = 'https://your-project.supabase.co';
--    ALTER DATABASE postgres SET app.supabase_anon_key = 'your-anon-key';
--
-- 4. Enable pg_net extension (if not already enabled):
--    CREATE EXTENSION IF NOT EXISTS pg_net;
-- ============================================================================
