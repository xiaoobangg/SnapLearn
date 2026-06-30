-- 微信登录用户无手机号，允许 phone 为 NULL
ALTER TABLE snap_users ALTER COLUMN phone DROP NOT NULL;

-- 用户必须至少有 phone 或 wechat_openid 之一
ALTER TABLE snap_users ADD CONSTRAINT chk_user_identifier
    CHECK (phone IS NOT NULL OR wechat_openid IS NOT NULL);
