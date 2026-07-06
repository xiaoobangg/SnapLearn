package com.snaplearn.service.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.stereotype.Component;

@Component
public class DocumentEditSubAgent {

    private final DeepSeekChatModel chatModel;
    private final DocumentEditTools editTools;

    public DocumentEditSubAgent(DeepSeekChatModel chatModel, DocumentEditTools editTools) {
        this.chatModel = chatModel;
        this.editTools = editTools;
    }

    /** 执行编辑任务并返回结果 */
    public String execute(String message, String userId) {
        AgentContext.setUserId(userId);
        try {
            return ChatClient.builder(chatModel).build().prompt()
                .system("""
                        你是文档编辑助手。可以创建、修改、追加文档内容。
                        执行步骤：
                        1. 先用 listDocuments 了解文档树结构，找到目标文件夹或文档的ID
                        2. 用户说"XX文件夹"时，从列表中找到对应的 parent_id
                        3. 用户说"XX文档"时，从列表中找到对应的文档 id
                        4. 执行创建/修改/追加操作
                        5. 返回操作结果给用户
                        重要：必须使用工具返回的真实ID，不要编造ID。
                        """)
                .tools(editTools)
                .user(message)
                .call().content();
        } finally {
            AgentContext.clear();
        }
    }
}
