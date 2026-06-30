package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.snaplearn.config.JsonbTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_test_questions")
public class TestQuestion {
    @TableId
    private String id;
    private String groupId;
    private String cardId;
    private String questionType;
    private String questionText;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String options; // JSON string → PostgreSQL jsonb
    private String correctAnswer;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
