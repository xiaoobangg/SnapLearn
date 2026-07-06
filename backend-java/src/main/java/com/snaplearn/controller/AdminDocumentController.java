package com.snaplearn.controller;

import com.snaplearn.entity.SnapDocument;
import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final DocumentService documentService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // ==================== CRUD ====================

    @GetMapping
    public Map<String, Object> list(
            @CurrentUser String userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = documentService.list(userId, keyword, category, status, page, size);
        return Map.of(
                "items", result.getRecords(),
                "total", result.getTotal(),
                "page", result.getCurrent(),
                "size", result.getSize()
        );
    }

    @GetMapping("/tree")
    public List<SnapDocument> tree(@CurrentUser String userId) {
        return documentService.listTree(userId);
    }

    @PostMapping("/tree/folders")
    public SnapDocument createFolder(@RequestBody Map<String, String> body, @CurrentUser String userId) {
        return documentService.createFolder(userId, body.get("title"), body.get("parentId"));
    }

    @GetMapping("/item/{id}")
    public SnapDocument get(@PathVariable String id, @CurrentUser String userId) {
        return documentService.getById(id, userId);
    }

    @PostMapping
    public SnapDocument create(@RequestBody Map<String, String> body, @CurrentUser String userId) {
        return documentService.createAndPublish(
                userId,
                body.get("title"),
                body.getOrDefault("content", ""),
                body.get("category"),
                body.get("tags"),
                body.get("parentId"),
                body.getOrDefault("visibility", "private")
        );
    }

    @PutMapping("/{id}")
    public SnapDocument update(@PathVariable String id, @RequestBody Map<String, String> body,
                                @CurrentUser String userId) {
        return documentService.updateAndPublish(
                id, userId,
                body.get("title"),
                body.get("content"),
                body.get("category"),
                body.get("tags"),
                body.getOrDefault("visibility", "private")
        );
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id, @CurrentUser String userId) {
        documentService.delete(id, userId);
        return Map.of("ok", true);
    }

    @PutMapping("/{id}/move")
    public Map<String, Object> move(@PathVariable String id, @RequestBody Map<String, String> body,
                                     @CurrentUser String userId) {
        documentService.move(id, body.get("parentId"), userId);
        return Map.of("ok", true);
    }

    // ==================== 发布 / 撤销 ====================

    @PostMapping("/{id}/publish")
    public SnapDocument publish(@PathVariable String id, @CurrentUser String userId) {
        return documentService.publish(id, userId);
    }

    @PostMapping("/{id}/unpublish")
    public SnapDocument unpublish(@PathVariable String id, @CurrentUser String userId) {
        return documentService.unpublish(id, userId);
    }

    @PostMapping("/batch-publish")
    public Map<String, Object> batchPublish(@RequestBody Map<String, List<String>> body,
                                             @CurrentUser String userId) {
        List<SnapDocument> docs = documentService.batchPublish(userId, body.getOrDefault("ids", List.of()));
        return Map.of("ok", true, "count", docs.size());
    }

    // ==================== 批量导入 MD ====================

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> importFiles(@RequestParam("files") List<MultipartFile> files,
                                           @CurrentUser String userId) {
        List<SnapDocument> docs = documentService.importFiles(userId, files);
        return Map.of("ok", true, "count", docs.size());
    }

    // ==================== 分类 ====================

    @GetMapping("/categories")
    public Map<String, Object> categories(@CurrentUser String userId) {
        return Map.of("categories", documentService.getCategories(userId));
    }

    // ==================== 图片上传 ====================

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String origName = file.getOriginalFilename();
            String ext = "";
            if (origName != null && origName.contains(".")) {
                ext = origName.substring(origName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + ext;
            // 必须用绝对路径，否则 Tomcat 会把文件写到临时目录
            Path dir = Paths.get(uploadDir, "images").toAbsolutePath();
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target.toAbsolutePath().toFile());
            String url = "/uploads/images/" + filename;
            return Map.of("url", url);
        } catch (Exception e) {
            return Map.of("error", "上传失败: " + e.getMessage());
        }
    }
}
