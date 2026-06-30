package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_api_keys")
public class ApiKey {
    @TableId
    private String id;
    private String userId;
    private String name;
    private String keyHash;
    private String keyPrefix;
    private Boolean isActive;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
}
