package com.snaplearn.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.postgresql.PostgresSaver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.snaplearn.service.agent.CardGroupAgentTools;
import com.snaplearn.service.agent.MemoryChatTools;
import com.snaplearn.util.PromptLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

@Configuration
public class ChatClientConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;

    @Value("${spring.ai.dashscope.base-url}")
    private String dashscopeBaseUrl;

    /*@Bean
    @ConfigurationProperties(prefix = "spring.ai.chat-memory.datasource")
    public DataSourceProperties aiMemoryDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource aiMemoryDataSource(DataSourceProperties aiMemoryDataSourceProperties) {
        return aiMemoryDataSourceProperties
                .initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean("aiMemoryJdbcTemplate")
    public JdbcTemplate aiMemoryJdbcTemplate(
            @Qualifier("aiMemoryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }*/

    /**
     * 会话记忆持久化仓库，基于 JDBC（PostgreSQL）。
     * <p>
     * Spring AI 框架自动管理 spring_ai_chat_memory 表的创建和读写，
     * 存储每轮对话的 user/assistant 消息，供 MessageChatMemoryAdvisor 加载历史上下文。
     *
     * @param jdbcTemplate 业务库 JdbcTemplate（复用主数据源）
     * @return JDBC 记忆仓库实例
     */
    @Bean
    public ChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate) {
        return JdbcChatMemoryRepository.builder().jdbcTemplate(jdbcTemplate).build();
    }

    /**
     * 会话记忆管理器，采用滑动窗口策略保留最近 20 条消息。
     * <p>
     * 超过窗口大小的历史消息会被自动丢弃，避免上下文过长导致 token 消耗过高。
     *
     * @param repository 记忆持久化仓库
     * @return 滑动窗口记忆管理器实例
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder().chatMemoryRepository(repository).maxMessages(20).build();
    }

    // spring 框架通过配置进行注入
    /*@Bean
    public EmbeddingModel embeddingModelV4() {
        DashScopeApi scopeApi = DashScopeApi.builder().apiKey(dashscopeApiKey).baseUrl(dashscopeBaseUrl).build();
        return DashScopeEmbeddingModel.builder().dashScopeApi(scopeApi).defaultOptions(DashScopeEmbeddingOptions.builder().model("text-embedding-v4").dimensions(1024).build()).build();
    }

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel).build();
    }*/

    /**
     * DeepSeek ChatClient 单例，默认挂载会话记忆 advisor。
     * <p>
     * 通过 MessageChatMemoryAdvisor 自动加载/保存 chatId 对应的历史消息，
     * 实现多轮对话上下文记忆。适用于不需要 RAG 的普通对话场景。
     *
     * @param chatModel  DeepSeek ChatModel（由 Spring AI 自动注入）
     * @param chatMemory 会话记忆管理器
     * @return 配置好记忆 advisor 的 ChatClient
     */
    @Bean
    public ChatClient deepSeekChatClient(DeepSeekChatModel chatModel, ChatMemory chatMemory) {
        MessageChatMemoryAdvisor chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        return ChatClient.builder(chatModel)
                .defaultAdvisors(chatMemoryAdvisor)
                .build();
    }

    /**
     * DashScope ChatClient 单例，默认挂载会话记忆 advisor。
     * <p>
     * 功能同 {@link #deepSeekChatClient}，但使用通义千问模型。适用于不需要 RAG 的普通对话场景。
     *
     * @param chatModel  DashScope ChatModel（由 Spring AI 自动注入）
     * @param chatMemory 会话记忆管理器
     * @return 配置好记忆 advisor 的 ChatClient
     */
    @Bean
    public ChatClient dashScopeChatClient(DashScopeChatModel chatModel, ChatMemory chatMemory) {
        MessageChatMemoryAdvisor chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        return ChatClient.builder(chatModel)
                .defaultAdvisors(chatMemoryAdvisor)
                .build();
    }

    /**
     * DeepSeek ChatClient（带 RAG），挂载会话记忆 + 知识库检索 advisor。
     * <p>
     * 通过 RetrievalAugmentationAdvisor 从向量库召回相关文档，增强 LLM 回答的准确性。
     * 注意：此 Bean 未做用户级文档隔离，如需按 userId 过滤，请在 service 层动态构建 advisor。
     *
     * @param chatModel   DeepSeek ChatModel
     * @param chatMemory  会话记忆管理器
     * @param vectorStore 向量库（用于 RAG 检索）
     * @return 配置好记忆 + RAG advisor 的 ChatClient
     */
    @Bean
    public ChatClient deepSeekChatRagClient(DeepSeekChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore) {
        MessageChatMemoryAdvisor chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        RetrievalAugmentationAdvisor ragAdvisor = buildRagAdvisor(vectorStore);
        return ChatClient.builder(chatModel)
                .defaultAdvisors(chatMemoryAdvisor, ragAdvisor)
                .build();
    }

    /**
     * DashScope ChatClient（带 RAG），挂载会话记忆 + 知识库检索 advisor。
     * <p>
     * 功能同 {@link #deepSeekChatRagClient}，但使用通义千问模型。
     * 注意：此 Bean 未做用户级文档隔离，如需按 userId 过滤，请在 service 层动态构建 advisor。
     *
     * @param chatModel   DashScope ChatModel
     * @param chatMemory  会话记忆管理器
     * @param vectorStore 向量库（用于 RAG 检索）
     * @return 配置好记忆 + RAG advisor 的 ChatClient
     */
    @Bean
    public ChatClient dashScopeChatRagClient(DashScopeChatModel chatModel, ChatMemory chatMemory, VectorStore vectorStore) {
        MessageChatMemoryAdvisor chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        RetrievalAugmentationAdvisor ragAdvisor = buildRagAdvisor(vectorStore);
        return ChatClient.builder(chatModel)
                .defaultAdvisors(chatMemoryAdvisor, ragAdvisor)
                .build();
    }

    /**
     * 构建一个 Bean 级 RAG advisor（不带用户过滤）。
     * 如需按用户隔离文档，请在 service 层按请求构建 advisor，参考 LLMService#getRetrievalAugmentationAdvisor。
     */
    private RetrievalAugmentationAdvisor buildRagAdvisor(VectorStore vectorStore) {
        var retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.5)
                .topK(10)
                .build();
        var queryAugmenter = ContextualQueryAugmenter.builder()
                .allowEmptyContext(true)
                .build();
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryAugmenter(queryAugmenter)
                .build();
    }

    /**
     * 自定义 DeepSeekApi 实例，关闭思考模式以降低成本。
     * <p>
     * 通过请求拦截器自动注入 {@code thinking: disabled} 参数（如果请求体中未显式设置），
     * 并配置连接超时 20s、读取超时 120s（适配大模型慢响应）。
     *
     * @param apiKey            DeepSeek API Key
     * @param baseUrl           DeepSeek API Base URL
     * @param restClientBuilder RestClient 构建器（Spring 自动注入）
     * @param objectMapper      JSON 序列化工具
     * @return 配置好拦截器和超时的 DeepSeekApi 实例
     */
    @Bean
    @Primary
    public DeepSeekApi deepSeekApi(
            @Value("${spring.ai.deepseek.api-key}") String apiKey,
            @Value("${spring.ai.deepseek.base-url}") String baseUrl,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {

        // 1. 设置超时时间
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(20_000); // 连接超时 10 秒
        requestFactory.setReadTimeout(120_000);   // 读取超时 120 秒 (大模型生成可能较慢)

        // 在原有的 builder 基础上添加拦截器
        RestClient.Builder builder = restClientBuilder.requestInterceptor((request, body, execution) -> {
            if (body != null && body.length > 0) {
                JsonNode root = objectMapper.readTree(body);

                // 确保 root 是一个可以修改的 ObjectNode
                if (root instanceof ObjectNode objectNode) {
                    // 如果请求体中没有显式设置 thinking 参数，则默认注入关闭思考的参数
                    if (!objectNode.has("thinking")) {
                        ObjectNode thinkingNode = objectMapper.createObjectNode();
                        thinkingNode.put("type", "disabled");
                        objectNode.set("thinking", thinkingNode);

                        // 将修改后的 JSON 写回 body
                        body = objectMapper.writeValueAsBytes(objectNode);
                    }
                }
            }
            return execution.execute(request, body);
        }).requestFactory(requestFactory);
        ;

        // 构建并返回自定义的 DeepSeekApi
        return DeepSeekApi.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .restClientBuilder(builder)
                .build();
    }

    /*
    @Value("${spring.ai.deepseek.api-key}")
    private String deepseekApiKey;

    @Value("${spring.ai.deepseek.base-url}")
    private String deepseekBaseUrl;
    @Bean
    public DeepSeekChatModel deepSeekChatModel() {
        DeepSeekApi scopeApi = DeepSeekApi.builder().apiKey(deepseekApiKey).baseUrl(deepseekBaseUrl).build();
        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model("deepseek-v4-flash")
                .temperature(0.1)
                .maxTokens(8192)
                .build();
        return DeepSeekChatModel.builder().deepSeekApi(scopeApi).defaultOptions(options).build();
    }*/

    /**
     * Agent 会话检查点持久化。
     * 复用业务库（同一个 PostgreSQL 实例 + database）做 ReactAgent 状态持久化。
     * 建表通过 Flyway V4__graph_checkpoint.sql 完成，这里 createTables=false 避免重启冲突。
     */
    @Bean
    public PostgresSaver postgresSaver(
            @Value("${DB_HOST:127.0.0.1}") String host,
            @Value("${DB_PORT:5433}") int port,
            @Value("${DB_NAME:snap_learn}") String database,
            @Value("${DB_USER:postgres}") String user,
            @Value("${DB_PASSWORD}") String password) {
        return PostgresSaver.builder()
                .host(host)
                .port(port)
                .database(database)
                .user(user)
                .password(password)
                .createTables(false)
                .dropTablesFirst(false)
                .build();
    }

    /**
     * 基于 DeepSeek 的卡片组创建 Agent（单例）。
     * 组合两类工具：卡片组工具 + 长期记忆工具（MemoryChatTools 与 ChatClient 流程共用）。
     * userId 在请求时通过 RunnableConfig metadata 注入，工具内经 ToolContextHelper 读取。
     */
    @Bean
    public ReactAgent deepSeekCardGroupAgent(DeepSeekChatModel deepSeekChatModel,
                                             CardGroupAgentTools tools,
                                             MemoryChatTools memoryChatTools,
                                             PromptLoader promptLoader,
                                             PostgresSaver postgresSaver) {
        return ReactAgent.builder()
                .name("card-group-builder")
                .model(deepSeekChatModel)
                .systemPrompt(promptLoader.load("agent-system.st"))
                .methodTools(tools, memoryChatTools)
                .saver(postgresSaver)
                .build();
    }

    /**
     * 基于 DashScope 的卡片组创建 Agent（单例）。
     */
    @Bean
    public ReactAgent dashScopeCardGroupAgent(DashScopeChatModel dashScopeChatModel,
                                              CardGroupAgentTools tools,
                                              MemoryChatTools memoryChatTools,
                                              PromptLoader promptLoader,
                                              PostgresSaver postgresSaver) {
        return ReactAgent.builder()
                .name("card-group-builder")
                .model(dashScopeChatModel)
                .systemPrompt(promptLoader.load("agent-system.st"))
                .methodTools(tools, memoryChatTools)
                .saver(postgresSaver)
                .build();
    }

}
