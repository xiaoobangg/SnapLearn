# 待实施方案记录

> 本文件记录所有讨论中的方案，确认后实施。每次讨论更新对应章节。

---

## v1.1 博客功能 + 用户体系统一

### 1. 博客化改造

**目标**：Admin 端文档管理开放为线上博客，免登录可浏览，登录后可评论。文档支持私有/共享。

#### 1.1 数据库

**snap_documents 加字段**
```sql
ALTER TABLE snap_documents ADD COLUMN visibility VARCHAR(10) DEFAULT 'private';
-- private：仅自己可见可编辑
-- shared：所有人可看，仅作者可编辑
```

**新建评论表**
```sql
CREATE TABLE snap_document_comments (
    id            VARCHAR(36)  PRIMARY KEY,
    document_id   VARCHAR(36)  NOT NULL,
    user_id       VARCHAR(36),           -- 登录用户有值，匿名游客 null
    author_name   VARCHAR(100) NOT NULL DEFAULT '匿名',
    content       TEXT         NOT NULL,
    parent_id     VARCHAR(36),           -- 楼中楼回复
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
```

#### 1.2 后端

**公开接口（免登录，Security 白名单）**

| 端点 | 说明 |
|------|------|
| `GET /api/v1/public/documents` | 博客列表（`published + shared`），分页、分类筛选 |
| `GET /api/v1/public/documents/{id}` | 文章详情 + 渲染 HTML |
| `GET /api/v1/public/documents/{id}/comments` | 评论列表（树形） |
| `POST /api/v1/public/documents/{id}/comments` | 发表评论（登录/匿名均可） |
| `DELETE /api/v1/public/comments/{id}` | 删除评论（仅作者或管理员） |

**Admin 接口改造**

- `SnapDocument` entity 加 `visibility` 字段
- CRUD 接口支持读写 `visibility`
- 列表接口返回 `visibility`

#### 1.3 前端

| 页面 | 路由 | 说明 |
|------|------|------|
| 博客列表 | `/blog` | 文章卡片，分类筛选，免登录 |
| 文章详情 | `/blog/:id` | Markdown 渲染 + 评论区 |
| 文档编辑器（改） | `/documents` | 加 visibility 开关 |
| 登录/注册 | `/login`, `/register` | 新，邮箱/用户名注册 |

#### 1.4 安全规则

- 公开接口仅返回 `status=published AND visibility=shared`
- 编辑接口仅作者本人可操作
- 评论：登录用户记录 userId，匿名只记录 authorName

---

### 2. 用户体系统一

**目标**：废弃 `snap_admins`，统一使用 `snap_users`，用 `role` 字段区分管理员和普通用户。

#### 2.1 当前问题

```
snap_users (小程序用户)          snap_admins (管理端)
├─ phone, wechat_openid          ├─ username, password_hash
├─ no password                   ├─ email, role
└─ 无角色字段                      └─ 无小程序关联
```

两套认证独立，JWT 签发逻辑重复。

#### 2.2 目标结构

```sql
-- snap_users 加两个字段
ALTER TABLE snap_users ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE snap_users ADD COLUMN role VARCHAR(20) DEFAULT 'user';  -- user / admin

-- 废弃表
DROP TABLE snap_admins;
DROP TABLE snap_admin_roles;
DROP TABLE snap_roles;
```

#### 2.3 认证改造

**注册**：`POST /api/v1/auth/register`
- 入参：`username` + `password`
- BCrypt 加密存入 `snap_users.password_hash`，role = user
- 无需邮箱验证、短信服务

**登录**：`POST /api/v1/auth/login`
- 入参：`username` + `password`
- 返回 JWT（含 userId + role）
- admin 登录也走此接口

**其他**：
- `GET /api/v1/auth/me` — 获取当前用户信息
- Admin 拦截器：检查 JWT + `role == "admin"`
- 普通 API：只验证 JWT，不检查 role
- 小程序端：继续保持 wechat_openid 登录，不受影响

#### 2.4 Admin 前端

- 登录页改为 username + password，调用 `/api/v1/auth/login`
- 新增注册页 `/register`（username + password）
- AdminLayout 用户信息从 JWT 解析
- 初始管理员已在 `snap_admins` 中有数据，迁移后手动设置 role=admin 即可

---

### 3. 实施顺序

| 步骤 | 内容 | 影响范围 |
|------|------|---------|
| 1 | V18：snap_documents 加 visibility + snap_document_comments 建表 | 无停机 |
| 2 | V19：snap_users 加字段，删 snap_admins | admin 短暂不可用 |
| 3 | 后端：统一 JWT 认证 + 博客公开接口 + 评论接口 + admin 改造 | 需配合前端 |
| 4 | 前端：博客页面 + 注册/登录页 + 编辑器加开关 | 需配合后端 |

> 状态：方案讨论中，确认后实施
