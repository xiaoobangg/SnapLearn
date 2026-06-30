ALTER TABLE snap_voices ADD COLUMN IF NOT EXISTS format VARCHAR(10) DEFAULT 'mp3';
ALTER TABLE snap_voices ADD COLUMN IF NOT EXISTS volume INTEGER DEFAULT 50;
ALTER TABLE snap_voices ADD COLUMN IF NOT EXISTS pitch DOUBLE PRECISION DEFAULT 1.0;

-- 更新已有音色的默认值
UPDATE snap_voices SET format = 'mp3' WHERE format IS NULL;
UPDATE snap_voices SET volume = 50 WHERE volume IS NULL;
UPDATE snap_voices SET pitch = 1.0 WHERE pitch IS NULL;
