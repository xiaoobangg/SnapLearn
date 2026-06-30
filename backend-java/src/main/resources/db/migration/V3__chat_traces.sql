-- AI 对话 trace 日志：每次 chat 请求的核心数据，供 admin 端审查
CREATE TABLE IF NOT EXISTS snap_chat_traces (
    id                VARCHAR(36)   PRIMARY KEY,
    user_id           VARCHAR(36)   NOT NULL,
    chat_id           VARCHAR(100)  NOT NULL,
    model             VARCHAR(50),
    user_message      TEXT,
    response_text     TEXT,
    prompt_tokens     INT,
    completion_tokens INT,
    total_tokens      INT,
    duration_ms       BIGINT,
    status            VARCHAR(20)   NOT NULL,
    error_message     TEXT,
    created_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_snap_ct_user ON snap_chat_traces (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_snap_ct_chat ON snap_chat_traces (chat_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_snap_ct_time ON snap_chat_traces (created_at DESC);
