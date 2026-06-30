package com.snaplearn.service.agent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注在需要 {@link AgentContext} 生命周期的 controller 方法上。
 * <p>
 * 由 {@code AgentContextAspect}（@Around）拦截：
 * <ol>
 *   <li>方法执行前从请求属性 {@code "userId"} 取值并 {@link AgentContext#setUserId}</li>
 *   <li>方法返回后在请求线程 {@link AgentContext#clear}（同步清理，避免跨线程泄漏）</li>
 * </ol>
 * 对返回 {@code Flux} 的方法：{@code proceed()} 同步执行完方法体（含 uid 捕获进 metadata）后返回 Flux，
 * {@code finally} 立即清理，正确包裹同步体。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentScope {
}
