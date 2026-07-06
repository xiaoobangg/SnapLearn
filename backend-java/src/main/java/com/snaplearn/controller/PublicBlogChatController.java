package com.snaplearn.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicBlogChatController {

    @Qualifier("blogChatClient")
    private final ChatClient blogChatClient;

    private final VectorStore vectorStore;

    /** 博客 AI 对话（SSE 流式，无需登录，向量检索 + 内存会话记忆） */
    @PostMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<ServerSentEvent<String>> chatStream(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        String chatId = body.getOrDefault("chat_id", UUID.randomUUID().toString());
        if (message.isBlank()) {
            return Flux.just(ServerSentEvent.builder("请提供问题").build());
        }

        return blogChatClient.prompt()
                .system("你是拍立学博客的AI助手。请根据检索到的博客文章内容回答用户问题，回答时引用文章信息。如果检索结果中没有相关信息，如实告知用户。使用中文回答。")
                .advisors(a -> a.param("chat_memory_conversation_id", chatId))
                .advisors(blogRagAdvisor())
                .user(message)
                .stream().content()
                .map(chunk -> ServerSentEvent.builder(chunk).build());
    }

    /** 构建博客 RAG advisor：只检索 published + shared 的文档 */
    private RetrievalAugmentationAdvisor blogRagAdvisor() {
        var filter = new FilterExpressionBuilder()
                .and(
                        new FilterExpressionBuilder().eq("status", "published"),
                        new FilterExpressionBuilder().eq("visibility", "shared"))
                .build();
        var retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.3)
                .topK(5)
                .filterExpression(filter)
                .build();
        var queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .build();
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryAugmenter(queryAugmenter)
                .build();
    }
}
