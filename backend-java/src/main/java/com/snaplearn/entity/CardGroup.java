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
@TableName("snap_card_groups")
public class CardGroup {
    @TableId
    private String id;
    private String userId;
    private String sourceImage;
    private String sourceText;
    private String title;
    private String groupStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
