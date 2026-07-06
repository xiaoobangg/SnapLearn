-- V20: 文档支持树形嵌套
ALTER TABLE snap_documents ADD COLUMN IF NOT EXISTS parent_id VARCHAR(36);
ALTER TABLE snap_documents ADD COLUMN IF NOT EXISTS doc_type VARCHAR(10) DEFAULT 'document';
