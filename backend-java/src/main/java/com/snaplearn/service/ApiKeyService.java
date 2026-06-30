package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.entity.ApiKey;
import com.snaplearn.mapper.ApiKeyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

/**
 * API Key 管理。Key 只创建时返回明文一次，存 hash。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyMapper apiKeyMapper;

    /** 创建新 Key，返回完整明文（调用方负责展示给用户，此后不再可得） */
    public Map<String, Object> create(String userId, String name) {
        String rawKey = "sk-" + randomHex(32);
        String hash = sha256(rawKey);
        String prefix = rawKey.substring(0, 10);

        ApiKey k = new ApiKey();
        k.setId(UUID.randomUUID().toString());
        k.setUserId(userId);
        k.setName(name);
        k.setKeyHash(hash);
        k.setKeyPrefix(prefix);
        k.setIsActive(true);
        apiKeyMapper.insert(k);

        return Map.of("id", k.getId(), "name", name, "key", rawKey, "prefix", prefix);
    }

    /** 用户的所有 Key 列表 */
    public List<ApiKey> listByUser(String userId) {
        QueryWrapper<ApiKey> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).orderByDesc("created_at");
        return apiKeyMapper.selectList(qw);
    }

    /** 撤销 */
    public void revoke(String id, String userId) {
        ApiKey k = apiKeyMapper.selectById(id);
        if (k != null && k.getUserId().equals(userId)) {
            k.setIsActive(false);
            apiKeyMapper.updateById(k);
        }
    }

    /** 按 hash 查 Key，通过则更新 last_used_at */
    public ApiKey authenticate(String rawKey) {
        String hash = sha256(rawKey);
        QueryWrapper<ApiKey> qw = new QueryWrapper<>();
        qw.eq("key_hash", hash).eq("is_active", true);
        ApiKey k = apiKeyMapper.selectOne(qw);
        if (k != null) {
            k.setLastUsedAt(LocalDateTime.now());
            apiKeyMapper.updateById(k);
        }
        return k;
    }

    private static String randomHex(int len) {
        byte[] b = new byte[len / 2];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
