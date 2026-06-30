package com.snaplearn.service.advisor;

import com.snaplearn.entity.ChatTrace;
import com.snaplearn.service.ChatTraceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

/**
 * 把每次 chat 请求的核心数据（用户消息 / 响应 / token / 耗时）持久化到 snap_chat_traces 表。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>order = HIGHEST_PRECEDENCE：最外层 advisor，能拿到最原始的用户消息和最终响应</li>
 *   <li>每次 chat 请求新建一个实例（不是单例），构造时携带 userId / chatId / model</li>
 *   <li>stream 路径 doOnNext 拼接所有 chunk，doOnComplete / doOnError 时触发写入</li>
 *   <li>写入失败由 ChatTraceService 内部吞掉，不影响 chat 响应</li>
 * </ul>
 */
@Slf4j
public class ChatTraceLoggingAdvisor implements CallAdvisor, StreamAdvisor {

    private final String userId;
    private final String chatId;
    private final String model;
    private final ChatTraceService chatTraceService;

    public ChatTraceLoggingAdvisor(String userId, String chatId, String model, ChatTraceService chatTraceService) {
        this.userId = userId;
        this.chatId = chatId;
        this.model = model;
        this.chatTraceService = chatTraceService;
    }

    @Override
    public String getName() {
        return "ChatTraceLoggingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        long start = System.currentTimeMillis();
        String userMessage = extractUserMessage(chatClientRequest);
        try {
            ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
            persistSuccess(userMessage, extractAssistantText(response), extractUsage(response),
                    System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            persistError(userMessage, e, System.currentTimeMillis() - start);
            throw e;
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {
        long start = System.currentTimeMillis();
        String userMessage = extractUserMessage(chatClientRequest);
        StringBuilder responseBuffer = new StringBuilder();
        Usage[] lastUsage = new Usage[1];

        return streamAdvisorChain.nextStream(chatClientRequest)
                .doOnNext(chunk -> {
                    String text = extractAssistantText(chunk);
                    if (text != null) {
                        responseBuffer.append(text);
                    }
                    Usage usage = extractUsage(chunk);
                    if (usage != null) {
                        // 流式响应里 token usage 通常只在最后一帧带上，覆盖更新即可
                        lastUsage[0] = usage;
                    }
                })
                .doOnComplete(() -> persistSuccess(userMessage, responseBuffer.toString(),
                        lastUsage[0], System.currentTimeMillis() - start))
                .doOnError(e -> persistError(userMessage, e, System.currentTimeMillis() - start));
    }

    private void persistSuccess(String userMessage, String responseText, Usage usage, long duration) {
        ChatTrace trace = baseTrace(userMessage, duration);
        trace.setStatus("success");
        trace.setResponseText(responseText);
        if (usage != null) {
            trace.setPromptTokens(toInt(usage.getPromptTokens()));
            trace.setCompletionTokens(toInt(usage.getCompletionTokens()));
            trace.setTotalTokens(toInt(usage.getTotalTokens()));
        }
        chatTraceService.record(trace);
    }

    private void persistError(String userMessage, Throwable error, long duration) {
        ChatTrace trace = baseTrace(userMessage, duration);
        trace.setStatus("error");
        trace.setErrorMessage(abbr(error.getClass().getSimpleName() + ": " + error.getMessage(), 1000));
        chatTraceService.record(trace);
    }

    private ChatTrace baseTrace(String userMessage, long duration) {
        ChatTrace trace = new ChatTrace();
        trace.setUserId(userId);
        trace.setChatId(chatId);
        trace.setModel(model);
        trace.setUserMessage(userMessage);
        trace.setDurationMs(duration);
        return trace;
    }

    /**
     * 从 request 里抽用户最新一条 USER 消息文本
     */
    private static String extractUserMessage(ChatClientRequest request) {
        try {
            return request.prompt().getInstructions().stream()
                    .filter(m -> m.getMessageType() == MessageType.USER)
                    .reduce((a, b) -> b)            // 拿最后一条 USER（前面可能是历史）
                    .map(m -> m.getText())
                    .orElse("");
        } catch (Exception e) {
            log.debug("extract user message failed", e);
            return "";
        }
    }

    /**
     * 从 response 抽 assistant 文本（同步路径是完整文本，stream 路径是单次 chunk）
     */
    private static String extractAssistantText(ChatClientResponse response) {
        try {
            ChatResponse cr = response.chatResponse();
            if (cr == null || cr.getResult() == null) {
                return null;
            }
            Generation gen = cr.getResult();
            AssistantMessage msg = gen.getOutput();
            return msg == null ? null : msg.getText();
        } catch (Exception e) {
            return null;
        }
    }

    private static Usage extractUsage(ChatClientResponse response) {
        try {
            ChatResponse cr = response.chatResponse();
            if (cr == null || cr.getMetadata() == null) {
                return null;
            }
            return cr.getMetadata().getUsage();
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer toInt(Integer v) {
        return v == null ? null : v;
    }

    private static String abbr(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
