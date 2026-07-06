package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.entity.User;
import com.snaplearn.entity.UserRole;
import com.snaplearn.mapper.UserMapper;
import com.snaplearn.mapper.UserRoleMapper;
import com.snaplearn.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    public static final String DEFAULT_ADMIN_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> login(String username, String password) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("username", username);
        User user = userMapper.selectOne(qw);
        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException(403, "账号已被禁用");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 获取用户角色列表
        QueryWrapper<UserRole> rq = new QueryWrapper<>();
        rq.eq("user_id", user.getId());
        List<String> roles = userRoleMapper.selectList(rq).stream()
                .map(UserRole::getRoleCode).toList();
        if (roles.isEmpty()) roles = List.of("user");

        String token = jwtUtil.createToken(user.getId(), roles);
        return Map.of(
                "token", token,
                "admin", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername() != null ? user.getUsername() : "",
                        "nickname", user.getNickname() != null ? user.getNickname() : "",
                        "role", roles.contains("admin") ? "admin" : "user"
                )
        );
    }

    /** Web 端注册：username + password → role=user */
    public Map<String, Object> register(String username, String password) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("username", username);
        if (userMapper.selectCount(qw) > 0) throw new BusinessException(400, "用户名已存在");

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setNickname(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setIsActive(true);
        userMapper.insert(user);

        UserRole ur = new UserRole();
        ur.setId(UUID.randomUUID().toString());
        ur.setUserId(user.getId());
        ur.setRoleCode("user");
        userRoleMapper.insert(ur);

        String token = jwtUtil.createToken(user.getId(), List.of("user"));
        return Map.of("token", token, "user", Map.of(
                "id", user.getId(), "username", username, "role", "user"));
    }

    public void deleteUser(String id) {
        // 同时删关联的角色
        QueryWrapper<UserRole> rq = new QueryWrapper<>();
        rq.eq("user_id", id);
        userRoleMapper.delete(rq);
        userMapper.deleteById(id);
    }

    public User getById(String id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException(404, "用户不存在");
        return user;
    }

    public void changePassword(String userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if (user.getPasswordHash() == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException(400, "原密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public void resetUserPassword(String userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public void ensureAdminExists() {
        QueryWrapper<UserRole> rq = new QueryWrapper<>();
        rq.eq("role_code", "admin");
        if (userRoleMapper.exists(rq)) return;

        // 创建默认管理员用户
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername("admin");
        user.setNickname("管理员");
        user.setEmail("admin@snaplearn.com");
        user.setPasswordHash(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
        user.setIsActive(true);
        userMapper.insert(user);

        UserRole ur = new UserRole();
        ur.setId(UUID.randomUUID().toString());
        ur.setUserId(user.getId());
        ur.setRoleCode("admin");
        userRoleMapper.insert(ur);
    }
}
