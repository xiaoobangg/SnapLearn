# Next Release (未发布)

---

### 2026-07-06 文档目录整理
- [refactor] 删除 3 个冗余 DONE-*.md 文件（内容已在 releases/v2.0.0.md 和 specs 中）
- [fix] 修复 REQUIREMENTS.md 中重复的数据流向章节
- [feat] 更新 PROJECT.md 中 admin 页面列表（新增 blog/documents/feedbacks）
- [feat] 更新 REQUIREMENTS.md 后台管理需求（新增博客/文档管理/用户反馈/AI 对话）
- [feat] 重写 docs/README.md 目录索引

### 2026-07-05 AI 对话页面优化
- [style] 优化博客列表页(BlogListPage)和博客详情页(BlogDetailPage)样式：统一变量、渐变装饰条、卡片阴影、Markdown 渲染
- [style] 优化文档管理页面(DocumentManagePage)样式：树节点圆角、AI 面板标题装饰条、模型切换按钮
- [style] 优化文档列表页面(DocumentListPage)样式：表格卡片化、筛选栏优化
- [fix] 修复 AI 对话删除按钮右对齐：Teleport 到 body 导致 scoped 样式失效，改用 popper-class + 非 scoped 样式
- [style] 优化 AI 对话删除按钮：hover 才显示、红色 hover 反馈、24px 正方形
- [fix] 修复模型切换按钮右对齐：ai-toolbar 使用 justify-content: space-between
- [fix] 修复首次进入对话页面不自动滚动到最新消息（watch aiMessages deep: true）
- [fix] 修复新建会话不生效：command 缺少冒号前缀（字符串 vs 对象）导致 cmd.type 为 undefined
- [fix] 修复新建会话后自动加载最新会话：添加 isCreatingNew 标志位
- [style] 优化 AI 输入框为 textarea 自适应高度（min 1 行, max 6 行），Enter 发送/Shift+Enter 换行