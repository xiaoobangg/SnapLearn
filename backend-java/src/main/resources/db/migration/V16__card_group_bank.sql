-- 卡片组默认词库（系统级，ID 固定）
INSERT INTO snap_word_banks (id, name, type, description)
VALUES ('card-group-bank', '卡片组词库', 'system', '卡片组通过测试后自动添加的单词')
ON CONFLICT (id) DO NOTHING;
