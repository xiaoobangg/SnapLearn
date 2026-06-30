package com.snaplearn.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snaplearn.entity.ApiAccessLog;
import com.snaplearn.mapper.ApiAccessLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class ApiLogAspect {

    private final ApiAccessLogMapper logMapper;
    private final ObjectMapper objectMapper;

    @Around("execution(* com.snaplearn.controller..*(..))")
    public Object logAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        // 捕获请求参数
        String reqBody = "";
        try {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                reqBody = objectMapper.writeValueAsString(args);
                if (reqBody.length() > 2000) {
                    reqBody = reqBody.substring(0, 2000);
                }
            }
        } catch (Exception ignored) {}

        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;

        // 捕获返回值
        String respBody = "";
        try {
            if (result != null) {
                respBody = objectMapper.writeValueAsString(result);
                if (respBody.length() > 2000) {
                    respBody = respBody.substring(0, 2000);
                }
            }
        } catch (Exception ignored) {}

        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                Object uid = req.getAttribute("userId");
                ApiAccessLog log = new ApiAccessLog();
                log.setId(UUID.randomUUID().toString());
                log.setUserId(uid != null ? uid.toString() : null);
                log.setMethod(req.getMethod());
                log.setUri(req.getRequestURI());
                log.setIp(req.getRemoteAddr());
                log.setRequestBody(reqBody);
                log.setResponseBody(respBody);
                log.setDurationMs(duration);
                log.setCreatedAt(LocalDateTime.now());
                logMapper.insert(log);
            }
        } catch (Exception ignored) {}
        return result;
    }
}
