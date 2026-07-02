package com.snaplearn.controller;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.snaplearn.entity.ChatConversation;
import com.snaplearn.mapper.ChatConversationMapper;
import com.snaplearn.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatConversationController {

    private final ChatConversationMapper conversationMapper;
    private final ChatMemory chatMemory;
    private final BaseCheckpointSaver saver;

    /**
     * 获取当前用户的会话列表，按创建时间倒序。
     */
    @GetMapping("/conversations")
    public List<Map<String, Object>> listConversations(@CurrentUser String userId) {
        QueryWrapper<ChatConversation> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).orderByDesc("created_at");
        List<ChatConversation> list = conversationMapper.selectList(qw);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatConversation conv : list) {
            result.add(Map.of(
                    "id", conv.getId(),
                    "chat_id", conv.getChatId(),
                    "title", conv.getTitle() != null ? conv.getTitle() : "新对话",
                    "created_at", conv.getCreatedAt() != null ? conv.getCreatedAt().toString() : ""
            ));
        }
        return result;
    }

    /**
     * 创建新会话，返回 chatId。
     */
    @PostMapping("/conversations")
    public Map<String, Object> createConversation(@CurrentUser String userId) {
        String chatId = UUID.randomUUID().toString();
        ChatConversation conv = new ChatConversation();
        conv.setId(UUID.randomUUID().toString());
        conv.setUserId(userId);
        conv.setChatId(chatId);
        conv.setTitle("新对话");
        conversationMapper.insert(conv);
        return Map.of("chat_id", chatId, "id", conv.getId());
    }

    /**
     * 删除会话，同时清除 ChatMemory 中的历史记录。
     */
    @DeleteMapping("/conversations/{chatId}")
    public Map<String, Object> deleteConversation(@PathVariable String chatId, @CurrentUser String userId) {
        QueryWrapper<ChatConversation> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("chat_id", chatId);
        conversationMapper.delete(qw);
        chatMemory.clear(chatId);
        return Map.of("ok", true);
    }

    /**
     * 获取会话历史消息。mode=chat 从 Spring AI ChatMemory 读取，mode=agent 从 PostgresSaver checkpoint 读取。
     */
    @GetMapping("/messages/{chatId}")
    public List<Map<String, Object>> getMessages(@PathVariable String chatId, @RequestParam(defaultValue = "chat") String mode) {
        if ("agent".equals(mode)) {
            return getAgentMessages(chatId);
        }
        // Chat 模式：从 SPRING_AI_CHAT_MEMORY 读
        var messages = chatMemory.get(chatId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (var msg : messages) {
            result.add(Map.of(
                    "role", msg.getMessageType().name().toLowerCase(),
                    "content", msg.getText()
            ));
        }
        return result;
    }

    /**
     * 通过 PostgresSaver 读取 Agent 消息。
     * saver.get() 使用正确的 StateSerializer 反序列化 state。
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAgentMessages(String chatId) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            var config = RunnableConfig.builder().threadId(chatId).build();
            var checkpoint = saver.get(config);
            if (checkpoint.isEmpty()) return result;

            var state = checkpoint.get().getState();
            if (state == null) return result;

            // state 里的 messages 是 Spring AI Message 对象（UserMessage / AssistantMessage）
            Object msgsObj = state.get("messages");
            if (msgsObj instanceof List<?> list) {
                for (Object m : list) {
                    String content = null;
                    String role = null;
                    if (m instanceof org.springframework.ai.chat.messages.Message msg) {
                        content = msg.getText();
                        role = msg.getMessageType().name().toLowerCase();
                    } else {
                        // fallback: toString 解析
                        String s = m.toString();
                        if (s.contains("UserMessage")) {
                            role = "user";
                            content = s.replaceAll(".*content='([^']*)'.*", "$1");
                        } else if (s.contains("AssistantMessage")) {
                            role = "assistant";
                            content = s.replaceAll(".*content='([^']*)'.*", "$1");
                        }
                    }
                    if (content != null && !content.isBlank()) {
                        // 去掉 buildUserMessage 中拼接的前缀
                        if ("user".equals(role)) {
                            content = content.replace("[候选词] ", "").replace("[原文] ", "").replace("[用户消息] ", "");
                        }
                        result.add(Map.of("role", role, "content", content));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[AGENT-MSG] decode failed", e);
        }
        return result;
    }
}
