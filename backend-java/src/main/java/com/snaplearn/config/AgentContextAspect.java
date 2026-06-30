package com.snaplearn.config;

import com.snaplearn.service.agent.AgentContext;
import com.snaplearn.service.agent.AgentScope;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 拦截 {@link AgentScope} 标注的 controller 方法，统一管理 {@link AgentContext} ThreadLocal 生命周期。
 * <p>
 * 从请求属性 {@code "userId"}（由 {@code JwtInterceptor.preHandle} 设置，{@code ApiLogAspect} 同样这么读）
 * 取 userId 并注入 AgentContext，方法返回后在请求线程清理——替代 controller 里手写的
 * {@code setUserId}/{@code clear}/{@code doFinally}，并修掉 {@code doFinally} 跑在异步终止线程导致清理泄漏的问题。
 * <p>
 * 对返回 {@code Flux} 的方法：{@code proceed()} 同步执行完方法体（uid 已被同步捕获进 metadata）后返回 Flux，
 * {@code finally} 立即在请求线程清理，正确包裹同步体。
 */
@Slf4j
@Aspect
@Component
public class AgentContextAspect {

    @Around("@annotation(agentScope)")
    public Object around(ProceedingJoinPoint pjp, AgentScope agentScope) throws Throwable {
        String userId = resolveUserId();
        AgentContext.setUserId(userId);
        try {
            return pjp.proceed();
        } finally {
            AgentContext.clear();
        }
    }

    /**
     * 从当前请求属性取 userId（与 @CurrentUser 解析器同源）。
     * 无请求上下文（如异步线程）时返回 null。
     */
    private String resolveUserId() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object uid = attrs.getRequest().getAttribute("userId");
        return uid == null ? null : uid.toString();
    }
}
