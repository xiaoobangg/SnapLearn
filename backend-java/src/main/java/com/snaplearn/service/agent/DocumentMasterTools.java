package com.snaplearn.service.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentMasterTools {

    private final DocumentEditSubAgent editSubAgent;
    private final DocumentChatSubAgent chatSubAgent;

    @Tool(description = """
            当用户想要创建、修改、新增、更新、追加、删除文档时调用此工具。
            将用户的完整需求传递给编辑子Agent处理，返回操作结果。
            """)
    public String callEditAgent(
            @ToolParam(description = "用户的完整需求描述，包含要创建/修改的内容、目标文件夹或文档名称") String message,
            ToolContext ctx) {
        String userId = UserIdResolver.resolve(ctx);
        return editSubAgent.execute(message, userId);
    }

    @Tool(description = """
            当用户想要搜索、查询、阅读、检索、查找、了解文档内容时调用此工具。
            将用户的搜索问题传递给检索子Agent处理，返回检索结果和回答。
            """)
    public String callChatAgent(
            @ToolParam(description = "用户的搜索问题或查询内容") String message,
            ToolContext ctx) {
        String userId = UserIdResolver.resolve(ctx);
        return chatSubAgent.execute(message, userId);
    }
}
