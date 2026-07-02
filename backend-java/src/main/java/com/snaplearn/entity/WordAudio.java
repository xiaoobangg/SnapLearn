package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_word_audios")
public class WordAudio {
    @TableId
    private String id;
    private String wordId;
    private String voiceId;
    private String audioType;
    private String audioUrl;
    private Integer durationMs;
    private Long fileSize;
    private LocalDateTime createdAt;
}
