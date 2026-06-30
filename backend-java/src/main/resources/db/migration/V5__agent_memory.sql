-- ReactAgent 长期记忆表：存储用户个性化信息
CREATE TABLE IF NOT EXISTS snap_agent_memories (
    id           VARCHAR(36)    PRIMARY KEY,
    user_id      VARCHAR(36)    NOT NULL,
    memory_key   VARCHAR(255)   NOT NULL,
    memory_value TEXT,
    created_at   TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_am_user_key ON snap_agent_memories (user_id, memory_key);
