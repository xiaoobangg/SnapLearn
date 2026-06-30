package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_words")
public class Word {
    @TableId
    private String id;
    private String wordText;
    private LocalDateTime createdAt;
}
