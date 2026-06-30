package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("snap_user_roles")
public class UserRole {
    @TableId
    private String id;
    private String userId;
    private String roleCode;
}
