package com.snaplearn.dto.request;

import java.util.List;

/**
 * /api/v1/chat/call 的扩展请求体，兼容原 chat 模式 + agent 模式。
 * <p>
 * agent 模式下 {@code mode="agent"}，且通常携带：
 * <ul>
 *   <li>{@link AgentContext} —— 来自拍照页的 OCR 文本和候选词</li>
 *   <li>{@link PendingAction} —— 上一轮未确认的写操作（如有），由前端透传</li>
 * </ul>
 */
public record AgentChatRequest(
        String message,
        String model,
        String chatId,
        String mode,
        AgentContext agentContext,
        PendingAction pendingAction
) {
    public boolean isAgentMode() {
        return "agent".equalsIgnoreCase(mode);
    }

    /**
     * 拍照→OCR→选词流程带入对话的上下文，让 Agent 直接拿到候选词
     */
    public record AgentContext(
            String ocrText,
            List<String> candidateWords,
            String sourceImageUrl
    ) {
    }

    /**
     * 待用户确认的写操作。当前仅一种类型：create_card_group。
     * payload 字段用 Map 是为了向前兼容未来扩展。
     */
    public record PendingAction(
            String type,
            java.util.Map<String, Object> payload
    ) {
    }
}
