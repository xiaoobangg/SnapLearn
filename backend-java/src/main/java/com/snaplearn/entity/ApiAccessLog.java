package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_api_access_logs")
public class ApiAccessLog {
    @TableId
    private String id;
    private String userId;
    private String method;
    private String uri;
    private String ip;
    private String requestBody;
    private String responseBody;
    private Long durationMs;
    private LocalDateTime createdAt;
}
