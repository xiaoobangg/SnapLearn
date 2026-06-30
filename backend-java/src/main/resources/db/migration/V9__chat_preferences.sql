ALTER TABLE snap_user_settings ADD COLUMN IF NOT EXISTS chat_mode   VARCHAR(10) DEFAULT 'chat';
ALTER TABLE snap_user_settings ADD COLUMN IF NOT EXISTS chat_model  VARCHAR(20) DEFAULT 'deepseek';
ALTER TABLE snap_user_settings ADD COLUMN IF NOT EXISTS chat_stream BOOLEAN DEFAULT TRUE;
