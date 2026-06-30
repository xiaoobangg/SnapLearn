package com.snaplearn.service.agent;

/**
 * 请求级 Agent 上下文持有器（ThreadLocal）。
 * <p>
 * 每次请求开始前由 ChatController 注入 userId，Agent tools 内部直接 get()
 * 而不需要每次请求 new 一份工具实例。请求结束后由 ChatController 清理。
 */
public final class AgentContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private AgentContext() {}

    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
