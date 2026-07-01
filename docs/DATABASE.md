# SnapLearn 数据库表结构说明

## 数据库概述

| 属性 | 值 |
|------|-----|
| 数据库类型 | PostgreSQL 16 |
| 字符集 | UTF-8 |
| 表前缀 | `snap_` |
| 主键类型 | VARCHAR(36) (UUID) |
| JSON 类型 | JSONB |
| 时间字段 | TIMESTAMP (UTC) |

## 设计规范

1. **表名**：`snap_` + 业务名（snake_case）
2. **主键**：VARCHAR(36)，使用 UUID
3. **时间字段**：`created_at`（自动）、`updated_at`（应用层更新）
4. **索引命名**：`idx_{表名}_{字段}`
5. **唯一键命名**：`uk_{表名}_{字段}`
6. **外键命名**：`fk_{表名}_{关联表}`
7. **状态字段**：VARCHAR，具体值在注释中标注

---

## 表关系图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        1. 用户与认证                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  snap_users  ──1:N──>  snap_user_roles  <──N:1──  snap_roles               │
│     │                                                                       │
│     ├───1:1──>  snap_user_settings                                          │
│     ├───1:N──>  snap_admins (管理员)                                        │
│     └───1:N──>  snap_api_keys                                               │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                        2. 单词与内容（核心）                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  snap_words  ──1:1──>  snap_word_contents  <──1:N──  snap_knowledge_points  │
│                                                        │                   │
│                                                        └───N:1──>  snap_cards │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                        3. 卡片组与卡片（任务驱动学习）                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  snap_card_groups  ──1:N──>  snap_cards  ──1:N──>  snap_test_questions     │
│      │                           │                              │          │
│      │                           └───N:1──>  snap_error_book    │          │
│      │                                              │           │          │
│      └───────────────────────────────────────────────┴───N:1──>  snap_test_attempts │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                        4. 词库与每日打卡（习惯驱动）                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  snap_word_banks  ──1:N──>  snap_word_bank_items  <──1:N──  snap_words     │
│      │                                                          │          │
│      └───N:1──>  snap_user_daily_pool  <──N:1──  snap_users     │          │
│           │                                                          │     │
│           └───1:N──>  snap_daily_checkin_log                          │     │
│                                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                        5. AI 聊天会话                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  snap_chat_conversations  ──1:N──>  SPRING_AI_CHAT_MEMORY                  │
│       │                                                                     │
│       └───1:N──>  snap_chat_traces                                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 表详细说明

### 1. 用户与认证

#### snap_users（用户表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 用户ID（UUID） |
| phone | VARCHAR(20) | UNIQUE NOT NULL | 手机号（登录账号） |
| nickname | VARCHAR(100) | - | 昵称 |
| avatar_url | VARCHAR(500) | - | 头像URL |
| wechat_openid | VARCHAR(128) | UNIQUE | 微信OpenID |
| email | VARCHAR(100) | - | 邮箱 |
| password_hash | VARCHAR(255) | - | 密码哈希（BCrypt） |
| is_active | BOOLEAN | DEFAULT TRUE | 是否启用 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

#### snap_roles（角色表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| role_code | VARCHAR(30) | PRIMARY KEY | 角色编码（admin / user） |
| role_name | VARCHAR(50) | NOT NULL | 角色名称 |

**预置数据**：
- `admin` → 管理员
- `user` → 普通用户

#### snap_user_roles（用户角色关联表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 关联ID |
| user_id | VARCHAR(36) | NOT NULL | 用户ID |
| role_code | VARCHAR(30) | NOT NULL | 角色编码 |

**约束**：UNIQUE(user_id, role_code)

#### snap_admins（管理员表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 管理员ID |
| username | VARCHAR(50) | UNIQUE NOT NULL | 用户名 |
| email | VARCHAR(100) | UNIQUE | 邮箱 |
| password_hash | VARCHAR(255) | NOT NULL | 密码哈希（BCrypt） |
| role | VARCHAR(20) | DEFAULT 'admin' | 角色（admin / super_admin） |
| is_active | BOOLEAN | DEFAULT TRUE | 是否启用 |
| last_login_at | TIMESTAMP | - | 最后登录时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

#### snap_user_settings（用户设置表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 设置ID |
| user_id | VARCHAR(36) | UNIQUE NOT NULL | 用户ID |
| daily_new_words | INT | DEFAULT 10 | 每日打卡新词量 |
| daily_review_words | INT | DEFAULT 20 | 每日打卡复习量 |
| checkin_reminder | BOOLEAN | DEFAULT FALSE | 打卡提醒开关 |
| reminder_time | TIME | - | 提醒时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

#### snap_api_keys（API密钥表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 密钥ID |
| user_id | VARCHAR(36) | NOT NULL | 用户ID |
| name | VARCHAR(100) | - | 密钥名称 |
| key_hash | VARCHAR(64) | NOT NULL | 密钥哈希（SHA-256） |
| key_prefix | VARCHAR(20) | NOT NULL | 密钥前缀（展示用） |
| is_active | BOOLEAN | DEFAULT TRUE | 是否启用 |
| last_used_at | TIMESTAMP | - | 最后使用时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

---

### 2. 单词与内容（核心）

#### snap_words（单词表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 单词ID |
| word_text | VARCHAR(200) | UNIQUE NOT NULL | 单词文本（去重） |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_snap_words_text(word_text)`

#### snap_word_contents（单词内容表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 内容ID |
| word_id | VARCHAR(36) | UNIQUE NOT NULL | 单词ID（1:1关联） |
| pronunciation | VARCHAR(200) | - | 发音 |
| pos | VARCHAR(20) | - | 词性（noun/verb/adj/adv/...） |
| general_meaning | TEXT | - | 通用释义 |
| extended_meaning | TEXT | - | 语境含义 |
| example_sentence | TEXT | - | 例句 |
| memory_tip | TEXT | - | 记忆技巧 |
| llm_version | VARCHAR(50) | - | LLM生成版本标识 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

**设计要点**：单词内容独立存储，一个单词只有一份内容。更新内容会同步影响所有引用该单词的卡片组和打卡池。

#### snap_knowledge_points（知识点表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 知识点ID |
| word_id | VARCHAR(36) | NOT NULL | 单词ID |
| card_id | VARCHAR(36) | - | 所属卡片（可为空） |
| point_type | VARCHAR(30) | NOT NULL | 类型（pronunciation/meaning/extended/example/tip/pos） |
| content | TEXT | NOT NULL | 知识点内容 |
| sort_order | INT | DEFAULT 0 | 展示顺序 |
| status | VARCHAR(20) | DEFAULT 'unshown' | 状态（unshown/show/confirmed） |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_snap_kp_word(word_id)`、`idx_snap_kp_card(card_id)`

**知识点类型说明**：
| point_type | 含义 |
|------------|------|
| pronunciation | 发音 |
| meaning | 通用释义 |
| extended | 语境含义 |
| example | 例句 |
| tip | 记忆技巧 |
| pos | 词性 |

---

### 3. 卡片组与卡片（任务驱动学习）

#### snap_card_groups（卡片组表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 卡片组ID |
| user_id | VARCHAR(36) | NOT NULL | 用户ID |
| title | VARCHAR(200) | - | 卡片组标题 |
| source_image | VARCHAR(500) | - | 来源图片URL |
| source_text | TEXT | - | 来源文本 |
| group_status | VARCHAR(20) | DEFAULT 'pending' | 状态 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

**索引**：`idx_snap_cg_user(user_id, created_at DESC)`、`idx_snap_cg_status(user_id, group_status)`

**状态流转**：
```
pending → learning → learn_done → testing → test_done
```

| group_status | 含义 |
|--------------|------|
| pending | 待学习 |
| learning | 学习中 |
| learn_done | 已学完 |
| testing | 测验中 |
| test_done | 已通关 |

#### snap_cards（卡片表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 卡片ID |
| group_id | VARCHAR(36) | NOT NULL | 卡片组ID |
| word_id | VARCHAR(36) | NOT NULL | 单词ID |
| user_id | VARCHAR(36) | NOT NULL | 用户ID |
| sort_order | INT | DEFAULT 0 | 组内排序 |
| card_status | VARCHAR(20) | DEFAULT 'unlearned' | 状态 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**约束**：UNIQUE(group_id, word_id)

**索引**：`idx_snap_cards_group(group_id)`、`idx_snap_cards_user(user_id)`

**卡片状态**：
| card_status | 含义 |
|-------------|------|
| unlearned | 未学习 |
| learning | 学习中 |
| relearn | 重学 |
| mastered | 已掌握 |
| error | 错误 |

---

### 4. 测试与错题

#### snap_test_questions（测试题目表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 题目ID |
| group_id | VARCHAR(36) | NOT NULL | 卡片组ID |
| card_id | VARCHAR(36) | NOT NULL | 卡片ID |
| question_type | VARCHAR(30) | NOT NULL | 题型 |
| question_text | TEXT | NOT NULL | 题目文本 |
| options | JSONB | NOT NULL | 选项数组 |
| correct_answer | VARCHAR(500) | NOT NULL | 正确答案 |
| sort_order | INT | DEFAULT 0 | 排序 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_snap_tq_group(group_id)`、`idx_snap_tq_card(card_id)`

**题型说明**：
| question_type | 含义 |
|---------------|------|
| meaning_select | 词义选择 |
| word_select | 单词选择 |
| collocation | 搭配填空 |
| spelling | 拼写测试 |

#### snap_test_attempts（测试答题记录表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 答题记录ID |
| question_id | VARCHAR(36) | NOT NULL | 题目ID |
| user_id | VARCHAR(36) | NOT NULL | 用户ID |
| user_answer | VARCHAR(500) | - | 用户答案 |
| is_correct | BOOLEAN | - | 是否答对 |
| attempt_round | INT | DEFAULT 1 | 测试轮次 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_snap_ta_user(user_id)`、`idx_snap_ta_question(question_id)`

#### snap_error_book（错题本表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 错题ID |
| group_id | VARCHAR(36) | NOT NULL | 卡片组ID |
| card_id | VARCHAR(36) | NOT NULL | 卡片ID |
| user_id | VARCHAR(36) | NOT NULL | 用户ID |
| test_attempt_id | VARCHAR(36) | NOT NULL | 答题记录ID |
| resolved | BOOLEAN | DEFAULT FALSE | 是否已解决 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**约束**：UNIQUE(card_id, test_attempt_id)

**索引**：`idx_snap_eb_group(group_id)`、`idx_snap_eb_user(user_id)`

---

### 5. 词库与每日打卡

#### snap_word_banks（词库表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 词库ID |
| name | VARCHAR(200) | NOT NULL | 词库名称 |
| type | VARCHAR(20) | NOT NULL | 类型 |
| description | VARCHAR(500) | - | 描述 |
| created_by | VARCHAR(36) | - | 创建者ID |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_snap_wb_type(type)`、`idx_snap_wb_creator(created_by)`

**词库类型**：
| type | 含义 |
|------|------|
| preset | 预置词库（管理员创建） |
| user | 用户自建词库 |

#### snap_word_bank_items（词库单词关联表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 关联ID |
| bank_id | VARCHAR(36) | NOT NULL | 词库ID |
| word_id | VARCHAR(36) | NOT NULL | 单词ID |
| added_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 添加时间 |

**约束**：UNIQUE(bank_id, word_id)

#### snap_user_daily_pool（用户每日学习池）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 学习池ID |
| user_id | VARCHAR(36) | NOT NULL | 用户ID |
| bank_id | VARCHAR(36) | NOT NULL | 词库ID |
| word_id | VARCHAR(36) | NOT NULL | 单词ID |
| pool_status | VARCHAR(20) | DEFAULT 'new' | 状态 |
| interval_days | INT | DEFAULT 0 | 当前复习间隔（天） |
| review_count | INT | DEFAULT 0 | 累计复习次数 |
| last_mark | VARCHAR(20) | - | 上次标记 |
| next_review_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 下次复习时间 |
| last_review_at | TIMESTAMP | - | 最后复习时间 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**约束**：UNIQUE(user_id, bank_id, word_id)

**索引**：`idx_snap_pool_user(user_id)`、`idx_snap_pool_review(user_id, next_review_at)`

**池状态**：
| pool_status | 含义 |
|-------------|------|
| new | 新词 |
| learning | 学习中 |
| review | 复习中 |
| mastered | 已掌握 |

**标记类型**（百词斩风格）：
| last_mark | 含义 | 效果 |
|-----------|------|------|
| known | 认识 | interval × 2 |
| fuzzy | 模糊 | interval 不变 |
| unknown | 不认识 | interval = 1 |

#### snap_daily_checkin_log（每日打卡日志）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 日志ID |
| user_id | VARCHAR(36) | NOT NULL | 用户ID |
| bank_id | VARCHAR(36) | NOT NULL | 词库ID |
| checkin_date | DATE | NOT NULL | 打卡日期 |
| new_words_count | INT | DEFAULT 0 | 新词数量 |
| review_words_count | INT | DEFAULT 0 | 复习数量 |
| known_count | INT | DEFAULT 0 | 认识数量 |
| fuzzy_count | INT | DEFAULT 0 | 模糊数量 |
| unknown_count | INT | DEFAULT 0 | 不认识数量 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**约束**：UNIQUE(user_id, checkin_date, bank_id)

**索引**：`idx_snap_checkin_user_date(user_id, checkin_date DESC)`、`idx_snap_checkin_date(checkin_date)`

---

### 6. AI 聊天会话

#### snap_chat_conversations（会话表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 会话ID |
| user_id | VARCHAR(36) | NOT NULL | 用户ID |
| chat_id | VARCHAR(100) | NOT NULL UNIQUE | Spring AI会话ID |
| title | VARCHAR(200) | - | 会话标题 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_snap_cc_user(user_id, created_at DESC)`

#### snap_chat_traces（对话追踪日志）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 追踪ID |
| user_id | VARCHAR(36) | NOT NULL | 用户ID |
| chat_id | VARCHAR(100) | NOT NULL | 会话ID |
| model | VARCHAR(50) | - | 使用模型 |
| user_message | TEXT | - | 用户消息 |
| response_text | TEXT | - | 响应文本 |
| prompt_tokens | INT | - | 提示词token数 |
| completion_tokens | INT | - | 回复token数 |
| total_tokens | INT | - | 总token数 |
| duration_ms | BIGINT | - | 耗时（毫秒） |
| status | VARCHAR(20) | NOT NULL | 状态 |
| error_message | TEXT | - | 错误信息 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_snap_ct_user(user_id, created_at DESC)`、`idx_snap_ct_chat(chat_id, created_at DESC)`、`idx_snap_ct_time(created_at DESC)`

#### SPRING_AI_CHAT_MEMORY（Spring AI 会话记忆）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| conversation_id | VARCHAR(36) | NOT NULL | 会话ID |
| content | TEXT | NOT NULL | 消息内容 |
| type | VARCHAR(10) | NOT NULL | 消息类型 |
| timestamp | TIMESTAMP | NOT NULL | 时间戳 |

**索引**：`SPRING_AI_CHAT_MEMORY_CONVERSATION_ID_TIMESTAMP_IDX(conversation_id, timestamp)`

**消息类型**：USER / ASSISTANT / SYSTEM / TOOL

---

### 7. 其他表

#### snap_api_access_logs（API访问日志）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 日志ID |
| user_id | VARCHAR(36) | - | 用户ID |
| method | VARCHAR(10) | NOT NULL | HTTP方法 |
| uri | VARCHAR(500) | NOT NULL | 请求URI |
| ip | VARCHAR(50) | - | 客户端IP |
| request_body | TEXT | - | 请求体 |
| response_body | TEXT | - | 响应体 |
| duration_ms | BIGINT | - | 耗时（毫秒） |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：`idx_saal_time(created_at DESC)`

#### snap_knowledge_files（知识库文件）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 文件ID |
| user_id | VARCHAR(36) | NOT NULL | 用户ID |
| file_name | VARCHAR(500) | NOT NULL | 文件名 |
| file_path | VARCHAR(500) | NOT NULL | 文件路径 |
| file_size | BIGINT | - | 文件大小 |
| chunk_count | INT | DEFAULT 0 | 分块数量 |
| upload_time | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 上传时间 |

#### snap_voices（音色表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 音色ID |
| name | VARCHAR(100) | - | 音色名称 |
| provider | VARCHAR(50) | - | 引擎（dashscope/volcengine） |
| voice_code | VARCHAR(100) | - | 音色标识 |
| tts_model | VARCHAR(100) | - | TTS模型 |
| format | VARCHAR(20) | - | 音频格式 |
| sample_rate | INT | - | 采样率 |
| volume | INT | - | 音量 |
| speech_rate | DOUBLE | - | 语速 |
| pitch | DOUBLE | - | 音调 |
| instruction | TEXT | - | 指令文本 |
| description | TEXT | - | 描述 |
| is_default | BOOLEAN | - | 是否默认 |
| is_active | BOOLEAN | - | 是否启用 |
| created_at | TIMESTAMP | - | 创建时间 |

#### snap_card_audios（卡片音频表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | VARCHAR(36) | PRIMARY KEY | 音频ID |
| card_id | VARCHAR(36) | NOT NULL | 卡片ID |
| audio_url | VARCHAR(500) | NOT NULL | 音频URL |
| voice_id | VARCHAR(36) | - | 音色ID |
| text | TEXT | - | 合成文本 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

#### vector_store（向量存储表）

| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | UUID | PRIMARY KEY | 记录ID |
| content | TEXT | - | 文档内容 |
| metadata | JSON | - | 元数据 |
| embedding | vector(1536) | - | 向量嵌入 |

**索引**：`vector_store_embedding_idx(embedding vector_cosine_ops)`

---

## 数据流转概览

### 卡片组学习流程

```
拍照/OCR/手动输入 → snap_words (findOrCreate)
                           ↓
                  snap_word_contents (LLM生成)
                           ↓
                  snap_knowledge_points (拆分6个KP)
                           ↓
           snap_card_groups → snap_cards (关联)
                           ↓
              学习：KnowledgePoint.status 流转
                           ↓
              测试：snap_test_questions → snap_test_attempts
                           ↓
              错题：snap_error_book
```

### 每日打卡流程

```
词库选择 → snap_user_daily_pool (每日推送)
                   ↓
         百词斩标记：known/fuzzy/unknown
                   ↓
         更新 interval_days + next_review_at
                   ↓
         snap_daily_checkin_log (记录打卡)
```

---

## 统计信息

| 类别 | 表数量 | 说明 |
|------|--------|------|
| 用户与认证 | 6 | users, roles, user_roles, admins, user_settings, api_keys |
| 单词与内容 | 3 | words, word_contents, knowledge_points |
| 卡片组与卡片 | 2 | card_groups, cards |
| 测试与错题 | 3 | test_questions, test_attempts, error_book |
| 词库与打卡 | 4 | word_banks, word_bank_items, user_daily_pool, daily_checkin_log |
| AI聊天 | 3 | chat_conversations, chat_traces, SPRING_AI_CHAT_MEMORY |
| 其他 | 4 | api_access_logs, knowledge_files, voices, card_audios |
| **总计** | **25** | - |
