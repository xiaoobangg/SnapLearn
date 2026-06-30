package com.snaplearn.controller;

import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.ApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户自服务：管理自己的 API Key。
 */
@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /** 创建新 Key，返回完整明文（仅此一次） */
    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, String> body, @CurrentUser String userId) {
        String name = body.getOrDefault("name", "未命名");
        return apiKeyService.create(userId, name);
    }

    /** 我的 Key 列表（不返回明文） */
    @GetMapping
    public List<Map<String, Object>> list(@CurrentUser String userId) {
        return apiKeyService.listByUser(userId).stream().map(k -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", k.getId());
            m.put("name", k.getName());
            m.put("prefix", k.getKeyPrefix());
            m.put("is_active", k.getIsActive());
            m.put("last_used_at", k.getLastUsedAt() != null ? k.getLastUsedAt().toString() : null);
            m.put("created_at", k.getCreatedAt() != null ? k.getCreatedAt().toString() : null);
            return m;
        }).collect(Collectors.toList());
    }

    /** 撤销 */
    @DeleteMapping("/{id}")
    public Map<String, Object> revoke(@PathVariable String id, @CurrentUser String userId) {
        apiKeyService.revoke(id, userId);
        return Map.of("ok", true);
    }
}
