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

    @Tool(description = "创建新文件夹。parentId 为空则创建在根目录下")
    public Map<String, Object> createFolder(
            @ToolParam(description = "文件夹名称") String title,
            @ToolParam(description = "父文件夹ID，不填则创建在根目录") String parentId) {
        String userId = AgentContext.getUserId();
        SnapDocument doc = documentService.createFolder(userId, title, parentId);
        return Map.of("ok", true, "id", doc.getId(), "title", doc.getTitle(), "type", "folder");
    }

    /**
     * 按文件夹名称查找其ID（用户隔离）
     */
    private String findFolderId(String folderName, String userId) {
        QueryWrapper<SnapDocument> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("title", folderName).eq("doc_type", "folder");
        SnapDocument doc = documentMapper.selectOne(qw);
        return doc != null ? doc.getId() : null;
    }

    @Tool(description = """
            批量创建文件夹和文档。一次性创建多个文件夹及文档，AI 自行分类组织。
            参数 items 是一个列表，每项包含：
            - type: "folder" 或 "document"
            - title: 名称
            - parentTitle: 父文件夹名称（可选，不填则创建在根目录）
            - content: Markdown 内容（仅文档需要）
            无需用户确认，直接创建。
            """)
    public Map<String, Object> batchCreateStructure(
            @ToolParam(description = """
                    JSON 数组，每项格式: {"type":"folder或document","title":"名称",
                    "parentTitle":"父文件夹名(可选)","content":"Markdown内容(仅文档)"}
                    """) List<Map<String, Object>> items) {
        String userId = AgentContext.getUserId();
        int folders = 0, docs = 0;
        Map<String, String> folderCache = new java.util.HashMap<>();

        for (var item : items) {
            String type = (String) item.getOrDefault("type", "document");
            String title = (String) item.get("title");
            String parentTitle = (String) item.get("parentTitle");
            String content = (String) item.getOrDefault("content", "");

            String parentId = null;
            if (parentTitle != null && !parentTitle.isEmpty()) {
                parentId = folderCache.computeIfAbsent(parentTitle, k -> findFolderId(k, userId));
            }

            if ("folder".equals(type)) {
                SnapDocument doc = documentService.createFolder(userId, title, parentId);
                folderCache.put(title, doc.getId());
                folders++;
            } else {
                documentService.createAndPublish(userId, title, content != null ? content : "",
                        title, "", parentId, "private");
                docs++;
            }
        }
        return Map.of("ok", true, "folders_created", folders, "documents_created", docs);
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

    @Tool(description = "获取文档的完整 Markdown 内容，用于修改前阅读原文")
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
                "status", doc.getStatus() != null ? doc.getStatus() : ""
        );
    }

    /** 按文档名查找文档ID */
    private SnapDocument findDocByName(String name, String userId) {
        QueryWrapper<SnapDocument> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("title", name).ne("doc_type", "folder");
        return documentMapper.selectOne(qw);
    }

    @Tool(description = "按文档名称修改文档内容。先根据名称自动查找文档ID，再更新。")
    public Map<String, Object> updateDocumentByName(
            @ToolParam(description = "文档名称（标题）") String docName,
            @ToolParam(description = "新的 Markdown 全文内容") String content) {
        String userId = AgentContext.getUserId();
        SnapDocument doc = findDocByName(docName, userId);
        if (doc == null) return Map.of("error", "未找到文档：「" + docName + "」，请先用 listDocuments 确认名称");
        documentService.updateAndPublish(doc.getId(), userId, null, content, null, null, doc.getVisibility());
        return Map.of("ok", true, "id", doc.getId(), "title", doc.getTitle());
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
