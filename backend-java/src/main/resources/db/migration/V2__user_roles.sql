-- ============================================================
-- V2: 用户角色重构
-- 统一 snap_users 表，新增角色体系
-- ============================================================

-- 1. snap_users 新增管理端字段
ALTER TABLE snap_users ADD COLUMN IF NOT EXISTS email VARCHAR(100);
ALTER TABLE snap_users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE snap_users ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- 2. 角色表
CREATE TABLE IF NOT EXISTS snap_roles (
    role_code   VARCHAR(30)   PRIMARY KEY,
    role_name   VARCHAR(50)   NOT NULL
);
INSERT INTO snap_roles VALUES ('admin', '管理员') ON CONFLICT DO NOTHING;
INSERT INTO snap_roles VALUES ('user', '普通用户') ON CONFLICT DO NOTHING;

-- 3. 用户角色关联表
CREATE TABLE IF NOT EXISTS snap_user_roles (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL,
    role_code   VARCHAR(30) NOT NULL,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_code),
    FOREIGN KEY (user_id) REFERENCES snap_users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_code) REFERENCES snap_roles(role_code)
);

-- 4. 为已有普通用户分配 user 角色
INSERT INTO snap_user_roles (id, user_id, role_code)
SELECT gen_random_uuid(), id, 'user'
FROM snap_users
WHERE NOT EXISTS (SELECT 1 FROM snap_user_roles WHERE snap_user_roles.user_id = snap_users.id);

-- 5. 将 snap_admins 中的管理员迁移到 snap_users + 分配 admin 角色
INSERT INTO snap_users (id, phone, nickname, email, password_hash, is_active)
SELECT gen_random_uuid(), username, username, email, password_hash, is_active
FROM snap_admins
WHERE NOT EXISTS (SELECT 1 FROM snap_users WHERE snap_users.phone = snap_admins.username);

INSERT INTO snap_user_roles (id, user_id, role_code)
SELECT gen_random_uuid(), u.id, 'admin'
FROM snap_admins a
JOIN snap_users u ON u.phone = a.username
WHERE NOT EXISTS (
    SELECT 1 FROM snap_user_roles ur
    WHERE ur.user_id = u.id AND ur.role_code = 'admin'
);
