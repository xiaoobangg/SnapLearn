# API 端点速查

> 更详细的业务说明见 [PROJECT.md](./PROJECT.md) 和 [REQUIREMENTS.md](./REQUIREMENTS.md)。

## 通用约定

- 所有 API 前缀：`/api/v1/`
- 健康检查：`/api/health`（无前缀）
- 认证：JWT Bearer Token（请求头 `Authorization: Bearer <token>`）
- 内容类型：`application/json`（除上传外）
- JSON 字段命名：`snake_case`（Spring Jackson 已配置）
- 时间格式：ISO 8601（`yyyy-MM-ddTHH:mm:ss`）

### JWT 双密钥体系

| 类型 | 密钥环境变量 | 必含 claim | 适用路径 |
|------|-------------|-----------|----------|
| 用户 token | `JWT_SECRET` | `sub=userId` | `/api/v1/**` |
| 管理员 token | `ADMIN_JWT_SECRET` | `sub=adminId` + `roles=admin` | `/api/v1/admin/**` |

`JwtInterceptor` 强制 `/api/v1/admin/**` 路径要求 `roles` 包含 `admin`，其余仅校验 token 合法。

### API Key 认证

`/api/v1/coze/tts` 使用 `X-API-Key` 请求头认证，由 `ApiKeyAuthFilter` 处理。

---

## 公开端点（无需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 健康检查 |
| POST | `/api/v1/auth/login` | 微信手机号登录 |
| POST | `/api/v1/auth/dev-login` | 开发测试登录（仅 DEBUG 模式） |
| POST | `/api/v1/ocr/recognize` | 百度 OCR 图片识别 |
| GET | `/api/v1/tts` | TTS 语音合成（返回音频 URL） |
| POST | `/api/v1/coze/tts` | Coze 插件专用 TTS（X-API-Key 认证） |
| POST | `/api/v1/admin/login` | 管理员登录 |

---

## 用户端 API（需 JWT）

### 认证 / 用户

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/auth/me` | 当前用户信息 |
| GET | `/api/v1/users/settings` | 用户偏好（每日新词数 / 复习数 + 聊天偏好） |
| PUT | `/api/v1/users/settings` | 更新偏好 |

### 卡片组

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/card-groups` | 创建卡片组（自动生成 word_contents + knowledge_points） |
| GET | `/api/v1/card-groups` | 列表（分页 + 状态筛选） |
| GET | `/api/v1/card-groups/{id}` | 详情（含卡片 + 知识点） |
| DELETE | `/api/v1/card-groups/{id}` | 删除 |
| POST | `/api/v1/card-groups/{id}/finish-learning` | 标记学习完成 |
| POST | `/api/v1/card-groups/cards/{cardId}/move` | 移动卡片到其他组 |

### 卡片 / 知识点

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/cards/{id}` | 卡片详情 |
| POST | `/api/v1/cards/{id}/mark` | 标记掌握 / 需再学 |
| POST | `/api/v1/knowledge-points/{id}/confirm` | 确认知识点 |

### 测试

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/tests/{groupId}/start` | 开始测试（生成 4 类题型） |
| POST | `/api/v1/tests/{groupId}/submit` | 整套提交批改 |
| GET | `/api/v1/tests/{groupId}/result` | 测试结果 + 错题 |
| GET | `/api/v1/error-book/{groupId}` | 错题本 |

### 每日打卡

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/checkin/today` | 当日打卡词列表（新词 + 复习） |
| POST | `/api/v1/checkin/mark` | 标记单词（认识/模糊/不认识） |
| GET | `/api/v1/checkin/calendar` | 打卡日历 |
| POST | `/api/v1/checkin/import-from-group` | 卡片组单词导入打卡词库 |
| POST | `/api/v1/checkin/create-group` | 当日打卡词创建卡片组 |

### 词库

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/word-banks` | 词库列表 |
| GET | `/api/v1/word-banks/{id}/items` | 词库单词 |

### AI 对话（支持 chat / agent 模式）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/chat/stream` | SSE 流式对话（`text/event-stream`，支持 mode=chat/agent） |
| POST | `/api/v1/chat/call` | 同步对话（支持 mode=chat/agent） |
| GET | `/api/v1/chat/conversations` | 当前用户会话列表 |
| POST | `/api/v1/chat/conversations` | 新建会话 |
| DELETE | `/api/v1/chat/conversations/{chatId}` | 删除会话（同清 memory） |
| GET | `/api/v1/chat/messages/{chatId}` | 获取会话历史消息 |

**chat 模式请求体**：

```json
{
  "message": "你好",
  "model": "deepseek",
  "chat_id": "abc123"
}
```

**agent 模式请求体**（新增）：

```json
{
  "message": "帮我创建气候变化相关的卡片组",
  "model": "deepseek",
  "chat_id": "abc123",
  "mode": "agent",
  "agent_context": {
    "candidate_words": ["climate", "change", "temperature"],
    "ocr_text": "Climate change is affecting global temperatures..."
  }
}
```

流式响应是标准 SSE：

```
data: 你好

data: ！很高兴

data: 见到你
```

### API Key 管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/api-keys` | 创建新 Key（返回完整明文，仅此一次） |
| GET | `/api/v1/api-keys` | 我的 Key 列表（不返回明文） |
| DELETE | `/api/v1/api-keys/{id}` | 撤销 Key |

**POST /api/v1/api-keys** 请求体：

```json
{
  "name": "我的第一个 Key"
}
```

响应（创建时仅返回一次完整 Key）：

```json
{
  "id": "abc123",
  "name": "我的第一个 Key",
  "api_key": "sk-abcdefghijklmnopqrstuvwxyz",
  "prefix": "sk-abcdef",
  "created_at": "2026-06-30T10:00:00"
}
```

**GET /api/v1/api-keys** 响应（不返回明文）：

```json
[
  {
    "id": "abc123",
    "name": "我的第一个 Key",
    "prefix": "sk-abcdef",
    "is_active": true,
    "last_used_at": "2026-06-30T11:00:00",
    "created_at": "2026-06-30T10:00:00"
  }
]
```

### Coze 插件 TTS

`POST /api/v1/coze/tts`（X-API-Key 认证，由 `ApiKeyAuthFilter` 处理）

请求体：

```json
{
  "text": "你好世界",
  "voice": "longxiaochun_v3",
  "format": "mp3",
  "sample_rate": 24000,
  "volume": 80,
  "rate": 1.2,
  "pitch": 0.9
}
```

| 参数 | 类型 | 必选 | 说明 |
|------|------|------|------|
| `text` | string | 是 | 待合成文本 |
| `voice` | string | 否 | 音色标识，不传则使用用户偏好音色或系统默认 |
| `format` | string | 否 | 音频格式：mp3（默认）/ pcm / wav / opus |
| `sample_rate` | integer | 否 | 采样率：8000 / 16000 / 22050（默认）/ 24000 / 44100 / 48000 |
| `volume` | integer | 否 | 音量 0~100，默认 50 |
| `rate` | float | 否 | 语速 0.5~2.0，默认 1.0 |
| `pitch` | float | 否 | 音调 0.5~2.0，默认 1.0 |

**参数优先级**：接口传参 > 音色库默认配置 > 系统默认值。

响应：

```json
{
  "audio_url": "https://example.com/uploads/audio/coze-abc123_1a2b3c4d.mp3",
  "voice_code": "longxiaochun_v3"
}
```

---

## 管理后台 API（需 JWT + admin 角色）

### 仪表盘 / 用户

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/me` | 当前管理员 |
| GET | `/api/v1/admin/dashboard` | 仪表盘统计 |
| GET | `/api/v1/admin/stats/daily?days=7` | 近 N 天每日统计 |
| GET | `/api/v1/admin/users?page=&pageSize=&keyword=` | 用户列表 |
| GET | `/api/v1/admin/users/{id}` | 用户详情 |
| PUT | `/api/v1/admin/users/{id}/password` | 重置密码（≥6 位） |
| GET | `/api/v1/admin/users/{id}/roles` | 查角色 |
| PUT | `/api/v1/admin/users/{id}/roles` | 改角色（admin / user） |

### 卡片组 / 卡片 / 词库

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/groups` | 卡片组列表 |
| GET | `/api/v1/admin/groups/{id}` | 卡片组详情 |
| DELETE | `/api/v1/admin/groups/{id}` | 删除 |
| GET | `/api/v1/admin/cards` | 卡片列表 |
| GET | `/api/v1/admin/word-banks` | 词库列表 |
| POST | `/api/v1/admin/word-banks` | 创建词库 |
| GET | `/api/v1/admin/word-contents` | 单词内容列表 |
| POST | `/api/v1/admin/word-contents/{wordId}/refresh` | 触发 LLM 重新生成 |

### RAG 知识库

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/admin/knowledge/upload` | 上传文档（multipart/form-data） |
| GET | `/api/v1/admin/knowledge` | 文档列表（含块数） |
| DELETE | `/api/v1/admin/knowledge/{id}` | 删除文档 + 向量 |

### AI 对话日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/chat-traces?page=&pageSize=&userId=&chatId=&status=` | 分页列表（user_message / response_text 列表预览截断 100 字） |
| GET | `/api/v1/admin/chat-traces/{id}` | 详情（完整 prompt + response + token 拆分 + error_message） |

### 系统

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/logs?page=&pageSize=&uri=` | API 访问日志 |

### 音色管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/voices` | 音色列表 |
| POST | `/api/v1/admin/voices` | 新建音色 |
| PUT | `/api/v1/admin/voices/{id}` | 编辑音色 |
| DELETE | `/api/v1/admin/voices/{id}` | 删除音色 |
| POST | `/api/v1/admin/voices/{id}/default` | 设为默认音色 |
| GET | `/api/v1/admin/voices/catalog` | 浏览官方音色库 |
| POST | `/api/v1/admin/voices/import` | 从官方库批量导入 |
| POST | `/api/v1/admin/voices/test/{id}` | 测试合成（返回音频 URL） |
| POST | `/api/v1/admin/voices/enroll` | 声音复刻（multipart 或 JSON） |
| GET | `/api/v1/admin/voices/enroll` | 已复刻音色列表 |
| POST | `/api/v1/admin/voices/enroll/import` | 导入复刻音色 |
| DELETE | `/api/v1/admin/voices/enroll/{voiceCode}` | 删除复刻音色 |

音色对象字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 主键 |
| `name` | string | 显示名称 |
| `provider` | string | 引擎（当前仅 dashscope） |
| `voice_code` | string | 音色标识（对应 DashScope voice 参数） |
| `tts_model` | string | 合成模型，默认 cosyvoice-v3-plus |
| `format` | string | 音频格式：mp3（默认）/ pcm / wav / opus |
| `sample_rate` | integer | 采样率 Hz，默认 22050 |
| `volume` | integer | 音量 0~100，默认 50 |
| `speech_rate` | float | 语速 0.5~2.0，默认 1.0 |
| `pitch` | float | 音调 0.5~2.0，默认 1.0 |
| `instruction` | string | 指令文本（方言/情感/角色控制，可选） |
| `description` | string | 描述 |
| `is_default` | boolean | 是否系统默认音色 |
| `is_active` | boolean | 是否启用 |

---

## 可观测端点

| 端点 | 说明 | 暴露范围 |
|------|------|----------|
| `/actuator/health` | 应用健康（生产模式 `show-details=never`） | 公开 |
| `/actuator/info` | 应用信息 | 公开 |
| `/actuator/metrics` | Micrometer 指标列表 | 公开 |
| `/actuator/prometheus` | Prometheus 格式指标 | **公开（生产建议防火墙限制或反向代理认证）** |

`/actuator/prometheus` 暴露的关键指标：

| Metric | 类型 | 含义 |
|--------|------|------|
| `gen_ai_client_token_usage_total` | counter | token 累计（按 model + token_type） |
| `gen_ai_client_operation_seconds_*` | histogram | LLM 调用耗时分布 |
| `spring_ai_advisor_seconds_*` | histogram | RAG advisor 耗时（如 Spring AI 版本支持） |
| `http_server_requests_seconds_*` | histogram | HTTP 接口耗时 |

> Metric 不包含 prompt / response 文本（隐私安全）。完整对话内容看 `snap_chat_traces` 表。

---

## 错误返回

| HTTP 状态 | 含义 | 响应体 |
|-----------|------|--------|
| 401 | 未登录 / Token 过期 / Token 非法 | `{"detail":"需要登录"}` 等 |
| 403 | 权限不足（缺 admin 角色） | `{"detail":"需要管理员权限"}` |
| 400 | 业务参数错误 | `{"detail":"具体错误信息"}` |
| 502 | 上游 AI 服务错误 | `{"detail":"LLM 服务异常"}` |
| 5xx | 服务器内部错误 | 标准 Spring 错误响应 |

业务异常通过 `BusinessException(code, message)` 抛出，全局拦截转 JSON。

---

## 测试用 curl 模板

```bash
# 用户登录（dev 模式）
curl -X POST http://localhost:8080/api/v1/auth/dev-login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000"}'

# 拿到 token 后调用 AI 对话（chat 模式）
curl -X POST http://localhost:8080/api/v1/chat/call \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"message":"你好","model":"deepseek","chat_id":"test-1"}'

# AI 对话（agent 模式，创建卡片组）
curl -X POST http://localhost:8080/api/v1/chat/call \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"message":"帮我创建气候变化相关的卡片组","model":"deepseek","chat_id":"agent-1","mode":"agent"}'

# 流式（注意 -N 不缓冲）
curl -N -X POST http://localhost:8080/api/v1/chat/stream \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"message":"讲个笑话","model":"deepseek","chat_id":"test-1"}'

# 创建 API Key
curl -X POST http://localhost:8080/api/v1/api-keys \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"测试 Key"}'

# 查看 API Key 列表
curl http://localhost:8080/api/v1/api-keys \
  -H "Authorization: Bearer <token>"

# 管理员登录
curl -X POST http://localhost:8080/api/v1/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 查询 AI 对话日志
curl http://localhost:8080/api/v1/admin/chat-traces?page=1&pageSize=20 \
  -H "Authorization: Bearer <admin-token>"

# Coze 插件 TTS（使用 API Key）
curl -X POST http://localhost:8080/api/v1/coze/tts \
  -H "X-API-Key: sk-abcdefghijklmnopqrstuvwxyz" \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello world","voice":"longxiaochun_v3"}'
```