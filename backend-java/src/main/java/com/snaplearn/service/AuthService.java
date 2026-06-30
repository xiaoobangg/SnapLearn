package com.snaplearn.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snaplearn.common.exception.BusinessException;
import com.snaplearn.config.AppProperties;
import com.snaplearn.dto.response.AuthResponse;
import com.snaplearn.dto.response.UserProfileResponse;
import com.snaplearn.entity.User;
import com.snaplearn.entity.UserRole;
import com.snaplearn.mapper.UserMapper;
import com.snaplearn.mapper.UserRoleMapper;
import com.snaplearn.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final JwtUtil jwtUtil;
    private final AppProperties appProperties;
    private final RestTemplate restTemplate;

    public AuthResponse loginByWechatCode(String code) {
        String phone = getPhoneByCode(code);
        return loginOrRegisterByPhone(phone);
    }

    /**
     * 个人号微信登录：wx.login() code → openid → 查找/创建用户
     */
    public AuthResponse loginByWxOpenid(String code) {
        String openid = getOpenidByWxCode(code);
        return loginOrRegisterByOpenid(openid);
    }

    public AuthResponse devLogin(String phone) {
        if (!appProperties.isDebug()) {
            throw new BusinessException(403, "仅开发模式可用");
        }
        return loginOrRegisterByPhone(phone);
    }

    public void updateProfile(String userId, String nickname, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(404, "用户不存在");
        if (nickname != null && !nickname.isBlank()) user.setNickname(nickname);
        if (avatarUrl != null && !avatarUrl.isBlank()) user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);
    }

    public UserProfileResponse getProfile(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return new UserProfileResponse(
                user.getId(),
                user.getPhone() != null ? user.getPhone() : "",
                user.getNickname() != null ? user.getNickname() : "",
                user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        );
    }

    private AuthResponse loginOrRegisterByPhone(String phone) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("phone", phone);
        User user = userMapper.selectOne(qw);

        if (user != null) {
            String token = jwtUtil.createToken(user.getId(), List.of("user"));
            return new AuthResponse(
                    user.getId(),
                    user.getPhone() != null ? user.getPhone() : phone,
                    user.getNickname() != null ? user.getNickname() : "",
                    false,
                    token
            );
        }

        String userId = UUID.randomUUID().toString();
        User newUser = new User();
        newUser.setId(userId);
        newUser.setPhone(phone);
        newUser.setNickname("");
        userMapper.insert(newUser);

        UserRole ur = new UserRole();
        ur.setId(UUID.randomUUID().toString());
        ur.setUserId(userId);
        ur.setRoleCode("user");
        userRoleMapper.insert(ur);

        String token = jwtUtil.createToken(userId, List.of("user"));
        return new AuthResponse(userId, phone, "", true, token);
    }

    private String getWechatAccessToken() {
        String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                appProperties.getWechat().getAppId(),
                appProperties.getWechat().getAppSecret()
        );
        try {
            JsonNode resp = restTemplate.getForObject(url, JsonNode.class);
            if (resp == null || resp.has("errcode") && resp.get("errcode").asInt() != 0) {
                String errmsg = resp != null ? resp.path("errmsg").asText("") : "";
                throw new BusinessException(502, "获取微信 access_token 失败: " + errmsg);
            }
            String token = resp.get("access_token").asText();
            if (token == null || token.isEmpty()) {
                throw new BusinessException(502, "获取微信 access_token 失败");
            }
            return token;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信 API 请求失败", e);
            throw new BusinessException(502, "微信服务暂不可用");
        }
    }

    private String getOpenidByWxCode(String code) {
        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appProperties.getWechat().getAppId(),
                appProperties.getWechat().getAppSecret(),
                code
        );

        try {
            // 1. 先获取字符串，兼容微信 text/plain 返回类型
            String responseStr = restTemplate.getForObject(url, String.class);
            JsonNode resp = new ObjectMapper().readTree(responseStr);

            // 2. 微信接口返回异常
            if (resp.has("errcode")) {
                int errCode = resp.get("errcode").asInt();
                String errMsg = resp.path("errmsg").asText("未知错误");

                log.error("jscode2session 错误 errcode={}, errmsg={}", errCode, errMsg);

                // 按微信官方错误码精细化提示
                String tip = switch (errCode) {
                    case -1 -> "微信服务器繁忙，请稍后再试";
                    case 40029 -> "登录凭证 code 无效或已过期，请重新登录";
                    case 45011 -> "登录频率过高，请稍后再试";
                    case 40125, 40013 -> "小程序 appid 配置错误";
                    case 40164 -> "请求 IP 不在白名单内，请检查微信公众平台";
                    default -> "微信登录失败：" + errMsg;
                };
                throw new BusinessException(401, tip);
            }

            // 3. 正常返回，取 openid
            String openid = resp.path("openid").asText("");
            if (openid.isBlank()) {
                throw new BusinessException(401, "微信登录失败：未获取到 openid");
            }

            return openid;

        } catch (BusinessException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("jscode2session 返回格式不是合法 JSON", e);
            throw new BusinessException(502, "微信登录响应格式异常");
        } catch (ResourceAccessException e) {
            log.error("jscode2session 网络请求失败", e);
            throw new BusinessException(502, "连接微信服务器失败，请检查网络");
        } catch (Exception e) {
            log.error("jscode2session 未知异常", e);
            throw new BusinessException(502, "微信登录服务异常");
        }
    }

    private AuthResponse loginOrRegisterByOpenid(String openid) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        qw.eq("wechat_openid", openid);
        User user = userMapper.selectOne(qw);

        if (user != null) {
            String token = jwtUtil.createToken(user.getId(), List.of("user"));
            return new AuthResponse(
                    user.getId(),
                    user.getPhone() != null ? user.getPhone() : "",
                    user.getNickname() != null ? user.getNickname() : "",
                    false,
                    token
            );
        }

        String userId = UUID.randomUUID().toString();
        User newUser = new User();
        newUser.setId(userId);
        newUser.setWechatOpenid(openid);
        newUser.setNickname("微信用户");
        userMapper.insert(newUser);

        UserRole ur = new UserRole();
        ur.setId(UUID.randomUUID().toString());
        ur.setUserId(userId);
        ur.setRoleCode("user");
        userRoleMapper.insert(ur);

        String token = jwtUtil.createToken(userId, List.of("user"));
        return new AuthResponse(userId, "", "微信用户", true, token);
    }

    private String getPhoneByCode(String code) {
        String accessToken = getWechatAccessToken();
        String url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;
        try {
            JsonNode resp = restTemplate.postForObject(url, Map.of("code", code), JsonNode.class);
            if (resp == null || resp.has("errcode") && resp.get("errcode").asInt() != 0) {
                String errmsg = resp != null ? resp.path("errmsg").asText("") : "";
                throw new BusinessException(401, "获取手机号失败: " + errmsg);
            }
            JsonNode phoneInfo = resp.get("phone_info");
            if (phoneInfo == null) {
                throw new BusinessException(401, "未能获取手机号");
            }
            String phone = phoneInfo.get("purePhoneNumber").asText("");
            if (phone.isEmpty()) {
                throw new BusinessException(401, "未能获取手机号");
            }
            return phone;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信手机号获取失败", e);
            throw new BusinessException(401, "获取手机号失败");
        }
    }
}
