-- ============================================================
-- 15. snap_random_test_pool 维度改为 (word_id, question_type, user_id)
-- ============================================================

ALTER TABLE snap_random_test_pool DROP CONSTRAINT IF EXISTS snap_random_test_pool_word_id_user_id_key;

ALTER TABLE snap_random_test_pool ADD COLUMN IF NOT EXISTS question_type VARCHAR(30) DEFAULT 'meaning_select';

ALTER TABLE snap_random_test_pool ADD CONSTRAINT uk_snap_rtp_word_qtype_user UNIQUE (word_id, question_type, user_id);

-- 随机测试打卡词库（系统级，ID 固定）
INSERT INTO snap_word_banks (id, name, type, description)
VALUES ('random-test-bank', '随机测试', 'system', '随机测试自动生成的打卡记录')
ON CONFLICT (id) DO NOTHING;
