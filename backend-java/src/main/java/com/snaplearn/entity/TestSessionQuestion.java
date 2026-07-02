package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_test_session_questions")
public class TestSessionQuestion {
    @TableId
    private String id;
    private String groupId;
    private String cardId;
    private String questionId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
