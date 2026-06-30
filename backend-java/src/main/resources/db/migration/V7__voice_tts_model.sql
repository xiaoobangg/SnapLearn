ALTER TABLE snap_voices ADD COLUMN IF NOT EXISTS tts_model VARCHAR(100) DEFAULT 'cosyvoice-v3-plus';
ALTER TABLE snap_voices ADD COLUMN IF NOT EXISTS sample_rate INTEGER DEFAULT 24000;
ALTER TABLE snap_voices ADD COLUMN IF NOT EXISTS speech_rate DOUBLE PRECISION DEFAULT 1.0;
ALTER TABLE snap_voices ADD COLUMN IF NOT EXISTS instruction VARCHAR(500);

-- 更新已有音色：系统音色升级为 v3-plus，复刻音色保持原 target_model
UPDATE snap_voices SET tts_model = 'cosyvoice-v3-plus'
    WHERE (tts_model IS NULL OR tts_model = '')
    AND description NOT LIKE '%声音复刻%';
-- 复刻音色保持原有模型不变
UPDATE snap_voices SET sample_rate = 24000 WHERE sample_rate IS NULL;
UPDATE snap_voices SET speech_rate = 1.0 WHERE speech_rate IS NULL;
