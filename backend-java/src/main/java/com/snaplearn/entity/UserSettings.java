package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("snap_user_settings")
public class UserSettings {
    @TableId
    private String id;
    private String userId;
    private Integer dailyNewWords;
    private Integer dailyReviewWords;
    private Boolean checkinReminder;
    private LocalTime reminderTime;
    private String voiceId;
    private String chatMode;
    private String chatModel;
    private Boolean chatStream;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
