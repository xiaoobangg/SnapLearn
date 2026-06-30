package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_knowledge_files")
public class KnowledgeFile {
    @TableId
    private String id;
    private String userId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private Integer chunkCount;
    private LocalDateTime uploadTime;
}
