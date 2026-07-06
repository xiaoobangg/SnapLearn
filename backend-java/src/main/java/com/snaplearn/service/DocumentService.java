package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.entity.KnowledgeFile;
import com.snaplearn.entity.SnapDocument;
import com.snaplearn.mapper.DocumentMapper;
import com.snaplearn.mapper.KnowledgeFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final KnowledgeFileMapper knowledgeFileMapper;
    private final KnowledgeVectorService knowledgeVectorService;

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    // ==================== CRUD ====================

    public SnapDocument getById(String id, String userId) {
        SnapDocument doc = documentMapper.selectById(id);
        if (doc == null || !doc.getUserId().equals(userId)) {
            throw new BusinessException(404, "文档不存在");
        }
        return doc;
    }

    public Page<SnapDocument> list(String userId, String keyword, String category, String status,
                                    int page, int size) {
        QueryWrapper<SnapDocument> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        if (category != null && !category.isEmpty()) {
            qw.eq("category", category);
        }
        if (status != null && !status.isEmpty()) {
            qw.eq("status", status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.like("title", keyword);
        }
        qw.orderByDesc("updated_at");
        return documentMapper.selectPage(new Page<>(page, size), qw);
    }

    /** 树形列表：返回用户所有文档（含文件夹），前端构建树 */
    public List<SnapDocument> listTree(String userId) {
        QueryWrapper<SnapDocument> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).orderByAsc("doc_type", "sort_order", "title");
        return documentMapper.selectList(qw);
    }

    /** 创建文件夹 */
    public SnapDocument createFolder(String userId, String title, String parentId) {
        SnapDocument doc = new SnapDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setUserId(userId);
        doc.setTitle(title);
        doc.setContent(""); // 设置为空字符串，满足非空约束
        doc.setCategory(title); // 分类名称就是文件夹名称
        doc.setDocType("folder");
        doc.setParentId(parentId);
        doc.setStatus("published");
        doc.setVisibility("private");
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(doc);
        return doc;
    }

    public SnapDocument createAndPublish(String userId, String title, String content, String category, String tags, String parentId, String visibility) {
        SnapDocument doc = new SnapDocument();
        doc.setId(UUID.randomUUID().toString());
        doc.setUserId(userId);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setCategory(category);
        doc.setTags(tags);
        doc.setParentId(parentId);
        doc.setVisibility(visibility != null ? visibility : "private");
        doc.setStatus("draft");
        doc.setSourceType("md");
        doc.setSortOrder(0);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(doc);
        // Auto-publish after create
        return publishDocument(doc, userId);
    }

    /** @deprecated use createAndPublish */
    public SnapDocument create(String userId, String title, String content, String category, String tags) {
        return createAndPublish(userId, title, content, category, tags, null, null);
    }

    public SnapDocument create(String userId, String title, String content, String category, String tags, String parentId) {
        return createAndPublish(userId, title, content, category, tags, parentId, null);
    }

    public SnapDocument updateAndPublish(String id, String userId, String title, String content, String category, String tags, String visibility) {
        SnapDocument doc = getById(id, userId);
        if (title != null) doc.setTitle(title);
        if (content != null) doc.setContent(content);
        if (category != null) doc.setCategory(category);
        if (tags != null) doc.setTags(tags);
        if (visibility != null) doc.setVisibility(visibility);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);
        // Auto-publish after save
        return publishDocument(doc, userId);
    }

    public void delete(String id, String userId) {
        SnapDocument doc = getById(id, userId);
        // Recursively delete children if folder
        if ("folder".equals(doc.getDocType())) {
            QueryWrapper<SnapDocument> cq = new QueryWrapper<>();
            cq.eq("parent_id", id);
            documentMapper.selectList(cq).forEach(c -> delete(c.getId(), userId));
        }
        if ("published".equals(doc.getStatus()) && doc.getKnowledgeFileId() != null) {
            unpublish(id, userId);
        }
        documentMapper.deleteById(id);
    }

    public void move(String id, String newParentId, String userId) {
        SnapDocument doc = getById(id, userId);
        // 防止将文件夹移动到自己或自己的子文件夹下
        if (newParentId != null && "folder".equals(doc.getDocType())) {
            if (id.equals(newParentId)) return;
            SnapDocument parent = getById(newParentId, userId);
            if (!"folder".equals(parent.getDocType())) return; // 只能移到文件夹下
        }
        doc.setParentId(newParentId);
        documentMapper.updateById(doc);
    }

    // ==================== 批量导入 ====================

    public List<SnapDocument> importFiles(String userId, List<MultipartFile> files) {
        List<SnapDocument> docs = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String content = new String(file.getBytes());
                String title = file.getOriginalFilename();
                if (title != null) {
                    title = title.replaceAll("\\.(md|markdown|txt)$", "");
                }
                SnapDocument doc = create(userId, title, content, "未分类", "");
                doc.setSourceName(file.getOriginalFilename());
                doc.setFileSize(file.getSize());
                doc.setUpdatedAt(LocalDateTime.now());
                documentMapper.updateById(doc);
                docs.add(doc);
            } catch (IOException e) {
                log.error("Failed to read imported file", e);
            }
        }
        return docs;
    }

    // ==================== 发布/撤销 ====================

    @Transactional
    public SnapDocument publish(String id, String userId) {
        return publishDocument(getById(id, userId), userId);
    }

    private SnapDocument publishDocument(SnapDocument doc, String userId) {
        // If already published, delete old vectors first
        if (doc.getKnowledgeFileId() != null) {
            knowledgeVectorService.deleteByFileId(doc.getKnowledgeFileId());
            deleteKnowledgeFileRecord(doc.getKnowledgeFileId(), userId);
        }

        try {
            // Write content to temp .md file
            Path knowledgeDir = Paths.get(uploadDir, "knowledge");
            Files.createDirectories(knowledgeDir);
            String fileId = UUID.randomUUID().toString();
            String fileName = doc.getTitle() + ".md";
            Path filePath = knowledgeDir.resolve(fileId + "_" + fileName);
            Files.writeString(filePath, doc.getContent());

            // Vectorize
            int chunkCount = knowledgeVectorService.vectorize(
                    filePath.toString(), fileName, fileId, userId,
                    doc.getStatus(), doc.getVisibility());

            // Save knowledge file record
            KnowledgeFile kf = new KnowledgeFile();
            kf.setId(fileId);
            kf.setUserId(userId);
            kf.setFileName(fileName);
            kf.setFilePath(filePath.toString());
            kf.setFileSize((long) doc.getContent().length());
            kf.setChunkCount(chunkCount);
            knowledgeFileMapper.insert(kf);

            // Update document
            doc.setStatus("published");
            doc.setKnowledgeFileId(fileId);
            doc.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(doc);

            log.info("Document published: id={}, title={}, chunks={}", doc.getId(), doc.getTitle(), chunkCount);
            return doc;
        } catch (Exception e) {
            log.error("Failed to publish document: id={}", doc.getId(), e);
            throw new BusinessException(500, "发布失败: " + e.getMessage());
        }
    }

    public SnapDocument unpublish(String id, String userId) {
        SnapDocument doc = getById(id, userId);
        if (!"published".equals(doc.getStatus())) {
            throw new BusinessException(400, "该文档未发布");
        }

        if (doc.getKnowledgeFileId() != null) {
            knowledgeVectorService.deleteByFileId(doc.getKnowledgeFileId());
            deleteKnowledgeFileRecord(doc.getKnowledgeFileId(), userId);
        }

        doc.setStatus("draft");
        doc.setKnowledgeFileId(null);
        doc.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(doc);
        return doc;
    }

    public List<SnapDocument> batchPublish(String userId, List<String> ids) {
        List<SnapDocument> docs = new ArrayList<>();
        for (String id : ids) {
            try {
                docs.add(publish(id, userId));
            } catch (Exception e) {
                log.error("Batch publish failed for doc: {}", id, e);
            }
        }
        return docs;
    }

    // ==================== Categories ====================

    public Set<String> getCategories(String userId) {
        QueryWrapper<SnapDocument> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).select("DISTINCT category");
        Set<String> cats = new LinkedHashSet<>();
        documentMapper.selectList(qw).forEach(d -> {
            if (d.getCategory() != null && !d.getCategory().isEmpty()) {
                cats.add(d.getCategory());
            }
        });
        return cats;
    }

    // ==================== Private helpers ====================

    private void deleteKnowledgeFileRecord(String fileId, String userId) {
        QueryWrapper<KnowledgeFile> qw = new QueryWrapper<>();
        qw.eq("id", fileId).eq("user_id", userId);
        knowledgeFileMapper.delete(qw);
    }
}
