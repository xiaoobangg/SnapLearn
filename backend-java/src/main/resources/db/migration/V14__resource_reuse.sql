-- ============================================================
-- 14. 资源复用：KP去card化、音频去card化、题库去group化、随机测试池
-- ============================================================

-- === 3.1 snap_knowledge_points — 退化为纯索引 ===
-- 备份旧数据到临时表（仅 dev 环境需要，可跳过）
-- CREATE TABLE snap_knowledge_points_backup AS SELECT * FROM snap_knowledge_points;

-- 删除旧约束和索引
ALTER TABLE snap_knowledge_points DROP CONSTRAINT IF EXISTS snap_knowledge_points_card_id_fkey;
DROP INDEX IF EXISTS idx_snap_kp_card;

-- 修改表结构
ALTER TABLE snap_knowledge_points
    DROP COLUMN IF EXISTS card_id,
    DROP COLUMN IF EXISTS content,
    DROP COLUMN IF EXISTS status;

-- 清理旧数据（按 word_id + point_type 去重，保留最早的）
DELETE FROM snap_knowledge_points a
    USING snap_knowledge_points b
    WHERE a.word_id = b.word_id AND a.point_type = b.point_type
      AND a.created_at > b.created_at;

-- 添加唯一约束
ALTER TABLE snap_knowledge_points
    ADD CONSTRAINT uk_snap_kp_word_type UNIQUE (word_id, point_type);

-- === 3.1 新增 snap_card_kp_progress ===
CREATE TABLE IF NOT EXISTS snap_card_kp_progress (
    id       VARCHAR(36)  PRIMARY KEY,
    card_id  VARCHAR(36)  NOT NULL,
    kp_id    VARCHAR(36)  NOT NULL,
    status   VARCHAR(20)  DEFAULT 'unshown',
    UNIQUE (card_id, kp_id)
);
CREATE INDEX IF NOT EXISTS idx_snap_ckp_card ON snap_card_kp_progress(card_id);
CREATE INDEX IF NOT EXISTS idx_snap_ckp_kp ON snap_card_kp_progress(kp_id);

-- === 3.2 snap_card_audios → snap_word_audios ===
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
CREATE INDEX IF NOT EXISTS idx_snap_wa_word ON snap_word_audios(word_id);
CREATE INDEX IF NOT EXISTS idx_snap_wa_voice ON snap_word_audios(voice_id);

-- === 3.3 snap_test_questions — 拆分为题库 + 测次关联 ===
-- 删除旧约束
ALTER TABLE snap_test_questions DROP CONSTRAINT IF EXISTS snap_test_questions_group_id_fkey;
ALTER TABLE snap_test_questions DROP CONSTRAINT IF EXISTS snap_test_questions_card_id_fkey;
DROP INDEX IF EXISTS idx_snap_tq_group;
DROP INDEX IF EXISTS idx_snap_tq_card;

-- 清理 group_id / card_id 数据后删除列
ALTER TABLE snap_test_questions
    DROP COLUMN IF EXISTS group_id,
    DROP COLUMN IF EXISTS card_id;

-- 添加 word_id（如果不存在）
ALTER TABLE snap_test_questions
    ADD COLUMN IF NOT EXISTS word_id VARCHAR(36);

-- 清理旧数据
DELETE FROM snap_test_questions WHERE word_id IS NULL;

-- 添加唯一约束
ALTER TABLE snap_test_questions
    ADD CONSTRAINT uk_snap_tq_word_type UNIQUE (word_id, question_type);

CREATE INDEX IF NOT EXISTS idx_snap_tq_word ON snap_test_questions(word_id);

-- 新建 snap_test_session_questions
CREATE TABLE IF NOT EXISTS snap_test_session_questions (
    id           VARCHAR(36)  PRIMARY KEY,
    group_id     VARCHAR(36)  NOT NULL,
    card_id      VARCHAR(36)  NOT NULL,
    question_id  VARCHAR(36)  NOT NULL,
    sort_order   INT          DEFAULT 0,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_snap_tsq_group ON snap_test_session_questions(group_id);
CREATE INDEX IF NOT EXISTS idx_snap_tsq_card ON snap_test_session_questions(card_id);
CREATE INDEX IF NOT EXISTS idx_snap_tsq_question ON snap_test_session_questions(question_id);

-- === 7. 随机测试 ===
CREATE TABLE IF NOT EXISTS snap_random_test_pool (
    id            VARCHAR(36)  PRIMARY KEY,
    word_id       VARCHAR(36)  NOT NULL,
    user_id       VARCHAR(36)  NOT NULL,
    review_count  INT          DEFAULT 2,
    source        VARCHAR(20)  DEFAULT 'auto',
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (word_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_snap_rtp_user ON snap_random_test_pool(user_id, review_count);
