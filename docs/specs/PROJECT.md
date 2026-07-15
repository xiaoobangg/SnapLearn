# SnapLearn（拍立学）技术文档

> 业务需求请参阅 [REQUIREMENTS.md](./REQUIREMENTS.md)；API 端点速查请参阅 [api.md](./api.md)。

## 项目简介

拍立学是一款**微信小程序**英语学习应用，核心流程为：**拍照 → OCR 识别文字 → 用户勾选单词 → AI 生成结构化学习卡片 → 知识点分步学习 → 多题型测试通关**。

此外提供：
- **每日打卡模块**（百词斩风格）：词库 + 间隔重复
- **AI 对话助手**：基于 Spring AI 1.1.7 + RAG 知识库 + ReactAgent 智能体
- **文档管理**：在线 Markdown 编辑、批量导入、发布到向量库、AI 辅助写作
- **Web 后台管理系统**（Vue 3 + Element Plus）：用户管理、卡片组管理、知识库管理、AI 对话日志、可观测性
- **Coze 插件集成**：支持豆包插件平台的 TTS 调用

---

## 技术架构

```
┌──────────────────┐    ┌──────────────────────────────┐
│  uni-app 小程序   │    │  Vue 3 后台管理 (admin/)      │
│  (frontend/)     │    │  Element Plus + ECharts       │
│  微信小程序/H5    │    │  - 用户/卡片组/词库管理        │
│                  │    │  - AI 对话入口 + 对话日志      │
│                  │    │  - 知识库管理 + 音色管理        │
│                  │    │  - Grafana 仪表盘             │
└──────┬───────────┘    └──────┬───────────────────────┘
       │ /api/v1/*             │ /api/v1/* + /api/v1/admin/*
       └──────────┬────────────┘
                  │
       ┌──────────┴──────────────────────────────┐
       │  Spring Boot 3.3.5 + Java 17           │
       │  MyBatis-Plus + PostgreSQL 16 (pgvector)│
       │  Spring AI 1.1.7 + Spring AI Alibaba    │
       │  JWT (jjwt 0.12.6) + Flyway 迁移         │
       │  ReactAgent (spring-ai-alibaba-graph)   │
       └──────────┬──────────────────────────────┘
                  │
    ┌─────────────┼──────────────┬─────────────────┐
    │             │              │                 │
 百度 OCR      DeepSeek         DashScope        pgvector
 (文字识别)    (Chat / Cheap)   (Chat / Embedding (RAG 向量库)
                                / TTS CosyVoice)
       │
       └── Coze 插件平台
           (通过 X-API-Key 认证调用 TTS)

       ┌─────────────────────────────────┐
       │  可观测性（observability/）       │
       │  Prometheus → Grafana            │
       │  抓 /actuator/prometheus         │
       └─────────────────────────────────┘
```

---

## 项目目录结构

```
SnapLearn/
├── frontend/                # uni-app 小程序用户端
│   ├── pages/               # 12 个页面
│   │   ├── index/           # 首页（卡片组列表）
│   │   ├── camera/          # 拍照页
│   │   ├── select-words/    # OCR 结果 + 选词
│   │   ├── card-detail/     # 卡片详情
│   │   ├── checkin/         # 每日打卡
│   │   ├── checkin-calendar/# 打卡日历
│   │   ├── test/            # 卡片组测试
│   │   ├── notebook/        # 学习本
│   │   ├── chat/            # AI 对话（流式 SSE + Agent 模式）
│   │   ├── profile/         # 个人中心
│   │   ├── login/           # 登录
│   │   └── review/          # 复习（已废弃，保留兼容）
│   ├── components/
│   │   ├── KnowledgeStepper.vue   # 知识点分步学习
│   │   ├── TestQuestion.vue       # 4 类题型组件
│   │   └── DailyCheckinCard.vue   # 打卡卡片
│   ├── api/                 # API 封装
│   ├── store/               # Pinia
│   └── styles/
│
├── backend-java/            # Spring Boot 后端
│   └── src/main/
│       ├── java/com/snaplearn/
│       │   ├── entity/        # 实体（含 ChatTrace、ChatConversation、ApiKey、Voice、AgentMemory 等）
│       │   ├── mapper/        # MyBatis-Plus Mapper
│       │   ├── service/
│       │   │   ├── LLMService.java                  # AI 对话 + RAG 流水线编排
│       │   │   ├── ChatTraceService.java            # AI 对话审计日志写入
│       │   │   ├── advisor/
│       │   │   │   └── ChatTraceLoggingAdvisor.java # CallAdvisor + StreamAdvisor
│   │   │   ├── agent/                           # ReactAgent 智能体
│       │   │   │   ├── AgentContext.java            # ThreadLocal 上下文
│       │   │   │   ├── AgentScope.java              # 注解式生命周期管理
│       │   │   │   ├── CardGroupAgentService.java   # ReactAgent 调用编排
│       │   │   │   ├── CardGroupAgentTools.java     # Agent 工具集（@Tool 注解）
│       │   │   │   ├── MemoryChatTools.java         # 长期记忆工具集
│       │   │   │   └── UserIdResolver.java          # userId 统一解析
│       │   │   ├── tts/                             # TTS 语音合成
│       │   │   │   ├── TtsProvider.java             # TTS 引擎抽象接口
│       │   │   │   ├── TtsService.java              # TTS 调度（路由+写文件）
│       │   │   │   ├── DashScopeCosyVoiceProvider.java  # 阿里云 CosyVoice
│       │   │   │   └── VoiceEnrollmentService.java  # 声音复刻 API
│       │   │   ├── BaiduOCRService.java             # 百度 OCR
│       │   │   ├── BaiduTTSService.java             # TTS（旧版百度降级链）
│       │   │   ├── VoiceService.java                # 音色管理
│       │   │   ├── CardAudioService.java            # 卡片音频（合成+缓存）
│       │   │   ├── CardAudioAsyncService.java       # 异步批量预生成
│   │   │   ├── CardGroupService.java            # 卡片组
│       │   │   ├── KnowledgePointService.java       # 知识点拆分/分步
│       │   │   ├── TestService.java                 # 4 类题型生成 + 评分
│       │   │   ├── ErrorBookService.java            # 错题本
│       │   │   ├── WordBankService.java             # 词库
│       │   │   ├── DailyCheckinService.java         # 每日打卡
│       │   │   ├── AdminService.java                # 管理员
│       │   │   ├── ApiKeyService.java               # API Key 管理
│       │   │   ├── AgentMemoryService.java          # 长期记忆 CRUD
│       │   │   └── StatsService.java                # 仪表盘统计
│       │   ├── controller/
│       │   │   ├── ChatController.java              # /api/v1/chat/* 流式+同步（兼容 agent 模式）
│       │   │   ├── ChatConversationController.java  # 会话 CRUD
│       │   │   ├── AdminChatTraceController.java    # AI 对话日志
│       │   │   ├── AdminVoiceController.java        # 音色管理 + 声音复刻
│       │   │   ├── ApiKeyController.java            # API Key 管理
│       │   │   ├── CozeTtsController.java           # Coze 插件专用 TTS
│       │   │   └── ...                               # 其余业务接口
│       │   ├── security/      # JWT 拦截器 + @CurrentUser + ApiKeyAuthFilter
│       │   └── config/
│       │       ├── ChatClientConfig.java            # ChatClient + ChatMemory + RAG 装配
│       │       └── AgentContextAspect.java          # AgentScope 注解切面
│       └── resources/
│           ├── application.yml          # 通用 + management/prometheus
│           ├── application-dev.yml      # 开发覆盖（DEBUG 日志）
│           ├── application-prod.yml     # 生产覆盖（INFO 日志）
│           ├── db/migration/            # Flyway 迁移（V1-V12）
│           │   ├── V1__init.sql
│           │   ├── V2__user_roles.sql
│           │   ├── V3__chat_traces.sql
│           │   ├── V4__graph_checkpoint.sql        # ReactAgent 会话检查点
│           │   ├── V5__agent_memory.sql            # 长期记忆表
│           │   ├── V6__voices_and_card_audios.sql  # 音色 + 音频缓存
│           │   ├── V7__voice_tts_model.sql         # TTS 模型字段
│           │   ├── V8__api_keys.sql                # API Key 表
│           │   ├── V9__chat_preferences.sql        # 聊天偏好设置
│           │   ├── V10__chat_conversation_mode.sql # 会话模式字段调整
│           │   ├── V11__voice_format_volume_pitch.sql # 音色参数扩展
│           │   └── V12__user_phone_nullable.sql    # 用户 phone 可空
│           └── prompts/                 # *.st 模板
│               ├── system-message.st               # 通用聊天系统提示
│               ├── agent-system.st                 # ReactAgent 系统提示
│               ├── generate-card-content.st        # 卡片内容生成
│               ├── query-compression.st            # RAG 多轮压缩
│               ├── query-rewrite.st                # RAG 查询重写
│               ├── query-expansion.st              # RAG 多查询扩展
│               └── test-*.st                       # 测试题生成模板
│
├── admin/                   # Vue 3 后台管理 SPA
│   └── src/pages/
│       ├── login/           # 登录 / 注册
│       ├── dashboard/       # 仪表盘
│       ├── blog/            # 博客列表 + 文章详情
│       ├── documents/       # 文档管理（列表 + 编辑器 + AI 助手）
│       ├── users/           # 用户管理（含详情 + 重置密码 + 角色分配）
│       ├── groups/          # 卡片组管理（列表 + 详情）
│       ├── cards/           # 卡片管理
│       ├── wordbanks/       # 词库
│       ├── wordcontents/    # 单词内容（手动 LLM 刷新）
│       ├── knowledge/       # RAG 知识库（上传/管理）
│       ├── chat/            # AI 对话入口（与小程序端方案一致）
│       ├── chat-traces/     # AI 对话日志列表 + 详情
│       ├── logs/            # API 访问日志
│       ├── voices/          # 音色管理 + 声音复刻
│       ├── api-keys/        # API Key 管理（用户自服务）
│       ├── feedbacks/       # 用户反馈管理
│       └── settings/        # 系统设置
│
├── observability/           # 本地可观测性栈（Docker Compose）
│   ├── docker-compose.yml   # Prometheus + Grafana
│   ├── prometheus.yml       # 服务器版（target = snaplearn-backend:8080）
│   ├── prometheus-dev.yml   # 本地版（target = host.docker.internal:8080）
│   ├── restart.ps1          # Windows PowerShell 管理脚本
│   └── grafana/provisioning/
│       ├── datasources/prometheus.yml      # 数据源（uid=snaplearn-prometheus）
│       └── dashboards/
│           ├── dashboards.yml               # Grafana provisioning 配置
│           └── snaplearn-ai-overview.json   # AI 综合仪表盘
│
├── docker-compose.server.yml    # 生产部署（backend + admin + Prometheus + Grafana）
├── deploy-server.sh             # 服务器一键部署（git pull + mvn + docker build + up）
├── restart-server.sh            # 服务器重启脚本（restart-all / recreate / logs / status）
├── rollback.sh                  # 回滚到上一版本镜像
└── CLAUDE.md                    # Claude Code 项目指引
```

---

## 技术栈详情

### 前端小程序（`frontend/`）

| 层面 | 技术 |
|------|------|
| 框架 | uni-app（Vue 3 + TypeScript） |
| 状态管理 | Pinia |
| 样式 | SCSS |
| 目标平台 | 微信小程序（主）+ H5 |
| 流式接收 | H5 走 `fetch` + ReadableStream；MP-WEIXIN 走 `uni.request` 解析 SSE 后逐字模拟 |

### 后端（`backend-java/`）

| 层面 | 技术 |
|------|------|
| 框架 | Spring Boot 3.3.5 |
| JDK | Java 17 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | **PostgreSQL 16**（含 pgvector 扩展） |
| 数据库迁移 | Flyway（`db/migration/V*.sql`） |
| AI 框架 | **Spring AI 1.1.7** + **Spring AI Alibaba 1.1.2.0** |
| 智能体 | **spring-ai-alibaba-agent-framework**（ReactAgent） |
| LLM | DeepSeek（OpenAI 兼容）+ DashScope qwen-plus |
| Embedding | DashScope text-embedding-v4（1536 维） |
| 向量库 | PgVectorStore（HNSW + COSINE_DISTANCE） |
| 认证 | JWT（jjwt 0.12.6 + HandlerInterceptor）+ API Key |
| JSON | Jackson（SNAKE_CASE） |
| 密码加密 | BCrypt |
| 可观测性 | Spring Boot Actuator + Micrometer Prometheus |

### 后台管理（`admin/`）

| 层面 | 技术 |
|------|------|
| 框架 | Vue 3 + Vite + TypeScript |
| UI 组件库 | Element Plus |
| 图表 | ECharts 5（vue-echarts） |
| HTTP 客户端 | Axios（baseURL 由 `.env.production` 注入 `/api/v1`） |
| 状态管理 | Pinia |
| 路由 | Vue Router 4 |

---

## 数据库表结构

> **架构原则**：单词结构化数据独立存储，卡片组与打卡池都通过 `word_id` 引用 `snap_word_contents`，LLM 刷新后所有引用方自动受益。

### 业务核心

| 表 | 说明 |
|---|---|
| `snap_users` | 用户（phone 可空，需 phone 或 wechat_openid 至少一个） |
| `snap_user_roles` | 用户角色（admin / user） |
| `snap_words` | 单词本体（唯一） |
| `snap_word_contents` | 结构化内容（LLM 生成，可独立刷新） |
| `snap_cards` | 卡片（关联 group + word + status） |
| `snap_card_groups` | 卡片组（状态：待学习/学习中/学习完成/测试中/测试完成） |
| `snap_knowledge_points` | 知识点（拆分自 word_contents，分步展示） |
| `snap_test_questions` | 测试题（4 类题型） |
| `snap_test_attempts` | 答题记录 |
| `snap_error_book` | 错题本（按卡片组归集） |
| `snap_word_bank` / `snap_word_bank_items` | 词库 |
| `snap_user_daily_pool` | 用户打卡池 |
| `snap_daily_checkin_log` | 打卡日志 |
| `snap_user_settings` | 用户偏好（每日新词数 / 复习数 + 聊天偏好 + 音色偏好） |

### TTS 音色

| 表 | 说明 |
|---|---|
| `snap_voices` | 音色配置（name / provider / voice_code / tts_model / format / sample_rate / volume / speech_rate / pitch / instruction） |
| `snap_card_audios` | 卡片音频缓存（card_id + voice_id + audio_type → audio_url） |

### AI 聊天 + RAG

| 表 | 说明 |
|---|---|
| `snap_chat_conversations` | 用户 ↔ 会话映射（业务表，存标题） |
| `SPRING_AI_CHAT_MEMORY` | Spring AI 自管理对话消息（schema 由 Spring AI 创建） |
| `vector_store` | RAG 向量库（pgvector + HNSW） |
| `snap_knowledge_files` | 上传文档元数据 |

### AI 对话审计

| 表 | 说明 |
|---|---|
| `snap_chat_traces` | 每次 chat 请求一条记录：用户消息、AI 回答、token 拆分、耗时、状态 |

字段：
```
id, user_id, chat_id, model,
user_message, response_text,
prompt_tokens, completion_tokens, total_tokens,
duration_ms, status (success/error), error_message,
created_at
```

### ReactAgent 智能体

| 表 | 说明 |
|---|---|
| `GraphThread` | ReactAgent 会话线程（thread_id / thread_name / is_released） |
| `GraphCheckpoint` | 会话检查点（持久化状态数据，parent_checkpoint_id 形成链路） |
| `snap_agent_memories` | 长期记忆（user_id + memory_key + memory_value） |

### API Key 管理

| 表 | 说明 |
|---|---|
| `snap_api_keys` | API Key（user_id / name / key_hash / key_prefix / is_active / last_used_at） |

### 其他

| 表 | 说明 |
|---|---|
| `snap_api_access_logs` | API 访问日志（method/uri/duration/body） |

> 详细字段请见 Flyway 迁移文件：[V1__init.sql](../backend-java/src/main/resources/db/migration/V1__init.sql) 至 [V12__user_phone_nullable.sql](../backend-java/src/main/resources/db/migration/V12__user_phone_nullable.sql)。

---

## API 端点

> 完整端点速查见 [api.md](./api.md)。

### 用户端（小程序）

公开（无需 JWT）：
- `GET /api/health` —— 健康检查
- `POST /api/v1/auth/login` / `/api/v1/auth/dev-login`
- `POST /api/v1/ocr/recognize`
- `GET /api/v1/tts`
- `POST /api/v1/coze/tts`（X-API-Key 认证）

需 JWT：所有 `/api/v1/**` 其余接口（卡片组、学习、测试、打卡、AI 对话、知识库、API Key 等）。

### AI 对话（需 JWT，admin 用户也可访问）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/chat/stream` | SSE 流式对话（兼容 chat / agent 模式） |
| POST | `/api/v1/chat/call` | 同步对话（兼容 chat / agent 模式） |
| GET | `/api/v1/chat/conversations` | 当前用户会话列表 |
| POST | `/api/v1/chat/conversations` | 新建会话 |
| DELETE | `/api/v1/chat/conversations/{chatId}` | 删除会话（同清 memory） |
| GET | `/api/v1/chat/messages/{chatId}` | 获取会话消息 |

### API Key 管理（需 JWT）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/api-keys` | 创建新 Key（返回完整明文，仅此一次） |
| GET | `/api/v1/api-keys` | 我的 Key 列表（不返回明文） |
| DELETE | `/api/v1/api-keys/{id}` | 撤销 Key |

### 管理后台（需 JWT + admin 角色）

| 路径 | 说明 |
|------|------|
| `/api/v1/admin/login` | 管理员登录 |
| `/api/v1/admin/me` | 当前管理员 |
| `/api/v1/admin/dashboard` | 仪表盘统计 |
| `/api/v1/admin/users/**` | 用户管理（含重置密码 / 角色） |
| `/api/v1/admin/groups/**` | 卡片组管理 |
| `/api/v1/admin/word-banks` / `word-contents` | 词库 / 内容管理 |
| `/api/v1/admin/knowledge/**` | RAG 知识库 |
| `/api/v1/admin/chat-traces` | AI 对话日志列表 + 详情 |
| `/api/v1/admin/voices/**` | 音色管理 + 声音复刻 |
| `/api/v1/admin/logs` | API 访问日志 |
| `/actuator/prometheus` | metric endpoint（无业务认证，建议反向代理或防火墙限制） |

---

## 核心业务逻辑

### 1. OCR 识别 → 卡片组创建

```
拍照/相册 → 上传 → 百度 OCR → 提取候选词 → 用户勾选 →
CardGroupService.create():
  ① WordService.findOrCreate() 查/写 snap_words
  ② WordContentService.generate() LLM 批量生成 word_contents
  ③ 创建 cards（仅关联 group + word + status）
  ④ KnowledgePointService.split() 把 word_content 拆 6 个知识点
```

### 2. 卡片组学习与测试

```
状态机：pending → learning → learn_done → testing → test_done
                    ↑___需再学循环___|
```

**学习阶段**：`KnowledgeStepper` 逐知识点展示，用户标记掌握/需再学；本轮结束后循环重学未掌握。

**测试阶段**：`TestService` 随机分配 4 类题型（释义选择/单词选择/组词搭配/单词拼接）→ 一次性提交批改 → 错题进 `snap_error_book` → 全对才能 `test_done`。

### 3. 每日打卡（百词斩风格）

```
DailyCheckinService.todayPush():
  ├─ 新词 N 个（按 user_settings.daily_new_words）
  └─ 复习 M 个（user_daily_pool 中 next_review_at 到期）

用户标记：
  认识  → interval_days × 2
  模糊  → interval_days 不变
  不认识 → interval_days = 1（重置）
```

### 4. AI 对话 + RAG 流水线

[`LLMService.getRetrievalAugmentationAdvisor`](../backend-java/src/main/java/com/snaplearn/service/LLMService.java) 装配 5 段流水线：

```
用户消息
  ↓
① CompressionQueryTransformer    （≥5 轮才生效，中文 prompt）
   把 "它/这个" 消歧成独立问题
  ↓
② RewriteQueryTransformer        （每次都跑，中文 prompt，target=向量数据库）
   清洗冗余客套，精简成检索友好查询
  ↓
③ MultiQueryExpander             （3 个变体 + 原 query = 4 路并发检索）
   语义扩展，提升召回
  ↓
④ VectorStoreDocumentRetriever   （按 user_id 过滤 + similarityThreshold=0.1 + topK=10）
   pgvector 相似度检索（loggingRetriever 包一层打印召回数和分数）
  ↓
⑤ ContextualQueryAugmenter       （allowEmptyContext=false）
   空召回时显式拒答，避免幻觉
  ↓
回答模型（DeepSeek / DashScope，按请求参数选）
```

无状态组件（① ② ③ ⑤）在 `LLMService` 构造期一次性建好，请求路径只重建按 `userId` 过滤的 retriever，避免每次请求重新解析 prompt 模板。

**关键参数**（`LLMService` 顶部常量）：
- `RAG_SIMILARITY_THRESHOLD = 0.1`（中文 embedding 下偏严即漏召）
- `RAG_TOP_K = 10`
- `COMPRESSION_MIN_ROUNDS = 5`
- `RAG_QUERY_VARIANTS = 3`

### 5. AI 对话审计日志

[`ChatTraceLoggingAdvisor`](../backend-java/src/main/java/com/snaplearn/service/advisor/ChatTraceLoggingAdvisor.java) 实现 `CallAdvisor + StreamAdvisor`，order = `Ordered.HIGHEST_PRECEDENCE`（最外层），每次请求新建一份。

```
chat 请求 → 装配 advisor (userId, chatId, model) →
  捕获用户消息 → 转下一层 → 收到响应（stream 模式 doOnNext 累积 chunk） →
  抽取 token usage / duration → ChatTraceService.record() 写库
```

写库失败由 `ChatTraceService` 内部吞掉 + `log.error`，**绝不影响 chat 响应链路**。

Admin 端通过 [`ChatTraceListPage.vue`](../admin/src/pages/chat-traces/ChatTraceListPage.vue) 列表+详情查询。

### 6. RAG 知识库管理

```
Admin 上传文档 → 按扩展名分流：
  .md     → MarkdownDocumentReader（按标题切块）
  其他    → TikaDocumentParser → TokenTextSplitter
       ↓
  DashScope text-embedding-v4 向量化（1536 维）
       ↓
  PgVectorStore 存入 vector_store 表（带 user_id metadata）
```

### 7. TTS 语音合成

**架构**：`TtsProvider` 接口 + 按 `Voice.provider` 路由，主引擎为阿里云 DashScope CosyVoice。

```
CardGroupService.create()  // 异步预生成
  → CardAudioAsyncService.preGenerate()
TtsController / CozeTtsController / AdminVoiceController  // 按需生成
       ↓
  TtsService.synthesizeAndSave(voice, text, prefix, overrides?)
       ↓ 按 voice.provider 路由
  DashScopeCosyVoiceProvider.synthesize()
       ↓ POST SpeechSynthesizer API
  下载音频 → 写本地文件 → 返回相对 URL
```

**参数优先级**：接口传参 > Voice 实体值 > 系统默认值。

**关键参数**（对应 DashScope API `input` 字段）：

| 参数 | Voice 字段 | API 字段 | 默认值 | 范围 |
|------|-----------|----------|--------|------|
| 音频格式 | `format` | `format` | mp3 | mp3 / pcm / wav / opus |
| 采样率 | `sampleRate` | `sample_rate` | 22050 | 8000 / 16000 / 22050 / 24000 / 44100 / 48000 |
| 音量 | `volume` | `volume` | 50 | 0 ~ 100 |
| 语速 | `speechRate` | `rate` | 1.0 | 0.5 ~ 2.0 |
| 音调 | `pitch` | `pitch` | 1.0 | 0.5 ~ 2.0 |
| 指令 | `instruction` | `instruction` | — | 方言/情感/角色控制 |

**音频缓存**：`CardAudioService` 按 `(card_id, voice_id, audio_type)` 缓存，命中直接返回 URL，未命中触发实时合成。

### 8. ReactAgent 智能体

**架构**：基于 `spring-ai-alibaba-agent-framework` 的 ReactAgent，支持对话式创建卡片组。

**工具集**（`CardGroupAgentTools`）：

| 工具 | 说明 |
|------|------|
| `extractWords` | 从文本提取英文单词 |
| `checkExistingWords` | 查询用户已学单词（已学/未学分类） |
| `recommendRelatedWords` | 基于种子词推荐相关词 |
| `createCardGroup` | 创建卡片组 |
| `deleteLastCardGroup` | 撤销最近创建的卡片组 |
| `saveMemory` | 保存长期记忆（考试目标、英语水平等） |
| `recallMemory` | 读取长期记忆 |
| `deleteMemory` | 删除长期记忆 |

**工作流程**：

```
用户消息（含候选词上下文）
  ↓
ReactAgent（基于 agent-system.st 提示词）
  ├─ 查重 checkExistingWords
  ├─ 推荐 recommendRelatedWords（可选）
  └─ 用户确认后 createCardGroup
       ↓
卡片组创建成功 → 返回结果给用户
```

**会话管理**：
- `GraphThread` / `GraphCheckpoint`：ReactAgent 内置状态持久化
- `snap_agent_memories`：用户级长期记忆（跨会话保留）
- `AgentScope` 注解 + `AgentContextAspect`：自动管理 `AgentContext` ThreadLocal 生命周期

**模式切换**：`ChatController` 通过 `mode` 参数切换：
- `mode=chat`（默认）：走原有 LLMService RAG 流程
- `mode=agent`：走 ReactAgent，返回结构化 `AgentChatResponse`

### 9. 文档管理

**架构**：统一管理所有 MD 文档，支持在线编辑、批量导入、发布到向量库（RAG 知识库）。

**数据流**：
```
管理端编辑器 → snap_documents（Markdown 原稿，status=draft）
                   ↓ 发布
         KnowledgeVectorService 解析 → 切片 → 向量化
                   ↓
         snap_knowledge_files + vector_store
                   ↓
         snap_documents.status = 'published'
```

**API**（`AdminDocumentController`）：

| 端点 | 说明 |
|------|------|
| `GET /admin/documents` | 文档列表（搜索/分类/状态筛选） |
| `GET /admin/documents/{id}` | 文档详情 |
| `POST /admin/documents` | 创建文档 |
| `PUT /admin/documents/{id}` | 更新文档 |
| `DELETE /admin/documents/{id}` | 删除文档（已发布先撤销） |
| `POST /admin/documents/{id}/publish` | 发布到向量库 |
| `POST /admin/documents/{id}/unpublish` | 撤销发布 |
| `POST /admin/documents/import` | 批量导入 MD 文件 |

**关键文件**：
- `service/DocumentService.java`：文档 CRUD + 发布/撤销逻辑
- `service/KnowledgeVectorService.java`：解析 → 切片 → 向量化（复用 AdminKnowledgeController 能力）
- `controller/AdminDocumentController.java`：REST API
- `admin/.../documents/`：管理端页面（列表/编辑器/AI助手）

### 10. 资源复用（v2）

**用途**：为 Coze 插件等外部系统提供无 JWT 的认证方式。

**安全设计**：
- 仅存储 `key_hash`（BCrypt），不存储明文
- 返回列表只显示 `key_prefix`（前 8 位），不显示完整 Key
- 创建时仅返回一次完整明文，后续无法找回
- 支持撤销（`is_active=false`）和最后使用时间记录

---

## 可观测性

### 链路

```
Spring AI Observation（自动埋点）
  ↓
Micrometer PrometheusMeterRegistry
  ↓
/actuator/prometheus（暴露文本格式指标）
  ↓
Prometheus（每 15s/30s 抓取，存 30 天）
  ↓
Grafana（仪表盘查询 + 可视化）
```

### 关键指标

| Metric | 含义 |
|--------|------|
| `gen_ai_client_token_usage_total{token_type=input/output/total, request_model}` | 累计 token 消耗（按模型 + 方向） |
| `gen_ai_client_operation_seconds_*` | LLM 调用耗时直方图（P50/P95/P99） |
| `spring_ai_advisor_seconds_*` | RAG 流水线各 advisor 耗时 |
| `http_server_requests_seconds_*` | HTTP 接口耗时 + QPS + 错误率 |

### Grafana 仪表盘

[snaplearn-ai-overview.json](../observability/grafana/provisioning/dashboards/snaplearn-ai-overview.json) 已 provision，包含 8 个面板：累计 input/output token、调用次数、P95 延迟、token 速率/累计趋势、LLM 延迟分位、RAG advisor 耗时、HTTP QPS、5xx 错误率。

### 排查清单

定位"AI 没用知识库"或"为什么慢"，按这个顺序：

1. **`loggingRetriever` 日志**（`LLMService` 内置）—— 直接打印召回数 / 分数 / 内容
2. **`org.springframework.ai.rag: DEBUG`** —— RAG 流水线内部日志
3. **`org.springframework.ai.vectorstore.pgvector: DEBUG`** —— pgvector 底层 SQL
4. **`org.springframework.ai.chat.client: TRACE`** —— 最终 prompt（含 RAG context）
5. **Grafana 仪表盘** —— 看趋势 / 拐点

---

## 构建与运行

### 环境要求

- **JDK 17**（`JAVA_HOME` 必须指向 JDK 17）
- **PostgreSQL 16**（含 pgvector 扩展）
- **Maven 3.6+**
- **Node.js 18+**
- **Docker Desktop**（本地观测栈 / 服务器部署）
- **微信开发者工具**（小程序调试）

### 后端

```bash
cd backend-java

# 编译
JAVA_HOME="D:/Program Files/Java/jdk-17.0.19" mvn clean package -DskipTests

# 启动（自动跑 Flyway 迁移 + 默认 admin/admin123）
JAVA_HOME="D:/Program Files/Java/jdk-17.0.19" mvn spring-boot:run
```

如果 8080 端口被僵尸进程占用，PowerShell 管理员权限：
```powershell
netsh interface ipv4 delete excludedportrange protocol=tcp startport=8080 numberofports=1
```

### 小程序前端

```bash
cd frontend
npm install
npm run dev:mp-weixin    # 微信小程序
npm run dev:h5            # H5
```

### 后台管理

```bash
cd admin
npm install
npm run dev               # localhost:3001，proxy /api → :8080
npm run build             # 产物在 admin/dist
```

### 本地可观测栈（Windows）

```powershell
cd D:\project\SnapLearn\observability
.\restart.ps1 start              # 起 Prometheus + Grafana
.\restart.ps1 status             # 查看状态
.\restart.ps1 logs prometheus    # 查日志
.\restart.ps1 stop               # 停（容器保留）
.\restart.ps1 down               # 停并删容器
```

地址：
- Prometheus：http://localhost:9090
- Grafana：http://localhost:3100（admin / admin）

---

## 部署

### 服务器一键部署

```bash
ssh user@server
cd /home/SnapLearn
./deploy-server.sh
```

脚本流程：`git pull` → `mvn clean package -DskipTests` → `docker tag previous`（用于回滚）→ `docker build backend / admin` → `docker compose up -d` → 健康检查。

### 服务器重启 / 维护

```bash
./restart-server.sh                    # 重启全部
./restart-server.sh backend            # 只重启某服务
./restart-server.sh recreate           # down + up，彻底重建
./restart-server.sh logs backend       # 跟踪日志
./restart-server.sh status             # 查看状态
```

### 回滚

```bash
./rollback.sh        # 从 :previous 标签恢复上一版本
```

### 服务清单（`docker-compose.server.yml`）

| 容器 | 端口 | 用途 |
|------|------|------|
| snaplearn-backend | 8080 | Spring Boot |
| snaplearn-admin | 3001, 443 | Nginx 静态 + SPA |
| snaplearn-prometheus | 9090 | metric 抓取 |
| snaplearn-grafana | 3100 | 仪表盘 |

PostgreSQL 在宿主机运行（不在 compose 内），通过 `extra_hosts: host.docker.internal:host-gateway` 访问。

### 环境变量（`.env.server`）

| 变量 | 说明 |
|------|------|
| `DB_*` | PostgreSQL 连接（HOST/PORT/NAME/USER/PASSWORD） |
| `JWT_SECRET` / `ADMIN_JWT_SECRET` | JWT 密钥（≥32 字节） |
| `LLM_API_KEY` / `LLM_MODEL` / `AI_BASE_URL` | DeepSeek 配置 |
| `AI_DASHSCOPE_API_KEY` / `AI_DASHSCOPE_BASE_URL` | DashScope 配置 |
| `BAIDU_OCR_API_KEY` / `BAIDU_OCR_SECRET_KEY` | 百度 OCR |
| `WECHAT_APP_ID` / `WECHAT_APP_SECRET` | 微信小程序 |
| `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` | Grafana 登录 |
| `SPRING_PROFILES_ACTIVE=prod` | 启用生产 profile（INFO 日志） |

### 部署后验证

```bash
# 后端健康
curl http://localhost:8080/api/health

# Prometheus 抓取状态
curl -s http://localhost:9090/api/v1/targets | grep -E "health|lastError"

# Grafana
curl http://localhost:3100/api/health
```

---

## 默认账号

| 角色 | 账号 | 密码 |
|------|------|------|
| 管理员 | `admin` | `admin123` |
| Grafana | `admin` | `admin`（由 `GRAFANA_ADMIN_PASSWORD` 覆盖） |

管理员账号在首次启动时由 `AdminService.ensureAdminExists()` 自动创建。

---

## 关键设计约定

1. **JWT 双密钥**：用户 token 和管理员 token 使用不同 secret（`JWT_SECRET` vs `ADMIN_JWT_SECRET`），互不通用
2. **角色拦截**：`/api/v1/admin/**` 路径在 `JwtInterceptor` 中强制要求 `roles` 含 `admin`；其他 `/api/v1/**` 仅校验 token 有效
3. **API Key 认证**：`/api/v1/coze/tts` 使用 `X-API-Key` 头，由 `ApiKeyAuthFilter` 处理
4. **数据库迁移**：使用 Flyway，`schema.sql` 仅作参考，**禁止运行时执行**
5. **JSON 字段**：使用 PostgreSQL `JSONB`（不是 MySQL `JSON`），MyBatis-Plus 透明处理
6. **Spring Profile**：`dev`（默认）开 DEBUG/TRACE 日志；`prod` 用 INFO，由 `SPRING_PROFILES_ACTIVE` 切换
7. **Prompt 模板**：通过 [`PromptLoader`](../backend-java/src/main/java/com/snaplearn/util/PromptLoader.java) 从 `classpath:prompts/*.st` 加载，避免硬编码
8. **观测性**：metric 不带任何用户 prompt/response 文本（隐私安全）；要看完整对话内容，去 `snap_chat_traces` 表
9. **TTS 架构**：通过 `TtsProvider` 接口抽象，按 `Voice.provider` 路由；主引擎为 DashScope CosyVoice（`DashScopeCosyVoiceProvider`），旧版百度降级链保留兼容
10. **音色配置**：`snap_voices` 存储 TTS 参数（model / format / sample_rate / volume / speech_rate / pitch / instruction），`TtsService.synthesizeAndSave()` 读取 Voice 字段构造 API 请求；Coze 插件接口支持参数覆盖（接口传参优先于音色默认值）
11. **ReactAgent 工具设计**：工具类必须无状态，userId 从 `ToolContext` metadata 读取；异常内部吞掉返回 `{error:"..."}`，不影响 agent 流程
12. **Agent 上下文管理**：`AgentScope` 注解 + `AgentContextAspect` 切面自动管理 `AgentContext` ThreadLocal 的生命周期（请求前注入、请求后清理）