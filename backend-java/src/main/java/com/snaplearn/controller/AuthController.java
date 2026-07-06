package com.snaplearn.controller;

import com.snaplearn.dto.request.DevLoginRequest;
import com.snaplearn.dto.request.LoginRequest;
import com.snaplearn.dto.response.AuthResponse;
import com.snaplearn.dto.response.UserProfileResponse;
import com.snaplearn.security.CurrentUser;
import com.snaplearn.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final com.snaplearn.service.AdminService adminService;

    @PostMapping("/web-login")
    public Map<String, Object> webLogin(@RequestBody Map<String, String> body) {
        return adminService.login(body.get("username"), body.get("password"));
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        return adminService.register(body.get("username"), body.get("password"));
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid LoginRequest req) {
        return authService.loginByWechatCode(req.code());
    }

    /** 微信登录：wx.login() code → openid（个人号可用） */
    @PostMapping("/wechat-login")
    public AuthResponse wechatLogin(@RequestBody @Valid LoginRequest req) {
        return authService.loginByWxOpenid(req.code());
    }

    @PostMapping("/dev-login")
    public AuthResponse devLogin(@RequestBody @Valid DevLoginRequest req) {
        return authService.devLogin(req.phone());
    }

    @GetMapping("/me")
    public UserProfileResponse me(@CurrentUser String userId) {
        return authService.getProfile(userId);
    }

    /** 更新用户个人资料（昵称/头像） */
    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@CurrentUser String userId,
                                              @RequestBody Map<String, String> body) {
        authService.updateProfile(userId,
                body.get("nickname"),
                body.get("avatar_url"));
        return Map.of("ok", true);
    }
}
