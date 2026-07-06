package com.snaplearn.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snaplearn.entity.DocumentComment;
import com.snaplearn.entity.SnapDocument;
import com.snaplearn.mapper.DocumentCommentMapper;
import com.snaplearn.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicBlogController {

    private final DocumentMapper documentMapper;
    private final DocumentCommentMapper commentMapper;

    /** 博客列表（仅 published + shared，可选按 userId 过滤） */
    @GetMapping("/documents")
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String parentIds,
            @RequestParam(required = false) String userId) {
        QueryWrapper<SnapDocument> qw = new QueryWrapper<>();
        qw.eq("status", "published").eq("visibility", "shared").ne("doc_type", "folder");
        if (category != null && !category.isEmpty()) qw.eq("category", category);
        if (parentIds != null && !parentIds.isEmpty()) {
            List<String> ids = Arrays.asList(parentIds.split(","));
            qw.in("parent_id", ids);
        }
        if (userId != null && !userId.isEmpty()) qw.eq("user_id", userId);
        qw.orderByDesc("updated_at");
        Page<SnapDocument> pg = new Page<>(page, size);
        Page<SnapDocument> result = documentMapper.selectPage(pg, qw);
        List<Map<String, Object>> items = result.getRecords().stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("title", d.getTitle());
            m.put("category", d.getCategory());
            m.put("tags", d.getTags());
            m.put("summary", d.getContent() != null ? d.getContent().substring(0, Math.min(d.getContent().length(), 200)) : "");
            long commentCount = commentMapper.selectCount(
                    new QueryWrapper<DocumentComment>().eq("document_id", d.getId()));
            m.put("comment_count", commentCount);
            m.put("updated_at", d.getUpdatedAt() != null ? d.getUpdatedAt().toString() : "");
            return m;
        }).toList();
        return Map.of("items", items, "total", result.getTotal(), "page", page, "size", size);
    }

    /** 文章详情 */
    @GetMapping("/documents/{id}")
    public Map<String, Object> detail(@PathVariable String id) {
        SnapDocument doc = documentMapper.selectById(id);
        if (doc == null || !"published".equals(doc.getStatus()) || !"shared".equals(doc.getVisibility())) {
            return Map.of("error", "文档不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", doc.getId());
        result.put("title", doc.getTitle());
        result.put("content", doc.getContent() != null ? doc.getContent() : "");
        result.put("category", doc.getCategory() != null ? doc.getCategory() : "");
        result.put("tags", doc.getTags() != null ? doc.getTags() : "");
        result.put("updated_at", doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : "");
        return result;
    }

    /** 评论列表 */
    @GetMapping("/documents/{id}/comments")
    public List<Map<String, Object>> comments(@PathVariable String id) {
        QueryWrapper<DocumentComment> qw = new QueryWrapper<>();
        qw.eq("document_id", id).isNull("parent_id").orderByDesc("created_at");
        List<DocumentComment> parents = commentMapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DocumentComment p : parents) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("author_name", p.getAuthorName());
            m.put("content", p.getContent());
            m.put("created_at", p.getCreatedAt() != null ? p.getCreatedAt().toString() : "");
            // Child replies
            QueryWrapper<DocumentComment> cqw = new QueryWrapper<>();
            cqw.eq("parent_id", p.getId()).orderByAsc("created_at");
            List<Map<String, Object>> children = commentMapper.selectList(cqw).stream().map(c -> {
                Map<String, Object> cm = new HashMap<>();
                cm.put("id", c.getId());
                cm.put("author_name", c.getAuthorName());
                cm.put("content", c.getContent());
                cm.put("created_at", c.getCreatedAt() != null ? c.getCreatedAt().toString() : "");
                return cm;
            }).toList();
            m.put("replies", children);
            result.add(m);
        }
        return result;
    }

    /** 发表评论（登录/匿名均可） */
    @PostMapping("/documents/{id}/comments")
    public Map<String, Object> postComment(@PathVariable String id,
                                            @RequestBody Map<String, String> body) {
        DocumentComment c = new DocumentComment();
        c.setId(UUID.randomUUID().toString());
        c.setDocumentId(id);
        c.setAuthorName(body.getOrDefault("author_name", "匿名"));
        c.setContent(body.get("content"));
        c.setParentId(body.get("parent_id"));
        commentMapper.insert(c);
        return Map.of("ok", true, "id", c.getId());
    }

    /** 文档树：所有 published 文件夹 + shared 文档（私有文件夹下的共享文档可见） */
    @GetMapping("/tree")
    public List<SnapDocument> tree(@RequestParam(required = false) String userId) {
        QueryWrapper<SnapDocument> qw = new QueryWrapper<>();
        qw.eq("status", "published")
          .and(w -> w.eq("visibility", "shared").or().eq("doc_type", "folder"));
        if (userId != null && !userId.isEmpty()) qw.eq("user_id", userId);
        qw.orderByAsc("doc_type", "sort_order", "title");
        return documentMapper.selectList(qw);
    }

    /** 分类列表 */
    @GetMapping("/categories")
    public Map<String, Object> categories() {
        QueryWrapper<SnapDocument> qw = new QueryWrapper<>();
        qw.eq("status", "published").eq("visibility", "shared").ne("doc_type", "folder").select("category");
        Set<String> cats = new LinkedHashSet<>();
        documentMapper.selectList(qw).forEach(d -> {
            if (d != null && d.getCategory() != null && !d.getCategory().isEmpty()) cats.add(d.getCategory());
        });
        return Map.of("categories", cats);
    }
}
