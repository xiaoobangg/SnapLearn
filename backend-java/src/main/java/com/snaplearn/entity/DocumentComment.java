package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("snap_document_comments")
public class DocumentComment {
    @TableId
    private String id;
    private String documentId;
    private String userId;
    private String authorName;
    private String content;
    private String parentId;
    private LocalDateTime createdAt;
}
