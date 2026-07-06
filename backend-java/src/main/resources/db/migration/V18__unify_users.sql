-- V18: snap_users 追加 username，废弃 snap_admins
ALTER TABLE snap_users ADD COLUMN IF NOT EXISTS username VARCHAR(50);

-- 历史数据：phone 值赋给 username
UPDATE snap_users SET username = phone WHERE username IS NULL AND phone IS NOT NULL;

DO $$ BEGIN
  ALTER TABLE snap_users ADD CONSTRAINT uk_snap_users_username UNIQUE (username);
EXCEPTION WHEN duplicate_table THEN NULL;
END $$;
ALTER TABLE snap_users ALTER COLUMN phone DROP NOT NULL;
ALTER TABLE snap_users DROP CONSTRAINT IF EXISTS chk_user_identifier;
DROP TABLE IF EXISTS snap_admins;
