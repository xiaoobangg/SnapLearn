package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.entity.AgentMemory;
import com.snaplearn.mapper.AgentMemoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 长期记忆 CRUD，Agent 和 LLM 共用。
 * <p>
 * 所有方法内部吞异常 + log.error，不抛给调用方（记忆是辅助功能，不能影响主链路）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentMemoryService {

    private final AgentMemoryMapper mapper;

    public void save(String userId, String key, String value) {
        try {
            QueryWrapper<AgentMemory> qw = new QueryWrapper<>();
            qw.eq("user_id", userId).eq("memory_key", key);
            AgentMemory existing = mapper.selectOne(qw);
            if (existing != null) {
                existing.setMemoryValue(value);
                existing.setUpdatedAt(LocalDateTime.now());
                mapper.updateById(existing);
            } else {
                AgentMemory m = new AgentMemory();
                m.setId(UUID.randomUUID().toString());
                m.setUserId(userId);
                m.setMemoryKey(key);
                m.setMemoryValue(value);
                m.setCreatedAt(LocalDateTime.now());
                m.setUpdatedAt(LocalDateTime.now());
                mapper.insert(m);
            }
        } catch (Exception e) {
            log.error("[MEMORY] save failed userId={} key={}", userId, key, e);
        }
    }

    public Map<String, Object> recall(String userId, String key) {
        try {
            if (key != null && !key.isBlank()) {
                QueryWrapper<AgentMemory> qw = new QueryWrapper<>();
                qw.eq("user_id", userId).eq("memory_key", key);
                AgentMemory m = mapper.selectOne(qw);
                if (m != null) {
                    return Map.of("key", m.getMemoryKey(), "value", m.getMemoryValue());
                }
                return Map.of("key", key, "value", "");
            } else {
                // 无 key 列出所有
                QueryWrapper<AgentMemory> qw = new QueryWrapper<>();
                qw.eq("user_id", userId).orderByAsc("memory_key");
                List<AgentMemory> list = mapper.selectList(qw);
                List<Map<String, String>> items = new ArrayList<>();
                for (AgentMemory m : list) {
                    items.add(Map.of("key", m.getMemoryKey(), "value", m.getMemoryValue() != null ? m.getMemoryValue() : ""));
                }
                return Map.of("items", items, "count", items.size());
            }
        } catch (Exception e) {
            log.error("[MEMORY] recall failed userId={} key={}", userId, key, e);
            return Map.of("error", "读取失败：" + e.getMessage());
        }
    }

    public void delete(String userId, String key) {
        try {
            QueryWrapper<AgentMemory> qw = new QueryWrapper<>();
            qw.eq("user_id", userId).eq("memory_key", key);
            mapper.delete(qw);
        } catch (Exception e) {
            log.error("[MEMORY] delete failed userId={} key={}", userId, key, e);
        }
    }
}
