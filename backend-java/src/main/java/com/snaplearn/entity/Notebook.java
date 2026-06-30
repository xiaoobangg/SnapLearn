package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("snap_notebook")
@Deprecated // v2.0 — replaced by UserDailyPool
public class Notebook {
    @TableId
    private String id;
    private String userId;
    private String cardId;
    private String status;
    private Double easeFactor;
    private Integer intervalDays;
    private Integer repetitions;
    private LocalDateTime nextReviewAt;
    private LocalDateTime lastReviewAt;
    private LocalDateTime createdAt;
}
