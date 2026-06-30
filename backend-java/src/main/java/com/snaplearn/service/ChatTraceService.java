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
