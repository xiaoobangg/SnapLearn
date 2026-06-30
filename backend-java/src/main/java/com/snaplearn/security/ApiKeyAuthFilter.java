package com.snaplearn.security;

import com.snaplearn.entity.ApiKey;
import com.snaplearn.service.ApiKeyService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 检查 X-API-Key Header，通过后注入 userId 到 request attribute。
 * 仅在 /api/v1/coze/** 路径生效，由 WebMvcConfig 注册。
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter implements Filter {

    private final ApiKeyService apiKeyService;

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest request = (HttpServletRequest) req;
            HttpServletResponse response = (HttpServletResponse) resp;

            System.err.println("[API-KEY-FILTER] request: " + request.getMethod() + " " + request.getRequestURI());

            String rawKey = request.getHeader("X-API-Key");
            if (rawKey == null || rawKey.isBlank()) {
                System.err.println("[API-KEY-FILTER] missing X-API-Key header");
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"detail\":\"缺少 X-API-Key\"}");
                return;
            }

            if (apiKeyService == null) {
                System.err.println("[API-KEY-FILTER] apiKeyService is NULL!");
                response.setStatus(500);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"detail\":\"API Key 服务未初始化\"}");
                return;
            }

            ApiKey key = apiKeyService.authenticate(rawKey);
            if (key == null) {
                System.err.println("[API-KEY-FILTER] invalid key prefix: " + rawKey.substring(0, Math.min(5, rawKey.length())));
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"detail\":\"API Key 无效\"}");
                return;
            }

            request.setAttribute("userId", key.getUserId());
            System.err.println("[API-KEY-FILTER] authenticated userId=" + key.getUserId());
            chain.doFilter(req, resp);
        } catch (Exception e) {
            System.err.println("[API-KEY-FILTER] ERROR: " + e.getMessage());
            e.printStackTrace();
            HttpServletResponse response = (HttpServletResponse) resp;
            response.setStatus(500);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"detail\":\"" + e.getMessage() + "\"}");
        }
    }
}
