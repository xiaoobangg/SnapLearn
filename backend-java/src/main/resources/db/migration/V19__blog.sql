-- V19: 博客功能 — 文档可见范围 + 评论
ALTER TABLE snap_documents ADD COLUMN IF NOT EXISTS visibility VARCHAR(10) DEFAULT 'private';

CREATE TABLE IF NOT EXISTS snap_document_comments (
    id            VARCHAR(36)  PRIMARY KEY,
    document_id   VARCHAR(36)  NOT NULL,
    user_id       VARCHAR(36),
    author_name   VARCHAR(100) NOT NULL DEFAULT '匿名',
    content       TEXT         NOT NULL,
    parent_id     VARCHAR(36),
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_snap_dc_doc ON snap_document_comments(document_id, created_at);
