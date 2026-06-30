-- 音色字典
CREATE TABLE IF NOT EXISTS snap_voices (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    provider    VARCHAR(50)  NOT NULL,
    voice_code  VARCHAR(100) NOT NULL,
    description TEXT,
    is_default  BOOLEAN      DEFAULT FALSE,
    is_active   BOOLEAN      DEFAULT TRUE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (provider, voice_code)
);

-- 卡片音频（card × voice × type）
CREATE TABLE IF NOT EXISTS snap_card_audios (
    id          VARCHAR(36)  PRIMARY KEY,
    card_id     VARCHAR(36)  NOT NULL,
    voice_id    VARCHAR(36)  NOT NULL,
    audio_type  VARCHAR(20)  NOT NULL,
    audio_url   VARCHAR(500) NOT NULL,
    duration_ms INTEGER,
    file_size   BIGINT,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (card_id, voice_id, audio_type),
    FOREIGN KEY (card_id)  REFERENCES snap_cards(id)  ON DELETE CASCADE,
    FOREIGN KEY (voice_id) REFERENCES snap_voices(id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_sca_card ON snap_card_audios (card_id);

-- 用户偏好的音色
ALTER TABLE snap_user_settings ADD COLUMN IF NOT EXISTS voice_id VARCHAR(36);

-- 音色数据通过 Admin → 浏览官方音色库 → 勾选导入，不使用种子数据
