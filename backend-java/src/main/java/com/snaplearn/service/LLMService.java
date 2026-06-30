package com.snaplearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.service.advisor.ChatTraceLoggingAdvisor;
import com.snaplearn.service.agent.MemoryChatTools;
import com.snaplearn.util.PromptLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LLMService {

    /**
     * 向量召回相似度阈值
     */
    private static final double RAG_SIMILARITY_THRESHOLD = 0.1;
    /**
     * 单次召回 topK
     */
    private static final int RAG_TOP_K = 10;
    /**
     * 启用多轮压缩的最少轮数门槛（1 轮 = user + assistant 共 2 条消息）。
     * 历史消息条数 ≥ COMPRESSION_MIN_ROUNDS * 2 时，才走 LLM 改写省成本。
     */
    private static final int COMPRESSION_MIN_ROUNDS = 5;
    /**
     * MultiQueryExpander 生成的查询变体数（不含原始 query）。
     * 总向量检索次数 = 该值 + 1，且每次请求会触发 1 次额外的 LLM 调用做扩展。设大了显著吃 token。
     */
    private static final int RAG_QUERY_VARIANTS = 3;
    /**
     * RewriteQueryTransformer 中 {target} 占位符的替换值，用中文避免污染中文输出。
     */
    private static final String RAG_REWRITE_TARGET = "向量数据库";

    private final ObjectMapper objectMapper;
    private final ChatClient deepSeekChatClient;
    private final ChatClient dashScopeChatClient;
    private final PromptLoader promptLoader;
    private final ChatModel deepSeekChatModel;
    private final ChatModel dashScopeChatModel;
    private final VectorStore vectorStore;
    private final ChatTraceService chatTraceService;
    private final MemoryChatTools memoryChatTools;

    /**
     * 与 userId 无关的 RAG 组件，构造期一次性建好——避免每次请求重建 ChatClient / 重新解析 PromptTemplate
     */
    private final QueryTransformer conditionalCompression;
    private final RewriteQueryTransformer rewriteQueryTransformer;
    private final MultiQueryExpander multiQueryExpander;
    private final ContextualQueryAugmenter contextualQueryAugmenter;

    public LLMService(ObjectMapper objectMapper, ChatClient deepSeekChatClient, ChatClient dashScopeChatClient, PromptLoader promptLoader, ChatModel deepSeekChatModel, ChatModel dashScopeChatModel, VectorStore vectorStore, ChatTraceService chatTraceService, MemoryChatTools memoryChatTools) {
        this.objectMapper = objectMapper;
        this.deepSeekChatClient = deepSeekChatClient;
        this.dashScopeChatClient = dashScopeChatClient;
        this.promptLoader = promptLoader;
        this.deepSeekChatModel = deepSeekChatModel;
        this.dashScopeChatModel = dashScopeChatModel;
        this.vectorStore = vectorStore;
        this.chatTraceService = chatTraceService;
        this.memoryChatTools = memoryChatTools;

        // 一次性构建 RAG 流水线里所有"用户无关"的组件，请求路径只重建 retriever
        this.conditionalCompression = onlyWhenMultiTurn(buildCompressionTransformer(), COMPRESSION_MIN_ROUNDS);
        this.rewriteQueryTransformer = buildRewriteTransformer();
        this.multiQueryExpander = buildMultiQueryExpander();
        this.contextualQueryAugmenter = buildEmptyContextRejectingAugmenter();
    }

    public List<CardContent> generateBatchCardContent(List<String> words, String context) {
        if (words.isEmpty()) {
            return List.of();
        }

        StringBuilder wordList = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            wordList.append(i + 1).append(". ").append(words.get(i)).append("\n");
        }

        String template = promptLoader.load("generate-card-content.st");
        String userMessage = template
                .replace("{wordList}", wordList.toString())
                .replace("{context}", context != null && !context.isEmpty() ? context : "无特定上下文");

        try {
            String content = ChatClient.create(deepSeekChatModel).prompt()
                    .system(promptLoader.load("system-message.st"))
                    .user(userMessage)
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                throw new BusinessException(502, "LLM 返回为空");
            }

            String cleaned = cleanMarkdownJson(content);
            JsonNode arr = objectMapper.readTree(cleaned);
            List<CardContent> results = new ArrayList<>();
            for (int i = 0; i < arr.size() && i < words.size(); i++) {
                JsonNode item = arr.get(i);
                CardContent cc = new CardContent(
                        item.path("word").asText(words.get(i)),
                        item.path("general_meaning").asText(""),
                        item.path("extended_meaning").asText(""),
                        item.path("example_sentence").asText(""),
                        item.path("memory_tip").asText(""),
                        item.path("pos").asText(""),
                        item.path("pronunciation").asText("")
                );
                results.add(cc);
            }
            return results;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("DeepSeek LLM 调用失败", e);
            throw new BusinessException(502, "LLM 服务异常");
        }
    }

    private String cleanMarkdownJson(String raw) {
        String s = raw.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    private ChatClient selectClient(String model) {
        return "dashscope".equalsIgnoreCase(model) ? dashScopeChatClient : deepSeekChatClient;
    }

    private ChatModel selectModel(String model) {
        return "dashscope".equalsIgnoreCase(model) ? dashScopeChatModel : deepSeekChatModel;
    }

    /**
     * 临时对话：直接用 ChatModel，不走 tools / RAG / memory / trace。
     * 适用场景：OCR 单词提取、简单文本处理等一次性 LLM 调用。
     */
    public String chatSimple(String message, String model) {
        try {
            String content = ChatClient.builder(selectModel(model)).build().prompt()
                    .user(message)
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                throw new BusinessException(502, "LLM 返回为空");
            }
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("临时对话失败 model={}", model, e);
            throw new BusinessException(502, "LLM 服务异常");
        }
    }

    public Flux<String> chatStream(String message, String model, String chatId, String userId) {
        return selectClient(model).prompt()
                .tools(memoryChatTools)
                .toolContext(Map.of("userId", userId))
                .advisors(a -> a.param("chat_memory_conversation_id", chatId))
                .advisors(new ChatTraceLoggingAdvisor(userId, chatId, model, chatTraceService))
                .advisors(getRetrievalAugmentationAdvisor(userId))
                .user(message)
                .stream()
                .content();
    }

    public String chatStr(String message, String model, String chatId, String userId) {
        return selectClient(model).prompt()
                .tools(memoryChatTools)
                .toolContext(Map.of("userId", userId))
                .advisors(a -> a.param("chat_memory_conversation_id", chatId))
                .advisors(new ChatTraceLoggingAdvisor(userId, chatId, model, chatTraceService))
                .advisors(getRetrievalAugmentationAdvisor(userId))
                .user(message)
                .call()
                .content();
    }

    /**
     * 构建按用户隔离的 RAG advisor。
     * 流水线（与 userId 无关的组件在构造期建好，请求路径只重建 retriever）：
     * 1. CompressionQueryTransformer  —— 多轮上下文压缩为独立问题（仅 ≥ {@link #COMPRESSION_MIN_ROUNDS} 轮才生效，中文 prompt）
     * 2. RewriteQueryTransformer      —— 清洗冗余客套，精简成检索友好的查询（中文 prompt，多调一次 LLM）
     * 3. MultiQueryExpander           —— 把 query 扩展成 N+1 个语义变体并发检索，提升召回（中文 prompt，多调一次 LLM）
     * 4. VectorStoreDocumentRetriever —— 按 user_id 过滤的向量召回
     * 5. ContextualQueryAugmenter     —— 空召回拒答，避免幻觉
     */
    private RetrievalAugmentationAdvisor getRetrievalAugmentationAdvisor(String userId) {
        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(conditionalCompression, rewriteQueryTransformer)
                .queryExpander(multiQueryExpander)
                .documentRetriever(loggingRetriever(buildUserRetriever(userId), userId))
                .queryAugmenter(contextualQueryAugmenter)
                .build();
    }

    /**
     * 多轮上下文压缩：把"它/这个"消歧成独立问题。用 DeepSeek 跑（便宜 + 快），与最终回答模型解耦。
     */
    private CompressionQueryTransformer buildCompressionTransformer() {
        return CompressionQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(deepSeekChatModel))
                .promptTemplate(loadTemplate("query-compression.st"))
                .build();
    }

    /**
     * 查询重写：清洗冗余、精简成检索友好的查询。{target} 也用中文，避免污染中文输出。
     */
    private RewriteQueryTransformer buildRewriteTransformer() {
        return RewriteQueryTransformer.builder()
                .chatClientBuilder(ChatClient.builder(deepSeekChatModel))
                .promptTemplate(loadTemplate("query-rewrite.st"))
                .targetSearchSystem(RAG_REWRITE_TARGET)
                .build();
    }

    /**
     * 多查询扩展：为每个 query 生成 N 个语义变体并发检索，提升召回。
     */
    private MultiQueryExpander buildMultiQueryExpander() {
        return MultiQueryExpander.builder()
                .chatClientBuilder(ChatClient.builder(deepSeekChatModel))
                .promptTemplate(loadTemplate("query-expansion.st"))
                .numberOfQueries(RAG_QUERY_VARIANTS)
                .includeOriginal(true)
                .build();
    }

    /**
     * 空召回时显式告知模型"无相关资料"，避免幻觉。
     */
    private ContextualQueryAugmenter buildEmptyContextRejectingAugmenter() {
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();
    }

    /**
     * 按 user_id 过滤的向量召回检索器。userId 是请求路径上唯一的变量，故每次新建。
     */
    private VectorStoreDocumentRetriever buildUserRetriever(String userId) {
        var filter = new FilterExpressionBuilder().eq("user_id", userId).build();
        return VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(RAG_SIMILARITY_THRESHOLD)
                .topK(RAG_TOP_K)
                .filterExpression(filter)
                .build();
    }

    /**
     * 从 classpath:prompts/ 加载模板并包装为 Spring AI {@link PromptTemplate}（{name} 占位符语法）。
     */
    private PromptTemplate loadTemplate(String name) {
        return new PromptTemplate(promptLoader.load(name));
    }

    /**
     * 把 {@link QueryTransformer} 包成"仅多轮才生效"：history 消息数 < minRounds * 2 时直接返回原 query，
     * 不触发底层 LLM 调用。
     */
    private static QueryTransformer onlyWhenMultiTurn(QueryTransformer delegate, int minRounds) {
        int minMessages = minRounds * 2;
        return query -> {
            int historySize = query.history() == null ? 0 : query.history().size();
            log.info("[RAG-PROBE] QueryTransformer 入口 history={} threshold={} text='{}'",
                    historySize, minMessages, query.text());
            if (historySize < minMessages) {
                return query;
            }
            log.info("[RAG-PROBE] 触发 query 压缩");
            return delegate.transform(query);
        };
    }

    /**
     * 包一层日志：打印实际送进向量库的 query / 召回数 / 每条文档的相似度分数 + 元数据 + 内容预览。
     * 用于排查"AI 没有用知识库"——大概率是召回 0 条，或召回到的相似度过低被阈值卡掉。
     */
    private static DocumentRetriever loggingRetriever(DocumentRetriever delegate, String userId) {
        return query -> {
            log.info("[RAG-PROBE] DocumentRetriever 入口 userId={} query='{}'", userId, query.text());
            List<Document> docs = delegate.retrieve(query);
            log.info("[RAG-PROBE] DocumentRetriever 出口 召回 {} 条文档", docs.size());
            for (int i = 0; i < docs.size(); i++) {
                Document d = docs.get(i);
                Object score = d.getMetadata() == null ? null : d.getMetadata().get("distance");
                log.info("[RAG-PROBE]   #{} score={} meta={} text={}",
                        i, score, d.getMetadata(), abbreviate(d.getText(), 200));
            }
            return docs;
        };
    }

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        s = s.replaceAll("\\s+", " ");
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    public record CardContent(
            String word,
            String generalMeaning,
            String extendedMeaning,
            String exampleSentence,
            String memoryTip,
            String pos,
            String pronunciation
    ) {
    }
}