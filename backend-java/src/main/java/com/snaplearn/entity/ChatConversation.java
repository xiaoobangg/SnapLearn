package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_chat_conversations")
public class ChatConversation {
    @TableId
    private String id;
    private String userId;
    private String chatId;
    private String title;
    private LocalDateTime createdAt;
}
