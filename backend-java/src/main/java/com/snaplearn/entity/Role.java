package com.snaplearn.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("snap_roles")
public class Role {
    @TableId
    private String roleCode;
    private String roleName;
}
