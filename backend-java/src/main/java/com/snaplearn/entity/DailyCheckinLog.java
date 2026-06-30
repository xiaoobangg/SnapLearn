package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("snap_daily_checkin_log")
public class DailyCheckinLog {
    @TableId
    private String id;
    private String userId;
    private String bankId;
    private LocalDate checkinDate;
    private Integer newWordsCount;
    private Integer reviewWordsCount;
    private Integer knownCount;
    private Integer fuzzyCount;
    private Integer unknownCount;
    private LocalDateTime createdAt;
}
