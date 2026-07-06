# SnapLearn 拍立学

> 拍照学英语 — 基于 AI 的微信小程序词汇学习应用 + 文档博客系统

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-brightgreen.svg)](https://vuejs.org/)

## 项目简介

SnapLearn（拍立学）是一款基于 AI 的微信小程序英语学习应用，同时包含**文档管理系统**和**公开博客**。

### 核心功能

**英语学习**
- **拍照学单词**：拍照识别图片中的英文单词，AI 生成释义、例句、记忆技巧
- **知识点分步学习**：每个单词拆分 6 个知识点，逐步确认掌握
- **多题型测试**：释义选择、单词选择、搭配填空、拼写测试
- **每日打卡**：百词斩风格，已知/模糊/未知三档标记
- **随机测试**：基于复习计数的间隔巩固

**文档管理 & 博客**
- **文档管理**：语雀式树形编辑器，行内创建/拖拽移动/右键菜单/自动保存/Markdown 实时预览/图片上传/批量导入
- **多 Agent AI 助手**：主 Agent（ReactAgent）+ 编辑子 Agent + 检索子 Agent，支持 @ 文档引用、批量创建、按名修改
- **博客系统**：公开免登录访问，三列布局（文档树 + 文章列表 + AI 对话），评论系统，向量检索 RAG
- **Web 后台**：用户管理、卡片组管理、知识库管理、文档管理、AI 对话日志

**其他**
- **AI 对话**：Spring AI + RAG 知识库，支持普通聊天和 Agent 模式
- **声音复刻**：阿里 CosyVoice，TTS 音色管理和声音复刻

## 技术栈

| 层级 | 技术 |
|------|------|
| 小程序 | uni-app + Vue 3 + TypeScript + Pinia |
| 后台管理 | Vue 3 + Vite + Element Plus + md-editor-v3 |
| 后端 | Spring Boot 3.3.5 + Java 17 + MyBatis-Plus + PostgreSQL 16 (pgvector) |
| AI | Spring AI 1.1.7 + DeepSeek + 阿里 DashScope + ReactAgent |
| 认证 | JWT (jjwt 0.12.6)，公开博客免登录 |

## 项目结构

```
SnapLearn/
├── frontend/              # uni-app 小程序
├── backend-java/           # Spring Boot 后端
│   ├── controller/         # REST API（含 PublicBlog 公开接口）
│   ├── service/agent/      # 多 Agent AI（DocumentAgentService 等）
│   ├── config/             # ChatClientConfig（Agent Bean 注册）
│   ├── prompts/            # AI 提示词模板（document-master.st 等）
│   └── resources/db/migration/  # Flyway 迁移（V18-V20）
├── admin/                 # Vue 3 后台管理
│   ├── pages/documents/   # 文档管理（树形编辑器）
│   ├── pages/blog/        # 博客（列表 + 详情）
│   └── pages/login/       # 登录/注册
└── docs/                  # 项目文档
```

## 快速开始

### 环境要求

- **JDK 17** | **PostgreSQL 16**（pgvector 扩展） | **Node.js 18+** | **Maven 3.8+**

### 启动

```bash
# 1. 数据库
createdb snaplearn
psql -d snaplearn -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 2. 后端 (localhost:8080)
cd backend-java
mvn spring-boot:run

# 3. 后台管理 (localhost:3001)
cd admin
npm install && npm run dev
```

## 配置

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_HOST` / `DB_PORT` / `DB_USER` / `DB_PASSWORD` | 数据库连接 | localhost:5433 |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | - |
| `DASHSCOPE_API_KEY` | 阿里 DashScope API Key | - |

默认管理员：`admin` / `admin123`

## API 概览

| 端点 | 说明 | 认证 |
|------|------|------|
| `POST /api/v1/auth/login` | 登录 | 否 |
| `POST /api/v1/chat/stream` | AI 流式对话（支持 agent_type=document） | JWT |
| `GET /api/v1/public/documents` | 博客文章列表 | **否** |
| `GET /api/v1/public/tree` | 博客文档树 | **否** |
| `POST /api/v1/public/chat/stream` | 博客 AI 对话 | **否** |
| `GET /api/v1/admin/documents/tree` | 文档管理树 | Admin JWT |
| `PUT /api/v1/admin/documents/{id}/move` | 拖拽移动文档 | Admin JWT |

详见 [docs/api.md](docs/api.md)

## 版本

当前版本 **v2.0.0** — 详见 [docs/releases/v2.0.0.md](docs/releases/v2.0.0.md)
