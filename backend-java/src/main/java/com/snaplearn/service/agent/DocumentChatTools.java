package com.snaplearn.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.entity.SnapDocument;
import com.snaplearn.mapper.DocumentMapper;
import com.snaplearn.service.agent.UserIdResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 文档管理 @Tool 工具集，供 AI Agent 在对话中操作用户的 Markdown 文档库。
 * <p>
 * 所有工具方法均按 userId 隔离数据，通过 {@link UserIdResolver} 从 ToolContext 解析当前用户。
 * 支持语义搜索、全文读取、新建、更新、追加、列表查询等操作。
 */
@Component
@RequiredArgsConstructor
public class DocumentChatTools {

    private final DocumentMapper documentMapper;
    private final VectorStore vectorStore;

    @Tool(description = "语义搜索文档库中的文档，返回匹配的文档标题和内容片段")
    public List<Map<String, Object>> searchDocuments(
            @ToolParam(description = "搜索关键词或问题") String query,
            ToolContext toolContext) {
        String userId = UserIdResolver.resolve(toolContext);
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(5)
                .filterExpression("user_id == '" + userId.replace("'", "\\'") + "'")
                .build();
        List<Document> results = vectorStore.similaritySearch(request);
        List<Map<String, Object>> docs = new ArrayList<>();
        for (Document d : results) {
            docs.add(Map.of(
                    "file_id", d.getMetadata().getOrDefault("file_id", ""),
                    "file_name", d.getMetadata().getOrDefault("file_name", ""),
                    "snippet", d.getText().substring(0, Math.min(d.getText().length(), 300))
            ));
        }
        return docs;
    }

    @Tool(description = "获取指定文档的完整 Markdown 内容")
    public Map<String, Object> getDocument(
            @ToolParam(description = "文档ID") String documentId,
            ToolContext toolContext) {
        String userId = UserIdResolver.resolve(toolContext);
        SnapDocument doc = documentMapper.selectById(documentId);
        if (doc == null || !doc.getUserId().equals(userId)) {
            return Map.of("error", "文档不存在");
        }
        return Map.of(
                "id", doc.getId(),
                "title", doc.getTitle(),
                "content", doc.getContent(),
                "status", doc.getStatus(),
                "category", doc.getCategory() != null ? doc.getCategory() : ""
        );
    }

    @Tool(description = "新建一篇 Markdown 文档，状态为草稿，需用户手动发布")
    public Map<String, Object> createDocument(
            @ToolParam(description = "文档标题") String title,
            @ToolParam(description = "Markdown 格式的文档内容") String content,
            @ToolParam(description = "文档分类，可选") String category,
            ToolContext toolContext) {
        String userId = UserIdResolver.resolve(toolContext);
        SnapDocument doc = new SnapDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setUserId(userId);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setCategory(category != null ? category : "");
        doc.setStatus("draft");
        doc.setSourceType("md");
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(doc);
        return Map.of("ok", true, "id", doc.getId(), "title", doc.getTitle(), "status", "draft",
                "message", "文档已创建，保存后自动发布");
    }

    @Tool(description = "全文替换修改一篇文档内容，保存后自动发布")
    public Map<String, Object> updateDocument(
            @ToolParam(description = "文档ID") String documentId,
            @ToolParam(description = "新的完整 Markdown 内容") String content,
            ToolContext toolContext) {
        String userId = UserIdResolver.resolve(toolContext);
        SnapDocument doc = documentMapper.selectById(documentId);
        if (doc == null || !doc.getUserId().equals(userId)) {
            return Map.of("error", "文档不存在");
        }
        doc.setContent(content);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);
        return Map.of("ok", true, "id", doc.getId(), "message", "文档已更新");
    }

    @Tool(description = "向文档末尾追加 Markdown 内容")
    public Map<String, Object> appendDocument(
            @ToolParam(description = "文档ID") String documentId,
            @ToolParam(description = "要追加的 Markdown 内容") String appendContent,
            ToolContext toolContext) {
        String userId = UserIdResolver.resolve(toolContext);
        SnapDocument doc = documentMapper.selectById(documentId);
        if (doc == null || !doc.getUserId().equals(userId)) {
            return Map.of("error", "文档不存在");
        }
        doc.setContent(doc.getContent() + "\n\n" + appendContent);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);
        return Map.of("ok", true, "id", doc.getId(), "message", "内容已追加");
    }

    @Tool(description = "列出文档库中的文档列表，可按分类和状态筛选")
    public List<Map<String, Object>> listDocuments(
            @ToolParam(description = "分类筛选，可选") String category,
            @ToolParam(description = "状态筛选: draft/published/archived，可选") String status,
            ToolContext toolContext) {
        String userId = UserIdResolver.resolve(toolContext);
        QueryWrapper<SnapDocument> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        if (category != null && !category.isEmpty()) qw.eq("category", category);
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        qw.orderByDesc("updated_at");
        return documentMapper.selectList(qw).stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("title", d.getTitle());
            m.put("status", d.getStatus());
            m.put("category", d.getCategory() != null ? d.getCategory() : "");
            return m;
        }).toList();
    }
}
