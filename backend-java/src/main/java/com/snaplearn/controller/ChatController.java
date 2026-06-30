package com.snaplearn.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snaplearn.dto.request.AgentChatRequest;
import com.snaplearn.dto.response.AgentChatResponse;
import com.snaplearn.entity.ChatConversation;
import com.snaplearn.mapper.ChatConversationMapper;
import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.LLMService;
import com.snaplearn.service.agent.AgentScope;
import com.snaplearn.service.agent.CardGroupAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final LLMService llmService;
    private final ChatConversationMapper conversationMapper;
    private final CardGroupAgentService cardGroupAgentService;
    private final ObjectMapper objectMapper;

    /**
     * /api/v1/chat/stream —— SSE 流式对话，兼容 chat 和 agent 两种模式。
     * <p>
     * AgentContext 生命周期由 {@link AgentScope} + AgentContextAspect 统一管理。
     */
    @PostMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    @AgentScope
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody Map<String, Object> body, @CurrentUser String userId) {
        String message = stringOrEmpty(body.get("message"));
        String model = stringOrDefault(body.get("model"), "deepseek");
        String chatId = stringOrDefault(body.get("chat_id"), UUID.randomUUID().toString());
        String mode = stringOrDefault(body.get("mode"), "chat");
        ensureConversation(userId, chatId, message);

        if ("agent".equalsIgnoreCase(mode)) {
            // Agent 模式：ReactAgent 同步执行，返回 SSE 格式
            Map<String, Object> agentReq = new java.util.LinkedHashMap<>();
            agentReq.put("message", message);
            agentReq.put("model", model);
            agentReq.put("chat_id", chatId);
            agentReq.put("mode", mode);
            if (body.containsKey("agent_context") && body.get("agent_context") != null) {
                agentReq.put("agentContext", body.get("agent_context"));
            }
            if (body.containsKey("pending_action") && body.get("pending_action") != null) {
                agentReq.put("pendingAction", body.get("pending_action"));
            }
            AgentChatRequest req = objectMapper.convertValue(agentReq, AgentChatRequest.class);
            log.info("[CHAT] agent mode stream userId={} chatId={}", userId, chatId);
            return cardGroupAgentService.runReactAgentStream(req);
        }

        // Chat 模式：LLM 流式
        return llmService.chatStream(message, model, chatId, userId)
                .map(content -> ServerSentEvent.builder(content).build());
    }

    /**
     * /api/v1/chat/call —— 同步对话。
     * <p>
     * 兼容两种模式：
     * <ul>
     *   <li>{@code mode} 缺省或 "chat" → 走原有 RAG chat 流程，返回纯文本</li>
     *   <li>{@code mode="agent"} → 走 ReactAgent，返回结构化 {@link AgentChatResponse}</li>
     * </ul>
     */
    @PostMapping(value = "/call")
    @AgentScope
    public Object strChat(@RequestBody Map<String, Object> body, @CurrentUser String userId) {
        String message = stringOrEmpty(body.get("message"));
        String model = stringOrDefault(body.get("model"), "deepseek");
        String chatId = stringOrDefault(body.get("chat_id"), UUID.randomUUID().toString());
        String mode = stringOrDefault(body.get("mode"), "chat");
        ensureConversation(userId, chatId, message);

        if ("agent".equalsIgnoreCase(mode)) {
            Map<String, Object> agentReq = new java.util.LinkedHashMap<>();
            agentReq.put("message", message);
            agentReq.put("model", model);
            agentReq.put("chat_id", chatId);
            agentReq.put("mode", mode);
            if (body.containsKey("agent_context") && body.get("agent_context") != null) {
                agentReq.put("agentContext", body.get("agent_context"));
            }
            if (body.containsKey("pending_action") && body.get("pending_action") != null) {
                agentReq.put("pendingAction", body.get("pending_action"));
            }
            AgentChatRequest req = objectMapper.convertValue(agentReq, AgentChatRequest.class);
            log.info("[CHAT] agent mode userId={} chatId={}", userId, chatId);
            return cardGroupAgentService.runReactAgent(req);
        }
        // 默认 chat 模式
        return llmService.chatStr(message, model, chatId, userId);
    }

    private void ensureConversation(String userId, String chatId, String message) {
        QueryWrapper<ChatConversation> qw = new QueryWrapper<>();
        qw.eq("user_id", userId).eq("chat_id", chatId);
        if (conversationMapper.selectCount(qw) > 0) {
            return;
        }
        ChatConversation conv = new ChatConversation();
        conv.setId(UUID.randomUUID().toString());
        conv.setUserId(userId);
        conv.setChatId(chatId);
        conv.setTitle(message.length() > 30 ? message.substring(0, 30) : message);
        conversationMapper.insert(conv);
    }

    private static String stringOrEmpty(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static String stringOrDefault(Object v, String def) {
        return v == null || String.valueOf(v).isBlank() ? def : String.valueOf(v);
    }
}
