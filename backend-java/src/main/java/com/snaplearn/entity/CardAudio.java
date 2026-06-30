package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_card_audios")
public class CardAudio {
    @TableId
    private String id;
    private String cardId;
    private String voiceId;
    /** 'word' | 'example' */
    private String audioType;
    private String audioUrl;
    private Integer durationMs;
    private Long fileSize;
    private LocalDateTime createdAt;
}
