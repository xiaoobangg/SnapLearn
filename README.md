# SnapLearn 拍立学

> 拍照学英语 — 基于 AI 的微信小程序词汇学习应用

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-brightgreen.svg)](https://vuejs.org/)

## 项目简介

SnapLearn（拍立学）是一款基于 AI 的微信小程序英语学习应用。核心流程：**拍照 → OCR 识别文字 → 用户勾选单词 → AI 生成结构化学习卡片 → 知识点分步学习 → 多题型测试通关**。

### 核心功能

- **拍照学单词**：拍照识别图片中的英文单词，AI 自动生成释义、例句、记忆技巧等结构化内容
- **知识点分步学习**：每个单词拆分为 6 个知识点，逐步确认掌握程度
- **多题型测试**：释义选择、单词选择、搭配填空、拼写测试 4 种题型，全部通过才算通关
- **每日打卡**：百词斩风格的每日单词推送，支持已知/模糊/未知三档标记
- **AI 对话助手**：基于 Spring AI + RAG 知识库的智能对话，支持普通聊天和 Agent 模式
- **声音复刻**：集成阿里 CosyVoice，支持 TTS 音色管理和声音复刻
- **Web 后台管理**：用户管理、卡片组管理、知识库管理、AI 对话日志、可观测性仪表盘

## 技术栈

### 前端

| 模块 | 技术 |
|------|------|
| 小程序端 | uni-app + Vue 3 + TypeScript + Pinia + SCSS |
| 后台管理 | Vue 3 + Vite + Element Plus + ECharts |

### 后端

| 技术 | 版本/说明 |
|------|-----------|
| Spring Boot | 3.3.5 |
| Java | 17 |
| MyBatis-Plus | 3.5.7 |
| PostgreSQL | 16 (pgvector) |
| Spring AI | 1.1.7 |
| Spring AI Alibaba | 1.1.2.0 |
| JWT | jjwt 0.12.6 |
| Hutool | 5.8.32 |

### AI & 第三方服务

| 服务 | 用途 |
|------|------|
| 百度 OCR | 图片文字识别 |
| DeepSeek | AI 对话（Chat / Cheap） |
| 阿里 DashScope | AI 对话、向量 Embedding、TTS (CosyVoice) |
| pgvector | RAG 知识库向量存储 |
| Coze 插件平台 | TTS 调用 |

### 可观测性

| 技术 | 说明 |
|------|------|
| Prometheus | 指标采集（/actuator/prometheus） |
| Grafana | 可视化仪表盘 |

## 项目结构

```
SnapLearn/
├── frontend/          # uni-app 小程序用户端
│   ├── pages/         # 页面（首页、拍照、选词、打卡、测试、AI对话等）
│   ├── components/    # 组件（KnowledgeStepper、TestQuestion、DailyCheckinCard）
│   ├── api/           # API 封装
│   ── store/         # Pinia 状态管理
├── backend-java/      # Spring Boot 后端服务
│   ├── src/main/java/com/snaplearn/
│   │   ├── controller/  # REST API 控制器
│   │   ├── service/     # 业务逻辑层
│   │   ├── entity/      # 数据实体
│   │   ├── mapper/      # MyBatis-Plus Mapper
│   │   ├── config/      # 配置类（JWT、Spring AI、CORS 等）
│   │   ── prompts/     # AI 提示词模板
│   └── src/main/resources/
│       ├── application.yml
│       └── schema.sql   # 数据库表结构
├── admin/             # Vue 3 后台管理系统
│   ├── src/pages/     # 管理页面（仪表盘、用户、卡片组、知识库等）
│   ├── src/layouts/   # 布局组件
│   └── src/store/     # Pinia 状态管理
── docs/              # 项目文档
└── observability/     # Prometheus + Grafana 配置
```

## 快速开始

### 环境要求

- **JDK 17**（必须，不支持 JDK 8）
- **PostgreSQL 16**（需安装 pgvector 扩展）
- **Node.js 18+**
- **Maven 3.8+**

### 1. 数据库准备

```bash
# 创建数据库
createdb snaplearn

# 连接数据库，启用 pgvector 扩展
psql -d snaplearn -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### 2. 后端启动

```bash
cd backend-java

# 配置数据库连接（可选，默认 localhost:5433）
# 通过环境变量或修改 application.yml 设置：
# DB_HOST, DB_PORT, DB_USER, DB_PASSWORD

# 构建
mvn clean package -DskipTests

# 运行
mvn spring-boot:run
```

后端服务默认运行在 `http://localhost:8080`

### 3. 小程序端启动

```bash
cd frontend

npm install

# 微信小程序
npm run dev:mp-weixin

# H5 网页
npm run dev:h5
```

### 4. 后台管理系统启动

```bash
cd admin

npm install
npm run dev
```

后台管理系统运行在 `http://localhost:3001`，默认代理 `/api` 到后端 `localhost:8080`

### 5. 可观测性（可选）

```bash
cd observability

# 启动 Prometheus + Grafana
docker-compose up -d
```

Grafana 仪表盘访问 `http://localhost:3000`

## 配置说明

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_HOST` | 数据库地址 | localhost |
| `DB_PORT` | 数据库端口 | 5433 |
| `DB_USER` | 数据库用户 | postgres |
| `DB_PASSWORD` | 数据库密码 | postgres |
| `DB_NAME` | 数据库名 | snaplearn |
| `BAIDU_OCR_API_KEY` | 百度 OCR API Key | - |
| `BAIDU_OCR_SECRET_KEY` | 百度 OCR Secret Key | - |
| `DASHSCOPE_API_KEY` | 阿里 DashScope API Key | - |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | - |

### 默认管理员账号

后台管理系统默认账号：`admin` / `admin123`（首次登录后请修改密码）

## API 文档

详见 [docs/api.md](docs/api.md)

### 主要端点

| 端点 | 说明 | 认证 |
|------|------|------|
| `POST /api/v1/auth/login` | 管理员登录 | 否 |
| `POST /api/v1/ocr/recognize` | OCR 文字识别 | 否 |
| `POST /api/v1/card-groups` | 创建卡片组 | JWT |
| `GET /api/v1/card-groups` | 卡片组列表 | JWT |
| `POST /api/v1/chat/stream` | AI 流式对话 | JWT |
| `GET /api/v1/chat/conversations` | 对话列表 | JWT |
| `GET /admin/users` | 用户管理 | Admin JWT |
| `GET /admin/knowledge` | 知识库管理 | Admin JWT |

## 数据架构

### 核心表结构

```
snap_words              # 单词文本（唯一）
snap_word_contents      # AI 生成的结构化内容（1:1 与单词）
snap_cards              # 卡片关联（card → word_id + group_id）
snap_knowledge_points   # 每个卡片 6 个知识点，分步学习
snap_card_groups        # 卡片组
snap_word_banks         # 词库
snap_daily_checkins     # 每日打卡记录
snap_chat_conversations # AI 对话会话
spring_ai_chat_memory   # Spring AI 对话记忆
```

### 学习流程

```
拍照 → OCR → 选词 → 创建卡片组
    → AI 生成 word_contents
    → 拆分为 knowledge_points
    → 分步学习（确认每个知识点）
    → 测试（4 种题型）
    → 全部通过 → 卡片组完成
```

## 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## License

[MIT License](LICENSE)

## 致谢

- [uni-app](https://uniapp.dcloud.io/) - 跨端开发框架
- [Spring Boot](https://spring.io/projects/spring-boot) - 后端框架
- [Spring AI](https://spring.io/projects/spring-ai) - AI 集成框架
- [Element Plus](https://element-plus.org/) - UI 组件库
- [百度 OCR](https://ai.baidu.com/tech/ocr) - 文字识别服务
- [阿里 DashScope](https://dashscope.aliyun.com/) - AI 模型服务
