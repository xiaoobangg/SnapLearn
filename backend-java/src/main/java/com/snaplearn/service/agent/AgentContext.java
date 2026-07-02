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

    /**
     * 设置当前线程的 userId。
     * <p>
     * 由 {@link AgentContextAspect} 在请求处理前调用，将 userId 绑定到当前线程。
     *
     * @param userId 当前用户 ID
     */
    public static void setUserId(String userId) {
        USER_ID.set(userId);
    }

    /**
     * 获取当前线程绑定的 userId。
     * <p>
     * Agent tools 内部直接调用此方法获取用户上下文，无需每次请求 new 工具实例。
     *
     * @return 当前用户 ID，未设置时返回 null
     */
    public static String getUserId() {
        return USER_ID.get();
    }

    /**
     * 清理当前线程绑定的 userId。
     * <p>
     * 由 {@link AgentContextAspect} 在请求完成后调用，防止线程池复用导致上下文泄漏。
     */
    public static void clear() {
        USER_ID.remove();
    }
}
