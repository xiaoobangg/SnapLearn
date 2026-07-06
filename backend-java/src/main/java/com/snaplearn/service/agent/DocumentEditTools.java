package com.snaplearn.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.entity.SnapDocument;
import com.snaplearn.mapper.DocumentMapper;
import com.snaplearn.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DocumentEditTools {

    private final DocumentMapper documentMapper;
    private final DocumentService documentService;

    @Tool(description = "创建新文档。parentId 为空则创建在根目录下")
    public Map<String, Object> createDocument(
            @ToolParam(description = "文档标题") String title,
            @ToolParam(description = "Markdown 内容") String content,
            @ToolParam(description = "父文件夹ID，不填则创建在根目录") String parentId,
            @ToolParam(description = "分类标签") String category) {
        String userId = AgentContext.getUserId();
        SnapDocument doc = documentService.createAndPublish(
                userId, title, content, category, "", parentId, "private");
        return Map.of("ok", true, "id", doc.getId(), "title", doc.getTitle(), "status", doc.getStatus());
    }

    @Tool(description = "全文替换更新文档内容")
    public Map<String, Object> updateDocument(
            @ToolParam(description = "文档ID") String documentId,
            @ToolParam(description = "新的 Markdown 全文内容") String content) {
        String userId = AgentContext.getUserId();
        SnapDocument doc = documentService.getById(documentId, userId);
        if (doc == null) return Map.of("error", "文档不存在");
        documentService.updateAndPublish(documentId, userId, null, content, null, null, doc.getVisibility());
        return Map.of("ok", true, "id", documentId, "title", doc.getTitle());
    }

    @Tool(description = "在文档末尾追加内容")
    public Map<String, Object> appendDocument(
            @ToolParam(description = "文档ID") String documentId,
            @ToolParam(description = "要追加的内容") String appendContent) {
        String userId = AgentContext.getUserId();
        SnapDocument doc = documentService.getById(documentId, userId);
        if (doc == null) return Map.of("error", "文档不存在");
        String newContent = (doc.getContent() != null ? doc.getContent() : "") + "\n\n" + appendContent;
        documentService.updateAndPublish(documentId, userId, null, newContent, null, null, doc.getVisibility());
        return Map.of("ok", true, "id", documentId, "title", doc.getTitle());
    }

    @Tool(description = "列出文档树结构，包含文件夹和文档，用于查找目标文件夹或文档的ID")
    public List<Map<String, Object>> listDocuments(
            @ToolParam(description = "按分类过滤，可选") String category,
            @ToolParam(description = "按状态过滤(draft/published)，可选") String status) {
        String userId = AgentContext.getUserId();
        QueryWrapper<SnapDocument> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        if (category != null && !category.isEmpty()) qw.eq("category", category);
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        qw.orderByAsc("doc_type", "sort_order", "title").last("LIMIT 50");
        return documentMapper.selectList(qw).stream().map(d -> {
            return Map.<String, Object>of(
                    "id", d.getId(),
                    "title", d.getTitle(),
                    "doc_type", d.getDocType() != null ? d.getDocType() : "document",
                    "parent_id", d.getParentId() != null ? d.getParentId() : "",
                    "status", d.getStatus() != null ? d.getStatus() : "",
                    "category", d.getCategory() != null ? d.getCategory() : ""
            );
        }).collect(Collectors.toList());
    }
}
