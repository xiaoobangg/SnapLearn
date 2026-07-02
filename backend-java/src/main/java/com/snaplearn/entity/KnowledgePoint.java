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
    private String pointType;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
