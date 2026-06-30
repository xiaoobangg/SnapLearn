package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("snap_word_contents")
public class WordContent {
    @TableId
    private String id;
    private String wordId;
    private String pronunciation;
    private String pos;
    private String generalMeaning;
    private String extendedMeaning;
    private String exampleSentence;
    private String memoryTip;
    private String llmVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
