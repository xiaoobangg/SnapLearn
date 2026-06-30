package com.snaplearn.service.agent;

import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import org.springframework.ai.chat.model.ToolContext;

/**
 * 从 {@link ToolContext} 解析当前 userId 的统一入口。
 * <p>
 * 优先从 ToolContext metadata（key = {@value #USER_ID_KEY}）取，兼容两种注入路径：
 * <ul>
 *   <li>ChatClient：{@code .toolContext(Map.of("userId", uid))}（见 LLMService）</li>
 *   <li>ReactAgent：{@code RunnableConfig.addMetadata("userId", uid)}，框架扁平化进 context（见 CardGroupAgentService）</li>
 * </ul>
 * 两者都落到 ToolContext 的 context map 上，{@link ToolContextHelper#getMetadataOrDefault} 直接读取。
 * <p>
 * metadata 缺失时 fallback 到 {@link AgentContext} ThreadLocal（非 Agent 场景兜底）。
 */
public final class UserIdResolver {

    /** userId 在 ToolContext context map / RunnableConfig metadata 中的 key */
    public static final String USER_ID_KEY = "userId";

    private UserIdResolver() {
    }

    /**
     * 解析当前请求的 userId。
     *
     * @param ctx Spring AI 自动注入的 ToolContext，可为 null
     * @return userId；metadata 与 ThreadLocal 均缺失时返回 null
     */
    public static String resolve(ToolContext ctx) {
        String metaUserId = ToolContextHelper.getMetadataOrDefault(ctx, USER_ID_KEY, String.class, null);
        if (metaUserId != null) {
            return metaUserId;
        }
        return AgentContext.getUserId();
    }
}
