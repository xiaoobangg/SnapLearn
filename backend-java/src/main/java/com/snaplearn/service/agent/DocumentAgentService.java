package com.snaplearn.service.agent;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.snaplearn.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAgentService {

    private final ReactAgent deepSeekDocumentMasterAgent;
    private final ReactAgent dashScopeDocumentMasterAgent;

    /** 流式执行文档 Agent */
    public Flux<ServerSentEvent<String>> runStream(String message, String model,
                                                    String chatId, String userId) {
        ReactAgent agent = selectAgent(model);
        RunnableConfig config = RunnableConfig.builder()
                .threadId(chatId)
                .addMetadata("userId", userId)
                .build();
        try {
            return agent.streamMessages(message, config)
                    .filter(msg -> msg instanceof AssistantMessage && msg.getText() != null && !msg.getText().isBlank())
                    .map(msg -> ServerSentEvent.builder(msg.getText()).build())
                    .onErrorResume(e -> {
                        log.error("Document agent error", e);
                        return Flux.just(ServerSentEvent.builder("处理出错: " + e.getMessage()).build());
                    });
        } catch (GraphRunnerException e) {
            log.error("Document agent stream failed", e);
            return Flux.error(e);
        }
    }

    /** 同步执行文档 Agent */
    public String run(String message, String model, String chatId, String userId) {
        ReactAgent agent = selectAgent(model);
        RunnableConfig config = RunnableConfig.builder()
                .threadId(chatId)
                .addMetadata("userId", userId)
                .build();
        try {
            Message result = agent.call(message, config);
            if (result instanceof AssistantMessage am) {
                return am.getText();
            }
            return result.getText();
        } catch (Exception e) {
            log.error("Document agent sync error", e);
            throw new BusinessException(502, "文档 Agent 执行失败: " + e.getMessage());
        }
    }

    private ReactAgent selectAgent(String model) {
        if ("dashscope".equalsIgnoreCase(model)) {
            return dashScopeDocumentMasterAgent;
        }
        return deepSeekDocumentMasterAgent;
    }
}
