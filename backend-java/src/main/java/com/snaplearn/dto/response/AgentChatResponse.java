package com.snaplearn.dto.response;

import java.util.List;

/**
 * Agent 模式的响应。同步返回 + 工具调用过程可见。
 */
public record AgentChatResponse(String reply,
                                List<AgentStep> trace,
                                PendingAction pendingAction) {
    /**
     * Agent 流程中的一步：tool_call / tool_result / final / direct_create / undo
     */
    public record AgentStep(
            int step,
            String action,
            String name,
            Object args,
            Object result,
            String content
    ) {
    }

    /**
     * 与 AgentChatRequest.PendingAction 对称，直接回传给前端
     */
    public record PendingAction(
            String type,
            java.util.Map<String, Object> payload
    ) {
    }
}
