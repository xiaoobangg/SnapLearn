package com.snaplearn.service.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.stereotype.Component;

@Component
public class DocumentChatSubAgent {

    private final DeepSeekChatModel chatModel;
    private final DocumentQueryTools queryTools;

    public DocumentChatSubAgent(DeepSeekChatModel chatModel, DocumentQueryTools queryTools) {
        this.chatModel = chatModel;
        this.queryTools = queryTools;
    }

    /** 执行检索任务并返回结果 */
    public String execute(String message, String userId) {
        AgentContext.setUserId(userId);
        try {
            return ChatClient.builder(chatModel).build().prompt()
                .system("""
                        你是文档检索助手。根据用户问题搜索和阅读相关文档，给出有引用的回答。
                        步骤：
                        1. 用 searchDocuments 语义搜索相关文档
                        2. 用 getDocument 获取关键文档的完整内容
                        3. 基于文档内容回答用户问题，引用文档标题
                        4. 如果没找到相关文档，如实告知
                        """)
                .tools(queryTools)
                .user(message)
                .call().content();
        } finally {
            AgentContext.clear();
        }
    }
}
