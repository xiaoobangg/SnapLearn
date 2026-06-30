package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_test_attempts")
public class TestAttempt {
    @TableId
    private String id;
    private String questionId;
    private String userId;
    private String userAnswer;
    private Boolean isCorrect;
    private Integer attemptRound;
    private LocalDateTime createdAt;
}
