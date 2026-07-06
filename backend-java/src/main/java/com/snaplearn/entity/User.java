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
@TableName("snap_users")
public class User {
    @TableId
    private String id;
    private String username;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private String wechatOpenid;
    private String email;
    private String passwordHash;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
