package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("snap_card_kp_progress")
public class CardKpProgress {
    @TableId
    private String id;
    private String cardId;
    private String kpId;
    private String status; // unshown / shown / confirmed
}
