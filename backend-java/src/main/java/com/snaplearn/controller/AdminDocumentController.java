package com.snaplearn.controller;

import com.snaplearn.entity.SnapDocument;
import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final DocumentService documentService;

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

    @GetMapping("/{id}")
    public SnapDocument get(@PathVariable String id, @CurrentUser String userId) {
        return documentService.getById(id, userId);
    }

    @PostMapping
    public SnapDocument create(@RequestBody Map<String, String> body, @CurrentUser String userId) {
        return documentService.create(
                userId,
                body.get("title"),
                body.getOrDefault("content", ""),
                body.get("category"),
                body.get("tags")
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
                body.get("tags")
        );
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable String id, @CurrentUser String userId) {
        documentService.delete(id, userId);
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
}
