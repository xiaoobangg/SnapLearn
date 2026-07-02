-- ============================================================
-- SnapLearn 数据库表结构 v2.0 (PostgreSQL)
-- 设计规范：

-- 启用 pgvector 扩展（向量存储）
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
--   1. 表名：snap_ 项目前缀 + 业务名（snake_case）
--   2. 主键：VARCHAR(36)，使用 UUID
--   3. 时间字段：TIMESTAMP，created_at（自动）、updated_at（应用层更新）
--   4. 索引命名：idx_{表名}_{字段}
--   5. 唯一键命名：uk_{表名}_{字段}
--   6. 外键命名：fk_{表名}_{关联表}
--   7. 状态枚举用 VARCHAR，具体值在注释中标注
--   8. 字符型字段不使用 TEXT 当预计长度 ≤ 500
--   9. JSON 字段使用 JSONB 类型
-- ============================================================

-- ============================================================
-- 1. 用户与认证
-- ============================================================

CREATE TABLE IF NOT EXISTS snap_users (
    id              VARCHAR(36)   PRIMARY KEY,
    phone           VARCHAR(20)   UNIQUE NOT NULL,
    nickname        VARCHAR(100),
    avatar_url      VARCHAR(500),
    wechat_openid   VARCHAR(128)  UNIQUE,
    email           VARCHAR(100),
    password_hash   VARCHAR(255),
    is_active       BOOLEAN       DEFAULT TRUE,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS snap_roles (
    role_code   VARCHAR(30)   PRIMARY KEY,
    role_name   VARCHAR(50)   NOT NULL
);
INSERT INTO snap_roles VALUES ('admin', '管理员') ON CONFLICT DO NOTHING;
INSERT INTO snap_roles VALUES ('user', '普通用户') ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS snap_user_roles (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL,
    role_code   VARCHAR(30) NOT NULL,
    UNIQUE (user_id, role_code),
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_code) REFERENCES snap_roles(role_code)
);

CREATE TABLE IF NOT EXISTS snap_admins (
    id              VARCHAR(36)   PRIMARY KEY,
    username        VARCHAR(50)   UNIQUE NOT NULL,
    email           VARCHAR(100)  UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    role            VARCHAR(20)   DEFAULT 'admin',        -- admin / super_admin
    is_active       BOOLEAN       DEFAULT TRUE,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS snap_user_settings (
    id                  VARCHAR(36)  PRIMARY KEY,
    user_id             VARCHAR(36)  UNIQUE NOT NULL,
    daily_new_words     INT          DEFAULT 10,          -- 每日打卡新词量
    daily_review_words  INT          DEFAULT 20,          -- 每日打卡复习量
    checkin_reminder    BOOLEAN      DEFAULT FALSE,       -- 打卡提醒开关
    reminder_time       TIME,                             -- 提醒时间
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE
);

-- ============================================================
-- 2. 单词与内容（核心：内容独立存储）
-- ============================================================

CREATE TABLE IF NOT EXISTS snap_words (
    id              VARCHAR(36)   PRIMARY KEY,
    word_text       VARCHAR(200)  UNIQUE NOT NULL,        -- 单词本身（去重）
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_snap_words_text ON snap_words (word_text);

CREATE TABLE IF NOT EXISTS snap_word_contents (
    id                  VARCHAR(36)   PRIMARY KEY,
    word_id             VARCHAR(36)   UNIQUE NOT NULL,    -- 一个单词只有一份内容
    pronunciation       VARCHAR(200),
    pos                 VARCHAR(20),                      -- 词性（noun/verb/adj/adv/...）
    general_meaning     TEXT,                             -- 通用释义
    extended_meaning    TEXT,                             -- 语境含义
    example_sentence    TEXT,                             -- 例句
    memory_tip          TEXT,                             -- 记忆技巧
    llm_version         VARCHAR(50),                      -- LLM 生成版本标识
    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (word_id) REFERENCES snap_words(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS snap_knowledge_points (
    id              VARCHAR(36)   PRIMARY KEY,
    word_id         VARCHAR(36)   NOT NULL,
    card_id         VARCHAR(36),                          -- 所属卡片，可为空（打卡池也会用到）
    point_type      VARCHAR(30)   NOT NULL,               -- pronunciation / meaning / extended / example / tip / pos
    content         TEXT          NOT NULL,               -- 知识点内容
    sort_order      INT           DEFAULT 0,              -- 展示顺序
    status          VARCHAR(20)   DEFAULT 'unshown',      -- unshown / shown / confirmed
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (word_id) REFERENCES snap_words(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_snap_kp_word ON snap_knowledge_points (word_id);
CREATE INDEX IF NOT EXISTS idx_snap_kp_card ON snap_knowledge_points (card_id);

-- ============================================================
-- 3. 卡片组与卡片（任务驱动学习）
-- ============================================================

CREATE TABLE IF NOT EXISTS snap_card_groups (
    id              VARCHAR(36)   PRIMARY KEY,
    user_id         VARCHAR(36)   NOT NULL,
    title           VARCHAR(200),
    source_image    VARCHAR(500),
    source_text     TEXT,
    group_status    VARCHAR(20)   DEFAULT 'pending',       -- pending / learning / learn_done / testing / test_done
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_snap_cg_user ON snap_card_groups (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_snap_cg_status ON snap_card_groups (user_id, group_status);

CREATE TABLE IF NOT EXISTS snap_cards (
    id              VARCHAR(36)   PRIMARY KEY,
    group_id        VARCHAR(36)   NOT NULL,
    word_id         VARCHAR(36)   NOT NULL,
    user_id         VARCHAR(36)   NOT NULL,
    sort_order      INT           DEFAULT 0,              -- 组内排序
    card_status     VARCHAR(20)   DEFAULT 'unlearned',     -- unlearned / learning / relearn / mastered / error
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_snap_cards_group_word UNIQUE (group_id, word_id),
    FOREIGN KEY (group_id) REFERENCES snap_card_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (word_id) REFERENCES snap_words(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_snap_cards_group ON snap_cards (group_id);
CREATE INDEX IF NOT EXISTS idx_snap_cards_user ON snap_cards (user_id);

-- ============================================================
-- 4. 测试与错题
-- ============================================================

CREATE TABLE IF NOT EXISTS snap_test_questions (
    id              VARCHAR(36)   PRIMARY KEY,
    group_id        VARCHAR(36)   NOT NULL,
    card_id         VARCHAR(36)   NOT NULL,
    question_type   VARCHAR(30)   NOT NULL,                -- meaning_select / word_select / collocation / spelling
    question_text   TEXT          NOT NULL,
    options         JSONB         NOT NULL,                -- ["选项A","选项B","选项C","选项D"]
    correct_answer  VARCHAR(500)  NOT NULL,
    sort_order      INT           DEFAULT 0,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES snap_card_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (card_id) REFERENCES snap_cards(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_snap_tq_group ON snap_test_questions (group_id);
CREATE INDEX IF NOT EXISTS idx_snap_tq_card ON snap_test_questions (card_id);

CREATE TABLE IF NOT EXISTS snap_test_attempts (
    id              VARCHAR(36)   PRIMARY KEY,
    question_id     VARCHAR(36)   NOT NULL,
    user_id         VARCHAR(36)   NOT NULL,
    user_answer     VARCHAR(500),
    is_correct      BOOLEAN,                                -- 是否答对
    attempt_round   INT           DEFAULT 1,                -- 第几次测试（重测累加）
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (question_id) REFERENCES snap_test_questions(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_snap_ta_user ON snap_test_attempts (user_id);
CREATE INDEX IF NOT EXISTS idx_snap_ta_question ON snap_test_attempts (question_id);

CREATE TABLE IF NOT EXISTS snap_error_book (
    id              VARCHAR(36)   PRIMARY KEY,
    group_id        VARCHAR(36)   NOT NULL,
    card_id         VARCHAR(36)   NOT NULL,
    user_id         VARCHAR(36)   NOT NULL,
    test_attempt_id VARCHAR(36)   NOT NULL,
    resolved        BOOLEAN       DEFAULT FALSE,            -- 是否已重学/重测正确
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_snap_eb_card_attempt UNIQUE (card_id, test_attempt_id),
    FOREIGN KEY (group_id) REFERENCES snap_card_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (card_id) REFERENCES snap_cards(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE,
    FOREIGN KEY (test_attempt_id) REFERENCES snap_test_attempts(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_snap_eb_group ON snap_error_book (group_id);
CREATE INDEX IF NOT EXISTS idx_snap_eb_user ON snap_error_book (user_id);

-- ============================================================
-- 5. 词库与每日打卡
-- ============================================================

CREATE TABLE IF NOT EXISTS snap_word_banks (
    id              VARCHAR(36)   PRIMARY KEY,
    name            VARCHAR(200)  NOT NULL,
    type            VARCHAR(20)   NOT NULL,                 -- preset(预置) / user(用户自建)
    description     VARCHAR(500),
    created_by      VARCHAR(36),                           -- 管理员ID(preset) 或 用户ID(user)
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_snap_wb_type ON snap_word_banks (type);
CREATE INDEX IF NOT EXISTS idx_snap_wb_creator ON snap_word_banks (created_by);
-- 随机测试打卡词库种子数据
INSERT INTO snap_word_banks (id, name, type, description) VALUES ('random-test-bank', '随机测试', 'system', '随机测试自动生成的打卡记录') ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS snap_word_bank_items (
    id              VARCHAR(36)   PRIMARY KEY,
    bank_id         VARCHAR(36)   NOT NULL,
    word_id         VARCHAR(36)   NOT NULL,
    added_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_snap_bank_word UNIQUE (bank_id, word_id),
    FOREIGN KEY (bank_id) REFERENCES snap_word_banks(id) ON DELETE CASCADE,
    FOREIGN KEY (word_id) REFERENCES snap_words(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS snap_user_daily_pool (
    id              VARCHAR(36)   PRIMARY KEY,
    user_id         VARCHAR(36)   NOT NULL,
    bank_id         VARCHAR(36)   NOT NULL,
    word_id         VARCHAR(36)   NOT NULL,
    pool_status     VARCHAR(20)   DEFAULT 'new',             -- new / learning / review / mastered
    interval_days   INT           DEFAULT 0,                 -- 当前复习间隔（天）
    review_count    INT           DEFAULT 0,                 -- 累计复习次数
    last_mark       VARCHAR(20),                             -- 上次标记：known / fuzzy / unknown
    next_review_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    last_review_at  TIMESTAMP,
    created_at      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_snap_pool_user_bank_word UNIQUE (user_id, bank_id, word_id),
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE,
    FOREIGN KEY (bank_id) REFERENCES snap_word_banks(id) ON DELETE CASCADE,
    FOREIGN KEY (word_id) REFERENCES snap_words(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_snap_pool_user ON snap_user_daily_pool (user_id);
CREATE INDEX IF NOT EXISTS idx_snap_pool_review ON snap_user_daily_pool (user_id, next_review_at);

CREATE TABLE IF NOT EXISTS snap_daily_checkin_log (
    id                  VARCHAR(36)   PRIMARY KEY,
    user_id             VARCHAR(36)   NOT NULL,
    bank_id             VARCHAR(36)   NOT NULL,
    checkin_date        DATE          NOT NULL,
    new_words_count     INT           DEFAULT 0,
    review_words_count  INT           DEFAULT 0,
    known_count         INT           DEFAULT 0,
    fuzzy_count         INT           DEFAULT 0,
    unknown_count       INT           DEFAULT 0,
    created_at          TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_snap_checkin_user_date_bank UNIQUE (user_id, checkin_date, bank_id),
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE,
    FOREIGN KEY (bank_id) REFERENCES snap_word_banks(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_snap_checkin_user_date ON snap_daily_checkin_log (user_id, checkin_date DESC);
CREATE INDEX IF NOT EXISTS idx_snap_checkin_date ON snap_daily_checkin_log (checkin_date);

-- ============================================================
-- 6. AI 聊天会话
-- ============================================================

CREATE TABLE IF NOT EXISTS snap_chat_conversations (
    id          VARCHAR(36)   PRIMARY KEY,
    user_id     VARCHAR(36)   NOT NULL,
    chat_id     VARCHAR(100)  NOT NULL UNIQUE,
    title       VARCHAR(200),
    created_at  TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_snap_cc_user ON snap_chat_conversations (user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS snap_api_access_logs (
    id            VARCHAR(36)   PRIMARY KEY,
    user_id       VARCHAR(36),
    method        VARCHAR(10)   NOT NULL,
    uri           VARCHAR(500)  NOT NULL,
    ip            VARCHAR(50),
    request_body  TEXT,
    response_body TEXT,
    duration_ms   BIGINT,
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_saal_time ON snap_api_access_logs (created_at DESC);

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

CREATE TABLE IF NOT EXISTS snap_knowledge_files (
    id          VARCHAR(36)   PRIMARY KEY,
    user_id     VARCHAR(36)   NOT NULL,
    file_name   VARCHAR(500)  NOT NULL,
    file_path   VARCHAR(500)  NOT NULL,
    file_size   BIGINT,
    chunk_count INT           DEFAULT 0,
    upload_time TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- Spring AI JDBC ChatMemory 表（大驼峰命名与框架一致）
CREATE TABLE IF NOT EXISTS SPRING_AI_CHAT_MEMORY (
    conversation_id VARCHAR(36)  NOT NULL,
    content         TEXT         NOT NULL,
    type            VARCHAR(10)  NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp"     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX
    ON SPRING_AI_CHAT_MEMORY (conversation_id, "timestamp");

-- ============================================================
-- V14 资源复用新增表
-- ============================================================
CREATE TABLE IF NOT EXISTS snap_card_kp_progress (
    id       VARCHAR(36)  PRIMARY KEY,
    card_id  VARCHAR(36)  NOT NULL,
    kp_id    VARCHAR(36)  NOT NULL,
    status   VARCHAR(20)  DEFAULT 'unshown',
    UNIQUE (card_id, kp_id)
);

CREATE TABLE IF NOT EXISTS snap_word_audios (
    id           VARCHAR(36)  PRIMARY KEY,
    word_id      VARCHAR(36)  NOT NULL,
    voice_id     VARCHAR(36)  NOT NULL,
    audio_type   VARCHAR(20)  NOT NULL,
    audio_url    VARCHAR(500) NOT NULL,
    duration_ms  INTEGER,
    file_size    BIGINT,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (word_id, voice_id, audio_type)
);

CREATE TABLE IF NOT EXISTS snap_test_session_questions (
    id           VARCHAR(36)  PRIMARY KEY,
    group_id     VARCHAR(36)  NOT NULL,
    card_id      VARCHAR(36)  NOT NULL,
    question_id  VARCHAR(36)  NOT NULL,
    sort_order   INT          DEFAULT 0,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS snap_random_test_pool (
    id              VARCHAR(36)  PRIMARY KEY,
    word_id         VARCHAR(36)  NOT NULL,
    question_type   VARCHAR(30)  NOT NULL DEFAULT 'meaning_select',
    user_id         VARCHAR(36)  NOT NULL,
    review_count    INT          DEFAULT 2,
    source          VARCHAR(20)  DEFAULT 'auto',
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (word_id, question_type, user_id)
);


CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store USING HNSW (embedding vector_cosine_ops);

