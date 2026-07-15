# SnapLearn 文档目录

```
docs/
├── releases/                    ← 版本发布记录
│   ├── v1.0.0.md                v1.0.0 发布
│   ├── v2.0.0.md                v2.0.0 发布（文档管理 + 博客 + 多 Agent AI）
│   └── NEXT-RELEASE.md          待发布变更记录
├── backlog/                     ← 需求池
│   └── SPRINT-BACKLOG.md        当前待办事项
└── specs/                       ← 技术规格文档
    ├── PROJECT.md               项目架构与技术栈
    ├── DATABASE.md              数据库表结构设计
    ├── REQUIREMENTS.md           业务需求文档
    └── api.md                   API 端点速查
```

### 文档说明

| 文档 | 用途 |
|------|------|
| `PROJECT.md` | 完整的项目架构、技术栈、业务逻辑、构建部署说明 |
| `DATABASE.md` | 29 张表的完整结构、索引、关系图、数据流转 |
| `REQUIREMENTS.md` | 16 个功能模块的业务需求、优先级、状态体系 |
| `api.md` | 所有 REST API 端点、请求/响应示例、curl 测试模板 |
| `SPRINT-BACKLOG.md` | 当前待实施方案 |
| `NEXT-RELEASE.md` | 头部带日期标记的待发布变更 |
| `releases/v1.0.0.md` | v1.0.0 核心功能与变更记录 |
| `releases/v2.0.0.md` | v2.0.0 文档管理、博客、多 Agent AI 变更清单 |

### 工作流

讨论方案 → 确认后实施 → 更新 NEXT-RELEASE.md → 发布时整理为 releases/v{version}.md