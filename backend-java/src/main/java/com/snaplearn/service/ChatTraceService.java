package com.snaplearn.service;

import com.snaplearn.entity.ChatTrace;
import com.snaplearn.mapper.ChatTraceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 持久化 AI 对话 trace。
 * 关键约定：写入失败一律吞掉异常 + log.error，不能影响 chat 响应链路。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatTraceService {

    private final ChatTraceMapper chatTraceMapper;

    /**
     * 持久化一条 AI 对话 trace 记录到 snap_chat_traces 表。
     * <p>
     * 自动补全 id（UUID）和 createdAt（当前时间）；写入失败吞掉异常 + log.error，不影响 chat 响应链路。
     *
     * @param trace 待写入的 trace 对象（userId / chatId / model / userMessage 等字段由调用方填充）
     */
    public void record(ChatTrace trace) {
        try {
            if (trace.getId() == null) {
                trace.setId(UUID.randomUUID().toString());
            }
            if (trace.getCreatedAt() == null) {
                trace.setCreatedAt(LocalDateTime.now());
            }
            chatTraceMapper.insert(trace);
        } catch (Exception e) {
            log.error("Persist chat trace failed: userId={}, chatId={}", trace.getUserId(), trace.getChatId(), e);
        }
    }
}
