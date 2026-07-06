-- V21: 用户反馈
CREATE TABLE IF NOT EXISTS snap_feedbacks (
    id          VARCHAR(36)  PRIMARY KEY,
    user_id     VARCHAR(36),
    content     TEXT         NOT NULL,
    status      VARCHAR(20)  DEFAULT 'pending',  -- pending / replied
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS snap_feedback_replies (
    id           VARCHAR(36)  PRIMARY KEY,
    feedback_id  VARCHAR(36)  NOT NULL,
    content      TEXT         NOT NULL,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
