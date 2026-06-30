package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_word_banks")
public class WordBank {
    @TableId
    private String id;
    private String name;
    private String type;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
}
