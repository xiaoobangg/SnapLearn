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
@TableName("snap_cards")
public class Card {
    @TableId
    private String id;
    private String groupId;
    private String wordId;
    private String userId;
    private Integer sortOrder;
    private String cardStatus;
    private LocalDateTime createdAt;
}
