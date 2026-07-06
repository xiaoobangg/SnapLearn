# SnapLearn 拍立学

> 拍照学英语 — 基于 AI 的微信小程序词汇学习应用 + 文档博客系统

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-brightgreen.svg)](https://vuejs.org/)

## 功能特性

**英语学习（微信小程序）**
- 拍照识别英文单词，AI 自动生成释义、例句、记忆技巧
- 每个单词拆分 6 个知识点，分步确认掌握
- 释义选择、单词选择、搭配填空、拼写测试 4 种题型，全部通过才算通关
- 每日打卡（百词斩风格），已知/模糊/未知三档标记
- 随机测试，基于间隔的复习算法

**文档管理 & 博客（Web 端）**
- 语雀式树形编辑器：行内创建、拖拽移动、右键菜单、Markdown 实时预览、自动保存
- 多 Agent AI 助手：主 Agent + 编辑子 Agent + 检索子 Agent，支持批量创建、按名修改
- 公开博客：免登录访问，文档树导航 + 文章列表 + AI 对话，支持评论
- 博客 AI 基于向量检索，只搜索已共享的文档

**后台管理**
- 仪表盘、用户管理、卡片组管理、词库管理、知识库管理
- 声音复刻（阿里 CosyVoice）
- AI 对话日志、访问日志

## 快速开始

### 环境要求

- JDK 17
- PostgreSQL 16（pgvector 扩展）
- Node.js 18+
- Maven 3.8+

### 启动

```bash
# 数据库
createdb snaplearn
psql -d snaplearn -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 后端 (localhost:8080)
cd backend-java
mvn spring-boot:run

# 小程序
cd frontend && npm install
npm run dev:mp-weixin

# 后台管理 (localhost:3001)
cd admin && npm install
npm run dev
```

默认管理员：`admin` / `admin123`

## 技术栈

| 层级 | 技术 |
|------|------|
| 小程序 | uni-app + Vue 3 + TypeScript + Pinia |
| 后台管理 | Vue 3 + Vite + Element Plus + md-editor-v3 |
| 后端 | Spring Boot 3.3.5 + MyBatis-Plus + PostgreSQL 16 |
| AI | Spring AI 1.1.7 + DeepSeek + 阿里 DashScope + ReactAgent |
| 认证 | JWT（jjwt 0.12.6） |
| 可观测 | Prometheus + Grafana |

## AI & 第三方服务

| 服务 | 用途 |
|------|------|
| DeepSeek | AI 对话、ReactAgent 推理 |
| 阿里 DashScope | AI 对话备选、向量 Embedding、TTS 语音合成 |
| 百度 OCR | 拍照文字识别 |
| pgvector | 向量存储与语义检索 |
| Coze 插件平台 | TTS 调用 |

## 项目结构

```
SnapLearn/
├── frontend/                    # uni-app 小程序
│   ├── pages/                   # 首页、拍照、选词、打卡、测试、AI 对话
│   ├── components/              # KnowledgeStepper、TestQuestion、DailyCheckinCard
│   └── store/                   # Pinia 状态管理
├── backend-java/                # Spring Boot 后端
│   ├── controller/              # REST API（含 PublicBlog 公开接口）
│   ├── service/agent/           # 多 Agent AI
│   ├── entity/                  # 数据实体
│   ├── config/                  # JWT、Spring AI、CORS 配置
│   └── prompts/                 # AI 提示词模板
├── admin/                       # Vue 3 后台管理 + 博客
│   ├── pages/documents/         # 文档管理（树形编辑器）
│   ├── pages/blog/              # 博客（列表 + 详情）
│   ├── pages/login/             # 登录/注册
│   ├── components/              # 共享组件
│   └── store/                   # Pinia 状态管理
├── docs/                        # 项目文档
└── observability/              # Prometheus + Grafana 配置
```

## 配置

| 变量 | 说明 |
|------|------|
| `DB_HOST` / `DB_PORT` / `DB_USER` / `DB_PASSWORD` | 数据库连接 |
| `DEEPSEEK_API_KEY` | DeepSeek API Key |
| `DASHSCOPE_API_KEY` | 阿里 DashScope API Key |
| `BAIDU_OCR_API_KEY` / `BAIDU_OCR_SECRET_KEY` | 百度 OCR |

## License

[MIT](LICENSE)
