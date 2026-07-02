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

    /**
     * 保存或更新一条长期记忆。
     * <p>
     * 按 (userId, key) 唯一匹配：存在则更新 value，不存在则新建。
     * 异常被吞掉仅 log.error，不影响调用方主链路。
     *
     * @param userId 用户 ID
     * @param key    记忆键（如 exam_goal、english_level）
     * @param value  记忆值
     */
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

    /**
     * 读取长期记忆。
     * <p>
     * 传入 key 时返回单条 {key, value}；key 为空/null 时列出该用户全部记忆 {items, count}。
     * 异常时返回 {error} 而非抛出，保证调用方主链路不受影响。
     *
     * @param userId 用户 ID
     * @param key    记忆键（可选，为空则返回全部）
     * @return 单条 {key, value} / 全部 {items, count} / 失败 {error}
     */
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

    /**
     * 删除一条长期记忆。
     * <p>
     * 按 (userId, key) 精确删除。异常被吞掉仅 log.error。
     *
     * @param userId 用户 ID
     * @param key    要删除的记忆键
     */
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
