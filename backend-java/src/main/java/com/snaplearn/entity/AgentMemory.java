package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_agent_memories")
public class AgentMemory {
    @TableId
    private String id;
    private String userId;
    private String memoryKey;
    private String memoryValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
