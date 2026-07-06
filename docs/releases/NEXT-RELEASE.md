# Next Release (未发布)

---

### 2026-07-05

- [feat] 文档树行内创建：替换 browser `prompt()` 为语雀式原地输入（Enter 确认 / Escape 取消）
- [feat] 文档节点右键新增"新建文件夹"（同级），树空白区域右键弹出根级创建菜单
- [feat] 文档树新增空文件夹占位提示（"暂无文档"）
- [feat] Markdown 编辑器升级为 md-editor-v3：工具栏 + 分屏预览 + 语法高亮，移除手写正则渲染
- [feat] 右键菜单添加快捷键提示（Ctrl+N、F2、Del 等）
- [feat] 文档编辑器新增可见性切换按钮（私有/已共享），树节点显示共享标识 🌐
- [feat] 后端 create 接口支持 `visibility` 参数，新建文档默认 private
- [fix] **修复树形结构失效**：Jackson SNAKE_CASE 导致 API 返回 `parent_id`/`doc_type`，前端读取 `parentId`/`docType` 始终为 undefined，所有节点被放到根级别。新增 `normalizeDoc()` 统一转换字段名
- [style] 新增创建行动画（滑入 + 虚线边框）和新建节点高亮动画
- [refactor] DocumentManagePage 移除 `prompt()` 调用，统一为 `startCreate`/`confirmCreate`/`cancelCreate` 流程

---

### 2026-07-04 (下午)

- [feat] 用户体系统一：snap_users 加 username，废弃 snap_admins，保留 snap_roles/snap_user_roles RBAC
- [feat] Web 注册：POST /auth/register（username+password → role=user）
- [feat] Web 登录：POST /auth/web-login
- [feat] Admin 注册页面 + 登录页注册链接
- [refactor] AdminService.login 改为查 username 字段
- [refactor] User entity 加 username 字段

---

### 2026-07-04 (上午)

- [feat] 用户体系统一：snap_users 加 username/role 字段，废弃 snap_admins/snap_user_roles/snap_roles
- [feat] Web 端注册登录：POST /auth/register + /auth/web-login，BCrypt 加密
- [feat] Admin 注册页面：username + password 注册，自动登录
- [refactor] AdminService 统一认证：直接查 role 字段，不再查 snap_user_roles 关联表
- [refactor] authApi 改为调用 /auth/web-login 和 /auth/register
- [fix] JWT 白名单增加 /auth/web-login 和 /auth/register

---

### 2026-07-04 (上午)

- [fix] 打卡页 finishCheckin 调用后端 completeCheckin 接口，写入 snap_daily_checkin_log
- [fix] CompleteRequest / MarkRequest 加 @JsonProperty，修复 Jackson SNAKE_CASE 映射问题
- [fix] logCheckin 改为 upsert，同一天多次打卡不会重复插入
- [fix] KnowledgePointService.getByCardIdWithProgress 修复 selectById 参数错误（wordId 非主键）
- [fix] 资源复用迁移遗漏：snap_word_audios 等 4 张 V14 表加到 schema.sql
- [fix] Audio 缓存修复：TtsController 加 wordId 直传参数，CardResponse 加 wordId 字段
- [fix] 随机测试 markWrong 从提交后批量改为每道题即时调用
- [fix] snap_random_test_pool 维度改为 (word_id, question_type, user_id)
- [fix] 测试题查询去掉 group_id 列，改为通过 cards→wordIds 查询
- [fix] 每日打卡自动 finishCheckin，最后一个单词标记后自动完成
- [fix] Nginx ^~ 前缀修复图片 404 问题（正则优先级）
- [fix] Flyway V15 checksum mismatch 修复

- [feat] 随机测试入口：小程序端页面 + 首页入口卡片
- [feat] 随机测试每日打卡记录（完成随机测试即生成打卡记录）
- [feat] 卡片组词库：测试通过后单词自动入池
- [feat] 词库列表按 bank_items/daily_pool 过滤空词库
- [feat] 打卡分享页签到日历改为标准月历布局（星期头 + 7 列网格 + 日数字）
- [feat] 打卡分享页从 DOM+Canvas 分离改为纯 Canvas 预览（所见即所得）
- [feat] 文档管理方案：资源复用 + 随机测试全部完成
- [feat] 项目命名为"拍立学"

- [style] 每日打卡页面全面优化：进度条笑脸、自动切词、完整知识点展示
- [style] 测验/卡片学习/打卡进度条笑脸位置统一（基于已完成进度百分比定位）
- [style] 测试完成页只显示返回首页按钮
- [style] 打卡分享页精简（移除统计行、分隔线、扫码文案），连续+累计左右并列

- [refactor] 随机测试 markWrong 前端即时驱动，不再依赖后端批量提交
- [refactor] 文档管理结构重组：docs/releases/ + backlog/ + specs/
- [refactor] CLAUDE.md 写入需求开发流程 + 变更登记规则
