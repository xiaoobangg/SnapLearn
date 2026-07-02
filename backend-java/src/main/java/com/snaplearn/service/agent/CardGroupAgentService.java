package com.snaplearn.service.agent;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.snaplearn.dto.request.AgentChatRequest;
import com.snaplearn.dto.response.AgentChatResponse;
import com.snaplearn.entity.ChatTrace;
import com.snaplearn.service.ChatTraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 编排 ReactAgent 创建卡片组。
 * <p>
 * 创建 / 撤销等业务操作由 Agent Tools（createCardGroup / deleteLastCardGroup）执行，
 * 本 Service 只负责按请求模型选择 Agent 并调用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardGroupAgentService {

    private final ReactAgent deepSeekCardGroupAgent;
    private final ReactAgent dashScopeCardGroupAgent;
    private final ChatTraceService chatTraceService;

    /**
     * 同步执行 ReactAgent 对话。
     * <p>
     * 根据请求中的 model 字段选择 DeepSeek 或 DashScope Agent，构建包含候选词上下文的用户消息，
     * 调用 Agent 的 call 方法执行工具调用流程（如创建卡片组），并记录 trace。
     *
     * @param req Agent 对话请求，包含 message、model、chatId、agentContext 等
     * @return Agent 响应，包含 reply 文本、trace 步骤、pendingAction（如有）
     */
    public AgentChatResponse runReactAgent(AgentChatRequest req) {
        ReactAgent agent = "dashscope".equalsIgnoreCase(req.model())
                ? dashScopeCardGroupAgent
                : deepSeekCardGroupAgent;

        String userMsg = buildUserMessage(req);
        RunnableConfig config = RunnableConfig.builder()
                .threadId(req.chatId())
                .addMetadata("userId", AgentContext.getUserId())
                .build();

        long start = System.currentTimeMillis();
        try {
            log.info("[AGENT] userId={} chatId={} model={} input.len={}",
                    AgentContext.getUserId(), req.chatId(), req.model(), userMsg.length());
            AssistantMessage resp = agent.call(userMsg, config);

            String reply = resp != null && resp.getText() != null ? resp.getText() : "(无响应)";
            recordTrace(req, reply, System.currentTimeMillis() - start, null);
            List<AgentChatResponse.AgentStep> trace = List.of(
                    new AgentChatResponse.AgentStep(1, "agent_call", "ReactAgent", null, null, "已生成回复")
            );
            return new AgentChatResponse(reply, trace, null);
        } catch (GraphRunnerException e) {
            log.error("[AGENT] failed userId={} chatId={}", AgentContext.getUserId(), req.chatId(), e);
            recordTrace(req, null, System.currentTimeMillis() - start, e);
            return new AgentChatResponse(
                    "AI 助手出了点问题：" + e.getMessage() + "。请重试或换种表达。",
                    List.of(new AgentChatResponse.AgentStep(1, "error", null, null, null, e.getMessage())),
                    null
            );
        } catch (Exception e) {
            log.error("[AGENT] unexpected error userId={} chatId={}", AgentContext.getUserId(), req.chatId(), e);
            recordTrace(req, null, System.currentTimeMillis() - start, e);
            return new AgentChatResponse(
                    "AI 助手暂时不可用，请稍后重试。",
                    List.of(new AgentChatResponse.AgentStep(1, "error", null, null, null, e.getMessage())),
                    null
            );
        }
    }

    /**
     * 流式 Agent 对话：使用 agent.streamMessages() 原生流式 API。
     */
    public Flux<ServerSentEvent<String>> runReactAgentStream(AgentChatRequest req) {
        ReactAgent agent = "dashscope".equalsIgnoreCase(req.model())
                ? dashScopeCardGroupAgent
                : deepSeekCardGroupAgent;

        String userMsg = buildUserMessage(req);
        final String uid = AgentContext.getUserId();
        RunnableConfig config = RunnableConfig.builder()
                .threadId(req.chatId())
                .addMetadata("userId", uid)
                .build();

        long start = System.currentTimeMillis();
        log.info("[AGENT-STREAM] userId={} chatId={} model={} input.len={}", uid, req.chatId(), req.model(), userMsg.length());

        try {
            return agent.streamMessages(userMsg, config)
                    .doOnNext(msg -> log.debug("[AGENT-STREAM] msg type={} text.len={}", msg.getMessageType(), msg.getText() != null ? msg.getText().length() : 0))
                    .filter(msg -> msg instanceof AssistantMessage && msg.getText() != null && !msg.getText().isBlank())
                    .map(msg -> ServerSentEvent.builder(msg.getText()).build())
                    .doOnComplete(() -> {
                        long duration = System.currentTimeMillis() - start;
                        log.info("[AGENT-STREAM] done userId={} chatId={} duration={}ms", uid, req.chatId(), duration);
                    })
                    .doOnError(e ->
                            log.error("[AGENT-STREAM] failed userId={} chatId={}", uid, req.chatId(), e)
                    );
        } catch (GraphRunnerException e) {
            log.error("[AGENT-STREAM] stream failed userId={} chatId={}", AgentContext.getUserId(), req.chatId(), e);
            return Flux.error(e);
        }
    }

    private void recordTrace(AgentChatRequest req, String reply, long durationMs, Throwable error) {
        try {
            ChatTrace t = new ChatTrace();
            t.setUserId(AgentContext.getUserId());
            t.setChatId(req.chatId());
            t.setModel((req.model() != null ? req.model() : "deepseek") + "-agent");
            t.setUserMessage(req.message());
            t.setResponseText(reply);
            t.setDurationMs(durationMs);
            if (error != null) {
                t.setStatus("error");
                t.setErrorMessage(error.getClass().getSimpleName() + ": " + error.getMessage());
            } else {
                t.setStatus("success");
            }
            chatTraceService.record(t);
        } catch (Exception e) {
            log.warn("[AGENT] 写 trace 失败", e);
        }
    }

    private String buildUserMessage(AgentChatRequest req) {
        StringBuilder sb = new StringBuilder();
        var ctx = req.agentContext();
        if (ctx != null && ctx.candidateWords() != null && !ctx.candidateWords().isEmpty()) {
            sb.append("[候选词] ").append(String.join(", ", ctx.candidateWords())).append("\n");
            if (ctx.ocrText() != null && !ctx.ocrText().isBlank()) {
                String snippet = ctx.ocrText().length() > 200 ? ctx.ocrText().substring(0, 200) + "..." : ctx.ocrText();
                sb.append("[原文] ").append(snippet).append("\n");
            }
        }
        sb.append("[用户消息] ").append(req.message());
        return sb.toString();
    }
}
