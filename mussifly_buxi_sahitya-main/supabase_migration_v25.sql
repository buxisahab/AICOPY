-- Migration: Add reply and delete-for-me support to messages table
-- Version: 25
-- Date: 2026-02-12

-- Add new columns for reply functionality
ALTER TABLE messages 
ADD COLUMN IF NOT EXISTS reply_to_message_id UUID,
ADD COLUMN IF NOT EXISTS reply_to_content TEXT,
ADD COLUMN IF NOT EXISTS is_deleted_for_me BOOLEAN DEFAULT FALSE;

-- Add index for efficient reply lookups
CREATE INDEX IF NOT EXISTS idx_messages_reply_to 
ON messages(reply_to_message_id);

-- Add foreign key constraint (optional, for data integrity)
-- This ensures reply_to_message_id references a valid message
ALTER TABLE messages
ADD CONSTRAINT fk_messages_reply_to
FOREIGN KEY (reply_to_message_id) 
REFERENCES messages(id)
ON DELETE SET NULL;

-- Add comment for documentation
COMMENT ON COLUMN messages.reply_to_message_id IS 'ID of the message being replied to';
COMMENT ON COLUMN messages.reply_to_content IS 'Cached content of the replied message for quick display';
COMMENT ON COLUMN messages.is_deleted_for_me IS 'Local-only flag (not synced) - indicates if user deleted message for themselves';
