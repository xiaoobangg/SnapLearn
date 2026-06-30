package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_knowledge_points")
public class KnowledgePoint {
    @TableId
    private String id;
    private String wordId;
    private String cardId;
    private String pointType;
    private String content;
    private Integer sortOrder;
    private String status;
    private LocalDateTime createdAt;
}
