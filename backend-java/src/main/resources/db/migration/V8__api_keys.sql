-- API Key 管理表
CREATE TABLE IF NOT EXISTS snap_api_keys (
    id           VARCHAR(36)   PRIMARY KEY,
    user_id      VARCHAR(36)   NOT NULL,
    name         VARCHAR(100)  NOT NULL,
    key_hash     VARCHAR(200)  NOT NULL UNIQUE,
    key_prefix   VARCHAR(10)   NOT NULL,
    is_active    BOOLEAN       DEFAULT TRUE,
    last_used_at TIMESTAMP,
    created_at   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_sak_user ON snap_api_keys (user_id);
CREATE INDEX IF NOT EXISTS idx_sak_hash ON snap_api_keys (key_hash);
