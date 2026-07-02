# 文档管理方案：MD 编辑、导入及知识库对接

> 状态：方案讨论中，待实施 | 日期：2026-07-02

## 1. 需求背景

在管理端统一管理所有 MD 文档——无论是系统新建的、还是通过 PDF/Word 上传后解析的——全部集中在 `snap_documents` 中。发布后自动向量化进入知识库。文档管理页面提供独立的 AI 对话入口，不仅能 RAG 检索，还能通过 Tool 能力直接修改 MD 文档内容。

### 文档来源

```
系统新建/编辑（在线 MD 编辑器）
     │
     ▼
 snap_documents（统一文档库）← PDF/Word 上传 → 解析为 MD → 人工校对
     │
     │ 发布时向量化
     ▼
vector_store（现有，不改动）→ 现有 AI 对话 RAG（不改动）
```

### 边界说明

- **现有代码不动**：原有的 AI 聊天、文件上传、RAG 链路保持原样
- **新建独立 AI 入口**：文档管理页面独立的 AI 对话，后端新建类实现（不复用现有 ChatController）
- **发布即入库**：所有文档发布后进同一个 vector_store
- **AI 具备 Tool 能力**：文档 AI 可以通过 Function Calling 读写 MD 文档

### 分阶段规划

```
阶段 1：MD 文档管理
  → 在线编辑、批量导入、分类标签、发布到向量库

阶段 2：Word/PDF 支持
  → 上传后解析为 MD → 存入 snap_documents → 人工校对后发布
```

## 2. 新增表

### 2.1 snap_documents（文档主表）

```sql
CREATE TABLE snap_documents (
    id            VARCHAR(36) PRIMARY KEY,
    user_id       VARCHAR(36) NOT NULL REFERENCES snap_users(id) ON DELETE CASCADE,
    title         VARCHAR(300) NOT NULL,
    content       TEXT NOT NULL,              -- Markdown 原文
    category      VARCHAR(50),               -- 分类
    tags          VARCHAR(500),              -- 逗号分隔标签
    status        VARCHAR(20) DEFAULT 'draft', -- draft / published / archived
    source_type   VARCHAR(20) DEFAULT 'md',  -- md / word / pdf
    source_name   VARCHAR(300),              -- 原始文件名（导入时记录）
    file_size     BIGINT,
    sort_order    INT DEFAULT 0,
    knowledge_file_id VARCHAR(36),           -- 发布后关联 snap_knowledge_files.id
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_snap_docs_user ON snap_documents(user_id);
CREATE INDEX idx_snap_docs_status ON snap_documents(user_id, status);
CREATE INDEX idx_snap_docs_category ON snap_documents(user_id, category);
```

### 2.2 不变的表

| 表 | 说明 |
|----|------|
| `snap_knowledge_files` | 复用，记录文件来源和切片数 |
| `vector_store` | 现有 pgvector 表，不改动 |
| 现有 RAG 链路 | 不变 |

## 3. 管理端功能

### 3.1 文档列表页

| 功能 | 说明 |
|------|------|
| 表格展示 | 标题、分类、状态（草稿/已发布/已归档）、更新时间 |
| 搜索 | 按标题关键字搜索 |
| 筛选 | 按分类、状态筛选 |
| 操作 | 编辑、发布/撤销、归档、删除 |

### 3.2 文档编辑器

- 接入 Markdown 编辑器，左侧编辑、右侧实时预览
- 支持插入图片（复用现有文件上传接口）
- 字段：标题、分类、标签（自由输入逗号分隔）、内容
- 状态：草稿 / 发布 / 归档

### 3.3 批量导入

- 选择本地多个 .md 文件上传
- 文件名作为 title，文件内容作为 content
- 统一标记为"草稿"状态

### 3.4 发布到向量库

发布时将 MD 内容向量化到 `vector_store`，复用现有的解析 + 向量化能力（从 `AdminKnowledgeController` 中抽取为可复用的工具方法）。

```
首次发布：
  1. 将 snap_documents.content 写入临时 .md 文件
  2. 调用解析方法 → 切片 → 向量化 → 写入 vector_store
  3. 写入 snap_knowledge_files（记录 file_id）
  4. 更新 snap_documents.status = 'published' + knowledge_file_id

重新发布（内容已修改）：
  1. 按 knowledge_file_id 删除旧切片（vectorStore.delete）
  2. 更新 snap_knowledge_files 的 chunk_count
  3. 走首次发布流程 1-2（重新切片 + 向量化）
```

### 3.5 撤销发布

```
  1. 按 knowledge_file_id 删除 vector_store 中的切片
  2. 删除 snap_knowledge_files 中对应行
  3. 更新 snap_documents.status = 'draft'，清空 knowledge_file_id
  4. 注意：文档内容保留在 snap_documents，后续可重新编辑发布
```

### 3.6 删除文档

```
  1. 如果已发布，先执行撤销发布流程
  2. 删除 snap_documents 中对应行
```

## 4. 数据流

### 4.1 在线编辑发布

```
管理端编辑器 → 保存 MD 原文到 snap_documents（status = draft）
  → 点击"发布"
    → 生成临时 .md 文件
    → 复用现有 parseFile() 解析
    → 复用现有 doVectorize() 切片 + 向量化
    → 写入 snap_knowledge_files + vector_store
    → 更新 snap_documents.status = 'published'
```

### 4.2 批量导入

```
选择本地 .md 文件 → 上传 → 文件名→title，内容→content
  → 写入 snap_documents（status = draft）
  → 后续逐个/批量发布（走 4.1 流程）
```

### 4.3 阶段 2：Word/PDF 导入

```
上传 Word/PDF → TikaDocumentParser 解析 → 转为 MD
  → 存入 snap_documents（status = draft, source_type = word/pdf）
  → 人工校对编辑 → 发布（走 4.1 流程）
```

## 5. 文档 AI（独立对话入口 + Tool 能力）

### 5.1 定位

- **独立 AI 入口**：放在文档管理页面内，与现有 AI 聊天页面隔离
- **独立后端类**：新建 `DocumentAiService`，不复用现有 `LLMService` / `ChatController`
- **共用向量库**：RAG 检索走同一个 `vector_store`，但通过 Tool 实现文档的读写操作

### 5.2 Tool 能力设计

AI 通过 Spring AI 的 Function Calling 机制，可以调用以下工具直接操作 `snap_documents`：

| Tool | 功能 | 参数 | 备注 |
|------|------|------|------|
| `searchDocuments` | 语义搜索文档 | `query: String` | 走 vector_store RAG 检索，结果包含文档标题和匹配片段 |
| `getDocument` | 获取文档全文 | `documentId: String` | 直接从 snap_documents 读取 content |
| `createDocument` | 新建 MD 文档 | `title: String`, `content: String`, `category: String`(可选) | 状态为 draft，需手动发布 |
| `updateDocument` | 全文替换 | `documentId: String`, `content: String` | 仅操作 draft 状态文档；如已发布则提示先撤销 |
| `appendDocument` | 追加到末尾 | `documentId: String`, `appendContent: String` | 仅操作 draft 状态文档 |
| `listDocuments` | 列出文档列表 | `category: String`(可选), `status: String`(可选) | 按条件筛选，返回标题列表 |

**Tool 权限约束**：
- `updateDocument` 和 `appendDocument` 只能操作 draft 状态的文档
- 已发布的文档需要先撤销发布才能修改，防止向量库内容与实际文档不一致
- `createDocument` 创建的文档为 draft 状态，AI 可以提醒用户后续手动发布

### 5.3 交互示例

```
用户：帮我写一篇关于 Git 常用命令的文档
  → AI 调用 createDocument(title="Git 常用命令", content="...")
  → 返回"已创建，点击查看"

用户：搜索一下关于数据库设计的文档
  → AI 调用 searchDocuments(query="数据库设计")
  → 返回检索到的文档列表

用户：把刚才那篇文档的第三段改一下，内容改成 xxx
  → AI 调用 getDocument(id) → 找到第三段
  → AI 调用 updateDocument(id, modifiedContent)
  → 返回"已修改"
```

### 5.4 技术实现

```java
// 新建独立类，不复用现有 LLMService 或 ChatController
@Service
public class DocumentAiService {

    private final ChatClient chatClient;       // Spring AI ChatClient
    private final VectorStore vectorStore;     // 复用现有 vector_store
    private final DocumentMapper docMapper;    // snap_documents 操作

    // Tool 注册：通过 @Tool 注解或 ChatClient Builder 注册 6 个 Function
    // RAG 检索：构建 RetrievalAugmentationAdvisor，检索 vector_store
    // 对话记忆：使用 MessageChatMemoryAdvisor（与现有 ChatController 类似）

    public Flux<String> chat(String chatId, String message, String userId) {
        // 1. 构建 ChatClient（注册 6 个 Tool + RAG Advisor + Memory Advisor）
        // 2. 流式返回 SSE
    }
}
```

**关键点**：
- Tool 方法用 `@Tool(description = "...")` 注解，Spring AI 自动注册
- RAG Advisor 复用 `vector_store`，但需在 metadata 或逻辑上限定用户可见范围
- 对话记忆用 `MessageChatMemoryAdvisor` + `JdbcChatMemoryRepository`，支持多轮对话
- 初期使用现有 DeepSeek/qwen-plus 模型即可

### 5.5 入口交互

- 文档管理页面右上角"AI 助手"按钮
- 点击弹出侧边栏或 Dialog，内嵌独立 AI 对话界面
- 对话界面：消息列表 + 输入框 + 流式输出
- AI 调用 Tool 后展示操作结果（如"已创建文档"、"已修改"）

## 6. API 设计

### 6.1 现有代码复用

发布到向量库需要复用 `AdminKnowledgeController` 中的解析和向量化能力，建议抽取为工具类：

```java
// 从 AdminKnowledgeController 抽取为独立 @Service，供 DocumentService 调用
public class KnowledgeVectorService {
    // parseFile() — 解析 MD 文件为 Spring AI Document 列表
    public List<Document> parseFile(File file);

    // doVectorize() — 切片 + 向量化 + 批量写入 vector_store
    public void vectorize(List<Document> docs, String userId, String fileId);

    // 删除指定 file_id 的所有向量切片
    public void deleteByFileId(String fileId);
}
```

### 6.2 文档 CRUD

| 端点 | 方法 | 说明 |
|------|------|------|
| `/admin/documents` | GET | 文档列表（支持搜索、分类筛选） |
| `/admin/documents/{id}` | GET | 文档详情（含 content） |
| `/admin/documents` | POST | 创建文档 |
| `/admin/documents/{id}` | PUT | 更新文档 |
| `/admin/documents/{id}` | DELETE | 删除文档（已发布则先撤销） |
| `/admin/documents/{id}/publish` | POST | 发布到向量库 |
| `/admin/documents/{id}/unpublish` | POST | 撤销发布 |
| `/admin/documents/import` | POST | 批量导入 MD 文件 |
| `/admin/documents/batch-publish` | POST | 批量发布 |

### 6.3 文档 AI

| 端点 | 方法 | 说明 |
|------|------|------|
| `/admin/documents/ai/chat` | POST | 文档 AI 对话（SSE 流式），带 Tool 调用 |

## 7. 实施清单

### 数据库
- [ ] 新建 `snap_documents` 表

### 后端（文档 CRUD）
- [ ] 新建 `Document` Entity + Mapper
- [ ] 新建 `AdminDocumentController`（CRUD + 发布/撤销 + 批量导入）
- [ ] 新建 `DocumentService`（核心逻辑）
- [ ] 发布时复用现有 `AdminKnowledgeController` 的解析 + 向量化逻辑
- [ ] 撤销发布时调用 `vectorStore.delete()`

### 后端（文档 AI）
- [ ] 新建 `DocumentAiService`（独立类，不复用 LLMService）
- [ ] 实现 6 个 Tool：searchDocuments / getDocument / createDocument / updateDocument / appendDocument / listDocuments
- [ ] 文档 AI 对话端点 `/admin/documents/ai/chat`（SSE 流式）
- [ ] RAG 检索复用 `vector_store`（与现有知识库共用）

### 管理端前端
- [ ] 文档列表页（表格 + 搜索 + 筛选）
- [ ] 文档编辑页（MD 编辑器 + 预览 + 发布按钮）
- [ ] 批量导入功能（文件选择 + 上传）
- [ ] 侧边栏菜单新增"文档管理"
- [ ] 文档管理页 AI 助手入口（侧边栏 Dialog，流式对话 + Tool 结果展示）
