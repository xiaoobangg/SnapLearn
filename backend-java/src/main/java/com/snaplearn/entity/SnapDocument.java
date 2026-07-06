package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_documents")
public class SnapDocument {
    @TableId
    private String id;
    private String userId;
    private String title;
    private String content;
    private String category;
    private String tags;
    private String status;          // draft / published / archived
    private String sourceType;      // md / word / pdf
    private String sourceName;
    private Long fileSize;
    private Integer sortOrder;
    private String knowledgeFileId;
    private String visibility;
    private String parentId;
    private String docType;        // folder / document
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
