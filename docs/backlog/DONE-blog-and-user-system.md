# v1.1 博客功能 + 用户体系统一（已完成）

> 实施于 v2.0.0，详情见 `docs/releases/v2.0.0.md`

## 1. 博客化改造

### 1.1 数据库

- [x] `snap_documents` 加 `visibility` 字段 (V19__blog.sql)
- [x] 新建 `snap_document_comments` 评论表 (V19__blog.sql)

### 1.2 后端

- [x] `PublicBlogController` — 公开博客接口（列表/详情/评论/分类/文档树）
- [x] `PublicBlogChatController` — 博客 AI 对话（SSE 流式，无需登录）
- [x] `AdminDocumentController` — create/update 接口支持 visibility
- [x] `PublicBlogController.tree()` — 文档树接口，文件夹放宽权限

### 1.3 前端

- [x] `BlogListPage.vue` — 博客列表，三列布局（树+文章+AI对话）
- [x] `BlogDetailPage.vue` — 文章详情 + 评论 + MdPreview
- [x] `DocumentManagePage.vue` — 编辑器加 visibility 开关
- [x] `LoginPage.vue` / `RegisterPage.vue` — 登录/注册

## 2. 用户体系统一

### 2.1 数据库

- [x] V18__unify_users.sql：snap_users 加 username/role 字段，废弃 snap_admins 相关表

### 2.2 认证改造

- [x] `POST /auth/register` — username + password 注册
- [x] `POST /auth/web-login` — username + password 登录
- [x] Admin 拦截器 + JWT 统一认证

### 2.3 前端

- [x] 登录页改为 username + password
- [x] 新增注册页
- [x] AdminLayout 根据登录状态显示/隐藏菜单
- [x] 未登录可访问博客，登录后才显示管理菜单
