# 资源复用方案：知识点、测试题、语音

> 状态：已实施 | 日期：2026-07-02

## 1. 问题背景

当前系统每次创建卡片组时，即使单词相同也会重复生成资源：

| 资源 | 当前行为 | 浪费 |
|------|---------|------|
| 知识点 (KP) | 每张卡片生成 6 条，两张卡片同一单词 = 12 条 | LLM 调用 + 存储 |
| 测试题 | 每组生成一套，同一单词在另一组重新生成 | LLM 调用 + 存储 |
| 语音 | 每张卡片存一份，同一单词两份 = 两个文件 | TTS 合成 + 磁盘 |

**根本原因**：这些资源都绑定了 `card_id` 或 `group_id`，无法跨卡片组复用。

## 2. 目标架构

核心思路：**内容和使用场景分离**——资源只跟"单词"绑定，不跟"卡片"绑定。

```
单词 (snap_words)
  ├─ snap_word_contents（LLM 生成的内容，6 个字段）← 唯一数据源
  ├─ snap_knowledge_points（纯索引：word_id + point_type）← 不存内容
  ├─ snap_test_questions（题库：word_id + 题型）← 不绑 group
  └─ snap_word_audios（语音：word_id + voice_id + audio_type）← 不绑 card

卡片 (snap_cards) ← 薄关联层，只做引用
  ├─ snap_card_kp_progress（学习进度）← 新：每张卡片每个 KP 的状态
  └─ snap_test_session_questions（测次关联）← 新：每次测试用了哪些题
```

## 3. 具体改动

---

### 3.1 snap_knowledge_points — 退化为纯索引

**现有结构**：`(id, word_id, card_id, point_type, content, status, sort_order, created_at)`

- 绑定 `card_id`，每张卡片独立生成
- `content` 存储知识点文本，完全来自 `snap_word_contents`，冗余
- `status` 字段（`unshown` / `shown` / `confirmed`）混在内容表中

**改为**：`(id, word_id, point_type, sort_order, created_at)`

- 去掉 `card_id`、`content`、`status`
- 不存 `user_id`——KP 只是 word_contents 的结构化索引，无用户差异
- UNIQUE: `(word_id, point_type)`
- 一个单词只有一份 KP 索引（最多 6 行），所有卡片组共享

**为什么必须新建 progress 表**

去掉 `card_id` 后一张 KP 行被多张卡片共享，`status` 无法留在 KP 表——status 是"某张卡片对某个 KP 的学习进度"，不是 KP 本身的属性：

```
KP 行 (word_id=apple, point_type=general_meaning)
  ├─ 卡片 A → status: confirmed  ← A 学过了
  └─ 卡片 B → status: unshown    ← B 还没学
      ↑ 两个值无法存进同一个字段
```

所以 status 必须移到新表，用 `(card_id, kp_id)` 标识唯一进度。
- 读取时 JOIN `snap_word_contents` 按 `point_type` 取对应字段：

| point_type | 取自 word_contents 哪个字段 |
|------------|---------------------------|
| `pronunciation` | `pronunciation` |
| `pos` | `pos` |
| `general_meaning` | `general_meaning` |
| `extended_meaning` | `extended_meaning` |
| `example_sentence` | `example_sentence` |
| `memory_tip` | `memory_tip` |

**新增 snap_card_kp_progress（学习进度）**：

```sql
CREATE TABLE snap_card_kp_progress (
    id       VARCHAR(36) PRIMARY KEY,
    card_id  VARCHAR(36) NOT NULL REFERENCES snap_cards(id) ON DELETE CASCADE,
    kp_id    VARCHAR(36) NOT NULL REFERENCES snap_knowledge_points(id) ON DELETE CASCADE,
    status   VARCHAR(20) DEFAULT 'unshown',  -- unshown / shown / confirmed
    UNIQUE (card_id, kp_id)
);
```

---

### 3.2 snap_card_audios → snap_word_audios

**现有结构**：`(id, card_id, voice_id, audio_type, audio_url, duration_ms, file_size, created_at)`

- 绑定 `card_id`，同一单词在 3 个卡片组 = 3 份音频
- UNIQUE: `(card_id, voice_id, audio_type)`

**改为 snap_word_audios**：`(id, word_id, voice_id, audio_type, audio_url, duration_ms, file_size, created_at)`

- 去掉 `card_id`，改为 `word_id`
- 不存 `user_id`——用户选什么音色由 `snap_user_settings.voice_id` 决定，走到音频表时已落到具体 `voice_id`
- UNIQUE: `(word_id, voice_id, audio_type)`
- 同一单词 + 同一音色 + 同一类型 = 只合成一次

| | 改前 `snap_card_audios` | 改后 `snap_word_audios` |
|---|---|---|
| 关联键 | `card_id` | `word_id` |
| 唯一约束 | `(card_id, voice_id, audio_type)` | `(word_id, voice_id, audio_type)` |
| 同一单词 3 组 | 3 行 + 3 个文件 | 1 行 + 1 个文件 |
| 同一单词不同音色 | 各 1 行 | 各 1 行 |

---

### 3.3 snap_test_questions — 拆分为两张表

**现有结构**：一张表 `(id, group_id, card_id, question_type, question_text, options, correct_answer, sort_order, created_at)`

- 绑定 `group_id` + `card_id`，每组独立生成
- 重新测试时删除旧题重新生成

**改为两张表**：

#### 表 1：`snap_test_questions` — 题库（存内容，可复用）

```sql
CREATE TABLE snap_test_questions (
    id              VARCHAR(36) PRIMARY KEY,
    word_id         VARCHAR(36) NOT NULL REFERENCES snap_words(id) ON DELETE CASCADE,
    question_type   VARCHAR(30) NOT NULL,   -- meaning_select / word_select / collocation / spelling
    question_text   TEXT NOT NULL,
    options         JSONB NOT NULL,         -- ["选项A","选项B","选项C","选项D"]
    correct_answer  VARCHAR(500) NOT NULL,
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (word_id, question_type)
);
```

- 绑定 `word_id`，不绑 `group_id` / `card_id`
- 一个单词 4 种题型 = 最多 4 行
- 跨组复用，不重复生成

#### 表 2：`snap_test_session_questions` — 测次关联（存使用关系）

```sql
CREATE TABLE snap_test_session_questions (
    id           VARCHAR(36) PRIMARY KEY,
    group_id     VARCHAR(36) NOT NULL REFERENCES snap_card_groups(id) ON DELETE CASCADE,
    card_id      VARCHAR(36) NOT NULL REFERENCES snap_cards(id) ON DELETE CASCADE,
    question_id  VARCHAR(36) NOT NULL REFERENCES snap_test_questions(id) ON DELETE CASCADE,
    sort_order   INT DEFAULT 0,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

- `card_id` 保留——测试是针对具体卡片的，后续查错题本、统计掌握度都需要知道是哪张卡片
- 同一道题（question_id）可以被多个 group 引用

#### 查题流程

```
开始测试（groupId）
  → 查 snap_cards WHERE group_id = ?
  → 拿到 word_id 列表
  → 查 snap_test_questions WHERE word_id IN (...)
  → 已有题目直接复用，缺失的 LLM 生成后补入
  → 写入 snap_test_session_questions（记录本次测次使用关系）
  → 用户作答 → 写入 snap_test_attempts（关联 question_id，不改）
```

---

### 3.4 不变的表

| 表 | 原因 |
|----|------|
| `snap_words` | 已是全局共享 |
| `snap_word_contents` | 已是 1:1 绑定 word，共享 |
| `snap_cards` | 薄关联层（card → word + group），设计正确 |
| `snap_card_groups` | 组级别元数据，无需改动 |
| `snap_test_attempts` | 每次作答天然唯一，关联 question_id 不变 |
| `snap_error_book` | 每次错误天然唯一，无需改动 |

## 4. 数据流变化

### 4.1 创建卡片组

**现在**：每个单词 → 创建 Card → 创建 6 条 KP（含 content）→ 异步生成语音
**改后**：每个单词 → 查已有 KP（无则按 word_contents 自动生成索引行）→ 创建 Card → 插入 card_kp_progress → 查已有语音（无则合成）

### 4.2 生成测试题

**现在**：删除组内旧题 → LLM 生成新题 → 写入
**改后**：按组内单词查题库 → 缺失的 LLM 生成补入题库 → 写入 session_questions 关联

### 4.3 生成语音

**现在**：按 `(card_id, voice_id, audio_type)` 查重
**改后**：按 `(word_id, voice_id, audio_type)` 查重

### 4.4 重新学习（relearn）

**现在**：`resetForCard(cardId)` → 删除旧 KP → 重新按 word_contents 生成
**改后**：`resetForCard(cardId)` → 仅重置 `snap_card_kp_progress` 中对应 card 的状态为 `unshown`

## 5. 收益预估

| 场景 | 当前 | 改后 |
|------|------|------|
| 同一单词出现在 3 个卡片组 | 3x KP + 3x 题目 + 3x 语音 | 1x KP 索引 + 1x 题目 + 1x 语音 |
| 同一单词重新测试 | 删除旧题，完整重新生成 | 复用题库已有题目 |
| 同一单词标记"需再学" | 删除旧 KP，重新生成 | 仅重置 progress 状态 |
| LLM 刷新单词内容 | 6 张卡片 KP 全部过期不一致 | word_contents 一处更新，全局生效 |

## 6. 实施清单

### 数据库
- [ ] `snap_knowledge_points`：去 `card_id`、`content`、`status`，加 UNIQUE(word_id, point_type)
- [ ] `snap_card_audios` → `snap_word_audios`：`card_id` → `word_id`，UNIQUE(word_id, voice_id, audio_type)
- [ ] `snap_test_questions`：去 `group_id`、`card_id`，加 `word_id`，UNIQUE(word_id, question_type)
- [ ] 新建 `snap_test_session_questions`：(group_id, card_id, question_id)
- [ ] 新建 `snap_card_kp_progress`：(card_id, kp_id, status)

### Service 层
- [ ] `KnowledgePointService`：创建逻辑改为按 word_id 生成索引行（不存 content）；读取时 JOIN word_contents
- [ ] `CardGroupService.create()`：卡片创建时关联已有 KP，写 card_kp_progress
- [ ] `TestService`：按 word_id 查题库，缺失的生成；写 session_questions 关联
- [ ] `CardAudioService`：查询改为 `(word_id, voice_id, audio_type)`
- [ ] 相关 reset/relearn 逻辑：重置 card_kp_progress 状态，不再删除 KP

### 前端
- [ ] 确认 API 返回结构不变（外层 DTO 不变，底层 JOIN 方式变）
- [ ] KnowledgeStepper：KP 的 content 通过 word_contents JOIN 获取（在此之前 API 已处理好）

---

## 7. 随机测试

### 7.1 需求概述

新增一个"随机测试"入口。用户点击后，系统从题库中随机抽取单词对应的测试题，重新进行测试。

### 7.2 新增表：snap_random_test_pool（随机测试题库）

```sql
CREATE TABLE snap_random_test_pool (
    id           VARCHAR(36) PRIMARY KEY,
    word_id      VARCHAR(36) NOT NULL REFERENCES snap_words(id) ON DELETE CASCADE,
    user_id      VARCHAR(36) NOT NULL REFERENCES snap_users(id) ON DELETE CASCADE,
    review_count INT DEFAULT 2,          -- 还需复习的次数
    source       VARCHAR(20) DEFAULT 'auto',  -- auto(首次生成) / error(答错加入)
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (word_id, user_id)
);
```

- `review_count`：核心计数字段，决定该单词是否出现在随机测试中
- `source`：入库来源，`auto` = 首次生成时自动入库，`error` = 答错重新入库

### 7.3 计数字段生命周期

```
                    测试题首次生成
                         │
                         ▼
              ┌── review_count = 2（自动入库）──┐
              │                                  │
              ▼                                  │
    ┌─── 任何测试中 ───┐                         │
    │                  │                         │
    ▼                  ▼                         │
  答对               答错                         │
    │                  │                         │
    ▼                  ▼                         │
 count -= 1       count = 4                      │
（仅随机测试        （所有测试                     │
 中减）            答错都触发）                     │
    │                  │                         │
    ▼                  │                         │
 count > 0？           │                         │
    │                  │                         │
    ├─ 是 → 留在池中 ───┘                         │
    │                                            │
    └─ 否（= 0）→ 出库，不再抽到                    │
         │                                       │
         │  后续任何测试中又答错                       │
         │                                       │
         └──→ count = 4，source = 'error'（重新入库）──┘
```

### 7.4 规则总结

| 规则 | 说明 |
|------|------|
| 入库 | 测试题首次生成时，自动入池 `review_count = 2` |
| 答错 | **所有测试**（卡片组测试 + 随机测试）中答错 → `review_count = 4` |
| 答对 | 仅在**随机测试**中答对 → `review_count -= 1`（卡片组测试答对不减） |
| 出库 | `review_count` 减到 0 → 不再出现在随机测试中 |
| 重新入库 | 已出库的单词在任何测试中再次答错 → `review_count = 4`，`source = 'error'` |

**为什么卡片组测试答对不减**：卡片组测试是任务驱动的，答对只说明当时记住了。随机测试是抽查性质的，随机测试答对才是真正的巩固信号。

### 7.5 随机测试流程

```
用户点击"随机测试"
  → 查 snap_random_test_pool WHERE user_id = ? AND review_count > 0
  → 拿到 word_id 列表
  → 随机抽取 N 个单词
  → 从 snap_test_questions 取对应题目
  → 组装测试
  → 用户提交答案：
      答对 → review_count -= 1（减到 0 则出库）
      答错 → review_count = 4

任何测试提交答案时：
  → 查 snap_test_attempts 中 is_correct = false 的记录
  → 对每个答错的单词：
      INSERT INTO snap_random_test_pool (word_id, user_id, review_count = 4, source = 'error')
      ON CONFLICT (word_id, user_id) DO UPDATE SET review_count = 4
```

### 7.6 实施清单（随机测试部分）

#### 数据库
- [ ] 新建 `snap_random_test_pool`：(word_id, user_id, review_count, source)

#### Service 层
- [ ] 新建 `RandomTestService`：抽题逻辑（随机选词 → 取题 → 组装测试）
- [ ] `TestService.submitAnswers()`：提交答案后同步更新 `review_count`
  - 所有答错 → `review_count = 4`
  - 随机测试中答对 → `review_count -= 1`
- [ ] 测试题首次生成时自动插入 `snap_random_test_pool`

#### 前端
- [ ] 首页/测试页新增"随机测试"入口按钮
- [ ] 随机测试页面（可复用现有测试页面，只是选题逻辑不同）
- [ ] 随机测试结果页
