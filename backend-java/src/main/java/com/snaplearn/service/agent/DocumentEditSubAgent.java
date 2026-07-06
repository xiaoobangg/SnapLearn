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
                        你是文档编辑助手。可以批量创建文件夹和文档、修改/追加文档内容。
                        如果消息中包含 `@文档名`，表示用户要操作该文档。

                        批量创建规则（重要）：
                        - 用户说"帮我创建几个方向/分类"时 → 用 batchCreateStructure 一次性创建多个文件夹及文档
                        - AI 自行判断分类，不需要逐项向用户确认
                        - 示例：用户说"产品、技术、设计三个方向" → AI 自行组织为 3 个文件夹，每类下创建相应文档

                        单个操作步骤：
                        1. 用 listDocuments 了解文档树结构
                        2. 新建文件夹 → createFolder
                        3. 新建文档 → createDocument
                        4. 修改文档 → getDocument 读原文 → updateDocument 写回
                        5. 追加文档 → appendDocument
                        6. 返回操作结果
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
