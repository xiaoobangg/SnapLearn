package com.snaplearn.service.agent;

import com.snaplearn.service.AgentMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 长期记忆 @Tool 工具集（无状态单例），ChatClient 与 ReactAgent 共用。
 * <p>
 * userId 解析统一委托 {@link UserIdResolver}，实际 CRUD 委托 {@link AgentMemoryService}，
 * 本类只做 @Tool 适配 + 调用日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryChatTools {

    private final AgentMemoryService agentMemoryService;

    private String userId(ToolContext ctx) {
        return UserIdResolver.resolve(ctx);
    }

    /**
     * 保存用户个性化信息到长期记忆。
     * <p>
     * 通过 @Tool 注解暴露给 LLM，支持保存考试目标、英语水平、学习偏好等个性化配置。
     * 内部委托 {@link AgentMemoryService#save} 执行实际存储，失败时吞异常不影响主链路。
     *
     * @param key         记忆键（如 exam_goal、english_level、learning_time）
     * @param value       记忆值（如 考研英语一、CET-4、晚上有空）
     * @param toolContext Spring AI 工具上下文，用于解析当前 userId
     * @return 包含 success 和 key 的响应 Map
     */
    @Tool(description = "保存用户的个性化信息到长期记忆中（考试目标、英语水平、学习偏好等）。")
    public Map<String, Object> saveMemory(
            @ToolParam(description = "记忆的键，如 exam_goal、english_level、learning_time") String key,
            @ToolParam(description = "记忆的值，如 考研英语一、CET-4、晚上有空") String value,
            ToolContext toolContext
    ) {
        String userId = userId(toolContext);
        agentMemoryService.save(userId, key, value);
        log.info("[MEMORY-TOOL] saveMemory userId={} key={}", userId, key);
        return Map.of("success", true, "key", key);
    }

    /**
     * 从长期记忆中读取用户个性化信息。
     * <p>
     * 支持按 key 查询单条记忆，或不传 key 返回全部记忆列表。
     * 内部委托 {@link AgentMemoryService#recall} 执行查询，失败时返回错误信息。
     *
     * @param key         记忆键（可选，留空则返回全部）
     * @param toolContext Spring AI 工具上下文，用于解析当前 userId
     * @return 单条记忆返回 {key, value}，全部记忆返回 {items, count}，失败返回 {error}
     */
    @Tool(description = "从长期记忆中读取用户的个性化信息。传入 key 查某一条，不传或传空则返回全部。")
    public Map<String, Object> recallMemory(
            @ToolParam(description = "记忆的键，留空则返回全部记忆") String key,
            ToolContext toolContext
    ) {
        String userId = userId(toolContext);
        Map<String, Object> result = agentMemoryService.recall(userId, key);
        log.info("[MEMORY-TOOL] recallMemory userId={} key={} hasData={}",
                userId, key, result.get("value") != null || result.get("items") != null);
        return result;
    }

    /**
     * 删除用户长期记忆中的某一条。
     * <p>
     * 通过 @Tool 注解暴露给 LLM，支持用户主动清除已失效的个性化配置。
     * 内部委托 {@link AgentMemoryService#delete} 执行删除，失败时吞异常不影响主链路。
     *
     * @param key         要删除的记忆键
     * @param toolContext Spring AI 工具上下文，用于解析当前 userId
     * @return 包含 success 和 key 的响应 Map
     */
    @Tool(description = "删除用户长期记忆中的某一条。")
    public Map<String, Object> deleteMemory(
            @ToolParam(description = "要删除的记忆的键") String key,
            ToolContext toolContext
    ) {
        String userId = userId(toolContext);
        agentMemoryService.delete(userId, key);
        log.info("[MEMORY-TOOL] deleteMemory userId={} key={}", userId, key);
        return Map.of("success", true, "key", key);
    }
}
