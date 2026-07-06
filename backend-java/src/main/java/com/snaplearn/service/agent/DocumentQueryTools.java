package com.snaplearn.service.agent;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.entity.SnapDocument;
import com.snaplearn.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DocumentQueryTools {

    private final VectorStore vectorStore;
    private final DocumentMapper documentMapper;

    @Tool(description = "语义搜索文档库中的文档，返回相关文档摘要")
    public List<Map<String, Object>> searchDocuments(
            @ToolParam(description = "搜索关键词或问题") String query) {
        String userId = AgentContext.getUserId();
        var filter = new FilterExpressionBuilder().eq("user_id", userId).build();
        var request = SearchRequest.builder()
                .query(query)
                .topK(5)
                .similarityThreshold(0.3)
                .filterExpression(filter)
                .build();
        return vectorStore.similaritySearch(request).stream().map(doc -> {
            String fileId = (String) doc.getMetadata().get("file_id");
            String fileName = (String) doc.getMetadata().get("file_name");
            return Map.<String, Object>of(
                    "file_id", fileId != null ? fileId : "",
                    "file_name", fileName != null ? fileName : "",
                    "snippet", doc.getText() != null ? doc.getText().substring(0, Math.min(doc.getText().length(), 300)) : ""
            );
        }).collect(Collectors.toList());
    }

    @Tool(description = "获取文档的完整 Markdown 内容")
    public Map<String, Object> getDocument(
            @ToolParam(description = "文档ID") String documentId) {
        String userId = AgentContext.getUserId();
        QueryWrapper<SnapDocument> qw = new QueryWrapper<>();
        qw.eq("id", documentId).eq("user_id", userId);
        SnapDocument doc = documentMapper.selectOne(qw);
        if (doc == null) return Map.of("error", "文档不存在");
        return Map.of(
                "id", doc.getId(),
                "title", doc.getTitle(),
                "content", doc.getContent() != null ? doc.getContent() : "",
                "status", doc.getStatus() != null ? doc.getStatus() : "",
                "category", doc.getCategory() != null ? doc.getCategory() : ""
        );
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
