package com.snaplearn.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, 401, "需要登录");
            return false;
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = jwtUtil.validateAndGetClaims(token);
            String userId = claims.getSubject();
            String rolesStr = claims.get("roles", String.class);
            List<String> roles = rolesStr != null ? Arrays.asList(rolesStr.split(",")) : List.of();

            String path = request.getRequestURI();
            if (path.startsWith("/api/v1/admin/")) {
                if (!roles.contains("admin")) {
                    sendError(response, 403, "需要管理员权限");
                    return false;
                }
            }

            request.setAttribute("userId", userId);
            request.setAttribute("roles", roles);
            return true;
        } catch (ExpiredJwtException e) {
            sendError(response, 401, "Token expired");
            return false;
        } catch (JwtException e) {
            sendError(response, 401, "Invalid token");
            return false;
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(Map.of("detail", message)));
    }
}
