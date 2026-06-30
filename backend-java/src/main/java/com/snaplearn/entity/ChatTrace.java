package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 对话 trace 日志：每次 chatStream / chatStr 完成后写入一条，供 admin 端事后审查。
 */
@Data
@TableName("snap_chat_traces")
public class ChatTrace {
    @TableId
    private String id;
    private String userId;
    private String chatId;
    private String model;
    private String userMessage;
    private String responseText;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long durationMs;
    /** success / error */
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
