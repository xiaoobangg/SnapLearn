-- ============================================================
-- 13. 文档管理 (Document Management)
-- ============================================================
CREATE TABLE IF NOT EXISTS snap_documents (
    id                 VARCHAR(36)  PRIMARY KEY,
    user_id            VARCHAR(36)  NOT NULL,
    title              VARCHAR(300) NOT NULL,
    content            TEXT         NOT NULL,
    category           VARCHAR(50),
    tags               VARCHAR(500),
    status             VARCHAR(20)  DEFAULT 'draft',
    source_type        VARCHAR(20)  DEFAULT 'md',
    source_name        VARCHAR(300),
    file_size          BIGINT,
    sort_order         INT          DEFAULT 0,
    knowledge_file_id  VARCHAR(36),
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_snap_docs_user ON snap_documents(user_id);
CREATE INDEX IF NOT EXISTS idx_snap_docs_status ON snap_documents(user_id, status);
CREATE INDEX IF NOT EXISTS idx_snap_docs_category ON snap_documents(user_id, category);
