package com.snaplearn.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.snaplearn.entity.ChatTrace;
import com.snaplearn.mapper.ChatTraceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin 端的 AI 对话日志查询。
 * - 列表接口出于性能考虑，user_message / response_text 截断预览
 * - 详情接口返回完整字段
 */
@RestController
@RequestMapping("/api/v1/admin/chat-traces")
@RequiredArgsConstructor
public class AdminChatTraceController {

    private static final int LIST_PREVIEW_LIMIT = 100;

    private final ChatTraceMapper chatTraceMapper;

    /**
     * 分页查询 AI 对话 trace 列表（Admin 端）。
     * <p>
     * 支持按 userId / chatId 模糊匹配、status 精确匹配、耗时区间筛选；
     * 出于性能考虑，user_message / response_text 截断为 {@value LIST_PREVIEW_LIMIT} 字符预览。
     *
     * @param page         当前页码（默认 1）
     * @param pageSize     每页条数（默认 20）
     * @param userId       用户 ID 模糊筛选（可选）
     * @param chatId       会话 ID 模糊筛选（可选）
     * @param status       状态筛选：success / error（可选）
     * @param minDurationMs 最小耗时 ms（可选）
     * @param maxDurationMs 最大耗时 ms（可选）
     * @return 分页结果，包含 items / total / page / page_size
     */
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String chatId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long minDurationMs,
            @RequestParam(required = false) Long maxDurationMs
    ) {
        QueryWrapper<ChatTrace> qw = new QueryWrapper<>();
        if (userId != null && !userId.isBlank()) {
            qw.like("user_id", userId);
        }
        if (chatId != null && !chatId.isBlank()) {
            qw.like("chat_id", chatId);
        }
        if (status != null && !status.isBlank()) {
            qw.eq("status", status);
        }
        if (minDurationMs != null) {
            qw.ge("duration_ms", minDurationMs);
        }
        if (maxDurationMs != null) {
            qw.le("duration_ms", maxDurationMs);
        }
        qw.orderByDesc("created_at");

        Page<ChatTrace> pg = chatTraceMapper.selectPage(new Page<>(page, pageSize), qw);
        List<Map<String, Object>> items = pg.getRecords().stream().map(t -> toListItem(t)).toList();

        return Map.of("items", items, "total", pg.getTotal(), "page", page, "page_size", pageSize);
    }

    /**
     * 查询单条 AI 对话 trace 详情（Admin 端）。
     * <p>
     * 返回完整字段，包括 user_message / response_text 全文、token 用量、耗时、错误信息等。
     *
     * @param id trace 记录 ID
     * @return 完整 trace 字段；记录不存在时返回空 Map
     */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable String id) {
        ChatTrace t = chatTraceMapper.selectById(id);
        if (t == null) {
            return Map.of();
        }
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("user_id", t.getUserId());
        m.put("chat_id", t.getChatId());
        m.put("model", t.getModel());
        m.put("user_message", nullToEmpty(t.getUserMessage()));
        m.put("response_text", nullToEmpty(t.getResponseText()));
        m.put("prompt_tokens", t.getPromptTokens());
        m.put("completion_tokens", t.getCompletionTokens());
        m.put("total_tokens", t.getTotalTokens());
        m.put("duration_ms", t.getDurationMs());
        m.put("status", t.getStatus());
        m.put("error_message", nullToEmpty(t.getErrorMessage()));
        m.put("created_at", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        return m;
    }

    private Map<String, Object> toListItem(ChatTrace t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", t.getId());
        m.put("user_id", t.getUserId());
        m.put("chat_id", t.getChatId());
        m.put("model", t.getModel());
        m.put("user_message", abbr(t.getUserMessage(), LIST_PREVIEW_LIMIT));
        m.put("response_text", abbr(t.getResponseText(), LIST_PREVIEW_LIMIT));
        m.put("prompt_tokens", t.getPromptTokens());
        m.put("completion_tokens", t.getCompletionTokens());
        m.put("total_tokens", t.getTotalTokens());
        m.put("duration_ms", t.getDurationMs());
        m.put("status", t.getStatus());
        m.put("created_at", t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        return m;
    }

    private static String abbr(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
