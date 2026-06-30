package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_error_book")
public class ErrorBook {
    @TableId
    private String id;
    private String groupId;
    private String cardId;
    private String userId;
    private String testAttemptId;
    private Boolean resolved;
    private LocalDateTime createdAt;
}
