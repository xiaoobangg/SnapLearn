package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_user_daily_pool")
public class UserDailyPool {
    @TableId
    private String id;
    private String userId;
    private String bankId;
    private String wordId;
    private String poolStatus;
    private Integer intervalDays;
    private Integer reviewCount;
    private String lastMark;
    private LocalDateTime nextReviewAt;
    private LocalDateTime lastReviewAt;
    private LocalDateTime createdAt;
}
