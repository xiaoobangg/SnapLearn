package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_random_test_pool")
public class RandomTestPool {
    @TableId
    private String id;
    private String wordId;
    private String questionType;
    private String userId;
    private Integer reviewCount;
    private String source; // auto / error
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
