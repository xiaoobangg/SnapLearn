package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("snap_feedback_replies")
public class FeedbackReply {
    @TableId
    private String id;
    private String feedbackId;
    private String content;
    private LocalDateTime createdAt;
}
